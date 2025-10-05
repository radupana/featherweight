package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.util.IdGenerator
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for the PR filtering logic in WorkoutCompletionViewModel.
 * Tests the filtering function in isolation without ViewModel lifecycle concerns.
 */
class WorkoutCompletionViewModelFilterTest {
    @Test
    fun `filterBestPersonalRecords returns empty list for empty input`() {
        val result = filterBestPersonalRecords(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `filterBestPersonalRecords returns single PR when only one exists`() {
        val singlePR = createWeightPR(exerciseId = "exercise1", weight = 100f, reps = 5)
        val result = filterBestPersonalRecords(listOf(singlePR))

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(singlePR)
    }

    @Test
    fun `filterBestPersonalRecords returns highest weight PR for same exercise WEIGHT type`() {
        val exerciseId = "romanian_deadlift"
        val pr1 = createWeightPR(exerciseId = exerciseId, weight = 125f, reps = 6)
        val pr2 = createWeightPR(exerciseId = exerciseId, weight = 125f, reps = 8)
        val pr3 = createWeightPR(exerciseId = exerciseId, weight = 120f, reps = 10)

        val result = filterBestPersonalRecords(listOf(pr1, pr2, pr3))

        assertThat(result).hasSize(1)
        assertThat(result.first().weight).isEqualTo(125f)
    }

    @Test
    fun `filterBestPersonalRecords returns highest estimated 1RM for ESTIMATED_1RM type`() {
        val exerciseId = "bench_press"
        val pr1 = createEstimated1RMPR(exerciseId = exerciseId, weight = 100f, reps = 5, estimated1RM = 115f)
        val pr2 = createEstimated1RMPR(exerciseId = exerciseId, weight = 95f, reps = 8, estimated1RM = 120f)
        val pr3 = createEstimated1RMPR(exerciseId = exerciseId, weight = 105f, reps = 3, estimated1RM = 112f)

        val result = filterBestPersonalRecords(listOf(pr1, pr2, pr3))

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(pr2) // Highest estimated 1RM
    }

    @Test
    fun `filterBestPersonalRecords keeps both WEIGHT and ESTIMATED_1RM PRs for same exercise`() {
        val exerciseId = "squat"
        val weightPR = createWeightPR(exerciseId = exerciseId, weight = 150f, reps = 5)
        val estimated1RMPR =
            createEstimated1RMPR(
                exerciseId = exerciseId,
                weight = 140f,
                reps = 8,
                estimated1RM = 175f,
            )

        val result = filterBestPersonalRecords(listOf(weightPR, estimated1RMPR))

        assertThat(result).hasSize(2)
        assertThat(result).contains(weightPR)
        assertThat(result).contains(estimated1RMPR)
    }

    @Test
    fun `filterBestPersonalRecords handles multiple exercises correctly`() {
        val squat1 = createWeightPR(exerciseId = "squat", weight = 140f, reps = 5)
        val squat2 = createWeightPR(exerciseId = "squat", weight = 150f, reps = 3)
        val bench1 = createWeightPR(exerciseId = "bench", weight = 100f, reps = 5)
        val bench2 = createWeightPR(exerciseId = "bench", weight = 95f, reps = 8)
        val deadlift = createWeightPR(exerciseId = "deadlift", weight = 180f, reps = 5)

        val result = filterBestPersonalRecords(listOf(squat1, squat2, bench1, bench2, deadlift))

        assertThat(result).hasSize(3)

        val squatPR = result.find { it.exerciseId == "squat" }
        assertThat(squatPR?.weight).isEqualTo(150f)

        val benchPR = result.find { it.exerciseId == "bench" }
        assertThat(benchPR?.weight).isEqualTo(100f)

        val deadliftPR = result.find { it.exerciseId == "deadlift" }
        assertThat(deadliftPR?.weight).isEqualTo(180f)
    }

    @Test
    fun `filterBestPersonalRecords handles null estimated1RM values correctly`() {
        val exerciseId = "pullup"
        val pr1 = createEstimated1RMPR(exerciseId = exerciseId, weight = 20f, reps = 5, estimated1RM = null)
        val pr2 = createEstimated1RMPR(exerciseId = exerciseId, weight = 15f, reps = 8, estimated1RM = 25f)
        val pr3 = createEstimated1RMPR(exerciseId = exerciseId, weight = 25f, reps = 3, estimated1RM = null)

        val result = filterBestPersonalRecords(listOf(pr1, pr2, pr3))

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(pr2)
    }

    @Test
    fun `filterBestPersonalRecords with equal weights returns one PR`() {
        val exerciseId = "romanian_deadlift"
        val pr6reps = createWeightPR(exerciseId = exerciseId, weight = 125f, reps = 6)
        val pr8reps = createWeightPR(exerciseId = exerciseId, weight = 125f, reps = 8)

        val result = filterBestPersonalRecords(listOf(pr6reps, pr8reps))

        assertThat(result).hasSize(1)
        assertThat(result.first().weight).isEqualTo(125f)
    }

    // Copy of the filtering logic from WorkoutCompletionViewModel
    private fun filterBestPersonalRecords(personalRecords: List<PersonalRecord>): List<PersonalRecord> =
        personalRecords
            .groupBy { pr -> pr.exerciseId to pr.recordType }
            .mapNotNull { (_, prsForExerciseAndType) ->
                when (prsForExerciseAndType.firstOrNull()?.recordType) {
                    PRType.WEIGHT -> prsForExerciseAndType.maxByOrNull { it.weight }
                    PRType.ESTIMATED_1RM -> prsForExerciseAndType.maxByOrNull { it.estimated1RM ?: 0f }
                    null -> null
                }
            }

    private fun createWeightPR(
        exerciseId: String,
        weight: Float,
        reps: Int,
        workoutId: String = "workout1",
    ): PersonalRecord =
        PersonalRecord(
            id = IdGenerator.generateId(),
            userId = "user1",
            exerciseId = exerciseId,
            weight = weight,
            reps = reps,
            rpe = 8f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 10f,
            recordType = PRType.WEIGHT,
            volume = weight * reps,
            estimated1RM = null,
            notes = "Test PR",
            workoutId = workoutId,
        )

    private fun createEstimated1RMPR(
        exerciseId: String,
        weight: Float,
        reps: Int,
        estimated1RM: Float?,
        workoutId: String = "workout1",
    ): PersonalRecord =
        PersonalRecord(
            id = IdGenerator.generateId(),
            userId = "user1",
            exerciseId = exerciseId,
            weight = weight,
            reps = reps,
            rpe = 8f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 10f,
            recordType = PRType.ESTIMATED_1RM,
            volume = weight * reps,
            estimated1RM = estimated1RM,
            notes = "Test 1RM PR",
            workoutId = workoutId,
        )
}
