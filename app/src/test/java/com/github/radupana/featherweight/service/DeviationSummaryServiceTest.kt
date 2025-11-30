package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWithDetailsRaw
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class DeviationSummaryServiceTest {
    private lateinit var service: DeviationSummaryService

    @Before
    fun setup() {
        service = DeviationSummaryService()
    }

    @Test
    fun summarizeDeviations_emptyDeviations_returnsZeroValues() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 5)

        val summary = service.summarizeDeviations(emptyList(), programmeDetails)

        assertThat(summary.programmeName).isEqualTo("Test Programme")
        assertThat(summary.programmeType).isEqualTo("STRENGTH")
        assertThat(summary.durationWeeks).isEqualTo(4)
        assertThat(summary.workoutsCompleted).isEqualTo(5)
        assertThat(summary.workoutsPrescribed).isEqualTo(12)
        assertThat(summary.avgVolumeDeviationPercent).isEqualTo(0f)
        assertThat(summary.avgIntensityDeviationPercent).isEqualTo(0f)
        assertThat(summary.exerciseSwapCount).isEqualTo(0)
        assertThat(summary.exerciseSkipCount).isEqualTo(0)
        assertThat(summary.exerciseAddCount).isEqualTo(0)
        assertThat(summary.keyDeviations).isEmpty()
    }

    @Test
    fun summarizeDeviations_volumeDeviations_calculatesAverageCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 3)
        val deviations =
            listOf(
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.15f),
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.25f),
                createDeviation(DeviationType.VOLUME_DEVIATION, -0.10f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.avgVolumeDeviationPercent).isWithin(0.01f).of(10f)
    }

    @Test
    fun summarizeDeviations_intensityDeviations_calculatesAverageCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 2)
        val deviations =
            listOf(
                createDeviation(DeviationType.INTENSITY_DEVIATION, 0.20f),
                createDeviation(DeviationType.INTENSITY_DEVIATION, 0.10f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.avgIntensityDeviationPercent).isWithin(0.01f).of(15f)
    }

    @Test
    fun summarizeDeviations_exerciseSwaps_countsCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 4)
        val deviations =
            listOf(
                createDeviation(DeviationType.EXERCISE_SWAP, 0f),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.exerciseSwapCount).isEqualTo(3)
    }

    @Test
    fun summarizeDeviations_exerciseSkips_countsCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 4)
        val deviations =
            listOf(
                createDeviation(DeviationType.EXERCISE_SKIPPED, 0f),
                createDeviation(DeviationType.EXERCISE_SKIPPED, 0f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.exerciseSkipCount).isEqualTo(2)
    }

    @Test
    fun summarizeDeviations_exerciseAdds_countsCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 4)
        val deviations =
            listOf(
                createDeviation(DeviationType.EXERCISE_ADDED, 0f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.exerciseAddCount).isEqualTo(1)
    }

    @Test
    fun summarizeDeviations_mixedDeviations_buildsSummariesCorrectly() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 6)
        val deviations =
            listOf(
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.20f),
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.10f),
                createDeviation(DeviationType.INTENSITY_DEVIATION, -0.15f),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f, workoutId = "w1"),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f, workoutId = "w2"),
                createDeviation(DeviationType.EXERCISE_SKIPPED, 0f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.keyDeviations).isNotEmpty()
        assertThat(summary.keyDeviations).hasSize(4)
    }

    @Test
    fun summarizeDeviations_keyDeviations_limitsToMaximum() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 10)
        val deviations =
            listOf(
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.20f),
                createDeviation(DeviationType.INTENSITY_DEVIATION, 0.15f),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f),
                createDeviation(DeviationType.EXERCISE_SKIPPED, 0f),
                createDeviation(DeviationType.EXERCISE_ADDED, 0f),
                createDeviation(DeviationType.SET_COUNT_DEVIATION, 0.25f),
                createDeviation(DeviationType.REP_DEVIATION, -0.10f),
                createDeviation(DeviationType.RPE_DEVIATION, 0.30f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        assertThat(summary.keyDeviations.size).isAtMost(7)
    }

    @Test
    fun summarizeDeviations_negativeVolumeDeviation_describesAsLower() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 2)
        val deviations =
            listOf(
                createDeviation(DeviationType.VOLUME_DEVIATION, -0.20f),
                createDeviation(DeviationType.VOLUME_DEVIATION, -0.30f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        val volumeSummary = summary.keyDeviations.first { it.contains("Volume") }
        assertThat(volumeSummary).contains("lower")
    }

    @Test
    fun summarizeDeviations_positiveVolumeDeviation_describesAsHigher() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 2)
        val deviations =
            listOf(
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.20f),
                createDeviation(DeviationType.VOLUME_DEVIATION, 0.30f),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        val volumeSummary = summary.keyDeviations.first { it.contains("Volume") }
        assertThat(volumeSummary).contains("higher")
    }

    @Test
    fun summarizeDeviations_swapsInDifferentWorkouts_countsUniqueWorkouts() {
        val programmeDetails = createProgrammeWithDetails(completedWorkouts = 4)
        val deviations =
            listOf(
                createDeviation(DeviationType.EXERCISE_SWAP, 0f, workoutId = "w1"),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f, workoutId = "w1"),
                createDeviation(DeviationType.EXERCISE_SWAP, 0f, workoutId = "w2"),
            )

        val summary = service.summarizeDeviations(deviations, programmeDetails)

        val swapSummary = summary.keyDeviations.first { it.contains("Swapped") }
        assertThat(swapSummary).contains("2 of 4 workouts")
    }

    private fun createProgrammeWithDetails(completedWorkouts: Int): ProgrammeWithDetailsRaw {
        val programme =
            Programme(
                id = "prog1",
                name = "Test Programme",
                description = "A test programme",
                durationWeeks = 4,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
            )

        val progress =
            ProgrammeProgress(
                id = "progress1",
                programmeId = "prog1",
                completedWorkouts = completedWorkouts,
                currentWeek = (completedWorkouts / 3) + 1,
                currentDay = (completedWorkouts % 3) + 1,
                totalWorkouts = 12,
                lastWorkoutDate = null,
            )

        return ProgrammeWithDetailsRaw(
            programme = programme,
            progress = progress,
        )
    }

    private fun createDeviation(
        type: DeviationType,
        magnitude: Float,
        workoutId: String = "workout1",
    ): WorkoutDeviation =
        WorkoutDeviation(
            id = "dev_${System.nanoTime()}",
            workoutId = workoutId,
            programmeId = "prog1",
            deviationType = type,
            deviationMagnitude = magnitude,
            timestamp = LocalDateTime.now(),
        )
}
