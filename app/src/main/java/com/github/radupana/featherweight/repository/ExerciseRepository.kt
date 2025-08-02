package com.github.radupana.featherweight.repository

import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
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

    suspend fun getAllExercises(): List<ExerciseWithDetails> =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllExercisesWithDetails()
        }

    suspend fun getAllExerciseNamesIncludingAliases(): List<String> =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercisesWithDetails()
            val allNames = mutableListOf<String>()

            exercises.forEach { exercise ->
                allNames.add(exercise.exercise.name)
                val aliases = exerciseDao.getAliasesForExercise(exercise.exercise.id)
                aliases.forEach { alias ->
                    allNames.add(alias.alias)
                }
            }

            allNames.distinct().sorted()
        }

    suspend fun getAllExerciseAliases() =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercisesWithDetails()
            val allAliases = mutableListOf<com.github.radupana.featherweight.data.exercise.ExerciseAlias>()

            exercises.forEach { exercise ->
                val aliases = exerciseDao.getAliasesForExercise(exercise.exercise.id)
                allAliases.addAll(aliases)
            }

            allAliases
        }

    suspend fun getAllExercisesWithUsageStats(): List<Pair<ExerciseWithDetails, Int>> =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercisesWithDetails()

            exercises
                .map { exercise ->
                    val usageCount = exerciseLogDao.getExerciseUsageCount(exercise.exercise.name)
                    Pair(exercise, usageCount)
                }.sortedWith(
                    compareByDescending<Pair<ExerciseWithDetails, Int>> { it.second }
                        .thenBy { it.first.exercise.name },
                )
        }

    suspend fun getExerciseById(id: Long): ExerciseWithDetails? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseWithDetails(id)
        }

    suspend fun getExerciseEntityById(exerciseId: Long): Exercise? = exerciseDao.getExerciseById(exerciseId)

    suspend fun getExerciseByName(name: String): Exercise? {
        // First try exact name match
        val exactMatch = exerciseDao.findExerciseByExactName(name)
        if (exactMatch != null) return exactMatch

        // Then try alias match
        return exerciseDao.findExerciseByAlias(name)
    }

    suspend fun searchExercises(query: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.searchExercises(query)
        }

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByCategory(category)
        }

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByMuscleGroup(muscleGroup)
        }

    suspend fun getExercisesByEquipment(equipment: Equipment): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByEquipment(equipment)
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

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long {
        return exerciseLogDao.insertExerciseLog(exerciseLog)
    }

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exerciseName: String,
        order: Int = 0,
    ): Long {
        val exercise =
            getExerciseByName(exerciseName)
                ?: throw IllegalArgumentException("Exercise '$exerciseName' not found in database")

        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = exerciseName,
                exerciseOrder = order,
            )
        return insertExerciseLog(exerciseLog)
    }

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: Long,
        exercise: ExerciseWithDetails,
        exerciseOrder: Int,
        notes: String? = null,
    ): Long {
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseName = exercise.exercise.name,
                exerciseId = exercise.exercise.id,
                exerciseOrder = exerciseOrder,
                notes = notes,
            )
        val id = exerciseLogDao.insertExerciseLog(exerciseLog)
        // Always increment usage count when adding exercise through this method
        exerciseDao.incrementUsageCount(exercise.exercise.id)
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

    suspend fun getExerciseDetailsForLog(exerciseLog: ExerciseLog): ExerciseWithDetails? =
        withContext(Dispatchers.IO) {
            val exercise = getExerciseByName(exerciseLog.exerciseName) ?: return@withContext null
            exerciseDao.getExerciseWithDetails(exercise.id)
        }

    // ===== EXERCISE STATS AND HISTORY =====

    suspend fun getExerciseStats(exerciseName: String): ExerciseStats? {
        return withContext(Dispatchers.IO) {
            val allWorkouts = db.workoutDao().getAllWorkouts()
            val allSetsForExercise = mutableListOf<SetLog>()

            // Only consider COMPLETED workouts for exercise stats
            for (workout in allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseName == exerciseName }
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
                exerciseName = exerciseName,
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

    suspend fun getLastPerformanceForExercise(exerciseName: String): ExercisePerformanceData? =
        withContext(Dispatchers.IO) {
            // Get all completed workouts ordered by date (newest first)
            val completedWorkouts =
                db.workoutDao().getAllWorkouts()
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .sortedByDescending { it.date }

            // Search through workouts for this exercise
            for (workout in completedWorkouts) {
                val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingLog = exerciseLogs.find { it.exerciseName == exerciseName }

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
                                exerciseName = exerciseName,
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


    suspend fun getPersonalRecords(exerciseName: String): List<Pair<Float, LocalDateTime>> =
        withContext(Dispatchers.IO) {
            println("🔵 ExerciseRepository.getPersonalRecords called for: $exerciseName")
            Log.d("Analytics", "=== getPersonalRecords for $exerciseName ===")
            val allWorkouts = db.workoutDao().getAllWorkouts()
            println("🔵 ExerciseRepository: Total workouts found: ${allWorkouts.size}")
            Log.d("Analytics", "Total workouts found: ${allWorkouts.size}")

            val records = mutableListOf<Pair<Float, LocalDateTime>>()
            var currentMaxWeight = 0f

            // Sort workouts by date to track progression over time
            // IMPORTANT: Only consider COMPLETED workouts for PR tracking
            val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
            Log.d("Analytics", "Completed workouts: ${completedWorkouts.size}")

            completedWorkouts
                .sortedBy { it.date }
                .forEach { workout ->
                    val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                    val matchingExercise = exercises.find { it.exerciseName == exerciseName }

                    if (matchingExercise != null) {
                        val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                        Log.d("Analytics", "Found ${sets.size} sets for $exerciseName in workout ${workout.id}")

                        // Log detailed set info
                        sets.forEach { set ->
                            Log.d(
                                "Analytics",
                                "  Set: targetReps=${set.targetReps}, targetWeight=${set.targetWeight}, actualReps=${set.actualReps}, actualWeight=${set.actualWeight}, isCompleted=${set.isCompleted}",
                            )
                        }

                        val maxWeightInWorkout =
                            sets
                                .filter { it.isCompleted && it.actualReps > 0 }
                                .maxOfOrNull { it.actualWeight } ?: 0f

                        Log.d("Analytics", "Max weight in workout: $maxWeightInWorkout (current max: $currentMaxWeight)")

                        if (maxWeightInWorkout > currentMaxWeight) {
                            currentMaxWeight = maxWeightInWorkout
                            records.add(Pair(maxWeightInWorkout, workout.date))
                            Log.d("Analytics", "New PR found: ${maxWeightInWorkout}kg on ${workout.date}")
                        }
                    }
                }

            println("🔵 ExerciseRepository: Total PR records found for $exerciseName: ${records.size}")
            Log.d("Analytics", "Total PR records found: ${records.size}")
            records.forEach { (weight, date) ->
                println("🔵   PR: ${weight}kg on $date")
            }
            records
        }

    // ===== EXERCISE SWAP OPERATIONS =====

    suspend fun swapExercise(
        exerciseLogId: Long,
        newExerciseId: Long,
        newExerciseName: String,
        originalExerciseId: Long,
    ) {
        val exerciseLog = exerciseLogDao.getExerciseLogById(exerciseLogId)
        if (exerciseLog != null) {
            val updatedLog =
                exerciseLog.copy(
                    exerciseId = newExerciseId,
                    exerciseName = newExerciseName,
                    originalExerciseId = originalExerciseId,
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

    suspend fun getSwapHistoryForExercise(userId: Long, exerciseId: Long): List<SwapHistoryCount> {
        return withContext(Dispatchers.IO) {
            exerciseSwapHistoryDao.getSwapHistoryForExercise(userId, exerciseId)
        }
    }
}
