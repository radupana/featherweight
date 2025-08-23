package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.ProgrammeTextParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ImportProgrammeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val parser = ProgrammeTextParser()

    private val _uiState = MutableStateFlow(ImportProgrammeUiState())
    val uiState: StateFlow<ImportProgrammeUiState> = _uiState

    fun updateInputText(text: String) {
        _uiState.value =
            _uiState.value.copy(
                inputText = text,
                error = null,
            )
    }

    fun setParsedProgramme(
        programme: ParsedProgramme,
        parseRequestId: Long? = null,
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

            // Create parse request in database and return immediately
            val requestId = repository.createParseRequest(rawText)

            // Clear the input
            _uiState.value =
                _uiState.value.copy(
                    inputText = "",
                    error = null,
                    isLoading = false,
                    successMessage = null, // Don't show message since we're navigating
                )

            // Start async processing in background
            processProgrammeAsync(requestId)

            // Navigate to Programmes screen immediately
            onNavigateToProgrammes()
        }
    }

    private fun processProgrammeAsync(requestId: Long) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                val allMaxes = repository.getAllCurrentMaxesWithNames(userId).first()

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
                    Log.e("ImportProgrammeViewModel", "Parse failed: $errorMessage")

                    // Update parse request with failure
                    repository.updateParseRequest(
                        parseRequest.copy(
                            status = ParseStatus.FAILED,
                            error = errorMessage,
                            completedAt = java.time.LocalDateTime.now(),
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e("ImportProgrammeViewModel", "Error processing programme", e)

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
        val matchedExerciseCache = mutableMapOf<String, Long?>()

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
            Log.d("ImportProgrammeViewModel", "Found ${unmatchedExercises.size} unmatched exercises")
        }

        return programme.copy(
            weeks = updatedWeeks,
            unmatchedExercises = unmatchedExercises.toList(),
        )
    }

    private fun findBestExerciseMatch(
        exerciseName: String,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseVariationWithAliases>,
    ): Long? {
        val nameLower = exerciseName.lowercase().trim()
        Log.d("ImportProgrammeViewModel", "=== EXERCISE MATCHING START ===")
        Log.d("ImportProgrammeViewModel", "Looking for: '$exerciseName' (normalized: '$nameLower')")

        // 1. Try exact match on name (case-insensitive)
        allExercises.find { it.name.lowercase() == nameLower }?.let {
            Log.d("ImportProgrammeViewModel", "Exact name match found: ${it.name} (ID: ${it.id})")
            return it.id
        }

        // 2. Try exact match on aliases (case-insensitive)
        allExercises
            .find { exercise ->
                exercise.aliases.any { alias -> alias.lowercase() == nameLower }
            }?.let {
                Log.d("ImportProgrammeViewModel", "Exact alias match found: ${it.name} (ID: ${it.id})")
                return it.id
            }

        // Extract equipment type from the exercise name
        val inputEquipment = extractEquipment(nameLower)

        // 3. Try to match with variations preserved (e.g., "Barbell Paused Bench Press")
        // Look for exercises that contain all important words
        val importantWords = nameLower.split(" ").filter { it.length > 2 }

        // Find exercises that contain all the important words
        // For exact word matches, we're more lenient with equipment
        allExercises
            .find { exercise ->
                val exerciseLower = exercise.name.lowercase()
                val exerciseEquipment = extractEquipment(exerciseLower)

                // For high-confidence matches (all words present), allow some equipment flexibility
                // but still prevent obvious mismatches like "dumbbell" to "barbell"
                val equipmentOk =
                    when {
                        inputEquipment == null || exerciseEquipment == null -> true
                        inputEquipment == exerciseEquipment -> true
                        // Block only the most egregious mismatches
                        inputEquipment == "dumbbell" && exerciseEquipment == "barbell" -> false
                        inputEquipment == "barbell" && exerciseEquipment == "dumbbell" -> false
                        else -> true // Allow cable/machine variations etc
                    }

                equipmentOk && importantWords.all { word -> exerciseLower.contains(word) }
            }?.let {
                Log.d("ImportProgrammeViewModel", "Important words match found: ${it.name} (ID: ${it.id})")
                return it.id
            }

        // 3. Special handling for variations like "Paused or Pin"
        if (nameLower.contains(" or ")) {
            // Try each variation separately
            val variations = nameLower.split(" or ")
            for (variation in variations) {
                val varTrimmed = variation.trim()
                allExercises
                    .find { exercise ->
                        exercise.name.lowercase().contains(varTrimmed)
                    }?.let { return it.id }
            }
        }

        // 4. Try with normalized equipment names but keep variations
        val nameWithVariations =
            nameLower
                .replace("weighted ", "") // For "Weighted Dips" -> "Dips"
                .trim()

        allExercises
            .find {
                it.name.lowercase().contains(nameWithVariations)
            }?.let { return it.id }

        // 5. Strip equipment and try again (last resort)
        val nameWithoutEquipment =
            nameLower
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

        // IMPORTANT: For Cable exercises, prefer exact Cable matches
        // Don't match "Cable Fly" to "Cable Rear Delt Fly"
        if (nameLower.startsWith("cable ")) {
            // For cable exercises, only match if the core movement matches exactly
            allExercises
                .find { exercise ->
                    val exName = exercise.name.lowercase()
                    exName.startsWith("cable ") &&
                        exName.removePrefix("cable ").trim() == nameWithoutEquipment
                }?.let { return it.id }
        } else {
            // For non-cable exercises, try to find exercise that ends with the core movement
            allExercises
                .find {
                    it.name.lowercase().endsWith(nameWithoutEquipment)
                }?.let { return it.id }
        }

        // 6. Handle common abbreviations and variations
        val normalizedName = normalizeExerciseName(nameLower)
        allExercises
            .find {
                normalizeExerciseName(it.name.lowercase()) == normalizedName
            }?.let {
                Log.d("ImportProgrammeViewModel", "Normalized name match found: ${it.name} (ID: ${it.id})")
                return it.id
            }

        // 7. No match found
        Log.d("ImportProgrammeViewModel", "NO MATCH FOUND for: $exerciseName")
        Log.d("ImportProgrammeViewModel", "=== EXERCISE MATCHING END ===")
        return null
    }

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

    fun clearParsedProgramme() {
        _uiState.value =
            _uiState.value.copy(
                parsedProgramme = null,
                parseRequestId = null,
            )
    }

    fun clearAll() {
        _uiState.value = ImportProgrammeUiState() // Reset to initial state
    }

    fun updateParsedWorkout(
        weekIndex: Int,
        workoutIndex: Int,
        updatedWorkout: ParsedWorkout,
    ) {
        Log.d("ImportProgrammeViewModel", "updateParsedWorkout: week=$weekIndex, workout=$workoutIndex, updatedWorkout=$updatedWorkout")
        val currentProgramme = _uiState.value.parsedProgramme
        if (currentProgramme == null) {
            Log.e("ImportProgrammeViewModel", "Current programme is null!")
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

    fun setExerciseMappings(mappings: Map<String, Long?>) {
        _uiState.value = _uiState.value.copy(exerciseMappings = mappings)
    }

    fun applyExerciseMappings() {
        val programme = _uiState.value.parsedProgramme ?: return
        val mappings = _uiState.value.exerciseMappings

        // Log mappings (only IDs, not names to avoid async issues)
        mappings.forEach { (exerciseName, mappedId) ->
            if (mappedId != null) {
                Log.d("ImportProgrammeViewModel", "Mapping: '$exerciseName' → Exercise ID $mappedId")
            } else {
                Log.d("ImportProgrammeViewModel", "Mapping: '$exerciseName' → Create as custom exercise")
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
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create programme: ${e.message}",
                    )
            }
        }
    }

    private suspend fun createProgrammeFromParsed(parsedProgramme: ParsedProgramme): Long {
        Log.d("ImportProgrammeViewModel", "Creating programme: ${parsedProgramme.name} (${parsedProgramme.durationWeeks} weeks)")

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

        Log.d("ImportProgrammeViewModel", "Built JSON structure with ${parsedProgramme.weeks.sumOf { week -> week.workouts.sumOf { it.exercises.size } }} total exercises")

        // Convert string programme type to enum
        val programmeType =
            try {
                com.github.radupana.featherweight.data.programme.ProgrammeType
                    .valueOf(parsedProgramme.programmeType)
            } catch (e: IllegalArgumentException) {
                com.github.radupana.featherweight.data.programme.ProgrammeType.GENERAL_FITNESS
            }

        // Convert string difficulty to enum
        val difficulty =
            try {
                com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
                    .valueOf(parsedProgramme.difficulty)
            } catch (e: IllegalArgumentException) {
                com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.INTERMEDIATE
            }

        val programmeId =
            repository.createImportedProgramme(
                name = parsedProgramme.name,
                description = parsedProgramme.description,
                durationWeeks = parsedProgramme.durationWeeks,
                jsonStructure = programmeStructure,
                programmeType = programmeType,
                difficulty = difficulty,
            )

        Log.d("ImportProgrammeViewModel", "Programme created with ID: $programmeId")

        repository.activateProgramme(programmeId)
        Log.d("ImportProgrammeViewModel", "Programme activated successfully")
        Log.d("ImportProgrammeViewModel", "=== END PROGRAMME CREATION ===")

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
        val weeks =
            programme.weeks.map { week ->
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
    val parseRequestId: Long? = null, // Track which parse request this came from
    val exerciseMappings: Map<String, Long?> = emptyMap(), // Store user's exercise mappings
)
