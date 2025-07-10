package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.service.ExerciseMatchingService.UnmatchedExercise

/**
 * Enhanced validation result that supports partial matches.
 */
data class ProgrammeValidationResult(
    val isValid: Boolean,
    val validatedExercises: Map<String, Exercise>, // AI name -> matched exercise
    val unmatchedExercises: List<UnmatchedExercise>,
    val validationScore: Float, // 0.0 to 1.0
    val canProceedWithPartial: Boolean,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val matchedCount: Int get() = validatedExercises.size
    val totalCount: Int get() = validatedExercises.size + unmatchedExercises.size
    val matchPercentage: Float get() = if (totalCount > 0) matchedCount.toFloat() / totalCount else 0f
}

/**
 * Options for handling validation results.
 */
enum class ValidationAction {
    PROCEED_WITH_ALL, // All exercises matched
    FIX_UNMATCHED, // User needs to select replacements
    REGENERATE, // Request new programme from AI
    CANCEL, // Cancel programme creation
}

/**
 * Represents a user's exercise selection for an unmatched exercise.
 */
data class ExerciseReplacement(
    val unmatchedExercise: UnmatchedExercise,
    val selectedExercise: Exercise,
)
