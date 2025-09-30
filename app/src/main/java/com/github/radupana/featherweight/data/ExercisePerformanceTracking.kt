package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * Tracks performance history for exercises within a programme context.
 * This is used for intelligent progression decisions including deloads.
 */
@Entity(
    tableName = "exercise_performance_tracking",
    indices = [androidx.room.Index("userId")],
)
data class ExercisePerformanceTracking(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val programmeId: String,
    val exerciseName: String,
    val targetWeight: Float,
    val achievedWeight: Float,
    val targetSets: Int,
    val completedSets: Int,
    val targetReps: Int?,
    val achievedReps: Int,
    val missedReps: Int,
    val wasSuccessful: Boolean,
    val workoutDate: LocalDateTime,
    val workoutId: String,
    val isDeloadWorkout: Boolean = false,
    val deloadReason: String? = null,
    val averageRpe: Float? = null,
    val notes: String? = null,
)

/**
 * Summary data for tracking consecutive failures and deload status
 */
data class ExerciseProgressionStatus(
    val exerciseName: String,
    val currentWeight: Float,
    val consecutiveFailures: Int,
    val lastSuccessDate: LocalDateTime?,
    val lastDeloadDate: LocalDateTime?,
    val totalDeloads: Int,
    val isInDeloadCycle: Boolean,
    val suggestedAction: ProgressionAction,
)

enum class ProgressionAction {
    PROGRESS,
    MAINTAIN,
    DELOAD,
    RESET,
    TEST_1RM,
}
