package com.github.radupana.featherweight.util

object LogSanitizer {
    private val EMAIL_REGEX = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""".toRegex()

    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) return "[no-email]"
        return "user_${email.hashCode().toString().takeLast(8)}"
    }

    fun summarizeJson(json: String): String = "[JSON: ${json.length} chars]"

    fun sanitizeText(text: String): String = text.replace(EMAIL_REGEX, "[email-redacted]")
}
