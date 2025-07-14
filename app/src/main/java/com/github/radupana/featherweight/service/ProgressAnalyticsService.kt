package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.repository.FeatherweightRepository
import java.time.LocalDateTime

/**
 * Service for calculating analytics and progress metrics
 */
class ProgressAnalyticsService(
    private val repository: FeatherweightRepository,
) {
    /**
     * Get exercise progress data for charts
     */
    suspend fun getExerciseProgressData(
        exerciseName: String,
        days: Int = 90,
    ): ExerciseProgressData {
        // For now, return sample data since we need to implement proper data fetching
        val dataPoints = emptyList<ProgressDataPoint>()

        return ExerciseProgressData(
            exerciseName = exerciseName,
            dataPoints = dataPoints,
            currentMax = 0f,
            progressPercentage = 0f,
            totalSessions = 0,
        )
    }

    /**
     * Get all exercises with basic progress info for the exercises list
     */
    suspend fun getAllExercisesSummary(): List<ExerciseSummary> {
        // For now, return empty list - will implement proper data fetching later
        return emptyList()
    }

    /**
     * Calculate weekly volume for an exercise
     */
    suspend fun calculateWeeklyVolume(exerciseName: String): Float {
        // For now, return 0 - will implement proper calculation later
        return 0f
    }

    /**
     * Get performance statistics for an exercise
     */
    suspend fun getPerformanceStats(exerciseName: String): PerformanceStats {
        // For now, return basic stats - will implement proper calculation later
        return PerformanceStats(
            exerciseName = exerciseName,
            bestSingle = null,
            bestVolume = null,
            averageRpe = null,
            consistency = 0f,
            totalSessions = 0,
        )
    }

    private fun calculateProgressPercentage(dataPoints: List<ProgressDataPoint>): Float {
        if (dataPoints.size < 2) return 0f

        val start = dataPoints.first().weight
        val end = dataPoints.last().weight

        return if (start > 0) {
            ((end - start) / start) * 100
        } else {
            0f
        }
    }

    private fun generateMiniChartData(sets: List<SetLog>): List<Float> {
        // For now, return empty list - will implement proper chart data later
        return emptyList()
    }
}

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
