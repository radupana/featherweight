package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.service.OneRMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing 1RM (one-rep max) calculations and storage.
 * Handles 1RM estimates, history, and pending updates.
 */
class OneRMRepository(
    private val db: FeatherweightDatabase,
    private val oneRMDao: OneRMDao,
    private val oneRMService: OneRMService,
) {
    private val _pendingOneRMUpdates = MutableStateFlow<List<PendingOneRMUpdate>>(emptyList())
    val pendingOneRMUpdates: StateFlow<List<PendingOneRMUpdate>> = _pendingOneRMUpdates.asStateFlow()

    /**
     * Clears all pending 1RM updates
     */
    fun clearPendingOneRMUpdates() {
        _pendingOneRMUpdates.value = emptyList()
    }

    /**
     * Applies a pending 1RM update
     */
    suspend fun applyOneRMUpdate(update: PendingOneRMUpdate) {
        withContext(Dispatchers.IO) {
            // Exercise variation check removed since not used
            val exerciseVariation = db.exerciseDao().getExerciseVariationById(update.exerciseVariationId)
            if (exerciseVariation == null) return@withContext

            val currentMax = oneRMDao.getCurrentMax(update.exerciseVariationId)
            
            if (currentMax != null) {
                // Update existing max
                val updatedMax = currentMax.copy(
                    mostWeightLifted = update.suggestedMax,
                    oneRMEstimate = update.suggestedMax,
                    oneRMContext = update.source,
                    oneRMDate = LocalDateTime.now(),
                )
                oneRMDao.updateExerciseMax(updatedMax)
            } else {
                // Insert new max
                val newMax = UserExerciseMax(
                    exerciseVariationId = update.exerciseVariationId,
                    mostWeightLifted = update.suggestedMax,
                    mostWeightReps = 1,
                    oneRMEstimate = update.suggestedMax,
                    oneRMContext = update.source,
                    oneRMConfidence = update.confidence,
                    oneRMDate = update.workoutDate ?: LocalDateTime.now(),
                )
                oneRMDao.insertExerciseMax(newMax)
            }

            // Save to history
            val history = OneRMHistory(
                exerciseVariationId = update.exerciseVariationId,
                oneRMEstimate = update.suggestedMax,
                context = update.source,
                recordedAt = update.workoutDate ?: LocalDateTime.now(),
            )
            oneRMDao.insertOneRMHistory(history)

            // Remove from pending updates (filtering by all fields since there's no id)
            _pendingOneRMUpdates.value = _pendingOneRMUpdates.value.filter { 
                it.exerciseVariationId != update.exerciseVariationId ||
                it.suggestedMax != update.suggestedMax
            }
        }
    }

    /**
     * Gets the current 1RM for an exercise
     */
    suspend fun getOneRMForExercise(exerciseVariationId: Long): Float? =
        withContext(Dispatchers.IO) {
            oneRMDao.getCurrentOneRMEstimate(exerciseVariationId)
        }

    /**
     * Gets estimated 1RM based on recent performance
     */
    suspend fun getEstimated1RM(exerciseVariationId: Long): Float? =
        withContext(Dispatchers.IO) {
            val lastSet = db.setLogDao().getLastCompletedSetForExercise(exerciseVariationId)
                ?: return@withContext null
            
            oneRMService.calculateEstimated1RM(lastSet.actualWeight, lastSet.actualReps)
        }

    /**
     * Calculates current 1RM estimate from a set
     */
    suspend fun getCurrentOneRMEstimate(weight: Float, reps: Int): Float? =
        oneRMService.calculateEstimated1RM(weight, reps)

    /**
     * Updates or inserts a 1RM record
     */
    suspend fun updateOrInsertOneRM(oneRMRecord: UserExerciseMax) {
        withContext(Dispatchers.IO) {
            val existing = oneRMDao.getCurrentMax(oneRMRecord.exerciseVariationId)
            
            if (existing != null) {
                val updated = existing.copy(
                    mostWeightLifted = oneRMRecord.mostWeightLifted,
                    mostWeightReps = oneRMRecord.mostWeightReps,
                    mostWeightRpe = oneRMRecord.mostWeightRpe,
                    mostWeightDate = oneRMRecord.mostWeightDate,
                    oneRMEstimate = oneRMRecord.oneRMEstimate,
                    oneRMContext = oneRMRecord.oneRMContext,
                    oneRMConfidence = oneRMRecord.oneRMConfidence,
                    oneRMDate = oneRMRecord.oneRMDate,
                    oneRMType = oneRMRecord.oneRMType,
                    notes = oneRMRecord.notes,
                )
                oneRMDao.updateExerciseMax(updated)
            } else {
                oneRMDao.insertExerciseMax(oneRMRecord)
            }

            // Also save to history
            saveOneRMToHistory(
                exerciseVariationId = oneRMRecord.exerciseVariationId,
                oneRMEstimate = oneRMRecord.oneRMEstimate,
                context = oneRMRecord.oneRMContext,
            )
        }
    }

    /**
     * Saves 1RM to history
     */
    suspend fun saveOneRMToHistory(
        exerciseVariationId: Long,
        oneRMEstimate: Float,
        context: String,
    ): Long {
        val history = OneRMHistory(
            exerciseVariationId = exerciseVariationId,
            oneRMEstimate = oneRMEstimate,
            context = context,
        )
        return oneRMDao.insertOneRMHistory(history)
    }

    /**
     * Gets 1RM history for an exercise
     */
    suspend fun getOneRMHistoryForExercise(
        exerciseVariationId: Long,
        limit: Int? = null,
    ): List<OneRMHistory> =
        withContext(Dispatchers.IO) {
            if (limit != null) {
                oneRMDao.getRecentOneRMHistory(exerciseVariationId, limit)
            } else {
                // Get all history in the last year
                val endDate = LocalDateTime.now()
                val startDate = endDate.minusYears(1)
                oneRMDao.getOneRMHistoryInRange(exerciseVariationId, startDate, endDate)
            }
        }

    /**
     * Gets current maxes for multiple exercises
     */
    suspend fun getCurrentMaxesForExercises(exerciseIds: List<Long>): Map<Long, Float> =
        withContext(Dispatchers.IO) {
            val maxes = oneRMDao.getCurrentMaxesForExercises(exerciseIds)
            exerciseIds.associateWith { exerciseId ->
                maxes.find { it.exerciseVariationId == exerciseId }?.oneRMEstimate ?: 0f
            }
        }

    /**
     * Gets all current maxes with exercise names
     */
    fun getAllCurrentMaxesWithNames(): Flow<List<OneRMWithExerciseName>> =
        oneRMDao.getAllCurrentMaxesWithNames()

    /**
     * Gets big 4 exercises (Squat, Bench, Deadlift, OHP)
     */
    suspend fun getBig4Exercises() =
        withContext(Dispatchers.IO) {
            db.exerciseDao().getBig4Exercises()
        }

    /**
     * Gets big 4 exercises with their maxes
     */
    fun getBig4ExercisesWithMaxes(): Flow<List<Big4ExerciseWithOptionalMax>> = 
        oneRMDao.getBig4ExercisesWithMaxes()

    /**
     * Gets other exercises with maxes (non-big 4)
     */
    fun getOtherExercisesWithMaxes(): Flow<List<OneRMWithExerciseName>> =
        oneRMDao.getOtherExercisesWithMaxes()

    /**
     * Deletes all maxes for an exercise
     */
    suspend fun deleteAllMaxesForExercise(exerciseId: Long) =
        withContext(Dispatchers.IO) {
            oneRMDao.deleteAllMaxesForExercise(exerciseId)
        }

    /**
     * Estimates 1RM for a set and checks if it's a new max
     */
    suspend fun checkForNew1RM(
        exerciseVariationId: Long,
        setLog: SetLog,
    ): PendingOneRMUpdate? {
        return withContext(Dispatchers.IO) {
            val estimated1RM = oneRMService.calculateEstimated1RM(setLog.actualWeight, setLog.actualReps)
                ?: return@withContext null

            val currentMax = getOneRMForExercise(exerciseVariationId) ?: 0f
            
            if (estimated1RM > currentMax * 1.05f) { // Only suggest if > 5% improvement
                val confidence = when {
                    setLog.actualReps <= 3 -> 0.95f
                    setLog.actualReps <= 5 -> 0.90f
                    setLog.actualReps <= 8 -> 0.85f
                    else -> 0.75f
                }
                
                val update = PendingOneRMUpdate(
                    exerciseVariationId = exerciseVariationId,
                    currentMax = if (currentMax > 0) currentMax else null,
                    suggestedMax = estimated1RM,
                    confidence = confidence,
                    source = "${setLog.actualWeight}kg × ${setLog.actualReps}${setLog.actualRpe?.let { " @ RPE $it" } ?: ""}",
                    workoutDate = LocalDateTime.now(),
                )
                
                // Add to pending updates
                _pendingOneRMUpdates.value = _pendingOneRMUpdates.value + update
                
                update
            } else {
                null
            }
        }
    }

    /**
     * Gets the best historical 1RM for an exercise
     */
    suspend fun getBestHistorical1RM(exerciseVariationId: Long): Float? =
        withContext(Dispatchers.IO) {
            val history = getOneRMHistoryForExercise(exerciseVariationId)
            history.maxOfOrNull { it.oneRMEstimate }
        }

    /**
     * Upserts a 1RM using the dao's upsert method
     */
    suspend fun upsertExerciseMax(
        exerciseVariationId: Long,
        maxWeight: Float,
        notes: String? = null,
    ) {
        oneRMDao.upsertExerciseMax(exerciseVariationId, maxWeight, notes)
    }
}
