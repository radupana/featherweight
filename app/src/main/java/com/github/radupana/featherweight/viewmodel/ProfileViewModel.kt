package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName

data class ProfileUiState(
    val isLoading: Boolean = false,
    val currentMaxes: List<ExerciseMaxWithName> = emptyList(),
    val big4Exercises: List<Big4Exercise> = emptyList(),
    val otherExercises: List<ExerciseMaxWithName> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val isOneRMSectionExpanded: Boolean = true,
    val isBig4SubSectionExpanded: Boolean = true,
    val isOtherSubSectionExpanded: Boolean = true,
)

data class ExerciseMaxWithName(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val oneRMDate: java.time.LocalDateTime,
    val oneRMContext: String,
    val oneRMType: com.github.radupana.featherweight.data.profile.OneRMType,
    val notes: String? = null,
    val sessionCount: Int = 0,
)

data class Big4Exercise(
    val exerciseId: Long,
    val exerciseName: String,
    val oneRMValue: Float? = null,
    val oneRMType: com.github.radupana.featherweight.data.profile.OneRMType? = null,
    val oneRMContext: String? = null,
    val oneRMDate: java.time.LocalDateTime? = null,
    val sessionCount: Int = 0,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
        observeCurrentMaxes()
        observeBig4AndOtherExercises()
    }

    fun refreshData() {
        println("🔄 ProfileViewModel: Refreshing profile data")
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Ensure user profile exists
                val userId = repository.getCurrentUserId()
                repository.ensureUserProfile(userId)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
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
            repository.getAllCurrentMaxesWithNames(userId).collect { maxes ->
                _uiState.value = _uiState.value.copy(
                    currentMaxes = maxes.map { max ->
                        ExerciseMaxWithName(
                            id = max.id,
                            exerciseId = max.exerciseId,
                            exerciseName = max.exerciseName,
                            oneRMEstimate = max.oneRMEstimate,
                            oneRMDate = max.oneRMDate,
                            oneRMContext = max.oneRMContext,
                            oneRMType = max.oneRMType,
                            notes = max.notes,
                            sessionCount = max.sessionCount
                        )
                    }
                )
            }
        }
    }
    
    private fun observeBig4AndOtherExercises() {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            
            // Get Big 4 exercises
            launch {
                repository.getBig4ExercisesWithMaxes(userId).collect { big4 ->
                    _uiState.value = _uiState.value.copy(
                        big4Exercises = big4.map { max ->
                            Big4Exercise(
                                exerciseId = max.exerciseId,
                                exerciseName = max.exerciseName,
                                oneRMValue = max.oneRMEstimate,
                                oneRMType = max.oneRMType,
                                oneRMContext = max.oneRMContext,
                                oneRMDate = max.oneRMDate,
                                sessionCount = max.sessionCount
                            )
                        }
                    )
                }
            }
            
            // Get other exercises
            launch {
                repository.getOtherExercisesWithMaxes(userId).collect { others ->
                    _uiState.value = _uiState.value.copy(
                        otherExercises = others.map { max ->
                            ExerciseMaxWithName(
                                id = max.id,
                                exerciseId = max.exerciseId,
                                exerciseName = max.exerciseName,
                                oneRMEstimate = max.oneRMEstimate,
                                oneRMDate = max.oneRMDate,
                                oneRMContext = max.oneRMContext,
                                oneRMType = max.oneRMType,
                                notes = max.notes,
                                sessionCount = max.sessionCount
                            )
                        }
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }


    fun update1RM(exerciseId: Long, exerciseName: String, newMax: Float) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                repository.upsertExerciseMax(
                    userId = userId,
                    exerciseId = exerciseId,
                    oneRMEstimate = newMax,
                    oneRMContext = "Manually set",
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED
                )
                _uiState.value = _uiState.value.copy(
                    successMessage = "Updated 1RM for $exerciseName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update 1RM: ${e.message}"
                )
            }
        }
    }

    fun deleteMax(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                repository.deleteAllMaxesForExercise(exerciseId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Deleted 1RM record"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete 1RM: ${e.message}"
                )
            }
        }
    }
    
    fun toggleOneRMSection() {
        _uiState.value = _uiState.value.copy(
            isOneRMSectionExpanded = !_uiState.value.isOneRMSectionExpanded
        )
    }
    
    fun toggleBig4SubSection() {
        _uiState.value = _uiState.value.copy(
            isBig4SubSectionExpanded = !_uiState.value.isBig4SubSectionExpanded
        )
    }
    
    fun toggleOtherSubSection() {
        _uiState.value = _uiState.value.copy(
            isOtherSubSectionExpanded = !_uiState.value.isOtherSubSectionExpanded
        )
    }
}