package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.components.ExerciseDataPoint
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExerciseProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val userPreferences = UserPreferences(application)

    sealed class ExerciseProgressState {
        object Loading : ExerciseProgressState()

        data class Success(val data: ExerciseProgressData?) : ExerciseProgressState()

        data class Error(val message: String) : ExerciseProgressState()
    }

    data class ExerciseProgressData(
        val exerciseName: String,
        val allTimePR: Float,
        val allTimePRDate: LocalDate?,
        val recentBest: Float,
        val recentBestDate: LocalDate? = null,
        val recentBestPercentOfPR: Int,
        val weeklyFrequency: Float,
        val frequencyTrend: FrequencyTrend,
        val lastPerformed: LocalDate?,
        val progressStatus: ProgressStatus,
        val progressStatusDetail: String,
        val plateauWeeks: Int = 0,
    )

    enum class FrequencyTrend {
        UP,
        DOWN,
        STABLE,
    }

    enum class ProgressStatus {
        MAKING_GAINS,
        STEADY_PROGRESS,
        PLATEAU,
        EXTENDED_BREAK,
        WORKING_LIGHTER,
    }

    private val _state = MutableStateFlow<ExerciseProgressState>(ExerciseProgressState.Loading)
    val state: StateFlow<ExerciseProgressState> = _state.asStateFlow()

    private val _chartData = MutableStateFlow<List<ExerciseDataPoint>>(emptyList())
    val chartData: StateFlow<List<ExerciseDataPoint>> = _chartData.asStateFlow()

    fun loadExerciseData(exerciseName: String) {
        viewModelScope.launch {
            _state.value = ExerciseProgressState.Loading

            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) {
                    _state.value = ExerciseProgressState.Error("No user selected")
                    return@launch
                }

                // Fetch global exercise progress
                val globalProgress = repository.getGlobalExerciseProgress(userId, exerciseName)

                if (globalProgress == null) {
                    _state.value = ExerciseProgressState.Success(null)
                    return@launch
                }

                // Get all-time PR (NO FALLBACK TO ESTIMATED MAX!)
                val prRecord = repository.getPersonalRecordForExercise(userId, exerciseName)
                val allTimePR = prRecord?.weight ?: 0f
                val allTimePRDate = prRecord?.recordDate?.toLocalDate()

                // DEBUG: Log PR record details
                android.util.Log.d("ExerciseProgress", "🔍 PR RECORD DEBUG for $exerciseName:")
                android.util.Log.d("ExerciseProgress", "  - PR Weight: $allTimePR")
                android.util.Log.d("ExerciseProgress", "  - PR Date from DB: ${prRecord?.recordDate}")
                android.util.Log.d("ExerciseProgress", "  - PR Date converted: $allTimePRDate")

                // Calculate Recent Best (30 days, extend to 60 if needed)
                val now = LocalDate.now()
                val thirtyDaysAgo = now.minusDays(30)
                var recentBestDate: LocalDate? = null
                val recentBest =
                    repository.getMaxWeightForExerciseInDateRange(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = thirtyDaysAgo,
                        endDate = now,
                    )?.also {
                        // Get the date when this recent best was achieved
                        recentBestDate =
                            repository.getDateOfMaxWeightForExercise(
                                userId = userId,
                                exerciseName = exerciseName,
                                weight = it,
                                startDate = thirtyDaysAgo,
                                endDate = now,
                            )
                        android.util.Log.d("ExerciseProgress", "🔍 RECENT BEST DEBUG (30 days) for $exerciseName:")
                        android.util.Log.d("ExerciseProgress", "  - Recent Best Weight: $it")
                        android.util.Log.d("ExerciseProgress", "  - Recent Best Date: $recentBestDate")
                    } ?: repository.getMaxWeightForExerciseInDateRange(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = now.minusDays(60),
                        endDate = now,
                    )?.also {
                        recentBestDate =
                            repository.getDateOfMaxWeightForExercise(
                                userId = userId,
                                exerciseName = exerciseName,
                                weight = it,
                                startDate = now.minusDays(60),
                                endDate = now,
                            )
                        android.util.Log.d("ExerciseProgress", "🔍 RECENT BEST DEBUG (60 days) for $exerciseName:")
                        android.util.Log.d("ExerciseProgress", "  - Recent Best Weight: $it")
                        android.util.Log.d("ExerciseProgress", "  - Recent Best Date: $recentBestDate")
                    } ?: 0f

                val recentBestPercentOfPR =
                    if (allTimePR > 0) {
                        ((recentBest / allTimePR) * 100).toInt()
                    } else {
                        0
                    }

                // Calculate frequency (8-week window)
                val eightWeeksAgo = now.minusWeeks(8)
                val fourWeeksAgo = now.minusWeeks(4)

                val sessionCountLast8Weeks =
                    repository.getDistinctWorkoutDatesForExercise(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = eightWeeksAgo,
                        endDate = now,
                    ).size

                val sessionCountLast4Weeks =
                    repository.getDistinctWorkoutDatesForExercise(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = fourWeeksAgo,
                        endDate = now,
                    ).size

                val sessionCountPrevious4Weeks =
                    repository.getDistinctWorkoutDatesForExercise(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = eightWeeksAgo,
                        endDate = fourWeeksAgo,
                    ).size

                val weeklyFrequency = sessionCountLast8Weeks / 8.0f

                // Determine frequency trend
                val frequencyTrend =
                    when {
                        sessionCountLast4Weeks > sessionCountPrevious4Weeks * 1.2 -> FrequencyTrend.UP
                        sessionCountLast4Weeks < sessionCountPrevious4Weeks * 0.8 -> FrequencyTrend.DOWN
                        else -> FrequencyTrend.STABLE
                    }

                // Determine progress status
                val daysSinceLastWorkout =
                    if (globalProgress.lastUpdated != null) {
                        ChronoUnit.DAYS.between(globalProgress.lastUpdated.toLocalDate(), now)
                    } else {
                        Long.MAX_VALUE
                    }

                val (progressStatus, progressStatusDetail, plateauWeeks) =
                    when {
                        daysSinceLastWorkout >= 14 -> {
                            Triple(ProgressStatus.EXTENDED_BREAK, "Last session $daysSinceLastWorkout days ago", 0)
                        }
                        recentBest < globalProgress.estimatedMax * 0.9 -> {
                            Triple(
                                ProgressStatus.WORKING_LIGHTER,
                                "Working: ${WeightFormatter.formatWeightWithUnit(
                                    recentBest,
                                )} | Est. Max: ${WeightFormatter.formatWeightWithUnit(globalProgress.estimatedMax)}",
                                0,
                            )
                        }
                        globalProgress.trend == ProgressTrend.STALLING -> {
                            val weeks = globalProgress.weeksAtCurrentWeight
                            Triple(
                                ProgressStatus.PLATEAU,
                                "$weeks weeks at ${WeightFormatter.formatWeightWithUnit(globalProgress.currentWorkingWeight)}",
                                weeks,
                            )
                        }
                        globalProgress.trend == ProgressTrend.IMPROVING -> {
                            val lastProgressDate = globalProgress.lastProgressionDate?.toLocalDate() ?: globalProgress.lastUpdated.toLocalDate()
                            val daysSinceProgress = ChronoUnit.DAYS.between(lastProgressDate, now)
                            val progressAmount = recentBest - (globalProgress.lastPrWeight ?: recentBest)
                            if (progressAmount > 0 && daysSinceProgress <= 30) {
                                Triple(ProgressStatus.MAKING_GAINS, "+${WeightFormatter.formatWeight(progressAmount)}kg this month", 0)
                            } else {
                                Triple(ProgressStatus.MAKING_GAINS, "Weight increasing", 0)
                            }
                        }
                        else -> {
                            Triple(ProgressStatus.STEADY_PROGRESS, "Consistent training", 0)
                        }
                    }

                val data =
                    ExerciseProgressData(
                        exerciseName = exerciseName,
                        allTimePR = allTimePR,
                        allTimePRDate = allTimePRDate,
                        recentBest = recentBest,
                        recentBestDate = recentBestDate ?: globalProgress.lastUpdated.toLocalDate(),
                        recentBestPercentOfPR = recentBestPercentOfPR,
                        weeklyFrequency = weeklyFrequency,
                        frequencyTrend = frequencyTrend,
                        lastPerformed = globalProgress.lastUpdated.toLocalDate(),
                        progressStatus = progressStatus,
                        progressStatusDetail = progressStatusDetail,
                        plateauWeeks = plateauWeeks,
                    )

                _state.value = ExerciseProgressState.Success(data)
            } catch (e: Exception) {
                _state.value = ExerciseProgressState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadChartData(exerciseName: String) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch

                // Get all workout data for this exercise
                val allWorkouts =
                    repository.getExerciseWorkoutsInDateRange(
                        userId = userId,
                        exerciseName = exerciseName,
                        startDate = LocalDate.now().minusYears(2), // Get 2 years of data
                        endDate = LocalDate.now(),
                    )

                // Group by date and get the best performance per day
                val chartPoints =
                    allWorkouts
                        .groupBy { it.workoutDate.toLocalDate() }
                        .map { (date, logs) ->
                            // Get the best actual weight lifted that day
                            val bestLog = logs.maxByOrNull { it.actualWeight }!!

                            ExerciseDataPoint(
                                date = date,
                                weight = bestLog.actualWeight, // Show actual weight, not estimated
                                reps = bestLog.actualReps,
                                isPR = false, // Will be calculated separately
                            )
                        }
                        .sortedBy { it.date }

                // Mark PRs
                val dataWithPRs =
                    if (chartPoints.isNotEmpty()) {
                        var currentMax = 0f
                        chartPoints.map { point ->
                            if (point.weight > currentMax) {
                                currentMax = point.weight
                                point.copy(isPR = true)
                            } else {
                                point
                            }
                        }
                    } else {
                        emptyList()
                    }

                _chartData.value = dataWithPRs
            } catch (e: Exception) {
                // Handle error silently for chart data
                _chartData.value = emptyList()
            }
        }
    }
}
