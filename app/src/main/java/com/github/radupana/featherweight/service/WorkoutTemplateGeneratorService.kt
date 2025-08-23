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
            val result =
                when (templateName) {
                    "Push" -> generatePushWorkout(config)
                    "Pull" -> generatePullWorkout(config)
                    "Legs" -> generateLegsWorkout(config)
                    "Upper", "Upper Body" -> generateUpperBodyWorkout(config)
                    "Lower" -> generateLowerBodyWorkout(config)
                    "Full Body" -> generateFullBodyWorkout(config)
                    else -> emptyList()
                }

            result
        }

    private suspend fun generatePushWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        // Define exercises based on time available
        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Dumbbell Fly")
                exercises.add("Cable Tricep Pushdown")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Incline Press")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Cable Lateral Raise")
                exercises.add("Cable Fly")
                exercises.add("Cable Tricep Pushdown")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Dumbbell Incline Press")
                exercises.add("Cable Fly")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Dumbbell Lateral Raise")
                exercises.add("Cable Face Pull")
                exercises.add("Cable Tricep Pushdown")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generatePullWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Row")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Cable Face Pull")
                exercises.add("Barbell Bicep Curl")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Row")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Cable Row")
                exercises.add("Cable Face Pull")
                exercises.add("Barbell Bicep Curl")
                exercises.add("Dumbbell Hammer Curl")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Row")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Cable Row")
                exercises.add("Cable Face Pull")
                exercises.add("Barbell Shrug")
                exercises.add("Barbell Bicep Curl")
                exercises.add("Dumbbell Hammer Curl")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generateLegsWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Romanian Deadlift")
                exercises.add("Machine Leg Curl")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Romanian Deadlift")
                exercises.add("Machine Leg Press")
                exercises.add("Machine Leg Curl")
                exercises.add("Barbell Calf Raise")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Romanian Deadlift")
                exercises.add("Machine Leg Press")
                exercises.add("Machine Leg Curl")
                exercises.add("Machine Leg Extension")
                exercises.add("Barbell Calf Raise")
                exercises.add("Dumbbell Walking Lunge")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generateUpperBodyWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Barbell Bicep Curl")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Barbell Bicep Curl")
                exercises.add("Cable Tricep Pushdown")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Incline Press")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Cable Lateral Raise")
                exercises.add("Barbell Bicep Curl")
                exercises.add("Cable Tricep Pushdown")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generateLowerBodyWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        // Lower body is similar to legs
        return generateLegsWorkout(config)
    }

    private suspend fun generateFullBodyWorkout(config: WorkoutTemplateGenerationConfig): List<Triple<ExerciseVariation, Int, Int>> {
        val exercises = mutableListOf<String>()

        when (config.time) {
            TimeAvailable.QUICK -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Shoulder Press")
            }
            TimeAvailable.STANDARD -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Barbell Romanian Deadlift")
                exercises.add("Barbell Bicep Curl")
            }
            TimeAvailable.EXTENDED -> {
                exercises.add("Barbell Back Squat")
                exercises.add("Barbell Bench Press")
                exercises.add("Barbell Row")
                exercises.add("Dumbbell Shoulder Press")
                exercises.add("Barbell Romanian Deadlift")
                exercises.add("Cable Lat Pulldown")
                exercises.add("Barbell Bicep Curl")
                exercises.add("Cable Tricep Pushdown")
            }
        }

        return generateWorkoutFromExercises(exercises, config)
    }

    private suspend fun generateWorkoutFromExercises(
        exerciseNames: List<String>,
        config: WorkoutTemplateGenerationConfig,
    ): List<Triple<ExerciseVariation, Int, Int>> {
        val result = mutableListOf<Triple<ExerciseVariation, Int, Int>>()
        val maxExercises = getMaxExercises(config.time, "Push")
        for ((index, name) in exerciseNames.withIndex()) {
            if (index >= maxExercises) break

            val exercise = findMatchingExercise(listOf(name))
            if (exercise == null) continue

            val sets = getSetsForExercise(exercise.name, config)
            val reps = getRepsForGoal(config.goal)
            result.add(Triple(exercise, sets, reps))
        }

        // Adapt for skill level
        val finalResult =
            when (config.skillLevel) {
                SkillLevel.BEGINNER -> result.take(result.size * 2 / 3)
                else -> result
            }

        return finalResult
    }

    private suspend fun findMatchingExercise(options: List<String>): ExerciseVariation? {
        for (exerciseName in options) {
            // First try exact name match
            val exercise = exerciseVariationDao.getExerciseVariationByName(exerciseName)
            if (exercise != null) return exercise

            // Try with case variations
            val exerciseLower = exerciseVariationDao.getExerciseVariationByName(exerciseName.lowercase())
            if (exerciseLower != null) return exerciseLower
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
