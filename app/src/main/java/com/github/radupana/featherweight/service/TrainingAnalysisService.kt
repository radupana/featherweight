package com.github.radupana.featherweight.service

import java.io.IOException

import com.github.radupana.featherweight.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for analyzing training data using OpenAI API.
 * This is the only AI functionality retained after removing programme generation.
 */
class TrainingAnalysisService {
    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-5-mini" // DO NOT CHANGE as per CLAUDE.md
        private const val MAX_TOKENS = 4096
        private const val TEMPERATURE = 0.7
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes training data and returns insights in JSON format.
     */
    suspend fun analyzeTraining(prompt: String): String =
        withContext(Dispatchers.IO) {
            val systemPrompt = "You are an expert strength training coach and sports scientist. Provide analysis in valid JSON format."
            callOpenAI(systemPrompt, prompt)
        }

    private suspend fun callOpenAI(systemPrompt: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.OPENAI_API_KEY
            if (apiKey.isNullOrEmpty() || apiKey == "your-api-key-here") {
                error("OpenAI API key not configured")
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", messages)
                put("max_tokens", MAX_TOKENS)
                put("temperature", TEMPERATURE)
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }

            val request = Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")

            if (!response.isSuccessful) {
                val errorJson = JSONObject(responseBody)
                val errorMessage = errorJson.optJSONObject("error")?.optString("message") 
                    ?: "API call failed with status ${response.code}"
                throw IOException(errorMessage)
            }

            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
}
