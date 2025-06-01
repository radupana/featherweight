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
            ).fallbackToDestructiveMigration()
            .build()

    private val workoutDao = db.workoutDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()

    suspend fun getAllWorkouts(): List<Workout> = workoutDao.getAllWorkouts()

    suspend fun insertWorkoutWithExercisesAndSets(exercises: List<Pair<String, List<Triple<Int, Float, Float?>>>>) {
        val workout = Workout(date = LocalDateTime.now(), notes = null)
        val workoutId = workoutDao.insertWorkout(workout)

        exercises.forEachIndexed { exerciseIndex, (exerciseName, sets) ->
            val exerciseLog =
                ExerciseLog(
                    workoutId = workoutId,
                    exerciseName = exerciseName,
                    exerciseOrder = exerciseIndex,
                    supersetGroup = null,
                    notes = null,
                )
            val exerciseLogId = exerciseLogDao.insertExerciseLog(exerciseLog)

            sets.forEachIndexed { setIndex, (reps, weight, rpe) ->
                val setLog =
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = setIndex,
                        reps = reps,
                        weight = weight,
                        rpe = rpe,
                        tag = null,
                        notes = null,
                    )
                setLogDao.insertSetLog(setLog)
            }
        }
    }

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    ) {
        setLogDao.markSetCompleted(setId, completed, completedAt)
    }

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog) {
        exerciseLogDao.insertExerciseLog(exerciseLog)
    }
}

class WorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    private val _selectedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val selectedWorkoutExercises: StateFlow<List<ExerciseLog>> = _selectedWorkoutExercises

    private val _selectedExerciseSets = MutableStateFlow<List<SetLog>>(emptyList())
    val selectedExerciseSets: StateFlow<List<SetLog>> = _selectedExerciseSets

    init {
        loadWorkouts()
    }

    private fun loadWorkouts() {
        viewModelScope.launch {
            _workouts.value = repository.getAllWorkouts()
        }
    }

    private fun loadExercisesForWorkout(workoutId: Long) {
        viewModelScope.launch {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)
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

    fun addExerciseToCurrentWorkout(exerciseName: String) {
        val currentWorkoutId = workouts.value.firstOrNull()?.id ?: return
        viewModelScope.launch {
            val exerciseLog =
                ExerciseLog(
                    workoutId = currentWorkoutId,
                    exerciseName = exerciseName,
                    exerciseOrder = selectedWorkoutExercises.value.size,
                    supersetGroup = null,
                    notes = null,
                )
            repository.insertExerciseLog(exerciseLog)
            loadExercisesForWorkout(currentWorkoutId)
        }
    }
}
