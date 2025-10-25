package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to migrate local user data to authenticated user ID after sign-in.
 * Handles data migration from "local" userId to Firebase userId.
 */
class LocalDataMigrationService(
    private val database: FeatherweightDatabase,
) {
    companion object {
        private const val TAG = "LocalDataMigration"
        private const val LOCAL_USER_ID = "local"
    }

    /**
     * Migrate all local data to the authenticated user ID.
     * Returns true if migration succeeded, false otherwise.
     */
    suspend fun migrateLocalDataToUser(targetUserId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (targetUserId == LOCAL_USER_ID) {
                Log.w(TAG, "Cannot migrate to local user ID")
                return@withContext false
            }

            try {
                database.runInTransaction {
                    migrateWorkouts(targetUserId)
                    migrateExerciseLogs(targetUserId)
                    migrateSetLogs(targetUserId)
                    migratePersonalRecords(targetUserId)
                    migrateGlobalExerciseProgress(targetUserId)
                    migrateUserExerciseUsage(targetUserId)
                    migrateCustomExercises(targetUserId)
                    migrateProgrammes(targetUserId)
                    migrateOtherTables(targetUserId)
                }
                Log.i(TAG, "Successfully migrated local data to user: $targetUserId")
                true
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to migrate local data - database error", e)
                false
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to migrate local data - invalid state", e)
                false
            }
        }

    private fun migrateWorkouts(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE workouts SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count workouts")
    }

    private fun migrateExerciseLogs(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE exercise_logs SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count exercise logs")
    }

    private fun migrateSetLogs(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE set_logs SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count set logs")
    }

    private fun migratePersonalRecords(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE personal_records SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count personal records")
    }

    private fun migrateGlobalExerciseProgress(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE global_exercise_progress SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count global exercise progress records")
    }

    private fun migrateUserExerciseUsage(targetUserId: String) {
        val count =
            database
                .compileStatement(
                    "UPDATE user_exercise_usage SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }
        Log.d(TAG, "Migrated $count user exercise usage records")
    }

    private fun migrateCustomExercises(targetUserId: String) {
        // Migrate custom exercises from unified exercises table
        val exerciseCount =
            database
                .compileStatement(
                    "UPDATE exercises SET userId = ? WHERE userId = ? AND type = 'USER'",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        Log.d(TAG, "Migrated $exerciseCount custom exercises")
    }

    private fun migrateProgrammes(targetUserId: String) {
        // Migrate programmes
        val programmeCount =
            database
                .compileStatement(
                    "UPDATE programmes SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate programme weeks
        val weekCount =
            database
                .compileStatement(
                    "UPDATE programme_weeks SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate programme workouts
        val workoutCount =
            database
                .compileStatement(
                    "UPDATE programme_workouts SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate programme progress
        val progressCount =
            database
                .compileStatement(
                    "UPDATE programme_progress SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        Log.d(TAG, "Migrated $programmeCount programmes, $weekCount weeks, $workoutCount workouts, $progressCount progress records")
    }

    private fun migrateOtherTables(targetUserId: String) {
        // Migrate exercise swap history
        val swapCount =
            database
                .compileStatement(
                    "UPDATE exercise_swap_history SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate programme exercise tracking
        val perfCount =
            database
                .compileStatement(
                    "UPDATE programme_exercise_tracking SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate training analysis
        val analysisCount =
            database
                .compileStatement(
                    "UPDATE training_analyses SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate parse requests
        val parseCount =
            database
                .compileStatement(
                    "UPDATE parse_requests SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate exercise max history
        val maxHistoryCount =
            database
                .compileStatement(
                    "UPDATE exercise_max_history SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate workout templates
        val templateCount =
            database
                .compileStatement(
                    "UPDATE workout_templates SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate template exercises
        val templateExCount =
            database
                .compileStatement(
                    "UPDATE template_exercises SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        // Migrate template sets
        val templateSetCount =
            database
                .compileStatement(
                    "UPDATE template_sets SET userId = ? WHERE userId = ?",
                ).use { statement ->
                    statement.bindString(1, targetUserId)
                    statement.bindString(2, LOCAL_USER_ID)
                    statement.executeUpdateDelete()
                }

        Log.d(TAG, "Migrated $swapCount swap history, $perfCount programme exercise tracking, $analysisCount analyses, $parseCount parse requests, $maxHistoryCount max history, $templateCount templates, $templateExCount template exercises, $templateSetCount template sets")
    }

    /**
     * Check if there is local data that needs migration.
     */
    suspend fun hasLocalData(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val count =
                    database
                        .compileStatement(
                            "SELECT COUNT(*) FROM workouts WHERE userId = ?",
                        ).use { statement ->
                            statement.bindString(1, LOCAL_USER_ID)
                            statement.simpleQueryForLong()
                        }
                count > 0
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to check for local data - database error", e)
                false
            }
        }

    /**
     * Delete all local data after successful migration.
     * This is a cleanup operation to prevent duplicate migrations.
     */
    suspend fun cleanupLocalData(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.runInTransaction {
                    database.compileStatement("DELETE FROM workouts WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM exercise_logs WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM set_logs WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM personal_records WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM global_exercise_progress WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM user_exercise_usage WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    // Delete custom exercises from unified exercises table
                    database.compileStatement("DELETE FROM exercises WHERE userId = ? AND type = 'USER'").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM programmes WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM programme_weeks WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM programme_workouts WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM programme_progress WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM exercise_swap_history WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM programme_exercise_tracking WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM training_analyses WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM parse_requests WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM exercise_max_history WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM workout_templates WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM template_exercises WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                    database.compileStatement("DELETE FROM template_sets WHERE userId = ?").use {
                        it.bindString(1, LOCAL_USER_ID)
                        it.executeUpdateDelete()
                    }
                }
                Log.i(TAG, "Successfully cleaned up local data")
                true
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to cleanup local data - database error", e)
                false
            }
        }
}
