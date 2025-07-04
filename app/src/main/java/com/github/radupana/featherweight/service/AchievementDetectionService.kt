package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.data.achievement.*
import com.github.radupana.featherweight.data.profile.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Service responsible for detecting when users unlock achievements
 */
class AchievementDetectionService(
    private val database: FeatherweightDatabase
) {
    
    private val userAchievementDao = database.userAchievementDao()
    private val workoutDao = database.workoutDao()
    private val setLogDao = database.setLogDao()
    private val profileDao = database.profileDao()
    private val globalProgressDao = database.globalExerciseProgressDao()
    
    /**
     * Check for newly unlocked achievements after a workout completion
     */
    suspend fun checkForNewAchievements(userId: Long, workoutId: Long): List<UserAchievement> = withContext(Dispatchers.IO) {
        val newAchievements = mutableListOf<UserAchievement>()
        
        // Get currently unlocked achievement IDs to avoid duplicates
        val unlockedIds = userAchievementDao.getUnlockedAchievementIds(userId)
        
        // Check each achievement category
        newAchievements.addAll(checkStrengthMilestones(userId, workoutId, unlockedIds))
        newAchievements.addAll(checkConsistencyStreaks(userId, unlockedIds))
        newAchievements.addAll(checkVolumeRecords(userId, workoutId, unlockedIds))
        newAchievements.addAll(checkProgressMedals(userId, unlockedIds))
        
        // Save new achievements to database
        newAchievements.forEach { achievement ->
            userAchievementDao.insertUserAchievement(achievement)
            println("🏆 Achievement Unlocked: ${achievement.achievementId}")
        }
        
        newAchievements
    }
    
    private suspend fun checkStrengthMilestones(
        userId: Long, 
        workoutId: Long, 
        unlockedIds: List<String>
    ): List<UserAchievement> {
        val newAchievements = mutableListOf<UserAchievement>()
        val userProfile = profileDao.getUserProfile(userId)
        
        // Get all completed sets from this workout
        val exerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)
        val allSets = exerciseLogs.flatMap { 
            database.setLogDao().getSetLogsForExercise(it.id)
        }.filter { it.isCompleted }
        
        for (achievement in AchievementDefinitions.STRENGTH_MILESTONES) {
            if (achievement.id in unlockedIds) continue
            
            when (val condition = achievement.condition) {
                is AchievementCondition.WeightThreshold -> {
                    val exerciseSets = allSets.filter { set ->
                        val exerciseLog = exerciseLogs.find { it.id == set.exerciseLogId }
                        exerciseLog?.exerciseName == condition.exerciseName
                    }
                    
                    val maxWeight = exerciseSets.maxOfOrNull { it.actualWeight } ?: 0f
                    if (maxWeight >= condition.weightKg) {
                        val contextData = """{"weight": $maxWeight, "exercise": "${condition.exerciseName}"}"""
                        newAchievements.add(
                            UserAchievement(
                                userId = userId,
                                achievementId = achievement.id,
                                unlockedDate = LocalDateTime.now(),
                                data = contextData
                            )
                        )
                    }
                }
                
                is AchievementCondition.BodyweightMultiple -> {
                    // Skip bodyweight-based achievements for now since UserProfile doesn't have bodyWeight field yet
                    // TODO: Add bodyWeight field to UserProfile and implement this
                    continue
                }
                else -> { /* Other conditions handled in different methods */ }
            }
        }
        
        return newAchievements
    }
    
    private suspend fun checkConsistencyStreaks(
        userId: Long,
        unlockedIds: List<String>
    ): List<UserAchievement> {
        val newAchievements = mutableListOf<UserAchievement>()
        
        // Calculate current workout streak
        val currentStreak = calculateWorkoutStreak(userId)
        
        for (achievement in AchievementDefinitions.CONSISTENCY_STREAKS) {
            if (achievement.id in unlockedIds) continue
            
            when (val condition = achievement.condition) {
                is AchievementCondition.WorkoutStreak -> {
                    if (currentStreak >= condition.days) {
                        val contextData = """{"streak": $currentStreak, "target": ${condition.days}}"""
                        newAchievements.add(
                            UserAchievement(
                                userId = userId,
                                achievementId = achievement.id,
                                unlockedDate = LocalDateTime.now(),
                                data = contextData
                            )
                        )
                    }
                }
                else -> { /* Other conditions */ }
            }
        }
        
        return newAchievements
    }
    
    private suspend fun checkVolumeRecords(
        userId: Long,
        workoutId: Long,
        unlockedIds: List<String>
    ): List<UserAchievement> {
        val newAchievements = mutableListOf<UserAchievement>()
        
        // Calculate total workout volume
        val exerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkout(workoutId)
        val allSets = exerciseLogs.flatMap { 
            database.setLogDao().getSetLogsForExercise(it.id)
        }.filter { it.isCompleted }
        
        val totalVolume = allSets.sumOf { (it.actualWeight * it.actualReps).toDouble() }.toFloat()
        val maxReps = allSets.maxOfOrNull { it.actualReps } ?: 0
        
        for (achievement in AchievementDefinitions.VOLUME_RECORDS) {
            if (achievement.id in unlockedIds) continue
            
            when (val condition = achievement.condition) {
                is AchievementCondition.SingleWorkoutVolume -> {
                    if (totalVolume >= condition.totalKg) {
                        val contextData = """{"volume": $totalVolume, "target": ${condition.totalKg}}"""
                        newAchievements.add(
                            UserAchievement(
                                userId = userId,
                                achievementId = achievement.id,
                                unlockedDate = LocalDateTime.now(),
                                data = contextData
                            )
                        )
                    }
                }
                
                is AchievementCondition.SingleSetReps -> {
                    if (maxReps >= condition.reps) {
                        val contextData = """{"reps": $maxReps, "target": ${condition.reps}}"""
                        newAchievements.add(
                            UserAchievement(
                                userId = userId,
                                achievementId = achievement.id,
                                unlockedDate = LocalDateTime.now(),
                                data = contextData
                            )
                        )
                    }
                }
                else -> { /* Other conditions */ }
            }
        }
        
        return newAchievements
    }
    
    private suspend fun checkProgressMedals(
        userId: Long,
        unlockedIds: List<String>
    ): List<UserAchievement> {
        val newAchievements = mutableListOf<UserAchievement>()
        
        for (achievement in AchievementDefinitions.PROGRESS_MEDALS) {
            if (achievement.id in unlockedIds) continue
            
            when (val condition = achievement.condition) {
                is AchievementCondition.StrengthGainPercentage -> {
                    // Check all exercises for strength gains
                    val allProgress = globalProgressDao.getAllProgress(userId)
                    
                    for (progress in allProgress) {
                        val daysAgo = LocalDateTime.now().minusDays(condition.timeframeDays.toLong())
                        
                        // For simplicity, use last progression date as baseline
                        val improvement = if (progress.lastProgressionDate != null && 
                                             progress.lastProgressionDate.isAfter(daysAgo) &&
                                             progress.estimatedMax > 0) {
                            
                            // Estimate improvement based on working weight progression
                            val estimatedBaselineWeight = progress.currentWorkingWeight * 0.8f // Rough estimate
                            if (estimatedBaselineWeight > 0) {
                                ((progress.currentWorkingWeight - estimatedBaselineWeight) / estimatedBaselineWeight) * 100
                            } else 0f
                        } else 0f
                        
                        if (improvement >= condition.percentageGain) {
                            val contextData = """{"exercise": "${progress.exerciseName}", "improvement": $improvement, "target": ${condition.percentageGain}}"""
                            newAchievements.add(
                                UserAchievement(
                                    userId = userId,
                                    achievementId = achievement.id,
                                    unlockedDate = LocalDateTime.now(),
                                    data = contextData
                                )
                            )
                            break // Only unlock once per achievement
                        }
                    }
                }
                
                is AchievementCondition.PlateauBreaker -> {
                    // Check for breaking through plateaus
                    val allProgress = globalProgressDao.getAllProgress(userId)
                    
                    for (progress in allProgress) {
                        if (progress.consecutiveStalls >= condition.stallsRequired && 
                            progress.trend == ProgressTrend.IMPROVING) {
                            
                            val contextData = """{"exercise": "${progress.exerciseName}", "stalls": ${progress.consecutiveStalls}}"""
                            newAchievements.add(
                                UserAchievement(
                                    userId = userId,
                                    achievementId = achievement.id,
                                    unlockedDate = LocalDateTime.now(),
                                    data = contextData
                                )
                            )
                            break // Only unlock once per achievement
                        }
                    }
                }
                else -> { /* Other conditions */ }
            }
        }
        
        return newAchievements
    }
    
    private suspend fun calculateWorkoutStreak(userId: Long): Int {
        // Get all workouts and filter for completed ones
        val allWorkouts = workoutDao.getAllWorkouts()
        val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
        
        if (completedWorkouts.isEmpty()) return 0
        
        // Sort by date descending (most recent first)
        val sortedWorkouts = completedWorkouts.sortedByDescending { it.date }
        
        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()
        
        for (workout in sortedWorkouts) {
            val workoutDate = workout.date.toLocalDate()
            val daysDiff = ChronoUnit.DAYS.between(workoutDate, currentDate)
            
            when {
                daysDiff <= 1 -> {
                    // Workout is today or yesterday, continue streak
                    streak++
                    currentDate = workoutDate
                }
                daysDiff > 1 -> {
                    // Gap in workouts, streak is broken
                    break
                }
            }
        }
        
        return streak
    }
    
    /**
     * Get summary of user's achievement progress
     */
    suspend fun getAchievementSummary(userId: Long): AchievementSummary = withContext(Dispatchers.IO) {
        val unlockedIds = userAchievementDao.getUnlockedAchievementIds(userId)
        val recentAchievements = userAchievementDao.getRecentUserAchievements(userId, 5)
        
        val totalAchievements = AchievementDefinitions.ALL_ACHIEVEMENTS.size
        val unlockedCount = unlockedIds.size
        
        val progressByCategory = AchievementCategory.entries.associateWith { category ->
            val categoryAchievements = AchievementDefinitions.getAchievementsByCategory(category)
            val unlockedInCategory = categoryAchievements.count { it.id in unlockedIds }
            AchievementCategoryProgress(
                category = category,
                total = categoryAchievements.size,
                unlocked = unlockedInCategory,
                progress = if (categoryAchievements.isNotEmpty()) unlockedInCategory.toFloat() / categoryAchievements.size else 0f
            )
        }
        
        AchievementSummary(
            totalAchievements = totalAchievements,
            unlockedCount = unlockedCount,
            progress = unlockedCount.toFloat() / totalAchievements,
            recentUnlocks = recentAchievements,
            categoryProgress = progressByCategory
        )
    }
}

data class AchievementSummary(
    val totalAchievements: Int,
    val unlockedCount: Int,
    val progress: Float,
    val recentUnlocks: List<UserAchievement>,
    val categoryProgress: Map<AchievementCategory, AchievementCategoryProgress>
)

data class AchievementCategoryProgress(
    val category: AchievementCategory,
    val total: Int,
    val unlocked: Int,
    val progress: Float
)