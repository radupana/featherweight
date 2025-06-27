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
        
        // Validate the programme
        val validationResult = validator.validate(preview)
        
        return preview.copy(validationResult = validationResult)
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
        
        val updatedPreview = currentPreview.copy(validationResult = validationResult)
        _currentPreview.value = updatedPreview
        _previewState.value = PreviewState.Success(updatedPreview)
    }
    
    fun regenerate(mode: RegenerationMode) {
        viewModelScope.launch {
            try {
                _previewState.value = PreviewState.Loading
                
                // TODO: Implement actual regeneration with AI service
                // For now, just simulate
                kotlinx.coroutines.delay(2000)
                
                _previewState.value = PreviewState.Error(
                    "Regeneration not yet implemented. This will be added in the next phase.",
                    canRetry = false
                )
            } catch (e: Exception) {
                _previewState.value = PreviewState.Error(
                    "Failed to regenerate: ${e.message}"
                )
            }
        }
    }
    
    fun activateProgramme(startDate: LocalDate, oneRms: Map<Long, Float>? = null) {
        viewModelScope.launch {
            try {
                val currentPreview = _currentPreview.value ?: return@launch
                
                // Validate all exercises are resolved
                val unresolvedExercises = currentPreview.weeks
                    .flatMap { it.workouts }
                    .flatMap { it.exercises }
                    .filter { it.matchedExerciseId == null || it.matchConfidence < 0.7f }
                
                if (unresolvedExercises.isNotEmpty()) {
                    _previewState.value = PreviewState.Error(
                        "Please resolve ${unresolvedExercises.size} exercise(s) before activating",
                        canRetry = false
                    )
                    return@launch
                }
                
                // Check for validation errors
                if (currentPreview.validationResult.errors.isNotEmpty()) {
                    _previewState.value = PreviewState.Error(
                        "Please fix validation errors before activating",
                        canRetry = false
                    )
                    return@launch
                }
                
                // TODO: Convert to active programme and save to database
                // This will be implemented when we integrate with the existing Programme system
                
                _previewState.value = PreviewState.Error(
                    "Programme activation not yet implemented. This will be added in the next phase.",
                    canRetry = false
                )
                
            } catch (e: Exception) {
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
}