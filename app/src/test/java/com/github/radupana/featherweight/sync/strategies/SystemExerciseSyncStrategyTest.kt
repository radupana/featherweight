package com.github.radupana.featherweight.sync.strategies

import android.text.TextUtils
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for SystemExerciseSyncStrategy.
 *
 * Note: These tests verify the strategy's behavioral contracts using relaxed mocks.
 * The production code uses Android Room's withTransaction which is not available in JVM tests.
 * We test the logic flow and error handling rather than transaction semantics.
 */
class SystemExerciseSyncStrategyTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var strategy: SystemExerciseSyncStrategy
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var exerciseAliasDao: ExerciseAliasDao
    private lateinit var exerciseInstructionDao: ExerciseInstructionDao

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            val str = arg<CharSequence?>(0)
            str == null || str.isEmpty()
        }

        database = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        exerciseMuscleDao = mockk(relaxed = true)
        exerciseAliasDao = mockk(relaxed = true)
        exerciseInstructionDao = mockk(relaxed = true)

        every { database.exerciseDao() } returns exerciseDao
        every { database.exerciseMuscleDao() } returns exerciseMuscleDao
        every { database.exerciseAliasDao() } returns exerciseAliasDao
        every { database.exerciseInstructionDao() } returns exerciseInstructionDao

        firestoreRepository = mockk(relaxed = true)
        strategy = SystemExerciseSyncStrategy(database, firestoreRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(TextUtils::class)
        clearAllMocks()
    }

    @Test
    fun `downloadAndMerge handles empty exercise list`() =
        runBlocking {
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())

            val result = strategy.downloadAndMerge(null, null)

            assertTrue(result.isSuccess)

            coVerify(exactly = 0) { exerciseDao.upsertExercises(any()) }
            coVerify(exactly = 0) { exerciseMuscleDao.deleteForExercises(any()) }
        }

    @Test
    fun `downloadAndMerge handles Firestore error`() =
        runBlocking {
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns
                Result.failure(com.google.firebase.FirebaseException("Network error"))

            val result = strategy.downloadAndMerge(null, null)

            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)

            coVerify(exactly = 0) { exerciseDao.upsertExercises(any()) }
        }

    @Test
    fun `uploadChanges does nothing for system exercises`() =
        runBlocking {
            val result = strategy.uploadChanges("userId", null)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `getDataType returns correct type`() {
        assertEquals("SystemExercises", strategy.getDataType())
    }
}
