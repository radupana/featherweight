package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
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

    private val oneRMDao = db.exerciseMaxTrackingDao()
    private val _pendingOneRMUpdates = MutableStateFlow<List<PendingOneRMUpdate>>(emptyList())
    val pendingOneRMUpdates = _pendingOneRMUpdates.asStateFlow()

    fun clearPendingOneRMUpdates() {
        _pendingOneRMUpdates.value = emptyList()
    }

    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        val userId = authManager.getCurrentUserId() ?: "local"
        val roundedWeight = WeightFormatter.roundToNearestQuarter(update.suggestedMax)
        val previousMax = oneRMDao.getCurrentMax(update.exerciseId, userId)?.oneRMEstimate
        val exercise = db.exerciseDao().getExerciseById(update.exerciseId)
        val exerciseName = exercise?.name ?: "Unknown"

        val currentMax = oneRMDao.getCurrentMax(update.exerciseId, userId)

        if (currentMax != null) {
            oneRMDao.update(
                currentMax.copy(
                    oneRMEstimate = roundedWeight,
                    mostWeightLifted = roundedWeight,
                    mostWeightReps = 1,
                    mostWeightDate = update.workoutDate ?: LocalDateTime.now(),
                    recordedAt = update.workoutDate ?: LocalDateTime.now(),
                    context = update.source,
                    notes = "Updated from ${update.source}",
                ),
            )
        } else {
            oneRMDao.insert(
                ExerciseMaxTracking(
                    userId = userId,
                    exerciseId = update.exerciseId,
                    oneRMEstimate = roundedWeight,
                    context = update.source,
                    sourceSetId = null,
                    recordedAt = update.workoutDate ?: LocalDateTime.now(),
                    mostWeightLifted = roundedWeight,
                    mostWeightReps = 1,
                    mostWeightRpe = null,
                    mostWeightDate = update.workoutDate ?: LocalDateTime.now(),
                    oneRMConfidence = update.confidence,
                    oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                    notes = "Updated from ${update.source}",
                ),
            )
        }

        Log.i(
            TAG,
            "1RM updated - exercise: $exerciseName, previous: ${previousMax ?: 0f}kg, " +
                "new: ${roundedWeight}kg, source: ${update.source}, " +
                "increase: ${roundedWeight - (previousMax ?: 0f)}kg",
        )

        _pendingOneRMUpdates.value =
            _pendingOneRMUpdates.value.filter {
                it.exerciseId != update.exerciseId
            }
    }

    suspend fun getCurrentMaxesForExercises(
        exerciseIds: List<String>,
    ): Map<String, Float> {
        val userId = authManager.getCurrentUserId() ?: "local"
        val maxes = oneRMDao.getCurrentMaxesForExercises(exerciseIds, userId)
        return maxes.associate { max ->
            max.exerciseId to max.oneRMEstimate
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

    suspend fun getOneRMForExercise(exerciseId: String): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        val exerciseMax = oneRMDao.getCurrentMax(exerciseId, userId)
        return exerciseMax?.oneRMEstimate
    }

    suspend fun deleteAllMaxesForExercise(exerciseId: String) {
        val userId = authManager.getCurrentUserId() ?: "local"
        oneRMDao.deleteAllForExercise(exerciseId, userId)
    }

    suspend fun updateOrInsertOneRM(oneRMRecord: ExerciseMaxTracking) =
        withContext(ioDispatcher) {
            val userId = authManager.getCurrentUserId() ?: "local"
            val roundedEstimate = WeightFormatter.roundToNearestQuarter(oneRMRecord.oneRMEstimate)
            val roundedRecord = oneRMRecord.copy(oneRMEstimate = roundedEstimate)

            val existing = oneRMDao.getCurrentMax(oneRMRecord.exerciseId, userId)
            val exercise = db.exerciseDao().getExerciseById(oneRMRecord.exerciseId)
            val exerciseName = exercise?.name ?: "Unknown"

            if (existing != null) {
                if (roundedEstimate > existing.oneRMEstimate) {
                    Log.i(
                        TAG,
                        "New 1RM PR! Exercise: $exerciseName, old: ${existing.oneRMEstimate}kg, new: ${roundedEstimate}kg",
                    )
                    oneRMDao.update(roundedRecord.copy(id = existing.id))
                } else {
                    Log.d(
                        TAG,
                        "1RM not updated (not a PR): $exerciseName, current: ${existing.oneRMEstimate}kg, attempted: ${roundedEstimate}kg",
                    )
                }
            } else {
                Log.i(
                    TAG,
                    "First 1RM recorded for $exerciseName: ${roundedEstimate}kg",
                )
                oneRMDao.insert(roundedRecord)
            }
        }

    suspend fun getCurrentOneRMEstimate(
        exerciseId: String,
    ): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        return oneRMDao.getCurrentOneRMEstimate(exerciseId, userId)
    }

    suspend fun getExercise1RM(
        exerciseId: String,
    ): Float? {
        val userId = authManager.getCurrentUserId() ?: "local"
        val exerciseMax = oneRMDao.getCurrentMax(exerciseId, userId)
        return exerciseMax?.oneRMEstimate
    }

    fun addPendingOneRMUpdate(update: PendingOneRMUpdate) {
        _pendingOneRMUpdates.value = _pendingOneRMUpdates.value.filter {
            it.exerciseId != update.exerciseId
        } + update

        Log.i(
            TAG,
            "Added pending 1RM update - exerciseId: ${update.exerciseId}, suggested: ${update.suggestedMax}kg, current: ${update.currentMax}kg",
        )
    }
}
