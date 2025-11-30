package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.repository.WorkoutTemplateRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateTemplateFromWorkoutViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val templateRepository = WorkoutTemplateRepository(application)

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _templateDescription = MutableStateFlow("")
    val templateDescription: StateFlow<String> = _templateDescription.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private var workoutId: String = ""
    private var isInitialized = false

    fun initialize(workoutId: String) {
        this.workoutId = workoutId
        _saveSuccess.value = false
        _templateName.value = ""
        _templateDescription.value = ""
        _isSaving.value = false
        isInitialized = true
    }

    fun isReadyForNavigation(): Boolean = isInitialized && _saveSuccess.value

    fun consumeNavigationEvent() {
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
        if (_templateName.value.isBlank()) return

        if (!isInitialized) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                templateRepository.createTemplateFromWorkout(
                    workoutId = workoutId,
                    templateName = _templateName.value.trim(),
                    templateDescription = _templateDescription.value.trim().takeIf { it.isNotEmpty() },
                )
                _saveSuccess.value = true
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
            }
        }
    }
}
