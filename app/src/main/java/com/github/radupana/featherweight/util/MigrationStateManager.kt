package com.github.radupana.featherweight.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages the state of local data migration.
 * Tracks migration attempts and success/failure states.
 */
class MigrationStateManager(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("migration_state", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"
        private const val KEY_MIGRATION_ATTEMPTS = "migration_attempts"
        private const val KEY_LAST_MIGRATION_ATTEMPT = "last_migration_attempt"
        private const val KEY_MIGRATION_USER_ID = "migration_user_id"
        const val MAX_MIGRATION_ATTEMPTS = 3
    }

    /**
     * Check if migration has been completed successfully.
     */
    fun isMigrationCompleted(): Boolean = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)

    /**
     * Mark migration as completed for a specific user.
     */
    fun markMigrationCompleted(userId: String) {
        prefs
            .edit()
            .putBoolean(KEY_MIGRATION_COMPLETED, true)
            .putString(KEY_MIGRATION_USER_ID, userId)
            .putLong(KEY_LAST_MIGRATION_ATTEMPT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get the number of migration attempts.
     */
    fun getMigrationAttempts(): Int = prefs.getInt(KEY_MIGRATION_ATTEMPTS, 0)

    /**
     * Increment the migration attempt counter.
     */
    fun incrementMigrationAttempts() {
        val current = getMigrationAttempts()
        prefs
            .edit {
                putInt(KEY_MIGRATION_ATTEMPTS, current + 1)
                    .putLong(KEY_LAST_MIGRATION_ATTEMPT, System.currentTimeMillis())
            }
    }

    /**
     * Check if we should attempt migration.
     * Returns false if max attempts reached or already completed.
     */
    fun shouldAttemptMigration(): Boolean = !isMigrationCompleted() && getMigrationAttempts() < MAX_MIGRATION_ATTEMPTS

    /**
     * Reset migration state (useful for testing or retry scenarios).
     */
    fun resetMigrationState() {
        prefs
            .edit()
            .clear()
            .apply()
    }

    /**
     * Get the user ID for which migration was completed.
     */
    fun getMigratedUserId(): String? = prefs.getString(KEY_MIGRATION_USER_ID, null)

    /**
     * Get the timestamp of the last migration attempt.
     */
    fun getLastMigrationAttempt(): Long = prefs.getLong(KEY_LAST_MIGRATION_ATTEMPT, 0)
}
