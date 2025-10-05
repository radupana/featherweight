package com.github.radupana.featherweight.repository

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseType
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

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseAliasDao: ExerciseAliasDao
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

        exerciseDao = mockk(relaxed = true)
        exerciseAliasDao = mockk(relaxed = true)
        userExerciseUsageDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)

        repository =
            CustomExerciseRepository(
                exerciseDao = exerciseDao,
                exerciseAliasDao = exerciseAliasDao,
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
            val exerciseId = "123"
            val customExerciseName = "Radu Squat"
            val customExercise =
                createCustomExercise(
                    id = exerciseId,
                    name = customExerciseName,
                )

            coEvery { exerciseDao.getExerciseById(exerciseId) } returns customExercise

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { exerciseDao.getExerciseById(exerciseId) }
            assertThat(result).isNotNull()
            assertThat(result?.id).isEqualTo(exerciseId)
            assertThat(result?.name).isEqualTo(customExerciseName)
        }

    @Test
    fun `getCustomExerciseById should return null when exercise not found`() =
        runTest {
            // Arrange
            val exerciseId = "456"
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns null

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { exerciseDao.getExerciseById(exerciseId) }
            assertThat(result).isNull()
        }

    @Test
    fun `getCustomExerciseById should call correct DAO method`() =
        runTest {
            // Arrange
            val exerciseId = "789"
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns null

            // Act
            repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify(exactly = 1) { exerciseDao.getExerciseById(exerciseId) }
        }

    private fun createCustomExercise(
        id: String,
        name: String,
        userId: String = "local",
    ) = Exercise(
        id = id,
        type = ExerciseType.USER.name,
        userId = userId,
        name = name,
        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST.name,
        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.PUSH.name,
        isCompound = true,
        equipment = Equipment.BARBELL.name,
        difficulty = ExerciseDifficulty.INTERMEDIATE.name,
        requiresWeight = true,
        rmScalingType = RMScalingType.STANDARD.name,
        restDurationSeconds = 90,
    )
}
