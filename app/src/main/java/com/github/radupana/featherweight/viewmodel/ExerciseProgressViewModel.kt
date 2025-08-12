package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.components.ExerciseDataPoint
import com.github.radupana.featherweight.ui.components.FrequencyDataPoint
import com.github.radupana.featherweight.ui.components.IntensityZoneData
import com.github.radupana.featherweight.ui.components.RepRangeDistribution
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExerciseProgressViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val repository = FeatherweightRepository(application)
    private val userPreferences = UserPreferences(application)

    sealed class ExerciseProgressState {
        object Loading : ExerciseProgressState()

        data class Success(
            val data: ExerciseProgressData?,
        ) : ExerciseProgressState()

        data class Error(
            val message: String,
        ) : ExerciseProgressState()
    }

    data class ExerciseProgressData(
        val exerciseVariationId: Long,
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

    private val _maxWeightChartData = MutableStateFlow<List<ExerciseDataPoint>>(emptyList())
    val maxWeightChartData: StateFlow<List<ExerciseDataPoint>> = _maxWeightChartData.asStateFlow()

    private val _volumeChartData = MutableStateFlow<List<ExerciseDataPoint>>(emptyList())
    val volumeChartData: StateFlow<List<ExerciseDataPoint>> = _volumeChartData.asStateFlow()

    private val _selectedChartType = MutableStateFlow(ChartType.ONE_RM)
    val selectedChartType: StateFlow<ChartType> = _selectedChartType.asStateFlow()

    private val _frequencyData = MutableStateFlow<List<FrequencyDataPoint>>(emptyList())
    val frequencyData: StateFlow<List<FrequencyDataPoint>> = _frequencyData.asStateFlow()

    private val _repRangeData = MutableStateFlow<List<RepRangeDistribution>>(emptyList())
    val repRangeData: StateFlow<List<RepRangeDistribution>> = _repRangeData.asStateFlow()

    private val _rpeZoneData = MutableStateFlow<List<IntensityZoneData>>(emptyList())
    val rpeZoneData: StateFlow<List<IntensityZoneData>> = _rpeZoneData.asStateFlow()

    private val _selectedPatternType = MutableStateFlow(PatternType.FREQUENCY)
    val selectedPatternType: StateFlow<PatternType> = _selectedPatternType.asStateFlow()

    enum class ChartType {
        ONE_RM,
        MAX_WEIGHT,
    }

    enum class PatternType {
        FREQUENCY,
        REP_RANGES,
        RPE_ZONES,
    }

    fun loadExerciseData(exerciseVariationId: Long) {
        viewModelScope.launch {
            _state.value = ExerciseProgressState.Loading

            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) {
                    _state.value = ExerciseProgressState.Error("No user selected")
                    return@launch
                }

                // Get exercise name for display
                val exercise = repository.getExerciseById(exerciseVariationId)
                val exerciseName = exercise?.name ?: "Unknown Exercise"

                // Fetch global exercise progress
                val globalProgress = repository.getGlobalExerciseProgress(userId, exerciseVariationId)

                if (globalProgress == null) {
                    _state.value = ExerciseProgressState.Success(null)
                    return@launch
                }

                // Get all-time PR (NO FALLBACK TO ESTIMATED MAX!)
                val prRecord = repository.getPersonalRecordForExercise(exerciseVariationId)
                val allTimePR = prRecord?.weight ?: 0f
                val allTimePRDate = prRecord?.recordDate?.toLocalDate()

                // Calculate Recent Best (30 days, extend to 60 if needed)
                val now = LocalDate.now()
                val thirtyDaysAgo = now.minusDays(30)
                var recentBestDate: LocalDate? = null
                val recentBest =
                    repository
                        .getMaxWeightForExerciseInDateRange(
                            exerciseVariationId = exerciseVariationId,
                            startDate = thirtyDaysAgo,
                            endDate = now,
                        )?.also {
                            // Get the date when this recent best was achieved
                            recentBestDate =
                                repository.getDateOfMaxWeightForExercise(
                                    exerciseVariationId = exerciseVariationId,
                                    weight = it,
                                    startDate = thirtyDaysAgo,
                                    endDate = now,
                                )
                        } ?: repository
                        .getMaxWeightForExerciseInDateRange(
                            exerciseVariationId = exerciseVariationId,
                            startDate = now.minusDays(60),
                            endDate = now,
                        )?.also {
                            recentBestDate =
                                repository.getDateOfMaxWeightForExercise(
                                    exerciseVariationId = exerciseVariationId,
                                    weight = it,
                                    startDate = now.minusDays(60),
                                    endDate = now,
                                )
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
                    repository
                        .getDistinctWorkoutDatesForExercise(
                            exerciseVariationId = exerciseVariationId,
                            startDate = eightWeeksAgo,
                            endDate = now,
                        ).size

                val sessionCountLast4Weeks =
                    repository
                        .getDistinctWorkoutDatesForExercise(
                            exerciseVariationId = exerciseVariationId,
                            startDate = fourWeeksAgo,
                            endDate = now,
                        ).size

                val sessionCountPrevious4Weeks =
                    repository
                        .getDistinctWorkoutDatesForExercise(
                            exerciseVariationId = exerciseVariationId,
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
                                "Working: ${
                                    WeightFormatter.formatWeightWithUnit(
                                        recentBest,
                                    )
                                } | Est. Max: ${WeightFormatter.formatWeightWithUnit(globalProgress.estimatedMax)}",
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
                            val lastProgressDate =
                                globalProgress.lastProgressionDate?.toLocalDate() ?: globalProgress.lastUpdated.toLocalDate()
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
                        exerciseVariationId = exerciseVariationId,
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

    fun loadChartData(exerciseVariationId: Long) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch

                // Get exercise name for display
                val exercise = repository.getExerciseById(exerciseVariationId)
                val exerciseName = exercise?.name ?: "Unknown Exercise"

                // Get 1RM history data for this exercise
                val startDate = LocalDate.now().minusYears(2).atStartOfDay()
                val endDate = LocalDate.now().atTime(23, 59, 59)

                val oneRMHistory =
                    repository.getOneRMHistoryForExercise(
                        exerciseName = exerciseName,
                        startDate = startDate,
                        endDate = endDate,
                    )

                // Group by date and get the best 1RM per day
                val chartPoints =
                    oneRMHistory
                        .groupBy { it.recordedAt.toLocalDate() }
                        .map { (date, records) ->
                            // Get the highest 1RM estimate for that day
                            val best1RM = records.maxByOrNull { it.oneRMEstimate }!!

                            ExerciseDataPoint(
                                date = date,
                                weight = best1RM.oneRMEstimate,
                                reps = 1, // 1RM always represents 1 rep
                                isPR = false, // Will be calculated separately
                                context = best1RM.context, // Include the context (e.g., "90kg × 3 @ RPE 8")
                            )
                        }.sortedBy { it.date }

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
                android.util.Log.e("ExerciseProgress", "Error loading 1RM history", e)
                _chartData.value = emptyList()
            }
        }
    }

    fun loadMaxWeightChartData(exerciseVariationId: Long) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch

                // Get workout data for this exercise in the last 2 years
                val startDate = LocalDate.now().minusYears(2)
                val endDate = LocalDate.now()

                val workouts =
                    repository.getExerciseWorkoutsInDateRange(
                        exerciseVariationId = exerciseVariationId,
                        startDate = startDate,
                        endDate = endDate,
                    )

                // Group by date and get the maximum weight lifted per session
                val chartPoints =
                    workouts
                        .filter { it.actualWeight > 0f }
                        .groupBy { it.workoutDate.toLocalDate() }
                        .map { (date, workoutsOnDate) ->
                            // Get the maximum weight lifted on this date
                            val maxWeightWorkout = workoutsOnDate.maxByOrNull { it.actualWeight }!!

                            ExerciseDataPoint(
                                date = date,
                                weight = maxWeightWorkout.actualWeight,
                                reps = maxWeightWorkout.actualReps,
                                isPR = false, // Will be calculated separately
                                context = "${maxWeightWorkout.actualWeight}kg × ${maxWeightWorkout.actualReps}",
                            )
                        }.sortedBy { it.date }

                // Mark PRs (new max weights)
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

                _maxWeightChartData.value = dataWithPRs
            } catch (e: Exception) {
                android.util.Log.e("ExerciseProgress", "Error loading max weight history", e)
                _maxWeightChartData.value = emptyList()
            }
        }
    }

    fun setChartType(chartType: ChartType) {
        _selectedChartType.value = chartType
    }

    fun loadVolumeChartData(exerciseVariationId: Long) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch

                // Get workout data for this exercise in the last 12 weeks
                val startDate = LocalDate.now().minusWeeks(12)
                val endDate = LocalDate.now()

                val workouts =
                    repository.getExerciseWorkoutsInDateRange(
                        exerciseVariationId = exerciseVariationId,
                        startDate = startDate,
                        endDate = endDate,
                    )

                // Group by week and sum the total volume
                val chartPoints =
                    workouts
                        .filter { it.totalVolume > 0f }
                        .groupBy { workout ->
                            // Get the start of the week for this workout
                            val workoutDate = workout.workoutDate.toLocalDate()
                            workoutDate.minusDays(workoutDate.dayOfWeek.value.toLong() - 1)
                        }.map { (weekStart, workoutsInWeek) ->
                            val totalWeeklyVolume = workoutsInWeek.sumOf { it.totalVolume.toDouble() }.toFloat()
                            ExerciseDataPoint(
                                date = weekStart,
                                weight = totalWeeklyVolume,
                                reps = workoutsInWeek.size, // Number of sessions that week
                                isPR = false,
                                context = "${workoutsInWeek.size} session${if (workoutsInWeek.size != 1) "s" else ""}",
                            )
                        }.sortedBy { it.date }

                _volumeChartData.value = chartPoints
            } catch (e: Exception) {
                android.util.Log.e("ExerciseProgress", "Error loading volume data", e)
                _volumeChartData.value = emptyList()
            }
        }
    }

    fun setPatternType(patternType: PatternType) {
        _selectedPatternType.value = patternType
    }

    fun loadTrainingPatternsData(exerciseVariationId: Long) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getCurrentUserId()
                if (userId == -1L) return@launch

                loadFrequencyData(exerciseVariationId)
                loadRepRangeData(exerciseVariationId)
                loadRPEZoneData(exerciseVariationId)
            } catch (e: Exception) {
                android.util.Log.e("ExerciseProgress", "Error loading training patterns data", e)
            }
        }
    }

    private suspend fun loadFrequencyData(exerciseVariationId: Long) {
        try {
            // Get workout data for this exercise in the last 12 weeks
            val startDate = LocalDate.now().minusWeeks(12)
            val endDate = LocalDate.now()

            val workouts =
                repository.getExerciseWorkoutsInDateRange(
                    exerciseVariationId = exerciseVariationId,
                    startDate = startDate,
                    endDate = endDate,
                )

            // Group by date and sum the volume for each day
            val frequencyPoints =
                workouts
                    .filter { it.totalVolume > 0f }
                    .groupBy { it.workoutDate.toLocalDate() }
                    .map { (date, workoutsOnDate) ->
                        FrequencyDataPoint(
                            date = date,
                            volume = workoutsOnDate.sumOf { it.totalVolume.toDouble() }.toFloat(),
                            sessions = workoutsOnDate.size,
                        )
                    }.sortedBy { it.date }

            _frequencyData.value = frequencyPoints
        } catch (e: Exception) {
            android.util.Log.e("ExerciseProgress", "Error loading frequency data", e)
            _frequencyData.value = emptyList()
        }
    }

    private suspend fun loadRepRangeData(exerciseVariationId: Long) {
        try {
            // Get workout data for this exercise in the last 12 weeks
            val startDate = LocalDate.now().minusWeeks(12)
            val endDate = LocalDate.now()

            val workouts =
                repository.getExerciseWorkoutsInDateRange(
                    exerciseVariationId = exerciseVariationId,
                    startDate = startDate,
                    endDate = endDate,
                )

            // Define rep ranges
            val repRanges =
                listOf(
                    "1-3" to (1..3),
                    "4-6" to (4..6),
                    "7-10" to (7..10),
                    "11-15" to (11..15),
                    "16+" to (16..Int.MAX_VALUE),
                )

            val repRangeData =
                repRanges.mapNotNull { (rangeLabel, range) ->
                    val workoutsInRange = workouts.filter { it.actualReps in range }
                    if (workoutsInRange.isNotEmpty()) {
                        RepRangeDistribution(
                            range = rangeLabel,
                            volume = workoutsInRange.sumOf { it.totalVolume.toDouble() }.toFloat(),
                            sets = workoutsInRange.size,
                            avgWeight = workoutsInRange.map { it.actualWeight }.average().toFloat(),
                        )
                    } else {
                        null
                    }
                }

            _repRangeData.value = repRangeData
        } catch (e: Exception) {
            android.util.Log.e("ExerciseProgress", "Error loading rep range data", e)
            _repRangeData.value = emptyList()
        }
    }

    private suspend fun loadRPEZoneData(exerciseVariationId: Long) {
        try {
            // Get set logs with RPE data for this exercise in the last 12 weeks
            val startDate = LocalDate.now().minusWeeks(12)
            val endDate = LocalDate.now()

            val sets =
                repository.getSetLogsForExerciseInDateRange(
                    exerciseVariationId = exerciseVariationId,
                    startDate = startDate,
                    endDate = endDate,
                )

            // Filter completed sets with RPE data
            val completedSetsWithRPE = sets.filter { it.isCompleted && it.actualRpe != null }

            if (completedSetsWithRPE.isEmpty()) {
                _rpeZoneData.value = emptyList()
                return
            }

            // Define RPE zones
            val rpeZones =
                listOf(
                    Triple("Light", "RPE 1-4", 1f..4f) to Color(0xFF81C784), // Light Green
                    Triple("Medium", "RPE 5-7", 5f..7f) to Color(0xFFFFB74D), // Amber
                    Triple("Heavy", "RPE 8-10", 8f..10f) to Color(0xFFE53935), // Red
                )

            val rpeData =
                rpeZones.mapNotNull { (zoneInfo, color) ->
                    val (zoneName, rangeLabel, rpeRange) = zoneInfo
                    val setsInZone =
                        completedSetsWithRPE.filter { set ->
                            set.actualRpe!! in rpeRange
                        }

                    if (setsInZone.isNotEmpty()) {
                        IntensityZoneData(
                            zone = zoneName,
                            range = rangeLabel,
                            volume = setsInZone.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat(),
                            sets = setsInZone.size,
                            avgWeight = setsInZone.map { it.actualWeight }.average().toFloat(),
                            color = color,
                        )
                    } else {
                        null
                    }
                }

            _rpeZoneData.value = rpeData
        } catch (e: Exception) {
            android.util.Log.e("ExerciseProgress", "Error loading RPE zone data", e)
            _rpeZoneData.value = emptyList()
        }
    }
}
