package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

enum class PRType {
    WEIGHT, // Higher weight lifted (for weighted exercises)
    ESTIMATED_1RM, // Higher estimated 1 rep max
    REPS, // More reps at bodyweight (for bodyweight exercises)
}

@Entity(
    tableName = "personal_records",
    indices = [
        Index(value = ["exerciseId"]),
        Index("userId"),
        Index("recordDate"),
        Index("sourceSetId"),
    ],
)
data class PersonalRecord(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val exerciseId: String,
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
    val workoutId: String? = null,
    val sourceSetId: String? = null,
)
