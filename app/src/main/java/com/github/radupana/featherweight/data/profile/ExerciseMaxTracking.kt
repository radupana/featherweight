package com.github.radupana.featherweight.data.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * Exercise max history tracking table that stores all max records over time.
 * The "current" max is simply the most recent entry for a given exercise.
 * Tracks both estimated 1RMs (calculated from multi-rep sets) and actual 1RMs.
 */
@Entity(
    tableName = "exercise_max_history",
    indices = [
        Index(value = ["exerciseId", "recordedAt"]),
        Index(value = ["userId"]),
        Index(value = ["userId", "exerciseId", "sourceSetId"], unique = true),
    ],
)
data class ExerciseMaxTracking(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val exerciseId: String,
    // Core 1RM data (from OneRMHistory)
    val oneRMEstimate: Float,
    val context: String,
    val sourceSetId: String? = null,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    // Extended fields (from UserExerciseMax)
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float? = null,
    val mostWeightDate: LocalDateTime = LocalDateTime.now(),
    val oneRMConfidence: Float,
    val oneRMType: OneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
    val notes: String? = null,
)
