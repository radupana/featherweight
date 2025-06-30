package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "exercise_swap_history",
    indices = [
        Index(value = ["userId", "originalExerciseId"]),
        Index(value = ["userId", "swappedToExerciseId"]),
        Index(value = ["swapDate"])
    ]
)
data class ExerciseSwapHistory(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val userId: Long,
    val originalExerciseId: Long,
    val swappedToExerciseId: Long,
    val swapDate: LocalDateTime,
    val workoutId: Long? = null,
    val programmeId: Long? = null
)