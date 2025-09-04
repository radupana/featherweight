package com.github.radupana.featherweight.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.withTransaction
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SwapHistoryCount
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationWithAliases
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.domain.ExerciseStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ExerciseRepository(
    private val db: FeatherweightDatabase,
) {
    companion object {
        private const val TAG = "ExerciseRepository"
    }

    private val exerciseDao = db.exerciseDao()
    private val exerciseLogDao = db.exerciseLogDao()
    private val setLogDao = db.setLogDao()
    private val exerciseSwapHistoryDao = db.exerciseSwapHistoryDao()
    private val exerciseCoreDao = db.exerciseCoreDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val variationMuscleDao = db.variationMuscleDao()

    // ===== EXERCISE QUERIES =====

    suspend fun getAllExercises(): List<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            exerciseDao.getAllExercises()
        }

    suspend fun getAllExercisesWithAliases(): List<ExerciseVariationWithAliases> =
        withContext(Dispatchers.IO) {
            val variations = exerciseDao.getAllExercises()
            variations.map { variation ->
                val aliases = exerciseDao.getAliasesForVariation(variation.id)
                ExerciseVariationWithAliases(
                    variation = variation,
                    aliases = aliases.map { it.alias },
                )
            }
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
            val startTime = System.currentTimeMillis()
            val variations = exerciseDao.getAllExercises()

            val result =
                variations
                    .map { variation ->
                        val usageCount = exerciseLogDao.getExerciseUsageCount(variation.id)
                        Pair(variation, usageCount)
                    }.sortedWith(
                        compareByDescending<Pair<ExerciseVariation, Int>> { it.second }
                            .thenBy { it.first.name },
                    )

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.d(
                TAG,
                "getAllExercisesWithUsageStats completed in ${elapsedTime}ms - " +
                    "totalExercises: ${variations.size}, " +
                    "topExercise: ${result.firstOrNull()?.first?.name ?: "none"}, " +
                    "topUsageCount: ${result.firstOrNull()?.second ?: 0}",
            )
            result
        }

    suspend fun getExerciseById(id: Long): ExerciseVariation? =
        withContext(Dispatchers.IO) {
            exerciseDao.getExerciseVariationById(id)
        }

    suspend fun getExerciseEntityById(exerciseVariationId: Long): ExerciseVariation? = exerciseDao.getExerciseVariationById(exerciseVariationId)

    suspend fun getExerciseByName(name: String): ExerciseVariation? {
        Log.i(TAG, "Searching for exercise: '$name'")
        // First try exact name match
        val exactMatch = exerciseDao.findVariationByExactName(name)
        if (exactMatch != null) {
            Log.i(TAG, "Found exact match: ${exactMatch.name} (id: ${exactMatch.id})")
            return exactMatch
        }

        // Then try alias match
        val aliasMatch = exerciseDao.findVariationByAlias(name)
        if (aliasMatch != null) {
            Log.d(TAG, "Found via alias: ${aliasMatch.name} (id: ${aliasMatch.id}) for query: '$name'")
        } else {
            Log.w(TAG, "Exercise not found: '$name'")
        }
        return aliasMatch
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

        Log.i(TAG, "Added exercise to workout - exercise: ${exerciseVariation.name}, workoutId: $workoutId, order: $exerciseOrder, exerciseLogId: $id")
        return id
    }

    suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.update(exerciseLog)

    suspend fun deleteExerciseLog(exerciseLogId: Long) = exerciseLogDao.deleteExerciseLog(exerciseLogId)

    suspend fun deleteSetsForExercise(exerciseLogId: Long) = setLogDao.deleteAllSetsForExercise(exerciseLogId)

    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    ) = exerciseLogDao.updateExerciseOrder(exerciseLogId, newOrder)

    suspend fun updateExerciseOrder(
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
            val startTime = System.currentTimeMillis()
            val variation = exerciseDao.getExerciseVariationById(exerciseVariationId) ?: return@withContext null
            val allWorkouts = db.workoutDao().getAllWorkouts()
            val allSetsForExercise = mutableListOf<SetLog>()

            // Only consider COMPLETED workouts for exercise stats
            val completedWorkouts = allWorkouts.filter { it.status == WorkoutStatus.COMPLETED }
            for (workout in completedWorkouts) {
                val exercises = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                val matchingExercise = exercises.find { it.exerciseVariationId == exerciseVariationId }
                if (matchingExercise != null) {
                    val sets = setLogDao.getSetLogsForExercise(matchingExercise.id)
                    allSetsForExercise.addAll(sets)
                }
            }

            if (allSetsForExercise.isEmpty()) {
                Log.d(TAG, "No sets found for exercise: ${variation.name}")
                return@withContext null
            }

            val completedSets =
                allSetsForExercise.filter {
                    it.isCompleted && it.actualWeight > 0 && it.actualReps > 0
                }

            if (completedSets.isEmpty()) {
                Log.d(TAG, "No completed sets found for exercise: ${variation.name}")
                return@withContext null
            }

            val stats =
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

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.d(
                TAG,
                "getExerciseStats completed in ${elapsedTime}ms - " +
                    "exercise: ${variation.name}, " +
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

            val originalExercise = exerciseDao.getExerciseVariationById(originalExerciseVariationId)
            val newExercise = exerciseDao.getExerciseVariationById(newExerciseVariationId)
            Log.i(
                TAG,
                "USER_ACTION: exercise_swap - " +
                    "original: ${originalExercise?.name ?: "unknown"}, " +
                    "swappedTo: ${newExercise?.name ?: "unknown"}, " +
                    "exerciseLogId: $exerciseLogId",
            )
        }
    }

    suspend fun recordExerciseSwap(
        originalExerciseId: Long,
        swappedToExerciseId: Long,
        workoutId: Long? = null,
        programmeId: Long? = null,
    ) {
        val swapHistory =
            ExerciseSwapHistory(
                originalExerciseId = originalExerciseId,
                swappedToExerciseId = swappedToExerciseId,
                swapDate = LocalDateTime.now(),
                workoutId = workoutId,
                programmeId = programmeId,
            )
        exerciseSwapHistoryDao.insert(swapHistory)
    }

    suspend fun getSwapHistoryForExercise(
        exerciseId: Long,
    ): List<SwapHistoryCount> =
        withContext(Dispatchers.IO) {
            exerciseSwapHistoryDao.getSwapHistoryForExercise(exerciseId)
        }

    // ===== CREATE CUSTOM EXERCISE =====

    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup> = emptySet(),
        equipment: Equipment,
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true,
        movementPattern: MovementPattern = MovementPattern.PUSH,
    ): Result<ExerciseVariation> =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Creating custom exercise: $name, category: $category, equipment: $equipment")

                // Check for duplicate names (case-insensitive)
                val existingExercise = exerciseVariationDao.findVariationByName(name)
                if (existingExercise != null) {
                    Log.w(TAG, "Custom exercise creation failed - duplicate name: $name")
                    return@withContext Result.failure(
                        IllegalArgumentException("An exercise with this name already exists: ${existingExercise.name}"),
                    )
                }

                // Find or create ExerciseCore based on category and movement pattern
                val coreName = inferCoreName(name, category)
                var exerciseCore = exerciseCoreDao.getExerciseCoreByName(coreName)

                if (exerciseCore == null) {
                    // Create new core
                    val coreId =
                        exerciseCoreDao.insertExerciseCore(
                            ExerciseCore(
                                name = coreName,
                                category = category,
                                movementPattern = movementPattern,
                                isCompound = true, // Most custom exercises are compound
                                createdAt = LocalDateTime.now(),
                                updatedAt = LocalDateTime.now(),
                            ),
                        )
                    exerciseCore = exerciseCoreDao.getExerciseCoreById(coreId)
                }

                if (exerciseCore == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Failed to create or find exercise core"),
                    )
                }

                // Create the exercise variation
                val variationId =
                    exerciseVariationDao.insertExerciseVariation(
                        ExerciseVariation(
                            coreExerciseId = exerciseCore.id,
                            name = name,
                            equipment = equipment,
                            difficulty = difficulty,
                            requiresWeight = requiresWeight,
                            usageCount = 0,
                            isCustom = true,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now(),
                        ),
                    )

                // Create muscle relationships
                val allMuscles = mutableListOf<VariationMuscle>()

                // Add primary muscles
                primaryMuscles.forEach { muscle ->
                    allMuscles.add(
                        VariationMuscle(
                            variationId = variationId,
                            muscle = muscle,
                            isPrimary = true,
                            emphasisModifier = 1.0f,
                        ),
                    )
                }

                // Add secondary muscles
                secondaryMuscles.forEach { muscle ->
                    allMuscles.add(
                        VariationMuscle(
                            variationId = variationId,
                            muscle = muscle,
                            isPrimary = false,
                            emphasisModifier = 0.5f,
                        ),
                    )
                }

                // Insert all muscle relationships
                if (allMuscles.isNotEmpty()) {
                    variationMuscleDao.insertVariationMuscles(allMuscles)
                }

                // Return the created exercise
                val createdExercise = exerciseVariationDao.getExerciseVariationById(variationId)
                if (createdExercise != null) {
                    Log.i(
                        TAG,
                        "USER_ACTION: custom_exercise_created - " +
                            "name: $name, " +
                            "category: ${category.name}, " +
                            "equipment: ${equipment.name}, " +
                            "primaryMuscles: ${primaryMuscles.joinToString(", ") { it.name }}, " +
                            "secondaryMuscles: ${secondaryMuscles.joinToString(", ") { it.name }}, " +
                            "exerciseId: $variationId",
                    )
                    Result.success(createdExercise)
                } else {
                    Log.e(TAG, "Failed to retrieve created custom exercise: $name")
                    Result.failure(IllegalStateException("Failed to retrieve created exercise"))
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLite error creating custom exercise: $name", e)
                Result.failure(e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument creating custom exercise: $name", e)
                Result.failure(e)
            }
        }

    // ===== DELETE CUSTOM EXERCISE =====

    suspend fun canDeleteExercise(exerciseVariationId: Long): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // Check if it's a custom exercise
                val exercise =
                    exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                        ?: return@withContext Result.failure(IllegalArgumentException("Exercise not found"))

                if (!exercise.isCustom) {
                    return@withContext Result.failure(IllegalArgumentException("Cannot delete built-in exercises"))
                }

                // Check if used in any completed workouts
                val completedWorkouts =
                    db
                        .workoutDao()
                        .getAllWorkouts()
                        .filter { it.status == WorkoutStatus.COMPLETED }

                for (workout in completedWorkouts) {
                    val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                    if (exerciseLogs.any { it.exerciseVariationId == exerciseVariationId }) {
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

    suspend fun deleteCustomExercise(exerciseVariationId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                val exerciseName = exercise?.name ?: "Unknown"

                Log.i(TAG, "Attempting to delete custom exercise: $exerciseName (id: $exerciseVariationId)")

                // First check if we can delete it
                val canDelete = canDeleteExercise(exerciseVariationId)
                if (canDelete.isFailure) {
                    Log.w(TAG, "Cannot delete exercise: $exerciseName - ${canDelete.exceptionOrNull()?.message}")
                    return@withContext Result.failure(canDelete.exceptionOrNull() ?: Exception("Cannot delete exercise"))
                }

                // Use a transaction to ensure all deletions happen atomically
                db.withTransaction {
                    // First, remove from ALL workouts (not just in-progress)
                    // We need to remove from ALL non-completed workouts to prevent foreign key issues
                    val allWorkouts = db.workoutDao().getAllWorkouts()

                    for (workout in allWorkouts.filter { it.status != WorkoutStatus.COMPLETED }) {
                        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)
                        val logsToDelete = exerciseLogs.filter { it.exerciseVariationId == exerciseVariationId }

                        for (log in logsToDelete) {
                            // Delete all sets for this exercise log FIRST
                            setLogDao.deleteAllSetsForExercise(log.id)
                            // Then delete the exercise log itself
                            exerciseLogDao.deleteExerciseLog(log.id)
                        }
                    }

                    // Now delete all muscle associations
                    variationMuscleDao.deleteMuscleMappingsForVariation(exerciseVariationId)

                    // Finally delete the exercise variation itself
                    exerciseVariationDao.deleteExerciseVariation(exerciseVariationId)

                    // Verify deletion
                    val stillExists = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                    check(stillExists == null) { "Exercise was not deleted from database!" }
                }

                Log.i(
                    TAG,
                    "USER_ACTION: custom_exercise_deleted - " +
                        "name: $exerciseName, exerciseId: $exerciseVariationId",
                )
                Result.success(Unit)
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLite error deleting custom exercise id: $exerciseVariationId", e)
                Result.failure(e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "State error deleting custom exercise id: $exerciseVariationId", e)
                Result.failure(e)
            }
        }

    private fun inferCoreName(
        name: String,
        category: ExerciseCategory,
    ): String {
        // Try to infer a core name from the exercise name
        val lowerName = name.lowercase()

        // Common core exercise patterns
        return when {
            lowerName.contains("squat") -> "Squat"
            lowerName.contains("deadlift") -> "Deadlift"
            lowerName.contains("bench") && lowerName.contains("press") -> "Bench Press"
            lowerName.contains("overhead") && lowerName.contains("press") -> "Overhead Press"
            lowerName.contains("row") -> "Row"
            lowerName.contains("pulldown") -> "Pulldown"
            lowerName.contains("pull up") || lowerName.contains("pullup") -> "Pull Up"
            lowerName.contains("chin up") || lowerName.contains("chinup") -> "Chin Up"
            lowerName.contains("curl") && lowerName.contains("bicep") -> "Bicep Curl"
            lowerName.contains("tricep") && lowerName.contains("extension") -> "Tricep Extension"
            lowerName.contains("fly") || lowerName.contains("flye") -> "Fly"
            lowerName.contains("raise") && lowerName.contains("lateral") -> "Lateral Raise"
            lowerName.contains("raise") && lowerName.contains("front") -> "Front Raise"
            lowerName.contains("lunge") -> "Lunge"
            lowerName.contains("plank") -> "Plank"
            lowerName.contains("crunch") -> "Crunch"
            else -> {
                // Use category as fallback
                when (category) {
                    ExerciseCategory.CHEST -> "Chest Exercise"
                    ExerciseCategory.BACK -> "Back Exercise"
                    ExerciseCategory.SHOULDERS -> "Shoulder Exercise"
                    ExerciseCategory.ARMS -> "Arm Exercise"
                    ExerciseCategory.LEGS -> "Leg Exercise"
                    ExerciseCategory.CORE -> "Core Exercise"
                    ExerciseCategory.CARDIO -> "Cardio Exercise"
                    ExerciseCategory.FULL_BODY -> "Full Body Exercise"
                }
            }
        }
    }
}
