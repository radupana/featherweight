@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.sync

import android.content.Context
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.LocalSyncMetadata
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.sync.strategies.CustomExerciseSyncStrategy
import com.github.radupana.featherweight.sync.strategies.SystemExerciseSyncStrategy
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.github.radupana.featherweight.utils.InstallationIdProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed class SyncState {
    data class Success(
        val timestamp: Timestamp,
    ) : SyncState()

    data class Error(
        val message: String,
    ) : SyncState()

    data class Skipped(
        val reason: String,
    ) : SyncState()
}

class SyncManager(
    private val context: Context,
    private val database: FeatherweightDatabase,
    private val authManager: AuthenticationManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Mutex to prevent concurrent sync operations
    private val syncMutex = Mutex()

    // Track last sync time to prevent rapid successive syncs
    private var lastSyncTime: Long = 0
    private val syncCooldownMs = 5 * 60 * 1000L // 5 minutes cooldown between syncs

    // SharedPreferences for persistent storage of sync metadata
    private val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SYNC_TIME = "last_successful_sync_time"
    }

    init {
        // Load last sync time from persistent storage
        lastSyncTime = syncPrefs.getLong(KEY_LAST_SYNC_TIME, 0)
    }

    // Sync strategies for modular sync logic
    private val systemExerciseStrategy = SystemExerciseSyncStrategy(database, firestoreRepository)
    private val customExerciseStrategy = CustomExerciseSyncStrategy(database, firestoreRepository)

    // Helpers for upload/download operations
    private val uploader = SyncUploader(database, firestoreRepository)
    private val downloader = SyncDownloader(database, firestoreRepository)

    // Use installation ID for device-specific sync tracking
    private val installationId: String by lazy {
        InstallationIdProvider.getId(context)
    }

    fun getLastSyncTime(): Long = syncPrefs.getLong(KEY_LAST_SYNC_TIME, 0)

    suspend fun syncAll(): Result<SyncState> =
        withContext(ioDispatcher) {
            CloudLogger.info("SyncManager", "syncAll: Attempting to acquire sync mutex...")
            syncMutex.withLock {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastSync = currentTime - lastSyncTime
                if (timeSinceLastSync < syncCooldownMs) {
                    val remainingCooldown = (syncCooldownMs - timeSinceLastSync) / 1000
                    CloudLogger.info("SyncManager", "Sync skipped - cooldown active (${remainingCooldown}s remaining)")
                    return@withContext Result.success(SyncState.Skipped("Sync cooldown active - ${remainingCooldown}s remaining"))
                }

                CloudLogger.info("SyncManager", "syncAll: Mutex acquired, starting sync")
                val userId = authManager.getCurrentUserId()
                CloudLogger.info("SyncManager", "syncAll started for userId: $userId")
                if (userId == null) {
                    CloudLogger.error("SyncManager", "User not authenticated")
                    val errorState = SyncState.Error("User not authenticated")
                    return@withContext Result.success(errorState)
                }

                val syncStartTime = System.currentTimeMillis()
                lastSyncTime = syncStartTime // Update last sync time

                try {
                    // Check if database is empty (fresh install scenario)
                    val isEmptyDatabase = isDatabaseEmpty(userId)
                    CloudLogger.info("SyncManager", "Database empty check: $isEmptyDatabase")

                    CloudLogger.info("SyncManager", "Getting last sync time...")
                    var lastSyncTime = getLastSyncTime(userId)

                    // If database is empty but sync metadata exists, force full sync
                    if (isEmptyDatabase && lastSyncTime != null) {
                        CloudLogger.info("SyncManager", "Empty database detected with existing sync metadata - forcing full restore")
                        lastSyncTime = null
                    }

                    CloudLogger.info("SyncManager", "Last sync time: $lastSyncTime")

                    CloudLogger.info("SyncManager", "Starting download of remote changes...")
                    downloadRemoteChanges(userId, lastSyncTime).fold(
                        onSuccess = {
                            CloudLogger.info("SyncManager", "Download successful, starting upload...")
                            uploadLocalChanges(userId, lastSyncTime).fold(
                                onSuccess = {
                                    CloudLogger.info("SyncManager", "Upload successful, updating metadata...")
                                    updateSyncMetadata(userId)
                                    val syncDuration = System.currentTimeMillis() - syncStartTime
                                    CloudLogger.info("SyncManager", "Sync completed successfully - duration: ${syncDuration}ms (${syncDuration / 1000.0}s)")

                                    // Save successful sync time to SharedPreferences
                                    val currentTime = System.currentTimeMillis()
                                    syncPrefs.edit().putLong(KEY_LAST_SYNC_TIME, currentTime).apply()

                                    Result.success(SyncState.Success(Timestamp.now()))
                                },
                                onFailure = { error ->
                                    CloudLogger.error("SyncManager", "Upload failed: ${error.message}", error)
                                    Result.failure(error)
                                },
                            )
                        },
                        onFailure = { error ->
                            CloudLogger.error("SyncManager", "Download failed: ${error.message}", error)
                            Result.failure(error)
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "Sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.failure(e)
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "Sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.failure(e)
                } catch (e: Exception) {
                    CloudLogger.error("SyncManager", "Sync failed with unexpected exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.failure(e)
                } finally {
                    CloudLogger.info("SyncManager", "syncAll: Releasing sync mutex")
                }
            } // End of mutex lock
        }

    private suspend fun uploadLocalChanges(
        userId: String,
        @Suppress("UNUSED_PARAMETER")
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            uploader.uploadAllLocalChanges(userId)
            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            ExceptionLogger.logNonCritical("SyncManager", "Upload failed - Firebase error", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            ExceptionLogger.logNonCritical("SyncManager", "Upload failed - database error", e)
            Result.failure(e)
        }

    private suspend fun isDatabaseEmpty(userId: String): Boolean {
        // Check if the database has any user data
        // We only need to check workouts as they are the primary data
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val userWorkoutCount = workouts.size

        val isEmpty = userWorkoutCount == 0
        CloudLogger.debug("SyncManager", "isDatabaseEmpty: userWorkouts=$userWorkoutCount, isEmpty=$isEmpty")

        return isEmpty
    }

    private suspend fun getLastSyncTime(
        userId: String,
        dataType: String = "all",
    ): Timestamp? {
        // Only use local database for device-specific sync time
        val localSyncTime =
            database.localSyncMetadataDao().getLastSyncTime(
                userId = userId,
                installationId = installationId,
                dataType = dataType,
            )

        // Convert LocalDateTime to Timestamp if we have a sync time
        return localSyncTime?.let {
            val instant = it.toInstant(ZoneOffset.UTC)
            Timestamp(instant.epochSecond, instant.nano)
        }
    }

    private suspend fun updateSyncMetadata(userId: String) {
        val now = LocalDateTime.now()

        // Only update local database (device-specific sync tracking)
        database.localSyncMetadataDao().insertOrUpdate(
            LocalSyncMetadata(
                userId = userId,
                installationId = installationId,
                dataType = "all",
                lastSyncTime = now,
                lastSuccessfulSync = now,
            ),
        )
    }

    private suspend fun downloadRemoteChanges(
        userId: String,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            CloudLogger.debug("SyncManager", "downloadRemoteChanges: Starting download for user $userId")

            downloadExercises(userId, lastSyncTime)
            downloadProgrammeData(userId)
            downloadWorkoutData(userId, lastSyncTime)
            downloadTemplateData(userId, lastSyncTime)
            downloadUserStats(userId)
            downloadTrackingData(userId)

            CloudLogger.debug("SyncManager", "downloadRemoteChanges: All downloads completed successfully")
            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error("SyncManager", "downloadRemoteChanges failed - Firebase error: ${e.message}", e)
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error("SyncManager", "downloadRemoteChanges failed - database error: ${e.message}", e)
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            CloudLogger.error("SyncManager", "downloadRemoteChanges failed - unexpected error: ${e.message}", e)
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        }

    private suspend fun executeWithLogging(
        operationName: String,
        operation: suspend () -> Unit,
    ) {
        CloudLogger.debug("SyncManager", "Downloading $operationName...")
        try {
            operation()
            CloudLogger.debug("SyncManager", "$operationName download completed")
        } catch (e: Exception) {
            CloudLogger.error("SyncManager", "$operationName download failed: ${e.message}", e)
            throw e
        }
    }

    private suspend fun downloadExercises(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        executeWithLogging("system exercises") {
            systemExerciseStrategy.downloadAndMerge(null, null).getOrThrow()
        }
        executeWithLogging("custom exercises") {
            customExerciseStrategy.downloadAndMerge(userId, lastSyncTime).getOrThrow()
        }
    }

    private suspend fun downloadProgrammeData(userId: String) {
        executeWithLogging("programmes") { downloader.downloadAndMergeProgrammes(userId) }
        executeWithLogging("programme weeks") { downloader.downloadAndMergeProgrammeWeeks(userId) }
        executeWithLogging("programme workouts") { downloader.downloadAndMergeProgrammeWorkouts(userId) }
        executeWithLogging("programme progress") { downloader.downloadAndMergeProgrammeProgress(userId) }
    }

    private suspend fun downloadWorkoutData(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        executeWithLogging("workouts") { downloader.downloadAndMergeWorkouts(userId, lastSyncTime) }
        executeWithLogging("exercise logs") { downloader.downloadAndMergeExerciseLogs(userId, lastSyncTime) }
        executeWithLogging("set logs") { downloader.downloadAndMergeSetLogs(userId, lastSyncTime) }
    }

    private suspend fun downloadTemplateData(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        executeWithLogging("workout templates") { downloader.downloadAndMergeWorkoutTemplates(userId, lastSyncTime) }
        executeWithLogging("template exercises") { downloader.downloadAndMergeTemplateExercises(userId, lastSyncTime) }
        executeWithLogging("template sets") { downloader.downloadAndMergeTemplateSets(userId, lastSyncTime) }
    }

    private suspend fun downloadUserStats(userId: String) {
        executeWithLogging("user exercise maxes") { downloader.downloadAndMergeUserExerciseMaxes(userId) }
        executeWithLogging("personal records") { downloader.downloadAndMergePersonalRecords(userId) }
        executeWithLogging("user exercise usages") { downloader.downloadAndMergeUserExerciseUsages(userId) }
    }

    private suspend fun downloadTrackingData(userId: String) {
        CloudLogger.debug("SyncManager", "Starting download of tracking data...")
        executeWithLogging("exercise swap history") { downloader.downloadAndMergeExerciseSwapHistory(userId) }
        executeWithLogging("exercise performance tracking") { downloader.downloadAndMergeExercisePerformanceTracking(userId) }
        executeWithLogging("global exercise progress") { downloader.downloadAndMergeGlobalExerciseProgress(userId) }
        executeWithLogging("training analyses") { downloader.downloadAndMergeTrainingAnalyses(userId) }
        executeWithLogging("parse requests") { downloader.downloadAndMergeParseRequests(userId) }
    }

    /**
     * Sync only system exercise reference data.
     * This should be called less frequently than user data sync.
     */
    suspend fun syncSystemExercises(): Result<SyncState> =
        withContext(ioDispatcher) {
            // Use mutex to prevent concurrent sync operations
            syncMutex.withLock {
                CloudLogger.info("SyncManager", "Starting system exercise sync")
                try {
                    // System exercises don't require authentication
                    systemExerciseStrategy.downloadAndMerge(null, null).fold(
                        onSuccess = {
                            CloudLogger.info("SyncManager", "System exercise sync completed successfully")
                            Result.success(SyncState.Success(Timestamp.now()))
                        },
                        onFailure = { error ->
                            CloudLogger.error("SyncManager", "System exercise sync failed: ${error.message}", error)
                            Result.failure(error)
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "System exercise sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "System exercise sync failed", e)
                    Result.failure(e)
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "System exercise sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "System exercise sync failed", e)
                    Result.failure(e)
                }
            } // End of mutex lock
        }

    /**
     * Sync only user-specific data.
     * This should be called more frequently than system data sync.
     */
    suspend fun syncUserData(userId: String): Result<SyncState> =
        withContext(ioDispatcher) {
            // Use mutex to prevent concurrent sync operations
            syncMutex.withLock {
                CloudLogger.info("SyncManager", "Starting user data sync for userId: $userId")

                try {
                    val lastSyncTime = getLastSyncTime(userId)

                    downloadUserData(userId, lastSyncTime).fold(
                        onSuccess = {
                            uploadUserData(userId).fold(
                                onSuccess = {
                                    updateSyncMetadata(userId)
                                    CloudLogger.info("SyncManager", "User data sync completed successfully")
                                    Result.success(SyncState.Success(Timestamp.now()))
                                },
                                onFailure = { error ->
                                    CloudLogger.error("SyncManager", "User data upload failed: ${error.message}", error)
                                    Result.failure(error)
                                },
                            )
                        },
                        onFailure = { error ->
                            CloudLogger.error("SyncManager", "User data download failed: ${error.message}", error)
                            Result.failure(error)
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "User data sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "User data sync failed", e)
                    Result.failure(e)
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "User data sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "User data sync failed", e)
                    Result.failure(e)
                }
            } // End of mutex lock
        }

    private suspend fun downloadUserData(
        userId: String,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            CloudLogger.debug("SyncManager", "Downloading custom exercises for user...")
            customExerciseStrategy.downloadAndMerge(userId, lastSyncTime).getOrThrow()

            downloader.downloadAndMergeProgrammes(userId)
            downloader.downloadAndMergeProgrammeWeeks(userId)
            downloader.downloadAndMergeProgrammeWorkouts(userId)
            downloader.downloadAndMergeProgrammeProgress(userId)

            downloader.downloadAndMergeWorkouts(userId, lastSyncTime)
            downloader.downloadAndMergeExerciseLogs(userId, lastSyncTime)
            downloader.downloadAndMergeSetLogs(userId, lastSyncTime)

            downloader.downloadAndMergeWorkoutTemplates(userId, lastSyncTime)
            downloader.downloadAndMergeTemplateExercises(userId, lastSyncTime)
            downloader.downloadAndMergeTemplateSets(userId, lastSyncTime)

            downloader.downloadAndMergeUserExerciseMaxes(userId)
            downloader.downloadAndMergePersonalRecords(userId)
            downloader.downloadAndMergeUserExerciseUsages(userId)

            downloader.downloadAndMergeExerciseSwapHistory(userId)
            downloader.downloadAndMergeExercisePerformanceTracking(userId)
            downloader.downloadAndMergeGlobalExerciseProgress(userId)
            downloader.downloadAndMergeTrainingAnalyses(userId)
            downloader.downloadAndMergeParseRequests(userId)

            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error("SyncManager", "downloadUserData failed - Firebase error: ${e.message}", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error("SyncManager", "downloadUserData failed - database error: ${e.message}", e)
            Result.failure(e)
        }

    private suspend fun uploadUserData(
        userId: String,
    ): Result<Unit> =
        try {
            uploader.uploadUserData(userId)
            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error("SyncManager", "uploadUserData failed - Firebase error: ${e.message}", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error("SyncManager", "uploadUserData failed - database error: ${e.message}", e)
            Result.failure(e)
        }
}
