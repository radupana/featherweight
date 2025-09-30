package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.UserExerciseMax
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
    private lateinit var oneRMDao: OneRMDao
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
        every { database.oneRMDao() } returns oneRMDao
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
            val exercise = createExerciseVariation("1", "Barbell Bench Press")
            val currentMax = createUserExerciseMax("1", 100f)

            coEvery { oneRMDao.getCurrentMax("1", "test-user") } returns currentMax
            coEvery { exerciseDao.getExerciseVariationById("1") } returns exercise
            coEvery { oneRMDao.updateExerciseMax(any()) } returns Unit
            coEvery { oneRMDao.insertOneRMHistory(any()) } just runs

            repository.addPendingOneRMUpdate(update)

            // When
            repository.applyOneRMUpdate(update)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedMax = slot<UserExerciseMax>()
            coVerify { oneRMDao.updateExerciseMax(capture(capturedMax)) }
            assertThat(capturedMax.captured.oneRMEstimate).isEqualTo(112.5f)
            assertThat(capturedMax.captured.notes).isEqualTo("Updated from 3×100kg @ RPE 9")

            coVerify { oneRMDao.insertOneRMHistory(any()) }
            assertThat(repository.pendingOneRMUpdates.value).isEmpty()
        }

    @Test
    fun `getCurrentMaxesForExercises should return map of exercise maxes`() =
        runTest(testDispatcher) {
            // Given
            val exerciseIds = listOf("1", "2", "3")
            val maxes =
                listOf(
                    createUserExerciseMax("1", 100f),
                    createUserExerciseMax("2", 150f),
                    createUserExerciseMax("3", 80f),
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
            val exerciseMax = createUserExerciseMax(exerciseId, 120f)
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
    fun `deleteAllMaxesForExercise should delete all maxes`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            coEvery { oneRMDao.deleteAllMaxesForExercise(exerciseId, "test-user") } returns Unit

            // When
            repository.deleteAllMaxesForExercise(exerciseId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { oneRMDao.deleteAllMaxesForExercise(exerciseId, "test-user") }
        }

    @Test
    fun `updateOrInsertOneRM should update existing record when new PR`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val existingMax = createUserExerciseMax(exerciseId, 100f).copy(id = "5")
            val newRecord = createUserExerciseMax(exerciseId, 110f)
            val exercise = createExerciseVariation(exerciseId, "Barbell Squat")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns existingMax
            coEvery { exerciseDao.getExerciseVariationById(exerciseId) } returns exercise
            coEvery { oneRMDao.updateExerciseMax(any()) } returns Unit
            coEvery { oneRMDao.insertOneRMHistory(any()) } just runs

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedRecord = slot<UserExerciseMax>()
            coVerify { oneRMDao.updateExerciseMax(capture(capturedRecord)) }
            assertThat(capturedRecord.captured.id).isEqualTo("5")
            assertThat(capturedRecord.captured.oneRMEstimate).isEqualTo(110f)
            coVerify { oneRMDao.insertOneRMHistory(any()) }
        }

    @Test
    fun `updateOrInsertOneRM should not update when not a PR`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val existingMax = createUserExerciseMax(exerciseId, 120f)
            val newRecord = createUserExerciseMax(exerciseId, 110f) // Lower than existing
            val exercise = createExerciseVariation(exerciseId, "Barbell Deadlift")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns existingMax
            coEvery { exerciseDao.getExerciseVariationById(exerciseId) } returns exercise

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { oneRMDao.updateExerciseMax(any()) }
            coVerify(exactly = 0) { oneRMDao.insertOneRMHistory(any()) }
        }

    @Test
    fun `updateOrInsertOneRM should insert new record when none exists`() =
        runTest(testDispatcher) {
            // Given
            val exerciseId = "1"
            val newRecord = createUserExerciseMax(exerciseId, 100f)
            val exercise = createExerciseVariation(exerciseId, "Barbell Row")

            coEvery { oneRMDao.getCurrentMax(exerciseId, "test-user") } returns null
            coEvery { exerciseDao.getExerciseVariationById(exerciseId) } returns exercise
            coEvery { oneRMDao.insertExerciseMax(any()) } just runs
            coEvery { oneRMDao.insertOneRMHistory(any()) } just runs

            // When
            repository.updateOrInsertOneRM(newRecord)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val capturedRecord = slot<UserExerciseMax>()
            coVerify { oneRMDao.insertExerciseMax(capture(capturedRecord)) }
            assertThat(capturedRecord.captured.oneRMEstimate).isEqualTo(100f)
            coVerify { oneRMDao.insertOneRMHistory(any()) }
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
            assertThat(pendingUpdates.find { it.exerciseVariationId == "1" }?.suggestedMax).isEqualTo(115f)
            assertThat(pendingUpdates.find { it.exerciseVariationId == "2" }?.suggestedMax).isEqualTo(90f)
        }

    // Helper functions
    private fun createPendingOneRMUpdate(
        exerciseId: String,
        currentMax: Float?,
        suggestedMax: Float,
        confidence: Float = 0.8f,
        source: String = "Test Source",
    ) = PendingOneRMUpdate(
        exerciseVariationId = exerciseId,
        currentMax = currentMax,
        suggestedMax = suggestedMax,
        confidence = confidence,
        source = source,
        workoutDate = LocalDateTime.now(),
    )

    private fun createUserExerciseMax(
        exerciseId: String,
        oneRMEstimate: Float,
    ) = UserExerciseMax(
        id = "0",
        exerciseVariationId = exerciseId,
        oneRMEstimate = oneRMEstimate,
        oneRMContext = "Test context",
        oneRMConfidence = 0.9f,
        oneRMDate = LocalDateTime.now(),
        mostWeightLifted = oneRMEstimate * 0.9f,
        mostWeightReps = 3,
        mostWeightDate = LocalDateTime.now(),
    )

    private fun createExerciseVariation(
        id: String,
        name: String,
    ) = ExerciseVariation(
        id = id,
        coreExerciseId = "1",
        name = name,
        equipment = Equipment.BARBELL,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        requiresWeight = true,
    )
}
