package com.github.radupana.featherweight.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseMuscleTest {
    @Test
    fun `new ExerciseMuscle defaults to isDeleted false`() {
        val muscle =
            ExerciseMuscle(
                exerciseId = "exercise-123",
                muscle = "CHEST",
                targetType = "PRIMARY",
            )

        assertFalse(muscle.isDeleted)
    }

    @Test
    fun `ExerciseMuscle can be created with isDeleted true`() {
        val muscle =
            ExerciseMuscle(
                exerciseId = "exercise-123",
                muscle = "CHEST",
                targetType = "PRIMARY",
                isDeleted = true,
            )

        assertTrue(muscle.isDeleted)
    }

    @Test
    fun `ExerciseMuscle copy preserves isDeleted value`() {
        val muscle =
            ExerciseMuscle(
                exerciseId = "exercise-123",
                muscle = "CHEST",
                targetType = "PRIMARY",
                isDeleted = false,
            )

        val deletedMuscle = muscle.copy(isDeleted = true)

        assertFalse(muscle.isDeleted)
        assertTrue(deletedMuscle.isDeleted)
    }

    @Test
    fun `ExerciseMuscle with all fields including isDeleted`() {
        val muscle =
            ExerciseMuscle(
                id = "muscle-id",
                exerciseId = "exercise-123",
                muscle = "CHEST",
                targetType = "PRIMARY",
                isDeleted = false,
            )

        assertEquals("muscle-id", muscle.id)
        assertEquals("exercise-123", muscle.exerciseId)
        assertEquals("CHEST", muscle.muscle)
        assertEquals("PRIMARY", muscle.targetType)
        assertFalse(muscle.isDeleted)
    }
}
