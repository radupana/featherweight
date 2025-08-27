package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ExerciseLogDaoTest {
    private lateinit var dao: ExerciseLogDao
    private lateinit var mockExerciseLog: ExerciseLog
    private lateinit var testDate: LocalDateTime
    
    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        mockExerciseLog = ExerciseLog(
            id = 1L,
            workoutId = 1L,
            exerciseVariationId = 1L,
            exerciseOrder = 1,
            supersetGroup = null,
            notes = "Test notes",
            originalVariationId = null,
            isSwapped = false
        )
    }

    @Test
    fun `insertExerciseLog_validLog_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertExerciseLog(any()) } returns 1L

        // Act
        val result = dao.insertExerciseLog(mockExerciseLog)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertExerciseLog(mockExerciseLog) }
    }

    @Test
    fun `insert_validLog_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insert(any()) } returns 1L

        // Act
        val result = dao.insert(mockExerciseLog)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insert(mockExerciseLog) }
    }

    @Test
    fun `getExerciseLogsForWorkout_existingWorkout_returnsSortedLogs`() = runTest {
        // Arrange
        val logs = listOf(
            mockExerciseLog.copy(exerciseOrder = 1),
            mockExerciseLog.copy(id = 2, exerciseOrder = 2),
            mockExerciseLog.copy(id = 3, exerciseOrder = 3)
        )
        coEvery { dao.getExerciseLogsForWorkout(1L) } returns logs

        // Act
        val result = dao.getExerciseLogsForWorkout(1L)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].exerciseOrder).isEqualTo(1)
        assertThat(result[1].exerciseOrder).isEqualTo(2)
        assertThat(result[2].exerciseOrder).isEqualTo(3)
    }

    @Test
    fun `getExerciseLogsForWorkout_noLogs_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getExerciseLogsForWorkout(999L) } returns emptyList()

        // Act
        val result = dao.getExerciseLogsForWorkout(999L)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `deleteExerciseLog_existingId_deletesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.deleteExerciseLog(1L) } returns Unit

        // Act
        dao.deleteExerciseLog(1L)

        // Assert
        coVerify(exactly = 1) { dao.deleteExerciseLog(1L) }
    }

    @Test
    fun `getExerciseUsageCount_usedExercise_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getExerciseUsageCount(1L) } returns 10

        // Act
        val result = dao.getExerciseUsageCount(1L)

        // Assert
        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `getExerciseUsageCount_unusedExercise_returnsZero`() = runTest {
        // Arrange
        coEvery { dao.getExerciseUsageCount(999L) } returns 0

        // Act
        val result = dao.getExerciseUsageCount(999L)

        // Assert
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `updateExerciseOrder_validParams_updatesOrder`() = runTest {
        // Arrange
        coEvery { dao.updateExerciseOrder(1L, 3) } returns Unit

        // Act
        dao.updateExerciseOrder(1L, 3)

        // Assert
        coVerify(exactly = 1) { dao.updateExerciseOrder(1L, 3) }
    }

    @Test
    fun `getExerciseLogsWithNames_validWorkout_returnsLogsWithNames`() = runTest {
        // Arrange
        val logsWithNames = listOf(
            ExerciseLogWithName(
                exerciseLog = mockExerciseLog.copy(notes = null),
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
    fun `getExerciseLogWithName_existingLog_returnsLogWithName`() = runTest {
        // Arrange
        val logWithName = ExerciseLogWithName(
            exerciseLog = mockExerciseLog.copy(notes = null),
            exerciseName = "Barbell Squat"
        )
        coEvery { dao.getExerciseLogWithName(1L) } returns logWithName

        // Act
        val result = dao.getExerciseLogWithName(1L)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result?.exerciseName).isEqualTo("Barbell Squat")
    }

    @Test
    fun `getExerciseLogWithName_nonExistentLog_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getExerciseLogWithName(999L) } returns null

        // Act
        val result = dao.getExerciseLogWithName(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getExerciseLogsWithNamesFlow_validWorkout_emitsLogsWithNames`() = runTest {
        // Arrange
        val logsWithNames = listOf(
            ExerciseLogWithName(
                exerciseLog = mockExerciseLog.copy(notes = null),
                exerciseName = "Dumbbell Curl"
            )
        )
        val flow: Flow<List<ExerciseLogWithName>> = flowOf(logsWithNames)
        every { dao.getExerciseLogsWithNamesFlow(1L) } returns flow

        // Act
        val result = dao.getExerciseLogsWithNamesFlow(1L).first()

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseName).isEqualTo("Dumbbell Curl")
    }

    @Test
    fun `update_validLog_updatesSuccessfully`() = runTest {
        // Arrange
        val updatedLog = mockExerciseLog.copy(notes = "Updated notes")
        coEvery { dao.update(updatedLog) } returns Unit

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
    fun `getExerciseLogsInDateRange_validRange_returnsLogsInRange`() = runTest {
        // Arrange
        val startDate = testDate
        val endDate = testDate.plusDays(7)
        val logs = listOf(mockExerciseLog, mockExerciseLog.copy(id = 2))
        coEvery { 
            dao.getExerciseLogsInDateRange(1L, startDate, endDate) 
        } returns logs

        // Act
        val result = dao.getExerciseLogsInDateRange(1L, startDate, endDate)

        // Assert
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getTotalSessionsForExercise_hasCompletedSessions_returnsCount`() = runTest {
        // Arrange
        coEvery { dao.getTotalSessionsForExercise(1L) } returns 5

        // Act
        val result = dao.getTotalSessionsForExercise(1L)

        // Assert
        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `getTotalSessionsForExercise_noCompletedSessions_returnsZero`() = runTest {
        // Arrange
        coEvery { dao.getTotalSessionsForExercise(999L) } returns 0

        // Act
        val result = dao.getTotalSessionsForExercise(999L)

        // Assert
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `getAllUniqueExerciseVariationIds_hasExercises_returnsUniqueIds`() = runTest {
        // Arrange
        val ids = listOf(1L, 2L, 3L, 4L)
        coEvery { dao.getAllUniqueExerciseVariationIds() } returns ids

        // Act
        val result = dao.getAllUniqueExerciseVariationIds()

        // Assert
        assertThat(result).containsExactly(1L, 2L, 3L, 4L).inOrder()
    }

    @Test
    fun `getAllUniqueExerciseVariationIds_noExercises_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getAllUniqueExerciseVariationIds() } returns emptyList()

        // Act
        val result = dao.getAllUniqueExerciseVariationIds()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `deleteAllExerciseLogs_called_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.deleteAllExerciseLogs() } returns Unit

        // Act
        dao.deleteAllExerciseLogs()

        // Assert
        coVerify(exactly = 1) { dao.deleteAllExerciseLogs() }
    }

    @Test
    fun `getDistinctWorkoutsForExercise_validRange_returnsDistinctWorkouts`() = runTest {
        // Arrange
        val startDate = testDate
        val endDate = testDate.plusDays(30)
        val workouts = listOf(
            Workout(
                id = 1L,
                name = "Workout 1",
                date = testDate.plusDays(1),
                status = WorkoutStatus.COMPLETED
            ),
            Workout(
                id = 2L,
                name = "Workout 2",
                date = testDate.plusDays(5),
                status = WorkoutStatus.COMPLETED
            )
        )
        coEvery { 
            dao.getDistinctWorkoutsForExercise(1L, startDate, endDate) 
        } returns workouts

        // Act
        val result = dao.getDistinctWorkoutsForExercise(1L, startDate, endDate)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Workout 1")
        assertThat(result[1].name).isEqualTo("Workout 2")
    }

    @Test
    fun `getExerciseVariationUsageStatistics_hasUsage_returnsStatisticsOrderedByCount`() = runTest {
        // Arrange
        val statistics = listOf(
            ExerciseUsageStatistic(exerciseVariationId = 1L, count = 50),
            ExerciseUsageStatistic(exerciseVariationId = 2L, count = 30),
            ExerciseUsageStatistic(exerciseVariationId = 3L, count = 20)
        )
        coEvery { dao.getExerciseVariationUsageStatistics() } returns statistics

        // Act
        val result = dao.getExerciseVariationUsageStatistics()

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].count).isEqualTo(50)
        assertThat(result[1].count).isEqualTo(30)
        assertThat(result[2].count).isEqualTo(20)
    }

    @Test
    fun `getExerciseVariationUsageStatistics_noUsage_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getExerciseVariationUsageStatistics() } returns emptyList()

        // Act
        val result = dao.getExerciseVariationUsageStatistics()

        // Assert
        assertThat(result).isEmpty()
    }
}