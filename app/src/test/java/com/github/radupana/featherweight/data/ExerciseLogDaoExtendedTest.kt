package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ExerciseLogDaoExtendedTest {
    private lateinit var dao: ExerciseLogDao
    private lateinit var mockExerciseLog: ExerciseLog
    
    @Before
    fun setup() {
        dao = mockk<ExerciseLogDao>()
        
        mockExerciseLog = ExerciseLog(
            id = 1L,
            workoutId = 1L,
            exerciseVariationId = 1L,
            exerciseOrder = 1,
            notes = "Test notes"
        )
    }
    
    @Test
    fun `getExerciseLogsWithNames_returnsLogsWithNames`() = runTest {
        // Arrange
        val logsWithNames = listOf(
            ExerciseLogWithName(
                exerciseLog = mockExerciseLog,
                exerciseName = "Barbell Bench Press"
            )
        )
        coEvery { dao.getExerciseLogsWithNames(1L) } returns logsWithNames
        
        // Act
        val result = dao.getExerciseLogsWithNames(1L)
        
        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseName).isEqualTo("Barbell Bench Press")
    }
    
    @Test
    fun `getExerciseLogWithName_returnsLogWithName`() = runTest {
        // Arrange
        val logWithName = ExerciseLogWithName(
            exerciseLog = mockExerciseLog,
            exerciseName = "Deadlift"
        )
        coEvery { dao.getExerciseLogWithName(1L) } returns logWithName
        
        // Act
        val result = dao.getExerciseLogWithName(1L)
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result?.exerciseName).isEqualTo("Deadlift")
    }
    
    @Test
    fun `getExerciseLogsWithNamesFlow_emitsLogsWithNames`() = runTest {
        // Arrange
        val logsWithNames = listOf(
            ExerciseLogWithName(
                exerciseLog = mockExerciseLog.copy(notes = "Flow test"),
                exerciseName = "Squat"
            )
        )
        every { dao.getExerciseLogsWithNamesFlow(1L) } returns flowOf(logsWithNames)
        
        // Act
        val result = dao.getExerciseLogsWithNamesFlow(1L).first()
        
        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseName).isEqualTo("Squat")
    }
    
    @Test
    fun `updateExerciseOrder_multipleUpdates_updatesAll`() = runTest {
        // Arrange
        coEvery { dao.updateExerciseOrder(any(), any()) } just runs
        
        // Act
        dao.updateExerciseOrder(1L, 2)
        dao.updateExerciseOrder(2L, 1)
        dao.updateExerciseOrder(3L, 3)
        
        // Assert
        coVerify(exactly = 1) { dao.updateExerciseOrder(1L, 2) }
        coVerify(exactly = 1) { dao.updateExerciseOrder(2L, 1) }
        coVerify(exactly = 1) { dao.updateExerciseOrder(3L, 3) }
    }
    
    @Test
    fun `getExerciseUsageCount_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getExerciseUsageCount(1L) } returns 25
        
        // Act
        val result = dao.getExerciseUsageCount(1L)
        
        // Assert
        assertThat(result).isEqualTo(25)
    }
    
    @Test
    fun `getExerciseUsageCount_noUsage_returnsZero`() = runTest {
        // Arrange
        coEvery { dao.getExerciseUsageCount(999L) } returns 0
        
        // Act
        val result = dao.getExerciseUsageCount(999L)
        
        // Assert
        assertThat(result).isEqualTo(0)
    }
    
    @Test
    fun `update_validLog_updatesSuccessfully`() = runTest {
        // Arrange
        val updatedLog = mockExerciseLog.copy(notes = "Updated notes")
        coEvery { dao.update(updatedLog) } just runs
        
        // Act
        dao.update(updatedLog)
        
        // Assert
        coVerify(exactly = 1) { dao.update(updatedLog) }
    }
    
    @Test
    fun `getExerciseLogById_existingId_returnsLog`() = runTest {
        // Arrange
        coEvery { dao.getExerciseLogById(1L) } returns mockExerciseLog
        
        // Act
        val result = dao.getExerciseLogById(1L)
        
        // Assert
        assertThat(result).isEqualTo(mockExerciseLog)
    }
    
    @Test
    fun `getExerciseLogById_nonExistentId_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getExerciseLogById(999L) } returns null
        
        // Act
        val result = dao.getExerciseLogById(999L)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `getExerciseLogsInDateRange_returnsLogsInRange`() = runTest {
        // Arrange
        val startDate = java.time.LocalDateTime.now().minusDays(7)
        val endDate = java.time.LocalDateTime.now()
        val logs = listOf(mockExerciseLog, mockExerciseLog.copy(id = 2L))
        coEvery { dao.getExerciseLogsInDateRange(1L, startDate, endDate) } returns logs
        
        // Act
        val result = dao.getExerciseLogsInDateRange(1L, startDate, endDate)
        
        // Assert
        assertThat(result).hasSize(2)
    }
    
    @Test
    fun `getTotalSessionsForExercise_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getTotalSessionsForExercise(1L) } returns 15
        
        // Act
        val result = dao.getTotalSessionsForExercise(1L)
        
        // Assert
        assertThat(result).isEqualTo(15)
    }
    
    @Test
    fun `getAllUniqueExerciseVariationIds_returnsDistinctIds`() = runTest {
        // Arrange
        val ids = listOf(1L, 2L, 3L, 5L, 8L)
        coEvery { dao.getAllUniqueExerciseVariationIds() } returns ids
        
        // Act
        val result = dao.getAllUniqueExerciseVariationIds()
        
        // Assert
        assertThat(result).hasSize(5)
        assertThat(result).containsExactly(1L, 2L, 3L, 5L, 8L)
    }
    
    @Test
    fun `deleteAllExerciseLogs_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.deleteAllExerciseLogs() } just runs
        
        // Act
        dao.deleteAllExerciseLogs()
        
        // Assert
        coVerify(exactly = 1) { dao.deleteAllExerciseLogs() }
    }
    
    @Test
    fun `getExerciseVariationUsageStatistics_returnsStats`() = runTest {
        // Arrange
        val stats = listOf(
            ExerciseUsageStatistic(exerciseVariationId = 1L, count = 50),
            ExerciseUsageStatistic(exerciseVariationId = 2L, count = 30),
            ExerciseUsageStatistic(exerciseVariationId = 3L, count = 20)
        )
        coEvery { dao.getExerciseVariationUsageStatistics() } returns stats
        
        // Act
        val result = dao.getExerciseVariationUsageStatistics()
        
        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].count).isEqualTo(50)
        assertThat(result[1].count).isEqualTo(30)
        assertThat(result[2].count).isEqualTo(20)
    }
}