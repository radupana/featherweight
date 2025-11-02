package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson

/**
 * Service for calling Firebase Cloud Functions
 * Provides secure server-side API access with quota management
 */
class CloudFunctionService(
    private val functionCaller: CloudFunctionCaller = FirebaseCloudFunctionCaller(),
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "CloudFunctionService"
    }

    /**
     * Response from the parseProgram Cloud Function
     */
    data class ParseProgramResponse(
        val programme: ParsedProgramme,
        val quota: QuotaInfo,
    )

    /**
     * Quota information returned from Cloud Function
     */
    data class QuotaInfo(
        val remaining: QuotaRemaining,
        val isAnonymous: Boolean,
    )

    /**
     * Remaining quota counts
     */
    data class QuotaRemaining(
        val daily: Int,
        val weekly: Int,
        val monthly: Int,
    )

    /**
     * Call the parseProgram Cloud Function
     * @param rawText The raw programme text to parse
     * @param userMaxes Optional map of exercise names to 1RM values (in kg)
     * @return ParsedProgramme or throws exception on error
     */
    suspend fun parseProgram(
        rawText: String,
        userMaxes: Map<String, Float>? = null,
    ): Result<ParsedProgramme> {
        CloudLogger.debug(TAG, "Calling parseProgram Cloud Function")
        CloudLogger.debug(TAG, "Text length: ${rawText.length}, Has maxes: ${userMaxes != null}")

        return try {
            // Prepare request data
            val data =
                hashMapOf<String, Any>(
                    "rawText" to rawText,
                ).apply {
                    userMaxes?.let {
                        // Convert Float values to Double for JSON serialization
                        put("userMaxes", it.mapValues { entry -> entry.value.toDouble() })
                    }
                }

            // Call Cloud Function
            val resultData = functionCaller.call("parseProgram", data)

            // Parse response
            val responseJson = gson.toJson(resultData)
            CloudLogger.debug(TAG, "Cloud Function response: $responseJson")

            val response = gson.fromJson(responseJson, ParseProgramResponse::class.java)

            // Log quota information
            CloudLogger.info(
                TAG,
                "Parse successful. Remaining quota - Daily: ${response.quota.remaining.daily}, " +
                    "Weekly: ${response.quota.remaining.weekly}, Monthly: ${response.quota.remaining.monthly}",
            )

            if (response.quota.isAnonymous && response.quota.remaining.daily <= 2) {
                CloudLogger.info(TAG, "Anonymous user approaching daily limit")
            }

            Result.success(response.programme)
        } catch (e: FirebaseFunctionsException) {
            handleCloudFunctionError(e)
        } catch (e: Exception) {
            ExceptionLogger.logException(TAG, "Unexpected error calling Cloud Function", e)
            Result.failure(
                Exception("Failed to parse programme. Please try again."),
            )
        }
    }

    /**
     * Handle errors from Cloud Functions with proper user messaging
     */
    private fun handleCloudFunctionError(e: FirebaseFunctionsException): Result<ParsedProgramme> {
        CloudLogger.error(TAG, "Cloud Function error: ${e.code} - ${e.message}")

        return when (e.code) {
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                // Parse details from the error to determine if user is anonymous
                val details = e.details as? Map<*, *>
                val isAnonymous = details?.get("isAnonymous") as? Boolean ?: false
                val remaining = details?.get("remaining") as? Map<*, *>

                val quotaInfo =
                    if (remaining != null) {
                        val daily = (remaining["daily"] as? Number)?.toInt() ?: 0
                        val weekly = (remaining["weekly"] as? Number)?.toInt() ?: 0
                        val monthly = (remaining["monthly"] as? Number)?.toInt() ?: 0
                        " (Daily: $daily, Weekly: $weekly, Monthly: $monthly remaining)"
                    } else {
                        ""
                    }

                val message =
                    if (isAnonymous) {
                        "You've reached your daily limit (5 parses). " +
                            "Sign in or create an account for 20 daily parses!$quotaInfo"
                    } else {
                        "You've reached your daily limit (20 parses). " +
                            "Quota resets at midnight.$quotaInfo"
                    }

                CloudLogger.info(TAG, "Quota exceeded for user (anonymous: $isAnonymous)")
                Result.failure(QuotaExceededException(message, isAnonymous))
            }

            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                CloudLogger.error(TAG, "Authentication failed")
                Result.failure(
                    Exception("Authentication failed. Please sign in and try again."),
                )
            }

            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                CloudLogger.warn(TAG, "Invalid argument: ${e.message}")
                Result.failure(
                    Exception(
                        e.message
                            ?: "Invalid programme text. Please provide a valid workout programme.",
                    ),
                )
            }

            FirebaseFunctionsException.Code.INTERNAL -> {
                CloudLogger.error(TAG, "Internal server error")
                Result.failure(
                    Exception("Server error. Please try again later."),
                )
            }

            else -> {
                CloudLogger.error(TAG, "Unknown error code: ${e.code}")
                Result.failure(
                    Exception("Failed to parse programme. Please try again."),
                )
            }
        }
    }

    /**
     * Exception thrown when quota is exceeded
     */
    class QuotaExceededException(
        message: String,
        val isAnonymous: Boolean,
    ) : Exception(message)
}
