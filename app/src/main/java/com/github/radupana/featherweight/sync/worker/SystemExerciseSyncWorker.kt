package com.github.radupana.featherweight.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.sync.SyncState
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker for syncing system exercise reference data.
 * Runs less frequently than user data sync (e.g., weekly).
 */
class SystemExerciseSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val syncManager = ServiceLocator.getSyncManager(applicationContext)

            try {
                syncManager.syncSystemExercises().fold(
                    onSuccess = { state ->
                        when (state) {
                            is SyncState.Success -> Result.success()
                            is SyncState.Skipped -> Result.success() // Skipped is not an error
                            is SyncState.Error -> {
                                ExceptionLogger.logNonCritical(
                                    "SystemExerciseSyncWorker",
                                    "System exercise sync failed: ${state.message}",
                                    Exception(state.message),
                                )
                                if (runAttemptCount < 3) {
                                    Result.retry()
                                } else {
                                    Result.failure()
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        ExceptionLogger.logNonCritical(
                            "SystemExerciseSyncWorker",
                            "System exercise sync failed with exception",
                            error as? Exception ?: Exception(error.message),
                        )
                        if (runAttemptCount < 3) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    },
                )
            } catch (e: com.google.firebase.FirebaseException) {
                ExceptionLogger.logNonCritical("SystemExerciseSyncWorker", "Unexpected Firebase sync error", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logNonCritical("SystemExerciseSyncWorker", "Unexpected database sync error", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
}
