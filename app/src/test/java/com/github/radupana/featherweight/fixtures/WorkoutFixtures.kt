package com.github.radupana.featherweight.fixtures

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import java.time.LocalDateTime

/**
 * Configuration for programme-related workout properties.
 */
data class ProgrammeConfig(
    val programmeId: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    val isProgrammeWorkout: Boolean = false,
)

/**
 * Configuration for workout timer properties.
 */
data class TimerConfig(
    val durationSeconds: String? = null,
    val timerStartTime: LocalDateTime? = null,
    val timerElapsedSeconds: Int = 0,
)

/**
 * Test fixtures for workout-related data.
 *
 * Note: These fixture functions have many parameters by design to enable
 * flexible test data creation. For production code, prefer data classes
 * with fewer parameters or builder patterns.
 */
@Suppress("LongParameterList")
object WorkoutFixtures {
    /**
     * Creates a workout with grouped configuration objects.
     * Use this for complex workout configurations.
     */
    fun createWorkout(
        id: String = "1",
        date: LocalDateTime = LocalDateTime.now(),
        name: String? = "Test Workout",
        notes: String? = null,
        status: WorkoutStatus = WorkoutStatus.IN_PROGRESS,
        programme: ProgrammeConfig = ProgrammeConfig(),
        timer: TimerConfig = TimerConfig(),
    ) = Workout(
        id = id,
        date = date,
        name = name,
        notes = notes,
        notesUpdatedAt = null,
        programmeId = programme.programmeId,
        weekNumber = programme.weekNumber,
        dayNumber = programme.dayNumber,
        programmeWorkoutName = programme.programmeWorkoutName,
        isProgrammeWorkout = programme.isProgrammeWorkout,
        status = status,
        durationSeconds = timer.durationSeconds,
        timerStartTime = timer.timerStartTime,
        timerElapsedSeconds = timer.timerElapsedSeconds,
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

    /**
     * Creates a SetLog for testing with direct performance parameters.
     * Parameters are ordered by frequency of use in tests.
     */
    @Suppress("LongParameterList")
    fun createSetLog(
        id: String = "1",
        userId: String? = "test-user-id",
        exerciseLogId: String = "1",
        setOrder: Int = 1,
        actualWeight: Float = 60f,
        actualReps: Int = 10,
        actualRpe: Float? = 8f,
        isCompleted: Boolean = true,
    ) = SetLog(
        id = id,
        userId = userId,
        exerciseLogId = exerciseLogId,
        setOrder = setOrder,
        targetReps = actualReps,
        targetWeight = actualWeight,
        actualReps = actualReps,
        actualWeight = actualWeight,
        actualRpe = actualRpe,
        tag = null,
        notes = null,
        isCompleted = isCompleted,
        completedAt = null,
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
                actualWeight = weight,
                actualReps = reps,
                actualRpe = null,
                isCompleted = true,
            )
        }
}
