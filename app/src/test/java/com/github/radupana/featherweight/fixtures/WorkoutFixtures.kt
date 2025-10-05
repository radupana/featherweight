package com.github.radupana.featherweight.fixtures

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import java.time.LocalDateTime

/**
 * Test fixtures for workout-related data
 */
object WorkoutFixtures {
    fun createWorkout(
        id: String = "1",
        date: LocalDateTime = LocalDateTime.now(),
        name: String? = "Test Workout",
        notes: String? = null,
        status: WorkoutStatus = WorkoutStatus.IN_PROGRESS,
        programmeId: String? = null,
        weekNumber: Int? = null,
        dayNumber: Int? = null,
        programmeWorkoutName: String? = null,
        isProgrammeWorkout: Boolean = false,
        durationSeconds: String? = null,
        timerStartTime: LocalDateTime? = null,
        timerElapsedSeconds: Int = 0,
    ) = Workout(
        id = id,
        date = date,
        name = name,
        notes = notes,
        notesUpdatedAt = null,
        programmeId = programmeId,
        weekNumber = weekNumber,
        dayNumber = dayNumber,
        programmeWorkoutName = programmeWorkoutName,
        isProgrammeWorkout = isProgrammeWorkout,
        status = status,
        durationSeconds = durationSeconds,
        timerStartTime = timerStartTime,
        timerElapsedSeconds = timerElapsedSeconds,
    )

    fun createExerciseLog(
        id: String = "1",
        workoutId: String = "1",
        exerciseId: String = "1",
        exerciseOrder: Int = 0,
        notes: String? = null,
        originalExerciseId: String? = null,
        isSwapped: Boolean = false,
    ) = ExerciseLog(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseOrder = exerciseOrder,
        notes = notes,
        originalExerciseId = originalExerciseId,
        isSwapped = isSwapped,
    )

    fun createSetLog(
        id: String = "1",
        userId: String? = "test-user-id",
        exerciseLogId: String = "1",
        setOrder: Int = 1,
        targetReps: Int? = 10,
        targetWeight: Float? = 60f,
        actualReps: Int = 10,
        actualWeight: Float = 60f,
        actualRpe: Float? = 8f,
        isCompleted: Boolean = true,
        completedAt: String? = null,
        tag: String? = null,
        notes: String? = null,
    ) = SetLog(
        id = id,
        userId = userId,
        exerciseLogId = exerciseLogId,
        setOrder = setOrder,
        targetReps = targetReps,
        targetWeight = targetWeight,
        actualReps = actualReps,
        actualWeight = actualWeight,
        actualRpe = actualRpe,
        tag = tag,
        notes = notes,
        isCompleted = isCompleted,
        completedAt = completedAt,
    )

    fun createCompletedSets(
        count: Int = 3,
        weight: Float = 100f,
        reps: Int = 5,
        userId: String? = "test-user-id",
    ): List<SetLog> =
        (1..count).map { setNumber ->
            createSetLog(
                id = setNumber.toString(),
                userId = userId,
                setOrder = setNumber,
                targetReps = reps,
                targetWeight = weight,
                actualReps = reps,
                actualWeight = weight,
                isCompleted = true,
            )
        }
}
