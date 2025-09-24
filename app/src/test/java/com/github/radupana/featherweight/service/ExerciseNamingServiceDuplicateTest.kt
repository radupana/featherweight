package com.github.radupana.featherweight.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseNamingServiceDuplicateTest {
    private val service = ExerciseNamingService()

    @Test
    fun `validateExerciseNameWithDuplicateCheck accepts unique name`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Dumbbell Fly",
                "Cable Crossover",
                "Bench Press", // Alias for Barbell Bench Press
                "BP", // Another alias
            )

        // Act
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "Dumbbell Bench Press",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck rejects exact duplicate`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Dumbbell Fly",
                "Cable Crossover",
            )

        // Act
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "Barbell Bench Press",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "An exercise with this name already exists",
            (result as ValidationResult.Invalid).reason,
        )
        assertTrue(result.suggestion?.contains("Barbell Bench Press") == true)
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck rejects case-insensitive duplicate`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Dumbbell Fly",
                "Cable Crossover",
            )

        // Act - different casing
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "barbell bench PRESS",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "An exercise with this name already exists",
            (result as ValidationResult.Invalid).reason,
        )
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck rejects duplicate alias`() {
        // Given existing exercises including aliases
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Bench Press", // Alias
                "Flat Bench", // Alias
                "BP", // Alias
                "Dumbbell Fly",
                "Chest Fly", // Alias for Dumbbell Fly
            )

        // Act - try to create exercise with name matching an alias
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "Bench Press",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "An exercise with this name already exists",
            (result as ValidationResult.Invalid).reason,
        )
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck handles trimmed input`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Dumbbell Fly",
            )

        // Act - with extra spaces
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "  Barbell Bench Press  ",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "An exercise with this name already exists",
            (result as ValidationResult.Invalid).reason,
        )
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck still applies standard validation`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Dumbbell Fly",
            )

        // Act - name with emoji (should fail standard validation first)
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "Custom Exercise 💪",
                existingExercises,
            )

        // Assert - should fail for emoji, not duplicate
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "Exercise name cannot contain emojis",
            (result as ValidationResult.Invalid).reason,
        )
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck accepts similar but different names`() {
        // Given existing exercises
        val existingExercises =
            listOf(
                "Barbell Bench Press",
                "Incline Barbell Bench Press",
                "Decline Barbell Bench Press",
                "Dumbbell Bench Press",
            )

        // Act - similar but unique names
        val results =
            listOf(
                service.validateExerciseNameWithDuplicateCheck("Close Grip Barbell Bench Press", existingExercises),
                service.validateExerciseNameWithDuplicateCheck("Wide Grip Barbell Bench Press", existingExercises),
                service.validateExerciseNameWithDuplicateCheck("Barbell Floor Press", existingExercises),
                service.validateExerciseNameWithDuplicateCheck("Swiss Bar Bench Press", existingExercises),
            )

        // Assert - all should be valid
        results.forEach { result ->
            assertTrue(result is ValidationResult.Valid)
        }
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck handles empty existing list`() {
        // Given no existing exercises
        val existingExercises = emptyList<String>()

        // Act
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "Barbell Bench Press",
                existingExercises,
            )

        // Assert
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateExerciseNameWithDuplicateCheck preserves original casing in error message`() {
        // Given existing exercises with specific casing
        val existingExercises =
            listOf(
                "Barbell BENCH Press", // Unusual casing
                "Dumbbell Fly",
            )

        // Act - try to duplicate with different casing
        val result =
            service.validateExerciseNameWithDuplicateCheck(
                "barbell bench press",
                existingExercises,
            )

        // Assert - error should show original casing
        assertTrue(result is ValidationResult.Invalid)
        val errorMessage = (result as ValidationResult.Invalid).suggestion
        assertTrue(errorMessage?.contains("Barbell BENCH Press") == true)
    }
}
