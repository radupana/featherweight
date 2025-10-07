package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

/**
 * Instructions for performing exercise variations.
 * Can include setup, execution, and tips.
 */
@Entity(
    tableName = "exercise_instructions",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["exerciseId", "instructionType", "orderIndex"]),
    ],
)
data class ExerciseInstruction(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val exerciseId: String,
    val instructionType: String, // Was InstructionType enum
    val orderIndex: Int,
    val instructionText: String,
    // Soft delete flag for sync
    val isDeleted: Boolean = false,
)
