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
 * Worker for syncing user-specific data.
 * Runs more frequently than system data sync (e.g., hourly or on-demand).
 */
class UserDataSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val syncManager = ServiceLocator.getSyncManager(applicationContext)
            val authManager = ServiceLocator.getAuthenticationManager(applicationContext)

            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                return@withContext Result.success()
            }

            try {
                syncManager.syncUserData(userId).fold(
                    onSuccess = { state ->
                        when (state) {
                            is SyncState.Success -> Result.success()
                            is SyncState.Skipped -> Result.success() // Skipped is not an error
                            is SyncState.Error -> {
                                ExceptionLogger.logNonCritical(
                                    "UserDataSyncWorker",
                                    "User data sync failed: ${state.message}",
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
                            "UserDataSyncWorker",
                            "User data sync failed with exception",
                            error as? Exception ?: Exception(error.message),
                        )
                        if (runAttemptCount < 3) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    },
                )
            } catch (e: Exception) {
                ExceptionLogger.logNonCritical("UserDataSyncWorker", "Unexpected sync error", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
}
