package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWithDetailsRaw
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ProgrammeRepositoryTest {
    private lateinit var repository: ProgrammeRepository
    private lateinit var database: FeatherweightDatabase
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var application: Application
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mock application and database
        application = mockk<Application>(relaxed = true)
        database = mockk()
        programmeDao = mockk()
        workoutDao = mockk()

        // Setup database to return DAOs
        every { database.programmeDao() } returns programmeDao
        every { database.workoutDao() } returns workoutDao

        // Create repository with mocked database
        repository = ProgrammeRepository(application, testDispatcher, database)
    }

    @Test
    fun `getAllProgrammes should return all programmes from dao`() =
        runTest(testDispatcher) {
            // Given
            val programmes =
                listOf(
                    createProgramme(1L, "Programme 1"),
                    createProgramme(2L, "Programme 2"),
                )
            coEvery { programmeDao.getAllProgrammes() } returns programmes

            // When
            val result = repository.getAllProgrammes()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(programmes)
            coVerify(exactly = 1) { programmeDao.getAllProgrammes() }
        }

    @Test
    fun `getActiveProgramme should return active programme from dao`() =
        runTest(testDispatcher) {
            // Given
            val activeProgramme = createProgramme(1L, "Active Programme", isActive = true)
            coEvery { programmeDao.getActiveProgramme() } returns activeProgramme

            // When
            val result = repository.getActiveProgramme()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(activeProgramme)
            coVerify(exactly = 1) { programmeDao.getActiveProgramme() }
        }

    @Test
    fun `getActiveProgramme should return null when no active programme`() =
        runTest(testDispatcher) {
            // Given
            coEvery { programmeDao.getActiveProgramme() } returns null

            // When
            val result = repository.getActiveProgramme()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isNull()
            coVerify(exactly = 1) { programmeDao.getActiveProgramme() }
        }

    @Test
    fun `getProgrammeById should return programme from dao`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 1L
            val programme = createProgramme(programmeId, "Test Programme")
            coEvery { programmeDao.getProgrammeById(programmeId) } returns programme

            // When
            val result = repository.getProgrammeById(programmeId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(programme)
            coVerify(exactly = 1) { programmeDao.getProgrammeById(programmeId) }
        }

    @Test
    fun `getProgrammeById should return null for non-existent programme`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 999L
            coEvery { programmeDao.getProgrammeById(programmeId) } returns null

            // When
            val result = repository.getProgrammeById(programmeId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isNull()
            coVerify(exactly = 1) { programmeDao.getProgrammeById(programmeId) }
        }

    @Test
    fun `getProgrammeWithDetails should return programme with details from dao`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 1L
            val programmeWithDetails =
                ProgrammeWithDetailsRaw(
                    programme = createProgramme(programmeId, "Test Programme"),
                    progress = null,
                )
            coEvery { programmeDao.getProgrammeWithDetails(programmeId) } returns programmeWithDetails

            // When
            val result = repository.getProgrammeWithDetails(programmeId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(programmeWithDetails)
            coVerify(exactly = 1) { programmeDao.getProgrammeWithDetails(programmeId) }
        }

    @Test
    fun `getInProgressWorkoutCountByProgramme should return count from workout dao`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 1L
            val count = 5
            coEvery { workoutDao.getInProgressWorkoutCountByProgramme(programmeId) } returns count

            // When
            val result = repository.getInProgressWorkoutCountByProgramme(programmeId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(count)
            coVerify(exactly = 1) { workoutDao.getInProgressWorkoutCountByProgramme(programmeId) }
        }

    @Test
    fun `updateProgrammeCompletionNotes should update notes when programme exists`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 1L
            val programme = createProgramme(programmeId, "Test Programme")
            val notes = "Completed successfully"
            coEvery { programmeDao.getProgrammeById(programmeId) } returns programme
            coEvery { programmeDao.updateProgramme(any()) } returns Unit

            // When
            repository.updateProgrammeCompletionNotes(programmeId, notes)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { programmeDao.getProgrammeById(programmeId) }
            coVerify(exactly = 1) {
                programmeDao.updateProgramme(
                    withArg { updatedProgramme ->
                        assertThat(updatedProgramme.completionNotes).isEqualTo(notes)
                        assertThat(updatedProgramme.id).isEqualTo(programmeId)
                        assertThat(updatedProgramme.name).isEqualTo(programme.name)
                    },
                )
            }
        }

    @Test
    fun `updateProgrammeCompletionNotes should not update when programme does not exist`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 999L
            val notes = "Completed successfully"
            coEvery { programmeDao.getProgrammeById(programmeId) } returns null

            // When
            repository.updateProgrammeCompletionNotes(programmeId, notes)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { programmeDao.getProgrammeById(programmeId) }
            coVerify(exactly = 0) { programmeDao.updateProgramme(any()) }
        }

    @Test
    fun `updateProgrammeCompletionNotes should handle null notes`() =
        runTest(testDispatcher) {
            // Given
            val programmeId = 1L
            val programme = createProgramme(programmeId, "Test Programme", completionNotes = "Old notes")
            coEvery { programmeDao.getProgrammeById(programmeId) } returns programme
            coEvery { programmeDao.updateProgramme(any()) } returns Unit

            // When
            repository.updateProgrammeCompletionNotes(programmeId, null)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                programmeDao.updateProgramme(
                    withArg { updatedProgramme ->
                        assertThat(updatedProgramme.completionNotes).isNull()
                    },
                )
            }
        }

    // Helper function to create a test Programme
    private fun createProgramme(
        id: Long,
        name: String,
        isActive: Boolean = false,
        completionNotes: String? = null,
    ) = Programme(
        id = id,
        name = name,
        description = "Test description for $name",
        durationWeeks = 8,
        programmeType = ProgrammeType.STRENGTH,
        difficulty = ProgrammeDifficulty.INTERMEDIATE,
        isCustom = false,
        isActive = isActive,
        status = if (isActive) ProgrammeStatus.IN_PROGRESS else ProgrammeStatus.NOT_STARTED,
        createdAt = LocalDateTime.now(),
        startedAt = if (isActive) LocalDateTime.now().minusDays(1) else null,
        completionNotes = completionNotes,
    )
}
