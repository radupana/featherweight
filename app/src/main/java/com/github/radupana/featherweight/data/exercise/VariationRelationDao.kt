package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing variation relations.
 */
@Dao
interface VariationRelationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: VariationRelation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<VariationRelation>): List<Long>

    @Query("SELECT * FROM variation_relations WHERE fromVariationId = :variationId OR toVariationId = :variationId")
    suspend fun getRelationsForVariation(variationId: Long): List<VariationRelation>

    @Query("SELECT * FROM variation_relations")
    suspend fun getAllRelations(): List<VariationRelation>

    @Query("SELECT * FROM variation_relations WHERE id = :id")
    suspend fun getRelationById(id: Long): VariationRelation?
}
