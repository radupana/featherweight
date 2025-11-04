package com.github.radupana.featherweight.util

class RateLimitException(
    val operationType: String,
    val remainingMinutes: Long,
) : Exception("Rate limit exceeded for $operationType. Please try again in $remainingMinutes minutes.")
