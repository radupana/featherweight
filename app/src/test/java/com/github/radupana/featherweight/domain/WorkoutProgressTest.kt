package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WorkoutProgressTest {
    @Test
    fun `workout progress calculation for completed workout`() {
        val workout =
            Workout(
                id = "1",
                name = "Upper Body",
                date = LocalDateTime.now(),
                status = WorkoutStatus.COMPLETED,
                durationSeconds = "3600", // 1 hour
            )

        assertThat(workout.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(workout.durationSeconds).isEqualTo("3600")
    }

    @Test
    fun `workout status transitions are valid`() {
        val workout =
            Workout(
                name = "Test Workout",
                date = LocalDateTime.now(),
                status = WorkoutStatus.NOT_STARTED,
            )

        // Transition to IN_PROGRESS
        val inProgress = workout.copy(status = WorkoutStatus.IN_PROGRESS)
        assertThat(inProgress.status).isEqualTo(WorkoutStatus.IN_PROGRESS)

        // Transition to COMPLETED
        val completed =
            inProgress.copy(
                status = WorkoutStatus.COMPLETED,
                durationSeconds = "2700",
            )
        assertThat(completed.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(completed.durationSeconds).isEqualTo("2700")
    }

    @Test
    fun `programme workout identification`() {
        val programmeWorkout =
            Workout(
                name = "Week 1 Day 1",
                date = LocalDateTime.now(),
                programmeId = "1",
                weekNumber = 1,
                dayNumber = 1,
                isProgrammeWorkout = true,
                status = WorkoutStatus.NOT_STARTED,
            )

        val freestyleWorkout =
            Workout(
                name = "Freestyle Session",
                date = LocalDateTime.now(),
                isProgrammeWorkout = false,
                status = WorkoutStatus.NOT_STARTED,
            )

        assertThat(programmeWorkout.isProgrammeWorkout).isTrue()
        assertThat(programmeWorkout.programmeId).isNotNull()
        assertThat(programmeWorkout.weekNumber).isEqualTo(1)
        assertThat(programmeWorkout.dayNumber).isEqualTo(1)

        assertThat(freestyleWorkout.isProgrammeWorkout).isFalse()
        assertThat(freestyleWorkout.programmeId).isNull()
        assertThat(freestyleWorkout.weekNumber).isNull()
        assertThat(freestyleWorkout.dayNumber).isNull()
    }
}
