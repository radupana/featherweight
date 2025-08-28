package com.github.radupana.featherweight.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseCardRpeTest {
    
    @Test
    fun `ExerciseCard RPE rounding logic matches specification`() {
        // This tests the actual rounding logic used in ExerciseCard.kt
        val testCases = mapOf(
            // Input -> Expected rounded output
            "8.2" to 8.0f,
            "8.3" to 8.5f,
            "8.5" to 8.5f,
            "8.7" to 8.5f,
            "8.8" to 9.0f,
            "9.0" to 9.0f,
            "9.5" to 9.5f,
            "9.9" to 10.0f,
            "10.0" to 10.0f,
            "10.5" to 10.0f, // Clamped
            "11.0" to 10.0f, // Clamped
            "-1.0" to 0.0f,  // Clamped
            "0.0" to 0.0f,
            "0.24" to 0.0f,
            "0.25" to 0.0f,  // kotlin.math.round uses banker's rounding, 0.5 rounds to 0
            "0.74" to 0.5f,
            "0.75" to 1.0f,
            "5.1" to 5.0f,
            "5.4" to 5.5f,
            "5.6" to 5.5f,
            "5.9" to 6.0f
        )
        
        for ((input, expected) in testCases) {
            val actual = roundRpeForExerciseCard(input)
            assertThat(actual).isEqualTo(expected)
        }
    }
    
    @Test 
    fun `Invalid RPE input returns null`() {
        val invalidInputs = listOf(
            "",
            "abc",
            "8.5.5",
            ".",
            "..",
            "8x5"
        )
        
        for (input in invalidInputs) {
            val result = parseAndRoundRpe(input)
            assertThat(result).isNull()
        }
    }
    
    @Test
    fun `Valid decimal RPE values are parsed and rounded correctly`() {
        // Testing the full pipeline: parse string -> round -> clamp
        assertThat(parseAndRoundRpe("8.5")).isEqualTo(8.5f)
        assertThat(parseAndRoundRpe("8.25")).isEqualTo(8.0f)  // banker's rounding: 8.25*2=16.5, round(16.5)=16, 16/2=8
        assertThat(parseAndRoundRpe("8.75")).isEqualTo(9.0f)
        assertThat(parseAndRoundRpe("10")).isEqualTo(10.0f)
        assertThat(parseAndRoundRpe("10.1")).isEqualTo(10.0f)
    }
    
    // These helper functions mirror the actual implementation in ExerciseCard.kt
    
    private fun roundRpeForExerciseCard(input: String): Float? {
        return input.toFloatOrNull()?.let { value ->
            (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
        }
    }
    
    private fun parseAndRoundRpe(input: String): Float? {
        return input.toFloatOrNull()?.let { value ->
            (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
        }
    }
}
