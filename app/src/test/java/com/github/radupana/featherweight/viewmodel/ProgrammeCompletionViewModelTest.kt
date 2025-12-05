package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.programme.ExerciseFrequency
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.data.programme.ProgrammeInsights
import com.github.radupana.featherweight.data.programme.StrengthImprovement
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ProgrammeCompletionViewModelTest {
    private lateinit var mockRepository: FeatherweightRepository
    private lateinit var viewModel: ProgrammeCompletionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        viewModel = ProgrammeCompletionViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true`() =
        runTest {
            val state = viewModel.uiState.first()
            assertThat(state.isLoading).isTrue()
        }

    @Test
    fun `initial state has null completionStats`() =
        runTest {
            val state = viewModel.uiState.first()
            assertThat(state.completionStats).isNull()
        }

    @Test
    fun `initial state has null error`() =
        runTest {
            val state = viewModel.uiState.first()
            assertThat(state.error).isNull()
        }

    @Test
    fun `loadProgrammeCompletionStats updates state with stats on success`() =
        runTest {
            val programmeId = "prog-123"
            val stats = createTestStats(programmeId)
            coEvery { mockRepository.calculateProgrammeCompletionStats(programmeId) } returns stats

            viewModel.loadProgrammeCompletionStats(programmeId)
            advanceUntilIdle()

            val state = viewModel.uiState.first()
            assertThat(state.isLoading).isFalse()
            assertThat(state.completionStats).isEqualTo(stats)
            assertThat(state.error).isNull()
        }

    @Test
    fun `loadProgrammeCompletionStats sets error on exception`() =
        runTest {
            val programmeId = "prog-456"
            val errorMessage = "Programme not found"
            coEvery {
                mockRepository.calculateProgrammeCompletionStats(programmeId)
            } throws IllegalStateException(errorMessage)

            viewModel.loadProgrammeCompletionStats(programmeId)
            advanceUntilIdle()

            val state = viewModel.uiState.first()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo(errorMessage)
        }

    @Test
    fun `saveProgrammeNotes calls repository`() =
        runTest {
            val programmeId = "prog-789"
            val notes = "Great programme, improved my squat significantly"
            coEvery { mockRepository.updateProgrammeCompletionNotes(programmeId, notes) } returns Unit

            viewModel.saveProgrammeNotes(programmeId, notes)
            advanceUntilIdle()

            coVerify { mockRepository.updateProgrammeCompletionNotes(programmeId, notes) }
        }

    @Test
    fun `saveProgrammeNotes sets error on exception`() =
        runTest {
            val programmeId = "prog-999"
            val notes = "Notes"
            val errorMessage = "Database error"
            coEvery {
                mockRepository.updateProgrammeCompletionNotes(programmeId, notes)
            } throws IllegalStateException(errorMessage)

            viewModel.saveProgrammeNotes(programmeId, notes)
            advanceUntilIdle()

            val state = viewModel.uiState.first()
            assertThat(state.error).contains("Failed to save notes")
            assertThat(state.error).contains(errorMessage)
        }

    @Test
    fun `ProgrammeCompletionUiState default values are correct`() {
        val state = ProgrammeCompletionUiState()

        assertThat(state.isLoading).isTrue()
        assertThat(state.completionStats).isNull()
        assertThat(state.error).isNull()
    }

    @Test
    fun `ProgrammeCompletionUiState copy works correctly`() {
        val state = ProgrammeCompletionUiState()
        val updated = state.copy(isLoading = false, error = "Some error")

        assertThat(updated.isLoading).isFalse()
        assertThat(updated.error).isEqualTo("Some error")
        assertThat(updated.completionStats).isNull()
    }

    private fun createTestStats(programmeId: String) =
        ProgrammeCompletionStats(
            programmeId = programmeId,
            programmeName = "Test Programme",
            startDate = LocalDateTime.of(2025, 1, 1, 9, 0),
            endDate = LocalDateTime.of(2025, 3, 1, 9, 0),
            totalWorkouts = 24,
            completedWorkouts = 22,
            totalVolume = 50000f,
            averageWorkoutDuration = Duration.ofMinutes(60),
            totalPRs = 5,
            strengthImprovements =
                listOf(
                    StrengthImprovement(
                        exerciseName = "Squat",
                        startingMax = 100f,
                        endingMax = 120f,
                        improvementKg = 20f,
                        improvementPercentage = 20f,
                    ),
                ),
            averageStrengthImprovement = 15f,
            insights =
                ProgrammeInsights(
                    totalTrainingDays = 22,
                    mostConsistentDay = "Monday",
                    averageRestDaysBetweenWorkouts = 2.5f,
                ),
            topExercises =
                listOf(
                    ExerciseFrequency(exerciseName = "Squat", frequency = 24),
                    ExerciseFrequency(exerciseName = "Bench Press", frequency = 16),
                ),
        )
}
