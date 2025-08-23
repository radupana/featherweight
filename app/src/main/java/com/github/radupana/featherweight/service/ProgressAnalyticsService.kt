package com.github.radupana.featherweight.service

import java.time.LocalDateTime

/**
 * Data classes for progress analytics
 */
data class ExerciseProgressData(
    val exerciseName: String,
    val dataPoints: List<ProgressDataPoint>,
    val currentMax: Float,
    val progressPercentage: Float,
    val totalSessions: Int,
)

data class ProgressDataPoint(
    val date: LocalDateTime,
    val weight: Float,
    val volume: Float,
    val rpe: Float?,
)

data class ExerciseSummary(
    val exerciseName: String,
    val currentMax: Float,
    val progressPercentage: Float,
    val lastWorkout: LocalDateTime?,
    val miniChartData: List<Float>,
    val sessionCount: Int,
)

data class GroupedExerciseSummary(
    val bigFourExercises: List<ExerciseSummary>,
    val otherExercises: List<ExerciseSummary>,
)

data class PerformanceStats(
    val exerciseName: String,
    val bestSingle: String?,
    val bestVolume: String?,
    val averageRpe: Float?,
    val consistency: Float, // 0-1 score based on workout frequency
    val totalSessions: Int,
)
