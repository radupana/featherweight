package com.github.radupana.featherweight.data

import androidx.room.Embedded

/**
 * Data class that combines ExerciseLog with the exercise name from ExerciseVariation.
 * Used for efficient display in UI without needing separate lookups.
 */
data class ExerciseLogWithName(
    @Embedded val exerciseLog: ExerciseLog,
    val exerciseName: String
)