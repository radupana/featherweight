package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.data.TextParsingResult
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.util.AnalyticsLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.WeightFormatter
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

open class ProgrammeTextParser(
    private val weightUnitManager: WeightUnitManager? = null,
) {
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
                Log.i(TAG, "Starting programme parsing - text length: ${request.rawText.length}, maxes: ${request.userMaxes.size}")

                val validationResult = validateInput(request.rawText)
                if (!validationResult.isValid) {
                    Log.w(TAG, "Validation failed: ${validationResult.error}")
                    return@withContext TextParsingResult(
                        success = false,
                        error = validationResult.error,
                    )
                }

                Log.d(TAG, "Validation passed, calling OpenAI API...")
                val parsedJson = callOpenAIAPI(request)
                Log.d(TAG, "Received parsed JSON response, length: ${parsedJson.length}")
                Log.d(TAG, "Full parsed JSON:\n$parsedJson")

                Log.d(TAG, "Parsing JSON to programme object...")
                val programme = parseJsonToProgramme(parsedJson, request.rawText)

                Log.d(TAG, "Programme parsed successfully!")
                Log.d(TAG, "Programme name: ${programme.name}")
                Log.d(TAG, "Duration: ${programme.durationWeeks} weeks")
                Log.d(TAG, "Number of weeks: ${programme.weeks.size}")
                programme.weeks.forEachIndexed { weekIdx, week ->
                    Log.d(TAG, "  Week ${weekIdx + 1}: ${week.workouts.size} workouts")
                    week.workouts.forEachIndexed { workoutIdx, workout ->
                        Log.d(TAG, "    Workout ${workoutIdx + 1}: ${workout.exercises.size} exercises")
                    }
                }

                TextParsingResult(
                    success = true,
                    programme = programme,
                )
            } catch (e: IOException) {
                Log.e(TAG, "=== Programme Parsing FAILED (IOException) ===")
                Log.e(TAG, "Network or API error: ${e.message}")
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Full stack trace:", e)

                val userFriendlyError =
                    when {
                        e.message?.contains("API key") == true -> "OpenAI API key not configured"
                        e.message?.contains("timeout") == true -> "Request timed out. Please try again."
                        e.message?.contains("Network") == true -> "Network error. Check your connection."
                        e.message?.contains("500") == true || e.message?.contains("503") == true ->
                            "AI service temporarily unavailable. Please try again in a few moments. (Error: ${e.message?.take(20)})"
                        e.message?.contains("401") == true -> "Authentication failed. Please contact support."
                        e.message?.contains("429") == true -> "Too many requests. Please wait a moment and try again."
                        else -> "Failed to connect to AI service: ${e.message}"
                    }

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            } catch (e: NumberFormatException) {
                Log.e(TAG, "=== Programme Parsing FAILED (NumberFormatException) ===")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Full stack trace:", e)

                val userFriendlyError = "Unable to parse programme: Invalid number format '${e.message}'. The AI returned a range value where a single number was expected."

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "=== Programme Parsing FAILED (IllegalArgumentException) ===")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Full stack trace:", e)

                val userFriendlyError =
                    when {
                        e.message?.contains("too complex") == true -> e.message
                        e.message?.contains("Unable to parse") == true -> e.message
                        e.message?.contains("JSON string is blank") == true ->
                            "Programme format couldn't be processed. Try simplifying the text or breaking it into smaller sections."
                        e.message?.contains("Network") == true -> e.message
                        e.message?.contains("timeout") == true ->
                            "Processing took too long. Please try a shorter programme or break it into parts."
                        else ->
                            "Unable to parse programme${if (e.message != null) ": ${e.message}" else ""}. Please check the format and try again."
                    }

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            }
        }

    private fun validateInput(text: String): ValidationResult {
        // Basic checks
        val basicError =
            when {
                text.isBlank() -> "Please provide programme text"
                text.length > 10000 -> "Programme text is too long. Maximum 10,000 characters"
                text.length < 50 -> "Your input seems too short. Please include at least one full workout with exercises, sets, and reps."
                else -> null
            }
        if (basicError != null) return ValidationResult(false, basicError)

        val lowerText = text.lowercase()

        // Content quality checks
        val qualityError = checkContentQuality(text, lowerText)
        if (qualityError != null) return ValidationResult(false, qualityError)

        // Structure and format checks
        val structureError = checkStructureAndFormat(text)
        if (structureError != null) return ValidationResult(false, structureError)

        return ValidationResult(true)
    }

    private fun checkContentQuality(
        text: String,
        lowerText: String,
    ): String? {
        // Check for profanity/spam
        val profanityPatterns = listOf("fuck", "shit", "damn", "hell", "ass", "bitch", "crap")
        val spamIndicators = profanityPatterns.count { lowerText.contains(it) }
        if (spamIndicators > 2 || (spamIndicators > 0 && text.length < 100)) {
            return "Please provide a serious workout programme to parse."
        }

        // Check for workout-related keywords
        val workoutKeywords =
            listOf(
                // Exercise names
                "squat",
                "press",
                "deadlift",
                "row",
                "curl",
                "extension",
                "raise",
                "pull",
                "push",
                "fly",
                "dip",
                "chin",
                "bench",
                "overhead",
                // Structure words
                "day",
                "week",
                "workout",
                "session",
                "exercise",
                // Sets/reps indicators
                "sets",
                "reps",
                "x",
                "×",
                "set of",
                // Weight/intensity
                "kg",
                "lbs",
                "pounds",
                "%",
                "rpe",
                "rir",
                "@",
            )

        val keywordCount = workoutKeywords.count { keyword -> lowerText.contains(keyword) }
        if (keywordCount < 2) {
            return "Couldn't find workout-related content. Please include exercises with sets and reps (e.g., 'Squat 3x5', 'Bench Press 4x8')."
        }

        return null
    }

    private fun checkStructureAndFormat(text: String): String? {
        // Check for excessive repetition
        val words = text.split(Regex("\\s+"))
        if (words.size > 3) {
            val wordGroups = words.groupBy { it.lowercase() }
            val maxRepetitions = wordGroups.values.maxOfOrNull { it.size } ?: 0
            val repetitionRatio = maxRepetitions.toFloat() / words.size

            if (repetitionRatio > 0.5) {
                return "Input appears to be spam or repetitive text. Please provide a real workout programme."
            }
        }

        // Check if mostly numbers/special chars
        val alphaCount = text.count { it.isLetter() }
        val alphaRatio = alphaCount.toFloat() / text.length
        if (alphaRatio < 0.3) {
            return "Input needs more exercise descriptions. Include exercise names along with sets and reps."
        }

        return null
    }

    internal open suspend fun callOpenAIAPI(request: TextParsingRequest): String {
        val configService = ConfigServiceFactory.getConfigService()
        val effectiveApiKey = configService.getOpenAIApiKey()

        if (effectiveApiKey.isNullOrEmpty()) {
            Log.e(TAG, "API key not available from Remote Config")
            throw IllegalArgumentException("OpenAI API key not configured. Please check your internet connection and try again.")
        }

        val prompt = buildPrompt(request)
        Log.d(TAG, "=== OpenAI API Request ===")
        Log.d(TAG, "Prompt length: ${prompt.length} characters")
        Log.d(TAG, "Full prompt:\n$prompt")

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

        Log.d(TAG, "Request body JSON:\n$requestBody")
        Log.d(TAG, "Calling OpenAI API with model: gpt-5-mini")

        val apiUrl = "https://api.openai.com/v1/chat/completions"
        AnalyticsLogger.logOpenAIRequest(
            endpoint = apiUrl,
            model = "gpt-5-mini",
            requestBody = requestBody.toString(),
        )

        val httpRequest =
            Request
                .Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $effectiveApiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

        val startTime = System.currentTimeMillis()
        val response = client.newCall(httpRequest).execute()
        val responseTime = System.currentTimeMillis() - startTime
        val responseBody = response.body.string()

        Log.d(TAG, "=== OpenAI API Response ===")
        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body size: ${responseBody.length} chars")
        Log.d(TAG, "Full response body:\n$responseBody")

        AnalyticsLogger.logOpenAIResponse(
            endpoint = apiUrl,
            statusCode = response.code,
            responseBody = responseBody,
            responseTimeMs = responseTime,
        )

        if (!response.isSuccessful) {
            Log.e(TAG, "API call failed with status ${response.code}")
            val errorJson =
                try {
                    JsonParser.parseString(responseBody).asJsonObject
                } catch (e: com.google.gson.JsonSyntaxException) {
                    ExceptionLogger.logException(TAG, "Failed to parse error response as JSON", e)
                    null
                } catch (e: IllegalStateException) {
                    ExceptionLogger.logException(TAG, "Invalid JSON state in error response", e)
                    null
                }
            val errorMessage =
                errorJson?.getAsJsonObject("error")?.get("message")?.asString
                    ?: "API call failed with status ${response.code}"

            Log.e(TAG, "Error message: $errorMessage")
            AnalyticsLogger.logOpenAIResponse(
                endpoint = apiUrl,
                statusCode = response.code,
                responseBody = null,
                responseTimeMs = responseTime,
                error = errorMessage,
            )
            throw IOException(errorMessage)
        }

        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject

        // Log token usage
        jsonResponse.getAsJsonObject("usage")?.let { usage ->
            AnalyticsLogger.logOpenAITokenUsage(
                promptTokens = usage.get("prompt_tokens")?.asInt ?: 0,
                completionTokens = usage.get("completion_tokens")?.asInt ?: 0,
                totalTokens = usage.get("total_tokens")?.asInt ?: 0,
            )
        }

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
                    // Convert max to current unit for display in prompt
                    val displayWeight = weightUnitManager?.convertFromKg(max) ?: max
                    val unit = weightUnitManager?.getCurrentUnit()?.suffix ?: "kg"
                    appendLine("$exercise: ${WeightFormatter.formatWeight(displayWeight)}$unit")
                }
            }

        return """
            First, validate if this text contains a workout programme.
            
            If the text:
            - Contains NO identifiable exercises
            - Is profanity, spam, or completely unrelated content
            - Cannot be interpreted as fitness/workout content
            
            Return: {
                "error_type": "INVALID_CONTENT",
                "error_message": "Unable to parse as a workout programme. Please provide text containing exercises, sets, and reps.",
                "name": null,
                "duration_weeks": 0,
                "weeks": []
            }
            
            Otherwise, parse this workout programme into structured JSON.
            
            Programme text:
            ${request.rawText}
            
            ${if (maxesInfo.isNotBlank()) "User's 1RM values:\n$maxesInfo" else ""}
            
            CRITICAL PARSING RULES:
            1. SETS×REPS: "3×5 100kg" = Create EXACTLY 3 set objects
               - [{"reps":5,"weight":100}, {"reps":5,"weight":100}, {"reps":5,"weight":100}]
               - "3 sets of 5" also = 3 separate sets
               - NEVER combine into one set with higher reps
            
            2. REP RANGES: "4×8-10" = 4 sets of 8 reps (use LOWER value)
               - Correct: [{"reps":8}, {"reps":8}, {"reps":8}, {"reps":8}]
               - Wrong: [{"reps":8}, {"reps":10}] - must be 4 sets!
            
            3. ONE ENTRY PER EXERCISE: Never duplicate exercises
               - "Squat 3×5, Squat 3×3" = ONE Squat with 6 total sets
            
            4. PRESERVE ORDER: Keep sets in exact input order
            
            5. DATA TYPE REQUIREMENTS - CRITICAL:
               - "reps": MUST be integer or null (e.g., 5, 10, null)
               - "weight": MUST be float/number or null (e.g., 100, 87.5, null)
               - "rpe": MUST be single float/number or null (e.g., 7, 8.5, null)
               
            6. HANDLING RANGES - ALWAYS USE LOWER VALUE:
               - RPE: "7-8" or "7.5-8" → use 7 or 7.5 (NEVER return "7-8" as string!)
               - Reps: "8-10" → use 8 (NEVER return "8-10" as string!)
               - Weight: "80-85kg" → use 80 (NEVER return "80-85" as string!)
               - NEVER RETURN ANY RANGES AS STRINGS - ALWAYS SINGLE NUMBERS!
            
            EXERCISE NAME RULES:
            Format: [Equipment] [Movement]
            - Default equipment when unspecified:
              - Squat/Bench/Deadlift/Press/Row → "Barbell"
              - Raises/Flyes → "Dumbbell"
            - Examples:
              - "squat" → "Barbell Squat"
              - "bench" or "bench press" → "Barbell Bench Press"
              - "DB press" → "Dumbbell Press"
              - "RDL" → "Romanian Deadlift"
            - Keep variations: "Paused Bench Press", "Front Squat"
            
            
            STRUCTURE:
            - No week/day markers = single week programme
            - "Day 1" or "Monday" = use as day field
            - Multiple workouts = separate by day markers or clear breaks
            
            Output JSON:
            {
                "name": "Imported Programme",
                "duration_weeks": 1,
                "weeks": [{
                    "week_number": 1,
                    "workouts": [{
                        "day": "Monday" or null,
                        "name": "Workout",
                        "exercises": [{
                            "name": "Barbell Squat",
                            "sets": [
                                {"reps": 3, "weight": 90, "rpe": null},
                                {"reps": 3, "weight": 90, "rpe": null},
                                {"reps": 3, "weight": 90, "rpe": null}
                            ]
                        }]
                    }]
                }]
            }
            """.trimIndent()
    }

    /**
     * Parses RPE values that can be either a single number (7.5) or a range (7.5-8).
     * For ranges, returns the first value in the range.
     */
    private fun parseRpeValue(rpeElement: com.google.gson.JsonElement?): Float? {
        if (rpeElement == null || rpeElement.isJsonNull) return null

        return try {
            // Try to parse as a number first
            rpeElement.asFloat
        } catch (e: NumberFormatException) {
            // If it's not a number, try to parse as a string range
            val rpeString = rpeElement.asString
            // Handle ranges like "7.5–8" or "7.5-8" or "6–7"
            val rangePattern = """(\d+(?:\.\d+)?)[–\-](\d+(?:\.\d+)?)""".toRegex()
            val matchResult = rangePattern.find(rpeString)
            if (matchResult != null) {
                // Use the first value of the range
                matchResult.groupValues[1].toFloatOrNull()
            } else {
                // Try to parse as a plain number string
                rpeString.toFloatOrNull()
            }
        }
    }

    private fun parseJsonToProgramme(
        jsonString: String,
        rawText: String,
    ): ParsedProgramme {
        Log.d(TAG, "=== parseJsonToProgramme START ===")
        Log.d(TAG, "JSON string length: ${jsonString.length}")

        require(jsonString.isNotBlank()) { "JSON string is blank" }

        val json =
            try {
                JsonParser.parseString(jsonString).asJsonObject
            } catch (e: com.google.gson.JsonSyntaxException) {
                ExceptionLogger.logException(TAG, "Failed to parse JSON string", e)
                Log.e(TAG, "Invalid JSON content:\n$jsonString")
                throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
            }

        Log.d(TAG, "JSON parsed successfully")
        Log.d(TAG, "Top-level keys: ${json.keySet()}")

        // Check if AI rejected the content as invalid
        json.get("error_type")?.let { errorType ->
            if (!errorType.isJsonNull && errorType.asString == "INVALID_CONTENT") {
                val errorMessage =
                    json.get("error_message")?.asString
                        ?: "Unable to parse as a workout programme."
                throw IllegalArgumentException(errorMessage)
            }
        }

        json.get("parsing_logic")?.let { logicElement ->
            if (!logicElement.isJsonNull && logicElement.isJsonObject) {
                val logic = logicElement.asJsonObject
                Log.d(TAG, "=== PARSING LOGIC DEBUG ===")
                logic.get("workout_type")?.let { workoutType ->
                    if (!workoutType.isJsonNull) {
                        Log.d(TAG, "Workout type detected: ${workoutType.asString}")
                    }
                }
                logic.get("disambiguation_applied")?.let { disambig ->
                    if (disambig.isJsonArray) {
                        disambig.asJsonArray.forEach { decision ->
                            Log.d(TAG, "Disambiguation: ${decision.asString}")
                        }
                    }
                }
                logic.get("set_interpretation")?.let { setInterp ->
                    if (setInterp.isJsonArray) {
                        setInterp.asJsonArray.forEach { interpretation ->
                            Log.d(TAG, "Set interpretation: ${interpretation.asString}")
                        }
                    }
                }
                Log.d(TAG, "=== END PARSING LOGIC ===")
            }
        }

        val name = json.get("name")?.asString ?: "Imported Programme"
        val durationWeeks = json.get("duration_weeks")?.asInt ?: 1
        Log.d(TAG, "Programme name: $name, duration: $durationWeeks weeks")
        val description = ""
        val programmeType = "GENERAL_FITNESS"
        val difficulty = "INTERMEDIATE"

        val weeks = mutableListOf<ParsedWeek>()
        val weeksArray = json.getAsJsonArray("weeks")
        Log.d(TAG, "Found ${weeksArray?.size() ?: 0} weeks in JSON")

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
                Log.d(TAG, "    Workout has ${exercisesArray?.size() ?: 0} exercises")

                exercisesArray?.forEach { exerciseElement ->
                    val exerciseObj = exerciseElement.asJsonObject
                    val exerciseName = exerciseObj.get("name")?.asString ?: "Unknown Exercise"
                    Log.d(TAG, "      Processing exercise: $exerciseName")

                    val sets = mutableListOf<ParsedSet>()
                    val setsArray = exerciseObj.getAsJsonArray("sets")
                    Log.d(TAG, "        Exercise has ${setsArray?.size() ?: 0} sets")

                    setsArray?.forEach { setElement ->
                        val setObj = setElement.asJsonObject
                        sets.add(
                            ParsedSet(
                                reps = setObj.get("reps")?.takeIf { !it.isJsonNull }?.asInt,
                                weight = setObj.get("weight")?.takeIf { !it.isJsonNull }?.asFloat,
                                rpe = parseRpeValue(setObj.get("rpe")),
                            ),
                        )
                    }

                    val existingSets = exerciseMap[exerciseName]
                    if (existingSets != null) {
                        existingSets.addAll(sets)
                        Log.w(TAG, "DUPLICATE EXERCISE FOUND: $exerciseName")
                        Log.w(TAG, "  Existing sets: ${existingSets.size - sets.size}, new sets: ${sets.size}")
                        Log.w(TAG, "  Total sets after merge: ${existingSets.size}")
                    } else {
                        exerciseMap[exerciseName] = sets
                        Log.d(TAG, "        Added $exerciseName with ${sets.size} sets")
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
            Log.d(TAG, "  Week $weekNumber added with ${workouts.size} workouts")
        }

        Log.d(TAG, "=== parseJsonToProgramme COMPLETE ===")
        Log.d(TAG, "Final programme: $name")
        Log.d(TAG, "Total weeks: ${weeks.size}")
        Log.d(TAG, "Total workouts: ${weeks.sumOf { it.workouts.size }}")
        Log.d(TAG, "Total exercises: ${weeks.sumOf { week -> week.workouts.sumOf { it.exercises.size } }}")

        // Post-parse validation
        require(weeks.isNotEmpty()) {
            "No valid workout weeks found. Please check your programme format."
        }

        val totalWorkouts = weeks.sumOf { it.workouts.size }
        require(totalWorkouts > 0) {
            "No workouts found. A programme must contain at least one workout."
        }

        val totalExercises =
            weeks.sumOf { week ->
                week.workouts.sumOf { it.exercises.size }
            }
        require(totalExercises > 0) {
            "No exercises found. Each workout must contain at least one exercise."
        }

        // Validate that exercises have sets
        var totalSets = 0
        weeks.forEach { week ->
            week.workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    if (exercise.sets.isEmpty()) {
                        Log.w(TAG, "Exercise '${exercise.exerciseName}' has no sets")
                    }
                    totalSets += exercise.sets.size
                }
            }
        }

        require(totalSets > 0) {
            "No sets found in any exercises. Please include sets and reps (e.g., '3x10', '4 sets of 8')."
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
