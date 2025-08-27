package com.github.radupana.featherweight.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class SmartSuggestionsTest {
    
    @Test
    fun `create SmartSuggestions with all fields`() {
        // Arrange
        val lastWorkout = LocalDateTime.of(2024, 1, 15, 18, 0)
        val alternatives = listOf(
            AlternativeSuggestion(
                reps = 5,
                weight = 110f,
                rpe = 8f,
                reasoning = "Heavy set option"
            ),
            AlternativeSuggestion(
                reps = 12,
                weight = 85f,
                rpe = 7f,
                reasoning = "Volume set option"
            )
        )
        
        // Act
        val suggestions = SmartSuggestions(
            suggestedWeight = 100f,
            suggestedReps = 8,
            suggestedRpe = 7.5f,
            lastWorkoutDate = lastWorkout,
            confidence = "High",
            reasoning = "Based on progressive overload",
            alternativeSuggestions = alternatives
        )
        
        // Assert
        assertThat(suggestions.suggestedWeight).isEqualTo(100f)
        assertThat(suggestions.suggestedReps).isEqualTo(8)
        assertThat(suggestions.suggestedRpe).isEqualTo(7.5f)
        assertThat(suggestions.lastWorkoutDate).isEqualTo(lastWorkout)
        assertThat(suggestions.confidence).isEqualTo("High")
        assertThat(suggestions.reasoning).isEqualTo("Based on progressive overload")
        assertThat(suggestions.alternativeSuggestions).hasSize(2)
    }
    
    @Test
    fun `create SmartSuggestions with default values`() {
        // Act
        val suggestions = SmartSuggestions(
            suggestedWeight = 80f,
            suggestedReps = 10,
            confidence = "Medium"
        )
        
        // Assert
        assertThat(suggestions.suggestedRpe).isNull()
        assertThat(suggestions.lastWorkoutDate).isNull()
        assertThat(suggestions.reasoning).isEmpty()
        assertThat(suggestions.alternativeSuggestions).isEmpty()
    }
    
    @Test
    fun `AlternativeSuggestion creation`() {
        // Arrange & Act
        val alternative = AlternativeSuggestion(
            reps = 6,
            weight = 95f,
            rpe = 8.5f,
            reasoning = "Strength focus"
        )
        
        // Assert
        assertThat(alternative.reps).isEqualTo(6)
        assertThat(alternative.weight).isEqualTo(95f)
        assertThat(alternative.rpe).isEqualTo(8.5f)
        assertThat(alternative.reasoning).isEqualTo("Strength focus")
    }
    
    @Test
    fun `AlternativeSuggestion with null rpe`() {
        // Arrange & Act
        val alternative = AlternativeSuggestion(
            reps = 10,
            weight = 70f,
            rpe = null,
            reasoning = "Endurance focus"
        )
        
        // Assert
        assertThat(alternative.rpe).isNull()
    }
    
    @Test
    fun `copy SmartSuggestions with modifications`() {
        // Arrange
        val original = SmartSuggestions(
            suggestedWeight = 100f,
            suggestedReps = 8,
            suggestedRpe = 7f,
            confidence = "High",
            reasoning = "Initial suggestion"
        )
        
        // Act
        val modified = original.copy(
            suggestedWeight = 105f,
            reasoning = "Updated based on feedback"
        )
        
        // Assert
        assertThat(modified.suggestedWeight).isEqualTo(105f)
        assertThat(modified.reasoning).isEqualTo("Updated based on feedback")
        assertThat(modified.suggestedReps).isEqualTo(8)
        assertThat(modified.suggestedRpe).isEqualTo(7f)
        assertThat(modified.confidence).isEqualTo("High")
    }
    
    @Test
    fun `equals and hashCode for SmartSuggestions`() {
        // Arrange
        val date = LocalDateTime.of(2024, 1, 20, 10, 0)
        val suggestion1 = SmartSuggestions(
            suggestedWeight = 90f,
            suggestedReps = 5,
            suggestedRpe = 8f,
            lastWorkoutDate = date,
            confidence = "High"
        )
        
        val suggestion2 = SmartSuggestions(
            suggestedWeight = 90f,
            suggestedReps = 5,
            suggestedRpe = 8f,
            lastWorkoutDate = date,
            confidence = "High"
        )
        
        val suggestion3 = SmartSuggestions(
            suggestedWeight = 85f,
            suggestedReps = 5,
            suggestedRpe = 8f,
            lastWorkoutDate = date,
            confidence = "High"
        )
        
        // Assert
        assertThat(suggestion1).isEqualTo(suggestion2)
        assertThat(suggestion1.hashCode()).isEqualTo(suggestion2.hashCode())
        assertThat(suggestion1).isNotEqualTo(suggestion3)
    }
    
    @Test
    fun `SmartSuggestions with multiple alternatives`() {
        // Arrange
        val alternatives = listOf(
            AlternativeSuggestion(3, 120f, 9f, "Heavy triples"),
            AlternativeSuggestion(5, 110f, 8f, "Standard 5s"),
            AlternativeSuggestion(8, 100f, 7f, "Volume 8s"),
            AlternativeSuggestion(12, 85f, 6f, "Light volume")
        )
        
        // Act
        val suggestions = SmartSuggestions(
            suggestedWeight = 105f,
            suggestedReps = 6,
            suggestedRpe = 7.5f,
            confidence = "Medium",
            alternativeSuggestions = alternatives
        )
        
        // Assert
        assertThat(suggestions.alternativeSuggestions).hasSize(4)
        assertThat(suggestions.alternativeSuggestions[0].reps).isEqualTo(3)
        assertThat(suggestions.alternativeSuggestions[1].weight).isEqualTo(110f)
        assertThat(suggestions.alternativeSuggestions[2].reasoning).isEqualTo("Volume 8s")
        assertThat(suggestions.alternativeSuggestions[3].rpe).isEqualTo(6f)
    }
}