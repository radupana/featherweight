package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.AnalyticsLogger
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.PromptSecurityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for analyzing training data using OpenAI API.
 * This is the only AI functionality retained after removing programme generation.
 */
class TrainingAnalysisService {
    companion object {
        private const val TAG = "TrainingAnalysisService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-5-mini" // DO NOT CHANGE as per CLAUDE.md
        private const val MAX_COMPLETION_TOKENS = 8000
    }

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    /**
     * Analyzes training data and returns insights in JSON format.
     * Includes security measures to prevent prompt injection attacks.
     */
    suspend fun analyzeTraining(prompt: String): String =
        withContext(Dispatchers.IO) {
            CloudLogger.info(TAG, "Starting training analysis")

            // Security check for prompt injection attempts
            if (PromptSecurityUtil.detectInjectionAttempt(prompt)) {
                PromptSecurityUtil.logSecurityIncident(
                    "training_analysis_injection",
                    prompt,
                )
                CloudLogger.warn(TAG, "Potential injection attempt detected, rejecting request")
                throw IllegalArgumentException("Invalid input detected. Please provide valid workout data.")
            }

            try {
                val sanitizedPrompt = PromptSecurityUtil.sanitizeInput(prompt)
                val result = callOpenAI(sanitizedPrompt)

                // Validate response structure
                val expectedFields = listOf("overall_assessment", "key_insights", "recommendations")
                if (!PromptSecurityUtil.validateJsonResponse(result, expectedFields)) {
                    CloudLogger.error(TAG, "Invalid response structure from AI")
                    throw IllegalStateException("Received invalid response format from AI service")
                }

                CloudLogger.info(TAG, "Training analysis completed successfully")
                result
            } catch (e: IOException) {
                ExceptionLogger.logException(TAG, "Training analysis failed - Network error", e)
                throw e
            } catch (e: IllegalArgumentException) {
                ExceptionLogger.logException(TAG, "Training analysis failed - Invalid argument", e)
                throw e
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException(TAG, "Training analysis failed - Invalid state", e)
                throw e
            }
        }

    private suspend fun callOpenAI(userPrompt: String): String =
        withContext(Dispatchers.IO) {
            val baseSystemPrompt = "You are an expert strength training coach and sports scientist. Provide analysis in valid JSON format."
            val systemPrompt = PromptSecurityUtil.createDefensiveSystemPrompt(baseSystemPrompt)
            val configService = ConfigServiceFactory.getConfigService()
            val apiKey = configService.getOpenAIApiKey()
            if (apiKey.isNullOrEmpty()) {
                CloudLogger.error(TAG, "OpenAI API key not available from Remote Config")
                error("OpenAI API key not configured. Please check your internet connection and try again.")
            }

            val messages =
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        },
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        },
                    )
                }

            val requestBody =
                JSONObject().apply {
                    put("model", MODEL)
                    put("messages", messages)
                    put("max_completion_tokens", MAX_COMPLETION_TOKENS)
                    put(
                        "response_format",
                        JSONObject().apply {
                            put("type", "json_object")
                        },
                    )
                }

            CloudLogger.debug(TAG, "OpenAI API Request - URL: $OPENAI_API_URL")
            CloudLogger.debug(TAG, "Request body: $requestBody")

            AnalyticsLogger.logOpenAIRequest(
                endpoint = OPENAI_API_URL,
                model = MODEL,
                requestBody = requestBody.toString(),
            )

            val request =
                Request
                    .Builder()
                    .url(OPENAI_API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

            val startTime = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            val responseBody = response.body.string()

            CloudLogger.debug(TAG, "OpenAI API Response - Status: ${response.code}, Time: ${responseTime}ms")
            CloudLogger.debug(TAG, "Response body: $responseBody")

            AnalyticsLogger.logOpenAIResponse(
                endpoint = OPENAI_API_URL,
                statusCode = response.code,
                responseBody = responseBody,
                responseTimeMs = responseTime,
            )

            if (!response.isSuccessful) {
                val errorJson = JSONObject(responseBody)
                val errorMessage =
                    errorJson.optJSONObject("error")?.optString("message")
                        ?: "API call failed with status ${response.code}"
                CloudLogger.error(TAG, "OpenAI API error: $errorMessage")
                AnalyticsLogger.logOpenAIResponse(
                    endpoint = OPENAI_API_URL,
                    statusCode = response.code,
                    responseBody = null,
                    responseTimeMs = responseTime,
                    error = errorMessage,
                )
                throw IOException(errorMessage)
            }

            val jsonResponse = JSONObject(responseBody)
            val usage = jsonResponse.optJSONObject("usage")
            usage?.let {
                AnalyticsLogger.logOpenAITokenUsage(
                    promptTokens = it.optInt("prompt_tokens", 0),
                    completionTokens = it.optInt("completion_tokens", 0),
                    totalTokens = it.optInt("total_tokens", 0),
                )
            }

            jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
}
