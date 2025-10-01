package com.github.radupana.featherweight.sync.repository

import android.util.Log
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreExerciseCore
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreExercisePerformanceTracking
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
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val useTestDatabase: Boolean = false,
) {
    private val firestore =
        if (useTestDatabase) {
            FirebaseFirestore.getInstance("featherweight-new")
        } else {
            FirebaseFirestore.getInstance("featherweight-v2")
        }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val WORKOUTS_COLLECTION = "workouts"
        private const val EXERCISE_LOGS_COLLECTION = "exerciseLogs"
        private const val SET_LOGS_COLLECTION = "setLogs"
        private const val SYNC_METADATA_COLLECTION = "syncMetadata"

        // Exercise collections
        private const val EXERCISES_COLLECTION = "exercises" // Denormalized collection
        private const val EXERCISE_CORES_COLLECTION = "exerciseCores" // Legacy normalized
        private const val EXERCISE_VARIATIONS_COLLECTION = "exerciseVariations" // Legacy normalized
        private const val VARIATION_MUSCLES_COLLECTION = "variationMuscles"
        private const val VARIATION_INSTRUCTIONS_COLLECTION = "variationInstructions"
        private const val VARIATION_ALIASES_COLLECTION = "variationAliases"

        // Programme collections
        private const val PROGRAMMES_COLLECTION = "programmes"
        private const val PROGRAMME_WEEKS_COLLECTION = "programmeWeeks"
        private const val PROGRAMME_WORKOUTS_COLLECTION = "programmeWorkouts"
        private const val PROGRAMME_PROGRESS_COLLECTION = "programmeProgress"

        // User profile/stats collections
        private const val USER_EXERCISE_MAXES_COLLECTION = "userExerciseMaxes"
        private const val ONE_RM_HISTORY_COLLECTION = "oneRMHistory"
        private const val PERSONAL_RECORDS_COLLECTION = "personalRecords"

        // Tracking/analysis collections
        private const val EXERCISE_SWAP_HISTORY_COLLECTION = "exerciseSwapHistory"
        private const val EXERCISE_PERFORMANCE_TRACKING_COLLECTION = "exercisePerformanceTracking"
        private const val GLOBAL_EXERCISE_PROGRESS_COLLECTION = "globalExerciseProgress"
        private const val TRAINING_ANALYSES_COLLECTION = "trainingAnalyses"
        private const val PARSE_REQUESTS_COLLECTION = "parseRequests"

        private const val BATCH_SIZE = 500
    }

    /**
     * Downloads system exercises from the denormalized collection.
     * @param lastSyncTime Optional timestamp to get only updated exercises
     * @return Map of exercise ID to FirestoreExercise
     */
    suspend fun downloadSystemExercises(lastSyncTime: Timestamp?): Result<Map<String, FirestoreExercise>> =
        try {
            var query: Query = firestore.collection(EXERCISES_COLLECTION)

            // Filter by update time if provided
            lastSyncTime?.let {
                query = query.whereGreaterThan("updatedAt", it.toDate().toInstant().toString())
            }

            val snapshot = query.get().await()
            val exercises = mutableMapOf<String, FirestoreExercise>()

            snapshot.documents.forEach { doc ->
                try {
                    val exercise = doc.toObject(FirestoreExercise::class.java)
                    if (exercise != null) {
                        exercises[doc.id] = exercise
                    }
                } catch (e: FirebaseException) {
                    Log.e("FirestoreRepository", "Failed to parse exercise ${doc.id} - Firebase error", e)
                } catch (e: IllegalStateException) {
                    Log.e("FirestoreRepository", "Failed to parse exercise ${doc.id} - invalid state", e)
                }
            }

            Log.d("FirestoreRepository", "Downloaded ${exercises.size} system exercises")
            Result.success(exercises)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "Failed to download system exercises - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e("FirestoreRepository", "Failed to download system exercises - network error", e)
            Result.failure(e)
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
            Log.i("FirestoreRepository", "downloadWorkouts: Starting download for user $userId, lastSync=$lastSyncTime")
            Log.i("FirestoreRepository", "Firestore project ID: ${firestore.app.options.projectId}")
            var query: Query =
                userDocument(userId)
                    .collection(WORKOUTS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            Log.i("FirestoreRepository", "downloadWorkouts: Executing query...")
            val snapshot = query.get().await()
            val workouts =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWorkout::class.java)
                }
            Log.i("FirestoreRepository", "downloadWorkouts: Downloaded ${workouts.size} workouts")
            Result.success(workouts)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "downloadWorkouts failed: ${e.message}", e)
            Log.e("FirestoreRepository", "FirebaseException type: ${e.javaClass.simpleName}")
            Log.e("FirestoreRepository", "Error code: ${e.message?.contains("PERMISSION_DENIED") ?: false}")
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download workouts", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e("FirestoreRepository", "downloadWorkouts network error: ${e.message}", e)
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
            Log.i("FirestoreRepository", "getSyncMetadata: Getting metadata for user $userId")
            val doc =
                firestore
                    .collection(SYNC_METADATA_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

            val metadata = doc.toObject(FirestoreSyncMetadata::class.java)
            Log.i("FirestoreRepository", "getSyncMetadata: Retrieved metadata - exists=${doc.exists()}, data=$metadata")
            Result.success(metadata)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "getSyncMetadata failed: ${e.message}", e)
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

    suspend fun uploadTrainingAnalyses(
        userId: String,
        analyses: List<FirestoreTrainingAnalysis>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(TRAINING_ANALYSES_COLLECTION), analyses)

    suspend fun uploadParseRequests(
        userId: String,
        requests: List<FirestoreParseRequest>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PARSE_REQUESTS_COLLECTION), requests)

    /**
     * Downloads custom exercises for a specific user.
     * @param userId The user whose custom exercises to download
     * @param lastSyncTime Optional timestamp to get only updated exercises
     * @return Map of exercise ID to custom exercise data
     */
    suspend fun downloadCustomExercises(
        userId: String,
        lastSyncTime: Timestamp?,
    ): Result<Map<String, Map<String, Any>>> =
        try {
            var query: Query = userDocument(userId).collection("customExercises")

            // Filter by update time if provided
            lastSyncTime?.let {
                query = query.whereGreaterThan("updatedAt", it)
            }

            val snapshot = query.get().await()
            val exercises = mutableMapOf<String, Map<String, Any>>()

            snapshot.documents.forEach { doc ->
                exercises[doc.id] = doc.data ?: emptyMap()
            }

            Log.d("FirestoreRepository", "Downloaded ${exercises.size} custom exercises for user $userId")
            Result.success(exercises)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "Failed to download custom exercises - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e("FirestoreRepository", "Failed to download custom exercises - network error", e)
            Result.failure(e)
        }

    /**
     * Uploads a custom exercise to Firestore.
     * @param userId The user who owns the exercise
     * @param variation The custom exercise variation
     * @param core The custom exercise core
     */
    suspend fun uploadCustomExercise(
        userId: String,
        variation: com.github.radupana.featherweight.data.exercise.ExerciseVariation,
        core: com.github.radupana.featherweight.data.exercise.ExerciseCore,
    ): Result<Unit> =
        try {
            val exerciseData =
                hashMapOf(
                    "id" to variation.id,
                    "name" to variation.name,
                    "coreId" to core.id,
                    "coreName" to core.name,
                    "category" to core.category.name,
                    "movementPattern" to core.movementPattern.name,
                    "isCompound" to core.isCompound,
                    "equipment" to variation.equipment.name,
                    "difficulty" to variation.difficulty.name,
                    "requiresWeight" to variation.requiresWeight,
                    "restDurationSeconds" to variation.restDurationSeconds,
                    "updatedAt" to Timestamp.now(),
                )

            userDocument(userId)
                .collection("customExercises")
                .document(variation.id.toString())
                .set(exerciseData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "Failed to upload custom exercise - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e("FirestoreRepository", "Failed to upload custom exercise - network error", e)
            Result.failure(e)
        }

    /**
     * Deletes a custom exercise from Firestore.
     * @param userId The user who owns the exercise
     * @param exerciseId The ID of the exercise to delete
     */
    suspend fun deleteCustomExercise(
        userId: String,
        exerciseId: String,
    ): Result<Unit> =
        try {
            userDocument(userId)
                .collection("customExercises")
                .document(exerciseId.toString())
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "Failed to delete custom exercise - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e("FirestoreRepository", "Failed to delete custom exercise - network error", e)
            Result.failure(e)
        }

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
            Log.i("FirestoreRepository", "downloadExerciseLogs: Starting download for user $userId")
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
            Log.i("FirestoreRepository", "downloadExerciseLogs: Downloaded ${logs.size} exercise logs")
            Result.success(logs)
        } catch (e: FirebaseException) {
            Log.e("FirestoreRepository", "downloadExerciseLogs failed: ${e.message}", e)
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

    suspend fun downloadExerciseCores(): Result<List<FirestoreExerciseCore>> = downloadBatchedData(firestore.collection(EXERCISE_CORES_COLLECTION))

    suspend fun downloadExerciseVariations(): Result<List<FirestoreExerciseVariation>> = downloadBatchedData(firestore.collection(EXERCISE_VARIATIONS_COLLECTION))

    suspend fun downloadVariationMuscles(): Result<List<FirestoreVariationMuscle>> = downloadBatchedData(firestore.collection(VARIATION_MUSCLES_COLLECTION))

    suspend fun downloadVariationInstructions(): Result<List<FirestoreVariationInstruction>> = downloadBatchedData(firestore.collection(VARIATION_INSTRUCTIONS_COLLECTION))

    suspend fun downloadVariationAliases(): Result<List<FirestoreVariationAlias>> = downloadBatchedData(firestore.collection(VARIATION_ALIASES_COLLECTION))

    suspend fun downloadProgrammes(userId: String): Result<List<FirestoreProgramme>> = downloadBatchedData(userDocument(userId).collection(PROGRAMMES_COLLECTION))

    suspend fun downloadProgrammeWeeks(userId: String): Result<List<FirestoreProgrammeWeek>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WEEKS_COLLECTION))

    suspend fun downloadProgrammeWorkouts(userId: String): Result<List<FirestoreProgrammeWorkout>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WORKOUTS_COLLECTION))

    suspend fun downloadProgrammeProgress(userId: String): Result<List<FirestoreProgrammeProgress>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_PROGRESS_COLLECTION))

    suspend fun downloadUserExerciseMaxes(userId: String): Result<List<FirestoreUserExerciseMax>> = downloadBatchedData(userDocument(userId).collection(USER_EXERCISE_MAXES_COLLECTION))

    suspend fun downloadOneRMHistory(userId: String): Result<List<FirestoreOneRMHistory>> = downloadBatchedData(userDocument(userId).collection(ONE_RM_HISTORY_COLLECTION))

    suspend fun downloadPersonalRecords(userId: String): Result<List<FirestorePersonalRecord>> = downloadBatchedData(userDocument(userId).collection(PERSONAL_RECORDS_COLLECTION))

    suspend fun downloadExerciseSwapHistory(userId: String): Result<List<FirestoreExerciseSwapHistory>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_SWAP_HISTORY_COLLECTION))

    @Suppress("MaxLineLength")
    suspend fun downloadExercisePerformanceTracking(userId: String): Result<List<FirestoreExercisePerformanceTracking>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_PERFORMANCE_TRACKING_COLLECTION))

    @Suppress("MaxLineLength")
    suspend fun downloadGlobalExerciseProgress(userId: String): Result<List<FirestoreGlobalExerciseProgress>> = downloadBatchedData(userDocument(userId).collection(GLOBAL_EXERCISE_PROGRESS_COLLECTION))

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
