package com.github.radupana.featherweight.data.exercise

/**
 * Data class representing an exercise variation with all its related data.
 * This is the primary structure for working with exercises in the new schema.
 */
data class ExerciseWithDetails(
    val variation: Exercise,
    val muscles: List<ExerciseMuscle> = emptyList(),
    val aliases: List<ExerciseAlias> = emptyList(),
    val instructions: List<ExerciseInstruction> = emptyList(),
    val usageCount: Int = 0, // User-specific usage count
) {
    // Derived property: true if this is a user's custom exercise (type = USER)
    val isCustom: Boolean get() = variation.type == ExerciseType.USER.name

    /**
     * Get primary muscles for this variation.
     */
    fun getPrimaryMuscles(): List<String> =
        muscles
            .filter { it.targetType == "PRIMARY" }
            .map { it.muscle }

    /**
     * Get secondary muscles for this variation.
     */
    fun getSecondaryMuscles(): List<String> =
        muscles
            .filter { it.targetType == "SECONDARY" }
            .map { it.muscle }
}
