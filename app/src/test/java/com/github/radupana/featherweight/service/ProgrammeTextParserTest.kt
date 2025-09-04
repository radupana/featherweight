package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ProgrammeTextParserTest {
    private lateinit var parser: TestProgrammeTextParser

    // Test implementation that allows us to control the API response
    private inner class TestProgrammeTextParser(
        private var mockApiResponse: String? = null,
    ) : ProgrammeTextParser() {
        override suspend fun callOpenAIAPI(request: TextParsingRequest): String = mockApiResponse ?: error("No mock response set")

        fun setMockResponse(response: String) {
            mockApiResponse = response
        }
    }

    @Before
    fun setUp() {
        LogMock.setup()
        
        parser = TestProgrammeTextParser()
    }

    // Validation Tests
    @Test
    fun parseText_withBlankText_returnsValidationError() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "",
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Please provide programme text")
            assertThat(result.programme).isNull()
        }

    @Test
    fun parseText_withOnlyWhitespace_returnsValidationError() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "   \n  \t  ",
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Please provide programme text")
        }

    @Test
    fun parseText_withTextOver10000Characters_returnsValidationError() =
        runTest {
            val longText = "a".repeat(10001)
            val request =
                TextParsingRequest(
                    rawText = longText,
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Programme text is too long. Maximum 10,000 characters")
        }

    @Test
    fun parseText_withTextExactly10000Characters_passesValidation() =
        runTest {
            val text = "a".repeat(10000)
            val request =
                TextParsingRequest(
                    rawText = text,
                    userMaxes = emptyMap(),
                )

            // Set a valid mock response
            parser.setMockResponse(createValidProgrammeJson())

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            assertThat(result.error).isNull()
        }

    // Successful Parsing Tests
    @Test
    fun parseText_withSimpleProgramme_parsesProperly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText =
                        """
                        Week 1
                        Day 1
                        Barbell Squat 3x5 @ 100kg
                        Barbell Bench Press 3x8 @ 80kg
                        """.trimIndent(),
                    userMaxes = mapOf("Barbell Squat" to 120f),
                )

            parser.setMockResponse(
                """
                {
                    "name": "Test Programme",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": "Monday",
                                    "name": "Day 1",
                                    "exercises": [
                                        {
                                            "name": "Barbell Squat",
                                            "sets": [
                                                {"reps": 5, "weight": 100},
                                                {"reps": 5, "weight": 100},
                                                {"reps": 5, "weight": 100}
                                            ]
                                        },
                                        {
                                            "name": "Barbell Bench Press",
                                            "sets": [
                                                {"reps": 8, "weight": 80},
                                                {"reps": 8, "weight": 80},
                                                {"reps": 8, "weight": 80}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            // If the test fails, print the error message to help debug
            if (!result.success) {
                println("Parse failed with error: ${result.error}")
            }

            assertThat(result.success).isTrue()
            assertThat(result.programme).isNotNull()
            assertThat(result.programme?.name).isEqualTo("Test Programme")
            assertThat(result.programme?.durationWeeks).isEqualTo(1)
            assertThat(result.programme?.weeks).hasSize(1)

            val week = result.programme?.weeks?.first()
            assertThat(week?.weekNumber).isEqualTo(1)
            assertThat(week?.workouts).hasSize(1)

            val workout = week?.workouts?.first()
            assertThat(workout?.dayOfWeek).isEqualTo("Monday")
            assertThat(workout?.name).isEqualTo("Day 1")
            assertThat(workout?.exercises).hasSize(2)

            val squat = workout?.exercises?.first()
            assertThat(squat?.exerciseName).isEqualTo("Barbell Squat")
            assertThat(squat?.sets).hasSize(3)
            assertThat(squat?.sets?.all { it.reps == 5 && it.weight == 100f }).isTrue()
        }

    @Test
    fun parseText_withMultipleWeeks_parsesAllWeeks() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Multi-week programme",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "4 Week Programme",
                    "duration_weeks": 4,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Workout A",
                                    "exercises": [
                                        {"name": "Barbell Squat", "sets": [{"reps": 5, "weight": 100}]}
                                    ]
                                }
                            ]
                        },
                        {
                            "week_number": 2,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Workout B",
                                    "exercises": [
                                        {"name": "Barbell Deadlift", "sets": [{"reps": 3, "weight": 140}]}
                                    ]
                                }
                            ]
                        },
                        {
                            "week_number": 3,
                            "workouts": [
                                {
                                    "day": "Wednesday",
                                    "name": "Workout C",
                                    "exercises": [
                                        {"name": "Barbell Press", "sets": [{"reps": 8, "weight": 60}]}
                                    ]
                                }
                            ]
                        },
                        {
                            "week_number": 4,
                            "workouts": [
                                {
                                    "day": "Friday",
                                    "name": "Deload",
                                    "exercises": [
                                        {"name": "Barbell Row", "sets": [{"reps": 10, "weight": 70}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            assertThat(result.programme?.durationWeeks).isEqualTo(4)
            assertThat(result.programme?.weeks).hasSize(4)
            assertThat(result.programme?.weeks?.map { it.weekNumber }).isEqualTo(listOf(1, 2, 3, 4))
        }

    @Test
    fun parseText_withRPEValues_parsesRPECorrectly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme with RPE",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "RPE Programme",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "RPE Workout",
                                    "exercises": [
                                        {
                                            "name": "Barbell Squat",
                                            "sets": [
                                                {"reps": 1, "weight": 150, "rpe": 9},
                                                {"reps": 3, "weight": 130, "rpe": 7.5},
                                                {"reps": 5, "weight": 120, "rpe": 6}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val sets =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
                    ?.first()
                    ?.sets
            assertThat(sets).hasSize(3)
            assertThat(sets?.get(0)?.rpe).isEqualTo(9f)
            assertThat(sets?.get(1)?.rpe).isEqualTo(7.5f)
            assertThat(sets?.get(2)?.rpe).isEqualTo(6f)
        }

    @Test
    fun parseText_withDuplicateExercisesInJson_mergesSetsCorrectly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme with duplicates",
                    userMaxes = emptyMap(),
                )

            // Simulate OpenAI returning duplicate exercises (which shouldn't happen per rules, but we handle it)
            parser.setMockResponse(
                """
                {
                    "name": "Duplicate Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Test",
                                    "exercises": [
                                        {
                                            "name": "Barbell Squat",
                                            "sets": [
                                                {"reps": 1, "weight": 150},
                                                {"reps": 1, "weight": 160}
                                            ]
                                        },
                                        {
                                            "name": "Barbell Bench Press",
                                            "sets": [{"reps": 8, "weight": 80}]
                                        },
                                        {
                                            "name": "Barbell Squat",
                                            "sets": [
                                                {"reps": 3, "weight": 140},
                                                {"reps": 3, "weight": 140},
                                                {"reps": 3, "weight": 140}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val exercises =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises

            // Should have only 2 unique exercises
            assertThat(exercises).hasSize(2)

            // Find the squat exercise
            val squat = exercises?.find { it.exerciseName == "Barbell Squat" }
            assertThat(squat).isNotNull()

            // Should have all 5 sets merged (2 + 3)
            assertThat(squat?.sets).hasSize(5)
            assertThat(squat?.sets?.get(0)?.weight).isEqualTo(150f)
            assertThat(squat?.sets?.get(1)?.weight).isEqualTo(160f)
            assertThat(squat?.sets?.get(2)?.weight).isEqualTo(140f)
        }

    @Test
    fun parseText_withNullDayValue_handlesCorrectly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme without specific days",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "No Day Programme",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Day 1",
                                    "exercises": [
                                        {"name": "Barbell Squat", "sets": [{"reps": 5, "weight": 100}]}
                                    ]
                                },
                                {
                                    "day": "Monday",
                                    "name": "Day 2",
                                    "exercises": [
                                        {"name": "Barbell Press", "sets": [{"reps": 5, "weight": 60}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val workouts =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
            assertThat(workouts?.get(0)?.dayOfWeek).isNull()
            assertThat(workouts?.get(1)?.dayOfWeek).isEqualTo("Monday")
        }

    // Error Handling Tests
    @Test
    fun parseText_withInvalidJson_returnsError() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Test programme",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse("not valid json {")

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Unable to parse programme. Please check the format and try again.")
        }

    @Test
    fun parseText_withEmptyJsonResponse_returnsError() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Test programme",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse("")

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Programme format couldn't be processed. Try simplifying the text or breaking it into smaller sections.")
        }

    @Test
    fun parseText_withMissingRequiredFields_usesDefaults() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Minimal programme",
                    userMaxes = emptyMap(),
                )

            // Missing name and duration_weeks
            parser.setMockResponse(
                """
                {
                    "weeks": [
                        {
                            "workouts": [
                                {
                                    "exercises": [
                                        {"sets": [{"reps": 5}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            assertThat(result.programme?.name).isEqualTo("Imported Programme")
            assertThat(result.programme?.durationWeeks).isEqualTo(1)

            val exercise =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
                    ?.first()
            assertThat(exercise?.exerciseName).isEqualTo("Unknown Exercise")
        }

    @Test
    fun parseText_withParsingLogic_logsButDoesNotAffectParsing() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme with parsing logic",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "parsing_logic": {
                        "workout_type": "upper",
                        "disambiguation_applied": ["Cable Fly after bench → Cable Fly (chest)"],
                        "set_interpretation": ["3x5 @ 100kg = 3 sets of 5 reps"]
                    },
                    "name": "Logic Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Test",
                                    "exercises": [
                                        {"name": "Cable Fly", "sets": [{"reps": 12, "weight": 15}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            assertThat(result.programme?.name).isEqualTo("Logic Test")
            // Parsing logic should not affect the actual programme structure
            val exercise =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
                    ?.first()
            assertThat(exercise?.exerciseName).isEqualTo("Cable Fly")
        }

    @Test
    fun parseText_withComplexSetNotation_parsesAllSets() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Complex sets",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "Complex Sets",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Test",
                                    "exercises": [
                                        {
                                            "name": "Barbell Squat",
                                            "sets": [
                                                {"reps": 1, "weight": 160, "rpe": 9},
                                                {"reps": 3, "weight": 140, "rpe": 7},
                                                {"reps": 3, "weight": 140, "rpe": 7},
                                                {"reps": 3, "weight": 140, "rpe": 7},
                                                {"reps": 5, "weight": 120, "rpe": 6},
                                                {"reps": 5, "weight": 120, "rpe": 6}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val sets =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
                    ?.first()
                    ?.sets
            assertThat(sets).hasSize(6)

            // Verify the sets maintain order
            assertThat(sets?.get(0)?.reps).isEqualTo(1)
            assertThat(sets?.get(1)?.reps).isEqualTo(3)
            assertThat(sets?.get(4)?.reps).isEqualTo(5)
        }

    @Test
    fun parseText_withFloatWeights_parsesCorrectly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Decimal weights",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "Decimal Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Test",
                                    "exercises": [
                                        {
                                            "name": "Barbell Press",
                                            "sets": [
                                                {"reps": 5, "weight": 42.5},
                                                {"reps": 5, "weight": 47.5},
                                                {"reps": 5, "weight": 52.5}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val sets =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
                    ?.first()
                    ?.sets
            assertThat(sets?.get(0)?.weight).isEqualTo(42.5f)
            assertThat(sets?.get(1)?.weight).isEqualTo(47.5f)
            assertThat(sets?.get(2)?.weight).isEqualTo(52.5f)
        }

    @Test
    fun parseText_withEmptyExerciseList_handlesGracefully() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Empty workout",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "Empty Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Rest Day",
                                    "exercises": []
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val exercises =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
                    ?.exercises
            assertThat(exercises).isEmpty()
        }

    @Test
    fun parseText_withEstimatedDuration_parsesCorrectly() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme with duration",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "Duration Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Quick Workout",
                                    "estimated_duration": 45,
                                    "exercises": [
                                        {"name": "Barbell Squat", "sets": [{"reps": 5, "weight": 100}]}
                                    ]
                                },
                                {
                                    "day": null,
                                    "name": "Long Workout",
                                    "estimated_duration": 90,
                                    "exercises": [
                                        {"name": "Barbell Deadlift", "sets": [{"reps": 5, "weight": 140}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val workouts =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
            assertThat(workouts?.get(0)?.estimatedDurationMinutes).isEqualTo(45)
            assertThat(workouts?.get(1)?.estimatedDurationMinutes).isEqualTo(90)
        }

    @Test
    fun parseText_withMissingEstimatedDuration_usesDefault() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Programme without duration",
                    userMaxes = emptyMap(),
                )

            parser.setMockResponse(
                """
                {
                    "name": "No Duration Test",
                    "duration_weeks": 1,
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": null,
                                    "name": "Workout",
                                    "exercises": [
                                        {"name": "Barbell Squat", "sets": [{"reps": 5, "weight": 100}]}
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
            )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            val workout =
                result.programme
                    ?.weeks
                    ?.first()
                    ?.workouts
                    ?.first()
            assertThat(workout?.estimatedDurationMinutes).isEqualTo(60) // Default value
        }

    // Helper function to create valid programme JSON
    private fun createValidProgrammeJson(): String =
        """
        {
            "name": "Test Programme",
            "duration_weeks": 1,
            "weeks": [
                {
                    "week_number": 1,
                    "workouts": [
                        {
                            "day": null,
                            "name": "Test Workout",
                            "exercises": [
                                {
                                    "name": "Test Exercise",
                                    "sets": [{"reps": 5, "weight": 100}]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
}
