package com.github.radupana.featherweight.repository

import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.programme.ExercisePerformanceData
import com.github.radupana.featherweight.domain.ExerciseStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ExerciseRepository(
    private val db: FeatherweightDatabase,
) {
    private val exerciseDao = db.exerciseDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseSwapHistoryDao = db.exerciseSwapHistoryDao()

    // ===== EXERCISE QUERIES =====

    suspend fun getAllExercises(): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllExercises()
        }

    suspend fun getAllExerciseNamesIncludingAliases(): List<String> =
        withContext(Dispatchers.IO) {
            val variations = exerciseDao.getAllExercises()
            val allNames = mutableListOf<String>()

            variations.forEach { variation ->
                allNames.add(variation.name)
                val aliases = exerciseDao.getAliasesForVariation(variation.id)
                aliases.forEach { alias ->
                    allNames.add(alias.alias)
                }
            }

            allNames.distinct().sorted()
        }

    suspend fun getAllExerciseAliases() =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllAliases()
        }

    suspend fun getAllExercisesWithUsageStats(): List<Pair<ExerciseVariation, Int>> =
        withContext(Dispatchers.IO) {
            val variations = exerciseDao.getAllExercises()

            variations
                .map { variation ->
                    val usageCount = exerciseLogDao.getExerciseUsageCount(variation.id)
                    Pair(variation, usageCount)
                }.sortedWith(
                    compareByDescending<Pair<ExerciseVariation, Int>> { it.second }
                        .thenBy { it.first.name },
                )
        }

    suspend fun getExerciseById(id: Long): ExerciseVariation? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseVariationById(id)
        }

    suspend fun getExerciseEntityById(exerciseVariationId: Long): ExerciseVariation? = exerciseDao.getExerciseVariationById(exerciseVariationId)

    suspend fun getExerciseByName(name: String): ExerciseVariation? {
        // First try exact name match
        val exactMatch = exerciseDao.findVariationByExactName(name)
        if (exactMatch != null) return exactMatch

        // Then try alias match
        return exerciseDao.findVariationByAlias(name)
    }

    suspend fun searchExercises(query: String): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.searchVariations(query)
        }

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.getVariationsByCategory(category)
        }

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.getVariationsByMuscleGroup(muscleGroup)
        }

    suspend fun getExercisesByEquipment(equipment: Equipment): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.getVariationsByEquipment(equipment)
        }

    suspend fun getBig4Exercises() =
        withContext(Dispatchers.IO) {
            val exerciseNames = listOf("Barbell Back Squat", "Deadlift", "Barbell Bench Press", "Overhead Press")
            exerciseNames.mapNotNull { name ->
                getExerciseByName(name)
            }
        }

    // ===== EXERCISE LOG OPERATIONS =====

    suspend fun getExercisesForWorkout(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: Long): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exerciseVariationId: Long,
        order: Int = 0,
    ): Long {
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseVariationId = exerciseVariationId,
                exerciseOrder = order,
            )
        return insertExerciseLog(exerciseLog)
    }

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exerciseVariation: ExerciseVariation,
        exerciseOrder: Int,
        notes: String? = null,
    ): Long {
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseVariationId = exerciseVariation.id,
                exerciseOrder = exerciseOrder,
                notes = notes,
            )
        val id = exerciseLogDao.insertExerciseLog(exerciseLog)
        // Always increment usage count when adding exercise through this method
        exerciseDao.incrementUsageCount(exerciseVariation.id)
        return id
    }

    suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.update(exerciseLog)

    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseLogDao.deleteExerciseLog(exerciseLogId)

    suspend fun deleteSetsForExercise(exerciseLogId: Long) = setLogDao.deleteAllSetsForExercise(exerciseLogId)

    suspend fun deleteSetsForExerciseLog(exerciseLogId: Long) = setLogDao.deleteAllSetsForExercise(exerciseLogId)

    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    ) = exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)

    suspend fun updateExerciseOrder(
        workoutId: Long,
        exerciseOrders: Map<Long, Int>,
    ) {
        withContext(Dispatchers.IO) {
            exerciseOrders.forEach { (exerciseLogId, newOrder) ->
                exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)
            }
        }
    }

    suspend fun getExerciseDetailsForLog(exerciseLog: ExerciseLog): ExerciseVariation? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseVariationById(exerciseLog.exerciseVariationId)
        }

    // ===== EXERCISE STATS AND HISTORY =====

    suspend fun getExerciseStats(exerciseVariationId: Long): ExerciseStats? {
        return withContext(Dispatchers.IO) {
            val variation = exerciseDao.getExerciseVariationById(exerciseVariationId) ?: return@withContext null
            val allWorkouts = db.workoutDao().getAllWorkouts()
            val allSetsForExercise = mutableListOf<SetLog>()

            // Only consider COMPLETED workouts for exercise stats
            for (workout in allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseVariationId == exerciseVariationId }
                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                    allSetsForExercise.addAll(sets)
                }
            }

            if (allSetsForExercise.isEmpty()) return@withContext null

            val completedSets =
                allSetsForExercise.filter {
                    it.isCompleted && it.actualWeight > 0 && it.actualReps > 0
                }

            if (completedSets.isEmpty()) return@withContext null

            ExerciseStats(
                exerciseName = variation.name,
                avgWeight = completedSets.map { it.actualWeight }.average().toFloat(),
                avgReps = completedSets.map { it.actualReps }.average().toInt(),
                avgRpe =
                    completedSets
                        .mapNotNull { it.actualRpe }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.toFloat(),
                maxWeight = completedSets.maxOf { it.actualWeight },
                totalSets = completedSets.size,
            )
        }
    }

    suspend fun getLastPerformanceForExercise(exerciseVariationId: Long): ExercisePerformanceData? =
        withContext(Dispatchers.IO) {
            val variation = exerciseDao.getExerciseVariationById(exerciseVariationId) ?: return@withContext null
            // Get all completed workouts ordered by date (newest first)
            val completedWorkouts =
                db
                    .workoutDao()
                    .getAllWorkouts()
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.date }

            // Search through workouts for this exercise
            for (workout in completedWorkouts) {
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingLog = exerciseLogs.find { it.exerciseVariationId == exerciseVariationId }

                if (matchingLog != null) {
                    // Get sets for this exercise
                    val sets = setLogDao.getSetLogsForExercise(matchingLog.id)
                    if (sets.isNotEmpty()) {
                        // Get the heaviest working set
                        val workingSets = sets
                        val heaviestSet = workingSets.maxByOrNull { it.actualWeight }

                        if (heaviestSet != null) {
                            val allCompleted = workingSets.all { it.isCompleted }

                            return@withContext ExercisePerformanceData(
                                exerciseName = variation.name,
                                weight = heaviestSet.actualWeight,
                                reps = heaviestSet.actualReps,
                                sets = workingSets.size,
                                workoutDate = workout.date,
                                allSetsCompleted = allCompleted,
                            )
                        }
                    }
                }
            }

            null
        }

    suspend fun getPersonalRecords(exerciseVariationId: Long): List<Pair<Float, LocalDateTime>> =
        withContext(Dispatchers.IO) {
            val variation = exerciseDao.getExerciseVariationById(exerciseVariationId)
            val exerciseName = variation?.name ?: "Unknown"
            val allWorkouts = db.workoutDao().getAllWorkouts()

            val records = mutableListOf<Pair<Float, LocalDateTime>>()
            var currentMaxWeight = 0f

            // Sort workouts by date to track progression over time
            // IMPORTANT: Only consider COMPLETED workouts for PR tracking
            val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }

            completedWorkouts
                .sortedBy { it.date }
                .forEach { workout ->
                    val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                    val matchingExercise = exercises.find { it.exerciseVariationId == exerciseVariationId }

                    if (matchingExercise != null) {
                        val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)

                        val maxWeightInWorkout =
                            sets
                                .filter { it.isCompleted && it.actualReps > 0 }
                                .maxOfOrNull { it.actualWeight } ?: 0f

                        if (maxWeightInWorkout > currentMaxWeight) {
                            currentMaxWeight = maxWeightInWorkout
                            records.add(Pair(maxWeightInWorkout, workout.date))
                        }
                    }
                }

            records
        }

    // ===== EXERCISE SWAP OPERATIONS =====

    suspend fun swapExercise(
        exerciseLogId: Long,
        newExerciseVariationId: Long,
        originalExerciseVariationId: Long,
    ) {
        val exerciseLog = exerciseLogDao.getExerciseLogById(exerciseLogId)
        if (exerciseLog != null) {
            val updatedLog =
                exerciseLog.copy(
                    exerciseVariationId = newExerciseVariationId,
                    originalVariationId = originalExerciseVariationId,
                    isSwapped = true,
                )
            exerciseLogDao.update(updatedLog)
        }
    }

    suspend fun recordExerciseSwap(
        userId: Long,
        originalExerciseId: Long,
        swappedToExerciseId: Long,
        workoutId: Long? = null,
        programmeId: Long? = null,
    ) {
        val swapHistory =
            ExerciseSwapHistory(
                userId = userId,
                originalExerciseId = originalExerciseId,
                swappedToExerciseId = swappedToExerciseId,
                swapDate = LocalDateTime.now(),
                workoutId = workoutId,
                programmeId = programmeId,
            )
        exerciseSwapHistoryDao.insert(swapHistory)
    }

    suspend fun getSwapHistoryForExercise(
        userId: Long,
        exerciseId: Long,
    ): List<SwapHistoryCount> =
        withContext(Dispatchers.IO) {
            exerciseSwapHistoryDao.getSwapHistoryForExercise(userId, exerciseId)
        }
}
