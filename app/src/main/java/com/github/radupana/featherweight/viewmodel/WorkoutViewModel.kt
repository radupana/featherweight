package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        FeatherweightDatabase::class.java, "featherweight-db"
    ).build()
    private val workoutDao = db.workoutDao()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            _workouts.value = workoutDao.getAllWorkouts()
        }
    }

    fun addWorkout(exerciseName: String, sets: Int, reps: Int, weight: Float) {
        viewModelScope.launch {
            val workoutId = workoutDao.insertWorkout(
                Workout(date = LocalDateTime.now())
            )
            workoutDao.insertExerciseLog(
                ExerciseLog(
                    workoutId = workoutId,
                    name = exerciseName,
                    sets = sets,
                    reps = reps,
                    weight = weight
                )
            )
            loadWorkouts()
        }
    }
}
