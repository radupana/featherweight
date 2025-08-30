package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.service.OneRMService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class PersonalRecordRepositoryTest {
    private lateinit var repository: PersonalRecordRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var oneRMService: OneRMService
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        db = mockk(relaxed = true)
        personalRecordDao = mockk(relaxed = true)
        oneRMService = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)

        every { db.personalRecordDao() } returns personalRecordDao
        every { db.exerciseDao() } returns exerciseDao

        repository = PersonalRecordRepository(db, personalRecordDao, oneRMService)
    }

    @Test
    fun `recordPR inserts personal record correctly`() =
        runTest {
            // Arrange
            val pr =
                PersonalRecord(
                    id = 0,
                    exerciseVariationId = 1L,
                    weight = 150f,
                    reps = 5,
                    rpe = 9f,
                    recordDate = LocalDateTime.now(),
                    previousWeight = 140f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(7),
                    improvementPercentage = 7.14f,
                    recordType = PRType.WEIGHT,
                    volume = 750f,
                    estimated1RM = 175f,
                    notes = "New PR!",
                    workoutId = 100L,
                )
            val expectedId = 42L

            coEvery { personalRecordDao.insertPersonalRecord(pr) } returns expectedId

            // Act
            val result = repository.recordPR(pr)

            // Assert
            assertEquals(expectedId, result)
            coVerify(exactly = 1) { personalRecordDao.insertPersonalRecord(pr) }
        }

    @Test
    fun `getPRsForExercise returns recent PRs for specific exercise`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = exerciseVariationId,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = 140f,
                        previousReps = 5,
                        previousDate = LocalDateTime.now().minusDays(7),
                        improvementPercentage = 7.14f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                    PersonalRecord(
                        id = 2,
                        exerciseVariationId = exerciseVariationId,
                        weight = 135f,
                        reps = 5,
                        estimated1RM = 157f,
                        recordDate = LocalDateTime.now().minusDays(3),
                        previousWeight = 130f,
                        previousReps = 5,
                        previousDate = LocalDateTime.now().minusDays(10),
                        improvementPercentage = 10f,
                        recordType = PRType.ESTIMATED_1RM,
                        workoutId = 101L,
                    ),
                )

            coEvery { personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100) } returns expectedPRs

            // Act
            val result = repository.getPRsForExercise(exerciseVariationId)

            // Assert
            assertEquals(expectedPRs, result)
            coVerify(exactly = 1) { personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100) }
        }

    @Test
    fun `getRecentPRs returns recent PRs across all exercises`() =
        runTest {
            // Arrange
            val limit = 10
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = 1L,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                )

            coEvery { personalRecordDao.getRecentPRs(limit) } returns expectedPRs

            // Act
            val result = repository.getRecentPRs(limit)

            // Assert
            assertEquals(expectedPRs, result)
            coVerify(exactly = 1) { personalRecordDao.getRecentPRs(limit) }
        }

    @Test
    fun `getPRsForWorkout returns PRs for specific workout`() =
        runTest {
            // Arrange
            val workoutId = 100L
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = 1L,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = workoutId,
                    ),
                )

            coEvery { personalRecordDao.getPersonalRecordsForWorkout(workoutId) } returns expectedPRs

            // Act
            val result = repository.getPRsForWorkout(workoutId)

            // Assert
            assertEquals(expectedPRs, result)
            coVerify(exactly = 1) { personalRecordDao.getPersonalRecordsForWorkout(workoutId) }
        }

    @Test
    fun `getCurrentPR returns latest PR for exercise and type`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val prType = PRType.WEIGHT
            val expectedPR =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    weight = 150f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = 140f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(7),
                    improvementPercentage = 7.14f,
                    recordType = prType,
                    workoutId = 100L,
                )

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, prType) } returns expectedPR

            // Act
            val result = repository.getCurrentPR(exerciseVariationId, prType)

            // Assert
            assertEquals(expectedPR, result)
            coVerify(exactly = 1) { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, prType) }
        }

    @Test
    fun `getCurrentPR returns null when no PR exists`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val prType = PRType.WEIGHT

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, prType) } returns null

            // Act
            val result = repository.getCurrentPR(exerciseVariationId, prType)

            // Assert
            assertNull(result)
        }

    @Test
    fun `getPRHistory returns filtered history by type`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val prType = PRType.WEIGHT
            val allPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = exerciseVariationId,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                    PersonalRecord(
                        id = 2,
                        exerciseVariationId = exerciseVariationId,
                        weight = 140f,
                        reps = 3,
                        estimated1RM = 150f,
                        recordDate = LocalDateTime.now().minusDays(3),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.ESTIMATED_1RM,
                        workoutId = 101L,
                    ),
                )

            coEvery { personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100) } returns allPRs

            // Act
            val result = repository.getPRHistory(exerciseVariationId, prType)

            // Assert
            assertEquals(1, result.size)
            assertEquals(PRType.WEIGHT, result[0].recordType)
        }

    @Test
    fun `checkAndRecordPR detects and records weight PR`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val workoutId = 100L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 50,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 150f,
                    actualWeight = 150f,
                    actualRpe = 9f,
                    completedAt = LocalDateTime.now().toString(),
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = exerciseVariationId,
                    coreExerciseId = 1,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                )

            val currentWeightPR =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    weight = 140f,
                    reps = 5,
                    recordDate = LocalDateTime.now().minusDays(7),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    workoutId = 99L,
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns currentWeightPR
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null
            coEvery { oneRMService.calculateEstimated1RM(150f, 5) } returns 175f

            val prSlot = slot<PersonalRecord>()
            coEvery { personalRecordDao.insertPersonalRecord(capture(prSlot)) } returns 42L

            // Act
            val result = repository.checkAndRecordPR(exerciseVariationId, setLog, workoutId)

            // Assert
            assertNotNull(result)
            assertEquals(PRType.WEIGHT, result?.recordType)
            assertEquals(150f, result?.weight)
            assertEquals(5, result?.reps)
            assertEquals(140f, result?.previousWeight)
            assertEquals(5, result?.previousReps)
            assertTrue(result?.improvementPercentage ?: 0f > 0)
            coVerify(exactly = 1) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkAndRecordPR detects and records estimated 1RM PR when weight not PR`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val workoutId = 100L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 50,
                    setOrder = 1,
                    targetReps = 3,
                    actualReps = 3,
                    targetWeight = 160f,
                    actualWeight = 160f,
                    actualRpe = 9f,
                    completedAt = LocalDateTime.now().toString(),
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = exerciseVariationId,
                    coreExerciseId = 1,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                )

            // Current Weight PR is higher, so not broken
            val currentWeightPR =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    weight = 165f, // Higher weight
                    reps = 3,
                    recordDate = LocalDateTime.now().minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    workoutId = 98L,
                )

            // Current 1RM PR is lower than new calculated
            val current1RMPR =
                PersonalRecord(
                    id = 2,
                    exerciseVariationId = exerciseVariationId,
                    weight = 150f,
                    reps = 5,
                    estimated1RM = 175f, // Lower than new 1RM
                    recordDate = LocalDateTime.now().minusDays(7),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.ESTIMATED_1RM,
                    workoutId = 99L,
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns currentWeightPR
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns current1RMPR
            coEvery { oneRMService.calculateEstimated1RM(160f, 3) } returns 185f // New higher 1RM

            coEvery { personalRecordDao.insertPersonalRecord(any()) } returns 42L

            // Act
            val result = repository.checkAndRecordPR(exerciseVariationId, setLog, workoutId)

            // Assert
            assertNotNull(result)
            assertEquals(PRType.ESTIMATED_1RM, result?.recordType)
            assertEquals(160f, result?.weight)
            assertEquals(3, result?.reps)
            assertEquals(185f, result?.estimated1RM)
            assertTrue((result?.estimated1RM ?: 0f) > (current1RMPR.estimated1RM ?: 0f))
            coVerify(exactly = 1) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkAndRecordPR returns null when no PR detected`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val workoutId = 100L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 50,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    actualRpe = 7f,
                    completedAt = LocalDateTime.now().toString(),
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = exerciseVariationId,
                    coreExerciseId = 1,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                )

            val currentWeightPR =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    weight = 150f,
                    reps = 5,
                    recordDate = LocalDateTime.now().minusDays(7),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    workoutId = 99L,
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns currentWeightPR
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns currentWeightPR.copy(estimated1RM = 200f)
            coEvery { oneRMService.calculateEstimated1RM(100f, 5) } returns 116f

            // Act
            val result = repository.checkAndRecordPR(exerciseVariationId, setLog, workoutId)

            // Assert
            assertNull(result)
            coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkAndRecordPR returns null when exercise does not exist`() =
        runTest {
            // Arrange
            val exerciseVariationId = 999L
            val workoutId = 100L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 50,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 150f,
                    actualWeight = 150f,
                    actualRpe = 9f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns null

            // Act
            val result = repository.checkAndRecordPR(exerciseVariationId, setLog, workoutId)

            // Assert
            assertNull(result)
            coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `getPRSummary returns correct summary statistics`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val weightPR =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    weight = 150f,
                    reps = 5,
                    volume = 750f,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    workoutId = 100L,
                )
            val estimated1RMPR =
                PersonalRecord(
                    id = 2,
                    exerciseVariationId = exerciseVariationId,
                    weight = 145f,
                    reps = 3,
                    estimated1RM = 175f,
                    recordDate = LocalDateTime.now().minusDays(1),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.ESTIMATED_1RM,
                    workoutId = 103L,
                )

            val allPRs = listOf(weightPR, estimated1RMPR)

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns weightPR
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns estimated1RMPR
            coEvery { personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 100) } returns allPRs

            // Act
            val result = repository.getPRSummary(exerciseVariationId)

            // Assert
            assertEquals(exerciseVariationId, result.exerciseVariationId)
            assertEquals(150f, result.maxWeight)
            assertEquals(5, result.maxReps) // From weight PR
            assertEquals(750f, result.maxVolume) // Calculated from weight PR
            assertEquals(175f, result.estimated1RM)
            assertEquals(2, result.totalPRs)
            assertNotNull(result.lastPRDate)
            assertEquals(2, result.recentPRs.size)
        }

    @Test
    fun `getPRTimelineFlow returns flow of PR history`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = exerciseVariationId,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                )

            coEvery { personalRecordDao.getPRHistoryForExercise(exerciseVariationId) } returns flowOf(expectedPRs)

            // Act
            val resultFlow = repository.getPRTimelineFlow(exerciseVariationId)

            // Assert
            resultFlow.collect { result ->
                assertEquals(expectedPRs, result)
            }
        }

    @Test
    fun `deletePR deletes personal record`() =
        runTest {
            // Arrange
            val prId = 42L

            coEvery { personalRecordDao.deletePR(prId) } returns Unit

            // Act
            repository.deletePR(prId)

            // Assert
            coVerify(exactly = 1) { personalRecordDao.deletePR(prId) }
        }

    @Test
    fun `getPRsInDateRange returns PRs within specified dates`() =
        runTest {
            // Arrange
            val startDate = LocalDateTime.now().minusDays(30)
            val endDate = LocalDateTime.now()
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = 1L,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now().minusDays(7),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                )

            coEvery { personalRecordDao.getPersonalRecordsInDateRange(startDate, endDate) } returns expectedPRs

            // Act
            val result = repository.getPRsInDateRange(startDate, endDate)

            // Assert
            assertEquals(expectedPRs, result)
            coVerify(exactly = 1) { personalRecordDao.getPersonalRecordsInDateRange(startDate, endDate) }
        }

    @Test
    fun `getMostImpressivePRs returns PRs sorted by improvement percentage`() =
        runTest {
            // Arrange
            val limit = 5
            val pr1 =
                PersonalRecord(
                    id = 1,
                    exerciseVariationId = 1L,
                    weight = 150f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = 140f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(7),
                    improvementPercentage = 7.14f,
                    recordType = PRType.WEIGHT,
                    workoutId = 100L,
                )
            val pr2 =
                PersonalRecord(
                    id = 2,
                    exerciseVariationId = 2L,
                    weight = 140f,
                    reps = 5,
                    estimated1RM = 163f,
                    recordDate = LocalDateTime.now(),
                    previousWeight = 135f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(7),
                    improvementPercentage = 15f,
                    recordType = PRType.ESTIMATED_1RM,
                    workoutId = 101L,
                )

            coEvery { personalRecordDao.getRecentPRs(limit) } returns listOf(pr1, pr2)

            // Act
            val result = repository.getMostImpressivePRs(limit)

            // Assert
            assertEquals(2, result.size)
            assertEquals(pr2, result[0]) // Higher improvement percentage
            assertEquals(pr1, result[1])
        }

    @Test
    fun `hasAnyPRs returns true when PRs exist`() =
        runTest {
            // Arrange
            val prs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = 1L,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                )

            coEvery { personalRecordDao.getAllPersonalRecords() } returns prs

            // Act
            val result = repository.hasAnyPRs()

            // Assert
            assertTrue(result)
        }

    @Test
    fun `hasAnyPRs returns false when no PRs exist`() =
        runTest {
            // Arrange
            coEvery { personalRecordDao.getAllPersonalRecords() } returns emptyList()

            // Act
            val result = repository.hasAnyPRs()

            // Assert
            assertFalse(result)
        }

    @Test
    fun `getTotalPRCount returns correct count`() =
        runTest {
            // Arrange
            val prs =
                listOf(
                    PersonalRecord(
                        id = 1,
                        exerciseVariationId = 1L,
                        weight = 150f,
                        reps = 5,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        workoutId = 100L,
                    ),
                    PersonalRecord(
                        id = 2,
                        exerciseVariationId = 2L,
                        weight = 140f,
                        reps = 5,
                        estimated1RM = 163f,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.ESTIMATED_1RM,
                        workoutId = 101L,
                    ),
                )

            coEvery { personalRecordDao.getAllPersonalRecords() } returns prs

            // Act
            val result = repository.getTotalPRCount()

            // Assert
            assertEquals(2, result)
        }

    @Test
    fun `getPRCountForExercise returns correct count`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val expectedCount = 5

            coEvery { personalRecordDao.getPRCountForExercise(exerciseVariationId) } returns expectedCount

            // Act
            val result = repository.getPRCountForExercise(exerciseVariationId)

            // Assert
            assertEquals(expectedCount, result)
            coVerify(exactly = 1) { personalRecordDao.getPRCountForExercise(exerciseVariationId) }
        }
}
