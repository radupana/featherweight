package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.fixtures.WorkoutFixtures
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
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
    private val oneRMDao: OneRMDao = mockk(relaxed = true)
    private val personalRecordDao: PersonalRecordDao = mockk(relaxed = true)
    private val exerciseVariationDao: ExerciseVariationDao = mockk(relaxed = true)
    
    private lateinit var tracker: GlobalProgressTracker
    
    @Before
    fun setUp() {
        // Setup database mocks
        every { database.globalExerciseProgressDao() } returns globalProgressDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.oneRMDao() } returns oneRMDao
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.exerciseVariationDao() } returns exerciseVariationDao
        
        tracker = GlobalProgressTracker(repository, database)
    }
    
    // ========== updateProgressAfterWorkout Tests ==========
    
    @Test
    fun `updateProgressAfterWorkout returns pending 1RM updates for completed workout`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        
        val workout = WorkoutFixtures.createWorkout(
            id = workoutId,
            programmeId = null // Freestyle workout
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = 1L,
                actualWeight = 100f,
                actualReps = 5,
                actualRpe = 8f,
                isCompleted = true
            )
        )
        
        coEvery { repository.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        
        // Mock existing progress
        coEvery { globalProgressDao.getProgressForExercise(userId, exerciseVariationId) } returns null
        
        // Mock current 1RM
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
        
        // Mock exercise variation to be a Big 4 exercise
        val exerciseVariation = mockk<ExerciseVariation> {
            coEvery { name } returns "Barbell Back Squat"
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns exerciseVariation
        
        // Act
        val result = tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert - Since we auto-update now, no pending updates are returned
        assertThat(result).isEmpty()
        
        // Verify progress was saved
        coVerify { globalProgressDao.insertOrUpdate(any()) }
        
        // Verify 1RM was automatically saved (for Big 4 exercise with good confidence)
        coVerify { oneRMDao.insertExerciseMax(any()) }
    }
    
    @Test
    fun `updateProgressAfterWorkout handles multiple exercises`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        
        val workout = WorkoutFixtures.createWorkout(id = workoutId)
        
        val exerciseLogs = listOf(
            WorkoutFixtures.createExerciseLog(id = 1L, workoutId = workoutId, exerciseVariationId = 100L),
            WorkoutFixtures.createExerciseLog(id = 2L, workoutId = workoutId, exerciseVariationId = 200L)
        )
        
        coEvery { repository.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns exerciseLogs
        
        // Mock sets for each exercise
        exerciseLogs.forEach { log ->
            val sets = WorkoutFixtures.createCompletedSets(3, 80f, 10)
            coEvery { setLogDao.getSetLogsForExercise(log.id) } returns sets
        }
        
        // Mock no existing progress
        coEvery { globalProgressDao.getProgressForExercise(any(), any()) } returns null
        coEvery { oneRMDao.getCurrentMax(any(), any()) } returns null
        
        // Mock exercise variations to be Big 4 exercises
        val variation1 = mockk<ExerciseVariation> {
            coEvery { name } returns "Barbell Back Squat"
        }
        val variation2 = mockk<ExerciseVariation> {
            coEvery { name } returns "Barbell Overhead Press"
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(100L) } returns variation1
        coEvery { exerciseVariationDao.getExerciseVariationById(200L) } returns variation2
        
        // Act
        val result = tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert - Since we auto-update now, no pending updates are returned
        assertThat(result).isEmpty()
        
        // Verify progress was saved for both exercises
        coVerify(exactly = 2) { globalProgressDao.insertOrUpdate(any()) }
    }
    
    @Test
    fun `updateProgressAfterWorkout detects stalls when weight unchanged`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val workingWeight = 80f
        
        val workout = WorkoutFixtures.createWorkout(id = workoutId)
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                actualWeight = workingWeight,
                actualReps = 8,
                isCompleted = true
            )
        )
        
        // Mock existing progress showing same weight
        val existingProgress = GlobalExerciseProgress(
            id = 1L,
            userId = userId,
            exerciseVariationId = exerciseVariationId,
            currentWorkingWeight = workingWeight, // Same weight as current
            estimatedMax = 100f,
            lastUpdated = LocalDateTime.now().minusDays(3),
            consecutiveStalls = 2, // Already stalled twice
            trend = ProgressTrend.STALLING
        )
        
        coEvery { repository.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { globalProgressDao.getProgressForExercise(userId, exerciseVariationId) } returns existingProgress
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
        
        val capturedProgress = slot<GlobalExerciseProgress>()
        coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns 1L
        
        // Act
        tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert
        val updatedProgress = capturedProgress.captured
        assertThat(updatedProgress.consecutiveStalls).isGreaterThan(existingProgress.consecutiveStalls)
        assertThat(updatedProgress.trend).isEqualTo(ProgressTrend.STALLING)
    }
    
    @Test
    fun `updateProgressAfterWorkout updates best performance when weight increases`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val oldWeight = 80f
        val newWeight = 85f
        
        val workout = WorkoutFixtures.createWorkout(id = workoutId)
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                actualWeight = newWeight,
                actualReps = 5,
                actualRpe = 8f,
                isCompleted = true
            )
        )
        
        // Mock existing progress with lower weight
        val existingProgress = GlobalExerciseProgress(
            id = 1L,
            userId = userId,
            exerciseVariationId = exerciseVariationId,
            currentWorkingWeight = oldWeight,
            estimatedMax = 90f,
            lastUpdated = LocalDateTime.now().minusDays(3),
            consecutiveStalls = 0,
            trend = ProgressTrend.IMPROVING,
            best5Rep = oldWeight
        )
        
        coEvery { repository.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { globalProgressDao.getProgressForExercise(userId, exerciseVariationId) } returns existingProgress
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
        
        val capturedProgress = slot<GlobalExerciseProgress>()
        coEvery { globalProgressDao.insertOrUpdate(capture(capturedProgress)) } returns 1L
        
        // Act
        tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert
        val updatedProgress = capturedProgress.captured
        assertThat(updatedProgress.currentWorkingWeight).isEqualTo(newWeight)
        assertThat(updatedProgress.best5Rep).isEqualTo(newWeight)
        assertThat(updatedProgress.consecutiveStalls).isEqualTo(0) // Reset on improvement
        assertThat(updatedProgress.trend).isEqualTo(ProgressTrend.IMPROVING)
    }
    
    @Test
    fun `updateProgressAfterWorkout handles empty workout`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        
        val workout = WorkoutFixtures.createWorkout(id = workoutId)
        
        coEvery { repository.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()
        
        // Act
        val result = tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no progress was saved
        coVerify(exactly = 0) { globalProgressDao.insertOrUpdate(any()) }
    }
    
    @Test
    fun `updateProgressAfterWorkout handles workout not found`() = runTest {
        // Arrange
        val workoutId = 999L
        val userId = 1L
        
        coEvery { repository.getWorkoutById(workoutId) } returns null
        
        // Act
        val result = tracker.updateProgressAfterWorkout(workoutId, userId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no further calls were made
        coVerify(exactly = 0) { exerciseLogDao.getExerciseLogsForWorkout(any()) }
        coVerify(exactly = 0) { globalProgressDao.insertOrUpdate(any()) }
    }
}
