package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class BatchCompletionServiceTest {
    private lateinit var service: BatchCompletionService

    @Before
    fun setup() {
        service = BatchCompletionService()
    }

    @Test
    fun `filterBestPRsPerExercise returns empty list when given empty list`() {
        val result = service.filterBestPRsPerExercise(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `filterBestPRsPerExercise returns single PR when given single PR`() {
        val pr =
            createPR(
                exerciseId = "exercise-1",
                weight = 100f,
                estimated1RM = 100f,
            )

        val result = service.filterBestPRsPerExercise(listOf(pr))

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(pr)
    }

    @Test
    fun `filterBestPRsPerExercise keeps highest 1RM when multiple PRs for same exercise`() {
        val pr120kg =
            createPR(
                exerciseId = "squat-1",
                weight = 120f,
                estimated1RM = 120f,
            )
        val pr125kg =
            createPR(
                exerciseId = "squat-1",
                weight = 125f,
                estimated1RM = 125f,
            )
        val pr110kg =
            createPR(
                exerciseId = "squat-1",
                weight = 110f,
                estimated1RM = 110f,
            )

        val result = service.filterBestPRsPerExercise(listOf(pr120kg, pr125kg, pr110kg))

        assertThat(result).hasSize(1)
        assertThat(result.first().estimated1RM).isEqualTo(125f)
    }

    @Test
    fun `filterBestPRsPerExercise keeps best PR for each different exercise`() {
        val squatPR1 =
            createPR(
                exerciseId = "squat-1",
                weight = 120f,
                estimated1RM = 120f,
            )
        val squatPR2 =
            createPR(
                exerciseId = "squat-1",
                weight = 125f,
                estimated1RM = 125f,
            )
        val benchPR1 =
            createPR(
                exerciseId = "bench-1",
                weight = 80f,
                estimated1RM = 80f,
            )
        val benchPR2 =
            createPR(
                exerciseId = "bench-1",
                weight = 85f,
                estimated1RM = 85f,
            )

        val result =
            service.filterBestPRsPerExercise(
                listOf(squatPR1, squatPR2, benchPR1, benchPR2),
            )

        assertThat(result).hasSize(2)

        val squatResult = result.find { it.exerciseId == "squat-1" }
        assertThat(squatResult?.estimated1RM).isEqualTo(125f)

        val benchResult = result.find { it.exerciseId == "bench-1" }
        assertThat(benchResult?.estimated1RM).isEqualTo(85f)
    }

    @Test
    fun `filterBestPRsPerExercise uses weight when 1RM is null`() {
        val pr1 =
            createPR(
                exerciseId = "exercise-1",
                weight = 100f,
                estimated1RM = null,
            )
        val pr2 =
            createPR(
                exerciseId = "exercise-1",
                weight = 105f,
                estimated1RM = null,
            )

        val result = service.filterBestPRsPerExercise(listOf(pr1, pr2))

        assertThat(result).hasSize(1)
        assertThat(result.first().weight).isEqualTo(105f)
    }

    @Test
    fun `filterBestPRsPerExercise prefers PR with 1RM over PR without`() {
        val prWith1RM =
            createPR(
                exerciseId = "exercise-1",
                weight = 100f,
                estimated1RM = 110f,
            )
        val prWithout1RM =
            createPR(
                exerciseId = "exercise-1",
                weight = 105f,
                estimated1RM = null,
            )

        val result = service.filterBestPRsPerExercise(listOf(prWithout1RM, prWith1RM))

        assertThat(result).hasSize(1)
        assertThat(result.first().estimated1RM).isEqualTo(110f)
    }

    @Test
    fun `shouldUseBatchCompletion returns false for single set`() {
        assertThat(service.shouldUseBatchCompletion(1)).isFalse()
    }

    @Test
    fun `shouldUseBatchCompletion returns true for multiple sets`() {
        assertThat(service.shouldUseBatchCompletion(2)).isTrue()
        assertThat(service.shouldUseBatchCompletion(3)).isTrue()
        assertThat(service.shouldUseBatchCompletion(10)).isTrue()
    }

    @Test
    fun `shouldUseBatchCompletion returns false for zero sets`() {
        assertThat(service.shouldUseBatchCompletion(0)).isFalse()
    }

    @Test
    fun `findBestSetForOneRM returns null for empty list`() {
        val result = service.findBestSetForOneRM(emptyList()) { null }
        assertThat(result).isNull()
    }

    @Test
    fun `findBestSetForOneRM returns single set when given single set`() {
        val set = createSetLog("set-1", 100f, 5)
        val calculate1RM: (SetLog) -> Float? = { 120f }

        val result = service.findBestSetForOneRM(listOf(set), calculate1RM)

        assertThat(result).isEqualTo(set)
    }

    @Test
    fun `findBestSetForOneRM returns set with highest 1RM`() {
        val set80kg = createSetLog("set-1", 80f, 5)
        val set90kg = createSetLog("set-2", 90f, 3)
        val set100kg = createSetLog("set-3", 100f, 2)

        // Simulate 1RM calculations
        val calculate1RM: (SetLog) -> Float? = { set ->
            when (set.id) {
                "set-1" -> 93f // 80kg x 5 ≈ 93kg 1RM
                "set-2" -> 95f // 90kg x 3 ≈ 95kg 1RM
                "set-3" -> 105f // 100kg x 2 ≈ 105kg 1RM (highest)
                else -> null
            }
        }

        val result =
            service.findBestSetForOneRM(
                listOf(set80kg, set90kg, set100kg),
                calculate1RM,
            )

        assertThat(result).isEqualTo(set100kg)
    }

    @Test
    fun `findBestSetForOneRM filters out sets with null 1RM`() {
        val set1 = createSetLog("set-1", 100f, 5)
        val set2 = createSetLog("set-2", 110f, 3)
        val set3 = createSetLog("set-3", 120f, 1)

        val calculate1RM: (SetLog) -> Float? = { set ->
            when (set.id) {
                "set-1" -> 115f
                "set-2" -> null // This set has no valid 1RM
                "set-3" -> 120f
                else -> null
            }
        }

        val result =
            service.findBestSetForOneRM(
                listOf(set1, set2, set3),
                calculate1RM,
            )

        assertThat(result).isEqualTo(set3)
    }

    @Test
    fun `groupSetsByExercise groups sets correctly`() {
        val squatSet1 = createSetLog("set-1", 100f, 5, "log-squat")
        val squatSet2 = createSetLog("set-2", 110f, 3, "log-squat")
        val benchSet1 = createSetLog("set-3", 80f, 8, "log-bench")

        val getExerciseId: (SetLog) -> String? = { set ->
            when (set.exerciseLogId) {
                "log-squat" -> "squat-1"
                "log-bench" -> "bench-1"
                else -> null
            }
        }

        val result =
            service.groupSetsByExercise(
                listOf(squatSet1, squatSet2, benchSet1),
                getExerciseId,
            )

        assertThat(result).hasSize(2)
        assertThat(result["squat-1"]).containsExactly(squatSet1, squatSet2)
        assertThat(result["bench-1"]).containsExactly(benchSet1)
    }

    @Test
    fun `groupSetsByExercise filters out sets with null exercise ID`() {
        val set1 = createSetLog("set-1", 100f, 5, "log-1")
        val set2 = createSetLog("set-2", 110f, 3, "log-2")
        val set3 = createSetLog("set-3", 120f, 1, "log-3")

        val getExerciseId: (SetLog) -> String? = { set ->
            when (set.exerciseLogId) {
                "log-1" -> "exercise-1"
                "log-2" -> null // This set has no valid exercise ID
                "log-3" -> "exercise-1"
                else -> null
            }
        }

        val result =
            service.groupSetsByExercise(
                listOf(set1, set2, set3),
                getExerciseId,
            )

        assertThat(result).hasSize(1)
        assertThat(result["exercise-1"]).containsExactly(set1, set3)
    }

    @Test
    fun `groupSetsByExercise returns empty map for empty list`() {
        val result = service.groupSetsByExercise(emptyList()) { null }
        assertThat(result).isEmpty()
    }

    private fun createPR(
        exerciseId: String,
        weight: Float,
        estimated1RM: Float?,
        reps: Int = 1,
    ): PersonalRecord =
        PersonalRecord(
            id = "pr-${System.nanoTime()}",
            userId = "user-1",
            exerciseId = exerciseId,
            weight = weight,
            reps = reps,
            rpe = 10f,
            recordDate = LocalDateTime.now(),
            previousWeight = weight - 10f,
            previousReps = reps,
            previousDate = LocalDateTime.now().minusDays(7),
            improvementPercentage = 10f,
            recordType = PRType.WEIGHT,
            volume = weight * reps,
            estimated1RM = estimated1RM,
            notes = "Test PR",
            workoutId = "workout-1",
        )

    private fun createSetLog(
        id: String,
        weight: Float,
        reps: Int,
        exerciseLogId: String = "log-1",
    ): SetLog =
        SetLog(
            id = id,
            userId = "user-1",
            exerciseLogId = exerciseLogId,
            setOrder = 1,
            targetReps = reps,
            targetWeight = weight,
            targetRpe = 8f,
            actualReps = reps,
            actualWeight = weight,
            actualRpe = 8f,
            tag = null,
            notes = null,
            isCompleted = true,
            completedAt = LocalDateTime.now().toString(),
        )
}
