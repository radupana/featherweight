package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.InstructionType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationInstruction
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
                recommendedRepRange = "6-10",
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

        // Then the core exercise is correct
        with(bundle.exerciseCore) {
            assertEquals("Bench Press", name)
            assertEquals(ExerciseCategory.CHEST, category)
            assertEquals(MovementPattern.PRESS, movementPattern)
            assertTrue(isCompound)
        }

        // And the variation is correct
        with(bundle.exerciseVariation) {
            assertEquals("Barbell Bench Press", name)
            assertEquals(Equipment.BARBELL, equipment)
            assertEquals(ExerciseDifficulty.INTERMEDIATE, difficulty)
            assertTrue(requiresWeight)
            assertEquals("6-10", recommendedRepRange)
            assertEquals(RMScalingType.STANDARD, rmScalingType)
            assertEquals(120, restDurationSeconds)
        }

        // And muscles are correct
        assertEquals(3, bundle.variationMuscles.size)
        with(bundle.variationMuscles[0]) {
            assertEquals(MuscleGroup.CHEST, muscle)
            assertTrue(isPrimary)
            assertEquals(1.0f, emphasisModifier)
        }
        with(bundle.variationMuscles[1]) {
            assertEquals(MuscleGroup.TRICEPS, muscle)
            assertFalse(isPrimary)
            assertEquals(0.5f, emphasisModifier)
        }

        // And aliases are correct
        assertEquals(3, bundle.variationAliases.size)
        assertEquals("Bench Press", bundle.variationAliases[0].alias)
        assertEquals("Flat Bench", bundle.variationAliases[1].alias)
        assertEquals("BP", bundle.variationAliases[2].alias)

        // And instructions are correct
        assertEquals(1, bundle.variationInstructions.size)
        with(bundle.variationInstructions[0]) {
            assertEquals(InstructionType.EXECUTION, instructionType)
            assertTrue(content.contains("Lie on bench"))
            assertEquals(0, orderIndex)
            assertEquals("en", languageCode)
        }

        // And firestore ID is preserved
        assertEquals("barbell-bench-press", bundle.firestoreId)
    }

    @Test
    fun `toFirestore converts normalized entities to denormalized exercise correctly`() {
        // Given normalized SQLite entities
        val core =
            ExerciseCore(
                id = "1",
                userId = "user123",
                name = "Custom Press",
                category = ExerciseCategory.CHEST,
                movementPattern = MovementPattern.PRESS,
                isCompound = true,
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
            )

        val variation =
            ExerciseVariation(
                id = "2",
                userId = "user123",
                coreExerciseId = "1",
                name = "Custom Barbell Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
                recommendedRepRange = "8-12",
                rmScalingType = RMScalingType.STANDARD,
                restDurationSeconds = 90,
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
                updatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
            )

        val muscles =
            listOf(
                VariationMuscle(
                    variationId = "2",
                    muscle = MuscleGroup.CHEST,
                    isPrimary = true,
                    emphasisModifier = 1.0f,
                ),
            )

        val aliases =
            listOf(
                VariationAlias(
                    id = "3",
                    variationId = "2",
                    alias = "Custom Press",
                    confidence = 1.0f,
                    languageCode = "en",
                    source = "user",
                ),
            )

        val instructions =
            listOf(
                VariationInstruction(
                    id = "4",
                    variationId = "2",
                    instructionType = InstructionType.EXECUTION,
                    content = "Custom instruction",
                    orderIndex = 0,
                    languageCode = "en",
                ),
            )

        // When converting to Firestore
        val firestoreExercise =
            ExerciseSyncConverter.toFirestore(
                core,
                variation,
                muscles,
                aliases,
                instructions,
            )

        // Then the denormalized structure is correct
        assertEquals("Custom Press", firestoreExercise.coreName)
        assertEquals("CHEST", firestoreExercise.coreCategory)
        assertEquals("PRESS", firestoreExercise.coreMovementPattern)
        assertTrue(firestoreExercise.coreIsCompound)
        assertEquals("Custom Barbell Press", firestoreExercise.name)
        assertEquals("BARBELL", firestoreExercise.equipment)
        assertEquals("INTERMEDIATE", firestoreExercise.difficulty)
        assertTrue(firestoreExercise.requiresWeight)
        assertEquals("8-12", firestoreExercise.recommendedRepRange)
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
        assertNull(bundle.exerciseVariation.recommendedRepRange)
        assertTrue(bundle.variationMuscles.isEmpty())
        assertTrue(bundle.variationAliases.isEmpty())
        assertTrue(bundle.variationInstructions.isEmpty())
        assertNotNull(bundle.exerciseCore.createdAt) // Should default to now
    }

    @Test
    fun `fromFirestore handles invalid enum values with defaults`() {
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
        assertEquals(ExerciseCategory.FULL_BODY, bundle.exerciseCore.category)
        assertEquals(MovementPattern.PUSH, bundle.exerciseCore.movementPattern)
        assertEquals(Equipment.NONE, bundle.exerciseVariation.equipment)
        assertEquals(ExerciseDifficulty.INTERMEDIATE, bundle.exerciseVariation.difficulty)
        assertEquals(RMScalingType.STANDARD, bundle.exerciseVariation.rmScalingType)
        assertEquals(MuscleGroup.FULL_BODY, bundle.variationMuscles[0].muscle)
        assertEquals(InstructionType.EXECUTION, bundle.variationInstructions[0].instructionType)
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
        assertEquals(bundle1.exerciseCore.id, bundle2.exerciseCore.id)
        assertEquals(bundle1.exerciseVariation.id, bundle2.exerciseVariation.id)
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
        assertTrue("Core ID should be a valid UUID", isValidUUID(bundle.exerciseCore.id))
        assertTrue("Variation ID should be a valid UUID", isValidUUID(bundle.exerciseVariation.id))
        bundle.variationAliases.forEach { alias ->
            assertTrue("Alias ID ${alias.id} should be a valid UUID", isValidUUID(alias.id))
        }
        bundle.variationInstructions.forEach { instruction ->
            assertTrue("Instruction ID ${instruction.id} should be a valid UUID", isValidUUID(instruction.id))
        }
    }

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

        // Then all emphasis modifiers are preserved
        assertEquals(4, bundle.variationMuscles.size)
        assertEquals(1.0f, bundle.variationMuscles[0].emphasisModifier)
        assertEquals(0.75f, bundle.variationMuscles[1].emphasisModifier)
        assertEquals(0.5f, bundle.variationMuscles[2].emphasisModifier)
        assertEquals(0.25f, bundle.variationMuscles[3].emphasisModifier)
    }

    @Test
    fun `roundtrip conversion preserves all data`() {
        // Given a custom exercise
        val originalCore =
            ExerciseCore(
                id = "100",
                userId = "user456",
                name = "Test Exercise",
                category = ExerciseCategory.BACK,
                movementPattern = MovementPattern.PULL,
                isCompound = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val originalVariation =
            ExerciseVariation(
                id = "200",
                userId = "user456",
                coreExerciseId = "100",
                name = "Cable Test Exercise",
                equipment = Equipment.CABLE,
                difficulty = ExerciseDifficulty.ADVANCED,
                requiresWeight = true,
                recommendedRepRange = "4-6",
                rmScalingType = RMScalingType.ISOLATION,
                restDurationSeconds = 180,
            )

        val originalMuscles =
            listOf(
                VariationMuscle("200", MuscleGroup.LATS, true, 1.0f),
                VariationMuscle("200", MuscleGroup.BICEPS, false, 0.6f),
            )

        val originalAliases =
            listOf(
                VariationAlias("300", "200", "Test Alias 1", 1.0f, "en", "user"),
                VariationAlias("301", "200", "Test Alias 2", 0.9f, "en", "user"),
            )

        val originalInstructions =
            listOf(
                VariationInstruction("400", "200", InstructionType.EXECUTION, "Step 1", 0, "en"),
                VariationInstruction("401", "200", InstructionType.EXECUTION, "Step 2", 1, "en"),
            )

        // When converting to Firestore and back
        val firestoreExercise =
            ExerciseSyncConverter.toFirestore(
                originalCore,
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

        // Then core data is preserved
        with(reconvertedBundle.exerciseCore) {
            assertEquals(originalCore.name, name)
            assertEquals(originalCore.category, category)
            assertEquals(originalCore.movementPattern, movementPattern)
            assertEquals(originalCore.isCompound, isCompound)
        }

        // And variation data is preserved
        with(reconvertedBundle.exerciseVariation) {
            assertEquals(originalVariation.name, name)
            assertEquals(originalVariation.equipment, equipment)
            assertEquals(originalVariation.difficulty, difficulty)
            assertEquals(originalVariation.requiresWeight, requiresWeight)
            assertEquals(originalVariation.recommendedRepRange, recommendedRepRange)
            assertEquals(originalVariation.rmScalingType, rmScalingType)
            assertEquals(originalVariation.restDurationSeconds, restDurationSeconds)
        }

        // And muscle data is preserved
        assertEquals(originalMuscles.size, reconvertedBundle.variationMuscles.size)
        originalMuscles.zip(reconvertedBundle.variationMuscles).forEach { (original, reconverted) ->
            assertEquals(original.muscle, reconverted.muscle)
            assertEquals(original.isPrimary, reconverted.isPrimary)
            assertEquals(original.emphasisModifier, reconverted.emphasisModifier)
        }

        // And aliases are preserved
        assertEquals(originalAliases.size, reconvertedBundle.variationAliases.size)
        assertEquals(originalAliases[0].alias, reconvertedBundle.variationAliases[0].alias)
        assertEquals(originalAliases[1].alias, reconvertedBundle.variationAliases[1].alias)

        // And instructions are preserved
        assertEquals(originalInstructions.size, reconvertedBundle.variationInstructions.size)
        originalInstructions.zip(reconvertedBundle.variationInstructions).forEach { (original, reconverted) ->
            assertEquals(original.content, reconverted.content)
            assertEquals(original.orderIndex, reconverted.orderIndex)
        }
    }
}
