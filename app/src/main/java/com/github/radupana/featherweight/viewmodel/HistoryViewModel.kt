package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.WorkoutSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _workoutHistory = MutableStateFlow<List<WorkoutSummary>>(emptyList())
    val workoutHistory: StateFlow<List<WorkoutSummary>> = _workoutHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Seed database if empty and load history
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            loadWorkoutHistory()
        }
    }

    fun loadWorkoutHistory() {
        viewModelScope.launch {
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
                _workoutHistory.value = uiHistory
            } catch (e: Exception) {
                // Handle error
                println("Error loading workout history: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshHistory() {
        loadWorkoutHistory()
    }
}
