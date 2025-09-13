package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.InsightCategory
import com.github.radupana.featherweight.data.InsightSeverity
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.TrainingInsight
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.TrainingAnalysisService
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InsightsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val repository = FeatherweightRepository(application)
    private val analysisService = TrainingAnalysisService()
    private val gson = Gson()

    companion object {
        private const val TAG = "InsightsViewModel"
        private const val MINIMUM_WORKOUTS_FOR_ANALYSIS = 16
        private const val ANALYSIS_PERIOD_WEEKS = 12
    }

    private val _trainingAnalysis = MutableStateFlow<TrainingAnalysis?>(null)
    val trainingAnalysis: StateFlow<TrainingAnalysis?> = _trainingAnalysis

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _currentWorkoutCount = MutableStateFlow(0)
    val currentWorkoutCount: StateFlow<Int> = _currentWorkoutCount

    // Reactive exercise name mapping
    private val _exerciseNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val exerciseNames: StateFlow<Map<Long, String>> = _exerciseNames

    init {
        loadExerciseNames()
    }

    private fun loadExerciseNames() {
        viewModelScope.launch {
            val exercises = repository.getAllExercises()
            _exerciseNames.value = exercises.associate { it.id to it.name }
        }
    }

    suspend fun getGroupedExercisesSummary(): com.github.radupana.featherweight.service.GroupedExerciseSummary =
        withContext(Dispatchers.IO) {
            try {
                repository.getExercisesSummary()
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("InsightsViewModel", "Error", e)
                com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            } catch (e: IllegalStateException) {
                Log.e("InsightsViewModel", "Error", e)
                com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            }
        }

    fun loadHighlightsData(
        onComplete: (
            recentPRs: List<com.github.radupana.featherweight.data.PersonalRecord>,
            weeklyWorkoutCount: Int,
            currentStreak: Int,
        ) -> Unit,
    ) {
        viewModelScope.launch {
            val trace = safeNewTrace("insights_calculation")
            trace?.start()

            try {
                val recentPRs =
                    withContext(Dispatchers.IO) {
                        repository.getRecentPRs(limit = 5)
                    }

                val now = LocalDateTime.now()
                // Start from Sunday 23:59:59 to include all of Monday onwards
                val weekStart =
                    now
                        .with(java.time.DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay()
                        .minusSeconds(1)
                val weeklyWorkoutCount =
                    withContext(Dispatchers.IO) {
                        repository.getCompletedWorkoutCountSince(weekStart)
                    }

                val currentStreak =
                    withContext(Dispatchers.IO) {
                        repository.getWeeklyStreak()
                    }

                trace?.putAttribute("pr_count", recentPRs.size.toString())
                trace?.putAttribute("weekly_workouts", weeklyWorkoutCount.toString())
                trace?.putAttribute("current_streak", currentStreak.toString())
                trace?.stop()

                onComplete(recentPRs, weeklyWorkoutCount, currentStreak)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("InsightsViewModel", "Error", e)
                trace?.stop()
                onComplete(emptyList(), 0, 0)
            } catch (e: IllegalStateException) {
                Log.e("InsightsViewModel", "Error", e)
                trace?.stop()
                onComplete(emptyList(), 0, 0)
            }
        }
    }

    // Training Analysis methods
    fun loadCachedAnalysis() {
        viewModelScope.launch {
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
            // Always calculate current workout count for accurate display
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            _currentWorkoutCount.value = repository.getWorkoutCountByDateRange(startDate, endDate)
        }
    }

    fun checkAndRunScheduledAnalysis() {
        viewModelScope.launch {
            // Always update current workout count
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            _currentWorkoutCount.value = repository.getWorkoutCountByDateRange(startDate, endDate)

            // Check if we've already checked today using SharedPreferences
            val prefs = getApplication<Application>().getSharedPreferences("training_analysis", 0)
            val lastCheckDate = prefs.getString("last_check_date", null)
            val today = LocalDate.now().toString()

            if (lastCheckDate == today) {
                // Already checked today, skip
                return@launch
            }

            // Update last check date
            prefs.edit { putString("last_check_date", today) }

            val lastAnalysis = repository.getLatestTrainingAnalysis()
            val shouldRunAnalysis =
                when {
                    lastAnalysis == null -> true // First time user
                    ChronoUnit.DAYS.between(lastAnalysis.analysisDate.toLocalDate(), LocalDate.now()) >= 7 -> true // Weekly
                    else -> false
                }

            if (shouldRunAnalysis) {
                runAnalysis()
            }
        }
    }

    private suspend fun runAnalysis() {
        _isAnalyzing.value = true
        try {
            // Check if we have enough data
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            val workoutCount = repository.getWorkoutCountByDateRange(startDate, endDate)
            _currentWorkoutCount.value = workoutCount

            if (workoutCount < MINIMUM_WORKOUTS_FOR_ANALYSIS) {
                // Save a placeholder analysis indicating insufficient data
                val insufficientDataAnalysis =
                    TrainingAnalysis(
                        analysisDate = LocalDateTime.now(),
                        periodStart = startDate,
                        periodEnd = endDate,
                        overallAssessment = "INSUFFICIENT_DATA:$workoutCount:$MINIMUM_WORKOUTS_FOR_ANALYSIS",
                        keyInsightsJson =
                            gson.toJson(
                                listOf(
                                    TrainingInsight(
                                        category = InsightCategory.PROGRESSION,
                                        message = "Continue building training history",
                                        severity = InsightSeverity.INFO,
                                    ),
                                ),
                            ),
                        recommendationsJson =
                            gson.toJson(
                                listOf("Complete more workouts to enable analysis"),
                            ),
                        warningsJson = gson.toJson(emptyList<String>()),
                    )
                repository.saveTrainingAnalysis(insufficientDataAnalysis)
                _trainingAnalysis.value = insufficientDataAnalysis
            } else {
                // Proceed with normal analysis
                val analysis = performAnalysis()
                repository.saveTrainingAnalysis(analysis)
                _trainingAnalysis.value = analysis
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: java.io.IOException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: IllegalStateException) {
            Log.e("InsightsViewModel", "Training analysis failed", e)
            // Keep existing cached analysis if API fails
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } finally {
            _isAnalyzing.value = false
        }
    }

    private suspend fun performAnalysis(): TrainingAnalysis =
        withContext(Dispatchers.IO) {
            val trace = safeNewTrace("training_analysis")
            trace?.start()

            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(ANALYSIS_PERIOD_WEEKS.toLong())
            val workouts = repository.getWorkoutsByDateRange(startDate, endDate)
            trace?.putAttribute("workout_count", workouts.size.toString())

            val jsonPayload = buildAnalysisPayload(workouts)

            val response = callOpenAIAPI(jsonPayload)

            val analysis = parseAIResponse(response, startDate, endDate)
            trace?.stop()
            analysis
        }

    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Firebase Performance not available - likely in test environment")
            null
        } catch (e: RuntimeException) {
            Log.d(TAG, "Firebase Performance trace creation failed: ${e.message}")
            null
        }

    private suspend fun buildAnalysisPayload(workouts: List<WorkoutSummary>): String {
        val payload = JsonObject()

        // Analysis period
        val period = JsonObject()
        period.addProperty(
            "start_date",
            workouts
                .firstOrNull()
                ?.date
                ?.toLocalDate()
                .toString(),
        )
        period.addProperty(
            "end_date",
            workouts
                .lastOrNull()
                ?.date
                ?.toLocalDate()
                .toString(),
        )
        period.addProperty("total_workouts", workouts.size)
        payload.add("analysis_period", period)

        // Build SUMMARIZED workouts array to reduce token usage
        // Group by week and only include key metrics
        val workoutsArray = com.google.gson.JsonArray()

        // Only include last 4 weeks of detailed data, older data as weekly summaries
        LocalDate.now().minusWeeks(4)

        for (workout in workouts) {
            val workoutObj = JsonObject()
            workoutObj.addProperty("id", workout.id)
            workoutObj.addProperty("date", workout.date.toLocalDate().toString())
            workoutObj.addProperty("name", workout.name ?: "Workout")
            workoutObj.addProperty("duration_minutes", workout.duration?.div(60) ?: 0)
            workoutObj.addProperty("notes", "")

            // Get exercises for this workout
            val exercises = repository.getExerciseLogsForWorkout(workout.id)
            val exercisesArray = com.google.gson.JsonArray()

            for (exercise in exercises) {
                val exerciseObj = JsonObject()
                val exerciseName = repository.getExerciseById(exercise.exerciseVariationId)?.name ?: "Unknown Exercise"
                exerciseObj.addProperty("name", exerciseName)

                // Get sets for this exercise
                val sets = repository.getSetLogsForExercise(exercise.id)
                val setsArray = com.google.gson.JsonArray()

                for ((index, set) in sets.withIndex()) {
                    val setObj = JsonObject()
                    setObj.addProperty("set_number", index + 1)
                    setObj.addProperty("weight", set.actualWeight ?: set.targetWeight)
                    setObj.addProperty("reps", set.actualReps ?: set.targetReps)
                    setObj.addProperty("rpe", set.actualRpe)
                    setObj.addProperty("rest_seconds", 180) // Default rest time
                    setObj.addProperty("completed", set.isCompleted)
                    setsArray.add(setObj)
                }

                exerciseObj.add("sets", setsArray)
                exercisesArray.add(exerciseObj)
            }

            workoutObj.add("exercises", exercisesArray)
            workoutsArray.add(workoutObj)
        }

        payload.add("workouts", workoutsArray)

        // Add personal records
        val prs = repository.getRecentPRs(limit = 20)
        val prsArray = com.google.gson.JsonArray()

        for (pr in prs) {
            val prObj = JsonObject()
            val exerciseName = repository.getExerciseById(pr.exerciseVariationId)?.name ?: "Unknown Exercise"
            prObj.addProperty("exercise", exerciseName)
            prObj.addProperty("date", pr.recordDate.toLocalDate().toString())
            prObj.addProperty("weight", pr.weight)
            prObj.addProperty("reps", pr.reps)

            if (pr.previousWeight != null) {
                val prevObj = JsonObject()
                prevObj.addProperty("weight", pr.previousWeight)
                prevObj.addProperty("reps", pr.previousReps)
                prevObj.addProperty("date", pr.previousDate?.toLocalDate().toString())
                prObj.add("previous_best", prevObj)
            }

            prsArray.add(prObj)
        }

        payload.add("personal_records", prsArray)

        return gson.toJson(payload)
    }

    private suspend fun callOpenAIAPI(jsonPayload: String): String {
        val prompt =
            """
            Analyze this training data and provide an EXTREMELY CONCISE analysis (readable in 10 seconds).

            OUTPUT FORMAT (JSON):
            {
              "overall_assessment": "ONE sentence, 50 words max. Focus on the single most important trend.",
              "key_insights": [
                {"category": "PROGRESSION|RECOVERY|BALANCE", 
                 "message": "Max 20 words. Just state the fact.",
                 "severity": "SUCCESS|WARNING|CRITICAL"}
              ],
              "warnings": [],
              "recommendations": ["Max 3 items. Each under 20 words. Ultra-specific actions."]
            }

            RULES:
            - overall_assessment: ONE sentence only (e.g., "Strong progression with 3 PRs, but needs more tricep work")
            - key_insights: Maximum 3 items. Only include if truly important
            - recommendations: Maximum 3 items. Must be ultra-specific (e.g., "Add 2x10 tricep extensions weekly")
            - Skip warnings array entirely (return empty)
            - Prioritize what matters most: PRs, overtraining risks, major imbalances
            - If everything looks good, just say so briefly
            
            Data: $jsonPayload
            """.trimIndent()

        return analysisService.analyzeTraining(prompt)
    }

    private fun parseAIResponse(
        response: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): TrainingAnalysis {
        try {
            val jsonResponse = JsonParser.parseString(response).asJsonObject

            val overallAssessment = jsonResponse.get("overall_assessment")?.asString ?: "Analysis complete."

            val keyInsights = mutableListOf<TrainingInsight>()
            jsonResponse.getAsJsonArray("key_insights")?.forEach { element ->
                val insight = element.asJsonObject
                val category =
                    try {
                        InsightCategory.valueOf(insight.get("category").asString)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown insight category, defaulting to PROGRESSION", e)
                        InsightCategory.PROGRESSION
                    }
                val severity =
                    try {
                        InsightSeverity.valueOf(insight.get("severity").asString)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown insight severity, defaulting to INFO", e)
                        InsightSeverity.INFO
                    }

                keyInsights.add(
                    TrainingInsight(
                        category = category,
                        message = insight.get("message").asString,
                        severity = severity,
                    ),
                )
            }

            val warnings = mutableListOf<String>()
            jsonResponse.getAsJsonArray("warnings")?.forEach { element ->
                warnings.add(element.asString)
            }

            val recommendations = mutableListOf<String>()
            jsonResponse.getAsJsonArray("recommendations")?.forEach { element ->
                recommendations.add(element.asString)
            }

            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = overallAssessment,
                keyInsightsJson = gson.toJson(keyInsights),
                recommendationsJson = gson.toJson(recommendations),
                warningsJson = gson.toJson(warnings),
            )
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e(TAG, "Failed to parse AI analysis, using fallback", e)
            val fallbackInsights =
                listOf(
                    TrainingInsight(
                        category = InsightCategory.PROGRESSION,
                        message = "Training data analyzed",
                        severity = InsightSeverity.INFO,
                    ),
                )
            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = "Analysis complete. Continue with your current training program.",
                keyInsightsJson = gson.toJson(fallbackInsights),
                recommendationsJson = gson.toJson(listOf("Continue current training program")),
                warningsJson = gson.toJson(emptyList<String>()),
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to parse AI analysis, using fallback", e)
            val fallbackInsights =
                listOf(
                    TrainingInsight(
                        category = InsightCategory.PROGRESSION,
                        message = "Training data analyzed",
                        severity = InsightSeverity.INFO,
                    ),
                )
            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = "Analysis complete. Continue with your current training program.",
                keyInsightsJson = gson.toJson(fallbackInsights),
                recommendationsJson = gson.toJson(listOf("Continue current training program")),
                warningsJson = gson.toJson(emptyList<String>()),
            )
        }
    }
}
