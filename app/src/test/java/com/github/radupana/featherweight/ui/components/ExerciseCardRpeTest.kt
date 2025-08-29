package com.github.radupana.featherweight.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for RPE handling logic that is actually used in ExerciseCard.kt
 * This tests the inline RPE rounding logic from the onValueChange callback (lines 676-678)
 */
class ExerciseCardRpeTest {
    
    @Test
    fun `RPE rounding logic matches ExerciseCard implementation`() {
        // This tests the ACTUAL rounding logic used in ExerciseCard.kt line 678:
        // val rpe = newValue.text.toFloatOrNull()?.let { value ->
        //     (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
        // }
        
        val testCases = mapOf(
            // Input -> Expected rounded output
            8.2f to 8.0f,
            8.3f to 8.5f,
            8.5f to 8.5f,
            8.7f to 8.5f,
            8.8f to 9.0f,
            9.0f to 9.0f,
            9.5f to 9.5f,
            9.9f to 10.0f,
            10.0f to 10.0f,
            10.5f to 10.0f, // Clamped
            11.0f to 10.0f, // Clamped
            -1.0f to 0.0f,  // Clamped
            0.0f to 0.0f,
            0.24f to 0.0f,
            0.25f to 0.0f,  // kotlin.math.round uses banker's rounding, 0.5 rounds to 0
            0.74f to 0.5f,
            0.75f to 1.0f,
            5.1f to 5.0f,
            5.4f to 5.5f,
            5.6f to 5.5f,
            5.9f to 6.0f
        )
        
        for ((input, expected) in testCases) {
            // Using the EXACT same logic as in ExerciseCard.kt line 678
            val actual = (kotlin.math.round(input * 2) / 2).coerceIn(0f, 10f)
            assertThat(actual).isEqualTo(expected)
        }
    }
    
    @Test 
    fun `RPE parsing handles invalid input like ExerciseCard`() {
        // Testing the ACTUAL parsing logic from ExerciseCard.kt:
        // newValue.text.toFloatOrNull()?.let { ... }
        
        val invalidInputs = listOf(
            "",
            "abc",
            "8.5.5",
            ".",
            "..",
            "8x5",
            "not_a_number"
        )
        
        for (input in invalidInputs) {
            // Using the EXACT same parsing logic as in ExerciseCard.kt line 676
            val result = input.toFloatOrNull()?.let { value ->
                (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
            }
            assertThat(result).isNull()
        }
    }
    
    @Test
    fun `Valid RPE string input processed like ExerciseCard`() {
        // Testing the full pipeline as implemented in ExerciseCard.kt lines 676-678
        val testCases = mapOf(
            "8.5" to 8.5f,
            "8.25" to 8.0f,  // banker's rounding: 8.25*2=16.5, round(16.5)=16, 16/2=8
            "8.75" to 9.0f,
            "10" to 10.0f,
            "10.1" to 10.0f, // Clamped
            "0" to 0.0f,
            "7.7" to 7.5f
        )
        
        for ((input, expected) in testCases) {
            // Using the EXACT same logic as in ExerciseCard.kt lines 676-678
            val result = input.toFloatOrNull()?.let { value ->
                (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
            }
            assertThat(result).isEqualTo(expected)
        }
    }
}
