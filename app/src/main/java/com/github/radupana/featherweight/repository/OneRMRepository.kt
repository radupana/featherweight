package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val oneRMDao = db.oneRMDao()
    private val _pendingOneRMUpdates = MutableStateFlow<List<PendingOneRMUpdate>>(emptyList())
    val pendingOneRMUpdates = _pendingOneRMUpdates.asStateFlow()

    fun clearPendingOneRMUpdates() {
        _pendingOneRMUpdates.value = emptyList()
    }

    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        // Round weight to nearest 0.25kg
        val roundedWeight = WeightFormatter.roundToNearestQuarter(update.suggestedMax)

        // Update the max
        oneRMDao.upsertExerciseMax(
            exerciseVariationId = update.exerciseVariationId,
            maxWeight = roundedWeight,
            notes = "Updated from ${update.source}",
        )

        // Save to history
        saveOneRMToHistory(
            exerciseVariationId = update.exerciseVariationId,
            estimatedMax = roundedWeight,
            source = update.source,
            date = update.workoutDate ?: LocalDateTime.now(),
        )

        // Remove from pending
        _pendingOneRMUpdates.value =
            _pendingOneRMUpdates.value.filter {
                it.exerciseVariationId != update.exerciseVariationId
            }
    }

    suspend fun upsertExerciseMax(
        exerciseVariationId: Long,
        maxWeight: Float,
        notes: String? = null,
    ) {
        oneRMDao.upsertExerciseMax(exerciseVariationId, maxWeight, notes)
    }

    suspend fun getCurrentMaxesForExercises(
        exerciseIds: List<Long>,
    ): Map<Long, Float> {
        val maxes = oneRMDao.getCurrentMaxesForExercises(exerciseIds)
        return maxes.associate { max ->
            max.exerciseVariationId to (max.oneRMEstimate ?: 0f)
        }
    }

    suspend fun getBig4Exercises() = db.exerciseDao().getBig4Exercises()

    fun getAllCurrentMaxesWithNames(): Flow<List<OneRMWithExerciseName>> = oneRMDao.getAllCurrentMaxesWithNames()

    fun getBig4ExercisesWithMaxes(): Flow<List<Big4ExerciseWithOptionalMax>> = oneRMDao.getBig4ExercisesWithMaxes()

    fun getOtherExercisesWithMaxes(): Flow<List<OneRMWithExerciseName>> = oneRMDao.getOtherExercisesWithMaxes()

    suspend fun getOneRMForExercise(exerciseVariationId: Long): Float? {
        val exerciseMax = oneRMDao.getCurrentMax(exerciseVariationId)
        return exerciseMax?.oneRMEstimate
    }

    suspend fun deleteAllMaxesForExercise(exerciseId: Long) = oneRMDao.deleteAllMaxesForExercise(exerciseId)

    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) =
        withContext(ioDispatcher) {
            // Check if we have an existing record for this exercise
            val existing = oneRMDao.getCurrentMax(oneRMRecord.exerciseVariationId)

            val shouldSaveHistory =
                if (existing != null) {
                    // Update existing record if new estimate is higher
                    if (oneRMRecord.oneRMEstimate > existing.oneRMEstimate) {
                        oneRMDao.updateExerciseMax(oneRMRecord.copy(id = existing.id))
                        true
                    } else {
                        false
                    }
                } else {
                    // Insert new record
                    oneRMDao.insertExerciseMax(oneRMRecord)
                    true
                }

            // Save to history if we made a change
            if (shouldSaveHistory) {
                saveOneRMToHistory(
                    exerciseVariationId = oneRMRecord.exerciseVariationId,
                    estimatedMax = oneRMRecord.oneRMEstimate,
                    source = oneRMRecord.oneRMContext,
                )
            }
        }

    suspend fun getCurrentOneRMEstimate(
        exerciseVariationId: Long,
    ): Float? = oneRMDao.getCurrentOneRMEstimate(exerciseVariationId)

    suspend fun saveOneRMToHistory(
        exerciseVariationId: Long,
        estimatedMax: Float,
        source: String,
        date: LocalDateTime = LocalDateTime.now(),
    ) {
        val history =
            OneRMHistory(
                exerciseVariationId = exerciseVariationId,
                oneRMEstimate = estimatedMax,
                context = source,
                recordedAt = date,
            )
        oneRMDao.insertOneRMHistory(history)
    }

    suspend fun getOneRMHistoryForExercise(
        exerciseVariationId: Long,
        limit: Int = 10,
    ): List<OneRMHistory> = oneRMDao.getRecentOneRMHistory(exerciseVariationId, limit)

    suspend fun getExercise1RM(
        exerciseVariationId: Long,
    ): Float? {
        val exerciseMax = oneRMDao.getCurrentMax(exerciseVariationId)
        return exerciseMax?.oneRMEstimate
    }

    // Add pending update
    fun addPendingOneRMUpdate(update: PendingOneRMUpdate) {
        _pendingOneRMUpdates.value = _pendingOneRMUpdates.value.filter {
            it.exerciseVariationId != update.exerciseVariationId
        } + update
    }
}
