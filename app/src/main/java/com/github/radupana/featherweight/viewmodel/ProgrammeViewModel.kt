package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import com.github.radupana.featherweight.util.CloudLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ProgrammeViewModel(
    application: Application,
    providedRepository: FeatherweightRepository? = null,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ProgrammeViewModel"
    }

    private val repository = providedRepository ?: FeatherweightRepository(application)

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

    // Track if initial load is complete to avoid flashing on refresh
    private var hasLoadedInitialData = false

    // Parse requests
    private val _parseRequests = MutableStateFlow<List<ParseRequest>>(emptyList())
    val parseRequests: StateFlow<List<ParseRequest>> = _parseRequests

    // Next workout info
    private val _nextWorkoutInfo = MutableStateFlow<NextProgrammeWorkoutInfo?>(null)
    val nextWorkoutInfo: StateFlow<NextProgrammeWorkoutInfo?> = _nextWorkoutInfo

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
                // Also refresh next workout info
                loadNextWorkoutInfo()
            }
        }
    }

    // Load next workout info
    fun loadNextWorkoutInfo() {
        viewModelScope.launch {
            val active = _activeProgramme.value
            if (active != null) {
                _nextWorkoutInfo.value = repository.getNextProgrammeWorkout(active.id)
            } else {
                _nextWorkoutInfo.value = null
            }
        }
    }

    private fun loadProgrammeData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            try {
                val startTime = System.currentTimeMillis()

                // Load active programme
                val active = repository.getActiveProgramme()
                _activeProgramme.value = active

                // Load progress if there's an active programme
                if (active != null) {
                    CloudLogger.debug(TAG, "Active programme loaded: ${active.name} (id: ${active.id})")
                    val progress = repository.getProgrammeWithDetails(active.id)?.progress
                    _programmeProgress.value = progress
                    // Load next workout info
                    _nextWorkoutInfo.value = repository.getNextProgrammeWorkout(active.id)
                }

                // Load all programmes (including inactive)
                val allProgs = repository.getAllProgrammes()

                CloudLogger.debug(
                    TAG,
                    "loadProgrammeData took ${System.currentTimeMillis() - startTime}ms - " +
                        "active: ${active?.name ?: "none"}, total: ${allProgs.size}, " +
                        "hasProgress: ${_programmeProgress.value != null}",
                )

                // Force update the UI state to ensure loading is false
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                    )
                hasLoadedInitialData = true
            } catch (e: SQLiteException) {
                CloudLogger.error(TAG, "Database error loading programmes", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            } catch (e: IOException) {
                CloudLogger.error(TAG, "IO error loading programmes", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    suspend fun getInProgressWorkoutCount(programme: Programme): Int = repository.getInProgressWorkoutCountByProgramme(programme.id)

    suspend fun getCompletedWorkoutCount(programme: Programme): Int = repository.getCompletedWorkoutCountForProgramme(programme.id)

    suspend fun getCompletedSetCount(programme: Programme): Int = repository.getCompletedSetCountForProgramme(programme.id)

    fun deleteProgramme(
        programme: Programme,
        deleteWorkouts: Boolean,
    ) {
        viewModelScope.launch {
            try {
                val action = if (deleteWorkouts) "Deleting" else "Archiving"
                CloudLogger.info(
                    TAG,
                    "$action programme - id: ${programme.id}, name: ${programme.name}, " +
                        "isActive: ${programme.isActive}, deleteWorkouts: $deleteWorkouts",
                )
                repository.deleteProgramme(programme, deleteWorkouts)
                loadProgrammeData()
                CloudLogger.info(TAG, "Programme ${if (deleteWorkouts) "deleted" else "archived"} successfully: ${programme.name}")
            } catch (e: SQLiteException) {
                CloudLogger.error(TAG, "Database error ${if (deleteWorkouts) "deleting" else "archiving"} programme: ${programme.name}", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to ${if (deleteWorkouts) "delete" else "archive"} programme: ${e.message}",
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
                CloudLogger.debug("ProgrammeViewModel", "Attempting to delete parse request: ${request.id}")
                repository.deleteParseRequest(request)

                // Force immediate UI update by removing from local state
                _parseRequests.value = _parseRequests.value.filter { it.id != request.id }
                CloudLogger.debug("ProgrammeViewModel", "Parse request ${request.id} deleted and removed from UI")

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
                CloudLogger.error("ProgrammeViewModel", "Database error deleting parse request ${request.id}", e)
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
