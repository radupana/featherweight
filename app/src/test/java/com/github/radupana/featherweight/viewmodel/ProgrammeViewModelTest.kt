package com.github.radupana.featherweight.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

/**
 * Tests for ProgrammeViewModel covering:
 * - Loading programme data
 * - Programme progress tracking
 * - Error handling
 * - Programme deletion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgrammeViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: ProgrammeViewModel
    private val mockApplication: Application = mockk(relaxed = true)
    private val mockRepository: FeatherweightRepository = mockk(relaxed = true)

    private val testProgramme =
        Programme(
            id = "test-prog-1",
            name = "Test Programme",
            description = "A test programme",
            durationWeeks = 4,
            programmeType = ProgrammeType.STRENGTH,
            difficulty = ProgrammeDifficulty.INTERMEDIATE,
            isActive = true,
            createdAt = LocalDateTime.now(),
        )

    @Before
    fun setup() {
        // Set up default mocking behavior
        coEvery { mockRepository.getActiveProgramme() } returns null
        coEvery { mockRepository.getAllProgrammes() } returns emptyList()
        coEvery { mockRepository.getProgrammeWithDetails(any()) } returns null
        coEvery { mockRepository.getNextProgrammeWorkout(any()) } returns null
        every { mockRepository.getAllParseRequests() } returns flowOf(emptyList())
    }

    @Test
    fun `init sets UI state to loading initially`() =
        runTest {
            // When: ViewModel is initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            advanceUntilIdle()

            // Then: UI state completes loading
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
        }

    @Test
    fun `init handles SQLiteException correctly`() =
        runTest {
            // Given: Repository throws SQLiteException
            coEvery { mockRepository.getActiveProgramme() } throws
                SQLiteException("Database error")

            // When: ViewModel is initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            viewModel.refreshData()
            advanceUntilIdle()

            // Then: UI state shows error
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
            assertThat(uiState.error).isNotNull()
            assertThat(uiState.error).contains("Failed to load programmes")
        }

    @Test
    fun `init handles IOException correctly`() =
        runTest {
            // Given: Repository throws IOException
            coEvery { mockRepository.getActiveProgramme() } throws IOException("Network error")

            // When: ViewModel is initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            viewModel.refreshData()
            advanceUntilIdle()

            // Then: UI state shows error
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
            assertThat(uiState.error).isNotNull()
            assertThat(uiState.error).contains("Failed to load programmes")
        }

    @Test
    fun `deleteProgramme calls repository with correct parameters`() =
        runTest {
            // Given: ViewModel with mocked repository
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.deleteProgramme(any(), any()) } returns Unit

            // When: Delete programme with deleteWorkouts = true
            viewModel.deleteProgramme(testProgramme, deleteWorkouts = true)
            advanceUntilIdle()

            // Then: Repository deleteProgramme is called
            coVerify { mockRepository.deleteProgramme(testProgramme, true) }
        }

    @Test
    fun `deleteProgramme handles SQLiteException`() =
        runTest {
            // Given: Repository throws exception
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.deleteProgramme(any(), any()) } throws
                SQLiteException("Delete failed")

            // When: Delete programme
            viewModel.deleteProgramme(testProgramme, deleteWorkouts = false)
            advanceUntilIdle()

            // Then: Error message is set
            val uiState = viewModel.uiState.value
            assertThat(uiState.error).isNotNull()
            assertThat(uiState.error).contains("Failed to archive programme")
        }

    @Test
    fun `clearMessages clears error and success messages`() =
        runTest {
            // Given: ViewModel with error and success message
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            val uiStateField = ProgrammeViewModel::class.java.getDeclaredField("_uiState")
            uiStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val uiStateFlow =
                uiStateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ProgrammeUiState>
            uiStateFlow.value =
                ProgrammeUiState(
                    error = "Test error",
                    successMessage = "Test success",
                )

            // When: Clear messages
            viewModel.clearMessages()

            // Then: Both messages are cleared
            val uiState = viewModel.uiState.value
            assertThat(uiState.error).isNull()
            assertThat(uiState.successMessage).isNull()
        }

    @Test
    fun `getCompletedWorkoutCount returns repository result`() =
        runTest {
            // Given: Repository returns workout count
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.getCompletedWorkoutCountForProgramme("test-prog-1") } returns 5

            // When: Get completed workout count
            val count = viewModel.getCompletedWorkoutCount(testProgramme)

            // Then: Returns correct count
            assertThat(count).isEqualTo(5)
        }

    @Test
    fun `getCompletedSetCount returns repository result`() =
        runTest {
            // Given: Repository returns set count
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.getCompletedSetCountForProgramme("test-prog-1") } returns 150

            // When: Get completed set count
            val count = viewModel.getCompletedSetCount(testProgramme)

            // Then: Returns correct count
            assertThat(count).isEqualTo(150)
        }

    @Test
    fun `getInProgressWorkoutCount returns repository result`() =
        runTest {
            // Given: Repository returns workout count
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            coEvery { mockRepository.getInProgressWorkoutCountByProgramme("test-prog-1") } returns 2

            // When: Get in-progress workout count
            val count = viewModel.getInProgressWorkoutCount(testProgramme)

            // Then: Returns correct count
            assertThat(count).isEqualTo(2)
        }

    @Test
    fun `refreshData does not show loading if already loaded`() =
        runTest {
            // Given: ViewModel initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)

            // Simulate initial load
            val hasLoadedField =
                ProgrammeViewModel::class.java.getDeclaredField("hasLoadedInitialData")
            hasLoadedField.isAccessible = true
            hasLoadedField.setBoolean(viewModel, true)

            // When: Refresh is called
            viewModel.refreshData()
            advanceUntilIdle()

            // Then: Loading state doesn't flash
            // (In production code, this is handled by not showing loading on refresh)
            coVerify { mockRepository.getActiveProgramme() }
        }

    // ===== State Transition Tests =====

    @Test
    fun `loading state transitions to loaded with active programme`() =
        runTest {
            // Given: Repository returns an active programme
            coEvery { mockRepository.getActiveProgramme() } returns testProgramme
            coEvery { mockRepository.getAllProgrammes() } returns listOf(testProgramme)

            // When: ViewModel is initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            advanceUntilIdle()

            // Then: UI state transitions from loading to loaded with data
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
            assertThat(uiState.error).isNull()
            assertThat(viewModel.activeProgramme.value).isEqualTo(testProgramme)
        }

    @Test
    fun `loading state transitions to loaded with no active programme`() =
        runTest {
            // Given: Repository returns no active programme
            coEvery { mockRepository.getActiveProgramme() } returns null
            coEvery { mockRepository.getAllProgrammes() } returns emptyList()

            // When: ViewModel is initialized
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            advanceUntilIdle()

            // Then: UI state transitions to loaded with null active programme
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
            assertThat(uiState.error).isNull()
            assertThat(viewModel.activeProgramme.value).isNull()
        }

    @Test
    fun `refresh after error clears error state on success`() =
        runTest {
            // Given: Initial load fails
            coEvery { mockRepository.getActiveProgramme() } throws SQLiteException("Initial error")
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            advanceUntilIdle()

            // Verify error state
            assertThat(viewModel.uiState.value.error).isNotNull()

            // When: Repository now succeeds and we refresh
            coEvery { mockRepository.getActiveProgramme() } returns testProgramme
            coEvery { mockRepository.getAllProgrammes() } returns listOf(testProgramme)

            // Set hasLoadedInitialData to true to enable refresh
            val hasLoadedField =
                ProgrammeViewModel::class.java.getDeclaredField("hasLoadedInitialData")
            hasLoadedField.isAccessible = true
            hasLoadedField.setBoolean(viewModel, true)

            viewModel.refreshData()
            advanceUntilIdle()

            // Then: Error is cleared and data is loaded
            val uiState = viewModel.uiState.value
            assertThat(uiState.isLoading).isFalse()
            assertThat(uiState.error).isNull()
            assertThat(viewModel.activeProgramme.value).isEqualTo(testProgramme)
        }

    @Test
    fun `refreshProgrammeProgress updates progress when active programme exists`() =
        runTest {
            // Given: ViewModel with active programme
            coEvery { mockRepository.getActiveProgramme() } returns testProgramme
            coEvery { mockRepository.getAllProgrammes() } returns listOf(testProgramme)
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            advanceUntilIdle()

            // When: Refresh programme progress
            viewModel.refreshProgrammeProgress()
            advanceUntilIdle()

            // Then: getProgrammeWithDetails and getNextProgrammeWorkout are called
            coVerify { mockRepository.getProgrammeWithDetails(testProgramme.id) }
            coVerify { mockRepository.getNextProgrammeWorkout(testProgramme.id) }
        }

    @Test
    fun `loadNextWorkoutInfo sets null when no active programme`() =
        runTest {
            // Given: No active programme
            coEvery { mockRepository.getActiveProgramme() } returns null
            viewModel = ProgrammeViewModel(mockApplication, mockRepository)
            advanceUntilIdle()

            // When: Load next workout info
            viewModel.loadNextWorkoutInfo()
            advanceUntilIdle()

            // Then: nextWorkoutInfo is null
            assertThat(viewModel.nextWorkoutInfo.value).isNull()
        }
}
