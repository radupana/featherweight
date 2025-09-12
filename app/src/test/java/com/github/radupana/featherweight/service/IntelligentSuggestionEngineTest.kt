package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntelligentSuggestionEngineTest {
    @Test
    fun `WeightSuggestion stores values correctly`() {
        // Arrange
        val sources =
            listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = 100f,
                    weight = 0.5f,
                    details = "Based on 1RM",
                ),
                SuggestionSourceData(
                    source = SuggestionSource.HISTORICAL_AVERAGE,
                    value = 95f,
                    weight = 0.3f,
                    details = "Historical average",
                ),
            )
        val alternatives = mapOf(5 to 100f, 8 to 85f, 10 to 75f)

        // Act
        val suggestion =
            WeightSuggestion(
                weight = 97.5f,
                confidence = 0.8f,
                sources = sources,
                explanation = "Based on multiple sources",
                alternativeWeights = alternatives,
            )

        // Assert
        assertThat(suggestion.weight).isEqualTo(97.5f)
        assertThat(suggestion.confidence).isEqualTo(0.8f)
        assertThat(suggestion.sources).hasSize(2)
        assertThat(suggestion.explanation).isEqualTo("Based on multiple sources")
        assertThat(suggestion.alternativeWeights).containsExactly(5, 100f, 8, 85f, 10, 75f)
    }

    @Test
    fun `WeightSuggestion defaults to empty alternative weights`() {
        // Arrange
        val sources =
            listOf(
                SuggestionSourceData(
                    source = SuggestionSource.RECENT_PERFORMANCE,
                    value = 80f,
                    weight = 1.0f,
                    details = "Recent workout",
                ),
            )

        // Act
        val suggestion =
            WeightSuggestion(
                weight = 80f,
                confidence = 0.9f,
                sources = sources,
                explanation = "Based on recent performance",
            )

        // Assert
        assertThat(suggestion.alternativeWeights).isEmpty()
    }

    @Test
    fun `SuggestionSourceData stores all fields correctly`() {
        // Act
        val sourceData =
            SuggestionSourceData(
                source = SuggestionSource.ONE_RM_CALCULATION,
                value = 120f,
                weight = 0.6f,
                details = "Calculated from 1RM of 150kg",
            )

        // Assert
        assertThat(sourceData.source).isEqualTo(SuggestionSource.ONE_RM_CALCULATION)
        assertThat(sourceData.value).isEqualTo(120f)
        assertThat(sourceData.weight).isEqualTo(0.6f)
        assertThat(sourceData.details).isEqualTo("Calculated from 1RM of 150kg")
    }

    @Test
    fun `SuggestionSource enum has expected values`() {
        // Act & Assert
        assertThat(SuggestionSource.values()).hasLength(3)
        assertThat(SuggestionSource.valueOf("ONE_RM_CALCULATION"))
            .isEqualTo(SuggestionSource.ONE_RM_CALCULATION)
        assertThat(SuggestionSource.valueOf("HISTORICAL_AVERAGE"))
            .isEqualTo(SuggestionSource.HISTORICAL_AVERAGE)
        assertThat(SuggestionSource.valueOf("RECENT_PERFORMANCE"))
            .isEqualTo(SuggestionSource.RECENT_PERFORMANCE)
    }

    @Test
    fun `WeightSuggestion confidence is within valid range`() {
        // Arrange
        val sources =
            listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = 100f,
                    weight = 1.0f,
                    details = "Test",
                ),
            )

        // Act - Test various confidence values
        val lowConfidence =
            WeightSuggestion(
                weight = 100f,
                confidence = 0.0f,
                sources = sources,
                explanation = "Low confidence",
            )

        val highConfidence =
            WeightSuggestion(
                weight = 100f,
                confidence = 1.0f,
                sources = sources,
                explanation = "High confidence",
            )

        val midConfidence =
            WeightSuggestion(
                weight = 100f,
                confidence = 0.5f,
                sources = sources,
                explanation = "Medium confidence",
            )

        // Assert
        assertThat(lowConfidence.confidence).isAtLeast(0.0f)
        assertThat(lowConfidence.confidence).isAtMost(1.0f)
        assertThat(highConfidence.confidence).isAtLeast(0.0f)
        assertThat(highConfidence.confidence).isAtMost(1.0f)
        assertThat(midConfidence.confidence).isAtLeast(0.0f)
        assertThat(midConfidence.confidence).isAtMost(1.0f)
    }

    @Test
    fun `SuggestionSourceData weight represents contribution`() {
        // Arrange - Create sources with different weights
        val primarySource =
            SuggestionSourceData(
                source = SuggestionSource.ONE_RM_CALCULATION,
                value = 100f,
                weight = 0.7f,
                details = "Primary source",
            )

        val secondarySource =
            SuggestionSourceData(
                source = SuggestionSource.HISTORICAL_AVERAGE,
                value = 90f,
                weight = 0.3f,
                details = "Secondary source",
            )

        // Act
        val sources = listOf(primarySource, secondarySource)
        val totalWeight = sources.sumOf { it.weight.toDouble() }.toFloat()

        // Assert - Weights should sum to 1.0 for proper contribution
        assertThat(totalWeight).isWithin(0.001f).of(1.0f)
        assertThat(primarySource.weight).isGreaterThan(secondarySource.weight)
    }

    @Test
    fun `WeightSuggestion can handle multiple alternative weights`() {
        // Arrange
        val sources =
            listOf(
                SuggestionSourceData(
                    source = SuggestionSource.ONE_RM_CALCULATION,
                    value = 100f,
                    weight = 1.0f,
                    details = "Test",
                ),
            )

        val alternatives =
            mapOf(
                3 to 110f,
                5 to 100f,
                8 to 85f,
                10 to 75f,
                12 to 65f,
                15 to 55f,
            )

        // Act
        val suggestion =
            WeightSuggestion(
                weight = 100f,
                confidence = 0.85f,
                sources = sources,
                explanation = "With many alternatives",
                alternativeWeights = alternatives,
            )

        // Assert
        assertThat(suggestion.alternativeWeights).hasSize(6)
        assertThat(suggestion.alternativeWeights[3]).isEqualTo(110f)
        assertThat(suggestion.alternativeWeights[15]).isEqualTo(55f)
        // Verify weights decrease as reps increase (typical pattern)
        assertThat(suggestion.alternativeWeights[3]).isGreaterThan(suggestion.alternativeWeights[15]!!)
    }

    @Test
    fun `WeightSuggestion can combine multiple source types`() {
        // Arrange & Act
        val suggestion =
            WeightSuggestion(
                weight = 95f,
                confidence = 0.75f,
                sources =
                    listOf(
                        SuggestionSourceData(
                            source = SuggestionSource.ONE_RM_CALCULATION,
                            value = 100f,
                            weight = 0.4f,
                            details = "From 1RM",
                        ),
                        SuggestionSourceData(
                            source = SuggestionSource.HISTORICAL_AVERAGE,
                            value = 95f,
                            weight = 0.3f,
                            details = "Past 4 weeks",
                        ),
                        SuggestionSourceData(
                            source = SuggestionSource.RECENT_PERFORMANCE,
                            value = 90f,
                            weight = 0.3f,
                            details = "Last workout",
                        ),
                    ),
                explanation = "Weighted combination of all sources",
            )

        // Assert
        assertThat(suggestion.sources).hasSize(3)
        val sourceTypes = suggestion.sources.map { it.source }.toSet()
        assertThat(sourceTypes).containsExactly(
            SuggestionSource.ONE_RM_CALCULATION,
            SuggestionSource.HISTORICAL_AVERAGE,
            SuggestionSource.RECENT_PERFORMANCE,
        )
    }
}
