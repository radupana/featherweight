package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow

/**
 * Service responsible for detecting weight personal records and creating PR entries.
 * Only tracks weight PRs - when the user lifts more weight than their previous best.
 */
class PRDetectionService(
    private val personalRecordDao: PersonalRecordDao,
    private val setLogDao: SetLogDao,
    private val exerciseVariationDao: ExerciseVariationDao,
) {
    companion object {
        private const val TAG = "PRDetectionService"
    }

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
                    } catch (e: java.time.format.DateTimeParseException) {
                        Log.w(TAG, "Failed to parse workout date for setLog: ${setLog.id}, using current time", e)
                        LocalDateTime.now()
                    }
                } else {
                    LocalDateTime.now()
                }

            // Get the exercise variation to determine scaling type
            val exerciseVariation = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
            val scalingType = exerciseVariation?.rmScalingType ?: RMScalingType.STANDARD

            Log.d("PRDetection", "Checking PRs for exercise $exerciseVariationId: ${currentWeight}kg × $currentReps @ RPE $currentRpe")

            // Calculate estimated 1RM for this set
            val estimated1RM = calculateEstimated1RM(currentWeight, currentReps, currentRpe, scalingType)

            if (estimated1RM != null) {
                Log.d("PRDetection", "Calculated 1RM: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg")
            } else {
                Log.d("PRDetection", "No 1RM calculated (RPE too low or other criteria not met)")
            }

            // Get the current max estimated 1RM for comparison
            val currentMax1RM = personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId)
            Log.d("PRDetection", "Current max 1RM in database: ${currentMax1RM?.let { WeightFormatter.formatDecimal(it, 2) } ?: "None"}kg")

            // Check for weight PR (higher absolute weight than ever before)
            val weightPR = checkWeightPR(exerciseVariationId, currentWeight, currentReps, currentRpe, currentDate, workoutId, estimated1RM, currentMax1RM)
            weightPR?.let {
                Log.d("PRDetection", "Weight PR detected: ${it.weight}kg × ${it.reps}")
                newPRs.add(it)
            }

            // Check for estimated 1RM PR (higher estimated 1RM than ever before)
            // Only check if we actually calculated a 1RM
            if (estimated1RM != null && estimated1RM > (currentMax1RM ?: 0f)) {
                Log.d("PRDetection", "New 1RM PR detected: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg > ${currentMax1RM ?: 0}kg")
                val oneRMPR = checkEstimated1RMPR(exerciseVariationId, currentWeight, currentReps, currentRpe, estimated1RM, currentDate, workoutId)
                oneRMPR?.let {
                    // Don't add duplicate 1RM PR if weight PR already includes it
                    if (weightPR == null || weightPR.estimated1RM != estimated1RM) {
                        newPRs.add(it)
                    }
                }
            }

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

    private suspend fun checkEstimated1RMPR(
        exerciseVariationId: Long,
        weight: Float,
        reps: Int,
        rpe: Float?,
        estimated1RM: Float,
        date: LocalDateTime,
        workoutId: Long?,
    ): PersonalRecord? {
        // Get the current max estimated 1RM for this exercise
        val currentMax1RM = personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId)

        // Create PR if this is the first record OR if it beats the existing record
        if (currentMax1RM == null || estimated1RM > currentMax1RM) {
            // Get previous 1RM PR for context
            val previousPR = personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM)

            val improvementPercentage =
                if (currentMax1RM != null && currentMax1RM > 0) {
                    ((estimated1RM - currentMax1RM) / currentMax1RM) * 100
                } else {
                    100f
                }

            val notes =
                if (currentMax1RM == null) {
                    "First 1RM estimate: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg from ${weight}kg × $reps"
                } else {
                    "New estimated 1RM: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg from ${weight}kg × $reps"
                }

            return PersonalRecord(
                exerciseVariationId = exerciseVariationId,
                weight = weight,
                reps = reps,
                rpe = rpe,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.ESTIMATED_1RM,
                volume = weight * reps,
                estimated1RM = estimated1RM,
                notes = notes,
                workoutId = workoutId,
            )
        }

        return null
    }

    private suspend fun checkWeightPR(
        exerciseVariationId: Long,
        weight: Float,
        reps: Int,
        rpe: Float?,
        date: LocalDateTime,
        workoutId: Long?,
        estimated1RM: Float?,
        currentMax1RM: Float?,
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
                when {
                    currentMaxWeight == null -> "First weight record: ${weight}kg × $reps"
                    currentMax1RM != null && estimated1RM != null && estimated1RM < currentMax1RM -> {
                        val potentialMax = WeightFormatter.formatDecimal(currentMax1RM, 1)
                        "New weight PR: ${weight}kg × $reps (Based on your ${potentialMax}kg 1RM, you could potentially lift more!)"
                    }
                    else -> "New weight PR: ${weight}kg × $reps"
                }

            val roundedWeight = WeightFormatter.roundToNearestQuarter(weight)
            Log.d("PRDetection", "Creating weight PR: ${roundedWeight}kg × $reps, notes: $notes")
            return PersonalRecord(
                exerciseVariationId = exerciseVariationId,
                weight = roundedWeight,
                reps = reps,
                rpe = rpe,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.WEIGHT,
                volume = roundedWeight * reps,
                estimated1RM = estimated1RM,
                notes = notes,
                workoutId = workoutId,
            )
        }

        return null
    }

    /**
     * Calculate estimated 1RM using appropriate formula based on exercise type
     * Returns null if RPE <= 6 (too unreliable)
     */
    private fun calculateEstimated1RM(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        scalingType: RMScalingType = RMScalingType.STANDARD,
    ): Float? {
        // Skip if RPE is too low for reliable estimate
        if (rpe != null && rpe <= 6.0f) {
            Log.d("PRDetection", "Skipping 1RM calculation: RPE $rpe <= 6.0 (unreliable)")
            return null
        }

        val totalRepCapacity =
            when {
                rpe != null && rpe > 6.0f -> {
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f)
                    reps + repsInReserve
                }
                else -> reps.toFloat()
            }

        if (totalRepCapacity == 1f) return weight
        if (totalRepCapacity > 15) {
            Log.d("PRDetection", "Skipping 1RM calculation: total rep capacity $totalRepCapacity > 15")
            return null
        }

        // Apply formula based on scaling type
        return when (scalingType) {
            RMScalingType.WEIGHTED_BODYWEIGHT -> {
                weight * (1 + totalRepCapacity * 0.035f)
            }
            RMScalingType.ISOLATION -> {
                weight * totalRepCapacity.pow(0.10f)
            }
            RMScalingType.STANDARD -> {
                weight / (1.0278f - 0.0278f * totalRepCapacity)
            }
        }
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
