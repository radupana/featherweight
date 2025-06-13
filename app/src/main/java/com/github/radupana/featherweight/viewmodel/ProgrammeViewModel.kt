package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.programme.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProgrammeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

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

    // User's 1RM values for setup
    private val _userMaxes = MutableStateFlow(UserMaxes())
    val userMaxes: StateFlow<UserMaxes> = _userMaxes

    // Filter state
    private val _selectedDifficulty = MutableStateFlow<ProgrammeDifficulty?>(null)
    private val _selectedType = MutableStateFlow<ProgrammeType?>(null)
    private val _searchText = MutableStateFlow("")

    init {
        println("🔄 ProgrammeViewModel: Initializing...")
        // Start with immediate loading
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Add a timeout fallback - if loading takes more than 10 seconds, force complete
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 seconds
            if (_uiState.value.isLoading) {
                println("⚠️ ProgrammeViewModel: Loading timeout - forcing completion")
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Loading timed out. Please try refreshing.",
                    )
            }
        }

        // Combine filter states to update filtered templates
        viewModelScope.launch {
            combine(
                _allTemplates,
                _selectedDifficulty,
                _selectedType,
                _searchText,
            ) { templates, difficulty, type, searchText ->
                println("🔄 ProgrammeViewModel: Combine flow triggered with ${templates.size} templates")
                filterTemplates(templates, difficulty, type, searchText)
            }.collect { filtered ->
                println("✅ ProgrammeViewModel: Combine flow collected ${filtered.size} filtered templates")
                _filteredTemplates.value = filtered
                // Only update templates, don't manage loading state here
                _uiState.value =
                    _uiState.value.copy(
                        templates = filtered,
                        searchText = _searchText.value,
                    )
                println("✅ ProgrammeViewModel: Templates updated in UI state")
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
                println("🔄 Refreshing programme progress for: ${active.name}")
                val progress = repository.getProgrammeWithDetails(active.id)?.progress
                _programmeProgress.value = progress
                println("✅ Programme progress refreshed: ${progress?.completedWorkouts}/${progress?.totalWorkouts}")
            }
        }
    }

    private fun filterTemplates(
        templates: List<ProgrammeTemplate>,
        difficulty: ProgrammeDifficulty?,
        type: ProgrammeType?,
        searchText: String,
    ): List<ProgrammeTemplate> {
        return templates.filter { template ->
            val matchesDifficulty = difficulty == null || template.difficulty == difficulty
            val matchesType = type == null || template.programmeType == type
            val matchesSearch =
                searchText.isBlank() ||
                    template.name.contains(searchText, ignoreCase = true) ||
                    template.description.contains(searchText, ignoreCase = true) ||
                    template.author.contains(searchText, ignoreCase = true)

            matchesDifficulty && matchesType && matchesSearch
        }
    }

    private fun loadProgrammeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            println("🔄 ProgrammeViewModel: Starting data load...")

            try {
                // Try to load templates first without seeding
                println("🔄 ProgrammeViewModel: Loading templates...")
                val templates =
                    try {
                        repository.getAllProgrammeTemplates()
                    } catch (e: Exception) {
                        println("❌ Error loading templates, trying to seed: ${e.message}")
                        // Only seed if loading fails
                        repository.seedDatabaseIfEmpty()
                        repository.getAllProgrammeTemplates()
                    }
                println("✅ ProgrammeViewModel: Loaded ${templates.size} templates")
                _allTemplates.value = templates

                // Load active programme
                println("🔄 ProgrammeViewModel: Loading active programme...")
                val active = repository.getActiveProgramme()
                println("✅ ProgrammeViewModel: Active programme: ${active?.name ?: "None"}")
                _activeProgramme.value = active

                // Load progress if there's an active programme
                if (active != null) {
                    println("🔄 ProgrammeViewModel: Loading progress...")
                    val progress = repository.getProgrammeWithDetails(active.id)?.progress
                    println("✅ ProgrammeViewModel: Progress loaded")
                    _programmeProgress.value = progress
                }

                // Load all programmes (including inactive)
                println("🔄 ProgrammeViewModel: Loading all programmes...")
                val allProgs = repository.getAllProgrammes()
                println("✅ ProgrammeViewModel: Loaded ${allProgs.size} programmes")
                _allProgrammes.value = allProgs

                println("✅ ProgrammeViewModel: Data loading complete")

                // Force update the UI state to ensure loading is false and templates are shown
                val filteredTemplates = filterTemplates(templates, null, null, "")
                _uiState.value =
                    _uiState.value.copy(
                        templates = filteredTemplates,
                        isLoading = false,
                    )
                println("✅ ProgrammeViewModel: UI state updated with ${filteredTemplates.size} templates, isLoading=false")
            } catch (e: Exception) {
                println("❌ ProgrammeViewModel: Error loading data: ${e.message}")
                e.printStackTrace()
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to load programmes: ${e.message}",
                        isLoading = false,
                    )
            }
        }
    }

    fun filterByDifficulty(difficulty: ProgrammeDifficulty?) {
        _selectedDifficulty.value = difficulty
    }

    fun filterByType(type: ProgrammeType?) {
        _selectedType.value = type
    }

    fun clearFilters() {
        _selectedDifficulty.value = null
        _selectedType.value = null
        _searchText.value = ""
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
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
                println("🔄 Creating programme from template: ${template.name}")
                val programmeId =
                    repository.createProgrammeFromTemplate(
                        templateId = template.id,
                        name = customName,
                        squatMax = if (template.requiresMaxes) maxes.squat else null,
                        benchMax = if (template.requiresMaxes) maxes.bench else null,
                        deadliftMax = if (template.requiresMaxes) maxes.deadlift else null,
                        ohpMax = if (template.requiresMaxes) maxes.ohp else null,
                        accessoryCustomizations = accessoryCustomizations,
                    )
                println("✅ Programme created with ID: $programmeId")

                // Activate the new programme
                println("🔄 Activating programme...")
                repository.activateProgramme(programmeId)
                println("✅ Programme activated")

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
                        successMessage = "Programme '${customName ?: template.name}' created and activated!",
                    )
                println("✅ Programme creation completed successfully")

                // Call success callback to navigate
                onSuccess?.invoke()
            } catch (e: Exception) {
                println("❌ Programme creation failed: ${e.message}")
                e.printStackTrace()
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to create programme: ${e.message}",
                        isCreating = false,
                    )
            }
        }
    }

    fun deactivateActiveProgramme() {
        viewModelScope.launch {
            try {
                repository.deactivateActiveProgramme()
                _activeProgramme.value = null
                _programmeProgress.value = null

                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Programme deactivated",
                    )

                // Reload to show deactivated programme
                loadProgrammeData()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to deactivate programme: ${e.message}",
                    )
            }
        }
    }

    fun reactivateProgramme(programme: Programme) {
        viewModelScope.launch {
            try {
                repository.activateProgramme(programme.id)
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Programme '${programme.name}' reactivated!",
                    )
                // Reload data to update UI
                loadProgrammeData()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to reactivate programme: ${e.message}",
                    )
            }
        }
    }

    fun deleteProgramme(programme: Programme) {
        viewModelScope.launch {
            try {
                repository.deleteProgramme(programme)
                _uiState.value =
                    _uiState.value.copy(
                        successMessage = "Programme '${programme.name}' deleted successfully",
                    )
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
        loadProgrammeData()
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
    val searchText: String = "",
    val showOverwriteWarning: Boolean = false,
    val pendingTemplate: ProgrammeTemplate? = null,
)

data class UserMaxes(
    val squat: Float? = null,
    val bench: Float? = null,
    val deadlift: Float? = null,
    val ohp: Float? = null,
) {
    fun isValid(requiresMaxes: Boolean): Boolean {
        return if (requiresMaxes) {
            squat != null && squat > 0 &&
                bench != null && bench > 0 &&
                deadlift != null && deadlift > 0 &&
                ohp != null && ohp > 0
        } else {
            true
        }
    }
}

enum class SetupStep {
    MAXES_INPUT,
    ACCESSORY_SELECTION,
    CONFIRMATION,
}
