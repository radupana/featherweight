package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.Exercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrainingMetricsCalculatorTest {
    @Test
    fun calculateVolumeMetrics_withMixedExercises_calculatesCorrectly() {
        val bench = createExercise("bench", "CHEST", "HORIZONTAL_PUSH", isCompound = true)
        val row = createExercise("row", "BACK", "HORIZONTAL_PULL", isCompound = true)
        val curl = createExercise("curl", "ARMS", "PULL", isCompound = false)

        val exercises = mapOf("bench" to bench, "row" to row, "curl" to curl)
        val setsByExercise = mapOf(
            "bench" to listOf(
                createSet(completed = true),
                createSet(completed = true),
                createSet(completed = true),
            ),
            "row" to listOf(
                createSet(completed = true),
                createSet(completed = true),
            ),
            "curl" to listOf(
                createSet(completed = true),
                createSet(completed = false),
            ),
        )

        val metrics = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, setsByExercise)

        assertThat(metrics.totalSets).isEqualTo(8)
        assertThat(metrics.totalCompletedSets).isEqualTo(6)
        assertThat(metrics.compoundSets).isEqualTo(5)
        assertThat(metrics.isolationSets).isEqualTo(1)
        assertThat(metrics.pushSets).isEqualTo(3)
        assertThat(metrics.pullSets).isEqualTo(3)
        assertThat(metrics.setsByCategory["CHEST"]).isEqualTo(3)
        assertThat(metrics.setsByCategory["BACK"]).isEqualTo(2)
        assertThat(metrics.setsByCategory["ARMS"]).isEqualTo(1)
    }

    @Test
    fun calculateVolumeMetrics_withOnlyPushExercises_showsImbalance() {
        val bench = createExercise("bench", "CHEST", "HORIZONTAL_PUSH", isCompound = true)
        val ohp = createExercise("ohp", "SHOULDERS", "VERTICAL_PUSH", isCompound = true)

        val exercises = mapOf("bench" to bench, "ohp" to ohp)
        val setsByExercise = mapOf(
            "bench" to listOf(createSet(true), createSet(true), createSet(true)),
            "ohp" to listOf(createSet(true), createSet(true)),
        )

        val metrics = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, setsByExercise)

        assertThat(metrics.pushSets).isEqualTo(5)
        assertThat(metrics.pullSets).isEqualTo(0)
    }

    @Test
    fun calculateVolumeMetrics_withSquatAndHinge_countsMovementPatterns() {
        val squat = createExercise("squat", "LEGS", "SQUAT", isCompound = true)
        val deadlift = createExercise("deadlift", "LEGS", "HINGE", isCompound = true)

        val exercises = mapOf("squat" to squat, "deadlift" to deadlift)
        val setsByExercise = mapOf(
            "squat" to listOf(createSet(true), createSet(true), createSet(true)),
            "deadlift" to listOf(createSet(true), createSet(true)),
        )

        val metrics = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, setsByExercise)

        assertThat(metrics.squatSets).isEqualTo(3)
        assertThat(metrics.hingeSets).isEqualTo(2)
    }

    @Test
    fun calculateIntensityMetrics_withMixedRpe_calculatesAverageCorrectly() {
        val sets = listOf(
            createSet(completed = true, rpe = 8.0f),
            createSet(completed = true, rpe = 9.0f),
            createSet(completed = true, rpe = 7.0f),
            createSet(completed = true, rpe = 8.5f),
            createSet(completed = true, rpe = 5.0f),
        )

        val metrics = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(metrics.avgRpe).isWithin(0.01f).of(7.5f)
        assertThat(metrics.setsWithRpe).isEqualTo(5)
        assertThat(metrics.setsAboveRpe8).isEqualTo(2)
        assertThat(metrics.setsBelowRpe6).isEqualTo(1)
    }

    @Test
    fun calculateIntensityMetrics_withNoRpeData_returnsZero() {
        val sets = listOf(
            createSet(completed = true, rpe = null),
            createSet(completed = true, rpe = null),
        )

        val metrics = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(metrics.avgRpe).isEqualTo(0f)
        assertThat(metrics.setsWithRpe).isEqualTo(0)
        assertThat(metrics.setsAboveRpe8).isEqualTo(0)
        assertThat(metrics.setsBelowRpe6).isEqualTo(0)
    }

    @Test
    fun calculateIntensityMetrics_withAllHighIntensity_detectsPattern() {
        val sets = listOf(
            createSet(completed = true, rpe = 9.0f),
            createSet(completed = true, rpe = 9.5f),
            createSet(completed = true, rpe = 8.5f),
            createSet(completed = true, rpe = 10.0f),
        )

        val metrics = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(metrics.avgRpe).isGreaterThan(8.5f)
        assertThat(metrics.setsAboveRpe8).isEqualTo(4)
    }

    @Test
    fun calculateProgressionMetrics_withProgressingExercise_detectsProgression() {
        val bench = createExercise("bench", "CHEST", "HORIZONTAL_PUSH", isCompound = true)
        val exercises = mapOf("bench" to bench)

        val sessions = listOf(
            WorkoutSessionData(
                date = "2025-01-01",
                exerciseData = listOf(ExerciseSessionData("bench", 100f, 1000f)),
            ),
            WorkoutSessionData(
                date = "2025-01-03",
                exerciseData = listOf(ExerciseSessionData("bench", 105f, 1050f)),
            ),
            WorkoutSessionData(
                date = "2025-01-05",
                exerciseData = listOf(ExerciseSessionData("bench", 110f, 1100f)),
            ),
        )

        val metrics = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(metrics).hasSize(1)
        assertThat(metrics[0].isProgressing).isTrue()
        assertThat(metrics[0].isPlateaued).isFalse()
    }

    @Test
    fun calculateProgressionMetrics_withPlateauedExercise_detectsPlateau() {
        val squat = createExercise("squat", "LEGS", "SQUAT", isCompound = true)
        val exercises = mapOf("squat" to squat)

        val sessions = listOf(
            WorkoutSessionData(
                date = "2025-01-01",
                exerciseData = listOf(ExerciseSessionData("squat", 100f, 1000f)),
            ),
            WorkoutSessionData(
                date = "2025-01-03",
                exerciseData = listOf(ExerciseSessionData("squat", 100f, 1000f)),
            ),
            WorkoutSessionData(
                date = "2025-01-05",
                exerciseData = listOf(ExerciseSessionData("squat", 100f, 1000f)),
            ),
        )

        val metrics = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(metrics).hasSize(1)
        assertThat(metrics[0].isPlateaued).isTrue()
        assertThat(metrics[0].isProgressing).isFalse()
    }

    @Test
    fun calculateProgressionMetrics_withLessThanThreeSessions_doesNotDetectPlateau() {
        val bench = createExercise("bench", "CHEST", "HORIZONTAL_PUSH", isCompound = true)
        val exercises = mapOf("bench" to bench)

        val sessions = listOf(
            WorkoutSessionData(
                date = "2025-01-01",
                exerciseData = listOf(ExerciseSessionData("bench", 100f, 1000f)),
            ),
            WorkoutSessionData(
                date = "2025-01-03",
                exerciseData = listOf(ExerciseSessionData("bench", 100f, 1000f)),
            ),
        )

        val metrics = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(metrics).hasSize(1)
        assertThat(metrics[0].isPlateaued).isFalse()
    }

    @Test
    fun calculateProgressionMetrics_withMultipleExercises_tracksEachSeparately() {
        val bench = createExercise("bench", "CHEST", "HORIZONTAL_PUSH", isCompound = true)
        val squat = createExercise("squat", "LEGS", "SQUAT", isCompound = true)
        val exercises = mapOf("bench" to bench, "squat" to squat)

        val sessions = listOf(
            WorkoutSessionData(
                date = "2025-01-01",
                exerciseData = listOf(
                    ExerciseSessionData("bench", 100f, 1000f),
                    ExerciseSessionData("squat", 200f, 2000f),
                ),
            ),
            WorkoutSessionData(
                date = "2025-01-03",
                exerciseData = listOf(
                    ExerciseSessionData("bench", 105f, 1050f),
                    ExerciseSessionData("squat", 200f, 2000f),
                ),
            ),
            WorkoutSessionData(
                date = "2025-01-05",
                exerciseData = listOf(
                    ExerciseSessionData("bench", 110f, 1100f),
                    ExerciseSessionData("squat", 200f, 2000f),
                ),
            ),
        )

        val metrics = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(metrics).hasSize(2)
        val benchMetric = metrics.find { it.exerciseId == "bench" }
        val squatMetric = metrics.find { it.exerciseId == "squat" }

        assertThat(benchMetric?.isProgressing).isTrue()
        assertThat(squatMetric?.isPlateaued).isTrue()
    }

    private fun createExercise(
        id: String,
        category: String,
        movementPattern: String,
        isCompound: Boolean,
    ): Exercise = Exercise(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        category = category,
        movementPattern = movementPattern,
        isCompound = isCompound,
        equipment = "BARBELL",
    )

    private fun createSet(
        completed: Boolean,
        rpe: Float? = null,
    ): SetLog = SetLog(
        exerciseLogId = "exercise_log",
        setOrder = 1,
        actualReps = 10,
        actualWeight = 100f,
        actualRpe = rpe,
        isCompleted = completed,
    )
}
