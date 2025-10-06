package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.google.firebase.Timestamp
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncConvertersTest {
    @Test
    fun `toFirestoreWorkout converts Workout correctly`() {
        val now = LocalDateTime.now()
        val workout =
            Workout(
                id = "1",
                userId = "user123",
                name = "Morning Workout",
                notes = "Great session",
                date = now,
                status = WorkoutStatus.COMPLETED,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                programmeWorkoutName = null,
                isProgrammeWorkout = false,
                durationSeconds = "3600",
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )

        val result = SyncConverters.toFirestoreWorkout(workout)

        assertEquals("1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("Morning Workout", result.name)
        assertEquals("Great session", result.notes)
        assertEquals("COMPLETED", result.status)
    }

    @Test
    fun `toFirestoreWorkout handles null userId`() {
        val workout =
            Workout(
                id = "1",
                userId = null,
                name = "Workout",
                notes = null,
                date = LocalDateTime.now(),
                status = WorkoutStatus.NOT_STARTED,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                programmeWorkoutName = null,
                isProgrammeWorkout = false,
                durationSeconds = null,
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )

        val result = SyncConverters.toFirestoreWorkout(workout)

        assertEquals("", result.userId)
    }

    @Test
    fun `fromFirestoreWorkout converts FirestoreWorkout correctly`() {
        val timestamp = Timestamp.now()
        val firestoreWorkout =
            FirestoreWorkout(
                id = "firebase-id",
                localId = "2",
                userId = "user456",
                name = "Evening Workout",
                notes = "Good form",
                date = timestamp,
                status = "IN_PROGRESS",
                lastModified = timestamp,
            )

        val result = SyncConverters.fromFirestoreWorkout(firestoreWorkout)

        assertEquals("2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("Evening Workout", result.name)
        assertEquals("Good form", result.notes)
        assertEquals(WorkoutStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun `fromFirestoreWorkout handles empty userId`() {
        val firestoreWorkout =
            FirestoreWorkout(
                id = "firebase-id",
                localId = "1",
                userId = "",
                name = "Workout",
                notes = null,
                date = Timestamp.now(),
                status = "NOT_STARTED",
            )

        val result = SyncConverters.fromFirestoreWorkout(firestoreWorkout)

        assertNull(result.userId)
    }

    @Test
    fun `toFirestoreWorkout and fromFirestoreWorkout handle notesUpdatedAt correctly`() {
        val notesTime = LocalDateTime.of(2024, 1, 15, 10, 30)
        val workout =
            Workout(
                id = "1",
                userId = "user123",
                name = "Test Workout",
                notes = "Updated notes",
                notesUpdatedAt = notesTime,
                date = LocalDateTime.now(),
                status = WorkoutStatus.COMPLETED,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                programmeWorkoutName = null,
                isProgrammeWorkout = false,
                durationSeconds = null,
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )

        val firestoreWorkout = SyncConverters.toFirestoreWorkout(workout)
        val convertedBack = SyncConverters.fromFirestoreWorkout(firestoreWorkout)

        assertEquals(notesTime, convertedBack.notesUpdatedAt)
    }

    @Test
    fun `toFirestoreWorkout handles null notesUpdatedAt`() {
        val workout =
            Workout(
                id = "1",
                userId = "user123",
                name = "Test",
                notes = null,
                notesUpdatedAt = null,
                date = LocalDateTime.now(),
                status = WorkoutStatus.NOT_STARTED,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                programmeWorkoutName = null,
                isProgrammeWorkout = false,
                durationSeconds = null,
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )

        val result = SyncConverters.toFirestoreWorkout(workout)
        assertNull(result.notesUpdatedAt)
    }

    @Test
    fun `toFirestoreExerciseLog converts ExerciseLog correctly`() {
        val exerciseLog =
            ExerciseLog(
                id = "10",
                userId = "user123",
                workoutId = "1",
                exerciseId = "5",
                exerciseOrder = 2,
                notes = "Focus on form",
                originalExerciseId = null,
                isSwapped = false,
            )

        val result = SyncConverters.toFirestoreExerciseLog(exerciseLog)

        assertEquals("10", result.localId)
        assertEquals("1", result.workoutId)
        assertEquals("5", result.exerciseId)
        assertEquals(2, result.exerciseOrder)
        assertEquals("Focus on form", result.notes)
    }

    @Test
    fun `fromFirestoreExerciseLog converts FirestoreExerciseLog correctly`() {
        val firestoreLog =
            FirestoreExerciseLog(
                id = "firebase-id",
                localId = "15",
                workoutId = "2",
                exerciseId = "8",
                exerciseOrder = 3,
                notes = "Good pump",
            )

        val result = SyncConverters.fromFirestoreExerciseLog(firestoreLog)

        assertEquals("15", result.id)
        assertEquals("2", result.workoutId)
        assertEquals("8", result.exerciseId)
        assertEquals(3, result.exerciseOrder)
        assertEquals("Good pump", result.notes)
        assertNull(result.userId)
    }

    @Test
    fun `toFirestoreSetLog converts SetLog correctly`() {
        val setLog =
            SetLog(
                id = "20",
                userId = "user123",
                exerciseLogId = "10",
                setOrder = 1,
                targetReps = 12,
                targetWeight = 50.5f,
                actualReps = 10,
                actualWeight = 50.5f,
                actualRpe = 8.5f,
                isCompleted = true,
                completedAt = null,
                tag = "Warmup",
                notes = "Easy",
            )

        val result = SyncConverters.toFirestoreSetLog(setLog)

        assertEquals("20", result.localId)
        assertEquals("10", result.exerciseLogId)
        assertEquals(1, result.setOrder)
        assertEquals(12, result.targetReps)
        assertEquals(50.5f, result.targetWeight)
        assertEquals(10, result.actualReps)
        assertEquals(50.5f, result.actualWeight)
        assertEquals(true, result.isCompleted)
    }

    @Test
    fun `fromFirestoreSetLog converts FirestoreSetLog correctly`() {
        val firestoreLog =
            FirestoreSetLog(
                id = "firebase-id",
                localId = "25",
                exerciseLogId = "15",
                setOrder = 2,
                targetReps = 8,
                targetWeight = 75f,
                actualReps = 8,
                actualWeight = 75f,
                isCompleted = true,
            )

        val result = SyncConverters.fromFirestoreSetLog(firestoreLog)

        assertEquals("25", result.id)
        assertEquals("15", result.exerciseLogId)
        assertEquals(2, result.setOrder)
        assertEquals(8, result.targetReps)
        assertEquals(75f, result.targetWeight)
        assertEquals(8, result.actualReps)
        assertEquals(75f, result.actualWeight)
        assertEquals(true, result.isCompleted)
        assertNull(result.userId)
        assertNull(result.actualRpe)
        assertNull(result.completedAt)
    }

    @Test
    fun `date conversion roundtrip maintains consistency`() {
        val originalDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45)
        val workout =
            Workout(
                id = "1",
                userId = "user",
                name = "Test",
                notes = null,
                date = originalDate,
                status = WorkoutStatus.NOT_STARTED,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                programmeWorkoutName = null,
                isProgrammeWorkout = false,
                durationSeconds = null,
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )

        val firestoreWorkout = SyncConverters.toFirestoreWorkout(workout)
        val convertedBack = SyncConverters.fromFirestoreWorkout(firestoreWorkout)

        assertEquals(
            originalDate.atZone(ZoneId.systemDefault()).toInstant().epochSecond,
            convertedBack.date
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .epochSecond,
        )
    }

    @Test
    fun `toFirestoreExerciseUsage converts UserExerciseUsage correctly`() {
        val lastUsed = LocalDateTime.of(2024, 1, 15, 10, 30)
        val created = LocalDateTime.of(2024, 1, 1, 9, 0)
        val updated = LocalDateTime.of(2024, 1, 15, 10, 30)

        val usage =
            com.github.radupana.featherweight.data.exercise.UserExerciseUsage(
                id = "usage-1",
                userId = "user123",
                exerciseId = "exercise-5",
                usageCount = 42,
                lastUsedAt = lastUsed,
                personalNotes = "My favorite exercise",
                createdAt = created,
                updatedAt = updated,
            )

        val result = SyncConverters.toFirestoreExerciseUsage(usage)

        assertEquals("usage-1", result.id)
        assertEquals("usage-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("exercise-5", result.exerciseId)
        assertEquals(42, result.usageCount)
        assertEquals("My favorite exercise", result.personalNotes)
    }

    @Test
    fun `toFirestoreExerciseUsage handles null lastUsedAt`() {
        val usage =
            com.github.radupana.featherweight.data.exercise.UserExerciseUsage(
                id = "usage-1",
                userId = "user123",
                exerciseId = "exercise-5",
                usageCount = 0,
                lastUsedAt = null,
                personalNotes = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val result = SyncConverters.toFirestoreExerciseUsage(usage)

        assertNull(result.lastUsedAt)
        assertNull(result.personalNotes)
    }

    @Test
    fun `fromFirestoreExerciseUsage converts FirestoreExerciseUsage correctly`() {
        val firestoreUsage =
            com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage(
                id = "firebase-id",
                localId = "usage-2",
                userId = "user456",
                exerciseId = "exercise-8",
                usageCount = 15,
                lastUsedAt = Timestamp.now(),
                personalNotes = "Great for strength",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )

        val result = SyncConverters.fromFirestoreExerciseUsage(firestoreUsage)

        assertEquals("usage-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("exercise-8", result.exerciseId)
        assertEquals(15, result.usageCount)
        assertEquals("Great for strength", result.personalNotes)
    }

    @Test
    fun `fromFirestoreExerciseUsage handles null timestamps`() {
        val firestoreUsage =
            com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage(
                id = "firebase-id",
                localId = "usage-3",
                userId = "user789",
                exerciseId = "exercise-10",
                usageCount = 5,
                lastUsedAt = null,
                personalNotes = null,
                createdAt = null,
                updatedAt = null,
            )

        val result = SyncConverters.fromFirestoreExerciseUsage(firestoreUsage)

        assertNull(result.lastUsedAt)
        assertNull(result.personalNotes)
    }

    @Test
    fun `toFirestoreExerciseUsage and fromFirestoreExerciseUsage roundtrip maintains data integrity`() {
        val lastUsed = LocalDateTime.of(2024, 1, 15, 10, 30)
        val created = LocalDateTime.of(2024, 1, 1, 9, 0)
        val updated = LocalDateTime.of(2024, 1, 15, 10, 30)

        val originalUsage =
            com.github.radupana.featherweight.data.exercise.UserExerciseUsage(
                id = "usage-roundtrip",
                userId = "user-roundtrip",
                exerciseId = "exercise-roundtrip",
                usageCount = 99,
                lastUsedAt = lastUsed,
                personalNotes = "Test notes for roundtrip",
                createdAt = created,
                updatedAt = updated,
            )

        val firestoreUsage = SyncConverters.toFirestoreExerciseUsage(originalUsage)
        val convertedBack = SyncConverters.fromFirestoreExerciseUsage(firestoreUsage)

        assertEquals(originalUsage.id, convertedBack.id)
        assertEquals(originalUsage.userId, convertedBack.userId)
        assertEquals(originalUsage.exerciseId, convertedBack.exerciseId)
        assertEquals(originalUsage.usageCount, convertedBack.usageCount)
        assertEquals(originalUsage.personalNotes, convertedBack.personalNotes)
        assertEquals(
            lastUsed.atZone(ZoneId.systemDefault()).toInstant().epochSecond,
            convertedBack.lastUsedAt
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.epochSecond,
        )
    }
}
