package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

/**
 * DAO for Exercise operations.
 * Handles both SYSTEM and USER type exercises.
 */
@Dao
interface ExerciseDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: Exercise)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    // Basic queries
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<Exercise>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseByName(name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findExerciseByName(name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) AND type = 'SYSTEM' LIMIT 1")
    suspend fun findSystemExerciseByName(name: String): Exercise?

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercises(): List<Exercise>

    // System exercise queries (type = SYSTEM)
    @Query("SELECT * FROM exercises WHERE type = 'SYSTEM' ORDER BY name ASC")
    suspend fun getSystemExercises(): List<Exercise>

    // Custom exercise queries (type = USER)
    @Query("SELECT * FROM exercises WHERE userId = :userId ORDER BY name ASC")
    suspend fun getCustomExercisesByUser(userId: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getCustomExerciseByUserAndName(
        userId: String,
        name: String,
    ): Exercise?

    @Query("SELECT * FROM exercises WHERE userId = :userId AND equipment = :equipment ORDER BY name ASC")
    suspend fun getCustomExercisesByEquipment(
        userId: String,
        equipment: String,
    ): List<Exercise>

    @Query("DELETE FROM exercises WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomExercise(
        id: String,
        userId: String,
    )

    @Query("DELETE FROM exercises WHERE userId = :userId")
    suspend fun deleteAllCustomExercisesByUser(userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM exercises WHERE id = :exerciseId AND type = 'USER')")
    suspend fun isCustomExercise(exerciseId: String): Boolean

    // Search exercises by name pattern
    @Query(
        """
        SELECT * FROM exercises
        WHERE LOWER(name) LIKE LOWER('%' || :query || '%')
        ORDER BY name ASC
    """,
    )
    suspend fun searchExercises(query: String): List<Exercise>

    // Get exercises by category
    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    suspend fun getExercisesByCategory(category: String): List<Exercise>

    // Get exercises by equipment
    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    suspend fun getExercisesByEquipment(equipment: String): List<Exercise>

    // Sync operations
    @Upsert
    suspend fun upsertExercise(exercise: Exercise)

    @Upsert
    suspend fun upsertExercises(exercises: List<Exercise>)

    @Query("UPDATE exercises SET isDeleted = 1 WHERE id = :exerciseId")
    suspend fun softDeleteExercise(exerciseId: String)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}
