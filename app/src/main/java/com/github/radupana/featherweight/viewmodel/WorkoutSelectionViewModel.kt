package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.WorkoutRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class WorkoutWithExercises(
    val id: String,
    val name: String,
    val date: LocalDateTime,
    val exercises: List<String>,
    val totalSets: Int,
    val duration: Long?,
)

class WorkoutSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val workoutRepository = WorkoutRepository(application)
    private val repository = FeatherweightRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _workouts = MutableStateFlow<List<WorkoutWithExercises>>(emptyList())
    val workouts: StateFlow<List<WorkoutWithExercises>> = _workouts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredWorkouts =
        combine(_workouts, _searchQuery) { workouts, query ->
            if (query.isEmpty()) {
                workouts
            } else {
                workouts.filter { workout ->
                    // Search in workout name
                    workout.name.contains(query, ignoreCase = true) ||
                        // Search in exercise names
                        workout.exercises.any { exercise ->
                            exercise.contains(query, ignoreCase = true)
                        }
                }
            }
        }

    init {
        loadCompletedWorkouts()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadCompletedWorkouts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val completedWorkouts =
                    workoutRepository
                        .getWorkoutHistory()
                        .filter { it.status == WorkoutStatus.COMPLETED }
                        .sortedByDescending { it.date }

                val workoutsWithExercises =
                    completedWorkouts.map { workout ->
                        val exerciseLogs = repository.getExerciseLogsForWorkout(workout.id)
                        val exerciseNames =
                            exerciseLogs
                                .mapNotNull { exerciseLog ->
                                    repository.getExerciseById(exerciseLog.exerciseId)?.name
                                }.distinct()

                        WorkoutWithExercises(
                            id = workout.id,
                            name =
                                workout.name ?: workout.date.format(
                                    java.time.format.DateTimeFormatter
                                        .ofPattern("MMM d, yyyy 'at' HH:mm"),
                                ),
                            date = workout.date,
                            exercises = exerciseNames,
                            totalSets = workout.setCount,
                            duration = workout.duration,
                        )
                    }

                _workouts.value = workoutsWithExercises
            } catch (e: IllegalArgumentException) {
                ExceptionLogger.logException("WorkoutSelectionVM", "Failed to load workouts", e)
                _workouts.value = emptyList()
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException("WorkoutSelectionVM", "Failed to load workouts", e)
                _workouts.value = emptyList()
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logException("WorkoutSelectionVM", "Failed to load workouts", e)
                _workouts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
