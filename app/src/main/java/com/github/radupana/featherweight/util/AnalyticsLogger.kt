package com.github.radupana.featherweight.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONException

object AnalyticsLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun logOpenAIRequest(
        endpoint: String,
        model: String,
        requestBody: String,
    ) {
        try {
            crashlytics.log("OPENAI_REQUEST")
            crashlytics.log("Endpoint: $endpoint")
            crashlytics.log("Model: $model")
            crashlytics.log("Request size: ${requestBody.length} chars")
            crashlytics.setCustomKey("openai_last_endpoint", endpoint)
            crashlytics.setCustomKey("openai_last_model", model)
            crashlytics.setCustomKey("openai_last_request_size", requestBody.length)
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
                val responseSize = responseBody?.length ?: 0
                crashlytics.log("Response size: $responseSize chars")
                crashlytics.setCustomKey("openai_last_response_size", responseSize)
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
}
