package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class FeatherweightRepositoryTest {
    private lateinit var repository: FeatherweightRepository
    private lateinit var application: Application
    private lateinit var database: FeatherweightDatabase
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var workoutDeviationDao: WorkoutDeviationDao

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

        application = mockk(relaxed = true)
        database = mockk(relaxed = true)
        programmeDao = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        workoutDeviationDao = mockk(relaxed = true)

        every { database.programmeDao() } returns programmeDao
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.workoutDeviationDao() } returns workoutDeviationDao

        repository = FeatherweightRepository(application, database, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    @Test
    fun `deleteProgramme with deleteWorkouts false archives programme and preserves workouts`() =
        runTest {
            val programme = createProgramme(id = "prog1", name = "Test Programme", isActive = false)
            val programmeSlot = slot<Programme>()

            coEvery { programmeDao.updateProgramme(capture(programmeSlot)) } just Runs

            repository.deleteProgramme(programme, deleteWorkouts = false)

            coVerify(exactly = 1) { programmeDao.updateProgramme(any()) }
            coVerify(exactly = 0) { workoutDao.deleteWorkoutsByProgramme(any()) }
            coVerify(exactly = 0) { programmeDao.deleteProgramme(any()) }

            assertThat(programmeSlot.captured.status).isEqualTo(ProgrammeStatus.CANCELLED)
            assertThat(programmeSlot.captured.isActive).isFalse()
            assertThat(programmeSlot.captured.startedAt).isNotNull()
            assertThat(programmeSlot.captured.completedAt).isNull()
        }

    @Test
    fun `deleteProgramme with deleteWorkouts true removes workouts and programme`() =
        runTest {
            val programme = createProgramme(id = "prog1", name = "Test Programme", isActive = false)

            coEvery { workoutDao.deleteWorkoutsByProgramme(any()) } just Runs
            coEvery { programmeDao.deleteProgramme(any()) } just Runs

            repository.deleteProgramme(programme, deleteWorkouts = true)

            coVerify(exactly = 1) { workoutDao.deleteWorkoutsByProgramme(programme.id) }
            coVerify(exactly = 1) { programmeDao.deleteProgramme(programme) }
            coVerify(exactly = 0) { programmeDao.updateProgrammeStatus(any(), any(), any()) }
            coVerify(exactly = 0) { programmeDao.updateProgramme(any()) }
        }

    @Test
    fun `deleteProgramme with deleteWorkouts true deactivates active programme first`() =
        runTest {
            val activeProgramme = createProgramme(id = "prog1", name = "Active Programme", isActive = true)

            coEvery { programmeDao.deactivateAllProgrammes() } just Runs
            coEvery { workoutDao.deleteWorkoutsByProgramme(any()) } just Runs
            coEvery { programmeDao.deleteProgramme(any()) } just Runs

            repository.deleteProgramme(activeProgramme, deleteWorkouts = true)

            coVerify(exactly = 1) { programmeDao.deactivateAllProgrammes() }
            coVerify(exactly = 1) { workoutDao.deleteWorkoutsByProgramme(activeProgramme.id) }
            coVerify(exactly = 1) { programmeDao.deleteProgramme(activeProgramme) }
        }

    @Test
    fun `getCompletedWorkoutCountForProgramme returns count from dao`() =
        runTest {
            val programmeId = "prog1"
            val expectedCount = 42

            coEvery { workoutDao.getCompletedWorkoutCountByProgramme(programmeId) } returns expectedCount

            val result = repository.getCompletedWorkoutCountForProgramme(programmeId)

            assertThat(result).isEqualTo(expectedCount)
            coVerify(exactly = 1) { workoutDao.getCompletedWorkoutCountByProgramme(programmeId) }
        }

    @Test
    fun `getCompletedSetCountForProgramme returns count from dao`() =
        runTest {
            val programmeId = "prog1"
            val expectedCount = 256

            coEvery { workoutDao.getCompletedSetCountByProgramme(programmeId) } returns expectedCount

            val result = repository.getCompletedSetCountForProgramme(programmeId)

            assertThat(result).isEqualTo(expectedCount)
            coVerify(exactly = 1) { workoutDao.getCompletedSetCountByProgramme(programmeId) }
        }

    @Test
    fun `calculateAndSaveDeviations saves deviations for programme workout`() =
        runTest {
            val workoutId = "workout1"
            val programmeId = "prog1"
            val weekId = "week1"

            val workout =
                Workout(
                    id = workoutId,
                    date = java.time.LocalDateTime.now(),
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    weekNumber = 1,
                    dayNumber = 1,
                )

            val week = ProgrammeWeek(programmeId = programmeId, weekNumber = 1, name = "Week 1", description = "")

            val programmeWorkout =
                ProgrammeWorkout(
                    id = "pw1",
                    weekId = weekId,
                    dayNumber = 1,
                    name = "Day 1",
                    description = "",
                    estimatedDuration = 60,
                    workoutStructure =
                        """
                        {
                            "day": 1,
                            "name": "Day 1",
                            "exercises": [
                                {
                                    "name": "Squat",
                                    "sets": 3,
                                    "reps": 5,
                                    "weights": [100.0, 100.0, 100.0]
                                }
                            ]
                        }
                        """.trimIndent(),
                )

            val exerciseLog =
                ExerciseLog(
                    id = "ex1",
                    workoutId = workoutId,
                    exerciseId = "exercise1",
                    exerciseOrder = 0,
                )

            val completedSets =
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 0,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                )

            coEvery { workoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { programmeDao.getAllWorkoutsForProgramme(programmeId) } returns listOf(programmeWorkout)
            coEvery { programmeDao.getWeekById(weekId) } returns week
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns completedSets
            coEvery { workoutDeviationDao.insertAll(any()) } just Runs

            repository.calculateAndSaveDeviations(workoutId)

            coVerify(atLeast = 1) { workoutDeviationDao.insertAll(match { it.isNotEmpty() }) }
        }

    @Test
    fun `calculateAndSaveDeviations does not save when no deviations found`() =
        runTest {
            val workoutId = "workout1"

            val workout = Workout(id = workoutId, date = java.time.LocalDateTime.now(), isProgrammeWorkout = false)

            coEvery { workoutDao.getWorkoutById(workoutId) } returns workout

            repository.calculateAndSaveDeviations(workoutId)

            coVerify(exactly = 0) { workoutDeviationDao.insertAll(any()) }
        }

    private fun createProgramme(
        id: String,
        name: String,
        isActive: Boolean = false,
        status: ProgrammeStatus = ProgrammeStatus.NOT_STARTED,
    ) = Programme(
        id = id,
        name = name,
        description = "Test description",
        durationWeeks = 4,
        programmeType = ProgrammeType.STRENGTH,
        difficulty = ProgrammeDifficulty.INTERMEDIATE,
        userId = "test-user",
        isActive = isActive,
        status = status,
    )
}
