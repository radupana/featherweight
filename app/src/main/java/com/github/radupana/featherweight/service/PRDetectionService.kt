package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow

/**
 * Context object bundling all data needed for PR detection.
 * Reduces parameter passing between methods.
 */
private data class PRCheckContext(
    val exerciseId: String,
    val weight: Float,
    val reps: Int,
    val rpe: Float?,
    val date: LocalDateTime,
    val workoutId: String?,
    val userId: String?,
    val sourceSetId: String,
    val estimated1RM: Float?,
    val currentMax1RM: Float?,
    val requiresWeight: Boolean,
)

/**
 * Service responsible for detecting weight personal records and creating PR entries.
 * Only tracks weight PRs - when the user lifts more weight than their previous best.
 */
class PRDetectionService(
    private val personalRecordDao: PersonalRecordDao,
    private val setLogDao: SetLogDao,
    private val exerciseDao: ExerciseDao,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
) {
    companion object {
        private const val TAG = "PRDetectionService"
    }

    /**
     * Checks if a completed set represents a new PR and creates record if so.
     * Detects weight PRs for weighted exercises, rep PRs for bodyweight exercises.
     * Returns the PersonalRecord(s) if PR was detected, empty list otherwise.
     */
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseId: String,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            // Basic validation - must be completed with reps
            if (!setLog.isCompleted || setLog.actualReps <= 0) {
                return@withContext emptyList()
            }

            // Check if exercise requires weight
            val exercise = exerciseDao.getExerciseById(exerciseId)
            val requiresWeight = exercise?.requiresWeight ?: true

            // For weighted exercises, require weight > 0
            if (requiresWeight && setLog.actualWeight <= 0) {
                return@withContext emptyList()
            }

            val context = buildPRCheckContext(setLog, exerciseId, requiresWeight)
            val newPRs = detectPRs(context)
            savePRsWithDuplicateHandling(newPRs)
            newPRs
        }

    private suspend fun buildPRCheckContext(
        setLog: SetLog,
        exerciseId: String,
        requiresWeight: Boolean,
    ): PRCheckContext {
        val workoutDate = getWorkoutDate(setLog.id)
        val workoutId = setLogDao.getWorkoutIdForSetLog(setLog.id)

        // Only calculate 1RM for weighted exercises
        val estimated1RM: Float?
        val currentMax1RM: Float?
        if (requiresWeight) {
            val scalingType = getScalingType(exerciseId)
            estimated1RM =
                calculateEstimated1RM(
                    setLog.actualWeight,
                    setLog.actualReps,
                    setLog.actualRpe,
                    scalingType,
                )
            currentMax1RM = personalRecordDao.getMaxEstimated1RMForExercise(exerciseId)
        } else {
            estimated1RM = null
            currentMax1RM = null
        }

        logPRCheckStart(exerciseId, setLog, estimated1RM, currentMax1RM, requiresWeight)

        return PRCheckContext(
            exerciseId = exerciseId,
            weight = setLog.actualWeight,
            reps = setLog.actualReps,
            rpe = setLog.actualRpe,
            date = workoutDate,
            workoutId = workoutId,
            userId = setLog.userId,
            sourceSetId = setLog.id,
            estimated1RM = estimated1RM,
            currentMax1RM = currentMax1RM,
            requiresWeight = requiresWeight,
        )
    }

    private suspend fun getWorkoutDate(setLogId: String): LocalDateTime {
        val workoutDateString = setLogDao.getWorkoutDateForSetLog(setLogId)
        return workoutDateString?.let { parseWorkoutDate(it, setLogId) } ?: LocalDateTime.now()
    }

    private fun parseWorkoutDate(
        dateString: String,
        setLogId: String,
    ): LocalDateTime =
        try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: java.time.format.DateTimeParseException) {
            CloudLogger.error(TAG, "Failed to parse workout date for setLog: $setLogId, using current time", e)
            LocalDateTime.now()
        }

    private suspend fun getScalingType(exerciseId: String): RMScalingType {
        val exercise = exerciseDao.getExerciseById(exerciseId)
        return exercise?.rmScalingType?.let { parseScalingType(it) } ?: RMScalingType.STANDARD
    }

    private fun parseScalingType(value: String): RMScalingType =
        try {
            RMScalingType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            CloudLogger.warn(TAG, "Invalid RMScalingType value: '$value', using default STANDARD", e)
            RMScalingType.STANDARD
        }

    private fun logPRCheckStart(
        exerciseId: String,
        setLog: SetLog,
        estimated1RM: Float?,
        currentMax1RM: Float?,
        requiresWeight: Boolean,
    ) {
        if (requiresWeight) {
            CloudLogger.debug(TAG, "Checking PRs for exercise $exerciseId: ${setLog.actualWeight}kg × ${setLog.actualReps} @ RPE ${setLog.actualRpe}")
            if (estimated1RM != null) {
                CloudLogger.debug(TAG, "Calculated 1RM: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg")
            } else {
                CloudLogger.debug(TAG, "No 1RM calculated (RPE too low or other criteria not met)")
            }
            CloudLogger.debug(TAG, "Current max 1RM in database: ${currentMax1RM?.let { WeightFormatter.formatDecimal(it, 2) } ?: "None"}kg")
        } else {
            CloudLogger.debug(TAG, "Checking rep PR for bodyweight exercise $exerciseId: ${setLog.actualReps} reps @ RPE ${setLog.actualRpe}")
        }
    }

    private suspend fun detectPRs(context: PRCheckContext): List<PersonalRecord> {
        val newPRs = mutableListOf<PersonalRecord>()

        // For bodyweight exercises, check for rep PRs
        if (!context.requiresWeight) {
            checkRepPR(context)?.let { repPR ->
                CloudLogger.debug(TAG, "Rep PR detected: ${repPR.reps} reps")
                newPRs.add(repPR)
            }
            return newPRs
        }

        // For weighted exercises, check for weight and 1RM PRs
        checkWeightPR(context)?.let { weightPR ->
            CloudLogger.debug(TAG, "Weight PR detected: ${weightPR.weight}kg × ${weightPR.reps}")
            newPRs.add(weightPR)
        }

        checkEstimated1RMPR(context, newPRs.firstOrNull())?.let { oneRMPR ->
            newPRs.add(oneRMPR)
        }

        return newPRs
    }

    private suspend fun checkWeightPR(context: PRCheckContext): PersonalRecord? {
        val currentMaxWeight = personalRecordDao.getMaxWeightForExercise(context.exerciseId)
        CloudLogger.debug(TAG, "Weight PR check: current weight=${context.weight}kg, max weight in DB=${currentMaxWeight ?: "None"}kg")

        if (currentMaxWeight != null && context.weight <= currentMaxWeight) {
            return null
        }

        val previousPR = personalRecordDao.getLatestPRForExerciseAndType(context.exerciseId, PRType.WEIGHT)
        val improvementPercentage = calculateImprovement(context.weight, currentMaxWeight)
        val notes = buildWeightPRNotes(context, currentMaxWeight)
        val roundedWeight = WeightFormatter.roundToNearestQuarter(context.weight)

        CloudLogger.debug(TAG, "Creating weight PR: ${roundedWeight}kg × ${context.reps}, notes: $notes")

        return PersonalRecord(
            userId = context.userId,
            exerciseId = context.exerciseId,
            weight = roundedWeight,
            reps = context.reps,
            rpe = context.rpe,
            recordDate = context.date,
            previousWeight = previousPR?.weight,
            previousReps = previousPR?.reps,
            previousDate = previousPR?.recordDate,
            improvementPercentage = improvementPercentage,
            recordType = PRType.WEIGHT,
            volume = roundedWeight * context.reps,
            estimated1RM = context.estimated1RM,
            notes = notes,
            workoutId = context.workoutId,
            sourceSetId = context.sourceSetId,
        )
    }

    private suspend fun checkRepPR(context: PRCheckContext): PersonalRecord? {
        val currentMaxReps = personalRecordDao.getMaxRepsForExercise(context.exerciseId)
        CloudLogger.debug(TAG, "Rep PR check: current reps=${context.reps}, max reps in DB=${currentMaxReps ?: "None"}")

        if (currentMaxReps != null && context.reps <= currentMaxReps) {
            return null
        }

        val previousPR = personalRecordDao.getLatestPRForExerciseAndType(context.exerciseId, PRType.REPS)
        val improvementPercentage =
            if (currentMaxReps != null && currentMaxReps > 0) {
                ((context.reps - currentMaxReps).toFloat() / currentMaxReps) * 100
            } else {
                100f
            }
        val notes =
            if (currentMaxReps == null) {
                "First rep record: ${context.reps} reps"
            } else {
                "New rep PR: ${context.reps} reps (previous: $currentMaxReps)"
            }

        CloudLogger.debug(TAG, "Creating rep PR: ${context.reps} reps, notes: $notes")

        return PersonalRecord(
            userId = context.userId,
            exerciseId = context.exerciseId,
            weight = 0f, // Bodyweight exercises have no external weight
            reps = context.reps,
            rpe = context.rpe,
            recordDate = context.date,
            previousWeight = 0f,
            previousReps = previousPR?.reps ?: currentMaxReps,
            previousDate = previousPR?.recordDate,
            improvementPercentage = improvementPercentage,
            recordType = PRType.REPS,
            volume = 0f,
            estimated1RM = null,
            notes = notes,
            workoutId = context.workoutId,
            sourceSetId = context.sourceSetId,
        )
    }

    private fun buildWeightPRNotes(
        context: PRCheckContext,
        currentMaxWeight: Float?,
    ): String =
        when {
            currentMaxWeight == null ->
                "First weight record: ${WeightFormatter.formatWeightWithUnit(context.weight)} × ${context.reps}"
            context.currentMax1RM != null && context.estimated1RM != null && context.estimated1RM < context.currentMax1RM -> {
                val potentialMax = WeightFormatter.formatWeightWithUnit(context.currentMax1RM)
                "New weight PR: ${WeightFormatter.formatWeightWithUnit(context.weight)} × ${context.reps} (Based on your $potentialMax 1RM, you could potentially lift more!)"
            }
            else ->
                "New weight PR: ${WeightFormatter.formatWeightWithUnit(context.weight)} × ${context.reps}"
        }

    private suspend fun checkEstimated1RMPR(
        context: PRCheckContext,
        weightPR: PersonalRecord?,
    ): PersonalRecord? {
        val estimated1RM = context.estimated1RM ?: return null
        val currentMax1RM = context.currentMax1RM ?: 0f

        if (estimated1RM <= currentMax1RM) {
            return null
        }

        // Don't add duplicate 1RM PR if weight PR already includes it
        if (weightPR != null && weightPR.estimated1RM == estimated1RM) {
            return null
        }

        CloudLogger.debug(TAG, "New 1RM PR detected: ${WeightFormatter.formatDecimal(estimated1RM, 2)}kg > ${currentMax1RM}kg")

        val previousPR = personalRecordDao.getLatestPRForExerciseAndType(context.exerciseId, PRType.ESTIMATED_1RM)
        val improvementPercentage = calculateImprovement(estimated1RM, context.currentMax1RM)
        val notes = build1RMPRNotes(context, estimated1RM)

        return PersonalRecord(
            userId = context.userId,
            exerciseId = context.exerciseId,
            weight = context.weight,
            reps = context.reps,
            rpe = context.rpe,
            recordDate = context.date,
            previousWeight = previousPR?.weight,
            previousReps = previousPR?.reps,
            previousDate = previousPR?.recordDate,
            improvementPercentage = improvementPercentage,
            recordType = PRType.ESTIMATED_1RM,
            volume = context.weight * context.reps,
            estimated1RM = estimated1RM,
            notes = notes,
            workoutId = context.workoutId,
            sourceSetId = context.sourceSetId,
        )
    }

    private fun build1RMPRNotes(
        context: PRCheckContext,
        estimated1RM: Float,
    ): String =
        if (context.currentMax1RM == null) {
            "First 1RM estimate: ${WeightFormatter.formatWeightWithUnit(estimated1RM)} from ${WeightFormatter.formatWeightWithUnit(context.weight)} × ${context.reps}"
        } else {
            "New estimated 1RM: ${WeightFormatter.formatWeightWithUnit(estimated1RM)} from ${WeightFormatter.formatWeightWithUnit(context.weight)} × ${context.reps}"
        }

    private fun calculateImprovement(
        newValue: Float,
        oldValue: Float?,
    ): Float =
        if (oldValue != null && oldValue > 0) {
            ((newValue - oldValue) / oldValue) * 100
        } else {
            100f
        }

    private suspend fun savePRsWithDuplicateHandling(prs: List<PersonalRecord>) {
        prs.forEach { pr -> savePRWithDuplicateCheck(pr) }
    }

    private suspend fun savePRWithDuplicateCheck(pr: PersonalRecord) {
        val workoutId = pr.workoutId
        if (workoutId == null) {
            personalRecordDao.insertPersonalRecord(pr)
            CloudLogger.info(TAG, "PR saved (no workoutId): ${pr.recordType} for exercise ${pr.exerciseId}, ${pr.weight}kg × ${pr.reps}")
            return
        }

        val existingPR = personalRecordDao.getPRForExerciseInWorkout(workoutId, pr.exerciseId, pr.recordType)
        if (existingPR == null) {
            personalRecordDao.insertPersonalRecord(pr)
            CloudLogger.info(TAG, "PR saved: ${pr.recordType} for exercise ${pr.exerciseId}, ${pr.weight}kg × ${pr.reps}")
            return
        }

        // For rep PRs, compare reps; for weight/1RM PRs, compare weight
        val shouldReplace =
            when (pr.recordType) {
                PRType.REPS -> pr.reps > existingPR.reps
                else -> pr.weight > existingPR.weight
            }

        if (shouldReplace) {
            deleteExistingPR(existingPR)
            personalRecordDao.insertPersonalRecord(pr)
            val prDetail = if (pr.recordType == PRType.REPS) "${pr.reps} reps" else "${pr.weight}kg × ${pr.reps}"
            CloudLogger.info(TAG, "PR saved (replaced existing): ${pr.recordType} for exercise ${pr.exerciseId}, $prDetail")
        }
    }

    private suspend fun deleteExistingPR(existingPR: PersonalRecord) {
        personalRecordDao.deletePR(existingPR.id)
        CloudLogger.info(TAG, "Deleted old PR ${existingPR.id} from Room")

        val userId = existingPR.userId
        if (userId != null && userId != "local") {
            val result = firestoreRepository.deletePersonalRecord(userId, existingPR.id)
            if (result.isSuccess) {
                CloudLogger.info(TAG, "Deleted old PR ${existingPR.id} from Firestore")
            } else {
                CloudLogger.error(TAG, "Failed to delete old PR from Firestore", result.exceptionOrNull())
            }
        }
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
            CloudLogger.debug(TAG, "Skipping 1RM calculation: RPE $rpe <= 6.0 (unreliable)")
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
            CloudLogger.debug(TAG, "Skipping 1RM calculation: total rep capacity $totalRepCapacity > 15")
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
            RMScalingType.CONSERVATIVE, RMScalingType.UNKNOWN -> {
                // Use standard formula with 5% reduction for safety
                (weight / (1.0278f - 0.0278f * totalRepCapacity)) * 0.95f
            }
        }
    }

    /**
     * Get recent PRs for an exercise
     */
    suspend fun getRecentPRsForExercise(
        exerciseId: String,
        limit: Int = 5,
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRsForExercise(exerciseId, limit)
        }

    /**
     * Get all recent PRs across exercises
     */
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRs(limit)
        }
}
