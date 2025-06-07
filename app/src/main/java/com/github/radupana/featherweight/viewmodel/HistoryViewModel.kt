package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.WorkoutSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryCache(
    val workouts: List<WorkoutSummary> = emptyList(),
    val lastUpdated: Long = 0L,
    val sessionWorkoutIds: Set<Long> = emptySet(),
)

class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _workoutHistory = MutableStateFlow<List<WorkoutSummary>>(emptyList())
    val workoutHistory: StateFlow<List<WorkoutSummary>> = _workoutHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var historyCache = HistoryCache()

    init {
        // Seed database if empty and load history
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            loadWorkoutHistory()
        }
    }

    fun loadWorkoutHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cacheAge = now - historyCache.lastUpdated
            val cacheValidDuration = 5 * 60 * 1000L // 5 minutes

            // Use cache-then-update strategy
            val shouldUseCache = !forceRefresh && cacheAge < cacheValidDuration && historyCache.lastUpdated > 0

            if (shouldUseCache) {
                // Hydrate immediately from cache
                hydrateFromCache()

                // Background refresh to check for new data
                backgroundRefresh()
            } else {
                // Full refresh
                fullRefresh()
            }
        }
    }

    private suspend fun hydrateFromCache() {
        if (historyCache.workouts.isNotEmpty()) {
            _workoutHistory.value = historyCache.workouts
            _isLoading.value = false
        }
    }

    private suspend fun backgroundRefresh() {
        _isRefreshing.value = true

        try {
            val freshWorkouts = repository.getWorkoutHistory()
            val freshWorkoutIds = freshWorkouts.map { it.id }.toSet()
            val cachedWorkoutIds = historyCache.sessionWorkoutIds

            // Check if there are new workouts since last cache
            val hasNewData = freshWorkoutIds != cachedWorkoutIds || freshWorkouts.size != historyCache.workouts.size

            if (hasNewData) {
                // Update with fresh data
                refreshWithNewData(freshWorkouts)
            }
        } catch (e: Exception) {
            // Silent failure for background refresh
            println("Background refresh failed: ${e.message}")
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun refreshWithNewData(freshWorkouts: List<com.github.radupana.featherweight.repository.WorkoutSummary>) {
        try {
            // Convert repository WorkoutSummary to UI WorkoutSummary
            val uiHistory =
                freshWorkouts.map { repoSummary ->
                    WorkoutSummary(
                        id = repoSummary.id,
                        date = repoSummary.date,
                        name = repoSummary.name,
                        exerciseCount = repoSummary.exerciseCount,
                        setCount = repoSummary.setCount,
                        totalWeight = repoSummary.totalWeight,
                        duration = repoSummary.duration,
                        isCompleted = repoSummary.isCompleted,
                    )
                }

            // Update cache
            historyCache =
                historyCache.copy(
                    workouts = uiHistory,
                    lastUpdated = System.currentTimeMillis(),
                    sessionWorkoutIds = freshWorkouts.map { it.id }.toSet(),
                )

            // Update UI
            _workoutHistory.value = uiHistory
        } catch (e: Exception) {
            println("Error refreshing workout history: ${e.message}")
        }
    }

    private suspend fun fullRefresh() {
        _isLoading.value = true
        try {
            val history = repository.getWorkoutHistory()

            // Convert repository WorkoutSummary to UI WorkoutSummary
            val uiHistory =
                history.map { repoSummary ->
                    WorkoutSummary(
                        id = repoSummary.id,
                        date = repoSummary.date,
                        name = repoSummary.name,
                        exerciseCount = repoSummary.exerciseCount,
                        setCount = repoSummary.setCount,
                        totalWeight = repoSummary.totalWeight,
                        duration = repoSummary.duration,
                        isCompleted = repoSummary.isCompleted,
                    )
                }

            // Update cache
            historyCache =
                historyCache.copy(
                    workouts = uiHistory,
                    lastUpdated = System.currentTimeMillis(),
                    sessionWorkoutIds = history.map { it.id }.toSet(),
                )

            _workoutHistory.value = uiHistory
        } catch (e: Exception) {
            // Handle error
            println("Error loading workout history: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun refreshHistory() {
        loadWorkoutHistory(forceRefresh = true)
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteWorkout(workoutId)

                // Update cache by removing the deleted workout
                val updatedWorkouts = historyCache.workouts.filter { it.id != workoutId }
                val updatedWorkoutIds = updatedWorkouts.map { it.id }.toSet()

                historyCache =
                    historyCache.copy(
                        workouts = updatedWorkouts,
                        lastUpdated = System.currentTimeMillis(),
                        sessionWorkoutIds = updatedWorkoutIds,
                    )

                // Update UI immediately
                _workoutHistory.value = updatedWorkouts
            } catch (e: Exception) {
                println("Error deleting workout: ${e.message}")
                // Refresh on error to ensure UI is accurate
                loadWorkoutHistory(forceRefresh = true)
            }
        }
    }

    // Method to invalidate cache when new workouts are added during the session
    fun addWorkoutToCache(newWorkout: WorkoutSummary) {
        val updatedWorkouts = listOf(newWorkout) + historyCache.workouts
        val updatedWorkoutIds = updatedWorkouts.map { it.id }.toSet()

        historyCache =
            historyCache.copy(
                workouts = updatedWorkouts,
                lastUpdated = System.currentTimeMillis(),
                sessionWorkoutIds = updatedWorkoutIds,
            )

        _workoutHistory.value = updatedWorkouts
    }
}
