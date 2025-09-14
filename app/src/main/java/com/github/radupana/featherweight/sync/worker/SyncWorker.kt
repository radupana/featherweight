package com.github.radupana.featherweight.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.sync.SyncState
import com.github.radupana.featherweight.util.ExceptionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val syncManager = ServiceLocator.getSyncManager(applicationContext)
            val authManager = ServiceLocator.getAuthenticationManager(applicationContext)

            if (authManager.getCurrentUserId() == null) {
                return@withContext Result.success()
            }

            try {
                syncManager.syncAll().fold(
                    onSuccess = { state ->
                        when (state) {
                            is SyncState.Success -> Result.success()
                            is SyncState.Error -> {
                                ExceptionLogger.logNonCritical(
                                    "SyncWorker",
                                    "Sync failed: ${state.message}",
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
                            "SyncWorker",
                            "Sync failed with exception",
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
                ExceptionLogger.logNonCritical("SyncWorker", "Unexpected sync error", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
}
