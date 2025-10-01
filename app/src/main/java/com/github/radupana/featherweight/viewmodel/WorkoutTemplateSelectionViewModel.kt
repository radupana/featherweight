package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.domain.TemplateSummary
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.WorkoutTemplateRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TemplateWithExercises(
    val summary: TemplateSummary,
    val exerciseNames: List<String>,
)

class WorkoutTemplateSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val templateRepository = WorkoutTemplateRepository(application)
    private val repository = FeatherweightRepository(application)

    private val _templates = MutableStateFlow<List<TemplateWithExercises>>(emptyList())
    val templates: StateFlow<List<TemplateWithExercises>> = _templates.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredTemplates =
        combine(_templates, _searchQuery) { templates, query ->
            if (query.isEmpty()) {
                templates
            } else {
                templates.filter { template ->
                    // Search in template name
                    template.summary.name.contains(query, ignoreCase = true)
                }
            }
        }

    companion object {
        private const val TAG = "TemplateSelectionVM"
    }

    init {
        Log.i(TAG, "ViewModel initialized, loading templates...")
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.i(TAG, "Loading templates...")
            try {
                val templateSummaries = templateRepository.getTemplates()
                Log.i(TAG, "Got ${templateSummaries.size} templates from repository")

                val templatesWithExercises =
                    templateSummaries.map { template ->
                        val exercises = repository.getTemplateExercises(template.id)
                        val exerciseNames =
                            exercises.mapNotNull { exerciseLog ->
                                repository.getExerciseById(exerciseLog.exerciseVariationId)?.name
                            }

                        Log.d(TAG, "Template ${template.id}: exercises=${exerciseNames.joinToString()}")

                        TemplateWithExercises(
                            summary = template,
                            exerciseNames = exerciseNames,
                        )
                    }

                _templates.value = templatesWithExercises
            } catch (e: IllegalArgumentException) {
                ExceptionLogger.logException(TAG, "Failed to load templates", e)
                _templates.value = emptyList()
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException(TAG, "Failed to load templates", e)
                _templates.value = emptyList()
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logException(TAG, "Failed to load templates", e)
                _templates.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.i(TAG, "Template loading complete - count: ${_templates.value.size}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            Log.i(TAG, "Deleting template with ID: $templateId")
            try {
                templateRepository.deleteTemplate(templateId)
                Log.i(TAG, "Template deleted successfully")
                loadTemplates() // Reload the list
            } catch (e: IllegalArgumentException) {
                ExceptionLogger.logException(TAG, "Failed to delete template", e)
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException(TAG, "Failed to delete template", e)
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logException(TAG, "Failed to delete template", e)
            }
        }
    }
}
