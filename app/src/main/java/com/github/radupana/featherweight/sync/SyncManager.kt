package com.github.radupana.featherweight.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.converters.SyncConverters
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.sync.strategies.CustomExerciseSyncStrategy
import com.github.radupana.featherweight.sync.strategies.SystemExerciseSyncStrategy
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

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
    private val useTestDatabase: Boolean = false,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(useTestDatabase),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Mutex to prevent concurrent sync operations
    private val syncMutex = Mutex()

    // Track last sync time to prevent rapid successive syncs
    private var lastSyncAttemptTime = 0L
    private val minSyncIntervalMs = 10000L // 10 seconds minimum between syncs

    // Sync strategies for modular sync logic
    private val systemExerciseStrategy = SystemExerciseSyncStrategy(database, firestoreRepository)
    private val customExerciseStrategy = CustomExerciseSyncStrategy(database, firestoreRepository)
    private val deviceId: String by lazy {
        context
            .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            .getString("device_id", null) ?: generateAndStoreDeviceId()
    }

    private val deviceName: String by lazy {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun generateAndStoreDeviceId(): String {
        val id = UUID.randomUUID().toString()
        context
            .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            .edit {
                putString("device_id", id)
            }
        return id
    }

    suspend fun syncAll(): Result<SyncState> =
        withContext(ioDispatcher) {
            // Use mutex to prevent concurrent sync operations
            syncMutex.withLock {
                // Debounce rapid sync calls
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSyncAttemptTime < minSyncIntervalMs) {
                    Log.i("SyncManager", "Sync skipped - too soon after last sync (${currentTime - lastSyncAttemptTime}ms < ${minSyncIntervalMs}ms)")
                    return@withContext Result.success(SyncState.Skipped("Sync throttled"))
                }
                lastSyncAttemptTime = currentTime

                val userId = authManager.getCurrentUserId()
                Log.i("SyncManager", "syncAll started for userId: $userId")
                if (userId == null) {
                    Log.e("SyncManager", "User not authenticated")
                    val errorState = SyncState.Error("User not authenticated")
                    return@withContext Result.success(errorState)
                }

                try {
                    // Check if database is empty (fresh install scenario)
                    val isEmptyDatabase = isDatabaseEmpty(userId)
                    Log.i("SyncManager", "Database empty check: $isEmptyDatabase")

                    Log.i("SyncManager", "Getting last sync time...")
                    var lastSyncTime = getLastSyncTime(userId)

                    // If database is empty but sync metadata exists, force full sync
                    if (isEmptyDatabase && lastSyncTime != null) {
                        Log.i("SyncManager", "Empty database detected with existing sync metadata - forcing full restore")
                        lastSyncTime = null
                    }

                    Log.i("SyncManager", "Last sync time: $lastSyncTime")

                    // Step 1: Download remote changes
                    Log.i("SyncManager", "Starting download of remote changes...")
                    downloadRemoteChanges(userId, lastSyncTime).fold(
                        onSuccess = {
                            Log.i("SyncManager", "Download successful, starting upload...")
                            // Step 2: Upload local changes
                            uploadLocalChanges(userId, lastSyncTime).fold(
                                onSuccess = {
                                    Log.i("SyncManager", "Upload successful, updating metadata...")
                                    updateSyncMetadata(userId)
                                    Log.i("SyncManager", "Sync completed successfully")
                                    Result.success(SyncState.Success(Timestamp.now()))
                                },
                                onFailure = { error ->
                                    Log.e("SyncManager", "Upload failed: ${error.message}", error)
                                    Result.success(SyncState.Error("Upload failed: ${error.message}"))
                                },
                            )
                        },
                        onFailure = { error ->
                            Log.e("SyncManager", "Download failed: ${error.message}", error)
                            Result.success(SyncState.Error("Download failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    Log.e("SyncManager", "Sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.success(SyncState.Error("Sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    Log.e("SyncManager", "Sync failed with database exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                    Result.success(SyncState.Error("Sync failed: ${e.message}"))
                }
            } // End of mutex lock
        }

    suspend fun restoreFromCloud(): Result<SyncState> =
        withContext(ioDispatcher) {
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                val errorState = SyncState.Error("User not authenticated")
                return@withContext Result.success(errorState)
            }

            try {
                // Clear all user data before restoring to avoid ID mismatches
                Log.d("SyncManager", "Clearing all user data before restore...")
                clearAllUserData(userId)

                // Download all data without lastSyncTime filter
                downloadRemoteChanges(userId, null).fold(
                    onSuccess = {
                        updateSyncMetadata(userId)
                        Result.success(SyncState.Success(Timestamp.now()))
                    },
                    onFailure = { error ->
                        Result.success(SyncState.Error("Restore failed: ${error.message}"))
                    },
                )
            } catch (e: com.google.firebase.FirebaseException) {
                ExceptionLogger.logNonCritical("SyncManager", "Restore failed - Firebase error", e)
                Result.success(SyncState.Error("Restore failed: ${e.message}"))
            } catch (e: android.database.sqlite.SQLiteException) {
                ExceptionLogger.logNonCritical("SyncManager", "Restore failed - database error", e)
                Result.success(SyncState.Error("Restore failed: ${e.message}"))
            }
        }

    private suspend fun clearAllUserData(userId: String) {
        // Clear all user-specific data
        database.workoutDao().deleteAllForUser(userId)
        database.exerciseLogDao().deleteAllForUser(userId)
        database.setLogDao().deleteAllForUser(userId)
        database.programmeDao().deleteAllProgrammesForUser(userId)
        database.programmeDao().deleteAllProgrammeWeeksForUser(userId)
        database.programmeDao().deleteAllProgrammeWorkoutsForUser(userId)
        database.programmeDao().deleteAllProgrammeProgressForUser(userId)
        database.exerciseMaxTrackingDao().deleteAllForUser(userId)
        database.personalRecordDao().deleteAllForUser(userId)
        database.exerciseSwapHistoryDao().deleteAllForUser(userId)
        database.programmeExerciseTrackingDao().deleteAllForUser(userId)
        database.globalExerciseProgressDao().deleteAllForUser(userId)
        database.trainingAnalysisDao().deleteAllByUserId(userId)
        database.parseRequestDao().deleteAllForUser(userId)
        // Clear custom exercises
        database.exerciseDao().deleteAllCustomExercisesByUser(userId)
    }

    private suspend fun uploadLocalChanges(
        userId: String,
        @Suppress("UNUSED_PARAMETER")
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            Log.d("SyncManager", "uploadLocalChanges: Starting upload for user $userId")

            Log.d("SyncManager", "Uploading workouts...")
            uploadWorkouts(userId)
            Log.d("SyncManager", "Uploading exercise logs...")
            uploadExerciseLogs(userId)
            Log.d("SyncManager", "Uploading set logs...")
            uploadSetLogs(userId)

            Log.d("SyncManager", "Uploading workout templates...")
            uploadWorkoutTemplates(userId)
            Log.d("SyncManager", "Uploading template exercises...")
            uploadTemplateExercises(userId)
            Log.d("SyncManager", "Uploading template sets...")
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
        Log.d("SyncManager", "isDatabaseEmpty: userWorkouts=$userWorkoutCount, isEmpty=$isEmpty")

        return isEmpty
    }

    private suspend fun getLastSyncTime(userId: String): Timestamp? =
        firestoreRepository.getSyncMetadata(userId).fold(
            onSuccess = { metadata -> metadata?.lastSyncTime },
            onFailure = { null },
        )

    private suspend fun updateSyncMetadata(userId: String) {
        firestoreRepository.updateSyncMetadata(userId, deviceId, deviceName).getOrThrow()
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
            Log.d("SyncManager", "downloadRemoteChanges: Starting download for user $userId")

            // CRITICAL: Download exercises FIRST before any entities that reference them
            // Download system exercises using new denormalized strategy
            Log.d("SyncManager", "Downloading system exercises...")
            systemExerciseStrategy.downloadAndMerge(null, lastSyncTime).getOrThrow()

            // Download custom exercises for this user
            Log.d("SyncManager", "Downloading custom exercises...")
            customExerciseStrategy.downloadAndMerge(userId, lastSyncTime).getOrThrow()

            // Download programme data FIRST (workouts have FK to programmes)
            Log.d("SyncManager", "Downloading programmes...")
            downloadAndMergeProgrammes(userId)
            Log.d("SyncManager", "Downloading programme weeks...")
            downloadAndMergeProgrammeWeeks(userId)
            Log.d("SyncManager", "Downloading programme workouts...")
            downloadAndMergeProgrammeWorkouts(userId)
            Log.d("SyncManager", "Downloading programme progress...")
            downloadAndMergeProgrammeProgress(userId)

            // NOW download entities that reference exercises and programmes
            Log.d("SyncManager", "Downloading workouts...")
            downloadAndMergeWorkouts(userId, lastSyncTime)
            Log.d("SyncManager", "Downloading exercise logs...")
            downloadAndMergeExerciseLogs(userId, lastSyncTime)
            Log.d("SyncManager", "Downloading set logs...")
            downloadAndMergeSetLogs(userId, lastSyncTime)

            // Download templates
            Log.d("SyncManager", "Downloading workout templates...")
            downloadAndMergeWorkoutTemplates(userId, lastSyncTime)
            Log.d("SyncManager", "Downloading template exercises...")
            downloadAndMergeTemplateExercises(userId, lastSyncTime)
            Log.d("SyncManager", "Downloading template sets...")
            downloadAndMergeTemplateSets(userId, lastSyncTime)

            // Download user stats
            Log.d("SyncManager", "Downloading user exercise maxes...")
            downloadAndMergeUserExerciseMaxes(userId)
            // Log.d("SyncManager", "Downloading OneRM history...")
            Log.d("SyncManager", "Downloading personal records...")
            downloadAndMergePersonalRecords(userId)
            Log.d("SyncManager", "Downloading user exercise usages...")
            downloadAndMergeUserExerciseUsages(userId)

            // Download tracking data
            Log.d("SyncManager", "Starting download of tracking data...")
            Log.d("SyncManager", "Downloading exercise swap history...")
            downloadAndMergeExerciseSwapHistory(userId)
            Log.d("SyncManager", "Downloading exercise performance tracking...")
            downloadAndMergeExercisePerformanceTracking(userId)
            Log.d("SyncManager", "Downloading global exercise progress...")
            downloadAndMergeGlobalExerciseProgress(userId)
            Log.d("SyncManager", "Downloading training analyses...")
            downloadAndMergeTrainingAnalyses(userId)
            Log.d("SyncManager", "Downloading parse requests...")
            downloadAndMergeParseRequests(userId)

            Log.d("SyncManager", "downloadRemoteChanges: All downloads completed successfully")
            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e("SyncManager", "downloadRemoteChanges failed - Firebase error: ${e.message}", e)
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("SyncManager", "downloadRemoteChanges failed - database error: ${e.message}", e)
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        }

    private suspend fun downloadAndMergeWorkouts(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        Log.d("SyncManager", "downloadAndMergeWorkouts: Starting for user $userId")
        val remoteWorkouts = firestoreRepository.downloadWorkouts(userId, lastSyncTime).getOrThrow()
        Log.d("SyncManager", "downloadAndMergeWorkouts: Downloaded ${remoteWorkouts.size} workouts")
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
        val remoteLogs = firestoreRepository.downloadExerciseLogs(userId, lastSyncTime).getOrThrow()
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreExerciseLog(it) }

        // Use upsert to avoid constraint violations
        localLogs.forEach { log ->
            database.exerciseLogDao().upsertExerciseLog(log)
        }
    }

    private suspend fun downloadAndMergeSetLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteLogs = firestoreRepository.downloadSetLogs(userId, lastSyncTime).getOrThrow()
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreSetLog(it) }

        // Use upsert to avoid constraint violations
        localLogs.forEach { log ->
            database.setLogDao().upsertSetLog(log)
        }
    }

    private suspend fun downloadAndMergeWorkoutTemplates(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        Log.d("SyncManager", "downloadAndMergeWorkoutTemplates: Starting for user $userId")
        val remoteTemplates = firestoreRepository.downloadWorkoutTemplates(userId, lastSyncTime).getOrThrow()
        Log.d("SyncManager", "downloadAndMergeWorkoutTemplates: Downloaded ${remoteTemplates.size} templates")
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

        // Use upsert to avoid constraint violations
        localExercises.forEach { exercise ->
            database.templateExerciseDao().upsertTemplateExercise(exercise)
        }
    }

    private suspend fun downloadAndMergeTemplateSets(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteSets = firestoreRepository.downloadTemplateSets(userId, lastSyncTime).getOrThrow()
        val localSets = remoteSets.map { SyncConverters.fromFirestoreTemplateSet(it) }

        // Use upsert to avoid constraint violations
        localSets.forEach { set ->
            database.templateSetDao().upsertTemplateSet(set)
        }
    }

    private suspend fun downloadAndMergeProgrammes(userId: String) {
        Log.d("SyncManager", "downloadAndMergeProgrammes: Starting")
        val remoteProgrammes = firestoreRepository.downloadProgrammes(userId).getOrThrow()
        Log.d("SyncManager", "downloadAndMergeProgrammes: Downloaded ${remoteProgrammes.size} programmes")
        val localProgrammes = remoteProgrammes.map { SyncConverters.fromFirestoreProgramme(it) }

        localProgrammes.forEach { programme ->
            Log.d("SyncManager", "downloadAndMergeProgrammes: Processing programme ${programme.id} - ${programme.name}")
            val existing = database.programmeDao().getProgrammeById(programme.id)
            if (existing == null) {
                Log.d("SyncManager", "downloadAndMergeProgrammes: Inserting new programme ${programme.id}")
                database.programmeDao().insertProgramme(programme)
            } else {
                Log.d("SyncManager", "downloadAndMergeProgrammes: Programme ${programme.id} already exists, skipping")
            }
        }
        Log.d("SyncManager", "downloadAndMergeProgrammes: Completed")
    }

    private suspend fun downloadAndMergeProgrammeWeeks(userId: String) {
        Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Starting")
        val remoteWeeks = firestoreRepository.downloadProgrammeWeeks(userId).getOrThrow()
        Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Downloaded ${remoteWeeks.size} weeks")
        val localWeeks = remoteWeeks.map { SyncConverters.fromFirestoreProgrammeWeek(it) }

        localWeeks.forEach { week ->
            Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Processing week ${week.id} for programme ${week.programmeId}")
            val existing = database.programmeDao().getProgrammeWeekById(week.id)
            if (existing == null) {
                Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Inserting new week ${week.id}")
                database.programmeDao().insertProgrammeWeek(week)
            } else {
                Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Week ${week.id} already exists, skipping")
            }
        }
        Log.d("SyncManager", "downloadAndMergeProgrammeWeeks: Completed")
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
                Log.i("SyncManager", "Starting system exercise sync")
                try {
                    // System exercises don't require authentication
                    systemExerciseStrategy.downloadAndMerge(null, null).fold(
                        onSuccess = {
                            Log.i("SyncManager", "System exercise sync completed successfully")
                            Result.success(SyncState.Success(Timestamp.now()))
                        },
                        onFailure = { error ->
                            Log.e("SyncManager", "System exercise sync failed: ${error.message}", error)
                            Result.success(SyncState.Error("System exercise sync failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    Log.e("SyncManager", "System exercise sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "System exercise sync failed", e)
                    Result.success(SyncState.Error("System exercise sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    Log.e("SyncManager", "System exercise sync failed with database exception: ${e.message}", e)
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
                Log.i("SyncManager", "Starting user data sync for userId: $userId")

                try {
                    val lastSyncTime = getLastSyncTime(userId)

                    // Download user data (excluding system exercises)
                    downloadUserData(userId, lastSyncTime).fold(
                        onSuccess = {
                            // Upload user data changes
                            uploadUserData(userId).fold(
                                onSuccess = {
                                    updateSyncMetadata(userId)
                                    Log.i("SyncManager", "User data sync completed successfully")
                                    Result.success(SyncState.Success(Timestamp.now()))
                                },
                                onFailure = { error ->
                                    Log.e("SyncManager", "User data upload failed: ${error.message}", error)
                                    Result.success(SyncState.Error("Upload failed: ${error.message}"))
                                },
                            )
                        },
                        onFailure = { error ->
                            Log.e("SyncManager", "User data download failed: ${error.message}", error)
                            Result.success(SyncState.Error("Download failed: ${error.message}"))
                        },
                    )
                } catch (e: com.google.firebase.FirebaseException) {
                    Log.e("SyncManager", "User data sync failed with Firebase exception: ${e.message}", e)
                    ExceptionLogger.logNonCritical("SyncManager", "User data sync failed", e)
                    Result.success(SyncState.Error("User data sync failed: ${e.message}"))
                } catch (e: android.database.sqlite.SQLiteException) {
                    Log.e("SyncManager", "User data sync failed with database exception: ${e.message}", e)
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
            // CRITICAL: Download custom exercises FIRST before entities that reference them
            Log.d("SyncManager", "Downloading custom exercises for user...")
            customExerciseStrategy.downloadAndMerge(userId, lastSyncTime).getOrThrow()

            // NOW download entities that reference exercises
            downloadAndMergeWorkouts(userId, lastSyncTime)
            downloadAndMergeExerciseLogs(userId, lastSyncTime)
            downloadAndMergeSetLogs(userId, lastSyncTime)

            // Programme data
            downloadAndMergeProgrammes(userId)
            downloadAndMergeProgrammeWeeks(userId)
            downloadAndMergeProgrammeWorkouts(userId)
            downloadAndMergeProgrammeProgress(userId)

            // User stats
            downloadAndMergeUserExerciseMaxes(userId)
            downloadAndMergePersonalRecords(userId)
            downloadAndMergeUserExerciseUsages(userId)

            // Tracking data
            downloadAndMergeExerciseSwapHistory(userId)
            downloadAndMergeExercisePerformanceTracking(userId)
            downloadAndMergeGlobalExerciseProgress(userId)
            downloadAndMergeTrainingAnalyses(userId)
            downloadAndMergeParseRequests(userId)

            Result.success(Unit)
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e("SyncManager", "downloadUserData failed - Firebase error: ${e.message}", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("SyncManager", "downloadUserData failed - database error: ${e.message}", e)
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
            Log.e("SyncManager", "uploadUserData failed - Firebase error: ${e.message}", e)
            Result.failure(e)
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e("SyncManager", "uploadUserData failed - database error: ${e.message}", e)
            Result.failure(e)
        }
}
