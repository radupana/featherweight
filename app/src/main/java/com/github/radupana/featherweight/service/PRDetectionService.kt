package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service responsible for detecting weight personal records and creating PR entries.
 * Only tracks weight PRs - when the user lifts more weight than their previous best.
 */
class PRDetectionService(
    private val personalRecordDao: PersonalRecordDao,
    private val setLogDao: SetLogDao,
) {
    /**
     * Checks if a completed set represents a new weight PR and creates record if so.
     * Only detects weight PRs (lifting more weight than previous best).
     * Returns the PersonalRecord if a weight PR was detected, empty list otherwise
     */
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseVariationId: Long,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            val newPRs = mutableListOf<PersonalRecord>()

            if (!setLog.isCompleted || setLog.actualReps <= 0 || setLog.actualWeight <= 0) {
                return@withContext newPRs
            }

            val currentWeight = setLog.actualWeight
            val currentReps = setLog.actualReps
            val currentRpe = setLog.actualRpe

            // Get the actual workout date and ID
            val workoutDateString = setLogDao.getWorkoutDateForSetLog(setLog.id)
            val workoutId = setLogDao.getWorkoutIdForSetLog(setLog.id)
            val currentDate =
                if (workoutDateString != null) {
                    try {
                        LocalDateTime.parse(workoutDateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }
                } else {
                    LocalDateTime.now()
                }

            // Only check for weight PR (higher weight than ever before)
            val weightPR = checkWeightPR(exerciseVariationId, currentWeight, currentReps, currentRpe, currentDate, workoutId)
            weightPR?.let { newPRs.add(it) }

            // Save all detected PRs, but check for duplicates within the same workout
            newPRs.forEach { pr ->

                // Check if there's already a PR for this exercise in this workout
                if (pr.workoutId != null) {
                    val existingPR =
                        personalRecordDao.getPRForExerciseInWorkout(
                            pr.workoutId,
                            pr.exerciseVariationId,
                            pr.recordType,
                        )

                    if (existingPR != null) {
                        // If new PR is better, delete the old one
                        if (pr.weight > existingPR.weight) {
                            personalRecordDao.deletePR(existingPR.id)
                            personalRecordDao.insertPersonalRecord(pr)
                        }
                    } else {
                        // No existing PR for this exercise in this workout
                        personalRecordDao.insertPersonalRecord(pr)
                    }
                } else {
                    // No workoutId (shouldn't happen with new code), just save it
                    personalRecordDao.insertPersonalRecord(pr)
                }
            }

            // Return all detected PRs
            newPRs
        }

    private suspend fun checkWeightPR(
        exerciseVariationId: Long,
        weight: Float,
        reps: Int,
        rpe: Float?,
        date: LocalDateTime,
        workoutId: Long?,
    ): PersonalRecord? {
        val currentMaxWeight = personalRecordDao.getMaxWeightForExercise(exerciseVariationId)

        // Create PR if this is the first record OR if it beats the existing record
        if (currentMaxWeight == null || weight > currentMaxWeight) {
            // Get previous weight PR for context
            val previousPR = personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT)

            val improvementPercentage =
                if (currentMaxWeight != null && currentMaxWeight > 0) {
                    ((weight - currentMaxWeight) / currentMaxWeight) * 100
                } else {
                    100f
                }

            val notes =
                if (currentMaxWeight == null) {
                    "First weight record: ${weight}kg × $reps"
                } else {
                    "New weight PR: ${weight}kg × $reps"
                }

            val roundedWeight = WeightFormatter.roundToNearestQuarter(weight)
            return PersonalRecord(
                exerciseVariationId = exerciseVariationId,
                weight = roundedWeight,
                reps = reps,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.WEIGHT,
                volume = roundedWeight * reps,
                estimated1RM = calculateEstimated1RM(roundedWeight, reps, rpe),
                notes = notes,
                workoutId = workoutId,
            )
        }

        return null
    }

    /**
     * Calculate estimated 1RM using Brzycki formula with RPE consideration
     */
    private fun calculateEstimated1RM(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
    ): Float {
        // Calculate effective reps based on RPE for singles
        val effectiveReps =
            when {
                reps == 1 && rpe != null -> {
                    // For singles with RPE, calculate total possible reps
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f).toInt()
                    reps + repsInReserve // Total reps possible at this weight
                }
                else -> reps
            }

        if (effectiveReps == 1) return weight
        if (effectiveReps > 15) return weight // Formula becomes unreliable beyond 15 reps

        // Brzycki formula: 1RM = weight / (1.0278 - 0.0278 × reps)
        return weight / (1.0278f - 0.0278f * effectiveReps)
    }

    /**
     * Get recent PRs for an exercise
     */
    suspend fun getRecentPRsForExercise(
        exerciseVariationId: Long,
        limit: Int = 5,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRsForExercise(exerciseVariationId, limit)
        }

    /**
     * Get all recent PRs across exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRs(limit)
        }
}
