package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.model.SkillLevel
import com.github.radupana.featherweight.data.model.TimeAvailable
import com.github.radupana.featherweight.data.model.TrainingGoal
import com.github.radupana.featherweight.data.model.WorkoutTemplateGenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for generating workout templates for standard splits
 */
class WorkoutTemplateGeneratorService(
    private val exerciseVariationDao: ExerciseVariationDao,
) {
    suspend fun generateTemplate(
        templateName: String,
        config: WorkoutTemplateGenerationConfig,
    ): List<Triple<ExerciseVariation, Int, Int>> =
        withContext(Dispatchers.IO) {
            when (templateName) {
                "Push" -> generatePushWorkout(config)
                "Pull" -> generatePullWorkout(config)
                "Legs" -> generateLegsWorkout(config)
                "Upper Body" -> generateUpperBodyWorkout(config)
                "Full Body" -> generateFullBodyWorkout(config)
                else -> emptyList()
            }
        }

    private suspend fun generatePushWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        // Define exercises based on time available
        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Dumbbell Fly")
                exercises.add("Cable Triceps Pushdown")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Incline Bench Press")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Cable Lateral Raise")
                exercises.add("Cable Fly")
                exercises.add("Cable Triceps Pushdown")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Incline Bench Press")
                exercises.add("Cable Fly")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Dumbbell Lateral Raise")
                exercises.add("Cable Face Pull")
                exercises.add("Cable Triceps Pushdown")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generatePullWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        // Similar implementation for pull workout
        return emptyList()
    }

    private suspend fun generateLegsWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        // Similar implementation for legs workout
        return emptyList()
    }

    private suspend fun generateUpperBodyWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        // Similar implementation for upper body workout
        return emptyList()
    }

    private suspend fun generateFullBodyWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        // Similar implementation for full body workout
        return emptyList()
    }

    private suspend fun generateWorkoutFromExercises(
        exerciseNames: List<String>,
        config: WorkoutTemplateGenerationConfig,
    ): List<Triple<ExerciseVariation, Int, Int>> {
        val result = mutableListOf<Triple<ExerciseVariation, Int, Int>>()
        val maxExercises = getMaxExercises(config.time, "Push")

        for ((index, name) in exerciseNames.withIndex()) {
            if (index >= maxExercises) break

            val exercise = findMatchingExercise(listOf(name)) ?: continue
            val sets = getSetsForExercise(exercise.name, config)
            val reps = getRepsForGoal(config.goal)
            result.add(Triple(exercise, sets, reps))
        }

        // Adapt for skill level
        return when (config.skillLevel) {
            SkillLevel.BEGINNER -> result.take(result.size * 2 / 3)
            else -> result
        }
    }

    private suspend fun findMatchingExercise(options: List<String>): ExerciseVariation? {
        // Simply find the first available exercise from the options
        // Assumes full commercial gym access
        for (exerciseName in options) {
            // First try exact name match
            val exercise = exerciseVariationDao.getExerciseVariationByName(exerciseName)
            if (exercise != null) {
                return exercise
            }
        }
        return null
    }

    private fun getMaxExercises(
        timeAvailable: TimeAvailable,
        templateName: String,
    ): Int =
        when (templateName) {
            "Push", "Pull" ->
                when (timeAvailable) {
                    TimeAvailable.QUICK -> 4
                    TimeAvailable.STANDARD -> 6
                    TimeAvailable.EXTENDED -> 7
                }

            "Legs" ->
                when (timeAvailable) {
                    TimeAvailable.QUICK -> 3
                    TimeAvailable.STANDARD -> 5
                    TimeAvailable.EXTENDED -> 7
                }

            else -> 5
        }

    private fun getSetsForExercise(
        exerciseName: String,
        config: WorkoutTemplateGenerationConfig,
    ): Int {
        val isCompound =
            exerciseName.contains("Press") ||
                exerciseName.contains("Squat") ||
                exerciseName.contains("Deadlift") ||
                exerciseName.contains("Row") ||
                exerciseName.contains("Pull Up") ||
                exerciseName.contains("Dip")

        return when (config.goal) {
            TrainingGoal.STRENGTH -> if (isCompound) 5 else 3
            TrainingGoal.HYPERTROPHY -> if (isCompound) 4 else 3
            TrainingGoal.ENDURANCE -> if (isCompound) 3 else 2
        }
    }

    private fun getRepsForGoal(goal: TrainingGoal): Int =
        when (goal) {
            TrainingGoal.STRENGTH -> 5
            TrainingGoal.HYPERTROPHY -> 10
            TrainingGoal.ENDURANCE -> 15
        }
}
