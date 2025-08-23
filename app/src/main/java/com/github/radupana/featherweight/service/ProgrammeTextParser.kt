package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.BuildConfig
import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.data.TextParsingResult
import com.google.gson.JsonParser
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

class ProgrammeTextParser {
    companion object {
        private const val TAG = "ProgrammeTextParser"
        private const val TIMEOUT_SECONDS = 300L // 5 minutes timeout
        private const val SYSTEM_PROMPT = "You are a fitness programme parser. Extract workout programmes into structured JSON format."
    }

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    suspend fun parseText(request: TextParsingRequest): TextParsingResult =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting programme parsing with ${request.rawText.length} characters")

                val validationResult = validateInput(request.rawText)
                if (!validationResult.isValid) {
                    Log.w(TAG, "Validation failed: ${validationResult.error}")
                    return@withContext TextParsingResult(
                        success = false,
                        error = validationResult.error,
                    )
                }

                val parsedJson = callOpenAIAPI(request)

                val programme = parseJsonToProgramme(parsedJson, request.rawText)

                Log.d(TAG, "Programme parsed successfully: ${programme.name} (${programme.durationWeeks} weeks)")

                TextParsingResult(
                    success = true,
                    programme = programme,
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to parse programme: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")

                // Provide user-friendly error messages
                val userFriendlyError =
                    when {
                        e.message?.contains("too complex") == true -> e.message
                        e.message?.contains("Unable to parse") == true -> e.message
                        e.message?.contains("JSON string is blank") == true ->
                            "Programme format couldn't be processed. Try simplifying the text or breaking it into smaller sections."
                        e.message?.contains("Network") == true -> e.message
                        e.message?.contains("timeout") == true ->
                            "Processing took too long. Please try a shorter programme or break it into parts."
                        else -> "Unable to parse programme. Please check the format and try again."
                    }

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            }
        }

    private fun validateInput(text: String): ValidationResult {
        if (text.isBlank()) {
            return ValidationResult(false, "Please provide programme text")
        }

        if (text.length > 10000) {
            return ValidationResult(false, "Programme text is too long. Maximum 10,000 characters")
        }

        return ValidationResult(true)
    }

    private fun callOpenAIAPI(request: TextParsingRequest): String {
        val apiKey = BuildConfig.OPENAI_API_KEY

        val prompt = buildPrompt(request)

        val requestBody =
            JSONObject().apply {
                put("model", "gpt-5-mini")
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "system")
                                put("content", SYSTEM_PROMPT)
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            },
                        )
                    },
                )
                put(
                    "response_format",
                    JSONObject().apply {
                        put("type", "json_object")
                    },
                )
                put("max_completion_tokens", 15000)
            }

        Log.d(TAG, "Calling OpenAI API with model: gpt-5-mini, text length: ${request.rawText.length} chars")

        val httpRequest =
            Request
                .Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body.string()

        Log.d(TAG, "OpenAI API response: ${response.code}, size: ${responseBody.length} chars")

        if (!response.isSuccessful) {
            val errorJson = JSONObject(responseBody)
            val errorMessage =
                errorJson.optJSONObject("error")?.optString("message")
                    ?: "API call failed with status ${response.code}"

            throw IOException(errorMessage)
        }

        val jsonResponse = JSONObject(responseBody)
        return jsonResponse
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun buildPrompt(request: TextParsingRequest): String {
        val maxesInfo =
            buildString {
                request.userMaxes.forEach { (exercise, max) ->
                    appendLine("$exercise: ${max}kg")
                }
            }

        return """
            Parse this workout programme into structured JSON. Analyze the full workout context to disambiguate exercises.
            
            Programme text:
            ${request.rawText}
            
            ${if (maxesInfo.isNotBlank()) "User's 1RM values:\n$maxesInfo" else ""}
            
            CRITICAL PARSING RULES:
            1. ONE ENTRY PER EXERCISE: Create only ONE exercise object per unique exercise name
               - Combine ALL sets for the same exercise into that single object
               - "Squat: 1×1 @9 • 3×3 @8" = ONE Squat with 4 total sets
               - Never create multiple entries for the same exercise
            2. SETS×REPS: "A×B" = Create EXACTLY A separate set objects, each with B reps
               - "3×5 100kg" = 3 sets: [{"reps":5,"weight":100}, {"reps":5,"weight":100}, {"reps":5,"weight":100}]
               - THE FIRST NUMBER IS ALWAYS THE EXACT SET COUNT - NEVER CREATE MORE OR FEWER
            3. REP RANGES: "A×B-C" = Create EXACTLY A sets, each with B reps (use LOWER value)
               - "4×8-10" = 4 sets: [{"reps":8}, {"reps":8}, {"reps":8}, {"reps":8}] ✓
               - "4×8-10" = 2 sets: [{"reps":8}, {"reps":9}] ✗ WRONG - must be 4 sets!
            4. WEIGHT RANGES: Use the LOWER value for all sets (92.5-95kg → all sets at 92.5kg)
            5. RPE RANGES: Use the average (@7-8 = 7.5, @9-10 = 9.5)
            6. PRESERVE SET ORDER: Add sets in EXACT order they appear in the input
               - "1×4 @heavy • 4×8 @light" = first the 1×4 set, THEN the 4×8 sets
               - NEVER reorder, sort, or rearrange sets - maintain input order!
            
            EXERCISE NAME RULES:
            1. Format: [Equipment] [Variation] [Movement]
            2. Keep ALL descriptors: "Paused", "Pin", "Close Grip", "Romanian", "Underhand"
            3. "Paused or Pin" → use first variation
            4. Never duplicate equipment: "Underhand Barbell Row" → "Barbell Underhand Row"
            5. Keep equipment in exercise names: "Cable Fly", "Weighted Dips"
            6. "DB Flat Press" or "Dumbbell Flat Press" → "Dumbbell Bench Press"
            7. Default equipment when unspecified:
               - Squat/Bench/Deadlift/Press/Row/Curls → "Barbell"
               - Raises/Flyes (without Cable) → "Dumbbell"
               - Leg Curl/Extension, Lat Pulldown → "Machine"
            
            CRITICAL: Extract only the actual exercise name:
            - Use your knowledge to identify the standard exercise name without any training context
            - Remove programming descriptors (competition style, volume phase, top singles, back-offs, etc.)
            - Remove tempo/technique modifiers unless they're part of the standard exercise name (keep "Paused" in "Paused Bench Press" but remove "2-count")
            - When multiple exercises are listed together (e.g., "Split Squat / Lunge"), select the single most appropriate exercise
            - Return the clean, standard exercise name that would appear in an exercise database
            - Examples:
              - "Back Squat (comp) – Top single" → "Barbell Back Squat"
              - "Bench Press (volume)" → "Barbell Bench Press"
              - "Paused Squat (2-count)" → "Barbell Paused Squat"
              - "DB Split Squat / Lunge" → "Dumbbell Split Squat"
              - "Back Squat (secondary/high-bar)" → "Barbell High Bar Squat"
            
            EXERCISE DISAMBIGUATION:
            Use workout context to resolve ambiguous exercises:
            1. Identify workout type from compound movements (bench day, squat day, etc.)
            2. Exercise order: compounds → assistance → isolation
            3. After chest work, "Fly" = chest fly; after back work, "Fly" = rear delt
            4. Consider weight: Cable Fly 15-25kg = chest, 5-10kg = rear delts
            5. When ambiguous, match the workout's primary muscle group
            
            STRUCTURE:
            - Week markers (Week 1, Week 2) or Day markers only
            - No markers = single week programme
            - Use null for "day" field when no specific weekday is mentioned (just Day 1, Day 2, etc.)
            
            Output JSON with minimal parsing_logic:
            {
                "parsing_logic": {
                    "workout_type": "chest|back|legs|shoulders|upper|lower|full_body",
                    "disambiguation_applied": ["Cable Fly after bench/dips → Cable Fly (chest)"],
                    "set_interpretation": [
                        "Squat 1×4 105kg @7-8 + 4×8 92.5-95kg @9 = 1 set of 4 reps @105kg, then 4 sets of 8 reps @92.5kg",
                        "DB Press 4×8-10 38kg = 4 sets of 8 reps @38kg (using lower rep value)"
                    ]
                },
                "name": "Programme name",
                "duration_weeks": number,
                "weeks": [
                    {
                        "week_number": number,
                        "workouts": [
                            {
                                "day": "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|null",
                                "name": "Workout name",
                                "exercises": [
                                    {
                                        "name": "Exercise with equipment and variation",
                                        "sets": [{"reps": number, "weight": number, "rpe": number or null}]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
    }

    private fun parseJsonToProgramme(
        jsonString: String,
        rawText: String,
    ): ParsedProgramme {
        require(jsonString.isNotBlank()) { "JSON string is blank" }

        val json = JsonParser.parseString(jsonString).asJsonObject

        // Log parsing_logic if present for debugging
        json.get("parsing_logic")?.asJsonObject?.let { logic ->
            Log.d(TAG, "=== PARSING LOGIC DEBUG ===")
            logic.get("workout_type")?.asString?.let {
                Log.d(TAG, "Workout type detected: $it")
            }
            logic.getAsJsonArray("disambiguation_applied")?.forEach { decision ->
                Log.d(TAG, "Disambiguation: ${decision.asString}")
            }
            logic.getAsJsonArray("set_interpretation")?.forEach { interpretation ->
                Log.d(TAG, "Set interpretation: ${interpretation.asString}")
            }
            Log.d(TAG, "=== END PARSING LOGIC ===")
        }

        val name = json.get("name")?.asString ?: "Imported Programme"
        val durationWeeks = json.get("duration_weeks")?.asInt ?: 1
        // Hardcode these - we don't use them anywhere
        val description = ""
        val programmeType = "GENERAL_FITNESS"
        val difficulty = "INTERMEDIATE"

        val weeks = mutableListOf<ParsedWeek>()
        json.getAsJsonArray("weeks")?.forEach { weekElement ->
            val weekObj = weekElement.asJsonObject
            val weekNumber = weekObj.get("week_number")?.asInt ?: 1
            val weekName = "Week $weekNumber"

            val workouts = mutableListOf<ParsedWorkout>()
            weekObj.getAsJsonArray("workouts")?.forEachIndexed { workoutIndex, workoutElement ->
                val workoutObj = workoutElement.asJsonObject
                // Handle null day - if null or JsonNull, use null (will display as Day 1, Day 2, etc.)
                val dayJson = workoutObj.get("day")
                val day =
                    when {
                        dayJson == null || dayJson.isJsonNull -> null
                        else -> dayJson.asString
                    }
                val workoutName = workoutObj.get("name")?.asString ?: "Workout"
                val estimatedDuration = workoutObj.get("estimated_duration")?.asInt ?: 60

                // Parse exercises and merge duplicates
                val exerciseMap = mutableMapOf<String, MutableList<ParsedSet>>()
                workoutObj.getAsJsonArray("exercises")?.forEach { exerciseElement ->
                    val exerciseObj = exerciseElement.asJsonObject
                    val exerciseName = exerciseObj.get("name")?.asString ?: "Unknown Exercise"

                    val sets = mutableListOf<ParsedSet>()
                    exerciseObj.getAsJsonArray("sets")?.forEach { setElement ->
                        val setObj = setElement.asJsonObject
                        sets.add(
                            ParsedSet(
                                reps = setObj.get("reps")?.asInt,
                                weight = setObj.get("weight")?.asFloat,
                                rpe = setObj.get("rpe")?.asFloat,
                            ),
                        )
                    }

                    // Merge sets if exercise already exists
                    val existingSets = exerciseMap[exerciseName]
                    if (existingSets != null) {
                        existingSets.addAll(sets)
                        Log.w(TAG, "Merging duplicate exercise: $exerciseName (AI created multiple entries)")
                    } else {
                        exerciseMap[exerciseName] = sets
                    }
                }

                // Convert map back to list
                val exercises =
                    exerciseMap.map { (name, sets) ->
                        ParsedExercise(
                            exerciseName = name,
                            sets = sets,
                            notes = null, // We don't use notes
                        )
                    }

                workouts.add(
                    ParsedWorkout(
                        dayOfWeek = day,
                        name = workoutName,
                        exercises = exercises,
                        estimatedDurationMinutes = estimatedDuration,
                    ),
                )
            }

            weeks.add(
                ParsedWeek(
                    weekNumber = weekNumber,
                    name = weekName,
                    description = null,
                    workouts = workouts,
                    focusAreas = null,
                    intensityLevel = null,
                    volumeLevel = null,
                    isDeload = false,
                    phase = null,
                ),
            )
        }

        return ParsedProgramme(
            name = name,
            description = description,
            durationWeeks = durationWeeks,
            programmeType = programmeType,
            difficulty = difficulty,
            weeks = weeks,
            rawText = rawText,
        )
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null,
    )
}
