package com.github.radupana.featherweight.data.model

enum class TimeAvailable {
    QUICK, // 20-30 minutes, 3-4 exercises
    STANDARD, // 45-60 minutes, 5-6 exercises
    EXTENDED, // 75+ minutes, 7-8 exercises
}

enum class TrainingGoal {
    STRENGTH, // Heavy weight, 3-5 reps
    HYPERTROPHY, // Moderate weight, 8-12 reps
    ENDURANCE, // Light weight, 15+ reps
}

enum class IntensityLevel {
    CONSERVATIVE, // Leave 3-4 reps in tank
    MODERATE, // Leave 1-2 reps in tank
    AGGRESSIVE, // Push close to failure
}

data class WorkoutTemplateConfig(
    val timeAvailable: TimeAvailable,
    val goal: TrainingGoal,
    val intensity: IntensityLevel,
)

data class ExerciseSlot(
    val required: Boolean,
    val exerciseOptions: List<String>, // Ordered by preference/effectiveness
)

data class WorkoutTemplate(
    val name: String,
    val muscleGroups: List<String>,
    val exerciseSlots: List<ExerciseSlot>,
)
