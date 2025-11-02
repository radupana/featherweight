@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.sync.repository

import com.github.radupana.featherweight.sync.models.FirestoreCustomExercise
import com.github.radupana.featherweight.sync.models.FirestoreCustomExerciseAlias
import com.github.radupana.featherweight.sync.models.FirestoreCustomExerciseInstruction
import com.github.radupana.featherweight.sync.models.FirestoreCustomExerciseMuscle
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreExerciseSwapHistory
import com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import com.github.radupana.featherweight.sync.models.FirestoreParseRequest
import com.github.radupana.featherweight.sync.models.FirestorePersonalRecord
import com.github.radupana.featherweight.sync.models.FirestoreProgramme
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeExerciseTracking
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWeek
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreSyncMetadata
import com.github.radupana.featherweight.sync.models.FirestoreTemplateExercise
import com.github.radupana.featherweight.sync.models.FirestoreTemplateSet
import com.github.radupana.featherweight.sync.models.FirestoreTrainingAnalysis
import com.github.radupana.featherweight.sync.models.FirestoreUserExerciseMax
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.sync.models.FirestoreWorkoutTemplate
import com.github.radupana.featherweight.util.CloudLogger
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

        // Template collections
        private const val WORKOUT_TEMPLATES_COLLECTION = "workoutTemplates"
        private const val TEMPLATE_EXERCISES_COLLECTION = "templateExercises"
        private const val TEMPLATE_SETS_COLLECTION = "templateSets"

        // Exercise collections
        private const val EXERCISES_COLLECTION = "exercises" // Denormalized collection
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
        private const val PERSONAL_RECORDS_COLLECTION = "personalRecords"
        private const val USER_EXERCISE_USAGE_COLLECTION = "userExerciseUsage"

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
                    CloudLogger.error("FirestoreRepository", "Failed to parse exercise ${doc.id} - Firebase error", e)
                } catch (e: IllegalStateException) {
                    CloudLogger.error("FirestoreRepository", "Failed to parse exercise ${doc.id} - invalid state", e)
                }
            }

            CloudLogger.debug("FirestoreRepository", "Downloaded ${exercises.size} system exercises")
            Result.success(exercises)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "Failed to download system exercises - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            CloudLogger.error("FirestoreRepository", "Failed to download system exercises - network error", e)
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
                    if (!workout.id.isNullOrEmpty()) {
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
            CloudLogger.info("FirestoreRepository", "downloadWorkouts: Starting download for user $userId, lastSync=$lastSyncTime")
            CloudLogger.info("FirestoreRepository", "Firestore project ID: ${firestore.app.options.projectId}")
            var query: Query =
                userDocument(userId)
                    .collection(WORKOUTS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            CloudLogger.info("FirestoreRepository", "downloadWorkouts: Executing query...")
            val snapshot = query.get().await()
            val workouts =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWorkout::class.java)
                }
            CloudLogger.info("FirestoreRepository", "downloadWorkouts: Downloaded ${workouts.size} workouts")
            Result.success(workouts)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "downloadWorkouts failed: ${e.message}", e)
            CloudLogger.error("FirestoreRepository", "FirebaseException type: ${e.javaClass.simpleName}")
            CloudLogger.error("FirestoreRepository", "Error code: ${e.message?.contains("PERMISSION_DENIED") ?: false}")
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download workouts", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            CloudLogger.error("FirestoreRepository", "downloadWorkouts network error: ${e.message}", e)
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
                        if (!log.id.isNullOrEmpty()) {
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
                        if (!log.id.isNullOrEmpty()) {
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

    suspend fun uploadWorkoutTemplates(
        userId: String,
        templates: List<FirestoreWorkoutTemplate>,
    ): Result<Unit> =
        try {
            val batch = firestore.batch()
            val collection = userDocument(userId).collection(WORKOUT_TEMPLATES_COLLECTION)

            templates.forEach { template ->
                val docRef =
                    if (!template.id.isNullOrEmpty()) {
                        collection.document(template.id)
                    } else {
                        collection.document()
                    }
                batch.set(docRef, template, SetOptions.merge())
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload workout templates", e)
            Result.failure(e)
        }

    suspend fun uploadTemplateExercises(
        userId: String,
        exercises: List<FirestoreTemplateExercise>,
    ): Result<Unit> =
        try {
            val batches = exercises.chunked(BATCH_SIZE)
            val collection = userDocument(userId).collection(TEMPLATE_EXERCISES_COLLECTION)

            batches.forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { exercise ->
                    val docRef =
                        if (!exercise.id.isNullOrEmpty()) {
                            collection.document(exercise.id)
                        } else {
                            collection.document()
                        }
                    writeBatch.set(docRef, exercise, SetOptions.merge())
                }
                writeBatch.commit().await()
            }

            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload template exercises", e)
            Result.failure(e)
        }

    suspend fun uploadTemplateSets(
        userId: String,
        sets: List<FirestoreTemplateSet>,
    ): Result<Unit> =
        try {
            val batches = sets.chunked(BATCH_SIZE)
            val collection = userDocument(userId).collection(TEMPLATE_SETS_COLLECTION)

            batches.forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { set ->
                    val docRef =
                        if (!set.id.isNullOrEmpty()) {
                            collection.document(set.id)
                        } else {
                            collection.document()
                        }
                    writeBatch.set(docRef, set, SetOptions.merge())
                }
                writeBatch.commit().await()
            }

            Result.success(Unit)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to upload template sets", e)
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
                    installationId = deviceId,
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
            CloudLogger.info("FirestoreRepository", "getSyncMetadata: Getting metadata for user $userId")
            val doc =
                firestore
                    .collection(SYNC_METADATA_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

            val metadata = doc.toObject(FirestoreSyncMetadata::class.java)
            CloudLogger.info("FirestoreRepository", "getSyncMetadata: Retrieved metadata - exists=${doc.exists()}, data=$metadata")
            Result.success(metadata)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "getSyncMetadata failed: ${e.message}", e)
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to get sync metadata", e)
            Result.failure(e)
        }

    suspend fun uploadExercises(
        userId: String,
        variations: List<FirestoreExercise>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_VARIATIONS_COLLECTION), variations)

    // Exercise-related data is now embedded in the denormalized FirestoreExercise document
    // No separate upload functions needed for muscles, instructions, or aliases

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

    suspend fun uploadPersonalRecords(
        userId: String,
        records: List<FirestorePersonalRecord>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(PERSONAL_RECORDS_COLLECTION), records)

    suspend fun uploadUserExerciseUsages(
        userId: String,
        usages: List<com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(USER_EXERCISE_USAGE_COLLECTION), usages)

    suspend fun uploadExerciseSwapHistory(
        userId: String,
        swaps: List<FirestoreExerciseSwapHistory>,
    ): Result<Unit> = uploadBatchedData(userDocument(userId).collection(EXERCISE_SWAP_HISTORY_COLLECTION), swaps)

    suspend fun uploadExercisePerformanceTracking(
        userId: String,
        tracking: List<FirestoreProgrammeExerciseTracking>,
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
     * Downloads custom exercises for a specific user from normalized Firestore structure.
     * Assembles main exercise documents + subcollections into typed FirestoreCustomExercise objects.
     * @param userId The user whose custom exercises to download
     * @param lastSyncTime Optional timestamp to get only updated exercises
     * @return List of FirestoreCustomExercise objects with embedded subcollection data
     */
    suspend fun downloadCustomExercises(
        userId: String,
        lastSyncTime: Timestamp?,
    ): Result<List<FirestoreCustomExercise>> =
        try {
            CloudLogger.debug("FirestoreRepository", "Downloading custom exercises for user $userId (lastSyncTime: $lastSyncTime)")
            var query: Query =
                userDocument(userId)
                    .collection("customExercises")
                    .whereEqualTo("isDeleted", false)

            // Filter by update time if provided
            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            CloudLogger.debug("FirestoreRepository", "Found ${snapshot.documents.size} custom exercise documents")
            val exercises = mutableListOf<FirestoreCustomExercise>()

            snapshot.documents.forEach { doc ->
                try {
                    CloudLogger.debug("FirestoreRepository", "Processing custom exercise ${doc.id}")
                    val baseExercise = doc.toObject(FirestoreCustomExercise::class.java)
                    if (baseExercise == null) {
                        CloudLogger.warn("FirestoreRepository", "Failed to parse custom exercise ${doc.id}")
                        return@forEach
                    }

                    // Fetch subcollections for this exercise
                    val musclesSnapshot =
                        doc.reference
                            .collection("muscles")
                            .whereEqualTo("isDeleted", false)
                            .get()
                            .await()
                    val aliasesSnapshot =
                        doc.reference
                            .collection("aliases")
                            .whereEqualTo("isDeleted", false)
                            .get()
                            .await()
                    val instructionsSnapshot =
                        doc.reference
                            .collection("instructions")
                            .whereEqualTo("isDeleted", false)
                            .get()
                            .await()

                    CloudLogger.debug(
                        "FirestoreRepository",
                        "Exercise ${doc.id}: ${musclesSnapshot.size()} muscles, " +
                            "${aliasesSnapshot.size()} aliases, ${instructionsSnapshot.size()} instructions",
                    )

                    // Convert subcollection documents to embedded types
                    val muscles =
                        musclesSnapshot.documents
                            .mapNotNull {
                                it.toObject(FirestoreCustomExerciseMuscle::class.java)
                            }.map { FirestoreMuscle(it.muscle, it.targetType == "primary", 1.0) }

                    val aliases =
                        aliasesSnapshot.documents.mapNotNull {
                            it.toObject(FirestoreCustomExerciseAlias::class.java)?.alias
                        }

                    val instructions =
                        instructionsSnapshot.documents
                            .mapNotNull {
                                it.toObject(FirestoreCustomExerciseInstruction::class.java)
                            }.map { FirestoreInstruction(it.instructionType, it.instructionText, it.orderIndex, "en") }

                    // Assemble complete exercise with subcollection data
                    exercises.add(
                        baseExercise.copy(
                            muscles = muscles,
                            aliases = aliases,
                            instructions = instructions,
                        ),
                    )
                    CloudLogger.debug("FirestoreRepository", "Successfully assembled custom exercise ${doc.id}: ${baseExercise.name}")
                } catch (e: FirebaseException) {
                    CloudLogger.error("FirestoreRepository", "Failed to process custom exercise ${doc.id} - Firebase error", e)
                } catch (e: IllegalStateException) {
                    CloudLogger.error("FirestoreRepository", "Failed to process custom exercise ${doc.id} - invalid state", e)
                }
            }

            CloudLogger.debug("FirestoreRepository", "Downloaded ${exercises.size} custom exercises for user $userId")
            Result.success(exercises)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "Failed to download custom exercises for user $userId - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            CloudLogger.error("FirestoreRepository", "Failed to download custom exercises for user $userId - network error", e)
            Result.failure(e)
        }

    /**
     * Uploads a custom exercise using normalized Firestore structure.
     * Writes main exercise document + subcollections for muscles, aliases, and instructions.
     * @param userId The user who owns the exercise
     * @param exercise The custom exercise
     * @param muscles List of muscle mappings for this exercise
     * @param aliases List of aliases for this exercise
     * @param instructions List of instructions for this exercise
     */
    suspend fun uploadCustomExercise(
        userId: String,
        exercise: com.github.radupana.featherweight.data.exercise.Exercise,
        muscles: List<com.github.radupana.featherweight.data.exercise.ExerciseMuscle>,
        aliases: List<com.github.radupana.featherweight.data.exercise.ExerciseAlias>,
        instructions: List<com.github.radupana.featherweight.data.exercise.ExerciseInstruction>,
    ): Result<Unit> =
        try {
            CloudLogger.debug(
                "FirestoreRepository",
                "Uploading custom exercise ${exercise.id} (${exercise.name}) for user $userId " +
                    "with ${muscles.size} muscles, ${aliases.size} aliases, ${instructions.size} instructions",
            )
            val batch = firestore.batch()

            // Main exercise document
            val exerciseRef = userDocument(userId).collection("customExercises").document(exercise.id)
            val exerciseData =
                hashMapOf(
                    // Don't include "id" - it's handled by @DocumentId annotation
                    "type" to exercise.type,
                    "userId" to exercise.userId,
                    "name" to exercise.name,
                    "category" to exercise.category,
                    "movementPattern" to exercise.movementPattern,
                    "isCompound" to exercise.isCompound,
                    "equipment" to exercise.equipment,
                    "difficulty" to exercise.difficulty,
                    "requiresWeight" to exercise.requiresWeight,
                    "rmScalingType" to exercise.rmScalingType,
                    "restDurationSeconds" to exercise.restDurationSeconds,
                    "createdAt" to exercise.createdAt.toString(),
                    "updatedAt" to exercise.updatedAt.toString(),
                    "isDeleted" to exercise.isDeleted,
                )
            batch.set(exerciseRef, exerciseData, SetOptions.merge())
            CloudLogger.debug("FirestoreRepository", "Batched main exercise document for ${exercise.id}")

            // Muscle subcollection documents
            muscles.forEach { muscle ->
                val muscleData =
                    hashMapOf(
                        // Don't include "id" - it's handled by @DocumentId annotation
                        "exerciseId" to muscle.exerciseId,
                        "muscle" to muscle.muscle,
                        "targetType" to muscle.targetType,
                        "isDeleted" to muscle.isDeleted,
                    )
                batch.set(exerciseRef.collection("muscles").document(muscle.id), muscleData, SetOptions.merge())
            }
            CloudLogger.debug("FirestoreRepository", "Batched ${muscles.size} muscle documents for ${exercise.id}")

            // Alias subcollection documents
            aliases.forEach { alias ->
                val aliasData =
                    hashMapOf(
                        // Don't include "id" - it's handled by @DocumentId annotation
                        "exerciseId" to alias.exerciseId,
                        "alias" to alias.alias,
                        "isDeleted" to alias.isDeleted,
                    )
                batch.set(exerciseRef.collection("aliases").document(alias.id), aliasData, SetOptions.merge())
            }
            CloudLogger.debug("FirestoreRepository", "Batched ${aliases.size} alias documents for ${exercise.id}")

            // Instruction subcollection documents
            instructions.forEach { instruction ->
                val instructionData =
                    hashMapOf(
                        // Don't include "id" - it's handled by @DocumentId annotation
                        "exerciseId" to instruction.exerciseId,
                        "instructionType" to instruction.instructionType,
                        "orderIndex" to instruction.orderIndex,
                        "instructionText" to instruction.instructionText,
                        "isDeleted" to instruction.isDeleted,
                    )
                batch.set(exerciseRef.collection("instructions").document(instruction.id), instructionData, SetOptions.merge())
            }
            CloudLogger.debug("FirestoreRepository", "Batched ${instructions.size} instruction documents for ${exercise.id}")

            batch.commit().await()
            CloudLogger.debug("FirestoreRepository", "Successfully uploaded custom exercise ${exercise.id} (${exercise.name})")
            Result.success(Unit)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "Failed to upload custom exercise ${exercise.id} - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            CloudLogger.error("FirestoreRepository", "Failed to upload custom exercise ${exercise.id} - network error", e)
            Result.failure(e)
        }

    /**
     * Soft-deletes a custom exercise from Firestore.
     * Sets isDeleted = true on main exercise document and all subcollection documents.
     * @param userId The user who owns the exercise
     * @param exerciseId The ID of the exercise to delete
     */
    suspend fun deleteCustomExercise(
        userId: String,
        exerciseId: String,
    ): Result<Unit> =
        try {
            CloudLogger.debug("FirestoreRepository", "Soft-deleting custom exercise $exerciseId for user $userId")
            val batch = firestore.batch()
            val exerciseRef = userDocument(userId).collection("customExercises").document(exerciseId)

            // Soft delete main exercise document
            batch.update(exerciseRef, "isDeleted", true)
            CloudLogger.debug("FirestoreRepository", "Batched soft-delete for main exercise document $exerciseId")

            // Soft delete all subcollection documents
            val musclesDocs = exerciseRef.collection("muscles").get().await()
            musclesDocs.documents.forEach { batch.update(it.reference, "isDeleted", true) }
            CloudLogger.debug("FirestoreRepository", "Batched soft-delete for ${musclesDocs.size()} muscle documents")

            val aliasesDocs = exerciseRef.collection("aliases").get().await()
            aliasesDocs.documents.forEach { batch.update(it.reference, "isDeleted", true) }
            CloudLogger.debug("FirestoreRepository", "Batched soft-delete for ${aliasesDocs.size()} alias documents")

            val instructionsDocs = exerciseRef.collection("instructions").get().await()
            instructionsDocs.documents.forEach { batch.update(it.reference, "isDeleted", true) }
            CloudLogger.debug("FirestoreRepository", "Batched soft-delete for ${instructionsDocs.size()} instruction documents")

            batch.commit().await()
            CloudLogger.debug("FirestoreRepository", "Successfully soft-deleted custom exercise $exerciseId")
            Result.success(Unit)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "Failed to delete custom exercise $exerciseId - Firebase error", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            CloudLogger.error("FirestoreRepository", "Failed to delete custom exercise $exerciseId - network error", e)
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
                    // Extract the ID from the object to use as Firestore document ID
                    // This preserves IDs across local/remote and prevents duplicates
                    val id =
                        try {
                            val idField = T::class.java.getDeclaredField("id")
                            idField.isAccessible = true
                            idField.get(item) as? String
                        } catch (e: Exception) {
                            null
                        }

                    val docRef =
                        if (!id.isNullOrEmpty()) {
                            collection.document(id) // Use existing ID to preserve references
                        } else {
                            collection.document() // Auto-generate only if no ID exists
                        }
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
            CloudLogger.info("FirestoreRepository", "downloadExerciseLogs: Starting download for user $userId")
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
            CloudLogger.info("FirestoreRepository", "downloadExerciseLogs: Downloaded ${logs.size} exercise logs")
            Result.success(logs)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "downloadExerciseLogs failed: ${e.message}", e)
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

    suspend fun downloadWorkoutTemplates(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreWorkoutTemplate>> =
        try {
            CloudLogger.info("FirestoreRepository", "downloadWorkoutTemplates: Starting download for user $userId")
            var query: Query =
                userDocument(userId)
                    .collection(WORKOUT_TEMPLATES_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val templates =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreWorkoutTemplate::class.java)
                }
            CloudLogger.info("FirestoreRepository", "downloadWorkoutTemplates: Downloaded ${templates.size} templates")
            Result.success(templates)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "downloadWorkoutTemplates failed: ${e.message}", e)
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download workout templates", e)
            Result.failure(e)
        }

    suspend fun downloadTemplateExercises(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreTemplateExercise>> =
        try {
            var query: Query =
                userDocument(userId)
                    .collection(TEMPLATE_EXERCISES_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val exercises =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreTemplateExercise::class.java)
                }
            Result.success(exercises)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download template exercises", e)
            Result.failure(e)
        }

    suspend fun downloadTemplateSets(
        userId: String,
        lastSyncTime: Timestamp? = null,
    ): Result<List<FirestoreTemplateSet>> =
        try {
            var query: Query =
                userDocument(userId)
                    .collection(TEMPLATE_SETS_COLLECTION)
                    .orderBy("lastModified", Query.Direction.DESCENDING)

            lastSyncTime?.let {
                query = query.whereGreaterThan("lastModified", it)
            }

            val snapshot = query.get().await()
            val sets =
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreTemplateSet::class.java)
                }
            Result.success(sets)
        } catch (e: FirebaseException) {
            ExceptionLogger.logNonCritical("FirestoreRepository", "Failed to download template sets", e)
            Result.failure(e)
        }

    suspend fun downloadExercises(): Result<List<FirestoreExercise>> = downloadBatchedData(firestore.collection(EXERCISE_VARIATIONS_COLLECTION))

    // Exercise-related data is now embedded in the denormalized FirestoreExercise document
    // No separate download functions needed for muscles, instructions, or aliases

    suspend fun downloadProgrammes(userId: String): Result<List<FirestoreProgramme>> = downloadBatchedData(userDocument(userId).collection(PROGRAMMES_COLLECTION))

    suspend fun downloadProgrammeWeeks(userId: String): Result<List<FirestoreProgrammeWeek>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WEEKS_COLLECTION))

    suspend fun downloadProgrammeWorkouts(userId: String): Result<List<FirestoreProgrammeWorkout>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_WORKOUTS_COLLECTION))

    suspend fun downloadProgrammeProgress(userId: String): Result<List<FirestoreProgrammeProgress>> = downloadBatchedData(userDocument(userId).collection(PROGRAMME_PROGRESS_COLLECTION))

    suspend fun downloadUserExerciseMaxes(userId: String): Result<List<FirestoreUserExerciseMax>> = downloadBatchedData(userDocument(userId).collection(USER_EXERCISE_MAXES_COLLECTION))

    suspend fun downloadPersonalRecords(userId: String): Result<List<FirestorePersonalRecord>> = downloadBatchedData(userDocument(userId).collection(PERSONAL_RECORDS_COLLECTION))

    suspend fun downloadUserExerciseUsages(userId: String): Result<List<com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage>> = downloadBatchedData(userDocument(userId).collection(USER_EXERCISE_USAGE_COLLECTION))

    suspend fun downloadExerciseSwapHistory(userId: String): Result<List<FirestoreExerciseSwapHistory>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_SWAP_HISTORY_COLLECTION))

    @Suppress("MaxLineLength")
    suspend fun downloadExercisePerformanceTracking(userId: String): Result<List<FirestoreProgrammeExerciseTracking>> = downloadBatchedData(userDocument(userId).collection(EXERCISE_PERFORMANCE_TRACKING_COLLECTION))

    @Suppress("MaxLineLength")
    suspend fun downloadGlobalExerciseProgress(userId: String): Result<List<FirestoreGlobalExerciseProgress>> = downloadBatchedData(userDocument(userId).collection(GLOBAL_EXERCISE_PROGRESS_COLLECTION))

    suspend fun downloadTrainingAnalyses(userId: String): Result<List<FirestoreTrainingAnalysis>> = downloadBatchedData(userDocument(userId).collection(TRAINING_ANALYSES_COLLECTION))

    suspend fun downloadParseRequests(userId: String): Result<List<FirestoreParseRequest>> = downloadBatchedData(userDocument(userId).collection(PARSE_REQUESTS_COLLECTION))

    suspend fun deleteParseRequest(
        userId: String,
        requestId: String,
    ): Result<Unit> =
        try {
            userDocument(userId)
                .collection(PARSE_REQUESTS_COLLECTION)
                .document(requestId)
                .delete()
                .await()
            CloudLogger.debug("FirestoreRepository", "Successfully deleted parse request $requestId from Firestore")
            Result.success(Unit)
        } catch (e: FirebaseException) {
            CloudLogger.error("FirestoreRepository", "Failed to delete parse request $requestId", e)
            ExceptionLogger.logNonCritical("FirestoreRepository", "Delete parse request failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            CloudLogger.error("FirestoreRepository", "Unexpected error deleting parse request $requestId", e)
            ExceptionLogger.logNonCritical("FirestoreRepository", "Delete parse request failed", e)
            Result.failure(e)
        }

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
