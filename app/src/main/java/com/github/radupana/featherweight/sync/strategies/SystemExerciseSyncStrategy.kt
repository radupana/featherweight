package com.github.radupana.featherweight.sync.strategies

import androidx.room.withTransaction
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.sync.SyncStrategy
import com.github.radupana.featherweight.sync.converters.ExerciseSyncConverter
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sync strategy for system exercise reference data.
 * Downloads from denormalized Firestore collection and stores in normalized SQLite tables.
 */
class SystemExerciseSyncStrategy(
    private val database: FeatherweightDatabase,
    private val firestoreRepository: FirestoreRepository,
) : SyncStrategy {
    companion object {
        private const val TAG = "SystemExerciseSync"
    }

    override suspend fun downloadAndMerge(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                CloudLogger.debug(TAG, "Starting system exercise sync")

                val remoteExercises = firestoreRepository.downloadSystemExercises(lastSyncTime).getOrThrow()
                CloudLogger.debug(TAG, "Downloaded ${remoteExercises.size} exercises from Firestore")

                if (remoteExercises.isEmpty()) {
                    CloudLogger.debug(TAG, "No exercises to sync")
                    return@withContext Result.success(Unit)
                }

                val bundles =
                    remoteExercises.mapNotNull { (exerciseId, firestoreExercise) ->
                        try {
                            ExerciseSyncConverter.fromFirestore(firestoreExercise, exerciseId)
                        } catch (e: IllegalArgumentException) {
                            CloudLogger.error(TAG, "Failed to convert exercise $exerciseId", e)
                            null
                        }
                    }

                val exerciseIds = bundles.map { it.exercise.id }
                val exercises = bundles.map { it.exercise }
                val muscles = bundles.flatMap { it.exerciseMuscles }
                val aliases = bundles.flatMap { it.exerciseAliases }
                val instructions = bundles.flatMap { it.exerciseInstructions }

                database.withTransaction {
                    database.exerciseMuscleDao().deleteForExercises(exerciseIds)
                    database.exerciseAliasDao().deleteForExercises(exerciseIds)
                    database.exerciseInstructionDao().deleteForExercises(exerciseIds)

                    database.exerciseDao().upsertExercises(exercises)
                    if (muscles.isNotEmpty()) {
                        database.exerciseMuscleDao().insertExerciseMuscles(muscles)
                    }
                    if (aliases.isNotEmpty()) {
                        database.exerciseAliasDao().insertAliases(aliases)
                    }
                    if (instructions.isNotEmpty()) {
                        database.exerciseInstructionDao().insertInstructions(instructions)
                    }
                }

                CloudLogger.debug(TAG, "System exercise sync completed successfully")
                Result.success(Unit)
            } catch (e: com.google.firebase.FirebaseException) {
                CloudLogger.error(TAG, "Failed to sync system exercises - Firebase error", e)
                Result.failure(e)
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to sync system exercises - database error", e)
                Result.failure(e)
            } catch (e: java.io.IOException) {
                CloudLogger.error(TAG, "Failed to sync system exercises - network error", e)
                Result.failure(e)
            }
        }

    override suspend fun uploadChanges(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit> {
        // System exercises are read-only from the app's perspective
        // Only admin tools can modify system exercises
        CloudLogger.debug(TAG, "System exercises are read-only, skipping upload")
        return Result.success(Unit)
    }

    override fun getDataType(): String = "SystemExercises"
}
