package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ProgrammeDaoTest {
    private lateinit var dao: ProgrammeDao
    private lateinit var mockProgramme: Programme
    private lateinit var mockWeek: ProgrammeWeek
    private lateinit var mockWorkout: ProgrammeWorkout
    private lateinit var testDate: LocalDateTime

    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        
        mockProgramme = Programme(
            id = 1L,
            name = "Test Programme",
            description = "Test Description",
            programmeType = ProgrammeType.STRENGTH,
            difficulty = ProgrammeDifficulty.INTERMEDIATE,
            durationWeeks = 12,
            status = ProgrammeStatus.NOT_STARTED,
            createdAt = testDate,
            isActive = false,
            isCustom = false
        )
        
        mockWeek = ProgrammeWeek(
            id = 1L,
            programmeId = 1L,
            weekNumber = 1,
            name = "Week 1",
            description = "First week",
            focusAreas = "Strength"
        )
        
        mockWorkout = ProgrammeWorkout(
            id = 1L,
            weekId = 1L,
            dayNumber = 1,
            name = "Day 1",
            description = "First workout",
            estimatedDuration = 60,
            workoutStructure = "{}"
        )
    }

    @Test
    fun `insertProgramme_validProgramme_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertProgramme(any()) } returns 1L

        // Act
        val result = dao.insertProgramme(mockProgramme)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertProgramme(mockProgramme) }
    }

    @Test
    fun `updateProgramme_existingProgramme_updatesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.updateProgramme(any()) } just runs

        // Act
        dao.updateProgramme(mockProgramme)

        // Assert
        coVerify(exactly = 1) { dao.updateProgramme(mockProgramme) }
    }

    @Test
    fun `deleteProgramme_existingProgramme_deletesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.deleteProgramme(any()) } just runs

        // Act
        dao.deleteProgramme(mockProgramme)

        // Assert
        coVerify(exactly = 1) { dao.deleteProgramme(mockProgramme) }
    }

    @Test
    fun `getProgrammeById_existingId_returnsProgramme`() = runTest {
        // Arrange
        coEvery { dao.getProgrammeById(1L) } returns mockProgramme

        // Act
        val result = dao.getProgrammeById(1L)

        // Assert
        assertThat(result).isEqualTo(mockProgramme)
    }

    @Test
    fun `getProgrammeById_nonExistentId_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getProgrammeById(999L) } returns null

        // Act
        val result = dao.getProgrammeById(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getActiveProgramme_hasActive_returnsActiveProgramme`() = runTest {
        // Arrange
        val activeProgramme = mockProgramme.copy(isActive = true)
        coEvery { dao.getActiveProgramme() } returns activeProgramme

        // Act
        val result = dao.getActiveProgramme()

        // Assert
        assertThat(result).isNotNull()
        assertThat(result?.isActive).isTrue()
    }

    @Test
    fun `getActiveProgramme_noActive_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getActiveProgramme() } returns null

        // Act
        val result = dao.getActiveProgramme()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getAllProgrammes_hasProgrammes_returnsListSortedByDate`() = runTest {
        // Arrange
        val programmes = listOf(
            mockProgramme.copy(id = 1),
            mockProgramme.copy(id = 2),
            mockProgramme.copy(id = 3)
        )
        coEvery { dao.getAllProgrammes() } returns programmes

        // Act
        val result = dao.getAllProgrammes()

        // Assert
        assertThat(result).hasSize(3)
    }

    @Test
    fun `getProgrammesByType_customProgrammes_returnsFilteredList`() = runTest {
        // Arrange
        val customProgrammes = listOf(
            mockProgramme.copy(id = 1, isCustom = true),
            mockProgramme.copy(id = 2, isCustom = true)
        )
        coEvery { dao.getProgrammesByType(true) } returns customProgrammes

        // Act
        val result = dao.getProgrammesByType(true)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result.all { it.isCustom }).isTrue()
    }

    @Test
    fun `deleteProgrammeById_validId_deletesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.deleteProgrammeById(1L) } just runs

        // Act
        dao.deleteProgrammeById(1L)

        // Assert
        coVerify(exactly = 1) { dao.deleteProgrammeById(1L) }
    }

    @Test
    fun `insertProgrammeWeek_validWeek_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertProgrammeWeek(any()) } returns 1L

        // Act
        val result = dao.insertProgrammeWeek(mockWeek)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertProgrammeWeek(mockWeek) }
    }

    @Test
    fun `insertProgrammeWeeks_multipleWeeks_insertsAll`() = runTest {
        // Arrange
        val weeks = listOf(mockWeek, mockWeek.copy(id = 2, weekNumber = 2))
        coEvery { dao.insertProgrammeWeeks(weeks) } just runs

        // Act
        dao.insertProgrammeWeeks(weeks)

        // Assert
        coVerify(exactly = 1) { dao.insertProgrammeWeeks(weeks) }
    }

    @Test
    fun `getWeeksForProgramme_hasWeeks_returnsSortedByWeekNumber`() = runTest {
        // Arrange
        val weeks = listOf(
            mockWeek,
            mockWeek.copy(id = 2, weekNumber = 2),
            mockWeek.copy(id = 3, weekNumber = 3)
        )
        coEvery { dao.getWeeksForProgramme(1L) } returns weeks

        // Act
        val result = dao.getWeeksForProgramme(1L)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].weekNumber).isEqualTo(1)
        assertThat(result[1].weekNumber).isEqualTo(2)
        assertThat(result[2].weekNumber).isEqualTo(3)
    }

    @Test
    fun `getWeekById_existingId_returnsWeek`() = runTest {
        // Arrange
        coEvery { dao.getWeekById(1L) } returns mockWeek

        // Act
        val result = dao.getWeekById(1L)

        // Assert
        assertThat(result).isEqualTo(mockWeek)
    }

    @Test
    fun `insertProgrammeWorkout_validWorkout_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertProgrammeWorkout(any()) } returns 1L

        // Act
        val result = dao.insertProgrammeWorkout(mockWorkout)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertProgrammeWorkout(mockWorkout) }
    }

    @Test
    fun `insertProgrammeWorkouts_multipleWorkouts_insertsAll`() = runTest {
        // Arrange
        val workouts = listOf(
            mockWorkout,
            mockWorkout.copy(id = 2, dayNumber = 2)
        )
        coEvery { dao.insertProgrammeWorkouts(workouts) } just runs

        // Act
        dao.insertProgrammeWorkouts(workouts)

        // Assert
        coVerify(exactly = 1) { dao.insertProgrammeWorkouts(workouts) }
    }

    @Test
    fun `getWorkoutsForWeek_hasWorkouts_returnsSortedByDayNumber`() = runTest {
        // Arrange
        val workouts = listOf(
            mockWorkout,
            mockWorkout.copy(id = 2, dayNumber = 2),
            mockWorkout.copy(id = 3, dayNumber = 3)
        )
        coEvery { dao.getWorkoutsForWeek(1L) } returns workouts

        // Act
        val result = dao.getWorkoutsForWeek(1L)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].dayNumber).isEqualTo(1)
        assertThat(result[1].dayNumber).isEqualTo(2)
        assertThat(result[2].dayNumber).isEqualTo(3)
    }

    @Test
    fun `getWorkoutById_existingId_returnsWorkout`() = runTest {
        // Arrange
        coEvery { dao.getWorkoutById(1L) } returns mockWorkout

        // Act
        val result = dao.getWorkoutById(1L)

        // Assert
        assertThat(result).isEqualTo(mockWorkout)
    }

    @Test
    fun `getAllWorkoutsForProgramme_hasWorkouts_returnsAllSorted`() = runTest {
        // Arrange
        val workouts = listOf(
            mockWorkout,
            mockWorkout.copy(id = 2),
            mockWorkout.copy(id = 3)
        )
        coEvery { dao.getAllWorkoutsForProgramme(1L) } returns workouts

        // Act
        val result = dao.getAllWorkoutsForProgramme(1L)

        // Assert
        assertThat(result).hasSize(3)
    }

    @Test
    fun `insertOrUpdateProgress_newProgress_insertsSuccessfully`() = runTest {
        // Arrange
        val progress = ProgrammeProgress(
            id = 1L,
            programmeId = 1L,
            currentWeek = 1,
            currentDay = 1,
            completedWorkouts = 0,
            totalWorkouts = 24,
            lastWorkoutDate = null,
            adherencePercentage = 0f,
            strengthProgress = null
        )
        coEvery { dao.insertOrUpdateProgress(progress) } returns 1L

        // Act
        val result = dao.insertOrUpdateProgress(progress)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertOrUpdateProgress(progress) }
    }
}