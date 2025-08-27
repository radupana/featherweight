package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
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
    fun `seedRealisticWorkouts creates workouts for valid config`() = runTest {
        // Arrange
        val config = WorkoutSeedingService.SeedConfig(
            numberOfWeeks = 2,
            workoutsPerWeek = 3,
            includeAccessories = true
        )
        
        val mockWorkout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED
        )
        
        coEvery { mockRepository.getCurrentUserId() } returns 1L
        coEvery { mockRepository.getExercise1RM(any(), any()) } returns null
        coEvery { mockRepository.getExerciseByName(any()) } returns null
        coEvery { mockRepository.insertWorkout(any()) } returns 1L
        coEvery { mockRepository.getWorkoutForDate(any()) } returns null
        coEvery { mockRepository.insertExerciseLog(any()) } returns 1L
        coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
        coEvery { mockRepository.insertSetLog(any()) } returns 1L
        coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
        coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
        coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit
        
        // Act
        val result = service.seedRealisticWorkouts(config)
        
        // Assert
        assertThat(result).isEqualTo(6) // 2 weeks * 3 workouts per week
        coVerify(exactly = 6) { mockRepository.insertWorkout(any()) }
    }
    
    @Test
    fun `seedRealisticWorkouts throws error when no user selected`() = runTest {
        // Arrange
        val config = WorkoutSeedingService.SeedConfig(numberOfWeeks = 1)
        coEvery { mockRepository.getCurrentUserId() } returns -1L
        
        // Act & Assert
        try {
            service.seedRealisticWorkouts(config)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("No user selected")
        }
    }
    
    @Test
    fun `seedRealisticWorkouts uses default 1RMs when none exist`() = runTest {
        // Arrange
        val config = WorkoutSeedingService.SeedConfig(
            numberOfWeeks = 1,
            workoutsPerWeek = 1
        )
        
        val mockWorkout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED
        )
        
        coEvery { mockRepository.getCurrentUserId() } returns 1L
        coEvery { mockRepository.getExercise1RM("Barbell Back Squat", 1L) } returns null
        coEvery { mockRepository.getExercise1RM("Barbell Bench Press", 1L) } returns null
        coEvery { mockRepository.getExercise1RM("Barbell Deadlift", 1L) } returns null
        coEvery { mockRepository.getExercise1RM("Barbell Overhead Press", 1L) } returns null
        coEvery { mockRepository.getExerciseByName(any()) } returns null
        coEvery { mockRepository.insertWorkout(any()) } returns 1L
        coEvery { mockRepository.getWorkoutForDate(any()) } returns null
        coEvery { mockRepository.insertExerciseLog(any()) } returns 1L
        coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
        coEvery { mockRepository.insertSetLog(any()) } returns 1L
        coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
        coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
        coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit
        
        // Act
        val result = service.seedRealisticWorkouts(config)
        
        // Assert
        assertThat(result).isGreaterThan(0)
        coVerify { mockRepository.getExercise1RM("Barbell Back Squat", 1L) }
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
        val mainLifts = listOf(
            WorkoutSeedingService.PlannedExercise(
                name = "Squat",
                sets = listOf(WorkoutSeedingService.PlannedSet(5, 8))
            )
        )
        val accessories = listOf(
            WorkoutSeedingService.PlannedExercise(
                name = "Leg Press",
                sets = listOf(WorkoutSeedingService.PlannedSet(10, 7))
            )
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
    fun `seedRealisticWorkouts skips existing workout dates`() = runTest {
        // Arrange
        val config = WorkoutSeedingService.SeedConfig(
            numberOfWeeks = 1,
            workoutsPerWeek = 2
        )
        
        val existingWorkout = Workout(
            id = 1L,
            date = LocalDateTime.now().minusDays(1),
            status = WorkoutStatus.COMPLETED
        )
        
        val mockWorkout = Workout(
            id = 2L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED
        )
        
        coEvery { mockRepository.getCurrentUserId() } returns 1L
        coEvery { mockRepository.getExercise1RM(any(), any()) } returns null
        coEvery { 
            mockRepository.getWorkoutForDate(LocalDate.now().minusDays(1)) 
        } returns existingWorkout
        coEvery { mockRepository.getWorkoutForDate(any()) } returns null
        coEvery { mockRepository.insertWorkout(any()) } returns 2L
        coEvery { mockRepository.getExerciseByName(any()) } returns null
        coEvery { mockRepository.insertExerciseLog(any()) } returns 1L
        coEvery { mockRepository.incrementExerciseUsageCount(any()) } returns Unit
        coEvery { mockRepository.insertSetLog(any()) } returns 1L
        coEvery { mockRepository.getWorkoutById(any()) } returns mockWorkout
        coEvery { mockRepository.getExerciseLogsForWorkout(any()) } returns emptyList()
        coEvery { mockRepository.completeWorkout(any(), any()) } returns Unit
        
        // Act
        val result = service.seedRealisticWorkouts(config)
        
        // Assert
        assertThat(result).isGreaterThan(0)
        coVerify { mockRepository.getWorkoutForDate(any()) }
    }
}
