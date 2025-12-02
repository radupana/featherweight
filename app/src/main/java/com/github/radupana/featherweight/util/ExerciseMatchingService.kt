package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases

/**
 * Service responsible for fuzzy matching exercise names to exercises in the database.
 * Extracted from ImportProgrammeViewModel for testability.
 */
object ExerciseMatchingService {
    /**
     * Finds the best matching exercise for a given name.
     * Tries multiple strategies in order of preference/confidence.
     *
     * @param exerciseName The exercise name to match (e.g., "Barbell Shoulder Press")
     * @param allExercises All available exercises with their aliases
     * @return The matched exercise ID, or null if no match found
     */
    fun findBestExerciseMatch(
        exerciseName: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val nameLower = exerciseName.lowercase().trim()

        // Try matching strategies in order of preference
        return tryExactNameMatch(nameLower, allExercises)
            ?: tryExactAliasMatch(nameLower, allExercises)
            ?: tryEquipmentPlusAliasMatch(nameLower, allExercises)
            ?: tryImportantWordsMatch(nameLower, allExercises)
            ?: tryVariationMatch(nameLower, allExercises)
            ?: tryNormalizedMatch(nameLower, allExercises)
            ?: tryEquipmentStrippedMatch(nameLower, allExercises)
            ?: tryAbbreviationMatch(nameLower, allExercises)
    }

    private fun tryExactNameMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? = allExercises.find { it.name.lowercase() == nameLower }?.id

    private fun tryExactAliasMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? =
        allExercises
            .find { exercise ->
                exercise.aliases.any { alias -> alias.lowercase() == nameLower }
            }?.id

    /**
     * Matches "barbell shoulder press" → exercise with alias "shoulder press" AND equipment BARBELL.
     * This handles the case where AI parser outputs "{equipment} {alias}" but the alias doesn't
     * include the equipment prefix.
     */
    private fun tryEquipmentPlusAliasMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val inputEquipment = extractEquipment(nameLower) ?: return null
        val nameWithoutEquipment = stripEquipmentFromName(nameLower)

        return allExercises
            .find { exercise ->
                val exerciseEquipment = exercise.exercise.equipment.lowercase()
                val equipmentMatches = exerciseEquipment == inputEquipment

                equipmentMatches &&
                    exercise.aliases.any { alias ->
                        alias.lowercase() == nameWithoutEquipment
                    }
            }?.id
    }

    private fun tryImportantWordsMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val inputEquipment = extractEquipment(nameLower)
        val importantWords = nameLower.split(" ").filter { it.length > 2 }

        return allExercises
            .find { exercise ->
                val exerciseLower = exercise.name.lowercase()
                val exerciseEquipment = extractEquipment(exerciseLower)
                val equipmentOk = isEquipmentCompatible(inputEquipment, exerciseEquipment)
                equipmentOk && importantWords.all { word -> exerciseLower.contains(word) }
            }?.id
    }

    internal fun isEquipmentCompatible(
        inputEquipment: String?,
        exerciseEquipment: String?,
    ): Boolean =
        when {
            // If input has no equipment specified, any exercise is compatible
            inputEquipment == null -> true
            // If exercise has no equipment (bodyweight), it's compatible with anything
            exerciseEquipment == null -> true
            // Equipment must match exactly - barbell != cable, dumbbell != band, etc.
            else -> inputEquipment == exerciseEquipment
        }

    private fun tryVariationMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        if (!nameLower.contains(" or ")) return null

        val variations = nameLower.split(" or ")
        for (variation in variations) {
            val varTrimmed = variation.trim()
            allExercises
                .find { exercise ->
                    exercise.name.lowercase().contains(varTrimmed)
                }?.let { return it.id }
        }
        return null
    }

    private fun tryNormalizedMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val nameWithVariations = nameLower.replace("weighted ", "").trim()
        return allExercises
            .find {
                it.name.lowercase().contains(nameWithVariations)
            }?.id
    }

    private fun tryEquipmentStrippedMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val inputEquipment = extractEquipment(nameLower)
        val nameWithoutEquipment = stripEquipmentFromName(nameLower)

        // If input specifies equipment, only match exercises with the SAME equipment
        // This prevents "barbell shoulder press" from matching "cable shoulder press"
        if (inputEquipment != null) {
            return allExercises
                .find { exercise ->
                    val exerciseEquipment = exercise.exercise.equipment.lowercase()
                    val exName = exercise.name.lowercase()
                    exerciseEquipment == inputEquipment &&
                        (
                            exName.endsWith(nameWithoutEquipment) ||
                                stripEquipmentFromName(exName) == nameWithoutEquipment
                        )
                }?.id
        }

        // If no equipment specified in input, find any exercise ending with the core movement
        return allExercises
            .find {
                it.name.lowercase().endsWith(nameWithoutEquipment)
            }?.id
    }

    private fun tryAbbreviationMatch(
        nameLower: String,
        allExercises: List<ExerciseWithAliases>,
    ): String? {
        val normalizedName = normalizeExerciseName(nameLower)
        return allExercises
            .find {
                normalizeExerciseName(it.name.lowercase()) == normalizedName
            }?.id
    }

    internal fun stripEquipmentFromName(name: String): String =
        name
            .replace("barbell ", "")
            .replace("dumbbell ", "")
            .replace("db ", "")
            .replace("cable ", "")
            .replace("machine ", "")
            .replace("smith ", "")
            .replace("kettlebell ", "")
            .replace("kb ", "")
            .replace("band ", "")
            .replace("resistance ", "")
            .replace("paused ", "")
            .replace("pin ", "")
            .trim()

    internal fun extractEquipment(exerciseName: String): String? {
        val name = exerciseName.lowercase()
        return when {
            name.startsWith("barbell ") || name.startsWith("bb ") -> "barbell"
            name.startsWith("dumbbell ") || name.startsWith("db ") -> "dumbbell"
            name.startsWith("cable ") -> "cable"
            name.startsWith("machine ") -> "machine"
            name.startsWith("smith ") -> "smith"
            name.startsWith("kettlebell ") || name.startsWith("kb ") -> "kettlebell"
            name.startsWith("band ") || name.startsWith("resistance ") -> "band"
            name.startsWith("weighted ") -> "weighted"
            else -> null
        }
    }

    private fun normalizeExerciseName(name: String): String =
        name
            .replace("ohp", "overhead press")
            .replace("rdl", "romanian deadlift")
            .replace("sldl", "stiff leg deadlift")
            .replace("ghr", "glute ham raise")
            .replace("db", "dumbbell")
            .replace("bb", "barbell")
            .replace("kb", "kettlebell")
            .replace("&", "and")
            .replace("-", " ")
            .replace("  ", " ")
            .trim()
}
