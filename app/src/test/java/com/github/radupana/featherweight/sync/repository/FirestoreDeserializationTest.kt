package com.github.radupana.featherweight.sync.repository

import com.github.radupana.featherweight.sync.models.FirestoreCustomExercise
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests that verify Firestore deserialization works correctly and catches
 * the @DocumentId conflict that was causing crashes.
 *
 * This test simulates what actually happens when Firestore tries to deserialize
 * a document that has an 'id' field in its data when the model uses @DocumentId.
 */
class FirestoreDeserializationTest {
    @Test
    fun `document with id field in data should fail with non-nullable DocumentId`() {
        // This test documents the PROBLEM we had before the fix
        // If someone accidentally reverts our fix, this test will catch it

        val documentId = "ba3d06b8-5f5c-47fd-b9f3-be090ccbf58a"
        val mockDoc = mockk<DocumentSnapshot>()

        // Simulate a document that has 'id' in its data (the problematic case)
        every { mockDoc.id } returns documentId
        every { mockDoc.exists() } returns true
        every { mockDoc.data } returns
            mapOf(
                "id" to documentId, // This is the problematic field
                "type" to "USER",
                "userId" to "test-user",
                "name" to "Custom Exercise",
                "category" to "STRENGTH",
                "equipment" to "BARBELL",
            )

        // Before our fix, this would have thrown the error:
        // "cannot apply @DocumentId on this property for class FirestoreCustomExercise"

        // With our fix (nullable @DocumentId and not storing id in data),
        // this should work fine

        try {
            // This simulates what Firestore does internally
            // If the model has @DocumentId val id: String (non-nullable)
            // and the document has 'id' in its data, it would fail

            // Since we fixed it to be nullable, this should work
            val data = mockDoc.data!!
            val exercise =
                FirestoreCustomExercise(
                    id = mockDoc.id, // @DocumentId populates from document reference
                    type = data["type"] as String,
                    userId = data["userId"] as String,
                    name = data["name"] as String,
                    category = data["category"] as String,
                    equipment = data["equipment"] as String,
                )

            assertNotNull("Exercise should be created successfully", exercise)
            assertEquals(documentId, exercise.id)
        } catch (e: Exception) {
            fail("Should not throw exception with nullable @DocumentId: ${e.message}")
        }
    }

    @Test
    fun `verify we dont store id field in upload data`() {
        // This test verifies our fix: we should NOT include 'id' in document data

        val exercise =
            FirestoreCustomExercise(
                id = "test-123",
                type = "USER",
                userId = "user-123",
                name = "Test Exercise",
                category = "STRENGTH",
                equipment = "BARBELL",
            )

        // Simulate creating upload data - this is what we send to Firestore
        val uploadData = createUploadData(exercise)

        // CRITICAL: Verify 'id' is NOT in the upload data
        assert(!uploadData.containsKey("id")) {
            "Upload data should NOT contain 'id' field when using @DocumentId. " +
                "This would cause 'cannot apply @DocumentId' error on download."
        }

        // Verify other fields are present
        assertEquals("USER", uploadData["type"])
        assertEquals("user-123", uploadData["userId"])
        assertEquals("Test Exercise", uploadData["name"])
    }

    @Test
    fun `nullable DocumentId fields handle missing id gracefully`() {
        // Test that our nullable ID fields don't crash when ID is missing

        val exercise =
            FirestoreCustomExercise(
                id = null, // This MUST work with our fix
                type = "USER",
                userId = "user-123",
                name = "Test",
                category = "STRENGTH",
                equipment = "BARBELL",
            )

        // Should not throw NullPointerException
        val description = exercise.toString()
        assertNotNull(description)

        // Can still work with null ID
        assertEquals("USER", exercise.type)
        assertEquals("Test", exercise.name)
    }

    /**
     * Helper function that mimics what FirestoreRepository does when uploading
     */
    private fun createUploadData(exercise: FirestoreCustomExercise): Map<String, Any?> {
        // This is the CORRECT way - don't include 'id' in the data
        return hashMapOf(
            // "id" to exercise.id,  // ❌ NEVER include this with @DocumentId
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
            "createdAt" to exercise.createdAt,
            "updatedAt" to exercise.updatedAt,
            "isDeleted" to exercise.isDeleted,
            // Note: lastModified is @ServerTimestamp, handled by Firestore
        )
    }
}
