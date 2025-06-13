package com.github.radupana.featherweight.data.programme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgrammeSeeder(private val programmeDao: ProgrammeDao) {
    suspend fun seedPopularProgrammes() {
        withContext(Dispatchers.IO) {
            val existingTemplates = programmeDao.getAllTemplates()
            if (existingTemplates.isNotEmpty()) {
                println("Programme templates already exist, skipping seeding")
                return@withContext
            }

            // Seed the 5 popular programs
            seedStrongLifts5x5()
            seedStartingStrength()
            seedWendler531Beginner()
            seedNSuns531()
            seedUpperLowerSplit()

            println("Seeded ${programmeDao.getAllTemplates().size} programme templates")
        }
    }

    private suspend fun seedStrongLifts5x5() {
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
                                    {"name": "Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Bench Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Bent-Over Barbell Row", "sets": 5, "reps": 5, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Workout B",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Overhead Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Conventional Deadlift", "sets": 1, "reps": 5, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 5,
                                "name": "Workout A",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Bench Press", "sets": 5, "reps": 5, "progression": "linear"},
                                    {"name": "Bent-Over Barbell Row", "sets": 5, "reps": 5, "progression": "linear"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": 2.5,
                    "deloadThreshold": 3
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
            )

        programmeDao.insertProgrammeTemplate(template)
    }

    private suspend fun seedStartingStrength() {
        val startingStrengthStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Day A",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Bench Press", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Conventional Deadlift", "sets": 1, "reps": 5, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Day B",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Overhead Press", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Power Clean", "sets": 5, "reps": 3, "progression": "linear"}
                                ]
                            },
                            {
                                "day": 5,
                                "name": "Day A",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Bench Press", "sets": 3, "reps": 5, "progression": "linear"},
                                    {"name": "Conventional Deadlift", "sets": 1, "reps": 5, "progression": "linear"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": 2.5,
                    "note": "Squat every workout, alternate bench/press and deadlift/power clean"
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "Starting Strength",
                description = "Mark Rippetoe's classic novice program. Focus on the basic barbell movements with linear progression.",
                durationWeeks = 16,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                author = "Mark Rippetoe",
                requiresMaxes = true,
                allowsAccessoryCustomization = false,
                jsonStructure = startingStrengthStructure,
            )

        programmeDao.insertProgrammeTemplate(template)
    }

    private suspend fun seedWendler531Beginner() {
        val wendler531Structure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1 (65%, 75%, 85%)",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Squat Day",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 3, "reps": [5, 3, "1+"], "intensity": [65, 75, 85], "progression": "531"},
                                    {"name": "Bench Press", "sets": 3, "reps": [5, 5, 5], "intensity": [65, 75, 85], "progression": "fsl"},
                                    {"name": "ACCESSORY_PUSH", "sets": 3, "reps": "8-12", "customizable": true},
                                    {"name": "ACCESSORY_CORE", "sets": 3, "reps": "10-15", "customizable": true}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Bench Day",
                                "exercises": [
                                    {"name": "Bench Press", "sets": 3, "reps": [5, 3, "1+"], "intensity": [65, 75, 85], "progression": "531"},
                                    {"name": "Back Squat", "sets": 3, "reps": [5, 5, 5], "intensity": [65, 75, 85], "progression": "fsl"},
                                    {"name": "ACCESSORY_PULL", "sets": 3, "reps": "8-12", "customizable": true},
                                    {"name": "ACCESSORY_SINGLE_LEG", "sets": 3, "reps": "10-15", "customizable": true}
                                ]
                            },
                            {
                                "day": 3,
                                "name": "Deadlift Day",
                                "exercises": [
                                    {"name": "Conventional Deadlift", "sets": 3, "reps": [5, 3, "1+"], "intensity": [65, 75, 85], "progression": "531"},
                                    {"name": "Overhead Press", "sets": 3, "reps": [5, 5, 5], "intensity": [65, 75, 85], "progression": "fsl"},
                                    {"name": "ACCESSORY_PUSH", "sets": 3, "reps": "8-12", "customizable": true},
                                    {"name": "ACCESSORY_CORE", "sets": 3, "reps": "10-15", "customizable": true}
                                ]
                            },
                            {
                                "day": 4,
                                "name": "Press Day",
                                "exercises": [
                                    {"name": "Overhead Press", "sets": 3, "reps": [5, 3, "1+"], "intensity": [65, 75, 85], "progression": "531"},
                                    {"name": "Conventional Deadlift", "sets": 3, "reps": [5, 5, 5], "intensity": [65, 75, 85], "progression": "fsl"},
                                    {"name": "ACCESSORY_PULL", "sets": 3, "reps": "8-12", "customizable": true},
                                    {"name": "ACCESSORY_SINGLE_LEG", "sets": 3, "reps": "10-15", "customizable": true}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "531",
                    "cycle": 3,
                    "increment": {"squat": 5, "bench": 2.5, "deadlift": 5, "press": 2.5}
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "Wendler 5/3/1 Beginner",
                description = "Jim Wendler's percentage-based program. Requires 1RM input. Includes customizable accessories.",
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                author = "Jim Wendler",
                requiresMaxes = true,
                allowsAccessoryCustomization = true,
                jsonStructure = wendler531Structure,
            )

        programmeDao.insertProgrammeTemplate(template)
    }

    private suspend fun seedNSuns531() {
        val nsunsStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Bench/OHP",
                                "exercises": [
                                    {"name": "Bench Press", "sets": 9, "reps": [8,6,4,4,4,6,8,8,8], "intensity": [65,75,85,85,85,80,75,70,65], "progression": "nsuns"},
                                    {"name": "Overhead Press", "sets": 8, "reps": [6,5,3,5,7,4,6,8], "intensity": [50,60,70,70,70,65,60,55], "progression": "nsuns_secondary"},
                                    {"name": "ACCESSORY_1", "sets": 3, "reps": "8-12", "customizable": true, "category": "pull"},
                                    {"name": "ACCESSORY_2", "sets": 3, "reps": "8-12", "customizable": true, "category": "triceps"},
                                    {"name": "ACCESSORY_3", "sets": 3, "reps": "8-12", "customizable": true, "category": "lateral_delt"}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Squat/Sumo",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 9, "reps": [5,3,1,3,3,3,5,5,5], "intensity": [75,85,95,90,85,80,75,70,65], "progression": "nsuns"},
                                    {"name": "Sumo Deadlift", "sets": 8, "reps": [5,3,5,3,5,3,3,5], "intensity": [50,60,70,70,70,65,60,55], "progression": "nsuns_secondary"},
                                    {"name": "ACCESSORY_1", "sets": 3, "reps": "8-12", "customizable": true, "category": "leg"},
                                    {"name": "ACCESSORY_2", "sets": 3, "reps": "8-12", "customizable": true, "category": "back"},
                                    {"name": "ACCESSORY_3", "sets": 3, "reps": "10-15", "customizable": true, "category": "core"}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "nsuns",
                    "increment": {"squat": 5, "bench": 2.5, "deadlift": 5, "press": 2.5},
                    "note": "High volume program, requires good recovery"
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "nSuns 531",
                description = "High-volume 531 variation with extensive accessory customization. Requires 1RM input. Very demanding program.",
                durationWeeks = 16,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.ADVANCED,
                author = "nSuns (Reddit)",
                requiresMaxes = true,
                allowsAccessoryCustomization = true,
                jsonStructure = nsunsStructure,
            )

        programmeDao.insertProgrammeTemplate(template)
    }

    private suspend fun seedUpperLowerSplit() {
        val upperLowerStructure =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Upper Body A",
                                "exercises": [
                                    {"name": "Bench Press", "sets": 4, "reps": "6-8", "progression": "linear"},
                                    {"name": "Bent-Over Barbell Row", "sets": 4, "reps": "6-8", "progression": "linear"},
                                    {"name": "Overhead Press", "sets": 3, "reps": "8-10", "progression": "linear"},
                                    {"name": "ACCESSORY_PULL", "sets": 3, "reps": "8-12", "customizable": true},
                                    {"name": "ACCESSORY_ARMS", "sets": 3, "reps": "10-12", "customizable": true}
                                ]
                            },
                            {
                                "day": 2,
                                "name": "Lower Body A",
                                "exercises": [
                                    {"name": "Back Squat", "sets": 4, "reps": "6-8", "progression": "linear"},
                                    {"name": "Romanian Deadlift", "sets": 3, "reps": "8-10", "progression": "linear"},
                                    {"name": "Bulgarian Split Squats", "sets": 3, "reps": "10-12", "progression": "linear"},
                                    {"name": "ACCESSORY_HAMSTRING", "sets": 3, "reps": "10-15", "customizable": true},
                                    {"name": "ACCESSORY_CALF", "sets": 4, "reps": "12-15", "customizable": true}
                                ]
                            },
                            {
                                "day": 4,
                                "name": "Upper Body B",
                                "exercises": [
                                    {"name": "Incline Dumbbell Press", "sets": 4, "reps": "8-10", "progression": "linear"},
                                    {"name": "Cable Rows", "sets": 4, "reps": "8-10", "progression": "linear"},
                                    {"name": "Dumbbell Shoulder Press", "sets": 3, "reps": "10-12", "progression": "linear"},
                                    {"name": "ACCESSORY_CHEST", "sets": 3, "reps": "10-12", "customizable": true},
                                    {"name": "ACCESSORY_BACK", "sets": 3, "reps": "10-12", "customizable": true}
                                ]
                            },
                            {
                                "day": 5,
                                "name": "Lower Body B",
                                "exercises": [
                                    {"name": "Conventional Deadlift", "sets": 4, "reps": "5-6", "progression": "linear"},
                                    {"name": "Front Squats", "sets": 3, "reps": "8-10", "progression": "linear"},
                                    {"name": "Walking Lunges", "sets": 3, "reps": "12-15", "progression": "linear"},
                                    {"name": "ACCESSORY_QUAD", "sets": 3, "reps": "12-15", "customizable": true},
                                    {"name": "ACCESSORY_GLUTE", "sets": 3, "reps": "12-15", "customizable": true}
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": 2.5,
                    "note": "Upper/Lower split allows for higher frequency and volume per muscle group"
                }
            }
            """.trimIndent()

        val template =
            ProgrammeTemplate(
                name = "Upper/Lower Split",
                description = "Flexible 4-day split focusing on upper and lower body. Highly customizable accessories. Good for strength and muscle building.",
                durationWeeks = 12,
                programmeType = ProgrammeType.HYBRID,
                difficulty = ProgrammeDifficulty.NOVICE,
                author = "Community Standard",
                requiresMaxes = false,
                allowsAccessoryCustomization = true,
                jsonStructure = upperLowerStructure,
            )

        programmeDao.insertProgrammeTemplate(template)
    }
}
