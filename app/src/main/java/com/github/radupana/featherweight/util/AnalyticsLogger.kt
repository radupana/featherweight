package com.github.radupana.featherweight.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONException
import org.json.JSONObject

object AnalyticsLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun logOpenAIRequest(
        endpoint: String,
        model: String,
        requestBody: String,
        userId: String? = null,
    ) {
        try {
            val sanitizedRequest = sanitizeRequest(requestBody)

            crashlytics.log("OPENAI_REQUEST")
            crashlytics.log("Endpoint: $endpoint")
            crashlytics.log("Model: $model")
            crashlytics.log("User: ${userId ?: "anonymous"}")
            crashlytics.log("Request: $sanitizedRequest")
            crashlytics.setCustomKey("openai_last_endpoint", endpoint)
            crashlytics.setCustomKey("openai_last_model", model)
            userId?.let { crashlytics.setUserId(it) }
        } catch (e: JSONException) {
            crashlytics.log("Failed to log OpenAI request: ${e.message}")
        }
    }

    fun logOpenAIResponse(
        endpoint: String,
        statusCode: Int,
        responseBody: String?,
        responseTimeMs: Long,
        error: String? = null,
    ) {
        try {
            crashlytics.log("OPENAI_RESPONSE")
            crashlytics.log("Endpoint: $endpoint")
            crashlytics.log("Status: $statusCode")
            crashlytics.log("Response Time: ${responseTimeMs}ms")

            if (error != null) {
                crashlytics.log("Error: $error")
                crashlytics.setCustomKey("openai_last_error", error)
            } else {
                val sanitizedResponse = sanitizeResponse(responseBody)
                crashlytics.log("Response: $sanitizedResponse")
            }

            crashlytics.setCustomKey("openai_last_status", statusCode)
            crashlytics.setCustomKey("openai_last_response_time_ms", responseTimeMs)
        } catch (e: JSONException) {
            crashlytics.log("Failed to log OpenAI response: ${e.message}")
        }
    }

    fun logOpenAITokenUsage(
        promptTokens: Int,
        completionTokens: Int,
        totalTokens: Int,
    ) {
        crashlytics.log("OPENAI_TOKENS")
        crashlytics.log("Prompt Tokens: $promptTokens")
        crashlytics.log("Completion Tokens: $completionTokens")
        crashlytics.log("Total Tokens: $totalTokens")
        crashlytics.setCustomKey("openai_last_total_tokens", totalTokens)
    }

    private fun sanitizeRequest(request: String): String =
        try {
            val json = JSONObject(request)
            json.remove("api_key")
            json.toString()
        } catch (e: JSONException) {
            CloudLogger.debug("AnalyticsLogger", "Failed to parse request as JSON for sanitization: ${e.message}")
            request
        }

    private fun sanitizeResponse(response: String?): String = response ?: "null"
}
