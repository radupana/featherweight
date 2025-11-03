package com.github.radupana.featherweight.manager

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth

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

    override fun isAuthenticated(): Boolean {
        val storedUserId = prefs.getString(KEY_USER_ID, null)
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Validate that stored user matches Firebase Auth state
        return when {
            storedUserId == null -> false
            firebaseUser == null -> {
                // Stored user exists but Firebase Auth is null - corrupted state
                // DO NOT LOG - could cause recursion issues during app initialization
                clearUserData()
                false
            }
            firebaseUser.uid != storedUserId -> {
                // User mismatch - update to correct user
                // DO NOT LOG - could cause recursion issues
                setCurrentUserId(firebaseUser.uid)
                isUserVerified(firebaseUser)
            }
            else -> isUserVerified(firebaseUser)
        }
    }

    private fun isUserVerified(firebaseUser: com.google.firebase.auth.FirebaseUser): Boolean {
        // Check if user signed in with email/password and needs verification
        val isPasswordProvider = firebaseUser.providerData.any { it.providerId == "password" }
        return if (isPasswordProvider) {
            firebaseUser.isEmailVerified
        } else {
            // User signed in with Google or other provider, no email verification needed
            true
        }
    }

    override fun getCurrentUserId(): String? {
        val storedUserId = prefs.getString(KEY_USER_ID, null)
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        return when {
            storedUserId == null -> null
            firebaseUser == null -> {
                // Stored user exists but Firebase Auth is null - corrupted state
                // DO NOT LOG HERE - it causes infinite recursion with CloudLogger!
                // CloudLogger calls getCurrentUserId() which would call CloudLogger again
                clearUserData()
                null
            }
            firebaseUser.uid != storedUserId -> {
                // User mismatch detected - this shouldn't happen during normal operation
                // Return Firebase user ID - DO NOT LOG to avoid recursion
                firebaseUser.uid
            }
            else -> storedUserId
        }
    }

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
            // Reset first launch flag so user sees WelcomeActivity after signing out
            putBoolean(KEY_FIRST_LAUNCH, true)
        }
    }

    override fun hasSeenUnauthenticatedWarning(): Boolean = prefs.getBoolean(KEY_SEEN_UNAUTHENTICATED_WARNING, false)

    override fun setUnauthenticatedWarningShown() {
        prefs.edit { putBoolean(KEY_SEEN_UNAUTHENTICATED_WARNING, true) }
    }
}
