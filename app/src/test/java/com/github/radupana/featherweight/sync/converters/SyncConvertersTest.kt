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
}
