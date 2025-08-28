package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SetLogDaoTest {
    private lateinit var dao: SetLogDao
    private lateinit var mockSetLog: SetLog
    
    @Before
    fun setup() {
        dao = mockk()
        mockSetLog = SetLog(
            id = 1L,
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 10,
            actualReps = 10,
            targetWeight = 80f,
            actualWeight = 80f,
            actualRpe = null,
            suggestedWeight = null,
            suggestedReps = null,
            suggestionSource = null,
            suggestionConfidence = null,
            calculationDetails = null,
            tag = null,
            notes = null,
            isCompleted = false,
            completedAt = null
        )
    }

    @Test
    fun `insertSetLog_validSetLog_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertSetLog(any()) } returns 1L

        // Act
        val result = dao.insertSetLog(mockSetLog)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertSetLog(mockSetLog) }
    }

    @Test
    fun `insert_validSetLog_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insert(any()) } returns 1L

        // Act
        val result = dao.insert(mockSetLog)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insert(mockSetLog) }
    }

    @Test
    fun `getSetLogsForExercise_existingExercise_returnsSortedSets`() = runTest {
        // Arrange
        val sets = listOf(
            mockSetLog.copy(setOrder = 1),
            mockSetLog.copy(setOrder = 2),
            mockSetLog.copy(setOrder = 3)
        )
        coEvery { dao.getSetLogsForExercise(1L) } returns sets

        // Act
        val result = dao.getSetLogsForExercise(1L)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].setOrder).isEqualTo(1)
        assertThat(result[1].setOrder).isEqualTo(2)
        assertThat(result[2].setOrder).isEqualTo(3)
    }

    @Test
    fun `getSetLogsForExercise_noSets_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getSetLogsForExercise(999L) } returns emptyList()

        // Act
        val result = dao.getSetLogsForExercise(999L)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `markSetCompleted_completingSet_updatesCompletionStatus`() = runTest {
        // Arrange
        val completedAt = "2024-01-01T10:00:00"
        coEvery { dao.markSetCompleted(1L, true, completedAt) } returns Unit

        // Act
        dao.markSetCompleted(1L, true, completedAt)

        // Assert
        coVerify(exactly = 1) { dao.markSetCompleted(1L, true, completedAt) }
    }

    @Test
    fun `markSetCompleted_uncompletingSet_clearsCompletionStatus`() = runTest {
        // Arrange
        coEvery { dao.markSetCompleted(1L, false, null) } returns Unit

        // Act
        dao.markSetCompleted(1L, false, null)

        // Assert
        coVerify(exactly = 1) { dao.markSetCompleted(1L, false, null) }
    }

    @Test
    fun `updateSetLog_validSetLog_updatesSuccessfully`() = runTest {
        // Arrange
        val updatedSet = mockSetLog.copy(actualWeight = 85f)
        coEvery { dao.updateSetLog(updatedSet) } returns Unit

        // Act
        dao.updateSetLog(updatedSet)

        // Assert
        coVerify(exactly = 1) { dao.updateSetLog(updatedSet) }
    }

    @Test
    fun `update_validSetLog_updatesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.update(mockSetLog) } returns Unit

        // Act
        dao.update(mockSetLog)

        // Assert
        coVerify(exactly = 1) { dao.update(mockSetLog) }
    }

    @Test
    fun `deleteSetLog_existingId_deletesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.deleteSetLog(1L) } returns Unit

        // Act
        dao.deleteSetLog(1L)

        // Assert
        coVerify(exactly = 1) { dao.deleteSetLog(1L) }
    }

    @Test
    fun `deleteAllSetsForExercise_validExerciseId_deletesAllSets`() = runTest {
        // Arrange
        coEvery { dao.deleteAllSetsForExercise(1L) } returns Unit

        // Act
        dao.deleteAllSetsForExercise(1L)

        // Assert
        coVerify(exactly = 1) { dao.deleteAllSetsForExercise(1L) }
    }

    @Test
    fun `getSetsForExerciseSince_validParams_returnsFilteredSets`() = runTest {
        // Arrange
        val sinceDate = "2024-01-01"
        val sets = listOf(
            mockSetLog.copy(isCompleted = true),
            mockSetLog.copy(id = 2, isCompleted = true)
        )
        coEvery { dao.getSetsForExerciseSince(1L, sinceDate) } returns sets

        // Act
        val result = dao.getSetsForExerciseSince(1L, sinceDate)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result.all { it.isCompleted }).isTrue()
    }

    @Test
    fun `getAllCompletedExerciseVariationIds_hasCompletedSets_returnsUniqueIds`() = runTest {
        // Arrange
        val variationIds = listOf(1L, 2L, 3L)
        coEvery { dao.getAllCompletedExerciseVariationIds() } returns variationIds

        // Act
        val result = dao.getAllCompletedExerciseVariationIds()

        // Assert
        assertThat(result).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `getAllCompletedExerciseVariationIds_noCompletedSets_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getAllCompletedExerciseVariationIds() } returns emptyList()

        // Act
        val result = dao.getAllCompletedExerciseVariationIds()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `getWorkoutDateForSetLog_existingSetLog_returnsDate`() = runTest {
        // Arrange
        val expectedDate = "2024-01-15"
        coEvery { dao.getWorkoutDateForSetLog(1L) } returns expectedDate

        // Act
        val result = dao.getWorkoutDateForSetLog(1L)

        // Assert
        assertThat(result).isEqualTo(expectedDate)
    }

    @Test
    fun `getWorkoutDateForSetLog_nonExistentSetLog_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getWorkoutDateForSetLog(999L) } returns null

        // Act
        val result = dao.getWorkoutDateForSetLog(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getWorkoutIdForSetLog_existingSetLog_returnsWorkoutId`() = runTest {
        // Arrange
        coEvery { dao.getWorkoutIdForSetLog(1L) } returns 5L

        // Act
        val result = dao.getWorkoutIdForSetLog(1L)

        // Assert
        assertThat(result).isEqualTo(5L)
    }

    @Test
    fun `getWorkoutIdForSetLog_nonExistentSetLog_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getWorkoutIdForSetLog(999L) } returns null

        // Act
        val result = dao.getWorkoutIdForSetLog(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getMaxWeightForExerciseBefore_hasCompletedSets_returnsMaxWeight`() = runTest {
        // Arrange
        coEvery { dao.getMaxWeightForExerciseBefore(1L, "2024-01-15") } returns 100f

        // Act
        val result = dao.getMaxWeightForExerciseBefore(1L, "2024-01-15")

        // Assert
        assertThat(result).isEqualTo(100f)
    }

    @Test
    fun `getMaxWeightForExerciseBefore_noSetsBeforeDate_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getMaxWeightForExerciseBefore(1L, "2024-01-01") } returns null

        // Act
        val result = dao.getMaxWeightForExerciseBefore(1L, "2024-01-01")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `deleteAllSetLogs_called_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.deleteAllSetLogs() } returns Unit

        // Act
        dao.deleteAllSetLogs()

        // Assert
        coVerify(exactly = 1) { dao.deleteAllSetLogs() }
    }

    @Test
    fun `getMaxWeightForExerciseInDateRange_hasWeightsInRange_returnsMaxWeight`() = runTest {
        // Arrange
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        coEvery { 
            dao.getMaxWeightForExerciseInDateRange(1L, startDate, endDate) 
        } returns 120f

        // Act
        val result = dao.getMaxWeightForExerciseInDateRange(1L, startDate, endDate)

        // Assert
        assertThat(result).isEqualTo(120f)
    }

    @Test
    fun `getMaxWeightForExerciseInDateRange_noWeightsInRange_returnsNull`() = runTest {
        // Arrange
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        coEvery { 
            dao.getMaxWeightForExerciseInDateRange(1L, startDate, endDate) 
        } returns null

        // Act
        val result = dao.getMaxWeightForExerciseInDateRange(1L, startDate, endDate)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getMaxWeightForExerciseInDateRange_onlyZeroWeights_returnsNull`() = runTest {
        // Arrange
        coEvery { 
            dao.getMaxWeightForExerciseInDateRange(1L, "2024-01-01", "2024-01-31") 
        } returns null

        // Act
        val result = dao.getMaxWeightForExerciseInDateRange(1L, "2024-01-01", "2024-01-31")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getLastCompletedSetForExercise_hasCompletedSets_returnsLastSet`() = runTest {
        // Arrange
        val expectedSet = SetLog(
            id = 5L,
            exerciseLogId = 2L,
            setOrder = 3,
            targetReps = 8,
            actualReps = 8,
            targetWeight = 100f,
            actualWeight = 100f,
            actualRpe = 8f,
            suggestedWeight = null,
            suggestedReps = null,
            suggestionSource = null,
            suggestionConfidence = null,
            calculationDetails = null,
            tag = null,
            notes = null,
            isCompleted = true,
            completedAt = "2024-01-20T10:30:00"
        )
        coEvery { dao.getLastCompletedSetForExercise(1L) } returns expectedSet

        // Act
        val result = dao.getLastCompletedSetForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(expectedSet)
        assertThat(result?.actualWeight).isEqualTo(100f)
        assertThat(result?.actualReps).isEqualTo(8)
    }

    @Test
    fun `getLastCompletedSetForExercise_noCompletedSets_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getLastCompletedSetForExercise(1L) } returns null

        // Act
        val result = dao.getLastCompletedSetForExercise(1L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getLastCompletedSetForExercise_nonExistentExercise_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getLastCompletedSetForExercise(999L) } returns null

        // Act
        val result = dao.getLastCompletedSetForExercise(999L)

        // Assert
        assertThat(result).isNull()
    }
}
