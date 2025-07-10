package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tracks performance history for exercises within a programme context.
 * This is used for intelligent progression decisions including deloads.
 */
@Entity(tableName = "exercise_performance_tracking")
data class ExercisePerformanceTracking(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Programme context
    val programmeId: Long,
    val exerciseName: String,
    // Performance metrics
    val targetWeight: Float,
    val achievedWeight: Float,
    val targetSets: Int,
    val completedSets: Int,
    val targetReps: Int,
    val achievedReps: Int, // Total across all sets
    val missedReps: Int, // Total missed reps
    // Success/Failure tracking
    val wasSuccessful: Boolean,
    val workoutDate: LocalDateTime,
    val workoutId: Long,
    // Deload tracking
    val isDeloadWorkout: Boolean = false,
    val deloadReason: String? = null,
    // Additional context
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
    PROGRESS, // Increase weight as planned
    MAINTAIN, // Keep same weight
    DELOAD, // Reduce weight
    RESET, // Major reset needed
    TEST_1RM, // Time to test new max
}
