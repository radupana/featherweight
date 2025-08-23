package com.github.radupana.featherweight.service

import kotlinx.coroutines.flow.Flow

/**
 * Core engine for providing intelligent weight and rep suggestions based on multiple data sources
 */
interface IntelligentSuggestionEngine {
    suspend fun suggestWeight(
        exerciseName: String,
        targetReps: Int?,
    ): WeightSuggestion

    suspend fun suggestReps(
        exerciseName: String,
        targetWeight: Float,
    ): RepsSuggestion

    suspend fun explainSuggestion(
        exerciseName: String,
        suggestion: WeightSuggestion,
    ): String

    // Real-time suggestions as user types
    fun observeWeightSuggestions(
        exerciseName: String,
        targetReps: Flow<Int>,
    ): Flow<WeightSuggestion>

    fun observeRepsSuggestions(
        exerciseName: String,
        targetWeight: Flow<Float>,
    ): Flow<RepsSuggestion>
}

data class WeightSuggestion(
    val weight: Float,
    val confidence: Float, // 0.0-1.0
    val sources: List<SuggestionSourceData>,
    val explanation: String,
    val alternativeWeights: Map<Int, Float> = emptyMap(), // reps -> weight for quick switching
)

data class RepsSuggestion(
    val reps: Int,
    val confidence: Float,
    val sources: List<SuggestionSourceData>,
    val explanation: String,
    val alternativeReps: Map<Float, Int> = emptyMap(), // weight -> reps for quick switching
)

data class SuggestionSourceData(
    val source: SuggestionSource,
    val value: Float,
    val weight: Float, // How much this source contributes to final suggestion
    val details: String,
)

enum class SuggestionSource {
    ONE_RM_CALCULATION,
    HISTORICAL_AVERAGE,
    PROGRAMME_PRESCRIPTION,
    CROSS_EXERCISE_CORRELATION,
    RECENT_PERFORMANCE,
    AI_PREDICTION,
}

data class PerformanceData(
    val targetReps: Int?,
    val targetWeight: Float?,
    val actualReps: Int,
    val actualWeight: Float,
    val actualRpe: Float?,
    val timestamp: String,
)
