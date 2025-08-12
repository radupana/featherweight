package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * Production implementation of intelligent suggestion engine
 */
class FeatherweightSuggestionEngine(
    private val repository: FeatherweightRepository,
) : IntelligentSuggestionEngine {
    override suspend fun suggestWeight(
        exerciseName: String,
        targetReps: Int?,
    ): WeightSuggestion {
        val sources = mutableListOf<SuggestionSourceData>()
        val weights = mutableListOf<Float>()
        val confidences = mutableListOf<Float>()

        // Get exercise ID from name
        val exercise = repository.getExerciseByName(exerciseName)
        val exerciseVariationId =
            exercise?.id ?: return WeightSuggestion(
                weight = 20f,
                confidence = 0f,
                sources = emptyList(),
                explanation = "Exercise not found",
            )

        // For freestyle workouts (null targetReps), use a default of 8-10 reps for calculations
        val effectiveTargetReps = targetReps ?: 8

        // 1. One RM calculation (if available)
        val oneRM = repository.getOneRMForExercise(exerciseVariationId)
        if (oneRM != null) {
            val percentage = calculatePercentageForReps(effectiveTargetReps)
            val weight = oneRM * percentage
            val confidence = if (targetReps != null) 0.9f else 0.7f // Lower confidence for freestyle
            sources.add(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = weight,
                    weight = 0.4f,
                    details =
                        if (targetReps != null) {
                            "1RM: ${oneRM}kg × ${(percentage * 100).roundToInt()}% for $targetReps reps"
                        } else {
                            "1RM: ${oneRM}kg × ${(percentage * 100).roundToInt()}% (estimated for freestyle)"
                        },
                ),
            )
            weights.add(weight)
            confidences.add(confidence)
        }

        // 2. Historical average for similar rep ranges
        val historicalData = repository.getHistoricalPerformance(exerciseVariationId, effectiveTargetReps - 1, effectiveTargetReps + 1)
        if (historicalData.isNotEmpty()) {
            val avgWeight = historicalData.map { it.actualWeight }.average().toFloat()
            val confidence = if (targetReps != null) 0.7f else 0.6f // Lower confidence for freestyle
            sources.add(
                SuggestionSourceData(
                    source = SuggestionSource.HISTORICAL_AVERAGE,
                    value = avgWeight,
                    weight = 0.3f,
                    details =
                        if (targetReps != null) {
                            "Average from ${historicalData.size} recent $targetReps±1 rep sets"
                        } else {
                            "Average from ${historicalData.size} recent similar rep sets"
                        },
                ),
            )
            weights.add(avgWeight)
            confidences.add(confidence)
        }

        // 3. Recent performance trend
        val recentSets = repository.getRecentPerformance(exerciseVariationId, limit = 5)
        if (recentSets.isNotEmpty()) {
            val trendWeight = calculateTrendWeight(recentSets, effectiveTargetReps)
            sources.add(
                SuggestionSourceData(
                    source = SuggestionSource.RECENT_PERFORMANCE,
                    value = trendWeight,
                    weight = 0.2f,
                    details = "Based on last ${recentSets.size} workouts trend",
                ),
            )
            weights.add(trendWeight)
            confidences.add(0.6f)
        }

        // 4. Cross-exercise correlation (if available)
        val correlatedWeight = calculateCorrelatedWeight(exerciseName, effectiveTargetReps)
        if (correlatedWeight != null) {
            sources.add(
                SuggestionSourceData(
                    source = SuggestionSource.CROSS_EXERCISE_CORRELATION,
                    value = correlatedWeight,
                    weight = 0.1f,
                    details = "Based on related exercise performance",
                ),
            )
            weights.add(correlatedWeight)
            confidences.add(0.5f)
        }

        // Calculate weighted average
        val finalWeight =
            if (weights.isNotEmpty()) {
                calculateWeightedAverage(weights, sources.map { it.weight })
            } else {
                // Fallback: ask user to establish baseline
                0f
            }

        val finalConfidence =
            if (confidences.isNotEmpty()) {
                confidences.average().toFloat()
            } else {
                0f
            }

        // Generate alternative rep ranges
        val alternatives = generateAlternativeWeights(sources, effectiveTargetReps)

        val explanation = buildExplanation(sources, finalWeight, finalConfidence)

        return WeightSuggestion(
            weight = finalWeight,
            confidence = finalConfidence,
            sources = sources,
            explanation = explanation,
            alternativeWeights = alternatives,
        )
    }

    override suspend fun suggestReps(
        exerciseName: String,
        targetWeight: Float,
    ): RepsSuggestion {
        val sources = mutableListOf<SuggestionSourceData>()

        // Get exercise ID from name
        val exercise = repository.getExerciseByName(exerciseName)
        val exerciseVariationId =
            exercise?.id ?: return RepsSuggestion(
                reps = 8,
                confidence = 0f,
                sources = emptyList(),
                explanation = "Exercise not found",
            )

        // 1. One RM calculation (if available)
        val oneRM = repository.getOneRMForExercise(exerciseVariationId)
        val suggestedReps =
            if (oneRM != null && oneRM > 0) {
                calculateRepsForWeight(targetWeight, oneRM)
            } else {
                // Look at historical data for this weight
                val historicalData = repository.getHistoricalPerformanceForWeight(exerciseVariationId, targetWeight - 2.5f, targetWeight + 2.5f)
                if (historicalData.isNotEmpty()) {
                    historicalData.map { it.actualReps }.average().roundToInt()
                } else {
                    8 // Default moderate rep range
                }
            }

        if (oneRM != null) {
            val percentage = targetWeight / oneRM
            sources.add(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = suggestedReps.toFloat(),
                    weight = 0.6f,
                    details = "${targetWeight}kg is ${(percentage * 100).roundToInt()}% of your 1RM (${oneRM}kg)",
                ),
            )
        }

        val explanation = "For ${targetWeight}kg, you should be able to complete approximately $suggestedReps reps"
        val alternatives = generateAlternativeReps(targetWeight, oneRM)

        return RepsSuggestion(
            reps = suggestedReps,
            confidence = if (oneRM != null) 0.8f else 0.4f,
            sources = sources,
            explanation = explanation,
            alternativeReps = alternatives,
        )
    }

    override suspend fun explainSuggestion(
        exerciseName: String,
        suggestion: WeightSuggestion,
    ): String = suggestion.explanation

    override fun observeWeightSuggestions(
        exerciseName: String,
        targetReps: Flow<Int>,
    ): Flow<WeightSuggestion> =
        targetReps.map { reps ->
            suggestWeight(exerciseName, reps)
        }

    override fun observeRepsSuggestions(
        exerciseName: String,
        targetWeight: Flow<Float>,
    ): Flow<RepsSuggestion> =
        targetWeight.map { weight ->
            suggestReps(exerciseName, weight)
        }

    // Helper methods

    private fun calculatePercentageForReps(reps: Int): Float {
        // Using Epley formula approximation
        return when (reps) {
            1 -> 1.0f
            2 -> 0.97f
            3 -> 0.94f
            4 -> 0.92f
            5 -> 0.89f
            6 -> 0.86f
            7 -> 0.83f
            8 -> 0.81f
            9 -> 0.78f
            10 -> 0.75f
            else -> 0.75f - (reps - 10) * 0.02f // Extrapolate for higher reps
        }.coerceAtLeast(0.4f) // Minimum 40% of 1RM
    }

    private fun calculateRepsForWeight(
        targetWeight: Float,
        oneRM: Float,
    ): Int {
        if (oneRM <= 0) return 8 // Default if no 1RM

        val percentage = targetWeight / oneRM

        return when {
            percentage >= 1.0f -> 1
            percentage >= 0.97f -> 2
            percentage >= 0.94f -> 3
            percentage >= 0.92f -> 4
            percentage >= 0.89f -> 5
            percentage >= 0.86f -> 6
            percentage >= 0.83f -> 7
            percentage >= 0.81f -> 8
            percentage >= 0.78f -> 9
            percentage >= 0.75f -> 10
            else -> (10 + ((0.75f - percentage) / 0.02f).roundToInt()).coerceAtMost(20)
        }
    }

    private fun calculateTrendWeight(
        recentSets: List<PerformanceData>,
        targetReps: Int,
    ): Float {
        if (recentSets.isEmpty()) return 0f

        // Simple linear progression calculation
        val relevantSets =
            recentSets.filter {
                kotlin.math.abs(it.actualReps - targetReps) <= 2
            }

        return if (relevantSets.isNotEmpty()) {
            val avgWeight = relevantSets.map { it.actualWeight }.average().toFloat()
            // Small progression bump if recent performance was good
            val successRate =
                relevantSets
                    .count {
                        it.targetReps != null && it.actualReps >= it.targetReps
                    }.toFloat() / relevantSets.size

            if (successRate > 0.8f) {
                avgWeight + 2.5f // Small progression
            } else {
                avgWeight
            }
        } else {
            recentSets.first().actualWeight // Use most recent weight
        }
    }

    private suspend fun calculateCorrelatedWeight(
        exerciseName: String,
        targetReps: Int,
    ): Float? {
        // Basic correlation logic - can be enhanced with more sophisticated analysis
        return when (exerciseName.lowercase()) {
            "barbell bench press" -> {
                val shoulderPressExercise = repository.getExerciseByName("Barbell Overhead Press")
                val shoulderPress = shoulderPressExercise?.id?.let { repository.getOneRMForExercise(it) }
                shoulderPress?.let { it * 1.3f * calculatePercentageForReps(targetReps) }
            }

            "barbell back squat" -> {
                val deadliftExercise = repository.getExerciseByName("Barbell Deadlift")
                val deadlift = deadliftExercise?.id?.let { repository.getOneRMForExercise(it) }
                deadlift?.let { it * 0.85f * calculatePercentageForReps(targetReps) }
            }

            else -> null
        }
    }

    private fun calculateWeightedAverage(
        values: List<Float>,
        weights: List<Float>,
    ): Float {
        if (values.isEmpty() || weights.isEmpty()) return 0f

        val totalWeight = weights.sum()
        val weightedSum = values.zip(weights) { value, weight -> value * weight }.sum()

        return (weightedSum / totalWeight).let {
            // Round to nearest 2.5kg
            (it / 2.5f).roundToInt() * 2.5f
        }
    }

    private fun generateAlternativeWeights(
        sources: List<SuggestionSourceData>,
        currentReps: Int,
    ): Map<Int, Float> {
        if (sources.isEmpty()) return emptyMap()

        val baseWeight =
            sources.firstOrNull { it.source == SuggestionSource.ONE_RM_CALCULATION }?.value
                ?: sources.maxByOrNull { it.weight }?.value
                ?: return emptyMap()

        return mapOf(
            currentReps - 2 to baseWeight * 1.1f,
            currentReps - 1 to baseWeight * 1.05f,
            currentReps + 1 to baseWeight * 0.95f,
            currentReps + 2 to baseWeight * 0.9f,
        ).mapValues { (_, weight) ->
            // Round to nearest 2.5kg
            (weight / 2.5f).roundToInt() * 2.5f
        }.filterValues { it > 0 }
    }

    private fun generateAlternativeReps(
        targetWeight: Float,
        oneRM: Float?,
    ): Map<Float, Int> {
        if (oneRM == null || oneRM <= 0) return emptyMap()

        return mapOf(
            targetWeight - 5f to calculateRepsForWeight(targetWeight - 5f, oneRM),
            targetWeight - 2.5f to calculateRepsForWeight(targetWeight - 2.5f, oneRM),
            targetWeight + 2.5f to calculateRepsForWeight(targetWeight + 2.5f, oneRM),
            targetWeight + 5f to calculateRepsForWeight(targetWeight + 5f, oneRM),
        ).filterKeys { it > 0 }
    }

    private fun buildExplanation(
        sources: List<SuggestionSourceData>,
        finalWeight: Float,
        confidence: Float,
    ): String {
        if (sources.isEmpty()) {
            return "No historical data available. Enter your best estimate to establish a baseline."
        }

        val confidenceText =
            when {
                confidence > 0.8f -> "High confidence"
                confidence > 0.6f -> "Moderate confidence"
                confidence > 0.4f -> "Low confidence"
                else -> "Very low confidence"
            }

        sources.maxByOrNull { it.weight }
        val explanation = StringBuilder("${finalWeight}kg suggested ($confidenceText)\n\n")

        sources.forEach { source ->
            explanation.append("• ${source.details}\n")
        }

        return explanation.toString().trim()
    }
}
