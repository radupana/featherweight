package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.radupana.featherweight.data.AdherenceAnalysis
import com.github.radupana.featherweight.data.InsightCategory
import com.github.radupana.featherweight.data.InsightSeverity
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.TrainingInsight
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.CloudFunctionService
import com.github.radupana.featherweight.service.DeviationSummaryService
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
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
import java.util.Locale

class InsightsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val repository = FeatherweightRepository(application)
    private val authManager = ServiceLocator.provideAuthenticationManager(application)
    private val cloudFunctionService = CloudFunctionService()
    private val deviationSummaryService = DeviationSummaryService()
    private val gson = Gson()

    companion object {
        private const val TAG = "InsightsViewModel"
        private const val MINIMUM_WORKOUTS_FOR_ANALYSIS = 1
        private const val MAX_WORKOUTS_FOR_ANALYSIS = 20

        fun calculateAnalysisMetadata(workouts: List<WorkoutSummary>): AnalysisMetadata {
            val startDate = workouts.lastOrNull()?.date?.toLocalDate()
            val endDate = workouts.firstOrNull()?.date?.toLocalDate()
            val totalWeeks =
                if (startDate != null && endDate != null) {
                    ChronoUnit.WEEKS
                        .between(startDate, endDate)
                        .toInt()
                        .coerceAtLeast(1)
                } else {
                    1
                }
            val avgFrequency =
                if (totalWeeks > 0) {
                    workouts.size.toFloat() / totalWeeks
                } else {
                    0f
                }

            return AnalysisMetadata(
                startDate = startDate,
                endDate = endDate,
                totalWorkouts = workouts.size,
                totalWeeks = totalWeeks,
                avgFrequencyPerWeek = avgFrequency,
            )
        }
    }

    data class AnalysisMetadata(
        val startDate: LocalDate?,
        val endDate: LocalDate?,
        val totalWorkouts: Int,
        val totalWeeks: Int,
        val avgFrequencyPerWeek: Float,
    )

    data class AnalysisQuota(
        val monthlyRemaining: Int,
        val resetDate: LocalDate,
    )

    private val _trainingAnalysis = MutableStateFlow<TrainingAnalysis?>(null)
    val trainingAnalysis: StateFlow<TrainingAnalysis?> = _trainingAnalysis

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _currentWorkoutCount = MutableStateFlow(0)
    val currentWorkoutCount: StateFlow<Int> = _currentWorkoutCount

    private val _analysisQuota = MutableStateFlow<AnalysisQuota?>(null)
    val analysisQuota: StateFlow<AnalysisQuota?> = _analysisQuota

    private val _lastAnalysisDate = MutableStateFlow<LocalDateTime?>(null)
    val lastAnalysisDate: StateFlow<LocalDateTime?> = _lastAnalysisDate

    // Reactive exercise name mapping
    private val _exerciseNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val exerciseNames: StateFlow<Map<String, String>> = _exerciseNames

    init {
        loadExerciseNames()
    }

    fun isAuthenticated(): Boolean = authManager.isAuthenticated()

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
                ExceptionLogger.logException("InsightsViewModel", "Error getting exercises summary", e)
                com.github.radupana.featherweight.service
                    .GroupedExerciseSummary(emptyList(), emptyList())
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException("InsightsViewModel", "Error getting exercises summary", e)
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
                ExceptionLogger.logException("InsightsViewModel", "Error loading highlights data", e)
                trace?.stop()
                onComplete(emptyList(), 0, 0)
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException("InsightsViewModel", "Error loading highlights data", e)
                trace?.stop()
                onComplete(emptyList(), 0, 0)
            }
        }
    }

    // Training Analysis methods
    fun loadCachedAnalysis() {
        viewModelScope.launch {
            val analysis = repository.getLatestTrainingAnalysis()
            _trainingAnalysis.value = analysis
            _lastAnalysisDate.value = analysis?.analysisDate
            val workouts = repository.getRecentWorkouts(MAX_WORKOUTS_FOR_ANALYSIS)
            _currentWorkoutCount.value = workouts.size
        }
    }

    fun triggerManualAnalysis(onQuotaExceeded: (AnalysisQuota) -> Unit = {}) {
        viewModelScope.launch {
            runAnalysis(onQuotaExceeded)
        }
    }

    private suspend fun runAnalysis(onQuotaExceeded: (AnalysisQuota) -> Unit = {}) {
        _isAnalyzing.value = true
        try {
            val workouts = repository.getRecentWorkouts(MAX_WORKOUTS_FOR_ANALYSIS)
            _currentWorkoutCount.value = workouts.size

            if (workouts.size < MINIMUM_WORKOUTS_FOR_ANALYSIS) {
                val startDate = workouts.firstOrNull()?.date?.toLocalDate() ?: LocalDate.now()
                val endDate = workouts.lastOrNull()?.date?.toLocalDate() ?: LocalDate.now()
                val insufficientDataAnalysis =
                    TrainingAnalysis(
                        analysisDate = LocalDateTime.now(),
                        periodStart = startDate,
                        periodEnd = endDate,
                        overallAssessment = "INSUFFICIENT_DATA:${workouts.size}:$MINIMUM_WORKOUTS_FOR_ANALYSIS",
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
                _lastAnalysisDate.value = insufficientDataAnalysis.analysisDate
            } else {
                val analysis = performCloudAnalysis(workouts, onQuotaExceeded)
                if (analysis != null) {
                    repository.saveTrainingAnalysis(analysis)
                    _trainingAnalysis.value = analysis
                    _lastAnalysisDate.value = analysis.analysisDate
                }
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            ExceptionLogger.logException("InsightsViewModel", "Training analysis failed", e)
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: java.io.IOException) {
            ExceptionLogger.logException("InsightsViewModel", "Training analysis failed", e)
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } catch (e: IllegalStateException) {
            ExceptionLogger.logException("InsightsViewModel", "Training analysis failed", e)
            _trainingAnalysis.value = repository.getLatestTrainingAnalysis()
        } finally {
            _isAnalyzing.value = false
        }
    }

    private suspend fun performCloudAnalysis(
        workouts: List<WorkoutSummary>,
        onQuotaExceeded: (AnalysisQuota) -> Unit,
    ): TrainingAnalysis? =
        withContext(Dispatchers.IO) {
            val trace = safeNewTrace("training_analysis")
            trace?.start()

            try {
                trace?.putAttribute("workout_count", workouts.size.toString())

                val jsonPayload = buildAnalysisPayload(workouts)

                CloudLogger.debug(TAG, "Sending analysis request to Cloud Function")
                CloudLogger.debug(TAG, "Request payload length: ${jsonPayload.length} chars")

                val result = cloudFunctionService.analyzeTraining(jsonPayload)

                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    if (exception is CloudFunctionService.AnalysisQuotaExceededException) {
                        val resetDate = LocalDate.now().withDayOfMonth(1).plusMonths(1)
                        val quota =
                            AnalysisQuota(
                                monthlyRemaining = exception.remainingQuota.monthly,
                                resetDate = resetDate,
                            )
                        _analysisQuota.value = quota
                        onQuotaExceeded(quota)
                        CloudLogger.info(TAG, "Analysis quota exceeded")
                        return@withContext null
                    }
                    throw exception ?: Exception("Analysis failed")
                }

                val response = result.getOrThrow()
                val resetDate = LocalDate.now().withDayOfMonth(1).plusMonths(1)
                _analysisQuota.value =
                    AnalysisQuota(
                        monthlyRemaining = response.quota.remaining.monthly,
                        resetDate = resetDate,
                    )

                val analysisJson = gson.toJson(response.analysis)
                CloudLogger.debug(TAG, "Received analysis response from Cloud Function")
                CloudLogger.debug(TAG, "Response JSON length: ${analysisJson.length} chars")

                val startDate = workouts.firstOrNull()?.date?.toLocalDate() ?: LocalDate.now()
                val endDate = workouts.lastOrNull()?.date?.toLocalDate() ?: LocalDate.now()
                val analysis = parseAIResponse(analysisJson, startDate, endDate)
                trace?.stop()
                analysis
            } catch (e: java.io.IOException) {
                trace?.stop()
                ExceptionLogger.logException(TAG, "Analysis failed", e)
                throw e
            } catch (e: IllegalStateException) {
                trace?.stop()
                ExceptionLogger.logException(TAG, "Analysis failed", e)
                throw e
            }
        }

    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: IllegalStateException) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance not available - likely in test environment", e)
            null
        } catch (e: ExceptionInInitializerError) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance trace creation failed", e)
            null
        } catch (e: NoClassDefFoundError) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance trace creation failed", e)
            null
        }

    private suspend fun buildAnalysisPayload(workouts: List<WorkoutSummary>): String {
        val payload = JsonObject()

        val metadata = calculateAnalysisMetadata(workouts)

        val period = JsonObject()
        period.addProperty("start_date", metadata.startDate.toString())
        period.addProperty("end_date", metadata.endDate.toString())
        period.addProperty("total_workouts", metadata.totalWorkouts)
        period.addProperty("total_weeks", metadata.totalWeeks)
        period.addProperty("avg_frequency_per_week", String.format(Locale.US, "%.1f", metadata.avgFrequencyPerWeek))
        payload.add("analysis_period", period)

        // Batch fetch all data upfront to avoid N+1 queries
        val workoutIds = workouts.map { it.id }
        val allExerciseLogs = repository.getExerciseLogsForWorkouts(workoutIds)
        val exerciseLogsByWorkout = allExerciseLogs.groupBy { it.workoutId }

        val exerciseLogIds = allExerciseLogs.map { it.id }
        val allSetLogs = repository.getSetLogsForExercises(exerciseLogIds)
        val setLogsByExerciseLog = allSetLogs.groupBy { it.exerciseLogId }

        val uniqueExerciseIds = allExerciseLogs.map { it.exerciseId }.distinct()
        val exerciseEntities = repository.getExercisesByIds(uniqueExerciseIds)
        val exerciseMap = exerciseEntities.associateBy { it.id }

        val allSets = mutableListOf<com.github.radupana.featherweight.data.SetLog>()
        val setsByExercise = mutableMapOf<String, MutableList<com.github.radupana.featherweight.data.SetLog>>()
        val exercisesUsed = mutableMapOf<String, com.github.radupana.featherweight.data.exercise.Exercise>()
        val workoutSessions = mutableListOf<com.github.radupana.featherweight.service.WorkoutSessionData>()

        val workoutsArray = com.google.gson.JsonArray()

        for (workout in workouts) {
            val workoutObj = JsonObject()
            workoutObj.addProperty("id", workout.id)
            workoutObj.addProperty("date", workout.date.toLocalDate().toString())
            workoutObj.addProperty("name", workout.name ?: "Workout")
            workoutObj.addProperty("duration_minutes", workout.duration?.div(60) ?: 0)
            workoutObj.addProperty("notes", "")

            val exercises = exerciseLogsByWorkout[workout.id] ?: emptyList()
            val exercisesArray = com.google.gson.JsonArray()
            val sessionData = mutableListOf<com.github.radupana.featherweight.service.ExerciseSessionData>()

            for (exercise in exercises) {
                val exerciseObj = JsonObject()
                val exerciseEntity = exerciseMap[exercise.exerciseId]
                val exerciseName = exerciseEntity?.name ?: "Unknown Exercise"
                exerciseObj.addProperty("name", exerciseName)

                if (exerciseEntity != null) {
                    exercisesUsed[exercise.exerciseId] = exerciseEntity
                }

                val sets = setLogsByExerciseLog[exercise.id] ?: emptyList()
                val setsArray = com.google.gson.JsonArray()

                allSets.addAll(sets)
                setsByExercise.getOrPut(exercise.exerciseId) { mutableListOf() }.addAll(sets)

                var maxWeight = 0f
                var totalVolume = 0f

                for ((index, set) in sets.withIndex()) {
                    val setObj = JsonObject()
                    setObj.addProperty("set_number", index + 1)
                    val weight = set.actualWeight ?: 0f
                    setObj.addProperty("weight", weight)
                    setObj.addProperty("reps", set.actualReps ?: set.targetReps)
                    setObj.addProperty("rpe", set.actualRpe)
                    setObj.addProperty("rest_seconds", 180)
                    setObj.addProperty("completed", set.isCompleted)
                    setsArray.add(setObj)

                    if (set.isCompleted) {
                        if (weight > maxWeight) maxWeight = weight
                        totalVolume += weight * (set.actualReps ?: 0)
                    }
                }

                exerciseObj.add("sets", setsArray)
                exercisesArray.add(exerciseObj)
            }

            workoutSessions.add(
                com.github.radupana.featherweight.service.WorkoutSessionData(
                    date = workout.date.toLocalDate().toString(),
                    exerciseData = sessionData,
                ),
            )

            workoutObj.add("exercises", exercisesArray)
            workoutsArray.add(workoutObj)
        }

        payload.add("workouts", workoutsArray)

        val volumeMetrics =
            com.github.radupana.featherweight.service.TrainingMetricsCalculator.calculateVolumeMetrics(
                exercisesUsed,
                setsByExercise,
            )
        val intensityMetrics =
            com.github.radupana.featherweight.service.TrainingMetricsCalculator
                .calculateIntensityMetrics(allSets)
        val progressionMetrics =
            com.github.radupana.featherweight.service.TrainingMetricsCalculator.calculateProgressionMetrics(
                exercisesUsed,
                workoutSessions,
            )

        val metricsObj = JsonObject()

        val volumeObj = JsonObject()
        volumeObj.addProperty("total_sets", volumeMetrics.totalSets)
        volumeObj.addProperty("total_completed_sets", volumeMetrics.totalCompletedSets)
        volumeObj.addProperty("compound_sets", volumeMetrics.compoundSets)
        volumeObj.addProperty("isolation_sets", volumeMetrics.isolationSets)
        volumeObj.addProperty("push_sets", volumeMetrics.pushSets)
        volumeObj.addProperty("pull_sets", volumeMetrics.pullSets)
        volumeObj.addProperty(
            "push_pull_ratio",
            if (volumeMetrics.pullSets > 0) {
                String.format(Locale.US, "%.2f", volumeMetrics.pushSets.toFloat() / volumeMetrics.pullSets)
            } else {
                "N/A"
            },
        )
        volumeObj.addProperty("squat_sets", volumeMetrics.squatSets)
        volumeObj.addProperty("hinge_sets", volumeMetrics.hingeSets)

        val categoryObj = JsonObject()
        volumeMetrics.setsByCategory.forEach { (category, count) ->
            categoryObj.addProperty(category, count)
        }
        volumeObj.add("sets_by_category", categoryObj)
        metricsObj.add("volume", volumeObj)

        val intensityObj = JsonObject()
        intensityObj.addProperty("avg_rpe", String.format(Locale.US, "%.2f", intensityMetrics.avgRpe))
        intensityObj.addProperty("sets_with_rpe", intensityMetrics.setsWithRpe)
        intensityObj.addProperty("sets_above_rpe_8", intensityMetrics.setsAboveRpe8)
        intensityObj.addProperty("sets_below_rpe_6", intensityMetrics.setsBelowRpe6)
        if (intensityMetrics.setsWithRpe > 0) {
            intensityObj.addProperty(
                "pct_high_intensity",
                String.format(Locale.US, "%.1f", intensityMetrics.setsAboveRpe8.toFloat() / intensityMetrics.setsWithRpe * 100),
            )
            intensityObj.addProperty(
                "pct_low_intensity",
                String.format(Locale.US, "%.1f", intensityMetrics.setsBelowRpe6.toFloat() / intensityMetrics.setsWithRpe * 100),
            )
        }
        metricsObj.add("intensity", intensityObj)

        val progressionArray = com.google.gson.JsonArray()
        val progressingExercises = progressionMetrics.filter { it.isProgressing }
        val plateauedExercises = progressionMetrics.filter { it.isPlateaued }

        progressingExercises.forEach { prog ->
            val progObj = JsonObject()
            progObj.addProperty("exercise", prog.exerciseName)
            progObj.addProperty("status", "progressing")
            progObj.addProperty("sessions", prog.sessions.size)
            progressionArray.add(progObj)
        }

        plateauedExercises.forEach { prog ->
            val progObj = JsonObject()
            progObj.addProperty("exercise", prog.exerciseName)
            progObj.addProperty("status", "plateaued")
            progObj.addProperty("sessions", prog.sessions.size)
            progObj.addProperty("weight", String.format(Locale.US, "%.1f", prog.sessions.last().maxWeight))
            progressionArray.add(progObj)
        }

        metricsObj.add("progression", progressionArray)

        payload.add("training_metrics", metricsObj)

        val prs = repository.getRecentPRs(limit = 20)
        val prsArray = com.google.gson.JsonArray()

        // Batch fetch exercise names for PRs
        val prExerciseIds = prs.map { it.exerciseId }.distinct()
        val prExerciseEntities = repository.getExercisesByIds(prExerciseIds)
        val prExerciseMap = prExerciseEntities.associateBy { it.id }

        for (pr in prs) {
            val prObj = JsonObject()
            val exerciseName = prExerciseMap[pr.exerciseId]?.name ?: "Unknown Exercise"
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

        addProgrammeDeviationSummary(payload)

        return gson.toJson(payload)
    }

    private suspend fun addProgrammeDeviationSummary(payload: JsonObject) {
        try {
            // Find the most recent active or completed programme with deviation data
            val programmeId = repository.getMostRecentProgrammeWithDeviations() ?: return

            val programmeDetails = repository.getProgrammeWithDetails(programmeId) ?: return

            val deviations = repository.getDeviationsForProgramme(programmeId)
            if (deviations.isEmpty()) return

            val summary = deviationSummaryService.summarizeDeviations(deviations, programmeDetails)

            val deviationObj = JsonObject()
            deviationObj.addProperty("programme_name", summary.programmeName)
            deviationObj.addProperty("programme_type", summary.programmeType)
            deviationObj.addProperty("duration_weeks", summary.durationWeeks)
            deviationObj.addProperty("workouts_completed", summary.workoutsCompleted)
            deviationObj.addProperty("workouts_prescribed", summary.workoutsPrescribed)
            deviationObj.addProperty(
                "avg_volume_deviation_percent",
                String.format(Locale.US, "%.1f", summary.avgVolumeDeviationPercent),
            )
            deviationObj.addProperty(
                "avg_intensity_deviation_percent",
                String.format(Locale.US, "%.1f", summary.avgIntensityDeviationPercent),
            )
            deviationObj.addProperty("exercise_swap_count", summary.exerciseSwapCount)
            deviationObj.addProperty("exercise_skip_count", summary.exerciseSkipCount)
            deviationObj.addProperty("exercise_add_count", summary.exerciseAddCount)

            val keyDeviationsArray = com.google.gson.JsonArray()
            summary.keyDeviations.forEach { keyDeviationsArray.add(it) }
            deviationObj.add("key_deviations", keyDeviationsArray)

            payload.add("programme_deviation_summary", deviationObj)
        } catch (e: android.database.sqlite.SQLiteException) {
            // Log but don't fail the entire analysis - deviation data is supplementary
            ExceptionLogger.logNonCritical(TAG, "Failed to add deviation summary", e)
        } catch (e: IllegalStateException) {
            ExceptionLogger.logNonCritical(TAG, "Failed to add deviation summary", e)
        }
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
                val category = parseInsightCategory(insight.get("category")?.asString)
                val severity = parseInsightSeverity(insight.get("severity")?.asString)

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

            val adherenceAnalysisJson = parseAdherenceAnalysis(jsonResponse)

            return TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate,
                overallAssessment = overallAssessment,
                keyInsightsJson = gson.toJson(keyInsights),
                recommendationsJson = gson.toJson(recommendations),
                warningsJson = gson.toJson(warnings),
                adherenceAnalysisJson = adherenceAnalysisJson,
            )
        } catch (e: com.google.gson.JsonSyntaxException) {
            ExceptionLogger.logException(TAG, "Failed to parse AI analysis, using fallback", e)
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
            ExceptionLogger.logException(TAG, "Failed to parse AI analysis, using fallback", e)
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

    private fun parseInsightCategory(categoryStr: String?): InsightCategory {
        if (categoryStr == null) return InsightCategory.PROGRESSION

        val normalized = categoryStr.uppercase().replace(" ", "_")
        val validCategories = InsightCategory.entries.map { it.name }

        if (normalized in validCategories) {
            return InsightCategory.valueOf(normalized)
        }

        return when {
            normalized.contains("VOLUME") -> InsightCategory.VOLUME
            normalized.contains("INTENSITY") -> InsightCategory.INTENSITY
            normalized.contains("FREQUENCY") || normalized.contains("CONSISTENCY") ->
                InsightCategory.CONSISTENCY
            normalized.contains("PROGRESS") -> InsightCategory.PROGRESSION
            normalized.contains("RECOVERY") || normalized.contains("PLATEAU") ->
                InsightCategory.RECOVERY
            normalized.contains("BALANCE") -> InsightCategory.BALANCE
            normalized.contains("TECHNIQUE") || normalized.contains("FORM") ->
                InsightCategory.TECHNIQUE
            else -> {
                Log.d(TAG, "Unknown category '$categoryStr', defaulting to PROGRESSION")
                InsightCategory.PROGRESSION
            }
        }
    }

    private fun parseInsightSeverity(severityStr: String?): InsightSeverity {
        if (severityStr == null) return InsightSeverity.INFO

        val normalized = severityStr.uppercase()
        val validSeverities = InsightSeverity.entries.map { it.name }

        if (normalized in validSeverities) {
            return InsightSeverity.valueOf(normalized)
        }

        return when (normalized) {
            "LOW", "MINOR" -> InsightSeverity.INFO
            "MEDIUM", "MODERATE" -> InsightSeverity.WARNING
            "HIGH", "SEVERE" -> InsightSeverity.CRITICAL
            "SUCCESS", "GOOD", "POSITIVE" -> InsightSeverity.SUCCESS
            else -> {
                Log.d(TAG, "Unknown severity '$severityStr', defaulting to INFO")
                InsightSeverity.INFO
            }
        }
    }

    private fun parseAdherenceAnalysis(jsonResponse: JsonObject): String? {
        val adherenceObj = jsonResponse.get("adherence_analysis")
        if (adherenceObj == null || adherenceObj.isJsonNull) return null

        return try {
            val obj = adherenceObj.asJsonObject

            val positivePatterns = mutableListOf<String>()
            obj.getAsJsonArray("positive_patterns")?.forEach { element ->
                positivePatterns.add(element.asString)
            }

            val negativePatterns = mutableListOf<String>()
            obj.getAsJsonArray("negative_patterns")?.forEach { element ->
                negativePatterns.add(element.asString)
            }

            val adherenceRecommendations = mutableListOf<String>()
            obj.getAsJsonArray("adherence_recommendations")?.forEach { element ->
                adherenceRecommendations.add(element.asString)
            }

            val analysis =
                AdherenceAnalysis(
                    adherenceScore = obj.get("adherence_score")?.asInt ?: 0,
                    scoreExplanation = obj.get("score_explanation")?.asString ?: "",
                    positivePatterns = positivePatterns,
                    negativePatterns = negativePatterns,
                    adherenceRecommendations = adherenceRecommendations,
                )

            gson.toJson(analysis)
        } catch (e: IllegalStateException) {
            CloudLogger.warn(TAG, "Failed to parse adherence analysis", e)
            null
        } catch (e: UnsupportedOperationException) {
            CloudLogger.warn(TAG, "Failed to parse adherence analysis", e)
            null
        }
    }
}
