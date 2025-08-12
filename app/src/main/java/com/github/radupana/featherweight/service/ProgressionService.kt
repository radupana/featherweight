package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExercisePerformanceTracking
import com.github.radupana.featherweight.data.ExercisePerformanceTrackingDao
import com.github.radupana.featherweight.data.ProgressionAction
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.programme.DeloadRules
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgressionRules
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Service responsible for calculating progression decisions including deloads.
 * This is programme-agnostic and uses the configured rules from each programme.
 */
class ProgressionService(
    private val performanceTrackingDao: ExercisePerformanceTrackingDao,
    private val programmeDao: ProgrammeDao,
    private val repository: FeatherweightRepository,
) {
    /**
     * Calculate the next weight for an exercise based on performance history and programme rules
     */
    suspend fun calculateProgressionWeight(
        exerciseName: String,
        programme: Programme,
    ): ProgressionDecision =
        withContext(Dispatchers.IO) {
            val progressionRules =
                programme.getProgressionRulesObject()
                    ?: return@withContext ProgressionDecision(
                        weight = 20f,
                        action = ProgressionAction.MAINTAIN,
                        reason = "No progression rules defined",
                    )

            // Get performance history
            val recentPerformance =
                performanceTrackingDao.getRecentPerformance(
                    programmeId = programme.id,
                    exerciseName = exerciseName,
                    limit = 5,
                )

            if (recentPerformance.isEmpty()) {
                return@withContext handleFirstWorkout(exerciseName, programme)
            }

            val lastPerformance = recentPerformance.first()
            val consecutiveFailures =
                performanceTrackingDao.getConsecutiveFailures(
                    programmeId = programme.id,
                    exerciseName = exerciseName,
                )

            // Check deload conditions
            val deloadRules = progressionRules.deloadRules
            if (deloadRules.autoDeload && consecutiveFailures >= deloadRules.triggerAfterFailures) {
                return@withContext handleDeload(
                    exerciseName = exerciseName,
                    lastWeight = lastPerformance.targetWeight,
                    deloadRules = deloadRules,
                    reason = "Reached $consecutiveFailures consecutive failures",
                )
            }

            // Check if recovering from deload
            if (lastPerformance.isDeloadWorkout) {
                return@withContext handleDeloadRecovery(
                    exerciseName = exerciseName,
                    deloadWeight = lastPerformance.achievedWeight,
                    progressionRules = progressionRules,
                    recentPerformance = recentPerformance,
                )
            }

            // Normal progression logic
            if (lastPerformance.wasSuccessful) {
                val increment =
                    progressionRules.incrementRules[exerciseName.lowercase()]
                        ?: progressionRules.incrementRules["default"]
                        ?: 2.5f

                val newWeight = lastPerformance.achievedWeight + increment

                return@withContext ProgressionDecision(
                    weight = newWeight,
                    action = ProgressionAction.PROGRESS,
                    reason = "Last workout successful - adding $increment kg",
                    isDeload = false,
                )
            } else {
                return@withContext ProgressionDecision(
                    weight = lastPerformance.targetWeight,
                    action = ProgressionAction.MAINTAIN,
                    reason = "Last workout not successful - repeating weight",
                    isDeload = false,
                )
            }
        }

    /**
     * Record workout performance for tracking
     */
    suspend fun recordWorkoutPerformance(
        workoutId: Long,
        programmeId: Long,
        exerciseName: String,
        sets: List<SetLog>,
    ) = withContext(Dispatchers.IO) {
        val targetSets = sets.size
        val completedSets = sets.count { it.isCompleted }
        val targetReps = sets.firstOrNull()?.targetReps
        val achievedReps = sets.sumOf { if (it.isCompleted) it.actualReps else 0 }
        val missedReps =
            if (targetReps != null) {
                val totalTargetReps = targetSets * targetReps
                totalTargetReps - achievedReps
            } else {
                0 // No concept of "missed reps" in freestyle workouts
            }

        // Get the target weight (should be consistent across sets for linear progression)
        val targetWeight = sets.firstOrNull()?.actualWeight ?: 0f
        val achievedWeight = sets.filter { it.isCompleted }.maxOfOrNull { it.actualWeight } ?: 0f

        // Calculate average RPE if available
        val rpeValues = sets.mapNotNull { it.actualRpe }.filter { it > 0 }
        val averageRpe =
            if (rpeValues.isNotEmpty()) {
                rpeValues.average().toFloat()
            } else {
                null
            }

        // Determine success based on programme rules
        val progressionRules =
            programmeDao
                .getProgrammeById(programmeId)
                ?.getProgressionRulesObject()

        val wasSuccessful =
            if (progressionRules != null && targetReps != null) {
                // Program workout with target reps - use progression rules
                val criteria = progressionRules.successCriteria
                val meetsSetRequirement = criteria.requiredSets == null || completedSets >= criteria.requiredSets
                val meetsRepRequirement =
                    criteria.requiredReps == null ||
                        (missedReps <= criteria.allowedMissedReps)
                val meetsRpeRequirement =
                    averageRpe == null ||
                        (criteria.minRPE == null || averageRpe >= criteria.minRPE) &&
                        (criteria.maxRPE == null || averageRpe <= criteria.maxRPE)

                meetsSetRequirement && meetsRepRequirement && meetsRpeRequirement
            } else if (targetReps != null) {
                // Program workout without progression rules - strict target adherence
                completedSets == targetSets && missedReps == 0
            } else {
                // Freestyle workout - success based on reasonable completion rate
                completedSets.toFloat() / targetSets >= 0.8f // 80% completion rate
            }

        val performanceRecord =
            ExercisePerformanceTracking(
                programmeId = programmeId,
                exerciseName = exerciseName,
                targetWeight = targetWeight,
                achievedWeight = achievedWeight,
                targetSets = targetSets,
                completedSets = completedSets,
                targetReps = targetReps,
                achievedReps = achievedReps,
                missedReps = missedReps,
                wasSuccessful = wasSuccessful,
                workoutDate = LocalDateTime.now(),
                workoutId = workoutId,
                averageRpe = averageRpe,
                isDeloadWorkout = false, // Will be set by deload logic
            )

        performanceTrackingDao.insertPerformanceRecord(performanceRecord)
    }

    private suspend fun handleFirstWorkout(
        exerciseName: String,
        programme: Programme,
    ): ProgressionDecision {
        // Get exercise ID from name
        val exercise = repository.getExerciseByName(exerciseName)
        val exerciseVariationId =
            exercise?.id ?: return ProgressionDecision(
                weight = 20f,
                action = ProgressionAction.MAINTAIN,
                reason = "Exercise not found",
            )

        // Try to get 1RM
        val oneRM = repository.getOneRMForExercise(exerciseVariationId)

        val startingWeight =
            if (oneRM != null && oneRM > 0) {
                // Start at 50% of 1RM for linear progression programmes
                val calculated = oneRM * 0.5f
                val rounded = (calculated / 2.5f).toInt() * 2.5f
                rounded.coerceAtLeast(20f)
            } else {
                // Start with empty bar
                20f
            }

        return ProgressionDecision(
            weight = startingWeight,
            action = ProgressionAction.PROGRESS,
            reason = if (oneRM != null) "Starting at 50% of 1RM (${oneRM}kg)" else "Starting with empty bar",
            isDeload = false,
        )
    }

    private fun handleDeload(
        exerciseName: String,
        lastWeight: Float,
        deloadRules: DeloadRules,
        reason: String,
    ): ProgressionDecision {
        val deloadWeight =
            (lastWeight * deloadRules.deloadPercentage)
                .coerceAtLeast(deloadRules.minimumWeight)

        // Round to nearest 2.5kg
        val roundedWeight = (deloadWeight / 2.5f).toInt() * 2.5f

        return ProgressionDecision(
            weight = roundedWeight,
            action = ProgressionAction.DELOAD,
            reason = reason,
            isDeload = true,
            deloadDetails =
                DeloadDetails(
                    previousWeight = lastWeight,
                    deloadPercentage = deloadRules.deloadPercentage,
                    minimumWeight = deloadRules.minimumWeight,
                ),
        )
    }

    private fun handleDeloadRecovery(
        exerciseName: String,
        deloadWeight: Float,
        progressionRules: ProgressionRules,
        recentPerformance: List<ExercisePerformanceTracking>,
    ): ProgressionDecision {
        // Find the weight we deloaded from
        val preDeloadWeight =
            recentPerformance
                .firstOrNull { !it.isDeloadWorkout }
                ?.targetWeight ?: deloadWeight

        val increment =
            progressionRules.incrementRules[exerciseName.lowercase()]
                ?: progressionRules.incrementRules["default"]
                ?: 2.5f

        val newWeight = deloadWeight + increment
        val cappedWeight = newWeight.coerceAtMost(preDeloadWeight)

        return ProgressionDecision(
            weight = cappedWeight,
            action = ProgressionAction.PROGRESS,
            reason = "Recovering from deload - progressing by $increment kg",
            isDeload = false,
        )
    }
}

/**
 * Result of a progression calculation
 */
data class ProgressionDecision(
    val weight: Float,
    val action: ProgressionAction,
    val reason: String,
    val isDeload: Boolean = false,
    val deloadDetails: DeloadDetails? = null,
)

data class DeloadDetails(
    val previousWeight: Float,
    val deloadPercentage: Float,
    val minimumWeight: Float,
)
