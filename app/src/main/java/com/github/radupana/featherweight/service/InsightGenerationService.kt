package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Service responsible for generating AI-powered insights about user's training progress
 */
class InsightGenerationService(
    private val database: FeatherweightDatabase
) {
    
    private val progressInsightDao = database.progressInsightDao()
    private val workoutDao = database.workoutDao()
    private val setLogDao = database.setLogDao()
    private val exerciseLogDao = database.exerciseLogDao()
    private val globalProgressDao = database.globalExerciseProgressDao()
    private val personalRecordDao = database.personalRecordDao()
    
    /**
     * Generate all insights for a user based on their recent training data
     */
    suspend fun generateInsightsForUser(userId: Long): List<ProgressInsight> = withContext(Dispatchers.IO) {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        
        // Avoid generating duplicate insights within the last 24 hours
        val duplicateCheckTime = now.minusHours(24)
        
        // Generate different types of insights
        insights.addAll(generateStrengthProgressInsights(userId, duplicateCheckTime))
        insights.addAll(generatePlateauWarningInsights(userId, duplicateCheckTime))
        insights.addAll(generateConsistencyInsights(userId, duplicateCheckTime))
        insights.addAll(generateVolumeAnalysisInsights(userId, duplicateCheckTime))
        insights.addAll(generateAutoregulationInsights(userId, duplicateCheckTime))
        insights.addAll(generateRecoveryInsights(userId, duplicateCheckTime))
        
        insights
    }
    
    /**
     * Generate insights about strength progress
     */
    private suspend fun generateStrengthProgressInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        val twoWeeksAgo = now.minusWeeks(2)
        val oneMonthAgo = now.minusMonths(1)
        
        // Get all progress data
        val allProgress = globalProgressDao.getAllProgress(userId)
        
        for (progress in allProgress) {
            // Skip if we already generated a similar insight recently
            val similarCount = progressInsightDao.getSimilarInsightCount(
                userId, InsightType.STRENGTH_PROGRESS, progress.exerciseName, duplicateCheckTime
            )
            if (similarCount > 0) continue
            
            // Check for significant strength gains
            if (progress.lastProgressionDate != null && 
                progress.lastProgressionDate.isAfter(twoWeeksAgo) &&
                progress.trend == ProgressTrend.IMPROVING) {
                
                // Calculate improvement percentage
                val previousWeight = progress.currentWorkingWeight * 0.9f // Rough estimate
                val improvementPercent = if (previousWeight > 0) {
                    ((progress.currentWorkingWeight - previousWeight) / previousWeight) * 100
                } else 0f
                
                when {
                    improvementPercent >= 15f -> {
                        insights.add(ProgressInsight(
                            userId = userId,
                            insightType = InsightType.STRENGTH_PROGRESS,
                            title = "🚀 Exceptional ${progress.exerciseName} Progress!",
                            message = "You've made outstanding progress on ${progress.exerciseName} with a ${String.format("%.1f", improvementPercent)}% strength increase this month. Keep up the excellent work!",
                            data = """{"exercise": "${progress.exerciseName}", "improvement": $improvementPercent, "workingWeight": ${progress.currentWorkingWeight}}""",
                            exerciseName = progress.exerciseName,
                            priority = InsightPriority.MEDIUM,
                            generatedDate = now
                        ))
                    }
                    improvementPercent >= 8f -> {
                        insights.add(ProgressInsight(
                            userId = userId,
                            insightType = InsightType.STRENGTH_PROGRESS,
                            title = "💪 Great ${progress.exerciseName} Gains",
                            message = "Solid ${String.format("%.1f", improvementPercent)}% strength improvement on ${progress.exerciseName} recently. Your progressive overload is working well!",
                            data = """{"exercise": "${progress.exerciseName}", "improvement": $improvementPercent, "workingWeight": ${progress.currentWorkingWeight}}""",
                            exerciseName = progress.exerciseName,
                            priority = InsightPriority.LOW,
                            generatedDate = now
                        ))
                    }
                }
            }
        }
        
        return insights
    }
    
    /**
     * Generate plateau warning insights
     */
    private suspend fun generatePlateauWarningInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        
        val allProgress = globalProgressDao.getAllProgress(userId)
        
        for (progress in allProgress) {
            // Skip if we already generated a similar insight recently
            val similarCount = progressInsightDao.getSimilarInsightCount(
                userId, InsightType.PLATEAU_WARNING, progress.exerciseName, duplicateCheckTime
            )
            if (similarCount > 0) continue
            
            // Check for stagnation or declining trend
            when {
                progress.consecutiveStalls >= 3 && progress.trend == ProgressTrend.STALLING -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.PLATEAU_WARNING,
                        title = "⚠️ ${progress.exerciseName} Plateau Detected",
                        message = "Your ${progress.exerciseName} has stalled for ${progress.consecutiveStalls} sessions. Consider a deload (reduce weight by 10-15%) or technique refinement.",
                        data = """{"exercise": "${progress.exerciseName}", "stallCount": ${progress.consecutiveStalls}, "currentWeight": ${progress.currentWorkingWeight}}""",
                        exerciseName = progress.exerciseName,
                        priority = InsightPriority.HIGH,
                        generatedDate = now,
                        isActionable = true,
                        actionType = "deload"
                    ))
                }
                progress.trend == ProgressTrend.DECLINING -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.PLATEAU_WARNING,
                        title = "📉 ${progress.exerciseName} Performance Declining",
                        message = "Your ${progress.exerciseName} performance is declining. This could indicate overreaching, poor recovery, or technique issues. Consider reducing volume temporarily.",
                        data = """{"exercise": "${progress.exerciseName}", "trend": "declining", "currentWeight": ${progress.currentWorkingWeight}}""",
                        exerciseName = progress.exerciseName,
                        priority = InsightPriority.CRITICAL,
                        generatedDate = now,
                        isActionable = true,
                        actionType = "reduce_volume"
                    ))
                }
            }
        }
        
        return insights
    }
    
    /**
     * Generate consistency insights
     */
    private suspend fun generateConsistencyInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        val oneWeekAgo = now.minusWeeks(1)
        val twoWeeksAgo = now.minusWeeks(2)
        
        // Skip if we already generated a similar insight recently
        val similarCount = progressInsightDao.getSimilarInsightCount(
            userId, InsightType.CONSISTENCY_PRAISE, null, duplicateCheckTime
        )
        if (similarCount > 0) return insights
        
        // Get completed workouts in the last week and two weeks
        val allWorkouts = workoutDao.getAllWorkouts()
        val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
        
        val thisWeekWorkouts = completedWorkouts.filter { it.date.isAfter(oneWeekAgo) }
        val lastWeekWorkouts = completedWorkouts.filter { 
            it.date.isAfter(twoWeeksAgo) && it.date.isBefore(oneWeekAgo) 
        }
        
        when {
            thisWeekWorkouts.size >= 4 -> {
                insights.add(ProgressInsight(
                    userId = userId,
                    insightType = InsightType.CONSISTENCY_PRAISE,
                    title = "🔥 Outstanding Consistency!",
                    message = "You've completed ${thisWeekWorkouts.size} workouts this week! This level of consistency is the foundation of all progress.",
                    data = """{"weeklyWorkouts": ${thisWeekWorkouts.size}, "period": "this_week"}""",
                    priority = InsightPriority.LOW,
                    generatedDate = now
                ))
            }
            thisWeekWorkouts.size >= 3 -> {
                insights.add(ProgressInsight(
                    userId = userId,
                    insightType = InsightType.CONSISTENCY_PRAISE,
                    title = "💪 Great Training Week!",
                    message = "You've trained ${thisWeekWorkouts.size} times this week. Excellent consistency!",
                    data = """{"weeklyWorkouts": ${thisWeekWorkouts.size}, "period": "this_week"}""",
                    priority = InsightPriority.LOW,
                    generatedDate = now
                ))
            }
            thisWeekWorkouts.size < 2 && lastWeekWorkouts.size >= 3 -> {
                insights.add(ProgressInsight(
                    userId = userId,
                    insightType = InsightType.CONSISTENCY_PRAISE,
                    title = "📅 Consistency Check",
                    message = "You trained well last week (${lastWeekWorkouts.size} sessions) but only ${thisWeekWorkouts.size} time(s) this week. Try to maintain your momentum!",
                    data = """{"thisWeek": ${thisWeekWorkouts.size}, "lastWeek": ${lastWeekWorkouts.size}}""",
                    priority = InsightPriority.MEDIUM,
                    generatedDate = now,
                    isActionable = true,
                    actionType = "schedule_workout"
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Generate volume analysis insights
     */
    private suspend fun generateVolumeAnalysisInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        val oneWeekAgo = now.minusWeeks(1)
        val twoWeeksAgo = now.minusWeeks(2)
        
        // Skip if we already generated a similar insight recently
        val similarCount = progressInsightDao.getSimilarInsightCount(
            userId, InsightType.VOLUME_ANALYSIS, null, duplicateCheckTime
        )
        if (similarCount > 0) return insights
        
        // Calculate volume for this week vs last week
        val allWorkouts = workoutDao.getAllWorkouts()
        val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
        
        val thisWeekWorkouts = completedWorkouts.filter { it.date.isAfter(oneWeekAgo) }
        val lastWeekWorkouts = completedWorkouts.filter { 
            it.date.isAfter(twoWeeksAgo) && it.date.isBefore(oneWeekAgo) 
        }
        
        // Calculate total volume
        var thisWeekVolume = 0f
        var lastWeekVolume = 0f
        
        for (workout in thisWeekWorkouts) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            for (exerciseLog in exerciseLogs) {
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id).filter { it.isCompleted }
                thisWeekVolume += sets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()
            }
        }
        
        for (workout in lastWeekWorkouts) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            for (exerciseLog in exerciseLogs) {
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id).filter { it.isCompleted }
                lastWeekVolume += sets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()
            }
        }
        
        if (lastWeekVolume > 0) {
            val volumeChange = ((thisWeekVolume - lastWeekVolume) / lastWeekVolume) * 100
            
            when {
                volumeChange >= 20f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.VOLUME_ANALYSIS,
                        title = "📈 Significant Volume Increase",
                        message = "Your training volume increased by ${String.format("%.1f", volumeChange)}% this week (${String.format("%.0f", thisWeekVolume)}kg vs ${String.format("%.0f", lastWeekVolume)}kg). Monitor recovery carefully!",
                        data = """{"thisWeekVolume": $thisWeekVolume, "lastWeekVolume": $lastWeekVolume, "change": $volumeChange}""",
                        priority = InsightPriority.MEDIUM,
                        generatedDate = now,
                        isActionable = true,
                        actionType = "monitor_recovery"
                    ))
                }
                volumeChange <= -15f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.VOLUME_ANALYSIS,
                        title = "📉 Volume Decrease",
                        message = "Your training volume decreased by ${String.format("%.1f", abs(volumeChange))}% this week. This might be intentional deload or missed sessions.",
                        data = """{"thisWeekVolume": $thisWeekVolume, "lastWeekVolume": $lastWeekVolume, "change": $volumeChange}""",
                        priority = InsightPriority.LOW,
                        generatedDate = now
                    ))
                }
                volumeChange >= 10f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.VOLUME_ANALYSIS,
                        title = "💪 Good Volume Progression",
                        message = "Solid ${String.format("%.1f", volumeChange)}% volume increase this week. This progressive overload should drive adaptation!",
                        data = """{"thisWeekVolume": $thisWeekVolume, "lastWeekVolume": $lastWeekVolume, "change": $volumeChange}""",
                        priority = InsightPriority.LOW,
                        generatedDate = now
                    ))
                }
            }
        }
        
        return insights
    }
    
    /**
     * Generate autoregulation insights based on RPE data
     */
    private suspend fun generateAutoregulationInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        val oneWeekAgo = now.minusWeeks(1)
        
        // Skip if we already generated a similar insight recently
        val similarCount = progressInsightDao.getSimilarInsightCount(
            userId, InsightType.AUTOREGULATION_FEEDBACK, null, duplicateCheckTime
        )
        if (similarCount > 0) return insights
        
        // Get recent sets with RPE data
        val allWorkouts = workoutDao.getAllWorkouts()
        val recentWorkouts = allWorkouts.filter { 
            it.status == WorkoutStatus.COMPLETED && it.date.isAfter(oneWeekAgo) 
        }
        
        val allRecentSets = mutableListOf<SetLog>()
        for (workout in recentWorkouts) {
            val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
            for (exerciseLog in exerciseLogs) {
                val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
                    .filter { it.isCompleted && it.rpe != null }
                allRecentSets.addAll(sets)
            }
        }
        
        if (allRecentSets.isNotEmpty()) {
            val averageRPE = allRecentSets.mapNotNull { it.rpe }.average().toFloat()
            val rpeVariance = allRecentSets.mapNotNull { it.rpe }
                .map { (it - averageRPE) * (it - averageRPE) }
                .average().toFloat()
            
            when {
                averageRPE <= 6.5f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.AUTOREGULATION_FEEDBACK,
                        title = "🎯 Conservative RPE Management",
                        message = "Your average RPE this week was ${String.format("%.1f", averageRPE)}. You might have room for slightly more intensity to maximize adaptations.",
                        data = """{"averageRPE": $averageRPE, "variance": $rpeVariance, "setCount": ${allRecentSets.size}}""",
                        priority = InsightPriority.LOW,
                        generatedDate = now,
                        isActionable = true,
                        actionType = "increase_intensity"
                    ))
                }
                averageRPE >= 8.5f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.AUTOREGULATION_FEEDBACK,
                        title = "🔥 High Intensity Training",
                        message = "Your average RPE this week was ${String.format("%.1f", averageRPE)}. This high intensity is great for strength but monitor fatigue closely.",
                        data = """{"averageRPE": $averageRPE, "variance": $rpeVariance, "setCount": ${allRecentSets.size}}""",
                        priority = InsightPriority.MEDIUM,
                        generatedDate = now,
                        isActionable = true,
                        actionType = "monitor_fatigue"
                    ))
                }
                rpeVariance <= 0.5f && averageRPE in 7.0f..8.0f -> {
                    insights.add(ProgressInsight(
                        userId = userId,
                        insightType = InsightType.AUTOREGULATION_FEEDBACK,
                        title = "🎖️ Excellent RPE Consistency",
                        message = "Your RPE management has been very consistent this week (avg ${String.format("%.1f", averageRPE)}). This shows good self-awareness and training control!",
                        data = """{"averageRPE": $averageRPE, "variance": $rpeVariance, "setCount": ${allRecentSets.size}}""",
                        priority = InsightPriority.LOW,
                        generatedDate = now
                    ))
                }
            }
        }
        
        return insights
    }
    
    /**
     * Generate recovery insights based on performance patterns
     */
    private suspend fun generateRecoveryInsights(
        userId: Long, 
        duplicateCheckTime: LocalDateTime
    ): List<ProgressInsight> {
        val insights = mutableListOf<ProgressInsight>()
        val now = LocalDateTime.now()
        val twoWeeksAgo = now.minusWeeks(2)
        
        // Skip if we already generated a similar insight recently
        val similarCount = progressInsightDao.getSimilarInsightCount(
            userId, InsightType.RECOVERY_INSIGHT, null, duplicateCheckTime
        )
        if (similarCount > 0) return insights
        
        // Analyze workout spacing and performance
        val allWorkouts = workoutDao.getAllWorkouts()
        val recentWorkouts = allWorkouts.filter { 
            it.status == WorkoutStatus.COMPLETED && it.date.isAfter(twoWeeksAgo) 
        }.sortedBy { it.date }
        
        if (recentWorkouts.size >= 4) {
            // Check for back-to-back workout performance drops
            var backToBackDeclines = 0
            
            for (i in 1 until recentWorkouts.size) {
                val currentWorkout = recentWorkouts[i]
                val previousWorkout = recentWorkouts[i-1]
                
                val daysBetween = ChronoUnit.DAYS.between(previousWorkout.date, currentWorkout.date)
                
                if (daysBetween <= 1) { // Back-to-back or same day
                    // Simple heuristic: check if RPE was notably higher
                    val currentSets = getWorkoutSets(currentWorkout.id)
                    val previousSets = getWorkoutSets(previousWorkout.id)
                    
                    val currentAvgRPE = currentSets.mapNotNull { it.rpe }.average()
                    val previousAvgRPE = previousSets.mapNotNull { it.rpe }.average()
                    
                    if (!currentAvgRPE.isNaN() && !previousAvgRPE.isNaN() && 
                        currentAvgRPE > previousAvgRPE + 0.5) {
                        backToBackDeclines++
                    }
                }
            }
            
            if (backToBackDeclines >= 2) {
                insights.add(ProgressInsight(
                    userId = userId,
                    insightType = InsightType.RECOVERY_INSIGHT,
                    title = "😴 Recovery Pattern Detected",
                    message = "Your performance tends to decline on back-to-back training days. Consider adding more rest days or adjusting training split.",
                    data = """{"backToBackDeclines": $backToBackDeclines, "totalWorkouts": ${recentWorkouts.size}}""",
                    priority = InsightPriority.MEDIUM,
                    generatedDate = now,
                    isActionable = true,
                    actionType = "adjust_schedule"
                ))
            }
        }
        
        return insights
    }
    
    /**
     * Helper to get all sets for a workout
     */
    private suspend fun getWorkoutSets(workoutId: Long): List<SetLog> {
        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)
        return exerciseLogs.flatMap { exerciseLog ->
            setLogDao.getSetLogsForExercise(exerciseLog.id).filter { it.isCompleted }
        }
    }
    
    /**
     * Cleanup old insights to prevent database bloat
     */
    suspend fun cleanupOldInsights(beforeDate: LocalDateTime = LocalDateTime.now().minusMonths(3)) {
        progressInsightDao.deleteOldInsights(beforeDate)
    }
}