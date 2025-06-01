package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.radupana.featherweight.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FeatherweightRepository(
    application: Application,
) {
    private val db =
        Room
            .databaseBuilder(
                application,
                FeatherweightDatabase::class.java,
                "featherweight-db",
            ).fallbackToDestructiveMigration(false)
            .build()

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()

    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    ) = setLogDao.markSetCompleted(setId, completed, completedAt)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)

    suspend fun insertSetLog(setLog: SetLog): Long = setLogDao.insertSetLog(setLog)

    suspend fun deleteSetLog(setId: Long) = setLogDao.deleteSetLog(setId)
}

class WorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _currentWorkoutId = MutableStateFlow<Long?>(null)
    val currentWorkoutId: StateFlow<Long?> = _currentWorkoutId

    private val _selectedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val selectedWorkoutExercises: StateFlow<List<ExerciseLog>> = _selectedWorkoutExercises

    private val _selectedExerciseSets = MutableStateFlow<List<SetLog>>(emptyList())
    val selectedExerciseSets: StateFlow<List<SetLog>> = _selectedExerciseSets

    init {
        startNewWorkout()
    }

    private fun startNewWorkout() {
        viewModelScope.launch {
            val workout = Workout(date = LocalDateTime.now(), notes = null)
            val workoutId = repository.insertWorkout(workout)
            _currentWorkoutId.value = workoutId
            loadExercisesForWorkout(workoutId)
        }
    }

    private fun loadExercisesForWorkout(workoutId: Long) {
        viewModelScope.launch {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)
            // When you load exercises, clear sets (until an exercise is selected)
            _selectedExerciseSets.value = emptyList()
        }
    }

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
        }
    }

    fun addSetToExercise(exerciseLogId: Long) {
        viewModelScope.launch {
            val setOrder = repository.getSetsForExercise(exerciseLogId).size
            val setLog =
                SetLog(
                    exerciseLogId = exerciseLogId,
                    setOrder = setOrder,
                    reps = 0,
                    weight = 0.0f,
                    rpe = null,
                    tag = null,
                    notes = null,
                    isCompleted = false,
                    completedAt = null,
                )
            repository.insertSetLog(setLog)
            loadSetsForExercise(exerciseLogId)
        }
    }

    fun loadSetsForExercise(exerciseLogId: Long) {
        viewModelScope.launch {
            _selectedExerciseSets.value = repository.getSetsForExercise(exerciseLogId)
        }
    }

    fun markSetCompleted(
        setId: Long,
        completed: Boolean,
    ) {
        val timestamp = if (completed) LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) else null
        viewModelScope.launch {
            repository.markSetCompleted(setId, completed, timestamp)
            val currentSets = _selectedExerciseSets.value
            val exerciseId = currentSets.firstOrNull { it.id == setId }?.exerciseLogId
            if (exerciseId != null) {
                _selectedExerciseSets.value = repository.getSetsForExercise(exerciseId)
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            val currentSets = _selectedExerciseSets.value
            val set = currentSets.firstOrNull { it.id == setId }
            if (set != null) {
                repository.deleteSetLog(setId)
                _selectedExerciseSets.value = repository.getSetsForExercise(set.exerciseLogId)
            }
        }
    }
}
