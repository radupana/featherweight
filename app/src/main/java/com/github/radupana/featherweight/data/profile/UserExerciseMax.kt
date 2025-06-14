package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.exercise.Exercise
import java.time.LocalDateTime

@Entity(
    tableName = "user_exercise_maxes",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "exerciseId", "recordedAt"], unique = true),
        Index(value = ["exerciseId"]),
        Index(value = ["userId"]),
    ],
)
data class UserExerciseMax(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val exerciseId: Long,
    val maxWeight: Float, // in kg
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    val notes: String? = null,
    val isEstimated: Boolean = false, // true if calculated from reps, false if actual 1RM test
)
