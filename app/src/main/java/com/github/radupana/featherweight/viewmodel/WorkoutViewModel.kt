package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
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

    init {
        checkForOngoingWorkout()
    }

    // Check if there's an ongoing workout when the app starts
    private fun checkForOngoingWorkout() {
        viewModelScope.launch {
            val ongoingWorkout = repository.getOngoingWorkout()
            if (ongoingWorkout != null) {
                // Resume existing workout
                _currentWorkoutId.value = ongoingWorkout.id
                _workoutState.value =
                    WorkoutState(
                        isActive = true,
                        isCompleted = false,
                        workoutId = ongoingWorkout.id,
                        startTime = ongoingWorkout.date,
                        workoutName = ongoingWorkout.notes,
                    )
                loadExercisesForWorkout(ongoingWorkout.id)
            }
        }
    }

    // Start a new workout
    fun startNewWorkout() {
        viewModelScope.launch {
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
                )

            loadExercisesForWorkout(workoutId)
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
                )
        }
    }

    // Update workout name
    fun updateWorkoutName(name: String?) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            repository.updateWorkoutName(currentId, name)
            _workoutState.value = _workoutState.value.copy(workoutName = name)
        }
    }

    // Get display name for workout
    fun getWorkoutDisplayName(): String {
        val state = _workoutState.value
        return state.workoutName ?: state.startTime?.format(
            DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
        ) ?: "Current Workout"
    }

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

    // Exercise management
    fun addExerciseToCurrentWorkout(exerciseName: String) {
        val currentId = _currentWorkoutId.value ?: return
        viewModelScope.launch {
            val exerciseLog =
                ExerciseLog(
                    workoutId = currentId,
                    exerciseName = exerciseName,
                    exerciseOrder = selectedWorkoutExercises.value.size,
                    supersetGroup = null,
                    notes = null,
                )
            repository.insertExerciseLog(exerciseLog)
            loadExercisesForWorkout(currentId)
            loadExerciseHistory(exerciseName)
        }
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

    // Set management
    fun addSetToExercise(
        exerciseLogId: Long,
        weight: Float = 0f,
        reps: Int = 0,
        rpe: Float? = null,
    ) {
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
        }
    }

    fun updateSet(
        setId: Long,
        reps: Int,
        weight: Float,
        rpe: Float?,
    ) {
        viewModelScope.launch {
            val currentSets = _selectedExerciseSets.value
            val currentSet = currentSets.firstOrNull { it.id == setId }
            if (currentSet != null) {
                val updatedSet = currentSet.copy(reps = reps, weight = weight, rpe = rpe)
                repository.updateSetLog(updatedSet)
                loadAllSetsForCurrentExercises()
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            repository.deleteSetLog(setId)
            loadAllSetsForCurrentExercises()
        }
    }

    fun markSetCompleted(
        setId: Long,
        completed: Boolean,
    ) {
        val timestamp =
            if (completed) {
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } else {
                null
            }

        viewModelScope.launch {
            repository.markSetCompleted(setId, completed, timestamp)
            loadAllSetsForCurrentExercises()
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
}
