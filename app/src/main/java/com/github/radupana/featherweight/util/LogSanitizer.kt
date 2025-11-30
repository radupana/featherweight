package com.github.radupana.featherweight.util

import java.security.MessageDigest

object LogSanitizer {
    private val EMAIL_REGEX = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""".toRegex()

    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) return "[no-email]"
        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(email.toByteArray())
                .take(4)
                .joinToString("") { "%02x".format(it) }
        return "user_$hash"
    }

    fun summarizeJson(json: String): String = "[JSON: ${json.length} chars]"

    fun sanitizeText(text: String): String = text.replace(EMAIL_REGEX, "[email-redacted]")
}
