package com.github.radupana.featherweight.data.programme

import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.validation.ExerciseValidator
import com.github.radupana.featherweight.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgrammeSeeder(
    private val programmeDao: ProgrammeDao,
    private val exerciseDao: ExerciseDao,
) {
    suspend fun seedPopularProgrammes() {
        withContext(Dispatchers.IO) {
            // Check and seed each programme individually
            val existingTemplates = programmeDao.getAllTemplates()
            val existingNames = existingTemplates.map { it.name }.toSet()

            if (!existingNames.contains("2-Week Test Programme")) {
                seed2WeekTestProgramme()
            }

        }
    }

    private suspend fun seed2WeekTestProgramme() {
        // Validate exercises FIRST before creating programme
        val requiredExercises =
            listOf(
                // Upper exercises
                "Barbell Bench Press",
                "Barbell Row",
                "Barbell Overhead Press",
                // Lower exercises
                "Barbell Back Squat",
                "Barbell Deadlift",
                "Barbell Romanian Deadlift",
            )

        val validator = ExerciseValidator(exerciseDao)
        validator.initialize()

        val validationErrors =
            requiredExercises.mapNotNull { exercise ->
                when (val result = validator.validateExerciseName(exercise)) {
                    is ValidationResult.Invalid -> {
                        result
                    }

                    else -> null
                }
            }

        if (validationErrors.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot seed 2-Week Test Programme: Invalid exercises found. " +
                    "Please ensure all exercises exist in the database:\n" +
                    validationErrors.joinToString("\n") { "- ${it.providedName}" },
            )
        }


        val testProgrammeStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1 - Volume Focus",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Upper Body",
                                "estimatedDuration": 45,
                                "exercises": [
                                    {"name": "Barbell Bench Press", "sets": 3, "reps": 10, "weight": 60, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 3, "reps": 10, "weight": 50, "progression": "linear"},
                                    {"name": "Barbell Overhead Press", "sets": 2, "reps": 12, "weight": 40, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Lower Body",
                                "estimatedDuration": 45,
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 3, "reps": 10, "weight": 80, "progression": "linear"},
                                    {"name": "Barbell Romanian Deadlift", "sets": 3, "reps": 10, "weight": 60, "progression": "linear"},
                                    {"name": "Barbell Deadlift", "sets": 2, "reps": 8, "weight": 100, "progression": "linear"}
                                ]
                            }
                        ]
                    },
                    {
                        "weekNumber": 2,
                        "name": "Week 2 - Intensity Focus",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Upper Body",
                                "estimatedDuration": 45,
                                "exercises": [
                                    {"name": "Barbell Bench Press", "sets": 3, "reps": 5, "weight": 80, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 3, "reps": 6, "weight": 70, "progression": "linear"},
                                    {"name": "Barbell Overhead Press", "sets": 2, "reps": 6, "weight": 50, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Lower Body",
                                "estimatedDuration": 45,
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 3, "reps": 5, "weight": 100, "progression": "linear"},
                                    {"name": "Barbell Deadlift", "sets": 3, "reps": 5, "weight": 120, "progression": "linear"},
                                    {"name": "Barbell Romanian Deadlift", "sets": 2, "reps": 6, "weight": 80, "progression": "linear"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": {
                        "Barbell Bench Press": 2.5,
                        "Barbell Row": 2.5,
                        "Barbell Overhead Press": 2.5,
                        "Barbell Back Squat": 5.0,
                        "Barbell Deadlift": 5.0,
                        "Barbell Romanian Deadlift": 2.5
                    }
                }
            }
            """.trimIndent()

        val weightCalcRules =
            """
            {
                "baseOn": "LAST_WORKOUT",
                "trainingMaxPercentage": 1.0,
                "roundingIncrement": 2.5,
                "minimumBarWeight": 20.0
            }
            """.trimIndent()

        val progressionRules =
            """
            {
                "type": "LINEAR",
                "incrementRules": {
                    "Barbell Bench Press": 2.5,
                    "Barbell Row": 2.5,
                    "Barbell Overhead Press": 2.5,
                    "Barbell Back Squat": 5.0,
                    "Barbell Deadlift": 5.0,
                    "Barbell Romanian Deadlift": 2.5
                },
                "deloadRules": {
                    "triggerAfterFailures": 3,
                    "deloadPercentage": 0.9,
                    "minimumWeight": 20.0
                },
                "successCriteria": {
                    "requiredSets": "ALL",
                    "requiredReps": "TARGET", 
                    "allowedMissedReps": 1
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "2-Week Test Programme",
                description = "Simple 2-week Upper/Lower split for testing programme completion. Week 1 focuses on volume (higher reps, moderate weight), Week 2 on intensity (lower reps, heavier weight). Only 2 workouts per week with 3 exercises each.",
                durationWeeks = 2,
                programmeType = ProgrammeType.BODYBUILDING,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                author = "Featherweight",
                requiresMaxes = false,
                allowsAccessoryCustomization = false,
                jsonStructure = testProgrammeStructure,
                weightCalculationRules = weightCalcRules,
                progressionRules = progressionRules,
            )

        programmeDao.insertProgrammeTemplate(template)
    }
}
