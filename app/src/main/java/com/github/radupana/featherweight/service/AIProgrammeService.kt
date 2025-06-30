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
    val maxWeeks: Int = 12,
)

data class AIProgrammeResponse(
    val success: Boolean,
    val programme: GeneratedProgramme? = null,
    val clarificationNeeded: String? = null,
    val error: String? = null,
)

data class GeneratedProgramme(
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val weeks: List<GeneratedWeek>,
)

data class GeneratedWeek(
    val weekNumber: Int,
    val name: String,
    val description: String,
    val intensityLevel: String,
    val volumeLevel: String,
    val focus: List<String>,
    val isDeload: Boolean = false,
    val workouts: List<GeneratedWorkout>,
)

data class GeneratedWorkout(
    val dayNumber: Int,
    val name: String,
    val exercises: List<GeneratedExercise>,
)

data class GeneratedExercise(
    val exerciseName: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val rpe: Float? = null,
    val restSeconds: Int = 180,
    val notes: String? = null,
    val suggestedWeight: Float? = null,
    val weightSource: String? = null,
)

class AIProgrammeService(private val context: android.content.Context) {
    companion object {
        private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY
        private const val BASE_URL = "https://api.openai.com/"
        private const val MODEL = "gpt-4.1-mini" // NOTE: This is correct - new model name
        private const val MAX_TOKENS = 32768
        private const val TEMPERATURE = 0.7
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true // This ensures default values are serialized
        }

    private val httpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            ).build()

    private val retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private val api = retrofit.create(OpenAIApi::class.java)


    suspend fun generateProgramme(request: AIProgrammeRequest): AIProgrammeResponse =
        withContext(Dispatchers.IO) {
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
                    error = "Failed to generate programme: ${e.message}",
                )
            }
        }

    private fun buildSystemPrompt(request: AIProgrammeRequest): String {
        
        return """
        You are an elite strength and conditioning coach with 20+ years of experience creating personalized training programmes. Your goal is to create safe, effective, and engaging programmes tailored to each individual.

        EXERCISE DATABASE LIMITATION:
        Our database contains approximately 500 well-known exercises covering all major movement patterns and equipment types (barbells, dumbbells, cables, bodyweight, machines). Please focus your workout recommendations on commonly used and well-established exercises rather than obscure or highly specialized movements. This ensures all exercises in your programme can be properly tracked and executed.

        EXERCISE NAMING RULES (CRITICAL - FOLLOW EXACTLY):
        - Always use long form names: "Barbell Back Squat" not "Squat" or "Back Squat"
        - Equipment first: "Dumbbell Chest Press" not "Chest Press with Dumbbells"
        - Standard equipment names: Barbell, Dumbbell, Cable, Machine, Bodyweight
        - Use these muscle names: Chest, Back, Bicep, Tricep, Quad, Hamstring, Glute, Calf, Shoulder, Core
        
        NAMING PATTERNS:
        - [Equipment] [Target] [Movement]: "Barbell Bicep Curl"
        - [Equipment] [Angle] [Movement]: "Dumbbell Incline Press"  
        - [Equipment] [Variation] [Movement]: "Barbell Romanian Deadlift"
        
        EXAMPLES:
        ✅ Correct: "Barbell Back Squat", "Dumbbell Chest Press", "Cable Lateral Raise"
        ❌ Wrong: "Squats", "DB Press", "Lateral Raises"

        PROGRAMME DESIGN PRINCIPLES:
        1. Safety First: Ensure proper exercise selection based on experience level
        2. Progressive Overload: Include logical progression from week to week
        3. Volume Balance: Respect muscle group volume guidelines (10-20 sets per muscle per week)
        4. Recovery: Allow adequate rest between sessions (48-72 hours for same muscles)
        5. Movement Patterns: Balance push/pull, squat/hinge, unilateral/bilateral

        CONSTRAINTS:
        - Maximum ${request.maxDays} training days per week
        - Programme duration: 4-${request.maxWeeks} weeks
        - intensityLevel options: "low", "moderate", "high", "very_high"
        - volumeLevel options: "low", "moderate", "high", "very_high"

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

        WEIGHT CALCULATION GUIDELINES:
        
        CRITICAL: RESPECT USER SPECIFICATIONS
        - If the user provides EXACT specifications (e.g., "5×5 @ 97.5kg"), use those EXACTLY
        - Do NOT modify sets, reps, or weights when the user is being specific
        - If user provides a structured programme with details, treat it as a template to follow precisely
        
        WHEN TO BE FLEXIBLE:
        - When user is vague (e.g., "I want a strength programme")
        - When user asks for suggestions or variations
        - When creating programmes from general descriptions
        
        WEIGHT ASSIGNMENT PRIORITY:
        1. User-specified exact weights (e.g., "5×5 @ 97.5kg") - USE EXACTLY
        2. User's saved 1RMs (provided below) - calculate percentages
        3. If no 1RMs available, use appropriate weights for average gym-goer based on gender and experience
        
        - Include "suggestedWeight" (in kg) and "weightSource" for each exercise
        - WeightSource options: "user_specified", "user_1rm", "average_estimate"

        OUTPUT FORMAT:
        Return a JSON object with this EXACT hierarchical structure:
        {
            "name": "Programme name (engaging and descriptive)",
            "description": "2-3 sentence description explaining the programme's focus and benefits",
            "durationWeeks": 8,
            "daysPerWeek": 4,
            "weeks": [
                {
                    "weekNumber": 1,
                    "name": "Foundation Phase",
                    "description": "Establishing baseline strength and movement patterns",
                    "intensityLevel": "moderate",
                    "volumeLevel": "moderate",
                    "focus": ["technique", "adaptation"],
                    "isDeload": false,
                    "workouts": [
                        {
                            "dayNumber": 1,
                            "name": "Lower Power",
                            "exercises": [
                                {
                                    "exerciseName": "Back Squat",
                                    "sets": 5,
                                    "repsMin": 5,
                                    "repsMax": 5,
                                    "rpe": 7.0,
                                    "restSeconds": 240,
                                    "notes": "Focus on depth and control",
                                    "suggestedWeight": 80.0,
                                    "weightSource": "user_1rm"
                                }
                            ]
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
        - Provide actionable notes for each exercise when beneficial
        - Create a logical weekly structure that promotes adaptation

        Remember: You're creating a programme that could change someone's life. Make it excellent.
        """.trimIndent()
    }

    private suspend fun callOpenAI(
        systemPrompt: String,
        userPrompt: String,
    ): String {
        if (OPENAI_API_KEY == "YOUR_API_KEY_HERE" || OPENAI_API_KEY.isBlank()) {
            println("❌ FATAL: OpenAI API key not configured")
            throw IllegalStateException("AI service is not configured. Please configure your OpenAI API key to use AI programme generation.")
        }

        return try {
            val request =
                OpenAIRequest(
                    model = MODEL,
                    messages =
                        listOf(
                            OpenAIMessage(role = "system", content = systemPrompt),
                            OpenAIMessage(role = "user", content = userPrompt),
                        ),
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE,
                    responseFormat = ResponseFormat(type = "json_object"),
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

            val response =
                api.createChatCompletion(
                    authorization = "Bearer $OPENAI_API_KEY",
                    request = request,
                )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.choices?.isNotEmpty() == true) {
                    val responseContent = body.choices[0].message.content

                    // Log the complete response
                    println("✅ OpenAI Response [${java.time.LocalDateTime.now()}]:")
                    println(
                        "📊 Usage: ${body.usage?.totalTokens ?: "unknown"} tokens (prompt: ${body.usage?.promptTokens}, completion: ${body.usage?.completionTokens})",
                    )
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
            // Log detailed error and throw exception instead of using mock
            println("OpenAI API call failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            throw Exception("AI service is temporarily unavailable. Please try again later or use a template programme.")
        }
    }

    private fun parseAIResponse(response: String): AIProgrammeResponse {
        return try {
            // Clean response to handle potential formatting issues
            val cleanResponse = response.trim()
            if (cleanResponse.isEmpty()) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Empty response from AI service",
                )
            }

            val json = JSONObject(cleanResponse)

            // Check if clarification is needed
            if (json.has("clarificationNeeded")) {
                return AIProgrammeResponse(
                    success = true,
                    clarificationNeeded = json.getString("clarificationNeeded"),
                )
            }

            // Validate required fields
            if (!json.has("name") || !json.has("weeks")) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Invalid response format: missing required fields",
                )
            }

            // Parse the programme with validation
            val weeks = parseWeeks(json.getJSONArray("weeks"))
            if (weeks.isEmpty()) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Programme must contain at least one week",
                )
            }

            val programme =
                GeneratedProgramme(
                    name = json.getString("name"),
                    description = json.optString("description", "A personalized training programme"),
                    durationWeeks = json.optInt("durationWeeks", weeks.size),
                    daysPerWeek = json.optInt("daysPerWeek", 3),
                    weeks = weeks,
                )

            // Validate programme structure
            if (programme.daysPerWeek > 7 || programme.durationWeeks > 52) {
                return AIProgrammeResponse(
                    success = false,
                    error = "Invalid programme parameters: days/week > 7 or duration > 52 weeks",
                )
            }

            AIProgrammeResponse(
                success = true,
                programme = programme,
            )
        } catch (e: Exception) {
            println("Failed to parse AI response: ${e.message}")
            println("Response content: $response")
            AIProgrammeResponse(
                success = false,
                error = "Failed to parse AI response: ${e.message}",
            )
        }
    }

    private fun parseWeeks(weeksArray: org.json.JSONArray): List<GeneratedWeek> {
        val weeks = mutableListOf<GeneratedWeek>()

        for (i in 0 until weeksArray.length()) {
            try {
                val weekJson = weeksArray.getJSONObject(i)

                // Parse focus as list of strings
                val focusList = mutableListOf<String>()
                if (weekJson.has("focus")) {
                    val focusArray = weekJson.getJSONArray("focus")
                    for (j in 0 until focusArray.length()) {
                        focusList.add(focusArray.getString(j))
                    }
                }

                val week =
                    GeneratedWeek(
                        weekNumber = weekJson.optInt("weekNumber", i + 1),
                        name = weekJson.optString("name", "Week ${i + 1}"),
                        description = weekJson.optString("description", ""),
                        intensityLevel = weekJson.optString("intensityLevel", "moderate"),
                        volumeLevel = weekJson.optString("volumeLevel", "moderate"),
                        focus = focusList,
                        isDeload = weekJson.optBoolean("isDeload", false),
                        workouts = parseWorkouts(weekJson.getJSONArray("workouts")),
                    )
                weeks.add(week)
            } catch (e: Exception) {
                println("Error parsing week ${i + 1}: ${e.message}")
            }
        }

        return weeks
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
                                rpe =
                                    if (exerciseJson.has("rpe")) {
                                        exerciseJson.getDouble("rpe").toFloat().coerceIn(1f, 10f)
                                    } else {
                                        null
                                    },
                                restSeconds = exerciseJson.optInt("restSeconds", 180).coerceIn(30, 600),
                                notes = exerciseJson.optString("notes", "").takeIf { it.isNotBlank() },
                            ),
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
                            exercises = exercises,
                        ),
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
