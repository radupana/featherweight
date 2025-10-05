package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class ProgrammeDataModelsTest {
    @Test
    fun programmeWeek_withAllData_shouldStoreCorrectly() {
        val week =
            ProgrammeWeek(
                id = "1",
                programmeId = "10",
                weekNumber = 1,
                name = "Foundation Week",
                description = "Build base strength",
            )

        assertThat(week.id).isEqualTo("1")
        assertThat(week.programmeId).isEqualTo("10")
        assertThat(week.weekNumber).isEqualTo(1)
        assertThat(week.name).isEqualTo("Foundation Week")
        assertThat(week.description).isEqualTo("Build base strength")
    }

    @Test
    fun programmeWeek_withDeloadWeek_shouldStoreCorrectly() {
        val week =
            ProgrammeWeek(
                programmeId = "5",
                weekNumber = 4,
                name = "Deload Week",
                description = "Recovery week",
            )

        assertThat(week.name).isEqualTo("Deload Week")
        assertThat(week.description).isEqualTo("Recovery week")
    }

    @Test
    fun programmeWeek_withNullOptionalFields_shouldHandleMissingData() {
        val week =
            ProgrammeWeek(
                programmeId = "3",
                weekNumber = 2,
                name = null,
                description = null,
            )

        assertThat(week.name).isNull()
        assertThat(week.description).isNull()
    }

    @Test
    fun programmeWeek_withHighIntensity_shouldStoreCorrectly() {
        val week =
            ProgrammeWeek(
                programmeId = "7",
                weekNumber = 3,
                name = "Peak Week",
                description = "Maximum intensity",
            )

        assertThat(week.name).isEqualTo("Peak Week")
        assertThat(week.description).isEqualTo("Maximum intensity")
    }

    @Test
    fun programmeWeek_dataClassCopy_shouldWorkCorrectly() {
        val original =
            ProgrammeWeek(
                programmeId = "1",
                weekNumber = 1,
                name = "Week 1",
                description = "Introduction",
            )

        val modified =
            original.copy(
                weekNumber = 2,
                name = "Week 2",
            )

        assertThat(modified.weekNumber).isEqualTo(2)
        assertThat(modified.name).isEqualTo("Week 2")
        assertThat(modified.programmeId).isEqualTo(original.programmeId)
    }

    @Test
    fun programmeWorkout_withAllData_shouldStoreCorrectly() {
        val workout =
            ProgrammeWorkout(
                id = "1",
                weekId = "10",
                dayNumber = 1,
                name = "Upper Power",
                description = "Heavy upper body workout",
                estimatedDuration = 75,
                workoutStructure = "{\"exercises\":[{\"name\":\"Bench Press\",\"sets\":5,\"reps\":\"3-5\"}]}",
            )

        assertThat(workout.id).isEqualTo("1")
        assertThat(workout.weekId).isEqualTo("10")
        assertThat(workout.dayNumber).isEqualTo(1)
        assertThat(workout.name).isEqualTo("Upper Power")
        assertThat(workout.description).isEqualTo("Heavy upper body workout")
        assertThat(workout.estimatedDuration).isEqualTo(75)
        assertThat(workout.workoutStructure).contains("Bench Press")
    }

    @Test
    fun programmeWorkout_withValidDayNumbers_shouldAcceptWeekdays() {
        val validDays = 1..7

        validDays.forEach { day ->
            val workout =
                ProgrammeWorkout(
                    weekId = "1",
                    dayNumber = day,
                    name = "Day $day Workout",
                    description = null,
                    estimatedDuration = 60,
                    workoutStructure = "{}",
                )

            assertThat(workout.dayNumber).isEqualTo(day)
            assertThat(workout.dayNumber).isIn(1..7)
        }
    }

    @Test
    fun programmeWorkout_withNullDescription_shouldHandleMissingData() {
        val workout =
            ProgrammeWorkout(
                weekId = "5",
                dayNumber = 3,
                name = "Quick Workout",
                description = null,
                estimatedDuration = null,
                workoutStructure = "{\"exercises\":[]}",
            )

        assertThat(workout.description).isNull()
        assertThat(workout.estimatedDuration).isNull()
    }

    @Test
    fun programmeWorkout_withLongDuration_shouldHandleExtendedWorkouts() {
        val workout =
            ProgrammeWorkout(
                weekId = "2",
                dayNumber = 6,
                name = "Saturday Marathon Session",
                description = "Long volume day",
                estimatedDuration = 180,
                workoutStructure = "{}",
            )

        assertThat(workout.estimatedDuration).isEqualTo(180)
    }

    @Test
    fun programmeProgress_withAllData_shouldStoreCorrectly() {
        val now = LocalDateTime.now()
        val progress =
            ProgrammeProgress(
                id = "1",
                programmeId = "10",
                currentWeek = 3,
                currentDay = 2,
                completedWorkouts = 8,
                totalWorkouts = 36,
                lastWorkoutDate = now,
            )

        assertThat(progress.id).isEqualTo("1")
        assertThat(progress.programmeId).isEqualTo("10")
        assertThat(progress.currentWeek).isEqualTo(3)
        assertThat(progress.currentDay).isEqualTo(2)
        assertThat(progress.completedWorkouts).isEqualTo(8)
        assertThat(progress.totalWorkouts).isEqualTo(36)
        assertThat(progress.lastWorkoutDate).isEqualTo(now)
    }

    @Test
    fun programmeProgress_withZeroProgress_shouldHandleNewProgramme() {
        val progress =
            ProgrammeProgress(
                programmeId = "5",
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = 48,
                lastWorkoutDate = null,
            )

        assertThat(progress.completedWorkouts).isEqualTo(0)
        assertThat(progress.lastWorkoutDate).isNull()
    }

    @Test
    fun programmeProgress_withPerfectAdherence_shouldShow100Percent() {
        val progress =
            ProgrammeProgress(
                programmeId = "3",
                currentWeek = 4,
                currentDay = 3,
                completedWorkouts = 12,
                totalWorkouts = 12,
                lastWorkoutDate = LocalDateTime.now(),
            )

        assertThat(progress.completedWorkouts).isEqualTo(progress.totalWorkouts)
    }

    @Test
    fun programmeProgress_calculatedCompletion_shouldMatchPercentage() {
        val progress =
            ProgrammeProgress(
                programmeId = "7",
                currentWeek = 6,
                currentDay = 2,
                completedWorkouts = 17,
                totalWorkouts = 24,
                lastWorkoutDate = LocalDateTime.now(),
            )

        val calculatedPercentage = (progress.completedWorkouts.toFloat() / progress.totalWorkouts) * 100
        assertThat(calculatedPercentage).isWithin(0.01f).of(70.83f)
    }

    @Test
    fun programmeWithDetails_shouldCombineAllData() {
        val programme =
            Programme(
                id = "1",
                name = "Test Programme",
                description = "Test",
                durationWeeks = 4,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
            )

        val week1 =
            ProgrammeWeek(
                id = "1",
                programmeId = "1",
                weekNumber = 1,
                name = "Week 1",
                description = null,
            )

        val workout1 =
            ProgrammeWorkout(
                weekId = "1",
                dayNumber = 1,
                name = "Day 1",
                description = null,
                estimatedDuration = 60,
                workoutStructure = "{}",
            )

        val weekWithWorkouts =
            ProgrammeWeekWithWorkouts(
                week = week1,
                workouts = listOf(workout1),
            )

        val progress =
            ProgrammeProgress(
                programmeId = "1",
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = 4,
                lastWorkoutDate = null,
            )

        val details =
            ProgrammeWithDetails(
                programme = programme,
                weeks = listOf(weekWithWorkouts),
                progress = progress,
                substitutions = emptyList(),
            )

        assertThat(details.programme.name).isEqualTo("Test Programme")
        assertThat(details.weeks).hasSize(1)
        assertThat(details.weeks[0].week.weekNumber).isEqualTo(1)
        assertThat(details.weeks[0].workouts).hasSize(1)
        assertThat(details.progress?.currentWeek).isEqualTo(1)
        assertThat(details.substitutions).hasSize(0)
    }

    @Test
    fun programmeWeekWithWorkouts_shouldGroupWorkoutsCorrectly() {
        val week =
            ProgrammeWeek(
                id = "1",
                programmeId = "1",
                weekNumber = 1,
                name = "Week 1",
                description = null,
            )

        val workouts =
            listOf(
                ProgrammeWorkout(weekId = "1", dayNumber = 1, name = "Monday", description = null, estimatedDuration = 60, workoutStructure = "{}"),
                ProgrammeWorkout(weekId = "1", dayNumber = 3, name = "Wednesday", description = null, estimatedDuration = 60, workoutStructure = "{}"),
                ProgrammeWorkout(weekId = "1", dayNumber = 5, name = "Friday", description = null, estimatedDuration = 60, workoutStructure = "{}"),
            )

        val weekWithWorkouts = ProgrammeWeekWithWorkouts(week, workouts)

        assertThat(weekWithWorkouts.week).isEqualTo(week)
        assertThat(weekWithWorkouts.workouts).hasSize(3)
        assertThat(weekWithWorkouts.workouts.map { it.dayNumber }).containsExactly(1, 3, 5)
    }

    @Test
    fun programmeWithDetails_withNullProgress_shouldHandleNotStarted() {
        val programme =
            Programme(
                name = "Not Started",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
            )

        val details =
            ProgrammeWithDetails(
                programme = programme,
                weeks = emptyList(),
                progress = null,
                substitutions = emptyList(),
            )

        assertThat(details.progress).isNull()
        assertThat(details.programme.status).isEqualTo(ProgrammeStatus.NOT_STARTED)
    }
}
