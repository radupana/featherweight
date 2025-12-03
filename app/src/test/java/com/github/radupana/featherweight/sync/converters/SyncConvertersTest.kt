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
    fun `toFirestoreSetLog converts undo tracking fields correctly`() {
        val setLog =
            SetLog(
                id = "30",
                userId = "user123",
                exerciseLogId = "10",
                setOrder = 1,
                actualReps = 5,
                actualWeight = 100f,
                isCompleted = true,
                triggeredUsageIncrement = true,
                previous1RMEstimate = 95.5f,
            )

        val result = SyncConverters.toFirestoreSetLog(setLog)

        assertEquals(true, result.triggeredUsageIncrement)
        assertEquals(95.5f, result.previous1RMEstimate)
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
    fun `fromFirestoreSetLog converts undo tracking fields correctly`() {
        val firestoreLog =
            FirestoreSetLog(
                id = "firebase-id",
                localId = "35",
                exerciseLogId = "15",
                setOrder = 1,
                actualReps = 5,
                actualWeight = 100f,
                isCompleted = true,
                triggeredUsageIncrement = true,
                previous1RMEstimate = 95.5f,
            )

        val result = SyncConverters.fromFirestoreSetLog(firestoreLog)

        assertEquals(true, result.triggeredUsageIncrement)
        assertEquals(95.5f, result.previous1RMEstimate)
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

    @Test
    fun `toFirestorePersonalRecord converts sourceSetId correctly`() {
        val record =
            com.github.radupana.featherweight.data.PersonalRecord(
                id = "pr-1",
                userId = "user123",
                exerciseId = "exercise-1",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = 95f,
                previousReps = 5,
                previousDate = null,
                improvementPercentage = 5.26f,
                recordType = com.github.radupana.featherweight.data.PRType.WEIGHT,
                sourceSetId = "set-123",
            )

        val result = SyncConverters.toFirestorePersonalRecord(record)

        assertEquals("set-123", result.sourceSetId)
    }

    @Test
    fun `fromFirestorePersonalRecord converts sourceSetId correctly`() {
        val firestoreRecord =
            com.github.radupana.featherweight.sync.models.FirestorePersonalRecord(
                id = "firebase-id",
                localId = "pr-2",
                userId = "user456",
                exerciseId = "exercise-2",
                weight = 110f,
                reps = 3,
                recordDate = Timestamp.now(),
                recordType = "WEIGHT",
                sourceSetId = "set-456",
            )

        val result = SyncConverters.fromFirestorePersonalRecord(firestoreRecord)

        assertEquals("set-456", result.sourceSetId)
    }

    @Test
    fun `fromFirestorePersonalRecord handles null sourceSetId`() {
        val firestoreRecord =
            com.github.radupana.featherweight.sync.models.FirestorePersonalRecord(
                id = "firebase-id",
                localId = "pr-3",
                userId = "user789",
                exerciseId = "exercise-3",
                weight = 120f,
                reps = 1,
                recordDate = Timestamp.now(),
                recordType = "WEIGHT",
                sourceSetId = null,
            )

        val result = SyncConverters.fromFirestorePersonalRecord(firestoreRecord)

        assertNull(result.sourceSetId)
    }

    // =====================================================
    // PROGRAMME CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreProgramme converts Programme correctly`() {
        val createdAt = LocalDateTime.of(2024, 1, 1, 9, 0)
        val startedAt = LocalDateTime.of(2024, 1, 5, 10, 30)

        val programme =
            com.github.radupana.featherweight.data.programme.Programme(
                id = "prog-1",
                userId = "user123",
                name = "5x5 Strength",
                description = "Progressive strength programme",
                durationWeeks = 12,
                programmeType = com.github.radupana.featherweight.data.programme.ProgrammeType.STRENGTH,
                difficulty = com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.INTERMEDIATE,
                isCustom = false,
                isActive = true,
                status = com.github.radupana.featherweight.data.programme.ProgrammeStatus.IN_PROGRESS,
                createdAt = createdAt,
                startedAt = startedAt,
                completedAt = null,
                completionNotes = null,
                notesCreatedAt = null,
                squatMax = 140f,
                benchMax = 100f,
                deadliftMax = 180f,
                ohpMax = 60f,
                weightCalculationRules = """{"type":"percentage"}""",
                progressionRules = """{"increase":2.5}""",
                templateName = "StrongLifts",
            )

        val result = SyncConverters.toFirestoreProgramme(programme)

        assertEquals("prog-1", result.id)
        assertEquals("prog-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("5x5 Strength", result.name)
        assertEquals("Progressive strength programme", result.description)
        assertEquals(12, result.durationWeeks)
        assertEquals("STRENGTH", result.programmeType)
        assertEquals("INTERMEDIATE", result.difficulty)
        assertEquals(false, result.isCustom)
        assertEquals(true, result.isActive)
        assertEquals("IN_PROGRESS", result.status)
        assertEquals(140f, result.squatMax)
        assertEquals(100f, result.benchMax)
        assertEquals(180f, result.deadliftMax)
        assertEquals(60f, result.ohpMax)
        assertEquals("""{"type":"percentage"}""", result.weightCalculationRules)
        assertEquals("""{"increase":2.5}""", result.progressionRules)
        assertEquals("StrongLifts", result.templateName)
    }

    @Test
    fun `fromFirestoreProgramme converts FirestoreProgramme correctly`() {
        val createdAt = Timestamp.now()
        val startedAt = Timestamp.now()
        val completedAt = Timestamp.now()
        val notesCreatedAt = Timestamp.now()

        val firestoreProgramme =
            com.github.radupana.featherweight.sync.models.FirestoreProgramme(
                id = null,
                localId = "prog-2",
                userId = "user456",
                name = "Hypertrophy Blast",
                description = "Volume-focused training",
                durationWeeks = 8,
                programmeType = "BODYBUILDING",
                difficulty = "ADVANCED",
                isCustom = true,
                isActive = false,
                status = "COMPLETED",
                createdAt = createdAt,
                startedAt = startedAt,
                completedAt = completedAt,
                completionNotes = "Great progress!",
                notesCreatedAt = notesCreatedAt,
                squatMax = 150f,
                benchMax = 110f,
                deadliftMax = 200f,
                ohpMax = 70f,
                weightCalculationRules = """{"method":"volume"}""",
                progressionRules = """{"style":"wave"}""",
                templateName = "CustomHypertrophy",
            )

        val result = SyncConverters.fromFirestoreProgramme(firestoreProgramme)

        assertEquals("prog-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("Hypertrophy Blast", result.name)
        assertEquals("Volume-focused training", result.description)
        assertEquals(8, result.durationWeeks)
        assertEquals(
            com.github.radupana.featherweight.data.programme.ProgrammeType.BODYBUILDING,
            result.programmeType,
        )
        assertEquals(
            com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.ADVANCED,
            result.difficulty,
        )
        assertEquals(true, result.isCustom)
        assertEquals(false, result.isActive)
        assertEquals(
            com.github.radupana.featherweight.data.programme.ProgrammeStatus.COMPLETED,
            result.status,
        )
        assertEquals("Great progress!", result.completionNotes)
        assertEquals(150f, result.squatMax)
        assertEquals(110f, result.benchMax)
        assertEquals(200f, result.deadliftMax)
        assertEquals(70f, result.ohpMax)
        assertEquals("""{"method":"volume"}""", result.weightCalculationRules)
        assertEquals("""{"style":"wave"}""", result.progressionRules)
        assertEquals("CustomHypertrophy", result.templateName)
    }

    @Test
    fun `toFirestoreProgramme and fromFirestoreProgramme roundtrip maintains data integrity`() {
        val createdAt = LocalDateTime.of(2024, 1, 1, 9, 0)
        val startedAt = LocalDateTime.of(2024, 1, 5, 10, 30)

        val originalProgramme =
            com.github.radupana.featherweight.data.programme.Programme(
                id = "prog-roundtrip",
                userId = "user-roundtrip",
                name = "Test Programme",
                description = "For testing roundtrip",
                durationWeeks = 10,
                programmeType = com.github.radupana.featherweight.data.programme.ProgrammeType.POWERLIFTING,
                difficulty = com.github.radupana.featherweight.data.programme.ProgrammeDifficulty.EXPERT,
                isCustom = false,
                isActive = true,
                status = com.github.radupana.featherweight.data.programme.ProgrammeStatus.IN_PROGRESS,
                createdAt = createdAt,
                startedAt = startedAt,
                completedAt = null,
                completionNotes = null,
                notesCreatedAt = null,
                squatMax = 160f,
                benchMax = 120f,
                deadliftMax = 220f,
                ohpMax = 80f,
                weightCalculationRules = """{"type":"test"}""",
                progressionRules = """{"rate":"fast"}""",
                templateName = "TestTemplate",
            )

        val firestoreProgramme = SyncConverters.toFirestoreProgramme(originalProgramme)
        val convertedBack = SyncConverters.fromFirestoreProgramme(firestoreProgramme)

        assertEquals(originalProgramme.id, convertedBack.id)
        assertEquals(originalProgramme.userId, convertedBack.userId)
        assertEquals(originalProgramme.name, convertedBack.name)
        assertEquals(originalProgramme.description, convertedBack.description)
        assertEquals(originalProgramme.durationWeeks, convertedBack.durationWeeks)
        assertEquals(originalProgramme.programmeType, convertedBack.programmeType)
        assertEquals(originalProgramme.difficulty, convertedBack.difficulty)
        assertEquals(originalProgramme.isCustom, convertedBack.isCustom)
        assertEquals(originalProgramme.isActive, convertedBack.isActive)
        assertEquals(originalProgramme.status, convertedBack.status)
        assertEquals(originalProgramme.squatMax, convertedBack.squatMax)
        assertEquals(originalProgramme.benchMax, convertedBack.benchMax)
        assertEquals(originalProgramme.deadliftMax, convertedBack.deadliftMax)
        assertEquals(originalProgramme.ohpMax, convertedBack.ohpMax)
        assertEquals(originalProgramme.weightCalculationRules, convertedBack.weightCalculationRules)
        assertEquals(originalProgramme.progressionRules, convertedBack.progressionRules)
        assertEquals(originalProgramme.templateName, convertedBack.templateName)
    }

    @Test
    fun `toFirestoreProgrammeWeek converts ProgrammeWeek correctly`() {
        val week =
            com.github.radupana.featherweight.data.programme.ProgrammeWeek(
                id = "week-1",
                userId = "user123",
                programmeId = "prog-1",
                weekNumber = 3,
                name = "Week 3 - Heavy",
                description = "Focus on compound lifts",
            )

        val result = SyncConverters.toFirestoreProgrammeWeek(week)

        assertEquals("week-1", result.id)
        assertEquals("week-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("prog-1", result.programmeId)
        assertEquals(3, result.weekNumber)
        assertEquals("Week 3 - Heavy", result.name)
        assertEquals("Focus on compound lifts", result.description)
    }

    @Test
    fun `fromFirestoreProgrammeWeek converts FirestoreProgrammeWeek correctly`() {
        val firestoreWeek =
            com.github.radupana.featherweight.sync.models.FirestoreProgrammeWeek(
                id = null,
                localId = "week-2",
                userId = "user456",
                programmeId = "prog-2",
                weekNumber = 5,
                name = "Week 5 - Deload",
                description = "Recovery week",
            )

        val result = SyncConverters.fromFirestoreProgrammeWeek(firestoreWeek)

        assertEquals("week-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("prog-2", result.programmeId)
        assertEquals(5, result.weekNumber)
        assertEquals("Week 5 - Deload", result.name)
        assertEquals("Recovery week", result.description)
    }

    @Test
    fun `toFirestoreProgrammeWeek and fromFirestoreProgrammeWeek roundtrip maintains data integrity`() {
        val originalWeek =
            com.github.radupana.featherweight.data.programme.ProgrammeWeek(
                id = "week-roundtrip",
                userId = "user-roundtrip",
                programmeId = "prog-roundtrip",
                weekNumber = 7,
                name = "Week 7 - Test",
                description = "Test description",
            )

        val firestoreWeek = SyncConverters.toFirestoreProgrammeWeek(originalWeek)
        val convertedBack = SyncConverters.fromFirestoreProgrammeWeek(firestoreWeek)

        assertEquals(originalWeek.id, convertedBack.id)
        assertEquals(originalWeek.userId, convertedBack.userId)
        assertEquals(originalWeek.programmeId, convertedBack.programmeId)
        assertEquals(originalWeek.weekNumber, convertedBack.weekNumber)
        assertEquals(originalWeek.name, convertedBack.name)
        assertEquals(originalWeek.description, convertedBack.description)
    }

    @Test
    fun `toFirestoreProgrammeWorkout converts ProgrammeWorkout correctly`() {
        val workout =
            com.github.radupana.featherweight.data.programme.ProgrammeWorkout(
                id = "workout-1",
                userId = "user123",
                weekId = "week-1",
                dayNumber = 1,
                name = "Day 1 - Squat Focus",
                description = "Main squat work",
                estimatedDuration = 90,
                workoutStructure = """{"exercises":[{"name":"Squat","sets":5}]}""",
            )

        val result = SyncConverters.toFirestoreProgrammeWorkout(workout)

        assertEquals("workout-1", result.id)
        assertEquals("workout-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("week-1", result.weekId)
        assertEquals(1, result.dayNumber)
        assertEquals("Day 1 - Squat Focus", result.name)
        assertEquals("Main squat work", result.description)
        assertEquals(90, result.estimatedDuration)
        assertEquals("""{"exercises":[{"name":"Squat","sets":5}]}""", result.workoutStructure)
    }

    @Test
    fun `fromFirestoreProgrammeWorkout converts FirestoreProgrammeWorkout correctly`() {
        val firestoreWorkout =
            com.github.radupana.featherweight.sync.models.FirestoreProgrammeWorkout(
                id = null,
                localId = "workout-2",
                userId = "user456",
                weekId = "week-2",
                dayNumber = 3,
                name = "Day 3 - Bench Press",
                description = "Upper body focus",
                estimatedDuration = 75,
                workoutStructure = """{"exercises":[{"name":"Bench","sets":4}]}""",
            )

        val result = SyncConverters.fromFirestoreProgrammeWorkout(firestoreWorkout)

        assertEquals("workout-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("week-2", result.weekId)
        assertEquals(3, result.dayNumber)
        assertEquals("Day 3 - Bench Press", result.name)
        assertEquals("Upper body focus", result.description)
        assertEquals(75, result.estimatedDuration)
        assertEquals("""{"exercises":[{"name":"Bench","sets":4}]}""", result.workoutStructure)
    }

    @Test
    fun `toFirestoreProgrammeWorkout and fromFirestoreProgrammeWorkout roundtrip maintains data integrity`() {
        val originalWorkout =
            com.github.radupana.featherweight.data.programme.ProgrammeWorkout(
                id = "workout-roundtrip",
                userId = "user-roundtrip",
                weekId = "week-roundtrip",
                dayNumber = 5,
                name = "Test Workout",
                description = "Test description",
                estimatedDuration = 60,
                workoutStructure = """{"test":"data"}""",
            )

        val firestoreWorkout = SyncConverters.toFirestoreProgrammeWorkout(originalWorkout)
        val convertedBack = SyncConverters.fromFirestoreProgrammeWorkout(firestoreWorkout)

        assertEquals(originalWorkout.id, convertedBack.id)
        assertEquals(originalWorkout.userId, convertedBack.userId)
        assertEquals(originalWorkout.weekId, convertedBack.weekId)
        assertEquals(originalWorkout.dayNumber, convertedBack.dayNumber)
        assertEquals(originalWorkout.name, convertedBack.name)
        assertEquals(originalWorkout.description, convertedBack.description)
        assertEquals(originalWorkout.estimatedDuration, convertedBack.estimatedDuration)
        assertEquals(originalWorkout.workoutStructure, convertedBack.workoutStructure)
    }

    @Test
    fun `toFirestoreProgrammeProgress converts ProgrammeProgress correctly`() {
        val lastWorkoutDate = LocalDateTime.of(2024, 1, 15, 18, 30)

        val progress =
            com.github.radupana.featherweight.data.programme.ProgrammeProgress(
                id = "progress-1",
                userId = "user123",
                programmeId = "prog-1",
                currentWeek = 4,
                currentDay = 2,
                completedWorkouts = 15,
                totalWorkouts = 36,
                lastWorkoutDate = lastWorkoutDate,
            )

        val result = SyncConverters.toFirestoreProgrammeProgress(progress)

        assertEquals("progress-1", result.id)
        assertEquals("progress-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("prog-1", result.programmeId)
        assertEquals(4, result.currentWeek)
        assertEquals(2, result.currentDay)
        assertEquals(15, result.completedWorkouts)
        assertEquals(36, result.totalWorkouts)
    }

    @Test
    fun `fromFirestoreProgrammeProgress converts FirestoreProgrammeProgress correctly`() {
        val lastWorkoutDate = Timestamp.now()

        val firestoreProgress =
            com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress(
                id = null,
                localId = "progress-2",
                userId = "user456",
                programmeId = "prog-2",
                currentWeek = 6,
                currentDay = 4,
                completedWorkouts = 22,
                totalWorkouts = 32,
                lastWorkoutDate = lastWorkoutDate,
            )

        val result = SyncConverters.fromFirestoreProgrammeProgress(firestoreProgress)

        assertEquals("progress-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("prog-2", result.programmeId)
        assertEquals(6, result.currentWeek)
        assertEquals(4, result.currentDay)
        assertEquals(22, result.completedWorkouts)
        assertEquals(32, result.totalWorkouts)
    }

    @Test
    fun `toFirestoreProgrammeProgress handles null lastWorkoutDate`() {
        val progress =
            com.github.radupana.featherweight.data.programme.ProgrammeProgress(
                id = "progress-3",
                userId = "user789",
                programmeId = "prog-3",
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = 24,
                lastWorkoutDate = null,
            )

        val result = SyncConverters.toFirestoreProgrammeProgress(progress)

        assertNull(result.lastWorkoutDate)
    }

    @Test
    fun `fromFirestoreProgrammeProgress handles null lastWorkoutDate`() {
        val firestoreProgress =
            com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress(
                id = null,
                localId = "progress-4",
                userId = "user890",
                programmeId = "prog-4",
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = 18,
                lastWorkoutDate = null,
            )

        val result = SyncConverters.fromFirestoreProgrammeProgress(firestoreProgress)

        assertNull(result.lastWorkoutDate)
    }

    // =====================================================
    // USER EXERCISE MAX CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreUserExerciseMax converts ExerciseMaxTracking correctly`() {
        val mostWeightDate = LocalDateTime.of(2024, 1, 10, 14, 30)
        val recordedAt = LocalDateTime.of(2024, 1, 10, 14, 45)

        val tracking =
            com.github.radupana.featherweight.data.profile.ExerciseMaxTracking(
                id = "max-1",
                userId = "user123",
                exerciseId = "exercise-1",
                sourceSetId = "set-1",
                mostWeightLifted = 140f,
                mostWeightReps = 5,
                mostWeightRpe = 8.5f,
                mostWeightDate = mostWeightDate,
                oneRMEstimate = 157.5f,
                context = "Estimated from 5x140",
                oneRMConfidence = 0.85f,
                recordedAt = recordedAt,
                oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
                notes = "Good form",
            )

        val result = SyncConverters.toFirestoreUserExerciseMax(tracking)

        assertEquals("max-1", result.id)
        assertEquals("max-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("exercise-1", result.exerciseId)
        assertEquals("set-1", result.sourceSetId)
        assertEquals(140f, result.mostWeightLifted)
        assertEquals(5, result.mostWeightReps)
        assertEquals(8.5f, result.mostWeightRpe)
        assertEquals(157.5f, result.oneRMEstimate)
        assertEquals("Estimated from 5x140", result.oneRMContext)
        assertEquals(0.85f, result.oneRMConfidence)
        assertEquals("AUTOMATICALLY_CALCULATED", result.oneRMType)
        assertEquals("Good form", result.notes)
    }

    @Test
    fun `fromFirestoreUserExerciseMax converts FirestoreUserExerciseMax correctly`() {
        val mostWeightDate = Timestamp.now()
        val oneRMDate = Timestamp.now()

        val firestoreMax =
            com.github.radupana.featherweight.sync.models.FirestoreUserExerciseMax(
                id = null,
                localId = "max-2",
                userId = "user456",
                exerciseId = "exercise-2",
                sourceSetId = "set-2",
                mostWeightLifted = 100f,
                mostWeightReps = 3,
                mostWeightRpe = 9f,
                mostWeightDate = mostWeightDate,
                oneRMEstimate = 106.5f,
                oneRMContext = "Estimated from 3x100",
                oneRMConfidence = 0.9f,
                oneRMDate = oneRMDate,
                oneRMType = "MANUALLY_ENTERED",
                notes = "PR day",
            )

        val result = SyncConverters.fromFirestoreUserExerciseMax(firestoreMax)

        assertEquals("max-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("exercise-2", result.exerciseId)
        assertEquals("set-2", result.sourceSetId)
        assertEquals(100f, result.mostWeightLifted)
        assertEquals(3, result.mostWeightReps)
        assertEquals(9f, result.mostWeightRpe)
        assertEquals(106.5f, result.oneRMEstimate)
        assertEquals("Estimated from 3x100", result.context)
        assertEquals(0.9f, result.oneRMConfidence)
        assertEquals(
            com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
            result.oneRMType,
        )
        assertEquals("PR day", result.notes)
    }

    @Test
    fun `toFirestoreUserExerciseMax and fromFirestoreUserExerciseMax roundtrip maintains data integrity`() {
        val mostWeightDate = LocalDateTime.of(2024, 1, 20, 15, 0)
        val recordedAt = LocalDateTime.of(2024, 1, 20, 15, 15)

        val originalTracking =
            com.github.radupana.featherweight.data.profile.ExerciseMaxTracking(
                id = "max-roundtrip",
                userId = "user-roundtrip",
                exerciseId = "exercise-roundtrip",
                sourceSetId = "set-roundtrip",
                mostWeightLifted = 180f,
                mostWeightReps = 1,
                mostWeightRpe = 10f,
                mostWeightDate = mostWeightDate,
                oneRMEstimate = 180f,
                context = "Actual 1RM",
                oneRMConfidence = 1.0f,
                recordedAt = recordedAt,
                oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                notes = "Test max",
            )

        val firestoreMax = SyncConverters.toFirestoreUserExerciseMax(originalTracking)
        val convertedBack = SyncConverters.fromFirestoreUserExerciseMax(firestoreMax)

        assertEquals(originalTracking.id, convertedBack.id)
        assertEquals(originalTracking.userId, convertedBack.userId)
        assertEquals(originalTracking.exerciseId, convertedBack.exerciseId)
        assertEquals(originalTracking.sourceSetId, convertedBack.sourceSetId)
        assertEquals(originalTracking.mostWeightLifted, convertedBack.mostWeightLifted)
        assertEquals(originalTracking.mostWeightReps, convertedBack.mostWeightReps)
        assertEquals(originalTracking.mostWeightRpe, convertedBack.mostWeightRpe)
        assertEquals(originalTracking.oneRMEstimate, convertedBack.oneRMEstimate)
        assertEquals(originalTracking.context, convertedBack.context)
        assertEquals(originalTracking.oneRMConfidence, convertedBack.oneRMConfidence)
        assertEquals(originalTracking.oneRMType, convertedBack.oneRMType)
        assertEquals(originalTracking.notes, convertedBack.notes)
    }

    // =====================================================
    // EXERCISE SWAP HISTORY CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreExerciseSwapHistory converts ExerciseSwapHistory correctly`() {
        val swapDate = LocalDateTime.of(2024, 1, 12, 16, 20)

        val swap =
            com.github.radupana.featherweight.data.ExerciseSwapHistory(
                id = "swap-1",
                userId = "user123",
                originalExerciseId = "exercise-1",
                swappedToExerciseId = "exercise-2",
                swapDate = swapDate,
                workoutId = "workout-1",
                programmeId = "prog-1",
            )

        val result = SyncConverters.toFirestoreExerciseSwapHistory(swap)

        assertEquals("swap-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("exercise-1", result.originalExerciseId)
        assertEquals("exercise-2", result.swappedToExerciseId)
        assertEquals("workout-1", result.workoutId)
        assertEquals("prog-1", result.programmeId)
    }

    @Test
    fun `fromFirestoreExerciseSwapHistory converts FirestoreExerciseSwapHistory correctly`() {
        val swapDate = Timestamp.now()

        val firestoreSwap =
            com.github.radupana.featherweight.sync.models.FirestoreExerciseSwapHistory(
                id = null,
                localId = "swap-2",
                userId = "user456",
                originalExerciseId = "exercise-3",
                swappedToExerciseId = "exercise-4",
                swapDate = swapDate,
                workoutId = "workout-2",
                programmeId = null,
            )

        val result = SyncConverters.fromFirestoreExerciseSwapHistory(firestoreSwap)

        assertEquals("swap-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("exercise-3", result.originalExerciseId)
        assertEquals("exercise-4", result.swappedToExerciseId)
        assertEquals("workout-2", result.workoutId)
        assertNull(result.programmeId)
    }

    @Test
    fun `toFirestoreExerciseSwapHistory and fromFirestoreExerciseSwapHistory roundtrip maintains data integrity`() {
        val swapDate = LocalDateTime.of(2024, 1, 25, 11, 45)

        val originalSwap =
            com.github.radupana.featherweight.data.ExerciseSwapHistory(
                id = "swap-roundtrip",
                userId = "user-roundtrip",
                originalExerciseId = "exercise-orig",
                swappedToExerciseId = "exercise-new",
                swapDate = swapDate,
                workoutId = "workout-roundtrip",
                programmeId = "prog-roundtrip",
            )

        val firestoreSwap = SyncConverters.toFirestoreExerciseSwapHistory(originalSwap)
        val convertedBack = SyncConverters.fromFirestoreExerciseSwapHistory(firestoreSwap)

        assertEquals(originalSwap.id, convertedBack.id)
        assertEquals(originalSwap.userId, convertedBack.userId)
        assertEquals(originalSwap.originalExerciseId, convertedBack.originalExerciseId)
        assertEquals(originalSwap.swappedToExerciseId, convertedBack.swappedToExerciseId)
        assertEquals(originalSwap.workoutId, convertedBack.workoutId)
        assertEquals(originalSwap.programmeId, convertedBack.programmeId)
    }

    // =====================================================
    // PROGRAMME EXERCISE TRACKING CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreProgrammeExerciseTracking converts ProgrammeExerciseTracking correctly`() {
        val workoutDate = LocalDateTime.of(2024, 1, 18, 17, 0)

        val tracking =
            com.github.radupana.featherweight.data.ProgrammeExerciseTracking(
                id = "tracking-1",
                userId = "user123",
                programmeId = "prog-1",
                exerciseId = "exercise-1",
                exerciseName = "Barbell Squat",
                targetWeight = 140f,
                achievedWeight = 140f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 25,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = workoutDate,
                workoutId = "workout-1",
                isDeloadWorkout = false,
                averageRpe = 8f,
            )

        val result = SyncConverters.toFirestoreProgrammeExerciseTracking(tracking)

        assertEquals("tracking-1", result.id)
        assertEquals("tracking-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("prog-1", result.programmeId)
        assertEquals("exercise-1", result.exerciseId)
        assertEquals("Barbell Squat", result.exerciseName)
        assertEquals(140f, result.targetWeight)
        assertEquals(140f, result.achievedWeight)
        assertEquals(5, result.targetSets)
        assertEquals(5, result.completedSets)
        assertEquals(5, result.targetReps)
        assertEquals(25, result.achievedReps)
        assertEquals(0, result.missedReps)
        assertEquals(true, result.wasSuccessful)
        assertEquals("workout-1", result.workoutId)
        assertEquals(false, result.isDeloadWorkout)
        assertEquals(8f, result.averageRpe)
    }

    @Test
    fun `fromFirestoreProgrammeExerciseTracking converts FirestoreProgrammeExerciseTracking correctly`() {
        val workoutDate = Timestamp.now()

        val firestoreTracking =
            com.github.radupana.featherweight.sync.models.FirestoreProgrammeExerciseTracking(
                id = null,
                localId = "tracking-2",
                userId = "user456",
                programmeId = "prog-2",
                exerciseId = "exercise-2",
                exerciseName = "Bench Press",
                targetWeight = 100f,
                achievedWeight = 95f,
                targetSets = 4,
                completedSets = 3,
                targetReps = 8,
                achievedReps = 20,
                missedReps = 4,
                wasSuccessful = false,
                workoutDate = workoutDate,
                workoutId = "workout-2",
                isDeloadWorkout = true,
                averageRpe = 9.5f,
            )

        val result = SyncConverters.fromFirestoreProgrammeExerciseTracking(firestoreTracking)

        assertEquals("tracking-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("prog-2", result.programmeId)
        assertEquals("exercise-2", result.exerciseId)
        assertEquals("Bench Press", result.exerciseName)
        assertEquals(100f, result.targetWeight)
        assertEquals(95f, result.achievedWeight)
        assertEquals(4, result.targetSets)
        assertEquals(3, result.completedSets)
        assertEquals(8, result.targetReps)
        assertEquals(20, result.achievedReps)
        assertEquals(4, result.missedReps)
        assertEquals(false, result.wasSuccessful)
        assertEquals("workout-2", result.workoutId)
        assertEquals(true, result.isDeloadWorkout)
        assertEquals(9.5f, result.averageRpe)
    }

    @Test
    fun `toFirestoreProgrammeExerciseTracking and fromFirestoreProgrammeExerciseTracking roundtrip maintains data integrity`() {
        val workoutDate = LocalDateTime.of(2024, 1, 22, 19, 30)

        val originalTracking =
            com.github.radupana.featherweight.data.ProgrammeExerciseTracking(
                id = "tracking-roundtrip",
                userId = "user-roundtrip",
                programmeId = "prog-roundtrip",
                exerciseId = "exercise-roundtrip",
                exerciseName = "Deadlift",
                targetWeight = 180f,
                achievedWeight = 180f,
                targetSets = 3,
                completedSets = 3,
                targetReps = 3,
                achievedReps = 9,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = workoutDate,
                workoutId = "workout-roundtrip",
                isDeloadWorkout = false,
                averageRpe = 7.5f,
            )

        val firestoreTracking = SyncConverters.toFirestoreProgrammeExerciseTracking(originalTracking)
        val convertedBack = SyncConverters.fromFirestoreProgrammeExerciseTracking(firestoreTracking)

        assertEquals(originalTracking.id, convertedBack.id)
        assertEquals(originalTracking.userId, convertedBack.userId)
        assertEquals(originalTracking.programmeId, convertedBack.programmeId)
        assertEquals(originalTracking.exerciseId, convertedBack.exerciseId)
        assertEquals(originalTracking.exerciseName, convertedBack.exerciseName)
        assertEquals(originalTracking.targetWeight, convertedBack.targetWeight)
        assertEquals(originalTracking.achievedWeight, convertedBack.achievedWeight)
        assertEquals(originalTracking.targetSets, convertedBack.targetSets)
        assertEquals(originalTracking.completedSets, convertedBack.completedSets)
        assertEquals(originalTracking.targetReps, convertedBack.targetReps)
        assertEquals(originalTracking.achievedReps, convertedBack.achievedReps)
        assertEquals(originalTracking.missedReps, convertedBack.missedReps)
        assertEquals(originalTracking.wasSuccessful, convertedBack.wasSuccessful)
        assertEquals(originalTracking.workoutId, convertedBack.workoutId)
        assertEquals(originalTracking.isDeloadWorkout, convertedBack.isDeloadWorkout)
        assertEquals(originalTracking.averageRpe, convertedBack.averageRpe)
    }

    // =====================================================
    // GLOBAL EXERCISE PROGRESS CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreGlobalExerciseProgress converts GlobalExerciseProgress correctly`() {
        val lastUpdated = LocalDateTime.of(2024, 1, 28, 20, 0)
        val lastPrDate = LocalDateTime.of(2024, 1, 15, 18, 30)

        val progress =
            com.github.radupana.featherweight.data.GlobalExerciseProgress(
                id = "progress-1",
                userId = "user123",
                exerciseId = "exercise-1",
                currentWorkingWeight = 120f,
                estimatedMax = 140f,
                lastUpdated = lastUpdated,
                recentAvgRpe = 8.2f,
                consecutiveStalls = 2,
                lastPrDate = lastPrDate,
                lastPrWeight = 135f,
                trend = com.github.radupana.featherweight.data.ProgressTrend.IMPROVING,
                volumeTrend = com.github.radupana.featherweight.data.VolumeTrend.INCREASING,
                totalVolumeLast30Days = 15000f,
            )

        val result = SyncConverters.toFirestoreGlobalExerciseProgress(progress)

        assertEquals("progress-1", result.id)
        assertEquals("progress-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("exercise-1", result.exerciseId)
        assertEquals(120f, result.currentWorkingWeight)
        assertEquals(140f, result.estimatedMax)
        assertEquals(8.2f, result.recentAvgRpe)
        assertEquals(2, result.consecutiveStalls)
        assertEquals(135f, result.lastPrWeight)
        assertEquals("IMPROVING", result.trend)
        assertEquals("INCREASING", result.volumeTrend)
        assertEquals(15000f, result.totalVolumeLast30Days)
    }

    @Test
    fun `fromFirestoreGlobalExerciseProgress converts FirestoreGlobalExerciseProgress correctly`() {
        val lastUpdated = Timestamp.now()
        val lastPrDate = Timestamp.now()

        val firestoreProgress =
            com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress(
                id = null,
                localId = "progress-2",
                userId = "user456",
                exerciseId = "exercise-2",
                currentWorkingWeight = 80f,
                estimatedMax = 90f,
                lastUpdated = lastUpdated,
                recentAvgRpe = 9f,
                consecutiveStalls = 3,
                lastPrDate = lastPrDate,
                lastPrWeight = 85f,
                trend = "STALLING",
                volumeTrend = "MAINTAINING",
                totalVolumeLast30Days = 8000f,
            )

        val result = SyncConverters.fromFirestoreGlobalExerciseProgress(firestoreProgress)

        assertEquals("progress-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("exercise-2", result.exerciseId)
        assertEquals(80f, result.currentWorkingWeight)
        assertEquals(90f, result.estimatedMax)
        assertEquals(9f, result.recentAvgRpe)
        assertEquals(3, result.consecutiveStalls)
        assertEquals(85f, result.lastPrWeight)
        assertEquals(com.github.radupana.featherweight.data.ProgressTrend.STALLING, result.trend)
        assertEquals(com.github.radupana.featherweight.data.VolumeTrend.MAINTAINING, result.volumeTrend)
        assertEquals(8000f, result.totalVolumeLast30Days)
    }

    @Test
    fun `toFirestoreGlobalExerciseProgress handles null volumeTrend`() {
        val lastUpdated = LocalDateTime.of(2024, 1, 30, 10, 0)

        val progress =
            com.github.radupana.featherweight.data.GlobalExerciseProgress(
                id = "progress-3",
                userId = "user789",
                exerciseId = "exercise-3",
                currentWorkingWeight = 60f,
                estimatedMax = 70f,
                lastUpdated = lastUpdated,
                recentAvgRpe = null,
                consecutiveStalls = 0,
                lastPrDate = null,
                lastPrWeight = null,
                trend = com.github.radupana.featherweight.data.ProgressTrend.DECLINING,
                volumeTrend = null,
                totalVolumeLast30Days = 5000f,
            )

        val result = SyncConverters.toFirestoreGlobalExerciseProgress(progress)

        assertNull(result.volumeTrend)
        assertNull(result.lastPrDate)
        assertNull(result.lastPrWeight)
        assertNull(result.recentAvgRpe)
    }

    @Test
    fun `fromFirestoreGlobalExerciseProgress handles null volumeTrend`() {
        val lastUpdated = Timestamp.now()

        val firestoreProgress =
            com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress(
                id = null,
                localId = "progress-4",
                userId = "user890",
                exerciseId = "exercise-4",
                currentWorkingWeight = 50f,
                estimatedMax = 55f,
                lastUpdated = lastUpdated,
                recentAvgRpe = null,
                consecutiveStalls = 1,
                lastPrDate = null,
                lastPrWeight = null,
                trend = "IMPROVING",
                volumeTrend = null,
                totalVolumeLast30Days = 3000f,
            )

        val result = SyncConverters.fromFirestoreGlobalExerciseProgress(firestoreProgress)

        assertNull(result.volumeTrend)
        assertNull(result.lastPrDate)
        assertNull(result.lastPrWeight)
        assertNull(result.recentAvgRpe)
    }

    // =====================================================
    // TRAINING ANALYSIS CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreTrainingAnalysis converts TrainingAnalysis correctly`() {
        val analysisDate = LocalDateTime.of(2024, 2, 1, 12, 0)
        val periodStart = java.time.LocalDate.of(2024, 1, 1)
        val periodEnd = java.time.LocalDate.of(2024, 1, 31)

        val analysis =
            com.github.radupana.featherweight.data.TrainingAnalysis(
                id = "analysis-1",
                userId = "user123",
                analysisDate = analysisDate,
                periodStart = periodStart,
                periodEnd = periodEnd,
                overallAssessment = "Good progress",
                keyInsightsJson = """[{"category":"VOLUME","message":"High volume","severity":"INFO"}]""",
                recommendationsJson = """["Increase rest days","Focus on recovery"]""",
                warningsJson = """["Watch for overtraining"]""",
            )

        val result = SyncConverters.toFirestoreTrainingAnalysis(analysis)

        assertEquals("analysis-1", result.id)
        assertEquals("analysis-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("2024-01-01", result.periodStart)
        assertEquals("2024-01-31", result.periodEnd)
        assertEquals("Good progress", result.overallAssessment)
        assertEquals(
            """[{"category":"VOLUME","message":"High volume","severity":"INFO"}]""",
            result.keyInsightsJson,
        )
        assertEquals("""["Increase rest days","Focus on recovery"]""", result.recommendationsJson)
        assertEquals("""["Watch for overtraining"]""", result.warningsJson)
    }

    @Test
    fun `fromFirestoreTrainingAnalysis converts FirestoreTrainingAnalysis correctly`() {
        val analysisDate = Timestamp.now()

        val firestoreAnalysis =
            com.github.radupana.featherweight.sync.models.FirestoreTrainingAnalysis(
                id = null,
                localId = "analysis-2",
                userId = "user456",
                analysisDate = analysisDate,
                periodStart = "2024-02-01",
                periodEnd = "2024-02-28",
                overallAssessment = "Steady improvement",
                keyInsightsJson = """[{"category":"INTENSITY","message":"Good intensity","severity":"SUCCESS"}]""",
                recommendationsJson = """["Keep current plan"]""",
                warningsJson = """[]""",
            )

        val result = SyncConverters.fromFirestoreTrainingAnalysis(firestoreAnalysis)

        assertEquals("analysis-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals(java.time.LocalDate.of(2024, 2, 1), result.periodStart)
        assertEquals(java.time.LocalDate.of(2024, 2, 28), result.periodEnd)
        assertEquals("Steady improvement", result.overallAssessment)
        assertEquals(
            """[{"category":"INTENSITY","message":"Good intensity","severity":"SUCCESS"}]""",
            result.keyInsightsJson,
        )
        assertEquals("""["Keep current plan"]""", result.recommendationsJson)
        assertEquals("""[]""", result.warningsJson)
    }

    @Test
    fun `toFirestoreTrainingAnalysis and fromFirestoreTrainingAnalysis roundtrip maintains data integrity`() {
        val analysisDate = LocalDateTime.of(2024, 3, 1, 9, 0)
        val periodStart = java.time.LocalDate.of(2024, 2, 1)
        val periodEnd = java.time.LocalDate.of(2024, 2, 29)

        val originalAnalysis =
            com.github.radupana.featherweight.data.TrainingAnalysis(
                id = "analysis-roundtrip",
                userId = "user-roundtrip",
                analysisDate = analysisDate,
                periodStart = periodStart,
                periodEnd = periodEnd,
                overallAssessment = "Test assessment",
                keyInsightsJson = """[{"category":"PROGRESSION","message":"Good","severity":"INFO"}]""",
                recommendationsJson = """["Test recommendation"]""",
                warningsJson = """["Test warning"]""",
            )

        val firestoreAnalysis = SyncConverters.toFirestoreTrainingAnalysis(originalAnalysis)
        val convertedBack = SyncConverters.fromFirestoreTrainingAnalysis(firestoreAnalysis)

        assertEquals(originalAnalysis.id, convertedBack.id)
        assertEquals(originalAnalysis.userId, convertedBack.userId)
        assertEquals(originalAnalysis.periodStart, convertedBack.periodStart)
        assertEquals(originalAnalysis.periodEnd, convertedBack.periodEnd)
        assertEquals(originalAnalysis.overallAssessment, convertedBack.overallAssessment)
        assertEquals(originalAnalysis.keyInsightsJson, convertedBack.keyInsightsJson)
        assertEquals(originalAnalysis.recommendationsJson, convertedBack.recommendationsJson)
        assertEquals(originalAnalysis.warningsJson, convertedBack.warningsJson)
    }

    // =====================================================
    // PARSE REQUEST CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreParseRequest converts ParseRequest correctly`() {
        val createdAt = LocalDateTime.of(2024, 1, 25, 14, 30)
        val completedAt = LocalDateTime.of(2024, 1, 25, 14, 35)

        val request =
            com.github.radupana.featherweight.data.ParseRequest(
                id = "request-1",
                userId = "user123",
                rawText = "Week 1: Squat 5x5@100kg",
                createdAt = createdAt,
                status = com.github.radupana.featherweight.data.ParseStatus.COMPLETED,
                error = null,
                resultJson = """{"programme":"parsed"}""",
                completedAt = completedAt,
            )

        val result = SyncConverters.toFirestoreParseRequest(request)

        assertEquals("request-1", result.id)
        assertEquals("request-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("Week 1: Squat 5x5@100kg", result.rawText)
        assertEquals("COMPLETED", result.status)
        assertNull(result.error)
        assertEquals("""{"programme":"parsed"}""", result.resultJson)
    }

    @Test
    fun `fromFirestoreParseRequest converts FirestoreParseRequest correctly`() {
        val createdAt = Timestamp.now()

        val firestoreRequest =
            com.github.radupana.featherweight.sync.models.FirestoreParseRequest(
                id = null,
                localId = "request-2",
                userId = "user456",
                rawText = "Week 2: Bench 4x8@80kg",
                createdAt = createdAt,
                status = "PROCESSING",
                error = null,
                resultJson = null,
                completedAt = null,
            )

        val result = SyncConverters.fromFirestoreParseRequest(firestoreRequest)

        assertEquals("request-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("Week 2: Bench 4x8@80kg", result.rawText)
        assertEquals(com.github.radupana.featherweight.data.ParseStatus.PROCESSING, result.status)
        assertNull(result.error)
        assertNull(result.resultJson)
        assertNull(result.completedAt)
    }

    @Test
    fun `toFirestoreParseRequest handles error status`() {
        val createdAt = LocalDateTime.of(2024, 1, 26, 10, 0)
        val completedAt = LocalDateTime.of(2024, 1, 26, 10, 1)

        val request =
            com.github.radupana.featherweight.data.ParseRequest(
                id = "request-3",
                userId = "user789",
                rawText = "Invalid programme text",
                createdAt = createdAt,
                status = com.github.radupana.featherweight.data.ParseStatus.FAILED,
                error = "Unable to parse programme",
                resultJson = null,
                completedAt = completedAt,
            )

        val result = SyncConverters.toFirestoreParseRequest(request)

        assertEquals("FAILED", result.status)
        assertEquals("Unable to parse programme", result.error)
        assertNull(result.resultJson)
    }

    @Test
    fun `fromFirestoreParseRequest handles imported status`() {
        val createdAt = Timestamp.now()
        val completedAt = Timestamp.now()

        val firestoreRequest =
            com.github.radupana.featherweight.sync.models.FirestoreParseRequest(
                id = null,
                localId = "request-4",
                userId = "user890",
                rawText = "Week 1: Full programme",
                createdAt = createdAt,
                status = "IMPORTED",
                error = null,
                resultJson = """{"programme":"imported"}""",
                completedAt = completedAt,
            )

        val result = SyncConverters.fromFirestoreParseRequest(firestoreRequest)

        assertEquals(com.github.radupana.featherweight.data.ParseStatus.IMPORTED, result.status)
    }

    // =====================================================
    // WORKOUT TEMPLATE CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreWorkoutTemplate converts WorkoutTemplate correctly`() {
        val createdAt = LocalDateTime.of(2024, 1, 10, 8, 0)
        val updatedAt = LocalDateTime.of(2024, 1, 20, 9, 30)

        val template =
            com.github.radupana.featherweight.data.WorkoutTemplate(
                id = "template-1",
                userId = "user123",
                name = "Push Day",
                description = "Chest, shoulders, triceps",
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        val result = SyncConverters.toFirestoreWorkoutTemplate(template)

        assertEquals("template-1", result.id)
        assertEquals("template-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("Push Day", result.name)
        assertEquals("Chest, shoulders, triceps", result.description)
    }

    @Test
    fun `fromFirestoreWorkoutTemplate converts FirestoreWorkoutTemplate correctly`() {
        val createdAt = Timestamp.now()
        val updatedAt = Timestamp.now()

        val firestoreTemplate =
            com.github.radupana.featherweight.sync.models.FirestoreWorkoutTemplate(
                id = null,
                localId = "template-2",
                userId = "user456",
                name = "Pull Day",
                description = "Back, biceps",
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        val result = SyncConverters.fromFirestoreWorkoutTemplate(firestoreTemplate)

        assertEquals("template-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("Pull Day", result.name)
        assertEquals("Back, biceps", result.description)
    }

    @Test
    fun `toFirestoreWorkoutTemplate and fromFirestoreWorkoutTemplate roundtrip maintains data integrity`() {
        val createdAt = LocalDateTime.of(2024, 1, 15, 10, 0)
        val updatedAt = LocalDateTime.of(2024, 1, 25, 11, 0)

        val originalTemplate =
            com.github.radupana.featherweight.data.WorkoutTemplate(
                id = "template-roundtrip",
                userId = "user-roundtrip",
                name = "Leg Day",
                description = "Quads, hamstrings, calves",
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        val firestoreTemplate = SyncConverters.toFirestoreWorkoutTemplate(originalTemplate)
        val convertedBack = SyncConverters.fromFirestoreWorkoutTemplate(firestoreTemplate)

        assertEquals(originalTemplate.id, convertedBack.id)
        assertEquals(originalTemplate.userId, convertedBack.userId)
        assertEquals(originalTemplate.name, convertedBack.name)
        assertEquals(originalTemplate.description, convertedBack.description)
    }

    // =====================================================
    // TEMPLATE EXERCISE CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreTemplateExercise converts TemplateExercise correctly`() {
        val templateExercise =
            com.github.radupana.featherweight.data.TemplateExercise(
                id = "tex-1",
                userId = "user123",
                templateId = "template-1",
                exerciseId = "exercise-1",
                exerciseOrder = 1,
                notes = "Focus on form",
            )

        val result = SyncConverters.toFirestoreTemplateExercise(templateExercise)

        assertEquals("tex-1", result.id)
        assertEquals("tex-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("template-1", result.templateId)
        assertEquals("exercise-1", result.exerciseId)
        assertEquals(1, result.exerciseOrder)
        assertEquals("Focus on form", result.notes)
    }

    @Test
    fun `fromFirestoreTemplateExercise converts FirestoreTemplateExercise correctly`() {
        val firestoreTemplateExercise =
            com.github.radupana.featherweight.sync.models.FirestoreTemplateExercise(
                id = null,
                localId = "tex-2",
                userId = "user456",
                templateId = "template-2",
                exerciseId = "exercise-2",
                exerciseOrder = 2,
                notes = "Slow eccentric",
            )

        val result = SyncConverters.fromFirestoreTemplateExercise(firestoreTemplateExercise)

        assertEquals("tex-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("template-2", result.templateId)
        assertEquals("exercise-2", result.exerciseId)
        assertEquals(2, result.exerciseOrder)
        assertEquals("Slow eccentric", result.notes)
    }

    @Test
    fun `toFirestoreTemplateExercise and fromFirestoreTemplateExercise roundtrip maintains data integrity`() {
        val originalTemplateExercise =
            com.github.radupana.featherweight.data.TemplateExercise(
                id = "tex-roundtrip",
                userId = "user-roundtrip",
                templateId = "template-roundtrip",
                exerciseId = "exercise-roundtrip",
                exerciseOrder = 3,
                notes = "Test notes",
            )

        val firestoreTemplateExercise = SyncConverters.toFirestoreTemplateExercise(originalTemplateExercise)
        val convertedBack = SyncConverters.fromFirestoreTemplateExercise(firestoreTemplateExercise)

        assertEquals(originalTemplateExercise.id, convertedBack.id)
        assertEquals(originalTemplateExercise.userId, convertedBack.userId)
        assertEquals(originalTemplateExercise.templateId, convertedBack.templateId)
        assertEquals(originalTemplateExercise.exerciseId, convertedBack.exerciseId)
        assertEquals(originalTemplateExercise.exerciseOrder, convertedBack.exerciseOrder)
        assertEquals(originalTemplateExercise.notes, convertedBack.notes)
    }

    // =====================================================
    // TEMPLATE SET CONVERTERS
    // =====================================================

    @Test
    fun `toFirestoreTemplateSet converts TemplateSet correctly`() {
        val templateSet =
            com.github.radupana.featherweight.data.TemplateSet(
                id = "tset-1",
                userId = "user123",
                templateExerciseId = "tex-1",
                setOrder = 1,
                targetReps = 10,
                targetWeight = 100f,
                targetRpe = 8f,
                notes = "Warmup set",
            )

        val result = SyncConverters.toFirestoreTemplateSet(templateSet)

        assertEquals("tset-1", result.id)
        assertEquals("tset-1", result.localId)
        assertEquals("user123", result.userId)
        assertEquals("tex-1", result.templateExerciseId)
        assertEquals(1, result.setOrder)
        assertEquals(10, result.targetReps)
        assertEquals(100f, result.targetWeight)
        assertEquals(8f, result.targetRpe)
        assertEquals("Warmup set", result.notes)
    }

    @Test
    fun `fromFirestoreTemplateSet converts FirestoreTemplateSet correctly`() {
        val firestoreTemplateSet =
            com.github.radupana.featherweight.sync.models.FirestoreTemplateSet(
                id = null,
                localId = "tset-2",
                userId = "user456",
                templateExerciseId = "tex-2",
                setOrder = 2,
                targetReps = 5,
                targetWeight = 140f,
                targetRpe = 9f,
                notes = "Working set",
            )

        val result = SyncConverters.fromFirestoreTemplateSet(firestoreTemplateSet)

        assertEquals("tset-2", result.id)
        assertEquals("user456", result.userId)
        assertEquals("tex-2", result.templateExerciseId)
        assertEquals(2, result.setOrder)
        assertEquals(5, result.targetReps)
        assertEquals(140f, result.targetWeight)
        assertEquals(9f, result.targetRpe)
        assertEquals("Working set", result.notes)
    }

    @Test
    fun `toFirestoreTemplateSet handles null weight and rpe`() {
        val templateSet =
            com.github.radupana.featherweight.data.TemplateSet(
                id = "tset-3",
                userId = "user789",
                templateExerciseId = "tex-3",
                setOrder = 1,
                targetReps = 12,
                targetWeight = null,
                targetRpe = null,
                notes = null,
            )

        val result = SyncConverters.toFirestoreTemplateSet(templateSet)

        assertNull(result.targetWeight)
        assertNull(result.targetRpe)
        assertNull(result.notes)
    }

    @Test
    fun `fromFirestoreTemplateSet handles null weight and rpe`() {
        val firestoreTemplateSet =
            com.github.radupana.featherweight.sync.models.FirestoreTemplateSet(
                id = null,
                localId = "tset-4",
                userId = "user890",
                templateExerciseId = "tex-4",
                setOrder = 3,
                targetReps = 15,
                targetWeight = null,
                targetRpe = null,
                notes = null,
            )

        val result = SyncConverters.fromFirestoreTemplateSet(firestoreTemplateSet)

        assertNull(result.targetWeight)
        assertNull(result.targetRpe)
        assertNull(result.notes)
    }
}
