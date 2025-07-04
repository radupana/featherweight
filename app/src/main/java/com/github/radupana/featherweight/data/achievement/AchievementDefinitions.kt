package com.github.radupana.featherweight.data.achievement

/**
 * Static definitions of all available achievements in the app
 */
object AchievementDefinitions {
    
    /**
     * Strength Milestone Achievements
     */
    val STRENGTH_MILESTONES = listOf(
        // Squat Milestones
        Achievement(
            id = "squat_100kg",
            title = "Century Squatter",
            description = "Squat 100kg for the first time",
            icon = "🏋️",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Back Squat", 100f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "squat_150kg",
            title = "Squat Warrior",
            description = "Squat 150kg",
            icon = "💪",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Back Squat", 150f),
            difficulty = AchievementDifficulty.HARD
        ),
        Achievement(
            id = "squat_200kg",
            title = "Squat Titan",
            description = "Squat 200kg",
            icon = "⚡",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Back Squat", 200f),
            difficulty = AchievementDifficulty.LEGENDARY
        ),
        
        // Deadlift Milestones
        Achievement(
            id = "deadlift_150kg",
            title = "Deadlift Novice",
            description = "Deadlift 150kg",
            icon = "🔥",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Deadlift", 150f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "deadlift_200kg",
            title = "Deadlift Beast",
            description = "Deadlift 200kg",
            icon = "🦾",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Deadlift", 200f),
            difficulty = AchievementDifficulty.HARD
        ),
        Achievement(
            id = "deadlift_2x_bodyweight",
            title = "Double Bodyweight Puller",
            description = "Deadlift 2x your bodyweight",
            icon = "💀",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.BodyweightMultiple("Barbell Deadlift", 2.0f),
            difficulty = AchievementDifficulty.HARD
        ),
        
        // Bench Press Milestones
        Achievement(
            id = "bench_80kg",
            title = "Bench Beginner",
            description = "Bench press 80kg",
            icon = "🏃",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Bench Press", 80f),
            difficulty = AchievementDifficulty.EASY
        ),
        Achievement(
            id = "bench_100kg",
            title = "Century Presser",
            description = "Bench press 100kg",
            icon = "💯",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Bench Press", 100f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "bench_bodyweight",
            title = "Bodyweight Presser",
            description = "Bench press your bodyweight",
            icon = "⚖️",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.BodyweightMultiple("Barbell Bench Press", 1.0f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        
        // Overhead Press Milestones
        Achievement(
            id = "ohp_60kg",
            title = "Overhead Novice",
            description = "Overhead press 60kg",
            icon = "🙌",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Overhead Press", 60f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "ohp_80kg",
            title = "Press Master",
            description = "Overhead press 80kg",
            icon = "👑",
            category = AchievementCategory.STRENGTH_MILESTONES,
            condition = AchievementCondition.WeightThreshold("Barbell Overhead Press", 80f),
            difficulty = AchievementDifficulty.HARD
        )
    )
    
    /**
     * Consistency Streak Achievements
     */
    val CONSISTENCY_STREAKS = listOf(
        Achievement(
            id = "streak_7_days",
            title = "Week Warrior",
            description = "Complete workouts for 7 consecutive days",
            icon = "🔥",
            category = AchievementCategory.CONSISTENCY_STREAKS,
            condition = AchievementCondition.WorkoutStreak(7),
            difficulty = AchievementDifficulty.EASY
        ),
        Achievement(
            id = "streak_14_days",
            title = "Fortnight Fighter",
            description = "Complete workouts for 14 consecutive days",
            icon = "🔥",
            category = AchievementCategory.CONSISTENCY_STREAKS,
            condition = AchievementCondition.WorkoutStreak(14),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "streak_30_days",
            title = "Monthly Machine",
            description = "Complete workouts for 30 consecutive days",
            icon = "🔥",
            category = AchievementCategory.CONSISTENCY_STREAKS,
            condition = AchievementCondition.WorkoutStreak(30),
            difficulty = AchievementDifficulty.HARD
        ),
        Achievement(
            id = "streak_90_days",
            title = "Iron Discipline",
            description = "Complete workouts for 90 consecutive days",
            icon = "🔥",
            category = AchievementCategory.CONSISTENCY_STREAKS,
            condition = AchievementCondition.WorkoutStreak(90),
            difficulty = AchievementDifficulty.LEGENDARY
        )
    )
    
    /**
     * Volume Record Achievements
     */
    val VOLUME_RECORDS = listOf(
        Achievement(
            id = "volume_5000kg",
            title = "5-Ton Club",
            description = "Lift 5,000kg total weight in a single workout",
            icon = "🏗️",
            category = AchievementCategory.VOLUME_RECORDS,
            condition = AchievementCondition.SingleWorkoutVolume(5000f),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "volume_10000kg",
            title = "10-Ton Club",
            description = "Lift 10,000kg total weight in a single workout",
            icon = "🚛",
            category = AchievementCategory.VOLUME_RECORDS,
            condition = AchievementCondition.SingleWorkoutVolume(10000f),
            difficulty = AchievementDifficulty.HARD
        ),
        Achievement(
            id = "century_set",
            title = "Century Set",
            description = "Complete a set with 100+ repetitions",
            icon = "💯",
            category = AchievementCategory.VOLUME_RECORDS,
            condition = AchievementCondition.SingleSetReps(100),
            difficulty = AchievementDifficulty.HARD
        )
    )
    
    /**
     * Progress Medal Achievements
     */
    val PROGRESS_MEDALS = listOf(
        Achievement(
            id = "strength_gain_25_percent",
            title = "Quarter Century Gainer",
            description = "Increase any exercise by 25% in 90 days",
            icon = "📈",
            category = AchievementCategory.PROGRESS_MEDALS,
            condition = AchievementCondition.StrengthGainPercentage("", 25f, 90),
            difficulty = AchievementDifficulty.MEDIUM
        ),
        Achievement(
            id = "strength_gain_50_percent",
            title = "Half Century Hero",
            description = "Increase any exercise by 50% in 90 days",
            icon = "🚀",
            category = AchievementCategory.PROGRESS_MEDALS,
            condition = AchievementCondition.StrengthGainPercentage("", 50f, 90),
            difficulty = AchievementDifficulty.HARD
        ),
        Achievement(
            id = "plateau_breaker",
            title = "Plateau Breaker",
            description = "Break through a 3+ workout plateau",
            icon = "💪",
            category = AchievementCategory.PROGRESS_MEDALS,
            condition = AchievementCondition.PlateauBreaker("", 3),
            difficulty = AchievementDifficulty.MEDIUM
        )
    )
    
    /**
     * All achievements combined
     */
    val ALL_ACHIEVEMENTS = STRENGTH_MILESTONES + CONSISTENCY_STREAKS + VOLUME_RECORDS + PROGRESS_MEDALS
    
    /**
     * Get achievement by ID
     */
    fun getAchievementById(id: String): Achievement? {
        return ALL_ACHIEVEMENTS.find { it.id == id }
    }
    
    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<Achievement> {
        return ALL_ACHIEVEMENTS.filter { it.category == category }
    }
}