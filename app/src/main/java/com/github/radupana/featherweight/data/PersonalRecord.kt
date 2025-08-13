package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class PRType {
    WEIGHT, // Higher weight for same reps
    REPS, // More reps with same weight
    VOLUME, // Total volume (weight × reps)
    ESTIMATED_1RM, // Estimated 1RM improvement
}

@Entity(
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
