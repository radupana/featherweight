package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class WorkoutDaoTest {
    private lateinit var dao: WorkoutDao
    private lateinit var mockWorkout: Workout
    private lateinit var testDate: LocalDateTime

    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        mockWorkout =
            Workout(
                id = 1L,
                name = "Test Workout",
                date = testDate,
                status = WorkoutStatus.NOT_STARTED,
                isProgrammeWorkout = false,
                programmeId = null,
                weekNumber = null,
                dayNumber = null,
                notes = "Test notes",
                notesUpdatedAt = null,
                programmeWorkoutName = null,
                durationSeconds = null,
                timerStartTime = null,
                timerElapsedSeconds = 0,
            )
    }

    @Test
    fun `insertWorkout_validWorkout_returnsId`() =
        runTest {
            // Arrange
            coEvery { dao.insertWorkout(any()) } returns 1L

            // Act
            val result = dao.insertWorkout(mockWorkout)

            // Assert
            assertThat(result).isEqualTo(1L)
            coVerify(exactly = 1) { dao.insertWorkout(mockWorkout) }
        }

    @Test
    fun `updateWorkout_existingWorkout_updatesSuccessfully`() =
        runTest {
            // Arrange
            coEvery { dao.updateWorkout(any()) } returns Unit

            // Act
            dao.updateWorkout(mockWorkout)

            // Assert
            coVerify(exactly = 1) { dao.updateWorkout(mockWorkout) }
        }

    @Test
    fun `getAllWorkouts_hasWorkouts_returnsListSortedByDateDesc`() =
        runTest {
            // Arrange
            val workouts =
                listOf(
                    mockWorkout.copy(id = 1, date = testDate.plusDays(2)),
                    mockWorkout.copy(id = 2, date = testDate.plusDays(1)),
                    mockWorkout.copy(id = 3, date = testDate),
                )
            coEvery { dao.getAllWorkouts() } returns workouts

            // Act
            val result = dao.getAllWorkouts()

            // Assert
            assertThat(result).hasSize(3)
            assertThat(result[0].id).isEqualTo(1)
            assertThat(result[1].id).isEqualTo(2)
            assertThat(result[2].id).isEqualTo(3)
        }

    @Test
    fun `getAllWorkouts_noWorkouts_returnsEmptyList`() =
        runTest {
            // Arrange
            coEvery { dao.getAllWorkouts() } returns emptyList()

            // Act
            val result = dao.getAllWorkouts()

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `getWorkoutById_existingId_returnsWorkout`() =
        runTest {
            // Arrange
            coEvery { dao.getWorkoutById(1L) } returns mockWorkout

            // Act
            val result = dao.getWorkoutById(1L)

            // Assert
            assertThat(result).isEqualTo(mockWorkout)
        }

    @Test
    fun `getWorkoutById_nonExistentId_returnsNull`() =
        runTest {
            // Arrange
            coEvery { dao.getWorkoutById(999L) } returns null

            // Act
            val result = dao.getWorkoutById(999L)

            // Assert
            assertThat(result).isNull()
        }

    @Test
    fun `getWorkoutsInDateRange_validRange_returnsFilteredWorkouts`() =
        runTest {
            // Arrange
            val startDate = testDate
            val endDate = testDate.plusDays(7)
            val workouts =
                listOf(
                    mockWorkout.copy(date = testDate.plusDays(1)),
                    mockWorkout.copy(date = testDate.plusDays(3)),
                )
            coEvery { dao.getWorkoutsInDateRange(startDate, endDate) } returns workouts

            // Act
            val result = dao.getWorkoutsInDateRange(startDate, endDate)

            // Assert
            assertThat(result).hasSize(2)
        }

    @Test
    fun `deleteWorkout_existingWorkout_deletesSuccessfully`() =
        runTest {
            // Arrange
            coEvery { dao.deleteWorkout(1L) } returns Unit

            // Act
            dao.deleteWorkout(1L)

            // Assert
            coVerify(exactly = 1) { dao.deleteWorkout(1L) }
        }

    @Test
    fun `deleteWorkoutsByProgramme_validProgrammeId_deletesAllRelatedWorkouts`() =
        runTest {
            // Arrange
            coEvery { dao.deleteWorkoutsByProgramme(1L) } returns Unit

            // Act
            dao.deleteWorkoutsByProgramme(1L)

            // Assert
            coVerify(exactly = 1) { dao.deleteWorkoutsByProgramme(1L) }
        }

    @Test
    fun `getInProgressWorkoutCountByProgramme_hasSomeInProgress_returnsCorrectCount`() =
        runTest {
            // Arrange
            coEvery { dao.getInProgressWorkoutCountByProgramme(1L) } returns 3

            // Act
            val result = dao.getInProgressWorkoutCountByProgramme(1L)

            // Assert
            assertThat(result).isEqualTo(3)
        }

    @Test
    fun `getWorkoutsByProgramme_validProgrammeId_returnsProgrammeWorkouts`() =
        runTest {
            // Arrange
            val programmeWorkouts =
                listOf(
                    mockWorkout.copy(programmeId = 1L, isProgrammeWorkout = true),
                )
            coEvery { dao.getWorkoutsByProgramme(1L) } returns programmeWorkouts

            // Act
            val result = dao.getWorkoutsByProgramme(1L)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].programmeId).isEqualTo(1L)
        }

    @Test
    fun `getProgrammeWorkoutsByWeek_validWeek_returnsWeekWorkouts`() =
        runTest {
            // Arrange
            val weekWorkouts =
                listOf(
                    mockWorkout.copy(weekNumber = 1, dayNumber = 1),
                    mockWorkout.copy(weekNumber = 1, dayNumber = 2),
                )
            coEvery { dao.getProgrammeWorkoutsByWeek(1L, 1) } returns weekWorkouts

            // Act
            val result = dao.getProgrammeWorkoutsByWeek(1L, 1)

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result[0].dayNumber).isEqualTo(1)
            assertThat(result[1].dayNumber).isEqualTo(2)
        }

    @Test
    fun `getFreestyleWorkouts_hasFreestyleWorkouts_returnsNonProgrammeWorkouts`() =
        runTest {
            // Arrange
            val freestyleWorkouts =
                listOf(
                    mockWorkout.copy(isProgrammeWorkout = false),
                )
            coEvery { dao.getFreestyleWorkouts() } returns freestyleWorkouts

            // Act
            val result = dao.getFreestyleWorkouts()

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].isProgrammeWorkout).isFalse()
        }

    @Test
    fun `getProgrammeWorkouts_hasProgrammeWorkouts_returnsProgrammeWorkoutsOnly`() =
        runTest {
            // Arrange
            val programmeWorkouts =
                listOf(
                    mockWorkout.copy(isProgrammeWorkout = true),
                )
            coEvery { dao.getProgrammeWorkouts() } returns programmeWorkouts

            // Act
            val result = dao.getProgrammeWorkouts()

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].isProgrammeWorkout).isTrue()
        }

    @Test
    fun `getCompletedProgrammeWorkoutCount_validProgramme_returnsCount`() =
        runTest {
            // Arrange
            coEvery { dao.getCompletedProgrammeWorkoutCount(1L) } returns 5

            // Act
            val result = dao.getCompletedProgrammeWorkoutCount(1L)

            // Assert
            assertThat(result).isEqualTo(5)
        }

    @Test
    fun `getTotalProgrammeWorkoutCount_validProgramme_returnsTotal`() =
        runTest {
            // Arrange
            coEvery { dao.getTotalProgrammeWorkoutCount(1L) } returns 12

            // Act
            val result = dao.getTotalProgrammeWorkoutCount(1L)

            // Assert
            assertThat(result).isEqualTo(12)
        }

    @Test
    fun `getCompletedWeeksForProgramme_hasCompletedWeeks_returnsWeekNumbers`() =
        runTest {
            // Arrange
            coEvery { dao.getCompletedWeeksForProgramme(1L) } returns listOf(1, 2, 3)

            // Act
            val result = dao.getCompletedWeeksForProgramme(1L)

            // Assert
            assertThat(result).containsExactly(1, 2, 3).inOrder()
        }

    @Test
    fun `getExerciseLogsByVariationId_validId_returnsLogs`() =
        runTest {
            // Arrange
            val logs =
                listOf(
                    ExerciseLog(id = 1, workoutId = 1, exerciseVariationId = 1, exerciseOrder = 1),
                    ExerciseLog(id = 2, workoutId = 2, exerciseVariationId = 1, exerciseOrder = 2),
                )
            coEvery { dao.getExerciseLogsByVariationId(1L) } returns logs

            // Act
            val result = dao.getExerciseLogsByVariationId(1L)

            // Assert
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getSetsForExercisesInRepRange_validRange_returnsFilteredSets`() =
        runTest {
            // Arrange
            val sets =
                listOf(
                    SetLog(id = 1, exerciseLogId = 1, setOrder = 1, actualReps = 8, isCompleted = true),
                    SetLog(id = 2, exerciseLogId = 1, setOrder = 2, actualReps = 10, isCompleted = true),
                )
            coEvery { dao.getSetsForExercisesInRepRange(listOf(1L), 5, 12) } returns sets

            // Act
            val result = dao.getSetsForExercisesInRepRange(listOf(1L), 5, 12)

            // Assert
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getRecentSetsForExercises_withLimit_returnsLimitedSets`() =
        runTest {
            // Arrange
            val sets =
                listOf(
                    SetLog(id = 1, exerciseLogId = 1, setOrder = 1),
                    SetLog(id = 2, exerciseLogId = 1, setOrder = 2),
                )
            coEvery { dao.getRecentSetsForExercises(listOf(1L), 10) } returns sets

            // Act
            val result = dao.getRecentSetsForExercises(listOf(1L), 10)

            // Assert
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getSetsForExercisesInWeightRange_validRange_returnsFilteredSets`() =
        runTest {
            // Arrange
            val sets =
                listOf(
                    SetLog(id = 1, exerciseLogId = 1, setOrder = 1, actualWeight = 80f, isCompleted = true),
                )
            coEvery { dao.getSetsForExercisesInWeightRange(listOf(1L), 70f, 90f) } returns sets

            // Act
            val result = dao.getSetsForExercisesInWeightRange(listOf(1L), 70f, 90f)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].actualWeight).isEqualTo(80f)
        }

    @Test
    fun `deleteAllWorkouts_called_deletesAll`() =
        runTest {
            // Arrange
            coEvery { dao.deleteAllWorkouts() } returns Unit

            // Act
            dao.deleteAllWorkouts()

            // Assert
            coVerify(exactly = 1) { dao.deleteAllWorkouts() }
        }

    @Test
    fun `getCompletedWorkoutsPaged_withPagination_returnsPagedResults`() =
        runTest {
            // Arrange
            val workouts =
                listOf(
                    mockWorkout.copy(status = WorkoutStatus.COMPLETED),
                )
            coEvery { dao.getCompletedWorkoutsPaged(10, 0) } returns workouts

            // Act
            val result = dao.getCompletedWorkoutsPaged(10, 0)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(WorkoutStatus.COMPLETED)
        }

    @Test
    fun `getWorkoutDateForMaxWeight_validParams_returnsDate`() =
        runTest {
            // Arrange
            val maxWeightDate = testDate.plusDays(5)
            coEvery {
                dao.getWorkoutDateForMaxWeight(1L, 100f, testDate, testDate.plusDays(30))
            } returns maxWeightDate

            // Act
            val result = dao.getWorkoutDateForMaxWeight(1L, 100f, testDate, testDate.plusDays(30))

            // Assert
            assertThat(result).isEqualTo(maxWeightDate)
        }

    @Test
    fun `getWorkoutCountsByDateRange_validRange_returnsCounts`() =
        runTest {
            // Arrange
            val counts =
                listOf(
                    WorkoutDateCount(testDate, 2),
                    WorkoutDateCount(testDate.plusDays(1), 1),
                )
            coEvery { dao.getWorkoutCountsByDateRange(testDate, testDate.plusDays(7)) } returns counts

            // Act
            val result = dao.getWorkoutCountsByDateRange(testDate, testDate.plusDays(7))

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result[0].count).isEqualTo(2)
        }

    @Test
    fun `getCompletedWorkoutCountsByDateRange_validRange_returnsCompletedCounts`() =
        runTest {
            // Arrange
            val counts =
                listOf(
                    WorkoutDateCount(testDate, 1),
                )
            coEvery {
                dao.getCompletedWorkoutCountsByDateRange(testDate, testDate.plusDays(7))
            } returns counts

            // Act
            val result = dao.getCompletedWorkoutCountsByDateRange(testDate, testDate.plusDays(7))

            // Assert
            assertThat(result).hasSize(1)
        }

    @Test
    fun `getWorkoutCountsByDateRangeWithStatus_validRange_returnsStatusCounts`() =
        runTest {
            // Arrange
            val counts =
                listOf(
                    WorkoutDateStatusCount(testDate, WorkoutStatus.COMPLETED, 1),
                    WorkoutDateStatusCount(testDate, WorkoutStatus.IN_PROGRESS, 2),
                )
            coEvery {
                dao.getWorkoutCountsByDateRangeWithStatus(testDate, testDate.plusDays(7))
            } returns counts

            // Act
            val result = dao.getWorkoutCountsByDateRangeWithStatus(testDate, testDate.plusDays(7))

            // Assert
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getWorkoutsByWeek_validWeek_returnsWeekWorkouts`() =
        runTest {
            // Arrange
            val startOfWeek = testDate
            val endOfWeek = testDate.plusDays(7)
            val workouts = listOf(mockWorkout)
            coEvery { dao.getWorkoutsByWeek(startOfWeek, endOfWeek) } returns workouts

            // Act
            val result = dao.getWorkoutsByWeek(startOfWeek, endOfWeek)

            // Assert
            assertThat(result).hasSize(1)
        }

    @Test
    fun `searchWorkouts_withQuery_returnsMatchingWorkouts`() =
        runTest {
            // Arrange
            val workouts =
                listOf(
                    mockWorkout.copy(name = "Leg Day"),
                )
            coEvery { dao.searchWorkouts("Leg", 100) } returns workouts

            // Act
            val result = dao.searchWorkouts("Leg", 100)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].name).contains("Leg")
        }

    @Test
    fun `getWorkoutsInDateRangePaged_validParams_returnsPagedWorkouts`() =
        runTest {
            // Arrange
            val workouts =
                listOf(
                    mockWorkout.copy(status = WorkoutStatus.COMPLETED),
                )
            coEvery {
                dao.getWorkoutsInDateRangePaged(
                    testDate,
                    testDate.plusDays(7),
                    WorkoutStatus.NOT_STARTED,
                    10,
                    0,
                )
            } returns workouts

            // Act
            val result =
                dao.getWorkoutsInDateRangePaged(
                    testDate,
                    testDate.plusDays(7),
                    WorkoutStatus.NOT_STARTED,
                    10,
                    0,
                )

            // Assert
            assertThat(result).hasSize(1)
        }

    @Test
    fun `getWorkoutCountInDateRange_validRange_returnsCount`() =
        runTest {
            // Arrange
            coEvery {
                dao.getWorkoutCountInDateRange(
                    testDate,
                    testDate.plusDays(7),
                    WorkoutStatus.NOT_STARTED,
                )
            } returns 5

            // Act
            val result =
                dao.getWorkoutCountInDateRange(
                    testDate,
                    testDate.plusDays(7),
                    WorkoutStatus.NOT_STARTED,
                )

            // Assert
            assertThat(result).isEqualTo(5)
        }

    @Test
    fun `getWorkoutsByWeek_onlyReturnsCompletedWorkouts`() =
        runTest {
            // Arrange
            val startOfWeek = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endOfWeek = LocalDateTime.of(2024, 1, 8, 0, 0)

            val completedWorkout = mockWorkout.copy(id = 1, status = WorkoutStatus.COMPLETED)

            // Should only return completed workouts based on the updated query
            coEvery { dao.getWorkoutsByWeek(startOfWeek, endOfWeek) } returns listOf(completedWorkout)

            // Act
            val result = dao.getWorkoutsByWeek(startOfWeek, endOfWeek)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(WorkoutStatus.COMPLETED)
            assertThat(result[0].id).isEqualTo(1)
        }

    @Test
    fun getWorkoutCountsByDateRange_onlyCountsCompletedWorkouts() =
        runTest {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 1, 31, 23, 59)

            // Should only count completed workouts based on the updated query
            val counts =
                listOf(
                    WorkoutDateCount(LocalDateTime.of(2024, 1, 5, 10, 0), 2),
                    WorkoutDateCount(LocalDateTime.of(2024, 1, 10, 10, 0), 1),
                )

            coEvery { dao.getWorkoutCountsByDateRange(startDate, endDate) } returns counts

            // Act
            val result = dao.getWorkoutCountsByDateRange(startDate, endDate)

            // Assert
            assertThat(result).hasSize(2)
            // Verify counts only include completed workouts
            assertThat(result[0].count).isEqualTo(2)
            assertThat(result[1].count).isEqualTo(1)
        }
}
