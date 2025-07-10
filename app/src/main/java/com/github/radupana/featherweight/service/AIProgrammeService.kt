package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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

@Serializable
data class GeneratedProgramme(
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val weeks: List<GeneratedWeek>,
)

@Serializable
data class GeneratedWeek(
    val weekNumber: Int,
    val workouts: List<GeneratedWorkout>,
)

@Serializable
data class GeneratedWorkout(
    val dayNumber: Int,
    val name: String,
    val exercises: List<GeneratedExercise>,
)

@Serializable
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

class AIProgrammeService(
    private val context: android.content.Context,
) {
    companion object {
        private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY
        private const val BASE_URL = "https://api.openai.com/"
        private const val MODEL = "gpt-4.1-mini" // NOTE: This is correct - new model name
        private const val MAX_TOKENS = 32768 // Increased to support 8-week programmes
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
            .readTimeout(300, TimeUnit.SECONDS)
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

    private fun buildSystemPrompt(request: AIProgrammeRequest): String =
        """
        You are an elite strength and conditioning coach with 20+ years of experience creating personalized training programmes. Your goal is to create safe, effective, and engaging programmes tailored to each individual.

        EXERCISE DATABASE LIMITATION:
        Our database contains approximately 500 well-known exercises covering all major movement patterns and equipment types (barbells, dumbbells, cables, bodyweight, machines). Please focus your workout recommendations on commonly used and well-established exercises rather than obscure or highly specialized movements. This ensures all exercises in your programme can be properly tracked and executed.

        EXERCISE NAMING STRUCTURE (FOLLOW EXACTLY):
        Pattern: [Equipment] + [Modifier] + [Movement]
        
        Equipment (ALWAYS first):
        - Barbell, Dumbbell, Cable, Machine, Bodyweight, Band, Kettlebell, Smith Machine
        
        Common Modifiers (optional, between equipment and movement):
        - Position: Seated, Standing, Lying, Kneeling
        - Angle: Incline, Decline, Flat
        - Grip: Close Grip, Wide Grip, Neutral Grip
        - Direction: Front, Back, Overhead, Reverse
        - Unilateral: Single Arm, Single Leg
        
        Movements (base patterns):
        - Press movements: Press, Bench Press, Overhead Press
        - Pull movements: Row, Pulldown, Pull Up, Curl
        - Leg movements: Squat, Deadlift, Lunge, Step Up
        
        CRITICAL RULES:
        ✅ "Dumbbell Step Up" (NOT "Dumbbell Step-Up")
        ✅ "Barbell Row" (NOT "Barbell Bent-Over Row")
        ✅ "Dumbbell Row" (NOT "Dumbbell One-Arm Row")
        ✅ "Bodyweight Push Up" (NOT "Push-Up" or "Pushup")
        ✅ Each word capitalized: "Barbell Bench Press"
        ✅ SINGULAR forms for exercises: "Curl" NOT "Curls", "Row" NOT "Rows", "Raise" NOT "Raises"
        ✅ When muscle names are part of exercise names, use SINGULAR: "Tricep" NOT "Triceps", "Bicep" NOT "Biceps"
        ✅ Examples: "Cable Tricep Pushdown", "Barbell Bicep Curl" (singular in exercise names)
        
        EXAMPLES FROM OUR DATABASE (use these exact names):
        1. "Barbell Back Squat" - [Equipment] + [Modifier] + [Movement]
        2. "Dumbbell Chest Press" - [Equipment] + [Target] + [Movement]
        3. "Cable Lat Pulldown" - [Equipment] + [Target] + [Movement]
        4. "Barbell Romanian Deadlift" - [Equipment] + [Variation] + [Movement]
        5. "Dumbbell Lateral Raise" - [Equipment] + [Direction] + [Movement]
        6. "Machine Leg Press" - [Equipment] + [Target] + [Movement]
        7. "Cable Tricep Pushdown" - [Equipment] + [Muscle] + [Movement] (note: "Tricep" not "Triceps")
        8. "Barbell Bicep Curl" - [Equipment] + [Muscle] + [Movement] (note: "Bicep" not "Biceps", "Curl" not "Curls")
        9. "Dumbbell Single Leg Deadlift" - [Equipment] + [Unilateral] + [Movement]
        10. "Cable Face Pull" - [Equipment] + [Target] + [Movement]

        PROGRAMME DESIGN PRINCIPLES:
        1. Safety First: Ensure proper exercise selection based on experience level
        2. Progressive Overload: Include logical progression from week to week
        3. Volume Balance: Respect muscle group volume guidelines (10-20 sets per muscle per week)
        4. Recovery: Allow adequate rest between sessions (48-72 hours for same muscles)
        5. Movement Patterns: Balance push/pull, squat/hinge, unilateral/bilateral

        CONSTRAINTS:
        - Maximum ${request.maxDays} training days per week
        - Programme duration: EXACTLY 8 weeks (no more, no less)
        - Progressive overload must be built into the 8-week structure

        AVAILABLE EXERCISE SUMMARY:
        - Total exercises: 500
        - Barbell exercises: 89 (Squat, Deadlift, Row, Press variations)
        - Dumbbell exercises: 124 (all major movements)
        - Cable exercises: 67 (pulldowns, rows, crossovers)
        - Machine exercises: 45 (leg press, hack squat, chest press)
        - Bodyweight exercises: 95 (push up, pull up, dip, plank)
        
        EQUIPMENT NOT IN DATABASE:
        - TRX, Bosu Ball, Battle Ropes, Sled
        - Specialized powerlifting equipment
        - Uncommon machine brands
        
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
        CRITICAL: Generate ONLY the fields shown below. DO NOT include week descriptions, intensity levels, volume levels, focus areas, or deload indicators.
        Return a JSON object with this EXACT structure:
        {
            "name": "Programme name (engaging and descriptive)",
            "description": "2-3 sentence description explaining the programme's focus and benefits",
            "durationWeeks": 8,
            "daysPerWeek": 4,
            "weeks": [
                {
                    "weekNumber": 1,
                    "workouts": [
                        {
                            "dayNumber": 1,
                            "name": "Upper Body Power",
                            "exercises": [
                                {
                                    "exerciseName": "Barbell Back Squat",
                                    "sets": 5,
                                    "repsMin": 5,
                                    "repsMax": 5,
                                    "rpe": 7.0,
                                    "restSeconds": 180,
                                    "suggestedWeight": 80.0,
                                    "weightSource": "user_1rm"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
        
        CRITICAL FIELD RESTRICTIONS:
        - weeks: ONLY include "weekNumber" and "workouts" (NO name, description, intensityLevel, volumeLevel, focus, isDeload)
        - workouts: Include "dayNumber", "name", and "exercises" (name should be descriptive, e.g., "Upper Body Power", "Lower Body Hypertrophy")
        - exercises: Include all fields shown above. Notes field is OPTIONAL - only add if critical for safety/form

        If you need clarification before creating a programme, return:
        {
            "clarificationNeeded": "Specific question about missing information needed to create an optimal programme"
        }

        CRITICAL REQUIREMENTS:
        - Always generate EXACTLY 8 weeks of programming
        - Programme must be appropriate for the user's stated experience level
        - Include variety while maintaining focus on the primary goal
        - Exercise notes are OPTIONAL - only include for critical safety/form cues
        - Create progressive overload through the 8 weeks
        - DO NOT include week names, descriptions, intensity/volume levels, or focus areas
        - Keep programme name and description concise (save tokens for exercises)

        Remember: You're creating a programme that could change someone's life. Make it excellent.
        """.trimIndent()

    private suspend fun callOpenAI(
        systemPrompt: String,
        userPrompt: String,
    ): String {
        if (OPENAI_API_KEY == "YOUR_API_KEY_HERE" || OPENAI_API_KEY.isBlank()) {
            println("❌ FATAL: OpenAI API key not configured")
            throw IllegalStateException(
                "AI service is not configured. Please configure your OpenAI API key to use AI programme generation.",
            )
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
            // Log the complete prompt in human-readable format (not JSON)
            println("\n" + "=".repeat(70))
            println("📜 COMPLETE LLM PROMPT (HUMAN READABLE)")
            println("=".repeat(70))
            println("\n🎯 SYSTEM PROMPT:")
            println("-".repeat(70))
            println(systemPrompt)
            println("\n💬 USER PROMPT:")
            println("-".repeat(70))
            println(userPrompt)
            println("\n" + "=".repeat(70) + "\n")

            val response =
                api.createChatCompletion(
                    authorization = "Bearer $OPENAI_API_KEY",
                    request = request,
                )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.choices?.isNotEmpty() == true) {
                    val responseContent = body.choices[0].message.content
                    val finishReason = body.choices[0].finishReason

                    // Log the complete response
                    println("✅ OpenAI Response [${java.time.LocalDateTime.now()}]:")
                    println(
                        "📊 Usage: ${body.usage?.totalTokens ?: "unknown"} tokens (prompt: ${body.usage?.promptTokens}, completion: ${body.usage?.completionTokens})",
                    )
                    println("💰 Estimated cost: ~$${((body.usage?.totalTokens ?: 0) * 0.00015f / 1000f)}")
                    println("📄 Response content length: ${responseContent.length} chars")
                    println("🏁 Finish reason: $finishReason")

                    // Log the FULL response for debugging truncation
                    println("\n" + "=".repeat(70))
                    println("📜 COMPLETE AI RESPONSE")
                    println("=".repeat(70))
                    println(responseContent)
                    println("=".repeat(70) + "\n")

                    // Check if response was truncated
                    if (finishReason == "length") {
                        println("⚠️ WARNING: Response was truncated due to token limit!")
                        throw Exception(
                            "Programme generation incomplete - response was truncated. Please try a shorter programme duration.",
                        )
                    }

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

    fun parseAIProgrammeResponse(jsonResponse: String): AIProgrammeResponse {
        return parseAIResponse(jsonResponse)
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

                val week =
                    GeneratedWeek(
                        weekNumber = weekJson.optInt("weekNumber", i + 1),
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
                                suggestedWeight =
                                    if (exerciseJson.has("suggestedWeight")) {
                                        exerciseJson.getDouble("suggestedWeight").toFloat()
                                    } else {
                                        null
                                    },
                                weightSource = exerciseJson.optString("weightSource", "").takeIf { it.isNotBlank() },
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
                            name = workoutJson.getString("name"),
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
