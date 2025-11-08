package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExerciseSearchUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExerciseMapping(
    val originalName: String,
    val exerciseId: String?, // null means create as custom
    val exerciseName: String,
)

data class ExerciseMappingUiState(
    val mappings: Map<String, ExerciseMapping> = emptyMap(),
    val suggestions: Map<String, List<Exercise>> = emptyMap(),
    val isLoading: Boolean = false,
)

class ExerciseMappingViewModel(
    application: Application,
    repository: FeatherweightRepository? = null,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        FeatherweightRepository(application),
    )

    companion object {
        private const val TAG = "ExerciseMappingViewModel"
        private const val MAX_SEARCH_RESULTS = 20
        private const val MAX_SUGGESTIONS = 3
        private const val AUTO_PROPOSE_THRESHOLD = 500
    }

    private val repository = repository ?: FeatherweightRepository(application)
    private val _uiState = MutableStateFlow(ExerciseMappingUiState())
    val uiState: StateFlow<ExerciseMappingUiState> = _uiState

    private val _searchResults = MutableStateFlow<List<Exercise>>(emptyList())
    val searchResults: StateFlow<List<Exercise>> = _searchResults

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var allExercises: List<Exercise> = emptyList()
    private var allExercisesWithAliases: List<com.github.radupana.featherweight.data.exercise.ExerciseWithAliases> = emptyList()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            allExercises = repository.getAllExercises()
            allExercisesWithAliases = repository.getAllExercisesWithAliases()
        }
    }

    fun initializeMappings(unmatchedExercises: List<String>) {
        viewModelScope.launch {
            CloudLogger.debug(TAG, "=== SMART SUGGESTIONS START ===")
            CloudLogger.debug(TAG, "Unmatched exercises: ${unmatchedExercises.size}")

            if (allExercisesWithAliases.isEmpty()) {
                CloudLogger.debug(TAG, "Loading exercises from database...")
                allExercises = repository.getAllExercises()
                allExercisesWithAliases = repository.getAllExercisesWithAliases()
                CloudLogger.debug(TAG, "Loaded ${allExercisesWithAliases.size} exercises with aliases")
            } else {
                CloudLogger.debug(TAG, "Using cached ${allExercisesWithAliases.size} exercises")
            }

            val suggestionsMap = mutableMapOf<String, List<Exercise>>()
            val autoMappings = mutableMapOf<String, ExerciseMapping>()

            unmatchedExercises.forEach { exerciseName ->
                CloudLogger.debug(TAG, "--- Processing: '$exerciseName' ---")

                val suggestions =
                    ExerciseSearchUtil
                        .filterAndSortExercises(
                            exercises = allExercisesWithAliases,
                            query = exerciseName,
                            nameExtractor = { it.name },
                            aliasExtractor = { it.aliases },
                        ).take(MAX_SUGGESTIONS)
                        .map { it.exercise }

                if (suggestions.isEmpty()) {
                    CloudLogger.debug(TAG, "  No suggestions found for '$exerciseName'")
                } else {
                    CloudLogger.debug(TAG, "  Found ${suggestions.size} suggestion(s):")
                    suggestions.forEachIndexed { index, exercise ->
                        val matchWithAliases = allExercisesWithAliases.firstOrNull { it.exercise.id == exercise.id }
                        val score =
                            if (matchWithAliases != null) {
                                ExerciseSearchUtil.scoreExerciseMatch(
                                    exerciseName = matchWithAliases.name,
                                    query = exerciseName,
                                    aliases = matchWithAliases.aliases,
                                )
                            } else {
                                0
                            }
                        CloudLogger.debug(TAG, "    ${index + 1}. ${exercise.name} (score: $score)")
                    }

                    suggestionsMap[exerciseName] = suggestions

                    val topMatch = allExercisesWithAliases.firstOrNull { it.exercise.id == suggestions.first().id }
                    if (topMatch != null) {
                        val score =
                            ExerciseSearchUtil.scoreExerciseMatch(
                                exerciseName = topMatch.name,
                                query = exerciseName,
                                aliases = topMatch.aliases,
                            )

                        if (score >= AUTO_PROPOSE_THRESHOLD) {
                            CloudLogger.debug(TAG, "  ✓ AUTO-PROPOSING: ${topMatch.name} (score $score >= threshold $AUTO_PROPOSE_THRESHOLD)")
                            autoMappings[exerciseName] =
                                ExerciseMapping(
                                    originalName = exerciseName,
                                    exerciseId = topMatch.id,
                                    exerciseName = topMatch.name,
                                )
                        } else {
                            CloudLogger.debug(TAG, "  ✗ Not auto-proposing: score $score < threshold $AUTO_PROPOSE_THRESHOLD")
                        }
                    }
                }
            }

            CloudLogger.debug(TAG, "=== SMART SUGGESTIONS COMPLETE ===")
            CloudLogger.debug(TAG, "Total suggestions generated: ${suggestionsMap.size}")
            CloudLogger.debug(TAG, "Total auto-proposals: ${autoMappings.size}")

            _uiState.value =
                _uiState.value.copy(
                    suggestions = suggestionsMap,
                    mappings = autoMappings,
                )
        }
    }

    fun mapExercise(
        originalName: String,
        exerciseId: String?,
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
        val results =
            ExerciseSearchUtil
                .filterAndSortExercises(
                    exercises = allExercises,
                    query = query,
                    nameExtractor = { it.name },
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

    fun getFinalMappings(): Map<String, String?> =
        _uiState.value.mappings.mapValues { (_, mapping) ->
            mapping.exerciseId
        }

    fun createCustomExercise(
        originalName: String,
        name: String,
        category: ExerciseCategory,
        equipment: Set<Equipment>,
        difficulty: ExerciseDifficulty,
        requiresWeight: Boolean,
    ) {
        viewModelScope.launch {
            val singleEquipment = equipment.firstOrNull() ?: Equipment.NONE

            val customExercise =
                repository.createCustomExercise(
                    name = name,
                    category = category,
                    equipment = singleEquipment,
                    difficulty = difficulty,
                    requiresWeight = requiresWeight,
                    movementPattern = MovementPattern.PUSH,
                )

            if (customExercise != null) {
                mapExercise(originalName, customExercise.id, customExercise.name)
                _errorMessage.value = null
            } else {
                _errorMessage.value = "Failed to create '$name': An exercise with this name already exists"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
