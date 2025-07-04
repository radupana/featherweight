package com.github.radupana.featherweight.data.achievement

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents an achievement that can be unlocked by users
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: AchievementCategory,
    val condition: AchievementCondition,
    val difficulty: AchievementDifficulty = AchievementDifficulty.MEDIUM
)

/**
 * User's unlocked achievement record
 */
@Entity(tableName = "user_achievements")
data class UserAchievement(
    @PrimaryKey val id: Long = 0,
    val userId: Long,
    val achievementId: String,
    val unlockedDate: LocalDateTime,
    val data: String? = null // JSON context data (e.g., weight achieved, streak count)
)

enum class AchievementCategory {
    STRENGTH_MILESTONES,
    CONSISTENCY_STREAKS,
    VOLUME_RECORDS,
    PROGRESS_MEDALS
}

enum class AchievementDifficulty {
    EASY, MEDIUM, HARD, LEGENDARY
}

/**
 * Defines the condition for unlocking an achievement
 */
sealed class AchievementCondition {
    data class WeightThreshold(
        val exerciseName: String,
        val weightKg: Float
    ) : AchievementCondition()
    
    data class BodyweightMultiple(
        val exerciseName: String,
        val multiplier: Float
    ) : AchievementCondition()
    
    data class WorkoutStreak(
        val days: Int
    ) : AchievementCondition()
    
    data class SingleWorkoutVolume(
        val totalKg: Float
    ) : AchievementCondition()
    
    data class SingleSetReps(
        val reps: Int
    ) : AchievementCondition()
    
    data class StrengthGainPercentage(
        val exerciseName: String,
        val percentageGain: Float,
        val timeframeDays: Int
    ) : AchievementCondition()
    
    data class PlateauBreaker(
        val exerciseName: String,
        val stallsRequired: Int
    ) : AchievementCondition()
}