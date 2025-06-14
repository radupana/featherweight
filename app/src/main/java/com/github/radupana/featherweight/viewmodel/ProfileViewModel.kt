package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val currentMaxes: List<ExerciseMaxWithName> = emptyList(),
    val big4Exercises: List<Exercise> = emptyList(),
    val showAddExerciseDialog: Boolean = false,
    val selectedExercise: Exercise? = null,
    val error: String? = null,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
        observeCurrentMaxes()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Ensure user profile exists
                val userId = repository.getCurrentUserId()
                repository.ensureUserProfile(userId)

                // Load Big 4 exercises
                val big4 = repository.getBig4Exercises()
                _uiState.value =
                    _uiState.value.copy(
                        big4Exercises = big4,
                        isLoading = false,
                    )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load profile data: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    private fun observeCurrentMaxes() {
        viewModelScope.launch {
            repository.getAllCurrentMaxes().collect { maxes ->
                _uiState.value = _uiState.value.copy(currentMaxes = maxes)
            }
        }
    }

    fun update1RM(
        exerciseId: Long,
        weight: Float,
    ) {
        viewModelScope.launch {
            try {
                repository.upsertExerciseMax(
                    exerciseId = exerciseId,
                    maxWeight = weight,
                    isEstimated = false,
                )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to update 1RM: ${e.message}",
                    )
            }
        }
    }

    fun showAddExerciseDialog() {
        _uiState.value = _uiState.value.copy(showAddExerciseDialog = true)
    }

    fun hideAddExerciseDialog() {
        _uiState.value =
            _uiState.value.copy(
                showAddExerciseDialog = false,
                selectedExercise = null,
            )
    }

    fun selectExercise(exercise: Exercise) {
        _uiState.value = _uiState.value.copy(selectedExercise = exercise)
    }

    fun addExercise1RM(
        exerciseId: Long,
        weight: Float,
    ) {
        viewModelScope.launch {
            try {
                repository.upsertExerciseMax(
                    exerciseId = exerciseId,
                    maxWeight = weight,
                    isEstimated = false,
                )
                hideAddExerciseDialog()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to add exercise 1RM: ${e.message}",
                    )
            }
        }
    }

    fun deleteMax(max: ExerciseMaxWithName) {
        viewModelScope.launch {
            try {
                repository.deleteExerciseMax(
                    com.github.radupana.featherweight.data.profile.UserExerciseMax(
                        id = max.id,
                        userId = max.userId,
                        exerciseId = max.exerciseId,
                        maxWeight = max.maxWeight,
                        recordedAt = max.recordedAt,
                        notes = max.notes,
                        isEstimated = max.isEstimated,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to delete 1RM: ${e.message}",
                    )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
