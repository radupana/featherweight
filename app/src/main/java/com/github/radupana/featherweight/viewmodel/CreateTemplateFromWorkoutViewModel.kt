package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateTemplateFromWorkoutViewModel(application: Application) : AndroidViewModel(application) {
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

    fun initialize(workoutId: Long) {
        this.workoutId = workoutId
    }

    fun updateTemplateName(name: String) {
        _templateName.value = name
    }

    fun updateTemplateDescription(description: String) {
        _templateDescription.value = description
    }

    fun saveTemplate() {
        android.util.Log.i("CreateTemplateVM", "saveTemplate called - name: '${_templateName.value}', workoutId: $workoutId")
        if (_templateName.value.isBlank()) {
            android.util.Log.w("CreateTemplateVM", "Template name is blank, returning")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            android.util.Log.i("CreateTemplateVM", "Starting template creation...")
            try {
                val templateId = workoutRepository.createTemplateFromWorkout(
                    workoutId = workoutId,
                    templateName = _templateName.value.trim(),
                    templateDescription = _templateDescription.value.trim().takeIf { it.isNotEmpty() },
                )
                android.util.Log.i("CreateTemplateVM", "Template created successfully with ID: $templateId")
                _saveSuccess.value = true
            } catch (e: Exception) {
                android.util.Log.e("CreateTemplateVM", "Failed to create template", e)
            } finally {
                _isSaving.value = false
                android.util.Log.i("CreateTemplateVM", "saveTemplate complete - success: ${_saveSuccess.value}")
            }
        }
    }
}
