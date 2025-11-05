package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.domain.WorkoutSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class InsightsViewModelTest {
    @Test
    fun calculateAnalysisMetadata_with12Workouts_calculatesCorrectly() {
        val workouts = createWorkouts(12, startDate = LocalDateTime.of(2025, 1, 1, 10, 0))

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(12)
        assertThat(metadata.totalWeeks).isEqualTo(4)
        assertThat(metadata.avgFrequencyPerWeek).isWithin(0.01f).of(3.0f)
    }

    @Test
    fun calculateAnalysisMetadata_with20WorkoutsOver8Weeks_calculatesCorrectly() {
        val workouts = createWorkouts(20, startDate = LocalDateTime.of(2025, 1, 1, 10, 0))

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(20)
        assertThat(metadata.totalWeeks).isEqualTo(8)
        assertThat(metadata.avgFrequencyPerWeek).isWithin(0.1f).of(2.5f)
    }

    @Test
    fun calculateAnalysisMetadata_withEmptyList_returnsZeroMetadata() {
        val workouts = emptyList<WorkoutSummary>()

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(0)
        assertThat(metadata.totalWeeks).isEqualTo(1)
        assertThat(metadata.avgFrequencyPerWeek).isEqualTo(0f)
        assertThat(metadata.startDate).isNull()
        assertThat(metadata.endDate).isNull()
    }

    @Test
    fun calculateAnalysisMetadata_withSingleWorkout_hasMinimumOneWeek() {
        val workouts = createWorkouts(1, startDate = LocalDateTime.of(2025, 1, 1, 10, 0))

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(1)
        assertThat(metadata.totalWeeks).isEqualTo(1)
        assertThat(metadata.avgFrequencyPerWeek).isEqualTo(1f)
    }

    @Test
    fun calculateAnalysisMetadata_withHighFrequencyTraining_calculatesCorrectly() {
        val workouts = createWorkouts(42, startDate = LocalDateTime.of(2025, 1, 1, 10, 0))

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(42)
        assertThat(metadata.totalWeeks).isEqualTo(17)
        assertThat(metadata.avgFrequencyPerWeek).isWithin(0.01f).of(2.47f)
    }

    @Test
    fun calculateAnalysisMetadata_withLowFrequencyTraining_calculatesCorrectly() {
        val workouts = createWorkouts(8, startDate = LocalDateTime.of(2025, 1, 1, 10, 0))

        val metadata = InsightsViewModel.calculateAnalysisMetadata(workouts)

        assertThat(metadata.totalWorkouts).isEqualTo(8)
        assertThat(metadata.totalWeeks).isEqualTo(3)
        assertThat(metadata.avgFrequencyPerWeek).isWithin(0.1f).of(2.67f)
    }

    private fun createWorkouts(
        count: Int,
        startDate: LocalDateTime,
    ): List<WorkoutSummary> {
        val workouts = mutableListOf<WorkoutSummary>()
        var currentDate = startDate

        for (i in 0 until count) {
            workouts.add(
                WorkoutSummary(
                    id = "workout_$i",
                    date = currentDate,
                    name = "Workout $i",
                    exerciseCount = 5,
                    setCount = 15,
                    totalWeight = 1000f,
                    duration = 3600L,
                    status = WorkoutStatus.COMPLETED,
                    hasNotes = false,
                ),
            )
            currentDate = currentDate.plusDays(3)
        }

        return workouts
    }
}
