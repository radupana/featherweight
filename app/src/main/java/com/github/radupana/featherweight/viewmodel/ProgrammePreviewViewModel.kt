package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.AIProgrammeRepository
import com.github.radupana.featherweight.service.*
import com.github.radupana.featherweight.ui.dialogs.UnmatchedExerciseDialog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class ProgrammePreviewViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val aiProgrammeRepository = AIProgrammeRepository(
        database.aiProgrammeRequestDao(),
        WorkManager.getInstance(application)
    )
    private val validator = ProgrammeValidator()
    private val exerciseMatcher = ExerciseNameMatcher()
    private val aiService = AIProgrammeService(application)

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Loading)
    val previewState = _previewState.asStateFlow()

    private val _selectedWeek = MutableStateFlow(1)
    val selectedWeek = _selectedWeek.asStateFlow()

    private val _editStates = MutableStateFlow<Map<String, ExerciseEditState>>(emptyMap())
    val editStates = _editStates.asStateFlow()

    private val _currentPreview = MutableStateFlow<GeneratedProgrammePreview?>(null)

    private val _unmatchedExercises = MutableStateFlow<List<ExerciseMatchingService.UnmatchedExercise>>(emptyList())
    val unmatchedExercises = _unmatchedExercises.asStateFlow()
    
    private val _showUnmatchedDialog = MutableStateFlow(false)
    val showUnmatchedDialog = _showUnmatchedDialog.asStateFlow()
    
    private val _currentUnmatchedExercise = MutableStateFlow<ExerciseMatchingService.UnmatchedExercise?>(null)
    val currentUnmatchedExercise = _currentUnmatchedExercise.asStateFlow()
    
    private val _allExercises = MutableStateFlow<List<com.github.radupana.featherweight.data.exercise.Exercise>>(emptyList())
    val allExercises = _allExercises.asStateFlow()
    
    fun loadGeneratedProgramme(response: AIProgrammeResponse, validationResult: ProgrammeValidationResult? = null) {
        viewModelScope.launch {
            try {
                _previewState.value = PreviewState.Loading

                response.programme?.let { generatedProgramme ->
                    // Check if we have unmatched exercises from validation
                    if (validationResult != null && validationResult.unmatchedExercises.isNotEmpty()) {
                        _unmatchedExercises.value = validationResult.unmatchedExercises
                        
                        // Show info about unmatched exercises
                        val unmatchedCount = validationResult.unmatchedExercises.size
                        val totalCount = validationResult.totalCount
                        
                        println("📋 Programme has $unmatchedCount unmatched exercises out of $totalCount total")
                    }
                    
                    // Get all available exercises for matching
                    val allExercises = repository.getAllExercises()

                    // Process and validate the programme
                    val preview = processGeneratedProgramme(generatedProgramme, allExercises, validationResult)

                    // Debug logging to understand week count
                    println("🔍 ProgrammePreviewViewModel: Generated programme has ${generatedProgramme.weeks.size} weeks")
                    println("🔍 ProgrammePreviewViewModel: Preview has ${preview.weeks.size} weeks")
                    preview.weeks.forEach { week ->
                        println("  - Week ${week.weekNumber}: ${week.workouts.size} workouts, ${week.weeklyVolume.totalSets} sets")
                    }

                    _currentPreview.value = preview
                    _previewState.value = PreviewState.Success(preview)
                } ?: run {
                    _previewState.value =
                        PreviewState.Error(
                            response.error ?: "Failed to generate programme",
                        )
                }
            } catch (e: Exception) {
                _previewState.value =
                    PreviewState.Error(
                        "Error processing programme: ${e.message}",
                    )
            }
        }
    }

    private suspend fun processGeneratedProgramme(
        generated: GeneratedProgramme,
        allExercises: List<ExerciseWithDetails>,
        validationResult: ProgrammeValidationResult? = null
    ): GeneratedProgrammePreview {
        // Convert generated weeks to preview format
        val weeks =
            generated.weeks.map { week ->
                val weekWorkouts =
                    week.workouts.map { workout ->
                        processWorkout(workout, allExercises)
                    }
                WeekPreview(
                    weekNumber = week.weekNumber,
                    workouts = weekWorkouts,
                    weeklyVolume = calculateWeekVolume(weekWorkouts),
                    progressionNotes = "${week.name}: ${week.description}",
                    intensityLevel = week.intensityLevel,
                    volumeLevel = week.volumeLevel,
                    isDeload = week.isDeload,
                )
            }

        // Create exercise match info
        val exerciseMatches = mutableListOf<ExerciseMatchInfo>()
        weeks.flatMap { it.workouts }.flatMap { it.exercises }.forEach { exercise ->
            val matches =
                exerciseMatcher.findBestMatches(
                    exercise.exerciseName,
                    allExercises.map { it.exercise.name },
                )
            exerciseMatches.add(
                ExerciseMatchInfo(
                    tempId = exercise.tempId,
                    originalName = exercise.exerciseName,
                    matches = matches,
                ),
            )
        }

        val preview =
            GeneratedProgrammePreview(
                name = generated.name,
                description = generated.description,
                durationWeeks = generated.durationWeeks,
                daysPerWeek = generated.daysPerWeek,
                focus = listOf(ProgrammeGoal.BUILD_STRENGTH), // Default goal
                volumeLevel = VolumeLevel.MODERATE, // Default volume
                weeks = weeks,
                validationResult = ValidationResult(),
                exerciseMatches = exerciseMatches,
                metadata =
                    GenerationMetadata(
                        generatedAt = System.currentTimeMillis(),
                        modelUsed = "gpt-4.1-mini",
                        tokensUsed = 0, // Would need to be passed from AI service
                        generationTimeMs = 0, // Would need to be tracked by AI service
                        userInputSummary = "AI Generated Programme",
                    ),
            )

        // Validate the programme (including exercise resolution)
        val baseValidationResult = validator.validate(preview)
        val enhancedValidationResult = addExerciseResolutionValidation(preview, baseValidationResult)

        return preview.copy(validationResult = enhancedValidationResult)
    }

    private suspend fun processWorkout(
        workout: GeneratedWorkout,
        allExercises: List<ExerciseWithDetails>,
    ): WorkoutPreview {
        val exercises =
            workout.exercises.map { exercise ->
                val tempId = UUID.randomUUID().toString()
                val matches = exerciseMatcher.findBestMatches(exercise.exerciseName, allExercises.map { it.exercise.name })
                val bestMatch = matches.firstOrNull()

                ExercisePreview(
                    tempId = tempId,
                    exerciseName = exercise.exerciseName,
                    matchedExerciseId = allExercises.find { it.exercise.name == bestMatch?.exerciseName }?.exercise?.id,
                    matchConfidence = bestMatch?.confidence ?: 0f,
                    sets = exercise.sets,
                    repsMin = exercise.repsMin,
                    repsMax = exercise.repsMax,
                    rpe = exercise.rpe,
                    restSeconds = exercise.restSeconds,
                    notes = exercise.notes,
                    suggestedWeight = exercise.suggestedWeight,
                    weightSource = exercise.weightSource,
                    alternatives =
                        matches.drop(1).take(2).map { match ->
                            ExerciseAlternative(
                                exerciseId = allExercises.find { it.exercise.name == match.exerciseName }?.exercise?.id ?: 0L,
                                name = match.exerciseName,
                                confidence = match.confidence,
                                reason = match.reason,
                            )
                        },
                )
            }

        val estimatedDuration = calculateWorkoutDuration(exercises)

        return WorkoutPreview(
            dayNumber = workout.dayNumber,
            name = workout.name,
            exercises = exercises,
            estimatedDuration = estimatedDuration,
        )
    }

    private fun calculateWeekVolume(workouts: List<WorkoutPreview>): com.github.radupana.featherweight.data.VolumeMetrics {
        val totalSets =
            workouts.sumOf { workout ->
                workout.exercises.sumOf { it.sets }
            }
        val totalReps =
            workouts.sumOf { workout ->
                workout.exercises.sumOf { exercise ->
                    exercise.sets * ((exercise.repsMin + exercise.repsMax) / 2)
                }
            }

        return com.github.radupana.featherweight.data.VolumeMetrics(
            totalSets = totalSets,
            totalReps = totalReps,
            muscleGroupVolume = emptyMap(), // TODO: Calculate
            movementPatternVolume = emptyMap(), // TODO: Calculate
        )
    }

    private fun calculateWorkoutDuration(exercises: List<ExercisePreview>): Int {
        // Estimate: 3 minutes per set + 1 minute between exercises
        val setTime = exercises.sumOf { it.sets } * 3
        val transitionTime = exercises.size * 1
        return setTime + transitionTime
    }

    fun selectWeek(weekNumber: Int) {
        _selectedWeek.value = weekNumber
    }

    fun resolveExercise(
        tempId: String,
        exerciseId: Long,
    ) {
        viewModelScope.launch {
            val currentPreview = _currentPreview.value ?: return@launch
            val exercise = repository.getExerciseById(exerciseId)

            if (exercise != null) {
                val updatedPreview =
                    updateExerciseInPreview(currentPreview, tempId) { exercisePreview ->
                        exercisePreview.copy(
                            matchedExerciseId = exerciseId,
                            exerciseName = exercise.exercise.name,
                            matchConfidence = 1.0f,
                        )
                    }

                if (updatedPreview != null) {
                    _currentPreview.value = updatedPreview
                    _previewState.value = PreviewState.Success(updatedPreview)
                    validateProgramme() // Re-validate after exercise resolution
                }
            }
        }
    }

    fun swapExercise(
        tempId: String,
        newExerciseName: String,
    ) {
        viewModelScope.launch {
            val currentPreview = _currentPreview.value ?: return@launch
            val allExercises = repository.getAllExercises()
            val matches = exerciseMatcher.findBestMatches(newExerciseName, allExercises.map { it.exercise.name })
            val bestMatch = matches.firstOrNull()

            val updatedPreview =
                updateExerciseInPreview(currentPreview, tempId) { exercisePreview ->
                    exercisePreview.copy(
                        exerciseName = newExerciseName,
                        matchedExerciseId = allExercises.find { it.exercise.name == bestMatch?.exerciseName }?.exercise?.id,
                        matchConfidence = bestMatch?.confidence ?: 0f,
                        alternatives =
                            matches.drop(1).take(2).map { match ->
                                ExerciseAlternative(
                                    exerciseId = allExercises.find { it.exercise.name == match.exerciseName }?.exercise?.id ?: 0L,
                                    name = match.exerciseName,
                                    confidence = match.confidence,
                                    reason = match.reason,
                                )
                            },
                    )
                }

            if (updatedPreview != null) {
                _currentPreview.value = updatedPreview
                _previewState.value = PreviewState.Success(updatedPreview)
                validateProgramme()
            }
        }
    }

    fun updateExercise(action: QuickEditAction.UpdateExercise) {
        val currentPreview = _currentPreview.value ?: return

        val updatedPreview =
            updateExerciseInPreview(currentPreview, action.tempId) { exercise ->
                exercise.copy(
                    sets = action.sets ?: exercise.sets,
                    repsMin = action.repsMin ?: exercise.repsMin,
                    repsMax = action.repsMax ?: exercise.repsMax,
                    rpe = action.rpe ?: exercise.rpe,
                    restSeconds = action.restSeconds ?: exercise.restSeconds,
                )
            }

        if (updatedPreview != null) {
            _currentPreview.value = updatedPreview
            _previewState.value = PreviewState.Success(updatedPreview)
            validateProgramme()
        }
    }

    private fun updateExerciseInPreview(
        preview: GeneratedProgrammePreview,
        tempId: String,
        updateFn: (ExercisePreview) -> ExercisePreview,
    ): GeneratedProgrammePreview? {
        val updatedWeeks =
            preview.weeks.map { week ->
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        if (exercise.tempId == tempId) {
                                            updateFn(exercise)
                                        } else {
                                            exercise
                                        }
                                    },
                            )
                        },
                )
            }

        return preview.copy(weeks = updatedWeeks)
    }

    fun validateProgramme() {
        val currentPreview = _currentPreview.value ?: return
        val validationResult = validator.validate(currentPreview)

        // Add exercise resolution validation
        val enhancedValidationResult = addExerciseResolutionValidation(currentPreview, validationResult)

        val updatedPreview = currentPreview.copy(validationResult = enhancedValidationResult)
        _currentPreview.value = updatedPreview
        _previewState.value = PreviewState.Success(updatedPreview)
    }

    private fun addExerciseResolutionValidation(
        preview: GeneratedProgrammePreview,
        baseValidation: ValidationResult,
    ): ValidationResult {
        val additionalErrors = mutableListOf<ValidationError>()
        val additionalWarnings = mutableListOf<ValidationWarning>()

        // Check for unresolved exercises
        val unresolvedExercises =
            preview.weeks
                .flatMap { it.workouts }
                .flatMap { it.exercises }
                .filter { it.matchedExerciseId == null || it.matchConfidence < 0.7f }

        if (unresolvedExercises.isNotEmpty()) {
            additionalErrors.add(
                ValidationError(
                    message = "Some exercises need to be resolved",
                    category = ValidationCategory.EXERCISE_SELECTION,
                    requiredAction = "Scroll down and click on exercises highlighted in red to resolve them",
                    isAutoFixable = false,
                ),
            )
        }

        // Check for low confidence matches
        val lowConfidenceExercises =
            preview.weeks
                .flatMap { it.workouts }
                .flatMap { it.exercises }
                .filter { it.matchedExerciseId != null && it.matchConfidence < 0.8f && it.matchConfidence >= 0.7f }

        if (lowConfidenceExercises.isNotEmpty()) {
            additionalWarnings.add(
                ValidationWarning(
                    message = "Some exercises have low confidence matches",
                    category = ValidationCategory.EXERCISE_SELECTION,
                    suggestion = "Review and confirm these exercise selections",
                ),
            )
        }

        val allErrors = baseValidation.errors + additionalErrors
        val allWarnings = baseValidation.warnings + additionalWarnings

        // Calculate score more intelligently - penalize unresolved exercises but don't make it 0%
        val exerciseResolutionPenalty = additionalErrors.size * 0.2f // 20% per unresolved exercise
        val newScore = (baseValidation.score - exerciseResolutionPenalty).coerceAtLeast(0.0f)

        return ValidationResult(
            warnings = allWarnings,
            errors = allErrors,
            score = newScore,
        )
    }

    fun regenerate() {
        viewModelScope.launch {
            try {
                _previewState.value = PreviewState.Loading

                val currentPreview = _currentPreview.value
                if (currentPreview == null) {
                    _previewState.value = PreviewState.Error("No programme to regenerate")
                    return@launch
                }

                // Regeneration requires calling the AI service with specific parameters
                // based on the RegenerationMode. This would need to be implemented
                // in the AIProgrammeService with appropriate prompts for each mode.
                _previewState.value =
                    PreviewState.Error(
                        "Programme regeneration is not yet implemented. Please generate a new programme instead.",
                    )
            } catch (e: Exception) {
                _previewState.value =
                    PreviewState.Error(
                        "Failed to regenerate: ${e.message}",
                    )
            }
        }
    }

    fun activateProgramme(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val currentPreview = _currentPreview.value ?: return@launch

                // Set activating state to show loading indicator
                _previewState.value = PreviewState.Activating()

                // Create the programme in the database
                val programmeId = repository.createAIGeneratedProgramme(currentPreview)
                println("✅ AI programme created and activated with ID: $programmeId")

                // Delete the AI request to prevent it from showing up as "Ready to preview"
                val aiRequestId = GeneratedProgrammeHolder.getAIRequestId()
                if (aiRequestId != null) {
                    println("🗑️ Deleting AI request: $aiRequestId")
                    aiProgrammeRepository.deleteRequest(aiRequestId)
                    println("✅ AI request deleted successfully")
                } else {
                    println("⚠️ No AI request ID found to delete")
                }

                // Clear the generated programme holder
                GeneratedProgrammeHolder.clearGeneratedProgramme()

                // Navigate immediately to home screen
                onSuccess()
            } catch (e: Exception) {
                println("❌ Failed to activate programme: ${e.message}")
                val currentPreview = _currentPreview.value
                // If activation fails, go back to Success state
                if (currentPreview != null) {
                    _previewState.value = PreviewState.Success(currentPreview)
                }
                _previewState.value =
                    PreviewState.Error(
                        "Failed to activate programme: ${e.message}",
                    )
            }
        }
    }

    fun toggleExerciseEdit(tempId: String) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)

        _editStates.value = currentStates + (
            tempId to
                currentState.copy(
                    isEditing = !currentState.isEditing,
                )
        )
    }

    fun showExerciseAlternatives(
        tempId: String,
        show: Boolean,
    ) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)

        _editStates.value = currentStates + (
            tempId to
                currentState.copy(
                    showAlternatives = show,
                )
        )
    }

    fun showExerciseResolution(
        tempId: String,
        show: Boolean,
    ) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)

        _editStates.value = currentStates + (
            tempId to
                currentState.copy(
                    showResolution = show,
                )
        )
    }

    fun updateProgrammeName(newName: String) {
        val currentPreview = _currentPreview.value ?: return
        val updatedPreview = currentPreview.copy(name = newName)

        _currentPreview.value = updatedPreview
        _previewState.value = PreviewState.Success(updatedPreview)
    }
    
    fun selectExerciseForUnmatched(unmatchedExercise: ExerciseMatchingService.UnmatchedExercise, selectedExercise: com.github.radupana.featherweight.data.exercise.Exercise) {
        viewModelScope.launch {
            try {
                // Update the programme preview with the selected exercise
                val currentPreview = _currentPreview.value ?: return@launch
                val updatedPreview = replaceUnmatchedExercise(currentPreview, unmatchedExercise, selectedExercise)
                
                // Update unmatched exercises list
                _unmatchedExercises.value = _unmatchedExercises.value.filter { 
                    it != unmatchedExercise 
                }
                
                // Update state
                _currentPreview.value = updatedPreview
                _previewState.value = PreviewState.Success(updatedPreview)
                
                // Close dialog
                _showUnmatchedDialog.value = false
                _currentUnmatchedExercise.value = null
                
                println("✅ Replaced '${unmatchedExercise.aiSuggested}' with '${selectedExercise.name}'")
            } catch (e: Exception) {
                println("❌ Failed to replace exercise: ${e.message}")
            }
        }
    }
    
    fun showUnmatchedExerciseDialog(unmatchedExercise: ExerciseMatchingService.UnmatchedExercise) {
        _currentUnmatchedExercise.value = unmatchedExercise
        _showUnmatchedDialog.value = true
    }
    
    fun hideUnmatchedExerciseDialog() {
        _showUnmatchedDialog.value = false
        _currentUnmatchedExercise.value = null
    }
    
    private fun replaceUnmatchedExercise(
        preview: GeneratedProgrammePreview,
        unmatchedExercise: ExerciseMatchingService.UnmatchedExercise,
        selectedExercise: com.github.radupana.featherweight.data.exercise.Exercise
    ): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.mapIndexed { weekIndex, week ->
            if (weekIndex + 1 == unmatchedExercise.weekNumber) {
                week.copy(
                    workouts = week.workouts.mapIndexed { workoutIndex, workout ->
                        if (workoutIndex + 1 == unmatchedExercise.workoutNumber) {
                            workout.copy(
                                exercises = workout.exercises.mapIndexed { exerciseIndex, exercise ->
                                    if (exerciseIndex == unmatchedExercise.exerciseIndex &&
                                        exercise.exerciseName == unmatchedExercise.aiSuggested) {
                                        // Replace with selected exercise
                                        exercise.copy(
                                            exerciseName = selectedExercise.name,
                                            matchedExerciseId = selectedExercise.id,
                                            matchConfidence = 1.0f
                                        )
                                    } else exercise
                                }
                            )
                        } else workout
                    }
                )
            } else week
        }
        
        return preview.copy(weeks = updatedWeeks)
    }
    
    fun loadExercises() {
        viewModelScope.launch {
            try {
                val exercises = repository.getAllExercises()
                _allExercises.value = exercises.map { it.exercise }
            } catch (e: Exception) {
                println("Failed to load exercises: ${e.message}")
            }
        }
    }

    fun applyBulkEdit(action: QuickEditAction) {
        val currentPreview = _currentPreview.value ?: return

        val updatedPreview =
            when (action) {
                is QuickEditAction.AdjustVolume -> adjustAllVolume(currentPreview, action.factor)
                is QuickEditAction.ShiftSchedule -> adjustSchedule(currentPreview, action.newDaysPerWeek)
                is QuickEditAction.ChangeFocus -> changeFocus(currentPreview, action.newGoal)
                is QuickEditAction.SimplifyForBeginner -> simplifyForBeginner(currentPreview)
                is QuickEditAction.AddProgressiveOverload -> addProgressiveOverload(currentPreview)
                else -> currentPreview // Other actions don't apply to bulk edits
            }

        if (updatedPreview != currentPreview) {
            _currentPreview.value = updatedPreview
            _previewState.value = PreviewState.Success(updatedPreview)
            validateProgramme()
        }
    }

    private fun adjustAllVolume(
        preview: GeneratedProgrammePreview,
        factor: Float,
    ): GeneratedProgrammePreview {
        val updatedWeeks =
            preview.weeks.map { week ->
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        val newSets = (exercise.sets * factor).toInt().coerceAtLeast(1).coerceAtMost(6)
                                        exercise.copy(sets = newSets)
                                    },
                            )
                        },
                )
            }
        return preview.copy(weeks = updatedWeeks)
    }

    private fun adjustSchedule(
        preview: GeneratedProgrammePreview,
        newDaysPerWeek: Int,
    ): GeneratedProgrammePreview {
        // For now, just update the metadata. Full schedule restructuring would be complex
        return preview.copy(daysPerWeek = newDaysPerWeek)
    }

    private fun changeFocus(
        preview: GeneratedProgrammePreview,
        newGoal: ProgrammeGoal,
    ): GeneratedProgrammePreview = preview.copy(focus = listOf(newGoal))

    private fun simplifyForBeginner(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks =
            preview.weeks.map { week ->
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        exercise.copy(
                                            sets = exercise.sets.coerceAtMost(3), // Max 3 sets for beginners
                                            repsMin = exercise.repsMin.coerceAtLeast(8), // Higher rep ranges
                                            repsMax = exercise.repsMax.coerceAtLeast(12),
                                            rpe = exercise.rpe?.let { it.coerceAtMost(7.5f) }, // Lower intensity
                                            restSeconds = exercise.restSeconds.coerceAtLeast(90), // Longer rest
                                        )
                                    },
                            )
                        },
                )
            }
        return preview.copy(weeks = updatedWeeks)
    }

    private fun addProgressiveOverload(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks =
            preview.weeks.mapIndexed { weekIndex, week ->
                val progressionFactor = 1.0f + (weekIndex * 0.05f) // 5% increase per week
                week.copy(
                    workouts =
                        week.workouts.map { workout ->
                            workout.copy(
                                exercises =
                                    workout.exercises.map { exercise ->
                                        exercise.copy(
                                            rpe =
                                                exercise.rpe?.let {
                                                    (it + weekIndex * 0.2f).coerceAtMost(9.5f)
                                                }, // Gradually increase intensity
                                        )
                                    },
                            )
                        },
                )
            }
        return preview.copy(weeks = updatedWeeks)
    }

    fun autoFixValidationIssue() {
        // Auto-fix functionality requires AI service to regenerate with specific constraints
        // TODO: Implement when AI service supports targeted fixes
    }
}
