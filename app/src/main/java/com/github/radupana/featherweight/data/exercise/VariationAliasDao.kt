package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for managing aliases for exercise variations.
 */
@Dao
interface VariationAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: VariationAlias): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<VariationAlias>): List<Long>

    @Update
    suspend fun updateAlias(alias: VariationAlias)

    @Query("DELETE FROM variation_aliases WHERE id = :id")
    suspend fun deleteAlias(id: Long)

    @Query("DELETE FROM variation_aliases WHERE variationId = :variationId")
    suspend fun deleteAllAliasesForVariation(variationId: Long)

    @Query("SELECT * FROM variation_aliases WHERE id = :id")
    suspend fun getAliasById(id: Long): VariationAlias?

    @Query("SELECT * FROM variation_aliases WHERE variationId = :variationId")
    suspend fun getAliasesForVariation(variationId: Long): List<VariationAlias>

    @Query("SELECT * FROM variation_aliases WHERE LOWER(alias) = LOWER(:alias) LIMIT 1")
    suspend fun findAliasByName(alias: String): VariationAlias?

    @Query(
        """
        SELECT * FROM variation_aliases 
        WHERE LOWER(alias) LIKE '%' || LOWER(:query) || '%'
        ORDER BY confidence DESC
    """,
    )
    suspend fun searchAliases(query: String): List<VariationAlias>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(a.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findVariationByAlias(alias: String): ExerciseVariation?

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        LEFT JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(v.name) = LOWER(:searchTerm) 
           OR LOWER(a.alias) = LOWER(:searchTerm)
        LIMIT 1
    """,
    )
    suspend fun findVariationByNameOrAlias(searchTerm: String): ExerciseVariation?

    @Query(
        """
        SELECT * FROM variation_aliases 
        WHERE variationId = :variationId AND languageCode = :languageCode
    """,
    )
    suspend fun getAliasesByLanguage(
        variationId: Long,
        languageCode: String,
    ): List<VariationAlias>

    @Query(
        """
        SELECT * FROM variation_aliases 
        WHERE variationId = :variationId AND source = :source
    """,
    )
    suspend fun getAliasesBySource(
        variationId: Long,
        source: String,
    ): List<VariationAlias>

    @Query(
        """
        SELECT * FROM variation_aliases 
        WHERE confidence >= :minConfidence
        ORDER BY confidence DESC
    """,
    )
    suspend fun getHighConfidenceAliases(minConfidence: Float): List<VariationAlias>

    @Query("SELECT DISTINCT languageCode FROM variation_aliases WHERE variationId = :variationId")
    suspend fun getAvailableLanguagesForVariation(variationId: Long): List<String>

    @Query("SELECT COUNT(*) FROM variation_aliases WHERE variationId = :variationId")
    suspend fun getAliasCountForVariation(variationId: Long): Int

    @Query(
        """
        SELECT variationId, COUNT(*) as count 
        FROM variation_aliases 
        GROUP BY variationId 
        ORDER BY count DESC
    """,
    )
    suspend fun getAliasCountsByVariation(): List<AliasCount>

    @Query("UPDATE variation_aliases SET confidence = :confidence WHERE id = :id")
    suspend fun updateAliasConfidence(
        id: Long,
        confidence: Float,
    )

    @Query(
        """
        SELECT COUNT(DISTINCT variationId) FROM variation_aliases 
        WHERE source = :source
    """,
    )
    suspend fun getVariationCountByAliasSource(source: String): Int
}

/**
 * Data class for alias statistics.
 */
data class AliasCount(
    val variationId: Long,
    val count: Int,
)
