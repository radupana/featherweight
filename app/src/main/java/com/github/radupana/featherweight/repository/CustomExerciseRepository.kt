package com.github.radupana.featherweight.repository

import android.util.Log
import com.github.radupana.featherweight.data.exercise.CustomExerciseCore
import com.github.radupana.featherweight.data.exercise.CustomExerciseDao
import com.github.radupana.featherweight.data.exercise.CustomExerciseVariation
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing custom exercises created by users.
 */
class CustomExerciseRepository(
    private val customExerciseDao: CustomExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val userExerciseUsageDao: UserExerciseUsageDao,
    private val authManager: AuthenticationManager?,
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
        recommendedRepRange: String? = null,
        rmScalingType: RMScalingType = RMScalingType.STANDARD,
        restDurationSeconds: Int = 90,
    ): CustomExerciseVariation? =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            try {
                // Check if name conflicts with system exercises
                val systemExercise = exerciseDao.findVariationByExactName(name)
                if (systemExercise != null) {
                    Log.w(TAG, "Cannot create custom exercise '$name': conflicts with system exercise")
                    return@withContext null
                }

                // Also check aliases to prevent conflicts
                val systemExerciseByAlias = exerciseDao.findVariationByAlias(name)
                if (systemExerciseByAlias != null) {
                    Log.w(TAG, "Cannot create custom exercise '$name': conflicts with system exercise alias")
                    return@withContext null
                }

                // Check if custom exercise with same name already exists for this user
                val existing = customExerciseDao.getCustomVariationByUserAndName(userId, name)
                if (existing != null) {
                    Log.w(TAG, "Custom exercise '$name' already exists for user $userId")
                    return@withContext null
                }

                // Create core exercise first
                val core =
                    CustomExerciseCore(
                        userId = userId,
                        name = name,
                        category = category,
                        movementPattern = movementPattern,
                        isCompound = isCompound,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    )

                val coreId = customExerciseDao.insertCustomCore(core)
                Log.d(TAG, "Created custom exercise core: $name (id: $coreId)")

                // Create variation
                val variation =
                    CustomExerciseVariation(
                        userId = userId,
                        customCoreExerciseId = coreId,
                        name = name,
                        equipment = equipment,
                        difficulty = difficulty,
                        requiresWeight = requiresWeight,
                        recommendedRepRange = recommendedRepRange,
                        rmScalingType = rmScalingType,
                        restDurationSeconds = restDurationSeconds,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    )

                val variationId = customExerciseDao.insertCustomVariation(variation)
                Log.d(TAG, "Created custom exercise variation: $name (id: $variationId)")

                return@withContext variation.copy(id = variationId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create custom exercise", e)
                return@withContext null
            }
        }

    /**
     * Get all custom exercises for the current user.
     */
    suspend fun getCustomExercises(): List<CustomExerciseVariation> =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"
            customExerciseDao.getAllCustomExercisesForUser(userId)
        }

    /**
     * Get a custom exercise by ID.
     */
    suspend fun getCustomExerciseById(exerciseId: Long): CustomExerciseVariation? =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Getting custom exercise by ID: $exerciseId")
            val result = customExerciseDao.getCustomVariationById(exerciseId)
            Log.d(TAG, "Custom exercise lookup result: ${result?.name ?: "NULL"} for ID $exerciseId")
            result
        }

    /**
     * Update a custom exercise.
     */
    suspend fun updateCustomExercise(
        exerciseId: Long,
        name: String? = null,
        equipment: Equipment? = null,
        difficulty: ExerciseDifficulty? = null,
        requiresWeight: Boolean? = null,
        recommendedRepRange: String? = null,
        rmScalingType: RMScalingType? = null,
        restDurationSeconds: Int? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            try {
                val existing = customExerciseDao.getCustomVariationById(exerciseId)
                if (existing == null || existing.userId != userId) {
                    Log.e(TAG, "Custom exercise $exerciseId not found or not owned by user")
                    return@withContext false
                }

                // Check if name change would create a duplicate
                if (name != null && name != existing.name) {
                    // Check system exercises first
                    val systemExercise = exerciseDao.findVariationByExactName(name)
                    if (systemExercise != null) {
                        Log.w(TAG, "Cannot rename to '$name': conflicts with system exercise")
                        return@withContext false
                    }

                    // Check system exercise aliases
                    val systemExerciseByAlias = exerciseDao.findVariationByAlias(name)
                    if (systemExerciseByAlias != null) {
                        Log.w(TAG, "Cannot rename to '$name': conflicts with system exercise alias")
                        return@withContext false
                    }

                    // Check other custom exercises
                    val duplicate = customExerciseDao.getCustomVariationByUserAndName(userId, name)
                    if (duplicate != null) {
                        Log.w(TAG, "Custom exercise with name '$name' already exists")
                        return@withContext false
                    }
                }

                val updated =
                    existing.copy(
                        name = name ?: existing.name,
                        equipment = equipment ?: existing.equipment,
                        difficulty = difficulty ?: existing.difficulty,
                        requiresWeight = requiresWeight ?: existing.requiresWeight,
                        recommendedRepRange = recommendedRepRange ?: existing.recommendedRepRange,
                        rmScalingType = rmScalingType ?: existing.rmScalingType,
                        restDurationSeconds = restDurationSeconds ?: existing.restDurationSeconds,
                        updatedAt = LocalDateTime.now(),
                    )

                customExerciseDao.updateCustomVariation(updated)

                // Update core if name changed
                if (name != null && name != existing.name) {
                    val core = customExerciseDao.getCustomCoreById(existing.customCoreExerciseId)
                    if (core != null) {
                        customExerciseDao.updateCustomCore(
                            core.copy(
                                name = name,
                                updatedAt = LocalDateTime.now(),
                            ),
                        )
                    }
                }

                Log.d(TAG, "Updated custom exercise: ${updated.name}")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update custom exercise", e)
                return@withContext false
            }
        }

    /**
     * Delete a custom exercise.
     * Returns true if deletion was successful.
     */
    suspend fun deleteCustomExercise(exerciseId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"

            try {
                val exercise = customExerciseDao.getCustomVariationById(exerciseId)
                if (exercise == null || exercise.userId != userId) {
                    Log.e(TAG, "Custom exercise $exerciseId not found or not owned by user")
                    return@withContext false
                }

                // Check if exercise is in use (has usage count > 0)
                val usage =
                    userExerciseUsageDao.getUsage(
                        userId = userId,
                        variationId = exerciseId,
                        isCustom = true,
                    )

                if (usage != null && usage.usageCount > 0) {
                    Log.w(TAG, "Cannot delete custom exercise in use: ${exercise.name} (usage: ${usage.usageCount})")
                    return@withContext false
                }

                // Delete the variation (core will be cascade deleted if no other variations reference it)
                customExerciseDao.deleteCustomVariation(exerciseId, userId)

                // Clean up any orphaned cores
                val core = customExerciseDao.getCustomCoreById(exercise.customCoreExerciseId)
                if (core != null) {
                    val remainingVariations =
                        customExerciseDao
                            .getCustomVariationsByUser(userId)
                            .filter { it.customCoreExerciseId == core.id }
                    if (remainingVariations.isEmpty()) {
                        customExerciseDao.deleteCustomCore(core.id, userId)
                        Log.d(TAG, "Deleted orphaned custom exercise core: ${core.name}")
                    }
                }

                Log.d(TAG, "Deleted custom exercise: ${exercise.name}")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete custom exercise", e)
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
            if (exerciseDao.findVariationByExactName(name) != null) {
                return@withContext false
            }

            // Check system exercise aliases
            if (exerciseDao.findVariationByAlias(name) != null) {
                return@withContext false
            }

            // Check custom exercises for this user
            customExerciseDao.getCustomVariationByUserAndName(userId, name) == null
        }

    /**
     * Get custom exercises by equipment type.
     */
    suspend fun getCustomExercisesByEquipment(equipment: Equipment): List<CustomExerciseVariation> =
        withContext(Dispatchers.IO) {
            val userId = authManager?.getCurrentUserId() ?: "local"
            customExerciseDao.getCustomVariationsByEquipment(userId, equipment)
        }

    /**
     * Delete all custom exercises for a user (used when deleting account).
     */
    suspend fun deleteAllCustomExercisesForUser(userId: String) =
        withContext(Dispatchers.IO) {
            try {
                customExerciseDao.deleteAllCustomVariationsByUser(userId)
                customExerciseDao.deleteAllCustomCoresByUser(userId)
                Log.d(TAG, "Deleted all custom exercises for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete custom exercises for user", e)
            }
        }

    /**
     * Check if an exercise variation is a custom exercise.
     * Returns true if the exercise is custom, false if it's a system exercise.
     */
    suspend fun isCustomExercise(exerciseVariationId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val customExercise = customExerciseDao.getCustomVariationById(exerciseVariationId)
            customExercise != null
        }
}
