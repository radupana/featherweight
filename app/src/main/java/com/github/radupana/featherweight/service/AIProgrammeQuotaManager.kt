package com.github.radupana.featherweight.service

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AIProgrammeQuotaManager(
    context: Context,
) {
    companion object {
        private const val PREFS_NAME = "ai_programme_quota"
        private const val KEY_USAGE_COUNT = "usage_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val DAILY_QUOTA = 5
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    data class QuotaStatus(
        val remainingGenerations: Int,
        val totalQuota: Int,
        val resetsToday: Boolean,
    )

    fun getQuotaStatus(): QuotaStatus {
        resetIfNewDay()

        val usageCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        val remaining = (DAILY_QUOTA - usageCount).coerceAtLeast(0)

        return QuotaStatus(
            remainingGenerations = remaining,
            totalQuota = DAILY_QUOTA,
            resetsToday = false, // For now, resets daily
        )
    }

    fun canGenerateProgramme(): Boolean {
        return true // Unlimited for testing
    }

    fun incrementUsage(): Boolean {
        return true // Always allow for testing
    }

    private fun resetIfNewDay() {
        val today = LocalDate.now().format(dateFormatter)
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "")

        if (lastResetDate != today) {
            // New day, reset quota
            prefs
                .edit()
                .putInt(KEY_USAGE_COUNT, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }

    // For debugging/admin
    fun resetQuota() {
        prefs
            .edit()
            .putInt(KEY_USAGE_COUNT, 0)
            .putString(KEY_LAST_RESET_DATE, LocalDate.now().format(dateFormatter))
            .apply()
    }

    fun getCurrentUsage(): Int {
        resetIfNewDay()
        return prefs.getInt(KEY_USAGE_COUNT, 0)
    }
}
