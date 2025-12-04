package com.github.radupana.featherweight.service

import android.content.Context
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.data.TextParsingResult
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.RateLimitConfig
import com.github.radupana.featherweight.util.RateLimitException
import com.github.radupana.featherweight.util.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

open class ProgrammeTextParser(
    context: Context,
    private val authenticationManager: AuthenticationManager,
    private val cloudFunctionService: CloudFunctionService = CloudFunctionService(),
) {
    private val rateLimiter =
        RateLimiter(
            context = context,
            operationType = "programme_parsing",
            maxRequests = RateLimitConfig.PROGRAMME_PARSING_MAX_REQUESTS,
            windowHours = RateLimitConfig.PROGRAMME_PARSING_WINDOW_HOURS,
        )

    companion object {
        private const val TAG = "ProgrammeTextParser"
    }

    suspend fun parseText(request: TextParsingRequest): TextParsingResult =
        withContext(Dispatchers.IO) {
            try {
                CloudLogger.info(TAG, "Starting programme parsing - text length: ${request.rawText.length}, maxes: ${request.userMaxes.size}")
                executeParsing(request)
            } catch (e: RateLimitException) {
                handleRateLimitException(e)
            } catch (e: IOException) {
                handleIOException(e)
            } catch (e: IllegalArgumentException) {
                handleIllegalArgumentException(e)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                handleUnexpectedException(e)
            }
        }

    private suspend fun executeParsing(request: TextParsingRequest): TextParsingResult {
        rateLimiter.checkLimit()

        val validationResult = validateInput(request.rawText)
        if (!validationResult.isValid) {
            CloudLogger.warn(TAG, "Validation failed: ${validationResult.error}")
            return TextParsingResult(success = false, error = validationResult.error)
        }

        CloudLogger.debug(TAG, "Validation passed, checking authentication...")

        if (!authenticationManager.isAuthenticated()) {
            CloudLogger.warn(TAG, "User not authenticated, cannot use AI parsing")
            return TextParsingResult(success = false, error = "Sign in required to use AI programme parsing")
        }

        CloudLogger.debug(TAG, "Authentication verified, calling Cloud Function...")
        val result = cloudFunctionService.parseProgram(request.rawText, request.userMaxes)
        rateLimiter.recordRequest()

        return processCloudFunctionResult(result)
    }

    private fun processCloudFunctionResult(result: Result<ParsedProgramme>): TextParsingResult {
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            CloudLogger.error(TAG, "Cloud Function call failed: ${exception?.message}")

            if (exception is CloudFunctionService.QuotaExceededException) {
                return TextParsingResult(success = false, error = exception.message ?: "Quota exceeded")
            }

            throw exception ?: IOException("Cloud Function call failed")
        }

        val programme = result.getOrThrow()
        logSuccessfulParse(programme)
        return TextParsingResult(success = true, programme = programme)
    }

    private fun logSuccessfulParse(programme: ParsedProgramme) {
        CloudLogger.debug(TAG, "Received programme from Cloud Function")
        CloudLogger.debug(TAG, "Programme parsed successfully!")
        CloudLogger.debug(TAG, "Programme name: ${programme.name}")
        CloudLogger.debug(TAG, "Duration: ${programme.durationWeeks} weeks")
        CloudLogger.debug(TAG, "Number of weeks: ${programme.weeks.size}")
        programme.weeks.forEachIndexed { weekIdx, week ->
            CloudLogger.debug(TAG, "  Week ${weekIdx + 1}: ${week.workouts.size} workouts")
            week.workouts.forEachIndexed { workoutIdx, workout ->
                CloudLogger.debug(TAG, "    Workout ${workoutIdx + 1}: ${workout.exercises.size} exercises")
            }
        }
    }

    private fun handleRateLimitException(e: RateLimitException): TextParsingResult {
        CloudLogger.warn(TAG, "Rate limit exceeded: ${e.message}")
        return TextParsingResult(success = false, error = e.message)
    }

    private fun handleIOException(e: IOException): TextParsingResult {
        CloudLogger.error(TAG, "=== Programme Parsing FAILED (IOException) ===")
        CloudLogger.error(TAG, "Network or API error: ${e.message}")
        CloudLogger.error(TAG, "Exception type: ${e.javaClass.name}")
        CloudLogger.error(TAG, "Full stack trace:", e)

        val userFriendlyError = mapIOExceptionToUserMessage(e)
        return TextParsingResult(success = false, error = userFriendlyError)
    }

    private fun mapIOExceptionToUserMessage(e: IOException): String =
        when {
            e.message?.contains("timeout") == true -> "Request timed out. Please try again."
            e.message?.contains("Network") == true -> "Network error. Check your connection."
            e.message?.contains("500") == true || e.message?.contains("503") == true ->
                "AI service temporarily unavailable. Please try again in a few moments."
            e.message?.contains("401") == true -> "Authentication failed. Please contact support."
            else -> "Failed to connect to AI service: ${e.message}"
        }

    private fun handleIllegalArgumentException(e: IllegalArgumentException): TextParsingResult {
        CloudLogger.error(TAG, "=== Programme Parsing FAILED (IllegalArgumentException) ===")
        CloudLogger.error(TAG, "Error message: ${e.message}")
        CloudLogger.error(TAG, "Exception type: ${e.javaClass.name}")
        CloudLogger.error(TAG, "Full stack trace:", e)

        val userFriendlyError = mapIllegalArgumentToUserMessage(e)
        return TextParsingResult(success = false, error = userFriendlyError)
    }

    private fun mapIllegalArgumentToUserMessage(e: IllegalArgumentException): String =
        when {
            e.message?.contains("too complex") == true -> e.message ?: "Programme too complex"
            e.message?.contains("Unable to parse") == true -> e.message ?: "Unable to parse"
            e.message?.contains("Invalid content") == true -> e.message ?: "Invalid content"
            else -> "Unable to parse programme${if (e.message != null) ": ${e.message}" else ""}. Please check the format and try again."
        }

    private fun handleUnexpectedException(e: Exception): TextParsingResult {
        CloudLogger.error(TAG, "=== Programme Parsing FAILED (Unexpected Exception) ===")
        CloudLogger.error(TAG, "Exception type: ${e.javaClass.name}")
        CloudLogger.error(TAG, "Error message: ${e.message}")
        CloudLogger.error(TAG, "Full stack trace:", e)

        val userFriendlyError = "An unexpected error occurred: ${e.message ?: "Unknown error"}. Please try again."
        return TextParsingResult(success = false, error = userFriendlyError)
    }

    private fun validateInput(text: String): ValidationResult {
        CloudLogger.debug(TAG, "Validating input text, length: ${text.length}")

        // Check minimum length
        if (text.length < 10) {
            return ValidationResult(
                isValid = false,
                error = "Text too short. Please provide a complete workout programme.",
            )
        }

        // Check maximum length
        if (text.length > 50000) {
            return ValidationResult(
                isValid = false,
                error = "Text too long. Please limit to 50,000 characters or split into smaller programmes.",
            )
        }

        // Check for basic structure issues
        val structureError = checkStructureAndFormat(text)
        if (structureError != null) {
            return ValidationResult(
                isValid = false,
                error = structureError,
            )
        }

        return ValidationResult(isValid = true)
    }

    private fun checkStructureAndFormat(text: String): String? {
        // Check for excessive repetition
        val words = text.split(Regex("\\s+"))
        if (words.size > 3) {
            val wordGroups = words.groupBy { it.lowercase() }
            val maxRepetitions = wordGroups.values.maxOfOrNull { it.size } ?: 0
            val repetitionRatio = maxRepetitions.toFloat() / words.size

            if (repetitionRatio > 0.5) {
                return "Input appears to be spam or repetitive text. Please provide a real workout programme."
            }
        }

        // Check if mostly numbers/special chars
        val alphaCount = text.count { it.isLetter() }
        val alphaRatio = alphaCount.toFloat() / text.length
        if (alphaRatio < 0.3) {
            return "Input needs more exercise descriptions. Include exercise names along with sets and reps."
        }

        return null
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null,
    )
}
