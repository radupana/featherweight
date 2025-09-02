package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.model.IntensityLevel
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlin.math.roundToInt

class WorkoutTemplateWeightService(
    private val repository: FeatherweightRepository,
    private val oneRMDao: OneRMDao,
    private val setLogDao: SetLogDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val freestyleIntelligenceService: FreestyleIntelligenceService,
) {
    /**
     * Apply weight suggestions to all sets in a template-generated workout
     */
    suspend fun applyWeightSuggestions(
        workoutId: Long,
        config: WorkoutTemplateConfig,
    ) {
        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

        for (exerciseLog in exerciseLogs) {
            val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
            repository.getExerciseById(exerciseLog.exerciseVariationId)

            val suggestedWeight =
                getSuggestedWeight(
                    exerciseVariationId = exerciseLog.exerciseVariationId,
                    targetReps = sets.firstOrNull()?.targetReps ?: 10,
                    intensity = config.intensity,
                )

            for (set in sets) {
                setLogDao.update(
                    set.copy(
                        suggestedWeight = suggestedWeight.weight,
                        suggestionSource = suggestedWeight.explanation,
                        targetWeight = suggestedWeight.weight,
                        actualWeight = suggestedWeight.weight,
                        actualReps = set.targetReps ?: 0,
                        suggestionConfidence = suggestedWeight.confidence,
                    ),
                )
            }
        }
    }

    private suspend fun getSuggestedWeight(
        exerciseVariationId: Long,
        targetReps: Int,
        intensity: IntensityLevel,
    ): WeightSuggestion {
        val exerciseMax = oneRMDao.getCurrentMax(exerciseVariationId)
        if (exerciseMax != null) {
            val percentage = getIntensityPercentage(intensity)
            val weight = calculateWeightFromPercentage(exerciseMax.oneRMEstimate.toDouble(), percentage, targetReps)
            return WeightSuggestion(
                weight = weight.toFloat(),
                confidence = 0.9f,
                sources =
                    listOf(
                        SuggestionSourceData(
                            source = SuggestionSource.ONE_RM_CALCULATION,
                            value = exerciseMax.oneRMEstimate,
                            weight = 1.0f,
                            details = "Profile 1RM: ${exerciseMax.oneRMEstimate}kg at ${(percentage * 100).toInt()}%",
                        ),
                    ),
                explanation = "Based on profile 1RM",
            )
        }

        val recentSets =
            repository.getRecentSetLogsForExercise(
                exerciseVariationId = exerciseVariationId,
                daysBack = 42,
            )

        if (recentSets.isNotEmpty()) {
            val estimated1RM = estimateFrom1RMFromHistory(recentSets)
            if (estimated1RM != null) {
                val percentage = getIntensityPercentage(intensity)
                val weight = calculateWeightFromPercentage(estimated1RM, percentage, targetReps)
                return WeightSuggestion(
                    weight = weight.toFloat(),
                    confidence = 0.8f,
                    sources =
                        listOf(
                            SuggestionSourceData(
                                source = SuggestionSource.RECENT_PERFORMANCE,
                                value = estimated1RM.toFloat(),
                                weight = 0.8f,
                                details = "Estimated 1RM: ${estimated1RM.toInt()}kg from recent history",
                            ),
                        ),
                    explanation = "Based on recent performance",
                )
            }
        }

        val smartSuggestions =
            freestyleIntelligenceService.getIntelligentSuggestions(
                exerciseVariationId = exerciseVariationId,
                targetReps = targetReps,
            )

        return WeightSuggestion(
            weight = smartSuggestions.suggestedWeight,
            confidence = 0.7f,
            sources =
                listOf(
                    SuggestionSourceData(
                        source = SuggestionSource.HISTORICAL_AVERAGE,
                        value = smartSuggestions.suggestedWeight,
                        weight = 0.7f,
                        details = "Freestyle intelligence suggestion",
                    ),
                ),
            explanation = smartSuggestions.reasoning,
        )
    }

    private fun estimateFrom1RMFromHistory(recentSets: List<SetLog>): Double? {
        val validSets =
            recentSets.filter {
                it.actualWeight > 0 &&
                    it.actualReps > 0 &&
                    it.isCompleted
            }

        if (validSets.isEmpty()) return null

        val estimated1RMs =
            validSets.map { set ->
                val weight = set.actualWeight.toDouble()
                val reps = set.actualReps
                val rpe = set.actualRpe

                val baseEstimate = weight * (1 + reps / 30.0)

                if (rpe != null) {
                    val rpeMultiplier =
                        when {
                            rpe >= 10f -> 1.0
                            rpe >= 9f -> 0.95
                            rpe >= 8f -> 0.9
                            rpe >= 7f -> 0.85
                            rpe >= 6f -> 0.8
                            else -> 0.75
                        }
                    baseEstimate * rpeMultiplier
                } else {
                    baseEstimate * 0.875
                }
            }

        return if (estimated1RMs.isNotEmpty()) {
            estimated1RMs.sorted()[estimated1RMs.size / 2]
        } else {
            null
        }
    }

    private fun getIntensityPercentage(intensity: IntensityLevel): Double =
        when (intensity) {
            IntensityLevel.CONSERVATIVE -> 0.675
            IntensityLevel.MODERATE -> 0.725
            IntensityLevel.AGGRESSIVE -> 0.775
        }

    private fun calculateWeightFromPercentage(
        oneRM: Double,
        percentage: Double,
        targetReps: Int,
    ): Double {
        // Adjust percentage based on rep range
        val repAdjustment =
            when {
                targetReps <= 5 -> 1.05 // Strength work can handle slightly more
                targetReps >= 15 -> 0.9 // Endurance work needs lower percentage
                else -> 1.0
            }

        return (oneRM * percentage * repAdjustment).roundToNearestTwoPointFive()
    }

    private fun Double.roundToNearestTwoPointFive(): Double = (this / 2.5).roundToInt() * 2.5
}
