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
        id: Long = 1L,
        date: LocalDateTime = LocalDateTime.now(),
        name: String? = "Test Workout",
        notes: String? = null,
        status: WorkoutStatus = WorkoutStatus.IN_PROGRESS,
        programmeId: Long? = null,
        weekNumber: Int? = null,
        dayNumber: Int? = null,
        programmeWorkoutName: String? = null,
        isProgrammeWorkout: Boolean = false,
        durationSeconds: Long? = null,
        timerStartTime: LocalDateTime? = null,
        timerElapsedSeconds: Int = 0
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
        timerElapsedSeconds = timerElapsedSeconds
    )
    
    fun createExerciseLog(
        id: Long = 1L,
        workoutId: Long = 1L,
        exerciseVariationId: Long = 1L,
        exerciseOrder: Int = 0,
        supersetGroup: Int? = null,
        notes: String? = null,
        originalVariationId: Long? = null,
        isSwapped: Boolean = false
    ) = ExerciseLog(
        id = id,
        workoutId = workoutId,
        exerciseVariationId = exerciseVariationId,
        exerciseOrder = exerciseOrder,
        supersetGroup = supersetGroup,
        notes = notes,
        originalVariationId = originalVariationId,
        isSwapped = isSwapped
    )
    
    fun createSetLog(
        id: Long = 1L,
        exerciseLogId: Long = 1L,
        setOrder: Int = 1,
        targetReps: Int? = 10,
        targetWeight: Float? = 60f,
        actualReps: Int = 10,
        actualWeight: Float = 60f,
        actualRpe: Float? = 8f,
        isCompleted: Boolean = true,
        completedAt: String? = null,
        suggestedWeight: Float? = null,
        suggestedReps: Int? = null,
        suggestionSource: String? = null,
        suggestionConfidence: Float? = null,
        calculationDetails: String? = null,
        tag: String? = null,
        notes: String? = null
    ) = SetLog(
        id = id,
        exerciseLogId = exerciseLogId,
        setOrder = setOrder,
        targetReps = targetReps,
        targetWeight = targetWeight,
        actualReps = actualReps,
        actualWeight = actualWeight,
        actualRpe = actualRpe,
        suggestedWeight = suggestedWeight,
        suggestedReps = suggestedReps,
        suggestionSource = suggestionSource,
        suggestionConfidence = suggestionConfidence,
        calculationDetails = calculationDetails,
        tag = tag,
        notes = notes,
        isCompleted = isCompleted,
        completedAt = completedAt
    )
    
    fun createCompletedSets(count: Int = 3, weight: Float = 100f, reps: Int = 5): List<SetLog> {
        return (1..count).map { setNumber ->
            createSetLog(
                id = setNumber.toLong(),
                setOrder = setNumber,
                targetReps = reps,
                targetWeight = weight,
                actualReps = reps,
                actualWeight = weight,
                isCompleted = true
            )
        }
    }
}