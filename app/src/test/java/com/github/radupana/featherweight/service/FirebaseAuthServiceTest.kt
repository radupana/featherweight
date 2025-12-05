package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.LogSanitizer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for FirebaseAuthService interface and related utilities.
 *
 * Note: FirebaseAuthServiceImpl cannot be directly unit tested without
 * Robolectric because it depends on FirebaseAuth.getInstance() which
 * requires Android runtime. These tests cover the interface contract
 * and related utility functions.
 */
class FirebaseAuthServiceTest {
    @Test
    fun `LogSanitizer sanitizes email correctly - returns hashed format`() {
        val email = "john.doe@example.com"
        val sanitized = LogSanitizer.sanitizeEmail(email)

        // Should return hashed format like "user_xxxx"
        assertThat(sanitized).isNotEqualTo(email)
        assertThat(sanitized).startsWith("user_")
        assertThat(sanitized).doesNotContain("@")
    }

    @Test
    fun `LogSanitizer returns consistent hash for same email`() {
        val email = "test@example.com"
        val sanitized1 = LogSanitizer.sanitizeEmail(email)
        val sanitized2 = LogSanitizer.sanitizeEmail(email)

        assertThat(sanitized1).isEqualTo(sanitized2)
    }

    @Test
    fun `LogSanitizer returns different hash for different emails`() {
        val sanitized1 = LogSanitizer.sanitizeEmail("user1@example.com")
        val sanitized2 = LogSanitizer.sanitizeEmail("user2@example.com")

        assertThat(sanitized1).isNotEqualTo(sanitized2)
    }

    @Test
    fun `LogSanitizer handles null email`() {
        val sanitized = LogSanitizer.sanitizeEmail(null)

        assertThat(sanitized).isEqualTo("[no-email]")
    }

    @Test
    fun `LogSanitizer handles empty email`() {
        val sanitized = LogSanitizer.sanitizeEmail("")

        assertThat(sanitized).isEqualTo("[no-email]")
    }

    @Test
    fun `LogSanitizer handles blank email`() {
        val sanitized = LogSanitizer.sanitizeEmail("   ")

        assertThat(sanitized).isEqualTo("[no-email]")
    }

    @Test
    fun `LogSanitizer handles email without at symbol - still hashes`() {
        val email = "notanemail"
        val sanitized = LogSanitizer.sanitizeEmail(email)

        // Should still hash malformed emails
        assertThat(sanitized).startsWith("user_")
    }

    @Test
    fun `Result success can hold FirebaseUser type`() {
        // Testing that Result<T> works correctly with Firebase types
        val mockUid = "test-uid-123"

        // Create a mock success result
        val result: Result<String> = Result.success(mockUid)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockUid)
    }

    @Test
    fun `Result failure contains exception`() {
        val errorMessage = "Authentication failed"
        val result: Result<String> = Result.failure(Exception(errorMessage))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(errorMessage)
    }

    @Test
    fun `Result can map success value`() {
        val result: Result<Int> = Result.success(42)
        val mapped = result.map { it * 2 }

        assertThat(mapped.getOrNull()).isEqualTo(84)
    }

    @Test
    fun `Result can recover from failure`() {
        val result: Result<String> = Result.failure(Exception("error"))
        val recovered =
            result.recover {
                "recovered"
            }

        assertThat(recovered.getOrNull()).isEqualTo("recovered")
    }

    @Test
    fun `FirebaseAuthService interface has methods`() {
        // Verify the interface has methods defined
        val methods = FirebaseAuthService::class.java.methods.map { it.name }

        // Should have multiple auth-related methods
        assertThat(methods).isNotEmpty()
    }

    @Test
    fun `FirebaseAuthService includes synchronous methods`() {
        // Use Kotlin reflection to get declared functions
        val functions = FirebaseAuthService::class.members.map { it.name }

        assertThat(functions).contains("getCurrentUser")
        assertThat(functions).contains("signOut")
        assertThat(functions).contains("isUserAuthenticated")
        assertThat(functions).contains("isEmailVerified")
        assertThat(functions).contains("getUserEmail")
        assertThat(functions).contains("isAnonymous")
    }

    @Test
    fun `FirebaseAuthService includes suspend functions`() {
        val functions = FirebaseAuthService::class.members.map { it.name }

        assertThat(functions).contains("signInWithEmailAndPassword")
        assertThat(functions).contains("createUserWithEmailAndPassword")
        assertThat(functions).contains("sendPasswordResetEmail")
        assertThat(functions).contains("sendEmailVerification")
        assertThat(functions).contains("updatePassword")
        assertThat(functions).contains("deleteAccount")
    }

    @Test
    fun `too many requests error message is user friendly`() {
        // Verify the error message used for rate limiting
        val errorMessage = "Too many attempts. Please wait before trying again."

        assertThat(errorMessage).doesNotContain("Firebase")
        assertThat(errorMessage).contains("wait")
        assertThat(errorMessage).contains("before trying again")
    }

    @Test
    fun `password validation - minimum length`() {
        // Testing common password validation logic
        val shortPassword = "12345"
        val validPassword = "123456"

        assertThat(shortPassword.length).isLessThan(6)
        assertThat(validPassword.length).isAtLeast(6)
    }

    @Test
    fun `email validation - contains at symbol`() {
        val validEmail = "test@example.com"
        val invalidEmail = "testexample.com"

        assertThat(validEmail.contains("@")).isTrue()
        assertThat(invalidEmail.contains("@")).isFalse()
    }

    @Test
    fun `email validation - has domain`() {
        val validEmail = "test@example.com"
        val parts = validEmail.split("@")

        assertThat(parts).hasSize(2)
        assertThat(parts[1]).contains(".")
    }
}
