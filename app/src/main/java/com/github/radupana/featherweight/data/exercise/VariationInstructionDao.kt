package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for managing instructions for exercise variations.
 */
@Dao
interface VariationInstructionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: VariationInstruction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<VariationInstruction>): List<Long>

    @Update
    suspend fun updateInstruction(instruction: VariationInstruction)

    @Query("DELETE FROM variation_instructions WHERE id = :id")
    suspend fun deleteInstruction(id: Long)

    @Query("DELETE FROM variation_instructions WHERE variationId = :variationId")
    suspend fun deleteAllInstructionsForVariation(variationId: Long)

    @Query("DELETE FROM variation_instructions WHERE variationId = :variationId AND instructionType = :type")
    suspend fun deleteInstructionsByType(
        variationId: Long,
        type: InstructionType,
    )

    @Query("SELECT * FROM variation_instructions WHERE id = :id")
    suspend fun getInstructionById(id: Long): VariationInstruction?

    @Query(
        """
        SELECT * FROM variation_instructions 
        WHERE variationId = :variationId 
        ORDER BY instructionType, orderIndex
    """,
    )
    suspend fun getInstructionsForVariation(variationId: Long): List<VariationInstruction>

    @Query(
        """
        SELECT * FROM variation_instructions 
        WHERE variationId = :variationId AND instructionType = :type
        ORDER BY orderIndex
    """,
    )
    suspend fun getInstructionsByType(
        variationId: Long,
        type: InstructionType,
    ): List<VariationInstruction>

    @Query(
        """
        SELECT * FROM variation_instructions 
        WHERE variationId = :variationId AND languageCode = :languageCode
        ORDER BY instructionType, orderIndex
    """,
    )
    suspend fun getInstructionsByLanguage(
        variationId: Long,
        languageCode: String,
    ): List<VariationInstruction>

    @Query(
        """
        SELECT * FROM variation_instructions 
        WHERE variationId = :variationId 
          AND instructionType = :type 
          AND languageCode = :languageCode
        ORDER BY orderIndex
    """,
    )
    suspend fun getInstructionsByTypeAndLanguage(
        variationId: Long,
        type: InstructionType,
        languageCode: String,
    ): List<VariationInstruction>

    @Query(
        """
        SELECT DISTINCT languageCode FROM variation_instructions 
        WHERE variationId = :variationId
    """,
    )
    suspend fun getAvailableLanguagesForVariation(variationId: Long): List<String>

    @Query("SELECT COUNT(*) FROM variation_instructions WHERE variationId = :variationId")
    suspend fun getInstructionCountForVariation(variationId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM variation_instructions 
        WHERE variationId = :variationId AND instructionType = :type
    """,
    )
    suspend fun getInstructionCountByType(
        variationId: Long,
        type: InstructionType,
    ): Int

    @Query(
        """
        SELECT variationId FROM variation_instructions 
        WHERE instructionType = :type 
        GROUP BY variationId
    """,
    )
    suspend fun getVariationIdsWithInstructionType(type: InstructionType): List<Long>

    @Query(
        """
        SELECT variationId, COUNT(*) as count 
        FROM variation_instructions 
        GROUP BY variationId 
        ORDER BY count DESC
    """,
    )
    suspend fun getInstructionCountsByVariation(): List<InstructionCount>
}

/**
 * Data class for instruction statistics.
 */
data class InstructionCount(
    val variationId: Long,
    val count: Int,
)
