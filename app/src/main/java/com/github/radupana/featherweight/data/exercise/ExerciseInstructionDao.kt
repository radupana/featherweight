package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing instructions for exercise variations.
 */
@Dao
interface ExerciseInstructionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: ExerciseInstruction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<ExerciseInstruction>)

    @Query(
        """
        SELECT * FROM exercise_instructions
        WHERE exerciseId = :exerciseId
        ORDER BY instructionType, orderIndex
    """,
    )
    suspend fun getInstructionsForVariation(exerciseId: String): List<ExerciseInstruction>

    @Query("SELECT * FROM exercise_instructions")
    suspend fun getAllInstructions(): List<ExerciseInstruction>

    @Query("SELECT * FROM exercise_instructions WHERE id = :id")
    suspend fun getInstructionById(id: String): ExerciseInstruction?

    @Query("DELETE FROM exercise_instructions WHERE exerciseId = :exerciseId")
    suspend fun deleteForVariation(exerciseId: String)
}
