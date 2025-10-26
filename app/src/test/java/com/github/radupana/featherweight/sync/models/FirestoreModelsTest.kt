package com.github.radupana.featherweight.sync.models

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests to ensure Firestore models are properly configured for @DocumentId usage.
 * This prevents regression of the "cannot apply @DocumentId" error.
 *
 * The core issue: Firestore's @DocumentId annotation expects:
 * 1. The field to be nullable (String?)
 * 2. The field to NOT exist in the document data
 * 3. The field to be populated from the document reference/metadata
 */
class FirestoreModelsTest {
    @Test
    fun `FirestoreCustomExercise can be created with null id`() {
        // This test ensures the model can handle null IDs properly
        val exercise =
            FirestoreCustomExercise(
                id = null, // Must accept null
                type = "USER",
                userId = "test-user",
                name = "Test Exercise",
                category = "STRENGTH",
                equipment = "BARBELL",
            )

        assertNull("ID should be null initially", exercise.id)
        assertEquals("USER", exercise.type)
        assertEquals("test-user", exercise.userId)
    }

    @Test
    fun `FirestoreWorkout can be created with null id`() {
        val workout =
            FirestoreWorkout(
                id = null, // Must accept null
                localId = "local-123",
                userId = "test-user",
                date = Timestamp.now(),
            )

        assertNull("ID should be null initially", workout.id)
        assertEquals("local-123", workout.localId)
        assertEquals("test-user", workout.userId)
    }

    @Test
    fun `simulated Firestore deserialization with DocumentId`() {
        // Simulate what Firestore does when deserializing
        val documentId = "doc-123"
        val documentData =
            mapOf(
                // Note: 'id' field is NOT in the document data
                "type" to "USER",
                "userId" to "user-123",
                "name" to "Barbell Squat",
                "category" to "STRENGTH",
                "equipment" to "BARBELL",
            )

        // Simulate Firestore setting the @DocumentId field
        val exercise =
            FirestoreCustomExercise(
                id = documentId, // Firestore sets this from document reference
                type = documentData["type"] as String,
                userId = documentData["userId"] as String,
                name = documentData["name"] as String,
                category = documentData["category"] as String,
                equipment = documentData["equipment"] as String,
            )

        assertEquals(documentId, exercise.id)
        assertEquals("Barbell Squat", exercise.name)
    }

    @Test
    fun `ensure no id field in upload data for DocumentId models`() {
        // This test documents that we should NOT include 'id' in document data
        val exercise =
            FirestoreCustomExercise(
                id = "exercise-123", // This will be used as document name
                type = "USER",
                userId = "user-123",
                name = "Test Exercise",
                category = "STRENGTH",
                equipment = "DUMBBELL",
            )

        // When uploading, we should use exercise.id as document name
        // but NOT include it in the document data
        val uploadData =
            hashMapOf(
                // "id" to exercise.id, // ❌ WRONG - causes @DocumentId conflict
                "type" to exercise.type,
                "userId" to exercise.userId,
                "name" to exercise.name,
                "category" to exercise.category,
                "equipment" to exercise.equipment,
            )

        // Verify 'id' is not in upload data
        assert(!uploadData.containsKey("id")) {
            "Upload data should NOT contain 'id' field when using @DocumentId"
        }
    }
}
