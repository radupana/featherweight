package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.toExerciseDifficultyOrNull
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.ExerciseNamingService
import com.github.radupana.featherweight.service.ValidationResult
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.ExerciseSearchUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ExerciseSelectorViewModel(
    application: Application,
    private val repository: FeatherweightRepository,
    private val namingService: ExerciseNamingService,
) : AndroidViewModel(application) {
    // Secondary constructor for Android ViewModelFactory
    constructor(application: Application) : this(
        application,
        FeatherweightRepository(application),
        ExerciseNamingService(),
    )

    companion object {
        private const val TAG = "ExerciseSelectorVM"
    }

    // Raw data
    private val allExercisesCache = MutableStateFlow<List<ExerciseWithDetails>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Success state for creation
    private val _exerciseCreated = MutableStateFlow<Exercise?>(null)
    val exerciseCreated: StateFlow<Exercise?> = _exerciseCreated

    // Name validation state
    private val _nameValidationError = MutableStateFlow<String?>(null)
    val nameValidationError: StateFlow<String?> = _nameValidationError

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

    // Computed state
    val isLoading: StateFlow<Boolean> = _isLoading

    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    // Available filter options
    val categories = MutableStateFlow(ExerciseCategory.entries.toList())

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
        // Check authentication state
        _isAuthenticated.value = repository.getCurrentUserId() != null

        // Combine all filter states and update filtered exercises
        viewModelScope.launch {
            combine(
                allExercisesCache,
                _searchQuery,
                _selectedCategory,
            ) { exercises, query, category ->
                filterExercises(exercises, query)
            }.collect { filteredResults ->
                _filteredExercises.value = filteredResults
            }
        }
    }

    private fun filterExercises(
        exercises: List<ExerciseWithDetails>,
        query: String,
    ): List<ExerciseWithDetails> =
        if (query.isNotEmpty()) {
            // Use the shared search utility with usage count bonus
            exercises
                .mapNotNull { exerciseWithDetails ->
                    val aliases = exerciseWithDetails.aliases.map { it.alias }
                    val baseScore =
                        ExerciseSearchUtil.scoreExerciseMatch(
                            exerciseName = exerciseWithDetails.variation.name,
                            query = query,
                            aliases = aliases,
                        )

                    if (baseScore > 0) {
                        // Add usage bonus - up to 20 points based on usage count
                        val usageBonus = exerciseWithDetails.usageCount.coerceAtMost(20)
                        val finalScore = baseScore + usageBonus
                        exerciseWithDetails to finalScore
                    } else {
                        null
                    }
                }.sortedWith(
                    compareByDescending<Pair<ExerciseWithDetails, Int>> { it.second }
                        .thenBy { it.first.variation.name },
                ).map { it.first }
        } else {
            // When not searching, sort by usage count (most used first)
            exercises.sortedWith(
                compareByDescending<ExerciseWithDetails> { it.usageCount }
                    .thenBy { it.variation.name },
            )
        }

    fun loadExercises() {
        loadExercisesInternal(null)
    }

    private fun loadExercisesByCategory(category: ExerciseCategory?) {
        loadExercisesInternal(category)
    }

    private fun loadExercisesInternal(
        category: ExerciseCategory?,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allExercises =
                    if (category != null) {
                        repository.getExercisesByCategory(category)
                    } else {
                        repository.getAllExercises()
                    }

                // Load exercises with full muscle data
                val exercises =
                    allExercises.map { exercise ->
                        loadExerciseWithDetails(exercise)
                    }

                allExercisesCache.value = exercises
            } catch (e: android.database.sqlite.SQLiteException) {
                _errorMessage.value = "Database error loading exercises: ${e.message}"
            } catch (e: IllegalStateException) {
                _errorMessage.value = "Invalid state loading exercises: ${e.message}"
            } catch (e: SecurityException) {
                _errorMessage.value = "Permission error loading exercises: ${e.message}"
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
        // Reload exercises from database when category changes
        loadExercisesByCategory(category)
    }

    fun createCustomExercise(
        name: String,
        category: ExerciseCategory? = null,
        equipment: Set<Equipment> = emptySet(),
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true,
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

                val finalEquipment =
                    if (equipment.isNotEmpty()) {
                        equipment.first()
                    } else {
                        components.equipment ?: Equipment.BODYWEIGHT
                    }

                val finalMovementPattern = components.movementPattern

                // Create the custom exercise using the repository
                val customExercise =
                    repository.createCustomExercise(
                        name = formattedName,
                        category = finalCategory,
                        equipment = finalEquipment,
                        difficulty = difficulty,
                        requiresWeight = requiresWeight,
                        movementPattern = finalMovementPattern,
                    )

                if (customExercise != null) {
                    // Already an Exercise
                    val variation = customExercise
                    // Reload exercises to include the new one
                    loadExercises()
                    // Signal success
                    _exerciseCreated.value = variation
                } else {
                    _errorMessage.value = "Failed to create custom exercise: Name may already exist"
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                _errorMessage.value = "Database error creating exercise: ${e.message}"
            } catch (e: IllegalArgumentException) {
                _errorMessage.value = "Invalid exercise data: ${e.message}"
            } catch (e: IllegalStateException) {
                _errorMessage.value = "Invalid state creating exercise: ${e.message}"
            } catch (e: SecurityException) {
                _errorMessage.value = "Permission error creating exercise: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearExerciseCreated() {
        _exerciseCreated.value = null
    }

    fun validateExerciseName(name: String) {
        if (name.isBlank()) {
            _nameValidationError.value = null
            return
        }

        val formattedName = namingService.formatExerciseName(name)
        val validationResult = namingService.validateExerciseName(formattedName)

        when (validationResult) {
            is ValidationResult.Valid -> {
                _nameValidationError.value = null
            }
            is ValidationResult.Invalid -> {
                _nameValidationError.value = validationResult.reason
            }
        }
    }

    fun clearNameValidation() {
        _nameValidationError.value = null
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

                // Delete custom exercise
                val deleted = repository.deleteUserCustomExercise(exercise.variation.id)
                if (deleted) {
                    // Reload exercises to reflect the deletion
                    loadExercises()
                    // Clear the delete state
                    _exerciseToDelete.value = null
                    _deleteError.value = null
                    // Show success message
                    _errorMessage.value = "Exercise deleted successfully"
                } else {
                    _deleteError.value = "Cannot delete exercise: It may be in use or not found"
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                _deleteError.value = "Database error deleting exercise: ${e.message}"
            } catch (e: IllegalArgumentException) {
                _deleteError.value = "Invalid exercise ID: ${e.message}"
            } catch (e: IllegalStateException) {
                _deleteError.value = "Invalid state deleting exercise: ${e.message}"
            } catch (e: SecurityException) {
                _deleteError.value = "Permission error deleting exercise: ${e.message}"
            }
        }
    }

    // Load swap suggestions for the given exercise
    fun loadSwapSuggestions(exerciseId: String) {
        viewModelScope.launch {
            try {
                // Wait for exercises to be loaded if they're not already
                if (allExercisesCache.value.isEmpty()) {
                    val variations = repository.getAllExercises()
                    // Load exercises with muscle data
                    val exercises =
                        variations.map { variation ->
                            loadExerciseWithDetails(variation)
                        }
                    allExercisesCache.value = exercises
                }

                // Get historical swaps
                val swapHistory = repository.getSwapHistoryForExercise(exerciseId)
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
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                when (e) {
                    is android.database.sqlite.SQLiteException,
                    is IllegalArgumentException,
                    is SecurityException,
                    -> {
                        ExceptionLogger.logException(TAG, "Error loading swap suggestions", e)
                        throw IllegalStateException("Error loading swap suggestions: ${e.message}", e)
                    }
                    else -> throw e
                }
            }
        }
    }

    private suspend fun generateSmartSuggestions(
        currentExercise: ExerciseWithDetails,
        previouslySwapped: List<ExerciseSuggestion>,
    ): List<ExerciseSuggestion> {
        // Ensure we have exercises loaded
        val allExercises =
            allExercisesCache.value.ifEmpty {
                repository
                    .getAllExercises()
                    .map { variation ->
                        loadExerciseWithDetails(variation)
                    }.also { allExercisesCache.value = it }
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
            score += primaryOverlap * 100 // High score for primary muscle matches

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
            score += secondaryOverlap * 30 // Medium score for secondary muscle matches
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
        val currentLevel =
            current.variation.difficulty
                ?.toExerciseDifficultyOrNull()
                ?.level ?: 0
        val candidateLevel =
            candidate.variation.difficulty
                ?.toExerciseDifficultyOrNull()
                ?.level ?: 0
        val difficultyDiff = kotlin.math.abs(currentLevel - candidateLevel)
        score += (5 - difficultyDiff) * 10 // Closer difficulty = higher score

        // Add usage count bonus
        score += candidate.usageCount.coerceAtMost(20)

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
        if (currentPrimary.isNotEmpty() &&
            candidatePrimary.isNotEmpty() &&
            currentPrimary.first() == candidatePrimary.first()
        ) {
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
        val patterns =
            mapOf(
                "squat" to "squat",
                "deadlift" to "hinge",
                "row" to "horizontal_pull",
                "curl" to "isolation_pull",
                "lunge" to "lunge",
                "step" to "unilateral_leg",
                "plank" to "isometric",
                "crunch" to "core_flexion",
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

    // Helper method to load system exercise with full details including muscles
    private suspend fun loadExerciseWithDetails(variation: Exercise): ExerciseWithDetails {
        val muscles = repository.getMusclesForVariation(variation.id)
        val aliases = repository.getAliasesForExercise(variation.id)

        // Get user-specific usage stats (use "local" for unauthenticated users)
        val userId = repository.getCurrentUserId() ?: "local"
        val usageStats = repository.getUserExerciseUsage(userId, variation.id)

        return ExerciseWithDetails(
            variation = variation,
            muscles = muscles,
            aliases = aliases,
            instructions = emptyList(), // Can load if needed
            usageCount = usageStats?.usageCount ?: 0,
            // isCustom is now a derived property based on variation.userId
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
