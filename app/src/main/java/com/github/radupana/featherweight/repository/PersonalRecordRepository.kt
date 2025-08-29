package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.service.OneRMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing personal record (PR) database operations.
 * Handles PR detection, storage, and retrieval.
 */
class PersonalRecordRepository(
    private val db: FeatherweightDatabase,
    private val personalRecordDao: PersonalRecordDao,
    private val oneRMService: OneRMService,
) {
    /**
     * Records a new personal record
     */
    suspend fun recordPR(pr: PersonalRecord): Long = personalRecordDao.insertPersonalRecord(pr)

    /**
     * Gets all PRs for an exercise
     */
    suspend fun getPRsForExercise(exerciseVariationId: Long): List<PersonalRecord> =
        personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100)

    /**
     * Gets recent PRs across all exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        personalRecordDao.getRecentPRs(limit)

    /**
     * Gets PRs for a specific workout
     */
    suspend fun getPRsForWorkout(workoutId: Long): List<PersonalRecord> =
        personalRecordDao.getPersonalRecordsForWorkout(workoutId)

    /**
     * Gets the current PR for an exercise by type
     */
    suspend fun getCurrentPR(exerciseVariationId: Long, prType: PRType): PersonalRecord? =
        personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, prType)

    /**
     * Gets PR history for an exercise
     */
    suspend fun getPRHistory(
        exerciseVariationId: Long,
        prType: PRType? = null,
    ): List<PersonalRecord> {
        return if (prType != null) {
            personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100).filter { it.recordType == prType }
        } else {
            personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100)
        }
    }

    /**
     * Checks if a set is a personal record and records it if so
     */
    suspend fun checkAndRecordPR(
        exerciseVariationId: Long,
        setLog: SetLog,
        workoutId: Long,
    ): PersonalRecord? {
        return withContext(Dispatchers.IO) {
            // Verify exercise exists
            val exerciseExists = db.exerciseDao().getExerciseVariationById(exerciseVariationId) != null
            if (!exerciseExists) return@withContext null

            // Check weight PR
            val currentWeightPR = getCurrentPR(exerciseVariationId, PRType.WEIGHT)
            val isWeightPR = currentWeightPR == null || 
                (setLog.actualWeight > currentWeightPR.weight && setLog.actualReps >= currentWeightPR.reps)

            // Check reps PR
            val currentRepsPR = getCurrentPR(exerciseVariationId, PRType.REPS)
            val isRepsPR = currentRepsPR == null || 
                (setLog.actualReps > currentRepsPR.reps && setLog.actualWeight >= currentRepsPR.weight)

            // Check volume PR
            val volume = setLog.actualWeight * setLog.actualReps
            val currentVolumePR = getCurrentPR(exerciseVariationId, PRType.VOLUME)
            val isVolumePR = currentVolumePR == null || volume > currentVolumePR.volume

            // Check estimated 1RM PR
            val estimated1RM = oneRMService.calculateEstimated1RM(setLog.actualWeight, setLog.actualReps)
            val current1RMPR = getCurrentPR(exerciseVariationId, PRType.ESTIMATED_1RM)
            val is1RMPR = current1RMPR == null || 
                (estimated1RM != null && current1RMPR.estimated1RM != null && estimated1RM > current1RMPR.estimated1RM)

            // Determine which type of PR to record (prioritize by significance)
            when {
                isWeightPR -> {
                    val pr = PersonalRecord(
                        exerciseVariationId = exerciseVariationId,
                        weight = setLog.actualWeight,
                        reps = setLog.actualReps,
                        rpe = setLog.actualRpe,
                        recordDate = LocalDateTime.now(),
                        previousWeight = currentWeightPR?.weight,
                        previousReps = currentWeightPR?.reps,
                        previousDate = currentWeightPR?.recordDate,
                        improvementPercentage = calculateImprovement(
                            current = setLog.actualWeight,
                            previous = currentWeightPR?.weight,
                        ),
                        recordType = PRType.WEIGHT,
                        volume = volume,
                        estimated1RM = estimated1RM,
                        workoutId = workoutId,
                    )
                    val id = recordPR(pr)
                    pr.copy(id = id)
                }
                isRepsPR -> {
                    val pr = PersonalRecord(
                        exerciseVariationId = exerciseVariationId,
                        weight = setLog.actualWeight,
                        reps = setLog.actualReps,
                        rpe = setLog.actualRpe,
                        recordDate = LocalDateTime.now(),
                        previousWeight = currentRepsPR?.weight,
                        previousReps = currentRepsPR?.reps,
                        previousDate = currentRepsPR?.recordDate,
                        improvementPercentage = calculateImprovement(
                            current = setLog.actualReps.toFloat(),
                            previous = currentRepsPR?.reps?.toFloat(),
                        ),
                        recordType = PRType.REPS,
                        volume = volume,
                        estimated1RM = estimated1RM,
                        workoutId = workoutId,
                    )
                    val id = recordPR(pr)
                    pr.copy(id = id)
                }
                isVolumePR -> {
                    val pr = PersonalRecord(
                        exerciseVariationId = exerciseVariationId,
                        weight = setLog.actualWeight,
                        reps = setLog.actualReps,
                        rpe = setLog.actualRpe,
                        recordDate = LocalDateTime.now(),
                        previousWeight = currentVolumePR?.weight,
                        previousReps = currentVolumePR?.reps,
                        previousDate = currentVolumePR?.recordDate,
                        improvementPercentage = calculateImprovement(
                            current = volume,
                            previous = currentVolumePR?.volume,
                        ),
                        recordType = PRType.VOLUME,
                        volume = volume,
                        estimated1RM = estimated1RM,
                        workoutId = workoutId,
                    )
                    val id = recordPR(pr)
                    pr.copy(id = id)
                }
                is1RMPR && estimated1RM != null -> {
                    val pr = PersonalRecord(
                        exerciseVariationId = exerciseVariationId,
                        weight = setLog.actualWeight,
                        reps = setLog.actualReps,
                        rpe = setLog.actualRpe,
                        recordDate = LocalDateTime.now(),
                        previousWeight = current1RMPR?.weight,
                        previousReps = current1RMPR?.reps,
                        previousDate = current1RMPR?.recordDate,
                        improvementPercentage = calculateImprovement(
                            current = estimated1RM,
                            previous = current1RMPR?.estimated1RM,
                        ),
                        recordType = PRType.ESTIMATED_1RM,
                        volume = volume,
                        estimated1RM = estimated1RM,
                        workoutId = workoutId,
                    )
                    val id = recordPR(pr)
                    pr.copy(id = id)
                }
                else -> null
            }
        }
    }

    /**
     * Gets PR summary statistics for an exercise
     */
    suspend fun getPRSummary(exerciseVariationId: Long): PRSummary {
        return withContext(Dispatchers.IO) {
            val weightPR = getCurrentPR(exerciseVariationId, PRType.WEIGHT)
            val repsPR = getCurrentPR(exerciseVariationId, PRType.REPS)
            val volumePR = getCurrentPR(exerciseVariationId, PRType.VOLUME)
            val estimated1RMPR = getCurrentPR(exerciseVariationId, PRType.ESTIMATED_1RM)
            
            val allPRs = getPRsForExercise(exerciseVariationId)
            val totalPRs = allPRs.size
            val lastPRDate = allPRs.maxByOrNull { it.recordDate }?.recordDate

            PRSummary(
                exerciseVariationId = exerciseVariationId,
                maxWeight = weightPR?.weight,
                maxReps = repsPR?.reps,
                maxVolume = volumePR?.volume,
                estimated1RM = estimated1RMPR?.estimated1RM,
                totalPRs = totalPRs,
                lastPRDate = lastPRDate,
                recentPRs = allPRs.sortedByDescending { it.recordDate }.take(5),
            )
        }
    }

    /**
     * Gets PR timeline for an exercise
     */
    fun getPRTimelineFlow(exerciseVariationId: Long): Flow<List<PersonalRecord>> =
        personalRecordDao.getPRHistoryForExercise(exerciseVariationId)

    /**
     * Deletes a personal record
     */
    suspend fun deletePR(prId: Long) = personalRecordDao.deletePR(prId)

    /**
     * Gets PRs within a date range
     */
    suspend fun getPRsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PersonalRecord> = personalRecordDao.getPersonalRecordsInDateRange(startDate, endDate)

    /**
     * Gets the most impressive PRs (biggest improvements)
     */
    suspend fun getMostImpressivePRs(limit: Int = 5): List<PersonalRecord> =
        personalRecordDao.getRecentPRs(limit).sortedByDescending { it.improvementPercentage }

    /**
     * Checks if user has any PRs
     */
    suspend fun hasAnyPRs(): Boolean = personalRecordDao.getAllPersonalRecords().isNotEmpty()

    /**
     * Gets total PR count
     */
    suspend fun getTotalPRCount(): Int = personalRecordDao.getAllPersonalRecords().size

    /**
     * Gets PR count for an exercise
     */
    suspend fun getPRCountForExercise(exerciseVariationId: Long): Int =
        personalRecordDao.getPRCountForExercise(exerciseVariationId)

    // Helper functions

    private fun calculateImprovement(current: Float?, previous: Float?): Float {
        return if (previous != null && previous > 0 && current != null) {
            ((current - previous) / previous) * 100
        } else {
            0f
        }
    }

    /**
     * Data class for PR summary information
     */
    data class PRSummary(
        val exerciseVariationId: Long,
        val maxWeight: Float?,
        val maxReps: Int?,
        val maxVolume: Float?,
        val estimated1RM: Float?,
        val totalPRs: Int,
        val lastPRDate: LocalDateTime?,
        val recentPRs: List<PersonalRecord>,
    )
}
