package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.ProgressTrend
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class ExerciseProgressionTest {
    @Test
    fun `personal record should be higher than previous weight`() {
        val previousPR =
            PersonalRecord(
                exerciseId = "1",
                weight = 100f,
                reps = 1,
                recordType = PRType.WEIGHT,
                recordDate = LocalDateTime.now().minusDays(7),
                previousWeight = 95f,
                previousReps = 1,
                previousDate = LocalDateTime.now().minusDays(14),
                improvementPercentage = 5.26f,
                estimated1RM = 100f,
            )

        val newPR =
            PersonalRecord(
                exerciseId = "1",
                weight = 105f,
                reps = 1,
                recordType = PRType.WEIGHT,
                recordDate = LocalDateTime.now(),
                previousWeight = 100f,
                previousReps = 1,
                previousDate = LocalDateTime.now().minusDays(7),
                improvementPercentage = 5f,
                estimated1RM = 105f,
            )

        // New PR should be heavier than previous
        assertThat(newPR.weight).isGreaterThan(previousPR.weight)
        assertThat(newPR.improvementPercentage).isGreaterThan(0f)
        assertThat(newPR.estimated1RM).isGreaterThan(previousPR.estimated1RM!!)
    }

    @Test
    fun `estimated 1RM PR with higher volume is tracked correctly`() {
        val estimated1RMPR =
            PersonalRecord(
                exerciseId = "1",
                weight = 80f,
                reps = 12,
                recordType = PRType.ESTIMATED_1RM,
                recordDate = LocalDateTime.now(),
                previousWeight = 80f,
                previousReps = 10,
                previousDate = LocalDateTime.now().minusDays(7),
                improvementPercentage = 20f, // Better estimated 1RM due to more reps
                volume = 960f, // 80kg × 12 reps
            )

        assertThat(estimated1RMPR.volume).isEqualTo(80f * 12)
        assertThat(estimated1RMPR.recordType).isEqualTo(PRType.ESTIMATED_1RM)

        val previousVolume = estimated1RMPR.previousWeight!! * estimated1RMPR.previousReps!!
        assertThat(estimated1RMPR.volume).isGreaterThan(previousVolume)
    }

    @Test
    fun `estimated 1RM PR tracks properly`() {
        val oneRMPR =
            PersonalRecord(
                exerciseId = "1",
                weight = 90f,
                reps = 5,
                recordType = PRType.ESTIMATED_1RM,
                recordDate = LocalDateTime.now(),
                previousWeight = 85f,
                previousReps = 5,
                previousDate = LocalDateTime.now().minusDays(14),
                improvementPercentage = 5.88f,
                estimated1RM = 105f, // Estimated from 90kg × 5 reps
            )

        assertThat(oneRMPR.recordType).isEqualTo(PRType.ESTIMATED_1RM)
        assertThat(oneRMPR.estimated1RM).isNotNull()
        assertThat(oneRMPR.estimated1RM).isGreaterThan(oneRMPR.weight) // 1RM should be higher than 5RM weight
    }

    @Test
    fun `global exercise progress tracking`() {
        val progress =
            GlobalExerciseProgress(
                exerciseId = "1",
                currentWorkingWeight = 100f,
                estimatedMax = 110f,
                lastUpdated = LocalDateTime.now(),
                trend = ProgressTrend.IMPROVING,
                weeksAtCurrentWeight = 2,
                lastProgressionDate = LocalDateTime.now().minusWeeks(2),
                lastPrWeight = 95f,
                lastPrDate = LocalDateTime.now().minusWeeks(4),
            )

        assertThat(progress.trend).isEqualTo(ProgressTrend.IMPROVING)
        assertThat(progress.estimatedMax).isGreaterThan(progress.currentWorkingWeight)
        assertThat(progress.weeksAtCurrentWeight).isLessThan(4) // Not stalling
    }

    @Test
    fun `stalling detection works correctly`() {
        val stallingProgress =
            GlobalExerciseProgress(
                exerciseId = "1",
                currentWorkingWeight = 80f,
                estimatedMax = 85f,
                lastUpdated = LocalDateTime.now(),
                trend = ProgressTrend.STALLING,
                weeksAtCurrentWeight = 6, // 6 weeks at same weight = stalling
                lastProgressionDate = LocalDateTime.now().minusWeeks(6),
                lastPrWeight = 80f,
                lastPrDate = LocalDateTime.now().minusWeeks(6),
            )

        assertThat(stallingProgress.trend).isEqualTo(ProgressTrend.STALLING)
        assertThat(stallingProgress.weeksAtCurrentWeight).isGreaterThan(4)
        assertThat(stallingProgress.lastProgressionDate).isNotNull()
    }

    @Test
    fun `declining trend detection`() {
        val decliningProgress =
            GlobalExerciseProgress(
                exerciseId = "1",
                currentWorkingWeight = 70f,
                estimatedMax = 75f,
                lastUpdated = LocalDateTime.now(),
                trend = ProgressTrend.DECLINING,
                weeksAtCurrentWeight = 1,
                lastProgressionDate = LocalDateTime.now().minusWeeks(8),
                lastPrWeight = 80f, // Was lifting heavier before
                lastPrDate = LocalDateTime.now().minusWeeks(8),
            )

        assertThat(decliningProgress.trend).isEqualTo(ProgressTrend.DECLINING)
        assertThat(decliningProgress.currentWorkingWeight).isLessThan(decliningProgress.lastPrWeight!!)
    }
}
