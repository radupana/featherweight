package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.ExerciseNamingService
import com.github.radupana.featherweight.service.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ExerciseSelectorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FeatherweightRepository(application)
    private val namingService = ExerciseNamingService()

    // Raw data
    private val _allExercises = MutableStateFlow<List<ExerciseWithDetails>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Success state for creation
    private val _exerciseCreated = MutableStateFlow<ExerciseVariation?>(null)
    val exerciseCreated: StateFlow<ExerciseVariation?> = _exerciseCreated
    
    // Name validation state
    private val _nameValidationError = MutableStateFlow<String?>(null)
    val nameValidationError: StateFlow<String?> = _nameValidationError
    
    private val _nameSuggestion = MutableStateFlow<String?>(null)
    val nameSuggestion: StateFlow<String?> = _nameSuggestion
    
    // Delete state
    private val _exerciseToDelete = MutableStateFlow<ExerciseWithDetails?>(null)
    val exerciseToDelete: StateFlow<ExerciseWithDetails?> = _exerciseToDelete
    
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

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

    // Current exercise being swapped
    private val _currentSwapExerciseName = MutableStateFlow<String?>(null)
    val currentSwapExerciseName: StateFlow<String?> = _currentSwapExerciseName

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
                    // For now, skip muscle group filtering since ExerciseVariation doesn't have muscleGroup
                    true
                }.filter { exercise ->
                    // Equipment filter
                    equipmentFilter?.let {
                        exercise.variation.equipment == it
                    } ?: true
                }

        // Then apply text search with multi-word support
        return if (query.isNotEmpty()) {
            val searchWords = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

            // Score each exercise based on word matches
            val scoredExercises =
                filteredByAttributes.mapNotNull { exerciseWithDetails ->
                    val exercise = exerciseWithDetails.variation
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

                    // ExerciseVariation doesn't have muscle group/category fields directly

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
                        .thenBy { it.first.variation.name },
                ).map { it.first }
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
                val variations = repository.getAllExercises()
                // Load exercises with full muscle data
                val exercises =
                    variations.map { variation ->
                        loadExerciseWithDetails(variation)
                    }
                _allExercises.value = exercises
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load exercises from database", e)
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
        _currentSwapExerciseName.value = null
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

    fun createCustomExercise(
        name: String,
        category: ExerciseCategory? = null,
        primaryMuscles: Set<MuscleGroup> = emptySet(),
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        equipment: Set<Equipment> = emptySet(),
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _exerciseCreated.value = null
                
                // Validate and format the exercise name
                val formattedName = namingService.formatExerciseName(name)
                val validationResult = namingService.validateExerciseName(formattedName)
                
                if (validationResult is ValidationResult.Invalid) {
                    _errorMessage.value = validationResult.reason
                    // If there's a suggestion, we could offer to use it
                    return@launch
                }
                
                // Extract components from the name if not provided
                val components = namingService.extractComponents(formattedName)
                
                // Use provided values or fall back to extracted/inferred values
                val finalCategory = category ?: components.category
                val finalPrimaryMuscles = if (primaryMuscles.isNotEmpty()) {
                    primaryMuscles
                } else if (components.muscleGroup != null) {
                    setOf(components.muscleGroup)
                } else {
                    inferMusclesFromName(formattedName)
                }
                
                val finalEquipment = if (equipment.isNotEmpty()) {
                    equipment.first()
                } else {
                    components.equipment ?: Equipment.BODYWEIGHT
                }
                
                val finalMovementPattern = components.movementPattern
                
                // Create the exercise using the repository
                val result = repository.createCustomExercise(
                    name = formattedName,
                    category = finalCategory,
                    primaryMuscles = finalPrimaryMuscles,
                    secondaryMuscles = secondaryMuscles,
                    equipment = finalEquipment,
                    difficulty = difficulty,
                    requiresWeight = requiresWeight,
                    movementPattern = finalMovementPattern
                )
                
                result.fold(
                    onSuccess = { createdExercise ->
                        // Reload exercises to include the new one
                        loadExercises()
                        // Signal success
                        _exerciseCreated.value = createdExercise
                    },
                    onFailure = { error ->
                        _errorMessage.value = when (error) {
                            is IllegalArgumentException -> error.message
                            else -> "Failed to create exercise: ${error.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
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
    
    fun validateExerciseName(name: String) {
        if (name.isBlank()) {
            _nameValidationError.value = null
            _nameSuggestion.value = null
            return
        }
        
        val formattedName = namingService.formatExerciseName(name)
        val validationResult = namingService.validateExerciseName(formattedName)
        
        when (validationResult) {
            is ValidationResult.Valid -> {
                _nameValidationError.value = null
                _nameSuggestion.value = if (formattedName != name) formattedName else null
            }
            is ValidationResult.Invalid -> {
                _nameValidationError.value = validationResult.reason
                _nameSuggestion.value = validationResult.suggestion ?: namingService.suggestCorrection(name)
            }
        }
    }
    
    fun clearNameValidation() {
        _nameValidationError.value = null
        _nameSuggestion.value = null
    }
    
    fun requestDeleteExercise(exercise: ExerciseWithDetails) {
        _exerciseToDelete.value = exercise
        _deleteError.value = null
    }
    
    fun cancelDelete() {
        _exerciseToDelete.value = null
        _deleteError.value = null
    }
    
    fun confirmDeleteExercise() {
        viewModelScope.launch {
            val exercise = _exerciseToDelete.value ?: return@launch
            
            try {
                _deleteError.value = null
                
                // First check if we can delete it
                val canDelete = repository.canDeleteExercise(exercise.variation.id)
                canDelete.fold(
                    onSuccess = {
                        // Proceed with deletion
                        val deleteResult = repository.deleteCustomExercise(exercise.variation.id)
                        deleteResult.fold(
                            onSuccess = {
                                // Reload exercises to reflect the deletion
                                loadExercises()
                                // Clear the delete state
                                _exerciseToDelete.value = null
                                _deleteError.value = null
                                // Show success message
                                _errorMessage.value = "Exercise deleted successfully"
                            },
                            onFailure = { error ->
                                _deleteError.value = error.message ?: "Failed to delete exercise"
                            }
                        )
                    },
                    onFailure = { error ->
                        _deleteError.value = error.message ?: "Cannot delete this exercise"
                    }
                )
            } catch (e: Exception) {
                _deleteError.value = "Unexpected error: ${e.message}"
            }
        }
    }

    // Load swap suggestions for the given exercise
    fun loadSwapSuggestions(exerciseId: Long) {
        viewModelScope.launch {
            try {
                // Wait for exercises to be loaded if they're not already
                if (_allExercises.value.isEmpty()) {
                    // Ensure exercises are loaded first
                    repository.seedDatabaseIfEmpty()
                    val variations = repository.getAllExercises()
                    // Load exercises with muscle data
                    val exercises =
                        variations.map { variation ->
                            loadExerciseWithDetails(variation)
                        }
                    _allExercises.value = exercises
                }

                // Get historical swaps
                val userId = repository.getCurrentUserId()
                val swapHistory = repository.getSwapHistoryForExercise(userId, exerciseId)
                val previouslySwapped = mutableListOf<ExerciseSuggestion>()

                // Get exercise details for historically swapped exercises
                swapHistory.forEach { historyCount ->
                    val variation = repository.getExerciseById(historyCount.swappedToExerciseId)
                    if (variation != null) {
                        val exerciseWithDetails = loadExerciseWithDetails(variation)
                        previouslySwapped.add(
                            ExerciseSuggestion(
                                exercise = exerciseWithDetails,
                                swapCount = historyCount.swapCount,
                                suggestionReason = "", // Empty - the swap count badge already shows this info
                            ),
                        )
                    }
                }

                _previouslySwappedExercises.value = previouslySwapped

                // Get current exercise details for smart suggestions
                val currentVariation = repository.getExerciseById(exerciseId)
                if (currentVariation != null) {
                    // Set the current exercise name for display
                    _currentSwapExerciseName.value = currentVariation.name
                    
                    val currentExercise = loadExerciseWithDetails(currentVariation)
                    val suggestions = generateSmartSuggestions(currentExercise, previouslySwapped)
                    _swapSuggestions.value = suggestions
                } else {
                    _currentSwapExerciseName.value = null
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load swap suggestions", e)
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
                repository
                    .getAllExercises()
                    .map { variation ->
                        loadExerciseWithDetails(variation)
                    }.also { _allExercises.value = it }
            } else {
                _allExercises.value
            }

        val suggestions = mutableListOf<ExerciseSuggestion>()
        val previouslySwappedIds = previouslySwapped.map { it.exercise.variation.id }.toSet()

        // Filter out the current exercise and previously swapped ones
        val candidateExercises =
            allExercises.filter {
                it.variation.id != currentExercise.variation.id &&
                    it.variation.id !in previouslySwappedIds
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

        // Primary muscle match (highest priority)
        val currentPrimary = current.getPrimaryMuscles().toSet()
        val candidatePrimary = candidate.getPrimaryMuscles().toSet()
        if (currentPrimary.isNotEmpty() && candidatePrimary.isNotEmpty()) {
            val primaryOverlap = currentPrimary.intersect(candidatePrimary).size
            score += primaryOverlap * 100  // High score for primary muscle matches
            
            // Extra bonus if the main primary muscle matches
            if (currentPrimary.first() == candidatePrimary.first()) {
                score += 50
            }
        }

        // Secondary muscle match
        val currentSecondary = current.getSecondaryMuscles().toSet()
        val candidateSecondary = candidate.getSecondaryMuscles().toSet()
        if (currentSecondary.isNotEmpty() && candidateSecondary.isNotEmpty()) {
            val secondaryOverlap = currentSecondary.intersect(candidateSecondary).size
            score += secondaryOverlap * 30  // Medium score for secondary muscle matches
        }

        // Same equipment (important for home workouts)
        if (current.variation.equipment == candidate.variation.equipment) {
            score += 50
        }
        
        // Similar movement pattern (inferred from exercise name patterns)
        val currentPattern = inferMovementPattern(current.variation.name)
        val candidatePattern = inferMovementPattern(candidate.variation.name)
        if (currentPattern != null && currentPattern == candidatePattern) {
            score += 40
        }

        // Similar difficulty
        val difficultyDiff = kotlin.math.abs(
            current.variation.difficulty.level - candidate.variation.difficulty.level
        )
        score += (5 - difficultyDiff) * 10  // Closer difficulty = higher score

        // Usage count (popular exercises are good alternatives)
        score += candidate.variation.usageCount.coerceAtMost(20)

        return score
    }

    private fun getSuggestionReason(
        current: ExerciseWithDetails,
        candidate: ExerciseWithDetails,
    ): String {
        val reasons = mutableListOf<String>()
        
        val currentPrimary = current.getPrimaryMuscles()
        val candidatePrimary = candidate.getPrimaryMuscles()
        
        // Check primary muscle match
        if (currentPrimary.isNotEmpty() && candidatePrimary.isNotEmpty() &&
            currentPrimary.first() == candidatePrimary.first()) {
            reasons.add("Same primary muscle")
        }
        
        // Check equipment match
        if (current.variation.equipment == candidate.variation.equipment) {
            reasons.add("Same equipment")
        }
        
        // Check movement pattern
        val currentPattern = inferMovementPattern(current.variation.name)
        val candidatePattern = inferMovementPattern(candidate.variation.name)
        if (currentPattern != null && currentPattern == candidatePattern) {
            reasons.add("Similar movement")
        }
        
        return reasons.joinToString(" • ")
    }
    
    // Helper to infer movement pattern from exercise name
    private fun inferMovementPattern(exerciseName: String): String? {
        val name = exerciseName.lowercase()
        
        // Map of keywords to movement patterns
        val patterns = mapOf(
            "squat" to "squat",
            "deadlift" to "hinge",
            "row" to "horizontal_pull",
            "curl" to "isolation_pull",
            "lunge" to "lunge",
            "step" to "unilateral_leg",
            "plank" to "isometric",
            "crunch" to "core_flexion"
        )
        
        // Check simple patterns first
        for ((keyword, pattern) in patterns) {
            if (name.contains(keyword)) return pattern
        }
        
        // Check compound patterns
        return when {
            name.contains("bench") && name.contains("press") -> "horizontal_push"
            name.contains("press") -> "vertical_push"
            name.contains("pull") -> "vertical_pull"
            name.contains("extension") || name.contains("tricep") -> "isolation_push"
            name.contains("fly") || name.contains("flye") -> "isolation_chest"
            name.contains("raise") -> "isolation_shoulders"
            else -> null
        }
    }
    
    // Helper method to load exercise with full details including muscles
    private suspend fun loadExerciseWithDetails(variation: ExerciseVariation): ExerciseWithDetails {
        val muscles = repository.getMusclesForVariation(variation.id)
        return ExerciseWithDetails(
            variation = variation,
            muscles = muscles,
            aliases = emptyList(), // Can load if needed
            instructions = emptyList() // Can load if needed
        )
    }
}

// Data class for exercise suggestions
data class ExerciseSuggestion(
    val exercise: ExerciseWithDetails,
    val swapCount: Int = 0,
    val suggestionReason: String,
    val relevanceScore: Int = 0,
)
