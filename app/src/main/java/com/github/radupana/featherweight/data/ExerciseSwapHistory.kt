package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

@Entity(
    tableName = "exercise_swap_history",
    indices = [
        Index(value = ["originalExerciseId"]),
        Index(value = ["swappedToExerciseId"]),
        Index(value = ["swapDate"]),
        Index("userId"),
        Index(
            value = ["userId", "originalExerciseId", "swappedToExerciseId", "workoutId"],
            unique = true,
        ),
    ],
)
data class ExerciseSwapHistory(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val originalExerciseId: String,
    val swappedToExerciseId: String,
    val swapDate: LocalDateTime,
    val workoutId: String? = null,
    val programmeId: String? = null,
)
