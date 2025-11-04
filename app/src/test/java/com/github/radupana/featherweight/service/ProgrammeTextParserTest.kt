package com.github.radupana.featherweight.service

import android.content.Context
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ProgrammeTextParserTest {
    private lateinit var parser: ProgrammeTextParser
    private lateinit var mockCloudFunctionService: CloudFunctionService
    private lateinit var mockAuthManager: AuthenticationManager
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        LogMock.setup()

        mockContext = mockk(relaxed = true)
        mockAuthManager = mockk(relaxed = true)
        mockCloudFunctionService = mockk(relaxed = true)
        every { mockAuthManager.isAuthenticated() } returns true
        parser = ProgrammeTextParser(mockContext, mockAuthManager, mockCloudFunctionService)
    }

    @Test
    fun parseText_whenNotAuthenticated_returnsAuthError() =
        runTest {
            every { mockAuthManager.isAuthenticated() } returns false
            val request =
                TextParsingRequest(
                    rawText = "Week 1 Day 1: Squats 3x5",
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Sign in required to use AI programme parsing")
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
            assertThat(result.error).isEqualTo("Text too short. Please provide a complete workout programme.")
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
            assertThat(result.error).isEqualTo("Text too short. Please provide a complete workout programme.")
        }

    @Test
    fun parseText_withTextOver50000Characters_returnsValidationError() =
        runTest {
            val longText = "a".repeat(50001)
            val request =
                TextParsingRequest(
                    rawText = longText,
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Text too long. Please limit to 50,000 characters or split into smaller programmes.")
        }

    @Test
    fun parseText_withSpamText_returnsValidationError() =
        runTest {
            val spamText = "test ".repeat(100) // 100 repetitions of "test"
            val request =
                TextParsingRequest(
                    rawText = spamText.trim(),
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Input appears to be spam or repetitive text. Please provide a real workout programme.")
        }

    @Test
    fun parseText_withMostlyNumbers_returnsValidationError() =
        runTest {
            val numbersText = "123 456 789 0123456789 ".repeat(5)
            val request =
                TextParsingRequest(
                    rawText = numbersText,
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Input needs more exercise descriptions. Include exercise names along with sets and reps.")
        }

    @Test
    fun parseText_withShortValidText_passesValidation() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Squat 3x5", // Exactly 9 chars, but should be rejected as too short (< 10)
                    userMaxes = emptyMap(),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isFalse()
            assertThat(result.error).isEqualTo("Text too short. Please provide a complete workout programme.")
        }

    @Test
    fun parseText_withMinimumValidText_passesValidation() =
        runTest {
            val request =
                TextParsingRequest(
                    rawText = "Squats 3x5", // Exactly 10 chars, minimum valid
                    userMaxes = emptyMap(),
                )

            // Mock the CloudFunctionService to return success
            coEvery {
                mockCloudFunctionService.parseProgram(any(), any())
            } returns
                Result.success(
                    ParsedProgramme(
                        name = "Test Programme",
                        description = "Test",
                        durationWeeks = 1,
                        programmeType = "Test",
                        difficulty = "Beginner",
                        weeks = emptyList(),
                        rawText = "Squats 3x5",
                    ),
                )

            val result = parser.parseText(request)

            assertThat(result.success).isTrue()
            assertThat(result.error).isNull()
            assertThat(result.programme).isNotNull()
        }
}
