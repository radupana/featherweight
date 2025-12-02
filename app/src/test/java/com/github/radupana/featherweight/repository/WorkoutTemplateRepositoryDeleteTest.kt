package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.TemplateExercise
import com.github.radupana.featherweight.data.TemplateExerciseDao
import com.github.radupana.featherweight.data.TemplateSet
import com.github.radupana.featherweight.data.TemplateSetDao
import com.github.radupana.featherweight.data.WorkoutTemplateDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for WorkoutTemplateRepository deletion with Firestore sync.
 */
class WorkoutTemplateRepositoryDeleteTest {
    private lateinit var repository: WorkoutTemplateRepository
    private lateinit var application: Application
    private lateinit var database: FeatherweightDatabase
    private lateinit var templateDao: WorkoutTemplateDao
    private lateinit var templateExerciseDao: TemplateExerciseDao
    private lateinit var templateSetDao: TemplateSetDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var firestoreRepository: FirestoreRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testUserId = "test-user-123"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseFirestore::class)
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseFirestore.getInstance(any<String>()) } returns firestore

        application = mockk(relaxed = true)
        database = mockk(relaxed = true)
        templateDao = mockk(relaxed = true)
        templateExerciseDao = mockk(relaxed = true)
        templateSetDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        every { database.workoutTemplateDao() } returns templateDao
        every { database.templateExerciseDao() } returns templateExerciseDao
        every { database.templateSetDao() } returns templateSetDao
        every { database.workoutDao() } returns mockk(relaxed = true)
        every { database.exerciseLogDao() } returns mockk(relaxed = true)
        every { database.setLogDao() } returns mockk(relaxed = true)

        every { authManager.getCurrentUserId() } returns testUserId

        repository =
            WorkoutTemplateRepository(
                application,
                testDispatcher,
                database,
                authManager,
                firestoreRepository,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    @Test
    fun `deleteTemplate collects exercise and set IDs before deletion`() =
        runTest(testDispatcher) {
            val templateId = "template-123"
            val exercises =
                listOf(
                    TemplateExercise(id = "ex-1", userId = testUserId, templateId = templateId, exerciseId = "e1", exerciseOrder = 1),
                    TemplateExercise(id = "ex-2", userId = testUserId, templateId = templateId, exerciseId = "e2", exerciseOrder = 2),
                )
            val sets1 =
                listOf(
                    TemplateSet(id = "set-1", userId = testUserId, templateExerciseId = "ex-1", setOrder = 1, targetReps = 10),
                    TemplateSet(id = "set-2", userId = testUserId, templateExerciseId = "ex-1", setOrder = 2, targetReps = 10),
                )
            val sets2 =
                listOf(
                    TemplateSet(id = "set-3", userId = testUserId, templateExerciseId = "ex-2", setOrder = 1, targetReps = 8),
                )

            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns exercises
            coEvery { templateSetDao.getSetsForTemplateExercise("ex-1") } returns sets1
            coEvery { templateSetDao.getSetsForTemplateExercise("ex-2") } returns sets2
            coEvery { templateDao.deleteTemplate(templateId) } just Runs
            coEvery { firestoreRepository.deleteWorkoutTemplate(any(), any(), any(), any()) } returns Result.success(Unit)

            repository.deleteTemplate(templateId)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { templateExerciseDao.getExercisesForTemplate(templateId) }
            coVerify(exactly = 1) { templateSetDao.getSetsForTemplateExercise("ex-1") }
            coVerify(exactly = 1) { templateSetDao.getSetsForTemplateExercise("ex-2") }
            coVerify(exactly = 1) { templateDao.deleteTemplate(templateId) }
            coVerify(exactly = 1) {
                firestoreRepository.deleteWorkoutTemplate(
                    testUserId,
                    templateId,
                    listOf("ex-1", "ex-2"),
                    listOf("set-1", "set-2", "set-3"),
                )
            }
        }

    @Test
    fun `deleteTemplate does not call Firestore when user is local`() =
        runTest(testDispatcher) {
            val templateId = "template-123"
            every { authManager.getCurrentUserId() } returns "local"

            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns emptyList()
            coEvery { templateDao.deleteTemplate(templateId) } just Runs

            repository.deleteTemplate(templateId)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { templateDao.deleteTemplate(templateId) }
            coVerify(exactly = 0) { firestoreRepository.deleteWorkoutTemplate(any(), any(), any(), any()) }
        }

    @Test
    fun `deleteTemplate handles empty template gracefully`() =
        runTest(testDispatcher) {
            val templateId = "template-123"

            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns emptyList()
            coEvery { templateDao.deleteTemplate(templateId) } just Runs
            coEvery { firestoreRepository.deleteWorkoutTemplate(any(), any(), any(), any()) } returns Result.success(Unit)

            repository.deleteTemplate(templateId)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { templateDao.deleteTemplate(templateId) }
            coVerify(exactly = 1) {
                firestoreRepository.deleteWorkoutTemplate(
                    testUserId,
                    templateId,
                    emptyList(),
                    emptyList(),
                )
            }
        }

    @Test
    fun `deleteTemplate continues even if Firestore fails`() =
        runTest(testDispatcher) {
            val templateId = "template-123"

            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns emptyList()
            coEvery { templateDao.deleteTemplate(templateId) } just Runs
            coEvery { firestoreRepository.deleteWorkoutTemplate(any(), any(), any(), any()) } returns
                Result.failure(Exception("Network error"))

            repository.deleteTemplate(templateId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Room deletion should still complete
            coVerify(exactly = 1) { templateDao.deleteTemplate(templateId) }
        }
}
