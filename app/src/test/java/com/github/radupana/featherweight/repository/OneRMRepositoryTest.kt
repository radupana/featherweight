package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.profile.Big4ExerciseWithOptionalMax
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.OneRMWithExerciseName
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.service.OneRMService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class OneRMRepositoryTest {
    private lateinit var repository: OneRMRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var oneRMDao: OneRMDao
    private lateinit var oneRMService: OneRMService
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var setLogDao: SetLogDao

    @Before
    fun setUp() {
        db = mockk(relaxed = true)
        oneRMDao = mockk(relaxed = true)
        oneRMService = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)

        every { db.exerciseDao() } returns exerciseDao
        every { db.setLogDao() } returns setLogDao

        repository = OneRMRepository(db, oneRMDao, oneRMService)
    }

    @Test
    fun `clearPendingOneRMUpdates clears all pending updates`() =
        runTest {
            // Arrange - First add some pending updates via checkForNew1RM
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    actualRpe = 8f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(100f, 5) } returns 115f
            coEvery { oneRMDao.getCurrentOneRMEstimate(1L) } returns 100f

            // Act - Add pending update
            repository.checkForNew1RM(1L, setLog)
            val beforeClear = repository.pendingOneRMUpdates.value

            // Clear updates
            repository.clearPendingOneRMUpdates()
            val afterClear = repository.pendingOneRMUpdates.value

            // Assert
            assertTrue(beforeClear.isNotEmpty())
            assertTrue(afterClear.isEmpty())
        }

    @Test
    fun `applyOneRMUpdate updates existing max correctly`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val update =
                PendingOneRMUpdate(
                    exerciseVariationId = exerciseVariationId,
                    currentMax = 100f,
                    suggestedMax = 110f,
                    confidence = 0.95f,
                    source = "3×100kg @ RPE 8",
                    workoutDate = LocalDateTime.now(),
                )

            val existingMax =
                UserExerciseMax(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = 100f,
                    mostWeightReps = 1,
                    oneRMEstimate = 100f,
                    oneRMContext = "Previous test",
                    oneRMConfidence = 0.9f,
                    oneRMDate = LocalDateTime.now().minusDays(7),
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = exerciseVariationId,
                    coreExerciseId = 1,
                    name = "Barbell Squat",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    isCustom = false,
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
            coEvery { oneRMDao.getCurrentMax(exerciseVariationId) } returns existingMax
            coEvery { oneRMDao.updateExerciseMax(any()) } just Runs
            coEvery { oneRMDao.insertOneRMHistory(any()) } returns 1L

            // Act
            repository.applyOneRMUpdate(update)

            // Assert
            coVerify(exactly = 1) { oneRMDao.updateExerciseMax(any()) }
            coVerify(exactly = 1) { oneRMDao.insertOneRMHistory(any()) }
        }

    @Test
    fun `applyOneRMUpdate inserts new max when none exists`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val update =
                PendingOneRMUpdate(
                    exerciseVariationId = exerciseVariationId,
                    currentMax = null,
                    suggestedMax = 100f,
                    confidence = 0.90f,
                    source = "5×90kg",
                    workoutDate = LocalDateTime.now(),
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = exerciseVariationId,
                    coreExerciseId = 1,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    isCustom = false,
                )

            coEvery { exerciseDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
            coEvery { oneRMDao.getCurrentMax(exerciseVariationId) } returns null
            coEvery { oneRMDao.insertExerciseMax(any()) } returns 1L
            coEvery { oneRMDao.insertOneRMHistory(any()) } returns 1L

            // Act
            repository.applyOneRMUpdate(update)

            // Assert
            coVerify(exactly = 1) { oneRMDao.insertExerciseMax(any()) }
            coVerify(exactly = 1) { oneRMDao.insertOneRMHistory(any()) }
        }

    @Test
    fun `applyOneRMUpdate does nothing when exercise variation not found`() =
        runTest {
            // Arrange
            val update =
                PendingOneRMUpdate(
                    exerciseVariationId = 999L,
                    currentMax = null,
                    suggestedMax = 100f,
                    confidence = 0.90f,
                    source = "Test",
                    workoutDate = LocalDateTime.now(),
                )

            coEvery { exerciseDao.getExerciseVariationById(999L) } returns null

            // Act
            repository.applyOneRMUpdate(update)

            // Assert
            coVerify(exactly = 0) { oneRMDao.insertExerciseMax(any()) }
            coVerify(exactly = 0) { oneRMDao.updateExerciseMax(any()) }
            coVerify(exactly = 0) { oneRMDao.insertOneRMHistory(any()) }
        }

    @Test
    fun `getOneRMForExercise returns correct 1RM`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val expected1RM = 150f

            coEvery { oneRMDao.getCurrentOneRMEstimate(exerciseVariationId) } returns expected1RM

            // Act
            val result = repository.getOneRMForExercise(exerciseVariationId)

            // Assert
            assertEquals(expected1RM, result)
        }

    @Test
    fun `getOneRMForExercise returns null when no 1RM exists`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L

            coEvery { oneRMDao.getCurrentOneRMEstimate(exerciseVariationId) } returns null

            // Act
            val result = repository.getOneRMForExercise(exerciseVariationId)

            // Assert
            assertNull(result)
        }

    @Test
    fun `getEstimated1RM calculates from last completed set`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val lastSet =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    completedAt = LocalDateTime.now().toString(),
                )
            val expected1RM = 115f

            coEvery { setLogDao.getLastCompletedSetForExercise(exerciseVariationId) } returns lastSet
            coEvery { oneRMService.calculateEstimated1RM(100f, 5) } returns expected1RM

            // Act
            val result = repository.getEstimated1RM(exerciseVariationId)

            // Assert
            assertEquals(expected1RM, result)
        }

    @Test
    fun `getEstimated1RM returns null when no completed sets`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L

            coEvery { setLogDao.getLastCompletedSetForExercise(exerciseVariationId) } returns null

            // Act
            val result = repository.getEstimated1RM(exerciseVariationId)

            // Assert
            assertNull(result)
        }

    @Test
    fun `getCurrentOneRMEstimate calculates correctly`() =
        runTest {
            // Arrange
            val weight = 80f
            val reps = 8
            val expected1RM = 100f

            coEvery { oneRMService.calculateEstimated1RM(weight, reps) } returns expected1RM

            // Act
            val result = repository.getCurrentOneRMEstimate(weight, reps)

            // Assert
            assertEquals(expected1RM, result)
        }

    @Test
    fun `updateOrInsertOneRM updates existing record`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val existingMax =
                UserExerciseMax(
                    id = 1,
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = 100f,
                    mostWeightReps = 1,
                    oneRMEstimate = 100f,
                    oneRMContext = "Old",
                    oneRMConfidence = 0.85f,
                    oneRMDate = LocalDateTime.now().minusDays(30),
                )

            val newRecord =
                UserExerciseMax(
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = 110f,
                    mostWeightReps = 1,
                    oneRMEstimate = 110f,
                    oneRMContext = "New PR",
                    oneRMConfidence = 0.95f,
                    oneRMDate = LocalDateTime.now(),
                )

            coEvery { oneRMDao.getCurrentMax(exerciseVariationId) } returns existingMax
            val capturedUpdate = slot<UserExerciseMax>()
            coEvery { oneRMDao.updateExerciseMax(capture(capturedUpdate)) } just Runs
            coEvery { oneRMDao.insertOneRMHistory(any()) } returns 1L

            // Act
            repository.updateOrInsertOneRM(newRecord)

            // Assert
            coVerify(exactly = 1) { oneRMDao.updateExerciseMax(any()) }
            assertEquals(110f, capturedUpdate.captured.mostWeightLifted)
            assertEquals("New PR", capturedUpdate.captured.oneRMContext)
        }

    @Test
    fun `updateOrInsertOneRM inserts new record when none exists`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val newRecord =
                UserExerciseMax(
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = 100f,
                    mostWeightReps = 1,
                    oneRMEstimate = 100f,
                    oneRMContext = "First max",
                    oneRMConfidence = 0.9f,
                    oneRMDate = LocalDateTime.now(),
                )

            coEvery { oneRMDao.getCurrentMax(exerciseVariationId) } returns null
            coEvery { oneRMDao.insertExerciseMax(any()) } returns 1L
            coEvery { oneRMDao.insertOneRMHistory(any()) } returns 1L

            // Act
            repository.updateOrInsertOneRM(newRecord)

            // Assert
            coVerify(exactly = 1) { oneRMDao.insertExerciseMax(any()) }
            coVerify(exactly = 0) { oneRMDao.updateExerciseMax(any()) }
        }

    @Test
    fun `saveOneRMToHistory saves correctly`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val oneRMEstimate = 120f
            val context = "Testing day"
            val expectedId = 42L

            val capturedHistory = slot<OneRMHistory>()
            coEvery { oneRMDao.insertOneRMHistory(capture(capturedHistory)) } returns expectedId

            // Act
            val result = repository.saveOneRMToHistory(exerciseVariationId, oneRMEstimate, context)

            // Assert
            assertEquals(expectedId, result)
            assertEquals(exerciseVariationId, capturedHistory.captured.exerciseVariationId)
            assertEquals(oneRMEstimate, capturedHistory.captured.oneRMEstimate)
            assertEquals(context, capturedHistory.captured.context)
        }

    @Test
    fun `getOneRMHistoryForExercise with limit returns limited history`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val limit = 5
            val expectedHistory =
                listOf(
                    OneRMHistory(1, exerciseVariationId, 120f, "Test 1", LocalDateTime.now()),
                    OneRMHistory(2, exerciseVariationId, 115f, "Test 2", LocalDateTime.now().minusDays(1)),
                    OneRMHistory(3, exerciseVariationId, 110f, "Test 3", LocalDateTime.now().minusDays(2)),
                )

            coEvery { oneRMDao.getRecentOneRMHistory(exerciseVariationId, limit) } returns expectedHistory

            // Act
            val result = repository.getOneRMHistoryForExercise(exerciseVariationId, limit)

            // Assert
            assertEquals(expectedHistory, result)
        }

    @Test
    fun `getOneRMHistoryForExercise without limit returns year of history`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val expectedHistory =
                listOf(
                    OneRMHistory(1, exerciseVariationId, 120f, "Test 1", LocalDateTime.now()),
                    OneRMHistory(2, exerciseVariationId, 115f, "Test 2", LocalDateTime.now().minusMonths(6)),
                )

            coEvery { oneRMDao.getOneRMHistoryInRange(eq(exerciseVariationId), any(), any()) } returns expectedHistory

            // Act
            val result = repository.getOneRMHistoryForExercise(exerciseVariationId)

            // Assert
            assertEquals(expectedHistory, result)
        }

    @Test
    fun `getCurrentMaxesForExercises returns correct map`() =
        runTest {
            // Arrange
            val exerciseIds = listOf(1L, 2L, 3L, 4L)
            val maxes =
                listOf(
                    UserExerciseMax(id = 1, exerciseVariationId = 1L, mostWeightLifted = 100f, mostWeightReps = 1, oneRMEstimate = 100f, oneRMContext = "Test", oneRMConfidence = 0.9f),
                    UserExerciseMax(id = 2, exerciseVariationId = 3L, mostWeightLifted = 150f, mostWeightReps = 1, oneRMEstimate = 150f, oneRMContext = "Test", oneRMConfidence = 0.9f),
                )

            coEvery { oneRMDao.getCurrentMaxesForExercises(exerciseIds) } returns maxes

            // Act
            val result = repository.getCurrentMaxesForExercises(exerciseIds)

            // Assert
            assertEquals(4, result.size)
            assertEquals(100f, result[1L])
            assertEquals(0f, result[2L]) // No max, should be 0
            assertEquals(150f, result[3L])
            assertEquals(0f, result[4L]) // No max, should be 0
        }

    @Test
    fun `getAllCurrentMaxesWithNames returns flow correctly`() =
        runTest {
            // Arrange
            val now = LocalDateTime.now()
            val expectedMaxes =
                listOf(
                    OneRMWithExerciseName(
                        id = 1,
                        exerciseVariationId = 1L,
                        exerciseName = "Squat",
                        oneRMEstimate = 200f,
                        oneRMDate = now,
                        oneRMContext = "Test",
                        mostWeightLifted = 200f,
                        mostWeightReps = 1,
                        mostWeightRpe = null,
                        mostWeightDate = now,
                        oneRMConfidence = 0.95f,
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                    OneRMWithExerciseName(
                        id = 2,
                        exerciseVariationId = 2L,
                        exerciseName = "Bench Press",
                        oneRMEstimate = 150f,
                        oneRMDate = now,
                        oneRMContext = "Test",
                        mostWeightLifted = 150f,
                        mostWeightReps = 1,
                        mostWeightRpe = null,
                        mostWeightDate = now,
                        oneRMConfidence = 0.95f,
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                )

            every { oneRMDao.getAllCurrentMaxesWithNames() } returns flowOf(expectedMaxes)

            // Act
            val result = repository.getAllCurrentMaxesWithNames().first()

            // Assert
            assertEquals(expectedMaxes, result)
        }

    @Test
    fun `getBig4Exercises returns big 4 exercises`() =
        runTest {
            // Arrange
            val expectedExercises =
                listOf(
                    ExerciseVariation(id = 1, coreExerciseId = 1, name = "Barbell Squat", equipment = Equipment.BARBELL, difficulty = ExerciseDifficulty.INTERMEDIATE, requiresWeight = true),
                    ExerciseVariation(id = 2, coreExerciseId = 2, name = "Barbell Bench Press", equipment = Equipment.BARBELL, difficulty = ExerciseDifficulty.INTERMEDIATE, requiresWeight = true),
                    ExerciseVariation(id = 3, coreExerciseId = 3, name = "Barbell Deadlift", equipment = Equipment.BARBELL, difficulty = ExerciseDifficulty.INTERMEDIATE, requiresWeight = true),
                    ExerciseVariation(id = 4, coreExerciseId = 4, name = "Barbell Overhead Press", equipment = Equipment.BARBELL, difficulty = ExerciseDifficulty.INTERMEDIATE, requiresWeight = true),
                )

            coEvery { exerciseDao.getBig4Exercises() } returns expectedExercises

            // Act
            val result = repository.getBig4Exercises()

            // Assert
            assertEquals(expectedExercises, result)
        }

    @Test
    fun `getBig4ExercisesWithMaxes returns flow correctly`() =
        runTest {
            // Arrange
            val now = LocalDateTime.now()
            val expectedData =
                listOf(
                    Big4ExerciseWithOptionalMax(
                        id = 1L,
                        exerciseVariationId = 1L,
                        exerciseName = "Barbell Squat",
                        oneRMEstimate = 200f,
                        oneRMDate = now,
                        oneRMContext = "Test",
                        mostWeightLifted = 200f,
                        mostWeightReps = 1,
                        mostWeightRpe = null,
                        mostWeightDate = now,
                        oneRMConfidence = 0.95f,
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                )

            every { oneRMDao.getBig4ExercisesWithMaxes() } returns flowOf(expectedData)

            // Act
            val result = repository.getBig4ExercisesWithMaxes().first()

            // Assert
            assertEquals(expectedData, result)
        }

    @Test
    fun `getOtherExercisesWithMaxes returns non-big4 exercises`() =
        runTest {
            // Arrange
            val now = LocalDateTime.now()
            val expectedMaxes =
                listOf(
                    OneRMWithExerciseName(
                        id = 5,
                        exerciseVariationId = 5L,
                        exerciseName = "Barbell Row",
                        oneRMEstimate = 100f,
                        oneRMDate = now,
                        oneRMContext = "Test",
                        mostWeightLifted = 100f,
                        mostWeightReps = 1,
                        mostWeightRpe = null,
                        mostWeightDate = now,
                        oneRMConfidence = 0.9f,
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                    OneRMWithExerciseName(
                        id = 6,
                        exerciseVariationId = 6L,
                        exerciseName = "Pull Up",
                        oneRMEstimate = 30f,
                        oneRMDate = now,
                        oneRMContext = "Test",
                        mostWeightLifted = 30f,
                        mostWeightReps = 1,
                        mostWeightRpe = null,
                        mostWeightDate = now,
                        oneRMConfidence = 0.9f,
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                )

            every { oneRMDao.getOtherExercisesWithMaxes() } returns flowOf(expectedMaxes)

            // Act
            val result = repository.getOtherExercisesWithMaxes().first()

            // Assert
            assertEquals(expectedMaxes, result)
        }

    @Test
    fun `deleteAllMaxesForExercise calls DAO correctly`() =
        runTest {
            // Arrange
            val exerciseId = 1L

            coEvery { oneRMDao.deleteAllMaxesForExercise(exerciseId) } just Runs

            // Act
            repository.deleteAllMaxesForExercise(exerciseId)

            // Assert
            coVerify(exactly = 1) { oneRMDao.deleteAllMaxesForExercise(exerciseId) }
        }

    @Test
    fun `checkForNew1RM suggests update when significant improvement`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 3,
                    actualReps = 3,
                    targetWeight = 140f,
                    actualWeight = 140f,
                    actualRpe = 9f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(140f, 3) } returns 152f
            coEvery { oneRMDao.getCurrentOneRMEstimate(exerciseVariationId) } returns 140f // Current max

            // Act
            val result = repository.checkForNew1RM(exerciseVariationId, setLog)

            // Assert
            assertNotNull(result)
            assertEquals(exerciseVariationId, result?.exerciseVariationId)
            assertEquals(140f, result?.currentMax)
            assertEquals(152f, result?.suggestedMax)
            assertEquals(0.95f, result?.confidence) // High confidence for 3 reps
            assertEquals("140.0kg × 3 @ RPE 9.0", result?.source)
        }

    @Test
    fun `checkForNew1RM returns null when improvement is minor`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(100f, 5) } returns 112f
            coEvery { oneRMDao.getCurrentOneRMEstimate(exerciseVariationId) } returns 110f // Only 1.8% improvement

            // Act
            val result = repository.checkForNew1RM(exerciseVariationId, setLog)

            // Assert
            assertNull(result)
        }

    @Test
    fun `checkForNew1RM calculates correct confidence based on reps`() =
        runTest {
            // Test different rep ranges
            val testCases =
                listOf(
                    Triple(2, 0.95f, "High confidence for 2 reps"),
                    Triple(5, 0.90f, "Good confidence for 5 reps"),
                    Triple(8, 0.85f, "Medium confidence for 8 reps"),
                    Triple(12, 0.75f, "Lower confidence for 12 reps"),
                )

            for ((reps, expectedConfidence, description) in testCases) {
                // Arrange
                val setLog =
                    SetLog(
                        id = 1,
                        exerciseLogId = 100,
                        setOrder = 1,
                        targetReps = reps,
                        actualReps = reps,
                        targetWeight = 100f,
                        actualWeight = 100f,
                        completedAt = LocalDateTime.now().toString(),
                    )

                coEvery { oneRMService.calculateEstimated1RM(100f, reps) } returns 120f
                coEvery { oneRMDao.getCurrentOneRMEstimate(1L) } returns 100f

                // Act
                val result = repository.checkForNew1RM(1L, setLog)

                // Assert
                assertNotNull(result)
                assertEquals(expectedConfidence, result?.confidence ?: 0f, 0.01f)
            }
        }

    @Test
    fun `checkForNew1RM handles first time max correctly`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 80f,
                    actualWeight = 80f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(80f, 5) } returns 92f
            coEvery { oneRMDao.getCurrentOneRMEstimate(exerciseVariationId) } returns null // No current max

            // Act
            val result = repository.checkForNew1RM(exerciseVariationId, setLog)

            // Assert
            assertNotNull(result)
            assertNull(result?.currentMax)
            assertEquals(92f, result?.suggestedMax)
        }

    @Test
    fun `checkForNew1RM returns null when calculation fails`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val setLog =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(100f, 5) } returns null

            // Act
            val result = repository.checkForNew1RM(exerciseVariationId, setLog)

            // Assert
            assertNull(result)
        }

    @Test
    fun `getBestHistorical1RM returns max from history`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val history =
                listOf(
                    OneRMHistory(1, exerciseVariationId, 100f, "Test 1", LocalDateTime.now().minusDays(30)),
                    OneRMHistory(2, exerciseVariationId, 115f, "Test 2", LocalDateTime.now().minusDays(15)),
                    OneRMHistory(3, exerciseVariationId, 110f, "Test 3", LocalDateTime.now()),
                )

            coEvery { oneRMDao.getOneRMHistoryInRange(eq(exerciseVariationId), any(), any()) } returns history

            // Act
            val result = repository.getBestHistorical1RM(exerciseVariationId)

            // Assert
            assertEquals(115f, result)
        }

    @Test
    fun `getBestHistorical1RM returns null when no history`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L

            coEvery { oneRMDao.getOneRMHistoryInRange(eq(exerciseVariationId), any(), any()) } returns emptyList()

            // Act
            val result = repository.getBestHistorical1RM(exerciseVariationId)

            // Assert
            assertNull(result)
        }

    @Test
    fun `upsertExerciseMax delegates to DAO correctly`() =
        runTest {
            // Arrange
            val exerciseVariationId = 1L
            val maxWeight = 150f
            val notes = "Competition day"

            coEvery { oneRMDao.upsertExerciseMax(exerciseVariationId, maxWeight, notes) } just Runs

            // Act
            repository.upsertExerciseMax(exerciseVariationId, maxWeight, notes)

            // Assert
            coVerify(exactly = 1) { oneRMDao.upsertExerciseMax(exerciseVariationId, maxWeight, notes) }
        }

    @Test
    fun `pending updates are managed correctly across operations`() =
        runTest {
            // Arrange
            val setLog1 =
                SetLog(
                    id = 1,
                    exerciseLogId = 100,
                    setOrder = 1,
                    targetReps = 3,
                    actualReps = 3,
                    targetWeight = 100f,
                    actualWeight = 100f,
                    actualRpe = 8f,
                    completedAt = LocalDateTime.now().toString(),
                )

            val setLog2 =
                SetLog(
                    id = 2,
                    exerciseLogId = 101,
                    setOrder = 1,
                    targetReps = 5,
                    actualReps = 5,
                    targetWeight = 80f,
                    actualWeight = 80f,
                    actualRpe = 7f,
                    completedAt = LocalDateTime.now().toString(),
                )

            coEvery { oneRMService.calculateEstimated1RM(100f, 3) } returns 109f
            coEvery { oneRMService.calculateEstimated1RM(80f, 5) } returns 92f
            coEvery { oneRMDao.getCurrentOneRMEstimate(1L) } returns 100f
            coEvery { oneRMDao.getCurrentOneRMEstimate(2L) } returns 85f

            // Act - Add two pending updates
            repository.checkForNew1RM(1L, setLog1)
            repository.checkForNew1RM(2L, setLog2)

            // Assert - Both updates should be pending
            val pendingUpdates = repository.pendingOneRMUpdates.value
            assertEquals(2, pendingUpdates.size)

            // Now apply one of the updates
            val exerciseVariation =
                ExerciseVariation(
                    id = 1L,
                    coreExerciseId = 1,
                    name = "Squat",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                )
            coEvery { exerciseDao.getExerciseVariationById(1L) } returns exerciseVariation
            coEvery { oneRMDao.getCurrentMax(1L) } returns null
            coEvery { oneRMDao.insertExerciseMax(any()) } returns 1L
            coEvery { oneRMDao.insertOneRMHistory(any()) } returns 1L

            repository.applyOneRMUpdate(pendingUpdates.first())

            // Assert - Only one update should remain
            val remainingUpdates = repository.pendingOneRMUpdates.value
            assertEquals(1, remainingUpdates.size)
            assertEquals(2L, remainingUpdates.first().exerciseVariationId)
        }
}
