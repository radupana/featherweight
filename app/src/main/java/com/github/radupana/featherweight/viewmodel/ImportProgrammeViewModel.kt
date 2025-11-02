package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.ProgrammeTextParser
import com.github.radupana.featherweight.util.CloudLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ImportProgrammeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ImportProgrammeViewModel"
    }

    private val repository = FeatherweightRepository(application)
    private val parser = ProgrammeTextParser()

    private val _uiState = MutableStateFlow(ImportProgrammeUiState())
    val uiState: StateFlow<ImportProgrammeUiState> = _uiState

    fun updateInputText(
        text: String,
        editingRequestId: String? = null,
    ) {
        _uiState.value =
            _uiState.value.copy(
                inputText = text,
                error = null,
                editingFailedRequestId = editingRequestId,
            )
    }

    fun setParsedProgramme(
        programme: ParsedProgramme,
        parseRequestId: String? = null,
    ) {
        _uiState.value =
            _uiState.value.copy(
                parsedProgramme = programme,
                inputText = "",
                error = null,
                isLoading = false,
                parseRequestId = parseRequestId,
            )
    }

    fun parseProgramme(onNavigateToProgrammes: () -> Unit) {
        viewModelScope.launch {
            val rawText = _uiState.value.inputText
            if (rawText.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Please enter programme text",
                    )
                return@launch
            }

            // Check for existing pending parse request (but allow if we're editing a failed one)
            val editingRequestId = _uiState.value.editingFailedRequestId

            if (editingRequestId == null && repository.hasPendingParseRequest()) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Please complete your pending programme review before importing another.",
                    )
                return@launch
            }

            // If we're editing a failed request, delete the old one first
            if (editingRequestId != null) {
                val oldRequest = repository.getParseRequest(editingRequestId)
                if (oldRequest != null) {
                    repository.deleteParseRequest(oldRequest)
                }
            }

            // Create parse request in database and return immediately
            val requestId = repository.createParseRequest(rawText)

            // Clear the input
            _uiState.value =
                _uiState.value.copy(
                    inputText = "",
                    error = null,
                    isLoading = false,
                    successMessage = null, // Don't show message since we're navigating
                    editingFailedRequestId = null, // Clear the editing ID
                )

            // Start async processing in background
            processProgrammeAsync(requestId)

            // Navigate to Programmes screen immediately
            onNavigateToProgrammes()
        }
    }

    private fun processProgrammeAsync(requestId: String) {
        viewModelScope.launch {
            try {
                val allMaxes = repository.getAllCurrentMaxesWithNames().first()

                val userMaxesMap =
                    allMaxes.associate {
                        it.exerciseName to it.oneRMEstimate
                    }

                val parseRequest = repository.getParseRequest(requestId) ?: return@launch

                val request =
                    TextParsingRequest(
                        rawText = parseRequest.rawText,
                        userMaxes = userMaxesMap,
                    )

                val result = parser.parseText(request)

                if (result.success && result.programme != null) {
                    val matchedProgramme = matchExercises(result.programme)

                    // Update parse request with success
                    repository.updateParseRequest(
                        parseRequest.copy(
                            status = ParseStatus.COMPLETED,
                            resultJson =
                                com.google.gson
                                    .Gson()
                                    .toJson(matchedProgramme),
                            completedAt = java.time.LocalDateTime.now(),
                        ),
                    )
                } else {
                    val errorMessage = result.error ?: "Failed to parse programme"
                    CloudLogger.error("ImportProgrammeViewModel", "Parse failed: $errorMessage")

                    // Update parse request with failure
                    repository.updateParseRequest(
                        parseRequest.copy(
                            status = ParseStatus.FAILED,
                            error = errorMessage,
                            completedAt = java.time.LocalDateTime.now(),
                        ),
                    )
                }
            } catch (e: IllegalStateException) {
                CloudLogger.error("ImportProgrammeViewModel", "Error processing programme", e)

                val parseRequest = repository.getParseRequest(requestId)
                if (parseRequest != null) {
                    repository.updateParseRequest(
                        parseRequest.copy(
                            status = ParseStatus.FAILED,
                            error = "Error: ${e.message}",
                            completedAt = java.time.LocalDateTime.now(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun matchExercises(programme: ParsedProgramme): ParsedProgramme {
        val allExercisesWithAliases = repository.getAllExercisesWithAliases()
        val unmatchedExercises = mutableSetOf<String>()
        val matchedExerciseCache = mutableMapOf<String, String?>()

        val updatedWeeks =
            programme.weeks.map { week ->
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        // Check cache first
                                        if (matchedExerciseCache.containsKey(exercise.exerciseName)) {
                                            val cachedId = matchedExerciseCache[exercise.exerciseName]
                                            if (cachedId != null) {
                                                exercise.copy(matchedExerciseId = cachedId)
                                            } else {
                                                exercise
                                            }
                                        } else {
                                            // Try fuzzy matching
                                            val matchedId = findBestExerciseMatch(exercise.exerciseName, allExercisesWithAliases)
                                            matchedExerciseCache[exercise.exerciseName] = matchedId

                                            if (matchedId != null) {
                                                exercise.copy(matchedExerciseId = matchedId)
                                            } else {
                                                unmatchedExercises.add(exercise.exerciseName)
                                                exercise
                                            }
                                        }
                                    },
                            )
                        },
                )
            }

        // Log summary of unmatched exercises
        if (unmatchedExercises.isNotEmpty()) {
            CloudLogger.debug("ImportProgrammeViewModel", "Found ${unmatchedExercises.size} unmatched exercises")
        }

        return programme.copy(
            weeks = updatedWeeks,
            unmatchedExercises = unmatchedExercises.toList(),
        )
    }

    private fun findBestExerciseMatch(
        exerciseName: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        val nameLower = exerciseName.lowercase().trim()
        CloudLogger.debug("ImportProgrammeViewModel", "=== EXERCISE MATCHING START ===")
        CloudLogger.debug("ImportProgrammeViewModel", "Looking for: '$exerciseName' (normalized: '$nameLower')")

        // Try matching strategies in order of preference
        val matchedId =
            tryExactNameMatch(nameLower, allExercises)
                ?: tryExactAliasMatch(nameLower, allExercises)
                ?: tryImportantWordsMatch(nameLower, allExercises)
                ?: tryVariationMatch(nameLower, allExercises)
                ?: tryNormalizedMatch(nameLower, allExercises)
                ?: tryEquipmentStrippedMatch(nameLower, allExercises)
                ?: tryAbbreviationMatch(nameLower, allExercises)

        if (matchedId == null) {
            CloudLogger.debug("ImportProgrammeViewModel", "NO MATCH FOUND for: $exerciseName")
        }
        CloudLogger.debug("ImportProgrammeViewModel", "=== EXERCISE MATCHING END ===")
        return matchedId
    }

    private fun tryExactNameMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? =
        allExercises.find { it.name.lowercase() == nameLower }?.let {
            CloudLogger.debug("ImportProgrammeViewModel", "Exact name match found: ${it.name} (ID: ${it.id})")
            it.id
        }

    private fun tryExactAliasMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? =
        allExercises
            .find { exercise ->
                exercise.aliases.any { alias -> alias.lowercase() == nameLower }
            }?.let {
                CloudLogger.debug("ImportProgrammeViewModel", "Exact alias match found: ${it.name} (ID: ${it.id})")
                it.id
            }

    private fun tryImportantWordsMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        val inputEquipment = extractEquipment(nameLower)
        val importantWords = nameLower.split(" ").filter { it.length > 2 }

        return allExercises
            .find { exercise ->
                val exerciseLower = exercise.name.lowercase()
                val exerciseEquipment = extractEquipment(exerciseLower)
                val equipmentOk = isEquipmentCompatible(inputEquipment, exerciseEquipment)
                equipmentOk && importantWords.all { word -> exerciseLower.contains(word) }
            }?.let {
                CloudLogger.debug("ImportProgrammeViewModel", "Important words match found: ${it.name} (ID: ${it.id})")
                it.id
            }
    }

    private fun isEquipmentCompatible(
        inputEquipment: String?,
        exerciseEquipment: String?,
    ): Boolean =
        when {
            inputEquipment == null || exerciseEquipment == null -> true
            inputEquipment == exerciseEquipment -> true
            inputEquipment == "dumbbell" && exerciseEquipment == "barbell" -> false
            inputEquipment == "barbell" && exerciseEquipment == "dumbbell" -> false
            else -> true
        }

    private fun tryVariationMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        if (!nameLower.contains(" or ")) return null

        val variations = nameLower.split(" or ")
        for (variation in variations) {
            val varTrimmed = variation.trim()
            allExercises
                .find { exercise ->
                    exercise.name.lowercase().contains(varTrimmed)
                }?.let { return it.id }
        }
        return null
    }

    private fun tryNormalizedMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        val nameWithVariations = nameLower.replace("weighted ", "").trim()
        return allExercises
            .find {
                it.name.lowercase().contains(nameWithVariations)
            }?.id
    }

    private fun tryEquipmentStrippedMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        val nameWithoutEquipment = stripEquipmentFromName(nameLower)

        // Special handling for cable exercises
        if (nameLower.startsWith("cable ")) {
            return allExercises
                .find { exercise ->
                    val exName = exercise.name.lowercase()
                    exName.startsWith("cable ") &&
                        exName.removePrefix("cable ").trim() == nameWithoutEquipment
                }?.id
        }

        // For non-cable exercises, try to find exercise that ends with the core movement
        return allExercises
            .find {
                it.name.lowercase().endsWith(nameWithoutEquipment)
            }?.id
    }

    private fun tryAbbreviationMatch(
        nameLower: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases>,
    ): String? {
        val normalizedName = normalizeExerciseName(nameLower)
        return allExercises
            .find {
                normalizeExerciseName(it.name.lowercase()) == normalizedName
            }?.let {
                CloudLogger.debug("ImportProgrammeViewModel", "Normalized name match found: ${it.name} (ID: ${it.id})")
                it.id
            }
    }

    private fun stripEquipmentFromName(name: String): String =
        name
            .replace("barbell ", "")
            .replace("dumbbell ", "")
            .replace("db ", "")
            .replace("cable ", "")
            .replace("machine ", "")
            .replace("smith ", "")
            .replace("kettlebell ", "")
            .replace("kb ", "")
            .replace("band ", "")
            .replace("resistance ", "")
            .replace("paused ", "")
            .replace("pin ", "")
            .trim()

    private fun extractEquipment(exerciseName: String): String? {
        val name = exerciseName.lowercase()
        return when {
            name.startsWith("barbell ") || name.startsWith("bb ") -> "barbell"
            name.startsWith("dumbbell ") || name.startsWith("db ") -> "dumbbell"
            name.startsWith("cable ") -> "cable"
            name.startsWith("machine ") -> "machine"
            name.startsWith("smith ") -> "smith"
            name.startsWith("kettlebell ") || name.startsWith("kb ") -> "kettlebell"
            name.startsWith("band ") || name.startsWith("resistance ") -> "band"
            name.startsWith("weighted ") -> "weighted"
            else -> null
        }
    }

    private fun normalizeExerciseName(name: String): String =
        name
            .replace("ohp", "overhead press")
            .replace("rdl", "romanian deadlift")
            .replace("sldl", "stiff leg deadlift")
            .replace("ghr", "glute ham raise")
            .replace("db", "dumbbell")
            .replace("bb", "barbell")
            .replace("kb", "kettlebell")
            .replace("&", "and")
            .replace("-", " ")
            .replace("  ", " ")
            .trim()

    fun clearAll() {
        _uiState.value = ImportProgrammeUiState() // Reset to initial state
    }

    fun updateParsedWorkout(
        weekIndex: Int,
        workoutIndex: Int,
        updatedWorkout: ParsedWorkout,
    ) {
        CloudLogger.debug("ImportProgrammeViewModel", "updateParsedWorkout: week=$weekIndex, workout=$workoutIndex, updatedWorkout=$updatedWorkout")
        val currentProgramme = _uiState.value.parsedProgramme
        if (currentProgramme == null) {
            CloudLogger.error("ImportProgrammeViewModel", "Current programme is null!")
            return
        }

        val updatedWeeks = currentProgramme.weeks.toMutableList()
        val week = updatedWeeks[weekIndex]

        val updatedWorkouts = week.workouts.toMutableList()
        updatedWorkouts[workoutIndex] = updatedWorkout

        updatedWeeks[weekIndex] = week.copy(workouts = updatedWorkouts)

        _uiState.value =
            _uiState.value.copy(
                parsedProgramme = currentProgramme.copy(weeks = updatedWeeks),
            )
    }

    fun getParsedWorkout(
        weekIndex: Int,
        workoutIndex: Int,
    ): ParsedWorkout? {
        val programme = _uiState.value.parsedProgramme ?: return null
        return programme.weeks
            .getOrNull(weekIndex)
            ?.workouts
            ?.getOrNull(workoutIndex)
    }

    fun setExerciseMappings(mappings: Map<String, String?>) {
        _uiState.value = _uiState.value.copy(exerciseMappings = mappings)
    }

    fun applyExerciseMappings() {
        val programme = _uiState.value.parsedProgramme ?: return
        val mappings = _uiState.value.exerciseMappings

        // Log mappings (only IDs, not names to avoid async issues)
        mappings.forEach { (exerciseName, mappedId) ->
            if (mappedId != null) {
                CloudLogger.debug("ImportProgrammeViewModel", "Mapping: '$exerciseName' → Exercise ID $mappedId")
            } else {
                CloudLogger.debug("ImportProgrammeViewModel", "Mapping: '$exerciseName' → Create as custom exercise")
            }
        }

        // Apply the mappings to the parsed programme
        val updatedWeeks =
            programme.weeks.map { week ->
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        // If this exercise has a mapping, apply it
                                        val mappedId = mappings[exercise.exerciseName]
                                        if (mappedId != null) {
                                            exercise.copy(matchedExerciseId = mappedId)
                                        } else if (mappings.containsKey(exercise.exerciseName)) {
                                            // User chose to create as custom (mapping exists but ID is null)
                                            exercise.copy(matchedExerciseId = null)
                                        } else {
                                            exercise
                                        }
                                    },
                            )
                        },
                )
            }

        // Update the programme with mappings applied
        _uiState.value =
            _uiState.value.copy(
                parsedProgramme =
                    programme.copy(
                        weeks = updatedWeeks,
                        unmatchedExercises = emptyList(), // Clear unmatched since user has mapped them
                    ),
            )
    }

    fun confirmAndCreateProgramme(onSuccess: () -> Unit = {}) {
        val programme = _uiState.value.parsedProgramme ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                createProgrammeFromParsed(programme)
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        parsedProgramme = null,
                        inputText = "",
                        exerciseMappings = emptyMap(),
                    )
                // Navigate after successful creation
                onSuccess()
            } catch (e: IllegalArgumentException) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create programme: ${e.message}",
                    )
            }
        }
    }

    private suspend fun createProgrammeFromParsed(parsedProgramme: ParsedProgramme): String {
        CloudLogger.debug("ImportProgrammeViewModel", "Creating programme: ${parsedProgramme.name} (${parsedProgramme.durationWeeks} weeks)")

        // Validation
        require(parsedProgramme.weeks.isNotEmpty()) { "Programme must have at least one week" }
        require(parsedProgramme.durationWeeks > 0) { "Programme duration must be positive" }

        // Validate each week has workouts
        parsedProgramme.weeks.forEach { week ->
            require(week.workouts.isNotEmpty()) { "Week ${week.weekNumber} has no workouts" }

            // Validate each workout has exercises
            week.workouts.forEach { workout ->
                require(workout.exercises.isNotEmpty()) {
                    "Week ${week.weekNumber}, ${workout.dayOfWeek ?: "Day ${week.workouts.indexOf(workout) + 1}"} has no exercises"
                }

                // Validate each exercise has sets
                workout.exercises.forEach { exercise ->
                    require(exercise.sets.isNotEmpty()) {
                        "Exercise '${exercise.exerciseName}' has no sets"
                    }
                }
            }
        }

        val programmeStructure = buildProgrammeJson(parsedProgramme)

        CloudLogger.debug("ImportProgrammeViewModel", "Built JSON structure with ${parsedProgramme.weeks.sumOf { week -> week.workouts.sumOf { it.exercises.size } }} total exercises")

        // Convert string programme type to enum
        val programmeType =
            try {
                com.github.radupana.featherweight.data.programme.ProgrammeType
                    .valueOf(parsedProgramme.programmeType)
            } catch (e: IllegalArgumentException) {
                CloudLogger.warn(TAG, "Unknown programme type: ${parsedProgramme.programmeType}, defaulting to GENERAL_FITNESS", e)
                com.github.radupana.featherweight.data.programme.ProgrammeType.GENERAL_FITNESS
            }

        // Convert string difficulty to enum
        val difficulty =
            try {
                com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
                    .valueOf(parsedProgramme.difficulty)
            } catch (e: IllegalArgumentException) {
                CloudLogger.warn(TAG, "Unknown difficulty: ${parsedProgramme.difficulty}, defaulting to INTERMEDIATE", e)
                com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.INTERMEDIATE
            }

        CloudLogger.debug("ImportProgrammeViewModel", "=== CREATING PROGRAMME IN REPOSITORY ===")
        CloudLogger.debug("ImportProgrammeViewModel", "Calling repository.createImportedProgramme with:")
        CloudLogger.debug("ImportProgrammeViewModel", "  name: ${parsedProgramme.name}")
        CloudLogger.debug("ImportProgrammeViewModel", "  durationWeeks: ${parsedProgramme.durationWeeks}")
        CloudLogger.debug("ImportProgrammeViewModel", "  jsonStructure length: ${programmeStructure.length} chars")

        val programmeId =
            repository.createImportedProgramme(
                name = parsedProgramme.name,
                description = parsedProgramme.description,
                durationWeeks = parsedProgramme.durationWeeks,
                jsonStructure = programmeStructure,
                programmeType = programmeType,
                difficulty = difficulty,
            )

        CloudLogger.debug("ImportProgrammeViewModel", "=== PROGRAMME CREATED ===")
        CloudLogger.debug("ImportProgrammeViewModel", "Programme ID: $programmeId")
        CloudLogger.debug("ImportProgrammeViewModel", "SUCCESS: Programme '${parsedProgramme.name}' created with ID $programmeId")

        repository.activateProgramme(programmeId)
        CloudLogger.debug("ImportProgrammeViewModel", "Programme activated successfully")
        CloudLogger.debug("ImportProgrammeViewModel", "=== END PROGRAMME CREATION ===")

        // Mark the parse request as IMPORTED if we have one
        _uiState.value.parseRequestId?.let { requestId ->
            val parseRequest = repository.getParseRequest(requestId)
            parseRequest?.let {
                repository.updateParseRequest(
                    it.copy(
                        status = ParseStatus.IMPORTED,
                        completedAt = java.time.LocalDateTime.now(),
                    ),
                )
            }
        }

        return programmeId
    }

    private fun buildProgrammeJson(programme: ParsedProgramme): String {
        CloudLogger.debug("ImportProgrammeViewModel", "=== BUILDING PROGRAMME JSON ===")
        CloudLogger.debug("ImportProgrammeViewModel", "Programme: ${programme.name}")

        val weeks =
            programme.weeks.map { week ->
                CloudLogger.debug("ImportProgrammeViewModel", "Processing Week ${week.weekNumber}: ${week.name}")

                mapOf(
                    "weekNumber" to week.weekNumber,
                    "name" to week.name,
                    "description" to week.description,
                    "focusAreas" to week.focusAreas,
                    "intensityLevel" to week.intensityLevel,
                    "volumeLevel" to week.volumeLevel,
                    "isDeload" to week.isDeload,
                    "phase" to week.phase,
                    "workouts" to
                        week.workouts.map { workout ->
                            CloudLogger.debug("ImportProgrammeViewModel", "  Processing Workout: ${workout.name} (${workout.dayOfWeek})")
                            CloudLogger.debug("ImportProgrammeViewModel", "    Exercise count: ${workout.exercises.size}")

                            workout.exercises.forEachIndexed { index, exercise ->
                                CloudLogger.debug("ImportProgrammeViewModel", "    Exercise ${index + 1}: ${exercise.exerciseName}")
                                CloudLogger.debug("ImportProgrammeViewModel", "      matchedExerciseId: ${exercise.matchedExerciseId}")
                                CloudLogger.debug("ImportProgrammeViewModel", "      sets: ${exercise.sets.size}")
                            }

                            mapOf(
                                "name" to workout.name,
                                "dayOfWeek" to workout.dayOfWeek,
                                "estimatedDurationMinutes" to workout.estimatedDurationMinutes,
                                "exercises" to
                                    workout.exercises.map { exercise ->
                                        mapOf(
                                            "exerciseName" to exercise.exerciseName,
                                            "exerciseId" to exercise.matchedExerciseId,
                                            "sets" to
                                                exercise.sets.map { set ->
                                                    mapOf(
                                                        "reps" to set.reps,
                                                        "weight" to set.weight,
                                                        "rpe" to set.rpe,
                                                    )
                                                },
                                        )
                                    },
                            )
                        },
                )
            }

        val totalExercises =
            programme.weeks.sumOf { week ->
                week.workouts.sumOf { it.exercises.size }
            }
        val exercisesWithIds =
            programme.weeks.sumOf { week ->
                week.workouts.sumOf { workout ->
                    workout.exercises.count { it.matchedExerciseId != null }
                }
            }

        CloudLogger.debug("ImportProgrammeViewModel", "=== PROGRAMME JSON SUMMARY ===")
        CloudLogger.debug("ImportProgrammeViewModel", "Total weeks: ${programme.weeks.size}")
        CloudLogger.debug("ImportProgrammeViewModel", "Total workouts: ${programme.weeks.sumOf { it.workouts.size }}")
        CloudLogger.debug("ImportProgrammeViewModel", "Total exercises: $totalExercises")
        CloudLogger.debug("ImportProgrammeViewModel", "Exercises with matched IDs: $exercisesWithIds")
        CloudLogger.debug("ImportProgrammeViewModel", "Exercises WITHOUT matched IDs: ${totalExercises - exercisesWithIds}")

        return com.google.gson
            .Gson()
            .toJson(mapOf("weeks" to weeks))
    }
}

data class ImportProgrammeUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val parsedProgramme: ParsedProgramme? = null,
    val successMessage: String? = null,
    val parseRequestId: String? = null, // Track which parse request this came from
    val exerciseMappings: Map<String, String?> = emptyMap(), // Store user's exercise mappings
    val editingFailedRequestId: String? = null, // Track which failed request we're editing
)
