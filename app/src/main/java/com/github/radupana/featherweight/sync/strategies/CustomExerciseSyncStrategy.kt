package com.github.radupana.featherweight.sync.strategies

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.Exercise
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
                val localVariations = database.exerciseDao().getCustomExercisesByUser(userId)
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
        exerciseId: String,
        firestoreData: Map<String, Any>,
    ) {
        val category = firestoreData["category"] as? String ?: return
        val movementPattern = firestoreData["movementPattern"] as? String ?: return
        val isCompound = firestoreData["isCompound"] as? Boolean ?: false

        val exercise =
            Exercise(
                id = exerciseId,
                userId = userId,
                name = firestoreData["name"] as? String ?: return,
                category = category,
                movementPattern = movementPattern,
                isCompound = isCompound,
                equipment = firestoreData["equipment"] as? String ?: "NONE",
                difficulty = firestoreData["difficulty"] as? String ?: "BEGINNER",
                requiresWeight = firestoreData["requiresWeight"] as? Boolean ?: false,
                restDurationSeconds = (firestoreData["restDurationSeconds"] as? Long)?.toInt() ?: 90,
            )
        database.exerciseDao().insertExercise(exercise)
    }

    private suspend fun updateLocalExercise(
        exerciseId: String,
        firestoreData: Map<String, Any>,
    ) {
        val existingVariation = database.exerciseDao().getExerciseById(exerciseId) ?: return

        // Update variation with remote data
        val updatedVariation =
            existingVariation.copy(
                name = firestoreData["name"] as? String ?: existingVariation.name,
                equipment = firestoreData["equipment"] as? String ?: existingVariation.equipment,
                difficulty = firestoreData["difficulty"] as? String ?: existingVariation.difficulty,
                requiresWeight = firestoreData["requiresWeight"] as? Boolean ?: existingVariation.requiresWeight,
                restDurationSeconds = (firestoreData["restDurationSeconds"] as? Long)?.toInt() ?: existingVariation.restDurationSeconds,
            )
        database.exerciseDao().updateExercise(updatedVariation)
    }

    private suspend fun uploadLocalChanges(
        userId: String,
        localVariations: List<Exercise>,
        lastSyncTime: Timestamp?,
    ) {
        localVariations.forEach { exercise ->
            try {
                // Check if this exercise is newer than last sync
                val timestamp = Timestamp(exercise.updatedAt.toEpochSecond(ZoneOffset.UTC), 0)
                if (lastSyncTime == null || timestamp.seconds > lastSyncTime.seconds) {
                    firestoreRepository.uploadCustomExercise(userId, exercise)
                    Log.d(TAG, "Uploaded custom exercise: ${exercise.name}")
                }
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Failed to upload custom exercise ${exercise.id} - Firebase error", e)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to upload custom exercise ${exercise.id} - network error", e)
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
                val localVariations = database.exerciseDao().getCustomExercisesByUser(userId)
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
