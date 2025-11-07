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
     * Response from the analyzeTraining Cloud Function
     */
    data class AnalyzeTrainingResponse(
        val analysis: Map<String, Any>,
        val quota: AnalysisQuotaInfo,
    )

    /**
     * Analysis quota information (monthly only)
     */
    data class AnalysisQuotaInfo(
        val remaining: AnalysisQuotaRemaining,
    )

    /**
     * Remaining analysis quota (monthly only)
     */
    data class AnalysisQuotaRemaining(
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

            // Check for null response
            if (resultData == null) {
                CloudLogger.error(TAG, "Null response from Cloud Function")
                return Result.failure(
                    Exception("No response from server. Please try again."),
                )
            }

            // Parse response
            val responseJson = gson.toJson(resultData)
            CloudLogger.debug(TAG, "Cloud Function response received, size: ${responseJson.length} chars")

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
        } catch (e: com.google.gson.JsonSyntaxException) {
            ExceptionLogger.logException(TAG, "JSON parsing error in Cloud Function response", e)
            Result.failure(
                Exception("Failed to parse programme response. Please try again."),
            )
        } catch (e: IllegalStateException) {
            ExceptionLogger.logException(TAG, "Unexpected state error calling Cloud Function", e)
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
     * Call the analyzeTraining Cloud Function
     * @param trainingData JSON string of training data
     * @return Analysis result and remaining quotas
     */
    suspend fun analyzeTraining(trainingData: String): Result<AnalyzeTrainingResponse> {
        CloudLogger.debug(TAG, "Calling analyzeTraining Cloud Function")
        CloudLogger.debug(TAG, "Training data length: ${trainingData.length}")

        return try {
            val data = hashMapOf<String, Any>("trainingData" to trainingData)

            val resultData = functionCaller.call("analyzeTraining", data)

            if (resultData == null) {
                CloudLogger.error(TAG, "Null response from analyzeTraining Cloud Function")
                return Result.failure(
                    Exception("No response from server. Please try again."),
                )
            }

            val responseJson = gson.toJson(resultData)
            val response = gson.fromJson(responseJson, AnalyzeTrainingResponse::class.java)

            CloudLogger.info(
                TAG,
                "Analysis successful. Remaining monthly quota: ${response.quota.remaining.monthly}",
            )

            Result.success(response)
        } catch (e: FirebaseFunctionsException) {
            handleAnalysisCloudFunctionError(e)
        } catch (e: com.google.gson.JsonSyntaxException) {
            ExceptionLogger.logException(TAG, "JSON parsing error in analyzeTraining response", e)
            Result.failure(
                Exception("Failed to parse analysis response. Please try again."),
            )
        } catch (e: IllegalStateException) {
            ExceptionLogger.logException(TAG, "Unexpected state error calling analyzeTraining", e)
            Result.failure(
                Exception("Failed to analyze training. Please try again."),
            )
        }
    }

    private fun handleAnalysisCloudFunctionError(e: FirebaseFunctionsException): Result<AnalyzeTrainingResponse> {
        CloudLogger.error(TAG, "Cloud Function error: ${e.code} - ${e.message}")

        return when (e.code) {
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                val details = e.details as? Map<*, *>
                val remaining = details?.get("remaining") as? Map<*, *>

                val quotaInfo =
                    if (remaining != null) {
                        val monthly = (remaining["monthly"] as? Number)?.toInt() ?: 0
                        AnalysisQuotaRemaining(monthly)
                    } else {
                        AnalysisQuotaRemaining(0)
                    }

                CloudLogger.info(TAG, "Analysis quota exceeded")
                Result.failure(
                    AnalysisQuotaExceededException(
                        e.message ?: "Analysis quota exceeded",
                        quotaInfo,
                    ),
                )
            }

            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                CloudLogger.error(TAG, "Authentication failed")
                Result.failure(
                    Exception("Sign in required to use AI training analysis."),
                )
            }

            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                CloudLogger.warn(TAG, "Invalid argument: ${e.message}")
                Result.failure(
                    Exception(e.message ?: "Invalid training data provided."),
                )
            }

            else -> {
                CloudLogger.error(TAG, "Unknown error code: ${e.code}")
                Result.failure(
                    Exception("Failed to analyze training. Please try again."),
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

    /**
     * Exception thrown when analysis quota is exceeded
     */
    class AnalysisQuotaExceededException(
        message: String,
        val remainingQuota: AnalysisQuotaRemaining,
    ) : Exception(message)
}
