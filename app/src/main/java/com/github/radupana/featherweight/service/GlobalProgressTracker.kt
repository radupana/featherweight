package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.repository.FeatherweightRepository
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
        workoutId: Long,
        userId: Long,
    ): List<PendingOneRMUpdate> {
        val pendingUpdates = mutableListOf<PendingOneRMUpdate>()
        println("🔄 GlobalProgressTracker: Updating progress for workout $workoutId")

        val workout = repository.getWorkoutById(workoutId) ?: return emptyList()
        val exercises = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)

        for (exercise in exercises) {
            val pendingUpdate =
                updateExerciseProgress(
                    userId = userId,
                    exerciseName = exercise.exerciseName,
                    workoutId = workoutId,
                    isProgrammeWorkout = workout.programmeId != null,
                )
            pendingUpdate?.let { pendingUpdates.add(it) }
        }

        return pendingUpdates
    }

    private suspend fun updateExerciseProgress(
        userId: Long,
        exerciseName: String,
        workoutId: Long,
        isProgrammeWorkout: Boolean,
    ): PendingOneRMUpdate? {
        val exerciseLogs =
            database
                .exerciseLogDao()
                .getExerciseLogsForWorkout(workoutId)
                .filter { it.exerciseName == exerciseName }

        if (exerciseLogs.isEmpty()) return null

        val sets = exerciseLogs.flatMap { database.setLogDao().getSetLogsForExercise(it.id) }
        val completedSets = sets.filter { it.isCompleted }

        if (completedSets.isEmpty()) {
            println("⚠️ No completed sets for $exerciseName, skipping progress update")
            return null
        }

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
            globalProgressDao.getProgressForExercise(userId, exerciseName)
                ?: createInitialProgress(userId, exerciseName)

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
        val (updatedProgress, pendingUpdate) = updateEstimatedMax(progress, completedSets, userId)
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

        println(
            "✅ Updated progress for $exerciseName: " +
                "Weight: ${previousWeight}kg → ${maxWeight}kg, " +
                "Volume: ${sessionVolume}kg, " +
                "Trend: ${progress.trend}",
        )

        return pendingUpdate
    }

    private suspend fun createInitialProgress(
        userId: Long,
        exerciseName: String,
    ): GlobalExerciseProgress {
        // Try to get 1RM from UserExerciseMax
        val exercise = database.exerciseDao().findExerciseByExactName(exerciseName)
        val userMax = exercise?.let { database.oneRMDao().getCurrentMax(userId, it.id) }
        val estimatedMax = userMax?.oneRMEstimate ?: 0f

        return GlobalExerciseProgress(
            userId = userId,
            exerciseName = exerciseName,
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
                println("🎉 New 1RM PR: ${weight}kg!")
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
        userId: Long,
    ): Pair<GlobalExerciseProgress, PendingOneRMUpdate?> {
        // Only consider sets with sufficient data for estimation
        val estimableSets =
            sets.filter { set ->
                set.actualReps in 1..12 && set.actualWeight > 0
            }

        if (estimableSets.isEmpty()) return Pair(progress, null)

        // Get current stored max from profile FIRST for confidence calculation
        val exercise = database.exerciseDao().findExerciseByExactName(progress.exerciseName)
        val currentUserMax = exercise?.let { database.oneRMDao().getCurrentMax(userId, it.id) }
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
                        if (bestEstimate == null ||
                            // Prefer lower reps
                            set.actualReps < (
                                estimableSets.find { it.actualWeight == bestEstimate.estimatedMax }?.actualReps
                                    ?: Int.MAX_VALUE
                            ) ||
                            // If same reps, prefer higher confidence
                            (
                                set.actualReps == estimableSets.find { it.actualWeight == bestEstimate.estimatedMax }?.actualReps &&
                                    estimate.confidence > bestEstimate.confidence
                            )
                        ) {
                            bestEstimate = estimate
                        }
                    }
                }
            }
        }

        // Only proceed if we have confidence >= 60%
        if (bestEstimate == null || bestEstimate.confidence < 0.6f) {
            println(
                "⚠️ Low confidence 1RM estimate: ${bestEstimate?.confidence?.let { (it * 100).roundToInt() }}% (need 60%), skipping update",
            )
            return Pair(progress, null)
        }

        val estimated1RM = bestEstimate.estimatedMax
        println("💪 Estimated 1RM: ${estimated1RM.roundToInt()}kg (confidence: ${(bestEstimate.confidence * 100).roundToInt()}%)")

        // Check if this is a Big 4 exercise
        val isBig4Exercise =
            progress.exerciseName in
                listOf(
                    "Barbell Back Squat",
                    "Barbell Deadlift",
                    "Barbell Bench Press",
                    "Barbell Overhead Press",
                )

        println("🔍 1RM Update Check: exercise=${progress.exerciseName}, isBig4=$isBig4Exercise")
        println("🔍 1RM Update Check: currentUserMax=${currentUserMax?.oneRMEstimate}, estimated1RM=$estimated1RM")

        // Decision logic for prompting user
        val pendingUpdate =
            when {
                // Clear improvement over stored max - prompt update
                // For actual 1-rep attempts (reps = 1), use a lower threshold (any improvement)
                // For rep-based estimates, use the 2% threshold
                currentUserMax != null &&
                    isBig4Exercise &&
                    (
                        (bestEstimate.source.contains("1 rep") && estimated1RM > currentUserMax.oneRMEstimate) ||
                            (estimated1RM > currentUserMax.oneRMEstimate * 1.02)
                    ) -> {
                    println(
                        "🎯 New estimated 1RM (${estimated1RM.roundToInt()}kg) exceeds stored max (${currentUserMax.oneRMEstimate.roundToInt()}kg)",
                    )
                    exercise?.let {
                        PendingOneRMUpdate(
                            exerciseId = it.id,
                            exerciseName = progress.exerciseName,
                            currentMax = currentUserMax.oneRMEstimate,
                            suggestedMax = estimated1RM,
                            confidence = bestEstimate.confidence,
                            source = bestEstimate.source,
                        )
                    }
                }

                // No stored max but high confidence estimate - suggest adding
                currentUserMax == null && bestEstimate.confidence >= 0.85f && isBig4Exercise -> {
                    println("💡 High confidence 1RM estimate (${estimated1RM.roundToInt()}kg) - suggest adding to profile")
                    exercise?.let {
                        PendingOneRMUpdate(
                            exerciseId = it.id,
                            exerciseName = progress.exerciseName,
                            currentMax = null,
                            suggestedMax = estimated1RM,
                            confidence = bestEstimate.confidence,
                            source = bestEstimate.source,
                        )
                    }
                }

                else -> {
                    println("🔍 1RM Update Check: No update triggered")
                    if (currentUserMax != null) {
                        val improvement =
                            if (currentUserMax.oneRMEstimate > 0) {
                                ((estimated1RM - currentUserMax.oneRMEstimate) / currentUserMax.oneRMEstimate) * 100
                            } else {
                                0f
                            }
                        println("🔍 1RM Update Check: Has stored max, improvement = $improvement% (need 2%+)")
                    } else {
                        println(
                            "🔍 1RM Update Check: No stored max, confidence = ${(bestEstimate.confidence * 100).roundToInt()}% (need 85%+)",
                        )
                    }
                    null
                }
            }

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

        println("🔍 1RM Confidence Calc: ${weight}kg x $reps, RPE = ${rpe ?: "null"}")

        // Calculate estimated 1RM using Brzycki formula
        val estimated1RM =
            if (reps == 1) {
                weight // Actual 1RM!
            } else {
                weight / (1.0278f - 0.0278f * reps)
            }

        // Base confidence primarily on rep count
        val baseConfidence =
            when (reps) {
                1 -> 1.0f // Singles are 100% confidence - it's your actual 1RM!
                in 2..3 -> 0.85f // Very reliable
                in 4..5 -> 0.75f // Reliable
                in 6..8 -> 0.65f // Decent
                in 9..12 -> 0.50f // Less reliable
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
                0.0f // No adjustment for singles
            }

        val finalConfidence = (baseConfidence + rpeAdjustment).coerceIn(0f, 1f)

        // For singles, we don't apply RPE adjustment to the weight - it IS the 1RM
        val adjustedEstimate =
            if (reps == 1) {
                weight // Always use actual weight for singles
            } else {
                estimated1RM
            }

        // Format source string
        val source =
            when {
                reps == 1 && rpe != null -> "1RM @ RPE ${rpe.toInt()}"
                reps == 1 -> "1RM"
                rpe != null -> "$reps×${weight.roundToInt()}kg @ RPE ${rpe.toInt()}"
                else -> "$reps×${weight.roundToInt()}kg"
            }

        val result =
            OneRMEstimate(
                estimatedMax = adjustedEstimate,
                confidence = finalConfidence,
                source = source,
            )

        println("🔍 1RM Confidence Result: ${result.estimatedMax.roundToInt()}kg, ${(result.confidence * 100).roundToInt()}% confidence")
        return result
    }

    private fun calculateConfidence(progress: GlobalExerciseProgress): Float {
        // Higher confidence with more data points and consistent RPE
        val dataPoints = minOf(progress.sessionsTracked / 10f, 1f)
        val rpeConsistency = if (progress.recentAvgRpe != null) 0.2f else 0f
        return (dataPoints * 0.8f + rpeConsistency).coerceIn(0f, 1f)
    }
}

data class ProgressAnalysis(
    val exerciseName: String,
    val hasData: Boolean,
    val currentWeight: Float = 0f,
    val estimatedMax: Float = 0f,
    val trend: ProgressTrend? = null,
    val suggestion: String,
    val confidence: Float,
    val lastPR: LocalDateTime? = null,
    val stalledSessions: Int = 0,
)
