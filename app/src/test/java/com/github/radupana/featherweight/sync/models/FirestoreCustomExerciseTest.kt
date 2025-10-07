package com.github.radupana.featherweight.sync.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FirestoreCustomExerciseTest {
    @Test
    fun `FirestoreCustomExercise has correct default values`() {
        val exercise = FirestoreCustomExercise()

        assertThat(exercise.id).isEqualTo("")
        assertThat(exercise.type).isEqualTo("USER")
        assertThat(exercise.userId).isEqualTo("")
        assertThat(exercise.name).isEqualTo("")
        assertThat(exercise.isDeleted).isFalse()
        assertThat(exercise.muscles).isEmpty()
        assertThat(exercise.aliases).isEmpty()
        assertThat(exercise.instructions).isEmpty()
    }

    @Test
    fun `FirestoreCustomExercise can be created with all fields`() {
        val muscles = listOf(FirestoreMuscle("CHEST", true, 1.0))
        val aliases = listOf("Bench Press")
        val instructions = listOf(FirestoreInstruction("SETUP", "Lie on bench", 0, "en"))

        val exercise =
            FirestoreCustomExercise(
                id = "test-id",
                type = "USER",
                userId = "user-123",
                name = "Barbell Bench Press",
                category = "CHEST",
                movementPattern = "PUSH",
                isCompound = true,
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
                requiresWeight = true,
                rmScalingType = "STANDARD",
                restDurationSeconds = 120,
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-02T00:00:00Z",
                isDeleted = false,
                muscles = muscles,
                aliases = aliases,
                instructions = instructions,
            )

        assertThat(exercise.id).isEqualTo("test-id")
        assertThat(exercise.type).isEqualTo("USER")
        assertThat(exercise.userId).isEqualTo("user-123")
        assertThat(exercise.name).isEqualTo("Barbell Bench Press")
        assertThat(exercise.category).isEqualTo("CHEST")
        assertThat(exercise.movementPattern).isEqualTo("PUSH")
        assertThat(exercise.isCompound).isTrue()
        assertThat(exercise.equipment).isEqualTo("BARBELL")
        assertThat(exercise.difficulty).isEqualTo("INTERMEDIATE")
        assertThat(exercise.requiresWeight).isTrue()
        assertThat(exercise.rmScalingType).isEqualTo("STANDARD")
        assertThat(exercise.restDurationSeconds).isEqualTo(120)
        assertThat(exercise.isDeleted).isFalse()
        assertThat(exercise.muscles).hasSize(1)
        assertThat(exercise.aliases).hasSize(1)
        assertThat(exercise.instructions).hasSize(1)
    }

    @Test
    fun `FirestoreCustomExercise copy preserves isDeleted flag`() {
        val exercise = FirestoreCustomExercise(id = "test-id", isDeleted = true)

        assertThat(exercise.isDeleted).isTrue()

        val copied = exercise.copy(name = "New Name")

        assertThat(copied.isDeleted).isTrue()
        assertThat(copied.name).isEqualTo("New Name")
    }

    @Test
    fun `FirestoreCustomExerciseMuscle has correct default values`() {
        val muscle = FirestoreCustomExerciseMuscle()

        assertThat(muscle.id).isEqualTo("")
        assertThat(muscle.exerciseId).isEqualTo("")
        assertThat(muscle.muscle).isEqualTo("")
        assertThat(muscle.targetType).isEqualTo("")
        assertThat(muscle.isDeleted).isFalse()
    }

    @Test
    fun `FirestoreCustomExerciseMuscle can be created with all fields`() {
        val muscle =
            FirestoreCustomExerciseMuscle(
                id = "muscle-id",
                exerciseId = "exercise-id",
                muscle = "CHEST",
                targetType = "primary",
                isDeleted = false,
            )

        assertThat(muscle.id).isEqualTo("muscle-id")
        assertThat(muscle.exerciseId).isEqualTo("exercise-id")
        assertThat(muscle.muscle).isEqualTo("CHEST")
        assertThat(muscle.targetType).isEqualTo("primary")
        assertThat(muscle.isDeleted).isFalse()
    }

    @Test
    fun `FirestoreCustomExerciseAlias has correct default values`() {
        val alias = FirestoreCustomExerciseAlias()

        assertThat(alias.id).isEqualTo("")
        assertThat(alias.exerciseId).isEqualTo("")
        assertThat(alias.alias).isEqualTo("")
        assertThat(alias.isDeleted).isFalse()
    }

    @Test
    fun `FirestoreCustomExerciseAlias can be created with all fields`() {
        val alias =
            FirestoreCustomExerciseAlias(
                id = "alias-id",
                exerciseId = "exercise-id",
                alias = "Bench Press",
                isDeleted = false,
            )

        assertThat(alias.id).isEqualTo("alias-id")
        assertThat(alias.exerciseId).isEqualTo("exercise-id")
        assertThat(alias.alias).isEqualTo("Bench Press")
        assertThat(alias.isDeleted).isFalse()
    }

    @Test
    fun `FirestoreCustomExerciseInstruction has correct default values`() {
        val instruction = FirestoreCustomExerciseInstruction()

        assertThat(instruction.id).isEqualTo("")
        assertThat(instruction.exerciseId).isEqualTo("")
        assertThat(instruction.instructionType).isEqualTo("")
        assertThat(instruction.orderIndex).isEqualTo(0)
        assertThat(instruction.instructionText).isEqualTo("")
        assertThat(instruction.isDeleted).isFalse()
    }

    @Test
    fun `FirestoreCustomExerciseInstruction can be created with all fields`() {
        val instruction =
            FirestoreCustomExerciseInstruction(
                id = "instruction-id",
                exerciseId = "exercise-id",
                instructionType = "SETUP",
                orderIndex = 1,
                instructionText = "Lie on bench",
                isDeleted = false,
            )

        assertThat(instruction.id).isEqualTo("instruction-id")
        assertThat(instruction.exerciseId).isEqualTo("exercise-id")
        assertThat(instruction.instructionType).isEqualTo("SETUP")
        assertThat(instruction.orderIndex).isEqualTo(1)
        assertThat(instruction.instructionText).isEqualTo("Lie on bench")
        assertThat(instruction.isDeleted).isFalse()
    }
}
