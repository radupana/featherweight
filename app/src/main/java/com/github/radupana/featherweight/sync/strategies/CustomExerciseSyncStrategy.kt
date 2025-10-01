package com.github.radupana.featherweight.sync.strategies

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.sync.SyncStrategy
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneOffset

/**
 * Sync strategy for user's custom exercises.
 * Handles bidirectional sync between local SQLite and Firestore.
 */
class CustomExerciseSyncStrategy(
    private val database: FeatherweightDatabase,
    private val firestoreRepository: FirestoreRepository,
) : SyncStrategy {
    companion object {
        private const val TAG = "CustomExerciseSync"
    }

    override suspend fun downloadAndMerge(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (userId == null) {
                Log.w(TAG, "No user ID provided for custom exercise sync")
                return@withContext Result.success(Unit)
            }

            try {
                Log.d(TAG, "Starting custom exercise sync for user: $userId")

                // Download custom exercises from Firestore
                val remoteExercises = firestoreRepository.downloadCustomExercises(userId, lastSyncTime).getOrThrow()
                Log.d(TAG, "Downloaded ${remoteExercises.size} custom exercises from Firestore")

                // Get local custom exercises for comparison
                val localVariations = database.exerciseVariationDao().getCustomVariationsByUser(userId)
                val localVariationMap = localVariations.associateBy { it.id }

                // Process remote exercises
                remoteExercises.forEach { (exerciseId, firestoreCustomExercise) ->
                    try {
                        val remoteVariationId = exerciseId

                        // Check if we have this exercise locally
                        val localVariation = localVariationMap[remoteVariationId]

                        if (localVariation == null) {
                            // New exercise from remote - insert it
                            insertRemoteExercise(userId, remoteVariationId, firestoreCustomExercise)
                        } else {
                            // Exercise exists locally - check timestamps for conflict resolution
                            val remoteTimestamp = firestoreCustomExercise["updatedAt"] as? Timestamp
                            val localTimestamp = Timestamp(localVariation.updatedAt.toEpochSecond(ZoneOffset.UTC), 0)

                            if (remoteTimestamp != null && remoteTimestamp.seconds > localTimestamp.seconds) {
                                // Remote is newer - update local
                                updateLocalExercise(remoteVariationId, firestoreCustomExercise)
                            }
                        }
                    } catch (e: com.google.firebase.FirebaseException) {
                        Log.e(TAG, "Failed to process custom exercise $exerciseId - Firebase error", e)
                    } catch (e: android.database.sqlite.SQLiteException) {
                        Log.e(TAG, "Failed to process custom exercise $exerciseId - database error", e)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Failed to process custom exercise $exerciseId - invalid state", e)
                    }
                }

                // Upload local exercises that are new or updated
                uploadLocalChanges(userId, localVariations, lastSyncTime)

                Log.d(TAG, "Custom exercise sync completed successfully")
                Result.success(Unit)
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Failed to sync custom exercises - Firebase error", e)
                Result.failure(e)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to sync custom exercises - database error", e)
                Result.failure(e)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to sync custom exercises - network error", e)
                Result.failure(e)
            }
        }

    private suspend fun insertRemoteExercise(
        userId: String,
        variationId: String,
        firestoreData: Map<String, Any>,
    ) {
        // Extract core data
        val coreId = (firestoreData["coreId"] as? String) ?: (firestoreData["coreId"] as? Long)?.toString() ?: return
        val coreName = firestoreData["coreName"] as? String ?: return
        val category = firestoreData["category"] as? String ?: return
        val movementPattern = firestoreData["movementPattern"] as? String ?: return
        val isCompound = firestoreData["isCompound"] as? Boolean ?: false

        // Insert core if it doesn't exist
        val existingCore = database.exerciseCoreDao().getCoreById(coreId)
        if (existingCore == null) {
            val core =
                ExerciseCore(
                    id = coreId,
                    userId = userId,
                    name = coreName,
                    category =
                        com.github.radupana.featherweight.data.exercise.ExerciseCategory
                            .valueOf(category),
                    movementPattern =
                        com.github.radupana.featherweight.data.exercise.MovementPattern
                            .valueOf(movementPattern),
                    isCompound = isCompound,
                )
            database.exerciseCoreDao().insertCore(core)
        }

        // Insert variation
        val variation =
            ExerciseVariation(
                id = variationId,
                userId = userId,
                coreExerciseId = coreId,
                name = firestoreData["name"] as? String ?: return,
                equipment =
                    com.github.radupana.featherweight.data.exercise.Equipment.valueOf(
                        firestoreData["equipment"] as? String ?: "NONE",
                    ),
                difficulty =
                    com.github.radupana.featherweight.data.exercise.ExerciseDifficulty.valueOf(
                        firestoreData["difficulty"] as? String ?: "BEGINNER",
                    ),
                requiresWeight = firestoreData["requiresWeight"] as? Boolean ?: false,
                restDurationSeconds = (firestoreData["restDurationSeconds"] as? Long)?.toInt() ?: 90,
            )
        database.exerciseVariationDao().insertExerciseVariation(variation)
    }

    private suspend fun updateLocalExercise(
        variationId: String,
        firestoreData: Map<String, Any>,
    ) {
        val existingVariation = database.exerciseVariationDao().getExerciseVariationById(variationId) ?: return

        // Update variation with remote data
        val updatedVariation =
            existingVariation.copy(
                name = firestoreData["name"] as? String ?: existingVariation.name,
                equipment =
                    com.github.radupana.featherweight.data.exercise.Equipment.valueOf(
                        firestoreData["equipment"] as? String ?: existingVariation.equipment.name,
                    ),
                difficulty =
                    com.github.radupana.featherweight.data.exercise.ExerciseDifficulty.valueOf(
                        firestoreData["difficulty"] as? String ?: existingVariation.difficulty.name,
                    ),
                requiresWeight = firestoreData["requiresWeight"] as? Boolean ?: existingVariation.requiresWeight,
                restDurationSeconds = (firestoreData["restDurationSeconds"] as? Long)?.toInt() ?: existingVariation.restDurationSeconds,
            )
        database.exerciseVariationDao().updateVariation(updatedVariation)
    }

    private suspend fun uploadLocalChanges(
        userId: String,
        localVariations: List<ExerciseVariation>,
        lastSyncTime: Timestamp?,
    ) {
        localVariations.forEach { variation ->
            try {
                // Check if this variation is newer than last sync
                val variationTimestamp = Timestamp(variation.updatedAt.toEpochSecond(ZoneOffset.UTC), 0)
                if (lastSyncTime == null || variationTimestamp.seconds > lastSyncTime.seconds) {
                    // Get the core for this variation
                    val core = database.exerciseCoreDao().getCoreById(variation.coreExerciseId)
                    if (core != null) {
                        // Upload to Firestore
                        firestoreRepository.uploadCustomExercise(userId, variation, core)
                        Log.d(TAG, "Uploaded custom exercise: ${variation.name}")
                    }
                }
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Failed to upload custom exercise ${variation.id} - Firebase error", e)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to upload custom exercise ${variation.id} - network error", e)
            }
        }
    }

    override suspend fun uploadChanges(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (userId == null) {
                return@withContext Result.success(Unit)
            }

            try {
                val localVariations = database.exerciseVariationDao().getCustomVariationsByUser(userId)
                uploadLocalChanges(userId, localVariations, lastSyncTime)
                Result.success(Unit)
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Failed to upload custom exercises - Firebase error", e)
                Result.failure(e)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to upload custom exercises - database error", e)
                Result.failure(e)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to upload custom exercises - network error", e)
                Result.failure(e)
            }
        }

    override fun getDataType(): String = "CustomExercises"
}
