package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for ExerciseVariation operations.
 */
@Dao
interface ExerciseVariationDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseVariation(variation: ExerciseVariation): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseVariations(variations: List<ExerciseVariation>): List<Long>

    @Query("DELETE FROM exercise_variations WHERE id = :id")
    suspend fun deleteExerciseVariation(id: Long)

    // Basic queries
    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseVariationById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE name = :name LIMIT 1")
    suspend fun getExerciseVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations ORDER BY name ASC")
    suspend fun getAllExerciseVariations(): List<ExerciseVariation>

    @Update
    suspend fun updateVariation(variation: ExerciseVariation)
}
