package com.github.radupana.featherweight.validation

import com.github.radupana.featherweight.data.exercise.ExerciseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Validates exercise names against the database to prevent mismatches
 */
class ExerciseValidator(private val exerciseDao: ExerciseDao) {
    private var validExerciseNames: Set<String> = emptySet()
    private var validExerciseIds: Set<Long> = emptySet()

    /**
     * Initialize the validator with all valid exercise names from the database
     * Should be called during app startup
     */
    suspend fun initialize() =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercisesWithDetails()
            validExerciseNames = exercises.map { it.exercise.name }.toSet()
            validExerciseIds = exercises.map { it.exercise.id }.toSet()
            println("ExerciseValidator initialized with ${validExerciseNames.size} exercises")
        }

    /**
     * Validate a single exercise name
     */
    fun validateExerciseName(name: String): ValidationResult {
        return when {
            validExerciseNames.contains(name) -> ValidationResult.Valid
            else -> {
                val suggestion = findClosestMatch(name)
                ValidationResult.Invalid(
                    providedName = name,
                    suggestion = suggestion,
                    reason = "Exercise '$name' does not exist in database",
                )
            }
        }
    }

    /**
     * Validate an exercise ID
     */
    fun validateExerciseId(id: Long): ValidationResult {
        return when {
            validExerciseIds.contains(id) -> ValidationResult.Valid
            else ->
                ValidationResult.Invalid(
                    providedName = "ID: $id",
                    suggestion = null,
                    reason = "Exercise with ID $id does not exist in database",
                )
        }
    }

    /**
     * Validate all exercise names in a programme structure
     */
    suspend fun validateProgrammeStructure(jsonStructure: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        try {
            // Simple regex to find exercise names in JSON
            val exercisePattern = """"name"\s*:\s*"([^"]+)"""".toRegex()
            val matches = exercisePattern.findAll(jsonStructure)

            matches.forEach { match ->
                val exerciseName = match.groupValues[1]
                // Skip non-exercise names (like workout names)
                if (!exerciseName.contains("Workout") && !exerciseName.contains("Week") && !exerciseName.contains("Day")) {
                    val result = validateExerciseName(exerciseName)
                    if (result is ValidationResult.Invalid) {
                        errors.add(
                            ValidationError(
                                field = "exercise",
                                value = exerciseName,
                                error = result.reason,
                                suggestion = result.suggestion,
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    field = "structure",
                    value = jsonStructure.take(100),
                    error = "Failed to parse programme structure: ${e.message}",
                    suggestion = null,
                ),
            )
        }

        return errors
    }

    /**
     * Find the closest matching exercise name using simple string similarity
     */
    private fun findClosestMatch(name: String): String? {
        if (validExerciseNames.isEmpty()) return null

        // First try exact substring matches
        val exactSubstringMatches =
            validExerciseNames.filter {
                it.contains(name, ignoreCase = true) || name.contains(it, ignoreCase = true)
            }
        if (exactSubstringMatches.isNotEmpty()) {
            return exactSubstringMatches.minByOrNull {
                kotlin.math.abs(it.length - name.length)
            }
        }

        // Then try word-based matching
        val nameWords = name.split(" ", "-", "_").filter { it.isNotEmpty() }
        val wordMatches =
            validExerciseNames.filter { validName ->
                val validWords = validName.split(" ", "-", "_").filter { it.isNotEmpty() }
                nameWords.any { nameWord ->
                    validWords.any { validWord ->
                        validWord.equals(nameWord, ignoreCase = true)
                    }
                }
            }

        return wordMatches.maxByOrNull { validName ->
            val validWords = validName.split(" ", "-", "_").filter { it.isNotEmpty() }
            nameWords.count { nameWord ->
                validWords.any { validWord ->
                    validWord.equals(nameWord, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Batch validate multiple exercise names
     */
    fun validateExerciseNames(names: List<String>): Map<String, ValidationResult> {
        return names.associateWith { validateExerciseName(it) }
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(
        val providedName: String,
        val suggestion: String?,
        val reason: String,
    ) : ValidationResult()
}

data class ValidationError(
    val field: String,
    val value: String,
    val error: String,
    val suggestion: String?,
)
