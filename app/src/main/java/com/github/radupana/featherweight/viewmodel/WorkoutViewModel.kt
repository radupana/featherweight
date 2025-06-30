package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.exercise.*
import com.github.radupana.featherweight.domain.ExerciseHistory
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WorkoutState(
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
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
    // Workout Timer
    val workoutTimerStartTime: Long? = null,
    val isWorkoutTimerActive: Boolean = false,
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
    private val repository = FeatherweightRepository(application)
    
    // Rest timer reference to manage its lifecycle
    private var restTimerViewModel: RestTimerViewModel? = null
    
    fun setRestTimerViewModel(restTimer: RestTimerViewModel) {
        this.restTimerViewModel = restTimer
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

    // Workout timer state
    private val _elapsedWorkoutTime = MutableStateFlow(0L)
    val elapsedWorkoutTime: StateFlow<Long> = _elapsedWorkoutTime
    private var workoutTimerJob: Job? = null

    // Exercise-related state
    private val _availableExercises = MutableStateFlow<List<ExerciseWithDetails>>(emptyList())
    val availableExercises: StateFlow<List<ExerciseWithDetails>> = _availableExercises

    private val _exerciseDetails = MutableStateFlow<Map<Long, ExerciseWithDetails>>(emptyMap())
    val exerciseDetails: StateFlow<Map<Long, ExerciseWithDetails>> = _exerciseDetails

    init {
        checkForOngoingWorkout()
        loadInProgressWorkouts()
        loadExercises()
    }

    // ===== EXERCISE METHODS =====

    private fun loadExercises() {
        viewModelScope.launch {
            try {
                val exercises = repository.getAllExercises()
                _availableExercises.value = exercises

                // Create lookup map for exercise details
                val detailsMap = exercises.associateBy { it.exercise.id }
                _exerciseDetails.value = detailsMap
            } catch (e: Exception) {
                println("Error loading exercises: ${e.message}")
            }
        }
    }

    suspend fun getAllExercises(): List<ExerciseWithDetails> = repository.getAllExercises()

    suspend fun searchExercises(query: String): List<Exercise> = repository.searchExercises(query)

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> = repository.getExercisesByCategory(category)

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<ExerciseWithDetails> {
        val allExercises = repository.getAllExercises()
        return allExercises.filter { it.exercise.muscleGroup.equals(muscleGroup, ignoreCase = true) }
    }

    suspend fun getFilteredExercises(
        category: ExerciseCategory? = null,
        muscleGroup: String? = null,
        equipment: Equipment? = null,
        availableEquipment: List<Equipment> = emptyList(),
        maxDifficulty: ExerciseDifficulty? = null,
        searchQuery: String = "",
    ): List<ExerciseWithDetails> {
        // TODO: Re-implement filtering logic
        val allExercises = repository.getAllExercises()
        return allExercises.filter { exercise ->
            (category == null || exercise.exercise.category == category) &&
            (muscleGroup == null || exercise.exercise.muscleGroup.equals(muscleGroup, ignoreCase = true)) &&
            (equipment == null || exercise.exercise.equipment == equipment) &&
            (searchQuery.isEmpty() || exercise.exercise.name.contains(searchQuery, ignoreCase = true))
        }
    }

    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory = ExerciseCategory.FULL_BODY,
        primaryMuscles: Set<String> = emptySet(),
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
    ): ExerciseWithDetails? {
        // TODO: Re-implement custom exercise creation
        println("Custom exercise creation not yet implemented: $name")
        return null
    }

    fun getExerciseDetails(exerciseId: Long): ExerciseWithDetails? = _exerciseDetails.value[exerciseId]

    fun getExerciseDetailsForLog(exerciseLog: ExerciseLog): ExerciseWithDetails? =
        exerciseLog.exerciseId?.let { exerciseId ->
            getExerciseDetails(exerciseId)
        }

    // ===== EXISTING WORKOUT METHODS (Updated to work with exercises) =====

    // Check if there's an ongoing workout when the app starts
    private fun checkForOngoingWorkout() {
        viewModelScope.launch {
            val ongoingWorkout = repository.getOngoingWorkout()
            if (ongoingWorkout != null) {
                resumeWorkout(ongoingWorkout.id)
            }
        }
    }

    // Load all in-progress workouts for home screen
    fun loadInProgressWorkouts() {
        viewModelScope.launch {
            val workoutHistory = repository.getWorkoutHistory()
            val inProgress =
                workoutHistory
                    .filter { !it.isCompleted }
                    .map { summary ->
                        // Calculate completed sets
                        val completedSets = try {
                            val exercises = repository.getExercisesForWorkout(summary.id)
                            exercises.sumOf { exercise ->
                                val sets = repository.getSetsForExercise(exercise.id)
                                sets.count { it.isCompleted }
                            }
                        } catch (e: Exception) {
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
                    if (notes.contains("[COMPLETED]")) null else notes
                },
            startDate = ongoingWorkout.date,
            exerciseCount = exercises.size,
            setCount = allSets.size,
            completedSets = allSets.count { it.isCompleted },
        )
    }

    // Resume an existing workout
    fun resumeWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            if (workout != null) {
                val isCompleted = workout.notes?.contains("[COMPLETED]") == true

                // Get programme information if this is a programme workout
                val programmeName =
                    if (workout.isProgrammeWorkout && workout.programmeId != null) {
                        try {
                            repository.getProgrammeById(workout.programmeId)?.name
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = !isCompleted,
                        isCompleted = isCompleted,
                        workoutId = workoutId,
                        startTime = workout.date,
                        workoutName =
                            workout.notes?.let { notes ->
                                if (notes.contains("[COMPLETED]")) {
                                    notes
                                        .replace(" [COMPLETED]", "")
                                        .replace("[COMPLETED]", "")
                                        .trim()
                                        .takeIf { it.isNotBlank() }
                                } else {
                                    notes.takeIf { it.isNotBlank() }
                                }
                            },
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
                    )

                // Wait for exercises to load completely before UI renders
                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()
                
                // Bind rest timer to this workout (only if not completed)
                if (!isCompleted) {
                    // Clear any existing timer state before binding to this workout
                    restTimerViewModel?.onWorkoutCompleted()
                    restTimerViewModel?.bindToWorkout(workoutId)
                }
            }
        }
    }

    // Start a completely new workout (force new)
    fun startNewWorkout(forceNew: Boolean = false) {
        viewModelScope.launch {
            if (forceNew || repository.getOngoingWorkout() == null) {
                val workout = Workout(date = LocalDateTime.now(), notes = null)
                val workoutId = repository.insertWorkout(workout)

                _currentWorkoutId.value = workoutId
                _workoutState.value =
                    WorkoutState(
                        isActive = true,
                        isCompleted = false,
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
                    )

                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()
                
                // Clear any existing timer state before binding to new workout
                restTimerViewModel?.onWorkoutCompleted()
                // Bind rest timer to this workout
                restTimerViewModel?.bindToWorkout(workoutId)
            } else {
                val ongoingWorkout = repository.getOngoingWorkout()
                if (ongoingWorkout != null) {
                    resumeWorkout(ongoingWorkout.id)
                }
            }
        }
    }

    fun enterEditMode() {
        val currentState = _workoutState.value
        if (currentState.isCompleted && !currentState.isInEditMode) {
            // Backup current data for potential rollback
            val backupData =
                Triple(
                    _selectedWorkoutExercises.value,
                    _selectedExerciseSets.value,
                    currentState.workoutName,
                )

            _workoutState.value =
                currentState.copy(
                    isInEditMode = true,
                    isReadOnly = false,
                    originalWorkoutData = backupData,
                )
        }
    }

    fun saveEditModeChanges() {
        val currentState = _workoutState.value
        if (currentState.isInEditMode) {
            // Changes are already persisted through normal operations
            // Just exit edit mode and return to read-only
            _workoutState.value =
                currentState.copy(
                    isInEditMode = false,
                    isReadOnly = true,
                    originalWorkoutData = null,
                )
            loadInProgressWorkouts()
        }
    }

    fun discardEditModeChanges() {
        val currentState = _workoutState.value
        if (currentState.isInEditMode && currentState.originalWorkoutData != null) {
            val (originalExercises, originalSets, originalName) = currentState.originalWorkoutData

            // Restore original data
            _selectedWorkoutExercises.value = originalExercises
            _selectedExerciseSets.value = originalSets

            // Exit edit mode
            _workoutState.value =
                currentState.copy(
                    isInEditMode = false,
                    isReadOnly = true,
                    workoutName = originalName,
                    originalWorkoutData = null,
                )

            // Note: In a real app, you'd want to rollback database changes too
            // For simplicity, we're just restoring the UI state
        }
    }

    // Complete the current workout (handles both regular and programme workouts)
    fun completeWorkout(onComplete: (() -> Unit)? = null) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            val state = _workoutState.value

            // Debug: Check programme progress before completion
            if (state.isProgrammeWorkout && state.programmeId != null) {
                println("🔍 DEBUG: Checking programme progress BEFORE completion")
                val progressBefore = repository.getProgrammeWorkoutProgress(state.programmeId)
                println("📊 Progress before: ${progressBefore.first}/${progressBefore.second} workouts completed")
            }

            // Calculate final duration and complete the workout
            val finalDuration = if (state.isWorkoutTimerActive) {
                _elapsedWorkoutTime.value
            } else null
            
            // Complete the workout (this will automatically update programme progress if applicable)
            repository.completeWorkout(currentId, finalDuration)

            // Debug: Check programme progress after completion
            if (state.isProgrammeWorkout && state.programmeId != null) {
                println("🔍 DEBUG: Checking programme progress AFTER completion")
                val progressAfter = repository.getProgrammeWorkoutProgress(state.programmeId)
                println("📊 Progress after: ${progressAfter.first}/${progressAfter.second} workouts completed")
            }

            // Stop workout timer
            stopWorkoutTimer()
            
            // Stop rest timer for this workout
            restTimerViewModel?.onWorkoutCompleted()

            _workoutState.value =
                state.copy(
                    isActive = false,
                    isCompleted = true,
                    isReadOnly = true,
                    isInEditMode = false,
                    originalWorkoutData = null,
                )

            loadInProgressWorkouts()

            if (state.isProgrammeWorkout) {
                println("✅ Completed programme workout: ${state.programmeWorkoutName}")
            }

            // Callback for UI to handle post-completion actions
            onComplete?.invoke()
        }
    }

    // Update workout name
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
        return !state.isReadOnly || state.isInEditMode
    }

    // Public synchronous function for UI
    fun canMarkSetComplete(set: SetLog): Boolean {
        return _setCompletionValidation.value[set.id] ?: (set.reps > 0 && set.weight > 0)
    }

    // Internal suspend function for validation logic
    private suspend fun canMarkSetCompleteInternal(set: SetLog): Boolean {
        // Find the exercise log for this set
        val exerciseLog = _selectedWorkoutExercises.value.find { it.id == set.exerciseLogId }

        // If exerciseLog has an exerciseId, check if the exercise requires weight
        if (exerciseLog?.exerciseId != null) {
            val exerciseDetails = repository.getExerciseById(exerciseLog.exerciseId)
            return if (exerciseDetails?.exercise?.requiresWeight == false) {
                // For exercises that don't require weight (like ab roll), only check reps
                set.reps > 0
            } else {
                // For exercises that require weight, check both reps and weight
                set.reps > 0 && set.weight > 0
            }
        }

        // Fallback for legacy data without exerciseId - require both reps and weight
        return set.reps > 0 && set.weight > 0
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
            validationMap[set.id] = canMarkSetCompleteInternal(set)
        }
        _setCompletionValidation.value = validationMap
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
        // Resume timer if workout was previously started
        resumeWorkoutTimerIfNeeded()
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

    // Get exercise suggestions based on current workout context
    suspend fun getExerciseSuggestions(
        query: String = "",
        currentMuscleGroups: Set<String> = emptySet(),
        availableEquipment: List<Equipment> = Equipment.getBasicEquipment(),
    ): List<ExerciseWithDetails> =
        try {
            if (query.isNotEmpty()) {
                // Search-based suggestions
                val searchResults = repository.searchExercises(query)
                searchResults.mapNotNull { exercise ->
                    repository.getExerciseById(exercise.id)
                }
            } else {
                // Context-based suggestions
                val suggestions = mutableListOf<ExerciseWithDetails>()

                // If no current muscle groups, suggest compound movements
                if (currentMuscleGroups.isEmpty()) {
                    suggestions.addAll(
                        getFilteredExercises(
                            maxDifficulty = ExerciseDifficulty.ADVANCED,
                        ).filter { exercise ->
                            exercise.exercise.movementPattern.contains("Squat", ignoreCase = true) ||
                            exercise.exercise.movementPattern.contains("Press", ignoreCase = true) ||
                            exercise.exercise.movementPattern.contains("Deadlift", ignoreCase = true) ||
                            exercise.exercise.movementPattern.contains("Row", ignoreCase = true)
                        }.take(10),
                    )
                } else {
                    // Suggest complementary muscle groups
                    val complementaryMuscles = getComplementaryMuscleGroups(currentMuscleGroups)
                    complementaryMuscles.forEach { muscleGroup ->
                        suggestions.addAll(
                            getExercisesByMuscleGroup(muscleGroup).take(3),
                        )
                    }
                }

                suggestions.distinctBy { it.exercise.id }.take(15)
            }
        } catch (e: Exception) {
            println("Error getting exercise suggestions: ${e.message}")
            emptyList()
        }

    private fun getComplementaryMuscleGroups(currentMuscles: Set<String>): List<String> {
        val complementary = mutableListOf<String>()

        // Basic push/pull balance
        val hasPush = currentMuscles.any { it.contains("Chest", ignoreCase = true) || it.contains("Tricep", ignoreCase = true) || it.contains("Shoulder", ignoreCase = true) }
        val hasPull = currentMuscles.any { it.contains("Back", ignoreCase = true) || it.contains("Lat", ignoreCase = true) || it.contains("Bicep", ignoreCase = true) }

        if (hasPush && !hasPull) {
            complementary.addAll(listOf("Lats", "Biceps"))
        } else if (hasPull && !hasPush) {
            complementary.addAll(listOf("Chest", "Triceps"))
        }

        // Upper/lower balance
        val hasUpper = currentMuscles.any { it.contains("Chest", ignoreCase = true) || it.contains("Back", ignoreCase = true) || it.contains("Shoulder", ignoreCase = true) }
        val hasLower = currentMuscles.any { it.contains("Quad", ignoreCase = true) || it.contains("Hamstring", ignoreCase = true) || it.contains("Glute", ignoreCase = true) }

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

    // ===== EXISTING SET MANAGEMENT METHODS =====

    // Set management - only allowed if workout can be edited
    fun addSetToExercise(
        exerciseLogId: Long,
        weight: Float = 0f,
        reps: Int = 0,
        rpe: Float? = null,
    ) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val setOrder = repository.getSetsForExercise(exerciseLogId).size
            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setOrder,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    tag = null,
                    notes = null,
                    isCompleted = false,
                    completedAt = null,
                )
            repository.insertSetLog(setLog)
            loadAllSetsForCurrentExercises()
            loadInProgressWorkouts()
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
                val updatedSet = currentSet.copy(reps = reps, weight = weight, rpe = rpe)

                // Update the set in the local state immediately to prevent UI flicker
                val updatedSets =
                    currentSets.map { set ->
                        if (set.id == setId) updatedSet else set
                    }
                _selectedExerciseSets.value = updatedSets

                // Update validation cache for this set
                val validationMap = _setCompletionValidation.value.toMutableMap()
                validationMap[setId] = canMarkSetCompleteInternal(updatedSet)
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

            // Start workout timer on first set completion
            if (completed && !_workoutState.value.isWorkoutTimerActive) {
                startWorkoutTimer()
            }

            // Then persist to database
            repository.markSetCompleted(setId, completed, timestamp)
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

    // Smart suggestions
    suspend fun getSmartSuggestions(exerciseName: String): SmartSuggestions? {
        val currentId = _currentWorkoutId.value ?: return null
        return repository.getSmartSuggestions(exerciseName, currentId)
    }

    fun loadSetsForExercise(exerciseLogId: Long) {
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

    fun reorderExercises(fromIndex: Int, toIndex: Int) {
        if (!canEditWorkout()) return

        viewModelScope.launch {
            val exercises = _selectedWorkoutExercises.value.toMutableList()
            if (fromIndex in exercises.indices && toIndex in exercises.indices) {
                // Move the item in the list
                val item = exercises.removeAt(fromIndex)
                exercises.add(toIndex, item)
                
                // Update the exerciseOrder for all affected items
                exercises.forEachIndexed { index, exercise ->
                    if (exercise.exerciseOrder != index) {
                        repository.updateExerciseOrder(exercise.id, index)
                    }
                }
                
                // Update the UI state
                _selectedWorkoutExercises.value = exercises
            }
        }
    }
    
    fun reorderExercisesInstantly(fromIndex: Int, toIndex: Int) {
        if (!canEditWorkout()) return
        
        val exercises = _selectedWorkoutExercises.value.toMutableList()
        if (fromIndex in exercises.indices && toIndex in exercises.indices && fromIndex != toIndex) {
            // Log before reorder
            android.util.Log.d("DragReorder", "BEFORE reorder: ${exercises.mapIndexed { idx, ex -> "$idx:${ex.exerciseName}(order=${ex.exerciseOrder})" }.joinToString()}")
            
            // Move the item in the list
            val item = exercises.removeAt(fromIndex)
            exercises.add(toIndex, item)
            
            // Update the exerciseOrder property in each object to match its new position
            val updatedExercises = exercises.mapIndexed { index, exercise ->
                exercise.copy(exerciseOrder = index)
            }
            
            // Log after reorder
            android.util.Log.d("DragReorder", "AFTER reorder: ${updatedExercises.mapIndexed { idx, ex -> "$idx:${ex.exerciseName}(order=${ex.exerciseOrder})" }.joinToString()}")
            
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
            // Stop timers for this workout
            restTimerViewModel?.onWorkoutDeleted(currentId)
            stopWorkoutTimer()
            
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
            // Stop rest timer for this workout
            restTimerViewModel?.onWorkoutDeleted(workoutId)
            repository.deleteWorkout(workoutId)
            loadInProgressWorkouts()
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
                println("❌ Programme not found or not active")
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }
            
            // Check if a workout already exists for this programme/week/day
            val existingWorkout = _inProgressWorkouts.value.find {
                it.isProgrammeWorkout && 
                it.programmeId == programmeId &&
                it.weekNumber == weekNumber &&
                it.dayNumber == dayNumber
            }
            
            if (existingWorkout != null) {
                println("✅ Found existing workout for this programme/week/day, resuming instead")
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
                println("❌ Failed to retrieve created workout")
                _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
                return
            }

            _currentWorkoutId.value = workoutId
            _workoutState.value =
                WorkoutState(
                    isActive = true,
                    isCompleted = false,
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
                )

            // IMPORTANT: Wait for exercises to load completely before allowing navigation
            loadExercisesForWorkout(workoutId)
            loadInProgressWorkouts()
            
            // Clear any existing timer state before binding to new workout
            restTimerViewModel?.onWorkoutCompleted()
            // Bind rest timer to this workout
            restTimerViewModel?.bindToWorkout(workoutId)

            println("✅ Started programme workout: ${workout.programmeWorkoutName} (Week $weekNumber, Day $dayNumber)")

            // Notify that workout is ready for navigation
            onReady?.invoke()
        } catch (e: Exception) {
            println("❌ Error starting programme workout: ${e.message}")
            e.printStackTrace()
            _workoutState.value = _workoutState.value.copy(isLoadingExercises = false)
        }
    }

    // Get next programme workout for the active programme
    suspend fun getNextProgrammeWorkout(): NextProgrammeWorkoutInfo? {
        val activeProgramme = repository.getActiveProgramme() ?: return null
        return repository.getNextProgrammeWorkout(activeProgramme.id)
    }

    // Check if the current workout is part of a programme
    fun isProgrammeWorkout(): Boolean {
        return _workoutState.value.isProgrammeWorkout
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

    // Get workouts for the current programme
    suspend fun getProgrammeWorkouts(): List<Workout>? {
        val state = _workoutState.value
        return if (state.isProgrammeWorkout && state.programmeId != null) {
            repository.getWorkoutsByProgramme(state.programmeId)
        } else {
            null
        }
    }

    // ===== WORKOUT TIMER METHODS =====

    private fun startWorkoutTimer() {
        val currentTime = System.currentTimeMillis()
        _workoutState.value = _workoutState.value.copy(
            workoutTimerStartTime = currentTime,
            isWorkoutTimerActive = true
        )
        
        // Start the timer coroutine
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (_workoutState.value.isWorkoutTimerActive) {
                val startTime = _workoutState.value.workoutTimerStartTime ?: currentTime
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                _elapsedWorkoutTime.value = elapsed
                delay(1000) // Update every second
            }
        }
    }

    private fun stopWorkoutTimer() {
        _workoutState.value = _workoutState.value.copy(
            isWorkoutTimerActive = false
        )
        workoutTimerJob?.cancel()
        workoutTimerJob = null
    }

    // Resume timer if workout was already started (for when user navigates away and back)
    private fun resumeWorkoutTimerIfNeeded() {
        val state = _workoutState.value
        if (state.isActive && !state.isCompleted) {
            // Check if any sets are completed to determine if timer should be active
            val hasCompletedSets = _selectedExerciseSets.value.any { it.isCompleted }
            if (hasCompletedSets && !state.isWorkoutTimerActive) {
                // Calculate the start time from the first completed set
                val firstCompletedSet = _selectedExerciseSets.value
                    .filter { it.isCompleted && it.completedAt != null }
                    .minByOrNull { it.completedAt!! }
                
                if (firstCompletedSet != null) {
                    // Parse the timestamp and calculate elapsed time
                    val firstSetTime = try {
                        LocalDateTime.parse(firstCompletedSet.completedAt)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (firstSetTime != null) {
                        val startTimeMillis = firstSetTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        _workoutState.value = _workoutState.value.copy(
                            workoutTimerStartTime = startTimeMillis,
                            isWorkoutTimerActive = true
                        )
                        
                        // Start the timer coroutine with the restored start time
                        workoutTimerJob?.cancel()
                        workoutTimerJob = viewModelScope.launch {
                            while (_workoutState.value.isWorkoutTimerActive) {
                                val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000
                                _elapsedWorkoutTime.value = elapsed
                                delay(1000) // Update every second
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        
        // Don't unbind rest timer here - let it continue running
        // The timer should persist across navigation
    }
}
