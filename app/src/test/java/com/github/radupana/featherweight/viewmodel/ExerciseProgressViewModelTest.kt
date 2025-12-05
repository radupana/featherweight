package com.github.radupana.featherweight.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ExerciseProgressViewModelTest {
    @Test
    fun `ExerciseProgressData stores all fields correctly`() {
        val data =
            ExerciseProgressViewModel.ExerciseProgressData(
                exerciseId = "bench-press",
                exerciseName = "Bench Press",
                allTimePR = 120f,
                allTimePRDate = LocalDate.of(2024, 12, 1),
                recentBest = 110f,
                recentBestDate = LocalDate.of(2025, 1, 10),
                recentBestPercentOfPR = 91,
                weeklyFrequency = 1.5f,
                frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.UP,
                lastPerformed = LocalDate.of(2025, 1, 10),
                progressStatus = ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS,
                progressStatusDetail = "+5kg this month",
                plateauWeeks = 0,
            )

        assertThat(data.exerciseId).isEqualTo("bench-press")
        assertThat(data.exerciseName).isEqualTo("Bench Press")
        assertThat(data.allTimePR).isEqualTo(120f)
        assertThat(data.recentBest).isEqualTo(110f)
        assertThat(data.recentBestPercentOfPR).isEqualTo(91)
        assertThat(data.weeklyFrequency).isEqualTo(1.5f)
        assertThat(data.frequencyTrend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.UP)
        assertThat(data.progressStatus).isEqualTo(ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS)
        assertThat(data.progressStatusDetail).isEqualTo("+5kg this month")
        assertThat(data.plateauWeeks).isEqualTo(0)
    }

    @Test
    fun `ExerciseProgressData can have null dates`() {
        val data =
            ExerciseProgressViewModel.ExerciseProgressData(
                exerciseId = "squat",
                exerciseName = "Squat",
                allTimePR = 0f,
                allTimePRDate = null,
                recentBest = 0f,
                recentBestDate = null,
                recentBestPercentOfPR = 0,
                weeklyFrequency = 0f,
                frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.STABLE,
                lastPerformed = null,
                progressStatus = ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK,
                progressStatusDetail = "No recent sessions",
            )

        assertThat(data.allTimePRDate).isNull()
        assertThat(data.recentBestDate).isNull()
        assertThat(data.lastPerformed).isNull()
    }

    @Test
    fun `FrequencyTrend enum has expected values`() {
        val trends = ExerciseProgressViewModel.FrequencyTrend.entries

        assertThat(trends).hasSize(3)
        assertThat(trends).contains(ExerciseProgressViewModel.FrequencyTrend.UP)
        assertThat(trends).contains(ExerciseProgressViewModel.FrequencyTrend.DOWN)
        assertThat(trends).contains(ExerciseProgressViewModel.FrequencyTrend.STABLE)
    }

    @Test
    fun `ProgressStatus enum has expected values`() {
        val statuses = ExerciseProgressViewModel.ProgressStatus.entries

        assertThat(statuses).hasSize(5)
        assertThat(statuses).contains(ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS)
        assertThat(statuses).contains(ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS)
        assertThat(statuses).contains(ExerciseProgressViewModel.ProgressStatus.PLATEAU)
        assertThat(statuses).contains(ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK)
        assertThat(statuses).contains(ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER)
    }

    @Test
    fun `ChartType enum has expected values`() {
        val chartTypes = ExerciseProgressViewModel.ChartType.entries

        assertThat(chartTypes).hasSize(2)
        assertThat(chartTypes).contains(ExerciseProgressViewModel.ChartType.ONE_RM)
        assertThat(chartTypes).contains(ExerciseProgressViewModel.ChartType.MAX_WEIGHT)
    }

    @Test
    fun `PatternType enum has expected values`() {
        val patternTypes = ExerciseProgressViewModel.PatternType.entries

        assertThat(patternTypes).hasSize(3)
        assertThat(patternTypes).contains(ExerciseProgressViewModel.PatternType.FREQUENCY)
        assertThat(patternTypes).contains(ExerciseProgressViewModel.PatternType.REP_RANGES)
        assertThat(patternTypes).contains(ExerciseProgressViewModel.PatternType.RPE_ZONES)
    }

    @Test
    fun `ExerciseProgressState Loading is a singleton`() {
        val loading1 = ExerciseProgressViewModel.ExerciseProgressState.Loading
        val loading2 = ExerciseProgressViewModel.ExerciseProgressState.Loading

        assertThat(loading1).isSameInstanceAs(loading2)
    }

    @Test
    fun `ExerciseProgressState Success can hold null data`() {
        val state = ExerciseProgressViewModel.ExerciseProgressState.Success(null)

        assertThat(state.data).isNull()
    }

    @Test
    fun `ExerciseProgressState Success can hold data`() {
        val progressData =
            ExerciseProgressViewModel.ExerciseProgressData(
                exerciseId = "deadlift",
                exerciseName = "Deadlift",
                allTimePR = 200f,
                allTimePRDate = LocalDate.now(),
                recentBest = 190f,
                recentBestPercentOfPR = 95,
                weeklyFrequency = 1f,
                frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.STABLE,
                lastPerformed = LocalDate.now(),
                progressStatus = ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS,
                progressStatusDetail = "Consistent training",
            )

        val state = ExerciseProgressViewModel.ExerciseProgressState.Success(progressData)

        assertThat(state.data).isNotNull()
        assertThat(state.data?.exerciseName).isEqualTo("Deadlift")
    }

    @Test
    fun `ExerciseProgressState Error contains message`() {
        val state = ExerciseProgressViewModel.ExerciseProgressState.Error("Database error")

        assertThat(state.message).isEqualTo("Database error")
    }

    @Test
    fun `recentBestPercentOfPR calculation boundary - 100 percent when equal`() {
        // Testing the logic: ((recentBest / allTimePR) * 100).toInt()
        val recentBest = 100f
        val allTimePR = 100f

        val percent =
            if (allTimePR > 0) {
                ((recentBest / allTimePR) * 100).toInt()
            } else {
                0
            }

        assertThat(percent).isEqualTo(100)
    }

    @Test
    fun `recentBestPercentOfPR calculation - below PR`() {
        val recentBest = 90f
        val allTimePR = 100f

        val percent =
            if (allTimePR > 0) {
                ((recentBest / allTimePR) * 100).toInt()
            } else {
                0
            }

        assertThat(percent).isEqualTo(90)
    }

    @Test
    fun `recentBestPercentOfPR calculation - zero PR returns zero`() {
        val recentBest = 100f
        val allTimePR = 0f

        val percent =
            if (allTimePR > 0) {
                ((recentBest / allTimePR) * 100).toInt()
            } else {
                0
            }

        assertThat(percent).isEqualTo(0)
    }

    @Test
    fun `frequency trend UP when recent frequency is 20 percent higher`() {
        val sessionCountLast4Weeks = 6
        val sessionCountPrevious4Weeks = 4

        val trend =
            when {
                sessionCountLast4Weeks > sessionCountPrevious4Weeks * 1.2 -> ExerciseProgressViewModel.FrequencyTrend.UP
                sessionCountLast4Weeks < sessionCountPrevious4Weeks * 0.8 -> ExerciseProgressViewModel.FrequencyTrend.DOWN
                else -> ExerciseProgressViewModel.FrequencyTrend.STABLE
            }

        // 6 > 4 * 1.2 = 4.8, so trend is UP
        assertThat(trend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.UP)
    }

    @Test
    fun `frequency trend DOWN when recent frequency is 20 percent lower`() {
        val sessionCountLast4Weeks = 2
        val sessionCountPrevious4Weeks = 5

        val trend =
            when {
                sessionCountLast4Weeks > sessionCountPrevious4Weeks * 1.2 -> ExerciseProgressViewModel.FrequencyTrend.UP
                sessionCountLast4Weeks < sessionCountPrevious4Weeks * 0.8 -> ExerciseProgressViewModel.FrequencyTrend.DOWN
                else -> ExerciseProgressViewModel.FrequencyTrend.STABLE
            }

        // 2 < 5 * 0.8 = 4, so trend is DOWN
        assertThat(trend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.DOWN)
    }

    @Test
    fun `frequency trend STABLE when within 20 percent`() {
        val sessionCountLast4Weeks = 4
        val sessionCountPrevious4Weeks = 4

        val trend =
            when {
                sessionCountLast4Weeks > sessionCountPrevious4Weeks * 1.2 -> ExerciseProgressViewModel.FrequencyTrend.UP
                sessionCountLast4Weeks < sessionCountPrevious4Weeks * 0.8 -> ExerciseProgressViewModel.FrequencyTrend.DOWN
                else -> ExerciseProgressViewModel.FrequencyTrend.STABLE
            }

        assertThat(trend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.STABLE)
    }

    @Test
    fun `frequency trend STABLE at boundary - exactly 1point2x`() {
        val sessionCountLast4Weeks = 6
        val sessionCountPrevious4Weeks = 5

        val trend =
            when {
                sessionCountLast4Weeks > sessionCountPrevious4Weeks * 1.2 -> ExerciseProgressViewModel.FrequencyTrend.UP
                sessionCountLast4Weeks < sessionCountPrevious4Weeks * 0.8 -> ExerciseProgressViewModel.FrequencyTrend.DOWN
                else -> ExerciseProgressViewModel.FrequencyTrend.STABLE
            }

        // 6 > 5 * 1.2 = 6, but we use strict >, so 6 is NOT > 6, hence STABLE
        assertThat(trend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.STABLE)
    }

    @Test
    fun `weekly frequency calculation`() {
        val sessionCountLast8Weeks = 12

        val weeklyFrequency = sessionCountLast8Weeks / 8.0f

        assertThat(weeklyFrequency).isEqualTo(1.5f)
    }
}
