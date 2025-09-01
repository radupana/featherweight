package com.github.radupana.featherweight.service

data class WeightSuggestion(
    val weight: Float,
    val confidence: Float, // 0.0-1.0
    val sources: List<SuggestionSourceData>,
    val explanation: String,
    val alternativeWeights: Map<Int, Float> = emptyMap(), // reps -> weight for quick switching
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
    RECENT_PERFORMANCE,
}

data class PerformanceData(
    val targetReps: Int?,
    val targetWeight: Float?,
    val actualReps: Int,
    val actualWeight: Float,
    val actualRpe: Float?,
    val timestamp: String,
)
