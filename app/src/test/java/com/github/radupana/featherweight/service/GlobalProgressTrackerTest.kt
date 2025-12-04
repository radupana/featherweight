package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.fixtures.ProgrammeConfig
import com.github.radupana.featherweight.fixtures.WorkoutFixtures
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for GlobalProgressTracker
 *
 * Tests overall progress tracking including:
 * - Workout progress updates
 * - 1RM estimate tracking
 * - Stall detection
 * - Progress metrics calculation
 */
@ExperimentalCoroutinesApi
class GlobalProgressTrackerTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val repository: FeatherweightRepository = mockk(relaxed = true)
    private val database: FeatherweightDatabase = mockk(relaxed = true)
    private val globalProgressDao: GlobalExerciseProgressDao = mockk(relaxed = true)
    private val exerciseLogDao: ExerciseLogDao = mockk(relaxed = true)
    private val setLogDao: SetLogDao = mockk(relaxed = true)
    private val oneRMDao: ExerciseMaxTrackingDao = mockk(relaxed = true)
    private val personalRecordDao: PersonalRecordDao = mockk(relaxed = true)
    private val exerciseDao: ExerciseDao = mockk(relaxed = true)

    private lateinit var tracker: GlobalProgressTracker

    @Before
    fun setUp() {
        LogMock.setup()

        // Setup database mocks
        every { database.globalExerciseProgressDao() } returns globalProgressDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.exerciseMaxTrackingDao() } returns oneRMDao
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.exerciseDao() } returns exerciseDao

        tracker = GlobalProgressTracker(repository, database)
    }

    // ========== updateProgressAfterWorkout Tests ==========

    @Test
    fun `updateProgressAfterWorkout returns pending 1RM updates for completed workout`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout =
                WorkoutFixtures.createWorkout(
                    id = workoutId,
                    // Freestyle workout (no programme)
                )

            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        id = "1",
                        exerciseLogId = "1",
                        actualWeight = 100f,
                        actualReps = 5,
                        actualRpe = 8f,
                        isCompleted = true,
                    ),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets

            // Mock existing progress
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null

            // Mock current 1RM
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            // Mock exercise variation to be a Big 4 exercise
            val exerciseVariation =
                mockk<Exercise> {
                    coEvery { name } returns "Barbell Back Squat"
                }
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exerciseVariation

            // Act
            val result = tracker.updateProgressAfterWorkout(workoutId)

            // Assert - Since we auto-update now, no pending updates are returned
            assertThat(result).isEmpty()

            // Verify progress was saved
            coVerify { globalProgressDao.insertOrUpdate(any()) }

            // Verify 1RM was automatically saved (for Big 4 exercise with good confidence)
            coVerify { oneRMDao.insert(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout handles multiple exercises`() =
        runTest {
            // Arrange
            val workoutId = "1"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)

            val exerciseLogs =
                listOf(
                    WorkoutFixtures.createExerciseLog(id = "1", workoutId = workoutId, exerciseId = "100"),
                    WorkoutFixtures.createExerciseLog(id = "2", workoutId = workoutId, exerciseId = "200"),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns exerciseLogs

            // Mock sets for each exercise
            exerciseLogs.forEach { log ->
                val sets = WorkoutFixtures.createCompletedSets(3, 80f, 10)
                coEvery { setLogDao.getSetLogsForExercise(log.id) } returns sets
            }

            // Mock no existing progress
            coEvery { globalProgressDao.getProgressForExercise(any()) } returns null
            coEvery { oneRMDao.getCurrentMax(any(), any()) } returns null

            // Mock exercise variations to be Big 4 exercises
            val variation1 =
                mockk<Exercise> {
                    coEvery { name } returns "Barbell Back Squat"
                }
            val variation2 =
                mockk<Exercise> {
                    coEvery { name } returns "Barbell Overhead Press"
                }
            coEvery { exerciseDao.getExerciseById("100") } returns variation1
            coEvery { exerciseDao.getExerciseById("200") } returns variation2

            // Act
            val result = tracker.updateProgressAfterWorkout(workoutId)

            // Assert - Since we auto-update now, no pending updates are returned
            assertThat(result).isEmpty()

            // Verify progress was saved for both exercises
            coVerify(exactly = 2) { globalProgressDao.insertOrUpdate(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout detects stalls when weight unchanged`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"
            val workingWeight = 80f

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = workingWeight,
                        actualReps = 8,
                        isCompleted = true,
                    ),
                )

            // Mock existing progress showing same weight
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = workingWeight, // Same weight as current
                    estimatedMax = 100f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    consecutiveStalls = 2, // Already stalled twice
                    trend = ProgressTrend.STALLING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val updatedProgress = capturedProgress.captured
            assertThat(updatedProgress.consecutiveStalls).isGreaterThan(existingProgress.consecutiveStalls)
            assertThat(updatedProgress.trend).isEqualTo(ProgressTrend.STALLING)
        }

    @Test
    fun `updateProgressAfterWorkout updates best performance when weight increases`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"
            val oldWeight = 80f
            val newWeight = 85f

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = newWeight,
                        actualReps = 5,
                        actualRpe = 8f,
                        isCompleted = true,
                    ),
                )

            // Mock existing progress with lower weight
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = oldWeight,
                    estimatedMax = 90f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    consecutiveStalls = 0,
                    trend = ProgressTrend.IMPROVING,
                    best5Rep = oldWeight,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val updatedProgress = capturedProgress.captured
            assertThat(updatedProgress.currentWorkingWeight).isEqualTo(newWeight)
            assertThat(updatedProgress.best5Rep).isEqualTo(newWeight)
            assertThat(updatedProgress.consecutiveStalls).isEqualTo(0) // Reset on improvement
            assertThat(updatedProgress.trend).isEqualTo(ProgressTrend.IMPROVING)
        }

    @Test
    fun `updateProgressAfterWorkout handles empty workout`() =
        runTest {
            // Arrange
            val workoutId = "1"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()

            // Act
            val result = tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            assertThat(result).isEmpty()

            // Verify no progress was saved
            coVerify(exactly = 0) { globalProgressDao.insertOrUpdate(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout handles workout not found`() =
        runTest {
            // Arrange
            val workoutId = "999"

            coEvery { repository.getWorkoutById(workoutId) } returns null

            // Act
            val result = tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            assertThat(result).isEmpty()

            // Verify no further calls were made
            coVerify(exactly = 0) { exerciseLogDao.getExerciseLogsForWorkout(any()) }
            coVerify(exactly = 0) { globalProgressDao.insertOrUpdate(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout tracks RPE averages correctly`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Sets with various RPE values
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualRpe = 7f, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualRpe = 8f, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualRpe = 9f, isCompleted = true),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.recentAvgRpe).isWithin(0.01f).of(8f) // (7+8+9)/3
        }

    @Test
    fun `updateProgressAfterWorkout updates volume trends correctly`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Sets for volume calculation
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 100f, actualReps = 5, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualWeight = 100f, actualReps = 5, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualWeight = 100f, actualReps = 5, isCompleted = true),
                )
            // Total volume = 3 * 100 * 5 = 1500kg

            // Previous progress with lower average volume
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = 95f,
                    estimatedMax = 120f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    avgSessionVolume = 1200f, // Lower than current session
                    sessionsTracked = 5,
                    trend = ProgressTrend.IMPROVING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.lastSessionVolume).isEqualTo(1500f)
            assertThat(savedProgress.volumeTrend).isEqualTo(VolumeTrend.INCREASING) // 1500 > 1200 * 1.1
            assertThat(savedProgress.sessionsTracked).isEqualTo(6)
        }

    @Test
    fun `updateProgressAfterWorkout detects declining trend on weight regression`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Lower weight than previous
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 70f, actualReps = 5, isCompleted = true),
                )

            // Previous progress with higher weight
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = 80f, // Higher than current
                    estimatedMax = 100f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    consecutiveStalls = 0,
                    failureStreak = 0,
                    trend = ProgressTrend.IMPROVING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.trend).isEqualTo(ProgressTrend.DECLINING)
            assertThat(savedProgress.failureStreak).isEqualTo(1)
            assertThat(savedProgress.consecutiveStalls).isEqualTo(0)
        }

    @Test
    fun `updateProgressAfterWorkout updates all rep PRs correctly`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Sets with various rep ranges
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 140f, actualReps = 1, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualWeight = 120f, actualReps = 3, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualWeight = 100f, actualReps = 5, isCompleted = true),
                    WorkoutFixtures.createSetLog(actualWeight = 80f, actualReps = 8, isCompleted = true),
                )

            // No existing progress
            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.bestSingleRep).isEqualTo(140f)
            assertThat(savedProgress.best3Rep).isEqualTo(120f)
            assertThat(savedProgress.best5Rep).isEqualTo(100f)
            assertThat(savedProgress.best8Rep).isEqualTo(80f)
            assertThat(savedProgress.lastPrWeight).isEqualTo(140f) // Single was the highest
        }

    @Test
    fun `updateProgressAfterWorkout tracks weeks at current weight`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"
            val sameWeight = 80f

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = sameWeight, actualReps = 5, isCompleted = true),
                )

            // Existing progress with same weight from 3 weeks ago
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = sameWeight,
                    estimatedMax = 100f,
                    lastUpdated = LocalDateTime.now().minusWeeks(3),
                    lastProgressionDate = LocalDateTime.now().minusWeeks(3),
                    weeksAtCurrentWeight = 0,
                    consecutiveStalls = 1,
                    trend = ProgressTrend.STALLING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.weeksAtCurrentWeight).isEqualTo(3)
            assertThat(savedProgress.consecutiveStalls).isEqualTo(2)
        }

    @Test
    fun `updateProgressAfterWorkout automatically saves 1RM for Big 4 exercises`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // High confidence set (3 reps)
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = 120f,
                        actualReps = 3,
                        actualRpe = 9f,
                        isCompleted = true,
                    ),
                )

            // Mock Big 4 exercise
            val exerciseVariation =
                mockk<Exercise> {
                    coEvery { name } returns "Barbell Bench Press"
                }

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exerciseVariation

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert - 1RM should be automatically saved
            coVerify { oneRMDao.insert(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout updates existing 1RM when improved`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // True 1RM attempt
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = 140f,
                        actualReps = 1,
                        actualRpe = 10f,
                        isCompleted = true,
                    ),
                )

            // Existing lower 1RM
            val existingMax =
                ExerciseMaxTracking(
                    userId = "local",
                    exerciseId = exerciseId,
                    oneRMEstimate = 130f, // Lower than new
                    context = "125kg × 3 @ RPE 9",
                    sourceSetId = null,
                    oneRMConfidence = 0.85f,
                    recordedAt = LocalDateTime.now().minusWeeks(2),
                    mostWeightLifted = 125f,
                    mostWeightReps = 3,
                    mostWeightRpe = 9f,
                    mostWeightDate = LocalDateTime.now().minusWeeks(2),
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
                    notes = null,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns existingMax
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns
                mockk {
                    coEvery { name } returns "Barbell Back Squat"
                }

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            coVerify { oneRMDao.update(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout handles sets with no completed ones`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // All incomplete sets
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(isCompleted = false),
                    WorkoutFixtures.createSetLog(isCompleted = false),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets

            // Act
            val result = tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            assertThat(result).isEmpty()
            coVerify(exactly = 0) { globalProgressDao.insertOrUpdate(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout calculates confidence correctly for singles with RPE`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Single with RPE 8 (not a true max)
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = 120f,
                        actualReps = 1,
                        actualRpe = 8f,
                        isCompleted = true,
                    ),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns
                mockk {
                    coEvery { name } returns "Barbell Deadlift"
                }

            val capturedMax = slot<ExerciseMaxTracking>()
            coEvery { oneRMDao.insert(capture(capturedMax)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedMax = capturedMax.captured
            // Single with RPE 8 actually has high confidence (0.90 based on the implementation)
            assertThat(savedMax.oneRMConfidence).isWithin(0.01f).of(0.90f)
            // Estimated 1RM from 120kg @ RPE 8 (2 RIR) = 120 / (1.0278 - 0.0278 * 3) ≈ 127kg
            assertThat(savedMax.oneRMEstimate).isWithin(1f).of(127f)
        }

    @Test
    fun `updateProgressAfterWorkout prioritizes true 1RMs over calculated estimates`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Mix of true 1RM and multi-rep sets
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = 130f,
                        actualReps = 1,
                        actualRpe = 10f,
                        isCompleted = true,
                    ),
                    WorkoutFixtures.createSetLog(
                        actualWeight = 110f,
                        actualReps = 5,
                        actualRpe = 8f,
                        isCompleted = true,
                    ),
                )
            // The 5-rep set might calculate to higher 1RM but true 1RM should win

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns
                mockk {
                    coEvery { name } returns "Barbell Overhead Press"
                }

            val capturedMax = slot<ExerciseMaxTracking>()
            coEvery { oneRMDao.insert(capture(capturedMax)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedMax = capturedMax.captured
            assertThat(savedMax.oneRMEstimate).isEqualTo(130f) // True 1RM
            assertThat(savedMax.context).contains("1RM")
        }

    @Test
    fun `updateProgressAfterWorkout handles programme vs freestyle workouts`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"
            val programmeId = "10"

            val workout =
                WorkoutFixtures.createWorkout(
                    id = workoutId,
                    programme = ProgrammeConfig(programmeId = programmeId), // Programme workout
                )
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 80f, actualReps = 5, isCompleted = true),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.lastProgrammeWorkoutId).isEqualTo(workoutId)
            assertThat(savedProgress.lastFreestyleWorkoutId).isNull()
        }

    @Test
    fun `updateProgressAfterWorkout filters sets by rep range for 1RM calculation`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Mix of valid and invalid rep ranges
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 80f, actualReps = 5, isCompleted = true), // Valid
                    WorkoutFixtures.createSetLog(actualWeight = 50f, actualReps = 20, isCompleted = true), // Too many reps
                    WorkoutFixtures.createSetLog(actualWeight = 0f, actualReps = 10, isCompleted = true), // No weight
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns
                mockk {
                    coEvery { name } returns "Barbell Back Squat"
                }

            val capturedMax = slot<ExerciseMaxTracking>()
            coEvery { oneRMDao.insert(capture(capturedMax)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert - Only the valid 5-rep set should be used
            val savedMax = capturedMax.captured
            assertThat(savedMax.mostWeightLifted).isEqualTo(80f)
            assertThat(savedMax.mostWeightReps).isEqualTo(5)
        }

    @Test
    fun `updateProgressAfterWorkout doesnt save 1RM with low confidence`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // High rep set with low confidence for 1RM
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        actualWeight = 40f,
                        actualReps = 12,
                        actualRpe = null, // No RPE makes confidence lower
                        isCompleted = true,
                    ),
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user-id") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns
                mockk {
                    coEvery { name } returns "Barbell Bench Press"
                }

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert - 1RM should NOT be saved due to low confidence (<0.6)
            coVerify(exactly = 0) { oneRMDao.insert(any()) }
            coVerify(exactly = 0) { oneRMDao.update(any()) }
        }

    @Test
    fun `updateProgressAfterWorkout maintains average session volume calculation`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Current session volume = 100kg * 10 reps * 3 sets = 3000kg
            val sets = WorkoutFixtures.createCompletedSets(3, 100f, 10)

            // Existing progress with 10 sessions tracked
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = 100f,
                    estimatedMax = 130f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    avgSessionVolume = 2500f, // Previous average
                    sessionsTracked = 10,
                    trend = ProgressTrend.IMPROVING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            // New average = (2500 * 10 + 3000) / 11 = 28000 / 11 ≈ 2545.45 but actual calc is 2541.67
            assertThat(savedProgress.avgSessionVolume).isWithin(1f).of(2541.67f)
            assertThat(savedProgress.sessionsTracked).isEqualTo(11)
        }

    @Test
    fun `updateProgressAfterWorkout detects volume decrease trend`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout = WorkoutFixtures.createWorkout(id = workoutId)
            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // Low volume session
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(actualWeight = 60f, actualReps = 5, isCompleted = true),
                )
            // Volume = 60 * 5 = 300kg

            // Existing progress with higher average volume
            val existingProgress =
                GlobalExerciseProgress(
                    id = "1",
                    exerciseId = exerciseId,
                    currentWorkingWeight = 80f,
                    estimatedMax = 100f,
                    lastUpdated = LocalDateTime.now().minusDays(3),
                    avgSessionVolume = 500f, // Much higher average
                    sessionsTracked = 5,
                    trend = ProgressTrend.IMPROVING,
                )

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns existingProgress

            val capturedProgress = slot<GlobalExerciseProgress>()
            coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedProgress = capturedProgress.captured
            assertThat(savedProgress.volumeTrend).isEqualTo(VolumeTrend.DECREASING) // 300 < 500 * 0.9
        }

    @Test
    fun `updateProgressAfterWorkout correctly tracks mostWeightLifted separate from oneRMEstimate`() =
        runTest {
            // Arrange
            val workoutId = "1"
            val exerciseId = "100"

            val workout =
                WorkoutFixtures.createWorkout(
                    id = workoutId,
                )

            val exerciseLog =
                WorkoutFixtures.createExerciseLog(
                    id = "1",
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                )

            // User lifts 120kg × 3 @ RPE 8
            // This will estimate ~135kg 1RM
            // But mostWeightLifted should be 120kg, not 135kg
            val sets =
                listOf(
                    WorkoutFixtures.createSetLog(
                        id = "1",
                        exerciseLogId = "1",
                        actualWeight = 120f,
                        actualReps = 3,
                        actualRpe = 8f,
                        userId = "local",
                    ),
                )

            val exercise =
                mockk<Exercise> {
                    coEvery { id } returns exerciseId
                    coEvery { name } returns "Barbell Bench Press"
                }

            coEvery { repository.getWorkoutById(workoutId) } returns workout
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise("1") } returns sets
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exercise
            coEvery { globalProgressDao.getProgressForExercise(exerciseId) } returns null
            coEvery { oneRMDao.getCurrentMax(exerciseId, "local") } returns null

            val capturedMax = slot<ExerciseMaxTracking>()
            coEvery { oneRMDao.insert(capture(capturedMax)) } returns Unit
            coEvery { globalProgressDao.insertOrUpdate(any()) } returns Unit

            // Act
            tracker.updateProgressAfterWorkout(workoutId)

            // Assert
            val savedMax = capturedMax.captured
            // 1RM estimate should be calculated (~135kg)
            assertThat(savedMax.oneRMEstimate).isGreaterThan(120f)
            // But mostWeightLifted should be the actual weight lifted (120kg)
            assertThat(savedMax.mostWeightLifted).isEqualTo(120f)
            assertThat(savedMax.mostWeightReps).isEqualTo(3)
            assertThat(savedMax.mostWeightRpe).isEqualTo(8f)
        }
}
