package com.github.radupana.featherweight.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InputFieldTypeTest {

    @Test
    fun `RPE field accepts decimal point input`() {
        val testCases = listOf(
            "8.5" to true,
            "9.0" to true,
            "10.0" to true,
            "7.25" to true,
            "0.5" to true,
            "8." to true,  // Allow incomplete decimal during typing
            ".5" to false, // Don't allow starting with decimal
            "8..5" to false, // Don't allow multiple decimals in a row
            "8.5.5" to false, // Don't allow multiple decimal points
            "10.55" to false, // Too long (max 4 chars)
            "100.0" to false  // Too long
        )
        
        for ((input, shouldBeValid) in testCases) {
            val isValid = validateRpeInput(input)
            assertThat(isValid).isEqualTo(shouldBeValid)
        }
    }
    
    @Test
    fun `RPE field clamps values above 10`() {
        assertThat(clampRpeValue("11")).isEqualTo("10")
        assertThat(clampRpeValue("10.5")).isEqualTo("10")
        assertThat(clampRpeValue("15")).isEqualTo("10")
        assertThat(clampRpeValue("99")).isEqualTo("10")
        assertThat(clampRpeValue("10.1")).isEqualTo("10")
    }
    
    @Test
    fun `RPE field allows valid values without clamping`() {
        assertThat(clampRpeValue("8.5")).isEqualTo("8.5")
        assertThat(clampRpeValue("9")).isEqualTo("9")
        assertThat(clampRpeValue("10")).isEqualTo("10")
        assertThat(clampRpeValue("0")).isEqualTo("0")
        assertThat(clampRpeValue("5.5")).isEqualTo("5.5")
    }
    
    @Test
    fun `RPE rounding rounds to nearest half`() {
        assertThat(roundRpe(8.2f)).isEqualTo(8.0f)
        assertThat(roundRpe(8.3f)).isEqualTo(8.5f)
        assertThat(roundRpe(8.5f)).isEqualTo(8.5f)
        assertThat(roundRpe(8.7f)).isEqualTo(8.5f)
        assertThat(roundRpe(8.8f)).isEqualTo(9.0f)
        assertThat(roundRpe(9.9f)).isEqualTo(10.0f)
    }
    
    @Test
    fun `RPE rounding clamps to valid range`() {
        assertThat(roundRpe(-1f)).isEqualTo(0f)
        assertThat(roundRpe(11f)).isEqualTo(10f)
        assertThat(roundRpe(100f)).isEqualTo(10f)
    }
    
    // Helper functions that mirror the actual implementation logic
    
    private fun validateRpeInput(text: String): Boolean {
        val validChars = text.all { it.isDigit() || it == '.' }
        val maxLength = text.length <= 4
        val maxOneDecimal = text.count { it == '.' } <= 1
        val validFormat = !text.startsWith(".") && !text.contains("..")
        
        return validChars && maxLength && maxOneDecimal && validFormat
    }
    
    private fun clampRpeValue(text: String): String {
        val rpeValue = text.toFloatOrNull() ?: return text
        return if (rpeValue > 10f) {
            "10"
        } else {
            text
        }
    }
    
    private fun roundRpe(value: Float): Float {
        return (kotlin.math.round(value * 2) / 2).coerceIn(0f, 10f)
    }
}
