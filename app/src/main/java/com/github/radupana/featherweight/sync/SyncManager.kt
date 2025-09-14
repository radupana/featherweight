package com.github.radupana.featherweight.sync

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.converters.SyncConverters
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class SyncState {
    data class Success(
        val timestamp: Timestamp,
    ) : SyncState()

    data class Error(
        val message: String,
    ) : SyncState()
}

class SyncManager(
    private val context: Context,
    private val database: FeatherweightDatabase,
    private val authManager: AuthenticationManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
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
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                val errorState = SyncState.Error("User not authenticated")
                return@withContext Result.success(errorState)
            }

            try {
                val lastSyncTime = getLastSyncTime(userId)

                // Step 1: Download remote changes
                downloadRemoteChanges(userId, lastSyncTime).fold(
                    onSuccess = {
                        // Step 2: Upload local changes
                        uploadLocalChanges(userId, lastSyncTime).fold(
                            onSuccess = {
                                updateSyncMetadata(userId)
                                Result.success(SyncState.Success(Timestamp.now()))
                            },
                            onFailure = { error ->
                                Result.success(SyncState.Error("Upload failed: ${error.message}"))
                            },
                        )
                    },
                    onFailure = { error ->
                        Result.success(SyncState.Error("Download failed: ${error.message}"))
                    },
                )
            } catch (e: Exception) {
                ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                Result.success(SyncState.Error("Sync failed: ${e.message}"))
            }
        }

    suspend fun restoreFromCloud(): Result<SyncState> =
        withContext(ioDispatcher) {
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                val errorState = SyncState.Error("User not authenticated")
                return@withContext Result.success(errorState)
            }

            try {
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
            } catch (e: Exception) {
                ExceptionLogger.logNonCritical("SyncManager", "Restore failed", e)
                Result.success(SyncState.Error("Restore failed: ${e.message}"))
            }
        }

    private suspend fun uploadLocalChanges(
        userId: String,
        @Suppress("UNUSED_PARAMETER")
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        try {
            uploadWorkouts(userId)
            uploadExerciseLogs(userId)
            uploadSetLogs(userId)

            uploadExerciseCores(userId)
            uploadExerciseVariations(userId)
            uploadVariationMuscles()
            uploadVariationInstructions()
            uploadVariationAliases()
            uploadVariationRelations()

            uploadProgrammes(userId)
            uploadProgrammeWeeks(userId)
            uploadProgrammeWorkouts(userId)
            uploadExerciseSubstitutions(userId)
            uploadProgrammeProgress(userId)

            uploadUserExerciseMaxes(userId)
            uploadOneRMHistory(userId)
            uploadPersonalRecords(userId)

            uploadExerciseSwapHistory(userId)
            uploadExercisePerformanceTracking(userId)
            uploadGlobalExerciseProgress(userId)
            uploadExerciseCorrelations()
            uploadTrainingAnalyses(userId)
            uploadParseRequests(userId)

            Result.success(Unit)
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("SyncManager", "Upload failed", e)
            Result.failure(e)
        }

    private suspend fun uploadWorkouts(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts()
        val userWorkouts = workouts.filter { it.userId == userId }
        val firestoreWorkouts = userWorkouts.map { SyncConverters.toFirestoreWorkout(it) }
        firestoreRepository.uploadWorkouts(userId, firestoreWorkouts).getOrThrow()
    }

    private suspend fun uploadExerciseLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts()
        val userWorkouts = workouts.filter { it.userId == userId }

        val exerciseLogs = mutableListOf<com.github.radupana.featherweight.data.ExerciseLog>()
        userWorkouts.forEach { workout ->
            val logs = database.exerciseLogDao().getExerciseLogsForWorkout(workout.id)
            exerciseLogs.addAll(logs)
        }

        val firestoreLogs = exerciseLogs.map { SyncConverters.toFirestoreExerciseLog(it) }
        firestoreRepository.uploadExerciseLogs(userId, firestoreLogs).getOrThrow()
    }

    private suspend fun uploadSetLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts()
        val userWorkouts = workouts.filter { it.userId == userId }

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

    private suspend fun getLastSyncTime(userId: String): Timestamp? =
        firestoreRepository.getSyncMetadata(userId).fold(
            onSuccess = { metadata -> metadata?.lastSyncTime },
            onFailure = { null },
        )

    private suspend fun updateSyncMetadata(userId: String) {
        firestoreRepository.updateSyncMetadata(userId, deviceId, deviceName).getOrThrow()
    }

    private suspend fun uploadExerciseCores(userId: String) {
        val cores = database.exerciseCoreDao().getAllCores()
        val firestoreCores = cores.map { SyncConverters.toFirestoreExerciseCore(it) }
        firestoreRepository.uploadExerciseCores(userId, firestoreCores).getOrThrow()
    }

    private suspend fun uploadExerciseVariations(userId: String) {
        val variations = database.exerciseVariationDao().getAllExerciseVariations()
        val firestoreVariations = variations.map { SyncConverters.toFirestoreExerciseVariation(it) }
        firestoreRepository.uploadExerciseVariations(userId, firestoreVariations).getOrThrow()
    }

    private suspend fun uploadVariationMuscles() {
        val muscles = database.variationMuscleDao().getAllVariationMuscles()
        val firestoreMuscles = muscles.map { SyncConverters.toFirestoreVariationMuscle(it) }
        firestoreRepository.uploadVariationMuscles(firestoreMuscles).getOrThrow()
    }

    private suspend fun uploadVariationInstructions() {
        val instructions = database.variationInstructionDao().getAllInstructions()
        val firestoreInstructions = instructions.map { SyncConverters.toFirestoreVariationInstruction(it) }
        firestoreRepository.uploadVariationInstructions(firestoreInstructions).getOrThrow()
    }

    private suspend fun uploadVariationAliases() {
        val aliases = database.variationAliasDao().getAllAliases()
        val firestoreAliases = aliases.map { SyncConverters.toFirestoreVariationAlias(it) }
        firestoreRepository.uploadVariationAliases(firestoreAliases).getOrThrow()
    }

    private suspend fun uploadVariationRelations() {
        val relations = database.variationRelationDao().getAllRelations()
        val firestoreRelations = relations.map { SyncConverters.toFirestoreVariationRelation(it) }
        firestoreRepository.uploadVariationRelations(firestoreRelations).getOrThrow()
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

    private suspend fun uploadExerciseSubstitutions(userId: String) {
        val substitutions = database.programmeDao().getAllSubstitutions()
        val userSubstitutions = substitutions.filter { it.userId == userId }
        val firestoreSubstitutions = userSubstitutions.map { SyncConverters.toFirestoreExerciseSubstitution(it) }
        firestoreRepository.uploadExerciseSubstitutions(userId, firestoreSubstitutions).getOrThrow()
    }

    private suspend fun uploadProgrammeProgress(userId: String) {
        val progress = database.programmeDao().getAllProgrammeProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreProgrammeProgress(it) }
        firestoreRepository.uploadProgrammeProgress(userId, firestoreProgress).getOrThrow()
    }

    private suspend fun uploadUserExerciseMaxes(userId: String) {
        val maxes = database.oneRMDao().getAllUserExerciseMaxes()
        val userMaxes = maxes.filter { it.userId == userId }
        val firestoreMaxes = userMaxes.map { SyncConverters.toFirestoreUserExerciseMax(it) }
        firestoreRepository.uploadUserExerciseMaxes(userId, firestoreMaxes).getOrThrow()
    }

    private suspend fun uploadOneRMHistory(userId: String) {
        val history = database.oneRMDao().getAllOneRMHistory()
        val userHistory = history.filter { it.userId == userId }
        val firestoreHistory = userHistory.map { SyncConverters.toFirestoreOneRMHistory(it) }
        firestoreRepository.uploadOneRMHistory(userId, firestoreHistory).getOrThrow()
    }

    private suspend fun uploadPersonalRecords(userId: String) {
        val records = database.personalRecordDao().getAllPersonalRecords()
        val userRecords = records.filter { it.userId == userId }
        val firestoreRecords = userRecords.map { SyncConverters.toFirestorePersonalRecord(it) }
        firestoreRepository.uploadPersonalRecords(userId, firestoreRecords).getOrThrow()
    }

    private suspend fun uploadExerciseSwapHistory(userId: String) {
        val swaps = database.exerciseSwapHistoryDao().getAllSwapHistory()
        val userSwaps = swaps.filter { it.userId == userId }
        val firestoreSwaps = userSwaps.map { SyncConverters.toFirestoreExerciseSwapHistory(it) }
        firestoreRepository.uploadExerciseSwapHistory(userId, firestoreSwaps).getOrThrow()
    }

    private suspend fun uploadExercisePerformanceTracking(userId: String) {
        val tracking = database.exercisePerformanceTrackingDao().getAllTracking()
        val userTracking = tracking.filter { it.userId == userId }
        val firestoreTracking = userTracking.map { SyncConverters.toFirestoreExercisePerformanceTracking(it) }
        firestoreRepository.uploadExercisePerformanceTracking(userId, firestoreTracking).getOrThrow()
    }

    private suspend fun uploadGlobalExerciseProgress(userId: String) {
        val progress = database.globalExerciseProgressDao().getAllProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreGlobalExerciseProgress(it) }
        firestoreRepository.uploadGlobalExerciseProgress(userId, firestoreProgress).getOrThrow()
    }

    private suspend fun uploadExerciseCorrelations() {
        val correlations = database.exerciseCorrelationDao().getAllCorrelations()
        val firestoreCorrelations = correlations.map { SyncConverters.toFirestoreExerciseCorrelation(it) }
        firestoreRepository.uploadExerciseCorrelations(firestoreCorrelations).getOrThrow()
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
            // Download workouts and merge
            downloadAndMergeWorkouts(userId, lastSyncTime)
            downloadAndMergeExerciseLogs(userId, lastSyncTime)
            downloadAndMergeSetLogs(userId, lastSyncTime)

            // Download exercise data (system-wide, not user-specific)
            downloadAndMergeExerciseCores(userId)
            downloadAndMergeExerciseVariations(userId)
            downloadAndMergeVariationMuscles()
            downloadAndMergeVariationInstructions()
            downloadAndMergeVariationAliases()
            downloadAndMergeVariationRelations()

            // Download programme data
            downloadAndMergeProgrammes(userId)
            downloadAndMergeProgrammeWeeks(userId)
            downloadAndMergeProgrammeWorkouts(userId)
            downloadAndMergeExerciseSubstitutions(userId)
            downloadAndMergeProgrammeProgress(userId)

            // Download user stats
            downloadAndMergeUserExerciseMaxes(userId)
            downloadAndMergeOneRMHistory(userId)
            downloadAndMergePersonalRecords(userId)

            // Download tracking data
            downloadAndMergeExerciseSwapHistory(userId)
            downloadAndMergeExercisePerformanceTracking(userId)
            downloadAndMergeGlobalExerciseProgress(userId)
            downloadAndMergeExerciseCorrelations()
            downloadAndMergeTrainingAnalyses(userId)
            downloadAndMergeParseRequests(userId)

            Result.success(Unit)
        } catch (e: Exception) {
            ExceptionLogger.logNonCritical("SyncManager", "Download failed", e)
            Result.failure(e)
        }

    private suspend fun downloadAndMergeWorkouts(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteWorkouts = firestoreRepository.downloadWorkouts(userId, lastSyncTime).getOrThrow()
        val localWorkouts = remoteWorkouts.map { SyncConverters.fromFirestoreWorkout(it) }

        localWorkouts.forEach { workout ->
            val existing = database.workoutDao().getWorkoutById(workout.id)
            if (existing == null) {
                database.workoutDao().insertWorkout(workout)
            } else {
                // Conflict resolution: last-write-wins based on date
                if (workout.date.isAfter(existing.date)) {
                    database.workoutDao().updateWorkout(workout)
                }
            }
        }
    }

    private suspend fun downloadAndMergeExerciseLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteLogs = firestoreRepository.downloadExerciseLogs(userId, lastSyncTime).getOrThrow()
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreExerciseLog(it) }

        localLogs.forEach { log ->
            val existing = database.exerciseLogDao().getExerciseLogById(log.id)
            if (existing == null) {
                database.exerciseLogDao().insertExerciseLog(log)
            }
            // No update for exercise logs - they're immutable once created
        }
    }

    private suspend fun downloadAndMergeSetLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteLogs = firestoreRepository.downloadSetLogs(userId, lastSyncTime).getOrThrow()
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreSetLog(it) }

        localLogs.forEach { log ->
            val existing = database.setLogDao().getSetLogById(log.id)
            if (existing == null) {
                database.setLogDao().insertSetLog(log)
            } else if (log.isCompleted && !existing.isCompleted) {
                // Update if remote is completed but local is not
                database.setLogDao().updateSetLog(log)
            }
        }
    }

    private suspend fun downloadAndMergeExerciseCores(userId: String) {
        val remoteCores = firestoreRepository.downloadExerciseCores(userId).getOrThrow()
        val localCores = remoteCores.map { SyncConverters.fromFirestoreExerciseCore(it) }

        localCores.forEach { core ->
            val existing = database.exerciseCoreDao().getCoreById(core.id)
            if (existing == null) {
                database.exerciseCoreDao().insertCore(core)
            }
        }
    }

    private suspend fun downloadAndMergeExerciseVariations(userId: String) {
        val remoteVariations = firestoreRepository.downloadExerciseVariations(userId).getOrThrow()
        val localVariations = remoteVariations.map { SyncConverters.fromFirestoreExerciseVariation(it) }

        localVariations.forEach { variation ->
            val existing = database.exerciseVariationDao().getExerciseVariationById(variation.id)
            if (existing == null) {
                database.exerciseVariationDao().insertExerciseVariation(variation)
            }
        }
    }

    private suspend fun downloadAndMergeVariationMuscles() {
        val remoteMuscles = firestoreRepository.downloadVariationMuscles().getOrThrow()
        val localMuscles = remoteMuscles.map { SyncConverters.fromFirestoreVariationMuscle(it) }

        localMuscles.forEach { muscle ->
            // VariationMuscle uses composite key, just insert with REPLACE strategy
            database.variationMuscleDao().insertVariationMuscle(muscle)
        }
    }

    private suspend fun downloadAndMergeVariationInstructions() {
        val remoteInstructions = firestoreRepository.downloadVariationInstructions().getOrThrow()
        val localInstructions = remoteInstructions.map { SyncConverters.fromFirestoreVariationInstruction(it) }

        localInstructions.forEach { instruction ->
            val existing = database.variationInstructionDao().getInstructionById(instruction.id)
            if (existing == null) {
                database.variationInstructionDao().insertInstruction(instruction)
            }
        }
    }

    private suspend fun downloadAndMergeVariationAliases() {
        val remoteAliases = firestoreRepository.downloadVariationAliases().getOrThrow()
        val localAliases = remoteAliases.map { SyncConverters.fromFirestoreVariationAlias(it) }

        localAliases.forEach { alias ->
            val existing = database.variationAliasDao().getAliasById(alias.id)
            if (existing == null) {
                database.variationAliasDao().insertAlias(alias)
            }
        }
    }

    private suspend fun downloadAndMergeVariationRelations() {
        val remoteRelations = firestoreRepository.downloadVariationRelations().getOrThrow()
        val localRelations = remoteRelations.map { SyncConverters.fromFirestoreVariationRelation(it) }

        localRelations.forEach { relation ->
            val existing = database.variationRelationDao().getRelationById(relation.id)
            if (existing == null) {
                database.variationRelationDao().insertRelation(relation)
            }
        }
    }

    private suspend fun downloadAndMergeProgrammes(userId: String) {
        val remoteProgrammes = firestoreRepository.downloadProgrammes(userId).getOrThrow()
        val localProgrammes = remoteProgrammes.map { SyncConverters.fromFirestoreProgramme(it) }

        localProgrammes.forEach { programme ->
            val existing = database.programmeDao().getProgrammeById(programme.id)
            if (existing == null) {
                database.programmeDao().insertProgramme(programme)
            }
        }
    }

    private suspend fun downloadAndMergeProgrammeWeeks(userId: String) {
        val remoteWeeks = firestoreRepository.downloadProgrammeWeeks(userId).getOrThrow()
        val localWeeks = remoteWeeks.map { SyncConverters.fromFirestoreProgrammeWeek(it) }

        localWeeks.forEach { week ->
            val existing = database.programmeDao().getProgrammeWeekById(week.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeWeek(week)
            }
        }
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

    private suspend fun downloadAndMergeExerciseSubstitutions(userId: String) {
        val remoteSubstitutions = firestoreRepository.downloadExerciseSubstitutions(userId).getOrThrow()
        val localSubstitutions = remoteSubstitutions.map { SyncConverters.fromFirestoreExerciseSubstitution(it) }

        localSubstitutions.forEach { substitution ->
            val existing = database.programmeDao().getSubstitutionById(substitution.id)
            if (existing == null) {
                database.programmeDao().insertSubstitution(substitution)
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
            val existing = database.oneRMDao().getUserExerciseMaxById(max.id)
            if (existing == null) {
                database.oneRMDao().insertUserExerciseMax(max)
            } else {
                // Keep the higher max value
                if (max.mostWeightLifted > existing.mostWeightLifted) {
                    database.oneRMDao().updateUserExerciseMax(max)
                }
            }
        }
    }

    private suspend fun downloadAndMergeOneRMHistory(userId: String) {
        val remoteHistory = firestoreRepository.downloadOneRMHistory(userId).getOrThrow()
        val localHistory = remoteHistory.map { SyncConverters.fromFirestoreOneRMHistory(it) }

        localHistory.forEach { history ->
            val existing = database.oneRMDao().getOneRMHistoryById(history.id)
            if (existing == null) {
                database.oneRMDao().insertOneRMHistory(history)
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

    private suspend fun downloadAndMergeExerciseSwapHistory(userId: String) {
        val remoteSwaps = firestoreRepository.downloadExerciseSwapHistory(userId).getOrThrow()
        val localSwaps = remoteSwaps.map { SyncConverters.fromFirestoreExerciseSwapHistory(it) }

        localSwaps.forEach { swap ->
            val existing = database.exerciseSwapHistoryDao().getSwapHistoryById(swap.id)
            if (existing == null) {
                database.exerciseSwapHistoryDao().insertSwapHistory(swap)
            }
        }
    }

    private suspend fun downloadAndMergeExercisePerformanceTracking(userId: String) {
        val remoteTracking = firestoreRepository.downloadExercisePerformanceTracking(userId).getOrThrow()
        val localTracking = remoteTracking.map { SyncConverters.fromFirestoreExercisePerformanceTracking(it) }

        localTracking.forEach { tracking ->
            val existing = database.exercisePerformanceTrackingDao().getTrackingById(tracking.id)
            if (existing == null) {
                database.exercisePerformanceTrackingDao().insertTracking(tracking)
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

    private suspend fun downloadAndMergeExerciseCorrelations() {
        val remoteCorrelations = firestoreRepository.downloadExerciseCorrelations().getOrThrow()
        val localCorrelations = remoteCorrelations.map { SyncConverters.fromFirestoreExerciseCorrelation(it) }

        localCorrelations.forEach { correlation ->
            val existing = database.exerciseCorrelationDao().getCorrelationById(correlation.id)
            if (existing == null) {
                database.exerciseCorrelationDao().insertCorrelation(correlation)
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
}
