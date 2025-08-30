package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.util.ExerciseSearchUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExerciseMapping(
    val originalName: String,
    val exerciseId: Long?, // null means create as custom
    val exerciseName: String,
)

data class ExerciseMappingUiState(
    val mappings: Map<String, ExerciseMapping> = emptyMap(),
    val isLoading: Boolean = false,
)

class ExerciseMappingViewModel(
    application: Application,
    private val repository: FeatherweightRepository = FeatherweightRepository(application)
) : AndroidViewModel(application) {
    companion object {
        private const val MAX_SEARCH_RESULTS = 20
    }

    private val _uiState = MutableStateFlow(ExerciseMappingUiState())
    val uiState: StateFlow<ExerciseMappingUiState> = _uiState

    private val _searchResults = MutableStateFlow<List<ExerciseVariation>>(emptyList())
    val searchResults: StateFlow<List<ExerciseVariation>> = _searchResults

    private var allExercises: List<ExerciseVariation> = emptyList()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            allExercises = repository.getAllExercises()
        }
    }

    fun initializeMappings(unmatchedExercises: List<String>) {
        // Initialize empty mappings for all unmatched exercises
        val mappings = _uiState.value.mappings.toMutableMap()
        unmatchedExercises.forEach { exerciseName ->
            if (!mappings.containsKey(exerciseName)) {
                // Don't initialize with any mapping - let user decide
                // mappings[exerciseName] = ExerciseMapping(exerciseName, null, exerciseName)
            }
        }
        _uiState.value = _uiState.value.copy(mappings = mappings)
    }

    fun mapExercise(
        originalName: String,
        exerciseId: Long?,
        exerciseName: String,
    ) {
        val mappings = _uiState.value.mappings.toMutableMap()
        mappings[originalName] = ExerciseMapping(originalName, exerciseId, exerciseName)
        _uiState.value = _uiState.value.copy(mappings = mappings)
    }

    fun clearMapping(exerciseName: String) {
        val mappings = _uiState.value.mappings.toMutableMap()
        mappings.remove(exerciseName)
        _uiState.value = _uiState.value.copy(mappings = mappings)
    }

    fun searchExercises(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        // Use the shared search utility for consistent search behavior
        val results = ExerciseSearchUtil.filterAndSortExercises(
            exercises = allExercises,
            query = query,
            nameExtractor = { it.name }
        ).take(MAX_SEARCH_RESULTS) // Limit results for performance

        _searchResults.value = results
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun allExercisesMapped(unmatchedExercises: List<String>): Boolean =
        unmatchedExercises.all { exerciseName ->
            _uiState.value.mappings.containsKey(exerciseName)
        }

    fun getFinalMappings(): Map<String, Long?> =
        _uiState.value.mappings.mapValues { (_, mapping) ->
            mapping.exerciseId
        }

}
