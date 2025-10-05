package com.github.radupana.featherweight.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.UserExerciseUsage
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.ExerciseNamingService
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Tests for ExerciseSelectorViewModel focusing on usage stats functionality
 * for both authenticated and unauthenticated users.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseSelectorViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var application: Application
    private lateinit var mockRepository: FeatherweightRepository
    private lateinit var mockNamingService: ExerciseNamingService
    private lateinit var viewModel: ExerciseSelectorViewModel

    @Before
    fun setup() {
        application = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockNamingService = mockk(relaxed = true)

        // Setup default behaviors
        coEvery { mockRepository.getAllExercises() } returns emptyList()
        coEvery { mockRepository.getCustomExercises() } returns emptyList()
        coEvery { mockRepository.getMusclesForVariation(any()) } returns emptyList()
        coEvery { mockRepository.getAliasesForExercise(any()) } returns emptyList()
        coEvery { mockRepository.getCurrentUserId() } returns null
        coEvery { mockRepository.getUserExerciseUsage(any(), any()) } returns null
    }

    @Test
    fun `unauthenticated users should fetch usage stats with local userId`() =
        runTest {
            // Arrange
            val exercise = createTestExercise(id = "1", name = "Barbell Squat")
            val usageStats = createUsageStats(exerciseId = "1", usageCount = 5)

            coEvery { mockRepository.getAllExercises() } returns listOf(exercise)
            coEvery { mockRepository.getCurrentUserId() } returns null // Unauthenticated
            coEvery { mockRepository.getUserExerciseUsage("local", "1") } returns usageStats

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Verify the repository was called with "local" userId
            coVerify { mockRepository.getUserExerciseUsage("local", "1") }

            // Assert - Check the exercises have correct usage count
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(1)
            assertThat(exercises[0].usageCount).isEqualTo(5)
        }

    @Test
    fun `authenticated users should fetch usage stats with actual userId`() =
        runTest {
            // Arrange
            val userId = "firebase_user_123"
            val exercise = createTestExercise(id = "1", name = "Barbell Deadlift")
            val usageStats = createUsageStats(exerciseId = "1", usageCount = 10, userId = userId)

            coEvery { mockRepository.getAllExercises() } returns listOf(exercise)
            coEvery { mockRepository.getCurrentUserId() } returns userId
            coEvery { mockRepository.getUserExerciseUsage(userId, "1") } returns usageStats

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Verify the repository was called with actual userId
            coVerify { mockRepository.getUserExerciseUsage(userId, "1") }

            // Assert - Check the exercises have correct usage count
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(1)
            assertThat(exercises[0].usageCount).isEqualTo(10)
        }

    @Test
    fun `exercises are sorted by usage count when not searching`() =
        runTest {
            // Arrange
            val exercise1 = createTestExercise(id = "1", name = "Squat")
            val exercise2 = createTestExercise(id = "2", name = "Deadlift")
            val exercise3 = createTestExercise(id = "3", name = "Bench Press")

            coEvery { mockRepository.getAllExercises() } returns listOf(exercise1, exercise2, exercise3)
            coEvery { mockRepository.getCurrentUserId() } returns null

            // Setup different usage counts
            coEvery { mockRepository.getUserExerciseUsage("local", "1") } returns
                createUsageStats("1", 3)
            coEvery { mockRepository.getUserExerciseUsage("local", "2") } returns
                createUsageStats("2", 10)
            coEvery { mockRepository.getUserExerciseUsage("local", "3") } returns
                createUsageStats("3", 5)

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Exercises should be sorted by usage count descending
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(3)
            assertThat(exercises[0].variation.name).isEqualTo("Deadlift") // 10 uses
            assertThat(exercises[0].usageCount).isEqualTo(10)
            assertThat(exercises[1].variation.name).isEqualTo("Bench Press") // 5 uses
            assertThat(exercises[1].usageCount).isEqualTo(5)
            assertThat(exercises[2].variation.name).isEqualTo("Squat") // 3 uses
            assertThat(exercises[2].usageCount).isEqualTo(3)
        }

    @Test
    fun `exercises with same usage count are sorted alphabetically`() =
        runTest {
            // Arrange
            val exercise1 = createTestExercise(id = "1", name = "Squat")
            val exercise2 = createTestExercise(id = "2", name = "Deadlift")
            val exercise3 = createTestExercise(id = "3", name = "Bench Press")

            coEvery { mockRepository.getAllExercises() } returns listOf(exercise1, exercise2, exercise3)
            coEvery { mockRepository.getCurrentUserId() } returns null

            // All have same usage count
            val sameUsageStats = createUsageStats("0", 5)
            coEvery { mockRepository.getUserExerciseUsage("local", any()) } returns sameUsageStats

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - With same usage count, should be alphabetical
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(3)
            assertThat(exercises[0].variation.name).isEqualTo("Bench Press")
            assertThat(exercises[1].variation.name).isEqualTo("Deadlift")
            assertThat(exercises[2].variation.name).isEqualTo("Squat")
        }

    @Test
    fun `custom exercises fetch usage stats correctly for unauthenticated users`() =
        runTest {
            // Arrange
            val customExercise = createCustomExercise(id = "100", name = "Cable Fly")
            val usageStats = createUsageStats("100", 7)

            coEvery { mockRepository.getAllExercises() } returns listOf(customExercise)
            coEvery { mockRepository.getCurrentUserId() } returns null
            coEvery { mockRepository.getUserExerciseUsage("local", "100") } returns usageStats

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Verify custom exercise usage was fetched with "local"
            coVerify { mockRepository.getUserExerciseUsage("local", "100") }

            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(1)
            assertThat(exercises[0].usageCount).isEqualTo(7)
            assertThat(exercises[0].isCustom).isTrue()
        }

    @Test
    fun `null usage stats defaults to zero count`() =
        runTest {
            // Arrange
            val exercise = createTestExercise(id = "1", name = "Test Exercise")

            coEvery { mockRepository.getAllExercises() } returns listOf(exercise)
            coEvery { mockRepository.getCurrentUserId() } returns null
            coEvery { mockRepository.getUserExerciseUsage(any(), any()) } returns null

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Should have 0 usage count when stats are null
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(1)
            assertThat(exercises[0].usageCount).isEqualTo(0)
        }

    @Test
    fun `getAllExercises returns both system and custom exercises without duplicates`() =
        runTest {
            // Arrange
            val systemExercise = createTestExercise(id = "1", name = "Barbell Squat")
            val customExercise = createCustomExercise(id = "2", name = "Radu Bench")

            // getAllExercises should return BOTH system and custom exercises
            coEvery { mockRepository.getAllExercises() } returns listOf(systemExercise, customExercise)
            coEvery { mockRepository.getCustomExercises() } returns listOf(customExercise)
            coEvery { mockRepository.getCurrentUserId() } returns null

            // Act
            viewModel = ExerciseSelectorViewModel(application, mockRepository, mockNamingService)
            viewModel.loadExercises()
            advanceUntilIdle()

            // Assert - Should have exactly 2 exercises, no duplicates
            val exercises = viewModel.filteredExercises.first()
            assertThat(exercises).hasSize(2)

            val exerciseIds = exercises.map { it.variation.id }
            assertThat(exerciseIds).containsExactly("1", "2")

            // Verify custom exercise appears only once
            val customExerciseMatches = exercises.filter { it.variation.name == "Radu Bench" }
            assertThat(customExerciseMatches).hasSize(1)
        }

    // Helper functions
    private fun createTestExercise(
        id: String,
        name: String,
    ) = Exercise(
        id = id,
        type = ExerciseType.SYSTEM.name,
        userId = null,
        name = name,
        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
        isCompound = true,
        equipment = Equipment.BARBELL.name,
        difficulty = ExerciseDifficulty.INTERMEDIATE.name,
        requiresWeight = true,
        rmScalingType = RMScalingType.STANDARD.name,
        restDurationSeconds = 90,
    )

    private fun createCustomExercise(
        id: String,
        name: String,
    ) = Exercise(
        id = id,
        type = ExerciseType.USER.name,
        userId = "local",
        name = name,
        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST.name,
        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.HORIZONTAL_PUSH.name,
        isCompound = false,
        equipment = Equipment.CABLE.name,
        difficulty = ExerciseDifficulty.INTERMEDIATE.name,
        requiresWeight = true,
        rmScalingType = RMScalingType.STANDARD.name,
        restDurationSeconds = 60,
    )

    private fun createUsageStats(
        exerciseId: String,
        usageCount: Int,
        userId: String = "local",
    ) = UserExerciseUsage(
        id = exerciseId,
        userId = userId,
        exerciseId = exerciseId,
        usageCount = usageCount,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
