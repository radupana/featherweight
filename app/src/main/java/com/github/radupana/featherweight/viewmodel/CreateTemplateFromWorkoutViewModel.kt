package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.WorkoutRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateTemplateFromWorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val workoutRepository = WorkoutRepository(application)

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _templateDescription = MutableStateFlow("")
    val templateDescription: StateFlow<String> = _templateDescription.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private var workoutId: Long = 0
    private var isInitialized = false

    fun initialize(workoutId: Long) {
        android.util.Log.i("CreateTemplateVM", "Initializing with workoutId: $workoutId")
        android.util.Log.i("CreateTemplateVM", "Previous saveSuccess state: ${_saveSuccess.value}")
        android.util.Log.i("CreateTemplateVM", "Previous isInitialized state: $isInitialized")

        this.workoutId = workoutId
        // Reset states for new template creation
        _saveSuccess.value = false
        _templateName.value = ""
        _templateDescription.value = ""
        _isSaving.value = false
        isInitialized = true

        android.util.Log.i("CreateTemplateVM", "Reset all states for new template creation")
        android.util.Log.i("CreateTemplateVM", "New saveSuccess state: ${_saveSuccess.value}")
        android.util.Log.i("CreateTemplateVM", "isInitialized set to: $isInitialized")
    }

    fun isReadyForNavigation(): Boolean {
        val ready = isInitialized && _saveSuccess.value
        android.util.Log.i("CreateTemplateVM", "isReadyForNavigation check - isInitialized: $isInitialized, saveSuccess: ${_saveSuccess.value}, ready: $ready")
        return ready
    }

    fun consumeNavigationEvent() {
        android.util.Log.i("CreateTemplateVM", "consumeNavigationEvent called - resetting saveSuccess and isInitialized")
        _saveSuccess.value = false
        isInitialized = false
    }

    fun updateTemplateName(name: String) {
        _templateName.value = name
    }

    fun updateTemplateDescription(description: String) {
        _templateDescription.value = description
    }

    fun saveTemplate() {
        android.util.Log.i("CreateTemplateVM", "saveTemplate called - name: '${_templateName.value}', workoutId: $workoutId, isInitialized: $isInitialized")
        if (_templateName.value.isBlank()) {
            android.util.Log.w("CreateTemplateVM", "Template name is blank, returning")
            return
        }

        if (!isInitialized) {
            android.util.Log.e("CreateTemplateVM", "saveTemplate called but ViewModel not initialized! This shouldn't happen")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            android.util.Log.i("CreateTemplateVM", "Starting template creation for workoutId: $workoutId with name: '${_templateName.value.trim()}'")
            try {
                val templateId =
                    workoutRepository.createTemplateFromWorkout(
                        workoutId = workoutId,
                        templateName = _templateName.value.trim(),
                        templateDescription = _templateDescription.value.trim().takeIf { it.isNotEmpty() },
                    )
                android.util.Log.i("CreateTemplateVM", "Template created successfully with ID: $templateId")
                android.util.Log.i("CreateTemplateVM", "Setting saveSuccess to true (was: ${_saveSuccess.value})")
                _saveSuccess.value = true
                android.util.Log.i("CreateTemplateVM", "saveSuccess is now: ${_saveSuccess.value}, isInitialized: $isInitialized")
            } catch (e: IllegalArgumentException) {
                ExceptionLogger.logException("CreateTemplateVM", "Failed to create template for workoutId: $workoutId", e)
                _saveSuccess.value = false
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException("CreateTemplateVM", "Failed to create template for workoutId: $workoutId", e)
                _saveSuccess.value = false
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logException("CreateTemplateVM", "Failed to create template for workoutId: $workoutId", e)
                _saveSuccess.value = false
            } finally {
                _isSaving.value = false
                android.util.Log.i("CreateTemplateVM", "saveTemplate complete - success: ${_saveSuccess.value}, isInitialized: $isInitialized")
            }
        }
    }
}
