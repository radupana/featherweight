package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.logging.BugfenderLogger
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.data.TextParsingResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

open class ProgrammeTextParser {
    companion object {
        private const val TAG = "ProgrammeTextParser"
        private const val TIMEOUT_SECONDS = 300L
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
                val correlationId = BugfenderLogger.createCorrelationId("programme-parse")
                BugfenderLogger.i(TAG, "Starting programme parsing - text length: ${request.rawText.length}, maxes: ${request.userMaxes.size}, correlationId: $correlationId")

                val validationResult = validateInput(request.rawText)
                if (!validationResult.isValid) {
                    BugfenderLogger.w(TAG, "Validation failed: ${validationResult.error}")
                    return@withContext TextParsingResult(
                        success = false,
                        error = validationResult.error,
                    )
                }

                BugfenderLogger.d(TAG, "Validation passed, calling OpenAI API...")
                val parsedJson = callOpenAIAPI(request)
                BugfenderLogger.d(TAG, "Received parsed JSON response, length: ${parsedJson.length}")
                BugfenderLogger.d(TAG, "Full parsed JSON:\n$parsedJson")

                BugfenderLogger.d(TAG, "Parsing JSON to programme object...")
                val programme = parseJsonToProgramme(parsedJson, request.rawText)

                BugfenderLogger.d(TAG, "Programme parsed successfully!")
                BugfenderLogger.d(TAG, "Programme name: ${programme.name}")
                BugfenderLogger.d(TAG, "Duration: ${programme.durationWeeks} weeks")
                BugfenderLogger.d(TAG, "Number of weeks: ${programme.weeks.size}")
                programme.weeks.forEachIndexed { weekIdx, week ->
                    BugfenderLogger.d(TAG, "  Week ${weekIdx + 1}: ${week.workouts.size} workouts")
                    week.workouts.forEachIndexed { workoutIdx, workout ->
                        BugfenderLogger.d(TAG, "    Workout ${workoutIdx + 1}: ${workout.exercises.size} exercises")
                    }
                }

                TextParsingResult(
                    success = true,
                    programme = programme,
                )
            } catch (e: IOException) {
                BugfenderLogger.e(TAG, "=== Programme Parsing FAILED (IOException) ===")
                BugfenderLogger.e(TAG, "Network or API error: ${e.message}")
                BugfenderLogger.e(TAG, "Exception type: ${e.javaClass.name}")
                BugfenderLogger.e(TAG, "Full stack trace:", e)

                val userFriendlyError =
                    when {
                        e.message?.contains("API key") == true -> "OpenAI API key not configured"
                        e.message?.contains("timeout") == true -> "Request timed out. Please try again."
                        e.message?.contains("Network") == true -> "Network error. Check your connection."
                        else -> "Failed to connect to AI service: ${e.message}"
                    }

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            } catch (e: IllegalArgumentException) {
                BugfenderLogger.e(TAG, "=== Programme Parsing FAILED (IllegalArgumentException) ===")
                BugfenderLogger.e(TAG, "Error message: ${e.message}")
                BugfenderLogger.e(TAG, "Exception type: ${e.javaClass.name}")
                BugfenderLogger.e(TAG, "Full stack trace:", e)

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
            } catch (e: Exception) {
                BugfenderLogger.e(TAG, "=== Programme Parsing FAILED (Unexpected Exception) ===")
                BugfenderLogger.e(TAG, "Unexpected error type: ${e.javaClass.name}")
                BugfenderLogger.e(TAG, "Error message: ${e.message}")
                BugfenderLogger.e(TAG, "Full stack trace:", e)

                TextParsingResult(
                    success = false,
                    error = "Unexpected error: ${e.message ?: "Unknown error"}",
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

    internal open suspend fun callOpenAIAPI(request: TextParsingRequest, correlationId: String? = null): String {
        val remoteConfigService = RemoteConfigService.getInstance()
        val effectiveApiKey = remoteConfigService.getOpenAIApiKey()

        if (effectiveApiKey.isNullOrEmpty()) {
            BugfenderLogger.e(TAG, "API key not available from Remote Config")
            throw IllegalArgumentException("OpenAI API key not configured. Please check your internet connection and try again.")
        }

        val prompt = buildPrompt(request)
        BugfenderLogger.d(TAG, "=== OpenAI API Request ===")
        BugfenderLogger.d(TAG, "Prompt length: ${prompt.length} characters")
        BugfenderLogger.d(TAG, "Full prompt:\n$prompt")

        val requestBody =
            JsonObject().apply {
                addProperty("model", "gpt-5-mini")

                val messages = JsonArray()
                messages.add(
                    JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", SYSTEM_PROMPT)
                    },
                )
                messages.add(
                    JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    },
                )
                add("messages", messages)

                add(
                    "response_format",
                    JsonObject().apply {
                        addProperty("type", "json_object")
                    },
                )

                addProperty("max_completion_tokens", 15000)
            }

        BugfenderLogger.d(TAG, "Request body JSON:\n$requestBody")
        BugfenderLogger.d(TAG, "Calling OpenAI API with model: gpt-5-mini")

        val httpRequest =
            Request
                .Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $effectiveApiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body.string()

        BugfenderLogger.d(TAG, "=== OpenAI API Response ===")
        BugfenderLogger.d(TAG, "Response code: ${response.code}")
        BugfenderLogger.d(TAG, "Response body size: ${responseBody.length} chars")
        BugfenderLogger.d(TAG, "Full response body:\n$responseBody")

        if (!response.isSuccessful) {
            BugfenderLogger.e(TAG, "API call failed with status ${response.code}")
            val errorJson =
                try {
                    JsonParser.parseString(responseBody).asJsonObject
                } catch (e: Exception) {
                    BugfenderLogger.e(TAG, "Failed to parse error response as JSON", e)
                    null
                }
            val errorMessage =
                errorJson?.getAsJsonObject("error")?.get("message")?.asString
                    ?: "API call failed with status ${response.code}"

            BugfenderLogger.e(TAG, "Error message: $errorMessage")
            throw IOException(errorMessage)
        }

        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        return jsonResponse
            .getAsJsonArray("choices")
            .get(0)
            .asJsonObject
            .getAsJsonObject("message")
            .get("content")
            .asString
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
        BugfenderLogger.d(TAG, "=== parseJsonToProgramme START ===")
        BugfenderLogger.d(TAG, "JSON string length: ${jsonString.length}")

        require(jsonString.isNotBlank()) { "JSON string is blank" }

        val json =
            try {
                JsonParser.parseString(jsonString).asJsonObject
            } catch (e: Exception) {
                BugfenderLogger.e(TAG, "Failed to parse JSON string", e)
                BugfenderLogger.e(TAG, "Invalid JSON content:\n$jsonString")
                throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
            }

        BugfenderLogger.d(TAG, "JSON parsed successfully")
        BugfenderLogger.d(TAG, "Top-level keys: ${json.keySet()}")

        json.get("parsing_logic")?.let { logicElement ->
            if (!logicElement.isJsonNull && logicElement.isJsonObject) {
                val logic = logicElement.asJsonObject
                BugfenderLogger.d(TAG, "=== PARSING LOGIC DEBUG ===")
                logic.get("workout_type")?.let { workoutType ->
                    if (!workoutType.isJsonNull) {
                        BugfenderLogger.d(TAG, "Workout type detected: ${workoutType.asString}")
                    }
                }
                logic.get("disambiguation_applied")?.let { disambig ->
                    if (disambig.isJsonArray) {
                        disambig.asJsonArray.forEach { decision ->
                            BugfenderLogger.d(TAG, "Disambiguation: ${decision.asString}")
                        }
                    }
                }
                logic.get("set_interpretation")?.let { setInterp ->
                    if (setInterp.isJsonArray) {
                        setInterp.asJsonArray.forEach { interpretation ->
                            BugfenderLogger.d(TAG, "Set interpretation: ${interpretation.asString}")
                        }
                    }
                }
                BugfenderLogger.d(TAG, "=== END PARSING LOGIC ===")
            }
        }

        val name = json.get("name")?.asString ?: "Imported Programme"
        val durationWeeks = json.get("duration_weeks")?.asInt ?: 1
        BugfenderLogger.d(TAG, "Programme name: $name, duration: $durationWeeks weeks")
        val description = ""
        val programmeType = "GENERAL_FITNESS"
        val difficulty = "INTERMEDIATE"

        val weeks = mutableListOf<ParsedWeek>()
        val weeksArray = json.getAsJsonArray("weeks")
        BugfenderLogger.d(TAG, "Found ${weeksArray?.size() ?: 0} weeks in JSON")

        weeksArray?.forEach { weekElement ->
            val weekObj = weekElement.asJsonObject
            val weekNumber = weekObj.get("week_number")?.asInt ?: 1
            val weekName = "Week $weekNumber"

            val workouts = mutableListOf<ParsedWorkout>()
            weekObj.getAsJsonArray("workouts")?.forEachIndexed { workoutIndex, workoutElement ->
                val workoutObj = workoutElement.asJsonObject
                val dayJson = workoutObj.get("day")
                val day =
                    when {
                        dayJson == null || dayJson.isJsonNull -> null
                        else -> dayJson.asString
                    }
                val workoutName = workoutObj.get("name")?.asString ?: "Workout"
                val estimatedDuration = workoutObj.get("estimated_duration")?.asInt ?: 60

                val exerciseMap = mutableMapOf<String, MutableList<ParsedSet>>()
                val exercisesArray = workoutObj.getAsJsonArray("exercises")
                BugfenderLogger.d(TAG, "    Workout has ${exercisesArray?.size() ?: 0} exercises")

                exercisesArray?.forEach { exerciseElement ->
                    val exerciseObj = exerciseElement.asJsonObject
                    val exerciseName = exerciseObj.get("name")?.asString ?: "Unknown Exercise"
                    BugfenderLogger.d(TAG, "      Processing exercise: $exerciseName")

                    val sets = mutableListOf<ParsedSet>()
                    val setsArray = exerciseObj.getAsJsonArray("sets")
                    BugfenderLogger.d(TAG, "        Exercise has ${setsArray?.size() ?: 0} sets")

                    setsArray?.forEach { setElement ->
                        val setObj = setElement.asJsonObject
                        sets.add(
                            ParsedSet(
                                reps = setObj.get("reps")?.takeIf { !it.isJsonNull }?.asInt,
                                weight = setObj.get("weight")?.takeIf { !it.isJsonNull }?.asFloat,
                                rpe = setObj.get("rpe")?.takeIf { !it.isJsonNull }?.asFloat,
                            ),
                        )
                    }

                    val existingSets = exerciseMap[exerciseName]
                    if (existingSets != null) {
                        existingSets.addAll(sets)
                        BugfenderLogger.w(TAG, "DUPLICATE EXERCISE FOUND: $exerciseName")
                        BugfenderLogger.w(TAG, "  Existing sets: ${existingSets.size - sets.size}, new sets: ${sets.size}")
                        BugfenderLogger.w(TAG, "  Total sets after merge: ${existingSets.size}")
                    } else {
                        exerciseMap[exerciseName] = sets
                        BugfenderLogger.d(TAG, "        Added $exerciseName with ${sets.size} sets")
                    }
                }

                val exercises =
                    exerciseMap.map { (name, sets) ->
                        ParsedExercise(
                            exerciseName = name,
                            sets = sets,
                            notes = null,
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
            BugfenderLogger.d(TAG, "  Week $weekNumber added with ${workouts.size} workouts")
        }

        BugfenderLogger.d(TAG, "=== parseJsonToProgramme COMPLETE ===")
        BugfenderLogger.d(TAG, "Final programme: $name")
        BugfenderLogger.d(TAG, "Total weeks: ${weeks.size}")
        BugfenderLogger.d(TAG, "Total workouts: ${weeks.sumOf { it.workouts.size }}")
        BugfenderLogger.d(TAG, "Total exercises: ${weeks.sumOf { week -> week.workouts.sumOf { it.exercises.size } }}")

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
