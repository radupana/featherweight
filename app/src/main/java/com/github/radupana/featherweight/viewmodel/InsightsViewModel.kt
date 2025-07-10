package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime

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
    val availableExercises: List<String> = listOf("Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift", "Barbell Overhead Press"),
    val selectedTimeframe: String = "1M", // 1W, 1M, 3M, 6M, 1Y
    val chartViewMode: ChartViewMode = ChartViewMode.ONE_RM,
    val cachedData: CachedAnalyticsData = CachedAnalyticsData(),
    val isRefreshing: Boolean = false, // Background refresh indicator
    val error: String? = null,
)

class InsightsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState

    init {
        println("🔵 AnalyticsViewModel: Initializing...")
        
        // Debug: Check PersonalRecord database
        viewModelScope.launch {
            try {
                val allPRs = repository.getAllPersonalRecordsFromDB()
                println("🏆 DEBUG: PersonalRecord database contains ${allPRs.size} records")
                allPRs.forEach { pr ->
                    println("🏆 DEBUG: ${pr.exerciseName} - ${pr.weight}kg x ${pr.reps} = ${pr.estimated1RM}kg 1RM on ${pr.recordDate}")
                }
            } catch (e: Exception) {
                println("🏆 DEBUG ERROR: Failed to query PersonalRecords - ${e.message}")
            }
        }
        
        loadInsightsData()
    }

    fun loadInsightsData(forceRefresh: Boolean = false) {
        println("🔵 InsightsViewModel: loadInsightsData called with forceRefresh=$forceRefresh")
        viewModelScope.launch {
            val currentState = _analyticsState.value
            val cachedData = currentState.cachedData
            val now = System.currentTimeMillis()
            val cacheAge = now - cachedData.lastUpdated
            val cacheValidDuration = 5 * 60 * 1000L // 5 minutes
            
            println("🔵 AnalyticsViewModel: Cache age: ${cacheAge}ms, Valid duration: ${cacheValidDuration}ms")
            println("🔵 AnalyticsViewModel: Cache last updated: ${cachedData.lastUpdated}")

            // Use cache-then-update strategy
            val shouldUseCache = !forceRefresh && cacheAge < cacheValidDuration && cachedData.lastUpdated > 0

            if (shouldUseCache) {
                println("🔵 AnalyticsViewModel: Using cached data")
                // Hydrate immediately from cache
                hydrateFromCache(cachedData)

                // Background refresh to check for new data
                backgroundRefresh()
            } else {
                println("🔵 AnalyticsViewModel: Performing full refresh")
                // Full refresh
                fullRefresh()
            }
        }
    }

    private suspend fun hydrateFromCache(cachedData: CachedAnalyticsData) {
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
        } catch (e: Exception) {
            // Silent failure for background refresh
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
        } catch (e: Exception) {
            // Handle refresh error
        }
    }

    private suspend fun fullRefresh() {
        println("🔵 AnalyticsViewModel: Starting full refresh...")
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
            println("🔵 AnalyticsViewModel: Checking if database needs seeding...")
            repository.seedDatabaseIfEmpty()

            // Load data sections in parallel
            val quickStatsJob = viewModelScope.async { loadQuickStats() }
            val volumeJob = viewModelScope.async { loadVolumeMetrics() }
            val strengthJob = viewModelScope.async { loadStrengthMetrics(_analyticsState.value.strengthMetrics.selectedExercise) }
            val performanceJob = viewModelScope.async { loadPerformanceMetrics() }

            // Update as each section completes and cache results
            val quickStats = quickStatsJob.await()
            println("🔵 AnalyticsViewModel: Quick stats loaded: $quickStats")
            _analyticsState.value =
                _analyticsState.value.copy(
                    quickStats = quickStats,
                    isQuickStatsLoading = false,
                )

            val volumeMetrics = volumeJob.await()
            println("🔵 AnalyticsViewModel: Volume metrics loaded: $volumeMetrics")
            _analyticsState.value =
                _analyticsState.value.copy(
                    volumeMetrics = volumeMetrics,
                    isVolumeLoading = false,
                )

            val strengthMetrics = strengthJob.await()
            println("🔵 AnalyticsViewModel: Strength metrics loaded: $strengthMetrics")
            _analyticsState.value =
                _analyticsState.value.copy(
                    strengthMetrics = strengthMetrics,
                    isStrengthLoading = false,
                )

            val performanceMetrics = performanceJob.await()
            println("🔵 AnalyticsViewModel: Performance metrics loaded: $performanceMetrics")

            // Cache all the fresh data
            val allWorkouts = repository.getWorkoutHistory()
            println("🔵 AnalyticsViewModel: All workouts count: ${allWorkouts.size}")
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
        } catch (e: Exception) {
            println("🔴 AnalyticsViewModel: Error loading analytics: ${e.message}")
            e.printStackTrace()
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
        val personalRecords = repository.getPersonalRecords(exerciseName)
        val estimated1RM = repository.getEstimated1RM(exerciseName)
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
        val monthStart = now.minusDays(30)

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
        loadInsightsData()
    }


    suspend fun getExercisesSummary(): List<com.github.radupana.featherweight.service.ExerciseSummary> {
        return withContext(Dispatchers.IO) {
            try {
                repository.getExercisesSummary()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    fun loadHighlightsData(
        onComplete: (
            recentPRs: List<com.github.radupana.featherweight.data.PersonalRecord>,
            weeklyWorkoutCount: Int,
            currentStreak: Int
        ) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Get recent PRs (last 30 days)
                val recentPRs = withContext(Dispatchers.IO) {
                    repository.getRecentPRs(limit = 5)
                }
                
                // Get workouts from this week
                val now = LocalDateTime.now()
                // Start from Sunday 23:59:59 to include all of Monday onwards
                val weekStart = now.with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay().minusSeconds(1)
                val weeklyWorkoutCount = withContext(Dispatchers.IO) {
                    repository.getCompletedWorkoutCountSince(weekStart)
                }
                
                // Get current streak (weeks with 3+ workouts)
                val currentStreak = withContext(Dispatchers.IO) {
                    repository.getWeeklyStreak()
                }
                
                onComplete(recentPRs, weeklyWorkoutCount, currentStreak)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(emptyList(), 0, 0)
            }
        }
    }
}
