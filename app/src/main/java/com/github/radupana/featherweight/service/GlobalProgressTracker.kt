package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.data.PendingOneRMUpdate
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
        val userMax = exercise?.let { database.profileDao().getCurrentMax(userId, it.id) }
        val estimatedMax = userMax?.maxWeight ?: 0f

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
        val currentUserMax = exercise?.let { database.profileDao().getCurrentMax(userId, it.id) }
        val storedMaxWeight = currentUserMax?.maxWeight

        // Find the best set for 1RM calculation with confidence scoring
        var bestEstimate: OneRMEstimate? = null

        for (set in estimableSets) {
            val estimate = calculateOneRMWithConfidence(set, storedMaxWeight)
            if (estimate != null && (bestEstimate == null || estimate.confidence > bestEstimate.confidence)) {
                bestEstimate = estimate
            }
        }

        // Only proceed if we have a reasonable confidence estimate (lowered for testing)
        if (bestEstimate == null || bestEstimate.confidence < 0.5f) {
            println(
                "⚠️ Low confidence 1RM estimate: ${bestEstimate?.confidence?.let { (it * 100).roundToInt() }}% (need 50%), skipping update",
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
        println("🔍 1RM Update Check: currentUserMax=${currentUserMax?.maxWeight}, estimated1RM=$estimated1RM")

        // Decision logic for prompting user
        val pendingUpdate =
            when {
                // Clear improvement over stored max - prompt update
                currentUserMax != null && estimated1RM > currentUserMax.maxWeight * 1.02 && isBig4Exercise -> {
                    println(
                        "🎯 New estimated 1RM (${estimated1RM.roundToInt()}kg) exceeds stored max (${currentUserMax.maxWeight.roundToInt()}kg)",
                    )
                    exercise?.let {
                        PendingOneRMUpdate(
                            exerciseId = it.id,
                            exerciseName = progress.exerciseName,
                            currentMax = currentUserMax.maxWeight,
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
                            if (currentUserMax.maxWeight > 0) {
                                ((estimated1RM - currentUserMax.maxWeight) / currentUserMax.maxWeight) * 100
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

    // New approach: calculate confidence based on baseline comparison + set characteristics
    private fun calculateOneRMWithConfidence(
        set: SetLog,
        currentStoredMax: Float? = null,
    ): OneRMEstimate? {
        val reps = set.actualReps
        val weight = set.actualWeight
        val rpe = set.actualRpe

        println("🔍 1RM Confidence Calc: ${weight}kg x $reps, RPE = ${rpe ?: "null"}, stored = ${currentStoredMax ?: "none"}")

        // Calculate basic estimated 1RM using Brzycki formula
        val estimated1RM =
            if (reps == 1) {
                weight
            } else {
                weight / (1.0278f - 0.0278f * reps)
            }

        // Base confidence on set characteristics
        val baseConfidence =
            when (reps) {
                1 -> 0.9f // Singles are very reliable
                in 2..3 -> 0.85f // Low reps are quite reliable
                in 4..6 -> 0.75f // Medium reps are decent
                in 7..10 -> 0.6f // Higher reps less reliable
                else -> 0.4f // Very high reps unreliable
            }

        // Adjust confidence based on stored baseline comparison
        val finalConfidence =
            if (currentStoredMax != null && currentStoredMax > 0) {
                val improvement = (estimated1RM - currentStoredMax) / currentStoredMax
                when {
                    improvement >= 0.05f -> Math.min(0.95f, baseConfidence + 0.2f) // 5%+ improvement = high confidence
                    improvement >= 0.02f -> Math.min(0.9f, baseConfidence + 0.1f) // 2%+ improvement = good confidence
                    improvement >= -0.02f -> baseConfidence // Similar to stored = base confidence
                    else -> Math.max(0.3f, baseConfidence - 0.2f) // Worse than stored = lower confidence
                }
            } else {
                // No stored baseline - use base confidence with RPE modifier
                if (rpe != null && rpe >= 8f) {
                    Math.min(0.9f, baseConfidence + 0.1f) // High RPE bonus
                } else {
                    baseConfidence
                }
            }

        // Apply RPE modifier for 1RM calculation
        val adjustedEstimate =
            if (reps == 1 && rpe != null) {
                weight * (1 + (10 - rpe) * 0.025f) // RPE adjustment for singles
            } else {
                estimated1RM
            }

        val result =
            OneRMEstimate(
                estimatedMax = adjustedEstimate,
                confidence = finalConfidence,
                source =
                    when {
                        reps == 1 && rpe != null -> "Single @ RPE $rpe"
                        reps == 1 -> "Single (no RPE)"
                        rpe != null -> "$reps reps @ RPE $rpe"
                        else -> "$reps reps (no RPE)"
                    },
            )

        println("🔍 1RM Confidence Result: ${result.estimatedMax.roundToInt()}kg, ${(result.confidence * 100).roundToInt()}% confidence")
        return result
    }

    /**
     * Analyzes exercise progress to provide actionable insights
     */
    suspend fun analyzeExerciseProgress(
        userId: Long,
        exerciseName: String,
    ): ProgressAnalysis {
        val progress =
            globalProgressDao.getProgressForExercise(userId, exerciseName)
                ?: return ProgressAnalysis(
                    exerciseName = exerciseName,
                    hasData = false,
                    suggestion = "No data yet. Complete a few workouts to see insights!",
                    confidence = 0f,
                )

        val suggestion =
            when (progress.trend) {
                ProgressTrend.STALLING -> {
                    when {
                        progress.consecutiveStalls >= 3 -> {
                            "You've stalled for ${progress.consecutiveStalls} sessions. " +
                                "Consider a deload to ${(progress.currentWorkingWeight * 0.85).roundToInt()}kg"
                        }
                        progress.recentAvgRpe != null && progress.recentAvgRpe > 9 -> {
                            "High RPE (${progress.recentAvgRpe}) suggests fatigue. Maintain current weight."
                        }
                        else -> {
                            "Stalled at ${progress.currentWorkingWeight}kg. Try adding 2.5kg next session."
                        }
                    }
                }
                ProgressTrend.IMPROVING -> {
                    when {
                        progress.recentAvgRpe != null && progress.recentAvgRpe < 7 -> {
                            "Low RPE (${progress.recentAvgRpe}) - you can handle more! " +
                                "Try ${(progress.currentWorkingWeight * 1.05).roundToInt()}kg"
                        }
                        else -> {
                            "Great progress! Continue with current progression."
                        }
                    }
                }
                ProgressTrend.DECLINING -> {
                    "Performance declining. Consider a deload week or check recovery/nutrition."
                }
            }

        return ProgressAnalysis(
            exerciseName = exerciseName,
            hasData = true,
            currentWeight = progress.currentWorkingWeight,
            estimatedMax = progress.estimatedMax,
            trend = progress.trend,
            suggestion = suggestion,
            confidence = calculateConfidence(progress),
            lastPR = progress.lastPrDate,
            stalledSessions = progress.consecutiveStalls,
        )
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
