package com.github.radupana.featherweight.repository

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.radupana.featherweight.data.exercise.CustomExerciseDao
import com.github.radupana.featherweight.data.exercise.CustomExerciseVariation
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Tests for CustomExerciseRepository, specifically testing the getCustomExerciseById method
 * that was added to fix the "Unknown Exercise" issue in WorkoutViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomExerciseRepositoryGetByIdTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var customExerciseDao: CustomExerciseDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var repository: CustomExerciseRepository

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        customExerciseDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        userExerciseUsageDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)

        repository =
            CustomExerciseRepository(
                customExerciseDao = customExerciseDao,
                exerciseDao = exerciseDao,
                userExerciseUsageDao = userExerciseUsageDao,
                authManager = authManager,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `getCustomExerciseById should return custom exercise when found`() =
        runTest {
            // Arrange
            val exerciseId = 123L
            val customExerciseName = "Radu Squat"
            val customExercise =
                createCustomExercise(
                    id = exerciseId,
                    name = customExerciseName,
                )

            coEvery { customExerciseDao.getCustomVariationById(exerciseId) } returns customExercise

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { customExerciseDao.getCustomVariationById(exerciseId) }
            assertThat(result).isNotNull()
            assertThat(result?.id).isEqualTo(exerciseId)
            assertThat(result?.name).isEqualTo(customExerciseName)
        }

    @Test
    fun `getCustomExerciseById should return null when exercise not found`() =
        runTest {
            // Arrange
            val exerciseId = 456L
            coEvery { customExerciseDao.getCustomVariationById(exerciseId) } returns null

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { customExerciseDao.getCustomVariationById(exerciseId) }
            assertThat(result).isNull()
        }

    @Test
    fun `getCustomExerciseById should call correct DAO method`() =
        runTest {
            // Arrange
            val exerciseId = 789L
            coEvery { customExerciseDao.getCustomVariationById(exerciseId) } returns null

            // Act
            repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify(exactly = 1) { customExerciseDao.getCustomVariationById(exerciseId) }
        }

    private fun createCustomExercise(
        id: Long,
        name: String,
        userId: String = "local",
    ) = CustomExerciseVariation(
        id = id,
        userId = userId,
        customCoreExerciseId = 1,
        name = name,
        equipment = Equipment.BARBELL,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        requiresWeight = true,
        recommendedRepRange = "8-12",
        rmScalingType = RMScalingType.STANDARD,
        restDurationSeconds = 90,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
