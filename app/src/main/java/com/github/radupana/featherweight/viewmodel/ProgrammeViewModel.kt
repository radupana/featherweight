package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeTemplate
import com.github.radupana.featherweight.repository.AIProgrammeRepository
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProgrammeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val aiProgrammeRepository =
        AIProgrammeRepository(
            database.aiProgrammeRequestDao(),
            WorkManager.getInstance(application),
        )

    // UI State
    private val _uiState = MutableStateFlow(ProgrammeUiState())
    val uiState: StateFlow<ProgrammeUiState> = _uiState

    // Programme Templates
    private val _allTemplates = MutableStateFlow<List<ProgrammeTemplate>>(emptyList())
    private val _filteredTemplates = MutableStateFlow<List<ProgrammeTemplate>>(emptyList())

    // Active Programme
    private val _activeProgramme = MutableStateFlow<Programme?>(null)
    val activeProgramme: StateFlow<Programme?> = _activeProgramme

    // Programme Progress
    private val _programmeProgress = MutableStateFlow<ProgrammeProgress?>(null)
    val programmeProgress: StateFlow<ProgrammeProgress?> = _programmeProgress

    // All user programmes (including inactive)
    private val _allProgrammes = MutableStateFlow<List<Programme>>(emptyList())
    val allProgrammes: StateFlow<List<Programme>> = _allProgrammes

    // AI programme requests
    val aiProgrammeRequests = aiProgrammeRepository.getAllRequests()

    // Track if initial load is complete to avoid flashing on refresh
    private var hasLoadedInitialData = false

    // User's 1RM values for setup
    private val _userMaxes = MutableStateFlow(UserMaxes())
    val userMaxes: StateFlow<UserMaxes> = _userMaxes

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

        // Cleanup stale AI requests on startup
        viewModelScope.launch {
            aiProgrammeRepository.cleanupStaleRequests()
        }

        // Monitor AI programme requests flow for debugging
        viewModelScope.launch {
            aiProgrammeRequests.collect { requests ->
            }
        }

        // Update templates directly without filtering
        viewModelScope.launch {
            _allTemplates.collect { templates ->
                _filteredTemplates.value = templates
                _uiState.value =
                    _uiState.value.copy(
                        templates = templates,
                    )
            }
        }

        // Load data immediately
        loadProgrammeData()
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
                // Try to load templates first without seeding
                val templates =
                    try {
                        repository.getAllProgrammeTemplates()
                    } catch (e: Exception) {
                        // Only seed if loading fails
                        repository.seedDatabaseIfEmpty()
                        repository.getAllProgrammeTemplates()
                    }
                _allTemplates.value = templates

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

                // Force update the UI state to ensure loading is false and templates are shown
                _uiState.value =
                    _uiState.value.copy(
                        templates = templates,
                        isLoading = false,
                    )
                hasLoadedInitialData = true
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Error", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    fun selectTemplate(template: ProgrammeTemplate) {
        // Check if there's an active programme
        if (_activeProgramme.value != null) {
            _uiState.value =
                _uiState.value.copy(
                    showOverwriteWarning = true,
                    pendingTemplate = template,
                )
        } else {
            _uiState.value =
                _uiState.value.copy(
                    selectedTemplate = template,
                    showSetupDialog = true,
                    setupStep = if (template.requiresMaxes) SetupStep.MAXES_INPUT else SetupStep.CONFIRMATION,
                )

            // Reset user maxes if template doesn't require them
            if (!template.requiresMaxes) {
                _userMaxes.value = UserMaxes()
            }
        }
    }

    fun confirmOverwriteProgramme() {
        val template = _uiState.value.pendingTemplate ?: return
        _uiState.value =
            _uiState.value.copy(
                selectedTemplate = template,
                showSetupDialog = true,
                setupStep = if (template.requiresMaxes) SetupStep.MAXES_INPUT else SetupStep.CONFIRMATION,
                showOverwriteWarning = false,
                pendingTemplate = null,
            )

        // Reset user maxes if template doesn't require them
        if (!template.requiresMaxes) {
            _userMaxes.value = UserMaxes()
        }
    }

    fun cancelOverwriteProgramme() {
        _uiState.value =
            _uiState.value.copy(
                showOverwriteWarning = false,
                pendingTemplate = null,
            )
    }

    fun updateUserMaxes(maxes: UserMaxes) {
        _userMaxes.value = maxes
    }

    fun nextSetupStep() {
        val currentStep = _uiState.value.setupStep
        val template = _uiState.value.selectedTemplate

        when (currentStep) {
            SetupStep.MAXES_INPUT -> {
                _uiState.value =
                    _uiState.value.copy(
                        setupStep =
                            if (template?.allowsAccessoryCustomization == true) {
                                SetupStep.ACCESSORY_SELECTION
                            } else {
                                SetupStep.CONFIRMATION
                            },
                    )
            }

            SetupStep.ACCESSORY_SELECTION -> {
                _uiState.value = _uiState.value.copy(setupStep = SetupStep.CONFIRMATION)
            }

            SetupStep.CONFIRMATION -> {
                // This should trigger programme creation
            }
        }
    }

    fun previousSetupStep() {
        val currentStep = _uiState.value.setupStep
        val template = _uiState.value.selectedTemplate

        when (currentStep) {
            SetupStep.ACCESSORY_SELECTION -> {
                _uiState.value =
                    _uiState.value.copy(
                        setupStep =
                            if (template?.requiresMaxes == true) {
                                SetupStep.MAXES_INPUT
                            } else {
                                SetupStep.CONFIRMATION
                            },
                    )
            }

            SetupStep.CONFIRMATION -> {
                if (template?.allowsAccessoryCustomization == true) {
                    _uiState.value = _uiState.value.copy(setupStep = SetupStep.ACCESSORY_SELECTION)
                } else if (template?.requiresMaxes == true) {
                    _uiState.value = _uiState.value.copy(setupStep = SetupStep.MAXES_INPUT)
                } else {
                    dismissSetupDialog()
                }
            }

            SetupStep.MAXES_INPUT -> {
                dismissSetupDialog()
            }
        }
    }

    fun dismissSetupDialog() {
        _uiState.value =
            _uiState.value.copy(
                selectedTemplate = null,
                showSetupDialog = false,
                setupStep = SetupStep.MAXES_INPUT,
                error = null,
            )
        _userMaxes.value = UserMaxes()
    }

    fun createProgrammeFromTemplate(
        customName: String? = null,
        accessoryCustomizations: Map<String, String> = emptyMap(),
        onSuccess: (() -> Unit)? = null,
    ) {
        val template = _uiState.value.selectedTemplate ?: return
        val maxes = _userMaxes.value

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)

            try {
                val programmeId =
                    repository.createProgrammeFromTemplate(
                        templateId = template.id,
                        name = customName,
                        squatMax = if (template.requiresMaxes) maxes.squat else null,
                        benchMax = if (template.requiresMaxes) maxes.bench else null,
                        deadliftMax = if (template.requiresMaxes) maxes.deadlift else null,
                        ohpMax = if (template.requiresMaxes) maxes.ohp else null,
                    )

                // Activate the new programme
                repository.activateProgramme(programmeId)

                // Get the newly created and activated programme
                val newActiveProgramme = repository.getActiveProgramme()
                val newProgress =
                    if (newActiveProgramme != null) {
                        repository.getProgrammeWithDetails(newActiveProgramme.id)?.progress
                    } else {
                        null
                    }

                // Update UI state directly instead of reloading everything
                _activeProgramme.value = newActiveProgramme
                _programmeProgress.value = newProgress

                // Close dialog
                dismissSetupDialog()

                _uiState.value =
                    _uiState.value.copy(
                        isCreating = false,
                    )

                // Check if we should update profile 1RMs BEFORE navigation
                if (template.requiresMaxes) {
                    val hasUpdates = checkAndPromptForProfileUpdate(maxes)
                    if (hasUpdates) {
                        // Store the success callback to execute after profile update
                        _uiState.value =
                            _uiState.value.copy(
                                pendingNavigationCallback = onSuccess,
                            )
                        return@launch // Don't navigate yet
                    }
                }

                // No profile updates needed, navigate immediately
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e("ProgrammeViewModel", "Error", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to create programme: ${e.message}",
                        isCreating = false,
                    )
            }
        }
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
            } catch (e: Exception) {
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

    fun forceRefreshAIRequests() {
        // Force refresh AI requests when returning from preview
        viewModelScope.launch {
            // This will trigger the Flow to re-emit
            aiProgrammeRepository.cleanupStaleRequests()
        }
    }

    private suspend fun checkAndPromptForProfileUpdate(
        enteredMaxes: UserMaxes,
    ): Boolean {
        val userId = repository.getCurrentUserId()
        val currentMaxes = repository.getAllCurrentMaxesWithNames(userId).first()
        val updates = mutableListOf<Pair<String, Float>>()

        // Check Squat - compare against actual value, not just null
        enteredMaxes.squat?.let { newSquat ->
            val currentSquat = currentMaxes.find { it.exerciseName == "Barbell Back Squat" }?.oneRMEstimate
            if (currentSquat != newSquat) {
                updates.add("Barbell Back Squat" to newSquat)
            }
        }

        // Check Bench - compare against actual value, not just null
        enteredMaxes.bench?.let { newBench ->
            val currentBench = currentMaxes.find { it.exerciseName == "Barbell Bench Press" }?.oneRMEstimate
            if (currentBench != newBench) {
                updates.add("Barbell Bench Press" to newBench)
            }
        }

        // Check Deadlift - compare against actual value, not just null
        enteredMaxes.deadlift?.let { newDeadlift ->
            val currentDeadlift = currentMaxes.find { it.exerciseName == "Barbell Deadlift" }?.oneRMEstimate
            if (currentDeadlift != newDeadlift) {
                updates.add("Barbell Deadlift" to newDeadlift)
            }
        }

        // Check OHP - compare against actual value, not just null
        enteredMaxes.ohp?.let { newOhp ->
            val currentOhp = currentMaxes.find { it.exerciseName == "Barbell Overhead Press" }?.oneRMEstimate
            if (currentOhp != newOhp) {
                updates.add("Barbell Overhead Press" to newOhp)
            }
        }

        // If there are updates, show prompt
        if (updates.isNotEmpty()) {
            _uiState.value =
                _uiState.value.copy(
                    showProfileUpdatePrompt = true,
                    pendingProfileUpdates = updates,
                )
            return true
        }
        return false
    }

    fun confirmProfileUpdate() {
        viewModelScope.launch {
            val updates = _uiState.value.pendingProfileUpdates

            // Wait for all updates to complete
            val updateJobs =
                updates.map { (exerciseName, newMax) ->
                    async {
                        // Call the suspend function directly instead of launching another coroutine
                        updateExerciseMaxDirectly(exerciseName, newMax)
                    }
                }

            // Wait for all updates to complete
            updateJobs.awaitAll()

            // Execute pending navigation if any
            val callback = _uiState.value.pendingNavigationCallback
            dismissProfileUpdatePrompt()
            callback?.invoke()
        }
    }

    private suspend fun updateExerciseMaxDirectly(
        exerciseName: String,
        maxWeight: Float,
    ) {
        try {
            val exercise = repository.getExerciseByName(exerciseName)
            if (exercise != null) {
                repository.upsertExerciseMax(
                    userId = repository.getCurrentUserId(),
                    exerciseVariationId = exercise.id,
                    oneRMEstimate = maxWeight,
                    oneRMContext = "Manually set",
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                    notes = "Updated from programme setup",
                )
                Log.d("ProgrammeViewModel", "Successfully updated 1RM for $exerciseName")
            } else {
                Log.w("ProgrammeViewModel", "Exercise not found for name: $exerciseName")
            }
        } catch (e: Exception) {
            Log.e("ProgrammeViewModel", "Error updating 1RM: ${e.message}", e)
        }
    }

    fun dismissProfileUpdatePrompt() {
        // Execute pending navigation even if user skips update
        val callback = _uiState.value.pendingNavigationCallback

        _uiState.value =
            _uiState.value.copy(
                showProfileUpdatePrompt = false,
                pendingProfileUpdates = emptyList(),
                pendingNavigationCallback = null,
            )

        callback?.invoke()
    }

    // Public methods for AI programme management
    fun retryAIGeneration(requestId: String) {
        viewModelScope.launch {
            aiProgrammeRepository.retryGeneration(requestId)
        }
    }

    fun deleteAIRequest(requestId: String) {
        viewModelScope.launch {
            aiProgrammeRepository.deleteRequest(requestId)
        }
    }

    fun previewAIProgramme(
        requestId: String,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val request = aiProgrammeRepository.getRequestById(requestId)
                if (request != null && request.generatedProgrammeJson != null) {
                    // Parse the generated programme JSON
                    val aiService =
                        com.github.radupana.featherweight.service
                            .AIProgrammeService(getApplication())
                    val response = aiService.parseAIProgrammeResponse(request.generatedProgrammeJson)

                    if (response.programme != null) {
                        // Store in holder for preview screen with request ID
                        GeneratedProgrammeHolder.setGeneratedProgramme(response, requestId)

                        // Validation will be done in ProgrammePreviewViewModel
                        onResult(true)
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                error = "Failed to parse programme data",
                            )
                        onResult(false)
                    }
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            error = "Programme not found or still generating",
                        )
                    onResult(false)
                }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Error loading programme: ${e.message}",
                    )
                onResult(false)
            }
        }
    }

    fun submitClarification(
        requestId: String,
        clarificationText: String,
    ) {
        viewModelScope.launch {
            try {
                aiProgrammeRepository.submitClarification(requestId, clarificationText)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to submit clarification: ${e.message}",
                    )
            }
        }
    }
}

// Data classes for UI state
data class ProgrammeUiState(
    val templates: List<ProgrammeTemplate> = emptyList(),
    val selectedTemplate: ProgrammeTemplate? = null,
    val showSetupDialog: Boolean = false,
    val setupStep: SetupStep = SetupStep.MAXES_INPUT,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showOverwriteWarning: Boolean = false,
    val pendingTemplate: ProgrammeTemplate? = null,
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

enum class SetupStep {
    MAXES_INPUT,
    ACCESSORY_SELECTION,
    CONFIRMATION,
}
