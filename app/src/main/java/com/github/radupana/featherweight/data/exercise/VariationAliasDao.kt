package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing aliases for exercise variations.
 */
@Dao
interface VariationAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: VariationAlias): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<VariationAlias>): List<Long>

    @Query("SELECT * FROM variation_aliases WHERE variationId = :variationId")
    suspend fun getAliasesForVariation(variationId: Long): List<VariationAlias>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(a.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findVariationByAlias(alias: String): ExerciseVariation?

    @Query("SELECT * FROM variation_aliases")
    suspend fun getAllAliases(): List<VariationAlias>

    @Query("SELECT * FROM variation_aliases WHERE id = :id")
    suspend fun getAliasById(id: Long): VariationAlias?

    @Query("DELETE FROM variation_aliases WHERE variationId = :variationId")
    suspend fun deleteForVariation(variationId: Long)
}
