package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.Exercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrainingMetricsCalculatorTest {
    // Helper functions
    private fun createExercise(
        id: String,
        name: String = "Test Exercise",
        category: String = "CHEST",
        movementPattern: String? = "PUSH",
        isCompound: Boolean = true,
    ) = Exercise(
        id = id,
        name = name,
        category = category,
        movementPattern = movementPattern,
        isCompound = isCompound,
        equipment = "BARBELL",
    )

    private fun createSetLog(
        exerciseLogId: String,
        setOrder: Int = 1,
        isCompleted: Boolean = true,
        actualRpe: Float? = null,
        actualWeight: Float = 100f,
        actualReps: Int = 10,
    ) = SetLog(
        id = "set-$exerciseLogId-$setOrder",
        exerciseLogId = exerciseLogId,
        setOrder = setOrder,
        isCompleted = isCompleted,
        actualRpe = actualRpe,
        actualWeight = actualWeight,
        actualReps = actualReps,
    )

    // ====== VolumeMetrics Tests ======

    @Test
    fun `calculateVolumeMetrics returns zero counts for empty inputs`() {
        val result =
            TrainingMetricsCalculator.calculateVolumeMetrics(
                exercises = emptyMap(),
                setsByExercise = emptyMap(),
            )

        assertThat(result.totalSets).isEqualTo(0)
        assertThat(result.totalCompletedSets).isEqualTo(0)
        assertThat(result.compoundSets).isEqualTo(0)
        assertThat(result.isolationSets).isEqualTo(0)
    }

    @Test
    fun `calculateVolumeMetrics counts total and completed sets correctly`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1"),
            )
        val sets =
            mapOf(
                "ex1" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = true),
                        createSetLog("log1", 3, isCompleted = false),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.totalSets).isEqualTo(3)
        assertThat(result.totalCompletedSets).isEqualTo(2)
    }

    @Test
    fun `calculateVolumeMetrics separates compound and isolation sets`() {
        val exercises =
            mapOf(
                "compound" to createExercise("compound", isCompound = true),
                "isolation" to createExercise("isolation", isCompound = false),
            )
        val sets =
            mapOf(
                "compound" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = true),
                    ),
                "isolation" to
                    listOf(
                        createSetLog("log2", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.compoundSets).isEqualTo(2)
        assertThat(result.isolationSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts PUSH movement pattern sets`() {
        val exercises =
            mapOf(
                "push" to createExercise("push", movementPattern = "PUSH"),
            )
        val sets =
            mapOf(
                "push" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pushSets).isEqualTo(2)
    }

    @Test
    fun `calculateVolumeMetrics counts VERTICAL_PUSH as push sets`() {
        val exercises =
            mapOf(
                "ohp" to createExercise("ohp", movementPattern = "VERTICAL_PUSH"),
            )
        val sets =
            mapOf(
                "ohp" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pushSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts HORIZONTAL_PUSH as push sets`() {
        val exercises =
            mapOf(
                "bench" to createExercise("bench", movementPattern = "HORIZONTAL_PUSH"),
            )
        val sets =
            mapOf(
                "bench" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pushSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts PULL movement pattern sets`() {
        val exercises =
            mapOf(
                "pull" to createExercise("pull", movementPattern = "PULL"),
            )
        val sets =
            mapOf(
                "pull" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pullSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts VERTICAL_PULL as pull sets`() {
        val exercises =
            mapOf(
                "pullup" to createExercise("pullup", movementPattern = "VERTICAL_PULL"),
            )
        val sets =
            mapOf(
                "pullup" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pullSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts HORIZONTAL_PULL as pull sets`() {
        val exercises =
            mapOf(
                "row" to createExercise("row", movementPattern = "HORIZONTAL_PULL"),
            )
        val sets =
            mapOf(
                "row" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.pullSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics counts SQUAT movement pattern sets`() {
        val exercises =
            mapOf(
                "squat" to createExercise("squat", movementPattern = "SQUAT"),
            )
        val sets =
            mapOf(
                "squat" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = true),
                        createSetLog("log1", 3, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.squatSets).isEqualTo(3)
    }

    @Test
    fun `calculateVolumeMetrics counts HINGE movement pattern sets`() {
        val exercises =
            mapOf(
                "deadlift" to createExercise("deadlift", movementPattern = "HINGE"),
            )
        val sets =
            mapOf(
                "deadlift" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.hingeSets).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics groups sets by category`() {
        val exercises =
            mapOf(
                "bench" to createExercise("bench", category = "CHEST"),
                "row" to createExercise("row", category = "BACK"),
            )
        val sets =
            mapOf(
                "bench" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = true),
                    ),
                "row" to
                    listOf(
                        createSetLog("log2", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.setsByCategory["CHEST"]).isEqualTo(2)
        assertThat(result.setsByCategory["BACK"]).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics ignores incomplete sets for category counts`() {
        val exercises =
            mapOf(
                "bench" to createExercise("bench", category = "CHEST"),
            )
        val sets =
            mapOf(
                "bench" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                        createSetLog("log1", 2, isCompleted = false),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.setsByCategory["CHEST"]).isEqualTo(1)
    }

    @Test
    fun `calculateVolumeMetrics handles exercise not in map`() {
        val exercises = emptyMap<String, Exercise>()
        val sets =
            mapOf(
                "unknown" to
                    listOf(
                        createSetLog("log1", 1, isCompleted = true),
                    ),
            )

        val result = TrainingMetricsCalculator.calculateVolumeMetrics(exercises, sets)

        assertThat(result.totalSets).isEqualTo(1)
        assertThat(result.totalCompletedSets).isEqualTo(1)
        assertThat(result.compoundSets).isEqualTo(0)
        assertThat(result.isolationSets).isEqualTo(0)
    }

    // ====== IntensityMetrics Tests ======

    @Test
    fun `calculateIntensityMetrics returns zero for empty sets`() {
        val result = TrainingMetricsCalculator.calculateIntensityMetrics(emptyList())

        assertThat(result.avgRpe).isEqualTo(0f)
        assertThat(result.setsWithRpe).isEqualTo(0)
        assertThat(result.setsAboveRpe8).isEqualTo(0)
        assertThat(result.setsBelowRpe6).isEqualTo(0)
    }

    @Test
    fun `calculateIntensityMetrics calculates average RPE correctly`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 7f),
                createSetLog("log1", 2, actualRpe = 8f),
                createSetLog("log1", 3, actualRpe = 9f),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.avgRpe).isEqualTo(8f)
    }

    @Test
    fun `calculateIntensityMetrics counts sets with RPE`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 7f),
                createSetLog("log1", 2, actualRpe = 8f),
                createSetLog("log1", 3, actualRpe = null),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsWithRpe).isEqualTo(2)
    }

    @Test
    fun `calculateIntensityMetrics counts sets above RPE 8`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 7f),
                createSetLog("log1", 2, actualRpe = 8.5f),
                createSetLog("log1", 3, actualRpe = 9f),
                createSetLog("log1", 4, actualRpe = 10f),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsAboveRpe8).isEqualTo(3)
    }

    @Test
    fun `calculateIntensityMetrics does not count RPE 8 as above 8`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 8f),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsAboveRpe8).isEqualTo(0)
    }

    @Test
    fun `calculateIntensityMetrics counts sets below RPE 6`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 5f),
                createSetLog("log1", 2, actualRpe = 5.5f),
                createSetLog("log1", 3, actualRpe = 6f),
                createSetLog("log1", 4, actualRpe = 7f),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsBelowRpe6).isEqualTo(2)
    }

    @Test
    fun `calculateIntensityMetrics does not count RPE 6 as below 6`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 6f),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsBelowRpe6).isEqualTo(0)
    }

    @Test
    fun `calculateIntensityMetrics ignores incomplete sets`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = 9f, isCompleted = true),
                createSetLog("log1", 2, actualRpe = 10f, isCompleted = false),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsWithRpe).isEqualTo(1)
        assertThat(result.avgRpe).isEqualTo(9f)
    }

    @Test
    fun `calculateIntensityMetrics handles null RPE in setsAboveRpe8 check`() {
        val sets =
            listOf(
                createSetLog("log1", 1, actualRpe = null),
            )

        val result = TrainingMetricsCalculator.calculateIntensityMetrics(sets)

        assertThat(result.setsAboveRpe8).isEqualTo(0)
        assertThat(result.setsBelowRpe6).isEqualTo(0)
    }

    // ====== ProgressionMetrics Tests ======

    @Test
    fun `calculateProgressionMetrics returns empty for empty inputs`() {
        val result =
            TrainingMetricsCalculator.calculateProgressionMetrics(
                exercises = emptyMap(),
                workoutSessions = emptyList(),
            )

        assertThat(result).isEmpty()
    }

    @Test
    fun `calculateProgressionMetrics requires at least 2 sessions for progression`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).isEmpty()
    }

    @Test
    fun `calculateProgressionMetrics detects progression when weight increases`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 105f, 1050f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].isProgressing).isTrue()
    }

    @Test
    fun `calculateProgressionMetrics does not detect progression when weight decreases`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 105f, 1050f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].isProgressing).isFalse()
    }

    @Test
    fun `calculateProgressionMetrics detects plateau when weight is constant for 3 sessions`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-15",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].isPlateaued).isTrue()
    }

    @Test
    fun `calculateProgressionMetrics does not detect plateau with only 2 sessions`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].isPlateaued).isFalse()
    }

    @Test
    fun `calculateProgressionMetrics does not detect plateau with zero weight`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 0f, 0f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 0f, 0f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-15",
                    exerciseData = listOf(ExerciseSessionData("ex1", 0f, 0f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].isPlateaued).isFalse()
    }

    @Test
    fun `calculateProgressionMetrics sorts sessions by date`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-15",
                    exerciseData = listOf(ExerciseSessionData("ex1", 105f, 1050f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].sessions[0].date).isEqualTo("2024-01-01")
        assertThat(result[0].sessions[1].date).isEqualTo("2024-01-15")
        assertThat(result[0].isProgressing).isTrue()
    }

    @Test
    fun `calculateProgressionMetrics excludes exercises not in exercise map`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData =
                        listOf(
                            ExerciseSessionData("ex1", 100f, 1000f),
                            ExerciseSessionData("unknown", 50f, 500f),
                        ),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData =
                        listOf(
                            ExerciseSessionData("ex1", 105f, 1050f),
                            ExerciseSessionData("unknown", 55f, 550f),
                        ),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseId).isEqualTo("ex1")
    }

    @Test
    fun `calculateProgressionMetrics handles multiple exercises`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
                "ex2" to createExercise("ex2", name = "Squat"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData =
                        listOf(
                            ExerciseSessionData("ex1", 100f, 1000f),
                            ExerciseSessionData("ex2", 140f, 1400f),
                        ),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData =
                        listOf(
                            ExerciseSessionData("ex1", 105f, 1050f),
                            ExerciseSessionData("ex2", 145f, 1450f),
                        ),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.exerciseId }).containsExactly("ex1", "ex2")
        assertThat(result.all { it.isProgressing }).isTrue()
    }

    @Test
    fun `calculateProgressionMetrics uses last 3 sessions for progression detection`() {
        val exercises =
            mapOf(
                "ex1" to createExercise("ex1", name = "Bench Press"),
            )
        val sessions =
            listOf(
                WorkoutSessionData(
                    date = "2024-01-01",
                    exerciseData = listOf(ExerciseSessionData("ex1", 100f, 1000f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-08",
                    exerciseData = listOf(ExerciseSessionData("ex1", 120f, 1200f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-15",
                    exerciseData = listOf(ExerciseSessionData("ex1", 110f, 1100f)),
                ),
                WorkoutSessionData(
                    date = "2024-01-22",
                    exerciseData = listOf(ExerciseSessionData("ex1", 115f, 1150f)),
                ),
            )

        val result = TrainingMetricsCalculator.calculateProgressionMetrics(exercises, sessions)

        assertThat(result).hasSize(1)
        // Last 3 sessions weights: [120, 110, 115]
        // Compares first(120) vs last(115) - 115 is NOT > 120, so not progressing
        assertThat(result[0].isProgressing).isFalse()
    }

    // ====== Data Class Tests ======

    @Test
    fun `VolumeMetrics stores all fields correctly`() {
        val metrics =
            VolumeMetrics(
                totalSets = 10,
                totalCompletedSets = 8,
                compoundSets = 6,
                isolationSets = 2,
                pushSets = 3,
                pullSets = 2,
                squatSets = 2,
                hingeSets = 1,
                setsByCategory = mapOf("CHEST" to 3, "BACK" to 2),
            )

        assertThat(metrics.totalSets).isEqualTo(10)
        assertThat(metrics.totalCompletedSets).isEqualTo(8)
        assertThat(metrics.compoundSets).isEqualTo(6)
        assertThat(metrics.isolationSets).isEqualTo(2)
        assertThat(metrics.pushSets).isEqualTo(3)
        assertThat(metrics.pullSets).isEqualTo(2)
        assertThat(metrics.squatSets).isEqualTo(2)
        assertThat(metrics.hingeSets).isEqualTo(1)
        assertThat(metrics.setsByCategory).containsEntry("CHEST", 3)
    }

    @Test
    fun `IntensityMetrics stores all fields correctly`() {
        val metrics =
            IntensityMetrics(
                avgRpe = 7.5f,
                setsWithRpe = 12,
                setsAboveRpe8 = 4,
                setsBelowRpe6 = 2,
            )

        assertThat(metrics.avgRpe).isEqualTo(7.5f)
        assertThat(metrics.setsWithRpe).isEqualTo(12)
        assertThat(metrics.setsAboveRpe8).isEqualTo(4)
        assertThat(metrics.setsBelowRpe6).isEqualTo(2)
    }

    @Test
    fun `ExerciseProgression stores all fields correctly`() {
        val sessions =
            listOf(
                SessionData("2024-01-01", 100f, 1000f),
                SessionData("2024-01-08", 105f, 1050f),
            )
        val progression =
            ExerciseProgression(
                exerciseId = "ex1",
                exerciseName = "Bench Press",
                sessions = sessions,
                isProgressing = true,
                isPlateaued = false,
            )

        assertThat(progression.exerciseId).isEqualTo("ex1")
        assertThat(progression.exerciseName).isEqualTo("Bench Press")
        assertThat(progression.sessions).hasSize(2)
        assertThat(progression.isProgressing).isTrue()
        assertThat(progression.isPlateaued).isFalse()
    }

    @Test
    fun `SessionData stores all fields correctly`() {
        val session =
            SessionData(
                date = "2024-01-01",
                maxWeight = 100f,
                totalVolume = 2000f,
            )

        assertThat(session.date).isEqualTo("2024-01-01")
        assertThat(session.maxWeight).isEqualTo(100f)
        assertThat(session.totalVolume).isEqualTo(2000f)
    }

    @Test
    fun `WorkoutSessionData stores all fields correctly`() {
        val exerciseData =
            listOf(
                ExerciseSessionData("ex1", 100f, 1000f),
            )
        val session =
            WorkoutSessionData(
                date = "2024-01-01",
                exerciseData = exerciseData,
            )

        assertThat(session.date).isEqualTo("2024-01-01")
        assertThat(session.exerciseData).hasSize(1)
    }

    @Test
    fun `ExerciseSessionData can be destructured`() {
        val data = ExerciseSessionData("ex1", 100f, 1000f)
        val (exerciseId, maxWeight, totalVolume) = data

        assertThat(exerciseId).isEqualTo("ex1")
        assertThat(maxWeight).isEqualTo(100f)
        assertThat(totalVolume).isEqualTo(1000f)
    }
}
