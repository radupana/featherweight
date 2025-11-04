package com.github.radupana.featherweight.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {
    @Test
    fun sanitizeEmail_withValidEmail_returnsHashedValue() {
        val email = "user@example.com"
        val sanitized = LogSanitizer.sanitizeEmail(email)

        assertTrue(sanitized.startsWith("user_"))
        assertFalse(sanitized.contains("@"))
        assertFalse(sanitized.contains("example.com"))
        assertEquals(13, sanitized.length)
    }

    @Test
    fun sanitizeEmail_withNullEmail_returnsPlaceholder() {
        val sanitized = LogSanitizer.sanitizeEmail(null)
        assertEquals("[no-email]", sanitized)
    }

    @Test
    fun sanitizeEmail_withBlankEmail_returnsPlaceholder() {
        val sanitized = LogSanitizer.sanitizeEmail("   ")
        assertEquals("[no-email]", sanitized)
    }

    @Test
    fun sanitizeEmail_consistentHashingForSameEmail() {
        val email = "test@test.com"
        val sanitized1 = LogSanitizer.sanitizeEmail(email)
        val sanitized2 = LogSanitizer.sanitizeEmail(email)

        assertEquals(sanitized1, sanitized2)
    }

    @Test
    fun sanitizeEmail_differentEmailsProduceDifferentHashes() {
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"
        val sanitized1 = LogSanitizer.sanitizeEmail(email1)
        val sanitized2 = LogSanitizer.sanitizeEmail(email2)

        assertFalse(sanitized1 == sanitized2)
    }

    @Test
    fun summarizeJson_withSmallJson_returnsCharacterCount() {
        val json = """{"key": "value"}"""
        val summary = LogSanitizer.summarizeJson(json)

        assertEquals("[JSON: 16 chars]", summary)
    }

    @Test
    fun summarizeJson_withLargeJson_returnsCharacterCount() {
        val json = """{"user": "test", "data": "sensitive", "nested": {"key": "value"}}"""
        val summary = LogSanitizer.summarizeJson(json)

        assertTrue(summary.startsWith("[JSON: "))
        assertTrue(summary.endsWith(" chars]"))
        assertFalse(summary.contains("sensitive"))
        assertFalse(summary.contains("user"))
    }

    @Test
    fun summarizeJson_withEmptyJson_returnsZeroChars() {
        val summary = LogSanitizer.summarizeJson("")
        assertEquals("[JSON: 0 chars]", summary)
    }

    @Test
    fun sanitizeText_withEmailInText_redactsEmail() {
        val text = "User email is user@example.com and should be hidden"
        val sanitized = LogSanitizer.sanitizeText(text)

        assertEquals("User email is [email-redacted] and should be hidden", sanitized)
    }

    @Test
    fun sanitizeText_withMultipleEmails_redactsAllEmails() {
        val text = "Contact user1@test.com or user2@test.org for help"
        val sanitized = LogSanitizer.sanitizeText(text)

        assertEquals("Contact [email-redacted] or [email-redacted] for help", sanitized)
    }

    @Test
    fun sanitizeText_withNoEmail_returnsUnchanged() {
        val text = "This text has no email addresses"
        val sanitized = LogSanitizer.sanitizeText(text)

        assertEquals(text, sanitized)
    }

    @Test
    fun sanitizeText_withComplexEmail_redactsCorrectly() {
        val text = "Send to test.user+tag@example.co.uk please"
        val sanitized = LogSanitizer.sanitizeText(text)

        assertEquals("Send to [email-redacted] please", sanitized)
    }
}
