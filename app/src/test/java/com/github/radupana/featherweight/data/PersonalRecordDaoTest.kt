package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class PersonalRecordDaoTest {
    private lateinit var dao: PersonalRecordDao
    private lateinit var mockPR: PersonalRecord
    private lateinit var testDate: LocalDateTime

    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        mockPR = PersonalRecord(
            id = 1L,
            exerciseVariationId = 1L,
            weight = 100f,
            reps = 1,
            rpe = 8f,
            recordDate = testDate,
            previousWeight = 90f,
            previousReps = 1,
            previousDate = testDate.minusDays(7),
            improvementPercentage = 11.1f,
            recordType = PRType.WEIGHT,
            volume = 100f,
            estimated1RM = 100f,
            notes = "Great lift",
            workoutId = 1L
        )
    }

    @Test
    fun `insertPersonalRecord_validRecord_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertPersonalRecord(any()) } returns 1L

        // Act
        val result = dao.insertPersonalRecord(mockPR)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertPersonalRecord(mockPR) }
    }

    @Test
    fun `getPRHistoryForExercise_hasRecords_returnsFlow`() = runTest {
        // Arrange
        val records = listOf(mockPR, mockPR.copy(id = 2, weight = 95f))
        val flow: Flow<List<PersonalRecord>> = flowOf(records)
        every { dao.getPRHistoryForExercise(1L) } returns flow

        // Act
        val result = dao.getPRHistoryForExercise(1L).first()

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].weight).isEqualTo(100f)
    }

    @Test
    fun `getRecentPRsForExercise_hasRecords_returnsLimitedList`() = runTest {
        // Arrange
        val records = listOf(mockPR)
        coEvery { dao.getRecentPRsForExercise(1L, 5) } returns records

        // Act
        val result = dao.getRecentPRsForExercise(1L, 5)

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(mockPR)
    }

    @Test
    fun `getRecentPRs_returnsRecentRecords`() = runTest {
        // Arrange
        val records = listOf(mockPR, mockPR.copy(id = 2, exerciseVariationId = 2L))
        coEvery { dao.getRecentPRs(10) } returns records

        // Act
        val result = dao.getRecentPRs(10)

        // Assert
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getPRsSince_validDate_returnsFilteredRecords`() = runTest {
        // Arrange
        val sinceDate = "2024-01-01"
        val records = listOf(mockPR)
        coEvery { dao.getPRsSince(sinceDate) } returns records

        // Act
        val result = dao.getPRsSince(sinceDate)

        // Assert
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getLatestPRForExerciseAndType_existingRecord_returnsPR`() = runTest {
        // Arrange
        coEvery { dao.getLatestPRForExerciseAndType(1L, PRType.WEIGHT) } returns mockPR

        // Act
        val result = dao.getLatestPRForExerciseAndType(1L, PRType.WEIGHT)

        // Assert
        assertThat(result).isEqualTo(mockPR)
    }

    @Test
    fun `getLatestPRForExerciseAndType_noRecord_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getLatestPRForExerciseAndType(999L, PRType.WEIGHT) } returns null

        // Act
        val result = dao.getLatestPRForExerciseAndType(999L, PRType.WEIGHT)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getMaxWeightForExercise_hasRecords_returnsMaxWeight`() = runTest {
        // Arrange
        coEvery { dao.getMaxWeightForExercise(1L) } returns 120f

        // Act
        val result = dao.getMaxWeightForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(120f)
    }

    @Test
    fun `getMaxWeightForExercise_noRecords_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getMaxWeightForExercise(999L) } returns null

        // Act
        val result = dao.getMaxWeightForExercise(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getMaxVolumeForExercise_hasRecords_returnsMaxVolume`() = runTest {
        // Arrange
        coEvery { dao.getMaxVolumeForExercise(1L) } returns 5000f

        // Act
        val result = dao.getMaxVolumeForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(5000f)
    }

    @Test
    fun `getMaxEstimated1RMForExercise_hasRecords_returnsMax1RM`() = runTest {
        // Arrange
        coEvery { dao.getMaxEstimated1RMForExercise(1L) } returns 110f

        // Act
        val result = dao.getMaxEstimated1RMForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(110f)
    }

    @Test
    fun `getPRCountForExercise_hasRecords_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getPRCountForExercise(1L) } returns 5

        // Act
        val result = dao.getPRCountForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `getPRCountSince_validDate_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getPRCountSince("2024-01-01") } returns 10

        // Act
        val result = dao.getPRCountSince("2024-01-01")

        // Assert
        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `deletePersonalRecord_validId_deletesRecord`() = runTest {
        // Arrange
        coEvery { dao.deletePersonalRecord(1L) } just runs

        // Act
        dao.deletePersonalRecord(1L)

        // Assert
        coVerify(exactly = 1) { dao.deletePersonalRecord(1L) }
    }

    @Test
    fun `getLatestRecordForExercise_hasRecord_returnsMostRecent`() = runTest {
        // Arrange
        coEvery { dao.getLatestRecordForExercise(1L) } returns mockPR

        // Act
        val result = dao.getLatestRecordForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(mockPR)
    }

    @Test
    fun `clearAllPersonalRecords_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.clearAllPersonalRecords() } just runs

        // Act
        dao.clearAllPersonalRecords()

        // Assert
        coVerify(exactly = 1) { dao.clearAllPersonalRecords() }
    }

    @Test
    fun `deleteAllPersonalRecords_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.deleteAllPersonalRecords() } just runs

        // Act
        dao.deleteAllPersonalRecords()

        // Assert
        coVerify(exactly = 1) { dao.deleteAllPersonalRecords() }
    }

    @Test
    fun `getAllPersonalRecords_returnsAllRecords`() = runTest {
        // Arrange
        val records = listOf(mockPR, mockPR.copy(id = 2), mockPR.copy(id = 3))
        coEvery { dao.getAllPersonalRecords() } returns records

        // Act
        val result = dao.getAllPersonalRecords()

        // Assert
        assertThat(result).hasSize(3)
    }

    @Test
    fun `getPersonalRecordsForDate_validDate_returnsRecords`() = runTest {
        // Arrange
        val records = listOf(mockPR)
        coEvery { dao.getPersonalRecordsForDate("2024-01-01") } returns records

        // Act
        val result = dao.getPersonalRecordsForDate("2024-01-01")

        // Assert
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getPersonalRecordsForWorkout_validWorkoutId_returnsRecords`() = runTest {
        // Arrange
        val records = listOf(mockPR, mockPR.copy(recordType = PRType.VOLUME))
        coEvery { dao.getPersonalRecordsForWorkout(1L) } returns records

        // Act
        val result = dao.getPersonalRecordsForWorkout(1L)

        // Assert
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getPRForExerciseInWorkout_existingPR_returnsRecord`() = runTest {
        // Arrange
        coEvery { dao.getPRForExerciseInWorkout(1L, 1L, PRType.WEIGHT) } returns mockPR

        // Act
        val result = dao.getPRForExerciseInWorkout(1L, 1L, PRType.WEIGHT)

        // Assert
        assertThat(result).isEqualTo(mockPR)
    }

    @Test
    fun `deletePR_validId_deletesRecord`() = runTest {
        // Arrange
        coEvery { dao.deletePR(1L) } just runs

        // Act
        dao.deletePR(1L)

        // Assert
        coVerify(exactly = 1) { dao.deletePR(1L) }
    }

    @Test
    fun `getPersonalRecordsInDateRange_validRange_returnsFilteredRecords`() = runTest {
        // Arrange
        val startDate = testDate.minusDays(7)
        val endDate = testDate.plusDays(7)
        val records = listOf(mockPR)
        coEvery { dao.getPersonalRecordsInDateRange(startDate, endDate) } returns records

        // Act
        val result = dao.getPersonalRecordsInDateRange(startDate, endDate)

        // Assert
        assertThat(result).hasSize(1)
    }
}