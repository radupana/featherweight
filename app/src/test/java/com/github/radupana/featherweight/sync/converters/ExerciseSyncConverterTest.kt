package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.exercise.InstructionType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import com.github.radupana.featherweight.testutil.LogMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class ExerciseSyncConverterTest {
    @Test
    fun `fromFirestore converts denormalized exercise to normalized entities correctly`() {
        // Given a denormalized Firestore exercise
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Bench Press",
                coreCategory = "CHEST",
                coreMovementPattern = "PRESS",
                coreIsCompound = true,
                name = "Barbell Bench Press",
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
                requiresWeight = true,
                rmScalingType = "STANDARD",
                restDurationSeconds = 120,
                muscles =
                    listOf(
                        FirestoreMuscle("CHEST", true, 1.0),
                        FirestoreMuscle("TRICEPS", false, 0.5),
                        FirestoreMuscle("SHOULDERS", false, 0.5),
                    ),
                aliases = listOf("Bench Press", "Flat Bench", "BP"),
                instructions =
                    listOf(
                        FirestoreInstruction(
                            "EXECUTION",
                            "Lie on bench with eyes under barbell. Grip bar slightly wider than shoulders.",
                            0,
                            "en",
                        ),
                    ),
                createdAt = "2024-01-01T00:00:00Z",
                updatedAt = "2024-01-01T00:00:00Z",
            )

        // When converting to SQLite entities
        val bundle =
            ExerciseSyncConverter.fromFirestore(
                firestoreExercise,
                "barbell-bench-press",
            )

        // Then the variation contains all fields (merged from core + variation)
        with(bundle.exercise) {
            assertEquals("Barbell Bench Press", name)
            assertEquals(ExerciseCategory.CHEST.name, category)
            assertEquals(MovementPattern.PUSH.name, movementPattern)
            assertTrue(isCompound)
            assertEquals(Equipment.BARBELL.name, equipment)
            assertEquals(ExerciseDifficulty.INTERMEDIATE.name, difficulty)
            assertTrue(requiresWeight)
            // recommendedRepRange field no longer exists
            assertEquals(RMScalingType.STANDARD.name, rmScalingType)
            assertEquals(120, restDurationSeconds)
        }

        // And muscles are correct
        assertEquals(3, bundle.exerciseMuscles.size)
        with(bundle.exerciseMuscles[0]) {
            assertEquals(MuscleGroup.CHEST.name, muscle)
            assertEquals("primary", targetType)
        }
        with(bundle.exerciseMuscles[1]) {
            assertEquals(MuscleGroup.TRICEPS.name, muscle)
            assertEquals("secondary", targetType)
        }

        // And aliases are correct
        assertEquals(3, bundle.exerciseAliases.size)
        assertEquals("Bench Press", bundle.exerciseAliases[0].alias)
        assertEquals("Flat Bench", bundle.exerciseAliases[1].alias)
        assertEquals("BP", bundle.exerciseAliases[2].alias)

        // And instructions are correct
        assertEquals(1, bundle.exerciseInstructions.size)
        with(bundle.exerciseInstructions[0]) {
            assertEquals(InstructionType.EXECUTION.name, instructionType)
            assertTrue(instructionText.contains("Lie on bench"))
            assertEquals(0, orderIndex)
        }

        // And firestore ID is preserved
        assertEquals("barbell-bench-press", bundle.firestoreId)
    }

    @Test
    fun `toFirestore converts normalized entities to denormalized exercise correctly`() {
        // Given normalized SQLite entities
        val variation =
            Exercise(
                id = "2",
                userId = "user123",
                name = "Custom Barbell Press",
                category = ExerciseCategory.CHEST.name,
                movementPattern = MovementPattern.PUSH.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
                rmScalingType = RMScalingType.STANDARD.name,
                restDurationSeconds = 90,
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
            )

        val muscles =
            listOf(
                ExerciseMuscle(
                    exerciseId = "2",
                    muscle = MuscleGroup.CHEST.name,
                    targetType = "primary",
                ),
            )

        val aliases =
            listOf(
                ExerciseAlias(
                    id = "3",
                    exerciseId = "2",
                    alias = "Custom Press",
                ),
            )

        val instructions =
            listOf(
                ExerciseInstruction(
                    id = "4",
                    exerciseId = "2",
                    instructionType = InstructionType.EXECUTION.name,
                    instructionText = "Custom instruction",
                    orderIndex = 0,
                ),
            )

        // When converting to Firestore
        val firestoreExercise =
            ExerciseSyncConverter.toFirestore(
                variation,
                muscles,
                aliases,
                instructions,
            )

        // Then the denormalized structure is correct
        assertEquals("Custom Barbell Press", firestoreExercise.coreName)
        assertEquals("CHEST", firestoreExercise.coreCategory)
        assertEquals("PUSH", firestoreExercise.coreMovementPattern)
        assertTrue(firestoreExercise.coreIsCompound)
        assertEquals("Custom Barbell Press", firestoreExercise.name)
        assertEquals("BARBELL", firestoreExercise.equipment)
        assertEquals("INTERMEDIATE", firestoreExercise.difficulty)
        assertTrue(firestoreExercise.requiresWeight)
        // recommendedRepRange field was removed from FirestoreExercise
        assertEquals("STANDARD", firestoreExercise.rmScalingType)
        assertEquals(90, firestoreExercise.restDurationSeconds)
        assertEquals(1, firestoreExercise.muscles.size)
        assertEquals("CHEST", firestoreExercise.muscles[0].muscle)
        assertEquals(1, firestoreExercise.aliases.size)
        assertEquals("Custom Press", firestoreExercise.aliases[0])
        assertEquals(1, firestoreExercise.instructions.size)
        assertEquals("Custom instruction", firestoreExercise.instructions[0].content)
    }

    @Test
    fun `fromFirestore handles missing optional fields gracefully`() {
        // Given minimal Firestore exercise
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Push Up",
                coreCategory = "CHEST",
                coreMovementPattern = "PUSH",
                coreIsCompound = true,
                name = "Bodyweight Push Up",
                equipment = "BODYWEIGHT",
                difficulty = "BEGINNER",
                requiresWeight = false,
                recommendedRepRange = null,
                rmScalingType = "STANDARD",
                restDurationSeconds = 60,
                muscles = emptyList(),
                aliases = emptyList(),
                instructions = emptyList(),
                createdAt = null,
                updatedAt = null,
            )

        // When converting
        val bundle =
            ExerciseSyncConverter.fromFirestore(
                firestoreExercise,
                "bodyweight-push-up",
            )

        // Then it should handle missing data
        // recommendedRepRange field no longer exists
        assertTrue(bundle.exerciseMuscles.isEmpty())
        assertTrue(bundle.exerciseAliases.isEmpty())
        assertTrue(bundle.exerciseInstructions.isEmpty())
        assertNotNull(bundle.exercise.createdAt) // Should default to now
    }

    @Test
    fun `fromFirestore handles invalid enum values with defaults`() {
        // Setup Log mock
        LogMock.setup()
        // Given Firestore exercise with invalid enums
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Test Exercise",
                coreCategory = "INVALID_CATEGORY",
                coreMovementPattern = "INVALID_PATTERN",
                coreIsCompound = false,
                name = "Test Exercise",
                equipment = "INVALID_EQUIPMENT",
                difficulty = "INVALID_DIFFICULTY",
                requiresWeight = false,
                rmScalingType = "INVALID_SCALING",
                restDurationSeconds = 90,
                muscles =
                    listOf(
                        FirestoreMuscle("INVALID_MUSCLE", true, 1.0),
                    ),
                aliases = emptyList(),
                instructions =
                    listOf(
                        FirestoreInstruction("INVALID_TYPE", "content", 0, "en"),
                    ),
            )

        // When converting
        val bundle =
            ExerciseSyncConverter.fromFirestore(
                firestoreExercise,
                "test-exercise",
            )

        // Then it should use defaults for invalid values
        assertEquals(ExerciseCategory.OTHER.name, bundle.exercise.category)
        assertEquals(MovementPattern.OTHER.name, bundle.exercise.movementPattern)
        assertEquals(Equipment.NONE.name, bundle.exercise.equipment)
        assertEquals(ExerciseDifficulty.INTERMEDIATE.name, bundle.exercise.difficulty)
        assertEquals(RMScalingType.STANDARD.name, bundle.exercise.rmScalingType)
        assertEquals(MuscleGroup.OTHER.name, bundle.exerciseMuscles[0].muscle)
        assertEquals(InstructionType.EXECUTION.name, bundle.exerciseInstructions[0].instructionType)
    }

    @Test
    fun `stable ID generation is consistent`() {
        // Given the same exercise converted multiple times
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Squat",
                coreCategory = "LEGS",
                coreMovementPattern = "SQUAT",
                coreIsCompound = true,
                name = "Barbell Back Squat",
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
            )

        // When converting multiple times
        val bundle1 = ExerciseSyncConverter.fromFirestore(firestoreExercise, "barbell-squat")
        val bundle2 = ExerciseSyncConverter.fromFirestore(firestoreExercise, "barbell-squat")

        // Then IDs should be stable
        assertEquals(bundle1.exercise.id, bundle2.exercise.id)
    }

    @Test
    fun `generated IDs are valid UUIDs`() {
        // Given a Firestore exercise
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Deadlift",
                coreCategory = "LEGS",
                coreMovementPattern = "HINGE",
                coreIsCompound = true,
                name = "Conventional Deadlift",
                equipment = "BARBELL",
                difficulty = "ADVANCED",
                aliases = listOf("DL"),
                instructions =
                    listOf(
                        FirestoreInstruction("EXECUTION", "Stand with feet hip-width", 0, "en"),
                    ),
                muscles =
                    listOf(
                        FirestoreMuscle("HAMSTRINGS", true, 1.0),
                    ),
            )

        // When converting to SQLite entities
        val bundle = ExerciseSyncConverter.fromFirestore(firestoreExercise, "deadlift")

        // Then all generated IDs should be valid UUIDs
        assertTrue("Variation ID should be a valid UUID", isValidUUID(bundle.exercise.id))
        bundle.exerciseAliases.forEach { alias ->
            assertTrue("Alias ID ${alias.id} should be a valid UUID", isValidUUID(alias.id))
        }
        bundle.exerciseInstructions.forEach { instruction ->
            assertTrue("Instruction ID ${instruction.id} should be a valid UUID", isValidUUID(instruction.id))
        }
    }

    @Suppress("SwallowedException")
    private fun isValidUUID(uuid: String): Boolean =
        try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

    @Test
    fun `fromFirestore preserves all muscle emphasis modifiers`() {
        // Given exercise with multiple muscles at different emphasis
        val firestoreExercise =
            FirestoreExercise(
                coreName = "Romanian Deadlift",
                coreCategory = "LEGS",
                coreMovementPattern = "HINGE",
                coreIsCompound = true,
                name = "Barbell Romanian Deadlift",
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
                muscles =
                    listOf(
                        FirestoreMuscle("HAMSTRINGS", true, 1.0),
                        FirestoreMuscle("GLUTES", false, 0.75),
                        FirestoreMuscle("LOWER_BACK", false, 0.5),
                        FirestoreMuscle("CORE", false, 0.25),
                    ),
            )

        // When converting
        val bundle = ExerciseSyncConverter.fromFirestore(firestoreExercise, "rdl")

        // Then all muscles are converted
        assertEquals(4, bundle.exerciseMuscles.size)
        // emphasisModifier field no longer exists in ExerciseMuscle
    }

    @Test
    fun `roundtrip conversion preserves all data`() {
        // Given a custom exercise
        val originalVariation =
            Exercise(
                id = "200",
                userId = "user456",
                name = "Cable Test Exercise",
                category = ExerciseCategory.BACK.name,
                movementPattern = MovementPattern.PULL.name,
                isCompound = true,
                equipment = Equipment.CABLE.name,
                difficulty = ExerciseDifficulty.ADVANCED.name,
                requiresWeight = true,
                rmScalingType = RMScalingType.ISOLATION.name,
                restDurationSeconds = 180,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val originalMuscles =
            listOf(
                ExerciseMuscle(exerciseId = "200", muscle = MuscleGroup.LATS.name, targetType = "primary"),
                ExerciseMuscle(exerciseId = "200", muscle = MuscleGroup.BICEPS.name, targetType = "secondary"),
            )

        val originalAliases =
            listOf(
                ExerciseAlias("300", "200", "Test Alias 1"),
                ExerciseAlias("301", "200", "Test Alias 2"),
            )

        val originalInstructions =
            listOf(
                ExerciseInstruction("400", "200", InstructionType.EXECUTION.name, 0, "Step 1"),
                ExerciseInstruction("401", "200", InstructionType.EXECUTION.name, 1, "Step 2"),
            )

        // When converting to Firestore and back
        val firestoreExercise =
            ExerciseSyncConverter.toFirestore(
                originalVariation,
                originalMuscles,
                originalAliases,
                originalInstructions,
            )

        val reconvertedBundle =
            ExerciseSyncConverter.fromFirestore(
                firestoreExercise,
                "test-exercise",
            )

        // Then all variation data is preserved (including merged core fields)
        with(reconvertedBundle.exercise) {
            assertEquals(originalVariation.name, name)
            assertEquals(originalVariation.category, category)
            assertEquals(originalVariation.movementPattern, movementPattern)
            assertEquals(originalVariation.isCompound, isCompound)
            assertEquals(originalVariation.equipment, equipment)
            assertEquals(originalVariation.difficulty, difficulty)
            assertEquals(originalVariation.requiresWeight, requiresWeight)
            // recommendedRepRange field no longer exists
            assertEquals(originalVariation.rmScalingType, rmScalingType)
            assertEquals(originalVariation.restDurationSeconds, restDurationSeconds)
        }

        // And muscle data is preserved
        assertEquals(originalMuscles.size, reconvertedBundle.exerciseMuscles.size)
        originalMuscles.zip(reconvertedBundle.exerciseMuscles).forEach { (original, reconverted) ->
            assertEquals(original.muscle, reconverted.muscle)
            assertEquals(original.targetType, reconverted.targetType)
        }

        // And aliases are preserved
        assertEquals(originalAliases.size, reconvertedBundle.exerciseAliases.size)
        assertEquals(originalAliases[0].alias, reconvertedBundle.exerciseAliases[0].alias)
        assertEquals(originalAliases[1].alias, reconvertedBundle.exerciseAliases[1].alias)

        // And instructions are preserved
        assertEquals(originalInstructions.size, reconvertedBundle.exerciseInstructions.size)
        originalInstructions.zip(reconvertedBundle.exerciseInstructions).forEach { (original, reconverted) ->
            assertEquals(original.instructionText, reconverted.instructionText)
            assertEquals(original.orderIndex, reconverted.orderIndex)
        }
    }
}
