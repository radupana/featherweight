package com.github.radupana.featherweight.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseInstructionTest {
    @Test
    fun `new ExerciseInstruction defaults to isDeleted false`() {
        val instruction =
            ExerciseInstruction(
                exerciseId = "exercise-123",
                instructionType = "SETUP",
                orderIndex = 0,
                instructionText = "Lie flat on bench",
            )

        assertFalse(instruction.isDeleted)
    }

    @Test
    fun `ExerciseInstruction can be created with isDeleted true`() {
        val instruction =
            ExerciseInstruction(
                exerciseId = "exercise-123",
                instructionType = "SETUP",
                orderIndex = 0,
                instructionText = "Lie flat on bench",
                isDeleted = true,
            )

        assertTrue(instruction.isDeleted)
    }

    @Test
    fun `ExerciseInstruction copy preserves isDeleted value`() {
        val instruction =
            ExerciseInstruction(
                exerciseId = "exercise-123",
                instructionType = "SETUP",
                orderIndex = 0,
                instructionText = "Lie flat on bench",
                isDeleted = false,
            )

        val deletedInstruction = instruction.copy(isDeleted = true)

        assertFalse(instruction.isDeleted)
        assertTrue(deletedInstruction.isDeleted)
    }

    @Test
    fun `ExerciseInstruction with all fields including isDeleted`() {
        val instruction =
            ExerciseInstruction(
                id = "instruction-id",
                exerciseId = "exercise-123",
                instructionType = "SETUP",
                orderIndex = 0,
                instructionText = "Lie flat on bench",
                isDeleted = false,
            )

        assertEquals("instruction-id", instruction.id)
        assertEquals("exercise-123", instruction.exerciseId)
        assertEquals("SETUP", instruction.instructionType)
        assertEquals(0, instruction.orderIndex)
        assertEquals("Lie flat on bench", instruction.instructionText)
        assertFalse(instruction.isDeleted)
    }
}
