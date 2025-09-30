package com.github.radupana.featherweight.repository

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
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

    private lateinit var exerciseCoreDao: ExerciseCoreDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
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

        exerciseCoreDao = mockk(relaxed = true)
        exerciseVariationDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        userExerciseUsageDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)

        repository =
            CustomExerciseRepository(
                exerciseCoreDao = exerciseCoreDao,
                exerciseVariationDao = exerciseVariationDao,
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
            val exerciseId = "123"
            val customExerciseName = "Radu Squat"
            val customExercise =
                createCustomExercise(
                    id = exerciseId,
                    name = customExerciseName,
                )

            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseId) } returns customExercise

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { exerciseVariationDao.getExerciseVariationById(exerciseId) }
            assertThat(result).isNotNull()
            assertThat(result?.id).isEqualTo(exerciseId)
            assertThat(result?.name).isEqualTo(customExerciseName)
        }

    @Test
    fun `getCustomExerciseById should return null when exercise not found`() =
        runTest {
            // Arrange
            val exerciseId = "456"
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseId) } returns null

            // Act
            val result = repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify { exerciseVariationDao.getExerciseVariationById(exerciseId) }
            assertThat(result).isNull()
        }

    @Test
    fun `getCustomExerciseById should call correct DAO method`() =
        runTest {
            // Arrange
            val exerciseId = "789"
            coEvery { exerciseVariationDao.getExerciseVariationById(exerciseId) } returns null

            // Act
            repository.getCustomExerciseById(exerciseId)

            // Assert
            coVerify(exactly = 1) { exerciseVariationDao.getExerciseVariationById(exerciseId) }
        }

    private fun createCustomExercise(
        id: String,
        name: String,
        userId: String = "local",
    ) = ExerciseVariation(
        id = id,
        userId = userId,
        coreExerciseId = "1",
        name = name,
        equipment = Equipment.BARBELL,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        requiresWeight = true,
        recommendedRepRange = "8-12",
        rmScalingType = RMScalingType.STANDARD,
        restDurationSeconds = 90,
    )
}
