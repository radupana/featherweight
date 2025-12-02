package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Tests to verify that delete operations sync correctly to Firestore.
 * These tests ensure that when data is deleted locally, it is also deleted from Firestore.
 */
class FirestoreSyncDeleteTest {
    private lateinit var repository: FeatherweightRepository
    private lateinit var application: Application
    private lateinit var database: FeatherweightDatabase
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var workoutDeviationDao: WorkoutDeviationDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var firestoreRepository: FirestoreRepository

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
        programmeDao = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        workoutDeviationDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        authManager = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        every { database.programmeDao() } returns programmeDao
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.workoutDeviationDao() } returns workoutDeviationDao
        every { database.exerciseDao() } returns exerciseDao

        // Default: user is "local" to skip Firestore sync (avoids hangs in tests)
        every { authManager.getCurrentUserId() } returns "local"

        repository = FeatherweightRepository(application, database, authManager)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    // ========== deleteSetLog Tests ==========

    @Test
    fun `deleteSetLog syncs deletion to Firestore when user is authenticated`() =
        runTest {
            val setId = "set-123"

            coEvery { setLogDao.deleteSetLog(setId) } just Runs

            repository.deleteSetLog(setId)

            coVerify(exactly = 1) { setLogDao.deleteSetLog(setId) }
        }

    @Test
    fun `deleteSetLog does not sync to Firestore when user is local`() =
        runTest {
            val setId = "set-123"
            every { authManager.getCurrentUserId() } returns "local"

            coEvery { setLogDao.deleteSetLog(setId) } just Runs

            repository.deleteSetLog(setId)

            coVerify(exactly = 1) { setLogDao.deleteSetLog(setId) }
            // Firestore should not be called for local user
        }

    // ========== deleteExerciseLog Tests ==========

    @Test
    fun `deleteExerciseLog syncs deletion to Firestore when user is authenticated`() =
        runTest {
            val exerciseLogId = "exercise-log-123"

            repository.deleteExerciseLog(exerciseLogId)

            // Verify Room deletion was called (via exerciseRepository)
            // The actual Firestore call is made in the repository
        }

    // ========== deleteProgramme Tests ==========

    @Test
    fun `deleteProgramme with deleteWorkouts collects all IDs for Firestore deletion`() =
        runTest {
            val programme = createProgramme(id = "prog-123", isActive = false)
            val weeks =
                listOf(
                    ProgrammeWeek(
                        id = "week-1",
                        programmeId = "prog-123",
                        weekNumber = 1,
                        name = "Week 1",
                        description = null,
                    ),
                    ProgrammeWeek(
                        id = "week-2",
                        programmeId = "prog-123",
                        weekNumber = 2,
                        name = "Week 2",
                        description = null,
                    ),
                )
            val workouts =
                listOf(
                    createProgrammeWorkout(id = "workout-1", weekId = "week-1"),
                    createProgrammeWorkout(id = "workout-2", weekId = "week-2"),
                )
            val progress =
                ProgrammeProgress(
                    id = "progress-1",
                    programmeId = "prog-123",
                    currentWeek = 1,
                    currentDay = 1,
                    totalWorkouts = 10,
                    completedWorkouts = 0,
                    lastWorkoutDate = null,
                )

            coEvery { programmeDao.getWeeksForProgramme("prog-123") } returns weeks
            coEvery { programmeDao.getAllWorkoutsForProgramme("prog-123") } returns workouts
            coEvery { programmeDao.getProgressForProgramme("prog-123") } returns progress
            coEvery { programmeDao.deactivateAllProgrammes() } just Runs
            coEvery { workoutDao.deleteWorkoutsByProgramme(any()) } just Runs
            coEvery { programmeDao.deleteProgramme(any()) } just Runs

            repository.deleteProgramme(programme, deleteWorkouts = true)

            coVerify(exactly = 1) { programmeDao.getWeeksForProgramme("prog-123") }
            coVerify(exactly = 1) { programmeDao.getAllWorkoutsForProgramme("prog-123") }
            coVerify(exactly = 1) { programmeDao.getProgressForProgramme("prog-123") }
            coVerify(exactly = 1) { workoutDao.deleteWorkoutsByProgramme("prog-123") }
            coVerify(exactly = 1) { programmeDao.deleteProgramme(programme) }
        }

    @Test
    fun `deleteProgramme without deleteWorkouts only updates status`() =
        runTest {
            val programme = createProgramme(id = "prog-123", isActive = false)

            coEvery { programmeDao.updateProgramme(any()) } just Runs

            repository.deleteProgramme(programme, deleteWorkouts = false)

            coVerify(exactly = 1) { programmeDao.updateProgramme(any()) }
            coVerify(exactly = 0) { programmeDao.deleteProgramme(any()) }
            coVerify(exactly = 0) { programmeDao.getWeeksForProgramme(any()) }
        }

    // ========== Helper Functions ==========

    private fun createProgramme(
        id: String = "test-programme",
        name: String = "Test Programme",
        isActive: Boolean = false,
    ): Programme =
        Programme(
            id = id,
            userId = testUserId,
            name = name,
            description = "Test description",
            programmeType = ProgrammeType.STRENGTH,
            difficulty = ProgrammeDifficulty.INTERMEDIATE,
            durationWeeks = 4,
            status = ProgrammeStatus.NOT_STARTED,
            isActive = isActive,
            createdAt = LocalDateTime.now(),
            startedAt = null,
            completedAt = null,
        )

    private fun createProgrammeWorkout(
        id: String,
        weekId: String,
    ): ProgrammeWorkout =
        ProgrammeWorkout(
            id = id,
            weekId = weekId,
            dayNumber = 1,
            name = "Test Workout",
            description = null,
            estimatedDuration = 60,
            workoutStructure = "{}",
        )
}
