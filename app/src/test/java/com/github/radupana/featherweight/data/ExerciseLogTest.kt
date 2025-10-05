package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseLogTest {
    @Test
    fun exerciseLog_withDefaultValues_hasCorrectDefaults() {
        // Act
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        // Assert
        assertThat(exerciseLog.id).isNotEmpty() // ID is auto-generated
        assertThat(exerciseLog.workoutId).isEqualTo("10")
        assertThat(exerciseLog.exerciseId).isEqualTo("5")
        assertThat(exerciseLog.exerciseOrder).isEqualTo(1)
        assertThat(exerciseLog.notes).isNull()
        assertThat(exerciseLog.originalExerciseId).isNull()
        assertThat(exerciseLog.isSwapped).isFalse()
    }

    @Test
    fun exerciseLog_withAllValues_storesCorrectly() {
        // Act
        val exerciseLog =
            ExerciseLog(
                id = "123",
                workoutId = "45",
                exerciseId = "88",
                exerciseOrder = 3,
                notes = "Focus on form today",
                originalExerciseId = "77",
                isSwapped = true,
            )

        // Assert
        assertThat(exerciseLog.id).isEqualTo("123")
        assertThat(exerciseLog.workoutId).isEqualTo("45")
        assertThat(exerciseLog.exerciseId).isEqualTo("88")
        assertThat(exerciseLog.exerciseOrder).isEqualTo(3)
        assertThat(exerciseLog.notes).isEqualTo("Focus on form today")
        assertThat(exerciseLog.originalExerciseId).isEqualTo("77")
        assertThat(exerciseLog.isSwapped).isTrue()
    }

    @Test
    fun exerciseLog_withSwappedExercise_tracksOriginal() {
        // Act - Exercise was swapped from barbell to dumbbell variation
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "55", // Current (swapped to)
                exerciseOrder = 1,
                originalExerciseId = "33", // Original (swapped from)
                isSwapped = true,
            )

        // Assert
        assertThat(exerciseLog.exerciseId).isEqualTo("55")
        assertThat(exerciseLog.originalExerciseId).isEqualTo("33")
        assertThat(exerciseLog.isSwapped).isTrue()
    }

    @Test
    fun exerciseLog_withoutSwap_hasNoOriginal() {
        // Act
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "55",
                exerciseOrder = 1,
                isSwapped = false,
            )

        // Assert
        assertThat(exerciseLog.originalExerciseId).isNull()
        assertThat(exerciseLog.isSwapped).isFalse()
    }

    @Test
    fun exerciseLog_withZeroOrder_isFirstExercise() {
        // Act
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 0, // First exercise
            )

        // Assert
        assertThat(exerciseLog.exerciseOrder).isEqualTo(0)
    }

    @Test
    fun exerciseLog_withHighOrder_isValidLaterExercise() {
        // Act - 20th exercise in a long workout
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 19,
            )

        // Assert
        assertThat(exerciseLog.exerciseOrder).isEqualTo(19)
    }

    @Test
    fun exerciseLog_withLongNotes_storesFullText() {
        // Arrange
        val longNotes =
            "Today I'm focusing on time under tension. " +
                "Going to slow down the eccentric phase to 3 seconds " +
                "and pause at the bottom for 1 second. " +
                "Last week I felt some discomfort in my shoulder " +
                "so I'm being extra careful with form today. " +
                "Target is 3 sets of 8-10 reps with good control."

        // Act
        val exerciseLog =
            ExerciseLog(
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
                notes = longNotes,
            )

        // Assert
        assertThat(exerciseLog.notes).isEqualTo(longNotes)
    }

    @Test
    fun exerciseLog_copy_createsNewInstance() {
        // Arrange
        val original =
            ExerciseLog(
                id = "1",
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
                notes = "Original notes",
            )

        // Act
        val copy =
            original.copy(
                exerciseOrder = 2,
                notes = "Updated notes",
            )

        // Assert
        assertThat(copy.id).isEqualTo("1")
        assertThat(copy.workoutId).isEqualTo("10")
        assertThat(copy.exerciseId).isEqualTo("5")
        assertThat(copy.exerciseOrder).isEqualTo(2)
        assertThat(copy.notes).isEqualTo("Updated notes")

        // Original should be unchanged
        assertThat(original.exerciseOrder).isEqualTo(1)
        assertThat(original.notes).isEqualTo("Original notes")
    }

    @Test
    fun exerciseLog_equals_worksCorrectly() {
        // Arrange
        val log1 =
            ExerciseLog(
                id = "1",
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        val log2 =
            ExerciseLog(
                id = "1",
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        val log3 =
            ExerciseLog(
                id = "2", // Different ID
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        // Assert
        assertThat(log1).isEqualTo(log2)
        assertThat(log1).isNotEqualTo(log3)
    }

    @Test
    fun exerciseLog_hashCode_isConsistent() {
        // Arrange
        val log1 =
            ExerciseLog(
                id = "1",
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        val log2 =
            ExerciseLog(
                id = "1",
                workoutId = "10",
                exerciseId = "5",
                exerciseOrder = 1,
            )

        // Assert
        assertThat(log1.hashCode()).isEqualTo(log2.hashCode())
    }

    @Test
    fun exerciseLog_forSameWorkout_haveSameWorkoutId() {
        // Act
        val exercise1 =
            ExerciseLog(
                workoutId = "100",
                exerciseId = "1",
                exerciseOrder = 0,
            )

        val exercise2 =
            ExerciseLog(
                workoutId = "100",
                exerciseId = "2",
                exerciseOrder = 1,
            )

        val exercise3 =
            ExerciseLog(
                workoutId = "100",
                exerciseId = "3",
                exerciseOrder = 2,
            )

        // Assert - All belong to same workout
        assertThat(exercise1.workoutId).isEqualTo("100")
        assertThat(exercise2.workoutId).isEqualTo("100")
        assertThat(exercise3.workoutId).isEqualTo("100")
    }

    @Test
    fun exerciseLog_forDifferentWorkouts_haveDifferentWorkoutIds() {
        // Act
        val workout1Exercise =
            ExerciseLog(
                workoutId = "100",
                exerciseId = "1",
                exerciseOrder = 0,
            )

        val workout2Exercise =
            ExerciseLog(
                workoutId = "200",
                exerciseId = "1", // Same exercise
                exerciseOrder = 0,
            )

        // Assert
        assertThat(workout1Exercise.workoutId).isNotEqualTo(workout2Exercise.workoutId)
        assertThat(workout1Exercise.exerciseId).isEqualTo(workout2Exercise.exerciseId)
    }

    @Test
    fun exerciseLog_toString_containsKeyInfo() {
        // Act
        val exerciseLog =
            ExerciseLog(
                id = "10",
                workoutId = "100",
                exerciseId = "55",
                exerciseOrder = 2,
            )

        val stringRep = exerciseLog.toString()

        // Assert - toString should contain key identifying information
        assertThat(stringRep).contains("10") // id
        assertThat(stringRep).contains("100") // workoutId
        assertThat(stringRep).contains("55") // exerciseId
        assertThat(stringRep).contains("2") // exerciseOrder
    }
}
