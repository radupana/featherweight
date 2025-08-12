package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for managing relationships between exercise variations.
 */
@Dao
interface VariationRelationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: VariationRelation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<VariationRelation>): List<Long>

    @Update
    suspend fun updateRelation(relation: VariationRelation)

    @Query("DELETE FROM variation_relations WHERE id = :id")
    suspend fun deleteRelation(id: Long)

    @Query("DELETE FROM variation_relations WHERE fromVariationId = :variationId OR toVariationId = :variationId")
    suspend fun deleteAllRelationsForVariation(variationId: Long)

    @Query("SELECT * FROM variation_relations WHERE id = :id")
    suspend fun getRelationById(id: Long): VariationRelation?

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE fromVariationId = :variationId OR toVariationId = :variationId
    """,
    )
    suspend fun getAllRelationsForVariation(variationId: Long): List<VariationRelation>

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE fromVariationId = :variationId
    """,
    )
    suspend fun getOutgoingRelations(variationId: Long): List<VariationRelation>

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE toVariationId = :variationId
    """,
    )
    suspend fun getIncomingRelations(variationId: Long): List<VariationRelation>

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE fromVariationId = :variationId AND relationType = :type
    """,
    )
    suspend fun getOutgoingRelationsByType(
        variationId: Long,
        type: ExerciseRelationType,
    ): List<VariationRelation>

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE toVariationId = :variationId AND relationType = :type
    """,
    )
    suspend fun getIncomingRelationsByType(
        variationId: Long,
        type: ExerciseRelationType,
    ): List<VariationRelation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_relations r ON v.id = r.toVariationId
        WHERE r.fromVariationId = :variationId AND r.relationType = :type
        ORDER BY r.strength DESC
    """,
    )
    suspend fun getRelatedVariations(
        variationId: Long,
        type: ExerciseRelationType,
    ): List<ExerciseVariation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_relations r ON v.id = r.toVariationId
        WHERE r.fromVariationId = :variationId 
          AND r.relationType = 'PROGRESSION'
        ORDER BY r.strength DESC
    """,
    )
    suspend fun getProgressions(variationId: Long): List<ExerciseVariation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_relations r ON v.id = r.toVariationId
        WHERE r.fromVariationId = :variationId 
          AND r.relationType = 'REGRESSION'
        ORDER BY r.strength DESC
    """,
    )
    suspend fun getRegressions(variationId: Long): List<ExerciseVariation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_relations r ON v.id = r.toVariationId
        WHERE r.fromVariationId = :variationId 
          AND r.relationType = 'ALTERNATIVE'
        ORDER BY r.strength DESC
    """,
    )
    suspend fun getAlternatives(variationId: Long): List<ExerciseVariation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_relations r ON v.id = r.toVariationId
        WHERE r.fromVariationId = :variationId 
          AND r.relationType = 'SUPERSET'
        ORDER BY r.strength DESC
    """,
    )
    suspend fun getSupersetPairings(variationId: Long): List<ExerciseVariation>

    @Query(
        """
        SELECT * FROM variation_relations 
        WHERE fromVariationId = :fromId AND toVariationId = :toId
        LIMIT 1
    """,
    )
    suspend fun getRelationBetween(
        fromId: Long,
        toId: Long,
    ): VariationRelation?

    @Query(
        """
        SELECT COUNT(*) FROM variation_relations 
        WHERE relationType = :type
    """,
    )
    suspend fun getRelationCountByType(type: ExerciseRelationType): Int

    @Query(
        """
        SELECT relationType, COUNT(*) as count 
        FROM variation_relations 
        GROUP BY relationType 
        ORDER BY count DESC
    """,
    )
    suspend fun getRelationStatistics(): List<RelationStatistics>

    @Query("UPDATE variation_relations SET strength = :strength WHERE id = :id")
    suspend fun updateRelationStrength(
        id: Long,
        strength: Float,
    )
}

/**
 * Data class for relation statistics.
 */
data class RelationStatistics(
    val relationType: ExerciseRelationType,
    val count: Int,
)
