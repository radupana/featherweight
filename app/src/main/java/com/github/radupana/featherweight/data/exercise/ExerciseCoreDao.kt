package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for ExerciseCore operations.
 * ExerciseCore is just a grouping mechanism, so queries are simple.
 */
@Dao
interface ExerciseCoreDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseCore(exerciseCore: ExerciseCore): Long

    // Basic queries
    @Query("SELECT * FROM exercise_cores WHERE id = :id")
    suspend fun getExerciseCoreById(id: Long): ExerciseCore?

    @Query("SELECT * FROM exercise_cores WHERE name = :name LIMIT 1")
    suspend fun getExerciseCoreByName(name: String): ExerciseCore?
}
