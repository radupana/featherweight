package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ExerciseSelectorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)

    // Raw data
    private val _allExercises = MutableStateFlow<List<ExerciseWithDetails>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Success state for creation
    private val _exerciseCreated = MutableStateFlow<String?>(null)
    val exerciseCreated: StateFlow<String?> = _exerciseCreated

    // Filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<ExerciseCategory?>(null)
    val selectedCategory: StateFlow<ExerciseCategory?> = _selectedCategory

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup

    private val _selectedEquipment = MutableStateFlow<Equipment?>(null)
    val selectedEquipment: StateFlow<Equipment?> = _selectedEquipment

    // Computed state
    val isLoading: StateFlow<Boolean> = _isLoading

    // Available filter options
    val categories = MutableStateFlow(ExerciseCategory.values().toList())
    val muscleGroups = MutableStateFlow(MuscleGroup.values().toList())
    val equipment = MutableStateFlow(Equipment.values().filter { it != Equipment.NONE })

    // Filtered exercises
    private val _filteredExercises = MutableStateFlow<List<ExerciseWithDetails>>(emptyList())
    val filteredExercises: StateFlow<List<ExerciseWithDetails>> = _filteredExercises

    init {
        // Combine all filter states and update filtered exercises
        viewModelScope.launch {
            combine(
                _allExercises,
                _searchQuery,
                _selectedCategory,
                _selectedMuscleGroup,
                _selectedEquipment,
            ) { exercises, query, category, muscleGroup, equipmentFilter ->
                filterExercises(exercises, query, category, muscleGroup, equipmentFilter)
            }.collect { filteredResults ->
                _filteredExercises.value = filteredResults
            }
        }
    }

    private fun filterExercises(
        exercises: List<ExerciseWithDetails>,
        query: String,
        category: ExerciseCategory?,
        muscleGroup: String?,
        equipmentFilter: Equipment?,
    ): List<ExerciseWithDetails> =
        exercises
            .filter { exercise ->
                // Text search filter
                if (query.isNotEmpty()) {
                    exercise.exercise.name.contains(query, ignoreCase = true) ||
                        exercise.exercise.muscleGroup.contains(query, ignoreCase = true) ||
                        exercise.exercise.category.name.replace('_', ' ')
                            .contains(query, ignoreCase = true)
                } else {
                    true
                }
            }.filter { exercise ->
                // Category filter
                category?.let { exercise.exercise.category == it } ?: true
            }.filter { exercise ->
                // Muscle group filter
                muscleGroup?.let {
                    exercise.exercise.muscleGroup.equals(it, ignoreCase = true)
                } ?: true
            }.filter { exercise ->
                // Equipment filter
                equipmentFilter?.let {
                    exercise.exercise.equipment == it
                } ?: true
            }.let { filteredList ->
                if (query.isNotEmpty()) {
                    // When searching, sort by search relevance first
                    filteredList.sortedWith(
                        compareBy<ExerciseWithDetails> {
                            when {
                                it.exercise.name.equals(query, ignoreCase = true) -> 0
                                it.exercise.name.startsWith(query, ignoreCase = true) -> 1
                                it.exercise.name.contains(query, ignoreCase = true) -> 2
                                else -> 3
                            }
                        }.thenBy { it.exercise.name },
                    )
                } else {
                    // When not searching, maintain the usage-based order from loadExercises()
                    filteredList
                }
            }

    fun loadExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure database is seeded first
                repository.seedDatabaseIfEmpty()

                // Load exercises efficiently (usage stats can be calculated on-demand)
                val exercises = repository.getAllExercises()
                _allExercises.value = exercises

                println("Loaded ${exercises.size} exercises from database (sorted by usage)")
            } catch (e: Exception) {
                println("Error loading exercises: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ExerciseCategory?) {
        _selectedCategory.value = category
    }

    fun selectMuscleGroup(muscleGroup: String?) {
        _selectedMuscleGroup.value = muscleGroup
    }

    fun selectEquipment(equipment: Equipment?) {
        _selectedEquipment.value = equipment
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedMuscleGroup.value = null
        _selectedEquipment.value = null
        _searchQuery.value = ""
    }

    fun createCustomExercise(name: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null // Clear any previous errors
                _exerciseCreated.value = null // Clear any previous success

                // Try to determine category from name
                val category = inferCategoryFromName(name)

                // Basic muscle groups based on common exercise patterns
                val primaryMuscles = inferMusclesFromName(name)

                // Default to bodyweight if we can't determine equipment
                val equipmentSet = inferEquipmentFromName(name)

                // TODO: Re-implement custom exercise creation
                println("Custom exercise creation not yet implemented: $name")

                // Reload exercises after creating
                loadExercises()

                // Signal success
                _exerciseCreated.value = name
            } catch (e: Exception) {
                _errorMessage.value = e.message
                println("Error creating custom exercise: ${e.message}")
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearExerciseCreated() {
        _exerciseCreated.value = null
    }

    private fun inferCategoryFromName(name: String): ExerciseCategory {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("press") || nameLower.contains("push") || nameLower.contains("chest") -> ExerciseCategory.CHEST
            nameLower.contains(
                "pull",
            ) ||
                nameLower.contains("row") ||
                nameLower.contains("lat") ||
                nameLower.contains("back") -> ExerciseCategory.BACK
            nameLower.contains("squat") ||
                nameLower.contains("lunge") ||
                nameLower.contains("leg") ||
                nameLower.contains("quad") ||
                nameLower.contains("glute") -> ExerciseCategory.LEGS
            nameLower.contains("shoulder") || nameLower.contains("delt") || nameLower.contains("raise") -> ExerciseCategory.SHOULDERS
            nameLower.contains(
                "curl",
            ) ||
                nameLower.contains("extension") ||
                nameLower.contains("tricep") ||
                nameLower.contains("bicep") -> ExerciseCategory.ARMS
            nameLower.contains(
                "plank",
            ) ||
                nameLower.contains("crunch") ||
                nameLower.contains("ab") ||
                nameLower.contains("core") -> ExerciseCategory.CORE
            else -> ExerciseCategory.FULL_BODY
        }
    }

    private fun inferMusclesFromName(name: String): Set<MuscleGroup> {
        val nameLower = name.lowercase()
        val muscles = mutableSetOf<MuscleGroup>()

        when {
            nameLower.contains("chest") || nameLower.contains("bench") -> muscles.add(MuscleGroup.CHEST)
            nameLower.contains("back") || nameLower.contains("row") -> muscles.add(MuscleGroup.UPPER_BACK)
            nameLower.contains("lat") -> muscles.add(MuscleGroup.LATS)
            nameLower.contains("squat") || nameLower.contains("quad") -> muscles.add(MuscleGroup.QUADS)
            nameLower.contains("deadlift") || nameLower.contains("glute") -> muscles.add(MuscleGroup.GLUTES)
            nameLower.contains("shoulder") -> muscles.add(MuscleGroup.FRONT_DELTS)
            nameLower.contains("bicep") || nameLower.contains("curl") -> muscles.add(MuscleGroup.BICEPS)
            nameLower.contains("tricep") -> muscles.add(MuscleGroup.TRICEPS)
            nameLower.contains("calf") -> muscles.add(MuscleGroup.CALVES)
            nameLower.contains("ab") || nameLower.contains("core") -> muscles.add(MuscleGroup.ABS)
        }

        // Default to full body if we can't determine specific muscles
        if (muscles.isEmpty()) {
            muscles.add(MuscleGroup.FULL_BODY)
        }

        return muscles
    }

    private fun inferEquipmentFromName(name: String): Set<Equipment> {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains(
                "barbell",
            ) ||
                nameLower.contains("bench") ||
                nameLower.contains("squat") ||
                nameLower.contains("deadlift") -> setOf(Equipment.BARBELL)
            nameLower.contains("dumbbell") || nameLower.contains("db") -> setOf(Equipment.DUMBBELL)
            nameLower.contains("cable") -> setOf(Equipment.CABLE_MACHINE)
            nameLower.contains("pull-up") || nameLower.contains("pullup") || nameLower.contains("chin-up") -> setOf(Equipment.PULL_UP_BAR)
            nameLower.contains("dip") -> setOf(Equipment.DIP_STATION)
            nameLower.contains("machine") -> setOf(Equipment.CHEST_PRESS) // Generic machine
            else -> setOf(Equipment.BODYWEIGHT)
        }
    }

    fun refreshExercises() {
        loadExercises()
    }
}
