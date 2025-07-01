package com.github.radupana.featherweight.service

import kotlinx.coroutines.flow.Flow

/**
 * Core engine for providing intelligent weight and rep suggestions based on multiple data sources
 */
interface IntelligentSuggestionEngine {
    suspend fun suggestWeight(exerciseName: String, targetReps: Int): WeightSuggestion
    suspend fun suggestReps(exerciseName: String, targetWeight: Float): RepsSuggestion
    suspend fun explainSuggestion(exerciseName: String, suggestion: WeightSuggestion): String
    
    // Real-time suggestions as user types
    fun observeWeightSuggestions(exerciseName: String, targetReps: Flow<Int>): Flow<WeightSuggestion>
    fun observeRepsSuggestions(exerciseName: String, targetWeight: Flow<Float>): Flow<RepsSuggestion>
}

data class WeightSuggestion(
    val weight: Float,
    val confidence: Float, // 0.0-1.0
    val sources: List<SuggestionSourceData>,
    val explanation: String,
    val alternativeWeights: Map<Int, Float> = emptyMap() // reps -> weight for quick switching
)

data class RepsSuggestion(
    val reps: Int,
    val confidence: Float,
    val sources: List<SuggestionSourceData>,
    val explanation: String,
    val alternativeReps: Map<Float, Int> = emptyMap() // weight -> reps for quick switching
)

data class SuggestionSourceData(
    val source: SuggestionSource,
    val value: Float,
    val weight: Float, // How much this source contributes to final suggestion
    val details: String
)

enum class SuggestionSource {
    ONE_RM_CALCULATION,
    HISTORICAL_AVERAGE,
    PROGRAMME_PRESCRIPTION,
    CROSS_EXERCISE_CORRELATION,
    RECENT_PERFORMANCE,
    AI_PREDICTION
}

/**
 * Failure analysis based on target vs actual performance
 */
interface FailureAnalysisEngine {
    suspend fun analyzePerformance(
        exerciseName: String,
        targetReps: Int,
        targetWeight: Float?,
        actualReps: Int,
        actualWeight: Float,
        actualRpe: Float?
    ): PerformanceAnalysis
    
    suspend fun detectStall(exerciseName: String, recentSets: List<PerformanceData>): StallAnalysis
    suspend fun suggestDeload(stallAnalysis: StallAnalysis): DeloadRecommendation
}

data class PerformanceAnalysis(
    val performanceRatio: Float, // actualReps/targetReps
    val loadAccuracy: Float, // actualWeight/targetWeight (if target exists)
    val fatigueIndicator: Float, // Based on RPE and performance
    val result: PerformanceResult
)

enum class PerformanceResult {
    EXCEEDED_TARGET,
    MET_TARGET,
    MINOR_MISS,
    MODERATE_FAILURE,
    MAJOR_FAILURE
}

data class StallAnalysis(
    val exerciseName: String,
    val consecutiveFailures: Int,
    val failurePattern: List<PerformanceResult>,
    val lastSuccessfulWeight: Float?,
    val recommendedAction: DeloadAction
)

enum class DeloadAction {
    CONTINUE_SAME_WEIGHT,
    MICRO_DELOAD, // 5-10%
    STANDARD_DELOAD, // 15%
    MAJOR_DELOAD // 25%+
}

data class DeloadRecommendation(
    val newWeight: Float,
    val reasoning: String,
    val progressionPlan: String,
    val confidence: Float
)

data class PerformanceData(
    val targetReps: Int,
    val targetWeight: Float?,
    val actualReps: Int,
    val actualWeight: Float,
    val actualRpe: Float?,
    val timestamp: String
)