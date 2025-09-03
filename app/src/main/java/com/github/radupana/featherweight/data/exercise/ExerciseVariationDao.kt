package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    @Query("SELECT * FROM exercise_variations ORDER BY usageCount DESC, name ASC")
    suspend fun getAllExerciseVariations(): List<ExerciseVariation>

    // Usage tracking
    @Query("UPDATE exercise_variations SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Long)

}

