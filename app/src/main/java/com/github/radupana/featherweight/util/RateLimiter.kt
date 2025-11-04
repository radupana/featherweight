package com.github.radupana.featherweight.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

class RateLimiter(
    context: Context,
    private val operationType: String,
    private val maxRequests: Int,
    windowHours: Int,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rate_limiter_$operationType", Context.MODE_PRIVATE)

    private val timestampsKey = "timestamps"
    private val windowMillis = TimeUnit.HOURS.toMillis(windowHours.toLong())

    fun checkLimit() {
        val currentTime = System.currentTimeMillis()
        val timestamps = getTimestamps()

        val recentTimestamps = timestamps.filter { currentTime - it < windowMillis }

        if (recentTimestamps.size >= maxRequests) {
            val oldestTimestamp = recentTimestamps.minOrNull() ?: currentTime
            val resetTime = oldestTimestamp + windowMillis
            val remainingMillis = resetTime - currentTime
            val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)

            throw RateLimitException(operationType, remainingMinutes)
        }
    }

    fun recordRequest() {
        val currentTime = System.currentTimeMillis()
        val timestamps = getTimestamps().toMutableList()

        timestamps.add(currentTime)

        val recentTimestamps = timestamps.filter { currentTime - it < windowMillis }

        saveTimestamps(recentTimestamps)
    }

    private fun getTimestamps(): List<Long> {
        val timestampsString = prefs.getString(timestampsKey, "") ?: ""
        return if (timestampsString.isEmpty()) {
            emptyList()
        } else {
            timestampsString.split(",").mapNotNull { it.toLongOrNull() }
        }
    }

    private fun saveTimestamps(timestamps: List<Long>) {
        prefs.edit().putString(timestampsKey, timestamps.joinToString(",")).apply()
    }

    fun getRemainingRequests(): Int {
        val currentTime = System.currentTimeMillis()
        val timestamps = getTimestamps()
        val recentTimestamps = timestamps.filter { currentTime - it < windowMillis }
        return maxRequests - recentTimestamps.size
    }

    fun getResetTimeMillis(): Long? {
        val currentTime = System.currentTimeMillis()
        val timestamps = getTimestamps()
        val recentTimestamps = timestamps.filter { currentTime - it < windowMillis }

        if (recentTimestamps.isEmpty()) return null

        val oldestTimestamp = recentTimestamps.minOrNull() ?: return null
        return oldestTimestamp + windowMillis
    }

    fun resetForTesting() {
        prefs.edit { clear() }
    }
}
