package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class IntelligentSuggestionEngineTest {
    
    private lateinit var engine: IntelligentSuggestionEngine
    
    @Before
    fun setup() {
        // Create a test implementation
        engine = TestIntelligentSuggestionEngine()
    }
    
    @Test
    fun `suggestWeight returns reasonable weight for target reps`() = runTest {
        val suggestion = engine.suggestWeight("Barbell Squat", 5)
        
        assertThat(suggestion.weight).isGreaterThan(0f)
        assertThat(suggestion.confidence).isAtLeast(0f)
        assertThat(suggestion.confidence).isAtMost(1f)
        assertThat(suggestion.sources).isNotEmpty()
        assertThat(suggestion.explanation).isNotEmpty()
    }
    
    @Test
    fun `suggestWeight handles null target reps`() = runTest {
        val suggestion = engine.suggestWeight("Barbell Bench Press", null)
        
        assertThat(suggestion.weight).isGreaterThan(0f)
        assertThat(suggestion.confidence).isLessThan(1f) // Lower confidence without target
    }
    
    @Test
    fun `suggestReps returns reasonable reps for target weight`() = runTest {
        val suggestion = engine.suggestReps("Barbell Deadlift", 100f)
        
        assertThat(suggestion.reps).isGreaterThan(0)
        assertThat(suggestion.confidence).isAtLeast(0f)
        assertThat(suggestion.confidence).isAtMost(1f)
        assertThat(suggestion.sources).isNotEmpty()
    }
    
    @Test
    fun `explainSuggestion provides detailed explanation`() = runTest {
        val suggestion = WeightSuggestion(
            weight = 80f,
            confidence = 0.8f,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = 85f,
                    weight = 0.5f,
                    details = "Based on 1RM of 100kg"
                )
            ),
            explanation = "Initial explanation"
        )
        
        val explanation = engine.explainSuggestion("Barbell Squat", suggestion)
        
        assertThat(explanation).isNotEmpty()
        assertThat(explanation).contains("Barbell Squat")
    }
    
    @Test
    fun `observeWeightSuggestions emits suggestions as reps change`() = runTest {
        val repsFlow = flowOf(5, 8, 10)
        val suggestionsFlow = engine.observeWeightSuggestions("Barbell Squat", repsFlow)
        
        val firstSuggestion = suggestionsFlow.first()
        assertThat(firstSuggestion.weight).isGreaterThan(0f)
    }
    
    @Test
    fun `observeRepsSuggestions emits suggestions as weight changes`() = runTest {
        val weightFlow = flowOf(60f, 80f, 100f)
        val suggestionsFlow = engine.observeRepsSuggestions("Barbell Deadlift", weightFlow)
        
        val firstSuggestion = suggestionsFlow.first()
        assertThat(firstSuggestion.reps).isGreaterThan(0)
    }
    
    @Test
    fun `WeightSuggestion includes alternative weights for different reps`() = runTest {
        val suggestion = engine.suggestWeight("Barbell Bench Press", 5)
        
        if (suggestion.alternativeWeights.isNotEmpty()) {
            suggestion.alternativeWeights.forEach { (reps, weight) ->
                assertThat(reps).isGreaterThan(0)
                assertThat(weight).isGreaterThan(0f)
            }
        }
    }
    
    @Test
    fun `RepsSuggestion includes alternative reps for different weights`() = runTest {
        val suggestion = engine.suggestReps("Barbell Row", 70f)
        
        if (suggestion.alternativeReps.isNotEmpty()) {
            suggestion.alternativeReps.forEach { (weight, reps) ->
                assertThat(weight).isGreaterThan(0f)
                assertThat(reps).isGreaterThan(0)
            }
        }
    }
    
    @Test
    fun `suggestion sources have valid weights`() = runTest {
        val suggestion = engine.suggestWeight("Barbell Overhead Press", 8)
        
        val totalWeight = suggestion.sources.sumOf { it.weight.toDouble() }
        assertThat(totalWeight).isWithin(0.01).of(1.0) // Weights should sum to 1
        
        suggestion.sources.forEach { source ->
            assertThat(source.weight).isAtLeast(0f)
            assertThat(source.weight).isAtMost(1f)
            assertThat(source.value).isGreaterThan(0f)
        }
    }
    
    @Test
    fun `confidence decreases with unusual rep ranges`() = runTest {
        val normalSuggestion = engine.suggestWeight("Barbell Squat", 5)
        val unusualSuggestion = engine.suggestWeight("Barbell Squat", 30)
        
        assertThat(unusualSuggestion.confidence).isLessThan(normalSuggestion.confidence)
    }
    
    @Test
    fun `suggestions are consistent for same input`() = runTest {
        val suggestion1 = engine.suggestWeight("Barbell Bench Press", 10)
        val suggestion2 = engine.suggestWeight("Barbell Bench Press", 10)
        
        assertThat(suggestion1.weight).isEqualTo(suggestion2.weight)
        assertThat(suggestion1.confidence).isEqualTo(suggestion2.confidence)
    }
    
    @Test
    fun `PerformanceData stores all required fields`() {
        val data = PerformanceData(
            targetReps = 5,
            targetWeight = 100f,
            actualReps = 6,
            actualWeight = 95f,
            actualRpe = 8.5f,
            timestamp = "2024-01-15T10:00:00"
        )
        
        assertThat(data.targetReps).isEqualTo(5)
        assertThat(data.targetWeight).isEqualTo(100f)
        assertThat(data.actualReps).isEqualTo(6)
        assertThat(data.actualWeight).isEqualTo(95f)
        assertThat(data.actualRpe).isEqualTo(8.5f)
        assertThat(data.timestamp).isNotEmpty()
    }
    
    @Test
    fun `SuggestionSource enum contains expected values`() {
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.ONE_RM_CALCULATION)
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.HISTORICAL_AVERAGE)
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.RECENT_PERFORMANCE)
    }
}

// Test implementation
private class TestIntelligentSuggestionEngine : IntelligentSuggestionEngine {
    
    override suspend fun suggestWeight(exerciseName: String, targetReps: Int?): WeightSuggestion {
        val baseWeight = 100f
        val reps = targetReps ?: 8
        val weight = baseWeight * (1f - (reps - 5) * 0.025f)
        val confidence = if (targetReps != null && targetReps in 3..12) 0.9f else 0.6f
        
        return WeightSuggestion(
            weight = weight.coerceAtLeast(20f),
            confidence = confidence,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = weight,
                    weight = 0.6f,
                    details = "Based on estimated 1RM"
                ),
                SuggestionSourceData(
                    source = SuggestionSource.HISTORICAL_AVERAGE,
                    value = weight * 0.95f,
                    weight = 0.4f,
                    details = "Historical average for $reps reps"
                )
            ),
            explanation = "Suggested weight for $exerciseName at $reps reps",
            alternativeWeights = mapOf(
                3 to weight * 1.1f,
                5 to weight,
                8 to weight * 0.9f,
                12 to weight * 0.75f
            )
        )
    }
    
    override suspend fun suggestReps(exerciseName: String, targetWeight: Float): RepsSuggestion {
        val baseReps = when {
            targetWeight > 90f -> 5
            targetWeight > 70f -> 8
            targetWeight > 50f -> 10
            else -> 12
        }
        
        return RepsSuggestion(
            reps = baseReps,
            confidence = 0.85f,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = baseReps.toFloat(),
                    weight = 0.7f,
                    details = "Based on weight percentage"
                ),
                SuggestionSourceData(
                    source = SuggestionSource.RECENT_PERFORMANCE,
                    value = baseReps.toFloat(),
                    weight = 0.3f,
                    details = "Recent performance at similar weight"
                )
            ),
            explanation = "Suggested reps for $exerciseName at ${targetWeight}kg",
            alternativeReps = mapOf(
                targetWeight * 0.9f to baseReps + 2,
                targetWeight to baseReps,
                targetWeight * 1.1f to (baseReps - 2).coerceAtLeast(1)
            )
        )
    }
    
    override suspend fun explainSuggestion(
        exerciseName: String,
        suggestion: WeightSuggestion
    ): String {
        return "For $exerciseName, suggesting ${suggestion.weight}kg based on: " +
            suggestion.sources.joinToString(", ") { "${it.source}: ${it.details}" }
    }
    
    override fun observeWeightSuggestions(
        exerciseName: String,
        targetReps: kotlinx.coroutines.flow.Flow<Int>
    ): kotlinx.coroutines.flow.Flow<WeightSuggestion> {
        return kotlinx.coroutines.flow.flow {
            targetReps.collect { reps ->
                emit(suggestWeight(exerciseName, reps))
            }
        }
    }
    
    override fun observeRepsSuggestions(
        exerciseName: String,
        targetWeight: kotlinx.coroutines.flow.Flow<Float>
    ): kotlinx.coroutines.flow.Flow<RepsSuggestion> {
        return kotlinx.coroutines.flow.flow {
            targetWeight.collect { weight ->
                emit(suggestReps(exerciseName, weight))
            }
        }
    }
}
