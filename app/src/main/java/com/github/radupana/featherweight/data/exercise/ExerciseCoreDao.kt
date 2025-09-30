package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for ExerciseCore operations.
 * Handles both system (userId = null) and custom (userId != null) exercises.
 */
@Dao
interface ExerciseCoreDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseCore(exerciseCore: ExerciseCore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCore(exerciseCore: ExerciseCore)

    @Update
    suspend fun updateCore(exerciseCore: ExerciseCore)

    // Basic queries
    @Query("SELECT * FROM exercise_cores WHERE id = :id")
    suspend fun getExerciseCoreById(id: String): ExerciseCore?

    @Query("SELECT * FROM exercise_cores WHERE id = :id")
    suspend fun getCoreById(id: String): ExerciseCore?

    @Query("SELECT * FROM exercise_cores WHERE name = :name AND userId IS NULL LIMIT 1")
    suspend fun getExerciseCoreByName(name: String): ExerciseCore?

    @Query("SELECT * FROM exercise_cores")
    suspend fun getAllCores(): List<ExerciseCore>

    // System exercise queries (userId = null)
    @Query("SELECT * FROM exercise_cores WHERE userId IS NULL")
    suspend fun getSystemCores(): List<ExerciseCore>

    // Custom exercise queries (userId != null)
    @Query("SELECT * FROM exercise_cores WHERE userId = :userId")
    suspend fun getCustomCoresByUser(userId: String): List<ExerciseCore>

    @Query("DELETE FROM exercise_cores WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomCore(
        id: String,
        userId: String,
    )

    @Query("DELETE FROM exercise_cores WHERE userId = :userId")
    suspend fun deleteAllCustomCoresByUser(userId: String)
}
