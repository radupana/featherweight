package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class PRType {
    WEIGHT,     // Higher weight for same reps
    REPS,       // More reps with same weight
    VOLUME,     // Total volume (weight × reps)
    ESTIMATED_1RM // Estimated 1RM improvement
}

@Entity
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val recordDate: LocalDateTime,
    val previousWeight: Float?,
    val previousReps: Int?,
    val previousDate: LocalDateTime?,
    val improvementPercentage: Float,
    val recordType: PRType,
    val volume: Float = weight * reps, // Calculated field for convenience
    val estimated1RM: Float? = null,   // Optional 1RM calculation
    val notes: String? = null          // Optional context about the PR
)