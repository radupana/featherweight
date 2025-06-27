package com.github.radupana.featherweight.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
        // TODO: Move to BuildConfig or environment variable
        private const val OPENAI_API_KEY = "YOUR_API_KEY_HERE"
        private const val API_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-3.5-turbo"
        private const val MAX_TOKENS = 2000
        private const val TEMPERATURE = 0.7
    }
    
    suspend fun generateProgramme(request: AIProgrammeRequest): AIProgrammeResponse {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = buildSystemPrompt(request)
                val userPrompt = request.userInput
                
                val response = callOpenAI(systemPrompt, userPrompt)
                parseAIResponse(response)
            } catch (e: Exception) {
                AIProgrammeResponse(
                    success = false,
                    error = "Failed to generate programme: ${e.message}"
                )
            }
        }
    }
    
    private fun buildSystemPrompt(request: AIProgrammeRequest): String {
        return """
            You are a professional strength and conditioning coach creating personalized training programmes.
            
            AVAILABLE EXERCISES (use ONLY these exact names):
            ${request.exerciseDatabase.joinToString(", ")}
            
            CONSTRAINTS:
            - Maximum ${request.maxDays} training days per week
            - Programme duration: 4-${request.maxWeeks} weeks
            - Use only exercises from the provided list
            - If user mentions an exercise not in the list, find the closest match
            
            OUTPUT FORMAT:
            Return a JSON object with this structure:
            {
                "name": "Programme name",
                "description": "Brief description",
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
                                "notes": "Focus on explosive concentric"
                            }
                        ]
                    }
                ]
            }
            
            If you need clarification, return:
            {
                "clarificationNeeded": "What specific information do you need?"
            }
            
            IMPORTANT:
            - Ensure proper exercise selection based on user's experience level
            - Include appropriate progression scheme
            - Balance volume and intensity appropriately
            - Consider recovery between sessions
        """.trimIndent()
    }
    
    private suspend fun callOpenAI(systemPrompt: String, userPrompt: String): String {
        // For Phase 1, return a mock response
        // TODO: Implement actual OpenAI API call
        return """
            {
                "name": "Strength Builder Programme",
                "description": "A 4-day upper/lower split focused on building strength",
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
                                "restSeconds": 240
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }
    
    private fun parseAIResponse(response: String): AIProgrammeResponse {
        return try {
            val json = JSONObject(response)
            
            // Check if clarification is needed
            if (json.has("clarificationNeeded")) {
                return AIProgrammeResponse(
                    success = true,
                    clarificationNeeded = json.getString("clarificationNeeded")
                )
            }
            
            // Parse the programme
            val programme = GeneratedProgramme(
                name = json.getString("name"),
                description = json.getString("description"),
                durationWeeks = json.getInt("durationWeeks"),
                daysPerWeek = json.getInt("daysPerWeek"),
                workouts = parseWorkouts(json.getJSONArray("workouts"))
            )
            
            AIProgrammeResponse(
                success = true,
                programme = programme
            )
        } catch (e: Exception) {
            AIProgrammeResponse(
                success = false,
                error = "Failed to parse AI response: ${e.message}"
            )
        }
    }
    
    private fun parseWorkouts(workoutsArray: org.json.JSONArray): List<GeneratedWorkout> {
        val workouts = mutableListOf<GeneratedWorkout>()
        
        for (i in 0 until workoutsArray.length()) {
            val workoutJson = workoutsArray.getJSONObject(i)
            val exercisesArray = workoutJson.getJSONArray("exercises")
            val exercises = mutableListOf<GeneratedExercise>()
            
            for (j in 0 until exercisesArray.length()) {
                val exerciseJson = exercisesArray.getJSONObject(j)
                exercises.add(
                    GeneratedExercise(
                        exerciseName = exerciseJson.getString("exerciseName"),
                        sets = exerciseJson.getInt("sets"),
                        repsMin = exerciseJson.getInt("repsMin"),
                        repsMax = exerciseJson.getInt("repsMax"),
                        rpe = if (exerciseJson.has("rpe")) exerciseJson.getDouble("rpe").toFloat() else null,
                        restSeconds = if (exerciseJson.has("restSeconds")) exerciseJson.getInt("restSeconds") else 180,
                        notes = if (exerciseJson.has("notes")) exerciseJson.getString("notes") else null
                    )
                )
            }
            
            workouts.add(
                GeneratedWorkout(
                    dayNumber = workoutJson.getInt("dayNumber"),
                    name = workoutJson.getString("name"),
                    exercises = exercises
                )
            )
        }
        
        return workouts
    }
}