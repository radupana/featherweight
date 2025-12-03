package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.data.BaseDaoTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for ExerciseAliasDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - JOIN queries for finding exercises by alias
 * - Query operations with filtering
 * - Soft delete operations
 * - Batch operations
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseAliasDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a test exercise.
     * Required for foreign key constraints on ExerciseAlias.
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
     * Creates a test ExerciseAlias.
     */
    private fun createAlias(
        exerciseId: String,
        alias: String,
        isDeleted: Boolean = false,
    ): ExerciseAlias =
        ExerciseAlias(
            exerciseId = exerciseId,
            alias = alias,
            isDeleted = isDeleted,
        )

    // CRUD Operations Tests

    @Test
    fun `insertAlias should add alias to database`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Bench Press")

            exerciseAliasDao.insertAlias(alias)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.alias).isEqualTo("Bench Press")
            assertThat(retrieved?.exerciseId).isEqualTo(exercise.id)
        }

    @Test
    fun `insertAlias with REPLACE should update existing alias`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Original Alias")

            exerciseAliasDao.insertAlias(alias)

            // Insert same ID with different alias text
            val updated = alias.copy(alias = "Updated Alias")
            exerciseAliasDao.insertAlias(updated)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)
            assertThat(retrieved?.alias).isEqualTo("Updated Alias")
        }

    @Test
    fun `insertAliases should add multiple aliases to database`() =
        runTest {
            val exercise = createExercise()

            val aliases =
                listOf(
                    createAlias(exercise.id, "Bench Press"),
                    createAlias(exercise.id, "BP"),
                    createAlias(exercise.id, "Flat Bench"),
                )

            exerciseAliasDao.insertAliases(aliases)

            val retrieved = exerciseAliasDao.getAliasesForExercise(exercise.id)
            assertThat(retrieved).hasSize(3)
            assertThat(retrieved.map { it.alias }).containsExactly("Bench Press", "BP", "Flat Bench")
        }

    @Test
    fun `upsertExerciseAlias should insert new alias`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "New Alias")

            exerciseAliasDao.upsertExerciseAlias(alias)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.alias).isEqualTo("New Alias")
        }

    @Test
    fun `upsertExerciseAlias should update existing alias`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Original")

            exerciseAliasDao.insertAlias(alias)

            val updated = alias.copy(alias = "Updated")
            exerciseAliasDao.upsertExerciseAlias(updated)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)
            assertThat(retrieved?.alias).isEqualTo("Updated")
        }

    // Query Operations Tests

    @Test
    fun `getAliasesForExercise should return all aliases for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Squat")

            exerciseAliasDao.insertAliases(
                listOf(
                    createAlias(exercise1.id, "BP"),
                    createAlias(exercise1.id, "Flat Bench"),
                    createAlias(exercise2.id, "Back Squat"),
                ),
            )

            val exercise1Aliases = exerciseAliasDao.getAliasesForExercise(exercise1.id)
            val exercise2Aliases = exerciseAliasDao.getAliasesForExercise(exercise2.id)

            assertThat(exercise1Aliases).hasSize(2)
            assertThat(exercise2Aliases).hasSize(1)
            assertThat(exercise1Aliases.map { it.alias }).containsExactly("BP", "Flat Bench")
            assertThat(exercise2Aliases[0].alias).isEqualTo("Back Squat")
        }

    @Test
    fun `getAliasesForExercise should return empty list for exercise with no aliases`() =
        runTest {
            val exercise = createExercise()

            val aliases = exerciseAliasDao.getAliasesForExercise(exercise.id)

            assertThat(aliases).isEmpty()
        }

    @Test
    fun `findExerciseByAlias should return exercise with matching alias case-insensitive`() =
        runTest {
            val exercise = createExercise(name = "Barbell Bench Press")
            exerciseAliasDao.insertAlias(createAlias(exercise.id, "Bench Press"))

            // Test case-insensitive matching
            val foundLower = exerciseAliasDao.findExerciseByAlias("bench press")
            val foundUpper = exerciseAliasDao.findExerciseByAlias("BENCH PRESS")
            val foundMixed = exerciseAliasDao.findExerciseByAlias("BeNcH pReSs")

            assertThat(foundLower).isNotNull()
            assertThat(foundLower?.id).isEqualTo(exercise.id)
            assertThat(foundUpper?.id).isEqualTo(exercise.id)
            assertThat(foundMixed?.id).isEqualTo(exercise.id)
        }

    @Test
    fun `findExerciseByAlias should return null for non-existent alias`() =
        runTest {
            val exercise = createExercise()
            exerciseAliasDao.insertAlias(createAlias(exercise.id, "Bench Press"))

            val found = exerciseAliasDao.findExerciseByAlias("Deadlift")

            assertThat(found).isNull()
        }

    @Test
    fun `findExerciseByAlias should return first matching exercise when multiple exist`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Barbell Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Dumbbell Bench Press")

            // Both exercises have the same alias
            exerciseAliasDao.insertAlias(createAlias(exercise1.id, "Bench Press"))
            exerciseAliasDao.insertAlias(createAlias(exercise2.id, "Bench Press"))

            val found = exerciseAliasDao.findExerciseByAlias("Bench Press")

            // Should return one of them (query has LIMIT 1)
            assertThat(found).isNotNull()
            assertThat(found?.id).isIn(listOf(exercise1.id, exercise2.id))
        }

    @Test
    fun `findExerciseByAlias should perform JOIN query correctly`() =
        runTest {
            val exercise = createExercise(name = "Barbell Squat")
            exerciseAliasDao.insertAlias(createAlias(exercise.id, "Back Squat"))

            val found = exerciseAliasDao.findExerciseByAlias("Back Squat")

            // Verify the full exercise entity is returned via JOIN
            assertThat(found).isNotNull()
            assertThat(found?.id).isEqualTo(exercise.id)
            assertThat(found?.name).isEqualTo("Barbell Squat")
            assertThat(found?.equipment).isEqualTo("BARBELL")
            assertThat(found?.category).isEqualTo("STRENGTH")
        }

    @Test
    fun `getAllAliases should return all aliases in database`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseAliasDao.insertAliases(
                listOf(
                    createAlias(exercise1.id, "Alias 1"),
                    createAlias(exercise1.id, "Alias 2"),
                    createAlias(exercise2.id, "Alias 3"),
                ),
            )

            val allAliases = exerciseAliasDao.getAllAliases()

            assertThat(allAliases).hasSize(3)
            assertThat(allAliases.map { it.alias }).containsExactly("Alias 1", "Alias 2", "Alias 3")
        }

    @Test
    fun `getAliasById should return specific alias`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Test Alias")

            exerciseAliasDao.insertAlias(alias)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(alias.id)
            assertThat(retrieved?.alias).isEqualTo("Test Alias")
        }

    @Test
    fun `getAliasById should return null for non-existent id`() =
        runTest {
            val retrieved = exerciseAliasDao.getAliasById("non-existent-id")

            assertThat(retrieved).isNull()
        }

    // Deletion Operations Tests

    @Test
    fun `deleteForVariation should remove all aliases for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseAliasDao.insertAliases(
                listOf(
                    createAlias(exercise1.id, "Alias 1"),
                    createAlias(exercise1.id, "Alias 2"),
                    createAlias(exercise2.id, "Alias 3"),
                ),
            )

            exerciseAliasDao.deleteForVariation(exercise1.id)

            val exercise1Aliases = exerciseAliasDao.getAliasesForExercise(exercise1.id)
            val exercise2Aliases = exerciseAliasDao.getAliasesForExercise(exercise2.id)

            assertThat(exercise1Aliases).isEmpty()
            assertThat(exercise2Aliases).hasSize(1)
        }

    @Test
    fun `deleteForExercises should remove aliases for multiple exercises`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")

            exerciseAliasDao.insertAliases(
                listOf(
                    createAlias(exercise1.id, "Alias 1"),
                    createAlias(exercise2.id, "Alias 2"),
                    createAlias(exercise3.id, "Alias 3"),
                ),
            )

            exerciseAliasDao.deleteForExercises(listOf(exercise1.id, exercise2.id))

            val exercise1Aliases = exerciseAliasDao.getAliasesForExercise(exercise1.id)
            val exercise2Aliases = exerciseAliasDao.getAliasesForExercise(exercise2.id)
            val exercise3Aliases = exerciseAliasDao.getAliasesForExercise(exercise3.id)

            assertThat(exercise1Aliases).isEmpty()
            assertThat(exercise2Aliases).isEmpty()
            assertThat(exercise3Aliases).hasSize(1)
        }

    @Test
    fun `deleteById should remove specific alias`() =
        runTest {
            val exercise = createExercise()
            val alias1 = createAlias(exercise.id, "Alias 1")
            val alias2 = createAlias(exercise.id, "Alias 2")

            exerciseAliasDao.insertAliases(listOf(alias1, alias2))

            exerciseAliasDao.deleteById(alias1.id)

            val retrieved1 = exerciseAliasDao.getAliasById(alias1.id)
            val retrieved2 = exerciseAliasDao.getAliasById(alias2.id)

            assertThat(retrieved1).isNull()
            assertThat(retrieved2).isNotNull()
        }

    // Soft Delete Operations Tests

    @Test
    fun `softDeleteExerciseAlias should set isDeleted flag to true`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Test Alias")

            exerciseAliasDao.insertAlias(alias)

            exerciseAliasDao.softDeleteExerciseAlias(alias.id)

            val retrieved = exerciseAliasDao.getAliasById(alias.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.isDeleted).isTrue()
        }

    @Test
    fun `soft deleted aliases should still appear in queries`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Test Alias")

            exerciseAliasDao.insertAlias(alias)
            exerciseAliasDao.softDeleteExerciseAlias(alias.id)

            // Soft-deleted aliases still appear in queries
            val aliases = exerciseAliasDao.getAliasesForExercise(exercise.id)
            assertThat(aliases).hasSize(1)
            assertThat(aliases[0].isDeleted).isTrue()
        }

    @Test
    fun `isDeleted flag should persist correctly`() =
        runTest {
            val exercise = createExercise()
            val deletedAlias = createAlias(exercise.id, "Deleted Alias", isDeleted = true)
            val activeAlias = createAlias(exercise.id, "Active Alias", isDeleted = false)

            exerciseAliasDao.insertAliases(listOf(deletedAlias, activeAlias))

            val retrieved = exerciseAliasDao.getAliasesForExercise(exercise.id)
            assertThat(retrieved).hasSize(2)

            val deletedRetrieved = retrieved.find { it.id == deletedAlias.id }
            val activeRetrieved = retrieved.find { it.id == activeAlias.id }

            assertThat(deletedRetrieved?.isDeleted).isTrue()
            assertThat(activeRetrieved?.isDeleted).isFalse()
        }

    // Foreign Key Constraint Tests

    @Test
    fun `foreign key cascade should delete aliases when exercise is deleted`() =
        runTest {
            val exercise = createExercise()
            val alias = createAlias(exercise.id, "Test Alias")

            exerciseAliasDao.insertAlias(alias)

            // Verify alias exists
            val beforeDelete = exerciseAliasDao.getAliasById(alias.id)
            assertThat(beforeDelete).isNotNull()

            // Delete the exercise - should cascade to aliases
            exerciseDao.deleteExercise(exercise.id)

            // Alias should be gone
            val afterDelete = exerciseAliasDao.getAliasById(alias.id)
            assertThat(afterDelete).isNull()
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `findExerciseByAlias should handle special characters in alias`() =
        runTest {
            val exercise = createExercise()
            exerciseAliasDao.insertAlias(createAlias(exercise.id, "Smith's Bench"))

            val found = exerciseAliasDao.findExerciseByAlias("Smith's Bench")

            assertThat(found).isNotNull()
            assertThat(found?.id).isEqualTo(exercise.id)
        }

    @Test
    fun `aliases should support unicode characters`() =
        runTest {
            val exercise = createExercise()
            val unicodeAlias = createAlias(exercise.id, "力量训练")

            exerciseAliasDao.insertAlias(unicodeAlias)

            val retrieved = exerciseAliasDao.getAliasById(unicodeAlias.id)
            assertThat(retrieved?.alias).isEqualTo("力量训练")
        }

    @Test
    fun `findExerciseByAlias should handle whitespace correctly`() =
        runTest {
            val exercise = createExercise()
            exerciseAliasDao.insertAlias(createAlias(exercise.id, "Bench  Press"))

            // Exact match required
            val foundExact = exerciseAliasDao.findExerciseByAlias("Bench  Press")
            val notFoundSingleSpace = exerciseAliasDao.findExerciseByAlias("Bench Press")

            assertThat(foundExact).isNotNull()
            assertThat(notFoundSingleSpace).isNull()
        }
}
