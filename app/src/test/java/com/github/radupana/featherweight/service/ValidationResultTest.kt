package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for ValidationResult sealed class used by ExerciseNamingService
 *
 * Tests validation result types and equality:
 * - Valid result representation
 * - Invalid result with reason and suggestion
 * - Equality and type checking
 */
class ValidationResultTest {
    @Test
    fun `Valid result indicates success`() {
        // This tests the ValidationResult from ExerciseNamingService
        val result = ValidationResult.Valid

        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat(result).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `Invalid result contains reason`() {
        val reason = "Name must be at least 3 characters"
        val result = ValidationResult.Invalid(reason, null)

        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat(result.reason).isEqualTo(reason)
        assertThat(result.suggestion).isNull()
    }

    @Test
    fun `Invalid result can contain suggestion`() {
        val reason = "Equipment should come first"
        val suggestion = "Barbell Bench Press"
        val result = ValidationResult.Invalid(reason, suggestion)

        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat(result.reason).isEqualTo(reason)
        assertThat(result.suggestion).isEqualTo(suggestion)
    }

    @Test
    fun `Valid and Invalid are distinct types`() {
        val valid = ValidationResult.Valid
        val invalid = ValidationResult.Invalid("Error", null)

        assertThat(valid).isNotEqualTo(invalid)
        assertThat(valid).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat(invalid).isInstanceOf(ValidationResult.Invalid::class.java)
    }

    @Test
    fun `Invalid results with same reason are equal`() {
        val result1 = ValidationResult.Invalid("Same error", null)
        val result2 = ValidationResult.Invalid("Same error", null)

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `Invalid results with different reasons are not equal`() {
        val result1 = ValidationResult.Invalid("Error 1", null)
        val result2 = ValidationResult.Invalid("Error 2", null)

        assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `Invalid results with same reason but different suggestions are not equal`() {
        val result1 = ValidationResult.Invalid("Error", "Suggestion 1")
        val result2 = ValidationResult.Invalid("Error", "Suggestion 2")

        assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `can check if result is valid using when expression`() {
        val validResult: ValidationResult = ValidationResult.Valid
        val invalidResult: ValidationResult = ValidationResult.Invalid("Error", null)

        val validCheck =
            when (validResult) {
                is ValidationResult.Valid -> true
                is ValidationResult.Invalid -> false
            }

        val invalidCheck =
            when (invalidResult) {
                is ValidationResult.Valid -> false
                is ValidationResult.Invalid -> true
            }

        assertThat(validCheck).isTrue()
        assertThat(invalidCheck).isTrue()
    }
}
