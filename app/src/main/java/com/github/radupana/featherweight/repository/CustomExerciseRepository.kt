package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing custom exercises created by users.
 */
class CustomExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val exerciseAliasDao: ExerciseAliasDao,
    private val userExerciseUsageDao: UserExerciseUsageDao,
    private val authManager: AuthenticationManager?,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
) {
    companion object {
        private const val TAG = "CustomExerciseRepo"
    }

    /**
     * Create a new custom exercise for the current user.
     * Returns the created variation ID or null if creation failed.
     */
    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        equipment: Equipment = Equipment.BODYWEIGHT,
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        requiresWeight: Boolean = true,
        movementPattern: MovementPattern = MovementPattern.PUSH,
        isCompound: Boolean = false,
        rmScalingType: RMScalingType = RMScalingType.STANDARD,
        restDurationSeconds: Int = 90,
    ): Exercise? =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            try {
                // Check if name conflicts with system exercises
                val systemExercise = exerciseDao.findSystemExerciseByName(name)
                if (systemExercise != null) {
                    CloudLogger.warn(TAG, "Cannot create custom exercise '$name': conflicts with system exercise")
                    return@withContext null
                }

                // Also check aliases to prevent conflicts
                val systemExerciseByAlias = exerciseAliasDao.findExerciseByAlias(name)
                if (systemExerciseByAlias != null) {
                    CloudLogger.warn(TAG, "Cannot create custom exercise '$name': conflicts with system exercise alias")
                    return@withContext null
                }

                // Check if custom exercise with same name already exists for this user
                val existing = exerciseDao.getCustomExerciseByUserAndName(userId, name)
                if (existing != null) {
                    CloudLogger.warn(TAG, "Custom exercise '$name' already exists for user $userId")
                    return@withContext null
                }

                val variation =
                    Exercise(
                        type = ExerciseType.USER.name,
                        userId = userId,
                        name = name,
                        category = category.name,
                        movementPattern = movementPattern.name,
                        isCompound = isCompound,
                        equipment = equipment.name,
                        difficulty = difficulty.name,
                        requiresWeight = requiresWeight,
                        rmScalingType = rmScalingType.name,
                        restDurationSeconds = restDurationSeconds,
                        updatedAt = LocalDateTime.now(),
                    )

                exerciseDao.insertExercise(variation)
                CloudLogger.debug(TAG, "Created custom exercise: $name (id: ${variation.id})")

                return@withContext variation
            } catch (e: IllegalArgumentException) {
                CloudLogger.error(TAG, "Failed to create custom exercise: invalid argument", e)
                return@withContext null
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to create custom exercise: database error", e)
                return@withContext null
            }
        }

    /**
     * Get all custom exercises for the current user.
     */
    suspend fun getCustomExercises(): List<Exercise> =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"
            exerciseDao.getCustomExercisesByUser(userId)
        }

    /**
     * Get a custom exercise by ID.
     */
    suspend fun getCustomExerciseById(exerciseId: String): Exercise? =
        withContext(Dispatchers.IO) {
            CloudLogger.debug(TAG, "Getting custom exercise by ID: $exerciseId")
            val result = exerciseDao.getExerciseById(exerciseId)
            if (result?.type == ExerciseType.USER.name) {
                CloudLogger.debug(TAG, "Custom exercise lookup result: ${result.name} for ID $exerciseId")
                result
            } else {
                CloudLogger.debug(TAG, "Exercise ID $exerciseId is not a custom exercise")
                null
            }
        }

    /**
     * Update a custom exercise.
     */
    suspend fun updateCustomExercise(
        exerciseId: String,
        name: String? = null,
        equipment: Equipment? = null,
        difficulty: ExerciseDifficulty? = null,
        requiresWeight: Boolean? = null,
        rmScalingType: RMScalingType? = null,
        restDurationSeconds: Int? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            try {
                val existing = exerciseDao.getExerciseById(exerciseId)
                if (existing == null || existing.type != ExerciseType.USER.name || existing.userId != userId) {
                    CloudLogger.error(TAG, "Custom exercise $exerciseId not found or not owned by user")
                    return@withContext false
                }

                // Check if name change would create a duplicate
                if (name != null && name != existing.name) {
                    // Check system exercises first
                    val systemExercise = exerciseDao.findSystemExerciseByName(name)
                    if (systemExercise != null) {
                        CloudLogger.warn(TAG, "Cannot rename to '$name': conflicts with system exercise")
                        return@withContext false
                    }

                    // Check system exercise aliases
                    val systemExerciseByAlias = exerciseAliasDao.findExerciseByAlias(name)
                    if (systemExerciseByAlias != null) {
                        CloudLogger.warn(TAG, "Cannot rename to '$name': conflicts with system exercise alias")
                        return@withContext false
                    }

                    // Check other custom exercises
                    val duplicate = exerciseDao.getCustomExerciseByUserAndName(userId, name)
                    if (duplicate != null) {
                        CloudLogger.warn(TAG, "Custom exercise with name '$name' already exists")
                        return@withContext false
                    }
                }

                val updated =
                    existing.copy(
                        name = name ?: existing.name,
                        equipment = equipment?.name ?: existing.equipment,
                        difficulty = difficulty?.name ?: existing.difficulty,
                        requiresWeight = requiresWeight ?: existing.requiresWeight,
                        rmScalingType = rmScalingType?.name ?: existing.rmScalingType,
                        restDurationSeconds = restDurationSeconds ?: existing.restDurationSeconds,
                        updatedAt = LocalDateTime.now(),
                    )

                exerciseDao.updateExercise(updated)
                CloudLogger.debug(TAG, "Updated custom exercise: ${updated.name}")
                return@withContext true
            } catch (e: IllegalArgumentException) {
                CloudLogger.error(TAG, "Failed to update custom exercise: invalid argument", e)
                return@withContext false
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to update custom exercise: database error", e)
                return@withContext false
            }
        }

    /**
     * Delete a custom exercise.
     * Returns true if deletion was successful.
     */
    suspend fun deleteCustomExercise(exerciseId: String): Boolean =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"
            CloudLogger.info(TAG, "deleteCustomExercise called - exerciseId: $exerciseId, userId: $userId")

            try {
                val exercise = exerciseDao.getExerciseById(exerciseId)
                if (exercise == null || exercise.userId != userId) {
                    CloudLogger.error(TAG, "Custom exercise $exerciseId not found or not owned by user")
                    return@withContext false
                }

                // Check if exercise is in use (has usage count > 0)
                val usage =
                    userExerciseUsageDao.getUsage(
                        userId = userId,
                        exerciseId = exerciseId,
                    )

                if (usage != null && usage.usageCount > 0) {
                    CloudLogger.warn(TAG, "Cannot delete custom exercise in use: ${exercise.name} (usage: ${usage.usageCount})")
                    return@withContext false
                }

                // Delete from Room first
                exerciseDao.deleteCustomExercise(exerciseId, userId)
                CloudLogger.info(TAG, "Successfully deleted custom exercise from Room: ${exercise.name}")

                // Then sync deletion to Firestore
                if (userId != "local") {
                    CloudLogger.info(TAG, "Deleting custom exercise from Firestore - exerciseId: $exerciseId")
                    val result = firestoreRepository.deleteCustomExercise(userId, exerciseId)
                    if (result.isSuccess) {
                        CloudLogger.info(TAG, "Successfully deleted custom exercise from Firestore - exerciseId: $exerciseId")
                    } else {
                        CloudLogger.error(
                            TAG,
                            "Failed to delete custom exercise from Firestore - exerciseId: $exerciseId",
                            result.exceptionOrNull(),
                        )
                    }
                }

                return@withContext true
            } catch (e: IllegalArgumentException) {
                CloudLogger.error(TAG, "Failed to delete custom exercise: invalid argument", e)
                return@withContext false
            } catch (e: android.database.sqlite.SQLiteException) {
                CloudLogger.error(TAG, "Failed to delete custom exercise: database error", e)
                return@withContext false
            }
        }

    /**
     * Check if an exercise name is available for the current user.
     */
    suspend fun isNameAvailable(name: String): Boolean =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            // Check system exercises first
            if (exerciseDao.findSystemExerciseByName(name) != null) {
                return@withContext false
            }

            // Check system exercise aliases
            if (exerciseAliasDao.findExerciseByAlias(name) != null) {
                return@withContext false
            }

            // Check custom exercises for this user
            exerciseDao.getCustomExerciseByUserAndName(userId, name) == null
        }
}
