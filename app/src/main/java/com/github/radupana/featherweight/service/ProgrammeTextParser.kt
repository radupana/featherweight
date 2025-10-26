package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.TextParsingRequest
import com.github.radupana.featherweight.data.TextParsingResult
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.util.AnalyticsLogger
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.util.PromptSecurityUtil
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
        private const val BASE_SYSTEM_PROMPT = "You are a fitness programme parser. Extract workout programmes into structured JSON format."
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
                CloudLogger.info(TAG, "Starting programme parsing - text length: ${request.rawText.length}, maxes: ${request.userMaxes.size}")

                val validationResult = validateInput(request.rawText)
                if (!validationResult.isValid) {
                    CloudLogger.warn(TAG, "Validation failed: ${validationResult.error}")
                    return@withContext TextParsingResult(
                        success = false,
                        error = validationResult.error,
                    )
                }

                CloudLogger.debug(TAG, "Validation passed, calling OpenAI API...")
                val parsedJson = callOpenAIAPI(request)
                CloudLogger.debug(TAG, "Received parsed JSON response, length: ${parsedJson.length}")
                CloudLogger.debug(TAG, "Full parsed JSON:\n$parsedJson")

                CloudLogger.debug(TAG, "Parsing JSON to programme object...")
                val programme = parseJsonToProgramme(parsedJson, request.rawText)

                CloudLogger.debug(TAG, "Programme parsed successfully!")
                CloudLogger.debug(TAG, "Programme name: ${programme.name}")
                CloudLogger.debug(TAG, "Duration: ${programme.durationWeeks} weeks")
                CloudLogger.debug(TAG, "Number of weeks: ${programme.weeks.size}")
                programme.weeks.forEachIndexed { weekIdx, week ->
                    CloudLogger.debug(TAG, "  Week ${weekIdx + 1}: ${week.workouts.size} workouts")
                    week.workouts.forEachIndexed { workoutIdx, workout ->
                        CloudLogger.debug(TAG, "    Workout ${workoutIdx + 1}: ${workout.exercises.size} exercises")
                    }
                }

                TextParsingResult(
                    success = true,
                    programme = programme,
                )
            } catch (e: IOException) {
                CloudLogger.error(TAG, "=== Programme Parsing FAILED (IOException) ===")
                CloudLogger.error(TAG, "Network or API error: ${e.message}")
                CloudLogger.error(TAG, "Exception type: ${e.javaClass.name}")
                CloudLogger.error(TAG, "Full stack trace:", e)

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
                CloudLogger.error(TAG, "=== Programme Parsing FAILED (NumberFormatException) ===")
                CloudLogger.error(TAG, "Error message: ${e.message}")
                CloudLogger.error(TAG, "Full stack trace:", e)

                val userFriendlyError = "Unable to parse programme: Invalid number format '${e.message}'. The AI returned a range value where a single number was expected."

                TextParsingResult(
                    success = false,
                    error = userFriendlyError,
                )
            } catch (e: IllegalArgumentException) {
                CloudLogger.error(TAG, "=== Programme Parsing FAILED (IllegalArgumentException) ===")
                CloudLogger.error(TAG, "Error message: ${e.message}")
                CloudLogger.error(TAG, "Exception type: ${e.javaClass.name}")
                CloudLogger.error(TAG, "Full stack trace:", e)

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
        // Security check for prompt injection attempts
        if (PromptSecurityUtil.detectInjectionAttempt(request.rawText)) {
            PromptSecurityUtil.logSecurityIncident(
                "programme_parser_injection",
                request.rawText,
            )
            CloudLogger.warn(TAG, "Potential injection attempt detected in programme text")
            throw IllegalArgumentException("Invalid content detected. Please provide a valid workout programme.")
        }

        val configService = ConfigServiceFactory.getConfigService()
        val effectiveApiKey = configService.getOpenAIApiKey()

        if (effectiveApiKey.isNullOrEmpty()) {
            CloudLogger.error(TAG, "API key not available from Remote Config")
            throw IllegalArgumentException("OpenAI API key not configured. Please check your internet connection and try again.")
        }

        // Sanitize the input before building the prompt
        val sanitizedRequest =
            request.copy(
                rawText = PromptSecurityUtil.sanitizeInput(request.rawText),
            )

        val prompt = buildPrompt(sanitizedRequest)
        CloudLogger.debug(TAG, "=== OpenAI API Request ===")
        CloudLogger.debug(TAG, "Prompt length: ${prompt.length} characters")
        CloudLogger.debug(TAG, "Full prompt:\n$prompt")

        // Create defensive system prompt
        val systemPrompt = PromptSecurityUtil.createDefensiveSystemPrompt(BASE_SYSTEM_PROMPT)

        val requestBody =
            JsonObject().apply {
                addProperty("model", "gpt-5-mini")

                val messages = JsonArray()
                messages.add(
                    JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
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

        CloudLogger.debug(TAG, "Request body JSON:\n$requestBody")
        CloudLogger.debug(TAG, "Calling OpenAI API with model: gpt-5-mini")

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

        CloudLogger.debug(TAG, "=== OpenAI API Response ===")
        CloudLogger.debug(TAG, "Response code: ${response.code}")
        CloudLogger.debug(TAG, "Response body size: ${responseBody.length} chars")
        CloudLogger.debug(TAG, "Full response body:\n$responseBody")

        AnalyticsLogger.logOpenAIResponse(
            endpoint = apiUrl,
            statusCode = response.code,
            responseBody = responseBody,
            responseTimeMs = responseTime,
        )

        if (!response.isSuccessful) {
            CloudLogger.error(TAG, "API call failed with status ${response.code}")
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

            CloudLogger.error(TAG, "Error message: $errorMessage")
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
            ${PromptSecurityUtil.wrapUserInput(request.rawText)}

            ${if (maxesInfo.isNotBlank()) "User's 1RM values:\n${PromptSecurityUtil.wrapUserInput(maxesInfo)}" else ""}
            
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
        CloudLogger.debug(TAG, "=== parseJsonToProgramme START ===")
        CloudLogger.debug(TAG, "JSON string length: ${jsonString.length}")

        require(jsonString.isNotBlank()) { "JSON string is blank" }

        val json =
            try {
                JsonParser.parseString(jsonString).asJsonObject
            } catch (e: com.google.gson.JsonSyntaxException) {
                ExceptionLogger.logException(TAG, "Failed to parse JSON string", e)
                CloudLogger.error(TAG, "Invalid JSON content:\n$jsonString")
                throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
            }

        CloudLogger.debug(TAG, "JSON parsed successfully")
        CloudLogger.debug(TAG, "Top-level keys: ${json.keySet()}")

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
                CloudLogger.debug(TAG, "=== PARSING LOGIC DEBUG ===")
                logic.get("workout_type")?.let { workoutType ->
                    if (!workoutType.isJsonNull) {
                        CloudLogger.debug(TAG, "Workout type detected: ${workoutType.asString}")
                    }
                }
                logic.get("disambiguation_applied")?.let { disambig ->
                    if (disambig.isJsonArray) {
                        disambig.asJsonArray.forEach { decision ->
                            CloudLogger.debug(TAG, "Disambiguation: ${decision.asString}")
                        }
                    }
                }
                logic.get("set_interpretation")?.let { setInterp ->
                    if (setInterp.isJsonArray) {
                        setInterp.asJsonArray.forEach { interpretation ->
                            CloudLogger.debug(TAG, "Set interpretation: ${interpretation.asString}")
                        }
                    }
                }
                CloudLogger.debug(TAG, "=== END PARSING LOGIC ===")
            }
        }

        val name = json.get("name")?.asString ?: "Imported Programme"
        val durationWeeks = json.get("duration_weeks")?.asInt ?: 1
        CloudLogger.debug(TAG, "Programme name: $name, duration: $durationWeeks weeks")
        val description = ""
        val programmeType = "GENERAL_FITNESS"
        val difficulty = "INTERMEDIATE"

        val weeks = mutableListOf<ParsedWeek>()
        val weeksArray = json.getAsJsonArray("weeks")
        CloudLogger.debug(TAG, "Found ${weeksArray?.size() ?: 0} weeks in JSON")

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
                CloudLogger.debug(TAG, "    Workout has ${exercisesArray?.size() ?: 0} exercises")

                exercisesArray?.forEach { exerciseElement ->
                    val exerciseObj = exerciseElement.asJsonObject
                    val exerciseName = exerciseObj.get("name")?.asString ?: "Unknown Exercise"
                    CloudLogger.debug(TAG, "      Processing exercise: $exerciseName")

                    val sets = mutableListOf<ParsedSet>()
                    val setsArray = exerciseObj.getAsJsonArray("sets")
                    CloudLogger.debug(TAG, "        Exercise has ${setsArray?.size() ?: 0} sets")

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
                        CloudLogger.warn(TAG, "DUPLICATE EXERCISE FOUND: $exerciseName")
                        CloudLogger.warn(TAG, "  Existing sets: ${existingSets.size - sets.size}, new sets: ${sets.size}")
                        CloudLogger.warn(TAG, "  Total sets after merge: ${existingSets.size}")
                    } else {
                        exerciseMap[exerciseName] = sets
                        CloudLogger.debug(TAG, "        Added $exerciseName with ${sets.size} sets")
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
            CloudLogger.debug(TAG, "  Week $weekNumber added with ${workouts.size} workouts")
        }

        CloudLogger.debug(TAG, "=== parseJsonToProgramme COMPLETE ===")
        CloudLogger.debug(TAG, "Final programme: $name")
        CloudLogger.debug(TAG, "Total weeks: ${weeks.size}")
        CloudLogger.debug(TAG, "Total workouts: ${weeks.sumOf { it.workouts.size }}")
        CloudLogger.debug(TAG, "Total exercises: ${weeks.sumOf { week -> week.workouts.sumOf { it.exercises.size } }}")

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
                        CloudLogger.warn(TAG, "Exercise '${exercise.exerciseName}' has no sets")
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
