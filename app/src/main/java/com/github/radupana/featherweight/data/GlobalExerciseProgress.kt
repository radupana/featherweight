package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class ProgressTrend {
    IMPROVING,
    STALLING,
    DECLINING
}

enum class VolumeTrend {
    INCREASING,
    MAINTAINING,
    DECREASING
}

@Entity(tableName = "global_exercise_progress")
data class GlobalExerciseProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val exerciseName: String,
    val currentWorkingWeight: Float,      // Most recent working weight
    val estimatedMax: Float,               // Calculated or from UserExerciseMax
    val lastUpdated: LocalDateTime,
    val recentAvgRpe: Float? = null,      // Average RPE from last 5 sessions
    val consecutiveStalls: Int = 0,       // Sessions at same weight
    val lastPrDate: LocalDateTime? = null, // When they last hit a PR
    val lastPrWeight: Float? = null,      // Their last PR weight
    val trend: ProgressTrend = ProgressTrend.STALLING,
    val volumeTrend: VolumeTrend? = null,
    val totalVolumeLast30Days: Float = 0f, // Sets × reps × weight
    
    // Additional tracking fields
    val sessionsTracked: Int = 0,         // Total sessions for this exercise
    val bestSingleRep: Float? = null,     // Actual 1RM if performed
    val best3Rep: Float? = null,          // Best 3RM
    val best5Rep: Float? = null,          // Best 5RM
    val best8Rep: Float? = null,          // Best 8RM
    val lastSessionVolume: Float? = null, // Volume from most recent session
    val avgSessionVolume: Float? = null,  // Average volume per session
    
    // Stall detection
    val weeksAtCurrentWeight: Int = 0,   // How long stuck at current weight
    val lastProgressionDate: LocalDateTime? = null, // Last time weight increased
    val failureStreak: Int = 0,           // Consecutive sessions with missed reps
    
    // Cross-workout tracking
    val lastProgrammeWorkoutId: Long? = null,  // Track if from programme
    val lastFreestyleWorkoutId: Long? = null   // Track if from freestyle
)