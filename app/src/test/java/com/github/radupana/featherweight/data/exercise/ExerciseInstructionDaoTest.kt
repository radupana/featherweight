package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.data.BaseDaoTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for ExerciseInstructionDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - Query operations with filtering
 * - Soft delete operations
 * - Foreign key constraints
 * - Batch operations
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseInstructionDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a test exercise.
     * Required for foreign key constraints on ExerciseInstruction.
     */
    private suspend fun createExercise(
        id: String = "test-exercise",
        name: String = "Test Exercise",
        type: String = ExerciseType.SYSTEM.name,
    ): Exercise {
        val exercise =
            Exercise(
                id = id,
                type = type,
                userId = if (type == ExerciseType.USER.name) "test-user" else null,
                name = name,
                category = "STRENGTH",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                isCompound = true,
                requiresWeight = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    /**
     * Creates a test ExerciseInstruction.
     */
    private fun createInstruction(
        exerciseId: String,
        instructionType: String = "SETUP",
        orderIndex: Int = 0,
        instructionText: String = "Test instruction",
        isDeleted: Boolean = false,
    ): ExerciseInstruction =
        ExerciseInstruction(
            exerciseId = exerciseId,
            instructionType = instructionType,
            orderIndex = orderIndex,
            instructionText = instructionText,
            isDeleted = isDeleted,
        )

    // CRUD Operations Tests

    @Test
    fun `insertInstruction should add instruction to database`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id, instructionText = "Place feet shoulder-width apart")

            exerciseInstructionDao.insertInstruction(instruction)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.instructionText).isEqualTo("Place feet shoulder-width apart")
            assertThat(retrieved?.exerciseId).isEqualTo(exercise.id)
        }

    @Test
    fun `insertInstruction with REPLACE should update existing instruction`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id, instructionText = "Original instruction")

            exerciseInstructionDao.insertInstruction(instruction)

            val updated = instruction.copy(instructionText = "Updated instruction")
            exerciseInstructionDao.insertInstruction(updated)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved?.instructionText).isEqualTo("Updated instruction")
        }

    @Test
    fun `insertInstructions should add multiple instructions to database`() =
        runTest {
            val exercise = createExercise()

            val instructions =
                listOf(
                    createInstruction(exercise.id, "SETUP", 0, "Step 1"),
                    createInstruction(exercise.id, "EXECUTION", 1, "Step 2"),
                    createInstruction(exercise.id, "TIPS", 2, "Step 3"),
                )

            exerciseInstructionDao.insertInstructions(instructions)

            val retrieved = exerciseInstructionDao.getInstructionsForVariation(exercise.id)
            assertThat(retrieved).hasSize(3)
            assertThat(retrieved.map { it.instructionText }).containsExactly("Step 1", "Step 2", "Step 3")
        }

    @Test
    fun `upsertExerciseInstruction should insert new instruction`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id, instructionText = "New instruction")

            exerciseInstructionDao.upsertExerciseInstruction(instruction)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.instructionText).isEqualTo("New instruction")
        }

    @Test
    fun `upsertExerciseInstruction should update existing instruction`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id, instructionText = "Original")

            exerciseInstructionDao.insertInstruction(instruction)

            val updated = instruction.copy(instructionText = "Updated")
            exerciseInstructionDao.upsertExerciseInstruction(updated)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved?.instructionText).isEqualTo("Updated")
        }

    // Query Operations Tests

    @Test
    fun `getInstructionsForVariation should return instructions ordered by type and index`() =
        runTest {
            val exercise = createExercise()

            val instructions =
                listOf(
                    createInstruction(exercise.id, "TIPS", 1, "Tip 1"),
                    createInstruction(exercise.id, "SETUP", 0, "Setup 1"),
                    createInstruction(exercise.id, "EXECUTION", 2, "Execute 2"),
                    createInstruction(exercise.id, "EXECUTION", 1, "Execute 1"),
                    createInstruction(exercise.id, "SETUP", 1, "Setup 2"),
                )

            exerciseInstructionDao.insertInstructions(instructions)

            val retrieved = exerciseInstructionDao.getInstructionsForVariation(exercise.id)

            assertThat(retrieved).hasSize(5)
            // Should be ordered by instructionType, then orderIndex
            assertThat(retrieved[0].instructionType).isEqualTo("EXECUTION")
            assertThat(retrieved[0].orderIndex).isEqualTo(1)
            assertThat(retrieved[1].instructionType).isEqualTo("EXECUTION")
            assertThat(retrieved[1].orderIndex).isEqualTo(2)
        }

    @Test
    fun `getInstructionsForVariation should return only instructions for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Squat")

            exerciseInstructionDao.insertInstructions(
                listOf(
                    createInstruction(exercise1.id, instructionText = "Bench instruction 1"),
                    createInstruction(exercise1.id, instructionText = "Bench instruction 2"),
                    createInstruction(exercise2.id, instructionText = "Squat instruction"),
                ),
            )

            val exercise1Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise1.id)
            val exercise2Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise2.id)

            assertThat(exercise1Instructions).hasSize(2)
            assertThat(exercise2Instructions).hasSize(1)
            assertThat(exercise1Instructions.map { it.instructionText })
                .containsExactly("Bench instruction 1", "Bench instruction 2")
        }

    @Test
    fun `getInstructionsForVariation should return empty list for exercise with no instructions`() =
        runTest {
            val exercise = createExercise()

            val instructions = exerciseInstructionDao.getInstructionsForVariation(exercise.id)

            assertThat(instructions).isEmpty()
        }

    @Test
    fun `getAllInstructions should return all instructions in database`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseInstructionDao.insertInstructions(
                listOf(
                    createInstruction(exercise1.id, instructionText = "Instruction 1"),
                    createInstruction(exercise1.id, instructionText = "Instruction 2"),
                    createInstruction(exercise2.id, instructionText = "Instruction 3"),
                ),
            )

            val allInstructions = exerciseInstructionDao.getAllInstructions()

            assertThat(allInstructions).hasSize(3)
            assertThat(allInstructions.map { it.instructionText })
                .containsExactly("Instruction 1", "Instruction 2", "Instruction 3")
        }

    @Test
    fun `getInstructionById should return specific instruction`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id, instructionText = "Test instruction")

            exerciseInstructionDao.insertInstruction(instruction)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(instruction.id)
            assertThat(retrieved?.instructionText).isEqualTo("Test instruction")
        }

    @Test
    fun `getInstructionById should return null for non-existent id`() =
        runTest {
            val retrieved = exerciseInstructionDao.getInstructionById("non-existent-id")

            assertThat(retrieved).isNull()
        }

    // Deletion Operations Tests

    @Test
    fun `deleteForVariation should remove all instructions for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseInstructionDao.insertInstructions(
                listOf(
                    createInstruction(exercise1.id, instructionText = "Instruction 1"),
                    createInstruction(exercise1.id, instructionText = "Instruction 2"),
                    createInstruction(exercise2.id, instructionText = "Instruction 3"),
                ),
            )

            exerciseInstructionDao.deleteForVariation(exercise1.id)

            val exercise1Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise1.id)
            val exercise2Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise2.id)

            assertThat(exercise1Instructions).isEmpty()
            assertThat(exercise2Instructions).hasSize(1)
        }

    @Test
    fun `deleteForExercises should remove instructions for multiple exercises`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")

            exerciseInstructionDao.insertInstructions(
                listOf(
                    createInstruction(exercise1.id, instructionText = "Instruction 1"),
                    createInstruction(exercise2.id, instructionText = "Instruction 2"),
                    createInstruction(exercise3.id, instructionText = "Instruction 3"),
                ),
            )

            exerciseInstructionDao.deleteForExercises(listOf(exercise1.id, exercise2.id))

            val exercise1Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise1.id)
            val exercise2Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise2.id)
            val exercise3Instructions = exerciseInstructionDao.getInstructionsForVariation(exercise3.id)

            assertThat(exercise1Instructions).isEmpty()
            assertThat(exercise2Instructions).isEmpty()
            assertThat(exercise3Instructions).hasSize(1)
        }

    @Test
    fun `deleteById should remove specific instruction`() =
        runTest {
            val exercise = createExercise()
            val instruction1 = createInstruction(exercise.id, instructionText = "Instruction 1")
            val instruction2 = createInstruction(exercise.id, instructionText = "Instruction 2")

            exerciseInstructionDao.insertInstructions(listOf(instruction1, instruction2))

            exerciseInstructionDao.deleteById(instruction1.id)

            val retrieved1 = exerciseInstructionDao.getInstructionById(instruction1.id)
            val retrieved2 = exerciseInstructionDao.getInstructionById(instruction2.id)

            assertThat(retrieved1).isNull()
            assertThat(retrieved2).isNotNull()
        }

    // Soft Delete Operations Tests

    @Test
    fun `softDeleteExerciseInstruction should set isDeleted flag to true`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id)

            exerciseInstructionDao.insertInstruction(instruction)

            exerciseInstructionDao.softDeleteExerciseInstruction(instruction.id)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.isDeleted).isTrue()
        }

    @Test
    fun `soft deleted instructions should still appear in queries`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id)

            exerciseInstructionDao.insertInstruction(instruction)
            exerciseInstructionDao.softDeleteExerciseInstruction(instruction.id)

            val instructions = exerciseInstructionDao.getInstructionsForVariation(exercise.id)
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0].isDeleted).isTrue()
        }

    @Test
    fun `isDeleted flag should persist correctly`() =
        runTest {
            val exercise = createExercise()
            val deletedInstruction = createInstruction(exercise.id, instructionText = "Deleted", isDeleted = true)
            val activeInstruction = createInstruction(exercise.id, instructionText = "Active", isDeleted = false)

            exerciseInstructionDao.insertInstructions(listOf(deletedInstruction, activeInstruction))

            val retrieved = exerciseInstructionDao.getInstructionsForVariation(exercise.id)
            assertThat(retrieved).hasSize(2)

            val deletedRetrieved = retrieved.find { it.id == deletedInstruction.id }
            val activeRetrieved = retrieved.find { it.id == activeInstruction.id }

            assertThat(deletedRetrieved?.isDeleted).isTrue()
            assertThat(activeRetrieved?.isDeleted).isFalse()
        }

    // Foreign Key Constraint Tests

    @Test
    fun `foreign key cascade should delete instructions when exercise is deleted`() =
        runTest {
            val exercise = createExercise()
            val instruction = createInstruction(exercise.id)

            exerciseInstructionDao.insertInstruction(instruction)

            val beforeDelete = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(beforeDelete).isNotNull()

            exerciseDao.deleteExercise(exercise.id)

            val afterDelete = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(afterDelete).isNull()
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `instructions should support long text content`() =
        runTest {
            val exercise = createExercise()
            val longText = "This is a very long instruction text. ".repeat(100)
            val instruction = createInstruction(exercise.id, instructionText = longText)

            exerciseInstructionDao.insertInstruction(instruction)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved?.instructionText).isEqualTo(longText)
        }

    @Test
    fun `instructions should support special characters`() =
        runTest {
            val exercise = createExercise()
            val specialText = "Use 45° angle, don't let your back arch!"
            val instruction = createInstruction(exercise.id, instructionText = specialText)

            exerciseInstructionDao.insertInstruction(instruction)

            val retrieved = exerciseInstructionDao.getInstructionById(instruction.id)
            assertThat(retrieved?.instructionText).isEqualTo(specialText)
        }

    @Test
    fun `instructions should support unicode characters`() =
        runTest {
            val exercise = createExercise()
            val unicodeInstruction = createInstruction(exercise.id, instructionText = "保持背部挺直")

            exerciseInstructionDao.insertInstruction(unicodeInstruction)

            val retrieved = exerciseInstructionDao.getInstructionById(unicodeInstruction.id)
            assertThat(retrieved?.instructionText).isEqualTo("保持背部挺直")
        }

    @Test
    fun `multiple instructions with same type should maintain order index`() =
        runTest {
            val exercise = createExercise()

            val instructions =
                listOf(
                    createInstruction(exercise.id, "SETUP", 0, "First setup step"),
                    createInstruction(exercise.id, "SETUP", 1, "Second setup step"),
                    createInstruction(exercise.id, "SETUP", 2, "Third setup step"),
                )

            exerciseInstructionDao.insertInstructions(instructions)

            val retrieved = exerciseInstructionDao.getInstructionsForVariation(exercise.id)
            assertThat(retrieved).hasSize(3)
            assertThat(retrieved[0].orderIndex).isEqualTo(0)
            assertThat(retrieved[1].orderIndex).isEqualTo(1)
            assertThat(retrieved[2].orderIndex).isEqualTo(2)
        }
}
