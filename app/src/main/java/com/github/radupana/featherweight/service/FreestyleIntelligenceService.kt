package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.domain.AlternativeSuggestion
import com.github.radupana.featherweight.domain.SmartSuggestions
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * Service that provides intelligent weight/rep suggestions for freestyle workouts
 * by analyzing global exercise progress, RPE trends, and performance patterns.
 */
class FreestyleIntelligenceService(
    private val globalProgressDao: GlobalExerciseProgressDao,
) {
    enum class RpeTrendAnalysis {
        TOO_EASY, // RPE < 7 - Can handle more weight
        OPTIMAL, // RPE 7-9 - Good training zone
        TOO_HARD, // RPE > 9 - Consider backing off
        UNKNOWN, // No RPE data
    }

    /**
     * Get intelligent weight suggestions for an exercise with detailed reasoning
     */
    suspend fun getIntelligentSuggestions(
        exerciseVariationId: Long,
        targetReps: Int? = null,
    ): SmartSuggestions {
        val progress = globalProgressDao.getProgressForExercise(exerciseVariationId)

        // If no progress data, fallback to basic suggestions
        if (progress == null) {
            return getBasicSuggestions(targetReps)
        }

        // Analyze RPE trend
        val rpeTrend = analyzeRpeTrend(progress)

        // Determine primary suggestion based on current state
        return when {
            // User has been hitting low RPEs - time to increase weight
            rpeTrend == RpeTrendAnalysis.TOO_EASY && progress.consecutiveStalls < 2 -> {
                suggestProgressiveOverload(progress, targetReps)
            }

            // Stalled for 3+ sessions - suggest deload or variation
            progress.consecutiveStalls >= 3 -> {
                suggestDeloadOrVariation(progress, targetReps)
            }

            // High RPE trend - maintain or slightly decrease
            rpeTrend == RpeTrendAnalysis.TOO_HARD -> {
                suggestMaintainOrDecrease(progress, targetReps)
            }

            // Normal progression based on trend
            else -> {
                suggestBasedOnTrend(progress, targetReps, rpeTrend)
            }
        }
    }

    /**
     * Get real-time suggestions as user types different rep counts
     */
    fun getSuggestionsForReps(
        exerciseVariationId: Long,
        repsFlow: kotlinx.coroutines.flow.Flow<Int>,
    ): kotlinx.coroutines.flow.Flow<SmartSuggestions> =
        repsFlow.map { reps ->
            getIntelligentSuggestions(exerciseVariationId, reps)
        }

    private fun analyzeRpeTrend(progress: GlobalExerciseProgress): RpeTrendAnalysis =
        when {
            progress.recentAvgRpe == null -> RpeTrendAnalysis.UNKNOWN
            progress.recentAvgRpe < 7f -> RpeTrendAnalysis.TOO_EASY
            progress.recentAvgRpe > 9f -> RpeTrendAnalysis.TOO_HARD
            else -> RpeTrendAnalysis.OPTIMAL
        }

    private fun suggestProgressiveOverload(
        progress: GlobalExerciseProgress,
        targetReps: Int?,
    ): SmartSuggestions {
        val reps = targetReps ?: 5 // Default to 5 reps if not specified
        val baseWeight = progress.currentWorkingWeight

        // Calculate weight increase based on RPE
        val increasePercentage =
            when {
                progress.recentAvgRpe != null && progress.recentAvgRpe < 6f -> 0.075f // +7.5%
                progress.recentAvgRpe != null && progress.recentAvgRpe < 7f -> 0.05f // +5%
                else -> 0.025f // +2.5% standard progression
            }

        val suggestedWeight = roundToNearestPlate(baseWeight * (1 + increasePercentage))

        val reasoning =
            buildString {
                append("Progressive overload recommended. ")
                append("Your average RPE of ${progress.recentAvgRpe?.format(1)} indicates ")
                append("you have capacity for more weight. ")
                append("Increasing from ${baseWeight}kg by ${(increasePercentage * 100).roundToInt()}%.")
            }

        return SmartSuggestions(
            suggestedWeight = suggestedWeight,
            suggestedReps = reps,
            suggestedRpe = 8f, // Target RPE 8 for sustainable progress
            lastWorkoutDate = progress.lastUpdated,
            confidence = "High - Based on ${progress.sessionsTracked} sessions",
            reasoning = reasoning,
            alternativeSuggestions = generateAlternatives(suggestedWeight, reps, progress),
        )
    }

    private fun suggestDeloadOrVariation(
        progress: GlobalExerciseProgress,
        targetReps: Int?,
    ): SmartSuggestions {
        val reps = targetReps ?: 5
        val deloadWeight = roundToNearestPlate(progress.currentWorkingWeight * 0.85f) // 15% deload

        val reasoning =
            buildString {
                append("Deload recommended. ")
                append("You've been at ${progress.currentWorkingWeight}kg for ${progress.consecutiveStalls} sessions. ")
                append("Reducing to 85% (${deloadWeight}kg) will help break through this plateau. ")
                if (progress.recentAvgRpe != null && progress.recentAvgRpe > 8.5f) {
                    append("Your high RPE (${progress.recentAvgRpe.format(1)}) also indicates accumulated fatigue.")
                }
            }

        // Suggest both deload and rep/intensity variations
        val alternatives =
            listOf(
                AlternativeSuggestion(
                    reps = 3,
                    weight = progress.currentWorkingWeight,
                    rpe = 7f,
                    reasoning = "Lower reps at same weight to maintain intensity",
                ),
                AlternativeSuggestion(
                    reps = 8,
                    weight = roundToNearestPlate(progress.currentWorkingWeight * 0.9f),
                    rpe = 8f,
                    reasoning = "Higher reps at lower weight for volume",
                ),
                AlternativeSuggestion(
                    reps = reps,
                    weight = deloadWeight,
                    rpe = 6f,
                    reasoning = "Standard deload - reduce fatigue",
                ),
            )

        return SmartSuggestions(
            suggestedWeight = deloadWeight,
            suggestedReps = reps,
            suggestedRpe = 6f, // Lower RPE for recovery
            lastWorkoutDate = progress.lastUpdated,
            confidence = "High - Clear stall pattern detected",
            reasoning = reasoning,
            alternativeSuggestions = alternatives,
        )
    }

    private fun suggestMaintainOrDecrease(
        progress: GlobalExerciseProgress,
        targetReps: Int?,
    ): SmartSuggestions {
        val reps = targetReps ?: 5
        val baseWeight = progress.currentWorkingWeight

        // High RPE suggests backing off slightly
        val suggestedWeight =
            if (progress.recentAvgRpe != null && progress.recentAvgRpe > 9.5f) {
                roundToNearestPlate(baseWeight * 0.95f) // -5%
            } else {
                baseWeight // Maintain
            }

        val reasoning =
            buildString {
                append("Maintain or reduce intensity. ")
                append("Your recent RPE of ${progress.recentAvgRpe?.format(1)} indicates high effort. ")
                if (suggestedWeight < baseWeight) {
                    append("Reducing weight by 5% to manage fatigue and maintain form.")
                } else {
                    append("Maintaining current weight while focusing on technique.")
                }
            }

        return SmartSuggestions(
            suggestedWeight = suggestedWeight,
            suggestedReps = reps,
            suggestedRpe = 7f, // Target lower RPE
            lastWorkoutDate = progress.lastUpdated,
            confidence = "Medium - Based on RPE feedback",
            reasoning = reasoning,
            alternativeSuggestions = generateAlternatives(suggestedWeight, reps, progress),
        )
    }

    private fun suggestBasedOnTrend(
        progress: GlobalExerciseProgress,
        targetReps: Int?,
        rpeTrend: RpeTrendAnalysis,
    ): SmartSuggestions {
        val reps = targetReps ?: 5
        val baseWeight = progress.currentWorkingWeight

        val (suggestedWeight, reasoning) =
            when (progress.trend) {
                ProgressTrend.IMPROVING -> {
                    val increase = if (rpeTrend == RpeTrendAnalysis.OPTIMAL) 0.025f else 0.0f
                    val weight = roundToNearestPlate(baseWeight * (1 + increase))
                    weight to "Steady progress detected. ${if (increase > 0) "Small increase recommended." else "Maintain current trajectory."}"
                }

                ProgressTrend.STALLING -> {
                    baseWeight to "Approaching a stall (${progress.consecutiveStalls} sessions). Consider varying intensity or volume next session."
                }

                ProgressTrend.DECLINING -> {
                    val weight = roundToNearestPlate(baseWeight * 0.95f)
                    weight to "Performance declining. Reduce weight by 5% and focus on recovery."
                }
            }

        return SmartSuggestions(
            suggestedWeight = suggestedWeight,
            suggestedReps = reps,
            suggestedRpe = 8f,
            lastWorkoutDate = progress.lastUpdated,
            confidence = "Medium - Based on ${progress.sessionsTracked} sessions",
            reasoning = reasoning,
            alternativeSuggestions = generateAlternatives(suggestedWeight, reps, progress),
        )
    }

    private fun getBasicSuggestions(
        targetReps: Int?,
    ): SmartSuggestions {
        // Return basic suggestions when no historical data exists
        return SmartSuggestions(
            suggestedWeight = 0f,
            suggestedReps = targetReps ?: 5,
            suggestedRpe = null,
            lastWorkoutDate = null,
            confidence = "Low - No historical data",
            reasoning = "No previous data for this exercise. Start conservatively and track your RPE.",
            alternativeSuggestions = emptyList(),
        )
    }

    private fun generateAlternatives(
        baseWeight: Float,
        baseReps: Int,
        progress: GlobalExerciseProgress,
    ): List<AlternativeSuggestion> {
        val alternatives = mutableListOf<AlternativeSuggestion>()

        // Use 1RM if available for percentage-based suggestions
        if (progress.estimatedMax > 0) {
            // Heavy singles/doubles
            if (baseReps > 3) {
                alternatives.add(
                    AlternativeSuggestion(
                        reps = 2,
                        weight = roundToNearestPlate(progress.estimatedMax * 0.9f),
                        rpe = 8f,
                        reasoning = "Heavy double at ~90% 1RM",
                    ),
                )
            }

            // Volume work
            if (baseReps < 8) {
                alternatives.add(
                    AlternativeSuggestion(
                        reps = 8,
                        weight = roundToNearestPlate(progress.estimatedMax * 0.7f),
                        rpe = 8f,
                        reasoning = "Volume work at ~70% 1RM",
                    ),
                )
            }
        }

        // Rep range variations
        val repRanges = listOf(3, 5, 8, 10, 12).filter { it != baseReps }
        repRanges.take(2).forEach { reps ->
            val repAdjustment =
                when {
                    reps < baseReps -> 1.0f + ((baseReps - reps) * 0.05f) // +5% per rep decrease
                    else -> 1.0f - ((reps - baseReps) * 0.025f) // -2.5% per rep increase
                }

            alternatives.add(
                AlternativeSuggestion(
                    reps = reps,
                    weight = roundToNearestPlate(baseWeight * repAdjustment),
                    rpe = null,
                    reasoning = "${if (reps < baseReps) "Lower" else "Higher"} rep alternative",
                ),
            )
        }

        return alternatives
    }

    private fun roundToNearestPlate(weight: Float): Float {
        // Round to nearest 2.5kg plate
        return (weight / 2.5f).roundToInt() * 2.5f
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}
