package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

@Entity(
    tableName = "exercise_logs",
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId"]),
        Index("userId"),
        Index(value = ["userId", "exerciseId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseLog(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val workoutId: String,
    val exerciseId: String, // References exercises table
    val exerciseOrder: Int,
    val notes: String? = null,
    val originalExerciseId: String? = null,
    val isSwapped: Boolean = false, // Flag to indicate if exercise was swapped
)
