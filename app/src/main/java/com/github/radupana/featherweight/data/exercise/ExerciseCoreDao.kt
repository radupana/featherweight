package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for ExerciseCore operations.
 * ExerciseCore is just a grouping mechanism, so queries are simple.
 */
@Dao
interface ExerciseCoreDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseCore(exerciseCore: ExerciseCore): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseCores(exerciseCores: List<ExerciseCore>): List<Long>

    @Update
    suspend fun updateExerciseCore(exerciseCore: ExerciseCore)

    @Query("DELETE FROM exercise_cores WHERE id = :id")
    suspend fun deleteExerciseCore(id: Long)

    // Basic queries
    @Query("SELECT * FROM exercise_cores WHERE id = :id")
    suspend fun getExerciseCoreById(id: Long): ExerciseCore?

    @Query("SELECT * FROM exercise_cores WHERE name = :name LIMIT 1")
    suspend fun getExerciseCoreByName(name: String): ExerciseCore?

    @Query("SELECT * FROM exercise_cores ORDER BY name ASC")
    suspend fun getAllExerciseCores(): List<ExerciseCore>

    // Search and filtering
    @Query("SELECT * FROM exercise_cores WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchExerciseCores(query: String): List<ExerciseCore>

    @Query("SELECT * FROM exercise_cores WHERE category = :category ORDER BY name ASC")
    suspend fun getExerciseCoresByCategory(category: ExerciseCategory): List<ExerciseCore>

    @Query("SELECT * FROM exercise_cores WHERE movementPattern = :pattern ORDER BY name ASC")
    suspend fun getExerciseCoresByMovementPattern(pattern: MovementPattern): List<ExerciseCore>

    @Query("SELECT * FROM exercise_cores WHERE isCompound = :isCompound ORDER BY name ASC")
    suspend fun getExerciseCoresByCompound(isCompound: Boolean): List<ExerciseCore>

    // Count queries
    @Query("SELECT COUNT(*) FROM exercise_cores")
    suspend fun getExerciseCoreCount(): Int

    @Query("SELECT COUNT(*) FROM exercise_cores WHERE category = :category")
    suspend fun getExerciseCoreCountByCategory(category: ExerciseCategory): Int

    @Query("SELECT COUNT(*) FROM exercise_cores WHERE movementPattern = :pattern")
    suspend fun getExerciseCoreCountByMovementPattern(pattern: MovementPattern): Int

    @Query("SELECT COUNT(*) FROM exercise_cores WHERE isCompound = :isCompound")
    suspend fun getExerciseCoreCountByCompound(isCompound: Boolean): Int
}
