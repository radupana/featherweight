package com.github.radupana.featherweight.repository

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing One Rep Max (1RM) data and calculations
 */
class OneRMRepository(
    application: Application,
    private val authManager: AuthenticationManager = ServiceLocator.provideAuthenticationManager(application),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
) {
    companion object {
        private const val TAG = "OneRMRepository"
    }

    private val oneRMDao = db.oneRMDao()
    private val _pendingOneRMUpdates = MutableStateFlow<List<PendingOneRMUpdate>>(emptyList())
    val pendingOneRMUpdates = _pendingOneRMUpdates.asStateFlow()

    fun clearPendingOneRMUpdates() {
        _pendingOneRMUpdates.value = emptyList()
    }

    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        val userId = authManager.getCurrentUserId() ?: "local"
        val roundedWeight = WeightFormatter.roundToNearestQuarter(update.suggestedMax)
        val previousMax = oneRMDao.getCurrentMax(update.exerciseVariationId, userId)?.oneRMEstimate
        val exercise = db.exerciseDao().getExerciseVariationById(update.exerciseVariationId)
        val exerciseName = exercise?.name ?: "Unknown"

        // Use proper method to maintain isCustomExercise flag
        val currentMax = oneRMDao.getCurrentMax(update.exerciseVariationId, userId)

        if (currentMax != null) {
            // Update existing record, preserving isCustomExercise flag
            oneRMDao.updateExerciseMax(
                currentMax.copy(
                    oneRMEstimate = roundedWeight,
                    mostWeightLifted = roundedWeight,
                    mostWeightReps = 1,
                    mostWeightDate = update.workoutDate ?: LocalDateTime.now(),
                    oneRMDate = update.workoutDate ?: LocalDateTime.now(),
                    oneRMContext = update.source,
                    notes = "Updated from ${update.source}",
                ),
            )
        } else {
            // Insert new record with correct isCustomExercise flag
            oneRMDao.insertExerciseMax(
                UserExerciseMax(
                    userId = userId,
                    exerciseVariationId = update.exerciseVariationId,
                    mostWeightLifted = roundedWeight,
                    mostWeightReps = 1,
                    mostWeightDate = update.workoutDate ?: LocalDateTime.now(),
                    oneRMEstimate = roundedWeight,
                    oneRMContext = update.source,
                    oneRMConfidence = update.confidence,
                    oneRMDate = update.workoutDate ?: LocalDateTime.now(),
                    notes = "Updated from ${update.source}",
                ),
            )
        }
        saveOneRMToHistory(
            exerciseVariationId = update.exerciseVariationId,
            estimatedMax = roundedWeight,
            source = update.source,
            sourceSetId = null,
            date = update.workoutDate ?: LocalDateTime.now(),
            userId = userId,
        )

        Log.i(
            TAG,
            "1RM updated - exercise: $exerciseName, previous: ${previousMax ?: 0f}kg, " +
                "new: ${roundedWeight}kg, source: ${update.source}, " +
                "increase: ${roundedWeight - (previousMax ?: 0f)}kg",
        )

        _pendingOneRMUpdates.value =
            _pendingOneRMUpdates.value.filter {
                it.exerciseVariationId != update.exerciseVariationId
            }
    }

    suspend fun getCurrentMaxesForExercises(
        exerciseIds: List<String>,
    ): Map<String, Float> {
        val userId = authManager.getCurrentUserId() ?: "local"
        val maxes = oneRMDao.getCurrentMaxesForExercises(exerciseIds, userId)
        return maxes.associate { max ->
            max.exerciseVariationId to (max.oneRMEstimate ?: 0f)
        }
    }

    fun getAllCurrentMaxesWithNames(): Flow<List<OneRMWithExerciseName>> {
        val userId = authManager.getCurrentUserId() ?: "local"
        return oneRMDao.getAllCurrentMaxesWithNames(userId)
    }

    fun getBig4ExercisesWithMaxes(): Flow<List<Big4ExerciseWithOptionalMax>> {
        val userId = authManager.getCurrentUserId() ?: "local"
        return oneRMDao.getBig4ExercisesWithMaxes(userId)
    }

    fun getOtherExercisesWithMaxes(): Flow<List<OneRMWithExerciseName>> {
        val userId = authManager.getCurrentUserId() ?: "local"
        return oneRMDao.getOtherExercisesWithMaxes(userId)
    }

    suspend fun getOneRMForExercise(exerciseVariationId: String): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        val exerciseMax = oneRMDao.getCurrentMax(exerciseVariationId, userId)
        return exerciseMax?.oneRMEstimate
    }

    suspend fun deleteAllMaxesForExercise(exerciseId: String) {
        val userId = authManager.getCurrentUserId() ?: "local"
        oneRMDao.deleteAllMaxesForExercise(exerciseId, userId)
    }

    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) =
        withContext(ioDispatcher) {
            val userId = authManager.getCurrentUserId() ?: "local"
            val roundedEstimate = WeightFormatter.roundToNearestQuarter(oneRMRecord.oneRMEstimate)
            val roundedRecord = oneRMRecord.copy(oneRMEstimate = roundedEstimate)

            val existing = oneRMDao.getCurrentMax(oneRMRecord.exerciseVariationId, userId)
            val exercise = db.exerciseDao().getExerciseVariationById(oneRMRecord.exerciseVariationId)
            val exerciseName = exercise?.name ?: "Unknown"

            val shouldSaveHistory =
                if (existing != null) {
                    if (roundedEstimate > existing.oneRMEstimate) {
                        Log.i(
                            TAG,
                            "New 1RM PR! Exercise: $exerciseName, old: ${existing.oneRMEstimate}kg, new: ${roundedEstimate}kg",
                        )
                        oneRMDao.updateExerciseMax(roundedRecord.copy(id = existing.id))
                        true
                    } else {
                        Log.d(
                            TAG,
                            "1RM not updated (not a PR): $exerciseName, current: ${existing.oneRMEstimate}kg, attempted: ${roundedEstimate}kg",
                        )
                        false
                    }
                } else {
                    Log.i(
                        TAG,
                        "First 1RM recorded for $exerciseName: ${roundedEstimate}kg",
                    )
                    oneRMDao.insertExerciseMax(roundedRecord)
                    true
                }

            if (shouldSaveHistory) {
                saveOneRMToHistory(
                    exerciseVariationId = oneRMRecord.exerciseVariationId,
                    estimatedMax = roundedEstimate,
                    source = oneRMRecord.oneRMContext,
                    sourceSetId = null,
                    userId = oneRMRecord.userId,
                )
            }
        }

    suspend fun getCurrentOneRMEstimate(
        exerciseVariationId: String,
    ): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        return oneRMDao.getCurrentOneRMEstimate(exerciseVariationId, userId)
    }

    suspend fun saveOneRMToHistory(
        exerciseVariationId: String,
        estimatedMax: Float,
        source: String,
        sourceSetId: String? = null,
        date: LocalDateTime = LocalDateTime.now(),
        userId: String? = null,
    ) {
        val roundedMax = WeightFormatter.roundToNearestQuarter(estimatedMax)
        val actualUserId = userId ?: authManager.getCurrentUserId() ?: "local"

        val history =
            OneRMHistory(
                userId = actualUserId,
                exerciseVariationId = exerciseVariationId,
                oneRMEstimate = roundedMax,
                context = source,
                sourceSetId = sourceSetId,
                recordedAt = date,
            )

        try {
            oneRMDao.insertOneRMHistory(history)
            Log.i(
                TAG,
                "Saved 1RM to history - exerciseId: $exerciseVariationId, max: ${roundedMax}kg, source: $source, sourceSetId: $sourceSetId",
            )
        } catch (e: SQLiteConstraintException) {
            Log.d(
                TAG,
                "1RM history already exists for sourceSetId: $sourceSetId - ${e.message}",
            )
        }
    }

    suspend fun getExercise1RM(
        exerciseVariationId: String,
    ): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        val exerciseMax = oneRMDao.getCurrentMax(exerciseVariationId, userId)
        return exerciseMax?.oneRMEstimate
    }

    fun addPendingOneRMUpdate(update: PendingOneRMUpdate) {
        _pendingOneRMUpdates.value = _pendingOneRMUpdates.value.filter {
            it.exerciseVariationId != update.exerciseVariationId
        } + update

        Log.i(
            TAG,
            "Added pending 1RM update - exerciseId: ${update.exerciseVariationId}, suggested: ${update.suggestedMax}kg, current: ${update.currentMax}kg",
        )
    }
}
