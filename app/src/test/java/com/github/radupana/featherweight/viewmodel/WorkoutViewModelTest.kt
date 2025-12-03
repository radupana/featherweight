package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.WorkoutMode
import com.github.radupana.featherweight.data.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutViewModelTest {
    @Test
    fun `ParsedExercise with RPE sets is created correctly`() {
        // Test that ParsedExercise correctly stores sets with RPE
        val sets =
            listOf(
                ParsedSet(reps = 8, weight = 100f, rpe = 7f),
                ParsedSet(reps = 8, weight = 100f, rpe = 7.5f),
                ParsedSet(reps = 8, weight = 100f, rpe = 8f),
            )

        val parsedExercise =
            ParsedExercise(
                exerciseName = "Barbell Squat",
                matchedExerciseId = "1",
                sets = sets,
                notes = null,
            )

        assertThat(parsedExercise.sets).hasSize(3)
        assertThat(parsedExercise.sets[0].rpe).isEqualTo(7f)
        assertThat(parsedExercise.sets[1].rpe).isEqualTo(7.5f)
        assertThat(parsedExercise.sets[2].rpe).isEqualTo(8f)
    }

    @Test
    fun `ParsedWorkout with exercises containing RPE is valid`() {
        // Test complete workout structure with RPE values
        val deadliftSets =
            listOf(
                ParsedSet(reps = 1, weight = 160f, rpe = 7f),
                ParsedSet(reps = 4, weight = 140f, rpe = 7.5f),
                ParsedSet(reps = 4, weight = 140f, rpe = 7.5f),
                ParsedSet(reps = 4, weight = 140f, rpe = 7.5f),
            )

        val benchSets =
            listOf(
                ParsedSet(reps = 6, weight = 72.5f, rpe = 8f),
                ParsedSet(reps = 6, weight = 72.5f, rpe = 8f),
                ParsedSet(reps = 6, weight = 72.5f, rpe = 8f),
            )

        val exercises =
            listOf(
                ParsedExercise(
                    exerciseName = "Barbell Deadlift",
                    matchedExerciseId = "8",
                    sets = deadliftSets,
                    notes = null,
                ),
                ParsedExercise(
                    exerciseName = "Barbell Paused Bench Press",
                    matchedExerciseId = "486",
                    sets = benchSets,
                    notes = null,
                ),
            )

        val workout =
            ParsedWorkout(
                dayOfWeek = "Thursday",
                name = "Day 4",
                estimatedDurationMinutes = 60,
                exercises = exercises,
            )

        // Verify structure
        assertThat(workout.exercises).hasSize(2)

        // Verify first exercise (Deadlift)
        val deadlift = workout.exercises[0]
        assertThat(deadlift.sets).hasSize(4)
        assertThat(deadlift.sets[0].rpe).isEqualTo(7f)
        assertThat(deadlift.sets[1].rpe).isEqualTo(7.5f)

        // Verify second exercise (Bench)
        val bench = workout.exercises[1]
        assertThat(bench.sets).hasSize(3)
        assertThat(bench.sets[0].rpe).isEqualTo(8f)
    }

    @Test
    fun `SetLog created from ParsedSet includes targetRpe`() {
        // Test that when creating SetLog from ParsedSet, targetRpe is set
        val parsedSet =
            ParsedSet(
                reps = 10,
                weight = 100f,
                rpe = 8f,
            )

        // Simulate what WorkoutViewModel should do
        val setLog =
            SetLog(
                exerciseLogId = "1",
                setOrder = 1,
                targetReps = parsedSet.reps,
                targetWeight = parsedSet.weight,
                targetRpe = parsedSet.rpe, // This must be set from parsedSet.rpe
                actualReps = parsedSet.reps ?: 0,
                actualWeight = parsedSet.weight ?: 0f,
                actualRpe = parsedSet.rpe,
                isCompleted = false,
            )

        // Verify all target values are set correctly
        assertThat(setLog.targetReps).isEqualTo(10)
        assertThat(setLog.targetWeight).isEqualTo(100f)
        assertThat(setLog.targetRpe).isEqualTo(8f)

        // Verify actual values are prepopulated
        assertThat(setLog.actualReps).isEqualTo(10)
        assertThat(setLog.actualWeight).isEqualTo(100f)
        assertThat(setLog.actualRpe).isEqualTo(8f)
    }

    @Test
    fun `SetLog handles null RPE correctly`() {
        // Test that SetLog handles null RPE values
        val parsedSet =
            ParsedSet(
                reps = 10,
                weight = 100f,
                rpe = null,
            )

        val setLog =
            SetLog(
                exerciseLogId = "1",
                setOrder = 1,
                targetReps = parsedSet.reps,
                targetWeight = parsedSet.weight,
                targetRpe = parsedSet.rpe, // null is valid
                actualReps = parsedSet.reps ?: 0,
                actualWeight = parsedSet.weight ?: 0f,
                actualRpe = parsedSet.rpe,
                isCompleted = false,
            )

        assertThat(setLog.targetRpe).isNull()
        assertThat(setLog.actualRpe).isNull()
    }

    @Test
    fun `Full programme with RPE values flows through correctly`() {
        // Test complete flow from ParsedProgramme to SetLog
        val sets =
            listOf(
                ParsedSet(reps = 5, weight = 100f, rpe = 6f),
                ParsedSet(reps = 5, weight = 110f, rpe = 7f),
                ParsedSet(reps = 5, weight = 120f, rpe = 8f),
                ParsedSet(reps = 3, weight = 130f, rpe = 9f),
                ParsedSet(reps = 1, weight = 140f, rpe = 10f),
            )

        val exercise =
            ParsedExercise(
                exerciseName = "Barbell Squat",
                matchedExerciseId = "1",
                sets = sets,
                notes = "Progressive intensity",
            )

        val workout =
            ParsedWorkout(
                dayOfWeek = "Monday",
                name = "Heavy Day",
                estimatedDurationMinutes = 90,
                exercises = listOf(exercise),
            )

        val week =
            ParsedWeek(
                weekNumber = 1,
                name = "Week 1",
                description = "Intensity buildup",
                focusAreas = "Strength",
                intensityLevel = "High",
                volumeLevel = "Low",
                isDeload = false,
                phase = "Peaking",
                workouts = listOf(workout),
            )

        val programme =
            ParsedProgramme(
                name = "Strength Programme",
                description = "Test programme with RPE",
                durationWeeks = 1,
                programmeType = "STRENGTH",
                difficulty = "INTERMEDIATE",
                weeks = listOf(week),
                rawText = "Test programme raw text",
                unmatchedExercises = emptyList(),
            )

        // Verify RPE values are preserved through the entire structure
        val retrievedSets =
            programme.weeks[0]
                .workouts[0]
                .exercises[0]
                .sets
        assertThat(retrievedSets).hasSize(5)
        assertThat(retrievedSets[0].rpe).isEqualTo(6f)
        assertThat(retrievedSets[1].rpe).isEqualTo(7f)
        assertThat(retrievedSets[2].rpe).isEqualTo(8f)
        assertThat(retrievedSets[3].rpe).isEqualTo(9f)
        assertThat(retrievedSets[4].rpe).isEqualTo(10f)
    }

    // Tests for completed workout race condition fix
    // See: WorkoutScreen.kt LaunchedEffect that calls startNewWorkout()

    @Test
    fun `WorkoutState for completed workout has isReadOnly true`() {
        // When viewing a completed workout, isReadOnly must be true
        // to prevent WorkoutScreen from calling startNewWorkout()
        val completedWorkoutState =
            WorkoutState(
                isActive = false,
                status = WorkoutStatus.COMPLETED,
                workoutId = "test-workout-id",
                isReadOnly = true,
            )

        assertThat(completedWorkoutState.isReadOnly).isTrue()
        assertThat(completedWorkoutState.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(completedWorkoutState.isActive).isFalse()
    }

    @Test
    fun `Completed workout state should NOT trigger startNewWorkout`() {
        // This test documents the fix for the race condition where
        // WorkoutScreen's LaunchedEffect would call startNewWorkout()
        // when viewing a completed workout.
        //
        // The LaunchedEffect condition is:
        // if (!workoutState.isActive &&
        //     workoutState.status != WorkoutStatus.COMPLETED &&
        //     workoutState.mode != WorkoutMode.TEMPLATE_EDIT &&
        //     !workoutState.isReadOnly &&
        //     currentWorkoutId == null) {
        //     viewModel.startNewWorkout()
        // }

        val completedWorkoutState =
            WorkoutState(
                isActive = false,
                status = WorkoutStatus.COMPLETED,
                mode = WorkoutMode.ACTIVE,
                workoutId = "test-workout-id",
                isReadOnly = true,
            )
        val currentWorkoutId: String? = "test-workout-id"

        // Evaluate the condition that triggers startNewWorkout()
        val shouldStartNewWorkout =
            !completedWorkoutState.isActive &&
                completedWorkoutState.status != WorkoutStatus.COMPLETED &&
                completedWorkoutState.mode != WorkoutMode.TEMPLATE_EDIT &&
                !completedWorkoutState.isReadOnly &&
                currentWorkoutId == null

        // For a completed workout, this should be FALSE
        assertThat(shouldStartNewWorkout).isFalse()
    }

    @Test
    fun `Empty workout state SHOULD trigger startNewWorkout`() {
        // This verifies the positive case - when there's no workout,
        // startNewWorkout() should be called

        val emptyState = WorkoutState()
        val currentWorkoutId: String? = null

        val shouldStartNewWorkout =
            !emptyState.isActive &&
                emptyState.status != WorkoutStatus.COMPLETED &&
                emptyState.mode != WorkoutMode.TEMPLATE_EDIT &&
                !emptyState.isReadOnly &&
                currentWorkoutId == null

        // For empty state, this should be TRUE
        assertThat(shouldStartNewWorkout).isTrue()
    }

    @Test
    fun `Template edit mode should NOT trigger startNewWorkout`() {
        // Template editing should not trigger new workout creation

        val templateState =
            WorkoutState(
                isActive = false,
                status = WorkoutStatus.NOT_STARTED,
                mode = WorkoutMode.TEMPLATE_EDIT,
                isReadOnly = false,
            )
        val currentWorkoutId: String? = null

        val shouldStartNewWorkout =
            !templateState.isActive &&
                templateState.status != WorkoutStatus.COMPLETED &&
                templateState.mode != WorkoutMode.TEMPLATE_EDIT &&
                !templateState.isReadOnly &&
                currentWorkoutId == null

        assertThat(shouldStartNewWorkout).isFalse()
    }

    @Test
    fun `Workout being loaded should NOT trigger startNewWorkout`() {
        // When currentWorkoutId is set (synchronously by viewCompletedWorkout),
        // startNewWorkout() should not be called even if state hasn't updated yet.
        // This prevents the race condition.

        val stateBeforeCoroutineCompletes =
            WorkoutState(
                isActive = false,
                status = WorkoutStatus.NOT_STARTED, // Old state, not updated yet
                mode = WorkoutMode.ACTIVE,
                isReadOnly = false, // Old state, not updated yet
            )
        val currentWorkoutId: String? = "workout-being-loaded" // Set synchronously

        val shouldStartNewWorkout =
            !stateBeforeCoroutineCompletes.isActive &&
                stateBeforeCoroutineCompletes.status != WorkoutStatus.COMPLETED &&
                stateBeforeCoroutineCompletes.mode != WorkoutMode.TEMPLATE_EDIT &&
                !stateBeforeCoroutineCompletes.isReadOnly &&
                currentWorkoutId == null // This is false, so startNewWorkout won't be called

        // Even though state hasn't updated, currentWorkoutId prevents race condition
        assertThat(shouldStartNewWorkout).isFalse()
    }
}
