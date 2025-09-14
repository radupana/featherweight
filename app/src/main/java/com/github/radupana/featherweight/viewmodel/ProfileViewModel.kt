package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.FirebaseAuthService
import com.github.radupana.featherweight.service.WorkoutSeedingService
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.worker.ExportWorkoutsWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class ProfileTab {
    ONE_RM,
    SETTINGS,
    DATA,
    DEVELOPER,
}

data class AccountInfo(
    val email: String?,
    val isEmailVerified: Boolean,
    val authProvider: String?,
    val creationTime: Long?,
)

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
    val isDataManagementSectionExpanded: Boolean = true,
    val seedingState: SeedingState = SeedingState.Idle,
    val seedingWeeks: Int = 12,
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val currentTab: ProfileTab = ProfileTab.ONE_RM,
    val currentWeightUnit: WeightUnit = WeightUnit.KG,
    val accountInfo: AccountInfo? = null,
    val signOutRequested: Boolean = false,
)

sealed class SeedingState {
    object Idle : SeedingState()

    object InProgress : SeedingState()

    data class Success(
        val workoutsCreated: Int,
    ) : SeedingState()

    data class Error(
        val message: String?,
    ) : SeedingState()
}

data class ExerciseMaxWithName(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val oneRMDate: LocalDateTime,
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
    val oneRMDate: LocalDateTime? = null,
    val sessionCount: Int = 0,
)

class ProfileViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val workoutSeedingService = WorkoutSeedingService(repository)
    private val weightUnitManager: WeightUnitManager = ServiceLocator.provideWeightUnitManager(application)
    private val authManager: AuthenticationManager = ServiceLocator.provideAuthenticationManager(application)
    private val firebaseAuth: FirebaseAuthService = ServiceLocator.provideFirebaseAuthService()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
        observeCurrentMaxes()
        observeBig4AndOtherExercises()
        loadCurrentWeightUnit()
        loadAccountInfo()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Profile data loaded immediately

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: android.database.sqlite.SQLiteException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Database error loading profile: ${e.message}",
                        isLoading = false,
                    )
            } catch (e: IllegalStateException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Invalid state loading profile: ${e.message}",
                        isLoading = false,
                    )
            } catch (e: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Permission error loading profile: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    private fun observeCurrentMaxes() {
        viewModelScope.launch {
            repository.getAllCurrentMaxesWithNames().collect { maxes ->
                _uiState.value =
                    _uiState.value.copy(
                        currentMaxes =
                            maxes.map { max ->
                                ExerciseMaxWithName(
                                    id = max.id,
                                    exerciseId = max.exerciseVariationId,
                                    exerciseName = max.exerciseName,
                                    oneRMEstimate = max.oneRMEstimate,
                                    oneRMDate = max.oneRMDate,
                                    oneRMContext = max.oneRMContext,
                                    oneRMType = max.oneRMType,
                                    notes = max.notes,
                                    sessionCount = max.sessionCount,
                                )
                            },
                    )
            }
        }
    }

    private fun observeBig4AndOtherExercises() {
        viewModelScope.launch {
            // Get Big 4 exercises
            launch {
                repository.getBig4ExercisesWithMaxes().collect { big4 ->
                    _uiState.value =
                        _uiState.value.copy(
                            big4Exercises =
                                big4.map { max ->
                                    Big4Exercise(
                                        exerciseId = max.exerciseVariationId,
                                        exerciseName = max.exerciseName,
                                        oneRMValue = max.oneRMEstimate,
                                        oneRMType = max.oneRMType,
                                        oneRMContext = max.oneRMContext,
                                        oneRMDate = max.oneRMDate,
                                        sessionCount = max.sessionCount,
                                    )
                                },
                        )
                }
            }

            // Get other exercises
            launch {
                repository.getOtherExercisesWithMaxes().collect { others ->
                    _uiState.value =
                        _uiState.value.copy(
                            otherExercises =
                                others.map { max ->
                                    ExerciseMaxWithName(
                                        id = max.id,
                                        exerciseId = max.exerciseVariationId,
                                        exerciseName = max.exerciseName,
                                        oneRMEstimate = max.oneRMEstimate,
                                        oneRMDate = max.oneRMDate,
                                        oneRMContext = max.oneRMContext,
                                        oneRMType = max.oneRMType,
                                        notes = max.notes,
                                        sessionCount = max.sessionCount,
                                    )
                                },
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

    fun update1RM(
        exerciseId: Long,
        exerciseName: String,
        newMax: Float,
    ) {
        viewModelScope.launch {
            try {
                repository.upsertExerciseMax(
                    exerciseVariationId = exerciseId,
                    oneRMEstimate = newMax,
                    oneRMContext = "Manually set",
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                )
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Updated 1RM for $exerciseName",
                    )
            } catch (e: android.database.sqlite.SQLiteException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Database error updating 1RM: ${e.message}",
                    )
            } catch (e: IllegalArgumentException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Invalid 1RM value: ${e.message}",
                    )
            } catch (e: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Permission error updating 1RM: ${e.message}",
                    )
            }
        }
    }

    fun deleteMax(exerciseId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAllMaxesForExercise(exerciseId)
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Deleted 1RM record",
                    )
            } catch (e: android.database.sqlite.SQLiteException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Database error deleting 1RM: ${e.message}",
                    )
            } catch (e: IllegalArgumentException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Invalid exercise ID: ${e.message}",
                    )
            } catch (e: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Permission error deleting 1RM: ${e.message}",
                    )
            }
        }
    }

    fun toggleOneRMSection() {
        _uiState.value =
            _uiState.value.copy(
                isOneRMSectionExpanded = !_uiState.value.isOneRMSectionExpanded,
            )
    }

    fun toggleBig4SubSection() {
        _uiState.value =
            _uiState.value.copy(
                isBig4SubSectionExpanded = !_uiState.value.isBig4SubSectionExpanded,
            )
    }

    fun toggleOtherSubSection() {
        _uiState.value =
            _uiState.value.copy(
                isOtherSubSectionExpanded = !_uiState.value.isOtherSubSectionExpanded,
            )
    }

    // Developer Tools Functions

    fun updateSeedingWeeks(weeks: Int) {
        _uiState.value = _uiState.value.copy(seedingWeeks = weeks.coerceIn(1, 52))
    }

    fun seedWorkoutData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(seedingState = SeedingState.InProgress)
            try {
                val config =
                    WorkoutSeedingService.SeedConfig(
                        numberOfWeeks = _uiState.value.seedingWeeks,
                        workoutsPerWeek = 4,
                        includeAccessories = true,
                    )
                val count = workoutSeedingService.seedRealisticWorkouts(config)
                _uiState.value =
                    _uiState.value.copy(
                        seedingState = SeedingState.Success(count),
                        successMessage = "Successfully created $count workouts",
                    )
            } catch (e: android.database.sqlite.SQLiteException) {
                _uiState.value =
                    _uiState.value.copy(
                        seedingState = SeedingState.Error(e.message),
                        error = "Database error seeding workouts: ${e.message}",
                    )
            } catch (e: IllegalArgumentException) {
                _uiState.value =
                    _uiState.value.copy(
                        seedingState = SeedingState.Error(e.message),
                        error = "Invalid seeding configuration: ${e.message}",
                    )
            } catch (e: IllegalStateException) {
                _uiState.value =
                    _uiState.value.copy(
                        seedingState = SeedingState.Error(e.message),
                        error = "Invalid state for seeding: ${e.message}",
                    )
            } catch (e: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(
                        seedingState = SeedingState.Error(e.message),
                        error = "Permission error seeding workouts: ${e.message}",
                    )
            }
        }
    }

    fun clearAllWorkoutData() {
        viewModelScope.launch {
            try {
                repository.clearAllWorkoutData()
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "All workout data cleared",
                        seedingState = SeedingState.Idle,
                    )
                // Refresh the profile data
                loadProfileData()
            } catch (e: android.database.sqlite.SQLiteException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Database error clearing data: ${e.message}",
                    )
            } catch (e: IllegalStateException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Invalid state clearing data: ${e.message}",
                    )
            } catch (e: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Permission error clearing data: ${e.message}",
                    )
            }
        }
    }

    fun resetSeedingState() {
        _uiState.value = _uiState.value.copy(seedingState = SeedingState.Idle)
    }

    fun toggleDataManagementSection() {
        _uiState.value =
            _uiState.value.copy(
                isDataManagementSectionExpanded = !_uiState.value.isDataManagementSectionExpanded,
            )
    }

    fun exportWorkouts(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ) {
        val context = getApplication<Application>()

        val exportOptions =
            ExportOptions(
                includeBodyweight = true,
                includeOneRepMaxes = true,
                includeNotes = true,
                includeProfile = true,
            )

        val inputData =
            workDataOf(
                "startDate" to startDate.toString(),
                "endDate" to endDate.toString(),
                "includeBodyweight" to exportOptions.includeBodyweight,
                "includeOneRepMaxes" to exportOptions.includeOneRepMaxes,
                "includeNotes" to exportOptions.includeNotes,
                "includeProfile" to exportOptions.includeProfile,
            )

        val exportRequest =
            OneTimeWorkRequestBuilder<ExportWorkoutsWorker>()
                .setInputData(inputData)
                .addTag("workout_export")
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                "workout_export_${System.currentTimeMillis()}",
                ExistingWorkPolicy.KEEP,
                exportRequest,
            )

        // Update UI to show export started
        _uiState.value =
            _uiState.value.copy(
                isExporting = true,
                successMessage = "Export started. You'll be notified when complete.",
            )

        // Observe work status
        WorkManager
            .getInstance(context)
            .getWorkInfoByIdLiveData(exportRequest.id)
            .observeForever { workInfo ->
                when (workInfo?.state) {
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        val filePath = workInfo.outputData.getString("filePath")
                        _uiState.value =
                            _uiState.value.copy(
                                isExporting = false,
                                successMessage = "Export completed successfully!",
                                exportedFilePath = filePath,
                            )
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isExporting = false,
                                error = "Export failed. Please try again.",
                            )
                    }
                    else -> { /* ongoing */ }
                }
            }
    }

    fun clearExportedFile() {
        _uiState.value = _uiState.value.copy(exportedFilePath = null)
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    private fun loadCurrentWeightUnit() {
        val currentUnit = weightUnitManager.getCurrentUnit()
        _uiState.value = _uiState.value.copy(currentWeightUnit = currentUnit)
    }

    fun setWeightUnit(unit: WeightUnit) {
        weightUnitManager.setUnit(unit)
        _uiState.value = _uiState.value.copy(currentWeightUnit = unit)
    }

    private fun loadAccountInfo() {
        val user = firebaseAuth.getCurrentUser()
        _uiState.value =
            _uiState.value.copy(
                accountInfo =
                    if (user != null) {
                        AccountInfo(
                            email = firebaseAuth.getUserEmail(),
                            isEmailVerified = firebaseAuth.isEmailVerified(),
                            authProvider = firebaseAuth.getAuthProvider(),
                            creationTime = firebaseAuth.getAccountCreationTime(),
                        )
                    } else {
                        null
                    },
            )
    }

    fun signOut() {
        firebaseAuth.signOut()
        authManager.clearUserData()
        _uiState.value =
            _uiState.value.copy(
                signOutRequested = true,
                accountInfo = null,
            )
    }

    fun clearSignOutRequest() {
        _uiState.value = _uiState.value.copy(signOutRequested = false)
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            firebaseAuth.sendEmailVerification().fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            successMessage = "Verification email sent to ${firebaseAuth.getUserEmail()}",
                        )
                },
                onFailure = { e ->
                    ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to send verification email", e)
                    _uiState.value =
                        _uiState.value.copy(
                            error = "Failed to send verification email: ${e.message}",
                        )
                },
            )
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
    ) {
        viewModelScope.launch {
            val email = firebaseAuth.getUserEmail()
            if (email == null) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "No email address found for current user",
                    )
                return@launch
            }

            firebaseAuth.reauthenticateWithEmail(email, currentPassword).fold(
                onSuccess = {
                    firebaseAuth.updatePassword(newPassword).fold(
                        onSuccess = {
                            _uiState.value =
                                _uiState.value.copy(
                                    successMessage = "Password changed successfully",
                                )
                        },
                        onFailure = { e ->
                            ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to update password", e)
                            _uiState.value =
                                _uiState.value.copy(
                                    error = "Failed to update password: ${e.message}",
                                )
                        },
                    )
                },
                onFailure = { e ->
                    ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to reauthenticate", e)
                    _uiState.value =
                        _uiState.value.copy(
                            error = "Current password is incorrect",
                        )
                },
            )
        }
    }

    fun resetPassword() {
        viewModelScope.launch {
            val email = firebaseAuth.getUserEmail()
            if (email == null) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "No email address found for current user",
                    )
                return@launch
            }

            firebaseAuth.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            successMessage = "Password reset email sent to $email",
                        )
                },
                onFailure = { e ->
                    ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to send password reset", e)
                    _uiState.value =
                        _uiState.value.copy(
                            error = "Failed to send password reset: ${e.message}",
                        )
                },
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            firebaseAuth.deleteAccount().fold(
                onSuccess = {
                    authManager.clearUserData()
                    _uiState.value =
                        _uiState.value.copy(
                            signOutRequested = true,
                            accountInfo = null,
                            successMessage = "Account deleted successfully",
                        )
                },
                onFailure = { e ->
                    ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to delete account", e)
                    _uiState.value =
                        _uiState.value.copy(
                            error = "Failed to delete account. You may need to sign in again: ${e.message}",
                        )
                },
            )
        }
    }

    fun refreshAccountInfo() {
        viewModelScope.launch {
            firebaseAuth.reloadUser().fold(
                onSuccess = {
                    loadAccountInfo()
                },
                onFailure = { e ->
                    ExceptionLogger.logNonCritical("ProfileViewModel", "Failed to reload user", e)
                },
            )
        }
    }
}
