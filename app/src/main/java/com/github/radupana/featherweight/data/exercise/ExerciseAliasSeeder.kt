package com.github.radupana.featherweight.data.exercise

class ExerciseAliasSeeder(private val exerciseDao: ExerciseDao) {
    
    suspend fun seedExerciseAliases() {
        // Map of exercise name to list of aliases
        val exerciseAliases = mapOf(
            // Squats
            "Back Squat" to listOf("Squat", "Barbell Squat", "Barbell Back Squat", "High Bar Squat", "Low Bar Squat", "Pause Squat"),
            "Front Squat" to listOf("Barbell Front Squat"),
            "Goblet Squat" to listOf("KB Goblet Squat", "Kettlebell Goblet Squat"),
            
            // Deadlifts
            "Conventional Deadlift" to listOf("Deadlift", "Barbell Deadlift", "Standard Deadlift"),
            "Romanian Deadlift" to listOf("RDL", "Stiff Leg Deadlift", "Stiff-Leg Deadlift"),
            "Sumo Deadlift" to listOf("Wide Stance Deadlift"),
            
            // Bench Press
            "Bench Press" to listOf("Barbell Bench Press", "Flat Bench Press", "Barbell Bench"),
            "Incline Bench Press" to listOf("Incline Barbell Press", "Incline Press"),
            "Decline Bench Press" to listOf("Decline Barbell Press", "Decline Press"),
            "Close-Grip Bench Press" to listOf("CGBP", "Close Grip Bench", "Tricep Bench Press"),
            "Dumbbell Bench Press" to listOf("DB Bench Press", "Dumbbell Bench"),
            "Incline Dumbbell Press" to listOf("Incline DB Press", "Incline Dumbbell Bench Press", "Incline DB Bench Press"),
            
            // Overhead Press
            "Overhead Press" to listOf("OHP", "Military Press", "Shoulder Press", "Standing Press", "Barbell Overhead Press"),
            "Push Press" to listOf("Barbell Push Press"),
            "Seated Dumbbell Press" to listOf("Seated Shoulder Press", "Seated DB Shoulder Press"),
            "Dumbbell Shoulder Press" to listOf("DB Press", "Standing DB Press", "Dumbbell Press"),
            "Machine Shoulder Press" to listOf("Machine Press", "Shoulder Press Machine"),
            
            // Rows
            "Barbell Row" to listOf("Row", "Bent Over Row", "Bent-Over Row", "Pendlay Row", "BB Row"),
            "Cable Row" to listOf("Seated Cable Row", "Low Row", "Seated Row"),
            "Dumbbell Row" to listOf("DB Row", "One Arm Row", "Single Arm Row", "1-Arm Row"),
            "T-Bar Row" to listOf("T Bar Row", "Landmine Row"),
            
            // Pull-ups/Chin-ups
            "Pull-up" to listOf("Pullup", "Pull Up", "Wide Grip Pull-up", "Weighted Pull-up", "Weighted Pullup"),
            "Chin-up" to listOf("Chinup", "Chin Up", "Underhand Pull-up", "Weighted Chins", "Weighted Chin-up", "Weighted Chinup"),
            "Lat Pulldown" to listOf("Pulldown", "Wide Grip Pulldown", "Cable Pulldown"),
            
            // Dips
            "Dip" to listOf("Weighted Dip", "Weighted Dips", "Body Weight Dip", "Bodyweight Dip", "Parallel Bar Dip", "Chest Dip"),
            
            // Curls
            "Barbell Curl" to listOf("BB Curl", "Standing Barbell Curl", "Barbell Bicep Curl"),
            "Dumbbell Curl" to listOf("DB Curl", "Dumbbell Bicep Curl", "DB Bicep Curl"),
            "Hammer Curl" to listOf("DB Hammer Curl", "Dumbbell Hammer Curl"),
            "Preacher Curl" to listOf("EZ Bar Preacher Curl", "Barbell Preacher Curl"),
            
            // Tricep
            "Tricep Extension" to listOf("Overhead Tricep Extension", "French Press", "Skull Crusher"),
            "Cable Tricep Extension" to listOf("Tricep Pushdown", "Cable Pushdown", "Rope Pushdown"),
            
            // Legs
            "Leg Press" to listOf("Machine Leg Press", "45 Degree Leg Press", "Leg Press Machine"),
            "Leg Curl" to listOf("Hamstring Curl", "Lying Leg Curl", "Seated Leg Curl"),
            "Leg Extension" to listOf("Quad Extension", "Machine Leg Extension"),
            "Walking Lunge" to listOf("Lunges", "Lunge", "Forward Lunge", "Walking Lunges", "Reverse Lunges", "Reverse Lunge"),
            "Bulgarian Split Squat" to listOf("BSS", "Rear Foot Elevated Split Squat", "Split Squat"),
            "Calf Raise" to listOf("Standing Calf Raise", "Calf Raises"),
            
            // Core
            "Plank" to listOf("Front Plank", "Standard Plank"),
            "Side Plank" to listOf("Lateral Plank"),
            "Ab Wheel" to listOf("Ab Rollout", "Ab Wheel Rollout"),
            "Hanging Knee Raise" to listOf("Knee Raise", "Hanging Leg Raise"),
            
            // Olympic
            "Power Clean" to listOf("Clean", "Hang Power Clean"),
            "Clean and Jerk" to listOf("C&J", "Clean & Jerk"),
            
            // Other compound movements
            "Hip Thrust" to listOf("Barbell Hip Thrust", "Glute Bridge", "BB Hip Thrust"),
            "Good Morning" to listOf("Barbell Good Morning", "GM"),
            "Face Pull" to listOf("Cable Face Pull", "Rear Delt Pull", "Face Pull (Cable or Band)", "Band Face Pull"),
            "Shrug" to listOf("Barbell Shrug", "Trap Shrug", "Shoulder Shrug"),
            
            // Machine exercises
            "Chest Fly" to listOf("Pec Fly", "Machine Fly", "Dumbbell Fly", "DB Fly"),
            "Cable Fly" to listOf("Cable Crossover", "Pec Crossover"),
            "Lateral Raise" to listOf("Side Raise", "Shoulder Raise", "DB Lateral Raise", "Dumbbell Lateral Raise"),
            "Front Raise" to listOf("DB Front Raise", "Shoulder Front Raise"),
            "Rear Delt Fly" to listOf("Reverse Fly", "Rear Fly", "Bent Over Fly")
        )
        
        // Create aliases for each exercise
        for ((exerciseName, aliases) in exerciseAliases) {
            val exercise = exerciseDao.findExerciseByExactName(exerciseName)
            if (exercise != null) {
                val aliasEntities = aliases.map { alias ->
                    ExerciseAlias(
                        exerciseId = exercise.id,
                        alias = alias,
                        confidence = 1.0f,
                        exactMatchOnly = false,
                        source = "common"
                    )
                }
                try {
                    exerciseDao.insertAliases(aliasEntities)
                } catch (e: Exception) {
                    // Ignore duplicate key exceptions
                    println("Failed to insert aliases for $exerciseName: ${e.message}")
                }
            }
        }
    }
}