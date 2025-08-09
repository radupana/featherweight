package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import com.github.radupana.featherweight.service.OneRMService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WorkoutState(
    val isActive: Boolean = false,
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    val workoutId: Long? = null,
    val startTime: LocalDateTime? = null,
    val workoutName: String? = null,
    val isReadOnly: Boolean = false,
    val isInEditMode: Boolean = false,
    // Backup for rollback
    val originalWorkoutData: Triple<List<ExerciseLog>, List<SetLog>, String?>? = null,
    // Programme Integration
    val isProgrammeWorkout: Boolean = false,
    val programmeId: Long? = null,
    val programmeName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    // Loading state to prevent UI from showing "No exercises" while loading
    val isLoadingExercises: Boolean = false,
    // PR Celebration
    val pendingPRs: List<PersonalRecord> = emptyList(),
    val shouldShowPRCelebration: Boolean = false,
)

data class InProgressWorkout(
    val id: Long,
    val name: String?,
    val startDate: LocalDateTime,
    val exerciseCount: Int,
    val setCount: Int,
    val completedSets: Int,
    // Programme information
    val isProgrammeWorkout: Boolean = false,
    val programmeName: String? = null,
    val programmeWorkoutName: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeId: Long? = null,
)

class WorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val repository = FeatherweightRepository(application)
    private val oneRMService = OneRMService()

    // Cache for 1RM estimates by exercise ID
    private val _oneRMEstimates = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val oneRMEstimates: StateFlow<Map<Long, Float>> = _oneRMEstimates

    // Expose pending 1RM updates from repository
    val pendingOneRMUpdates = repository.pendingOneRMUpdates

    // Apply a pending 1RM update
    fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        viewModelScope.launch {
            try {
                repository.applyOneRMUpdate(update)
                // Reload 1RM estimates after update
                loadOneRMEstimatesForCurrentExercises()
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to apply 1RM update", e)
            }
        }
    }

    // Clear all pending updates
    fun clearPendingOneRMUpdates() {
        repository.clearPendingOneRMUpdates()
    }

    // Clear pending PRs after celebration
    fun clearPendingPRs() {
        _workoutState.value =
            _workoutState.value.copy(
                pendingPRs = emptyList(),
                shouldShowPRCelebration = false,
            )
    }

    // Workout state management
    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState

    // Core state
    private val _currentWorkoutId = MutableStateFlow<Long?>(null)
    val currentWorkoutId: StateFlow<Long?> = _currentWorkoutId

    private val _selectedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val selectedWorkoutExercises: StateFlow<List<ExerciseLog>> = _selectedWorkoutExercises

    private val _selectedExerciseSets = MutableStateFlow<List<SetLog>>(emptyList())
    val selectedExerciseSets: StateFlow<List<SetLog>> = _selectedExerciseSets

    // Cache validation state for sets
    private val _setCompletionValidation = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val setCompletionValidation: StateFlow<Map<Long, Boolean>> = _setCompletionValidation

    private val _exerciseHistory = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    val exerciseHistory: StateFlow<Map<String, ExerciseHistory>> = _exerciseHistory

    // In-progress workouts for home screen
    private val _inProgressWorkouts = MutableStateFlow<List<InProgressWorkout>>(emptyList())
    val inProgressWorkouts: StateFlow<List<InProgressWorkout>> = _inProgressWorkouts

    // Last completed workout
    private val _lastCompletedWorkout = MutableStateFlow<Workout?>(null)
    val lastCompletedWorkout: StateFlow<Workout?> = _lastCompletedWorkout

    // Last completed workout exercises
    private val _lastCompletedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val lastCompletedWorkoutExercises: StateFlow<List<ExerciseLog>> = _lastCompletedWorkoutExercises

    // Exercise-related state

    private val _exerciseDetails = MutableStateFlow<Map<Long, ExerciseWithDetails>>(emptyMap())

    // Exercise swap state
    private val _swappingExercise = MutableStateFlow<ExerciseLog?>(null)
    val swappingExercise: StateFlow<ExerciseLog?> = _swappingExercise

    // Rest timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds

    private val _restTimerInitialSeconds = MutableStateFlow(0)
    val restTimerInitialSeconds: StateFlow<Int> = _restTimerInitialSeconds

    private val _restTimerExpanded = MutableStateFlow(false)
    val restTimerExpanded: StateFlow<Boolean> = _restTimerExpanded

    private var restTimerJob: Job? = null

    // Workout timer state
    private val _workoutTimerSeconds = MutableStateFlow(0)
    val workoutTimerSeconds: StateFlow<Int> = _workoutTimerSeconds

    private var workoutTimerJob: Job? = null
    private var workoutTimerStartTime: LocalDateTime? = null

    init {
        loadInProgressWorkouts()
        checkForOngoingWorkout()
        loadExercises()
    }

    // ===== EXERCISE METHODS =====

    private fun loadExercises() {
        viewModelScope.launch {
            try {
                val exercises = repository.getAllExercises()

                // Create lookup map for exercise details
                val detailsMap = exercises.associateBy { it.exercise.id }
                _exerciseDetails.value = detailsMap
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to load exercises", e)
            }
        }
    }




    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<ExerciseWithDetails> {
        val allExercises = repository.getAllExercises()
        return allExercises.filter { it.exercise.muscleGroup.equals(muscleGroup, ignoreCase = true) }
    }

    fun getExerciseDetails(exerciseId: Long): ExerciseWithDetails? = _exerciseDetails.value[exerciseId]


    // ===== EXISTING WORKOUT METHODS (Updated to work with exercises) =====

    // Check if there's an ongoing workout when the app starts
    private fun checkForOngoingWorkout() {
        viewModelScope.launch {
            // Only check for ongoing workouts if we don't already have a workout loaded
            // This prevents overriding when navigating from history
            if (_currentWorkoutId.value == null) {
                val ongoingWorkout = repository.getOngoingWorkout()
                if (ongoingWorkout != null) {
                    resumeWorkout(ongoingWorkout.id)
                }
            }
        }
    }

    // Load all in-progress workouts for home screen
    fun loadInProgressWorkouts() {
        viewModelScope.launch {
            val workoutHistory = repository.getWorkoutHistory()
            val inProgress =
                workoutHistory
                    .filter { it.status != WorkoutStatus.COMPLETED }
                    .map { summary ->
                        // Calculate completed sets
                        val completedSets =
                            try {
                                val exercises = repository.getExercisesForWorkout(summary.id)
                                exercises.sumOf { exercise ->
                                    val sets = repository.getSetsForExercise(exercise.id)
                                    sets.count { it.isCompleted }
                                }
                            } catch (e: Exception) {
                                Log.e("WorkoutViewModel", "Failed to calculate completed sets", e)
                                0
                            }

                        InProgressWorkout(
                            id = summary.id,
                            name = summary.name,
                            startDate = summary.date,
                            exerciseCount = summary.exerciseCount,
                            setCount = summary.setCount,
                            completedSets = completedSets,
                            // Include programme information
                            isProgrammeWorkout = summary.isProgrammeWorkout,
                            programmeName = summary.programmeName,
                            programmeWorkoutName = summary.programmeWorkoutName,
                            weekNumber = summary.weekNumber,
                            dayNumber = summary.dayNumber,
                            // Get programme ID from the summary
                            programmeId = summary.programmeId,
                        )
                    }
            _inProgressWorkouts.value = inProgress
        }
    }

    fun loadLastCompletedWorkout() {
        viewModelScope.launch {
            val workoutHistory = repository.getWorkoutHistory()
            val lastCompleted =
                workoutHistory
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.date }
                    .firstOrNull()

            if (lastCompleted != null) {
                val workout = repository.getWorkoutById(lastCompleted.id)
                _lastCompletedWorkout.value = workout

                // Also load the exercises
                val exercises = repository.getExercisesForWorkout(lastCompleted.id)
                _lastCompletedWorkoutExercises.value = exercises
            }
        }
    }

    // Check if there are any in-progress workouts
    suspend fun hasInProgressWorkouts(): Boolean {
        val ongoingWorkout = repository.getOngoingWorkout()
        return ongoingWorkout != null
    }

    // Get the most recent in-progress workout
    suspend fun getMostRecentInProgressWorkout(): InProgressWorkout? {
        val ongoingWorkout = repository.getOngoingWorkout() ?: return null
        val exercises = repository.getExercisesForWorkout(ongoingWorkout.id)
        val allSets = mutableListOf<SetLog>()
        exercises.forEach { exercise ->
            allSets.addAll(repository.getSetsForExercise(exercise.id))
        }

        return InProgressWorkout(
            id = ongoingWorkout.id,
            name =
                ongoingWorkout.notes?.let { notes ->
                    notes
                },
            startDate = ongoingWorkout.date,
            exerciseCount = exercises.size,
            setCount = allSets.size,
            completedSets = allSets.count { it.isCompleted },
        )
    }

    // View a completed workout (read-only, no timers, no state changes)
    fun viewCompletedWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.getWorkoutById(workoutId)?.let { workout ->
                if (workout.status != WorkoutStatus.COMPLETED) {
                    Log.w("WorkoutViewModel", "viewCompletedWorkout called on non-completed workout")
                    return@let
                }

                // Stop any running timers - we're just viewing
                stopWorkoutTimer()
                skipRestTimer()

                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = false,
                        status = WorkoutStatus.COMPLETED,
                        workoutId = workoutId,
                        startTime = workout.date,
                        workoutName = workout.name,
                        isReadOnly = true,
                        isInEditMode = false,
                        originalWorkoutData = null,
                        isProgrammeWorkout = workout.isProgrammeWorkout,
                        programmeId = workout.programmeId,
                        programmeName =
                            workout.programmeId?.let {
                                repository.getProgrammeById(it)?.name
                            },
                        programmeWorkoutName = workout.programmeWorkoutName,
                        weekNumber = workout.weekNumber,
                        dayNumber = workout.dayNumber,
                    )

                // Set the timer to the final duration (static, not running)
                _workoutTimerSeconds.value = workout.durationSeconds?.toInt() ?: 0
                workoutTimerStartTime = null

                // Load exercises and sets
                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()
            }
        }
    }

    // Resume an existing workout
    fun resumeWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            if (workout != null) {
                val isCompleted = workout.status == WorkoutStatus.COMPLETED

                // Get programme information if this is a programme workout
                val programmeName =
                    if (workout.isProgrammeWorkout && workout.programmeId != null) {
                        try {
                            repository.getProgrammeById(workout.programmeId)?.name
                        } catch (e: Exception) {
                            Log.e("WorkoutViewModel", "Failed to get programme name", e)
                            null
                        }
                    } else {
                        null
                    }

                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = !isCompleted,
                        status = if (isCompleted) WorkoutStatus.COMPLETED else WorkoutStatus.IN_PROGRESS,
                        workoutId = workoutId,
                        startTime = workout.date,
                        workoutName = workout.name,
                        isReadOnly = isCompleted,
                        isInEditMode = false,
                        originalWorkoutData = null,
                        // Programme Integration
                        isProgrammeWorkout = workout.isProgrammeWorkout,
                        programmeId = workout.programmeId,
                        programmeName = programmeName,
                        weekNumber = workout.weekNumber,
                        dayNumber = workout.dayNumber,
                        programmeWorkoutName = workout.programmeWorkoutName,
                        // Clear any pending celebrations from previous sessions
                        pendingPRs = emptyList(),
                        shouldShowPRCelebration = false,
                    )

                // Wait for exercises to load completely before UI renders
                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()

                // Handle workout timer
                if (workout.status == WorkoutStatus.COMPLETED) {
                    // For completed workouts, show the final duration (NEVER start a timer)
                    stopWorkoutTimer() // Ensure any running timer is stopped
                    _workoutTimerSeconds.value = workout.durationSeconds?.toInt() ?: 0
                    workoutTimerStartTime = null
                } else if (workout.status == WorkoutStatus.IN_PROGRESS && workout.timerStartTime != null) {
                    // For in-progress workouts, resume the running timer
                    resumeWorkoutTimer(workout.timerStartTime)
                } else {
                    // No timer data or not started
                    stopWorkoutTimer()
                    _workoutTimerSeconds.value = 0
                    workoutTimerStartTime = null
                }
            }
        }
    }

    // Start a completely new workout (force new)
    fun startNewWorkout(forceNew: Boolean = false) {
        viewModelScope.launch {
            if (forceNew || repository.getOngoingWorkout() == null) {
                // Clear any existing timers
                stopWorkoutTimer()
                _workoutTimerSeconds.value = 0
                workoutTimerStartTime = null

                // Clear validation cache
                _setCompletionValidation.value = emptyMap()

                val workout = Workout(date = LocalDateTime.now(), notes = null)
                val workoutId = repository.insertWorkout(workout)

                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = true,
                        status = WorkoutStatus.IN_PROGRESS,
                        workoutId = workoutId,
                        startTime = LocalDateTime.now(),
                        workoutName = null,
                        isReadOnly = false,
                        isInEditMode = false,
                        originalWorkoutData = null,
                        // No programme context for regular workouts
                        isProgrammeWorkout = false,
                        programmeId = null,
                        programmeName = null,
                        weekNumber = null,
                        dayNumber = null,
                        programmeWorkoutName = null,
                        // Clear any pending celebrations from previous workouts
                        pendingPRs = emptyList(),
                        shouldShowPRCelebration = false,
                    )

                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()
            } else {
                val ongoingWorkout = repository.getOngoingWorkout()
                if (ongoingWorkout != null) {
                    resumeWorkout(ongoingWorkout.id)
                }
            }
        }
    }

    // Complete the current workout (handles both regular and programme workouts)
    fun completeWorkout(
        onComplete: (() -> Unit)? = null,
        onProgrammeComplete: ((Long) -> Unit)? = null,
    ) {
        val currentId = _currentWorkoutId.value ?: return

        viewModelScope.launch {
            val state = _workoutState.value

            // Store programme ID before completion if this is a programme workout
            val programmeId =
                if (state.isProgrammeWorkout) {
                    repository.getWorkoutById(currentId)?.programmeId
                } else {
                    null
                }

            // Calculate final duration and complete the workout
            val finalDuration =
                if (workoutTimerStartTime != null) {
                    java.time.Duration
                        .between(workoutTimerStartTime, LocalDateTime.now())
                        .seconds
                } else {
                    _workoutTimerSeconds.value.toLong()
                }

            // Complete the workout (this will automatically update programme progress if applicable)
            repository.completeWorkout(currentId, finalDuration)

            val newStatus = WorkoutStatus.COMPLETED

            // Clear rest timer on workout completion
            skipRestTimer()

            // Stop workout timer
            stopWorkoutTimer()

            _workoutState.value =
                state.copy(
                    isActive = false,
                    status = newStatus,
                    isReadOnly = true,
                    isInEditMode = false,
                    originalWorkoutData = null,
                )

            loadInProgressWorkouts()

            // Check if programme is complete
            if (programmeId != null) {
                val programme = repository.getProgrammeById(programmeId)
                if (programme?.status == com.github.radupana.featherweight.data.programme.ProgrammeStatus.COMPLETED) {
                    onProgrammeComplete?.invoke(programmeId)
                    return@launch
                }
            }

            // Callback for UI to handle post-completion actions
            onComplete?.invoke()
        }
    }

    // Update workout name (separate from notes)
    fun updateWorkoutName(name: String?) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.updateWorkoutName(currentId, name)
            _workoutState.value = _workoutState.value.copy(workoutName = name)
            loadInProgressWorkouts()
        }
    }

    // Get display name for workout
    fun getWorkoutDisplayName(): String {
        val state = _workoutState.value
        return state.workoutName ?: state.startTime?.format(
            DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
        ) ?: "Current Workout"
    }

    // Check if current workout can be edited
    fun canEditWorkout(): Boolean {
        val state = _workoutState.value
        // Completed workouts are always read-only, no exceptions
        return !state.isReadOnly
    }

    // Public synchronous function for UI
    fun canMarkSetComplete(set: SetLog): Boolean {
        // Use cached validation result if available
        val cachedResult = _setCompletionValidation.value[set.id]
        if (cachedResult != null) {
            return cachedResult
        }

        // If no cached result, trigger async validation and force UI update
        viewModelScope.launch {
            val validationResult = canMarkSetCompleteInternal(set)
            val validationMap = _setCompletionValidation.value.toMutableMap()
            validationMap[set.id] = validationResult
            _setCompletionValidation.value = validationMap

            // Force UI update by updating sets state to trigger recomposition
            _selectedExerciseSets.value = _selectedExerciseSets.value.toList()
        }

        // Use the same logic as canMarkSetCompleteInternal for fallback
        // This ensures consistency between cached and uncached results
        val exerciseLog = _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId }
        val fallbackResult =
            if (exerciseLog?.exerciseId == null) {
                // Legacy exercise without exercise reference - assume weight is required
                set.actualReps > 0 && set.actualWeight > 0
            } else {
                // For now, assume weight is required unless we know otherwise
                // This is conservative but matches what most users expect
                set.actualReps > 0 && set.actualWeight > 0
            }

        return fallbackResult
    }

    // Internal suspend function for validation logic
    private suspend fun canMarkSetCompleteInternal(set: SetLog): Boolean {
        // Find the exercise log for this set
        val exerciseLog = _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId }

        // If exerciseLog has an exerciseId, check if the exercise requires weight
        val exerciseDetails = exerciseLog?.exerciseId?.let { repository.getExerciseById(it) }
        val requiresWeight = exerciseDetails?.exercise?.requiresWeight ?: true

        val result =
            if (!requiresWeight) {
                // For exercises that don't require weight (like pull-ups), only check actualReps
                set.actualReps > 0
            } else {
                // For exercises that require weight, check both actualReps and actualWeight
                set.actualReps > 0 && set.actualWeight > 0
            }

        return result
    }

    private suspend fun loadExercisesForWorkout(workoutId: Long) {
        // Set loading state to true before starting
        _workoutState.value = _workoutState.value.copy(isLoadingExercises = true)

        try {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)
            // Wait for sets to be loaded completely
            loadAllSetsForCurrentExercisesAndWait()
        } finally {
            // Always set loading state to false when done
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        }
    }

    private suspend fun updateSetCompletionValidation() {
        val validationMap = mutableMapOf<Long, Boolean>()
        _selectedExerciseSets.value.forEach { set ->
            val isValid = canMarkSetCompleteInternal(set)
            validationMap[set.id] = isValid
        }
        _setCompletionValidation.value = validationMap

        // Force UI recomposition by updating the sets state
        // This ensures the UI reflects the updated validation cache
        _selectedExerciseSets.value = _selectedExerciseSets.value.toList()
    }

    private fun loadAllSetsForCurrentExercises() {
        viewModelScope.launch {
            val allSets = mutableListOf<SetLog>()
            _selectedWorkoutExercises.value.forEach { exercise ->
                val sets = repository.getSetsForExercise(exercise.id)
                allSets.addAll(sets)
            }
            _selectedExerciseSets.value = allSets
            updateSetCompletionValidation()
            // Load 1RM estimates when sets are loaded
            loadOneRMEstimatesForCurrentExercises()
        }
    }

    private fun loadOneRMEstimatesForCurrentExercises() {
        viewModelScope.launch {
            val exerciseIds = _selectedWorkoutExercises.value.mapNotNull { it.exerciseId }
            if (exerciseIds.isEmpty()) {
                _oneRMEstimates.value = emptyMap()
                return@launch
            }

            try {
                val userId = repository.getCurrentUserId()
                val maxes = repository.getCurrentMaxesForExercises(userId, exerciseIds)

                val estimatesMap = mutableMapOf<Long, Float>()
                maxes.forEach { max ->
                    if (max.oneRMEstimate > 0) {
                        estimatesMap[max.exerciseId] = max.oneRMEstimate
                    }
                }

                _oneRMEstimates.value = estimatesMap
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to load 1RM estimates", e)
            }
        }
    }

    private suspend fun loadAllSetsForCurrentExercisesAndWait() {
        val allSets = mutableListOf<SetLog>()
        _selectedWorkoutExercises.value.forEach { exercise ->
            val sets = repository.getSetsForExercise(exercise.id)
            allSets.addAll(sets)
        }
        _selectedExerciseSets.value = allSets
        updateSetCompletionValidation()
    }

    // ===== ENHANCED EXERCISE MANAGEMENT =====

    // Updated to work with ExerciseWithDetails instead of just exercise name
    fun addExerciseToCurrentWorkout(exercise: ExerciseWithDetails) {
        if (!canEditWorkout()) return

        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.insertExerciseLogWithExerciseReference(
                workoutId = currentId,
                exercise = exercise,
                exerciseOrder = selectedWorkoutExercises.value.size,
            )
            loadExercisesForWorkout(currentId)
            loadExerciseHistory(exercise.exercise.name)
            loadInProgressWorkouts()
        }
    }

    // Backward compatibility method for simple exercise addition by name
    fun addExerciseToCurrentWorkout(exerciseName: String) {
        if (!canEditWorkout()) return

        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            // Try to find existing exercise by name
            val matchingExercises = repository.searchExercises(exerciseName)
            val existingExercise =
                matchingExercises.firstOrNull {
                    it.name.equals(exerciseName, ignoreCase = true)
                }

            val exerciseLog =
                if (existingExercise != null) {
                    ExerciseLog(
                        workoutId = currentId,
                        exerciseName = existingExercise.name,
                        exerciseId = existingExercise.id,
                        exerciseOrder = selectedWorkoutExercises.value.size,
                    )
                } else {
                    // Create as legacy exercise log without exercise reference
                    ExerciseLog(
                        workoutId = currentId,
                        exerciseName = exerciseName,
                        exerciseId = null,
                        exerciseOrder = selectedWorkoutExercises.value.size,
                    )
                }

            repository.insertExerciseLog(exerciseLog)
            loadExercisesForWorkout(currentId)
            loadExerciseHistory(exerciseName)
            loadInProgressWorkouts()
        }
    }


    private fun getComplementaryMuscleGroups(currentMuscles: Set<String>): List<String> {
        val complementary = mutableListOf<String>()

        // Basic push/pull balance
        val hasPush =
            currentMuscles.any {
                it.contains("Chest", ignoreCase = true) ||
                    it.contains("Tricep", ignoreCase = true) ||
                    it.contains("Shoulder", ignoreCase = true)
            }
        val hasPull =
            currentMuscles.any {
                it.contains("Back", ignoreCase = true) ||
                    it.contains("Lat", ignoreCase = true) ||
                    it.contains("Bicep", ignoreCase = true)
            }

        if (hasPush && !hasPull) {
            complementary.addAll(listOf("Lats", "Biceps"))
        } else if (hasPull && !hasPush) {
            complementary.addAll(listOf("Chest", "Triceps"))
        }

        // Upper/lower balance
        val hasUpper =
            currentMuscles.any {
                it.contains("Chest", ignoreCase = true) ||
                    it.contains("Back", ignoreCase = true) ||
                    it.contains("Shoulder", ignoreCase = true)
            }
        val hasLower =
            currentMuscles.any {
                it.contains("Quad", ignoreCase = true) ||
                    it.contains("Hamstring", ignoreCase = true) ||
                    it.contains("Glute", ignoreCase = true)
            }

        if (hasUpper && !hasLower) {
            complementary.addAll(listOf("Quadriceps", "Hamstrings", "Glutes"))
        } else if (hasLower && !hasUpper) {
            complementary.addAll(listOf("Chest", "Lats", "Shoulders"))
        }

        return complementary.distinct()
    }

    private fun loadExerciseHistory(exerciseName: String) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            val history = repository.getExerciseHistory(exerciseName, currentId)
            if (history != null) {
                val currentHistory = _exerciseHistory.value.toMutableMap()
                currentHistory[exerciseName] = history
                _exerciseHistory.value = currentHistory
            }
        }
    }

    // Public method to load exercise history for SetEditingModal
    fun loadExerciseHistoryForName(exerciseName: String) {
        loadExerciseHistory(exerciseName)
    }

    // ===== EXISTING SET MANAGEMENT METHODS =====

    // Set management - only allowed if workout can be edited
    fun addSetToExercise(
        exerciseLogId: Long,
        targetReps: Int = 0,
        targetWeight: Float? = null,
        weight: Float = 0f,
        reps: Int = 0,
        rpe: Float? = null,
        onSetCreated: (Long) -> Unit = {},
    ) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val setOrder = repository.getSetsForExercise(exerciseLogId).size
            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setOrder,
                    targetReps = targetReps,
                    targetWeight = targetWeight,
                    actualReps = reps,
                    actualWeight = weight,
                    actualRpe = rpe,
                    tag = null,
                    notes = null,
                    isCompleted = false,
                    completedAt = null,
                )
            val newSetId = repository.insertSetLog(setLog)

            // Immediately update validation cache for the new set with actual values
            val newSet = setLog.copy(id = newSetId)
            val validationResult = canMarkSetCompleteInternal(newSet)
            val validationMap = _setCompletionValidation.value.toMutableMap()
            validationMap[newSetId] = validationResult
            _setCompletionValidation.value = validationMap

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
            onSetCreated(newSetId)
        }
    }

    fun updateSet(
        setId: Long,
        reps: Int,
        weight: Float,
        rpe: Float?,
    ) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val currentSets = _selectedExerciseSets.value
            val currentSet = currentSets.firstOrNull { it.id == setId }
            if (currentSet != null) {
                val updatedSet =
                    currentSet.copy(
                        actualReps = reps,
                        actualWeight = weight,
                        actualRpe = rpe,
                    )

                // Update the set in the local state immediately to prevent UI flicker
                val updatedSets =
                    currentSets.map { set ->
                        if (set.id == setId) updatedSet else set
                    }
                _selectedExerciseSets.value = updatedSets

                // Update validation cache for this set immediately within the same coroutine
                val validationResult = canMarkSetCompleteInternal(updatedSet)
                val validationMap = _setCompletionValidation.value.toMutableMap()
                validationMap[setId] = validationResult
                _setCompletionValidation.value = validationMap

                // Then persist to database
                repository.updateSetLog(updatedSet)

                // Only reload in-progress workouts to update the card
                loadInProgressWorkouts()
            }
        }
    }

    fun deleteSet(setId: Long) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            // Update local state immediately
            val updatedSets = _selectedExerciseSets.value.filter { it.id != setId }
            _selectedExerciseSets.value = updatedSets

            // Then delete from database
            repository.deleteSetLog(setId)
            loadInProgressWorkouts()
        }
    }

    fun markSetCompleted(
        setId: Long,
        completed: Boolean,
    ) {
        // Validation: Only allow marking complete if set has reps and weight
        if (completed) {
            val set = _selectedExerciseSets.value.find { it.id == setId }
            if (set == null || !canMarkSetComplete(set)) {
                return // Don't allow completion without valid data
            }
        }

        val timestamp =
            if (completed) {
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } else {
                null
            }

        viewModelScope.launch {
            // Update local state immediately
            val updatedSets =
                _selectedExerciseSets.value.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            isCompleted = completed,
                            completedAt = timestamp,
                        )
                    } else {
                        set
                    }
                }
            _selectedExerciseSets.value = updatedSets

            // Update workout status to IN_PROGRESS when first set is completed
            val currentWorkoutId = _currentWorkoutId.value
            if (completed && currentWorkoutId != null) {
                val currentState = _workoutState.value
                if (currentState.isActive && currentState.status != WorkoutStatus.COMPLETED) {
                    // Check if this is the first completed set
                    val hasCompletedSets = _selectedExerciseSets.value.any { it.isCompleted && it.id != setId }
                    if (!hasCompletedSets) {
                        repository.updateWorkoutStatus(currentWorkoutId, WorkoutStatus.IN_PROGRESS)
                    }
                }
            }

            // Then persist to database
            repository.markSetCompleted(setId, completed, timestamp)

            // Auto-start rest timer when set is completed
            if (completed) {
                startRestTimer(90) // Hardcoded 90 seconds

                // Start workout timer on first set completion
                if (workoutTimerStartTime == null) {
                    startWorkoutTimer()
                }

                // Check for 1RM updates
                val completedSet = _selectedExerciseSets.value.find { it.id == setId }
                completedSet?.let { set ->
                    checkAndUpdateOneRM(set)
                }
            }

            // Check for Personal Records AFTER database save (when completed)
            if (completed) {
                try {
                    // Get the set data from local state (which now includes the completion)
                    val completedSet = updatedSets.find { it.id == setId }

                    // Get exercise name for this set
                    val exerciseLog =
                        _selectedWorkoutExercises.value.find { exerciseLog ->
                            updatedSets.any { it.exerciseLogId == exerciseLog.id && it.id == setId }
                        }

                    if (exerciseLog != null && completedSet != null) {
                        val allPRs = repository.checkForPR(completedSet, exerciseLog.exerciseName)

                        if (allPRs.isNotEmpty()) {
                            allPRs.forEach { pr ->
                            }

                            // Update state to show PR celebration
                            _workoutState.value =
                                _workoutState.value.copy(
                                    pendingPRs = _workoutState.value.pendingPRs + allPRs,
                                    shouldShowPRCelebration = true,
                                )
                        }
                    } else {
                    }
                } catch (e: Exception) {
                    // Log error but don't fail set completion
                    Log.e("WorkoutViewModel", "PR detection failed", e)
                }

                // Check if we should update 1RM estimate
                try {
                    val completedSet = updatedSets.find { it.id == setId }
                    val exerciseLog =
                        _selectedWorkoutExercises.value.find { exerciseLog ->
                            updatedSets.any { it.exerciseLogId == exerciseLog.id && it.id == setId }
                        }

                    if (exerciseLog != null && completedSet != null) {
                        val currentEstimate = _oneRMEstimates.value[exerciseLog.exerciseId]
                        val newEstimate =
                            oneRMService.calculateEstimated1RM(
                                completedSet.actualWeight,
                                completedSet.actualReps,
                            )

                        if (newEstimate != null &&
                            oneRMService.shouldUpdateOneRM(
                                completedSet,
                                currentEstimate,
                                newEstimate,
                            )
                        ) {
                            // Calculate confidence
                            val percentOf1RM =
                                if (currentEstimate != null && currentEstimate > 0) {
                                    completedSet.actualWeight / currentEstimate
                                } else {
                                    1f
                                }

                            val confidence =
                                oneRMService.calculateConfidence(
                                    completedSet.actualReps,
                                    completedSet.actualRpe,
                                    percentOf1RM,
                                )

                            // Get current user and update 1RM
                            val userId = repository.getCurrentUserId()
                            val exerciseId = exerciseLog.exerciseId
                            if (exerciseId != null) {
                                val oneRMRecord =
                                    oneRMService.createOneRMRecord(
                                        userId = userId,
                                        exerciseId = exerciseId,
                                        set = completedSet,
                                        estimate = newEstimate,
                                        confidence = confidence,
                                    )

                                repository.updateOrInsertOneRM(oneRMRecord)

                                // Reload 1RM estimates
                                loadOneRMEstimatesForCurrentExercises()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutViewModel", "Failed to update 1RM estimate", e)
                }
            }

            loadInProgressWorkouts()
        }
    }

    fun completeAllSetsInExercise(exerciseLogId: Long) {
        viewModelScope.launch {
            val exerciseSets = _selectedExerciseSets.value.filter { it.exerciseLogId == exerciseLogId }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            exerciseSets.forEach { set ->
                // Only mark as complete if set has valid data
                if (canMarkSetCompleteInternal(set) && !set.isCompleted) {
                    repository.markSetCompleted(set.id, true, timestamp)
                }
            }

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
        }
    }

    fun completeAllSetsInWorkout() {
        viewModelScope.launch {
            val allSets = _selectedExerciseSets.value
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            allSets.forEach { set ->
                // Only mark as complete if set has valid data and is not already completed
                if (canMarkSetCompleteInternal(set) && !set.isCompleted) {
                    repository.markSetCompleted(set.id, true, timestamp)
                }
            }

            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
        }
    }

    fun canCompleteAllSetsInWorkout(): Boolean {
        val allSets = _selectedExerciseSets.value
        val result = allSets.isNotEmpty() && allSets.any { canMarkSetComplete(it) && !it.isCompleted }

        return result
    }

    // Smart suggestions
    suspend fun getSmartSuggestions(exerciseName: String): SmartSuggestions? {
        val currentId = _currentWorkoutId.value ?: return null
        return repository.getSmartSuggestions(exerciseName, currentId)
    }

    // Get intelligent suggestions with reasoning and alternatives
    suspend fun getIntelligentSuggestions(exerciseName: String): SmartSuggestions = repository.getSmartSuggestionsEnhanced(exerciseName)

    fun loadSetsForExercise() {
        viewModelScope.launch {
            loadAllSetsForCurrentExercises()
        }
    }

    fun deleteExercise(exerciseLogId: Long) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            repository.deleteExerciseLog(exerciseLogId)
            val currentId = _currentWorkoutId.value ?: return@launch
            loadExercisesForWorkout(currentId)
            loadInProgressWorkouts()
        }
    }


    // ===== EXERCISE SWAP METHODS =====

    fun initiateExerciseSwap(exerciseLogId: Long) {
        if (!canEditWorkout()) return

        val exercise = _selectedWorkoutExercises.value.find { it.id == exerciseLogId }
        if (exercise != null) {
            _swappingExercise.value = exercise
        }
    }

    fun cancelExerciseSwap() {
        _swappingExercise.value = null
    }

    fun confirmExerciseSwap(newExerciseId: Long) {
        if (!canEditWorkout()) return

        val swappingExercise = _swappingExercise.value ?: return

        viewModelScope.launch {
            try {
                // Get the new exercise details
                val newExercise = repository.getExerciseEntityById(newExerciseId)
                if (newExercise != null) {
                    // Perform the swap
                    repository.swapExercise(
                        exerciseLogId = swappingExercise.id,
                        newExerciseId = newExerciseId,
                        newExerciseName = newExercise.name,
                        originalExerciseId = swappingExercise.exerciseId ?: swappingExercise.id,
                    )

                    // Clear all sets for this exercise
                    repository.deleteSetsForExerciseLog(swappingExercise.id)

                    // Record swap history
                    val userId = repository.getCurrentUserId()
                    repository.recordExerciseSwap(
                        userId = userId,
                        originalExerciseId = swappingExercise.exerciseId ?: swappingExercise.id,
                        swappedToExerciseId = newExerciseId,
                        workoutId = _currentWorkoutId.value,
                        programmeId = _workoutState.value.programmeId,
                    )

                    // Reload exercises for the workout
                    val currentId = _currentWorkoutId.value ?: return@launch
                    loadExercisesForWorkout(currentId)

                    // Clear swap state
                    _swappingExercise.value = null
                }
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to swap exercise", e)
            }
        }
    }

    fun reorderExercisesInstantly(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (!canEditWorkout()) return

        val exercises = _selectedWorkoutExercises.value.toMutableList()
        if (fromIndex in exercises.indices && toIndex in exercises.indices && fromIndex != toIndex) {
            // Log before reorder
            Log.d(
                "DragReorder",
                "BEFORE reorder: ${
                    exercises.mapIndexed {
                        idx,
                        ex,
                        ->
                        "$idx:${ex.exerciseName}(order=${ex.exerciseOrder})"
                    }.joinToString()
                }",
            )

            // Move the item in the list
            val item = exercises.removeAt(fromIndex)
            exercises.add(toIndex, item)

            // Update the exerciseOrder property in each object to match its new position
            val updatedExercises =
                exercises.mapIndexed { index, exercise ->
                    exercise.copy(exerciseOrder = index)
                }

            // Log after reorder
            Log.d(
                "DragReorder",
                "AFTER reorder: ${
                    updatedExercises.mapIndexed {
                        idx,
                        ex,
                        ->
                        "$idx:${ex.exerciseName}(order=${ex.exerciseOrder})"
                    }.joinToString()
                }",
            )

            // Update the UI state immediately for smooth animation
            _selectedWorkoutExercises.value = updatedExercises

            // Update the database in the background
            viewModelScope.launch {
                // Update all exercise orders in a single transaction
                updatedExercises.forEach { exercise ->
                    repository.updateExerciseOrder(exercise.id, exercise.exerciseOrder)
                }
            }
        }
    }

    // Delete the current workout entirely
    fun deleteCurrentWorkout() {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.deleteWorkout(currentId)

            // Reset state after deletion
            _currentWorkoutId.value = null
            _workoutState.value = WorkoutState()
            _selectedWorkoutExercises.value = emptyList()
            _selectedExerciseSets.value = emptyList()
            loadInProgressWorkouts()
        }
    }

    // Delete any workout by ID
    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            // Stop timers if deleting current workout
            if (workoutId == _currentWorkoutId.value) {
                skipRestTimer()
                stopWorkoutTimer()
            }

            repository.deleteWorkout(workoutId)
            loadInProgressWorkouts()
        }
    }

    // Repeat a completed workout as a new freestyle workout
    fun repeatWorkout() {
        viewModelScope.launch {
            val currentWorkoutId = _currentWorkoutId.value
            if (currentWorkoutId != null) {
                try {
                    val newWorkoutId = repository.copyWorkoutAsFreestyle(currentWorkoutId)

                    // Clear current state
                    _currentWorkoutId.value = null
                    _workoutState.value = WorkoutState()
                    _selectedWorkoutExercises.value = emptyList()
                    _selectedExerciseSets.value = emptyList()
                    _setCompletionValidation.value = emptyMap() // Clear validation cache

                    // Clear timer state for new workout
                    stopWorkoutTimer()
                    _workoutTimerSeconds.value = 0
                    workoutTimerStartTime = null

                    // Load the new workout - this will populate the validation cache
                    resumeWorkout(newWorkoutId)

                    // Update only the workout name after loading is complete
                    // Don't overwrite the entire state which would interfere with validation
                    delay(100) // Small delay to ensure resumeWorkout completes
                    _workoutState.value =
                        _workoutState.value.copy(
                            workoutName = "Repeat Workout",
                        )
                } catch (e: Exception) {
                    Log.e("WorkoutViewModel", "Error repeating workout", e)
                }
            }
        }
    }

    // ===== PROGRAMME WORKOUT METHODS =====

    // Start a workout from a programme template
    suspend fun startProgrammeWorkout(
        programmeId: Long,
        weekNumber: Int,
        dayNumber: Int,
        userMaxes: Map<String, Float> = emptyMap(),
        onReady: (() -> Unit)? = null,
    ) {
        try {
            // Set loading state immediately
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = true)

            // Get programme information
            val programme = repository.getActiveProgramme()
            if (programme == null || programme.id != programmeId) {
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }

            // Check if a workout already exists for this programme/week/day
            val existingWorkout =
                _inProgressWorkouts.value.find {
                    it.isProgrammeWorkout &&
                        it.programmeId == programmeId &&
                        it.weekNumber == weekNumber &&
                        it.dayNumber == dayNumber
                }

            if (existingWorkout != null) {
                resumeWorkout(existingWorkout.id)
                onReady?.invoke()
                return
            }

            // Create workout from programme template
            // Note: dayNumber here is the sequential day (1,2,3,4), not the template day
            val workoutId =
                repository.createWorkoutFromProgrammeTemplate(
                    programmeId = programmeId,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    userMaxes = userMaxes,
                )

            // Get the created workout to extract metadata
            val workout = repository.getWorkoutById(workoutId)
            if (workout == null) {
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }

            _currentWorkoutId.value = workoutId
            _workoutState.value =
                WorkoutState(
                    isActive = true,
                    status = WorkoutStatus.IN_PROGRESS,
                    workoutId = workoutId,
                    startTime = workout.date,
                    workoutName = workout.programmeWorkoutName,
                    isReadOnly = false,
                    isInEditMode = false,
                    originalWorkoutData = null,
                    // Programme context
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    programmeName = programme.name,
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    programmeWorkoutName = workout.programmeWorkoutName,
                    // Ensure loading state is set
                    isLoadingExercises = true,
                    // Clear any pending celebrations from previous workouts
                    pendingPRs = emptyList(),
                    shouldShowPRCelebration = false,
                )

            // IMPORTANT: Wait for exercises to load completely before allowing navigation
            loadExercisesForWorkout(workoutId)
            loadInProgressWorkouts()

            // Notify that workout is ready for navigation
            onReady?.invoke()
        } catch (e: Exception) {
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        }
    }

    // Get next programme workout for the active programme
    suspend fun getNextProgrammeWorkout(): NextProgrammeWorkoutInfo? {
        val activeProgramme = repository.getActiveProgramme() ?: return null
        return repository.getNextProgrammeWorkout(activeProgramme.id)
    }


    // Get programme display name for current workout
    fun getProgrammeDisplayName(): String? {
        val state = _workoutState.value
        return if (state.isProgrammeWorkout) {
            buildString {
                state.programmeName?.let { append(it) }
                if (state.weekNumber != null && state.dayNumber != null) {
                    if (isNotEmpty()) append(" - ")
                    append("Week ${state.weekNumber}, Day ${state.dayNumber}")
                }
                state.programmeWorkoutName?.let { name ->
                    if (isNotEmpty()) append("\n")
                    append(name)
                }
            }.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }

    // Get programme progress information
    suspend fun getProgrammeProgress(): Pair<Int, Int>? {
        val state = _workoutState.value
        return if (state.isProgrammeWorkout && state.programmeId != null) {
            repository.getProgrammeWorkoutProgress(state.programmeId)
        } else {
            null
        }
    }


    // ===== ENHANCED SET MANAGEMENT FOR TARGET/ACTUAL =====

    fun updateSetTarget(
        setId: Long,
        targetReps: Int,
        targetWeight: Float?,
    ) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val currentSets = _selectedExerciseSets.value
            val currentSet = currentSets.firstOrNull { it.id == setId }
            if (currentSet != null) {
                val updatedSet =
                    currentSet.copy(
                        targetReps = targetReps,
                        targetWeight = targetWeight,
                    )

                // Update the set in the local state immediately
                val updatedSets =
                    currentSets.map { set ->
                        if (set.id == setId) updatedSet else set
                    }
                _selectedExerciseSets.value = updatedSets

                // Persist to database
                repository.updateSetLog(updatedSet)
            }
        }
    }

    // ===== PR CELEBRATION METHODS =====



    // ===== INTELLIGENT SUGGESTIONS =====

    // Removed weight suggestion feature - will be reimplemented later with better intelligence

    // ===== REST TIMER FUNCTIONS =====

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restTimerSeconds.value = seconds
        _restTimerInitialSeconds.value = seconds

        restTimerJob =
            viewModelScope.launch {
                while (_restTimerSeconds.value > 0) {
                    delay(1000)
                    _restTimerSeconds.value -= 1
                }
                // Timer completed - vibration will be handled by UI
            }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = 0
        _restTimerInitialSeconds.value = 0
    }

    fun adjustRestTimer(adjustment: Int) {
        val newValue = (_restTimerSeconds.value + adjustment).coerceAtLeast(0)
        if (newValue > 0) {
            _restTimerSeconds.value = newValue
            // Only update initialSeconds when adding time (+15s), not when subtracting (-15s)
            if (adjustment > 0) {
                _restTimerInitialSeconds.value = newValue
            }
        } else {
            skipRestTimer()
        }
    }

    fun selectRestTimerPreset(seconds: Int) {
        startRestTimer(seconds)
    }

    fun toggleRestTimerExpanded() {
        _restTimerExpanded.value = !_restTimerExpanded.value
    }

    // Workout timer functions
    private fun startWorkoutTimer() {
        val currentWorkoutId = _currentWorkoutId.value ?: return

        viewModelScope.launch {
            // Save timer start time to database
            workoutTimerStartTime = LocalDateTime.now()
            repository.updateWorkoutTimerStart(currentWorkoutId, workoutTimerStartTime!!)

            // Capture the start time in a local variable to avoid race conditions
            val startTime = workoutTimerStartTime!!

            // Start the timer coroutine
            workoutTimerJob?.cancel()
            workoutTimerJob =
                viewModelScope.launch {
                    while (coroutineContext.isActive) {
                        val elapsed =
                            java.time.Duration
                                .between(startTime, LocalDateTime.now())
                                .seconds
                        _workoutTimerSeconds.value = elapsed.toInt()
                        delay(1000)
                    }
                }
        }
    }

    private fun stopWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = null
    }

    private fun resumeWorkoutTimer(startTime: LocalDateTime) {
        workoutTimerStartTime = startTime

        // Calculate elapsed time
        val elapsed =
            java.time.Duration
                .between(startTime, LocalDateTime.now())
                .seconds
        _workoutTimerSeconds.value = elapsed.toInt()

        // Capture the start time in a local variable to avoid race conditions
        val capturedStartTime = startTime

        // Resume counting
        workoutTimerJob =
            viewModelScope.launch {
                while (coroutineContext.isActive) {
                    val newElapsed =
                        java.time.Duration
                            .between(capturedStartTime, LocalDateTime.now())
                            .seconds
                    _workoutTimerSeconds.value = newElapsed.toInt()
                    delay(1000)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
        workoutTimerJob?.cancel()
    }

    private fun checkAndUpdateOneRM(set: SetLog) {
        viewModelScope.launch {
            // Get exercise info
            val exercise = _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId } ?: return@launch
            val exerciseId = exercise.exerciseId ?: return@launch

            // Calculate estimated 1RM from this set (now with RPE consideration)
            val estimated1RM = oneRMService.calculateEstimated1RM(set.actualWeight, set.actualReps, set.actualRpe) ?: return@launch

            // Get current 1RM
            val userId = repository.getCurrentUserId()
            val currentMax =
                repository
                    .getCurrentMaxesForExercises(userId, listOf(exerciseId))
                    .firstOrNull()
                    ?.oneRMEstimate

            // Check if we should update
            if (oneRMService.shouldUpdateOneRM(set, currentMax, estimated1RM)) {
                // Calculate confidence
                val percentOf1RM =
                    if (currentMax != null && currentMax > 0) {
                        set.actualWeight / currentMax
                    } else {
                        1f
                    }

                // Create context string
                val context = oneRMService.buildContext(set.actualWeight, set.actualReps, set.actualRpe)

                // Get the workout date
                val workoutDate =
                    _currentWorkoutId.value?.let { workoutId ->
                        repository.getWorkoutById(workoutId)?.date
                    }

                // Update the 1RM with the workout date
                repository.upsertExerciseMax(
                    userId = userId,
                    exerciseId = exerciseId,
                    oneRMEstimate = estimated1RM,
                    oneRMContext = context,
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
                    notes = "Updated from workout performance",
                    workoutDate = workoutDate,
                )

                // Reload 1RM estimates to update UI immediately
                loadOneRMEstimatesForCurrentExercises()
            }
        }
    }

    // Notes methods
    fun loadWorkoutNotes(
        workoutId: Long,
        callback: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val notes = repository.getWorkoutNotes(workoutId)
                callback(notes)
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to load workout notes", e)
                callback(null)
            }
        }
    }

    fun saveWorkoutNotes(
        workoutId: Long,
        notes: String,
    ) {
        viewModelScope.launch {
            try {
                repository.updateWorkoutNotes(workoutId, notes)
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Failed to save workout notes", e)
            }
        }
    }
}
