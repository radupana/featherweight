package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

data class AIProgrammeRequest(
    val userInput: String,
    val exerciseDatabase: List<String>,
    val maxDays: Int = 7,
    val maxWeeks: Int = 12
)

data class AIProgrammeResponse(
    val success: Boolean,
    val programme: GeneratedProgramme? = null,
    val clarificationNeeded: String? = null,
    val error: String? = null
)

data class GeneratedProgramme(
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val workouts: List<GeneratedWorkout>
)

data class GeneratedWorkout(
    val dayNumber: Int,
    val name: String,
    val exercises: List<GeneratedExercise>
)

data class GeneratedExercise(
    val exerciseName: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val rpe: Float? = null,
    val restSeconds: Int = 180,
    val notes: String? = null
)

class AIProgrammeService {
    companion object {
        private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY
        private const val BASE_URL = "https://api.openai.com/"
        private const val MODEL = "gpt-4o-mini"
        private const val MAX_TOKENS = 16384 // gpt-4o-mini max tokens
        private const val TEMPERATURE = 0.7
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true // This ensures default values are serialized
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
        
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val api = retrofit.create(OpenAIApi::class.java)
    
    // Fallback to mock for development/testing
    private val useMockFallback = OPENAI_API_KEY == "YOUR_API_KEY_HERE"
    
    suspend fun generateProgramme(request: AIProgrammeRequest): AIProgrammeResponse {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = buildSystemPrompt(request)
                val userPrompt = request.userInput
                
                // Log token estimation for cost tracking
                val estimatedTokens = estimateTokens(systemPrompt + userPrompt)
                println("Estimated tokens for request: $estimatedTokens")
                
                val response = callOpenAI(systemPrompt, userPrompt)
                val parsedResponse = parseAIResponse(response)
                
                // Log successful generation for analytics
                if (parsedResponse.success) {
                    println("Programme generation successful")
                }
                
                parsedResponse
            } catch (e: Exception) {
                println("Programme generation failed: ${e.message}")
                AIProgrammeResponse(
                    success = false,
                    error = "Failed to generate programme: ${e.message}"
                )
            }
        }
    }
    
    private fun buildSystemPrompt(request: AIProgrammeRequest): String {
        return """
            You are an elite strength and conditioning coach with 20+ years of experience creating personalized training programmes. Your goal is to create safe, effective, and engaging programmes tailored to each individual.

            AVAILABLE EXERCISES (use ONLY these exact names):
            ${request.exerciseDatabase.joinToString(", ")}

            PROGRAMME DESIGN PRINCIPLES:
            1. Safety First: Ensure proper exercise selection based on experience level
            2. Progressive Overload: Include logical progression from week to week
            3. Volume Balance: Respect muscle group volume guidelines (10-20 sets per muscle per week)
            4. Recovery: Allow adequate rest between sessions (48-72 hours for same muscles)
            5. Movement Patterns: Balance push/pull, squat/hinge, unilateral/bilateral

            CONSTRAINTS:
            - Maximum ${request.maxDays} training days per week
            - Programme duration: 4-${request.maxWeeks} weeks
            - Use only exercises from the provided list (exact names required)
            - If user mentions an exercise not in the list, find the closest functional match

            EXERCISE SELECTION GUIDELINES:
            - Compound movements first (Squat, Deadlift, Bench Press, Row)
            - Include unilateral work for balance
            - Add isolation work based on goals and time available
            - Consider equipment availability implied by user input

            REP AND SET GUIDELINES:
            - Strength: 1-6 reps, 3-6 sets, RPE 7-9, rest 3-5 minutes
            - Hypertrophy: 6-15 reps, 3-5 sets, RPE 6-8, rest 2-4 minutes
            - Endurance: 12+ reps, 2-4 sets, RPE 5-7, rest 1-3 minutes
            - Power: 1-5 reps, 3-6 sets, RPE 6-8, rest 3-5 minutes

            OUTPUT FORMAT:
            Return a JSON object with this exact structure:
            {
                "name": "Programme name (engaging and descriptive)",
                "description": "2-3 sentence description explaining the programme's focus and benefits",
                "durationWeeks": 8,
                "daysPerWeek": 4,
                "workouts": [
                    {
                        "dayNumber": 1,
                        "name": "Day 1 - Upper Power",
                        "exercises": [
                            {
                                "exerciseName": "Barbell Bench Press",
                                "sets": 5,
                                "repsMin": 3,
                                "repsMax": 5,
                                "rpe": 8.0,
                                "restSeconds": 240,
                                "notes": "Focus on explosive concentric movement"
                            }
                        ]
                    }
                ]
            }

            If you need clarification before creating a programme, return:
            {
                "clarificationNeeded": "Specific question about missing information needed to create an optimal programme"
            }

            CRITICAL REQUIREMENTS:
            - Programme must be appropriate for the user's stated experience level
            - Include variety while maintaining focus on the primary goal
            - Ensure exercise names match exactly from the provided list
            - Provide actionable notes for each exercise when beneficial
            - Create a logical weekly structure that promotes adaptation

            Remember: You're creating a programme that could change someone's life. Make it excellent.
        """.trimIndent()
    }
    
    private suspend fun callOpenAI(systemPrompt: String, userPrompt: String): String {
        if (useMockFallback) {
            println("🤖 OpenAI API: Using mock fallback (API key not configured)")
            return getMockResponse()
        }
        
        return try {
            val request = OpenAIRequest(
                model = MODEL,
                messages = listOf(
                    OpenAIMessage(role = "system", content = systemPrompt),
                    OpenAIMessage(role = "user", content = userPrompt)
                ),
                maxTokens = MAX_TOKENS,
                temperature = TEMPERATURE,
                responseFormat = ResponseFormat(type = "json_object")
            )
            
            // Log the complete request
            println("🤖 OpenAI Request [${java.time.LocalDateTime.now()}]:")
            println("📝 Model: $MODEL")
            println("🎯 Temperature: $TEMPERATURE")
            println("📏 Max Tokens: $MAX_TOKENS")
            println("📋 System Prompt (${systemPrompt.length} chars):")
            println("   ${systemPrompt.take(200)}...")
            println("💬 User Prompt (${userPrompt.length} chars):")
            println("   $userPrompt")
            println("🔧 Request JSON:")
            println("   ${kotlinx.serialization.json.Json.encodeToString(OpenAIRequest.serializer(), request)}")
            
            val response = api.createChatCompletion(
                authorization = "Bearer $OPENAI_API_KEY",
                request = request
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.choices?.isNotEmpty() == true) {
                    val responseContent = body.choices[0].message.content
                    
                    // Log the complete response
                    println("✅ OpenAI Response [${java.time.LocalDateTime.now()}]:")
                    println("📊 Usage: ${body.usage?.totalTokens ?: "unknown"} tokens (prompt: ${body.usage?.promptTokens}, completion: ${body.usage?.completionTokens})")
                    println("💰 Estimated cost: ~$${((body.usage?.totalTokens ?: 0) * 0.00015f / 1000f)}")
                    println("📄 Response content (${responseContent.length} chars):")
                    println("   ${responseContent.take(500)}...")
                    println("🔍 Full Response JSON:")
                    println("   ${kotlinx.serialization.json.Json.encodeToString(OpenAIResponse.serializer(), body)}")
                    
                    responseContent
                } else {
                    println("❌ OpenAI Response: Empty choices array")
                    throw Exception("Empty response from OpenAI")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("❌ OpenAI API Error [${response.code()}]: $errorBody")
                throw Exception("OpenAI API error: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            // Log detailed error and fallback to mock
            println("OpenAI API call failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            getMockResponse()
        }
    }
    
    private fun getMockResponse(): String {
        return """
            {
                "name": "AI Strength Programme",
                "description": "A personalized strength training programme designed by AI",
                "durationWeeks": 8,
                "daysPerWeek": 4,
                "workouts": [
                    {
                        "dayNumber": 1,
                        "name": "Day 1 - Upper Power",
                        "exercises": [
                            {
                                "exerciseName": "Barbell Bench Press",
                                "sets": 5,
                                "repsMin": 3,
                                "repsMax": 5,
                                "rpe": 8.0,
                                "restSeconds": 240,
                                "notes": "Focus on explosive movement"
                            },
                            {
                                "exerciseName": "Bent-over Barbell Row",
                                "sets": 4,
                                "repsMin": 6,
                                "repsMax": 8,
                                "rpe": 7.5,
                                "restSeconds": 180,
                                "notes": "Maintain neutral spine"
                            }
                        ]
                    },
                    {
                        "dayNumber": 2,
                        "name": "Day 2 - Lower Power",
                        "exercises": [
                            {
                                "exerciseName": "Barbell Back Squat",
                                "sets": 5,
                                "repsMin": 3,
                                "repsMax": 5,
                                "rpe": 8.0,
                                "restSeconds": 300,
                                "notes": "Full depth, explosive up"
                            },
                            {
                                "exerciseName": "Romanian Deadlift",
                                "sets": 4,
                                "repsMin": 6,
                                "repsMax": 8,
                                "rpe": 7.0,
                                "restSeconds": 180,
                                "notes": "Focus on hip hinge"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }
    
    private fun parseAIResponse(response: String): AIProgrammeResponse {
        return try {
            // Clean response to handle potential formatting issues
            val cleanResponse = response.trim()
            if (cleanResponse.isEmpty()) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Empty response from AI service"
                )
            }
            
            val json = JSONObject(cleanResponse)
            
            // Check if clarification is needed
            if (json.has("clarificationNeeded")) {
                return AIProgrammeResponse(
                    success = true,
                    clarificationNeeded = json.getString("clarificationNeeded")
                )
            }
            
            // Validate required fields
            if (!json.has("name") || !json.has("workouts")) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Invalid response format: missing required fields"
                )
            }
            
            // Parse the programme with validation
            val workouts = parseWorkouts(json.getJSONArray("workouts"))
            if (workouts.isEmpty()) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Programme must contain at least one workout"
                )
            }
            
            val programme = GeneratedProgramme(
                name = json.getString("name"),
                description = json.optString("description", "A personalized training programme"),
                durationWeeks = json.optInt("durationWeeks", 8),
                daysPerWeek = json.optInt("daysPerWeek", workouts.size),
                workouts = workouts
            )
            
            // Validate programme structure
            if (programme.daysPerWeek > 7 || programme.durationWeeks > 52) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Invalid programme parameters: days/week > 7 or duration > 52 weeks"
                )
            }
            
            AIProgrammeResponse(
                success = true,
                programme = programme
            )
        } catch (e: Exception) {
            println("Failed to parse AI response: ${e.message}")
            println("Response content: $response")
            AIProgrammeResponse(
                success = false,
                error = "Failed to parse AI response: ${e.message}"
            )
        }
    }
    
    private fun parseWorkouts(workoutsArray: org.json.JSONArray): List<GeneratedWorkout> {
        val workouts = mutableListOf<GeneratedWorkout>()
        
        for (i in 0 until workoutsArray.length()) {
            try {
                val workoutJson = workoutsArray.getJSONObject(i)
                val exercisesArray = workoutJson.getJSONArray("exercises")
                val exercises = mutableListOf<GeneratedExercise>()
                
                for (j in 0 until exercisesArray.length()) {
                    try {
                        val exerciseJson = exercisesArray.getJSONObject(j)
                        
                        // Validate required exercise fields
                        if (!exerciseJson.has("exerciseName") || !exerciseJson.has("sets")) {
                            println("Skipping exercise due to missing required fields")
                            continue
                        }
                        
                        val sets = exerciseJson.getInt("sets")
                        val repsMin = exerciseJson.optInt("repsMin", 1)
                        val repsMax = exerciseJson.optInt("repsMax", repsMin)
                        
                        // Validate exercise parameters
                        if (sets <= 0 || sets > 20 || repsMin <= 0 || repsMax > 100) {
                            println("Skipping exercise with invalid parameters: sets=$sets, reps=$repsMin-$repsMax")
                            continue
                        }
                        
                        exercises.add(
                            GeneratedExercise(
                                exerciseName = exerciseJson.getString("exerciseName").trim(),
                                sets = sets,
                                repsMin = repsMin,
                                repsMax = repsMax,
                                rpe = if (exerciseJson.has("rpe")) {
                                    exerciseJson.getDouble("rpe").toFloat().coerceIn(1f, 10f)
                                } else null,
                                restSeconds = exerciseJson.optInt("restSeconds", 180).coerceIn(30, 600),
                                notes = exerciseJson.optString("notes", null)?.takeIf { it.isNotBlank() }
                            )
                        )
                    } catch (e: Exception) {
                        println("Failed to parse exercise $j: ${e.message}")
                        continue
                    }
                }
                
                if (exercises.isNotEmpty()) {
                    workouts.add(
                        GeneratedWorkout(
                            dayNumber = workoutJson.optInt("dayNumber", i + 1),
                            name = workoutJson.optString("name", "Day ${i + 1}"),
                            exercises = exercises
                        )
                    )
                }
            } catch (e: Exception) {
                println("Failed to parse workout $i: ${e.message}")
                continue
            }
        }
        
        return workouts
    }
    
    // Simple token estimation for cost tracking
    private fun estimateTokens(text: String): Int {
        // Rough estimation: ~4 characters per token
        return (text.length / 4).coerceAtLeast(1)
    }
}