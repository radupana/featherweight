package com.github.radupana.featherweight.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ExerciseTest {
    @Test
    fun `new Exercise defaults to isDeleted false`() {
        val exercise =
            Exercise(
                name = "Barbell Bench Press",
                category = "CHEST",
                movementPattern = "PUSH",
                equipment = "BARBELL",
            )

        assertFalse(exercise.isDeleted)
    }

    @Test
    fun `Exercise can be created with isDeleted true`() {
        val exercise =
            Exercise(
                name = "Barbell Bench Press",
                category = "CHEST",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                isDeleted = true,
            )

        assertTrue(exercise.isDeleted)
    }

    @Test
    fun `Exercise copy preserves isDeleted value`() {
        val exercise =
            Exercise(
                name = "Barbell Bench Press",
                category = "CHEST",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                isDeleted = false,
            )

        val deletedExercise = exercise.copy(isDeleted = true)

        assertFalse(exercise.isDeleted)
        assertTrue(deletedExercise.isDeleted)
    }

    @Test
    fun `Exercise with all fields including isDeleted`() {
        val now = LocalDateTime.now()
        val exercise =
            Exercise(
                id = "test-id",
                type = ExerciseType.SYSTEM.name,
                userId = null,
                name = "Barbell Bench Press",
                category = "CHEST",
                movementPattern = "PUSH",
                isCompound = true,
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
                requiresWeight = true,
                rmScalingType = "STANDARD",
                restDurationSeconds = 180,
                createdAt = now,
                updatedAt = now,
                isDeleted = false,
            )

        assertEquals("test-id", exercise.id)
        assertEquals("Barbell Bench Press", exercise.name)
        assertFalse(exercise.isDeleted)
    }
}
