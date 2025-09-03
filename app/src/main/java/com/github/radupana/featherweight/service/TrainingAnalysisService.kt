package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.logging.BugfenderLogger
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
     */
    suspend fun analyzeTraining(prompt: String): String =
        withContext(Dispatchers.IO) {
            val correlationId = BugfenderLogger.createCorrelationId("training-analysis")
            BugfenderLogger.i(TAG, "Starting training analysis, correlationId: $correlationId")
            val systemPrompt = "You are an expert strength training coach and sports scientist. Provide analysis in valid JSON format."
            try {
                val result = callOpenAI(systemPrompt, prompt, correlationId)
                BugfenderLogger.i(TAG, "Training analysis completed successfully, correlationId: $correlationId")
                result
            } catch (e: Exception) {
                BugfenderLogger.e(TAG, "Training analysis failed, correlationId: $correlationId", e)
                throw e
            } finally {
                BugfenderLogger.clearCorrelationId("training-analysis")
            }
        }

    private suspend fun callOpenAI(
        systemPrompt: String,
        userPrompt: String,
        correlationId: String? = null,
    ): String =
        withContext(Dispatchers.IO) {
            val remoteConfigService = RemoteConfigService.getInstance()
            val apiKey = remoteConfigService.getOpenAIApiKey()
            if (apiKey.isNullOrEmpty()) {
                BugfenderLogger.e(TAG, "OpenAI API key not available from Remote Config")
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

            BugfenderLogger.logApiRequest(
                tag = TAG,
                url = OPENAI_API_URL,
                method = "POST",
                requestBody = requestBody.toString(),
                headers = mapOf("Content-Type" to "application/json"),
                correlationId = correlationId,
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

            BugfenderLogger.logApiResponse(
                tag = TAG,
                url = OPENAI_API_URL,
                statusCode = response.code,
                responseBody = responseBody,
                responseTime = responseTime,
                correlationId = correlationId,
            )

            if (!response.isSuccessful) {
                val errorJson = JSONObject(responseBody)
                val errorMessage =
                    errorJson.optJSONObject("error")?.optString("message")
                        ?: "API call failed with status ${response.code}"
                BugfenderLogger.e(TAG, "OpenAI API error: $errorMessage, correlationId: $correlationId")
                throw IOException(errorMessage)
            }

            val jsonResponse = JSONObject(responseBody)
            jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
}
