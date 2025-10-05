package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup

/**
 * Service for validating and formatting exercise names according to app conventions.
 *
 * Naming Convention: `[Equipment] [Muscle Target] [Movement Type]`
 * Example: "Barbell Bench Press", "Dumbbell Bicep Curl"
 *
 * Rules:
 * - Equipment MUST come first
 * - Use proper case for all words
 * - No hyphens (use "Step Up" not "Step-Up")
 * - Use singular forms ("Curl" not "Curls")
 * - No emojis
 * - Cannot duplicate existing system exercise names or aliases
 */
class ExerciseNamingService {
    companion object {
        private const val MIN_NAME_LENGTH = 3
        private const val MAX_NAME_LENGTH = 50

        // Common equipment keywords that should appear first
        private val EQUIPMENT_KEYWORDS =
            setOf(
                "barbell",
                "dumbbell",
                "cable",
                "machine",
                "bodyweight",
                "kettlebell",
                "band",
                "plate",
                "smith",
                "trap bar",
                "ez bar",
                "db",
                "bb",
                "kb",
            )

        // Common muscle group keywords
        private val MUSCLE_KEYWORDS =
            setOf(
                "chest",
                "back",
                "shoulder",
                "bicep",
                "tricep",
                "quad",
                "hamstring",
                "glute",
                "calf",
                "ab",
                "core",
                "lat",
                "delt",
                "trap",
                "forearm",
                "leg",
            )

        // Common movement keywords
        private val MOVEMENT_KEYWORDS =
            setOf(
                "press",
                "curl",
                "extension",
                "fly",
                "row",
                "pulldown",
                "raise",
                "squat",
                "deadlift",
                "lunge",
                "crunch",
                "plank",
                "pull",
                "push",
                "dip",
                "shrug",
                "kickback",
            )

        // Words that should be singular
        private val SINGULAR_MAPPINGS =
            mapOf(
                "curls" to "curl",
                "presses" to "press",
                "rows" to "row",
                "raises" to "raise",
                "extensions" to "extension",
                "flies" to "fly",
                "flyes" to "fly",
                "crunches" to "crunch",
                "dips" to "dip",
                "shrugs" to "shrug",
                "kickbacks" to "kickback",
                "pulldowns" to "pulldown",
                "pushdowns" to "pushdown",
                "lunges" to "lunge",
                "squats" to "squat",
            )

        // Equipment abbreviation mappings
        private val EQUIPMENT_ABBREVIATIONS =
            mapOf(
                "db" to "Dumbbell",
                "bb" to "Barbell",
                "kb" to "Kettlebell",
                "ez" to "EZ Bar",
            )
    }

    /**
     * Validates an exercise name according to app conventions.
     * Does NOT check for duplicates with existing exercises - use validateExerciseNameWithDuplicateCheck for that.
     */
    fun validateExerciseName(name: String): ValidationResult {
        val trimmedName = name.trim()

        // Check basic length requirements
        val lengthValidation = validateLength(trimmedName)
        if (lengthValidation != null) return lengthValidation

        // Check for invalid characters
        val characterValidation = validateCharacters(trimmedName)
        if (characterValidation != null) return characterValidation

        // Check formatting
        val formattingValidation = validateFormatting(trimmedName)
        if (formattingValidation != null) return formattingValidation

        // Check equipment order
        val equipmentValidation = validateEquipmentOrder(trimmedName)
        if (equipmentValidation != null) return equipmentValidation

        return ValidationResult.Valid
    }

    private fun validateLength(name: String): ValidationResult.Invalid? =
        when {
            name.length < MIN_NAME_LENGTH ->
                ValidationResult.Invalid(
                    reason = "Exercise name must be at least $MIN_NAME_LENGTH characters",
                    suggestion = null,
                )
            name.length > MAX_NAME_LENGTH ->
                ValidationResult.Invalid(
                    reason = "Exercise name must be less than $MAX_NAME_LENGTH characters",
                    suggestion = name.take(MAX_NAME_LENGTH),
                )
            else -> null
        }

    private fun validateCharacters(name: String): ValidationResult.Invalid? =
        when {
            containsEmoji(name) ->
                ValidationResult.Invalid(
                    reason = "Exercise name cannot contain emojis",
                    suggestion = removeEmojis(name),
                )
            name.contains("-") ->
                ValidationResult.Invalid(
                    reason = "Use spaces instead of hyphens (e.g., 'Step Up' not 'Step-Up')",
                    suggestion = formatExerciseName(name.replace("-", " ")),
                )
            else -> null
        }

    private fun validateFormatting(name: String): ValidationResult.Invalid? {
        val formatted = formatExerciseName(name)
        return if (formatted != name) {
            ValidationResult.Invalid(
                reason = "Exercise name should be properly formatted",
                suggestion = formatted,
            )
        } else {
            null
        }
    }

    private fun validateEquipmentOrder(name: String): ValidationResult.Invalid? {
        val components = extractComponents(name)
        val shouldCheckEquipmentOrder =
            components.equipment == null &&
                !name.lowercase().startsWith("bodyweight")

        if (!shouldCheckEquipmentOrder) return null

        val firstWord = name.split(" ").firstOrNull()?.lowercase() ?: ""
        val isFirstWordEquipment = EQUIPMENT_KEYWORDS.contains(firstWord)

        if (isFirstWordEquipment) return null

        val detectedEquipment = detectEquipmentInName(name)
        return if (detectedEquipment != null) {
            ValidationResult.Invalid(
                reason = "Equipment should come first in the exercise name",
                suggestion = suggestWithEquipmentFirst(name, detectedEquipment),
            )
        } else {
            null
        }
    }

    /**
     * Formats an exercise name according to conventions.
     */
    fun formatExerciseName(name: String): String {
        var formatted =
            name
                .trim()
                .replace(Regex("\\s+"), " ") // Multiple spaces to single
                .replace("-", " ") // Hyphens to spaces

        // Apply singular forms
        val words = formatted.split(" ").toMutableList()
        for (i in words.indices) {
            val lowerWord = words[i].lowercase()

            // Check for singular mappings
            SINGULAR_MAPPINGS[lowerWord]?.let { singular ->
                words[i] = singular
            }

            // Check for equipment abbreviations
            EQUIPMENT_ABBREVIATIONS[lowerWord]?.let { fullName ->
                words[i] = fullName
            }
        }

        // Apply proper case
        formatted =
            words.joinToString(" ") { word ->
                word.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            }

        // Handle special cases
        formatted =
            formatted
                .replace("Ez Bar", "EZ Bar")
                .replace("Db ", "Dumbbell ")
                .replace("Bb ", "Barbell ")
                .replace("Kb ", "Kettlebell ")

        return formatted
    }

    /**
     * Extracts components from an exercise name.
     */
    fun extractComponents(name: String): ExerciseComponents {
        val words = name.lowercase().split(" ")

        var equipment: Equipment? = null
        var muscleGroup: MuscleGroup? = null
        var movement: String? = null

        // Try to identify equipment
        for (word in words) {
            if (EQUIPMENT_KEYWORDS.contains(word)) {
                equipment = mapWordToEquipment(word)
                break
            }
        }

        // Try to identify muscle group
        for (word in words) {
            if (MUSCLE_KEYWORDS.contains(word)) {
                muscleGroup = mapWordToMuscleGroup(word)
                break
            }
        }

        // Try to identify movement
        for (word in words) {
            if (MOVEMENT_KEYWORDS.contains(word)) {
                movement = word
                break
            }
        }

        return ExerciseComponents(
            equipment = equipment,
            muscleGroup = muscleGroup,
            movement = movement,
            category = inferCategory(name, muscleGroup),
            movementPattern = inferMovementPattern(name),
        )
    }

    /**
     * Suggests a correction for an invalid exercise name.
     */
    fun suggestCorrection(name: String): String? {
        val formatted = formatExerciseName(name)

        // If no equipment is detected at the start, try to add it
        val components = extractComponents(formatted)
        if (components.equipment == null) {
            // Default to bodyweight if no equipment detected
            return "Bodyweight $formatted"
        }

        // Ensure equipment comes first
        val words = formatted.split(" ")
        val firstWord = words.firstOrNull()?.lowercase() ?: ""

        if (!EQUIPMENT_KEYWORDS.contains(firstWord)) {
            val detectedEquipment = detectEquipmentInName(formatted)
            if (detectedEquipment != null) {
                return suggestWithEquipmentFirst(formatted, detectedEquipment)
            }
        }

        return formatted
    }

    private fun containsEmoji(text: String): Boolean =
        text.any { char ->
            val type = Character.getType(char).toByte()
            type == Character.SURROGATE || type == Character.OTHER_SYMBOL
        }

    private fun removeEmojis(text: String): String =
        text.filter { char ->
            val type = Character.getType(char).toByte()
            type != Character.SURROGATE && type != Character.OTHER_SYMBOL
        }

    private fun detectEquipmentInName(name: String): String? {
        val lowerName = name.lowercase()
        for (equipment in EQUIPMENT_KEYWORDS) {
            if (lowerName.contains(equipment)) {
                return equipment
            }
        }
        return null
    }

    private fun suggestWithEquipmentFirst(
        name: String,
        equipment: String,
    ): String {
        val words = name.split(" ").toMutableList()
        val equipmentIndex = words.indexOfFirst { it.lowercase() == equipment }

        if (equipmentIndex > 0) {
            // Move equipment to first position
            words.removeAt(equipmentIndex)
            words.add(0, equipment.replaceFirstChar { it.uppercase() })
        }

        return formatExerciseName(words.joinToString(" "))
    }

    private fun mapWordToEquipment(word: String): Equipment =
        when (word.lowercase()) {
            "barbell", "bb" -> Equipment.BARBELL
            "dumbbell", "db" -> Equipment.DUMBBELL
            "cable" -> Equipment.CABLE
            "machine" -> Equipment.MACHINE
            "kettlebell", "kb" -> Equipment.KETTLEBELL
            "bodyweight" -> Equipment.BODYWEIGHT
            "band" -> Equipment.BAND
            "smith" -> Equipment.SMITH_MACHINE
            else -> Equipment.NONE
        }

    private fun mapWordToMuscleGroup(word: String): MuscleGroup =
        when (word.lowercase()) {
            "chest" -> MuscleGroup.CHEST
            "back" -> MuscleGroup.UPPER_BACK
            "shoulder", "delt" -> MuscleGroup.SHOULDERS
            "bicep" -> MuscleGroup.BICEPS
            "tricep" -> MuscleGroup.TRICEPS
            "quad" -> MuscleGroup.QUADS
            "hamstring" -> MuscleGroup.HAMSTRINGS
            "glute" -> MuscleGroup.GLUTES
            "calf" -> MuscleGroup.CALVES
            "ab", "core" -> MuscleGroup.CORE
            "lat" -> MuscleGroup.LATS
            "trap" -> MuscleGroup.TRAPS
            "forearm" -> MuscleGroup.FOREARMS
            else -> MuscleGroup.FULL_BODY
        }

    private fun inferCategory(
        name: String,
        muscleGroup: MuscleGroup?,
    ): ExerciseCategory {
        val lowerName = name.lowercase()

        // Use muscle group if available
        muscleGroup?.let {
            return when (it) {
                MuscleGroup.CHEST -> ExerciseCategory.CHEST
                MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.LOWER_BACK -> ExerciseCategory.BACK
                MuscleGroup.SHOULDERS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS -> ExerciseCategory.SHOULDERS
                MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> ExerciseCategory.ARMS
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES -> ExerciseCategory.LEGS
                MuscleGroup.OBLIQUES, MuscleGroup.CORE -> ExerciseCategory.CORE
                else -> ExerciseCategory.FULL_BODY
            }
        }

        // Infer from name patterns
        return when {
            lowerName.contains("press") && (lowerName.contains("bench") || lowerName.contains("chest")) -> ExerciseCategory.CHEST
            lowerName.contains("fly") || lowerName.contains("flye") -> ExerciseCategory.CHEST
            lowerName.contains("row") || lowerName.contains("pulldown") || lowerName.contains("pull up") -> ExerciseCategory.BACK
            lowerName.contains("curl") && lowerName.contains("bicep") -> ExerciseCategory.ARMS
            lowerName.contains("tricep") || lowerName.contains("pushdown") || lowerName.contains("extension") -> ExerciseCategory.ARMS
            lowerName.contains("squat") || lowerName.contains("lunge") || lowerName.contains("leg") -> ExerciseCategory.LEGS
            lowerName.contains("deadlift") -> ExerciseCategory.LEGS
            lowerName.contains("shoulder") || lowerName.contains("delt") || lowerName.contains("raise") -> ExerciseCategory.SHOULDERS
            lowerName.contains("plank") || lowerName.contains("crunch") || lowerName.contains("ab") -> ExerciseCategory.CORE
            else -> ExerciseCategory.FULL_BODY
        }
    }

    private fun inferMovementPattern(name: String): MovementPattern {
        val lowerName = name.lowercase()

        return when {
            lowerName.contains("squat") -> MovementPattern.SQUAT
            lowerName.contains("deadlift") -> MovementPattern.HINGE
            lowerName.contains("bench") && lowerName.contains("press") -> MovementPattern.HORIZONTAL_PUSH
            lowerName.contains("overhead") || (lowerName.contains("shoulder") && lowerName.contains("press")) -> MovementPattern.VERTICAL_PUSH
            lowerName.contains("row") -> MovementPattern.HORIZONTAL_PULL
            lowerName.contains("pulldown") || lowerName.contains("pull up") || lowerName.contains("chin up") -> MovementPattern.VERTICAL_PULL
            lowerName.contains("lunge") -> MovementPattern.LUNGE
            lowerName.contains("curl") || lowerName.contains("extension") || lowerName.contains("raise") -> MovementPattern.VERTICAL_PULL
            lowerName.contains("carry") -> MovementPattern.CARRY
            else -> MovementPattern.PUSH
        }
    }

    /**
     * Validates an exercise name including checking for duplicates with existing exercises.
     * @param name The exercise name to validate
     * @param existingExercises List of existing exercise names (including aliases) to check against
     * @return ValidationResult indicating if the name is valid or not
     */
    fun validateExerciseNameWithDuplicateCheck(
        name: String,
        existingExercises: List<String>,
    ): ValidationResult {
        // First check for duplicates (case-insensitive) before formatting validation
        // This ensures we catch duplicates even if they have different formatting
        val trimmedName = name.trim()
        val lowerName = trimmedName.lowercase()

        val existingLowerNames = existingExercises.map { it.lowercase() }
        if (lowerName in existingLowerNames) {
            // Find the original casing of the duplicate
            val originalName = existingExercises.firstOrNull { it.lowercase() == lowerName } ?: name
            return ValidationResult.Invalid(
                reason = "An exercise with this name already exists",
                suggestion = "Exercise name '$originalName' is already taken. Please choose a different name.",
            )
        }

        // Then do standard validation if no duplicates found
        val standardValidation = validateExerciseName(name)
        if (standardValidation is ValidationResult.Invalid) {
            return standardValidation
        }

        return ValidationResult.Valid
    }
}

/**
 * Result of exercise name validation.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(
        val reason: String,
        val suggestion: String?,
    ) : ValidationResult()
}

/**
 * Components extracted from an exercise name.
 */
data class ExerciseComponents(
    val equipment: Equipment?,
    val muscleGroup: MuscleGroup?,
    val movement: String?,
    val category: ExerciseCategory,
    val movementPattern: MovementPattern,
)
