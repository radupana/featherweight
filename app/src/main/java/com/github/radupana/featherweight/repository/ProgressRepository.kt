package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ExercisePerformanceTracking
import com.github.radupana.featherweight.data.ExercisePerformanceTrackingDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.service.FreestyleIntelligenceService
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.OneRMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing progress tracking and performance data.
 * Handles global progress, performance tracking, and progression calculations.
 */
class ProgressRepository(
    @Suppress("UNUSED_PARAMETER") private val db: FeatherweightDatabase,
    private val globalProgressDao: GlobalExerciseProgressDao,
    private val performanceDao: ExercisePerformanceTrackingDao,
    private val globalProgressTracker: GlobalProgressTracker,
    @Suppress("UNUSED_PARAMETER") private val oneRMService: OneRMService,
    @Suppress("UNUSED_PARAMETER") private val freestyleIntelligenceService: FreestyleIntelligenceService,
) {
    /**
     * Gets global progress for an exercise
     */
    suspend fun getGlobalExerciseProgress(exerciseVariationId: Long): GlobalExerciseProgress? = globalProgressDao.getProgressForExercise(exerciseVariationId)

    /**
     * Updates global exercise progress
     */
    suspend fun updateGlobalProgress(progress: GlobalExerciseProgress) = globalProgressDao.insertOrUpdate(progress)

    /**
     * Gets all global progress records
     */
    suspend fun getAllGlobalProgress(): List<GlobalExerciseProgress> = globalProgressDao.getAllProgress()

    /**
     * Gets progress flow for an exercise
     */
    fun getProgressFlow(exerciseVariationId: Long): Flow<GlobalExerciseProgress?> = globalProgressDao.observeProgressForExercise(exerciseVariationId)

    /**
     * Updates progress based on completed workout
     */
    suspend fun updateProgressFromWorkout(workoutId: Long) {
        globalProgressTracker.updateProgressAfterWorkout(workoutId)
    }

    /**
     * Gets performance tracking data for a programme and exercise
     */
    suspend fun getPerformanceTracking(
        programmeId: Long,
        exerciseName: String,
    ): ExercisePerformanceTracking? = performanceDao.getLastSuccess(programmeId, exerciseName)

    /**
     * Records performance tracking data
     */
    suspend fun recordPerformanceTracking(tracking: ExercisePerformanceTracking) = performanceDao.insertPerformanceRecord(tracking)

    /**
     * Gets consecutive failures for an exercise in a programme
     */
    suspend fun getConsecutiveFailures(
        programmeId: Long,
        exerciseName: String,
    ): Int = performanceDao.getConsecutiveFailures(programmeId, exerciseName)

    /**
     * Gets the last deload for an exercise in a programme
     */
    suspend fun getLastDeload(
        programmeId: Long,
        exerciseName: String,
    ): ExercisePerformanceTracking? = performanceDao.getLastDeload(programmeId, exerciseName)

    /**
     * Gets total deloads for an exercise in a programme
     */
    suspend fun getTotalDeloads(
        programmeId: Long,
        exerciseName: String,
    ): Int = performanceDao.getTotalDeloads(programmeId, exerciseName)

    /**
     * Updates estimated max for an exercise
     */
    suspend fun updateEstimatedMax(
        exerciseVariationId: Long,
        newMax: Float,
    ) {
        globalProgressDao.updateEstimatedMax(
            exerciseVariationId = exerciseVariationId,
            newMax = newMax,
            updateTime = LocalDateTime.now(),
        )
    }

    /**
     * Records a new PR
     */
    suspend fun recordNewPR(
        exerciseVariationId: Long,
        prWeight: Float,
    ) {
        globalProgressDao.recordNewPR(
            exerciseVariationId = exerciseVariationId,
            prWeight = prWeight,
            prDate = LocalDateTime.now(),
            updateTime = LocalDateTime.now(),
        )
    }

    /**
     * Increments stall count for an exercise
     */
    suspend fun incrementStallCount(
        exerciseVariationId: Long,
        weeksAtWeight: Int,
    ) {
        globalProgressDao.incrementStallCount(
            exerciseVariationId = exerciseVariationId,
            weeksAtWeight = weeksAtWeight,
            updateTime = LocalDateTime.now(),
        )
    }

    /**
     * Records progression for an exercise
     */
    suspend fun recordProgression(
        exerciseVariationId: Long,
        newWeight: Float,
    ) {
        globalProgressDao.recordProgression(
            exerciseVariationId = exerciseVariationId,
            newWeight = newWeight,
            progressDate = LocalDateTime.now(),
            updateTime = LocalDateTime.now(),
        )
    }

    /**
     * Gets stalled exercises
     */
    suspend fun getStalledExercises(minStalls: Int = 3): List<GlobalExerciseProgress> = globalProgressDao.getStalledExercises(minStalls)

    /**
     * Gets top volume exercises
     */
    suspend fun getTopVolumeExercises(limit: Int = 10): List<GlobalExerciseProgress> = globalProgressDao.getTopVolumeExercises(limit)

    /**
     * Gets neglected exercises
     */
    suspend fun getNeglectedExercises(daysSinceLastWorkout: Int = 14): List<GlobalExerciseProgress> {
        val cutoffDate = LocalDateTime.now().minusDays(daysSinceLastWorkout.toLong())
        return globalProgressDao.getNeglectedExercises(cutoffDate)
    }

    /**
     * Deletes all progress data
     */
    suspend fun deleteAllProgress() {
        withContext(Dispatchers.IO) {
            globalProgressDao.deleteAllGlobalProgress()
        }
    }

    /**
     * Deletes performance history for a programme
     */
    suspend fun deletePerformanceHistoryForProgramme(programmeId: Long) {
        performanceDao.deletePerformanceHistoryForProgramme(programmeId)
    }
}
