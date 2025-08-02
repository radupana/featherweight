package com.github.radupana.featherweight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

class WorkoutCompletionViewModel(
    private val repository: FeatherweightRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutCompletionUiState())
    val uiState: StateFlow<WorkoutCompletionUiState> = _uiState.asStateFlow()

    fun loadWorkoutSummary(workoutId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val workout = repository.getWorkoutById(workoutId)
                if (workout == null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            error = "Workout not found",
                        )
                    return@launch
                }

                val exercises = repository.getExercisesForWorkout(workoutId)
                val sets = repository.getSetsForWorkout(workoutId)
                val personalRecords = repository.getPersonalRecordsForWorkout(workoutId)

                val summary = calculateSummary(workout, exercises, sets, personalRecords)

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        workoutSummary = summary,
                    )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load workout summary",
                    )
            }
        }
    }

    private suspend fun calculateSummary(
        workout: com.github.radupana.featherweight.data.Workout,
        exercises: List<ExerciseLog>,
        sets: List<SetLog>,
        personalRecords: List<PersonalRecord>,
    ): CompletionSummary {
        val completedSets = sets.filter { it.isCompleted }

        // Calculate total volume
        val totalVolume =
            completedSets
                .sumOf {
                    (it.actualWeight * it.actualReps).toDouble()
                }.toFloat()

        // Calculate duration
        val duration =
            if (workout.durationSeconds != null) {
                Duration.ofSeconds(workout.durationSeconds)
            } else {
                Duration.ofMinutes(0)
            }

        // Calculate total reps
        val totalReps = completedSets.sumOf { it.actualReps }

        // Calculate average RPE (if available)
        val rpeValues = completedSets.mapNotNull { it.actualRpe }
        val averageRpe =
            if (rpeValues.isNotEmpty()) {
                rpeValues.average().toFloat()
            } else {
                null
            }

        // Find heaviest set (by total weight)
        val heaviestSet =
            completedSets.maxByOrNull { it.actualWeight * it.actualReps }?.let { set ->
                val exercise = exercises.find { it.id == set.exerciseLogId }
                SetInfo(
                    weight = set.actualWeight,
                    reps = set.actualReps,
                    exerciseName = exercise?.exerciseName ?: "",
                )
            }

        // Calculate volume by exercise
        val volumeByExercise =
            completedSets
                .groupBy { set ->
                    exercises.find { it.id == set.exerciseLogId }?.exerciseName ?: ""
                }.mapValues { (_, sets) ->
                    sets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()
                }

        val volumeLeader =
            volumeByExercise.maxByOrNull { it.value }?.let {
                ExerciseVolume(
                    exerciseName = it.key,
                    totalVolume = it.value,
                    setCount =
                        completedSets.count { set ->
                            exercises.find { ex -> ex.id == set.exerciseLogId }?.exerciseName == it.key
                        },
                )
            }

        // Calculate average intensity if 1RMs are available
        val averageIntensity = calculateAverageIntensity(exercises, completedSets)

        return CompletionSummary(
            totalVolume = totalVolume,
            duration = duration,
            setsCompleted = completedSets.size,
            totalSets = sets.size,
            totalReps = totalReps,
            exerciseCount = exercises.size,
            personalRecords = personalRecords,
            averageRpe = averageRpe,
            heaviestSet = heaviestSet,
            volumeLeader = volumeLeader,
            averageIntensity = averageIntensity,
        )
    }

    private suspend fun calculateAverageIntensity(
        exercises: List<ExerciseLog>,
        completedSets: List<SetLog>,
    ): Float? {
        val intensities = mutableListOf<Float>()

        for (set in completedSets) {
            val exercise = exercises.find { it.id == set.exerciseLogId } ?: continue
            val exerciseEntity = repository.getExerciseByName(exercise.exerciseName) ?: continue
            val oneRM = repository.getCurrentOneRMEstimate(1L, exerciseEntity.id) ?: continue

            if (oneRM > 0) {
                val intensity = (set.actualWeight / oneRM) * 100
                intensities.add(intensity)
            }
        }

        return if (intensities.isNotEmpty()) {
            intensities.average().toFloat()
        } else {
            null
        }
    }
}

data class WorkoutCompletionUiState(
    val isLoading: Boolean = false,
    val workoutSummary: CompletionSummary? = null,
    val error: String? = null,
)

data class CompletionSummary(
    val totalVolume: Float,
    val duration: Duration,
    val setsCompleted: Int,
    val totalSets: Int,
    val totalReps: Int,
    val exerciseCount: Int,
    val personalRecords: List<PersonalRecord>,
    val averageRpe: Float?,
    val heaviestSet: SetInfo?,
    val volumeLeader: ExerciseVolume?,
    val averageIntensity: Float?,
)

data class SetInfo(
    val weight: Float,
    val reps: Int,
    val exerciseName: String,
)

data class ExerciseVolume(
    val exerciseName: String,
    val totalVolume: Float,
    val setCount: Int,
)
