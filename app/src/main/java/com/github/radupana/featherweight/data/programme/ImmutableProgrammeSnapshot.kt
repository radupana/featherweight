package com.github.radupana.featherweight.data.programme

import kotlinx.serialization.Serializable

@Serializable
data class ImmutableProgrammeSnapshot(
    val programmeId: String,
    val programmeName: String,
    val durationWeeks: Int,
    val capturedAt: String,
    val weeks: List<WeekSnapshot>,
)

@Serializable
data class WeekSnapshot(
    val weekNumber: Int,
    val workouts: List<WorkoutSnapshot>,
)

@Serializable
data class WorkoutSnapshot(
    val dayNumber: Int,
    val workoutName: String?,
    val exercises: List<ExerciseStructure>,
)
