package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.service.PRDetectionService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class PersonalRecordRepositoryTest {
    private lateinit var repository: PersonalRecordRepository
    private lateinit var database: FeatherweightDatabase
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var prDetectionService: PRDetectionService
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
        personalRecordDao = mockk()
        setLogDao = mockk()
        exerciseVariationDao = mockk()
        prDetectionService = mockk()

        // Setup database to return DAOs
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.setLogDao() } returns setLogDao
        every { database.exerciseVariationDao() } returns exerciseVariationDao

        // Create repository with mocked dependencies
        repository = PersonalRecordRepository(application, testDispatcher, database, prDetectionService)
    }

    @Test
    fun `getRecentPRs should return recent PRs from dao`() =
        runTest(testDispatcher) {
            // Given
            val limit = 10
            val expectedPRs =
                listOf(
                    createPersonalRecord("1", PRType.WEIGHT),
                    createPersonalRecord("2", PRType.ESTIMATED_1RM),
                )
            coEvery { personalRecordDao.getRecentPRs(limit) } returns expectedPRs

            // When
            val result = repository.getRecentPRs(limit)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(expectedPRs)
            coVerify(exactly = 1) { personalRecordDao.getRecentPRs(limit) }
        }

    @Test
    fun `getRecentPRs should use default limit when not specified`() =
        runTest(testDispatcher) {
            // Given
            val expectedPRs = listOf(createPersonalRecord("1", PRType.WEIGHT))
            coEvery { personalRecordDao.getRecentPRs(10) } returns expectedPRs

            // When
            val result = repository.getRecentPRs()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(expectedPRs)
            coVerify(exactly = 1) { personalRecordDao.getRecentPRs(10) }
        }

    @Test
    fun `getPersonalRecordsForWorkout should return PRs for specific workout`() =
        runTest(testDispatcher) {
            // Given
            val workoutId = "123"
            val expectedPRs =
                listOf(
                    createPersonalRecord("1", PRType.WEIGHT, workoutId = workoutId),
                    createPersonalRecord("2", PRType.ESTIMATED_1RM, workoutId = workoutId),
                )
            coEvery { personalRecordDao.getPersonalRecordsForWorkout(workoutId) } returns expectedPRs

            // When
            val result = repository.getPersonalRecordsForWorkout(workoutId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(expectedPRs)
            coVerify(exactly = 1) { personalRecordDao.getPersonalRecordsForWorkout(workoutId) }
        }

    @Test
    fun `getPersonalRecordForExercise should return latest PR for exercise`() =
        runTest(testDispatcher) {
            // Given
            val exerciseVariationId = "42"
            val expectedPR = createPersonalRecord("1", PRType.WEIGHT, exerciseVariationId = exerciseVariationId)
            coEvery { personalRecordDao.getLatestRecordForExercise(exerciseVariationId) } returns expectedPR

            // When
            val result = repository.getPersonalRecordForExercise(exerciseVariationId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(expectedPR)
            coVerify(exactly = 1) { personalRecordDao.getLatestRecordForExercise(exerciseVariationId) }
        }

    @Test
    fun `getPersonalRecordForExercise should return null when no PR exists`() =
        runTest(testDispatcher) {
            // Given
            val exerciseVariationId = "999"
            coEvery { personalRecordDao.getLatestRecordForExercise(exerciseVariationId) } returns null

            // When
            val result = repository.getPersonalRecordForExercise(exerciseVariationId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isNull()
            coVerify(exactly = 1) { personalRecordDao.getLatestRecordForExercise(exerciseVariationId) }
        }

    @Test
    fun `checkForPR should detect PRs and return them`() =
        runTest(testDispatcher) {
            // Given
            val setLog = createSetLog()
            val exerciseVariationId = "1"
            val prs =
                listOf(
                    createPersonalRecord("1", PRType.WEIGHT, weight = 100f, reps = 5),
                    createPersonalRecord("2", PRType.WEIGHT, weight = 90f, reps = 12),
                )
            val exerciseVariation = createExerciseVariation(exerciseVariationId, "Barbell Bench Press")

            coEvery { prDetectionService.checkForPR(setLog, exerciseVariationId) } returns prs
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation

            val updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit = mockk(relaxed = true)

            // When
            val result = repository.checkForPR(setLog, exerciseVariationId, updateOrInsertOneRM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(prs)
            coVerify(exactly = 1) { prDetectionService.checkForPR(setLog, exerciseVariationId) }
            coVerify(exactly = 1) { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) }
        }

    @Test
    fun `checkForPR should handle empty PR list`() =
        runTest(testDispatcher) {
            // Given
            val setLog = createSetLog()
            val exerciseVariationId = "1"

            coEvery { prDetectionService.checkForPR(setLog, exerciseVariationId) } returns emptyList()

            val updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit = mockk(relaxed = true)

            // When
            val result = repository.checkForPR(setLog, exerciseVariationId, updateOrInsertOneRM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEmpty()
            coVerify(exactly = 1) { prDetectionService.checkForPR(setLog, exerciseVariationId) }
            coVerify(exactly = 0) { exerciseVariationDao.getExerciseVariationById(any()) }
            coVerify(exactly = 0) { updateOrInsertOneRM(any()) }
        }

    @Test
    fun `checkForPR should update OneRM when ESTIMATED_1RM PR is achieved`() =
        runTest(testDispatcher) {
            // Given
            val setLog = createSetLog(actualWeight = 100f, actualReps = 5)
            val exerciseVariationId = "1"
            val estimated1RM = 116.7f
            val prs =
                listOf(
                    createPersonalRecord("1", PRType.ESTIMATED_1RM, weight = 100f, reps = 5, estimated1RM = estimated1RM),
                )
            val exerciseVariation = createExerciseVariation(exerciseVariationId, "Barbell Squat")

            coEvery { prDetectionService.checkForPR(setLog, exerciseVariationId) } returns prs
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation

            val capturedOneRM = slot<UserExerciseMax>()
            val updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit = mockk(relaxed = true)
            coEvery { updateOrInsertOneRM(capture(capturedOneRM)) } returns Unit

            // When
            val result = repository.checkForPR(setLog, exerciseVariationId, updateOrInsertOneRM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(prs)
            coVerify(exactly = 1) { updateOrInsertOneRM(any()) }

            val oneRM = capturedOneRM.captured
            assertThat(oneRM.exerciseVariationId).isEqualTo(exerciseVariationId)
            assertThat(oneRM.oneRMEstimate).isEqualTo(estimated1RM)
            assertThat(oneRM.oneRMConfidence).isEqualTo(1.0f)
            assertThat(oneRM.mostWeightLifted).isEqualTo(100f)
            assertThat(oneRM.mostWeightReps).isEqualTo(5)
        }

    @Test
    fun `checkForPR should not update OneRM when no ESTIMATED_1RM PR`() =
        runTest(testDispatcher) {
            // Given
            val setLog = createSetLog()
            val exerciseVariationId = "1"
            val prs =
                listOf(
                    createPersonalRecord("1", PRType.WEIGHT, weight = 100f, reps = 5),
                    createPersonalRecord("2", PRType.WEIGHT, weight = 90f, reps = 12),
                )
            val exerciseVariation = createExerciseVariation(exerciseVariationId, "Dumbbell Curl")

            coEvery { prDetectionService.checkForPR(setLog, exerciseVariationId) } returns prs
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation

            val updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit = mockk(relaxed = true)

            // When
            val result = repository.checkForPR(setLog, exerciseVariationId, updateOrInsertOneRM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(prs)
            coVerify(exactly = 0) { updateOrInsertOneRM(any()) }
        }

    @Test
    fun `checkForPR should handle null exercise name gracefully`() =
        runTest(testDispatcher) {
            // Given
            val setLog = createSetLog()
            val exerciseVariationId = "1"
            val prs = listOf(createPersonalRecord("1", PRType.WEIGHT))

            coEvery { prDetectionService.checkForPR(setLog, exerciseVariationId) } returns prs
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns null

            val updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit = mockk(relaxed = true)

            // When
            val result = repository.checkForPR(setLog, exerciseVariationId, updateOrInsertOneRM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(prs)
            // Should still log with "Unknown" exercise name - no exception thrown
        }

    // Helper functions
    private fun createPersonalRecord(
        id: String,
        type: PRType,
        weight: Float = 100f,
        reps: Int = 10,
        exerciseVariationId: String = "1",
        workoutId: String = "1",
        estimated1RM: Float? = null,
    ) = PersonalRecord(
        id = id,
        exerciseVariationId = exerciseVariationId,
        workoutId = workoutId,
        recordType = type,
        weight = weight,
        reps = reps,
        estimated1RM = estimated1RM,
        previousWeight = weight - 5f,
        previousReps = reps - 1,
        previousDate = LocalDateTime.now().minusDays(7),
        improvementPercentage = 5f,
        recordDate = LocalDateTime.now(),
    )

    private fun createSetLog(
        id: String = "1",
        actualWeight: Float = 100f,
        actualReps: Int = 10,
    ) = SetLog(
        id = id,
        userId = null,
        exerciseLogId = "1",
        setOrder = 1,
        targetReps = 10,
        targetWeight = 100f,
        actualReps = actualReps,
        actualWeight = actualWeight,
        isCompleted = true,
    )

    private fun createExerciseVariation(
        id: String,
        name: String,
    ) = ExerciseVariation(
        id = id,
        coreExerciseId = "1",
        name = name,
        equipment = Equipment.BARBELL,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        requiresWeight = true,
    )
}
