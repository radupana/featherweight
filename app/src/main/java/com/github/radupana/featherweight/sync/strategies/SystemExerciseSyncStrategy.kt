package com.github.radupana.featherweight.sync.strategies

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.sync.SyncStrategy
import com.github.radupana.featherweight.sync.converters.ExerciseSyncConverter
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
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
                Log.d(TAG, "Starting system exercise sync")

                // Download all exercises from denormalized collection
                val remoteExercises = firestoreRepository.downloadSystemExercises(lastSyncTime).getOrThrow()
                Log.d(TAG, "Downloaded ${remoteExercises.size} exercises from Firestore")

                // Process each exercise
                remoteExercises.forEach { (exerciseId, firestoreExercise) ->
                    processExercise(exerciseId, firestoreExercise)
                }

                Log.d(TAG, "System exercise sync completed successfully")
                Result.success(Unit)
            } catch (e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Failed to sync system exercises - Firebase error", e)
                Result.failure(e)
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e(TAG, "Failed to sync system exercises - database error", e)
                Result.failure(e)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to sync system exercises - network error", e)
                Result.failure(e)
            }
        }

    private suspend fun processExercise(
        exerciseId: String,
        firestoreExercise: FirestoreExercise,
    ) {
        try {
            // Convert denormalized Firestore data to normalized SQLite entities
            val bundle = ExerciseSyncConverter.fromFirestore(firestoreExercise, exerciseId)

            // Insert or update ExerciseCore
            val existingCore = database.exerciseCoreDao().getCoreById(bundle.exerciseCore.id)
            if (existingCore == null) {
                database.exerciseCoreDao().insertCore(bundle.exerciseCore)
            } else if (existingCore.updatedAt < bundle.exerciseCore.updatedAt) {
                // Update if remote is newer
                database.exerciseCoreDao().updateCore(bundle.exerciseCore)
            }

            // Insert or update ExerciseVariation
            val existingVariation = database.exerciseVariationDao().getExerciseVariationById(bundle.exerciseVariation.id)
            if (existingVariation == null) {
                database.exerciseVariationDao().insertExerciseVariation(bundle.exerciseVariation)
            } else if (existingVariation.updatedAt < bundle.exerciseVariation.updatedAt) {
                database.exerciseVariationDao().updateVariation(bundle.exerciseVariation)
            }

            // Clear and re-insert related data (muscles, aliases, instructions)
            // This ensures we have the latest data without complex merge logic

            // Muscles
            database.variationMuscleDao().deleteForVariation(bundle.exerciseVariation.id)
            if (bundle.variationMuscles.isNotEmpty()) {
                database.variationMuscleDao().insertVariationMuscles(bundle.variationMuscles)
            }

            // Aliases
            database.variationAliasDao().deleteForVariation(bundle.exerciseVariation.id)
            if (bundle.variationAliases.isNotEmpty()) {
                database.variationAliasDao().insertAliases(bundle.variationAliases)
            }

            // Instructions
            database.variationInstructionDao().deleteForVariation(bundle.exerciseVariation.id)
            if (bundle.variationInstructions.isNotEmpty()) {
                database.variationInstructionDao().insertInstructions(bundle.variationInstructions)
            }

            Log.v(TAG, "Processed exercise: ${bundle.exerciseVariation.name}")
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e(TAG, "Failed to process exercise $exerciseId - Firebase error", e)
            // Don't fail the whole sync for one bad exercise
        } catch (e: android.database.sqlite.SQLiteException) {
            Log.e(TAG, "Failed to process exercise $exerciseId - database error", e)
            // Don't fail the whole sync for one bad exercise
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to process exercise $exerciseId - invalid state", e)
            // Don't fail the whole sync for one bad exercise
        }
    }

    override suspend fun uploadChanges(
        userId: String?,
        lastSyncTime: Timestamp?,
    ): Result<Unit> {
        // System exercises are read-only from the app's perspective
        // Only admin tools can modify system exercises
        Log.d(TAG, "System exercises are read-only, skipping upload")
        return Result.success(Unit)
    }

    override fun getDataType(): String = "SystemExercises"
}
