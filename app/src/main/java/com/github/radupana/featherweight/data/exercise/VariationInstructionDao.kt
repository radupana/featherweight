package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing instructions for exercise variations.
 */
@Dao
interface VariationInstructionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: VariationInstruction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<VariationInstruction>)

    @Query(
        """
        SELECT * FROM variation_instructions
        WHERE variationId = :variationId
        ORDER BY instructionType, orderIndex
    """,
    )
    suspend fun getInstructionsForVariation(variationId: String): List<VariationInstruction>

    @Query("SELECT * FROM variation_instructions")
    suspend fun getAllInstructions(): List<VariationInstruction>

    @Query("SELECT * FROM variation_instructions WHERE id = :id")
    suspend fun getInstructionById(id: String): VariationInstruction?

    @Query("DELETE FROM variation_instructions WHERE variationId = :variationId")
    suspend fun deleteForVariation(variationId: String)
}
