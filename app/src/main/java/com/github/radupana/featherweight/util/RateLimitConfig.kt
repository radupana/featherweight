package com.github.radupana.featherweight.util

object RateLimitConfig {
    // 1 request per 10 minutes = 6 per hour max
    const val PROGRAMME_PARSING_MAX_REQUESTS = 6
    const val PROGRAMME_PARSING_WINDOW_HOURS = 1
}
