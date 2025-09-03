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
