package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.repository.FeatherweightRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class GlobalProgressTracker(
    private val repository: FeatherweightRepository,
    private val database: FeatherweightDatabase
) {
    private val globalProgressDao = database.globalExerciseProgressDao()
    
    /**
     * Updates global exercise progress after any workout completion (programme or freestyle)
     */
    suspend fun updateProgressAfterWorkout(workoutId: Long, userId: Long): List<PendingOneRMUpdate> {
        val pendingUpdates = mutableListOf<PendingOneRMUpdate>()
        println("🔄 GlobalProgressTracker: Updating progress for workout $workoutId")
        
        val workout = repository.getWorkoutById(workoutId) ?: return emptyList()
        val exercises = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)
        
        for (exercise in exercises) {
            val pendingUpdate = updateExerciseProgress(
                userId = userId,
                exerciseName = exercise.exerciseName,
                workoutId = workoutId,
                isProgrammeWorkout = workout.programmeId != null
            )
            pendingUpdate?.let { pendingUpdates.add(it) }
        }
        
        return pendingUpdates
    }
    
    private suspend fun updateExerciseProgress(
        userId: Long,
        exerciseName: String,
        workoutId: Long,
        isProgrammeWorkout: Boolean
    ): PendingOneRMUpdate? {
        val exerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)
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
        val sessionVolume = completedSets.sumOf { 
            (it.actualWeight * it.actualReps).toDouble() 
        }.toFloat()
        val avgRpe = completedSets.mapNotNull { it.actualRpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        
        // Get or create progress record
        var progress = globalProgressDao.getProgressForExercise(userId, exerciseName)
            ?: createInitialProgress(userId, exerciseName)
        
        // Update working weight
        val previousWeight = progress.currentWorkingWeight
        progress = progress.copy(
            currentWorkingWeight = maxWeight,
            lastUpdated = LocalDateTime.now(),
            sessionsTracked = progress.sessionsTracked + 1,
            lastSessionVolume = sessionVolume
        )
        
        // Update RPE tracking
        if (avgRpe != null) {
            val newAvgRpe = if (progress.recentAvgRpe != null) {
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
        progress = if (isProgrammeWorkout) {
            progress.copy(lastProgrammeWorkoutId = workoutId)
        } else {
            progress.copy(lastFreestyleWorkoutId = workoutId)
        }
        
        // Save updated progress
        globalProgressDao.insertOrUpdate(progress)
        
        println("✅ Updated progress for $exerciseName: " +
                "Weight: ${previousWeight}kg → ${maxWeight}kg, " +
                "Volume: ${sessionVolume}kg, " +
                "Trend: ${progress.trend}")
        
        return pendingUpdate
    }
    
    private suspend fun createInitialProgress(userId: Long, exerciseName: String): GlobalExerciseProgress {
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
            trend = ProgressTrend.STALLING
        )
    }
    
    private fun checkAndUpdatePRs(
        progress: GlobalExerciseProgress,
        sets: List<SetLog>
    ): GlobalExerciseProgress {
        var updated = progress
        
        // Check various rep PRs
        val maxByReps = sets.groupBy { it.actualReps }
            .mapValues { (_, setList) -> setList.maxOf { it.actualWeight } }
        
        // Update 1RM
        maxByReps[1]?.let { weight ->
            if (weight > (updated.bestSingleRep ?: 0f)) {
                updated = updated.copy(
                    bestSingleRep = weight,
                    lastPrDate = LocalDateTime.now(),
                    lastPrWeight = weight
                )
                println("🎉 New 1RM PR: ${weight}kg!")
            }
        }
        
        // Update 3RM
        maxByReps[3]?.let { weight ->
            if (weight > (updated.best3Rep ?: 0f)) {
                updated = updated.copy(best3Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated = updated.copy(
                        lastPrDate = LocalDateTime.now(),
                        lastPrWeight = weight
                    )
                }
            }
        }
        
        // Update 5RM
        maxByReps[5]?.let { weight ->
            if (weight > (updated.best5Rep ?: 0f)) {
                updated = updated.copy(best5Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated = updated.copy(
                        lastPrDate = LocalDateTime.now(),
                        lastPrWeight = weight
                    )
                }
            }
        }
        
        // Update 8RM
        maxByReps[8]?.let { weight ->
            if (weight > (updated.best8Rep ?: 0f)) {
                updated = updated.copy(best8Rep = weight)
                if (weight > (updated.lastPrWeight ?: 0f)) {
                    updated = updated.copy(
                        lastPrDate = LocalDateTime.now(),
                        lastPrWeight = weight
                    )
                }
            }
        }
        
        return updated
    }
    
    private fun updateStallTracking(
        progress: GlobalExerciseProgress,
        previousWeight: Float,
        currentWeight: Float
    ): GlobalExerciseProgress {
        return when {
            currentWeight > previousWeight -> {
                // Progression!
                progress.copy(
                    consecutiveStalls = 0,
                    weeksAtCurrentWeight = 0,
                    lastProgressionDate = LocalDateTime.now(),
                    failureStreak = 0,
                    trend = ProgressTrend.IMPROVING
                )
            }
            currentWeight == previousWeight -> {
                // Stalled at same weight
                val weeksSinceLastUpdate = if (progress.lastProgressionDate != null) {
                    ChronoUnit.WEEKS.between(progress.lastProgressionDate, LocalDateTime.now()).toInt()
                } else {
                    1
                }
                
                val newTrend = if (progress.consecutiveStalls >= 2) {
                    ProgressTrend.STALLING
                } else {
                    progress.trend
                }
                
                progress.copy(
                    consecutiveStalls = progress.consecutiveStalls + 1,
                    weeksAtCurrentWeight = weeksSinceLastUpdate,
                    trend = newTrend
                )
            }
            else -> {
                // Regression
                progress.copy(
                    consecutiveStalls = 0,
                    weeksAtCurrentWeight = 0,
                    failureStreak = progress.failureStreak + 1,
                    trend = ProgressTrend.DECLINING
                )
            }
        }
    }
    
    private fun updateVolumeTrends(
        progress: GlobalExerciseProgress,
        sessionVolume: Float
    ): GlobalExerciseProgress {
        val avgVolume = progress.avgSessionVolume ?: sessionVolume
        val newAvgVolume = (avgVolume * progress.sessionsTracked + sessionVolume) / (progress.sessionsTracked + 1)
        
        val volumeTrend = when {
            sessionVolume > avgVolume * 1.1 -> VolumeTrend.INCREASING
            sessionVolume < avgVolume * 0.9 -> VolumeTrend.DECREASING
            else -> VolumeTrend.MAINTAINING
        }
        
        // Update 30-day volume (simplified - would need date filtering in production)
        val new30DayVolume = progress.totalVolumeLast30Days + sessionVolume
        
        return progress.copy(
            avgSessionVolume = newAvgVolume,
            volumeTrend = volumeTrend,
            totalVolumeLast30Days = new30DayVolume
        )
    }
    
    private suspend fun updateEstimatedMax(
        progress: GlobalExerciseProgress,
        sets: List<SetLog>,
        userId: Long
    ): Pair<GlobalExerciseProgress, PendingOneRMUpdate?> {
        // Only consider sets with sufficient data for estimation
        val estimableSets = sets.filter { set ->
            set.actualReps in 1..12 && set.actualWeight > 0
        }
        
        if (estimableSets.isEmpty()) return Pair(progress, null)
        
        // Find the best set for 1RM calculation with confidence scoring
        var bestEstimate: OneRMEstimate? = null
        
        for (set in estimableSets) {
            val estimate = calculateOneRMWithConfidence(set)
            if (estimate != null && (bestEstimate == null || estimate.confidence > bestEstimate.confidence)) {
                bestEstimate = estimate
            }
        }
        
        // Only proceed if we have a high-confidence estimate
        if (bestEstimate == null || bestEstimate.confidence < 0.7f) {
            println("⚠️ Low confidence 1RM estimate, skipping update")
            return Pair(progress, null)
        }
        
        val estimated1RM = bestEstimate.estimatedMax
        println("💪 Estimated 1RM: ${estimated1RM.roundToInt()}kg (confidence: ${(bestEstimate.confidence * 100).roundToInt()}%)")
        
        // Get current stored max from profile
        val exercise = database.exerciseDao().findExerciseByExactName(progress.exerciseName)
        val currentUserMax = exercise?.let { database.profileDao().getCurrentMax(userId, it.id) }
        
        // Check if this is a Big 4 exercise
        val isBig4Exercise = progress.exerciseName in listOf(
            "Barbell Back Squat", 
            "Barbell Deadlift", 
            "Barbell Bench Press", 
            "Barbell Overhead Press"
        )
        
        // Decision logic for prompting user
        val pendingUpdate = when {
            // Clear improvement over stored max - prompt update
            currentUserMax != null && estimated1RM > currentUserMax.maxWeight * 1.02 && isBig4Exercise -> {
                println("🎯 New estimated 1RM (${estimated1RM.roundToInt()}kg) exceeds stored max (${currentUserMax.maxWeight.roundToInt()}kg)")
                exercise?.let {
                    PendingOneRMUpdate(
                        exerciseId = it.id,
                        exerciseName = progress.exerciseName,
                        currentMax = currentUserMax.maxWeight,
                        suggestedMax = estimated1RM,
                        confidence = bestEstimate.confidence,
                        source = bestEstimate.source
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
                        source = bestEstimate.source
                    )
                }
            }
            
            else -> null
        }
        
        // Always update internal tracking
        val updatedProgress = if (estimated1RM > progress.estimatedMax) {
            progress.copy(estimatedMax = estimated1RM)
        } else {
            progress
        }
        
        return Pair(updatedProgress, pendingUpdate)
    }
    
    private data class OneRMEstimate(
        val estimatedMax: Float,
        val confidence: Float,
        val source: String
    )
    
    private fun calculateOneRMWithConfidence(set: SetLog): OneRMEstimate? {
        val reps = set.actualReps
        val weight = set.actualWeight
        val rpe = set.actualRpe
        
        // Special handling for singles
        if (reps == 1) {
            return when {
                // High RPE single - very close to true max
                rpe != null && rpe >= 9f -> OneRMEstimate(
                    estimatedMax = weight * (1 + (10 - rpe) * 0.025f), // 2.5% per RPE point
                    confidence = 0.95f,
                    source = "Single @ RPE $rpe"
                )
                // Single with no RPE - could be anything
                rpe == null -> null
                // Low RPE single - less reliable
                else -> OneRMEstimate(
                    estimatedMax = weight * (1 + (10 - rpe) * 0.025f),
                    confidence = 0.6f,
                    source = "Single @ RPE $rpe"
                )
            }
        }
        
        // For multiple reps, adjust for RPE
        val effectiveReps = if (rpe != null) {
            // Add reps in reserve based on RPE
            reps + (10 - rpe).toInt()
        } else {
            // No RPE - assume RPE 8 for conservative estimate
            reps + 2
        }
        
        // Calculate confidence based on rep range and RPE availability
        val confidence = when {
            rpe == null -> 0.5f // Low confidence without RPE
            reps in 2..5 && rpe >= 8 -> 0.9f // Sweet spot
            reps in 2..5 && rpe >= 7 -> 0.8f
            reps in 6..8 && rpe >= 8 -> 0.75f
            reps in 9..12 && rpe >= 8 -> 0.65f
            else -> 0.4f
        }
        
        // Skip very low confidence estimates
        if (confidence < 0.5f) return null
        
        // Use Epley formula with effective reps
        val estimated1RM = if (effectiveReps >= 1) {
            weight * (1 + effectiveReps / 30f)
        } else {
            weight // Already at max
        }
        
        val source = if (rpe != null) {
            "${reps}×${weight}kg @ RPE $rpe"
        } else {
            "${reps}×${weight}kg (no RPE)"
        }
        
        return OneRMEstimate(estimated1RM, confidence, source)
    }
    
    /**
     * Analyzes exercise progress to provide actionable insights
     */
    suspend fun analyzeExerciseProgress(userId: Long, exerciseName: String): ProgressAnalysis {
        val progress = globalProgressDao.getProgressForExercise(userId, exerciseName)
            ?: return ProgressAnalysis(
                exerciseName = exerciseName,
                hasData = false,
                suggestion = "No data yet. Complete a few workouts to see insights!",
                confidence = 0f
            )
        
        val suggestion = when (progress.trend) {
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
            stalledSessions = progress.consecutiveStalls
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
    val stalledSessions: Int = 0
)