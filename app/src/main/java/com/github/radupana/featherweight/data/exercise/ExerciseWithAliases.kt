package com.github.radupana.featherweight.data.exercise

/**
 * Exercise with its aliases loaded
 * Used for exercise matching/searching where we need to check aliases
 */
data class ExerciseWithAliases(
    val exercise: Exercise,
    val aliases: List<String> = emptyList(),
) {
    val id: String get() = exercise.id
    val name: String get() = exercise.name
}
