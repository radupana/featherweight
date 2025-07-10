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

    // Swap suggestions
    private val _swapSuggestions = MutableStateFlow<List<ExerciseSuggestion>>(emptyList())
    val swapSuggestions: StateFlow<List<ExerciseSuggestion>> = _swapSuggestions

    private val _previouslySwappedExercises = MutableStateFlow<List<ExerciseSuggestion>>(emptyList())
    val previouslySwappedExercises: StateFlow<List<ExerciseSuggestion>> = _previouslySwappedExercises

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
    ): List<ExerciseWithDetails> {
        // First apply category, muscle group, and equipment filters
        val filteredByAttributes =
            exercises
                .filter { exercise ->
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
                }

        // Then apply text search with multi-word support
        return if (query.isNotEmpty()) {
            val searchWords = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

            // Score each exercise based on word matches
            val scoredExercises =
                filteredByAttributes.mapNotNull { exerciseWithDetails ->
                    val exercise = exerciseWithDetails.exercise
                    val nameLower = exercise.name.lowercase()
                    val queryLower = query.lowercase()

                    // Calculate score
                    var score = 0

                    // Exact match gets highest score
                    if (nameLower == queryLower) {
                        score = 1000
                    } else if (nameLower.contains(queryLower)) {
                        // Full query as substring gets high score
                        score = 800
                        // Bonus if it starts with the query
                        if (nameLower.startsWith(queryLower)) {
                            score += 100
                        }
                    } else {
                        // Multi-word matching
                        val nameWords = nameLower.split("\\s+".toRegex())
                        var matchedWords = 0
                        var positionBonus = 0

                        searchWords.forEachIndexed { index, searchWord ->
                            val searchWordLower = searchWord.lowercase()

                            // Check if any word in the exercise name contains this search word
                            if (nameWords.any { it.contains(searchWordLower) }) {
                                matchedWords++
                                // Bonus for words at the beginning
                                if (index == 0 && nameWords.first().startsWith(searchWordLower)) {
                                    positionBonus += 50
                                }
                            }
                        }

                        // Only include if at least one word matches
                        if (matchedWords > 0) {
                            // Base score for partial matches
                            score = 100 * matchedWords
                            // Bonus for matching all search words
                            if (matchedWords == searchWords.size) {
                                score += 200
                            }
                            // Add position bonus
                            score += positionBonus
                            // Small bonus based on usage
                            score += exercise.usageCount / 10
                        }
                    }

                    // Also check muscle group and category for matches
                    if (score == 0) {
                        searchWords.forEach { searchWord ->
                            if (exercise.muscleGroup.contains(searchWord, ignoreCase = true)) {
                                score += 30
                            }
                            if (exercise.category.name.replace('_', ' ').contains(searchWord, ignoreCase = true)) {
                                score += 20
                            }
                        }
                    }

                    if (score > 0) {
                        exerciseWithDetails to score
                    } else {
                        null
                    }
                }

            // Sort by score (highest first), then by name
            scoredExercises
                .sortedWith(
                    compareByDescending<Pair<ExerciseWithDetails, Int>> { it.second }
                        .thenBy { it.first.exercise.name },
                )
                .map { it.first }
        } else {
            // When not searching, maintain the usage-based order
            filteredByAttributes
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

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun clearSwapSuggestions() {
        _swapSuggestions.value = emptyList()
        _previouslySwappedExercises.value = emptyList()
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
            nameLower.contains("cable") -> setOf(Equipment.CABLE)
            nameLower.contains("pull-up") || nameLower.contains("pullup") || nameLower.contains("chin-up") -> setOf(Equipment.PULL_UP_BAR)
            nameLower.contains("dip") -> setOf(Equipment.DIP_STATION)
            nameLower.contains("machine") -> setOf(Equipment.CHEST_PRESS) // Generic machine
            else -> setOf(Equipment.BODYWEIGHT)
        }
    }

    fun refreshExercises() {
        loadExercises()
    }

    // Load swap suggestions for the given exercise
    fun loadSwapSuggestions(exerciseId: Long) {
        viewModelScope.launch {
            try {
                println("Loading swap suggestions for exercise ID: $exerciseId")

                // Wait for exercises to be loaded if they're not already
                if (_allExercises.value.isEmpty()) {
                    println("Exercises not loaded yet, waiting...")
                    // Ensure exercises are loaded first
                    repository.seedDatabaseIfEmpty()
                    val exercises = repository.getAllExercises()
                    _allExercises.value = exercises
                    println("Loaded ${exercises.size} exercises")
                }

                // Get historical swaps
                val swapHistory = repository.getSwapHistoryForExercise(exerciseId)
                val previouslySwapped = mutableListOf<ExerciseSuggestion>()

                println("Found ${swapHistory.size} historical swaps")

                // Get exercise details for historically swapped exercises
                swapHistory.forEach { historyCount ->
                    val exercise = repository.getExerciseById(historyCount.swappedToExerciseId)
                    if (exercise != null) {
                        previouslySwapped.add(
                            ExerciseSuggestion(
                                exercise = exercise,
                                swapCount = historyCount.swapCount,
                                suggestionReason = "Previously swapped ${historyCount.swapCount}x",
                            ),
                        )
                    }
                }

                _previouslySwappedExercises.value = previouslySwapped

                // Get current exercise details for smart suggestions
                val currentExercise = repository.getExerciseById(exerciseId)
                if (currentExercise != null) {
                    val suggestions = generateSmartSuggestions(currentExercise, previouslySwapped)
                    println("Generated ${suggestions.size} smart suggestions")
                    _swapSuggestions.value = suggestions
                } else {
                    println("Could not find current exercise with ID: $exerciseId")
                }
            } catch (e: Exception) {
                println("Error loading swap suggestions: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun generateSmartSuggestions(
        currentExercise: ExerciseWithDetails,
        previouslySwapped: List<ExerciseSuggestion>,
    ): List<ExerciseSuggestion> {
        // Ensure we have exercises loaded
        val allExercises =
            if (_allExercises.value.isEmpty()) {
                repository.getAllExercises().also { _allExercises.value = it }
            } else {
                _allExercises.value
            }

        val suggestions = mutableListOf<ExerciseSuggestion>()
        val previouslySwappedIds = previouslySwapped.map { it.exercise.exercise.id }.toSet()

        // Filter out the current exercise and previously swapped ones
        val candidateExercises =
            allExercises.filter {
                it.exercise.id != currentExercise.exercise.id &&
                    it.exercise.id !in previouslySwappedIds
            }

        candidateExercises.forEach { exercise ->
            val score = calculateSuggestionScore(currentExercise, exercise)
            if (score > 0) {
                val reason = getSuggestionReason(currentExercise, exercise)
                suggestions.add(
                    ExerciseSuggestion(
                        exercise = exercise,
                        swapCount = 0,
                        suggestionReason = reason,
                        relevanceScore = score,
                    ),
                )
            }
        }

        // Sort by relevance score (higher is better) and take top 10
        return suggestions.sortedByDescending { it.relevanceScore }.take(10)
    }

    private fun calculateSuggestionScore(
        current: ExerciseWithDetails,
        candidate: ExerciseWithDetails,
    ): Int {
        var score = 0

        // Same category gets high score
        if (current.exercise.category == candidate.exercise.category) {
            score += 50
        }

        // Same equipment gets medium score
        if (current.exercise.equipment == candidate.exercise.equipment) {
            score += 30
        }

        // Same muscle group gets high score
        if (current.exercise.muscleGroup == candidate.exercise.muscleGroup) {
            score += 40
        }

        // Similar difficulty gets small score
        if (current.exercise.difficulty == candidate.exercise.difficulty) {
            score += 10
        }

        // Usage count adds to score (more used = better suggestion)
        score += candidate.exercise.usageCount

        return score
    }

    private fun getSuggestionReason(
        current: ExerciseWithDetails,
        candidate: ExerciseWithDetails,
    ): String {
        // For smart suggestions, we'll build details in the UI
        // This is kept for backwards compatibility but won't be displayed
        return ""
    }
}

// Data class for exercise suggestions
data class ExerciseSuggestion(
    val exercise: ExerciseWithDetails,
    val swapCount: Int = 0,
    val suggestionReason: String,
    val relevanceScore: Int = 0,
)
