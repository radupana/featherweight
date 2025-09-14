package com.github.radupana.featherweight.manager

import android.content.Context
import androidx.core.content.edit

class AuthenticationManagerImpl(
    context: Context,
) : AuthenticationManager {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WARNING_SHOWN = "unauthenticated_warning_shown"
        private const val KEY_SEEN_UNAUTHENTICATED_WARNING = "seen_unauthenticated_warning"
    }

    override fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    override fun setFirstLaunchComplete() {
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
    }

    override fun isAuthenticated(): Boolean = getCurrentUserId() != null

    override fun getCurrentUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun setCurrentUserId(userId: String?) {
        prefs.edit { putString(KEY_USER_ID, userId) }
    }

    override fun shouldShowWarning(): Boolean = !isAuthenticated() && !prefs.getBoolean(KEY_WARNING_SHOWN, false)

    override fun setWarningShown() {
        prefs.edit { putBoolean(KEY_WARNING_SHOWN, true) }
    }

    override fun clearUserData() {
        prefs.edit {
            remove(KEY_USER_ID)
            putBoolean(KEY_WARNING_SHOWN, false)
            putBoolean(KEY_SEEN_UNAUTHENTICATED_WARNING, false)
        }
    }

    override fun hasSeenUnauthenticatedWarning(): Boolean = prefs.getBoolean(KEY_SEEN_UNAUTHENTICATED_WARNING, false)

    override fun setUnauthenticatedWarningShown() {
        prefs.edit { putBoolean(KEY_SEEN_UNAUTHENTICATED_WARNING, true) }
    }
}
