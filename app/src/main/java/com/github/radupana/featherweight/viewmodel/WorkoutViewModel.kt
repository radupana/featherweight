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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    val originalWorkoutData: Triple<List<ExerciseLog>, List<SetLog>, String?>? = null, // Backup for rollback
)

data class InProgressWorkout(
    val id: Long,
    val name: String?,
    val startDate: LocalDateTime,
    val exerciseCount: Int,
    val setCount: Int,
    val completedSets: Int,
)

class WorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

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

    private val _exerciseHistory = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    val exerciseHistory: StateFlow<Map<String, ExerciseHistory>> = _exerciseHistory

    // In-progress workouts for home screen
    private val _inProgressWorkouts = MutableStateFlow<List<InProgressWorkout>>(emptyList())
    val inProgressWorkouts: StateFlow<List<InProgressWorkout>> = _inProgressWorkouts

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

    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseWithDetails> =
        repository.getExercisesByMuscleGroup(muscleGroup)

    suspend fun getFilteredExercises(
        category: ExerciseCategory? = null,
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null,
        availableEquipment: List<Equipment> = emptyList(),
        maxDifficulty: ExerciseDifficulty? = null,
        searchQuery: String = "",
    ): List<ExerciseWithDetails> =
        repository.getFilteredExercises(
            category,
            muscleGroup,
            equipment,
            availableEquipment,
            maxDifficulty,
            true,
            searchQuery,
        )

    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory = ExerciseCategory.FULL_BODY,
        primaryMuscles: Set<MuscleGroup> = emptySet(),
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
    ): ExerciseWithDetails? =
        try {
            val exerciseId =
                repository.createCustomExercise(
                    name = name,
                    category = category,
                    primaryMuscles = primaryMuscles,
                    requiredEquipment = equipment,
                    userId = "current_user", // TODO: Get actual user ID
                )

            // Reload exercises to include the new one
            loadExercises()

            // Return the created exercise
            repository.getExerciseById(exerciseId)
        } catch (e: Exception) {
            println("Error creating custom exercise: ${e.message}")
            null
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
                        InProgressWorkout(
                            id = summary.id,
                            name = summary.name,
                            startDate = summary.date,
                            exerciseCount = summary.exerciseCount,
                            setCount = summary.setCount,
                            completedSets = 0, // TODO: Calculate from sets
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
                    )

                loadExercisesForWorkout(workoutId)
                loadInProgressWorkouts()
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

    // Complete the current workout
    fun completeWorkout() {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.completeWorkout(currentId)
            _workoutState.value =
                _workoutState.value.copy(
                    isActive = false,
                    isCompleted = true,
                    isReadOnly = true,
                    isInEditMode = false,
                    originalWorkoutData = null,
                )
            loadInProgressWorkouts()
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

    // Check if we can mark a set as complete (validation)
    fun canMarkSetComplete(set: SetLog): Boolean = set.reps > 0 && set.weight > 0

    private fun loadExercisesForWorkout(workoutId: Long) {
        viewModelScope.launch {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)
            loadAllSetsForCurrentExercises()
        }
    }

    private fun loadAllSetsForCurrentExercises() {
        viewModelScope.launch {
            val allSets = mutableListOf<SetLog>()
            _selectedWorkoutExercises.value.forEach { exercise ->
                val sets = repository.getSetsForExercise(exercise.id)
                allSets.addAll(sets)
            }
            _selectedExerciseSets.value = allSets
        }
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
        currentMuscleGroups: Set<MuscleGroup> = emptySet(),
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
                        repository
                            .getFilteredExercises(
                                availableEquipment = availableEquipment,
                                maxDifficulty = ExerciseDifficulty.ADVANCED,
                            ).filter { exercise ->
                                exercise.primaryMovements.any {
                                    it in MovementPattern.getCompoundMovements()
                                }
                            }.take(10),
                    )
                } else {
                    // Suggest complementary muscle groups
                    val complementaryMuscles = getComplementaryMuscleGroups(currentMuscleGroups)
                    complementaryMuscles.forEach { muscleGroup ->
                        suggestions.addAll(
                            repository.getExercisesByMuscleGroup(muscleGroup).take(3),
                        )
                    }
                }

                suggestions.distinctBy { it.exercise.id }.take(15)
            }
        } catch (e: Exception) {
            println("Error getting exercise suggestions: ${e.message}")
            emptyList()
        }

    private fun getComplementaryMuscleGroups(currentMuscles: Set<MuscleGroup>): List<MuscleGroup> {
        val complementary = mutableListOf<MuscleGroup>()

        // Basic push/pull balance
        val hasPush = currentMuscles.any { it in setOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS) }
        val hasPull = currentMuscles.any { it in setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.BICEPS) }

        if (hasPush && !hasPull) {
            complementary.addAll(listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.BICEPS))
        } else if (hasPull && !hasPush) {
            complementary.addAll(listOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS))
        }

        // Upper/lower balance
        val hasUpper = currentMuscles.any { it in setOf(MuscleGroup.CHEST, MuscleGroup.UPPER_BACK, MuscleGroup.FRONT_DELTS) }
        val hasLower = currentMuscles.any { it in setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES) }

        if (hasUpper && !hasLower) {
            complementary.addAll(listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES))
        } else if (hasLower && !hasUpper) {
            complementary.addAll(listOf(MuscleGroup.CHEST, MuscleGroup.UPPER_BACK, MuscleGroup.FRONT_DELTS))
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
                if (canMarkSetComplete(set) && !set.isCompleted) {
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
}
