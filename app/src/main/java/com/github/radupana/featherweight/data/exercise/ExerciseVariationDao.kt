package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for ExerciseVariation operations.
 * Handles both system (userId = null) and custom (userId != null) exercises.
 */
@Dao
interface ExerciseVariationDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseVariation(variation: ExerciseVariation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseVariations(variations: List<ExerciseVariation>)

    @Update
    suspend fun updateVariation(variation: ExerciseVariation)

    @Query("DELETE FROM exercise_variations WHERE id = :id")
    suspend fun deleteExerciseVariation(id: String)

    // Basic queries
    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseVariationById(id: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE name = :name AND userId IS NULL LIMIT 1")
    suspend fun getExerciseVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE LOWER(name) = LOWER(:name) AND userId IS NULL LIMIT 1")
    suspend fun findVariationByName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations ORDER BY name ASC")
    suspend fun getAllExerciseVariations(): List<ExerciseVariation>

    // System exercise queries (userId = null)
    @Query("SELECT * FROM exercise_variations WHERE userId IS NULL ORDER BY name ASC")
    suspend fun getSystemVariations(): List<ExerciseVariation>

    // Custom exercise queries (userId != null)
    @Query("SELECT * FROM exercise_variations WHERE userId = :userId ORDER BY name ASC")
    suspend fun getCustomVariationsByUser(userId: String): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getCustomVariationByUserAndName(
        userId: String,
        name: String,
    ): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE userId = :userId AND equipment = :equipment ORDER BY name ASC")
    suspend fun getCustomVariationsByEquipment(
        userId: String,
        equipment: Equipment,
    ): List<ExerciseVariation>

    @Query("DELETE FROM exercise_variations WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomVariation(
        id: String,
        userId: String,
    )

    @Query("DELETE FROM exercise_variations WHERE userId = :userId")
    suspend fun deleteAllCustomVariationsByUser(userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM exercise_variations WHERE id = :variationId AND userId IS NOT NULL)")
    suspend fun isCustomExercise(variationId: String): Boolean
}
