package com.github.radupana.featherweight.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseAliasTest {
    @Test
    fun `new ExerciseAlias defaults to isDeleted false`() {
        val alias =
            ExerciseAlias(
                exerciseId = "exercise-123",
                alias = "Bench Press",
            )

        assertFalse(alias.isDeleted)
    }

    @Test
    fun `ExerciseAlias can be created with isDeleted true`() {
        val alias =
            ExerciseAlias(
                exerciseId = "exercise-123",
                alias = "Bench Press",
                isDeleted = true,
            )

        assertTrue(alias.isDeleted)
    }

    @Test
    fun `ExerciseAlias copy preserves isDeleted value`() {
        val alias =
            ExerciseAlias(
                exerciseId = "exercise-123",
                alias = "Bench Press",
                isDeleted = false,
            )

        val deletedAlias = alias.copy(isDeleted = true)

        assertFalse(alias.isDeleted)
        assertTrue(deletedAlias.isDeleted)
    }

    @Test
    fun `ExerciseAlias with all fields including isDeleted`() {
        val alias =
            ExerciseAlias(
                id = "alias-id",
                exerciseId = "exercise-123",
                alias = "Bench Press",
                isDeleted = false,
            )

        assertEquals("alias-id", alias.id)
        assertEquals("exercise-123", alias.exerciseId)
        assertEquals("Bench Press", alias.alias)
        assertFalse(alias.isDeleted)
    }
}
