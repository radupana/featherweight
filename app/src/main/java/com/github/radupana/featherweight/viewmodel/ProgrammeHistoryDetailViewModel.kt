package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.domain.ProgrammeHistoryDetails
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.WorkoutExportService
import com.github.radupana.featherweight.utils.ExportHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgrammeHistoryDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val database = FeatherweightDatabase.getDatabase(application)
    private val authManager = ServiceLocator.provideAuthenticationManager(application)
    private val exportService =
        WorkoutExportService(
            database.workoutDao(),
            database.exerciseLogDao(),
            database.setLogDao(),
            database.oneRMDao(),
            repository,
            authManager,
            ServiceLocator.provideWeightUnitManager(application),
        )
    private val exportHandler = ExportHandler(application)

    companion object {
        private const val TAG = "ProgrammeHistoryDetailViewModel"
    }

    private val _programmeDetails = MutableStateFlow<ProgrammeHistoryDetails?>(null)
    val programmeDetails: StateFlow<ProgrammeHistoryDetails?> = _programmeDetails.asStateFlow()

    private val _completionStats = MutableStateFlow<ProgrammeCompletionStats?>(null)
    val completionStats: StateFlow<ProgrammeCompletionStats?> = _completionStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _pendingExportFile = MutableStateFlow<java.io.File?>(null)
    val pendingExportFile: StateFlow<java.io.File?> = _pendingExportFile.asStateFlow()

    fun loadProgrammeDetails(programmeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val details = repository.getProgrammeHistoryDetails(programmeId)
                _programmeDetails.value = details

                if (details == null) {
                    _error.value = "Programme not found"
                } else {
                    // Also load completion stats for completed programmes
                    val stats = repository.calculateProgrammeCompletionStats(programmeId)
                    _completionStats.value = stats
                }
            } catch (e: IllegalStateException) {
                _error.value = e.message ?: "Failed to load programme details"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportProgramme(programmeId: Long) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f

            try {
                val exportOptions =
                    ExportOptions(
                        includeBodyweight = false,
                        includeOneRepMaxes = true,
                        includeNotes = true,
                        includeProfile = false,
                    )

                val file =
                    exportService.exportProgrammeWorkouts(
                        getApplication(),
                        programmeId,
                        exportOptions,
                    ) { current, total ->
                        _exportProgress.value = current.toFloat() / total.toFloat()
                    }

                _pendingExportFile.value = file
                _isExporting.value = false
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to export programme", e)
                _isExporting.value = false
            } finally {
                _exportProgress.value = 0f
            }
        }
    }

    fun saveExportedFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _pendingExportFile.value?.let { file ->
                try {
                    exportHandler.copyFileContent(file, uri)
                    file.delete()
                    _pendingExportFile.value = null
                } catch (e: java.io.IOException) {
                    Log.e(TAG, "Failed to save exported file", e)
                }
            }
        }
    }

    fun clearPendingExport() {
        _pendingExportFile.value?.delete()
        _pendingExportFile.value = null
    }
}
