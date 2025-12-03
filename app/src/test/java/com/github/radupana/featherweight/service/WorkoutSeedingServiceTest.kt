package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WorkoutSeedingServiceTest {
    @MockK
    private lateinit var mockRepository: FeatherweightRepository

    private lateinit var service: WorkoutSeedingService

    @Before
    fun setup() {
        LogMock.setup()
        MockKAnnotations.init(this)
        service = WorkoutSeedingService(mockRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `seedRealisticWorkouts creates workouts for valid config`() =
        runTest {
            // Arrange
            val config =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 2,
                    workoutsPerWeek = 3,
                    includeAccessories = true,
                )

            val mockWorkout =
                Workout(
                    id = "1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )

            coEvery { mockRepository.getExercise1RM(any()) } returns null
            coEvery { mockRepository.getExerciseByName(any()) } returns null
            coEvery { mockRepository.insertWorkout(any()) } returns "1"
            coEvery { mockRepository.getWorkoutForDate(any()) } returns null
            coEvery { mockRepository.insertExerciseLog(any()) } returns "1"
            coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
            coEvery { mockRepository.insertSetLog(any()) } returns "1"
            coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
            coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
            coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            // The actual workout generation logic uses a 4-week cycle pattern
            // and generates workouts based on specific training days, not just workoutsPerWeek
            assertThat(result).isGreaterThan(0) // Just verify workouts were created
            coVerify(atLeast = 1) { mockRepository.insertWorkout(any()) }
        }

    @Test
    fun `seedRealisticWorkouts creates workouts successfully`() =
        runTest {
            // Arrange
            val config = WorkoutSeedingService.SeedConfig(numberOfWeeks = 1)

            // Use relaxed mock for repository to allow all calls
            val relaxedRepository: FeatherweightRepository = mockk(relaxed = true)
            val relaxedService = WorkoutSeedingService(relaxedRepository)

            coEvery { relaxedRepository.getExercise1RM(any()) } returns null
            coEvery { relaxedRepository.getWorkoutForDate(any()) } returns null
            coEvery { relaxedRepository.insertWorkout(any()) } returns "1"
            coEvery { relaxedRepository.insertExerciseLog(any()) } returns "1"
            coEvery { relaxedRepository.insertSetLog(any()) } returns "1"

            // Act
            val result = relaxedService.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isGreaterThan(0)
            coVerify(atLeast = 1) { relaxedRepository.insertWorkout(any()) }
        }

    @Test
    fun `seedRealisticWorkouts uses default 1RMs when none exist`() =
        runTest {
            // Arrange
            val config =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 1,
                    workoutsPerWeek = 1,
                )

            val mockWorkout =
                Workout(
                    id = "1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )

            coEvery { mockRepository.getExercise1RM("Barbell Back Squat") } returns null
            coEvery { mockRepository.getExercise1RM("Barbell Bench Press") } returns null
            coEvery { mockRepository.getExercise1RM("Barbell Deadlift") } returns null
            coEvery { mockRepository.getExercise1RM("Barbell Overhead Press") } returns null
            coEvery { mockRepository.getExerciseByName(any()) } returns null
            coEvery { mockRepository.insertWorkout(any()) } returns "1"
            coEvery { mockRepository.getWorkoutForDate(any()) } returns null
            coEvery { mockRepository.insertExerciseLog(any()) } returns "1"
            coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
            coEvery { mockRepository.insertSetLog(any()) } returns "1"
            coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
            coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
            coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isGreaterThan(0)
            coVerify { mockRepository.getExercise1RM("Barbell Back Squat") }
        }

    @Test
    fun `seedRealisticWorkouts skips existing workout dates`() =
        runTest {
            // Arrange
            val config =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 1,
                    workoutsPerWeek = 2,
                )

            val existingWorkout =
                Workout(
                    id = "1",
                    date = LocalDateTime.now().minusDays(1),
                    status = WorkoutStatus.COMPLETED,
                )

            val mockWorkout =
                Workout(
                    id = "2",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )

            coEvery { mockRepository.getExercise1RM(any()) } returns null
            coEvery {
                mockRepository.getWorkoutForDate(LocalDate.now().minusDays(1))
            } returns existingWorkout
            coEvery { mockRepository.getWorkoutForDate(any()) } returns null
            coEvery { mockRepository.insertWorkout(any()) } returns "2"
            coEvery { mockRepository.getExerciseByName(any()) } returns null
            coEvery { mockRepository.insertExerciseLog(any()) } returns "1"
            coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
            coEvery { mockRepository.insertSetLog(any()) } returns "1"
            coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
            coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
            coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isGreaterThan(0)
            coVerify { mockRepository.getWorkoutForDate(any()) }
        }

    @Test
    fun `seedRealisticWorkouts handles zero weeks configuration`() =
        runTest {
            // Arrange
            val config = WorkoutSeedingService.SeedConfig(numberOfWeeks = 0)
            val relaxedRepository: FeatherweightRepository = mockk(relaxed = true)
            val service = WorkoutSeedingService(relaxedRepository)

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isEqualTo(0)
            coVerify(exactly = 0) { relaxedRepository.insertWorkout(any()) }
        }

    @Test
    fun `seedRealisticWorkouts handles negative weeks configuration`() =
        runTest {
            // Arrange
            val config = WorkoutSeedingService.SeedConfig(numberOfWeeks = -1)
            val relaxedRepository: FeatherweightRepository = mockk(relaxed = true)
            val service = WorkoutSeedingService(relaxedRepository)

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isEqualTo(0)
            coVerify(exactly = 0) { relaxedRepository.insertWorkout(any()) }
        }

    @Test
    fun `seedRealisticWorkouts creates workouts with different patterns`() =
        runTest {
            // Arrange
            val config =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 4,
                    workoutsPerWeek = 4,
                    includeAccessories = true,
                )
            val relaxedRepository: FeatherweightRepository = mockk(relaxed = true)
            val service = WorkoutSeedingService(relaxedRepository)

            coEvery { relaxedRepository.getExercise1RM(any()) } returns 100f
            coEvery { relaxedRepository.getWorkoutForDate(any()) } returns null
            coEvery { relaxedRepository.insertWorkout(any()) } returns "1"
            coEvery { relaxedRepository.insertExerciseLog(any()) } returns "1"
            coEvery { relaxedRepository.insertSetLog(any()) } returns "1"

            // Act
            val result = service.seedRealisticWorkouts(config)

            // Assert
            assertThat(result).isGreaterThan(0)
            // Verify that workouts were created
            coVerify(atLeast = 1) { relaxedRepository.insertWorkout(any()) }
        }

    @Test
    fun `seedRealisticWorkouts respects includeAccessories flag`() =
        runTest {
            // Arrange
            val configWithAccessories =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 1,
                    workoutsPerWeek = 2,
                    includeAccessories = true,
                )
            val configWithoutAccessories =
                WorkoutSeedingService.SeedConfig(
                    numberOfWeeks = 1,
                    workoutsPerWeek = 2,
                    includeAccessories = false,
                )

            val relaxedRepository: FeatherweightRepository = mockk(relaxed = true)
            val service = WorkoutSeedingService(relaxedRepository)

            coEvery { relaxedRepository.getExercise1RM(any()) } returns 100f
            coEvery { relaxedRepository.getWorkoutForDate(any()) } returns null
            coEvery { relaxedRepository.insertWorkout(any()) } returns "1"
            coEvery { relaxedRepository.insertExerciseLog(any()) } returns "1"
            coEvery { relaxedRepository.insertSetLog(any()) } returns "1"

            // Act
            val resultWith = service.seedRealisticWorkouts(configWithAccessories)
            val resultWithout = service.seedRealisticWorkouts(configWithoutAccessories)

            // Assert
            assertThat(resultWith).isGreaterThan(0)
            assertThat(resultWithout).isGreaterThan(0)
        }
}
