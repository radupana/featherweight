package com.github.radupana.featherweight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgrammeCompletionUiState(
    val isLoading: Boolean = true,
    val completionStats: ProgrammeCompletionStats? = null,
    val error: String? = null,
)

class ProgrammeCompletionViewModel(
    private val repository: FeatherweightRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgrammeCompletionUiState())
    val uiState: StateFlow<ProgrammeCompletionUiState> = _uiState.asStateFlow()

    fun loadProgrammeCompletionStats(programmeId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val stats = repository.calculateProgrammeCompletionStats(programmeId)
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        completionStats = stats,
                        error = null,
                    )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message,
                    )
            }
        }
    }

    fun saveProgrammeNotes(
        programmeId: Long,
        notes: String,
    ) {
        viewModelScope.launch {
            try {
                repository.updateProgrammeCompletionNotes(programmeId, notes)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        error = "Failed to save notes: ${e.message}",
                    )
            }
        }
    }
}
