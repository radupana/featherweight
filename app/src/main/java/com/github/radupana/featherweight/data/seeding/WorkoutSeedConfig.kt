package com.github.radupana.featherweight.data.seeding

data class WorkoutSeedConfig(
    val squatRM: Float,
    val benchRM: Float,
    val deadliftRM: Float,
    val ohpRM: Float,
    val numberOfWorkouts: Int,
    val workoutsPerWeek: Int,
    val programStyle: String,
    val includeFailures: Boolean,
    val includeVariation: Boolean,
)
