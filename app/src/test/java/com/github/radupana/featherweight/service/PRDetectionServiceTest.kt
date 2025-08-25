package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.fixtures.WorkoutFixtures
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for PRDetectionService
 * 
 * Tests personal record detection logic including:
 * - Weight PR detection
 * - Estimated 1RM PR detection
 * - PR comparison logic
 * - Edge cases and validation
 */
@ExperimentalCoroutinesApi
class PRDetectionServiceTest {
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private val personalRecordDao: PersonalRecordDao = mockk(relaxed = true)
    private val setLogDao: SetLogDao = mockk(relaxed = true)
    private val exerciseVariationDao: ExerciseVariationDao = mockk(relaxed = true)
    
    private lateinit var service: PRDetectionService
    
    @Before
    fun setUp() {
        LogMock.setup()
        service = PRDetectionService(personalRecordDao, setLogDao, exerciseVariationDao)
    }
    
    // ========== checkForPR Tests ==========
    
    @Test
    fun `checkForPR detects weight PR when lifting more weight`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 100f,
            actualReps = 5,
            actualRpe = 8f,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Mock exercise variation
        val variation = mockk<ExerciseVariation> {
            coEvery { rmScalingType } returns RMScalingType.STANDARD
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation
        
        // Mock previous best weight (less than current)
        coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 95f // Less than current 100f
        coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 110f // Some 1RM
        
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns 
            PersonalRecord(
                id = 1,
                exerciseVariationId = exerciseVariationId,
                weight = 95f,
                reps = 5,
                rpe = null,
                recordDate = LocalDateTime.now().minusDays(7),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
                estimated1RM = null,
                notes = null,
                workoutId = 1L
            )
        
        // Mock 1RM records
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null
        
        // Mock workout data
        coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
        coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns 2L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
        assertThat(result[0].weight).isEqualTo(100f)
        assertThat(result[0].reps).isEqualTo(5)
        
        // Verify the PR was saved
        coVerify { personalRecordDao.insertPersonalRecord(any()) }
    }
    
    @Test
    fun `checkForPR returns empty when weight is not a PR`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 90f,
            actualReps = 5,
            actualRpe = 8f,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Mock exercise variation
        val variation = mockk<ExerciseVariation> {
            coEvery { rmScalingType } returns RMScalingType.STANDARD
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation
        
        // Mock previous best weight (higher than current)
        coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 100f // Higher than current 90f
        coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 120f // Some high 1RM
        
        // Mock previous PR for context
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns 
            PersonalRecord(
                id = 1,
                exerciseVariationId = exerciseVariationId,
                weight = 100f,  // Higher than current
                reps = 5,
                rpe = null,
                recordDate = LocalDateTime.now().minusDays(7),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
                estimated1RM = null,
                notes = null,
                workoutId = 1L
            )
        
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no PR was saved
        coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
    }
    
    @Test
    fun `checkForPR detects estimated 1RM PR`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 80f,
            actualReps = 8,
            actualRpe = 9f,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Mock exercise variation
        val variation = mockk<ExerciseVariation> {
            coEvery { rmScalingType } returns RMScalingType.STANDARD
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation
        
        // Mock previous PRs
        coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 75f // Lower than current 80f
        coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 95f // Previous best 1RM
        
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns null
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns 
            PersonalRecord(
                id = 1,
                exerciseVariationId = exerciseVariationId,
                weight = 75f,
                reps = 10,
                rpe = null,
                recordDate = LocalDateTime.now().minusDays(7),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
                estimated1RM = 95f,  // Previous best 1RM
                notes = null,
                workoutId = 1L
            )
        
        // Mock workout data
        coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
        coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns 2L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert - The estimated 1RM for 80kg x 8 @ RPE 9 should be higher than 95kg
        // 80kg is also a weight PR (higher than 75kg), so we should get both PRs
        assertThat(result).isNotEmpty()
        
        // Could be weight PR, 1RM PR, or both (but not duplicate)
        val hasWeightPR = result.any { it.recordType == PRType.WEIGHT }
        val has1RMPR = result.any { it.recordType == PRType.ESTIMATED_1RM }
        
        assertThat(hasWeightPR || has1RMPR).isTrue()
        
        // Check that any PR has the correct estimated 1RM
        val prWith1RM = result.find { it.estimated1RM != null }
        assertThat(prWith1RM).isNotNull()
        assertThat(prWith1RM!!.estimated1RM).isGreaterThan(95f)
        
        // Verify the PR was saved
        coVerify { personalRecordDao.insertPersonalRecord(any()) }
    }
    
    @Test
    fun `checkForPR handles no previous PR records`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 60f,
            actualReps = 10,
            actualRpe = 8f,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Mock exercise variation
        val variation = mockk<ExerciseVariation> {
            coEvery { rmScalingType } returns RMScalingType.STANDARD
        }
        coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation
        
        // No previous PRs
        coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
        coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns null
        coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null
        
        // Mock workout data
        coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
        coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns 1L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert - Should create first PR record (weight PR which includes estimated 1RM)
        assertThat(result).hasSize(1) // Only weight PR (includes estimated 1RM)
        assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
        assertThat(result[0].weight).isEqualTo(60f)
        assertThat(result[0].estimated1RM).isNotNull() // Weight PR includes estimated 1RM
    }
    
    @Test
    fun `checkForPR ignores incomplete sets`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 100f,
            actualReps = 5,
            isCompleted = false  // Not completed
        )
        val exerciseVariationId = 1L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no database calls were made
        coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
        coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
    }
    
    @Test
    fun `checkForPR ignores sets with zero weight`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 0f,
            actualReps = 10,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no database calls were made
        coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
        coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
    }
    
    @Test
    fun `checkForPR ignores sets with zero reps`() = runTest {
        // Arrange
        val setLog = WorkoutFixtures.createSetLog(
            actualWeight = 100f,
            actualReps = 0,
            isCompleted = true
        )
        val exerciseVariationId = 1L
        
        // Act
        val result = service.checkForPR(setLog, exerciseVariationId)
        
        // Assert
        assertThat(result).isEmpty()
        
        // Verify no database calls were made
        coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
        coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
    }
}