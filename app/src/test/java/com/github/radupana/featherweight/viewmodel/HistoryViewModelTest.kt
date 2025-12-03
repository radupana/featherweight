package com.github.radupana.featherweight.viewmodel

import android.app.Application
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tests for HistoryViewModel covering:
 * - Loading workout history
 * - Pagination
 * - Calendar data loading
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: HistoryViewModel
    private val mockApplication: Application = mockk(relaxed = true)
    private val mockRepository: FeatherweightRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mock repository responses
        coEvery { mockRepository.getCompletedProgrammesPaged(any(), any()) } returns emptyList()
        coEvery { mockRepository.getWorkoutCountsByMonth(any(), any()) } returns emptyMap()
        coEvery { mockRepository.getWorkoutCountsByMonthWithStatus(any(), any()) } returns emptyMap()
        coEvery { mockRepository.getWorkoutsByWeek(any()) } returns emptyList()
        every { mockApplication.applicationContext } returns mockApplication
    }

    @Test
    fun `init loads initial data successfully`() =
        runTest {
            // Given: Repository returns programmes
            coEvery { mockRepository.getCompletedProgrammesPaged(0, 20) } returns emptyList()

            // When: ViewModel is initialized
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            advanceUntilIdle()

            // Then: History state is loaded
            val historyState = viewModel.historyState.value
            assertThat(historyState.isLoading).isFalse()
        }

    @Test
    fun `loadInitialData handles IllegalArgumentException`() =
        runTest {
            // Given: Repository throws IllegalArgumentException
            coEvery { mockRepository.getCompletedProgrammesPaged(any(), any()) } throws
                IllegalArgumentException("Invalid argument")

            // When: ViewModel loads initial data
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            advanceUntilIdle()

            // Then: Error is set
            val historyState = viewModel.historyState.value
            assertThat(historyState.isLoading).isFalse()
            assertThat(historyState.error).isNotNull()
            assertThat(historyState.error).contains("Failed to load programmes")
        }

    @Test
    fun `refreshHistory reloads first page successfully`() =
        runTest {
            // Given: ViewModel with existing data
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.getCompletedProgrammesPaged(0, 20) } returns emptyList()

            // When: Refresh is called
            viewModel.refreshHistory()
            advanceUntilIdle()

            // Then: Data is reloaded
            val historyState = viewModel.historyState.value
            assertThat(historyState.currentProgrammePage).isEqualTo(0)
            assertThat(historyState.error).isNull()
        }

    @Test
    fun `clearError clears error message`() =
        runTest {
            // Given: ViewModel with error
            viewModel = HistoryViewModel(mockApplication, mockRepository)
            val historyStateField =
                HistoryViewModel::class.java.getDeclaredField("_historyState")
            historyStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val historyStateFlow =
                historyStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<PaginatedHistoryState>
            historyStateFlow.value =
                PaginatedHistoryState(
                    error = "Test error",
                )

            // When: Clear error
            viewModel.clearError()

            // Then: Error is cleared
            assertThat(viewModel.historyState.value.error).isNull()
        }

    @Test
    fun `loadCalendarData loads workout counts for current month`() =
        runTest {
            // Given: Repository returns workout counts
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            val workoutCounts =
                mapOf(
                    LocalDate.of(2025, 1, 15) to 1,
                    LocalDate.of(2025, 1, 16) to 2,
                )

            coEvery { mockRepository.getWorkoutCountsByMonth(2025, 1) } returns workoutCounts
            coEvery { mockRepository.getWorkoutCountsByMonthWithStatus(2025, 1) } returns emptyMap()

            // Set calendar state to January 2025
            val calendarStateField =
                HistoryViewModel::class.java.getDeclaredField("_calendarState")
            calendarStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val calendarStateFlow =
                calendarStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<CalendarState>
            calendarStateFlow.value =
                CalendarState(
                    currentMonth = YearMonth.of(2025, 1),
                )

            // When: Load calendar data
            viewModel.loadCalendarData()
            advanceUntilIdle()

            // Then: Calendar state is updated
            val calendarState = viewModel.calendarState.value
            assertThat(calendarState.isLoading).isFalse()
            assertThat(calendarState.workoutCounts).isEqualTo(workoutCounts)
        }

    @Test
    fun `navigateToMonth updates current month and reloads data`() =
        runTest {
            // Given: ViewModel with calendar in January
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.getWorkoutCountsByMonth(2025, 2) } returns emptyMap()
            coEvery { mockRepository.getWorkoutCountsByMonthWithStatus(2025, 2) } returns emptyMap()
            coEvery { mockRepository.getWorkoutsByWeek(any()) } returns emptyList()

            // When: Navigate to February
            viewModel.navigateToMonth(YearMonth.of(2025, 2))
            advanceUntilIdle()

            // Then: Current month is updated
            val calendarState = viewModel.calendarState.value
            assertThat(calendarState.currentMonth).isEqualTo(YearMonth.of(2025, 2))

            // And data is loaded for February
            coVerify { mockRepository.getWorkoutCountsByMonth(2025, 2) }
        }

    @Test
    fun `selectDate updates selected date`() =
        runTest {
            // Given: ViewModel with no selected date
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            // When: Select date
            val testDate = LocalDate.of(2025, 1, 15)
            viewModel.selectDate(testDate)

            // Then: Selected date is updated
            val calendarState = viewModel.calendarState.value
            assertThat(calendarState.selectedDate).isEqualTo(testDate)
        }

    @Test
    fun `toggleWeekExpanded adds week to expanded set`() =
        runTest {
            // Given: ViewModel with no expanded weeks
            viewModel = HistoryViewModel(mockApplication, mockRepository)

            // When: Toggle week expanded
            viewModel.toggleWeekExpanded("week-1")

            // Then: Week is in expanded set
            val weekGroupState = viewModel.weekGroupState.value
            assertThat(weekGroupState.expandedWeeks).contains("week-1")
        }

    @Test
    fun `toggleWeekExpanded removes week from expanded set if already expanded`() =
        runTest {
            // Given: ViewModel with week already expanded
            viewModel = HistoryViewModel(mockApplication, mockRepository)
            val weekGroupStateField =
                HistoryViewModel::class.java.getDeclaredField("_weekGroupState")
            weekGroupStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val weekGroupStateFlow =
                weekGroupStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<WeekGroupState>
            weekGroupStateFlow.value =
                WeekGroupState(
                    expandedWeeks = setOf("week-1"),
                )

            // When: Toggle week expanded
            viewModel.toggleWeekExpanded("week-1")

            // Then: Week is removed from expanded set
            val weekGroupState = viewModel.weekGroupState.value
            assertThat(weekGroupState.expandedWeeks).doesNotContain("week-1")
        }
}
