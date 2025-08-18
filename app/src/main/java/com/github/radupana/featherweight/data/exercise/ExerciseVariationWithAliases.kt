package com.github.radupana.featherweight.data.exercise

/**
 * ExerciseVariation with its aliases loaded
 * Used for exercise matching/searching where we need to check aliases
 */
data class ExerciseVariationWithAliases(
    val variation: ExerciseVariation,
    val aliases: List<String> = emptyList()
) {
    val id: Long get() = variation.id
    val name: String get() = variation.name
}
