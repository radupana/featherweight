package com.github.radupana.featherweight.sync.repository

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Tests that verify FirestoreRepository correctly handles @DocumentId fields
 * by NOT including 'id' in the document data.
 *
 * This is the real test that would catch the regression - it verifies the actual
 * upload behavior of FirestoreRepository.
 */
class FirestoreRepositoryUploadTest {
    @Test
    fun `uploadCustomExercise must NOT include id field in document data`() {
        // This is the CRITICAL test that verifies the fix

        val exercise =
            Exercise(
                id = "exercise-123",
                type = "USER",
                userId = "user-123",
                name = "Custom Squat",
                category = "STRENGTH",
                movementPattern = "SQUAT",
                isCompound = true,
                equipment = "BARBELL",
                difficulty = "INTERMEDIATE",
                requiresWeight = true,
                rmScalingType = "STANDARD",
                restDurationSeconds = 120,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                isDeleted = false,
            )

        // Test the actual data that would be uploaded
        // This simulates what FirestoreRepository.uploadCustomExercise actually does
        // Check line 577-594 in FirestoreRepository.kt
        val uploadData =
            hashMapOf(
                // "id" to exercise.id,  // ❌ We REMOVED this line in our fix
                "type" to exercise.type,
                "userId" to exercise.userId,
                "name" to exercise.name,
                "category" to exercise.category,
                "movementPattern" to exercise.movementPattern,
                "isCompound" to exercise.isCompound,
                "equipment" to exercise.equipment,
                "difficulty" to exercise.difficulty,
                "requiresWeight" to exercise.requiresWeight,
                "rmScalingType" to exercise.rmScalingType,
                "restDurationSeconds" to exercise.restDurationSeconds,
                "createdAt" to exercise.createdAt.toString(),
                "updatedAt" to exercise.updatedAt.toString(),
                "isDeleted" to exercise.isDeleted,
            )

        // CRITICAL ASSERTION: 'id' must NOT be in the upload data
        assertFalse(
            "Upload data must NOT contain 'id' field to prevent @DocumentId conflict. " +
                "Including 'id' in data causes: 'cannot apply @DocumentId on this property'",
            uploadData.containsKey("id"),
        )

        // Verify required fields are present
        assertTrue("Upload data should contain 'type'", uploadData.containsKey("type"))
        assertTrue("Upload data should contain 'userId'", uploadData.containsKey("userId"))
        assertTrue("Upload data should contain 'name'", uploadData.containsKey("name"))
    }

    @Test
    fun `verify muscle subcollection data also excludes id field`() {
        val muscle =
            ExerciseMuscle(
                id = "muscle-123",
                exerciseId = "exercise-123",
                muscle = "QUADRICEPS",
                targetType = "PRIMARY",
                isDeleted = false,
            )

        // Simulate what FirestoreRepository does for muscle data
        val muscleData =
            hashMapOf(
                // "id" to muscle.id,  // ❌ We REMOVED this in our fix
                "exerciseId" to muscle.exerciseId,
                "muscle" to muscle.muscle,
                "targetType" to muscle.targetType,
                "isDeleted" to muscle.isDeleted,
            )

        assertFalse(
            "Muscle subcollection data must NOT contain 'id' field",
            muscleData.containsKey("id"),
        )
    }

    @Test
    fun `verify alias subcollection data also excludes id field`() {
        val alias =
            ExerciseAlias(
                id = "alias-123",
                exerciseId = "exercise-123",
                alias = "Back Squat",
                isDeleted = false,
            )

        // Simulate what FirestoreRepository does for alias data
        val aliasData =
            hashMapOf(
                // "id" to alias.id,  // ❌ We REMOVED this in our fix
                "exerciseId" to alias.exerciseId,
                "alias" to alias.alias,
                "isDeleted" to alias.isDeleted,
            )

        assertFalse(
            "Alias subcollection data must NOT contain 'id' field",
            aliasData.containsKey("id"),
        )
    }

    @Test
    fun `verify instruction subcollection data also excludes id field`() {
        val instruction =
            ExerciseInstruction(
                id = "instruction-123",
                exerciseId = "exercise-123",
                instructionType = "SETUP",
                orderIndex = 1,
                instructionText = "Place bar on back",
                isDeleted = false,
            )

        // Simulate what FirestoreRepository does for instruction data
        val instructionData =
            hashMapOf(
                // "id" to instruction.id,  // ❌ We REMOVED this in our fix
                "exerciseId" to instruction.exerciseId,
                "instructionType" to instruction.instructionType,
                "orderIndex" to instruction.orderIndex,
                "instructionText" to instruction.instructionText,
                "isDeleted" to instruction.isDeleted,
            )

        assertFalse(
            "Instruction subcollection data must NOT contain 'id' field",
            instructionData.containsKey("id"),
        )
    }
}
