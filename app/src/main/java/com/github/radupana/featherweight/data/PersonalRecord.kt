package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class PRType {
    WEIGHT, // Higher weight lifted
    ESTIMATED_1RM, // Higher estimated 1 rep max
}

@Entity(
    indices = [androidx.room.Index(value = ["exerciseVariationId"]), androidx.room.Index("userId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = com.github.radupana.featherweight.data.exercise.ExerciseVariation::class,
            parentColumns = ["id"],
            childColumns = ["exerciseVariationId"],
            onDelete = androidx.room.ForeignKey.RESTRICT,
        ),
    ],
)
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String? = null,
    val exerciseVariationId: Long,
    val weight: Float,
    val reps: Int,
    val rpe: Float? = null,
    val recordDate: LocalDateTime,
    val previousWeight: Float?,
    val previousReps: Int?,
    val previousDate: LocalDateTime?,
    val improvementPercentage: Float,
    val recordType: PRType,
    val volume: Float = weight * reps,
    val estimated1RM: Float? = null,
    val notes: String? = null,
    val workoutId: Long? = null,
)
