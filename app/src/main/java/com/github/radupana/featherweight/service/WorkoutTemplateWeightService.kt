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
        userId: Long,
    ) {
        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

        for (exerciseLog in exerciseLogs) {
            val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)
            val exercise = repository.getExerciseById(exerciseLog.exerciseVariationId)
            val exerciseName = exercise?.name ?: "Unknown Exercise"

            // Try to get weight suggestion
            val suggestedWeight =
                getSuggestedWeight(
                    exerciseVariationId = exerciseLog.exerciseVariationId,
                    targetReps = sets.firstOrNull()?.targetReps ?: 10,
                    intensity = config.intensity,
                    userId = userId,
                )

            // Update all sets with the suggested weight
            for (set in sets) {
                setLogDao.update(
                    set.copy(
                        suggestedWeight = suggestedWeight.weight,
                        suggestionSource = suggestedWeight.explanation,
                        targetWeight = suggestedWeight.weight,
                        // Also populate actual values so the checkbox is enabled
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
        userId: Long,
    ): WeightSuggestion {
        // First, check for profile 1RM
        val exerciseMax = oneRMDao.getCurrentMax(userId, exerciseVariationId)
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

        // Second, check recent workout history
        val recentSets =
            repository.getRecentSetLogsForExercise(
                exerciseVariationId = exerciseVariationId,
                daysBack = 42, // 6 weeks
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

        // No data available - let FreestyleIntelligenceService handle it
        val smartSuggestions =
            freestyleIntelligenceService.getIntelligentSuggestions(
                exerciseVariationId = exerciseVariationId,
                userId = userId,
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
                val rpe = set.actualRpe?.toInt()

                // Epley formula with RPE adjustment
                val baseEstimate = weight * (1 + reps / 30.0)

                if (rpe != null) {
                    // RPE adjustment multiplier
                    val rpeMultiplier =
                        when (rpe) {
                            10 -> 1.0
                            9 -> 0.95
                            8 -> 0.9
                            7 -> 0.85
                            6 -> 0.8
                            else -> 0.75
                        }
                    baseEstimate * rpeMultiplier
                } else {
                    // Conservative estimate without RPE (assume RPE 7-8)
                    baseEstimate * 0.875
                }
            }

        // Return median for stability
        return if (estimated1RMs.isNotEmpty()) {
            estimated1RMs.sorted()[estimated1RMs.size / 2]
        } else {
            null
        }
    }

    private fun getIntensityPercentage(intensity: IntensityLevel): Double =
        when (intensity) {
            IntensityLevel.CONSERVATIVE -> 0.675 // 65-70% midpoint
            IntensityLevel.MODERATE -> 0.725 // 70-75% midpoint
            IntensityLevel.AGGRESSIVE -> 0.775 // 75-80% midpoint
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

        return (oneRM * percentage * repAdjustment).roundToNearest2_5()
    }

    private fun Double.roundToNearest2_5(): Double = (this / 2.5).roundToInt() * 2.5
}
