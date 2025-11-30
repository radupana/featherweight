@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.sync

import android.content.Context
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.LocalSyncMetadata
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.converters.SyncConverters
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

    // Use installation ID for device-specific sync tracking
    private val installationId: String by lazy {
        InstallationIdProvider.getId(context)
    }

    fun getLastSyncTime(): Long = syncPrefs.getLong(KEY_LAST_SYNC_TIME, 0)

    suspend fun syncAll(): Result<SyncState> =
        withContext(ioDispatcher) {
            // Check cooldown period to prevent rapid successive syncs
            val currentTime = System.currentTimeMillis()
            val timeSinceLastSync = currentTime - lastSyncTime
            if (timeSinceLastSync < syncCooldownMs) {
                val remainingCooldown = (syncCooldownMs - timeSinceLastSync) / 1000
                CloudLogger.info("SyncManager", "Sync skipped - cooldown active (${remainingCooldown}s remaining)")
                return@withContext Result.success(SyncState.Skipped("Sync cooldown active - ${remainingCooldown}s remaining"))
            }

            CloudLogger.info("SyncManager", "syncAll: Attempting to acquire sync mutex...")
            // Use mutex to prevent concurrent sync operations
            // Multiple sync requests will queue and execute sequentially
            syncMutex.withLock {
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
                                    Result.success(SyncState.Error("Upload failed: ${error.message}"))
                                },
                            )
                        },
                        onFailure = { error ->
                            CloudLogger.error("SyncManager", "Download failed: ${error.message}", error)
                            Result.success(SyncState.Error("Download failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "Sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.success(SyncState.Error("Sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "Sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.success(SyncState.Error("Sync failed: ${e.message}"))
                } catch (e: Exception) {
                    CloudLogger.error("SyncManager", "Sync failed with unexpected exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.success(SyncState.Error("Sync failed: ${e.message}"))
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
            CloudLogger.debug("SyncManager", "uploadLocalChanges: Starting upload for user $userId")

            CloudLogger.debug("SyncManager", "Uploading workouts...")
            uploadWorkouts(userId)
            CloudLogger.debug("SyncManager", "Uploading exercise logs...")
            uploadExerciseLogs(userId)
            CloudLogger.debug("SyncManager", "Uploading set logs...")
            uploadSetLogs(userId)

            CloudLogger.debug("SyncManager", "Uploading workout templates...")
            uploadWorkoutTemplates(userId)
            CloudLogger.debug("SyncManager", "Uploading template exercises...")
            uploadTemplateExercises(userId)
            CloudLogger.debug("SyncManager", "Uploading template sets...")
            uploadTemplateSets(userId)

            // Custom exercises are uploaded via CustomExerciseSyncStrategy

            uploadProgrammes(userId)
            uploadProgrammeWeeks(userId)
            uploadProgrammeWorkouts(userId)
            uploadProgrammeProgress(userId)

            uploadUserExerciseMaxes(userId)
            uploadPersonalRecords(userId)
            uploadUserExerciseUsages(userId)

            uploadExerciseSwapHistory(userId)
            uploadExercisePerformanceTracking(userId)
            uploadGlobalExerciseProgress(userId)
            uploadTrainingAnalyses(userId)
            uploadParseRequests(userId)

            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            ExceptionLogger.logNonCritical("SyncManager", "Upload failed - Firebase error", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            ExceptionLogger.logNonCritical("SyncManager", "Upload failed - database error", e)
            Result.failure(e)
        }

    private suspend fun uploadWorkouts(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val userWorkouts = workouts
        val firestoreWorkouts = userWorkouts.map { SyncConverters.toFirestoreWorkout(it) }
        firestoreRepository.uploadWorkouts(userId, firestoreWorkouts).getOrThrow()
    }

    private suspend fun uploadExerciseLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val userWorkouts = workouts

        val exerciseLogs = mutableListOf<com.github.radupana.featherweight.data.ExerciseLog>()
        userWorkouts.forEach { workout ->
            val logs = database.exerciseLogDao().getExerciseLogsForWorkout(workout.id)
            exerciseLogs.addAll(logs)
        }

        val firestoreLogs = exerciseLogs.map { SyncConverters.toFirestoreExerciseLog(it) }
        firestoreRepository.uploadExerciseLogs(userId, firestoreLogs).getOrThrow()
    }

    private suspend fun uploadSetLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val userWorkouts = workouts

        val exerciseLogs = mutableListOf<com.github.radupana.featherweight.data.ExerciseLog>()
        userWorkouts.forEach { workout ->
            val logs = database.exerciseLogDao().getExerciseLogsForWorkout(workout.id)
            exerciseLogs.addAll(logs)
        }

        val setLogs = mutableListOf<com.github.radupana.featherweight.data.SetLog>()
        exerciseLogs.forEach { exerciseLog ->
            val sets = database.setLogDao().getSetLogsForExercise(exerciseLog.id)
            setLogs.addAll(sets)
        }

        val firestoreLogs = setLogs.map { SyncConverters.toFirestoreSetLog(it) }
        firestoreRepository.uploadSetLogs(userId, firestoreLogs).getOrThrow()
    }

    private suspend fun uploadWorkoutTemplates(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val firestoreTemplates = templates.map { SyncConverters.toFirestoreWorkoutTemplate(it) }
        firestoreRepository.uploadWorkoutTemplates(userId, firestoreTemplates).getOrThrow()
    }

    private suspend fun uploadTemplateExercises(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val templateExercises = mutableListOf<com.github.radupana.featherweight.data.TemplateExercise>()
        templates.forEach { template ->
            val exercises = database.templateExerciseDao().getExercisesForTemplate(template.id)
            templateExercises.addAll(exercises)
        }
        val firestoreExercises = templateExercises.map { SyncConverters.toFirestoreTemplateExercise(it) }
        firestoreRepository.uploadTemplateExercises(userId, firestoreExercises).getOrThrow()
    }

    private suspend fun uploadTemplateSets(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val templateExercises = mutableListOf<com.github.radupana.featherweight.data.TemplateExercise>()
        templates.forEach { template ->
            val exercises = database.templateExerciseDao().getExercisesForTemplate(template.id)
            templateExercises.addAll(exercises)
        }

        val templateSets = mutableListOf<com.github.radupana.featherweight.data.TemplateSet>()
        templateExercises.forEach { exercise ->
            val sets = database.templateSetDao().getSetsForTemplateExercise(exercise.id)
            templateSets.addAll(sets)
        }

        val firestoreSets = templateSets.map { SyncConverters.toFirestoreTemplateSet(it) }
        firestoreRepository.uploadTemplateSets(userId, firestoreSets).getOrThrow()
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

    private suspend fun uploadProgrammes(userId: String) {
        val programmes = database.programmeDao().getAllProgrammes()
        val userProgrammes = programmes.filter { it.userId == userId }
        val firestoreProgrammes = userProgrammes.map { SyncConverters.toFirestoreProgramme(it) }
        firestoreRepository.uploadProgrammes(userId, firestoreProgrammes).getOrThrow()
    }

    private suspend fun uploadProgrammeWeeks(userId: String) {
        val weeks = database.programmeDao().getAllProgrammeWeeks()
        val userWeeks = weeks.filter { it.userId == userId }
        val firestoreWeeks = userWeeks.map { SyncConverters.toFirestoreProgrammeWeek(it) }
        firestoreRepository.uploadProgrammeWeeks(userId, firestoreWeeks).getOrThrow()
    }

    private suspend fun uploadProgrammeWorkouts(userId: String) {
        val workouts = database.programmeDao().getAllProgrammeWorkouts()
        val userWorkouts = workouts.filter { it.userId == userId }
        val firestoreWorkouts = userWorkouts.map { SyncConverters.toFirestoreProgrammeWorkout(it) }
        firestoreRepository.uploadProgrammeWorkouts(userId, firestoreWorkouts).getOrThrow()
    }

    private suspend fun uploadProgrammeProgress(userId: String) {
        val progress = database.programmeDao().getAllProgrammeProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreProgrammeProgress(it) }
        firestoreRepository.uploadProgrammeProgress(userId, firestoreProgress).getOrThrow()
    }

    private suspend fun uploadUserExerciseMaxes(userId: String) {
        val allTracking = database.exerciseMaxTrackingDao().getAllForUser(userId)
        val firestoreMaxes = allTracking.map { SyncConverters.toFirestoreUserExerciseMax(it) }
        firestoreRepository.uploadUserExerciseMaxes(userId, firestoreMaxes).getOrThrow()
    }

    private suspend fun uploadPersonalRecords(userId: String) {
        val records = database.personalRecordDao().getAllPersonalRecords()
        val userRecords = records.filter { it.userId == userId }
        val firestoreRecords = userRecords.map { SyncConverters.toFirestorePersonalRecord(it) }
        firestoreRepository.uploadPersonalRecords(userId, firestoreRecords).getOrThrow()
    }

    private suspend fun uploadUserExerciseUsages(userId: String) {
        val usages = database.userExerciseUsageDao().getAllUsageForUser(userId)
        val firestoreUsages = usages.map { SyncConverters.toFirestoreExerciseUsage(it) }
        firestoreRepository.uploadUserExerciseUsages(userId, firestoreUsages).getOrThrow()
    }

    private suspend fun uploadExerciseSwapHistory(userId: String) {
        val swaps = database.exerciseSwapHistoryDao().getAllSwapHistory()
        val userSwaps = swaps.filter { it.userId == userId }
        val firestoreSwaps = userSwaps.map { SyncConverters.toFirestoreExerciseSwapHistory(it) }
        firestoreRepository.uploadExerciseSwapHistory(userId, firestoreSwaps).getOrThrow()
    }

    private suspend fun uploadExercisePerformanceTracking(userId: String) {
        val tracking = database.programmeExerciseTrackingDao().getAllTracking()
        val userTracking = tracking.filter { it.userId == userId }
        val firestoreTracking = userTracking.map { SyncConverters.toFirestoreProgrammeExerciseTracking(it) }
        firestoreRepository.uploadExercisePerformanceTracking(userId, firestoreTracking).getOrThrow()
    }

    private suspend fun uploadGlobalExerciseProgress(userId: String) {
        val progress = database.globalExerciseProgressDao().getAllProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreGlobalExerciseProgress(it) }
        firestoreRepository.uploadGlobalExerciseProgress(userId, firestoreProgress).getOrThrow()
    }

    private suspend fun uploadTrainingAnalyses(userId: String) {
        val analyses = database.trainingAnalysisDao().getAllAnalyses()
        val userAnalyses = analyses.filter { it.userId == userId }
        val firestoreAnalyses = userAnalyses.map { SyncConverters.toFirestoreTrainingAnalysis(it) }
        firestoreRepository.uploadTrainingAnalyses(userId, firestoreAnalyses).getOrThrow()
    }

    private suspend fun uploadParseRequests(userId: String) {
        val requests = database.parseRequestDao().getAllRequestsList()
        val userRequests = requests.filter { it.userId == userId }
        val firestoreRequests = userRequests.map { SyncConverters.toFirestoreParseRequest(it) }
        firestoreRepository.uploadParseRequests(userId, firestoreRequests).getOrThrow()
    }

    private suspend fun downloadRemoteChanges(
        userId: String,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            CloudLogger.debug("SyncManager", "downloadRemoteChanges: Starting download for user $userId")

            CloudLogger.debug("SyncManager", "Downloading system exercises...")
            try {
                systemExerciseStrategy.downloadAndMerge(null, null).getOrThrow()
                CloudLogger.debug("SyncManager", "System exercises download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "System exercises download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading custom exercises...")
            try {
                customExerciseStrategy.downloadAndMerge(userId, lastSyncTime).getOrThrow()
                CloudLogger.debug("SyncManager", "Custom exercises download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Custom exercises download failed: ${e.message}", e)
                throw e
            }
            CloudLogger.debug("SyncManager", "Downloading programmes...")
            try {
                downloadAndMergeProgrammes(userId)
                CloudLogger.debug("SyncManager", "Programmes download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Programmes download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading programme weeks...")
            try {
                downloadAndMergeProgrammeWeeks(userId)
                CloudLogger.debug("SyncManager", "Programme weeks download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Programme weeks download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading programme workouts...")
            try {
                downloadAndMergeProgrammeWorkouts(userId)
                CloudLogger.debug("SyncManager", "Programme workouts download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Programme workouts download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading programme progress...")
            try {
                downloadAndMergeProgrammeProgress(userId)
                CloudLogger.debug("SyncManager", "Programme progress download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Programme progress download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading workouts...")
            try {
                downloadAndMergeWorkouts(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Workouts download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Workouts download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading exercise logs...")
            try {
                downloadAndMergeExerciseLogs(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Exercise logs download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Exercise logs download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading set logs...")
            try {
                downloadAndMergeSetLogs(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Set logs download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Set logs download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading workout templates...")
            try {
                downloadAndMergeWorkoutTemplates(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Workout templates download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Workout templates download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading template exercises...")
            try {
                downloadAndMergeTemplateExercises(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Template exercises download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Template exercises download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading template sets...")
            try {
                downloadAndMergeTemplateSets(userId, lastSyncTime)
                CloudLogger.debug("SyncManager", "Template sets download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Template sets download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading user exercise maxes...")
            try {
                downloadAndMergeUserExerciseMaxes(userId)
                CloudLogger.debug("SyncManager", "User exercise maxes download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "User exercise maxes download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading personal records...")
            try {
                downloadAndMergePersonalRecords(userId)
                CloudLogger.debug("SyncManager", "Personal records download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Personal records download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading user exercise usages...")
            try {
                downloadAndMergeUserExerciseUsages(userId)
                CloudLogger.debug("SyncManager", "User exercise usages download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "User exercise usages download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Starting download of tracking data...")
            CloudLogger.debug("SyncManager", "Downloading exercise swap history...")
            try {
                downloadAndMergeExerciseSwapHistory(userId)
                CloudLogger.debug("SyncManager", "Exercise swap history download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Exercise swap history download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading exercise performance tracking...")
            try {
                downloadAndMergeExercisePerformanceTracking(userId)
                CloudLogger.debug("SyncManager", "Exercise performance tracking download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Exercise performance tracking download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading global exercise progress...")
            try {
                downloadAndMergeGlobalExerciseProgress(userId)
                CloudLogger.debug("SyncManager", "Global exercise progress download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Global exercise progress download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading training analyses...")
            try {
                downloadAndMergeTrainingAnalyses(userId)
                CloudLogger.debug("SyncManager", "Training analyses download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Training analyses download failed: ${e.message}", e)
                throw e
            }

            CloudLogger.debug("SyncManager", "Downloading parse requests...")
            try {
                downloadAndMergeParseRequests(userId)
                CloudLogger.debug("SyncManager", "Parse requests download completed")
            } catch (e: Exception) {
                CloudLogger.error("SyncManager", "Parse requests download failed: ${e.message}", e)
                throw e
            }

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

    private suspend fun downloadAndMergeWorkouts(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncManager", "downloadAndMergeWorkouts: Starting for user $userId")
        val remoteWorkouts = firestoreRepository.downloadWorkouts(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeWorkouts: Downloaded ${remoteWorkouts.size} workouts")
        val localWorkouts = remoteWorkouts.map { SyncConverters.fromFirestoreWorkout(it) }

        // Use upsert to avoid constraint violations - Room will handle insert or update
        localWorkouts.forEach { workout ->
            database.workoutDao().upsertWorkout(workout)
        }
    }

    private suspend fun downloadAndMergeExerciseLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncManager", "downloadAndMergeExerciseLogs: Starting for user $userId")
        val remoteLogs = firestoreRepository.downloadExerciseLogs(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeExerciseLogs: Downloaded ${remoteLogs.size} exercise logs")
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreExerciseLog(it) }

        // Get all local workout IDs to filter out orphaned exercise logs
        val localWorkoutIds =
            database
                .workoutDao()
                .getAllWorkouts(userId)
                .map { it.id }
                .toSet()

        var insertedCount = 0
        var skippedCount = 0
        localLogs.forEach { log ->
            // Skip exercise logs that reference non-existent workouts (orphaned data)
            if (log.workoutId !in localWorkoutIds) {
                CloudLogger.warn(
                    "SyncManager",
                    "Skipping orphaned exercise log ${log.id} - workout ${log.workoutId} doesn't exist",
                )
                skippedCount++
                return@forEach
            }
            database.exerciseLogDao().upsertExerciseLog(log)
            insertedCount++
        }
        CloudLogger.debug(
            "SyncManager",
            "downloadAndMergeExerciseLogs: Inserted $insertedCount, skipped $skippedCount orphaned exercise logs",
        )
    }

    private suspend fun downloadAndMergeSetLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncManager", "downloadAndMergeSetLogs: Starting for user $userId")
        val remoteLogs = firestoreRepository.downloadSetLogs(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeSetLogs: Downloaded ${remoteLogs.size} set logs")
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreSetLog(it) }

        // Get all local exercise log IDs to filter out orphaned set logs
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val workoutIds = workouts.map { it.id }
        val allExerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkouts(workoutIds)
        val localExerciseLogIds = allExerciseLogs.map { it.id }.toSet()

        var insertedCount = 0
        var skippedCount = 0
        localLogs.forEach { log ->
            // Skip set logs that reference non-existent exercise logs (orphaned data)
            if (log.exerciseLogId !in localExerciseLogIds) {
                CloudLogger.warn(
                    "SyncManager",
                    "Skipping orphaned set log ${log.id} - exercise log ${log.exerciseLogId} doesn't exist",
                )
                skippedCount++
                return@forEach
            }
            database.setLogDao().upsertSetLog(log)
            insertedCount++
        }
        CloudLogger.debug(
            "SyncManager",
            "downloadAndMergeSetLogs: Inserted $insertedCount, skipped $skippedCount orphaned set logs",
        )
    }

    private suspend fun downloadAndMergeWorkoutTemplates(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncManager", "downloadAndMergeWorkoutTemplates: Starting for user $userId")
        val remoteTemplates = firestoreRepository.downloadWorkoutTemplates(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeWorkoutTemplates: Downloaded ${remoteTemplates.size} templates")
        val localTemplates = remoteTemplates.map { SyncConverters.fromFirestoreWorkoutTemplate(it) }

        // Use upsert to avoid constraint violations
        localTemplates.forEach { template ->
            database.workoutTemplateDao().upsertTemplate(template)
        }
    }

    private suspend fun downloadAndMergeTemplateExercises(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteExercises = firestoreRepository.downloadTemplateExercises(userId, lastSyncTime).getOrThrow()
        val localExercises = remoteExercises.map { SyncConverters.fromFirestoreTemplateExercise(it) }

        // Get all local template IDs to filter out orphaned template exercises
        val localTemplateIds =
            database
                .workoutTemplateDao()
                .getTemplates(userId)
                .map { it.id }
                .toSet()

        var insertedCount = 0
        var skippedCount = 0
        localExercises.forEach { exercise ->
            if (exercise.templateId !in localTemplateIds) {
                CloudLogger.warn(
                    "SyncManager",
                    "Skipping orphaned template exercise ${exercise.id} - template ${exercise.templateId} doesn't exist",
                )
                skippedCount++
                return@forEach
            }
            database.templateExerciseDao().upsertTemplateExercise(exercise)
            insertedCount++
        }
        if (skippedCount > 0) {
            CloudLogger.debug(
                "SyncManager",
                "downloadAndMergeTemplateExercises: Inserted $insertedCount, skipped $skippedCount orphaned",
            )
        }
    }

    private suspend fun downloadAndMergeTemplateSets(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteSets = firestoreRepository.downloadTemplateSets(userId, lastSyncTime).getOrThrow()
        val localSets = remoteSets.map { SyncConverters.fromFirestoreTemplateSet(it) }

        // Get all local template exercise IDs to filter out orphaned template sets
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val localTemplateExerciseIds = mutableSetOf<String>()
        templates.forEach { template ->
            val exercises = database.templateExerciseDao().getExercisesForTemplate(template.id)
            exercises.forEach { localTemplateExerciseIds.add(it.id) }
        }

        var insertedCount = 0
        var skippedCount = 0
        localSets.forEach { set ->
            if (set.templateExerciseId !in localTemplateExerciseIds) {
                CloudLogger.warn(
                    "SyncManager",
                    "Skipping orphaned template set ${set.id} - template exercise ${set.templateExerciseId} doesn't exist",
                )
                skippedCount++
                return@forEach
            }
            database.templateSetDao().upsertTemplateSet(set)
            insertedCount++
        }
        if (skippedCount > 0) {
            CloudLogger.debug(
                "SyncManager",
                "downloadAndMergeTemplateSets: Inserted $insertedCount, skipped $skippedCount orphaned",
            )
        }
    }

    private suspend fun downloadAndMergeProgrammes(userId: String) {
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Starting")
        val remoteProgrammes = firestoreRepository.downloadProgrammes(userId).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Downloaded ${remoteProgrammes.size} programmes")
        val localProgrammes = remoteProgrammes.map { SyncConverters.fromFirestoreProgramme(it) }

        localProgrammes.forEach { programme ->
            CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Processing programme ${programme.id} - ${programme.name}")
            val existing = database.programmeDao().getProgrammeById(programme.id)
            if (existing == null) {
                CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Inserting new programme ${programme.id}")
                database.programmeDao().insertProgramme(programme)
            } else {
                CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Programme ${programme.id} already exists, skipping")
            }
        }
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammes: Completed")
    }

    private suspend fun downloadAndMergeProgrammeWeeks(userId: String) {
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Starting")
        val remoteWeeks = firestoreRepository.downloadProgrammeWeeks(userId).getOrThrow()
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Downloaded ${remoteWeeks.size} weeks")
        val localWeeks = remoteWeeks.map { SyncConverters.fromFirestoreProgrammeWeek(it) }

        localWeeks.forEach { week ->
            CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Processing week ${week.id} for programme ${week.programmeId}")
            val existing = database.programmeDao().getProgrammeWeekById(week.id)
            if (existing == null) {
                CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Inserting new week ${week.id}")
                database.programmeDao().insertProgrammeWeek(week)
            } else {
                CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Week ${week.id} already exists, skipping")
            }
        }
        CloudLogger.debug("SyncManager", "downloadAndMergeProgrammeWeeks: Completed")
    }

    private suspend fun downloadAndMergeProgrammeWorkouts(userId: String) {
        val remoteWorkouts = firestoreRepository.downloadProgrammeWorkouts(userId).getOrThrow()
        val localWorkouts = remoteWorkouts.map { SyncConverters.fromFirestoreProgrammeWorkout(it) }

        localWorkouts.forEach { workout ->
            val existing = database.programmeDao().getProgrammeWorkoutById(workout.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeWorkout(workout)
            }
        }
    }

    private suspend fun downloadAndMergeProgrammeProgress(userId: String) {
        val remoteProgress = firestoreRepository.downloadProgrammeProgress(userId).getOrThrow()
        val localProgress = remoteProgress.map { SyncConverters.fromFirestoreProgrammeProgress(it) }

        localProgress.forEach { progress ->
            val existing = database.programmeDao().getProgrammeProgressById(progress.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeProgress(progress)
            } else {
                // Update progress if remote is further along
                if (progress.currentWeek > existing.currentWeek ||
                    (progress.currentWeek == existing.currentWeek && progress.currentDay > existing.currentDay)
                ) {
                    database.programmeDao().updateProgrammeProgress(progress)
                }
            }
        }
    }

    private suspend fun downloadAndMergeUserExerciseMaxes(userId: String) {
        val remoteMaxes = firestoreRepository.downloadUserExerciseMaxes(userId).getOrThrow()
        val localMaxes = remoteMaxes.map { SyncConverters.fromFirestoreUserExerciseMax(it) }

        localMaxes.forEach { max ->
            val existing = database.exerciseMaxTrackingDao().getById(max.id)
            if (existing == null) {
                database.exerciseMaxTrackingDao().insert(max)
            } else {
                // Keep the higher max value
                if (max.oneRMEstimate > existing.oneRMEstimate) {
                    database.exerciseMaxTrackingDao().update(max)
                }
            }
        }
    }

    private suspend fun downloadAndMergePersonalRecords(userId: String) {
        val remoteRecords = firestoreRepository.downloadPersonalRecords(userId).getOrThrow()
        val localRecords = remoteRecords.map { SyncConverters.fromFirestorePersonalRecord(it) }

        localRecords.forEach { record ->
            val existing = database.personalRecordDao().getPersonalRecordById(record.id)
            if (existing == null) {
                database.personalRecordDao().insertPersonalRecord(record)
            } else {
                // Keep the better record (higher weight)
                val shouldUpdate =
                    when (record.recordType) {
                        PRType.WEIGHT -> record.weight > existing.weight
                        PRType.ESTIMATED_1RM -> (record.estimated1RM ?: 0f) > (existing.estimated1RM ?: 0f)
                    }
                if (shouldUpdate) {
                    database.personalRecordDao().updatePersonalRecord(record)
                }
            }
        }
    }

    private suspend fun downloadAndMergeUserExerciseUsages(userId: String) {
        val remoteUsages = firestoreRepository.downloadUserExerciseUsages(userId).getOrThrow()
        val localUsages = remoteUsages.map { SyncConverters.fromFirestoreExerciseUsage(it) }

        localUsages.forEach { remote ->
            val existing = database.userExerciseUsageDao().getUsage(userId, remote.exerciseId)
            if (existing == null) {
                database.userExerciseUsageDao().insertUsage(remote)
            } else {
                // Merge logic: take max usage count and most recent lastUsedAt
                val mergedUsage =
                    existing.copy(
                        usageCount = maxOf(existing.usageCount, remote.usageCount),
                        lastUsedAt =
                            when {
                                existing.lastUsedAt == null -> remote.lastUsedAt
                                remote.lastUsedAt == null -> existing.lastUsedAt
                                remote.lastUsedAt.isAfter(existing.lastUsedAt) -> remote.lastUsedAt
                                else -> existing.lastUsedAt
                            },
                        personalNotes = remote.personalNotes ?: existing.personalNotes,
                        updatedAt = java.time.LocalDateTime.now(),
                    )
                database.userExerciseUsageDao().updateUsage(mergedUsage)
            }
        }
    }

    private suspend fun downloadAndMergeExerciseSwapHistory(userId: String) {
        val remoteSwaps = firestoreRepository.downloadExerciseSwapHistory(userId).getOrThrow()
        val localSwaps = remoteSwaps.map { SyncConverters.fromFirestoreExerciseSwapHistory(it) }

        localSwaps.forEach { swap ->
            // Check for existing swap by logical identity, not just ID
            val existing =
                database.exerciseSwapHistoryDao().getExistingSwap(
                    userId = swap.userId,
                    originalExerciseId = swap.originalExerciseId,
                    swappedToExerciseId = swap.swappedToExerciseId,
                    workoutId = swap.workoutId,
                )

            if (existing == null) {
                // No duplicate found, insert the swap
                database.exerciseSwapHistoryDao().insertSwapHistory(swap)
            } else {
                // Duplicate found, use upsert to update if needed
                database.exerciseSwapHistoryDao().upsertSwapHistory(swap)
            }
        }
    }

    private suspend fun downloadAndMergeExercisePerformanceTracking(userId: String) {
        val remoteTracking = firestoreRepository.downloadExercisePerformanceTracking(userId).getOrThrow()
        val localTracking = remoteTracking.map { SyncConverters.fromFirestoreProgrammeExerciseTracking(it) }

        localTracking.forEach { tracking ->
            val existing = database.programmeExerciseTrackingDao().getTrackingById(tracking.id)
            if (existing == null) {
                database.programmeExerciseTrackingDao().insertTracking(tracking)
            }
        }
    }

    private suspend fun downloadAndMergeGlobalExerciseProgress(userId: String) {
        val remoteProgress = firestoreRepository.downloadGlobalExerciseProgress(userId).getOrThrow()
        val localProgress = remoteProgress.map { SyncConverters.fromFirestoreGlobalExerciseProgress(it) }

        localProgress.forEach { progress ->
            val existing = database.globalExerciseProgressDao().getProgressById(progress.id)
            if (existing == null) {
                database.globalExerciseProgressDao().insertProgress(progress)
            }
        }
    }

    private suspend fun downloadAndMergeTrainingAnalyses(userId: String) {
        val remoteAnalyses = firestoreRepository.downloadTrainingAnalyses(userId).getOrThrow()
        val localAnalyses = remoteAnalyses.map { SyncConverters.fromFirestoreTrainingAnalysis(it) }

        localAnalyses.forEach { analysis ->
            val existing = database.trainingAnalysisDao().getAnalysisById(analysis.id)
            if (existing == null) {
                database.trainingAnalysisDao().insertAnalysis(analysis)
            }
        }
    }

    private suspend fun downloadAndMergeParseRequests(userId: String) {
        val remoteRequests = firestoreRepository.downloadParseRequests(userId).getOrThrow()
        val localRequests = remoteRequests.map { SyncConverters.fromFirestoreParseRequest(it) }

        localRequests.forEach { request ->
            val existing = database.parseRequestDao().getParseRequestById(request.id)
            if (existing == null) {
                database.parseRequestDao().insertParseRequest(request)
            }
        }
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
                            Result.success(SyncState.Error("System exercise sync failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "System exercise sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "System exercise sync failed", e)
                    Result.success(SyncState.Error("System exercise sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "System exercise sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "System exercise sync failed", e)
                    Result.success(SyncState.Error("System exercise sync failed: ${e.message}"))
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
                                    Result.success(SyncState.Error("Upload failed: ${error.message}"))
                                },
                            )
                        },
                        onFailure = { error ->
                            CloudLogger.error("SyncManager", "User data download failed: ${error.message}", error)
                            Result.success(SyncState.Error("Download failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    CloudLogger.error("SyncManager", "User data sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "User data sync failed", e)
                    Result.success(SyncState.Error("User data sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    CloudLogger.error("SyncManager", "User data sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "User data sync failed", e)
                    Result.success(SyncState.Error("User data sync failed: ${e.message}"))
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

            downloadAndMergeProgrammes(userId)
            downloadAndMergeProgrammeWeeks(userId)
            downloadAndMergeProgrammeWorkouts(userId)
            downloadAndMergeProgrammeProgress(userId)

            downloadAndMergeWorkouts(userId, lastSyncTime)
            downloadAndMergeExerciseLogs(userId, lastSyncTime)
            downloadAndMergeSetLogs(userId, lastSyncTime)

            downloadAndMergeWorkoutTemplates(userId, lastSyncTime)
            downloadAndMergeTemplateExercises(userId, lastSyncTime)
            downloadAndMergeTemplateSets(userId, lastSyncTime)

            downloadAndMergeUserExerciseMaxes(userId)
            downloadAndMergePersonalRecords(userId)
            downloadAndMergeUserExerciseUsages(userId)

            downloadAndMergeExerciseSwapHistory(userId)
            downloadAndMergeExercisePerformanceTracking(userId)
            downloadAndMergeGlobalExerciseProgress(userId)
            downloadAndMergeTrainingAnalyses(userId)
            downloadAndMergeParseRequests(userId)

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
            // Upload all user-specific data
            uploadWorkouts(userId)
            uploadExerciseLogs(userId)
            uploadSetLogs(userId)

            // Programme data
            uploadProgrammes(userId)
            uploadProgrammeWeeks(userId)
            uploadProgrammeWorkouts(userId)
            uploadProgrammeProgress(userId)

            // User stats
            uploadUserExerciseMaxes(userId)
            uploadPersonalRecords(userId)
            uploadUserExerciseUsages(userId)

            // Tracking data
            uploadExerciseSwapHistory(userId)
            uploadExercisePerformanceTracking(userId)
            uploadGlobalExerciseProgress(userId)
            uploadTrainingAnalyses(userId)
            uploadParseRequests(userId)

            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            CloudLogger.error("SyncManager", "uploadUserData failed - Firebase error: ${e.message}", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            CloudLogger.error("SyncManager", "uploadUserData failed - database error: ${e.message}", e)
            Result.failure(e)
        }
}
