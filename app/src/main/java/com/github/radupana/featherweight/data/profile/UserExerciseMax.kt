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
        Index(value = ["userId", "exerciseId"], unique = true),
        Index(value = ["exerciseId"]),
        Index(value = ["userId"]),
    ],
)
data class UserExerciseMax(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val exerciseId: Long,
    // Most weight ever lifted
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float? = null,
    val mostWeightDate: LocalDateTime = LocalDateTime.now(),
    // All-time best 1RM estimate
    val oneRMEstimate: Float,
    val oneRMContext: String, // e.g., "140kg × 3 @ RPE 8"
    val oneRMConfidence: Float, // 0.0 to 1.0
    val oneRMDate: LocalDateTime = LocalDateTime.now(),
    val oneRMType: OneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
    val notes: String? = null,
)
