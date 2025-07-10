package com.github.radupana.featherweight.data.programme

/**
 * Represents the last performance of an exercise
 */
data class ExercisePerformanceData(
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val sets: Int,
    val workoutDate: java.time.LocalDateTime,
    val allSetsCompleted: Boolean,
)
