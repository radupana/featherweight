package com.github.radupana.featherweight.utils

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Provides a unique installation ID for this app installation.
 * The ID is generated on first access and persists across app launches
 * but is reset on app reinstall.
 */
object InstallationIdProvider {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_INSTALLATION_ID = "installation_id"

    @Volatile
    private var installationId: String? = null

    /**
     * Gets the unique installation ID for this app installation.
     * Generates a new UUID on first call after app install.
     */
    fun getId(context: Context): String {
        // Double-checked locking for thread safety
        return installationId ?: synchronized(this) {
            installationId ?: generateAndStoreId(context).also { installationId = it }
        }
    }

    private fun generateAndStoreId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if we already have an ID stored
        val storedId = prefs.getString(KEY_INSTALLATION_ID, null)
        if (storedId != null) {
            return storedId
        }

        // Generate new UUID
        val newId = UUID.randomUUID().toString()

        // Store it
        prefs.edit { putString(KEY_INSTALLATION_ID, newId) }

        return newId
    }

    /**
     * For testing purposes only - clears the cached installation ID
     */
    internal fun clearCache() {
        installationId = null
    }
}
