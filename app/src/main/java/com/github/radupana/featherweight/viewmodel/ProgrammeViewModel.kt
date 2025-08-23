package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ProgrammeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    // UI State
    private val _uiState = MutableStateFlow(ProgrammeUiState())
    val uiState: StateFlow<ProgrammeUiState> = _uiState

    // Programme Templates

    // Active Programme
    private val _activeProgramme = MutableStateFlow<Programme?>(null)
    val activeProgramme: StateFlow<Programme?> = _activeProgramme

    // Programme Progress
    private val _programmeProgress = MutableStateFlow<ProgrammeProgress?>(null)
    val programmeProgress: StateFlow<ProgrammeProgress?> = _programmeProgress

    // All user programmes (including inactive)
    private val _allProgrammes = MutableStateFlow<List<Programme>>(emptyList())
    val allProgrammes: StateFlow<List<Programme>> = _allProgrammes

    // Track if initial load is complete to avoid flashing on refresh
    private var hasLoadedInitialData = false

    // User's 1RM values for setup
    private val _userMaxes = MutableStateFlow(UserMaxes())
    val userMaxes: StateFlow<UserMaxes> = _userMaxes

    // Parse requests
    private val _parseRequests = MutableStateFlow<List<ParseRequest>>(emptyList())
    val parseRequests: StateFlow<List<ParseRequest>> = _parseRequests

    init {
        // Start with immediate loading
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Add a timeout fallback - if loading takes more than 10 seconds, force complete
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 seconds
            if (_uiState.value.isLoading) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Loading timed out. Please try refreshing.",
                    )
            }
        }

        // Load data immediately
        loadProgrammeData()

        // Observe parse requests
        observeParseRequests()
    }

    // Public method to refresh programme progress
    fun refreshProgrammeProgress() {
        viewModelScope.launch {
            val active = _activeProgramme.value
            if (active != null) {
                val progress = repository.getProgrammeWithDetails(active.id)?.progress
                _programmeProgress.value = progress
            }
        }
    }

    private fun loadProgrammeData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            try {
                // Seed database if needed
                repository.seedDatabaseIfEmpty()

                // Load active programme
                val active = repository.getActiveProgramme()
                _activeProgramme.value = active

                // Load progress if there's an active programme
                if (active != null) {
                    val progress = repository.getProgrammeWithDetails(active.id)?.progress
                    _programmeProgress.value = progress
                }

                // Load all programmes (including inactive)
                val allProgs = repository.getAllProgrammes()
                _allProgrammes.value = allProgs

                // Force update the UI state to ensure loading is false
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                    )
                hasLoadedInitialData = true
            } catch (e: SQLiteException) {
                Log.e("ProgrammeViewModel", "Database error loading programmes", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            } catch (e: IOException) {
                Log.e("ProgrammeViewModel", "IO error loading programmes", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    fun updateUserMaxes(maxes: UserMaxes) {
        _userMaxes.value = maxes
    }

    fun dismissSetupDialog() {
        _uiState.value =
            _uiState.value.copy(
                showSetupDialog = false,
                error = null,
            )
        _userMaxes.value = UserMaxes()
    }

    // Removed deactivateActiveProgramme and reactivateProgramme - we only support delete now

    suspend fun getInProgressWorkoutCount(programme: Programme): Int = repository.getInProgressWorkoutCountByProgramme(programme.id)

    fun deleteProgramme(programme: Programme) {
        viewModelScope.launch {
            try {
                repository.deleteProgramme(programme)
                // Programme deleted successfully - no notification needed
                // Refresh data to update the UI
                loadProgrammeData()
            } catch (e: SQLiteException) {
                Log.e("ProgrammeViewModel", "Database error deleting programme", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to delete programme: ${e.message}",
                    )
            }
        }
    }

    fun clearMessages() {
        _uiState.value =
            _uiState.value.copy(
                error = null,
                successMessage = null,
            )
    }

    fun refreshData() {
        // Only refresh if we've already loaded initial data to avoid duplicate loading
        if (hasLoadedInitialData) {
            // Don't show loading state on refresh to avoid flash
            loadProgrammeData(showLoading = false)
        }
    }

    private fun observeParseRequests() {
        viewModelScope.launch {
            repository.getAllParseRequests().collect { requests ->
                // Filter out IMPORTED requests - those have already been converted to programmes
                _parseRequests.value = requests.filter { it.status != ParseStatus.IMPORTED }
            }
        }
    }

    fun deleteParseRequest(request: ParseRequest) {
        viewModelScope.launch {
            try {
                Log.d("ProgrammeViewModel", "Attempting to delete parse request: ${request.id}")
                repository.deleteParseRequest(request)

                // Force immediate UI update by removing from local state
                _parseRequests.value = _parseRequests.value.filter { it.id != request.id }
                Log.d("ProgrammeViewModel", "Parse request ${request.id} deleted and removed from UI")

                // Show success feedback
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Parse request deleted",
                    )

                // Clear message after 2 seconds
                kotlinx.coroutines.delay(2000)
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = null,
                    )
            } catch (e: SQLiteException) {
                Log.e("ProgrammeViewModel", "Database error deleting parse request ${request.id}", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to delete: ${e.message}",
                    )
            }
        }
    }
}

// Data classes for UI state
data class ProgrammeUiState(
    val showSetupDialog: Boolean = false,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showOverwriteWarning: Boolean = false,
    val showProfileUpdatePrompt: Boolean = false,
    val pendingProfileUpdates: List<Pair<String, Float>> = emptyList(),
    val pendingNavigationCallback: (() -> Unit)? = null,
)

data class UserMaxes(
    val squat: Float? = null,
    val bench: Float? = null,
    val deadlift: Float? = null,
    val ohp: Float? = null,
) {
    fun isValid(requiresMaxes: Boolean): Boolean =
        if (requiresMaxes) {
            squat != null &&
                squat > 0 &&
                bench != null &&
                bench > 0 &&
                deadlift != null &&
                deadlift > 0 &&
                ohp != null &&
                ohp > 0
        } else {
            true
        }
}
