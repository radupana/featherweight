package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

class ProgrammePreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val validator = ProgrammeValidator()
    private val exerciseMatcher = ExerciseNameMatcher()
    private val aiService = AIProgrammeService()
    
    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Loading)
    val previewState = _previewState.asStateFlow()
    
    private val _selectedWeek = MutableStateFlow(1)
    val selectedWeek = _selectedWeek.asStateFlow()
    
    private val _editStates = MutableStateFlow<Map<String, ExerciseEditState>>(emptyMap())
    val editStates = _editStates.asStateFlow()
    
    private val _currentPreview = MutableStateFlow<GeneratedProgrammePreview?>(null)
    
    fun loadGeneratedProgramme(response: AIProgrammeResponse) {
        viewModelScope.launch {
            try {
                _previewState.value = PreviewState.Loading
                
                response.programme?.let { generatedProgramme ->
                    // Get all available exercises for matching
                    val allExercises = repository.getAllExercises()
                    
                    // Process and validate the programme
                    val preview = processGeneratedProgramme(generatedProgramme, allExercises)
                    
                    _currentPreview.value = preview
                    _previewState.value = PreviewState.Success(preview)
                } ?: run {
                    _previewState.value = PreviewState.Error(
                        response.error ?: "Failed to generate programme"
                    )
                }
            } catch (e: Exception) {
                _previewState.value = PreviewState.Error(
                    "Error processing programme: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun processGeneratedProgramme(
        generated: GeneratedProgramme,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithDetails>
    ): GeneratedProgrammePreview {
        // Convert generated workouts to preview format
        val weeks = generated.workouts.groupBy { (it.dayNumber - 1) / generated.daysPerWeek + 1 }
            .map { (weekNum, workouts) ->
                val weekWorkouts = workouts.map { workout ->
                    processWorkout(workout, allExercises)
                }
                WeekPreview(
                    weekNumber = weekNum,
                    workouts = weekWorkouts,
                    weeklyVolume = calculateWeekVolume(weekWorkouts),
                    progressionNotes = if (weekNum == 1) "Week 1: Establish baseline" else null
                )
            }
        
        // Create exercise match info
        val exerciseMatches = mutableListOf<ExerciseMatchInfo>()
        weeks.flatMap { it.workouts }.flatMap { it.exercises }.forEach { exercise ->
            val matches = exerciseMatcher.findBestMatches(
                exercise.exerciseName,
                allExercises.map { it.exercise.name }
            )
            exerciseMatches.add(
                ExerciseMatchInfo(
                    tempId = exercise.tempId,
                    originalName = exercise.exerciseName,
                    matches = matches
                )
            )
        }
        
        val preview = GeneratedProgrammePreview(
            name = generated.name,
            description = generated.description,
            durationWeeks = generated.durationWeeks,
            daysPerWeek = generated.daysPerWeek,
            focus = listOf(ProgrammeGoal.BUILD_STRENGTH), // TODO: Infer from description
            volumeLevel = VolumeLevel.MODERATE, // TODO: Calculate from actual volume
            weeks = weeks,
            validationResult = ValidationResult(),
            exerciseMatches = exerciseMatches,
            metadata = GenerationMetadata(
                generatedAt = System.currentTimeMillis(),
                modelUsed = "mock",
                tokensUsed = 0,
                generationTimeMs = 0,
                userInputSummary = "Generated programme"
            )
        )
        
        // Validate the programme (including exercise resolution)
        val baseValidationResult = validator.validate(preview)
        val enhancedValidationResult = addExerciseResolutionValidation(preview, baseValidationResult)
        
        return preview.copy(validationResult = enhancedValidationResult)
    }
    
    private suspend fun processWorkout(
        workout: GeneratedWorkout,
        allExercises: List<com.github.radupana.featherweight.data.exercise.ExerciseWithDetails>
    ): WorkoutPreview {
        val exercises = workout.exercises.map { exercise ->
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
                alternatives = matches.drop(1).take(2).map { match ->
                    ExerciseAlternative(
                        exerciseId = allExercises.find { it.exercise.name == match.exerciseName }?.exercise?.id ?: 0L,
                        name = match.exerciseName,
                        confidence = match.confidence,
                        reason = match.reason
                    )
                }
            )
        }
        
        val estimatedDuration = calculateWorkoutDuration(exercises)
        
        return WorkoutPreview(
            dayNumber = workout.dayNumber,
            name = workout.name,
            exercises = exercises,
            estimatedDuration = estimatedDuration
        )
    }
    
    private fun calculateWeekVolume(workouts: List<WorkoutPreview>): com.github.radupana.featherweight.data.VolumeMetrics {
        val totalSets = workouts.sumOf { workout ->
            workout.exercises.sumOf { it.sets }
        }
        val totalReps = workouts.sumOf { workout ->
            workout.exercises.sumOf { exercise ->
                exercise.sets * ((exercise.repsMin + exercise.repsMax) / 2)
            }
        }
        
        return com.github.radupana.featherweight.data.VolumeMetrics(
            totalSets = totalSets,
            totalReps = totalReps,
            muscleGroupVolume = emptyMap(), // TODO: Calculate
            movementPatternVolume = emptyMap() // TODO: Calculate
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
    
    fun resolveExercise(tempId: String, exerciseId: Long) {
        viewModelScope.launch {
            val currentPreview = _currentPreview.value ?: return@launch
            val exercise = repository.getExerciseById(exerciseId)
            
            if (exercise != null) {
                val updatedPreview = updateExerciseInPreview(currentPreview, tempId) { exercisePreview ->
                    exercisePreview.copy(
                        matchedExerciseId = exerciseId,
                        exerciseName = exercise.exercise.name,
                        matchConfidence = 1.0f
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
    
    fun swapExercise(tempId: String, newExerciseName: String) {
        viewModelScope.launch {
            val currentPreview = _currentPreview.value ?: return@launch
            val allExercises = repository.getAllExercises()
            val matches = exerciseMatcher.findBestMatches(newExerciseName, allExercises.map { it.exercise.name })
            val bestMatch = matches.firstOrNull()
            
            val updatedPreview = updateExerciseInPreview(currentPreview, tempId) { exercisePreview ->
                exercisePreview.copy(
                    exerciseName = newExerciseName,
                    matchedExerciseId = allExercises.find { it.exercise.name == bestMatch?.exerciseName }?.exercise?.id,
                    matchConfidence = bestMatch?.confidence ?: 0f,
                    alternatives = matches.drop(1).take(2).map { match ->
                        ExerciseAlternative(
                            exerciseId = allExercises.find { it.exercise.name == match.exerciseName }?.exercise?.id ?: 0L,
                            name = match.exerciseName,
                            confidence = match.confidence,
                            reason = match.reason
                        )
                    }
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
        
        val updatedPreview = updateExerciseInPreview(currentPreview, action.tempId) { exercise ->
            exercise.copy(
                sets = action.sets ?: exercise.sets,
                repsMin = action.repsMin ?: exercise.repsMin,
                repsMax = action.repsMax ?: exercise.repsMax,
                rpe = action.rpe ?: exercise.rpe,
                restSeconds = action.restSeconds ?: exercise.restSeconds
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
        updateFn: (ExercisePreview) -> ExercisePreview
    ): GeneratedProgrammePreview? {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            if (exercise.tempId == tempId) {
                                updateFn(exercise)
                            } else {
                                exercise
                            }
                        }
                    )
                }
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
        baseValidation: ValidationResult
    ): ValidationResult {
        val additionalErrors = mutableListOf<ValidationError>()
        val additionalWarnings = mutableListOf<ValidationWarning>()
        
        // Check for unresolved exercises
        val unresolvedExercises = preview.weeks
            .flatMap { it.workouts }
            .flatMap { it.exercises }
            .filter { it.matchedExerciseId == null || it.matchConfidence < 0.7f }
        
        if (unresolvedExercises.isNotEmpty()) {
            additionalErrors.add(
                ValidationError(
                    message = "${unresolvedExercises.size} exercise(s) need to be resolved",
                    category = ValidationCategory.EXERCISE_SELECTION,
                    requiredAction = "Scroll down and click on exercises highlighted in red to resolve them",
                    isAutoFixable = false
                )
            )
        }
        
        // Check for low confidence matches
        val lowConfidenceExercises = preview.weeks
            .flatMap { it.workouts }
            .flatMap { it.exercises }
            .filter { it.matchedExerciseId != null && it.matchConfidence < 0.8f && it.matchConfidence >= 0.7f }
        
        if (lowConfidenceExercises.isNotEmpty()) {
            additionalWarnings.add(
                ValidationWarning(
                    message = "${lowConfidenceExercises.size} exercise(s) have low confidence matches",
                    category = ValidationCategory.EXERCISE_SELECTION,
                    suggestion = "Review and confirm these exercise selections"
                )
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
            score = newScore
        )
    }
    
    fun regenerate(mode: RegenerationMode) {
        viewModelScope.launch {
            try {
                _previewState.value = PreviewState.Loading
                
                val currentPreview = _currentPreview.value
                if (currentPreview == null) {
                    _previewState.value = PreviewState.Error("No programme to regenerate")
                    return@launch
                }
                
                // Simulate regeneration based on mode
                kotlinx.coroutines.delay(2500)
                
                val regeneratedResponse = when (mode) {
                    RegenerationMode.MORE_VOLUME -> {
                        // Increase training volume with more sets and exercises
                        generateHighVolumeVersion(currentPreview)
                    }
                    RegenerationMode.LESS_VOLUME -> {
                        // Reduce training volume for easier recovery
                        generateLowVolumeVersion(currentPreview)
                    }
                    RegenerationMode.MORE_INTENSITY -> {
                        // Increase intensity with heavier weights and lower reps
                        generateHighIntensityVersion(currentPreview)
                    }
                    RegenerationMode.LESS_INTENSITY -> {
                        // Reduce intensity with lighter weights and higher reps
                        generateLowIntensityVersion(currentPreview)
                    }
                }
                
                // Process the regenerated programme
                val allExercises = repository.getAllExercises()
                val newPreview = processGeneratedProgramme(regeneratedResponse.programme!!, allExercises)
                
                _currentPreview.value = newPreview
                _previewState.value = PreviewState.Success(newPreview)
                
            } catch (e: Exception) {
                _previewState.value = PreviewState.Error(
                    "Failed to regenerate: ${e.message}"
                )
            }
        }
    }
    
    private fun generateVariantWithNewExercises(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Create a variant that keeps the same structure but swaps exercises
        val alternativeExercises = mapOf(
            "Barbell Back Squat" to "Front Squat",
            "Barbell Bench Press" to "Dumbbell Press",
            "Conventional Deadlift" to "Sumo Deadlift",
            "Barbell Row" to "Cable Row",
            "Pull-up" to "Lat Pulldown",
            "Overhead Press" to "Dumbbell Shoulder Press"
        )
        
        val newWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = workout.name + " (Variant)",
                exercises = workout.exercises.map { exercise ->
                    val newName = alternativeExercises[exercise.exerciseName] ?: exercise.exerciseName
                    GeneratedExercise(
                        exerciseName = newName,
                        sets = exercise.sets,
                        repsMin = exercise.repsMin,
                        repsMax = exercise.repsMax,
                        rpe = exercise.rpe,
                        restSeconds = exercise.restSeconds,
                        notes = exercise.notes
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = original.name + " (Variant)",
                description = "Alternative exercise selection maintaining the same programme structure",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = newWorkouts
            )
        )
    }
    
    private fun generateAlternativeApproach(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Generate a different programme style (e.g., if original was strength, make it hypertrophy-focused)
        return MockProgrammeGenerator.generateMockProgramme(
            goal = when (original.focus.firstOrNull()) {
                ProgrammeGoal.BUILD_STRENGTH -> ProgrammeGoal.BUILD_MUSCLE
                ProgrammeGoal.BUILD_MUSCLE -> ProgrammeGoal.ATHLETIC_PERFORMANCE
                ProgrammeGoal.LOSE_FAT -> ProgrammeGoal.BUILD_STRENGTH
                else -> ProgrammeGoal.BUILD_MUSCLE
            },
            frequency = original.daysPerWeek,
            duration = SessionDuration.STANDARD,
            inputText = "Alternative approach programme"
        )
    }
    
    private fun generateFixedVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Apply automatic fixes for validation errors
        val fixedWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = workout.name,
                exercises = workout.exercises.map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = exercise.sets.coerceIn(2, 5), // Fix volume issues
                        repsMin = exercise.repsMin.coerceIn(5, 15),
                        repsMax = exercise.repsMax.coerceIn(8, 20),
                        rpe = exercise.rpe?.coerceIn(6.0f, 9.0f),
                        restSeconds = exercise.restSeconds.coerceIn(60, 300),
                        notes = exercise.notes
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = original.name + " (Fixed)",
                description = "Validated version with automatic corrections applied",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = fixedWorkouts
            )
        )
    }
    
    private fun generateVarietyVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Add more exercise variety and variations
        val extraExercises = listOf(
            GeneratedExercise("Goblet Squat", 3, 12, 15, 6.5f, 75, "Mobility and activation"),
            GeneratedExercise("Band Pull Apart", 3, 15, 20, 5.0f, 45, "Rear delt activation"),
            GeneratedExercise("Plank", 3, 30, 60, null, 60, "Hold for time")
        )
        
        val enhancedWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = workout.name + " Plus",
                exercises = workout.exercises.map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = exercise.sets,
                        repsMin = exercise.repsMin,
                        repsMax = exercise.repsMax,
                        rpe = exercise.rpe,
                        restSeconds = exercise.restSeconds,
                        notes = exercise.notes
                    )
                } + extraExercises.take(2)
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = original.name + " (Enhanced)",
                description = "Expanded version with additional exercises for variety and balance",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = enhancedWorkouts
            )
        )
    }
    
    private fun generateSimplifiedVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Create a simpler version with fewer exercises and complexity
        val simplifiedWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = "Simple " + workout.name,
                exercises = workout.exercises.take(4).map { exercise -> // Keep only first 4 exercises
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = 3, // Standardize to 3 sets
                        repsMin = 8,
                        repsMax = 12, // Standard hypertrophy range
                        rpe = 7.0f, // Moderate intensity
                        restSeconds = 90, // Standard rest
                        notes = "Keep it simple and focus on form"
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = "Simple " + original.name,
                description = "Simplified version focusing on essential movements with beginner-friendly parameters",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = simplifiedWorkouts
            )
        )
    }
    
    fun activateProgramme(startDate: LocalDate, oneRms: Map<Long, Float>? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val currentPreview = _currentPreview.value ?: return@launch
                
                // Simplified activation - no validation checks required
                
                // Set activating state
                _previewState.value = PreviewState.Activating(currentPreview)
                
                // Create the programme in the database
                val programmeId = repository.createAIGeneratedProgramme(currentPreview)
                
                // Show success state briefly before navigation
                _previewState.value = PreviewState.Success(
                    currentPreview.copy(
                        name = "✅ " + currentPreview.name + " Activated!",
                        description = "Programme has been successfully activated! You can now find it in your Programmes section and start your first workout."
                    )
                )
                
                // Wait a moment to show success feedback, then navigate
                kotlinx.coroutines.delay(2000)
                onSuccess()
                
            } catch (e: Exception) {
                val currentPreview = _currentPreview.value
                // If activation fails, go back to Success state
                if (currentPreview != null) {
                    _previewState.value = PreviewState.Success(currentPreview)
                }
                _previewState.value = PreviewState.Error(
                    "Failed to activate programme: ${e.message}"
                )
            }
        }
    }
    
    fun toggleExerciseEdit(tempId: String) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)
        
        _editStates.value = currentStates + (tempId to currentState.copy(
            isEditing = !currentState.isEditing
        ))
    }
    
    fun showExerciseAlternatives(tempId: String, show: Boolean) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)
        
        _editStates.value = currentStates + (tempId to currentState.copy(
            showAlternatives = show
        ))
    }
    
    fun showExerciseResolution(tempId: String, show: Boolean) {
        val currentStates = _editStates.value
        val currentState = currentStates[tempId] ?: ExerciseEditState(tempId)
        
        _editStates.value = currentStates + (tempId to currentState.copy(
            showResolution = show
        ))
    }
    
    fun updateProgrammeName(newName: String) {
        val currentPreview = _currentPreview.value ?: return
        val updatedPreview = currentPreview.copy(name = newName)
        
        _currentPreview.value = updatedPreview
        _previewState.value = PreviewState.Success(updatedPreview)
    }
    
    fun applyBulkEdit(action: QuickEditAction) {
        val currentPreview = _currentPreview.value ?: return
        
        val updatedPreview = when (action) {
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
    
    private fun adjustAllVolume(preview: GeneratedProgrammePreview, factor: Float): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            val newSets = (exercise.sets * factor).toInt().coerceAtLeast(1).coerceAtMost(6)
                            exercise.copy(sets = newSets)
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun adjustSchedule(preview: GeneratedProgrammePreview, newDaysPerWeek: Int): GeneratedProgrammePreview {
        // For now, just update the metadata. Full schedule restructuring would be complex
        return preview.copy(daysPerWeek = newDaysPerWeek)
    }
    
    private fun changeFocus(preview: GeneratedProgrammePreview, newGoal: ProgrammeGoal): GeneratedProgrammePreview {
        return preview.copy(focus = listOf(newGoal))
    }
    
    private fun simplifyForBeginner(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            exercise.copy(
                                sets = exercise.sets.coerceAtMost(3), // Max 3 sets for beginners
                                repsMin = exercise.repsMin.coerceAtLeast(8), // Higher rep ranges
                                repsMax = exercise.repsMax.coerceAtLeast(12),
                                rpe = exercise.rpe?.let { it.coerceAtMost(7.5f) }, // Lower intensity
                                restSeconds = exercise.restSeconds.coerceAtLeast(90) // Longer rest
                            )
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun addProgressiveOverload(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.mapIndexed { weekIndex, week ->
            val progressionFactor = 1.0f + (weekIndex * 0.05f) // 5% increase per week
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            exercise.copy(
                                rpe = exercise.rpe?.let { 
                                    (it + weekIndex * 0.2f).coerceAtMost(9.5f) 
                                } // Gradually increase intensity
                            )
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    fun autoFixValidationIssue(issue: ValidationIssue) {
        val currentPreview = _currentPreview.value ?: return
        
        val fixedPreview = when (issue.category) {
            ValidationCategory.VOLUME -> autoFixVolumeIssue(currentPreview, issue)
            ValidationCategory.EXERCISE_SELECTION -> autoFixExerciseIssue(currentPreview, issue)
            ValidationCategory.BALANCE -> autoFixBalanceIssue(currentPreview, issue)
            ValidationCategory.RECOVERY -> autoFixRecoveryIssue(currentPreview, issue)
            ValidationCategory.DURATION -> autoFixDurationIssue(currentPreview, issue)
            ValidationCategory.SAFETY -> autoFixSafetyIssue(currentPreview, issue)
            ValidationCategory.PROGRESSION -> autoFixProgressionIssue(currentPreview, issue)
        }
        
        if (fixedPreview != currentPreview) {
            _currentPreview.value = fixedPreview
            _previewState.value = PreviewState.Success(fixedPreview)
            validateProgramme() // Re-validate after fix
        }
    }
    
    private fun autoFixVolumeIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("Low") && issue.message.contains("volume") -> {
                // Extract muscle group and add sets
                val muscleGroup = extractMuscleGroupFromMessage(issue.message)
                addVolumeForMuscleGroup(preview, muscleGroup)
            }
            issue.message.contains("High") && issue.message.contains("volume") -> {
                // Extract muscle group and reduce sets
                val muscleGroup = extractMuscleGroupFromMessage(issue.message)
                reduceVolumeForMuscleGroup(preview, muscleGroup)
            }
            else -> preview
        }
    }
    
    private fun autoFixExerciseIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        // For exercise selection issues, we can't auto-fix easily as it requires user choice
        // But we can highlight the exercises that need attention
        return preview
    }
    
    private fun autoFixBalanceIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("Push/pull imbalance") && issue.message.contains("Add more pulling") -> {
                addPullingExercise(preview)
            }
            issue.message.contains("Push/pull imbalance") && issue.message.contains("Add more pushing") -> {
                addPushingExercise(preview)
            }
            issue.message.contains("Quad-dominant") -> {
                addHamstringGluteExercise(preview)
            }
            else -> preview
        }
    }
    
    private fun autoFixRecoveryIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("Limited recovery time") -> {
                // Reduce training frequency by 1 day
                preview.copy(daysPerWeek = (preview.daysPerWeek - 1).coerceAtLeast(3))
            }
            issue.message.contains("High volume on consecutive days") -> {
                redistributeVolume(preview)
            }
            else -> preview
        }
    }
    
    private fun autoFixDurationIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("very long") -> {
                // Reduce number of exercises in long workouts
                reduceLongWorkouts(preview)
            }
            issue.message.contains("very short") -> {
                // Add exercises to short workouts
                addExercisesToShortWorkouts(preview)
            }
            else -> preview
        }
    }
    
    private fun autoFixSafetyIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("High reps with high RPE") -> {
                reduceRPEForHighRepSets(preview)
            }
            issue.message.contains("High volume on single exercise") -> {
                redistributeExerciseVolume(preview)
            }
            else -> preview
        }
    }
    
    private fun autoFixProgressionIssue(preview: GeneratedProgrammePreview, issue: ValidationIssue): GeneratedProgrammePreview {
        return when {
            issue.message.contains("No clear progression scheme") -> {
                addProgressionNotes(preview)
            }
            else -> preview
        }
    }
    
    // Helper functions for specific fixes
    private fun extractMuscleGroupFromMessage(message: String): String {
        return when {
            message.contains("chest", ignoreCase = true) -> "chest"
            message.contains("back", ignoreCase = true) -> "back"
            message.contains("shoulders", ignoreCase = true) -> "shoulders"
            message.contains("quads", ignoreCase = true) -> "quads"
            message.contains("hamstrings", ignoreCase = true) -> "hamstrings"
            message.contains("glutes", ignoreCase = true) -> "glutes"
            else -> "unknown"
        }
    }
    
    private fun addVolumeForMuscleGroup(preview: GeneratedProgrammePreview, muscleGroup: String): GeneratedProgrammePreview {
        val exercisesToAdd = when (muscleGroup) {
            "chest" -> listOf("Push-up", "Incline Dumbbell Press")
            "back" -> listOf("Lat Pulldown", "Cable Row")
            "shoulders" -> listOf("Lateral Raise", "Face Pull")
            "quads" -> listOf("Leg Extension", "Goblet Squat")
            "hamstrings" -> listOf("Leg Curl", "Romanian Deadlift")
            "glutes" -> listOf("Glute Bridge", "Hip Thrust")
            else -> listOf("Push-up") // Default fallback
        }
        
        return addExercisesToFirstWorkout(preview, exercisesToAdd)
    }
    
    private fun reduceVolumeForMuscleGroup(preview: GeneratedProgrammePreview, muscleGroup: String): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            if (exerciseTargetsMuscleGroup(exercise.exerciseName, muscleGroup)) {
                                exercise.copy(sets = (exercise.sets - 1).coerceAtLeast(1))
                            } else {
                                exercise
                            }
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun addPullingExercise(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        return addExercisesToFirstWorkout(preview, listOf("Cable Row"))
    }
    
    private fun addPushingExercise(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        return addExercisesToFirstWorkout(preview, listOf("Push-up"))
    }
    
    private fun addHamstringGluteExercise(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        return addExercisesToFirstWorkout(preview, listOf("Romanian Deadlift"))
    }
    
    private fun addExercisesToFirstWorkout(preview: GeneratedProgrammePreview, exerciseNames: List<String>): GeneratedProgrammePreview {
        if (preview.weeks.isEmpty() || preview.weeks.first().workouts.isEmpty()) return preview
        
        val firstWorkout = preview.weeks.first().workouts.first()
        val newExercises = exerciseNames.map { name ->
            // Try to auto-resolve the exercise by finding exact matches
            val exactMatch = findExactExerciseMatch(name)
            
            ExercisePreview(
                tempId = java.util.UUID.randomUUID().toString(),
                exerciseName = name,
                matchedExerciseId = exactMatch?.id, // Auto-resolve if possible
                matchConfidence = if (exactMatch != null) 1.0f else 0.5f, // High confidence for exact matches
                sets = 3,
                repsMin = 8,
                repsMax = 12,
                rpe = 6.5f,
                restSeconds = 90,
                notes = if (exactMatch != null) "Added automatically (exact match found)" else "Added automatically to fix volume issue",
                alternatives = emptyList()
            )
        }
        
        val updatedFirstWorkout = firstWorkout.copy(
            exercises = firstWorkout.exercises + newExercises
        )
        
        val updatedWeeks = preview.weeks.mapIndexed { weekIndex, week ->
            if (weekIndex == 0) {
                week.copy(
                    workouts = week.workouts.mapIndexed { workoutIndex, workout ->
                        if (workoutIndex == 0) updatedFirstWorkout else workout
                    }
                )
            } else {
                week
            }
        }
        
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun exerciseTargetsMuscleGroup(exerciseName: String, muscleGroup: String): Boolean {
        val lowerName = exerciseName.lowercase()
        return when (muscleGroup) {
            "chest" -> lowerName.contains("bench") || lowerName.contains("press") || lowerName.contains("flye")
            "back" -> lowerName.contains("pull") || lowerName.contains("row") || lowerName.contains("lat")
            "shoulders" -> lowerName.contains("shoulder") || lowerName.contains("lateral") || lowerName.contains("overhead")
            "quads" -> lowerName.contains("squat") || lowerName.contains("leg press") || lowerName.contains("extension")
            "hamstrings" -> lowerName.contains("deadlift") || lowerName.contains("curl") || lowerName.contains("romanian")
            "glutes" -> lowerName.contains("hip thrust") || lowerName.contains("bridge") || lowerName.contains("deadlift")
            else -> false
        }
    }
    
    private fun redistributeVolume(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        // Simple redistribution: reduce volume on high-volume days
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    val totalSets = workout.exercises.sumOf { it.sets }
                    if (totalSets > 15) {
                        workout.copy(
                            exercises = workout.exercises.map { exercise ->
                                exercise.copy(sets = (exercise.sets - 1).coerceAtLeast(1))
                            }
                        )
                    } else {
                        workout
                    }
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun reduceLongWorkouts(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    if (workout.estimatedDuration > 120) {
                        // Remove exercises with lowest priority (highest index, assuming they're accessory)
                        workout.copy(
                            exercises = workout.exercises.dropLast(1)
                        )
                    } else {
                        workout
                    }
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun addExercisesToShortWorkouts(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    if (workout.estimatedDuration < 20) {
                        val newExercise = ExercisePreview(
                            tempId = java.util.UUID.randomUUID().toString(),
                            exerciseName = "Plank",
                            matchedExerciseId = null,
                            matchConfidence = 0.8f,
                            sets = 3,
                            repsMin = 30,
                            repsMax = 60,
                            rpe = 6.0f,
                            restSeconds = 60,
                            notes = "Added to extend workout duration",
                            alternatives = emptyList()
                        )
                        workout.copy(exercises = workout.exercises + newExercise)
                    } else {
                        workout
                    }
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun reduceRPEForHighRepSets(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            if (exercise.repsMax > 15 && exercise.rpe != null && exercise.rpe > 8.5f) {
                                exercise.copy(rpe = 7.5f)
                            } else {
                                exercise
                            }
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun redistributeExerciseVolume(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.map { week ->
            week.copy(
                workouts = week.workouts.map { workout ->
                    workout.copy(
                        exercises = workout.exercises.map { exercise ->
                            if (exercise.sets > 8) {
                                exercise.copy(sets = 6) // Cap at 6 sets per exercise
                            } else {
                                exercise
                            }
                        }
                    )
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun addProgressionNotes(preview: GeneratedProgrammePreview): GeneratedProgrammePreview {
        val updatedWeeks = preview.weeks.mapIndexed { index, week ->
            week.copy(
                progressionNotes = when (index) {
                    0 -> "Week 1: Establish baseline with conservative weights"
                    1 -> "Week 2: Increase weight by 2.5-5kg for compounds"
                    2 -> "Week 3: Continue progressive overload"
                    3 -> "Week 4: Deload week - reduce weight by 10%"
                    else -> "Week ${index + 1}: Progressive overload based on performance"
                }
            )
        }
        return preview.copy(weeks = updatedWeeks)
    }
    
    private fun findExactExerciseMatch(exerciseName: String): com.github.radupana.featherweight.data.exercise.Exercise? {
        // This is a simplified version - in a real implementation, we'd access the repository
        // For now, we'll use common exercise name mappings
        val commonExercises = mapOf(
            "Push-up" to 1L,
            "Incline Dumbbell Press" to 2L,
            "Cable Row" to 3L,
            "Lat Pulldown" to 4L,
            "Lateral Raise" to 5L,
            "Face Pull" to 6L,
            "Leg Extension" to 7L,
            "Goblet Squat" to 8L,
            "Leg Curl" to 9L,
            "Romanian Deadlift" to 10L,
            "Glute Bridge" to 11L,
            "Hip Thrust" to 12L,
            "Plank" to 13L
        )
        
        val exerciseId = commonExercises[exerciseName]
        return if (exerciseId != null) {
            // Create a mock exercise object - in real implementation, this would come from the database
            com.github.radupana.featherweight.data.exercise.Exercise(
                id = exerciseId,
                name = exerciseName,
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST,
                type = com.github.radupana.featherweight.data.exercise.ExerciseType.STRENGTH,
                difficulty = com.github.radupana.featherweight.data.exercise.ExerciseDifficulty.BEGINNER,
                requiresWeight = true,
                instructions = null,
                usageCount = 0
            )
        } else {
            null
        }
    }

    // New simplified regeneration methods
    private fun generateHighVolumeVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Increase training volume with more sets and exercises
        val highVolumeWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = "High Volume " + workout.name,
                exercises = workout.exercises.map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = (exercise.sets + 1).coerceAtMost(6), // Add 1 set, max 6
                        repsMin = exercise.repsMin,
                        repsMax = exercise.repsMax,
                        rpe = exercise.rpe,
                        restSeconds = (exercise.restSeconds + 30).coerceAtMost(180), // Longer rest for volume
                        notes = "Increased volume for greater training stimulus"
                    )
                } + listOf(
                    // Add one extra exercise per workout
                    GeneratedExercise(
                        exerciseName = "Additional Accessory",
                        sets = 3,
                        repsMin = 12,
                        repsMax = 15,
                        rpe = 7.0f,
                        restSeconds = 60,
                        notes = "Extra volume exercise"
                    )
                )
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = "High Volume " + original.name,
                description = "Increased volume version with more sets and exercises for greater training stimulus",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = highVolumeWorkouts
            )
        )
    }

    private fun generateLowVolumeVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Reduce training volume for easier recovery
        val lowVolumeWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = "Low Volume " + workout.name,
                exercises = workout.exercises.take(workout.exercises.size - 1).map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = (exercise.sets - 1).coerceAtLeast(2), // Remove 1 set, min 2
                        repsMin = exercise.repsMin,
                        repsMax = exercise.repsMax,
                        rpe = exercise.rpe,
                        restSeconds = (exercise.restSeconds - 30).coerceAtLeast(60), // Shorter rest
                        notes = "Reduced volume for better recovery"
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = "Low Volume " + original.name,
                description = "Reduced volume version for easier recovery and time efficiency",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = lowVolumeWorkouts
            )
        )
    }

    private fun generateHighIntensityVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Increase intensity with heavier weights and lower reps
        val highIntensityWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = "High Intensity " + workout.name,
                exercises = workout.exercises.map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = exercise.sets,
                        repsMin = (exercise.repsMin - 2).coerceAtLeast(1), // Lower reps
                        repsMax = (exercise.repsMax - 2).coerceAtLeast(3),
                        rpe = (exercise.rpe?.plus(1.0f))?.coerceAtMost(9.5f) ?: 8.5f, // Higher intensity
                        restSeconds = (exercise.restSeconds + 60).coerceAtMost(300), // Longer rest for heavy lifting
                        notes = "High intensity - focus on heavy weights and perfect form"
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = "High Intensity " + original.name,
                description = "High intensity version with heavier weights and lower reps for strength focus",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = highIntensityWorkouts
            )
        )
    }

    private fun generateLowIntensityVersion(original: GeneratedProgrammePreview): AIProgrammeResponse {
        // Reduce intensity with lighter weights and higher reps
        val lowIntensityWorkouts = original.weeks.first().workouts.map { workout ->
            GeneratedWorkout(
                dayNumber = workout.dayNumber,
                name = "Low Intensity " + workout.name,
                exercises = workout.exercises.map { exercise ->
                    GeneratedExercise(
                        exerciseName = exercise.exerciseName,
                        sets = exercise.sets,
                        repsMin = (exercise.repsMin + 3).coerceAtMost(15), // Higher reps
                        repsMax = (exercise.repsMax + 3).coerceAtMost(20),
                        rpe = (exercise.rpe?.minus(1.0f))?.coerceAtLeast(5.5f) ?: 6.5f, // Lower intensity
                        restSeconds = (exercise.restSeconds - 30).coerceAtLeast(45), // Shorter rest
                        notes = "Low intensity - focus on muscle endurance and technique"
                    )
                }
            )
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = GeneratedProgramme(
                name = "Low Intensity " + original.name,
                description = "Low intensity version with lighter weights and higher reps for endurance and technique",
                durationWeeks = original.durationWeeks,
                daysPerWeek = original.daysPerWeek,
                workouts = lowIntensityWorkouts
            )
        )
    }
}