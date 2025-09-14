package com.github.radupana.featherweight.sync.repository

import com.github.radupana.featherweight.sync.models.FirestoreExerciseCore
import com.github.radupana.featherweight.sync.models.FirestoreExerciseCorrelation
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreExercisePerformanceTracking
import com.github.radupana.featherweight.sync.models.FirestoreExerciseSubstitution
import com.github.radupana.featherweight.sync.models.FirestoreExerciseSwapHistory
import com.github.radupana.featherweight.sync.models.FirestoreExerciseVariation
import com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress
import com.github.radupana.featherweight.sync.models.FirestoreOneRMHistory
import com.github.radupana.featherweight.sync.models.FirestoreParseRequest
import com.github.radupana.featherweight.sync.models.FirestorePersonalRecord
import com.github.radupana.featherweight.sync.models.FirestoreProgramme
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWeek
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreSyncMetadata
import com.github.radupana.featherweight.sync.models.FirestoreTrainingAnalysis
import com.github.radupana.featherweight.sync.models.FirestoreUserExerciseMax
import com.github.radupana.featherweight.sync.models.FirestoreVariationAlias
import com.github.radupana.featherweight.sync.models.FirestoreVariationInstruction
import com.github.radupana.featherweight.sync.models.FirestoreVariationMuscle
import com.github.radupana.featherweight.sync.models.FirestoreVariationRelation
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val WORKOUTS_COLLECTION = "workouts"
        private const val EXERCISE_LOGS_COLLECTION = "exerciseLogs"
        private const val SET_LOGS_COLLECTION = "setLogs"
        private const val SYNC_METADATA_COLLECTION = "syncMetadata"

        // Exercise collections
        private const val EXERCISE_CORES_COLLECTION = "exerciseCores"
        private const val EXERCISE_VARIATIONS_COLLECTION = "exerciseVariations"
        private const val VARIATION_MUSCLES_COLLECTION = "variationMuscles"
        private const val VARIATION_INSTRUCTIONS_COLLECTION = "variationInstructions"
        private const val VARIATION_ALIASES_COLLECTION = "variationAliases"
        private const val VARIATION_RELATIONS_COLLECTION = "variationRelations"

        // Programme collections
        private const val PROGRAMMES_COLLECTION = "programmes"
        private const val PROGRAMME_WEEKS_COLLECTION = "programmeWeeks"
        private const val PROGRAMME_WORKOUTS_COLLECTION = "programmeWorkouts"
        private const val EXERCISE_SUBSTITUTIONS_COLLECTION = "exerciseSubstitutions"
        private const val PROGRAMME_PROGRESS_COLLECTION = "programmeProgress"

        // User profile/stats collections
        private const val USER_EXERCISE_MAXES_COLLECTION = "userExerciseMaxes"
        private const val ONE_RM_HISTORY_COLLECTION = "oneRMHistory"
        private const val PERSONAL_RECORDS_COLLECTION = "personalRecords"

        // Tracking/analysis collections
        private const val EXERCISE_SWAP_HISTORY_COLLECTION = "exerciseSwapHistory"
        private const val EXERCISE_PERFORMANCE_TRACKING_COLLECTION = "exercisePerformanceTracking"
        private const val GLOBAL_EXERCISE_PROGRESS_COLLECTION = "globalExerciseProgress"
        private const val EXERCISE_CORRELATIONS_COLLECTION = "exerciseCorrelations"
        private const val TRAINING_ANALYSES_COLLECTION = "trainingAnalyses"
        private const val PARSE_REQUESTS_COLLECTION = "parseRequests"

        private const val BATCH_SIZE = 500
    }

    private fun userDocument(userId: String) = firestore.collection(USERS_COLLECTION).document(userId)

    suspend fun uploadWorkouts(
        userId: String,
        workouts: List<FirestoreWorkout>,
    ): Result<Unit> =
        try {
            val batch = firestore.batch()
            val collection = userDocument(userId).collection(WORKOUTS_COLLECTION)

            workouts.forEach { workout ->
                val docRef =
                    if (workout.id.isNotEmpty()) {
                        collection.document(workout.id)
                    } else {
                        collection.document()
                    }
                batch.set(docRef, workout, SetOptions.merge())
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload workouts", e)
            Result.failure(e)
        }

    suspend fun downloadWorkouts(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreWorkout>> =
        try {
            var query: Query =
                userDocument(userId)
                    .collection(WORKOUTS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val workouts =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWorkout::class.java)
                }
            Result.success(workouts)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download workouts", e)
            Result.failure(e)
        }

    suspend fun uploadExerciseLogs(
        userId: String,
        exerciseLogs: List<FirestoreExerciseLog>,
    ): Result<Unit> =
        try {
            val batches = exerciseLogs.chunked(BATCH_SIZE)
            val collection = userDocument(userId).collection(EXERCISE_LOGS_COLLECTION)

            batches.forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { log ->
                    val docRef =
                        if (log.id.isNotEmpty()) {
                            collection.document(log.id)
                        } else {
                            collection.document()
                        }
                    writeBatch.set(docRef, log, SetOptions.merge())
                }
                writeBatch.commit().await()
            }

            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload exercise logs", e)
            Result.failure(e)
        }

    suspend fun uploadSetLogs(
        userId: String,
        setLogs: List<FirestoreSetLog>,
    ): Result<Unit> =
        try {
            val batches = setLogs.chunked(BATCH_SIZE)
            val collection = userDocument(userId).collection(SET_LOGS_COLLECTION)

            batches.forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { log ->
                    val docRef =
                        if (log.id.isNotEmpty()) {
                            collection.document(log.id)
                        } else {
                            collection.document()
                        }
                    writeBatch.set(docRef, log, SetOptions.merge())
                }
                writeBatch.commit().await()
            }

            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload set logs", e)
            Result.failure(e)
        }

    suspend fun updateSyncMetadata(
        userId: String,
        deviceId: String,
        deviceName: String,
    ): Result<Unit> =
        try {
            val metadata =
                FirestoreSyncMetadata(
                    userId = userId,
                    deviceId = deviceId,
                    deviceName = deviceName,
                )

            firestore
                .collection(SYNC_METADATA_COLLECTION)
                .document(userId)
                .set(metadata, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to update sync metadata", e)
            Result.failure(e)
        }

    suspend fun getSyncMetadata(userId: String): Result<FirestoreSyncMetadata?> =
        try {
            val doc =
                firestore
                    .collection(SYNC_METADATA_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

            val metadata = doc.toObject(FirestoreSyncMetadata::class.java)
            Result.success(metadata)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to get sync metadata", e)
            Result.failure(e)
        }

    suspend fun uploadExerciseCores(
        userId: String,
        cores: List<FirestoreExerciseCore>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_CORES_COLLECTION), cores)

    suspend fun uploadExerciseVariations(
        userId: String,
        variations: List<FirestoreExerciseVariation>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_VARIATIONS_COLLECTION), variations)

    suspend fun uploadVariationMuscles(muscles: List<FirestoreVariationMuscle>): Result<Unit> = uploadBatchedData(firestore.collection(VARIATION_MUSCLES_COLLECTION), muscles)

    suspend fun uploadVariationInstructions(instructions: List<FirestoreVariationInstruction>): Result<Unit> = uploadBatchedData(firestore.collection(VARIATION_INSTRUCTIONS_COLLECTION), instructions)

    suspend fun uploadVariationAliases(aliases: List<FirestoreVariationAlias>): Result<Unit> = uploadBatchedData(firestore.collection(VARIATION_ALIASES_COLLECTION), aliases)

    suspend fun uploadVariationRelations(relations: List<FirestoreVariationRelation>): Result<Unit> = uploadBatchedData(firestore.collection(VARIATION_RELATIONS_COLLECTION), relations)

    suspend fun uploadProgrammes(
        userId: String,
        programmes: List<FirestoreProgramme>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PROGRAMMES_COLLECTION), programmes)

    suspend fun uploadProgrammeWeeks(
        userId: String,
        weeks: List<FirestoreProgrammeWeek>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PROGRAMME_WEEKS_COLLECTION), weeks)

    suspend fun uploadProgrammeWorkouts(
        userId: String,
        workouts: List<FirestoreProgrammeWorkout>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PROGRAMME_WORKOUTS_COLLECTION), workouts)

    suspend fun uploadExerciseSubstitutions(
        userId: String,
        substitutions: List<FirestoreExerciseSubstitution>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_SUBSTITUTIONS_COLLECTION), substitutions)

    suspend fun uploadProgrammeProgress(
        userId: String,
        progress: List<FirestoreProgrammeProgress>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PROGRAMME_PROGRESS_COLLECTION), progress)

    suspend fun uploadUserExerciseMaxes(
        userId: String,
        maxes: List<FirestoreUserExerciseMax>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(USER_EXERCISE_MAXES_COLLECTION), maxes)

    suspend fun uploadOneRMHistory(
        userId: String,
        history: List<FirestoreOneRMHistory>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(ONE_RM_HISTORY_COLLECTION), history)

    suspend fun uploadPersonalRecords(
        userId: String,
        records: List<FirestorePersonalRecord>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PERSONAL_RECORDS_COLLECTION), records)

    suspend fun uploadExerciseSwapHistory(
        userId: String,
        swaps: List<FirestoreExerciseSwapHistory>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_SWAP_HISTORY_COLLECTION), swaps)

    suspend fun uploadExercisePerformanceTracking(
        userId: String,
        tracking: List<FirestoreExercisePerformanceTracking>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_PERFORMANCE_TRACKING_COLLECTION), tracking)

    suspend fun uploadGlobalExerciseProgress(
        userId: String,
        progress: List<FirestoreGlobalExerciseProgress>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(GLOBAL_EXERCISE_PROGRESS_COLLECTION), progress)

    suspend fun uploadExerciseCorrelations(correlations: List<FirestoreExerciseCorrelation>): Result<Unit> = uploadBatchedData(firestore.collection(EXERCISE_CORRELATIONS_COLLECTION), correlations)

    suspend fun uploadTrainingAnalyses(
        userId: String,
        analyses: List<FirestoreTrainingAnalysis>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(TRAINING_ANALYSES_COLLECTION), analyses)

    suspend fun uploadParseRequests(
        userId: String,
        requests: List<FirestoreParseRequest>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PARSE_REQUESTS_COLLECTION), requests)

    private suspend inline fun <reified T : Any> uploadBatchedData(
        collection: com.google.firebase.firestore.CollectionReference,
        data: List<T>,
    ): Result<Unit> =
        try {
            val batches = data.chunked(BATCH_SIZE)
            batches.forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { item ->
                    val docRef = collection.document()
                    writeBatch.set(docRef, item, SetOptions.merge())
                }
                writeBatch.commit().await()
            }
            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload ${T::class.simpleName}", e)
            Result.failure(e)
        }

    suspend fun downloadExerciseLogs(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreExerciseLog>> =
        try {
            var query: Query =
                userDocument(userId)
                    .collection(EXERCISE_LOGS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val logs =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreExerciseLog::class.java)
                }
            Result.success(logs)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download exercise logs", e)
            Result.failure(e)
        }

    suspend fun downloadSetLogs(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreSetLog>> =
        try {
            var query: Query =
                userDocument(userId)
                    .collection(SET_LOGS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val logs =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreSetLog::class.java)
                }
            Result.success(logs)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download set logs", e)
            Result.failure(e)
        }

    suspend fun downloadExerciseCores(userId: String): Result<List<FirestoreExerciseCore>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_CORES_COLLECTION))

    suspend fun downloadExerciseVariations(userId: String): Result<List<FirestoreExerciseVariation>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_VARIATIONS_COLLECTION))

    suspend fun downloadVariationMuscles(): Result<List<FirestoreVariationMuscle>> = downloadBatchedData(firestore.collection(VARIATION_MUSCLES_COLLECTION))

    suspend fun downloadVariationInstructions(): Result<List<FirestoreVariationInstruction>> = downloadBatchedData(firestore.collection(VARIATION_INSTRUCTIONS_COLLECTION))

    suspend fun downloadVariationAliases(): Result<List<FirestoreVariationAlias>> = downloadBatchedData(firestore.collection(VARIATION_ALIASES_COLLECTION))

    suspend fun downloadVariationRelations(): Result<List<FirestoreVariationRelation>> = downloadBatchedData(firestore.collection(VARIATION_RELATIONS_COLLECTION))

    suspend fun downloadProgrammes(userId: String): Result<List<FirestoreProgramme>> = downloadBatchedData(userDocument(userId).collection(PROGRAMMES_COLLECTION))

    suspend fun downloadProgrammeWeeks(userId: String): Result<List<FirestoreProgrammeWeek>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WEEKS_COLLECTION))

    suspend fun downloadProgrammeWorkouts(userId: String): Result<List<FirestoreProgrammeWorkout>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WORKOUTS_COLLECTION))

    suspend fun downloadExerciseSubstitutions(userId: String): Result<List<FirestoreExerciseSubstitution>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_SUBSTITUTIONS_COLLECTION))

    suspend fun downloadProgrammeProgress(userId: String): Result<List<FirestoreProgrammeProgress>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_PROGRESS_COLLECTION))

    suspend fun downloadUserExerciseMaxes(userId: String): Result<List<FirestoreUserExerciseMax>> = downloadBatchedData(userDocument(userId).collection(USER_EXERCISE_MAXES_COLLECTION))

    suspend fun downloadOneRMHistory(userId: String): Result<List<FirestoreOneRMHistory>> = downloadBatchedData(userDocument(userId).collection(ONE_RM_HISTORY_COLLECTION))

    suspend fun downloadPersonalRecords(userId: String): Result<List<FirestorePersonalRecord>> = downloadBatchedData(userDocument(userId).collection(PERSONAL_RECORDS_COLLECTION))

    suspend fun downloadExerciseSwapHistory(userId: String): Result<List<FirestoreExerciseSwapHistory>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_SWAP_HISTORY_COLLECTION))

    suspend fun downloadExercisePerformanceTracking(userId: String): Result<List<FirestoreExercisePerformanceTracking>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_PERFORMANCE_TRACKING_COLLECTION))

    suspend fun downloadGlobalExerciseProgress(userId: String): Result<List<FirestoreGlobalExerciseProgress>> = downloadBatchedData(userDocument(userId).collection(GLOBAL_EXERCISE_PROGRESS_COLLECTION))

    suspend fun downloadExerciseCorrelations(): Result<List<FirestoreExerciseCorrelation>> = downloadBatchedData(firestore.collection(EXERCISE_CORRELATIONS_COLLECTION))

    suspend fun downloadTrainingAnalyses(userId: String): Result<List<FirestoreTrainingAnalysis>> = downloadBatchedData(userDocument(userId).collection(TRAINING_ANALYSES_COLLECTION))

    suspend fun downloadParseRequests(userId: String): Result<List<FirestoreParseRequest>> = downloadBatchedData(userDocument(userId).collection(PARSE_REQUESTS_COLLECTION))

    private suspend inline fun <reified T : Any> downloadBatchedData(
        collection: com.google.firebase.firestore.CollectionReference,
    ): Result<List<T>> =
        try {
            val snapshot = collection.get().await()
            val data =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(T::class.java)
                }
            Result.success(data)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download ${T::class.simpleName}", e)
            Result.failure(e)
        }
}
