package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
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

            // Get the actual workout date instead of using current date
            val workoutDateString = setLogDao.getWorkoutDateForSetLog(setLog.id)
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
            val weightPR = checkWeightPR(exerciseName, currentWeight, currentReps, currentDate)
            weightPR?.let { newPRs.add(it) }

            // Save all detected PRs
            Log.d("PRDetection", "🏆 Detected ${newPRs.size} total PRs")
            newPRs.forEach { pr ->
                Log.d("PRDetection", "🏆 Saving ${pr.recordType} PR: ${pr.exerciseName} - ${pr.weight}kg x ${pr.reps}")
                personalRecordDao.insertPersonalRecord(pr)
            }

            // Return all detected PRs
            newPRs
        }

    private suspend fun checkWeightPR(
        exerciseName: String,
        weight: Float,
        reps: Int,
        date: LocalDateTime,
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

            return PersonalRecord(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                recordDate = date,
                previousWeight = previousPR?.weight,
                previousReps = previousPR?.reps,
                previousDate = previousPR?.recordDate,
                improvementPercentage = improvementPercentage,
                recordType = PRType.WEIGHT,
                volume = weight * reps,
                estimated1RM = calculateEstimated1RM(weight, reps),
                notes = notes,
            )
        }

        return null
    }

    /**
     * Calculate estimated 1RM using Brzycki formula
     */
    private fun calculateEstimated1RM(
        weight: Float,
        reps: Int,
    ): Float {
        if (reps == 1) return weight
        if (reps > 15) return weight // Formula becomes unreliable beyond 15 reps

        // Brzycki formula: 1RM = weight / (1.0278 - 0.0278 × reps)
        return weight / (1.0278f - 0.0278f * reps)
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
