package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*

object MockProgrammeGenerator {
    
    fun generateMockProgramme(
        goal: ProgrammeGoal?,
        frequency: Int?,
        duration: SessionDuration?,
        inputText: String
    ): AIProgrammeResponse {
        
        // Create a realistic mock programme based on input
        val programme = when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> generateStrengthProgramme(frequency ?: 3, duration)
            ProgrammeGoal.BUILD_MUSCLE -> generateMuscleBuildingProgramme(frequency ?: 4, duration)
            ProgrammeGoal.LOSE_FAT -> generateFatLossProgramme(frequency ?: 4, duration)
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> generateAthleticProgramme(frequency ?: 4, duration)
            ProgrammeGoal.CUSTOM -> generateGeneralFitnessProgramme(frequency ?: 3, duration)
            null -> generateGeneralFitnessProgramme(frequency ?: 3, duration)
        }
        
        return AIProgrammeResponse(
            success = true,
            programme = programme,
            error = null
        )
    }
    
    private fun generateStrengthProgramme(frequency: Int, duration: SessionDuration?): GeneratedProgramme {
        val sessionMinutes = when (duration) {
            SessionDuration.QUICK -> 45
            SessionDuration.STANDARD -> 60
            SessionDuration.EXTENDED -> 75
            SessionDuration.LONG -> 90
            null -> 60
        }
        
        return GeneratedProgramme(
            name = "Strength Builder ${frequency}x/Week",
            description = "A focused strength programme emphasizing compound movements and progressive overload. Perfect for building raw strength in the big 3 lifts.",
            durationWeeks = 8,
            daysPerWeek = frequency,
            workouts = when (frequency) {
                3 -> generateThreeDayStrengthWorkouts(sessionMinutes)
                4 -> generateFourDayStrengthWorkouts(sessionMinutes)
                5 -> generateFiveDayStrengthWorkouts(sessionMinutes)
                else -> generateThreeDayStrengthWorkouts(sessionMinutes)
            }
        )
    }
    
    private fun generateMuscleBuildingProgramme(frequency: Int, duration: SessionDuration?): GeneratedProgramme {
        val sessionMinutes = when (duration) {
            SessionDuration.QUICK -> 50
            SessionDuration.STANDARD -> 70
            SessionDuration.EXTENDED -> 85
            SessionDuration.LONG -> 100
            null -> 70
        }
        
        return GeneratedProgramme(
            name = "Muscle Builder ${frequency}x/Week",
            description = "A hypertrophy-focused programme with optimal volume distribution across muscle groups. Combines compound and isolation work for maximum muscle growth.",
            durationWeeks = 12,
            daysPerWeek = frequency,
            workouts = when (frequency) {
                4 -> generateFourDayHypertrophyWorkouts(sessionMinutes)
                5 -> generateFiveDayHypertrophyWorkouts(sessionMinutes)
                6 -> generateSixDayHypertrophyWorkouts(sessionMinutes)
                else -> generateFourDayHypertrophyWorkouts(sessionMinutes)
            }
        )
    }
    
    private fun generateFatLossProgramme(frequency: Int, duration: SessionDuration?): GeneratedProgramme {
        val sessionMinutes = when (duration) {
            SessionDuration.QUICK -> 40
            SessionDuration.STANDARD -> 55
            SessionDuration.EXTENDED -> 70
            SessionDuration.LONG -> 85
            null -> 55
        }
        
        return GeneratedProgramme(
            name = "Fat Loss Circuit ${frequency}x/Week",
            description = "High-intensity programme combining strength training with metabolic circuits. Designed to preserve muscle while maximizing calorie burn.",
            durationWeeks = 8,
            daysPerWeek = frequency,
            workouts = generateFatLossWorkouts(frequency, sessionMinutes)
        )
    }
    
    private fun generateAthleticProgramme(frequency: Int, duration: SessionDuration?): GeneratedProgramme {
        val sessionMinutes = when (duration) {
            SessionDuration.QUICK -> 50
            SessionDuration.STANDARD -> 65
            SessionDuration.EXTENDED -> 80
            SessionDuration.LONG -> 95
            null -> 65
        }
        
        return GeneratedProgramme(
            name = "Athletic Performance ${frequency}x/Week",
            description = "Sport-specific training focusing on power, agility, and functional movement patterns. Includes plyometrics and movement quality work.",
            durationWeeks = 10,
            daysPerWeek = frequency,
            workouts = generateAthleticWorkouts(frequency, sessionMinutes)
        )
    }
    
    private fun generateGeneralFitnessProgramme(frequency: Int, duration: SessionDuration?): GeneratedProgramme {
        val sessionMinutes = when (duration) {
            SessionDuration.QUICK -> 45
            SessionDuration.STANDARD -> 60
            SessionDuration.EXTENDED -> 75
            SessionDuration.LONG -> 90
            null -> 60
        }
        
        return GeneratedProgramme(
            name = "General Fitness ${frequency}x/Week",
            description = "A balanced programme combining strength, cardio, and mobility work. Perfect for overall health and fitness improvement.",
            durationWeeks = 10,
            daysPerWeek = frequency,
            workouts = generateGeneralFitnessWorkouts(frequency, sessionMinutes)
        )
    }
    
    private fun generateThreeDayStrengthWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        return listOf(
            GeneratedWorkout(
                dayNumber = 1,
                name = "Squat Focus",
                exercises = listOf(
                    GeneratedExercise("Barbell Back Squat", 4, 3, 5, 8.5f, 180, "Focus on depth and control"),
                    GeneratedExercise("Romanian Deadlift", 3, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Bulgarian Split Squat", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Barbell Row", 3, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Plank", 3, 30, 45, null, 60, "Hold for time")
                )
            ),
            GeneratedWorkout(
                dayNumber = 2,
                name = "Bench Focus",
                exercises = listOf(
                    GeneratedExercise("Barbell Bench Press", 4, 3, 5, 8.5f, 180, "Pause on chest"),
                    GeneratedExercise("Overhead Press", 3, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Incline Dumbbell Press", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Pull-up", 3, 5, 8, 7.5f, 120, "Use assistance if needed"),
                    GeneratedExercise("Tricep Dip", 3, 8, 12, 7.0f, 60, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 3,
                name = "Deadlift Focus",
                exercises = listOf(
                    GeneratedExercise("Conventional Deadlift", 4, 3, 5, 8.5f, 180, "Focus on hip hinge"),
                    GeneratedExercise("Front Squat", 3, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Barbell Hip Thrust", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Lat Pulldown", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Face Pull", 3, 12, 15, 6.5f, 60, "Focus on rear delts")
                )
            )
        )
    }
    
    private fun generateFourDayStrengthWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        return listOf(
            GeneratedWorkout(
                dayNumber = 1,
                name = "Upper Power",
                exercises = listOf(
                    GeneratedExercise("Barbell Bench Press", 5, 3, 5, 8.5f, 180, null),
                    GeneratedExercise("Barbell Row", 4, 5, 6, 8.0f, 150, null),
                    GeneratedExercise("Overhead Press", 3, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Pull-up", 3, 5, 8, 7.5f, 120, null),
                    GeneratedExercise("Close Grip Bench Press", 3, 8, 10, 7.0f, 90, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 2,
                name = "Lower Power",
                exercises = listOf(
                    GeneratedExercise("Barbell Back Squat", 5, 3, 5, 8.5f, 180, null),
                    GeneratedExercise("Romanian Deadlift", 4, 5, 6, 8.0f, 150, null),
                    GeneratedExercise("Bulgarian Split Squat", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Barbell Hip Thrust", 3, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Calf Raise", 3, 12, 15, 6.5f, 60, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 3,
                name = "Upper Hypertrophy",
                exercises = listOf(
                    GeneratedExercise("Incline Dumbbell Press", 4, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Cable Row", 4, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Dumbbell Shoulder Press", 3, 10, 12, 6.5f, 75, null),
                    GeneratedExercise("Lat Pulldown", 3, 10, 12, 6.5f, 75, null),
                    GeneratedExercise("Barbell Curl", 3, 10, 12, 6.5f, 60, null),
                    GeneratedExercise("Tricep Extension", 3, 10, 12, 6.5f, 60, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 4,
                name = "Lower Hypertrophy",
                exercises = listOf(
                    GeneratedExercise("Front Squat", 4, 8, 10, 7.0f, 120, null),
                    GeneratedExercise("Stiff Leg Deadlift", 4, 8, 10, 7.0f, 120, null),
                    GeneratedExercise("Walking Lunge", 3, 10, 12, 6.5f, 75, "Per leg"),
                    GeneratedExercise("Leg Curl", 3, 12, 15, 6.5f, 60, null),
                    GeneratedExercise("Leg Extension", 3, 12, 15, 6.5f, 60, null)
                )
            )
        )
    }
    
    private fun generateFiveDayStrengthWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        // Return first 4 days from 4-day split + add a 5th day
        val fourDayWorkouts = generateFourDayStrengthWorkouts(sessionMinutes)
        return fourDayWorkouts + listOf(
            GeneratedWorkout(
                dayNumber = 5,
                name = "Accessory & Recovery",
                exercises = listOf(
                    GeneratedExercise("Goblet Squat", 3, 12, 15, 6.0f, 60, "Light weight, focus on mobility"),
                    GeneratedExercise("Push-up", 3, 10, 15, 6.0f, 60, null),
                    GeneratedExercise("Band Pull Apart", 3, 15, 20, 5.0f, 45, null),
                    GeneratedExercise("Plank", 3, 30, 60, null, 60, "Hold for time"),
                    GeneratedExercise("Bird Dog", 3, 8, 10, null, 45, "Per side")
                )
            )
        )
    }
    
    private fun generateFourDayHypertrophyWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        return listOf(
            GeneratedWorkout(
                dayNumber = 1,
                name = "Chest & Triceps",
                exercises = listOf(
                    GeneratedExercise("Barbell Bench Press", 4, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Incline Dumbbell Press", 4, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Dumbbell Flye", 3, 10, 12, 6.5f, 75, null),
                    GeneratedExercise("Tricep Dip", 3, 8, 12, 7.0f, 90, null),
                    GeneratedExercise("Overhead Tricep Extension", 3, 10, 12, 6.5f, 60, null),
                    GeneratedExercise("Tricep Pushdown", 3, 12, 15, 6.0f, 60, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 2,
                name = "Back & Biceps",
                exercises = listOf(
                    GeneratedExercise("Pull-up", 4, 6, 10, 7.5f, 120, null),
                    GeneratedExercise("Barbell Row", 4, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Lat Pulldown", 3, 10, 12, 6.5f, 75, null),
                    GeneratedExercise("Cable Row", 3, 10, 12, 6.5f, 75, null),
                    GeneratedExercise("Barbell Curl", 3, 10, 12, 6.5f, 60, null),
                    GeneratedExercise("Hammer Curl", 3, 12, 15, 6.0f, 60, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 3,
                name = "Legs & Glutes",
                exercises = listOf(
                    GeneratedExercise("Barbell Back Squat", 4, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Romanian Deadlift", 4, 8, 10, 7.0f, 90, null),
                    GeneratedExercise("Bulgarian Split Squat", 3, 10, 12, 6.5f, 75, "Per leg"),
                    GeneratedExercise("Leg Curl", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Leg Extension", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Calf Raise", 3, 15, 20, 6.0f, 45, null)
                )
            ),
            GeneratedWorkout(
                dayNumber = 4,
                name = "Shoulders & Arms",
                exercises = listOf(
                    GeneratedExercise("Overhead Press", 4, 6, 8, 7.5f, 120, null),
                    GeneratedExercise("Lateral Raise", 4, 10, 12, 6.5f, 60, null),
                    GeneratedExercise("Rear Delt Flye", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Face Pull", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Barbell Curl", 3, 10, 12, 6.5f, 60, null),
                    GeneratedExercise("Close Grip Bench Press", 3, 10, 12, 6.5f, 75, null)
                )
            )
        )
    }
    
    private fun generateFiveDayHypertrophyWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        val fourDayWorkouts = generateFourDayHypertrophyWorkouts(sessionMinutes)
        return fourDayWorkouts + listOf(
            GeneratedWorkout(
                dayNumber = 5,
                name = "Full Body Pump",
                exercises = listOf(
                    GeneratedExercise("Goblet Squat", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Push-up", 3, 10, 15, 6.0f, 60, null),
                    GeneratedExercise("Dumbbell Row", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Dumbbell Shoulder Press", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Plank", 3, 30, 60, null, 60, "Hold for time")
                )
            )
        )
    }
    
    private fun generateSixDayHypertrophyWorkouts(sessionMinutes: Int): List<GeneratedWorkout> {
        val fiveDayWorkouts = generateFiveDayHypertrophyWorkouts(sessionMinutes)
        return fiveDayWorkouts + listOf(
            GeneratedWorkout(
                dayNumber = 6,
                name = "Cardio & Core",
                exercises = listOf(
                    GeneratedExercise("Treadmill Walk", 1, 20, 30, 5.0f, 0, "Moderate pace"),
                    GeneratedExercise("Bicycle Crunch", 3, 15, 20, 6.0f, 45, "Per side"),
                    GeneratedExercise("Russian Twist", 3, 20, 30, 6.0f, 45, null),
                    GeneratedExercise("Dead Bug", 3, 8, 12, 5.0f, 45, "Per side"),
                    GeneratedExercise("Mountain Climber", 3, 20, 30, 6.0f, 60, "Total reps")
                )
            )
        )
    }
    
    private fun generateFatLossWorkouts(frequency: Int, sessionMinutes: Int): List<GeneratedWorkout> {
        return (1..frequency).map { day ->
            GeneratedWorkout(
                dayNumber = day,
                name = "Fat Loss Circuit ${day}",
                exercises = listOf(
                    GeneratedExercise("Burpee", 4, 8, 12, 7.0f, 60, "Explosive movement"),
                    GeneratedExercise("Kettlebell Swing", 4, 15, 20, 7.0f, 60, null),
                    GeneratedExercise("Mountain Climber", 4, 20, 30, 6.5f, 45, "Total reps"),
                    GeneratedExercise("Jump Squat", 3, 12, 15, 6.5f, 45, null),
                    GeneratedExercise("Push-up", 3, 10, 15, 6.5f, 45, null),
                    GeneratedExercise("High Knee", 3, 30, 45, 6.0f, 30, "Seconds")
                )
            )
        }
    }
    
    private fun generateAthleticWorkouts(frequency: Int, sessionMinutes: Int): List<GeneratedWorkout> {
        return (1..frequency).map { day ->
            GeneratedWorkout(
                dayNumber = day,
                name = "Athletic Performance ${day}",
                exercises = listOf(
                    GeneratedExercise("Box Jump", 4, 5, 8, 7.0f, 90, "Focus on landing"),
                    GeneratedExercise("Medicine Ball Slam", 4, 8, 10, 7.0f, 75, null),
                    GeneratedExercise("Lateral Bound", 3, 6, 8, 6.5f, 75, "Per side"),
                    GeneratedExercise("Agility Ladder", 3, 30, 45, 6.0f, 60, "Seconds"),
                    GeneratedExercise("Single Leg Deadlift", 3, 8, 10, 6.5f, 60, "Per leg"),
                    GeneratedExercise("Bear Crawl", 3, 20, 30, 6.0f, 60, "Steps")
                )
            )
        }
    }
    
    private fun generateGeneralFitnessWorkouts(frequency: Int, sessionMinutes: Int): List<GeneratedWorkout> {
        return (1..frequency).map { day ->
            GeneratedWorkout(
                dayNumber = day,
                name = "General Fitness ${day}",
                exercises = listOf(
                    GeneratedExercise("Bodyweight Squat", 3, 12, 15, 6.0f, 60, null),
                    GeneratedExercise("Push-up", 3, 8, 12, 6.5f, 60, null),
                    GeneratedExercise("Glute Bridge", 3, 12, 15, 6.0f, 45, null),
                    GeneratedExercise("Plank", 3, 30, 45, null, 60, "Hold for time"),
                    GeneratedExercise("Jumping Jack", 3, 20, 30, 6.0f, 45, null),
                    GeneratedExercise("Wall Sit", 3, 20, 30, 6.0f, 60, "Seconds")
                )
            )
        }
    }
}