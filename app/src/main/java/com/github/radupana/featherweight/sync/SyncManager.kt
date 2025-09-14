package com.github.radupana.featherweight.sync

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.converters.SyncConverters
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()

    object Syncing : SyncState()

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
        withContext(Dispatchers.IO) {
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                val errorState = SyncState.Error("User not authenticated")
                return@withContext Result.success(errorState)
            }

            try {
                val lastSyncTime = getLastSyncTime(userId)

                uploadLocalChanges(userId, lastSyncTime).fold(
                    onSuccess = {
                        updateSyncMetadata(userId)
                        Result.success(SyncState.Success(Timestamp.now()))
                    },
                    onFailure = { error ->
                        Result.success(SyncState.Error("Upload failed: ${error.message}"))
                    },
                )
            } catch (e: Exception) {
                ExceptionLogger.logNonCritical("SyncManager", "Sync failed", e)
                Result.success(SyncState.Error("Sync failed: ${e.message}"))
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
}
