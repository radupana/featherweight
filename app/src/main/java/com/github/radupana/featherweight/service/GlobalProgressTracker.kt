package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.repository.CustomExerciseRepository
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class GlobalProgressTracker(
    private val repository: FeatherweightRepository,
    private val database: FeatherweightDatabase,
    private val customExerciseRepository: CustomExerciseRepository? = null,
) {
    private val globalProgressDao = database.globalExerciseProgressDao()

    /**
     * Updates global exercise progress after any workout completion (programme or freestyle)
     */
    suspend fun updateProgressAfterWorkout(
        workoutId: String,
    ): List<PendingOneRMUpdate> {
        val pendingUpdates = mutableListOf<PendingOneRMUpdate>()

        val workout = repository.getWorkoutById(workoutId) ?: return emptyList()
        val exercises = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)

        for (exercise in exercises) {
            val pendingUpdate =
                updateExerciseProgress(
                    exerciseVariationId = exercise.exerciseVariationId,
                    workoutId = workoutId,
                    isProgrammeWorkout = workout.programmeId != null,
                )
            pendingUpdate?.let { pendingUpdates.add(it) }
        }

        return pendingUpdates
    }

    private suspend fun updateExerciseProgress(
        exerciseVariationId: String,
        workoutId: String,
        isProgrammeWorkout: Boolean,
    ): PendingOneRMUpdate? {
        val exerciseLogs =
            database
                .exerciseLogDao()
                .getExerciseLogsForWorkout(workoutId)
                .filter { it.exerciseVariationId == exerciseVariationId }

        if (exerciseLogs.isEmpty()) return null

        val sets = exerciseLogs.flatMap { database.setLogDao().getSetLogsForExercise(it.id) }
        val completedSets = sets.filter { it.isCompleted }

        if (completedSets.isEmpty()) {
            return null
        }

        val userId = completedSets.firstOrNull()?.userId

        // Calculate session metrics
        val maxWeight = completedSets.maxOf { it.actualWeight }
        val sessionVolume =
            completedSets
                .sumOf {
                    (it.actualWeight * it.actualReps).toDouble()
                }.toFloat()
        val avgRpe =
            completedSets
                .mapNotNull { it.actualRpe }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toFloat()

        // Get or create progress record
        var progress =
            globalProgressDao.getProgressForExercise(exerciseVariationId)
                ?: createInitialProgress(exerciseVariationId, userId)

        // Update working weight
        val previousWeight = progress.currentWorkingWeight
        progress =
            progress.copy(
                currentWorkingWeight = maxWeight,
                lastUpdated = LocalDateTime.now(),
                sessionsTracked = progress.sessionsTracked + 1,
                lastSessionVolume = sessionVolume,
            )

        // Update RPE tracking
        if (avgRpe != null) {
            val newAvgRpe =
                if (progress.recentAvgRpe != null) {
                    // Weighted average of last 5 sessions
                    (progress.recentAvgRpe * 4 + avgRpe) / 5
                } else {
                    avgRpe
                }
            progress = progress.copy(recentAvgRpe = newAvgRpe)
        }

        // Check for PRs
        progress = checkAndUpdatePRs(progress, completedSets)

        // Update stall/progression tracking
        progress = updateStallTracking(progress, previousWeight, maxWeight)

        // Update volume trends
        progress = updateVolumeTrends(progress, sessionVolume)

        // Update 1RM estimate and get pending update if applicable
        val (updatedProgress, pendingUpdate) = updateEstimatedMax(progress, completedSets)
        progress = updatedProgress

        // Track workout source
        progress =
            if (isProgrammeWorkout) {
                progress.copy(lastProgrammeWorkoutId = workoutId)
            } else {
                progress.copy(lastFreestyleWorkoutId = workoutId)
            }

        // Save updated progress
        globalProgressDao.insertOrUpdate(progress)

        return pendingUpdate
    }

    private suspend fun createInitialProgress(
        exerciseVariationId: String,
        userId: String?,
    ): GlobalExerciseProgress {
        // Try to get 1RM from UserExerciseMax
        val userMax = database.oneRMDao().getCurrentMax(exerciseVariationId, userId ?: "local")
        val estimatedMax = userMax?.oneRMEstimate ?: 0f

        val isCustom = customExerciseRepository?.isCustomExercise(exerciseVariationId) ?: false
        return GlobalExerciseProgress(
            userId = userId,
            exerciseVariationId = exerciseVariationId,
            isCustomExercise = isCustom,
            currentWorkingWeight = 0f,
            estimatedMax = estimatedMax,
            lastUpdated = LocalDateTime.now(),
            trend = ProgressTrend.STALLING,
        )
    }

    private fun checkAndUpdatePRs(
        progress: GlobalExerciseProgress,
        sets: List<SetLog>,
    ): GlobalExerciseProgress {
        var updated = progress

        // Check various rep PRs
        val maxByReps =
            sets
                .groupBy { it.actualReps }
                .mapValues { (_, setList) -> setList.maxOf { it.actualWeight } }

        // Update 1RM
        maxByReps[1]?.let { weight ->
            if (weight > (updated.bestSingleRep ?: 0f)) {
                updated =
                    updated.copy(
                        bestSingleRep = weight,
                        lastPrDate = LocalDateTime.now(),
                        lastPrWeight = weight,
                    )
            }
        }

        // Update 3RM
        maxByReps[3]?.let { weight ->
            if (weight > (updated.best3Rep ?: 0f)) {
                updated = updated.copy(best3Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated =
                        updated.copy(
                            lastPrDate = LocalDateTime.now(),
                            lastPrWeight = weight,
                        )
                }
            }
        }

        // Update 5RM
        maxByReps[5]?.let { weight ->
            if (weight > (updated.best5Rep ?: 0f)) {
                updated = updated.copy(best5Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated =
                        updated.copy(
                            lastPrDate = LocalDateTime.now(),
                            lastPrWeight = weight,
                        )
                }
            }
        }

        // Update 8RM
        maxByReps[8]?.let { weight ->
            if (weight > (updated.best8Rep ?: 0f)) {
                updated = updated.copy(best8Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated =
                        updated.copy(
                            lastPrDate = LocalDateTime.now(),
                            lastPrWeight = weight,
                        )
                }
            }
        }

        return updated
    }

    private fun updateStallTracking(
        progress: GlobalExerciseProgress,
        previousWeight: Float,
        currentWeight: Float,
    ): GlobalExerciseProgress =
        when {
            currentWeight > previousWeight -> {
                // Progression!
                progress.copy(
                    consecutiveStalls = 0,
                    weeksAtCurrentWeight = 0,
                    lastProgressionDate = LocalDateTime.now(),
                    failureStreak = 0,
                    trend = ProgressTrend.IMPROVING,
                )
            }

            currentWeight == previousWeight -> {
                // Stalled at same weight
                val weeksSinceLastUpdate =
                    if (progress.lastProgressionDate != null) {
                        ChronoUnit.WEEKS.between(progress.lastProgressionDate, LocalDateTime.now()).toInt()
                    } else {
                        1
                    }

                val newTrend =
                    if (progress.consecutiveStalls >= 2) {
                        ProgressTrend.STALLING
                    } else {
                        progress.trend
                    }

                progress.copy(
                    consecutiveStalls = progress.consecutiveStalls + 1,
                    weeksAtCurrentWeight = weeksSinceLastUpdate,
                    trend = newTrend,
                )
            }

            else -> {
                // Regression
                progress.copy(
                    consecutiveStalls = 0,
                    weeksAtCurrentWeight = 0,
                    failureStreak = progress.failureStreak + 1,
                    trend = ProgressTrend.DECLINING,
                )
            }
        }

    private fun updateVolumeTrends(
        progress: GlobalExerciseProgress,
        sessionVolume: Float,
    ): GlobalExerciseProgress {
        val avgVolume = progress.avgSessionVolume ?: sessionVolume
        val newAvgVolume = (avgVolume * progress.sessionsTracked + sessionVolume) / (progress.sessionsTracked + 1)

        val volumeTrend =
            when {
                sessionVolume > avgVolume * 1.1 -> VolumeTrend.INCREASING
                sessionVolume < avgVolume * 0.9 -> VolumeTrend.DECREASING
                else -> VolumeTrend.MAINTAINING
            }

        // Update 30-day volume (simplified - would need date filtering in production)
        val new30DayVolume = progress.totalVolumeLast30Days + sessionVolume

        return progress.copy(
            avgSessionVolume = newAvgVolume,
            volumeTrend = volumeTrend,
            totalVolumeLast30Days = new30DayVolume,
        )
    }

    private suspend fun updateEstimatedMax(
        progress: GlobalExerciseProgress,
        sets: List<SetLog>,
    ): Pair<GlobalExerciseProgress, PendingOneRMUpdate?> {
        // Only consider sets with sufficient data for estimation
        val estimableSets =
            sets.filter { set ->
                set.actualReps in 1..12 && set.actualWeight > 0
            }

        if (estimableSets.isEmpty()) return Pair(progress, null)

        // Get current stored max from profile FIRST for confidence calculation
        val currentUserMax = database.oneRMDao().getCurrentMax(progress.exerciseVariationId, progress.userId ?: "local")
        val storedMaxWeight = currentUserMax?.oneRMEstimate

        // Find the best set for 1RM calculation
        // Priority: 1) Actual 1RMs, 2) Lowest reps with highest weight, 3) Highest confidence
        var bestEstimate: OneRMEstimate? = null
        var hasActual1RM = false

        for (set in estimableSets) {
            val estimate = calculateOneRMWithConfidence(set, storedMaxWeight)
            if (estimate != null) {
                when {
                    // Always prefer actual 1RMs
                    set.actualReps == 1 -> {
                        if (!hasActual1RM || estimate.estimatedMax > (bestEstimate?.estimatedMax ?: 0f)) {
                            bestEstimate = estimate
                            hasActual1RM = true
                        }
                    }
                    // If we don't have a 1RM yet, use multi-rep sets
                    !hasActual1RM -> {
                        val currentBestReps = estimableSets.find { it.actualWeight == bestEstimate?.estimatedMax }?.actualReps ?: Int.MAX_VALUE
                        val hasLowerReps = set.actualReps < currentBestReps
                        val hasSameRepsHigherConfidence = set.actualReps == currentBestReps && estimate.confidence > (bestEstimate?.confidence ?: 0f)

                        if (bestEstimate == null || hasLowerReps || hasSameRepsHigherConfidence) {
                            bestEstimate = estimate
                        }
                    }
                }
            }
        }

        // Only proceed if we have confidence >= 60%
        if (bestEstimate == null || bestEstimate.confidence < 0.6f) {
            return Pair(progress, null)
        }

        val estimated1RM = bestEstimate.estimatedMax

        // Automatically update 1RM if it's an improvement or first record
        val shouldUpdate1RM =
            when {
                // First time recording - save if confidence is reasonable
                currentUserMax == null && bestEstimate.confidence >= 0.60f -> true

                // Clear improvement - always update
                currentUserMax != null && estimated1RM > currentUserMax.oneRMEstimate -> true

                else -> false
            }

        if (shouldUpdate1RM) {
            // Automatically save the new 1RM
            val bestSet = estimableSets.maxByOrNull { it.actualWeight }
            if (bestSet != null) {
                if (currentUserMax != null) {
                    val updatedMax =
                        currentUserMax.copy(
                            oneRMEstimate = estimated1RM,
                            oneRMContext = bestEstimate.source,
                            oneRMConfidence = bestEstimate.confidence,
                            oneRMDate = LocalDateTime.now(),
                            // Update most weight if this set had the most weight
                            mostWeightLifted = maxOf(currentUserMax.mostWeightLifted, bestSet.actualWeight),
                            mostWeightReps = if (bestSet.actualWeight > currentUserMax.mostWeightLifted) bestSet.actualReps else currentUserMax.mostWeightReps,
                            mostWeightRpe = if (bestSet.actualWeight > currentUserMax.mostWeightLifted) bestSet.actualRpe else currentUserMax.mostWeightRpe,
                            mostWeightDate = if (bestSet.actualWeight > currentUserMax.mostWeightLifted) LocalDateTime.now() else currentUserMax.mostWeightDate,
                        )
                    database.oneRMDao().updateExerciseMax(updatedMax)
                } else {
                    val isCustom = customExerciseRepository?.isCustomExercise(progress.exerciseVariationId) ?: false
                    val newMax =
                        UserExerciseMax(
                            userId = bestSet.userId,
                            exerciseVariationId = progress.exerciseVariationId,
                            oneRMEstimate = estimated1RM,
                            oneRMContext = bestEstimate.source,
                            oneRMConfidence = bestEstimate.confidence,
                            oneRMDate = LocalDateTime.now(),
                            mostWeightLifted = bestSet.actualWeight,
                            mostWeightReps = bestSet.actualReps,
                            mostWeightRpe = bestSet.actualRpe,
                            mostWeightDate = LocalDateTime.now(),
                        )
                    database.oneRMDao().insertExerciseMax(newMax)
                }
            }
        }

        // No longer return pending updates since we're not prompting users
        val pendingUpdate: PendingOneRMUpdate? = null

        // Always update internal tracking
        val updatedProgress =
            if (estimated1RM > progress.estimatedMax) {
                progress.copy(estimatedMax = estimated1RM)
            } else {
                progress
            }

        return Pair(updatedProgress, pendingUpdate)
    }

    private data class OneRMEstimate(
        val estimatedMax: Float,
        val confidence: Float,
        val source: String,
    )

    // Calculate confidence based primarily on rep count, with minor RPE adjustments
    private fun calculateOneRMWithConfidence(
        set: SetLog,
        currentStoredMax: Float? = null,
    ): OneRMEstimate? {
        val reps = set.actualReps
        val weight = set.actualWeight
        val rpe = set.actualRpe

        // Calculate effective reps based on RPE for singles
        val effectiveReps =
            when {
                reps == 1 && rpe != null -> {
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f)
                    reps + repsInReserve
                }
                else -> reps.toFloat()
            }

        // Calculate estimated 1RM using Brzycki formula
        val estimated1RM =
            if (effectiveReps == 1f) {
                weight
            } else if (effectiveReps <= 15f) {
                weight / (1.0278f - 0.0278f * effectiveReps)
            } else {
                weight * 1.5f
            }

        // Base confidence primarily on rep count and context
        val baseConfidence =
            when {
                // Single that exceeds current max - high confidence regardless of RPE
                reps == 1 && weight > (currentStoredMax ?: 0f) -> 0.90f
                // Single with high RPE - near maximal
                reps == 1 && rpe != null && rpe >= 9.5f -> 0.95f
                reps == 1 && rpe != null && rpe >= 8.5f -> 0.85f
                // Single with moderate/low RPE - adjusted estimate
                reps == 1 && rpe != null -> 0.70f
                // Single without RPE below current max - could be warm-up
                reps == 1 -> 0.40f
                // Multi-rep sets
                reps in 2..3 -> 0.85f // Very reliable
                reps in 4..5 -> 0.75f // Reliable
                reps in 6..8 -> 0.65f // Decent
                reps in 9..12 -> 0.50f // Less reliable
                else -> 0.30f // Very high reps unreliable
            }

        // Minor RPE adjustment (only for multi-rep sets, max ±5%)
        val rpeAdjustment =
            if (reps > 1 && rpe != null) {
                when {
                    rpe >= 9f -> 0.05f // Near maximal effort
                    rpe >= 7f -> 0.0f // Neutral
                    else -> -0.05f // Too easy to be accurate
                }
            } else if (reps > 1) {
                -0.05f // Missing RPE data
            } else {
                0.0f // No adjustment for singles (already handled in base confidence)
            }

        val finalConfidence = (baseConfidence + rpeAdjustment).coerceIn(0f, 1f)

        // Format source string
        val source =
            when {
                reps == 1 && rpe != null && rpe < 10 ->
                    "1×${weight.roundToInt()}kg @ RPE ${WeightFormatter.formatRPE(rpe)} (est. ${effectiveReps.toInt()}RM)"
                reps == 1 && rpe != null -> "1RM @ RPE ${WeightFormatter.formatRPE(rpe)}"
                reps == 1 -> "1RM"
                rpe != null -> "$reps×${weight.roundToInt()}kg @ RPE ${WeightFormatter.formatRPE(rpe)}"
                else -> "$reps×${weight.roundToInt()}kg"
            }

        val result =
            OneRMEstimate(
                estimatedMax = estimated1RM,
                confidence = finalConfidence,
                source = source,
            )

        return result
    }
}
