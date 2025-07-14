package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.seeding.WorkoutSeedConfig
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.WorkoutSeedingService
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
    val successMessage: String? = null,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val seedingService = WorkoutSeedingService(repository)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
        observeCurrentMaxes()
    }
    
    fun refreshData() {
        println("🔄 ProfileViewModel: Refreshing profile data")
        loadProfileData()
        // Re-subscribe to the Flow to ensure we get the latest data
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
            val userId = repository.getCurrentUserId()
            println("🔍 ProfileViewModel: Observing maxes for user $userId")
            
            repository.getAllCurrentMaxes().collect { maxes ->
                println("📊 ProfileViewModel: Received ${maxes.size} max records")
                maxes.forEach { max ->
                    println("  - ${max.exerciseName}: ${max.maxWeight}kg (userId: ${max.userId})")
                }
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
                    UserExerciseMax(
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

    fun clearAllMaxesForExercise(exerciseId: Long) {
        viewModelScope.launch {
            try {
                println("🗑️ ProfileViewModel: Clearing all 1RM records for exercise ID: $exerciseId")
                repository.deleteAllMaxesForExercise(exerciseId)
                println("✅ ProfileViewModel: Successfully cleared all 1RM records")
            } catch (e: Exception) {
                println("❌ ProfileViewModel: Failed to clear 1RM: ${e.message}")
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to clear 1RM: ${e.message}",
                    )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun updateExerciseMax(
        exerciseName: String,
        maxWeight: Float,
    ) {
        viewModelScope.launch {
            try {
                println("🔄 ProfileViewModel: Updating 1RM for $exerciseName to $maxWeight kg")
                // Find the exercise by name
                val exercise = repository.getExerciseByName(exerciseName)
                if (exercise != null) {
                    println("✅ ProfileViewModel: Found exercise ${exercise.name} with ID ${exercise.id}")
                    repository.upsertExerciseMax(
                        exerciseId = exercise.id,
                        maxWeight = maxWeight,
                        isEstimated = false,
                        notes = "Updated from programme setup",
                    )
                    println("✅ ProfileViewModel: Successfully updated 1RM")
                } else {
                    println("❌ ProfileViewModel: Exercise not found for name: $exerciseName")
                }
            } catch (e: Exception) {
                println("❌ ProfileViewModel: Error updating 1RM: ${e.message}")
                e.printStackTrace()
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to update 1RM for $exerciseName: ${e.message}",
                    )
            }
        }
    }

    fun findAndSelectExercise(exerciseName: String) {
        viewModelScope.launch {
            try {
                val exercise = repository.getExerciseByName(exerciseName)
                if (exercise != null) {
                    _uiState.value = _uiState.value.copy(selectedExercise = exercise)
                }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to find exercise: ${e.message}",
                    )
            }
        }
    }

    fun seedWorkouts(config: WorkoutSeedConfig) {
        viewModelScope.launch {
            try {
                println("🌱 ProfileViewModel: Starting workout seeding with config: $config")
                val generatedCount = seedingService.seedWorkouts(config)
                println("✅ ProfileViewModel: Workout seeding completed successfully")
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Successfully generated $generatedCount workouts",
                    )
            } catch (e: Exception) {
                println("❌ ProfileViewModel: Failed to seed workouts: ${e.message}")
                e.printStackTrace()
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to seed workouts: ${e.message}",
                    )
            }
        }
    }

    fun clearAllWorkouts() {
        viewModelScope.launch {
            try {
                println("🗑️ ProfileViewModel: Clearing all workouts")
                repository.deleteAllWorkouts()
                println("✅ ProfileViewModel: All workouts cleared successfully")
            } catch (e: Exception) {
                println("❌ ProfileViewModel: Failed to clear workouts: ${e.message}")
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to clear workouts: ${e.message}",
                    )
            }
        }
    }
}
