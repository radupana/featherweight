package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.ProgrammeHistoryDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgrammeHistoryDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    private val _programmeDetails = MutableStateFlow<ProgrammeHistoryDetails?>(null)
    val programmeDetails: StateFlow<ProgrammeHistoryDetails?> = _programmeDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProgrammeDetails(programmeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val details = repository.getProgrammeHistoryDetails(programmeId)
                _programmeDetails.value = details

                if (details == null) {
                    _error.value = "Programme not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load programme details"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
