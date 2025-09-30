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
    fun `checkForPR detects weight PR when lifting more weight`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight (less than current)
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 95f // Less than current 100f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 110f // Some 1RM

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns
                PersonalRecord(
                    id = "1",
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
                    workoutId = "1",
                )

            // Mock 1RM records
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "2"

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
    fun `checkForPR returns empty when weight is not a PR`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 90f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight (higher than current)
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 100f // Higher than current 90f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 120f // Some high 1RM

            // Mock previous PR for context
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns
                PersonalRecord(
                    id = "1",
                    exerciseVariationId = exerciseVariationId,
                    weight = 100f, // Higher than current
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
                    workoutId = "1",
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
    fun `checkForPR detects estimated 1RM PR`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 80f,
                    actualReps = 8,
                    actualRpe = 9f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous PRs
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 75f // Lower than current 80f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 95f // Previous best 1RM

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns
                PersonalRecord(
                    id = "1",
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
                    estimated1RM = 95f, // Previous best 1RM
                    notes = null,
                    workoutId = "1",
                )

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "2"

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
    fun `checkForPR handles no previous PR records`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 60f,
                    actualReps = 10,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
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
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert - Should create first PR record (weight PR which includes estimated 1RM)
            assertThat(result).hasSize(1) // Only weight PR (includes estimated 1RM)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].weight).isEqualTo(60f)
            assertThat(result[0].estimated1RM).isNotNull() // Weight PR includes estimated 1RM
        }

    @Test
    fun `checkForPR ignores incomplete sets`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    isCompleted = false, // Not completed
                )
            val exerciseVariationId = "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).isEmpty()

            // Verify no database calls were made
            coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
            coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkForPR ignores sets with zero weight`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 0f,
                    actualReps = 10,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).isEmpty()

            // Verify no database calls were made
            coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
            coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkForPR ignores sets with zero reps`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 0,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).isEmpty()

            // Verify no database calls were made
            coVerify(exactly = 0) { personalRecordDao.getLatestPRForExerciseAndType(any(), any()) }
            coVerify(exactly = 0) { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkForPR with weighted bodyweight scaling type`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 20f, // 20kg added weight
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation with weighted bodyweight scaling
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.WEIGHTED_BODYWEIGHT
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // No previous PRs
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].weight).isEqualTo(20f)
            assertThat(result[0].estimated1RM).isNotNull()
            // For weighted BW with 5 reps @ RPE 8, estimated 1RM uses modified Epley
            // RPE 8 = 2 RIR, so total capacity = 7 reps
            // 20 * (1 + 7 * 0.035) = 20 * 1.245 = 24.9
            assertThat(result[0].estimated1RM).isWithin(0.1f).of(24.9f)
        }

    @Test
    fun `checkForPR with isolation exercise scaling type`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 15f,
                    actualReps = 12,
                    actualRpe = 9f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation with isolation scaling
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.ISOLATION
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // No previous PRs
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].weight).isEqualTo(15f)
            assertThat(result[0].estimated1RM).isNotNull()
            // For isolation with 12 reps @ RPE 9, estimated 1RM uses Lombardi
            // RPE 9 = 1 RIR, so total capacity = 13 reps
            // 15 * 13^0.10 ≈ 15 * 1.291 = 19.36
            assertThat(result[0].estimated1RM).isWithin(0.5f).of(19.36f)
        }

    @Test
    fun `checkForPR skips 1RM calculation when RPE is 6 or below`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 80f,
                    actualReps = 10,
                    actualRpe = 6f, // Too low for reliable 1RM estimate
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // No previous PRs
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].weight).isEqualTo(80f)
            assertThat(result[0].estimated1RM).isNull() // No 1RM calculated due to low RPE
            assertThat(result[0].notes).contains("First weight record")
        }

    @Test
    fun `checkForPR handles duplicate PR in same workout`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"
            val workoutId = "2"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 90f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 100f

            // Mock existing PR in the same workout (lower weight)
            val existingPRInWorkout =
                PersonalRecord(
                    id = "10",
                    exerciseVariationId = exerciseVariationId,
                    weight = 95f, // Lower than new PR
                    reps = 5,
                    rpe = 7f,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 5f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 105f,
                    notes = "Previous PR",
                    workoutId = workoutId,
                )

            coEvery {
                personalRecordDao.getPRForExerciseInWorkout(workoutId, exerciseVariationId, PRType.WEIGHT)
            } returns existingPRInWorkout

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns workoutId

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].weight).isEqualTo(100f)

            // Verify old PR was deleted and new one inserted
            coVerify { personalRecordDao.deletePR("10") }
            coVerify { personalRecordDao.insertPersonalRecord(any()) }
        }

    @Test
    fun `checkForPR handles date parsing error gracefully`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data with invalid date format
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns "invalid-date-format"
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert - Should still create PR with current date
            assertThat(result).hasSize(1)
            assertThat(result[0].weight).isEqualTo(100f)
            assertThat(result[0].recordDate).isNotNull()
        }

    @Test
    fun `checkForPR calculates correct improvement percentage`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 110f, // 10% more than previous
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 100f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 110f

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns
                PersonalRecord(
                    id = "1",
                    exerciseVariationId = exerciseVariationId,
                    weight = 100f,
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
                    workoutId = "1",
                )

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "2"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].weight).isEqualTo(110f)
            assertThat(result[0].improvementPercentage).isWithin(0.01f).of(10f) // 10% improvement
        }

    @Test
    fun `checkForPR generates helpful notes for potential improvement`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 85f, // New weight PR
                    actualReps = 12, // High reps
                    actualRpe = 7f, // Low effort (3 RIR)
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight - lower than current
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 80f
            // High existing 1RM - higher than what would be calculated from current set
            // 85kg x 12 @ RPE 7 (3 RIR = 15 total capacity)
            // = 85 / (1.0278 - 0.0278 * 15) = 85 / 0.611 = 139kg
            // So we need existing 1RM > 139kg for the message to appear
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 145f

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.WEIGHT) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, PRType.ESTIMATED_1RM) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].weight).isEqualTo(85f)
            // The estimated 1RM from 85kg x 12 @ RPE 7 is ~139kg
            // This is less than the current max 1RM of 145kg
            // So the note should mention potential for more
            assertThat(result[0].notes).contains("you could potentially lift more")
        }

    @Test
    fun `checkForPR with more than 15 total rep capacity skips 1RM`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 50f,
                    actualReps = 15,
                    actualRpe = 7f, // 3 RIR = 18 total capacity
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // No previous PRs
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].estimated1RM).isNull() // No 1RM due to high rep capacity
        }

    @Test
    fun `checkForPR handles null workout date`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data with null date
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns null
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns null

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].recordDate).isNotNull() // Uses current date
            assertThat(result[0].workoutId).isNull()
        }

    @Test
    fun `checkForPR with true 1 rep max`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 140f,
                    actualReps = 1,
                    actualRpe = 10f, // True max effort
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].weight).isEqualTo(140f)
            assertThat(result[0].estimated1RM).isEqualTo(140f) // True 1RM
            assertThat(result[0].reps).isEqualTo(1)
        }

    // ========== getRecentPRsForExercise Tests ==========

    @Test
    fun `getRecentPRsForExercise returns PRs for specific exercise`() =
        runTest {
            // Arrange
            val exerciseVariationId = "1"
            val limit = 5
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = "1",
                        exerciseVariationId = exerciseVariationId,
                        weight = 100f,
                        reps = 5,
                        rpe = 8f,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        estimated1RM = 112f,
                        notes = "Weight PR",
                        workoutId = "1",
                    ),
                    PersonalRecord(
                        id = "2",
                        exerciseVariationId = exerciseVariationId,
                        weight = 90f,
                        reps = 8,
                        rpe = 9f,
                        recordDate = LocalDateTime.now().minusDays(7),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.ESTIMATED_1RM,
                        estimated1RM = 110f,
                        notes = "1RM PR",
                        workoutId = "2",
                    ),
                )

            coEvery {
                personalRecordDao.getRecentPRsForExercise(exerciseVariationId, limit)
            } returns expectedPRs

            // Act
            val result = service.getRecentPRsForExercise(exerciseVariationId, limit)

            // Assert
            assertThat(result).isEqualTo(expectedPRs)
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getRecentPRsForExercise with default limit`() =
        runTest {
            // Arrange
            val exerciseVariationId = "1"
            val expectedPRs = emptyList<PersonalRecord>()

            coEvery {
                personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 5) // Default limit
            } returns expectedPRs

            // Act
            val result = service.getRecentPRsForExercise(exerciseVariationId)

            // Assert
            assertThat(result).isEmpty()
            coVerify { personalRecordDao.getRecentPRsForExercise(exerciseVariationId, 5) }
        }

    // ========== getRecentPRs Tests ==========

    @Test
    fun `getRecentPRs returns all recent PRs`() =
        runTest {
            // Arrange
            val limit = 10
            val expectedPRs =
                listOf(
                    PersonalRecord(
                        id = "1",
                        exerciseVariationId = "1",
                        weight = 100f,
                        reps = 5,
                        rpe = 8f,
                        recordDate = LocalDateTime.now(),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        estimated1RM = 112f,
                        notes = "Squat PR",
                        workoutId = "1",
                    ),
                    PersonalRecord(
                        id = "2",
                        exerciseVariationId = "2",
                        weight = 80f,
                        reps = 5,
                        rpe = 9f,
                        recordDate = LocalDateTime.now().minusDays(1),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        estimated1RM = 88f,
                        notes = "Bench PR",
                        workoutId = "2",
                    ),
                )

            coEvery { personalRecordDao.getRecentPRs(limit) } returns expectedPRs

            // Act
            val result = service.getRecentPRs(limit)

            // Assert
            assertThat(result).isEqualTo(expectedPRs)
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getRecentPRs with default limit`() =
        runTest {
            // Arrange
            val expectedPRs = emptyList<PersonalRecord>()

            coEvery { personalRecordDao.getRecentPRs(10) } returns expectedPRs // Default limit

            // Act
            val result = service.getRecentPRs()

            // Assert
            assertThat(result).isEmpty()
            coVerify { personalRecordDao.getRecentPRs(10) }
        }

    @Test
    fun `checkForPR doesnt duplicate 1RM PR when weight PR includes same 1RM`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 3,
                    actualRpe = 9f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock exercise variation
            val variation =
                mockk<ExerciseVariation> {
                    coEvery { rmScalingType } returns RMScalingType.STANDARD
                }
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns variation

            // Previous best: both weight and 1RM are lower
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns 95f
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns 105f

            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert - Should only get weight PR, not duplicate 1RM PR
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].estimated1RM).isNotNull() // Weight PR includes estimated 1RM
        }

    @Test
    fun `checkForPR handles null exercise variation gracefully`() =
        runTest {
            // Arrange
            val setLog =
                WorkoutFixtures.createSetLog(
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = 8f,
                    isCompleted = true,
                )
            val exerciseVariationId = "1"

            // Mock null exercise variation
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseVariationId) } returns null

            // Mock previous best weight
            coEvery { personalRecordDao.getMaxWeightForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getMaxEstimated1RMForExercise(exerciseVariationId) } returns null
            coEvery { personalRecordDao.getLatestPRForExerciseAndType(exerciseVariationId, any()) } returns null

            // Mock workout data
            coEvery { setLogDao.getWorkoutDateForSetLog(setLog.id) } returns LocalDateTime.now().toString()
            coEvery { setLogDao.getWorkoutIdForSetLog(setLog.id) } returns "1"

            // Act
            val result = service.checkForPR(setLog, exerciseVariationId)

            // Assert - Should still work with default STANDARD scaling
            assertThat(result).hasSize(1)
            assertThat(result[0].recordType).isEqualTo(PRType.WEIGHT)
            assertThat(result[0].weight).isEqualTo(100f)
            assertThat(result[0].estimated1RM).isNotNull() // Should use standard formula
        }
}
