package com.github.radupana.featherweight.data.exercise

/**
 * Data class representing an exercise variation with all its related data.
 * This is the primary structure for working with exercises in the new schema.
 */
data class ExerciseWithDetails(
    val variation: ExerciseVariation,
    val muscles: List<VariationMuscle> = emptyList(),
    val aliases: List<VariationAlias> = emptyList(),
    val instructions: List<VariationInstruction> = emptyList(),
    val usageCount: Int = 0, // User-specific usage count
    val isFavorite: Boolean = false, // User-specific favorite status
    val isCustom: Boolean = false, // true if this is a user's custom exercise
) {
    /**
     * Get primary muscles for this variation.
     */
    fun getPrimaryMuscles(): List<MuscleGroup> =
        muscles
            .filter { it.isPrimary }
            .map { it.muscle }

    /**
     * Get secondary muscles for this variation.
     */
    fun getSecondaryMuscles(): List<MuscleGroup> =
        muscles
            .filter { !it.isPrimary }
            .map { it.muscle }
}
