package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import java.time.LocalDateTime

@Entity(
    tableName = "user_exercise_maxes",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["exerciseVariationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exerciseVariationId"], unique = true),
        Index("userId"),
    ],
)
data class UserExerciseMax(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String? = null,
    val exerciseVariationId: Long,
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
