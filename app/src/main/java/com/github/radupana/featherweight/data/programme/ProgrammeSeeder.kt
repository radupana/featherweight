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
            val existingTemplates = programmeDao.getAllTemplates()
            if (existingTemplates.isNotEmpty()) {
                println("Programme templates already exist, skipping seeding")
                return@withContext
            }

            // Only seed StrongLifts 5x5 for now - perfect one programme before adding more
            seedStrongLifts5x5()

            println("Seeded ${programmeDao.getAllTemplates().size} programme template")
        }
    }

    private suspend fun seedStrongLifts5x5() {
        // Validate exercises FIRST before creating programme
        val requiredExercises =
            listOf(
                "Barbell Back Squat",
                "Barbell Bench Press",
                "Barbell Row",
                "Barbell Overhead Press",
                "Barbell Deadlift",
            )

        val validator = ExerciseValidator(exerciseDao)
        validator.initialize()

        val validationErrors =
            requiredExercises.mapNotNull { exercise ->
                when (val result = validator.validateExerciseName(exercise)) {
                    is ValidationResult.Invalid -> {
                        println("❌ Exercise validation failed: ${result.reason}")
                        if (result.suggestion != null) {
                            println("   Suggestion: Use '${result.suggestion}' instead")
                        }
                        result
                    }
                    else -> null
                }
            }

        if (validationErrors.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot seed StrongLifts 5x5: Invalid exercises found. " +
                    "Please ensure all exercises exist in the database:\n" +
                    validationErrors.joinToString("\n") { "- ${it.providedName}" },
            )
        }

        println("✅ All StrongLifts exercises validated successfully")

        val strongLiftsStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Workout A",
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Bench Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 5, "reps": 5, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Workout B",
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Overhead Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Deadlift", "sets": 1, "reps": 5, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Workout A",
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Bench Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 5, "reps": 5, "progression": "linear"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": {
                        "Barbell Back Squat": 5.0,
                        "Barbell Deadlift": 5.0,
                        "Barbell Bench Press": 2.5,
                        "Barbell Overhead Press": 2.5,
                        "Barbell Row": 2.5
                    },
                    "deloadThreshold": 3,
                    "deloadPercentage": 0.85,
                    "successCriteria": {
                        "requiredSets": 5,
                        "requiredReps": 5,
                        "allowedMissedReps": 2
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
                    "Barbell Back Squat": 5.0,
                    "Barbell Deadlift": 5.0,
                    "Barbell Bench Press": 2.5,
                    "Barbell Overhead Press": 2.5,
                    "Barbell Row": 2.5
                },
                "deloadRules": {
                    "triggerAfterFailures": 3,
                    "deloadPercentage": 0.85,
                    "minimumWeight": 20.0
                },
                "successCriteria": {
                    "requiredSets": 5,
                    "requiredReps": 5,
                    "allowedMissedReps": 2
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "StrongLifts 5x5",
                description = "Simple, effective strength program focusing on compound movements. Perfect for beginners. Add 2.5kg every workout.",
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                author = "Mehdi Hadim",
                requiresMaxes = true,
                allowsAccessoryCustomization = false,
                jsonStructure = strongLiftsStructure,
                weightCalculationRules = weightCalcRules,
                progressionRules = progressionRules,
            )

        programmeDao.insertProgrammeTemplate(template)
    }
}
