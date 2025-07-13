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
            
            if (!existingNames.contains("StrongLifts 5x5")) {
                seedStrongLifts5x5()
                println("Seeded StrongLifts 5x5")
            }
            
            if (!existingNames.contains("2-Week Test Programme")) {
                seed2WeekTestProgramme()
                println("Seeded 2-Week Test Programme")
            }

            println("Total programme templates: ${programmeDao.getAllTemplates().size}")
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

    private suspend fun seed2WeekTestProgramme() {
        // Validate exercises FIRST before creating programme
        val requiredExercises =
            listOf(
                // Push exercises
                "Barbell Bench Press",
                "Barbell Overhead Press", 
                "Band Chest Fly",  // Fixed from "Dumbbell Chest Fly"
                "Cable Tricep Pushdown",
                // Pull exercises
                "Barbell Deadlift",
                "Bodyweight Pull Up",  // Fixed from "Pull Up"
                "Barbell Row",
                "Barbell Bicep Curl",
                // Leg exercises
                "Barbell Back Squat",
                "Barbell Romanian Deadlift",
                "Band Leg Press",  // Fixed from "Leg Press"
                "Barbell Calf Raise"  // Fixed from "Calf Raise"
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
                "Cannot seed 2-Week Test Programme: Invalid exercises found. " +
                    "Please ensure all exercises exist in the database:\n" +
                    validationErrors.joinToString("\n") { "- ${it.providedName}" },
            )
        }

        println("✅ All 2-Week Test Programme exercises validated successfully")

        val testProgrammeStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Push Day",
                                "exercises": [
                                    {"name": "Barbell Bench Press", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Overhead Press", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Band Chest Fly", "sets": 3, "reps": 12, "progression": "linear"},
                                    {"name": "Cable Tricep Pushdown", "sets": 3, "reps": 15, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Pull Day",
                                "exercises": [
                                    {"name": "Barbell Deadlift", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Bodyweight Pull Up", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 3, "reps": 10, "progression": "linear"},
                                    {"name": "Barbell Bicep Curl", "sets": 3, "reps": 12, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Leg Day",
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Romanian Deadlift", "sets": 3, "reps": 10, "progression": "linear"},
                                    {"name": "Band Leg Press", "sets": 3, "reps": 12, "progression": "linear"},
                                    {"name": "Barbell Calf Raise", "sets": 3, "reps": 15, "progression": "linear"}
                                ]
                            }
                        ]
                    },
                    {
                        "weekNumber": 2,
                        "name": "Week 2",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Push Day",
                                "exercises": [
                                    {"name": "Barbell Bench Press", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Overhead Press", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Band Chest Fly", "sets": 3, "reps": 12, "progression": "linear"},
                                    {"name": "Cable Tricep Pushdown", "sets": 3, "reps": 15, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Pull Day",
                                "exercises": [
                                    {"name": "Barbell Deadlift", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Bodyweight Pull Up", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Row", "sets": 3, "reps": 10, "progression": "linear"},
                                    {"name": "Barbell Bicep Curl", "sets": 3, "reps": 12, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Leg Day",
                                "exercises": [
                                    {"name": "Barbell Back Squat", "sets": 3, "reps": 8, "progression": "linear"},
                                    {"name": "Barbell Romanian Deadlift", "sets": 3, "reps": 10, "progression": "linear"},
                                    {"name": "Band Leg Press", "sets": 3, "reps": 12, "progression": "linear"},
                                    {"name": "Barbell Calf Raise", "sets": 3, "reps": 15, "progression": "linear"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": {
                        "Barbell Bench Press": 2.5,
                        "Barbell Overhead Press": 2.5,
                        "Band Chest Fly": 2.5,
                        "Cable Tricep Pushdown": 2.5,
                        "Barbell Deadlift": 5.0,
                        "Bodyweight Pull Up": 0.0,
                        "Barbell Row": 2.5,
                        "Barbell Bicep Curl": 2.5,
                        "Barbell Back Squat": 5.0,
                        "Barbell Romanian Deadlift": 2.5,
                        "Band Leg Press": 5.0,
                        "Barbell Calf Raise": 2.5
                    },
                    "deloadThreshold": 3,
                    "deloadPercentage": 0.9,
                    "successCriteria": {
                        "requiredSets": 3,
                        "requiredReps": "TARGET",
                        "allowedMissedReps": 1
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
                    "Barbell Overhead Press": 2.5,
                    "Dumbbell Chest Fly": 2.5,
                    "Cable Tricep Pushdown": 2.5,
                    "Barbell Deadlift": 5.0,
                    "Pull Up": 0.0,
                    "Barbell Row": 2.5,
                    "Barbell Bicep Curl": 2.5,
                    "Barbell Back Squat": 5.0,
                    "Barbell Romanian Deadlift": 2.5,
                    "Leg Press": 5.0,
                    "Calf Raise": 2.5
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
                description = "Short test programme with Push/Pull/Legs split for quick testing. Linear progression with 4 exercises per workout.",
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
