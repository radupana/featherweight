package com.github.radupana.featherweight.data.programme

import java.time.Duration
import java.time.LocalDateTime

data class ProgrammeCompletionStats(
    val programmeId: Long,
    val programmeName: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val totalWorkouts: Int,
    val completedWorkouts: Int,
    val totalVolume: Float,
    val averageWorkoutDuration: Duration,
    val totalPRs: Int,
    val strengthImprovements: List<StrengthImprovement>,
    val averageStrengthImprovement: Float,
    val insights: ProgrammeInsights
)

data class StrengthImprovement(
    val exerciseName: String,
    val startingMax: Float,
    val endingMax: Float,
    val improvementKg: Float,
    val improvementPercentage: Float
)

data class ProgrammeInsights(
    val totalTrainingDays: Int,
    val mostConsistentDay: String?, // "Monday", "Tuesday", etc.
    val averageRestDaysBetweenWorkouts: Float
)