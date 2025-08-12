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
    suspend fun deleteMuscleMappingsForVariation(variationId: Long)

    @Query("DELETE FROM variation_muscles WHERE variationId = :variationId AND muscle = :muscle")
    suspend fun deleteSpecificMuscleMappingForVariation(
        variationId: Long,
        muscle: MuscleGroup,
    )

    @Query("SELECT * FROM variation_muscles WHERE variationId = :variationId")
    suspend fun getMusclesForVariation(variationId: Long): List<VariationMuscle>

    @Query("SELECT * FROM variation_muscles WHERE variationId = :variationId AND isPrimary = 1")
    suspend fun getPrimaryMusclesForVariation(variationId: Long): List<VariationMuscle>

    @Query("SELECT * FROM variation_muscles WHERE variationId = :variationId AND isPrimary = 0")
    suspend fun getSecondaryMusclesForVariation(variationId: Long): List<VariationMuscle>

    @Query(
        """
        SELECT variationId FROM variation_muscles 
        WHERE muscle = :muscle 
        GROUP BY variationId
    """,
    )
    suspend fun getVariationIdsByMuscle(muscle: MuscleGroup): List<Long>

    @Query(
        """
        SELECT variationId FROM variation_muscles 
        WHERE muscle = :muscle AND isPrimary = 1
        GROUP BY variationId
    """,
    )
    suspend fun getVariationIdsByPrimaryMuscle(muscle: MuscleGroup): List<Long>

    @Query(
        """
        SELECT variationId FROM variation_muscles 
        WHERE muscle IN (:muscles)
        GROUP BY variationId
        HAVING COUNT(DISTINCT muscle) = :muscleCount
    """,
    )
    suspend fun getVariationIdsByMuscles(
        muscles: List<MuscleGroup>,
        muscleCount: Int,
    ): List<Long>

    @Query("UPDATE variation_muscles SET emphasisModifier = :modifier WHERE variationId = :variationId AND muscle = :muscle")
    suspend fun updateEmphasisModifier(
        variationId: Long,
        muscle: MuscleGroup,
        modifier: Float,
    )

    @Query(
        """
        SELECT muscle, COUNT(DISTINCT variationId) as count 
        FROM variation_muscles 
        WHERE isPrimary = 1
        GROUP BY muscle 
        ORDER BY count DESC
    """,
    )
    suspend fun getPrimaryMuscleStatistics(): List<MuscleStatistics>

    @Query("SELECT COUNT(DISTINCT variationId) FROM variation_muscles WHERE muscle = :muscle")
    suspend fun getVariationCountForMuscle(muscle: MuscleGroup): Int
}

/**
 * Data class for muscle statistics.
 */
data class MuscleStatistics(
    val muscle: MuscleGroup,
    val count: Int,
)
