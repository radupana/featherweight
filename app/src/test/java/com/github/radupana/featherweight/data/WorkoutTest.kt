package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WorkoutTest {
    @Test
    fun workout_creation_withAllFields_createsCorrectly() {
        val date = LocalDateTime.of(2024, 1, 15, 9, 0)
        val notesUpdatedAt = LocalDateTime.of(2024, 1, 15, 10, 30)
        val timerStartTime = LocalDateTime.of(2024, 1, 15, 9, 5)

        val workout =
            Workout(
                id = 1L,
                date = date,
                name = "Push Day A",
                notes = "Feeling strong today",
                notesUpdatedAt = notesUpdatedAt,
                programmeId = 100L,
                weekNumber = 2,
                dayNumber = 1,
                programmeWorkoutName = "Week 2 Day 1 - Push",
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
                durationSeconds = 3600L,
                timerStartTime = timerStartTime,
                timerElapsedSeconds = 3600,
            )

        assertThat(workout.id).isEqualTo(1L)
        assertThat(workout.date).isEqualTo(date)
        assertThat(workout.name).isEqualTo("Push Day A")
        assertThat(workout.notes).isEqualTo("Feeling strong today")
        assertThat(workout.notesUpdatedAt).isEqualTo(notesUpdatedAt)
        assertThat(workout.programmeId).isEqualTo(100L)
        assertThat(workout.weekNumber).isEqualTo(2)
        assertThat(workout.dayNumber).isEqualTo(1)
        assertThat(workout.programmeWorkoutName).isEqualTo("Week 2 Day 1 - Push")
        assertThat(workout.isProgrammeWorkout).isTrue()
        assertThat(workout.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(workout.durationSeconds).isEqualTo(3600L)
        assertThat(workout.timerStartTime).isEqualTo(timerStartTime)
        assertThat(workout.timerElapsedSeconds).isEqualTo(3600)
    }

    @Test
    fun workout_creation_withMinimalFields_usesDefaults() {
        val date = LocalDateTime.now()

        val workout = Workout(date = date)

        assertThat(workout.id).isEqualTo(0L)
        assertThat(workout.date).isEqualTo(date)
        assertThat(workout.name).isNull()
        assertThat(workout.notes).isNull()
        assertThat(workout.notesUpdatedAt).isNull()
        assertThat(workout.programmeId).isNull()
        assertThat(workout.weekNumber).isNull()
        assertThat(workout.dayNumber).isNull()
        assertThat(workout.programmeWorkoutName).isNull()
        assertThat(workout.isProgrammeWorkout).isFalse()
        assertThat(workout.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        assertThat(workout.durationSeconds).isNull()
        assertThat(workout.timerStartTime).isNull()
        assertThat(workout.timerElapsedSeconds).isEqualTo(0)
    }

    @Test
    fun workout_freestyleWorkout_hasNoProgrammeFields() {
        val workout =
            Workout(
                date = LocalDateTime.now(),
                name = "Custom Upper Body",
                notes = "Quick gym session",
                isProgrammeWorkout = false,
                status = WorkoutStatus.IN_PROGRESS,
            )

        assertThat(workout.isProgrammeWorkout).isFalse()
        assertThat(workout.programmeId).isNull()
        assertThat(workout.weekNumber).isNull()
        assertThat(workout.dayNumber).isNull()
        assertThat(workout.programmeWorkoutName).isNull()
        assertThat(workout.name).isEqualTo("Custom Upper Body")
    }

    @Test
    fun workout_programmeWorkout_hasAllProgrammeFields() {
        val workout =
            Workout(
                date = LocalDateTime.now(),
                programmeId = 42L,
                weekNumber = 4,
                dayNumber = 3,
                programmeWorkoutName = "Week 4 Day 3 - Legs",
                isProgrammeWorkout = true,
                status = WorkoutStatus.NOT_STARTED,
            )

        assertThat(workout.isProgrammeWorkout).isTrue()
        assertThat(workout.programmeId).isEqualTo(42L)
        assertThat(workout.weekNumber).isEqualTo(4)
        assertThat(workout.dayNumber).isEqualTo(3)
        assertThat(workout.programmeWorkoutName).isEqualTo("Week 4 Day 3 - Legs")
    }

    @Test
    fun workout_allStatusValues_areValid() {
        val baseDate = LocalDateTime.now()

        val notStarted =
            Workout(
                date = baseDate,
                status = WorkoutStatus.NOT_STARTED,
            )
        assertThat(notStarted.status).isEqualTo(WorkoutStatus.NOT_STARTED)

        val inProgress =
            Workout(
                date = baseDate,
                status = WorkoutStatus.IN_PROGRESS,
            )
        assertThat(inProgress.status).isEqualTo(WorkoutStatus.IN_PROGRESS)

        val completed =
            Workout(
                date = baseDate,
                status = WorkoutStatus.COMPLETED,
            )
        assertThat(completed.status).isEqualTo(WorkoutStatus.COMPLETED)
    }

    @Test
    fun workout_withTimer_tracksTimeCorrectly() {
        val startTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0)

        val workout =
            Workout(
                date = LocalDateTime.of(2024, 1, 15, 9, 30),
                timerStartTime = startTime,
                timerElapsedSeconds = 1800, // 30 minutes
                status = WorkoutStatus.IN_PROGRESS,
            )

        assertThat(workout.timerStartTime).isEqualTo(startTime)
        assertThat(workout.timerElapsedSeconds).isEqualTo(1800)
    }

    @Test
    fun workout_withLongDuration_handlesCorrectly() {
        val workout =
            Workout(
                date = LocalDateTime.now(),
                durationSeconds = 10800L, // 3 hours
                status = WorkoutStatus.COMPLETED,
            )

        assertThat(workout.durationSeconds).isEqualTo(10800L)
    }

    @Test
    fun workout_withVeryShortDuration_handlesCorrectly() {
        val workout =
            Workout(
                date = LocalDateTime.now(),
                durationSeconds = 60L, // 1 minute
                status = WorkoutStatus.COMPLETED,
            )

        assertThat(workout.durationSeconds).isEqualTo(60L)
    }

    @Test
    fun workout_withLongNotes_storesCorrectly() {
        val longNotes =
            "Today's workout was exceptional. Started with proper warm-up including " +
                "dynamic stretching and mobility work. Main sets felt strong, maintained good form " +
                "throughout. Progressive overload is working well - increased weight on all major lifts. " +
                "Need to focus more on mind-muscle connection for isolation exercises. Recovery between " +
                "sets was adequate. Finished with 10 minutes of cardio cooldown. Overall energy levels " +
                "were high, likely due to good nutrition and sleep the night before."

        val workout =
            Workout(
                date = LocalDateTime.now(),
                notes = longNotes,
                notesUpdatedAt = LocalDateTime.now(),
            )

        assertThat(workout.notes).isEqualTo(longNotes)
        assertThat(workout.notes).hasLength(longNotes.length)
    }

    @Test
    fun workout_weekNumber_validRange() {
        val workout1 =
            Workout(
                date = LocalDateTime.now(),
                weekNumber = 1, // First week
                isProgrammeWorkout = true,
            )
        assertThat(workout1.weekNumber).isEqualTo(1)

        val workout2 =
            Workout(
                date = LocalDateTime.now(),
                weekNumber = 52, // Full year programme
                isProgrammeWorkout = true,
            )
        assertThat(workout2.weekNumber).isEqualTo(52)
    }

    @Test
    fun workout_dayNumber_validRange() {
        val workout1 =
            Workout(
                date = LocalDateTime.now(),
                dayNumber = 1, // Monday
                isProgrammeWorkout = true,
            )
        assertThat(workout1.dayNumber).isEqualTo(1)

        val workout7 =
            Workout(
                date = LocalDateTime.now(),
                dayNumber = 7, // Sunday
                isProgrammeWorkout = true,
            )
        assertThat(workout7.dayNumber).isEqualTo(7)
    }

    @Test
    fun workout_dateTime_maintainsPrecision() {
        val date = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456789)
        val notesUpdated = LocalDateTime.of(2024, 3, 15, 15, 45, 30, 987654321)
        val timerStart = LocalDateTime.of(2024, 3, 15, 14, 35, 0, 0)

        val workout =
            Workout(
                date = date,
                notesUpdatedAt = notesUpdated,
                timerStartTime = timerStart,
            )

        assertThat(workout.date).isEqualTo(date)
        assertThat(workout.notesUpdatedAt).isEqualTo(notesUpdated)
        assertThat(workout.timerStartTime).isEqualTo(timerStart)
        assertThat(workout.date.year).isEqualTo(2024)
        assertThat(workout.date.monthValue).isEqualTo(3)
        assertThat(workout.date.dayOfMonth).isEqualTo(15)
        assertThat(workout.date.hour).isEqualTo(14)
        assertThat(workout.date.minute).isEqualTo(30)
        assertThat(workout.date.second).isEqualTo(45)
    }

    @Test
    fun workout_timerElapsedSeconds_edgeCases() {
        val workout1 =
            Workout(
                date = LocalDateTime.now(),
                timerElapsedSeconds = 0, // Just started
            )
        assertThat(workout1.timerElapsedSeconds).isEqualTo(0)

        val workout2 =
            Workout(
                date = LocalDateTime.now(),
                timerElapsedSeconds = 86400, // 24 hours - extreme case
            )
        assertThat(workout2.timerElapsedSeconds).isEqualTo(86400)

        val workout3 =
            Workout(
                date = LocalDateTime.now(),
                timerElapsedSeconds = -1, // Edge case - should not happen in practice
            )
        assertThat(workout3.timerElapsedSeconds).isEqualTo(-1)
    }

    @Test
    fun workout_equality_basedOnAllFields() {
        val date = LocalDateTime.now()
        val notesUpdated = date.plusMinutes(30)
        val timerStart = date.plusMinutes(5)

        val workout1 =
            Workout(
                id = 1L,
                date = date,
                name = "Push Day",
                notes = "Good workout",
                notesUpdatedAt = notesUpdated,
                programmeId = 10L,
                weekNumber = 1,
                dayNumber = 1,
                programmeWorkoutName = "Week 1 Day 1",
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
                durationSeconds = 3600L,
                timerStartTime = timerStart,
                timerElapsedSeconds = 3600,
            )

        val workout2 =
            Workout(
                id = 1L,
                date = date,
                name = "Push Day",
                notes = "Good workout",
                notesUpdatedAt = notesUpdated,
                programmeId = 10L,
                weekNumber = 1,
                dayNumber = 1,
                programmeWorkoutName = "Week 1 Day 1",
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
                durationSeconds = 3600L,
                timerStartTime = timerStart,
                timerElapsedSeconds = 3600,
            )

        val workout3 =
            Workout(
                id = 2L, // Different ID
                date = date,
                name = "Push Day",
                notes = "Good workout",
                notesUpdatedAt = notesUpdated,
                programmeId = 10L,
                weekNumber = 1,
                dayNumber = 1,
                programmeWorkoutName = "Week 1 Day 1",
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
                durationSeconds = 3600L,
                timerStartTime = timerStart,
                timerElapsedSeconds = 3600,
            )

        assertThat(workout1).isEqualTo(workout2)
        assertThat(workout1).isNotEqualTo(workout3)
        assertThat(workout1.hashCode()).isEqualTo(workout2.hashCode())
    }

    @Test
    fun workout_toString_includesKeyInfo() {
        val workout =
            Workout(
                id = 1L,
                date = LocalDateTime.of(2024, 1, 15, 10, 0),
                name = "Leg Day",
                status = WorkoutStatus.IN_PROGRESS,
            )

        val toString = workout.toString()
        assertThat(toString).contains("Workout")
        assertThat(toString).contains("id=1")
        assertThat(toString).contains("name=Leg Day")
        assertThat(toString).contains("status=IN_PROGRESS")
    }

    @Test
    fun workout_copy_createsIndependentInstance() {
        val original =
            Workout(
                date = LocalDateTime.now(),
                name = "Upper Body",
                status = WorkoutStatus.NOT_STARTED,
            )

        val copy =
            original.copy(
                name = "Full Body",
                status = WorkoutStatus.IN_PROGRESS,
                timerElapsedSeconds = 600,
            )

        assertThat(copy.name).isEqualTo("Full Body")
        assertThat(copy.status).isEqualTo(WorkoutStatus.IN_PROGRESS)
        assertThat(copy.timerElapsedSeconds).isEqualTo(600)
        assertThat(copy.date).isEqualTo(original.date)
        assertThat(original.name).isEqualTo("Upper Body")
        assertThat(original.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        assertThat(original.timerElapsedSeconds).isEqualTo(0)
    }

    @Test
    fun workout_mixedProgrammeAndFreestyle_fields() {
        // Edge case: freestyle workout with some programme fields set (shouldn't happen in practice)
        val workout =
            Workout(
                date = LocalDateTime.now(),
                name = "Custom Workout",
                isProgrammeWorkout = false,
                weekNumber = 2, // Shouldn't be set for freestyle
                dayNumber = 3, // Shouldn't be set for freestyle
            )

        // The data class allows this, but business logic should prevent it
        assertThat(workout.isProgrammeWorkout).isFalse()
        assertThat(workout.weekNumber).isEqualTo(2)
        assertThat(workout.dayNumber).isEqualTo(3)
    }

    @Test
    fun workout_notesUpdatedWithoutNotes_edgeCase() {
        // Edge case: notesUpdatedAt set but notes is null
        val updatedAt = LocalDateTime.now()
        val workout =
            Workout(
                date = LocalDateTime.now().minusHours(1),
                notes = null,
                notesUpdatedAt = updatedAt,
            )

        assertThat(workout.notes).isNull()
        assertThat(workout.notesUpdatedAt).isEqualTo(updatedAt)
    }
}
