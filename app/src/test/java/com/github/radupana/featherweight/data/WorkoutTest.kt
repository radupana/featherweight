package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WorkoutTest {
    
    @Test
    fun `workout defaults to NOT_STARTED status`() {
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now()
        )
        
        assertThat(workout.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        assertThat(workout.isProgrammeWorkout).isFalse()
        assertThat(workout.timerElapsedSeconds).isEqualTo(0)
    }
    
    @Test
    fun `workout stores programme information`() {
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            programmeId = 10L,
            weekNumber = 2,
            dayNumber = 3,
            programmeWorkoutName = "Upper Power",
            isProgrammeWorkout = true
        )
        
        assertThat(workout.programmeId).isEqualTo(10L)
        assertThat(workout.weekNumber).isEqualTo(2)
        assertThat(workout.dayNumber).isEqualTo(3)
        assertThat(workout.programmeWorkoutName).isEqualTo("Upper Power")
        assertThat(workout.isProgrammeWorkout).isTrue()
    }
    
    @Test
    fun `workout stores timer information`() {
        val startTime = LocalDateTime.now().minusMinutes(30)
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.IN_PROGRESS,
            timerStartTime = startTime,
            timerElapsedSeconds = 1800
        )
        
        assertThat(workout.timerStartTime).isEqualTo(startTime)
        assertThat(workout.timerElapsedSeconds).isEqualTo(1800)
    }
    
    @Test
    fun `workout stores duration for completed workouts`() {
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED,
            durationSeconds = 3600
        )
        
        assertThat(workout.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(workout.durationSeconds).isEqualTo(3600)
    }
    
    @Test
    fun `workout stores notes with timestamp`() {
        val now = LocalDateTime.now()
        val workout = Workout(
            id = 1L,
            date = now,
            name = "Morning Workout",
            notes = "Felt strong today",
            notesUpdatedAt = now
        )
        
        assertThat(workout.name).isEqualTo("Morning Workout")
        assertThat(workout.notes).isEqualTo("Felt strong today")
        assertThat(workout.notesUpdatedAt).isEqualTo(now)
    }
}