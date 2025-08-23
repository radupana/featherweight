package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.InsightCategory
import com.github.radupana.featherweight.data.InsightSeverity
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.TrainingInsight
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.TrainingAnalysisService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class VolumeMetrics(
    val thisWeek: Float = 0f,
    val thisMonth: Float = 0f,
    val averageWeekly: Float = 0f,
    val weeklyChange: Float = 0f,
    val monthlyChange: Float = 0f,
    val weeklyHistory: List<Pair<String, Float>> = emptyList(),
)

data class StrengthMetrics(
    val selectedExercise: String = "Barbell Back Squat",
    val currentMax: Float? = null,
    val estimated1RM: Float? = null,
    val personalRecords: List<Pair<Float, LocalDateTime>> = emptyList(),
    val recentProgress: Float? = null,
)

data class PerformanceMetrics(
    val trainingFrequency: Int = 0,
    val averageRPE: Float? = null,
    val trainingStreak: Int = 0,
    val consistencyScore: Float? = null,
)

data class QuickStats(
    val weeklyVolume: String = "0kg",
    val recentPR: Pair<String, Float>? = null,
    val trainingStreak: Int = 0,
    val avgTrainingDaysPerWeek: Float = 0f,
    val monthlyProgress: Float? = null,
)

data class CachedAnalyticsData(
    val quickStats: QuickStats? = null,
    val volumeMetrics: VolumeMetrics? = null,
    val performanceMetrics: PerformanceMetrics? = null,
    val exerciseDataCache: Map<String, StrengthMetrics> = emptyMap(),
    val lastUpdated: Long = 0L, // Timestamp
    val sessionWorkouts: List<Long> = emptyList(), // Track new workouts this session
)

enum class ChartViewMode {
    ONE_RM,
    VOLUME,
}

data class AnalyticsState(
    val isLoading: Boolean = true,
    val isQuickStatsLoading: Boolean = true,
    val isVolumeLoading: Boolean = true,
    val isStrengthLoading: Boolean = true,
    val isPerformanceLoading: Boolean = true,
    val volumeMetrics: VolumeMetrics = VolumeMetrics(),
    val strengthMetrics: StrengthMetrics = StrengthMetrics(),
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val quickStats: QuickStats = QuickStats(),
    val availableExercises: List<String> =
        listOf("Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift", "Barbell Overhead Press"),
    val selectedTimeframe: String = "1M", // 1W, 1M, 3M, 6M, 1Y
    val chartViewMode: ChartViewMode = ChartViewMode.ONE_RM,
    val cachedData: CachedAnalyticsData = CachedAnalyticsData(),
    val isRefreshing: Boolean = false, // Background refresh indicator
    val error: String? = null,
)

class InsightsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val repository = FeatherweightRepository(application)
    private val analysisService = TrainingAnalysisService()
    private val gson = Gson()

    companion object {
        private const val TAG = "InsightsViewModel"
        private const val MINIMUM_WORKOUTS_FOR_ANALYSIS = 16
        private const val ANALYSIS_PERIOD_WEEKS = 12
    }

    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState

    private val _trainingAnalysis = MutableStateFlow<TrainingAnalysis?>(null)
    val trainingAnalysis: StateFlow<TrainingAnalysis?> = _trainingAnalysis

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _currentWorkoutCount = MutableStateFlow(0)
    val currentWorkoutCount: StateFlow<Int> = _currentWorkoutCount

    // Reactive exercise name mapping
    private val _exerciseNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val exerciseNames: StateFlow<Map<Long, String>> = _exerciseNames

    init {
        loadInsightsData()
        loadExerciseNames()
    }

    private fun loadExerciseNames() {
        viewModelScope.launch {
            val exercises = repository.getAllExercises()
            _exerciseNames.value = exercises.associate { it.id to it.name }
        }
    }

    fun loadInsightsData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Always update current workout count for accurate display
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            _currentWorkoutCount.value = repository.getWorkoutCountByDateRange(startDate, endDate)

            val currentState = _analyticsState.value
            val cachedData = currentState.cachedData
            val now = System.currentTimeMillis()
            val cacheAge = now - cachedData.lastUpdated
            val cacheValidDuration = 5 * 60 * 1000L // 5 minutes

            // Use cache-then-update strategy
            val shouldUseCache = !forceRefresh && cacheAge < cacheValidDuration && cachedData.lastUpdated > 0

            if (shouldUseCache) {
                // Hydrate immediately from cache
                hydrateFromCache(cachedData)

                // Background refresh to check for new data
                backgroundRefresh()
            } else {
                // Full refresh
                fullRefresh()
            }
        }
    }

    private fun hydrateFromCache(cachedData: CachedAnalyticsData) {
        val currentState = _analyticsState.value

        _analyticsState.value =
            currentState.copy(
                quickStats = cachedData.quickStats ?: currentState.quickStats,
                volumeMetrics = cachedData.volumeMetrics ?: currentState.volumeMetrics,
                performanceMetrics = cachedData.performanceMetrics ?: currentState.performanceMetrics,
                strengthMetrics =
                    cachedData.exerciseDataCache[currentState.strengthMetrics.selectedExercise]
                        ?: currentState.strengthMetrics,
                isQuickStatsLoading = false,
                isVolumeLoading = false,
                isStrengthLoading = false,
                isPerformanceLoading = false,
                isLoading = false,
            )
    }

    private suspend fun backgroundRefresh() {
        _analyticsState.value = _analyticsState.value.copy(isRefreshing = true)

        try {
            // Check for new workouts since last cache
            val newWorkouts = repository.getWorkoutHistory()
            val cachedWorkoutIds = _analyticsState.value.cachedData.sessionWorkouts
            val hasNewData = newWorkouts.any { !cachedWorkoutIds.contains(it.id) }

            if (hasNewData) {
                // Incremental update - only refresh affected data
                refreshWithNewData(newWorkouts.map { it.id })
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("InsightsViewModel", "Background refresh failed", e)
            // Background refresh failed - this is expected behavior for offline scenarios
            _analyticsState.value = _analyticsState.value.copy(error = "Background refresh failed")
        } catch (e: IllegalStateException) {
            Log.e("InsightsViewModel", "Background refresh failed", e)
            // Background refresh failed - this is expected behavior for offline scenarios
            _analyticsState.value = _analyticsState.value.copy(error = "Background refresh failed")
        } finally {
            _analyticsState.value = _analyticsState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun refreshWithNewData(allWorkoutIds: List<Long>) {
        try {
            // Update cache with fresh data
            val quickStats = loadQuickStats()
            val volumeMetrics = loadVolumeMetrics()
            val performanceMetrics = loadPerformanceMetrics()

            // Update current exercise data
            val currentExercise = _analyticsState.value.strengthMetrics.selectedExercise
            val strengthMetrics = loadStrengthMetrics(currentExercise)

            val newCachedData =
                _analyticsState.value.cachedData.copy(
                    quickStats = quickStats,
                    volumeMetrics = volumeMetrics,
                    performanceMetrics = performanceMetrics,
                    exerciseDataCache =
                        _analyticsState.value.cachedData.exerciseDataCache +
                            (currentExercise to strengthMetrics),
                    lastUpdated = System.currentTimeMillis(),
                    sessionWorkouts = allWorkoutIds,
                )

            _analyticsState.value =
                _analyticsState.value.copy(
                    quickStats = quickStats,
                    volumeMetrics = volumeMetrics,
                    performanceMetrics = performanceMetrics,
                    strengthMetrics = strengthMetrics,
                    cachedData = newCachedData,
                )
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("InsightsViewModel", "Data refresh failed", e)
            // Refresh failed - update error state for user awareness
            _analyticsState.value = _analyticsState.value.copy(error = "Data refresh failed: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("InsightsViewModel", "Data refresh failed", e)
            // Refresh failed - update error state for user awareness
            _analyticsState.value = _analyticsState.value.copy(error = "Data refresh failed: ${e.message}")
        }
    }

    private suspend fun fullRefresh() {
        _analyticsState.value =
            _analyticsState.value.copy(
                isLoading = true,
                isQuickStatsLoading = true,
                isVolumeLoading = true,
                isStrengthLoading = true,
                isPerformanceLoading = true,
                error = null,
            )

        try {
            repository.seedDatabaseIfEmpty()

            // Load data sections in parallel
            val quickStatsJob = viewModelScope.async { loadQuickStats() }
            val volumeJob = viewModelScope.async { loadVolumeMetrics() }
            val strengthJob = viewModelScope.async { loadStrengthMetrics(_analyticsState.value.strengthMetrics.selectedExercise) }
            val performanceJob = viewModelScope.async { loadPerformanceMetrics() }

            // Update as each section completes and cache results
            val quickStats = quickStatsJob.await()
            _analyticsState.value =
                _analyticsState.value.copy(
                    quickStats = quickStats,
                    isQuickStatsLoading = false,
                )

            val volumeMetrics = volumeJob.await()
            _analyticsState.value =
                _analyticsState.value.copy(
                    volumeMetrics = volumeMetrics,
                    isVolumeLoading = false,
                )

            val strengthMetrics = strengthJob.await()
            _analyticsState.value =
                _analyticsState.value.copy(
                    strengthMetrics = strengthMetrics,
                    isStrengthLoading = false,
                )

            val performanceMetrics = performanceJob.await()

            // Cache all the fresh data
            val allWorkouts = repository.getWorkoutHistory()
            val newCachedData =
                CachedAnalyticsData(
                    quickStats = quickStats,
                    volumeMetrics = volumeMetrics,
                    performanceMetrics = performanceMetrics,
                    exerciseDataCache = mapOf(strengthMetrics.selectedExercise to strengthMetrics),
                    lastUpdated = System.currentTimeMillis(),
                    sessionWorkouts = allWorkouts.map { it.id },
                )

            _analyticsState.value =
                _analyticsState.value.copy(
                    performanceMetrics = performanceMetrics,
                    isPerformanceLoading = false,
                    isLoading = false,
                    cachedData = newCachedData,
                )
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("InsightsViewModel", "Failed to load analytics data", e)
            _analyticsState.value =
                _analyticsState.value.copy(
                    isLoading = false,
                    isQuickStatsLoading = false,
                    isVolumeLoading = false,
                    isStrengthLoading = false,
                    isPerformanceLoading = false,
                    error = "Failed to load analytics: ${e.message}",
                )
        } catch (e: IllegalStateException) {
            Log.e("InsightsViewModel", "Failed to load analytics data", e)
            _analyticsState.value =
                _analyticsState.value.copy(
                    isLoading = false,
                    isQuickStatsLoading = false,
                    isVolumeLoading = false,
                    isStrengthLoading = false,
                    isPerformanceLoading = false,
                    error = "Failed to load analytics: ${e.message}",
                )
        }
    }

    fun selectExercise(exerciseName: String) {
        viewModelScope.launch {
            val currentState = _analyticsState.value

            // Check cache first for instant switching
            val cachedData = currentState.cachedData.exerciseDataCache[exerciseName]
            if (cachedData != null) {
                _analyticsState.value =
                    currentState.copy(
                        strengthMetrics = cachedData,
                    )
            } else {
                // Show loading state for new exercise
                _analyticsState.value =
                    currentState.copy(
                        isStrengthLoading = true,
                    )

                // Load and cache new data
                val strengthMetrics = loadStrengthMetrics(exerciseName)
                val newCache = currentState.cachedData.exerciseDataCache.toMutableMap()
                newCache[exerciseName] = strengthMetrics

                val updatedCachedData =
                    currentState.cachedData.copy(
                        exerciseDataCache = newCache,
                    )

                _analyticsState.value =
                    currentState.copy(
                        strengthMetrics = strengthMetrics,
                        isStrengthLoading = false,
                        cachedData = updatedCachedData,
                    )
            }
        }
    }

    fun selectTimeframe(timeframe: String) {
        viewModelScope.launch {
            _analyticsState.value = _analyticsState.value.copy(selectedTimeframe = timeframe)
            loadInsightsData() // Reload with new timeframe
        }
    }

    fun setChartViewMode(mode: ChartViewMode) {
        _analyticsState.value = _analyticsState.value.copy(chartViewMode = mode)
    }

    private suspend fun loadVolumeMetrics(): VolumeMetrics {
        val now = LocalDateTime.now()
        val weekStart = now.minusDays(7)
        val monthStart = now.minusDays(30)
        val prevWeekStart = now.minusDays(14)
        val prevMonthStart = now.minusDays(60)

        val thisWeekVolume = repository.getWeeklyVolumeTotal(weekStart)
        val thisMonthVolume = repository.getMonthlyVolumeTotal(monthStart)
        val prevWeekVolume = repository.getWeeklyVolumeTotal(prevWeekStart)
        val prevMonthVolume = repository.getMonthlyVolumeTotal(prevMonthStart)

        // Calculate average weekly volume over last 4 weeks
        val fourWeeksAgo = now.minusDays(28)
        val totalVolumeLastMonth = repository.getMonthlyVolumeTotal(fourWeeksAgo)
        val averageWeekly = totalVolumeLastMonth / 4f

        // Calculate percentage changes
        val weeklyChange =
            if (prevWeekVolume > 0) {
                ((thisWeekVolume - prevWeekVolume) / prevWeekVolume) * 100
            } else {
                0f
            }

        val monthlyChange =
            if (prevMonthVolume > 0) {
                ((thisMonthVolume - prevMonthVolume) / prevMonthVolume) * 100
            } else {
                0f
            }

        // Get historical data for charts
        val weeklyHistory = repository.getWeeklyVolumeHistory(8) // Last 8 weeks

        return VolumeMetrics(
            thisWeek = thisWeekVolume,
            thisMonth = thisMonthVolume,
            averageWeekly = averageWeekly,
            weeklyChange = weeklyChange,
            monthlyChange = monthlyChange,
            weeklyHistory = weeklyHistory,
        )
    }

    private suspend fun loadStrengthMetrics(exerciseName: String): StrengthMetrics {
        val exerciseVariation = repository.getExerciseByName(exerciseName)
        val exerciseVariationId = exerciseVariation?.id ?: 0L
        val personalRecords = repository.getPersonalRecords(exerciseVariationId)
        val estimated1RM = repository.getEstimated1RM(exerciseVariationId)
        val currentMax = personalRecords.lastOrNull()?.first

        // Calculate recent progress (last 30 days vs previous 30 days)
        val recentProgress = repository.getProgressPercentage(30)

        return StrengthMetrics(
            selectedExercise = exerciseName,
            currentMax = currentMax,
            estimated1RM = estimated1RM,
            personalRecords = personalRecords,
            recentProgress = recentProgress,
        )
    }

    private suspend fun loadPerformanceMetrics(): PerformanceMetrics {
        val now = LocalDateTime.now()
        val weekStart = now.minusDays(7)

        val trainingFrequency = repository.getTrainingFrequency(weekStart, now)
        val averageRPE = repository.getAverageRPE(daysSince = 30)
        val trainingStreak = repository.getTrainingStreak()

        // Calculate consistency score based on training frequency
        val consistencyScore =
            when {
                trainingFrequency >= 5 -> 100f
                trainingFrequency >= 4 -> 85f
                trainingFrequency >= 3 -> 70f
                trainingFrequency >= 2 -> 50f
                trainingFrequency >= 1 -> 25f
                else -> 0f
            }

        return PerformanceMetrics(
            trainingFrequency = trainingFrequency,
            averageRPE = averageRPE,
            trainingStreak = trainingStreak,
            consistencyScore = consistencyScore,
        )
    }

    private suspend fun loadQuickStats(): QuickStats {
        val now = LocalDateTime.now()
        val weekStart = now.minusDays(7)

        val weeklyVolume = repository.getWeeklyVolumeTotal(weekStart)
        val recentPR = repository.getRecentPR()
        val trainingStreak = repository.getTrainingStreak()
        val avgTrainingDaysPerWeek = repository.getAverageTrainingDaysPerWeek()
        val monthlyProgress = repository.getProgressPercentage(30)

        return QuickStats(
            weeklyVolume = "${weeklyVolume.toInt()}kg",
            recentPR = recentPR,
            trainingStreak = trainingStreak,
            avgTrainingDaysPerWeek = avgTrainingDaysPerWeek,
            monthlyProgress = monthlyProgress,
        )
    }

    fun refreshData() {
        loadInsightsData() // This already updates workout count
    }

    suspend fun getGroupedExercisesSummary(): com.github.radupana.featherweight.service.GroupedExerciseSummary =
        withContext(Dispatchers.IO) {
            try {
                repository.getExercisesSummary()
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("InsightsViewModel", "Error", e)
                com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            } catch (e: IllegalStateException) {
                Log.e("InsightsViewModel", "Error", e)
                com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            }
        }

    fun loadHighlightsData(
        onComplete: (
            recentPRs: List<com.github.radupana.featherweight.data.PersonalRecord>,
            weeklyWorkoutCount: Int,
            currentStreak: Int,
        ) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                // Get recent PRs (last 30 days)
                val recentPRs =
                    withContext(Dispatchers.IO) {
                        repository.getRecentPRs(limit = 5)
                    }

                // Get workouts from this week
                val now = LocalDateTime.now()
                // Start from Sunday 23:59:59 to include all of Monday onwards
                val weekStart =
                    now
                        .with(java.time.DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay()
                        .minusSeconds(1)
                val weeklyWorkoutCount =
                    withContext(Dispatchers.IO) {
                        repository.getCompletedWorkoutCountSince(weekStart)
                    }

                // Get current streak (weeks with 3+ workouts)
                val currentStreak =
                    withContext(Dispatchers.IO) {
                        repository.getWeeklyStreak()
                    }

                onComplete(recentPRs, weeklyWorkoutCount, currentStreak)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("InsightsViewModel", "Error", e)
                onComplete(emptyList(), 0, 0)
            } catch (e: IllegalStateException) {
                Log.e("InsightsViewModel", "Error", e)
                onComplete(emptyList(), 0, 0)
            }
        }
    }

    // Training Analysis methods
    fun loadCachedAnalysis() {
        viewModelScope.launch {
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
            // Always calculate current workout count for accurate display
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            _currentWorkoutCount.value = repository.getWorkoutCountByDateRange(startDate, endDate)
        }
    }

    fun checkAndRunScheduledAnalysis() {
        viewModelScope.launch {
            // Always update current workout count
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            _currentWorkoutCount.value = repository.getWorkoutCountByDateRange(startDate, endDate)

            // Check if we've already checked today using SharedPreferences
            val prefs = getApplication<Application>().getSharedPreferences("training_analysis", 0)
            val lastCheckDate = prefs.getString("last_check_date", null)
            val today = LocalDate.now().toString()

            if (lastCheckDate == today) {
                // Already checked today, skip
                return@launch
            }

            // Update last check date
            prefs.edit().putString("last_check_date", today).apply()

            val lastAnalysis = repository.getLatestTrainingAnalysis()
            val shouldRunAnalysis =
                when {
                    lastAnalysis == null -> true // First time user
                    ChronoUnit.DAYS.between(lastAnalysis.analysisDate.toLocalDate(), LocalDate.now()) >= 7 -> true // Weekly
                    else -> false
                }

            if (shouldRunAnalysis) {
                runAnalysis()
            }
        }
    }

    fun forceAnalysis() { // For developer tools only
        viewModelScope.launch {
            runAnalysis()
        }
    }

    private suspend fun runAnalysis() {
        _isAnalyzing.value = true
        try {
            // Check if we have enough data
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            val workoutCount = repository.getWorkoutCountByDateRange(startDate, endDate)
            _currentWorkoutCount.value = workoutCount

            if (workoutCount < MINIMUM_WORKOUTS_FOR_ANALYSIS) {
                // Save a placeholder analysis indicating insufficient data
                val insufficientDataAnalysis =
                    TrainingAnalysis(
                        analysisDate = LocalDateTime.now(),
                        periodStart = startDate,
                        periodEnd = endDate,
                        overallAssessment = "INSUFFICIENT_DATA:$workoutCount:$MINIMUM_WORKOUTS_FOR_ANALYSIS",
                        keyInsightsJson =
                            gson.toJson(
                                listOf(
                                    TrainingInsight(
                                        category = InsightCategory.PROGRESSION,
                                        message = "Continue building training history",
                                        severity = InsightSeverity.INFO,
                                    ),
                                ),
                            ),
                        recommendationsJson =
                            gson.toJson(
                                listOf("Complete more workouts to enable analysis"),
                            ),
                        warningsJson = gson.toJson(emptyList<String>()),
                        userId = 1,
                    )
                repository.saveTrainingAnalysis(insufficientDataAnalysis)
                _trainingAnalysis.value = insufficientDataAnalysis
            } else {
                // Proceed with normal analysis
                val analysis = performAnalysis()
                repository.saveTrainingAnalysis(analysis)
                _trainingAnalysis.value = analysis
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: java.io.IOException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: IllegalStateException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } finally {
            _isAnalyzing.value = false
        }
    }

    private suspend fun performAnalysis(): TrainingAnalysis =
        withContext(Dispatchers.IO) {
            // 1. Fetch raw workout data
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            val workouts = repository.getWorkoutsByDateRange(startDate, endDate)

            // 2. Build JSON payload
            val jsonPayload = buildAnalysisPayload(workouts)

            // 3. Call OpenAI API
            val response = callOpenAIAPI(jsonPayload)

            // 4. Parse response into TrainingAnalysis
            parseAIResponse(response, startDate, endDate)
        }

    private suspend fun buildAnalysisPayload(workouts: List<com.github.radupana.featherweight.repository.WorkoutSummary>): String {
        val payload = JsonObject()

        // Analysis period
        val period = JsonObject()
        period.addProperty(
            "start_date",
            workouts
                .firstOrNull()
                ?.date
                ?.toLocalDate()
                .toString(),
        )
        period.addProperty(
            "end_date",
            workouts
                .lastOrNull()
                ?.date
                ?.toLocalDate()
                .toString(),
        )
        period.addProperty("total_workouts", workouts.size)
        payload.add("analysis_period", period)

        // Build SUMMARIZED workouts array to reduce token usage
        // Group by week and only include key metrics
        val workoutsArray = com.google.gson.JsonArray()

        // Only include last 4 weeks of detailed data, older data as weekly summaries
        LocalDate.now().minusWeeks(4)

        for (workout in workouts) {
            val workoutObj = JsonObject()
            workoutObj.addProperty("id", workout.id)
            workoutObj.addProperty("date", workout.date.toLocalDate().toString())
            workoutObj.addProperty("name", workout.name ?: "Workout")
            workoutObj.addProperty("duration_minutes", workout.duration?.div(60) ?: 0)
            workoutObj.addProperty("notes", "")

            // Get exercises for this workout
            val exercises = repository.getExerciseLogsForWorkout(workout.id)
            val exercisesArray = com.google.gson.JsonArray()

            for (exercise in exercises) {
                val exerciseObj = JsonObject()
                val exerciseName = repository.getExerciseById(exercise.exerciseVariationId)?.name ?: "Unknown Exercise"
                exerciseObj.addProperty("name", exerciseName)

                // Get sets for this exercise
                val sets = repository.getSetLogsForExercise(exercise.id)
                val setsArray = com.google.gson.JsonArray()

                for ((index, set) in sets.withIndex()) {
                    val setObj = JsonObject()
                    setObj.addProperty("set_number", index + 1)
                    setObj.addProperty("weight", set.actualWeight ?: set.targetWeight)
                    setObj.addProperty("reps", set.actualReps ?: set.targetReps)
                    setObj.addProperty("rpe", set.actualRpe)
                    setObj.addProperty("rest_seconds", 180) // Default rest time
                    setObj.addProperty("completed", set.isCompleted)
                    setsArray.add(setObj)
                }

                exerciseObj.add("sets", setsArray)
                exercisesArray.add(exerciseObj)
            }

            workoutObj.add("exercises", exercisesArray)
            workoutsArray.add(workoutObj)
        }

        payload.add("workouts", workoutsArray)

        // Add personal records
        val prs = repository.getRecentPRs(limit = 20)
        val prsArray = com.google.gson.JsonArray()

        for (pr in prs) {
            val prObj = JsonObject()
            val exerciseName = repository.getExerciseById(pr.exerciseVariationId)?.name ?: "Unknown Exercise"
            prObj.addProperty("exercise", exerciseName)
            prObj.addProperty("date", pr.recordDate.toLocalDate().toString())
            prObj.addProperty("weight", pr.weight)
            prObj.addProperty("reps", pr.reps)

            if (pr.previousWeight != null) {
                val prevObj = JsonObject()
                prevObj.addProperty("weight", pr.previousWeight)
                prevObj.addProperty("reps", pr.previousReps)
                prevObj.addProperty("date", pr.previousDate?.toLocalDate().toString())
                prObj.add("previous_best", prevObj)
            }

            prsArray.add(prObj)
        }

        payload.add("personal_records", prsArray)

        return gson.toJson(payload)
    }

    private suspend fun callOpenAIAPI(jsonPayload: String): String {
        val prompt =
            """
            Analyze this training data and provide an EXTREMELY CONCISE analysis (readable in 10 seconds).

            OUTPUT FORMAT (JSON):
            {
              "overall_assessment": "ONE sentence, 50 words max. Focus on the single most important trend.",
              "key_insights": [
                {"category": "PROGRESSION|RECOVERY|BALANCE", 
                 "message": "Max 20 words. Just state the fact.",
                 "severity": "SUCCESS|WARNING|CRITICAL"}
              ],
              "warnings": [],
              "recommendations": ["Max 3 items. Each under 20 words. Ultra-specific actions."]
            }

            RULES:
            - overall_assessment: ONE sentence only (e.g., "Strong progression with 3 PRs, but needs more tricep work")
            - key_insights: Maximum 3 items. Only include if truly important
            - recommendations: Maximum 3 items. Must be ultra-specific (e.g., "Add 2x10 tricep extensions weekly")
            - Skip warnings array entirely (return empty)
            - Prioritize what matters most: PRs, overtraining risks, major imbalances
            - If everything looks good, just say so briefly
            
            Data: $jsonPayload
            """.trimIndent()

        return analysisService.analyzeTraining(prompt)
    }

    private fun parseAIResponse(
        response: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): TrainingAnalysis {
        try {
            val jsonResponse = JsonParser.parseString(response).asJsonObject

            val overallAssessment = jsonResponse.get("overall_assessment")?.asString ?: "Analysis complete."

            val keyInsights = mutableListOf<TrainingInsight>()
            jsonResponse.getAsJsonArray("key_insights")?.forEach { element ->
                val insight = element.asJsonObject
                val category =
                    try {
                        InsightCategory.valueOf(insight.get("category").asString)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown insight category, defaulting to PROGRESSION", e)
                        InsightCategory.PROGRESSION
                    }
                val severity =
                    try {
                        InsightSeverity.valueOf(insight.get("severity").asString)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown insight severity, defaulting to INFO", e)
                        InsightSeverity.INFO
                    }

                keyInsights.add(
                    TrainingInsight(
                        category = category,
                        message = insight.get("message").asString,
                        severity = severity,
                    ),
                )
            }

            val warnings = mutableListOf<String>()
            jsonResponse.getAsJsonArray("warnings")?.forEach { element ->
                warnings.add(element.asString)
            }

            val recommendations = mutableListOf<String>()
            jsonResponse.getAsJsonArray("recommendations")?.forEach { element ->
                recommendations.add(element.asString)
            }

            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = overallAssessment,
                keyInsightsJson = gson.toJson(keyInsights),
                recommendationsJson = gson.toJson(recommendations),
                warningsJson = gson.toJson(warnings),
                userId = 1,
            )
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e(TAG, "Failed to parse AI analysis, using fallback", e)
            val fallbackInsights =
                listOf(
                    TrainingInsight(
                        category = InsightCategory.PROGRESSION,
                        message = "Training data analyzed",
                        severity = InsightSeverity.INFO,
                    ),
                )
            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = "Analysis complete. Continue with your current training program.",
                keyInsightsJson = gson.toJson(fallbackInsights),
                recommendationsJson = gson.toJson(listOf("Continue current training program")),
                warningsJson = gson.toJson(emptyList<String>()),
                userId = 1,
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to parse AI analysis, using fallback", e)
            val fallbackInsights =
                listOf(
                    TrainingInsight(
                        category = InsightCategory.PROGRESSION,
                        message = "Training data analyzed",
                        severity = InsightSeverity.INFO,
                    ),
                )
            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = "Analysis complete. Continue with your current training program.",
                keyInsightsJson = gson.toJson(fallbackInsights),
                recommendationsJson = gson.toJson(listOf("Continue current training program")),
                warningsJson = gson.toJson(emptyList<String>()),
                userId = 1,
            )
        }
    }
}
