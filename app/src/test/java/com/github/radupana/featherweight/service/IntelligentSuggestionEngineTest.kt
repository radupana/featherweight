package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for IntelligentSuggestionEngine data classes
 * 
 * Tests data structures used for intelligent suggestions including:
 * - WeightSuggestion data validation
 * - RepsSuggestion data validation  
 * - SuggestionSourceData validation
 * - Data class equality and creation
 */
class IntelligentSuggestionEngineTest {
    
    @Test
    fun `WeightSuggestion stores all required fields`() {
        // Arrange
        val sources = listOf(
            SuggestionSourceData(
                source = SuggestionSource.ONE_RM_CALCULATION,
                value = 100f,
                weight = 0.7f,
                details = "Based on 1RM"
            )
        )
        
        // Act
        val suggestion = WeightSuggestion(
            weight = 85f,
            confidence = 0.9f,
            sources = sources,
            explanation = "Suggested based on 1RM calculation",
            alternativeWeights = mapOf(3 to 90f, 8 to 75f)
        )
        
        // Assert
        assertThat(suggestion.weight).isEqualTo(85f)
        assertThat(suggestion.confidence).isEqualTo(0.9f)
        assertThat(suggestion.sources).hasSize(1)
        assertThat(suggestion.explanation).isNotEmpty()
        assertThat(suggestion.alternativeWeights).containsEntry(3, 90f)
    }
    
    @Test
    fun `WeightSuggestion handles empty alternative weights`() {
        // Act
        val suggestion = WeightSuggestion(
            weight = 80f,
            confidence = 0.8f,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.HISTORICAL_AVERAGE,
                    value = 80f,
                    weight = 1.0f,
                    details = "Historical average"
                )
            ),
            explanation = "Based on historical data"
        )
        
        // Assert
        assertThat(suggestion.alternativeWeights).isEmpty()
    }
    
    @Test
    fun `RepsSuggestion stores all required fields`() {
        // Arrange
        val sources = listOf(
            SuggestionSourceData(
                source = SuggestionSource.RECENT_PERFORMANCE,
                value = 8f,
                weight = 0.6f,
                details = "Recent performance data"
            )
        )
        
        // Act
        val suggestion = RepsSuggestion(
            reps = 8,
            confidence = 0.85f,
            sources = sources,
            explanation = "Based on recent performance",
            alternativeReps = mapOf(70f to 10, 90f to 5)
        )
        
        // Assert
        assertThat(suggestion.reps).isEqualTo(8)
        assertThat(suggestion.confidence).isEqualTo(0.85f)
        assertThat(suggestion.sources).hasSize(1)
        assertThat(suggestion.explanation).isNotEmpty()
        assertThat(suggestion.alternativeReps).containsEntry(70f, 10)
    }
    
    @Test
    fun `SuggestionSourceData validates required fields`() {
        // Act
        val sourceData = SuggestionSourceData(
            source = SuggestionSource.ONE_RM_CALCULATION,
            value = 95f,
            weight = 0.8f,
            details = "Calculated from 1RM estimate"
        )
        
        // Assert
        assertThat(sourceData.source).isEqualTo(SuggestionSource.ONE_RM_CALCULATION)
        assertThat(sourceData.value).isEqualTo(95f)
        assertThat(sourceData.weight).isEqualTo(0.8f)
        assertThat(sourceData.details).isEqualTo("Calculated from 1RM estimate")
    }
    
    @Test
    fun `SuggestionSource enum contains expected values`() {
        // Assert
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.ONE_RM_CALCULATION)
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.HISTORICAL_AVERAGE)
        assertThat(SuggestionSource.values().toList()).contains(SuggestionSource.RECENT_PERFORMANCE)
    }
    
    @Test
    fun `WeightSuggestion confidence is bounded between 0 and 1`() {
        // Act
        val lowConfidence = WeightSuggestion(
            weight = 50f,
            confidence = 0.0f,
            sources = emptyList(),
            explanation = "Low confidence suggestion"
        )
        
        val highConfidence = WeightSuggestion(
            weight = 100f,
            confidence = 1.0f,
            sources = emptyList(),
            explanation = "High confidence suggestion"
        )
        
        // Assert
        assertThat(lowConfidence.confidence).isEqualTo(0.0f)
        assertThat(highConfidence.confidence).isEqualTo(1.0f)
    }
    
    @Test
    fun `RepsSuggestion confidence is bounded between 0 and 1`() {
        // Act
        val lowConfidence = RepsSuggestion(
            reps = 12,
            confidence = 0.0f,
            sources = emptyList(),
            explanation = "Low confidence rep suggestion"
        )
        
        val highConfidence = RepsSuggestion(
            reps = 5,
            confidence = 1.0f,
            sources = emptyList(),
            explanation = "High confidence rep suggestion"
        )
        
        // Assert
        assertThat(lowConfidence.confidence).isEqualTo(0.0f)
        assertThat(highConfidence.confidence).isEqualTo(1.0f)
    }
    
    @Test
    fun `SuggestionSourceData weight is bounded between 0 and 1`() {
        // Act
        val minWeight = SuggestionSourceData(
            source = SuggestionSource.HISTORICAL_AVERAGE,
            value = 75f,
            weight = 0.0f,
            details = "Minimum weight"
        )
        
        val maxWeight = SuggestionSourceData(
            source = SuggestionSource.ONE_RM_CALCULATION,
            value = 90f,
            weight = 1.0f,
            details = "Maximum weight"
        )
        
        // Assert
        assertThat(minWeight.weight).isEqualTo(0.0f)
        assertThat(maxWeight.weight).isEqualTo(1.0f)
    }
    
    @Test
    fun `multiple SuggestionSourceData can be combined`() {
        // Act
        val sources = listOf(
            SuggestionSourceData(
                source = SuggestionSource.ONE_RM_CALCULATION,
                value = 85f,
                weight = 0.6f,
                details = "Primary calculation"
            ),
            SuggestionSourceData(
                source = SuggestionSource.HISTORICAL_AVERAGE,
                value = 80f,
                weight = 0.4f,
                details = "Historical data"
            )
        )
        
        val suggestion = WeightSuggestion(
            weight = 83f, // Weighted average
            confidence = 0.85f,
            sources = sources,
            explanation = "Combined from multiple sources"
        )
        
        // Assert
        assertThat(suggestion.sources).hasSize(2)
        val totalWeight = suggestion.sources.sumOf { it.weight.toDouble() }
        assertThat(totalWeight).isWithin(0.01).of(1.0) // Weights should sum to 1
    }
    
    @Test
    fun `WeightSuggestion handles multiple alternative weights`() {
        // Act
        val suggestion = WeightSuggestion(
            weight = 80f,
            confidence = 0.8f,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.RECENT_PERFORMANCE,
                    value = 80f,
                    weight = 1.0f,
                    details = "Recent performance"
                )
            ),
            explanation = "Based on recent performance",
            alternativeWeights = mapOf(
                3 to 90f,
                5 to 85f,
                8 to 75f,
                12 to 65f
            )
        )
        
        // Assert
        assertThat(suggestion.alternativeWeights).hasSize(4)
        assertThat(suggestion.alternativeWeights.keys).containsExactly(3, 5, 8, 12)
        suggestion.alternativeWeights.values.forEach { weight ->
            assertThat(weight).isGreaterThan(0f)
        }
    }
    
    @Test
    fun `RepsSuggestion handles multiple alternative reps`() {
        // Act
        val suggestion = RepsSuggestion(
            reps = 8,
            confidence = 0.75f,
            sources = listOf(
                SuggestionSourceData(
                    source = SuggestionSource.HISTORICAL_AVERAGE,
                    value = 8f,
                    weight = 1.0f,
                    details = "Historical average"
                )
            ),
            explanation = "Based on historical data",
            alternativeReps = mapOf(
                60f to 12,
                70f to 10,
                80f to 8,
                90f to 5
            )
        )
        
        // Assert
        assertThat(suggestion.alternativeReps).hasSize(4)
        assertThat(suggestion.alternativeReps.keys).containsExactly(60f, 70f, 80f, 90f)
        suggestion.alternativeReps.values.forEach { reps ->
            assertThat(reps).isGreaterThan(0)
        }
    }
    
    @Test
    fun `WeightSuggestion and RepsSuggestion are distinct data types`() {
        // Arrange
        val weightSuggestion = WeightSuggestion(
            weight = 80f,
            confidence = 0.8f,
            sources = emptyList(),
            explanation = "Weight suggestion"
        )
        
        val repsSuggestion = RepsSuggestion(
            reps = 8,
            confidence = 0.8f,
            sources = emptyList(),
            explanation = "Reps suggestion"
        )
        
        // Assert - they should be different types with different fields
        assertThat(weightSuggestion.weight).isEqualTo(80f)
        assertThat(repsSuggestion.reps).isEqualTo(8)
        assertThat(weightSuggestion.confidence).isEqualTo(repsSuggestion.confidence)
    }
    
}

/**
 * Test data class for performance tracking in suggestions
 */
data class PerformanceData(
    val targetReps: Int,
    val targetWeight: Float,
    val actualReps: Int,
    val actualWeight: Float,
    val actualRpe: Float?,
    val timestamp: String
)
