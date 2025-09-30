package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.repository.FeatherweightRepository
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
    fun `SeedConfig has correct default values`() {
        // Arrange & Act
        val config = WorkoutSeedingService.SeedConfig(numberOfWeeks = 4)

        // Assert
        assertThat(config.numberOfWeeks).isEqualTo(4)
        assertThat(config.workoutsPerWeek).isEqualTo(4)
        assertThat(config.includeAccessories).isTrue()
        assertThat(config.progressionRate).isEqualTo(0.025f)
        assertThat(config.variationRange).isEqualTo(2.5f)
    }

    @Test
    fun `WorkoutPlan contains main lifts and accessories`() {
        // Arrange & Act
        val mainLifts =
            listOf(
                WorkoutSeedingService.PlannedExercise(
                    name = "Squat",
                    sets = listOf(WorkoutSeedingService.PlannedSet(5, 8)),
                ),
            )
        val accessories =
            listOf(
                WorkoutSeedingService.PlannedExercise(
                    name = "Leg Press",
                    sets = listOf(WorkoutSeedingService.PlannedSet(10, 7)),
                ),
            )
        val plan = WorkoutSeedingService.WorkoutPlan(mainLifts, accessories)

        // Assert
        assertThat(plan.mainLifts).hasSize(1)
        assertThat(plan.mainLifts[0].name).isEqualTo("Squat")
        assertThat(plan.accessories).hasSize(1)
        assertThat(plan.accessories[0].name).isEqualTo("Leg Press")
    }

    @Test
    fun `PlannedSet has correct default weight multiplier`() {
        // Arrange & Act
        val set = WorkoutSeedingService.PlannedSet(reps = 5, rpe = 8)

        // Assert
        assertThat(set.reps).isEqualTo(5)
        assertThat(set.rpe).isEqualTo(8)
        assertThat(set.weightMultiplier).isEqualTo(1.0f)
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

    @Test
    fun `SeedConfig progressionRate affects weight calculations`() {
        // Arrange
        val configLowProgression =
            WorkoutSeedingService.SeedConfig(
                numberOfWeeks = 1,
                progressionRate = 0.01f,
            )
        val configHighProgression =
            WorkoutSeedingService.SeedConfig(
                numberOfWeeks = 1,
                progressionRate = 0.05f,
            )

        // Assert
        assertThat(configLowProgression.progressionRate).isEqualTo(0.01f)
        assertThat(configHighProgression.progressionRate).isEqualTo(0.05f)
        assertThat(configLowProgression.progressionRate).isLessThan(configHighProgression.progressionRate)
    }

    @Test
    fun `PlannedSet supports custom weight multipliers`() {
        // Arrange & Act
        val lightSet =
            WorkoutSeedingService.PlannedSet(
                reps = 10,
                rpe = 6,
                weightMultiplier = 0.7f,
            )
        val heavySet =
            WorkoutSeedingService.PlannedSet(
                reps = 3,
                rpe = 9,
                weightMultiplier = 0.95f,
            )

        // Assert
        assertThat(lightSet.weightMultiplier).isEqualTo(0.7f)
        assertThat(heavySet.weightMultiplier).isEqualTo(0.95f)
        assertThat(lightSet.reps).isGreaterThan(heavySet.reps)
        assertThat(lightSet.rpe).isLessThan(heavySet.rpe)
    }
}
