package com.github.radupana.featherweight.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tests for HistoryViewModel.
 *
 * Note: The HistoryViewModel has hardcoded dependencies to FeatherweightDatabase.getDatabase()
 * and ServiceLocator which require Android framework classes. These tests are limited to
 * testing state classes and utilities that don't require the ViewModel instance.
 *
 * Full ViewModel behavior tests require Android instrumentation tests or refactoring
 * the ViewModel to accept dependencies through the constructor.
 */
class HistoryViewModelTest {
    @Test
    fun `PaginatedHistoryState has correct default values`() {
        val state = PaginatedHistoryState()

        assertThat(state.programmes).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.hasMoreProgrammes).isTrue()
        assertThat(state.currentProgrammePage).isEqualTo(0)
        assertThat(state.pageSize).isEqualTo(20)
    }

    @Test
    fun `CalendarState requires currentMonth parameter`() {
        val currentMonth = YearMonth.of(2025, 1)
        val state = CalendarState(currentMonth = currentMonth)

        assertThat(state.currentMonth).isEqualTo(currentMonth)
        assertThat(state.selectedDate).isNull()
        assertThat(state.workoutCounts).isEmpty()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `CalendarState can update workout counts`() {
        val counts =
            mapOf(
                LocalDate.of(2025, 1, 1) to 2,
                LocalDate.of(2025, 1, 2) to 1,
            )

        val state =
            CalendarState(
                currentMonth = YearMonth.of(2025, 1),
                workoutCounts = counts,
            )

        assertThat(state.workoutCounts[LocalDate.of(2025, 1, 1)]).isEqualTo(2)
        assertThat(state.workoutCounts[LocalDate.of(2025, 1, 2)]).isEqualTo(1)
    }

    @Test
    fun `PaginatedHistoryState can update loading status`() {
        val loadingState = PaginatedHistoryState(isLoading = true)
        val completedState = PaginatedHistoryState(isLoading = false)

        assertThat(loadingState.isLoading).isTrue()
        assertThat(completedState.isLoading).isFalse()
    }

    @Test
    fun `PaginatedHistoryState can track error messages`() {
        val errorState =
            PaginatedHistoryState(
                isLoading = false,
                error = "Failed to load programmes",
            )

        assertThat(errorState.error).isEqualTo("Failed to load programmes")
    }

    @Test
    fun `WeekGroupState has correct default values`() {
        val state = WeekGroupState()

        assertThat(state.weeks).isEmpty()
        assertThat(state.expandedWeeks).isEmpty()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `WeekGroupState can track expanded weeks`() {
        val state =
            WeekGroupState(
                expandedWeeks = setOf("2025-W01", "2025-W02"),
            )

        assertThat(state.expandedWeeks).contains("2025-W01")
        assertThat(state.expandedWeeks).contains("2025-W02")
        assertThat(state.expandedWeeks).hasSize(2)
    }
}
