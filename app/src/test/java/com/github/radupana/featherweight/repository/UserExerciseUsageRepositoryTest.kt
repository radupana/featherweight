package com.github.radupana.featherweight.repository

import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.exercise.VariationMuscleDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class UserExerciseUsageRepositoryTest {
    private lateinit var repository: ExerciseRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var mockVariation: ExerciseVariation

    private val testUserId = "test-user-id"

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        // Setup mocks
        db = mockk()
        exerciseLogDao = mockk()
        userExerciseUsageDao = mockk()
        authManager = mockk()

        // Setup other required DAOs
        val exerciseDao = mockk<ExerciseDao>()
        val setLogDao = mockk<SetLogDao>()
        val exerciseSwapHistoryDao = mockk<ExerciseSwapHistoryDao>()
        val exerciseCoreDao = mockk<ExerciseCoreDao>()
        val exerciseVariationDao = mockk<ExerciseVariationDao>()
        val variationMuscleDao = mockk<VariationMuscleDao>()

        // Setup database mock
        every { db.exerciseDao() } returns exerciseDao
        every { db.exerciseLogDao() } returns exerciseLogDao
        every { db.setLogDao() } returns setLogDao
        every { db.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { db.exerciseCoreDao() } returns exerciseCoreDao
        every { db.exerciseVariationDao() } returns exerciseVariationDao
        every { db.variationMuscleDao() } returns variationMuscleDao
        every { db.userExerciseUsageDao() } returns userExerciseUsageDao

        // Setup auth manager
        every { authManager.getCurrentUserId() } returns testUserId

        repository = ExerciseRepository(db, authManager)

        // Setup test data
        mockVariation =
            ExerciseVariation(
                id = 1L,
                coreExerciseId = 1L,
                name = "Barbell Bench Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            )
    }

    @Test
    fun `insertExerciseLogWithExerciseReference increments usage count for authenticated user`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just Runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            // Assert
            assertThat(result).isEqualTo(5L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = testUserId,
                    variationId = 1L,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference increments usage for unauthenticated user with local userId`() =
        runTest {
            // Arrange
            val mockAuthManager = mockk<AuthenticationManager>(relaxed = true)
            every { mockAuthManager.getCurrentUserId() } returns null
            val unauthRepository = ExerciseRepository(db, mockAuthManager)
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just Runs

            // Act
            val result =
                unauthRepository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            // Assert
            assertThat(result).isEqualTo(5L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = 1L,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference handles usage increment error gracefully`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any())
            } throws RuntimeException("Database error")

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            // Assert - Should still return the exercise log ID even if usage tracking fails
            assertThat(result).isEqualTo(5L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = testUserId,
                    variationId = 1L,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference passes correct timestamp to usage tracking`() =
        runTest {
            // Arrange
            val timestampSlot = slot<LocalDateTime>()
            val beforeTime = LocalDateTime.now()

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any(), capture(timestampSlot))
            } just Runs

            // Act
            repository.insertExerciseLogWithExerciseReference(
                workoutId = 1L,
                exerciseVariation = mockVariation,
                exerciseOrder = 1,
            )

            val afterTime = LocalDateTime.now()

            // Assert - Timestamp should be between before and after test execution
            assertThat(timestampSlot.captured).isNotNull()
            assertThat(timestampSlot.captured.isAfter(beforeTime.minusSeconds(1))).isTrue()
            assertThat(timestampSlot.captured.isBefore(afterTime.plusSeconds(1))).isTrue()
        }

    @Test
    fun `insertExerciseLogWithExerciseReference correctly identifies custom exercises`() =
        runTest {
            // Arrange - Custom exercise detection is handled in FeatherweightRepository
            // This test verifies ExerciseRepository behavior which always uses isCustom=false
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just Runs

            // Act
            repository.insertExerciseLogWithExerciseReference(
                workoutId = 1L,
                exerciseVariation = mockVariation,
                exerciseOrder = 1,
            )

            // Assert
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = testUserId,
                    variationId = 1L,
                    isCustom = false, // Always false in ExerciseRepository
                    timestamp = any(),
                )
            }
        }
}
