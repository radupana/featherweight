package com.github.radupana.featherweight.service

import android.util.Log
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
        exerciseName: String,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            Log.d("PRDetection", "=== PR Detection Started ===")
            Log.d("PRDetection", "Exercise: $exerciseName")
            Log.d(
                "PRDetection",
                "SetLog: isCompleted=${setLog.isCompleted}, actualReps=${setLog.actualReps}, actualWeight=${setLog.actualWeight}",
            )

            val newPRs = mutableListOf<PersonalRecord>()

            if (!setLog.isCompleted || setLog.actualReps <= 0 || setLog.actualWeight <= 0) {
                Log.d("PRDetection", "Skipping PR check - invalid data or not completed")
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
                        Log.w("PRDetection", "Failed to parse workout date: $workoutDateString, using current date")
                        LocalDateTime.now()
                    }
                } else {
                    Log.w("PRDetection", "No workout date found for setLog ${setLog.id}, using current date")
                    LocalDateTime.now()
                }

            // Only check for weight PR (higher weight than ever before)
            val weightPR = checkWeightPR(exerciseName, currentWeight, currentReps, currentRpe, currentDate, workoutId)
            weightPR?.let { newPRs.add(it) }

            // Save all detected PRs, but check for duplicates within the same workout
            Log.d("PRDetection", "🏆 Detected ${newPRs.size} total PRs")
            newPRs.forEach { pr ->
                Log.d("PRDetection", "🏆 Processing ${pr.recordType} PR: ${pr.exerciseName} - ${pr.weight}kg x ${pr.reps}")

                // Check if there's already a PR for this exercise in this workout
                if (pr.workoutId != null) {
                    val existingPR =
                        personalRecordDao.getPRForExerciseInWorkout(
                            pr.workoutId,
                            pr.exerciseName,
                            pr.recordType,
                        )

                    if (existingPR != null) {
                        // If new PR is better, delete the old one
                        if (pr.weight > existingPR.weight) {
                            Log.d("PRDetection", "🏆 New PR is better than existing (${pr.weight}kg > ${existingPR.weight}kg), replacing")
                            personalRecordDao.deletePR(existingPR.id)
                            personalRecordDao.insertPersonalRecord(pr)
                        } else {
                            Log.d("PRDetection", "🏆 Existing PR is better (${existingPR.weight}kg >= ${pr.weight}kg), keeping existing")
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
        exerciseName: String,
        weight: Float,
        reps: Int,
        rpe: Float?,
        date: LocalDateTime,
        workoutId: Long?,
    ): PersonalRecord? {
        val currentMaxWeight = personalRecordDao.getMaxWeightForExercise(exerciseName)

        // Create PR if this is the first record OR if it beats the existing record
        if (currentMaxWeight == null || weight > currentMaxWeight) {
            // Get previous weight PR for context
            val previousPR = personalRecordDao.getLatestPRForExerciseAndType(exerciseName, PRType.WEIGHT)

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
                exerciseName = exerciseName,
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
        val effectiveReps = when {
            reps == 1 && rpe != null -> {
                // For singles with RPE, calculate total possible reps
                val repsInReserve = (10f - rpe).coerceAtLeast(0f).toInt()
                reps + repsInReserve  // Total reps possible at this weight
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
        exerciseName: String,
        limit: Int = 5,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRsForExercise(exerciseName, limit)
        }

    /**
     * Get all recent PRs across exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRs(limit)
        }
}
