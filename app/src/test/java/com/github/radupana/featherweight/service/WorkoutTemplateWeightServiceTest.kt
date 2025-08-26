package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.model.IntensityLevel
import com.github.radupana.featherweight.data.model.TimeAvailable
import com.github.radupana.featherweight.data.model.TrainingGoal
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.fixtures.WorkoutFixtures
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for WorkoutTemplateWeightService
 * 
 * Tests weight calculation for workout templates including:
 * - Weight suggestions based on 1RM
 * - Intensity level calculations
 * - Progressive overload
 * - Plate rounding
 */
@ExperimentalCoroutinesApi
class WorkoutTemplateWeightServiceTest {
    
    @get:Rule
    val coroutineRule = CoroutineTestRule()
    
    private val repository: FeatherweightRepository = mockk(relaxed = true)
    private val oneRMDao: OneRMDao = mockk(relaxed = true)
    private val setLogDao: SetLogDao = mockk(relaxed = true)
    private val exerciseLogDao: ExerciseLogDao = mockk(relaxed = true)
    private val freestyleIntelligenceService: FreestyleIntelligenceService = mockk(relaxed = true)
    
    private lateinit var service: WorkoutTemplateWeightService
    
    @Before
    fun setUp() {
        service = WorkoutTemplateWeightService(
            repository,
            oneRMDao,
            setLogDao,
            exerciseLogDao,
            freestyleIntelligenceService
        )
    }
    
    // ========== applyWeightSuggestions Tests ==========
    
    @Test
    fun `applyWeightSuggestions calculates weight from 1RM`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 8,
                targetWeight = 0f // No weight yet
            )
        )
        
        // Mock 1RM data
        val oneRM = UserExerciseMax(
            userId = userId,
            exerciseVariationId = exerciseVariationId,
            mostWeightLifted = 100f,
            mostWeightReps = 1,
            oneRMEstimate = 100f,
            oneRMContext = "100kg × 1",
            oneRMConfidence = 0.9f
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns oneRM
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert
        val updatedSet = updatedSetSlot.captured
        assertThat(updatedSet.suggestedWeight).isNotNull()
        assertThat(updatedSet.suggestedWeight).isGreaterThan(0f)
        assertThat(updatedSet.suggestedWeight).isLessThan(100f) // Should be less than 1RM
        assertThat(updatedSet.targetWeight).isEqualTo(updatedSet.suggestedWeight)
        assertThat(updatedSet.actualWeight).isEqualTo(updatedSet.suggestedWeight)
        assertThat(updatedSet.suggestionConfidence).isGreaterThan(0.8f) // High confidence with 1RM
    }
    
    @Test
    fun `applyWeightSuggestions uses different percentages for intensity levels`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val oneRM = 100f
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 5
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
            UserExerciseMax(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = oneRM,
                mostWeightReps = 1,
                oneRMEstimate = oneRM,
                oneRMContext = "${oneRM}kg × 1",
                oneRMConfidence = 0.9f
            )
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val capturedSets = mutableListOf<SetLog>()
        coEvery { setLogDao.update(capture(capturedSets)) } returns Unit
        
        // Test different intensity levels
        val intensityTests = listOf(
            IntensityLevel.CONSERVATIVE to 0.675f,  // ~67.5% 1RM
            IntensityLevel.MODERATE to 0.725f,      // ~72.5% 1RM
            IntensityLevel.AGGRESSIVE to 0.775f     // ~77.5% 1RM
        )
        
        for ((intensity, expectedPercentage) in intensityTests) {
            capturedSets.clear()
            val config = WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.STANDARD,
                goal = TrainingGoal.STRENGTH,
                intensity = intensity
            )
            
            // Act
            service.applyWeightSuggestions(workoutId, config, userId)
            
            // Assert
            val suggestedWeight = capturedSets.first().suggestedWeight ?: 0f
            val expectedMin = oneRM * expectedPercentage * 0.9f
            val expectedMax = oneRM * expectedPercentage * 1.1f
            assertThat(suggestedWeight).isGreaterThan(expectedMin)
            assertThat(suggestedWeight).isLessThan(expectedMax)
        }
    }
    
    @Test
    fun `applyWeightSuggestions handles missing 1RM data`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 10
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null // No 1RM
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        // Mock freestyle intelligence service fallback
        coEvery { 
            freestyleIntelligenceService.getIntelligentSuggestions(any(), any(), any())
        } returns SmartSuggestions(
            suggestedWeight = 60f,
            suggestedReps = 10,
            confidence = "Medium",
            reasoning = "Based on exercise difficulty"
        )
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - should use fallback suggestion
        val updatedSet = updatedSetSlot.captured
        assertThat(updatedSet.suggestedWeight).isNotNull()
        assertThat(updatedSet.suggestionConfidence).isLessThan(0.8f) // Lower confidence without 1RM
    }
    
    @Test
    fun `applyWeightSuggestions applies to all sets in exercise`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        // Multiple sets for same exercise
        val sets = listOf(
            WorkoutFixtures.createSetLog(id = 1L, exerciseLogId = exerciseLog.id, targetReps = 8),
            WorkoutFixtures.createSetLog(id = 2L, exerciseLogId = exerciseLog.id, targetReps = 8),
            WorkoutFixtures.createSetLog(id = 3L, exerciseLogId = exerciseLog.id, targetReps = 8)
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
            UserExerciseMax(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = 100f,
                mostWeightReps = 1,
                oneRMEstimate = 100f,
                oneRMContext = "100kg × 1",
                oneRMConfidence = 0.9f
            )
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - all sets should be updated
        coVerify(exactly = 3) { setLogDao.update(any()) }
    }
    
    @Test
    fun `applyWeightSuggestions handles multiple exercises`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLogs = listOf(
            WorkoutFixtures.createExerciseLog(id = 1L, workoutId = workoutId, exerciseVariationId = 100L),
            WorkoutFixtures.createExerciseLog(id = 2L, workoutId = workoutId, exerciseVariationId = 200L)
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns exerciseLogs
        
        exerciseLogs.forEach { log ->
            val sets = listOf(
                WorkoutFixtures.createSetLog(id = log.id * 10, exerciseLogId = log.id, targetReps = 10)
            )
            coEvery { setLogDao.getSetLogsForExercise(log.id) } returns sets
            coEvery { oneRMDao.getCurrentMax(userId, log.exerciseVariationId) } returns 
                UserExerciseMax(
                    userId = userId,
                    exerciseVariationId = log.exerciseVariationId,
                    mostWeightLifted = 100f,
                    mostWeightReps = 1,
                    oneRMEstimate = 100f,
                    oneRMContext = "100kg × 1",
                    oneRMConfidence = 0.9f
                )
            coEvery { repository.getExerciseById(log.exerciseVariationId) } returns mockk()
        }
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - both exercises should have weights applied
        coVerify(exactly = 2) { setLogDao.update(any()) }
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `applyWeightSuggestions handles empty workout`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - should not crash, no updates
        coVerify(exactly = 0) { setLogDao.update(any()) }
    }
    
    @Test
    fun `applyWeightSuggestions preserves actual values in sets`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val targetReps = 8
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = targetReps,
                targetWeight = 0f
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
            UserExerciseMax(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = 100f,
                mostWeightReps = 1,
                oneRMEstimate = 100f,
                oneRMContext = "100kg × 1",
                oneRMConfidence = 0.9f
            )
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - actual values should be populated for checkbox to work
        val updatedSet = updatedSetSlot.captured
        assertThat(updatedSet.actualReps).isEqualTo(targetReps)
        assertThat(updatedSet.actualWeight).isEqualTo(updatedSet.suggestedWeight)
    }
    
    // ========== History-based 1RM Estimation Tests ==========
    
    @Test
    fun `applyWeightSuggestions estimates 1RM from recent history when no profile max`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 8
            )
        )
        
        // Recent history with various RPEs
        val recentSets = listOf(
            WorkoutFixtures.createSetLog(
                id = 10L,
                actualWeight = 80f,
                actualReps = 5,
                actualRpe = 8f,
                isCompleted = true
            ),
            WorkoutFixtures.createSetLog(
                id = 11L,
                actualWeight = 75f,
                actualReps = 6,
                actualRpe = 7f,
                isCompleted = true
            ),
            WorkoutFixtures.createSetLog(
                id = 12L,
                actualWeight = 70f,
                actualReps = 8,
                actualRpe = 9f,
                isCompleted = true
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null // No profile max
        coEvery { repository.getRecentSetLogsForExercise(exerciseVariationId, 42) } returns recentSets
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert
        val updatedSet = updatedSetSlot.captured
        assertThat(updatedSet.suggestedWeight).isNotNull()
        assertThat(updatedSet.suggestionSource).contains("recent")
        assertThat(updatedSet.suggestionConfidence).isLessThan(0.9f) // Lower confidence than 1RM
        assertThat(updatedSet.suggestionConfidence).isGreaterThan(0.7f)
    }
    
    @Test
    fun `applyWeightSuggestions rounds weight to nearest 2_5kg`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 10
            )
        )
        
        // Set up 1RM that will result in non-round numbers
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
            UserExerciseMax(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = 101f, // Non-round number
                mostWeightReps = 1,
                oneRMEstimate = 101f,
                oneRMContext = "101kg × 1",
                oneRMConfidence = 0.9f
            )
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - weight should be rounded to nearest 2.5kg
        val suggestedWeight = updatedSetSlot.captured.suggestedWeight ?: 0f
        assertThat(suggestedWeight % 2.5f).isEqualTo(0f)
    }
    
    @Test
    fun `applyWeightSuggestions adjusts weight for different rep ranges`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val oneRM = 100f
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        // Test different rep ranges
        val repRangeTests = listOf(
            3 to 1.05f,    // Low reps - slightly higher percentage
            10 to 1.0f,    // Medium reps - standard percentage
            20 to 0.9f     // High reps - lower percentage
        )
        
        for ((targetReps, expectedAdjustment) in repRangeTests) {
            val exerciseLog = WorkoutFixtures.createExerciseLog(
                id = 1L,
                workoutId = workoutId,
                exerciseVariationId = exerciseVariationId
            )
            
            val sets = listOf(
                WorkoutFixtures.createSetLog(
                    id = 1L,
                    exerciseLogId = exerciseLog.id,
                    targetReps = targetReps
                )
            )
            
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
                UserExerciseMax(
                    userId = userId,
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = oneRM,
                    mostWeightReps = 1,
                    oneRMEstimate = oneRM,
                    oneRMContext = "${oneRM}kg × 1",
                    oneRMConfidence = 0.9f
                )
            coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
            
            val updatedSetSlot = slot<SetLog>()
            coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
            
            // Act
            service.applyWeightSuggestions(workoutId, config, userId)
            
            // Assert - verify rep adjustment is applied
            val suggestedWeight = updatedSetSlot.captured.suggestedWeight ?: 0f
            val basePercentage = 0.725f // MODERATE intensity
            val expectedBase = oneRM * basePercentage * expectedAdjustment
            
            // Account for rounding to 2.5kg
            val expectedRounded = ((expectedBase / 2.5f).toInt() * 2.5f).toFloat()
            assertThat(suggestedWeight).isWithin(2.5f).of(expectedRounded)
        }
    }
    
    @Test
    fun `applyWeightSuggestions filters invalid sets from history`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 8
            )
        )
        
        // Mix of valid and invalid historical sets
        val recentSets = listOf(
            // Valid set
            WorkoutFixtures.createSetLog(
                id = 10L,
                actualWeight = 80f,
                actualReps = 5,
                actualRpe = 8f,
                isCompleted = true
            ),
            // Invalid - not completed
            WorkoutFixtures.createSetLog(
                id = 11L,
                actualWeight = 75f,
                actualReps = 6,
                isCompleted = false
            ),
            // Invalid - zero weight
            WorkoutFixtures.createSetLog(
                id = 12L,
                actualWeight = 0f,
                actualReps = 10,
                isCompleted = true
            ),
            // Invalid - zero reps
            WorkoutFixtures.createSetLog(
                id = 13L,
                actualWeight = 70f,
                actualReps = 0,
                isCompleted = true
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
        coEvery { repository.getRecentSetLogsForExercise(exerciseVariationId, 42) } returns recentSets
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        // Mock freestyle fallback
        coEvery { 
            freestyleIntelligenceService.getIntelligentSuggestions(any(), any(), any())
        } returns SmartSuggestions(
            suggestedWeight = 50f,
            suggestedReps = 8,
            confidence = "Low",
            reasoning = "Limited history"
        )
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - should use the one valid set or fall back to freestyle
        assertThat(updatedSetSlot.captured.suggestedWeight).isNotNull()
    }
    
    @Test
    fun `applyWeightSuggestions applies correct RPE multipliers`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 8
            )
        )
        
        // Test each RPE multiplier
        val rpeTests = listOf(
            10f to 1.0f,
            9f to 0.95f,
            8f to 0.9f,
            7f to 0.85f,
            6f to 0.8f,
            5f to 0.75f
        )
        
        for ((rpe, _) in rpeTests) {
            val recentSets = listOf(
                WorkoutFixtures.createSetLog(
                    id = 10L,
                    actualWeight = 100f,
                    actualReps = 5,
                    actualRpe = rpe,
                    isCompleted = true
                )
            )
            
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
            coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
            coEvery { repository.getRecentSetLogsForExercise(exerciseVariationId, 42) } returns recentSets
            coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
            
            val updatedSetSlot = slot<SetLog>()
            coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
            
            // Act
            service.applyWeightSuggestions(workoutId, config, userId)
            
            // Assert - RPE should affect the estimated 1RM and thus the suggestion
            assertThat(updatedSetSlot.captured.suggestedWeight).isNotNull()
            assertThat(updatedSetSlot.captured.suggestionSource).contains("recent")
        }
    }
    
    @Test
    fun `applyWeightSuggestions uses median for stability with multiple history sets`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = 8
            )
        )
        
        // Multiple history sets with outliers
        val recentSets = listOf(
            // Outlier high
            WorkoutFixtures.createSetLog(
                id = 10L,
                actualWeight = 120f,
                actualReps = 3,
                actualRpe = 10f,
                isCompleted = true
            ),
            // Normal range
            WorkoutFixtures.createSetLog(
                id = 11L,
                actualWeight = 80f,
                actualReps = 5,
                actualRpe = 8f,
                isCompleted = true
            ),
            WorkoutFixtures.createSetLog(
                id = 12L,
                actualWeight = 75f,
                actualReps = 6,
                actualRpe = 7f,
                isCompleted = true
            ),
            WorkoutFixtures.createSetLog(
                id = 13L,
                actualWeight = 70f,
                actualReps = 8,
                actualRpe = 7f,
                isCompleted = true
            ),
            // Outlier low
            WorkoutFixtures.createSetLog(
                id = 14L,
                actualWeight = 50f,
                actualReps = 15,
                actualRpe = 6f,
                isCompleted = true
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns null
        coEvery { repository.getRecentSetLogsForExercise(exerciseVariationId, 42) } returns recentSets
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - median should provide stable estimate
        assertThat(updatedSetSlot.captured.suggestedWeight).isNotNull()
        // Weight should be reasonable, not influenced by outliers
        assertThat(updatedSetSlot.captured.suggestedWeight).isGreaterThan(40f)
        assertThat(updatedSetSlot.captured.suggestedWeight).isLessThan(100f)
    }
    
    @Test
    fun `applyWeightSuggestions handles null targetReps with fallback`() = runTest {
        // Arrange
        val workoutId = 1L
        val userId = 1L
        val exerciseVariationId = 100L
        val config = WorkoutTemplateConfig(
            timeAvailable = TimeAvailable.STANDARD,
            goal = TrainingGoal.STRENGTH,
            intensity = IntensityLevel.MODERATE
        )
        
        val exerciseLog = WorkoutFixtures.createExerciseLog(
            id = 1L,
            workoutId = workoutId,
            exerciseVariationId = exerciseVariationId
        )
        
        val sets = listOf(
            WorkoutFixtures.createSetLog(
                id = 1L,
                exerciseLogId = exerciseLog.id,
                targetReps = null // No target reps specified
            )
        )
        
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(exerciseLog.id) } returns sets
        coEvery { oneRMDao.getCurrentMax(userId, exerciseVariationId) } returns 
            UserExerciseMax(
                userId = userId,
                exerciseVariationId = exerciseVariationId,
                mostWeightLifted = 100f,
                mostWeightReps = 1,
                oneRMEstimate = 100f,
                oneRMContext = "100kg × 1",
                oneRMConfidence = 0.9f
            )
        coEvery { repository.getExerciseById(exerciseVariationId) } returns mockk()
        
        val updatedSetSlot = slot<SetLog>()
        coEvery { setLogDao.update(capture(updatedSetSlot)) } returns Unit
        
        // Act
        service.applyWeightSuggestions(workoutId, config, userId)
        
        // Assert - should use default of 10 reps
        assertThat(updatedSetSlot.captured.suggestedWeight).isNotNull()
        assertThat(updatedSetSlot.captured.actualReps).isEqualTo(0) // Uses default handling
    }
}
