package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.model.TimeAvailable
import com.github.radupana.featherweight.data.model.TrainingGoal
import com.github.radupana.featherweight.data.model.WorkoutTemplate
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import java.time.LocalDateTime

class WorkoutTemplateGeneratorService(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
) {
    suspend fun generateWorkout(
        template: WorkoutTemplate,
        config: WorkoutTemplateConfig,
    ): Long {
        val exercises = selectExercises(template, config)

        // Create workout
        val workout =
            Workout(
                date = LocalDateTime.now(),
                notes = "${template.name} workout - ${config.timeAvailable.name.lowercase().replace('_', ' ')}",
                isProgrammeWorkout = false,
                status = WorkoutStatus.NOT_STARTED,
            )

        val workoutId = workoutDao.insertWorkout(workout)

        // Add exercises and sets
        exercises.forEachIndexed { index, exerciseData ->
            val (exercise, sets, reps) = exerciseData

            val exerciseLog =
                ExerciseLog(
                    workoutId = workoutId,
                    exerciseName = exercise.name,
                    exerciseId = exercise.id,
                    exerciseOrder = index,
                    notes = "",
                )

            val exerciseLogId = exerciseLogDao.insert(exerciseLog)

            // Create sets
            repeat(sets) { setIndex ->
                val setLog =
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = setIndex + 1,
                        targetReps = reps,
                        targetWeight = null, // Will be filled by weight suggestion service
                        actualReps = 0,
                        actualWeight = 0f,
                        isCompleted = false,
                    )
                setLogDao.insert(setLog)
            }
        }

        return workoutId
    }

    private suspend fun selectExercises(
        template: WorkoutTemplate,
        config: WorkoutTemplateConfig,
    ): List<Triple<Exercise, Int, Int>> {
        val result = mutableListOf<Triple<Exercise, Int, Int>>()
        val maxExercises = getMaxExercises(config.timeAvailable, template.name)

        println("FeatherweightDebug: Generating ${template.name} workout with max $maxExercises exercises")

        // First, add all required exercises
        for (slot in template.exerciseSlots.filter { it.required }) {
            val exercise = findMatchingExercise(slot.exerciseOptions)
            if (exercise != null) {
                val sets = getSetsForExercise(exercise.name, slot.required)
                val reps = getRepsForGoal(config.goal)
                result.add(Triple(exercise, sets, reps))
                println("FeatherweightDebug: Added required exercise: ${exercise.name}")
            } else {
                println("FeatherweightDebug: FAILED to find required exercise from options: ${slot.exerciseOptions}")
            }
        }

        // Then fill remaining slots up to max exercises
        val remainingSlots = maxExercises - result.size
        if (remainingSlots > 0) {
            val optionalSlots = template.exerciseSlots.filter { !it.required }
            for (slot in optionalSlots.take(remainingSlots)) {
                val exercise = findMatchingExercise(slot.exerciseOptions)
                if (exercise != null) {
                    val sets = getSetsForExercise(exercise.name, slot.required)
                    val reps = getRepsForGoal(config.goal)
                    result.add(Triple(exercise, sets, reps))
                }
            }
        }

        return result
    }

    private suspend fun findMatchingExercise(
        options: List<String>,
    ): Exercise? {
        // Simply find the first available exercise from the options
        // Assumes full commercial gym access
        for (exerciseName in options) {
            // First try exact name match
            var exercise = exerciseDao.findExerciseByExactName(exerciseName)
            if (exercise != null) {
                return exercise
            }

            // If not found, try alias match
            exercise = exerciseDao.findExerciseByAlias(exerciseName)
            if (exercise != null) {
                println("FeatherweightDebug: Found exercise via alias: '$exerciseName' -> '${exercise.name}'")
                return exercise
            } else {
                // Log when an exercise isn't found
                println("FeatherweightDebug: Exercise not found in database: '$exerciseName'")
            }
        }
        println("FeatherweightDebug: No matching exercise found from options: $options")
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

            else ->
                when (timeAvailable) { // Full Body, Upper, Lower
                    TimeAvailable.QUICK -> 3
                    TimeAvailable.STANDARD -> 6
                    TimeAvailable.EXTENDED -> 9
                }
        }

    private fun getSetsForExercise(
        exerciseName: String,
        isRequired: Boolean,
    ): Int {
        val name = exerciseName.lowercase()

        // Big compounds get more sets
        return when {
            name.contains("squat") && name.contains("barbell") -> 5
            name.contains("deadlift") -> 5
            name.contains("bench press") && name.contains("barbell") -> 5
            name.contains("row") && name.contains("barbell") -> 4
            name.contains("press") && isRequired -> 4
            name.contains("pull") && isRequired -> 4
            isRequired -> 4
            else -> 3 // Isolation exercises
        }
    }

    private fun getRepsForGoal(goal: TrainingGoal): Int =
        when (goal) {
            TrainingGoal.STRENGTH -> 5
            TrainingGoal.HYPERTROPHY -> 10
            TrainingGoal.ENDURANCE -> 15
        }
}
