package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.testutil.LogMock
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CloudFunctionService using the CloudFunctionCaller abstraction
 */
class CloudFunctionServiceTest2 {
    private lateinit var service: CloudFunctionService
    private lateinit var mockFunctionCaller: CloudFunctionCaller

    @Before
    fun setup() {
        LogMock.setup()
        mockFunctionCaller = mockk()
        service = CloudFunctionService(mockFunctionCaller)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `parseProgram successfully parses programme`() =
        runTest {
            // Arrange
            val rawText = "Squat 3x5 @ 100kg"
            val userMaxes = mapOf("Squat" to 150f, "Bench Press" to 100f)

            val responseData =
                mapOf(
                    "programme" to
                        mapOf(
                            "name" to "Test Programme",
                            "description" to "Test Description",
                            "durationWeeks" to 1,
                            "programmeType" to "Strength",
                            "difficulty" to "Beginner",
                            "weeks" to
                                listOf(
                                    mapOf(
                                        "weekNumber" to 1,
                                        "name" to "Week 1",
                                        "workouts" to
                                            listOf(
                                                mapOf(
                                                    "dayOfWeek" to null,
                                                    "name" to "Day 1",
                                                    "exercises" to
                                                        listOf(
                                                            mapOf(
                                                                "exerciseName" to "Barbell Squat",
                                                                "sets" to
                                                                    listOf(
                                                                        mapOf("reps" to 5, "weight" to 100.0),
                                                                        mapOf("reps" to 5, "weight" to 100.0),
                                                                        mapOf("reps" to 5, "weight" to 100.0),
                                                                    ),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                            "rawText" to rawText,
                        ),
                    "quota" to
                        mapOf(
                            "remaining" to
                                mapOf(
                                    "daily" to 19,
                                    "weekly" to 99,
                                    "monthly" to 199,
                                ),
                            "isAnonymous" to false,
                        ),
                )

            coEvery { mockFunctionCaller.call(any(), any()) } returns responseData

            // Act
            val result = service.parseProgram(rawText, userMaxes)

            // Assert
            assertTrue(result.isSuccess)
            val programme = result.getOrNull()
            assertEquals("Test Programme", programme?.name)
            assertEquals(1, programme?.durationWeeks)
            assertEquals(1, programme?.weeks?.size)
        }

    @Test
    fun `parseProgram handles null response`() =
        runTest {
            // Arrange
            val rawText = "Squat 3x5 @ 100kg"

            coEvery { mockFunctionCaller.call(any(), any()) } returns null

            // Act
            val result = service.parseProgram(rawText, null)

            // Assert
            // Null response should be handled gracefully
            assertTrue(result.isFailure)
        }
}
