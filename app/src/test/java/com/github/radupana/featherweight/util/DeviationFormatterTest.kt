package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class DeviationFormatterTest {
    private val timestamp = LocalDateTime.now()

    @Test
    fun `formatDeviations groups deviations by exercise`() {
        val deviations =
            listOf(
                createDeviation("ex1", DeviationType.VOLUME_DEVIATION, -0.30f),
                createDeviation("ex1", DeviationType.INTENSITY_DEVIATION, -0.05f),
                createDeviation("ex2", DeviationType.RPE_DEVIATION, 0.14f),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.exerciseDeviations).hasSize(2)
        assertThat(result.exerciseDeviations["ex1"]?.deviations).hasSize(2)
        assertThat(result.exerciseDeviations["ex2"]?.deviations).hasSize(1)
    }

    @Test
    fun `formatDeviations identifies added exercises`() {
        val deviations =
            listOf(
                createDeviation("ex1", DeviationType.EXERCISE_ADDED, 1.0f),
                createDeviation("ex2", DeviationType.EXERCISE_ADDED, 1.0f),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.addedExercises).containsExactly("ex1", "ex2")
        assertThat(result.exerciseDeviations).isEmpty()
    }

    @Test
    fun `formatDeviations identifies skipped exercises`() {
        val deviations =
            listOf(
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.EXERCISE_SKIPPED,
                    deviationMagnitude = 1.0f,
                    timestamp = timestamp,
                ),
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.EXERCISE_SKIPPED,
                    deviationMagnitude = 1.0f,
                    timestamp = timestamp,
                ),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.skippedExercises).hasSize(2)
    }

    @Test
    fun `formatDeviations captures exercise names for skipped exercises`() {
        val deviations =
            listOf(
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.EXERCISE_SKIPPED,
                    deviationMagnitude = 1.0f,
                    notes = "Rear Delt Fly",
                    timestamp = timestamp,
                ),
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.EXERCISE_SKIPPED,
                    deviationMagnitude = 1.0f,
                    notes = "Cable Bicep Curl",
                    timestamp = timestamp,
                ),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.skippedExercises).hasSize(2)
        assertThat(result.skippedExercises[0].exerciseName).isEqualTo("Rear Delt Fly")
        assertThat(result.skippedExercises[1].exerciseName).isEqualTo("Cable Bicep Curl")
    }

    @Test
    fun `formatSingleDeviation formats volume deviation with correct severity`() {
        val deviation = createDeviation("ex1", DeviationType.VOLUME_DEVIATION, -0.30f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Volume: Significantly lower")
    }

    @Test
    fun `formatSingleDeviation formats intensity deviation with higher direction`() {
        val deviation = createDeviation("ex1", DeviationType.INTENSITY_DEVIATION, 0.20f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Intensity: Moderately higher")
    }

    @Test
    fun `formatSingleDeviation uses Slightly label for deviations under 15 percent`() {
        val deviation = createDeviation("ex1", DeviationType.RPE_DEVIATION, 0.10f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("RPE: Slightly higher")
    }

    @Test
    fun `formatSingleDeviation uses Moderately label for deviations 15-30 percent`() {
        val deviation = createDeviation("ex1", DeviationType.SET_COUNT_DEVIATION, -0.25f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Sets: Moderately lower")
    }

    @Test
    fun `formatSingleDeviation uses Significantly label for deviations 30-50 percent`() {
        val deviation = createDeviation("ex1", DeviationType.REP_DEVIATION, -0.40f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Reps: Significantly lower")
    }

    @Test
    fun `formatSingleDeviation uses Much label for deviations over 50 percent`() {
        val deviation = createDeviation("ex1", DeviationType.VOLUME_DEVIATION, -0.60f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Volume: Much lower")
    }

    @Test
    fun `formatSingleDeviation handles exercise swap specially`() {
        val deviation = createDeviation("ex1", DeviationType.EXERCISE_SWAP, 1.0f)
        val result = DeviationFormatter.formatDeviations(listOf(deviation))

        assertThat(result.exerciseDeviations["ex1"]?.deviations?.first())
            .isEqualTo("Exercise swapped")
    }

    @Test
    fun `formatDeviations handles mixed deviation types correctly`() {
        val deviations =
            listOf(
                createDeviation("ex1", DeviationType.VOLUME_DEVIATION, -0.30f),
                createDeviation("ex2", DeviationType.EXERCISE_ADDED, 1.0f),
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.EXERCISE_SKIPPED,
                    deviationMagnitude = 1.0f,
                    timestamp = timestamp,
                ),
                createDeviation("ex3", DeviationType.RPE_DEVIATION, 0.14f),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.exerciseDeviations).hasSize(2)
        assertThat(result.addedExercises).containsExactly("ex2")
        assertThat(result.skippedExercises).hasSize(1)
    }

    @Test
    fun `formatDeviations ignores null exerciseLogId for non-skipped deviations`() {
        val deviations =
            listOf(
                WorkoutDeviation(
                    workoutId = "w1",
                    programmeId = "p1",
                    exerciseLogId = null,
                    deviationType = DeviationType.VOLUME_DEVIATION,
                    deviationMagnitude = -0.30f,
                    timestamp = timestamp,
                ),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.exerciseDeviations).isEmpty()
    }

    @Test
    fun `formatDeviations handles empty list`() {
        val result = DeviationFormatter.formatDeviations(emptyList())

        assertThat(result.exerciseDeviations).isEmpty()
        assertThat(result.addedExercises).isEmpty()
        assertThat(result.skippedExercises).isEmpty()
    }

    @Test
    fun `formatDeviations handles multiple deviations for same exercise`() {
        val deviations =
            listOf(
                createDeviation("ex1", DeviationType.VOLUME_DEVIATION, -0.30f),
                createDeviation("ex1", DeviationType.INTENSITY_DEVIATION, -0.05f),
                createDeviation("ex1", DeviationType.SET_COUNT_DEVIATION, -0.33f),
                createDeviation("ex1", DeviationType.REP_DEVIATION, -0.55f),
                createDeviation("ex1", DeviationType.RPE_DEVIATION, 0.14f),
            )

        val result = DeviationFormatter.formatDeviations(deviations)

        assertThat(result.exerciseDeviations["ex1"]?.deviations).hasSize(5)
        assertThat(result.exerciseDeviations["ex1"]?.deviations).containsExactly(
            "Volume: Significantly lower",
            "Intensity: Slightly lower",
            "Sets: Significantly lower",
            "Reps: Much lower",
            "RPE: Slightly higher",
        )
    }

    private fun createDeviation(
        exerciseLogId: String,
        type: DeviationType,
        magnitude: Float,
    ) = WorkoutDeviation(
        workoutId = "w1",
        programmeId = "p1",
        exerciseLogId = exerciseLogId,
        deviationType = type,
        deviationMagnitude = magnitude,
        timestamp = timestamp,
    )
}
