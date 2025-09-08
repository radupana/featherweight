package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.programme.WorkoutStructure

data class NextProgrammeWorkoutInfo(
    val actualWeekNumber: Int,
    val workoutStructure: WorkoutStructure,
)
