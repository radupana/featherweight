package com.github.radupana.featherweight.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class SmartSuggestionsTest {
    @Test
    fun smartSuggestions_withAllFields_createsCorrectly() {
        // Arrange
        val lastWorkoutDate = LocalDateTime.of(2024, 6, 15, 10, 0)
        val alternativeSuggestion1 =
            AlternativeSuggestion(
                reps = 8,
                weight = 60f,
                rpe = 7.5f,
                reasoning = "Lower reps for strength focus",
            )
        val alternativeSuggestion2 =
            AlternativeSuggestion(
                reps = 15,
                weight = 40f,
                rpe = 8.5f,
                reasoning = "Higher reps for endurance",
            )

        // Act
        val suggestions =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = lastWorkoutDate,
                confidence = "HIGH",
                reasoning = "Based on recent performance trends",
                alternativeSuggestions = listOf(alternativeSuggestion1, alternativeSuggestion2),
            )

        // Assert
        assertThat(suggestions.suggestedWeight).isEqualTo(50f)
        assertThat(suggestions.suggestedReps).isEqualTo(10)
        assertThat(suggestions.suggestedRpe).isEqualTo(8f)
        assertThat(suggestions.lastWorkoutDate).isEqualTo(lastWorkoutDate)
        assertThat(suggestions.confidence).isEqualTo("HIGH")
        assertThat(suggestions.reasoning).isEqualTo("Based on recent performance trends")
        assertThat(suggestions.alternativeSuggestions).hasSize(2)
        assertThat(suggestions.alternativeSuggestions[0]).isEqualTo(alternativeSuggestion1)
        assertThat(suggestions.alternativeSuggestions[1]).isEqualTo(alternativeSuggestion2)
    }

    @Test
    fun smartSuggestions_withNullableFields_handlesNulls() {
        // Act
        val suggestions =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = null,
                lastWorkoutDate = null,
                confidence = "MEDIUM",
                reasoning = "",
                alternativeSuggestions = emptyList(),
            )

        // Assert
        assertThat(suggestions.suggestedWeight).isEqualTo(50f)
        assertThat(suggestions.suggestedReps).isEqualTo(10)
        assertThat(suggestions.suggestedRpe).isNull()
        assertThat(suggestions.lastWorkoutDate).isNull()
        assertThat(suggestions.confidence).isEqualTo("MEDIUM")
        assertThat(suggestions.reasoning).isEmpty()
        assertThat(suggestions.alternativeSuggestions).isEmpty()
    }

    @Test
    fun smartSuggestions_withDefaultValues_usesDefaults() {
        // Act
        val suggestions =
            SmartSuggestions(
                suggestedWeight = 60f,
                suggestedReps = 12,
                confidence = "LOW",
            )

        // Assert
        assertThat(suggestions.suggestedWeight).isEqualTo(60f)
        assertThat(suggestions.suggestedReps).isEqualTo(12)
        assertThat(suggestions.suggestedRpe).isNull()
        assertThat(suggestions.lastWorkoutDate).isNull()
        assertThat(suggestions.confidence).isEqualTo("LOW")
        assertThat(suggestions.reasoning).isEmpty()
        assertThat(suggestions.alternativeSuggestions).isEmpty()
    }

    @Test
    fun smartSuggestions_equality_worksCorrectly() {
        // Arrange
        val date = LocalDateTime.of(2024, 7, 1, 14, 30)
        val alternative =
            AlternativeSuggestion(
                reps = 10,
                weight = 50f,
                rpe = 7f,
                reasoning = "Standard set",
            )

        val suggestions1 =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = date,
                confidence = "HIGH",
                reasoning = "Test",
                alternativeSuggestions = listOf(alternative),
            )

        val suggestions2 =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = date,
                confidence = "HIGH",
                reasoning = "Test",
                alternativeSuggestions = listOf(alternative),
            )

        val suggestions3 =
            SmartSuggestions(
                suggestedWeight = 55f, // Different weight
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = date,
                confidence = "HIGH",
                reasoning = "Test",
                alternativeSuggestions = listOf(alternative),
            )

        // Assert
        assertThat(suggestions1).isEqualTo(suggestions2)
        assertThat(suggestions1).isNotEqualTo(suggestions3)
        assertThat(suggestions1.hashCode()).isEqualTo(suggestions2.hashCode())
    }

    @Test
    fun smartSuggestions_copy_createsIndependentCopy() {
        // Arrange
        val original =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = LocalDateTime.of(2024, 6, 15, 10, 0),
                confidence = "HIGH",
                reasoning = "Original reasoning",
                alternativeSuggestions =
                    listOf(
                        AlternativeSuggestion(
                            reps = 8,
                            weight = 55f,
                            rpe = 7f,
                            reasoning = "Alternative",
                        ),
                    ),
            )

        // Act
        val copy =
            original.copy(
                suggestedWeight = 60f,
                reasoning = "Updated reasoning",
            )

        // Assert
        assertThat(copy.suggestedWeight).isEqualTo(60f)
        assertThat(copy.suggestedReps).isEqualTo(10) // Unchanged
        assertThat(copy.suggestedRpe).isEqualTo(8f) // Unchanged
        assertThat(copy.lastWorkoutDate).isEqualTo(original.lastWorkoutDate) // Unchanged
        assertThat(copy.confidence).isEqualTo("HIGH") // Unchanged
        assertThat(copy.reasoning).isEqualTo("Updated reasoning")
        assertThat(copy.alternativeSuggestions).isEqualTo(original.alternativeSuggestions) // Unchanged

        // Verify original is unchanged
        assertThat(original.suggestedWeight).isEqualTo(50f)
        assertThat(original.reasoning).isEqualTo("Original reasoning")
    }

    @Test
    fun alternativeSuggestion_withAllFields_createsCorrectly() {
        // Act
        val alternative =
            AlternativeSuggestion(
                reps = 12,
                weight = 45f,
                rpe = 8.5f,
                reasoning = "Higher volume approach",
            )

        // Assert
        assertThat(alternative.reps).isEqualTo(12)
        assertThat(alternative.weight).isEqualTo(45f)
        assertThat(alternative.rpe).isEqualTo(8.5f)
        assertThat(alternative.reasoning).isEqualTo("Higher volume approach")
    }

    @Test
    fun alternativeSuggestion_withNullRpe_handlesNull() {
        // Act
        val alternative =
            AlternativeSuggestion(
                reps = 5,
                weight = 80f,
                rpe = null,
                reasoning = "Heavy set without RPE tracking",
            )

        // Assert
        assertThat(alternative.reps).isEqualTo(5)
        assertThat(alternative.weight).isEqualTo(80f)
        assertThat(alternative.rpe).isNull()
        assertThat(alternative.reasoning).isEqualTo("Heavy set without RPE tracking")
    }

    @Test
    fun alternativeSuggestion_equality_worksCorrectly() {
        // Arrange
        val alternative1 =
            AlternativeSuggestion(
                reps = 10,
                weight = 50f,
                rpe = 7f,
                reasoning = "Standard",
            )

        val alternative2 =
            AlternativeSuggestion(
                reps = 10,
                weight = 50f,
                rpe = 7f,
                reasoning = "Standard",
            )

        val alternative3 =
            AlternativeSuggestion(
                reps = 10,
                weight = 50f,
                rpe = 7f,
                reasoning = "Different", // Different reasoning
            )

        // Assert
        assertThat(alternative1).isEqualTo(alternative2)
        assertThat(alternative1).isNotEqualTo(alternative3)
        assertThat(alternative1.hashCode()).isEqualTo(alternative2.hashCode())
    }

    @Test
    fun smartSuggestions_withMultipleAlternatives_maintainsOrder() {
        // Arrange
        val alternatives =
            listOf(
                AlternativeSuggestion(5, 80f, 9f, "Heavy"),
                AlternativeSuggestion(8, 70f, 8f, "Medium"),
                AlternativeSuggestion(12, 60f, 7f, "Light"),
                AlternativeSuggestion(15, 50f, 6f, "Very Light"),
            )

        // Act
        val suggestions =
            SmartSuggestions(
                suggestedWeight = 65f,
                suggestedReps = 10,
                confidence = "HIGH",
                alternativeSuggestions = alternatives,
            )

        // Assert
        assertThat(suggestions.alternativeSuggestions).hasSize(4)
        assertThat(suggestions.alternativeSuggestions[0].weight).isEqualTo(80f)
        assertThat(suggestions.alternativeSuggestions[1].weight).isEqualTo(70f)
        assertThat(suggestions.alternativeSuggestions[2].weight).isEqualTo(60f)
        assertThat(suggestions.alternativeSuggestions[3].weight).isEqualTo(50f)
    }

    @Test
    fun smartSuggestions_toString_includesAllFields() {
        // Arrange
        val suggestions =
            SmartSuggestions(
                suggestedWeight = 50f,
                suggestedReps = 10,
                suggestedRpe = 8f,
                lastWorkoutDate = LocalDateTime.of(2024, 6, 15, 10, 0),
                confidence = "HIGH",
                reasoning = "Test reason",
                alternativeSuggestions =
                    listOf(
                        AlternativeSuggestion(8, 55f, 7f, "Alt"),
                    ),
            )

        // Act
        val stringRepresentation = suggestions.toString()

        // Assert
        assertThat(stringRepresentation).contains("suggestedWeight=50.0")
        assertThat(stringRepresentation).contains("suggestedReps=10")
        assertThat(stringRepresentation).contains("suggestedRpe=8.0")
        assertThat(stringRepresentation).contains("confidence=HIGH")
        assertThat(stringRepresentation).contains("reasoning=Test reason")
        assertThat(stringRepresentation).contains("alternativeSuggestions")
    }
}
