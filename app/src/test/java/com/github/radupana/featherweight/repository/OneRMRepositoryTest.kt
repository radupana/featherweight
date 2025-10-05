package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class OneRMRepositoryTest {
    private lateinit var repository: OneRMRepository
    private lateinit var database: FeatherweightDatabase
    private lateinit var oneRMDao: ExerciseMaxTrackingDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var application: Application
    private lateinit var authManager: AuthenticationManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0

        // Mock application and database
        application = mockk<Application>(relaxed = true)
        database = mockk()
        oneRMDao = mockk()
        exerciseDao = mockk()
        userExerciseUsageDao = mockk()
        authManager = mockk(relaxed = true)
        every { authManager.getCurrentUserId() } returns "test-user"

        // Setup database to return DAOs
        every { database.exerciseMaxTrackingDao() } returns oneRMDao
        every { database.exerciseDao() } returns exerciseDao
        every { database.userExerciseUsageDao() } returns userExerciseUsageDao

        // Create repository with mocked dependencies
        repository = OneRMRepository(application, authManager, testDispatcher, database)
    }

    @Test
    fun `clearPendingOneRMUpdates should clear all pending updates`() =
        runTest(testDispatcher) {
            // Given - add some pending updates first
            val update1 = createPendingOneRMUpdate("1", 100f, 110f)
            val update2 = createPendingOneRMUpdate("2", 150f, 160f)
            repository.addPendingOneRMUpdate(update1)
            repository.addPendingOneRMUpdate(update2)

            // When
            repository.clearPendingOneRMUpdates()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(repository.pendingOneRMUpdates.value).isEmpty()
        }

    @Test
    fun `applyOneRMUpdate should update max and remove from pending`() =
        runTest(testDispatcher) {
            // Given
            val update = createPendingOneRMUpdate("1", 100f, 112.5f, source = "3×100kg @ RPE 9")
            val exercise = createExercise("1", "Barbell Bench Press")
            val currentMax = createOneRMTracking("1", 100f)

            coEvery { oneRMDao.getCurrentMax("1", "test-user") } returns currentMax
            coEvery { exerciseDao.getExerciseById("1") } returns exercise
            coEvery { oneRMDao.update(any()) } returns Unit
            coEvery { oneRMDao.insert(any()) } just runs

            repository.addPendingOneRMUpdate(update)

            // When
            repository.applyOneRMUpdate(update)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedMax = slot<ExerciseMaxTracking>()
            coVerify { oneRMDao.update(capture(capturedMax)) }
            assertThat(capturedMax.captured.oneRMEstimate).isEqualTo(112.5f)
            assertThat(capturedMax.captured.notes).isEqualTo("Updated from 3×100kg @ RPE 9")

            coVerify(exactly = 0) { oneRMDao.insert(any()) }
            assertThat(repository.pendingOneRMUpdates.value).isEmpty()
        }

    @Test
    fun `getCurrentMaxesForExercises should return map of exercise maxes`() =
        runTest(testDispatcher) {
            // Given
            val exerciseIds = listOf("1", "2", "3")
            val maxes =
                listOf(
                    createOneRMTracking("1", 100f),
                    createOneRMTracking("2", 150f),
                    createOneRMTracking("3", 80f),
                )
            coEvery { oneRMDao.getCurrentMaxesForExercises(exerciseIds, "test-user") } returns maxes

            // When
            val result = repository.getCurrentMaxesForExercises(exerciseIds)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).containsExactly(
                "1",
                100f,
                "2",
                150f,
                "3",
                80f,
            )
        }

    @Test
    fun `getOneRMForExercise should return max for specific exercise`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val exerciseMax = createOneRMTracking(exerciseId, 120f)
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns exerciseMax

            // When
            val result = repository.getOneRMForExercise(exerciseId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isEqualTo(120f)
        }

    @Test
    fun `getOneRMForExercise should return null when no max exists`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "999"
            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns null

            // When
            val result = repository.getOneRMForExercise(exerciseId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertThat(result).isNull()
        }

    @Test
    fun `deleteAllForExercise should delete all maxes`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            coEvery { oneRMDao.deleteAllForExercise(exerciseId, "test-user") } returns Unit

            // When
            repository.deleteAllMaxesForExercise(exerciseId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { oneRMDao.deleteAllForExercise(exerciseId, "test-user") }
        }

    @Test
    fun `updateOrInsertOneRM should update existing record when new PR`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val existingMax = createOneRMTracking(exerciseId, 100f).copy(id = "5")
            val newRecord = createOneRMTracking(exerciseId, 110f)
            val exercise = createExercise(exerciseId, "Barbell Squat")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns existingMax
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exercise
            coEvery { oneRMDao.update(any()) } returns Unit
            coEvery { oneRMDao.insert(any()) } just runs

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedRecord = slot<ExerciseMaxTracking>()
            coVerify { oneRMDao.update(capture(capturedRecord)) }
            assertThat(capturedRecord.captured.id).isEqualTo("5")
            assertThat(capturedRecord.captured.oneRMEstimate).isEqualTo(110f)
            coVerify(exactly = 0) { oneRMDao.insert(any()) }
        }

    @Test
    fun `updateOrInsertOneRM should not update when not a PR`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val existingMax = createOneRMTracking(exerciseId, 120f)
            val newRecord = createOneRMTracking(exerciseId, 110f) // Lower than existing
            val exercise = createExercise(exerciseId, "Barbell Deadlift")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns existingMax
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exercise

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { oneRMDao.update(any()) }
            coVerify(exactly = 0) { oneRMDao.insert(any()) }
        }

    @Test
    fun `updateOrInsertOneRM should insert new record when none exists`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val newRecord = createOneRMTracking(exerciseId, 100f)
            val exercise = createExercise(exerciseId, "Barbell Row")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns null
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns exercise
            coEvery { oneRMDao.insert(any()) } just runs
            coEvery { oneRMDao.insert(any()) } just runs

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedRecord = slot<ExerciseMaxTracking>()
            coVerify { oneRMDao.insert(capture(capturedRecord)) }
            assertThat(capturedRecord.captured.oneRMEstimate).isEqualTo(100f)
            coVerify { oneRMDao.insert(any()) }
        }

    @Test
    fun `addPendingOneRMUpdate should replace existing update for same exercise`() =
        runTest(testDispatcher) {
            // Given
            val update1 = createPendingOneRMUpdate("1", 100f, 110f)
            val update2 = createPendingOneRMUpdate("1", 100f, 115f) // Same exercise, different value
            val update3 = createPendingOneRMUpdate("2", 80f, 90f) // Different exercise

            // When
            repository.addPendingOneRMUpdate(update1)
            repository.addPendingOneRMUpdate(update3)
            repository.addPendingOneRMUpdate(update2) // Should replace update1
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val pendingUpdates = repository.pendingOneRMUpdates.value
            assertThat(pendingUpdates).hasSize(2)
            assertThat(pendingUpdates.find { it.exerciseId == "1" }?.suggestedMax).isEqualTo(115f)
            assertThat(pendingUpdates.find { it.exerciseId == "2" }?.suggestedMax).isEqualTo(90f)
        }

    // Helper functions
    private fun createPendingOneRMUpdate(
        exerciseId: String,
        currentMax: Float?,
        suggestedMax: Float,
        confidence: Float = 0.8f,
        source: String = "Test Source",
    ) = PendingOneRMUpdate(
        exerciseId = exerciseId,
        currentMax = currentMax,
        suggestedMax = suggestedMax,
        confidence = confidence,
        source = source,
        workoutDate = LocalDateTime.now(),
    )

    private fun createOneRMTracking(
        exerciseId: String,
        oneRMEstimate: Float,
    ) = ExerciseMaxTracking(
        id = "0",
        userId = "test-user",
        exerciseId = exerciseId,
        oneRMEstimate = oneRMEstimate,
        context = "Test context",
        sourceSetId = null,
        oneRMConfidence = 0.9f,
        recordedAt = LocalDateTime.now(),
        mostWeightLifted = oneRMEstimate * 0.9f,
        mostWeightReps = 3,
        mostWeightRpe = null,
        mostWeightDate = LocalDateTime.now(),
        oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.AUTOMATICALLY_CALCULATED,
        notes = null,
    )

    private fun createExercise(
        id: String,
        name: String,
    ) = Exercise(
        id = id,
        name = name,
        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST.name,
        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.PUSH.name,
        isCompound = true,
        equipment = Equipment.BARBELL.name,
        difficulty = ExerciseDifficulty.INTERMEDIATE.name,
        requiresWeight = true,
    )
}
