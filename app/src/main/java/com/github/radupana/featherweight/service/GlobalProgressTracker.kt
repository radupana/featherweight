package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class GlobalProgressTracker(
    private val repository: FeatherweightRepository,
    private val database: FeatherweightDatabase,
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
                    exerciseId = exercise.exerciseId,
                    workoutId = workoutId,
                    isProgrammeWorkout = workout.programmeId != null,
                )
            pendingUpdate?.let { pendingUpdates.add(it) }
        }

        return pendingUpdates
    }

    private suspend fun updateExerciseProgress(
        exerciseId: String,
        workoutId: String,
        isProgrammeWorkout: Boolean,
    ): PendingOneRMUpdate? {
        val exerciseLogs =
            database
                .exerciseLogDao()
                .getExerciseLogsForWorkout(workoutId)
                .filter { it.exerciseId == exerciseId }

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
            globalProgressDao.getProgressForExercise(exerciseId)
                ?: createInitialProgress(exerciseId, userId)

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
        exerciseId: String,
        userId: String?,
    ): GlobalExerciseProgress {
        // Try to get 1RM from UserExerciseMax
        val userMax = database.exerciseMaxTrackingDao().getCurrentMax(exerciseId, userId ?: "local")
        val estimatedMax = userMax?.oneRMEstimate ?: 0f

        return GlobalExerciseProgress(
            userId = userId,
            exerciseId = exerciseId,
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
        val estimableSets = sets.filter { it.actualReps in 1..12 && it.actualWeight > 0 }
        if (estimableSets.isEmpty()) return Pair(progress, null)

        val currentUserMax = database.exerciseMaxTrackingDao().getCurrentMax(progress.exerciseId, progress.userId ?: "local")
        val storedMaxWeight = currentUserMax?.oneRMEstimate

        val bestEstimate = findBestEstimate(estimableSets, storedMaxWeight)
        if (bestEstimate == null || bestEstimate.confidence < 0.6f) {
            return Pair(progress, null)
        }

        val estimated1RM = bestEstimate.estimatedMax

        if (shouldUpdateStoredMax(currentUserMax, bestEstimate, estimated1RM)) {
            val bestSet = estimableSets.maxByOrNull { it.actualWeight }
            if (bestSet != null) {
                persistMaxUpdate(currentUserMax, bestSet, bestEstimate, estimated1RM, progress.exerciseId)
            }
        }

        val updatedProgress =
            if (estimated1RM > progress.estimatedMax) {
                progress.copy(estimatedMax = estimated1RM)
            } else {
                progress
            }

        return Pair(updatedProgress, null)
    }

    private fun findBestEstimate(
        estimableSets: List<SetLog>,
        storedMaxWeight: Float?,
    ): OneRMEstimate? {
        var bestEstimate: OneRMEstimate? = null
        var hasActual1RM = false

        for (set in estimableSets) {
            val estimate = calculateOneRMWithConfidence(set, storedMaxWeight) ?: continue

            when {
                set.actualReps == 1 -> {
                    if (!hasActual1RM || estimate.estimatedMax > (bestEstimate?.estimatedMax ?: 0f)) {
                        bestEstimate = estimate
                        hasActual1RM = true
                    }
                }
                !hasActual1RM -> {
                    if (isBetterMultiRepEstimate(set, estimate, bestEstimate, estimableSets)) {
                        bestEstimate = estimate
                    }
                }
            }
        }

        return bestEstimate
    }

    private fun isBetterMultiRepEstimate(
        set: SetLog,
        estimate: OneRMEstimate,
        currentBest: OneRMEstimate?,
        allSets: List<SetLog>,
    ): Boolean {
        if (currentBest == null) return true
        val currentBestReps = allSets.find { it.actualWeight == currentBest.estimatedMax }?.actualReps ?: Int.MAX_VALUE
        val hasLowerReps = set.actualReps < currentBestReps
        val hasSameRepsHigherConfidence = set.actualReps == currentBestReps && estimate.confidence > currentBest.confidence
        return hasLowerReps || hasSameRepsHigherConfidence
    }

    private fun shouldUpdateStoredMax(
        currentUserMax: ExerciseMaxTracking?,
        bestEstimate: OneRMEstimate,
        estimated1RM: Float,
    ): Boolean =
        when {
            currentUserMax == null && bestEstimate.confidence >= 0.60f -> true
            currentUserMax != null && estimated1RM > currentUserMax.oneRMEstimate -> true
            else -> false
        }

    private suspend fun persistMaxUpdate(
        currentUserMax: ExerciseMaxTracking?,
        bestSet: SetLog,
        bestEstimate: OneRMEstimate,
        estimated1RM: Float,
        exerciseId: String,
    ) {
        if (currentUserMax != null) {
            val updatedMax = buildUpdatedMax(currentUserMax, bestSet, bestEstimate, estimated1RM)
            database.exerciseMaxTrackingDao().update(updatedMax)
        } else {
            val newMax = buildNewMax(bestSet, bestEstimate, estimated1RM, exerciseId)
            database.exerciseMaxTrackingDao().insert(newMax)
        }
    }

    private fun buildUpdatedMax(
        currentMax: ExerciseMaxTracking,
        bestSet: SetLog,
        bestEstimate: OneRMEstimate,
        estimated1RM: Float,
    ): ExerciseMaxTracking {
        val isNewBestWeight = bestSet.actualWeight > currentMax.mostWeightLifted
        return currentMax.copy(
            oneRMEstimate = WeightFormatter.roundToNearestQuarter(estimated1RM),
            context = bestEstimate.source,
            sourceSetId = bestSet.id,
            oneRMConfidence = bestEstimate.confidence,
            recordedAt = LocalDateTime.now(),
            mostWeightLifted = if (isNewBestWeight) bestSet.actualWeight else currentMax.mostWeightLifted,
            mostWeightReps = if (isNewBestWeight) bestSet.actualReps else currentMax.mostWeightReps,
            mostWeightRpe = if (isNewBestWeight) bestSet.actualRpe else currentMax.mostWeightRpe,
            mostWeightDate = if (isNewBestWeight) LocalDateTime.now() else currentMax.mostWeightDate,
        )
    }

    private fun buildNewMax(
        bestSet: SetLog,
        bestEstimate: OneRMEstimate,
        estimated1RM: Float,
        exerciseId: String,
    ): ExerciseMaxTracking =
        ExerciseMaxTracking(
            userId = bestSet.userId,
            exerciseId = exerciseId,
            oneRMEstimate = WeightFormatter.roundToNearestQuarter(estimated1RM),
            context = bestEstimate.source,
            sourceSetId = bestSet.id,
            recordedAt = LocalDateTime.now(),
            mostWeightLifted = bestSet.actualWeight,
            mostWeightReps = bestSet.actualReps,
            mostWeightRpe = bestSet.actualRpe,
            mostWeightDate = LocalDateTime.now(),
            oneRMConfidence = bestEstimate.confidence,
            oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
            notes = null,
        )

    private data class OneRMEstimate(
        val estimatedMax: Float,
        val confidence: Float,
        val source: String,
    )

    private fun calculateOneRMWithConfidence(
        set: SetLog,
        currentStoredMax: Float? = null,
    ): OneRMEstimate? {
        val reps = set.actualReps
        val weight = set.actualWeight
        val rpe = set.actualRpe

        val effectiveReps = calculateEffectiveReps(reps, rpe)
        val estimated1RM = calculateEstimated1RM(weight, effectiveReps)
        val baseConfidence = calculateBaseConfidence(reps, weight, rpe, currentStoredMax)
        val rpeAdjustment = calculateRpeAdjustment(reps, rpe)
        val finalConfidence = (baseConfidence + rpeAdjustment).coerceIn(0f, 1f)
        val source = formatSourceString(reps, weight, rpe, effectiveReps)

        return OneRMEstimate(
            estimatedMax = estimated1RM,
            confidence = finalConfidence,
            source = source,
        )
    }

    private fun calculateEffectiveReps(
        reps: Int,
        rpe: Float?,
    ): Float =
        if (reps == 1 && rpe != null) {
            val repsInReserve = (10f - rpe).coerceAtLeast(0f)
            reps + repsInReserve
        } else {
            reps.toFloat()
        }

    private fun calculateEstimated1RM(
        weight: Float,
        effectiveReps: Float,
    ): Float =
        when {
            effectiveReps == 1f -> weight
            effectiveReps <= 15f -> weight / (1.0278f - 0.0278f * effectiveReps)
            else -> weight * 1.5f
        }

    private fun calculateBaseConfidence(
        reps: Int,
        weight: Float,
        rpe: Float?,
        currentStoredMax: Float?,
    ): Float =
        when {
            reps == 1 -> calculateSingleRepConfidence(weight, rpe, currentStoredMax)
            reps in 2..3 -> 0.85f
            reps in 4..5 -> 0.75f
            reps in 6..8 -> 0.65f
            reps in 9..12 -> 0.50f
            else -> 0.30f
        }

    private fun calculateSingleRepConfidence(
        weight: Float,
        rpe: Float?,
        currentStoredMax: Float?,
    ): Float =
        when {
            weight > (currentStoredMax ?: 0f) -> 0.90f
            rpe != null && rpe >= 9.5f -> 0.95f
            rpe != null && rpe >= 8.5f -> 0.85f
            rpe != null -> 0.70f
            else -> 0.40f
        }

    private fun calculateRpeAdjustment(
        reps: Int,
        rpe: Float?,
    ): Float =
        when {
            reps == 1 -> 0.0f
            rpe == null -> -0.05f
            rpe >= 9f -> 0.05f
            rpe >= 7f -> 0.0f
            else -> -0.05f
        }

    private fun formatSourceString(
        reps: Int,
        weight: Float,
        rpe: Float?,
        effectiveReps: Float,
    ): String =
        when {
            reps == 1 && rpe != null && rpe < 10 ->
                "1×${weight.roundToInt()}kg @ RPE ${WeightFormatter.formatRPE(rpe)} (est. ${effectiveReps.toInt()}RM)"
            reps == 1 && rpe != null -> "1RM @ RPE ${WeightFormatter.formatRPE(rpe)}"
            reps == 1 -> "1RM"
            rpe != null -> "$reps×${weight.roundToInt()}kg @ RPE ${WeightFormatter.formatRPE(rpe)}"
            else -> "$reps×${weight.roundToInt()}kg"
        }
}
