package com.github.radupana.featherweight.domain

data class ExerciseStats(
    val exerciseName: String,
    val avgWeight: Float,
    val avgReps: Int,
    val avgRpe: Float?,
    val maxWeight: Float,
    val totalSets: Int,
)
