package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TemplateWithExercises(
    val summary: WorkoutSummary,
    val exerciseNames: List<String>,
    val description: String? = null,
)

class WorkoutTemplateSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val workoutRepository = WorkoutRepository(application)
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
                    // Search only in template name
                    template.summary.name?.contains(query, ignoreCase = true) == true
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
                val templateWorkouts = workoutRepository.getTemplates()
                Log.i(TAG, "Got ${templateWorkouts.size} templates from repository")

                val templatesWithExercises =
                    templateWorkouts.map { template ->
                        val exercises = repository.getExercisesForWorkout(template.id)
                        val exerciseNames =
                            exercises.mapNotNull { exerciseLog ->
                                repository.getExerciseById(exerciseLog.exerciseVariationId)?.name
                            }

                        // Get workout notes as description
                        val workout = repository.getWorkoutById(template.id)
                        val description = workout?.notes

                        Log.d(TAG, "Template ${template.id}: exercises=${exerciseNames.joinToString()}")

                        TemplateWithExercises(
                            summary = template,
                            exerciseNames = exerciseNames,
                            description = description,
                        )
                    }

                _templates.value = templatesWithExercises
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load templates", e)
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

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            Log.i(TAG, "Deleting template with ID: $templateId")
            try {
                workoutRepository.deleteWorkoutById(templateId)
                Log.i(TAG, "Template deleted successfully")
                loadTemplates() // Reload the list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete template", e)
            }
        }
    }
}
