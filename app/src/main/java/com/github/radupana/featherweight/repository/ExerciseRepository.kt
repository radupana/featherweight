package com.github.radupana.featherweight.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.domain.ExerciseStats
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.service.ExerciseNamingService
import com.github.radupana.featherweight.service.ValidationResult
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ExerciseRepository(
    private val db: FeatherweightDatabase,
    private val authManager: AuthenticationManager,
) {
    companion object {
        private const val TAG = "ExerciseRepository"
    }

    private val exerciseDao = db.exerciseDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseSwapHistoryDao = db.exerciseSwapHistoryDao()
    private val exerciseAliasDao = db.exerciseAliasDao()
    private val exerciseMuscleDao = db.exerciseMuscleDao()
    private val userExerciseUsageDao = db.userExerciseUsageDao()

    // ===== EXERCISE QUERIES =====

    suspend fun getAllExercises(): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllExercises()
        }

    suspend fun getAllExercisesWithAliases(): List<ExerciseWithAliases> =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercises()
            exercises.map { exercise ->
                val aliases = exerciseAliasDao.getAliasesForExercise(exercise.id)
                ExerciseWithAliases(
                    exercise = exercise,
                    aliases = aliases.map { it.alias },
                )
            }
        }

    suspend fun getAllExerciseNamesIncludingAliases(): List<String> =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getAllExercises()
            val allNames = mutableListOf<String>()

            exercises.forEach { exercise ->
                allNames.add(exercise.name)
                val aliases = exerciseAliasDao.getAliasesForExercise(exercise.id)
                aliases.forEach { alias ->
                    allNames.add(alias.alias)
                }
            }

            allNames.distinct().sorted()
        }

    suspend fun getAllExerciseAliases() =
        withContext(Dispatchers.IO) {
            exerciseAliasDao.getAllAliases()
        }

    suspend fun getAllExercisesWithUsageStats(): List<Pair<Exercise, Int>> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val exercises = exerciseDao.getAllExercises()
            val userId = authManager?.getCurrentUserId()

            val result =
                exercises
                    .map { exercise ->
                        val usageCount =
                            if (userId != null) {
                                // Get user-specific usage count from UserExerciseUsage
                                userExerciseUsageDao
                                    .getUsage(
                                        userId = userId,
                                        exerciseId = exercise.id,
                                    )?.usageCount ?: 0
                            } else {
                                // Fallback to global count if no user is logged in
                                exerciseLogDao.getExerciseUsageCount(exercise.id, "local")
                            }
                        Pair(exercise, usageCount)
                    }.sortedWith(
                        compareByDescending<Pair<Exercise, Int>> { it.second }
                            .thenBy { it.first.name },
                    )

            val elapsedTime = System.currentTimeMillis() - startTime
            CloudLogger.debug(
                TAG,
                "getAllExercisesWithUsageStats completed in ${elapsedTime}ms - " +
                    "totalExercises: ${exercises.size}, " +
                    "topExercise: ${result.firstOrNull()?.first?.name ?: "none"}, " +
                    "topUsageCount: ${result.firstOrNull()?.second ?: 0}",
            )
            result
        }

    suspend fun getExerciseById(id: String): Exercise? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseById(id)
        }

    suspend fun getExerciseEntityById(exerciseId: String): Exercise? = exerciseDao.getExerciseById(exerciseId)

    suspend fun findExerciseByName(name: String): Exercise? = exerciseDao.findExerciseByName(name)

    suspend fun findSystemExerciseByName(name: String): Exercise? = exerciseDao.findSystemExerciseByName(name)

    suspend fun getExerciseByName(name: String): Exercise? {
        CloudLogger.info(TAG, "Searching for exercise: '$name'")
        // First try exact name match
        val exactMatch = exerciseDao.findExerciseByName(name)
        if (exactMatch != null) {
            CloudLogger.info(TAG, "Found exact match: ${exactMatch.name} (id: ${exactMatch.id})")
            return exactMatch
        }

        // Then try alias match
        val aliasMatch = exerciseAliasDao.findExerciseByAlias(name)
        if (aliasMatch != null) {
            CloudLogger.debug(TAG, "Found via alias: ${aliasMatch.name} (id: ${aliasMatch.id}) for query: '$name'")
        } else {
            CloudLogger.warn(TAG, "Exercise not found: '$name'")
        }
        return aliasMatch
    }

    suspend fun searchExercises(query: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            val trace = safeNewTrace("exercise_search")
            trace?.start()
            trace?.putAttribute("query_length", query.length.toString())
            val results = exerciseDao.searchExercises(query)
            trace?.putAttribute("result_count", results.size.toString())
            trace?.stop()
            results
        }

    // Suppress TooGenericExceptionCaught: This is a safe wrapper that must handle ALL exceptions
    // from Firebase Performance initialization, including RuntimeException from unmocked Android
    // methods in test environments. The method is explicitly designed to never throw.
    @Suppress("TooGenericExceptionCaught")
    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: Throwable) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance not available: ${e.javaClass.simpleName}", e)
            null
        }

    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByCategory(category.name)
        }

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseMuscleDao.getExercisesByMuscleGroup(muscleGroup)
        }

    suspend fun getExercisesByEquipment(equipment: Equipment): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesByEquipment(equipment.name)
        }

    // ===== EXERCISE LOG OPERATIONS =====

    suspend fun getExercisesForWorkout(workoutId: String): List<ExerciseLog> = exerciseLogDao.getExerciseLogsForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseLogId: String): List<SetLog> = setLogDao.getSetLogsForExercise(exerciseLogId)

    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): String {
        val exerciseWithUserId = exerciseLog.copy(userId = authManager.getCurrentUserId() ?: "local")
        exerciseLogDao.insertExerciseLog(exerciseWithUserId)
        return exerciseWithUserId.id
    }

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: String,
        exerciseId: String,
        order: Int = 0,
    ): String {
        val exerciseLog =
            ExerciseLog(
                userId = authManager?.getCurrentUserId() ?: "local",
                workoutId = workoutId,
                exerciseId = exerciseId,
                exerciseOrder = order,
            )

        return insertExerciseLog(exerciseLog)
    }

    suspend fun insertExerciseLogWithExerciseReference(
        workoutId: String,
        exerciseVariation: Exercise,
        exerciseOrder: Int,
        notes: String? = null,
    ): String {
        val exerciseLog =
            ExerciseLog(
                userId = authManager?.getCurrentUserId() ?: "local",
                workoutId = workoutId,
                exerciseId = exerciseVariation.id,
                exerciseOrder = exerciseOrder,
                notes = notes,
            )
        exerciseLogDao.insertExerciseLog(exerciseLog)
        CloudLogger.info(TAG, "Added exercise to workout - exercise: ${exerciseVariation.name}, workoutId: $workoutId, order: $exerciseOrder, exerciseLogId: ${exerciseLog.id}")
        return exerciseLog.id
    }

    suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.update(exerciseLog)

    suspend fun deleteExerciseLog(exerciseLogId: String) = exerciseLogDao.deleteExerciseLog(exerciseLogId)

    suspend fun deleteSetsForExercise(exerciseLogId: String) = setLogDao.deleteAllSetsForExercise(exerciseLogId)

    suspend fun updateExerciseOrder(
        exerciseLogId: String,
        newOrder: Int,
    ) = exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)

    suspend fun updateExerciseOrder(
        exerciseOrders: Map<String, Int>,
    ) {
        withContext(Dispatchers.IO) {
            exerciseOrders.forEach { (exerciseLogId, newOrder) ->
                exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)
            }
        }
    }

    suspend fun getExerciseDetailsForLog(exerciseLog: ExerciseLog): Exercise? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseById(exerciseLog.exerciseId)
        }

    // ===== EXERCISE STATS AND HISTORY =====

    suspend fun getExerciseStats(exerciseId: String): ExerciseStats? {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val userId = authManager.getCurrentUserId() ?: "local"
            val exercise = exerciseDao.getExerciseById(exerciseId) ?: return@withContext null
            val allWorkouts = db.workoutDao().getAllWorkouts(userId)
            val allSetsForExercise = mutableListOf<SetLog>()

            // Only consider COMPLETED workouts for exercise stats
            val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
            for (workout in completedWorkouts) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseId == exerciseId }
                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                    allSetsForExercise.addAll(sets)
                }
            }

            if (allSetsForExercise.isEmpty()) {
                CloudLogger.debug(TAG, "No sets found for exercise: ${exercise.name}")
                return@withContext null
            }

            val completedSets =
                allSetsForExercise.filter {
                    it.isCompleted && it.actualWeight > 0 && it.actualReps > 0
                }

            if (completedSets.isEmpty()) {
                CloudLogger.debug(TAG, "No completed sets found for exercise: ${exercise.name}")
                return@withContext null
            }

            val stats =
                ExerciseStats(
                    exerciseName = exercise.name,
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

            val elapsedTime = System.currentTimeMillis() - startTime
            CloudLogger.debug(
                TAG,
                "getExerciseStats completed in ${elapsedTime}ms - " +
                    "exercise: ${exercise.name}, " +
                    "workoutsAnalyzed: ${completedWorkouts.size}, " +
                    "setsFound: ${allSetsForExercise.size}, " +
                    "completedSets: ${completedSets.size}, " +
                    "maxWeight: ${stats.maxWeight}, " +
                    "avgWeight: ${stats.avgWeight}",
            )
            stats
        }
    }

    suspend fun swapExercise(
        exerciseLogId: String,
        newExerciseId: String,
        originalExerciseId: String,
    ) {
        val exerciseLog = exerciseLogDao.getExerciseLogById(exerciseLogId)
        if (exerciseLog != null) {
            val updatedLog =
                exerciseLog.copy(
                    exerciseId = newExerciseId,
                    originalExerciseId = originalExerciseId,
                    isSwapped = true,
                )
            exerciseLogDao.update(updatedLog)

            val originalExercise = exerciseDao.getExerciseById(originalExerciseId)
            val newExercise = exerciseDao.getExerciseById(newExerciseId)
            CloudLogger.info(
                TAG,
                "USER_ACTION: exercise_swap - " +
                    "original: ${originalExercise?.name ?: "unknown"}, " +
                    "swappedTo: ${newExercise?.name ?: "unknown"}, " +
                    "exerciseLogId: $exerciseLogId",
            )
        }
    }

    suspend fun recordExerciseSwap(
        originalExerciseId: String,
        swappedToExerciseId: String,
        workoutId: String? = null,
        programmeId: String? = null,
    ) {
        val userId = authManager?.getCurrentUserId() ?: "local"

        // Check if an identical swap already exists
        val existingSwap =
            exerciseSwapHistoryDao.getExistingSwap(
                userId = userId,
                originalExerciseId = originalExerciseId,
                swappedToExerciseId = swappedToExerciseId,
                workoutId = workoutId,
            )

        // Only insert if no duplicate exists
        if (existingSwap == null) {
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
    }

    suspend fun getSwapHistoryForExercise(
        exerciseId: String,
    ): List<SwapHistoryCount> =
        withContext(Dispatchers.IO) {
            exerciseSwapHistoryDao.getSwapHistoryForExercise(exerciseId)
        }

    // ===== CREATE CUSTOM EXERCISE =====

    @Suppress("LongParameterList")
    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        equipment: Equipment,
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true,
        movementPattern: MovementPattern = MovementPattern.PUSH,
    ): Result<Exercise> =
        withContext(Dispatchers.IO) {
            try {
                CloudLogger.info(TAG, "Creating custom exercise: $name, category: $category, equipment: $equipment")

                // Get all existing exercise names and aliases for duplicate checking
                val existingNames = mutableListOf<String>()

                // Add all exercise names
                val exercises = exerciseDao.getAllExercises()
                existingNames.addAll(exercises.map { it.name })

                // Add all aliases
                val aliases = exerciseAliasDao.getAllAliases()
                existingNames.addAll(aliases.map { it.alias })

                // Validate name with duplicate check
                val namingService = ExerciseNamingService()
                val validationResult = namingService.validateExerciseNameWithDuplicateCheck(name, existingNames)

                if (validationResult is ValidationResult.Invalid) {
                    CloudLogger.warn(TAG, "Custom exercise creation failed - ${validationResult.reason}: $name")
                    val errorMessage =
                        if (validationResult.suggestion != null) {
                            "${validationResult.reason}. ${validationResult.suggestion}"
                        } else {
                            validationResult.reason
                        }
                    return@withContext Result.failure(
                        IllegalArgumentException(errorMessage),
                    )
                }

                // Create exercise with all fields (no separate core)
                val newExercise =
                    Exercise(
                        name = name,
                        category = category.name,
                        movementPattern = movementPattern?.name,
                        isCompound = true, // Most custom exercises are compound
                        equipment = equipment.name,
                        difficulty = difficulty?.name,
                        requiresWeight = requiresWeight,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    )
                exerciseDao.insertExercise(newExercise)
                val exerciseId = newExercise.id

                // Create muscle relationships
                val allMuscles = mutableListOf<ExerciseMuscle>()

                // Add primary muscles
                primaryMuscles.forEach { muscle ->
                    allMuscles.add(
                        ExerciseMuscle(
                            exerciseId = exerciseId,
                            muscle = muscle.name,
                            targetType = "PRIMARY",
                        ),
                    )
                }

                // Add secondary muscles
                secondaryMuscles.forEach { muscle ->
                    allMuscles.add(
                        ExerciseMuscle(
                            exerciseId = exerciseId,
                            muscle = muscle.name,
                            targetType = "SECONDARY",
                        ),
                    )
                }

                // Insert all muscle relationships
                if (allMuscles.isNotEmpty()) {
                    exerciseMuscleDao.insertExerciseMuscles(allMuscles)
                }

                // Return the created exercise
                val createdExercise = exerciseDao.getExerciseById(exerciseId)
                if (createdExercise != null) {
                    CloudLogger.info(
                        TAG,
                        "USER_ACTION: custom_exercise_created - " +
                            "name: $name, " +
                            "category: ${category.name}, " +
                            "equipment: ${equipment.name}, " +
                            "primaryMuscles: ${primaryMuscles.joinToString(", ") { it.name }}, " +
                            "secondaryMuscles: ${secondaryMuscles.joinToString(", ") { it.name }}, " +
                            "exerciseId: $exerciseId",
                    )
                    Result.success(createdExercise)
                } else {
                    CloudLogger.error(TAG, "Failed to retrieve created custom exercise: $name")
                    Result.failure(IllegalStateException("Failed to retrieve created exercise"))
                }
            } catch (e: SQLiteException) {
                CloudLogger.error(TAG, "SQLite error creating custom exercise: $name", e)
                Result.failure(e)
            } catch (e: IllegalArgumentException) {
                CloudLogger.error(TAG, "Invalid argument creating custom exercise: $name", e)
                Result.failure(e)
            }
        }

    // ===== DELETE CUSTOM EXERCISE =====

    suspend fun canDeleteExercise(exerciseId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // Check if it's a custom exercise
                exerciseDao.getExerciseById(exerciseId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Exercise not found"))

                // System exercises cannot be deleted
                return@withContext Result.failure(
                    IllegalArgumentException("System exercises cannot be deleted. Only custom exercises can be removed."),
                )

                // Check if used in any completed workouts
                val userId = authManager.getCurrentUserId() ?: "local"
                val completedWorkouts =
                    db
                        .workoutDao()
                        .getAllWorkouts(userId)
                        .filter { it.status == WorkoutStatus.COMPLETED }

                for (workout in completedWorkouts) {
                    val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                    if (exerciseLogs.any { it.exerciseId == exerciseId }) {
                        return@withContext Result.failure(
                            IllegalStateException("Cannot delete exercise: Used in completed workouts"),
                        )
                    }
                }

                Result.success(true)
            } catch (e: SQLiteException) {
                Result.failure(e)
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }

    suspend fun deleteCustomExercise(exerciseId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val exercise = exerciseDao.getExerciseById(exerciseId)
                val exerciseName = exercise?.name ?: "Unknown"

                CloudLogger.info(TAG, "Attempting to delete custom exercise: $exerciseName (id: $exerciseId)")

                // First check if we can delete it
                val canDelete = canDeleteExercise(exerciseId)
                if (canDelete.isFailure) {
                    CloudLogger.warn(TAG, "Cannot delete exercise: $exerciseName - ${canDelete.exceptionOrNull()?.message}")
                    return@withContext Result.failure(canDelete.exceptionOrNull() ?: Exception("Cannot delete exercise"))
                }

                // Use a transaction to ensure all deletions happen atomically
                db.withTransaction {
                    // First, remove from ALL workouts (not just in-progress)
                    // We need to remove from ALL non-completed workouts to prevent foreign key issues
                    val userId = authManager.getCurrentUserId() ?: "local"
                    val allWorkouts = db.workoutDao().getAllWorkouts(userId)

                    for (workout in allWorkouts.filter { it.status != WorkoutStatus.COMPLETED }) {
                        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                        val logsToDelete = exerciseLogs.filter { it.exerciseId == exerciseId }

                        for (log in logsToDelete) {
                            // Delete all sets for this exercise log FIRST
                            setLogDao.deleteAllSetsForExercise(log.id)
                            // Then delete the exercise log itself
                            exerciseLogDao.deleteExerciseLog(log.id)
                        }
                    }

                    // Now delete all muscle associations
                    exerciseMuscleDao.deleteMuscleMappingsForExercise(exerciseId)

                    // Finally delete the exercise exercise itself
                    exerciseDao.deleteExercise(exerciseId)

                    // Verify deletion
                    val stillExists = exerciseDao.getExerciseById(exerciseId)
                    check(stillExists == null) { "Exercise was not deleted from database!" }
                }

                CloudLogger.info(
                    TAG,
                    "USER_ACTION: custom_exercise_deleted - " +
                        "name: $exerciseName, exerciseId: $exerciseId",
                )
                Result.success(Unit)
            } catch (e: SQLiteException) {
                CloudLogger.error(TAG, "SQLite error deleting custom exercise id: $exerciseId", e)
                Result.failure(e)
            } catch (e: IllegalStateException) {
                CloudLogger.error(TAG, "State error deleting custom exercise id: $exerciseId", e)
                Result.failure(e)
            }
        }
}
