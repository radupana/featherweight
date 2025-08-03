package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.WorkoutSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaginatedHistoryState(
    val workouts: List<WorkoutSummary> = emptyList(),
    val programmes: List<com.github.radupana.featherweight.repository.ProgrammeSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreProgrammes: Boolean = false,
    val hasMoreData: Boolean = true,
    val hasMoreProgrammes: Boolean = true,
    val currentPage: Int = 0,
    val currentProgrammePage: Int = 0,
    val pageSize: Int = 20,
    val totalCount: Int? = null,
    val error: String? = null,
)

class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _historyState = MutableStateFlow(PaginatedHistoryState())
    val historyState: StateFlow<PaginatedHistoryState> = _historyState

    // Store selected programme ID for navigation
    var selectedProgrammeId: Long? = null

    // Legacy compatibility - expose workouts and loading separately
    val workoutHistory: StateFlow<List<WorkoutSummary>> =
        _historyState
            .map { state ->
                state.workouts
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isLoading: StateFlow<Boolean> =
        _historyState
            .map { state ->
                state.isLoading
            }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        // Seed database if empty and load initial page
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        val currentState = _historyState.value
        if (currentState.workouts.isNotEmpty()) {
            println("🔍 HistoryViewModel: Already loaded ${currentState.workouts.size} workouts, skipping")
            return // Already loaded
        }

        println("🔍 HistoryViewModel: Loading initial data...")
        _historyState.value = currentState.copy(isLoading = true, error = null)

        try {
            val totalCount = repository.getTotalWorkoutCount()
            println("🔍 HistoryViewModel: Total workout count: $totalCount")

            val firstPageRepo = repository.getCompletedWorkoutsPagedOptimized(page = 0, pageSize = currentState.pageSize)
            println("🔍 HistoryViewModel: Received ${firstPageRepo.size} workouts from repository")

            // Debug logging for workout statuses
            firstPageRepo.forEach { workout ->
                println("  Workout ${workout.id}: status=${workout.status}, exercises=${workout.exerciseCount}, sets=${workout.setCount}")
            }

            val firstPage =
                firstPageRepo.map { repoSummary ->
                    WorkoutSummary(
                        id = repoSummary.id,
                        date = repoSummary.date,
                        name = repoSummary.name ?: repoSummary.programmeName,
                        exerciseCount = repoSummary.exerciseCount,
                        setCount = repoSummary.setCount,
                        totalWeight = repoSummary.totalWeight,
                        duration = repoSummary.duration,
                        status = repoSummary.status,
                        hasNotes = repoSummary.hasNotes,
                    )
                }

            val hasMoreData = firstPage.size == currentState.pageSize && firstPage.size < totalCount
            println("🔍 HistoryViewModel: Setting state with ${firstPage.size} workouts, hasMoreData: $hasMoreData")
            println(
                "🔍 HistoryViewModel: Logic: firstPage.size (${firstPage.size}) == pageSize (${currentState.pageSize}) && < totalCount ($totalCount)",
            )

            // Load programmes too
            val firstPageProgrammes = repository.getCompletedProgrammesPaged(page = 0, pageSize = currentState.pageSize)
            println("🔍 HistoryViewModel: Received ${firstPageProgrammes.size} programmes from repository")

            val hasMoreProgrammes = firstPageProgrammes.size == currentState.pageSize

            _historyState.value =
                currentState.copy(
                    workouts = firstPage,
                    programmes = firstPageProgrammes,
                    isLoading = false,
                    isLoadingMore = false, // Reset any concurrent loading state
                    isLoadingMoreProgrammes = false, // Reset programme loading too
                    currentPage = 0,
                    currentProgrammePage = 0,
                    totalCount = totalCount,
                    hasMoreData = hasMoreData,
                    hasMoreProgrammes = hasMoreProgrammes,
                )
        } catch (e: Exception) {
            println("🔍 HistoryViewModel: Error loading initial data: ${e.message}")
            e.printStackTrace()
            _historyState.value =
                currentState.copy(
                    isLoading = false,
                    error = "Failed to load workout history: ${e.message}",
                )
        }
    }

    fun loadWorkoutHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                refreshHistory()
            } else {
                loadInitialData()
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentState = _historyState.value

            try {
                val totalCount = repository.getTotalWorkoutCount()
                val firstPageRepo = repository.getCompletedWorkoutsPagedOptimized(page = 0, pageSize = currentState.pageSize)
                val firstPage =
                    firstPageRepo.map { repoSummary ->
                        WorkoutSummary(
                            id = repoSummary.id,
                            date = repoSummary.date,
                            name = repoSummary.name ?: repoSummary.programmeName,
                            exerciseCount = repoSummary.exerciseCount,
                            setCount = repoSummary.setCount,
                            totalWeight = repoSummary.totalWeight,
                            duration = repoSummary.duration,
                            status = repoSummary.status,
                            hasNotes = repoSummary.hasNotes,
                        )
                    }

                // Also refresh programmes
                val firstPageProgrammes = repository.getCompletedProgrammesPaged(page = 0, pageSize = currentState.pageSize)
                val hasMoreProgrammes = firstPageProgrammes.size == currentState.pageSize

                _historyState.value =
                    currentState.copy(
                        workouts = firstPage,
                        programmes = firstPageProgrammes,
                        isLoadingMore = false, // Reset loading states
                        isLoadingMoreProgrammes = false,
                        currentPage = 0,
                        currentProgrammePage = 0,
                        totalCount = totalCount,
                        hasMoreData = firstPage.size == currentState.pageSize && firstPage.size < totalCount,
                        hasMoreProgrammes = hasMoreProgrammes,
                        error = null,
                    )
            } catch (e: Exception) {
                _historyState.value =
                    currentState.copy(
                        error = "Failed to refresh workout history: ${e.message}",
                    )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadNextPage() {
        println("🔍 HistoryViewModel: loadNextPage() called")
        viewModelScope.launch {
            val currentState = _historyState.value

            // Don't load if already loading, no more data, or error state
            if (currentState.isLoadingMore || !currentState.hasMoreData || currentState.error != null) {
                println(
                    "🔍 HistoryViewModel: Skipping loadNextPage - isLoadingMore: ${currentState.isLoadingMore}, hasMoreData: ${currentState.hasMoreData}, error: ${currentState.error}",
                )
                return@launch
            }

            // Don't try to load next page if we haven't loaded initial data yet
            if (currentState.workouts.isEmpty() && (currentState.isLoading || currentState.currentPage == 0)) {
                println("🔍 HistoryViewModel: Skipping loadNextPage - initial data not loaded yet")
                return@launch
            }

            println("🔍 HistoryViewModel: Starting to load next page ${currentState.currentPage + 1}")
            _historyState.value = currentState.copy(isLoadingMore = true)

            try {
                val nextPage = currentState.currentPage + 1
                val newWorkoutsRepo =
                    repository.getCompletedWorkoutsPagedOptimized(
                        page = nextPage,
                        pageSize = currentState.pageSize,
                    )
                val newWorkouts =
                    newWorkoutsRepo.map { repoSummary ->
                        WorkoutSummary(
                            id = repoSummary.id,
                            date = repoSummary.date,
                            name = repoSummary.name ?: repoSummary.programmeName,
                            exerciseCount = repoSummary.exerciseCount,
                            setCount = repoSummary.setCount,
                            totalWeight = repoSummary.totalWeight,
                            duration = repoSummary.duration,
                            status = repoSummary.status,
                            hasNotes = repoSummary.hasNotes,
                        )
                    }

                val allWorkouts = currentState.workouts + newWorkouts
                val hasMoreData =
                    newWorkouts.size == currentState.pageSize &&
                        (currentState.totalCount?.let { allWorkouts.size < it } ?: true)

                println("🔍 HistoryViewModel: loadNextPage results:")
                println("  📦 newWorkouts.size: ${newWorkouts.size}")
                println("  📚 allWorkouts.size after merge: ${allWorkouts.size}")
                println("  🎯 totalCount: ${currentState.totalCount}")
                println("  ✅ hasMoreData: $hasMoreData")

                _historyState.value =
                    currentState.copy(
                        workouts = allWorkouts,
                        currentPage = nextPage,
                        isLoadingMore = false,
                        hasMoreData = hasMoreData,
                    )
            } catch (e: Exception) {
                _historyState.value =
                    currentState.copy(
                        isLoadingMore = false,
                        error = "Failed to load more workouts: ${e.message}",
                    )
            }
        }
    }

    fun loadNextProgrammePage() {
        println("🔍 HistoryViewModel: loadNextProgrammePage() called")
        viewModelScope.launch {
            val currentState = _historyState.value

            // Don't load if already loading, no more data, or error state
            if (currentState.isLoadingMoreProgrammes || !currentState.hasMoreProgrammes || currentState.error != null) {
                println(
                    "🔍 HistoryViewModel: Skipping loadNextProgrammePage - isLoadingMoreProgrammes: ${currentState.isLoadingMoreProgrammes}, hasMoreProgrammes: ${currentState.hasMoreProgrammes}, error: ${currentState.error}",
                )
                return@launch
            }

            println("🔍 HistoryViewModel: Starting to load next programme page ${currentState.currentProgrammePage + 1}")
            _historyState.value = currentState.copy(isLoadingMoreProgrammes = true)

            try {
                val nextPage = currentState.currentProgrammePage + 1
                val newProgrammes =
                    repository.getCompletedProgrammesPaged(
                        page = nextPage,
                        pageSize = currentState.pageSize,
                    )

                val allProgrammes = currentState.programmes + newProgrammes
                val hasMoreProgrammes = newProgrammes.size == currentState.pageSize

                println("🔍 HistoryViewModel: Loaded ${newProgrammes.size} new programmes, hasMoreProgrammes: $hasMoreProgrammes")

                _historyState.value =
                    currentState.copy(
                        programmes = allProgrammes,
                        isLoadingMoreProgrammes = false,
                        currentProgrammePage = nextPage,
                        hasMoreProgrammes = hasMoreProgrammes,
                    )
            } catch (e: Exception) {
                _historyState.value =
                    currentState.copy(
                        isLoadingMoreProgrammes = false,
                        error = "Failed to load more programmes: ${e.message}",
                    )
            }
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteWorkout(workoutId)

                val currentState = _historyState.value
                val updatedWorkouts = currentState.workouts.filter { it.id != workoutId }
                val newTotalCount = (currentState.totalCount ?: 0) - 1

                _historyState.value =
                    currentState.copy(
                        workouts = updatedWorkouts,
                        totalCount = newTotalCount,
                        error = null,
                    )
            } catch (e: Exception) {
                val currentState = _historyState.value
                _historyState.value =
                    currentState.copy(
                        error = "Failed to delete workout: ${e.message}",
                    )
                // Refresh on error to ensure UI is accurate
                refreshHistory()
            }
        }
    }

    fun clearError() {
        val currentState = _historyState.value
        _historyState.value = currentState.copy(error = null)
    }

    // Check if we should load more data when user scrolls near the end
    fun shouldLoadMore(visibleItemIndex: Int): Boolean {
        val currentState = _historyState.value
        val threshold = 5 // Load more when 5 items from the end
        val shouldLoad =
            visibleItemIndex >= (currentState.workouts.size - threshold) &&
                currentState.hasMoreData &&
                !currentState.isLoadingMore &&
                currentState.error == null

        if (visibleItemIndex >= 0) { // Only log if we have valid index
            println("🔍 HistoryViewModel: shouldLoadMore check:")
            println("  👀 visibleItemIndex: $visibleItemIndex")
            println("  📚 workouts.size: ${currentState.workouts.size}")
            println("  🎯 threshold: $threshold")
            println("  ✅ hasMoreData: ${currentState.hasMoreData}")
            println("  ⏳ isLoadingMore: ${currentState.isLoadingMore}")
            println("  ❌ error: ${currentState.error}")
            println("  🚀 shouldLoad: $shouldLoad")
        }

        return shouldLoad
    }
}
