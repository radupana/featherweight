package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing muscle associations with exercise variations.
 */
@Dao
interface VariationMuscleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariationMuscle(muscle: VariationMuscle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariationMuscles(muscles: List<VariationMuscle>)

    @Query("DELETE FROM variation_muscles WHERE variationId = :variationId")
    suspend fun deleteMuscleMappingsForVariation(variationId: String)

    @Query("SELECT * FROM variation_muscles WHERE variationId = :variationId")
    suspend fun getMusclesForVariation(variationId: String): List<VariationMuscle>

    @Query("SELECT * FROM variation_muscles")
    suspend fun getAllVariationMuscles(): List<VariationMuscle>

    @Query("DELETE FROM variation_muscles WHERE variationId = :variationId")
    suspend fun deleteForVariation(variationId: String)
}
