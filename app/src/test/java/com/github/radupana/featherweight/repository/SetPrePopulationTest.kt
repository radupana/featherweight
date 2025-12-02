package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.TemplateExercise
import com.github.radupana.featherweight.data.TemplateExerciseDao
import com.github.radupana.featherweight.data.TemplateSet
import com.github.radupana.featherweight.data.TemplateSetDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutTemplate
import com.github.radupana.featherweight.data.WorkoutTemplateDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SetPrePopulationTest {
    private lateinit var repository: WorkoutTemplateRepository
    private lateinit var application: Application
    private lateinit var database: FeatherweightDatabase
    private lateinit var templateDao: WorkoutTemplateDao
    private lateinit var templateExerciseDao: TemplateExerciseDao
    private lateinit var templateSetDao: TemplateSetDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var firestoreRepository: FirestoreRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseFirestore::class)
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns firestore

        application = mockk(relaxed = true)
        database = mockk(relaxed = true)
        templateDao = mockk(relaxed = true)
        templateExerciseDao = mockk(relaxed = true)
        templateSetDao = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        every { database.workoutTemplateDao() } returns templateDao
        every { database.templateExerciseDao() } returns templateExerciseDao
        every { database.templateSetDao() } returns templateSetDao
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao

        coEvery { authManager.getCurrentUserId() } returns "test-user"

        repository =
            WorkoutTemplateRepository(
                application = application,
                ioDispatcher = testDispatcher,
                db = database,
                authManager = authManager,
                firestoreRepository = firestoreRepository,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    @Test
    fun `startWorkoutFromTemplate pre-populates actual values from target values`() =
        runTest(testDispatcher) {
            val templateId = "template1"
            val template =
                WorkoutTemplate(
                    id = templateId,
                    userId = "test-user",
                    name = "Test Template",
                    description = "Test",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            val templateExercise =
                TemplateExercise(
                    id = "tex1",
                    userId = "test-user",
                    templateId = templateId,
                    exerciseId = "exercise1",
                    exerciseOrder = 0,
                )

            val templateSet =
                TemplateSet(
                    id = "tset1",
                    userId = "test-user",
                    templateExerciseId = "tex1",
                    setOrder = 1,
                    targetReps = 8,
                    targetWeight = 100f,
                    targetRpe = 7.5f,
                )

            coEvery { templateDao.getTemplateById(templateId) } returns template
            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns listOf(templateExercise)
            coEvery { templateSetDao.getSetsForTemplateExercise("tex1") } returns listOf(templateSet)

            val capturedSetLog = slot<SetLog>()
            coEvery { setLogDao.insertSetLog(capture(capturedSetLog)) } returns Unit

            repository.startWorkoutFromTemplate(templateId)

            assertThat(capturedSetLog.captured.targetReps).isEqualTo(8)
            assertThat(capturedSetLog.captured.targetWeight).isEqualTo(100f)
            assertThat(capturedSetLog.captured.targetRpe).isEqualTo(7.5f)
            assertThat(capturedSetLog.captured.actualReps).isEqualTo(8)
            assertThat(capturedSetLog.captured.actualWeight).isEqualTo(100f)
            assertThat(capturedSetLog.captured.actualRpe).isEqualTo(7.5f)
        }

    @Test
    fun `startWorkoutFromTemplate handles bodyweight exercises with zero weight`() =
        runTest(testDispatcher) {
            val templateId = "template1"
            val template =
                WorkoutTemplate(
                    id = templateId,
                    userId = "test-user",
                    name = "Bodyweight Template",
                    description = "Test",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            val templateExercise =
                TemplateExercise(
                    id = "tex1",
                    userId = "test-user",
                    templateId = templateId,
                    exerciseId = "pullups",
                    exerciseOrder = 0,
                )

            val templateSet =
                TemplateSet(
                    id = "tset1",
                    userId = "test-user",
                    templateExerciseId = "tex1",
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 0f,
                    targetRpe = null,
                )

            coEvery { templateDao.getTemplateById(templateId) } returns template
            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns listOf(templateExercise)
            coEvery { templateSetDao.getSetsForTemplateExercise("tex1") } returns listOf(templateSet)

            val capturedSetLog = slot<SetLog>()
            coEvery { setLogDao.insertSetLog(capture(capturedSetLog)) } returns Unit

            repository.startWorkoutFromTemplate(templateId)

            assertThat(capturedSetLog.captured.targetReps).isEqualTo(10)
            assertThat(capturedSetLog.captured.targetWeight).isEqualTo(0f)
            assertThat(capturedSetLog.captured.targetRpe).isNull()
            assertThat(capturedSetLog.captured.actualReps).isEqualTo(10)
            assertThat(capturedSetLog.captured.actualWeight).isEqualTo(0f)
            assertThat(capturedSetLog.captured.actualRpe).isNull()
        }

    @Test
    fun `startWorkoutFromTemplate handles null target weight as zero`() =
        runTest(testDispatcher) {
            val templateId = "template1"
            val template =
                WorkoutTemplate(
                    id = templateId,
                    userId = "test-user",
                    name = "Template with null weight",
                    description = "Test",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )

            val templateExercise =
                TemplateExercise(
                    id = "tex1",
                    userId = "test-user",
                    templateId = templateId,
                    exerciseId = "exercise1",
                    exerciseOrder = 0,
                )

            val templateSet =
                TemplateSet(
                    id = "tset1",
                    userId = "test-user",
                    templateExerciseId = "tex1",
                    setOrder = 1,
                    targetReps = 12,
                    targetWeight = null,
                    targetRpe = 8f,
                )

            coEvery { templateDao.getTemplateById(templateId) } returns template
            coEvery { templateExerciseDao.getExercisesForTemplate(templateId) } returns listOf(templateExercise)
            coEvery { templateSetDao.getSetsForTemplateExercise("tex1") } returns listOf(templateSet)

            val capturedSetLog = slot<SetLog>()
            coEvery { setLogDao.insertSetLog(capture(capturedSetLog)) } returns Unit

            repository.startWorkoutFromTemplate(templateId)

            assertThat(capturedSetLog.captured.targetReps).isEqualTo(12)
            assertThat(capturedSetLog.captured.targetWeight).isNull()
            assertThat(capturedSetLog.captured.targetRpe).isEqualTo(8f)
            assertThat(capturedSetLog.captured.actualReps).isEqualTo(12)
            assertThat(capturedSetLog.captured.actualWeight).isEqualTo(0f)
            assertThat(capturedSetLog.captured.actualRpe).isEqualTo(8f)
        }
}
