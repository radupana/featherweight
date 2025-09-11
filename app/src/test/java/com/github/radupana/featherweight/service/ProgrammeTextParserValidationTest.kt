package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for validation logic in ProgrammeTextParser.
 * These tests verify the actual client-side validation before API calls.
 */
class ProgrammeTextParserValidationTest {
    private lateinit var parser: TestProgrammeTextParser

    private inner class TestProgrammeTextParser : ProgrammeTextParser() {
        override suspend fun callOpenAIAPI(request: TextParsingRequest): String {
            // Return a properly formatted response that parseResult() expects
            return """
                {
                    "name": "Test Programme",
                    "exercises": [
                        {
                            "name": "Squat",
                            "sets": [
                                {
                                    "reps": 5,
                                    "weight": 100.0,
                                    "rpe": null,
                                    "percentage": null
                                }
                            ]
                        }
                    ],
                    "weeks": [
                        {
                            "week_number": 1,
                            "workouts": [
                                {
                                    "day": 1,
                                    "exercises": [
                                        {
                                            "name": "Squat",
                                            "sets": [
                                                {
                                                    "reps": 5,
                                                    "weight": 100.0,
                                                    "rpe": null
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    "estimated_duration": 45
                }
            """
        }
    }

    @Before
    fun setUp() {
        LogMock.setup()
        parser = TestProgrammeTextParser()
    }

    @Test
    fun `text under 50 characters returns validation error`() =
        runTest {
            // Given
            val shortText = "Day 1: Squat 3x5"
            val request =
                TextParsingRequest(
                    rawText = shortText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("too short")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `text with exactly 50 characters passes length validation`() =
        runTest {
            // Given
            val text = "Day 1: Squat 3x5x100kg, Bench 3x5x80kg, Row 3x5x60"
            assertThat(text.length).isEqualTo(50)

            val request =
                TextParsingRequest(
                    rawText = text,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then - Should pass and call API
            assertThat(result.success).isTrue()
            assertThat(result.programme).isNotNull()
            assertThat(result.programme?.name).isEqualTo("Test Programme")
        }

    @Test
    fun `text with profanity returns validation error`() =
        runTest {
            // Given
            val profaneText = "Day 1: Do some fucking squats with 100kg for 3 sets of 5 reps"
            val request =
                TextParsingRequest(
                    rawText = profaneText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("serious workout programme")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `text without workout keywords returns validation error`() =
        runTest {
            // Given
            val nonWorkoutText = "This is just a random story about my day. I went to the store and bought groceries today."
            val request =
                TextParsingRequest(
                    rawText = nonWorkoutText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("workout")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `single line text now passes validation after removing multi-line requirement`() =
        runTest {
            // Given - single line workout should now be valid!
            val singleLine = "Do squats bench press deadlifts rows and pullups all in one day today"
            val request =
                TextParsingRequest(
                    rawText = singleLine,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then - Should pass now that we removed the stupid multi-line requirement
            assertThat(result.success).isTrue()
            assertThat(result.programme).isNotNull()
        }

    @Test
    fun `valid workout text passes all validation and calls API`() =
        runTest {
            // Given
            val validText =
                """
                Week 1 - Strength Programme
                
                Monday:
                Squat: 3 sets of 5 reps at 100kg
                Bench Press: 3 sets of 5 reps at 80kg
                Barbell Row: 3 sets of 8 reps at 60kg
                
                Wednesday:
                Deadlift: 1 set of 5 reps at 120kg
                Overhead Press: 3 sets of 5 reps at 50kg
                Pull-ups: 3 sets of 8 reps
                """.trimIndent()

            val request =
                TextParsingRequest(
                    rawText = validText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then - Should pass validation and return parsed programme
            assertThat(result.success).isTrue()
            assertThat(result.error).isNull()
            assertThat(result.programme).isNotNull()
            assertThat(result.programme?.name).isEqualTo("Test Programme")
        }

    @Test
    fun `minimal 2-exercise workout passes validation`() =
        runTest {
            // Given - Test that 1-2 exercise workouts are valid
            val minimalWorkout =
                """
                Workout:
                Squat: 3 sets of 3 reps at 90kg
                Bench: 5 sets of 8 reps at RPE 9
                """.trimIndent()

            val request =
                TextParsingRequest(
                    rawText = minimalWorkout,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then - Should pass (not "NO identifiable exercises")
            assertThat(result.success).isTrue()
            assertThat(result.programme).isNotNull()
        }

    @Test
    fun `text with mixed case workout keywords passes validation`() =
        runTest {
            // Given
            val mixedCaseText =
                """
                WEEK 1 - Upper/Lower Split
                
                Day 1 - UPPER:
                BENCH press: 4x8 @ 75kg
                barbell ROW: 4x10
                overhead PRESS: 3x8
                """.trimIndent()

            val request =
                TextParsingRequest(
                    rawText = mixedCaseText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then - Should find workout keywords regardless of case
            assertThat(result.success).isTrue()
            assertThat(result.programme).isNotNull()
        }

    @Test
    fun `numbers only text returns validation error`() =
        runTest {
            // Given
            val numbersOnly = "3x5 100 4x8 80 5x10 60 3x12 40 2x15 30 1x20 20"
            val request =
                TextParsingRequest(
                    rawText = numbersOnly,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("workout")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `text over 10000 characters returns validation error`() =
        runTest {
            // Given
            val longText = "a".repeat(10001)
            val request =
                TextParsingRequest(
                    rawText = longText,
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("too long")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `empty text returns validation error`() =
        runTest {
            // Given
            val request =
                TextParsingRequest(
                    rawText = "",
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("provide programme text")
            assertThat(result.programme).isNull()
        }

    @Test
    fun `whitespace only text returns validation error`() =
        runTest {
            // Given
            val request =
                TextParsingRequest(
                    rawText = "   \n  \t  ",
                    userMaxes = emptyMap(),
                )

            // When
            val result = parser.parseText(request)

            // Then
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("provide programme text")
            assertThat(result.programme).isNull()
        }
}
