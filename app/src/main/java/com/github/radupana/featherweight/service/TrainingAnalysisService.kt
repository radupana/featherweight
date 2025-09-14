package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.util.AnalyticsLogger
import com.github.radupana.featherweight.util.ExceptionLogger
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
            Log.i(TAG, "Starting training analysis")
            try {
                val result = callOpenAI(prompt)
                Log.i(TAG, "Training analysis completed successfully")
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
            val systemPrompt = "You are an expert strength training coach and sports scientist. Provide analysis in valid JSON format."
            val configService = ConfigServiceFactory.getConfigService()
            val apiKey = configService.getOpenAIApiKey()
            if (apiKey.isNullOrEmpty()) {
                Log.e(TAG, "OpenAI API key not available from Remote Config")
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

            Log.d(TAG, "OpenAI API Request - URL: $OPENAI_API_URL")
            Log.d(TAG, "Request body: $requestBody")

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

            Log.d(TAG, "OpenAI API Response - Status: ${response.code}, Time: ${responseTime}ms")
            Log.d(TAG, "Response body: $responseBody")

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
                Log.e(TAG, "OpenAI API error: $errorMessage")
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
