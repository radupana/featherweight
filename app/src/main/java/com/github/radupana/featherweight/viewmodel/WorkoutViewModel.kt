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

// A simple repository class for decoupling, can be moved to its own file later
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

    suspend fun insertWorkoutWithExercisesAndSets(
        exercises: List<Pair<String, List<Triple<Int, Float, Float?>>>>, // List of (exerciseName, [ (reps, weight, rpe) ])
    ) {
        val workoutId =
            workoutDao.insertWorkout(
                Workout(date = LocalDateTime.now()),
            )
        exercises.forEachIndexed { idx, (exerciseName, setsList) ->
            val exerciseLogId =
                exerciseLogDao.insertExerciseLog(
                    ExerciseLog(
                        workoutId = workoutId,
                        exerciseName = exerciseName,
                        exerciseOrder = idx,
                        supersetGroup = null,
                    ),
                )
            setsList.forEachIndexed { setIdx, (reps, weight, rpe) ->
                setLogDao.insertSetLog(
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = setIdx,
                        reps = reps,
                        weight = weight,
                        rpe = rpe,
                    ),
                )
            }
        }
    }

    suspend fun getAllWorkouts(): List<Workout> = workoutDao.getAllWorkouts()

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)
}

class WorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    // For detail display
    private val _selectedWorkoutExercises = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val selectedWorkoutExercises: StateFlow<List<ExerciseLog>> = _selectedWorkoutExercises

    private val _selectedExerciseSets = MutableStateFlow<List<SetLog>>(emptyList())
    val selectedExerciseSets: StateFlow<List<SetLog>> = _selectedExerciseSets

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            _workouts.value = repository.getAllWorkouts()
        }
    }

    fun addWorkout(exercises: List<Pair<String, List<Triple<Int, Float, Float?>>>>) {
        viewModelScope.launch {
            repository.insertWorkoutWithExercisesAndSets(exercises)
            loadWorkouts()
        }
    }

    fun loadExercisesForWorkout(workoutId: Long) {
        viewModelScope.launch {
            _selectedWorkoutExercises.value = repository.getExercisesForWorkout(workoutId)
        }
    }

    fun loadSetsForExercise(exerciseLogId: Long) {
        viewModelScope.launch {
            _selectedExerciseSets.value = repository.getSetsForExercise(exerciseLogId)
        }
    }
}
