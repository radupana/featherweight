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
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ImmutableProgrammeSnapshot
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.WeekSnapshot
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
import com.github.radupana.featherweight.data.programme.WorkoutSnapshot
import com.github.radupana.featherweight.data.programme.WorkoutStructure
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
    private lateinit var exerciseDao: ExerciseDao

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

        every { database.programmeDao() } returns programmeDao
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.workoutDeviationDao() } returns workoutDeviationDao
        every { database.exerciseDao() } returns exerciseDao

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
        runBlocking {
            val programme = createProgramme(id = "prog1", name = "Test Programme", isActive = false)

            // Mock the new methods required for Firestore sync
            coEvery { programmeDao.getWeeksForProgramme(any()) } returns emptyList()
            coEvery { programmeDao.getAllWorkoutsForProgramme(any()) } returns emptyList()
            coEvery { programmeDao.getProgressForProgramme(any()) } returns null
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
        runBlocking {
            val activeProgramme = createProgramme(id = "prog1", name = "Active Programme", isActive = true)

            // Mock the new methods required for Firestore sync
            coEvery { programmeDao.getWeeksForProgramme(any()) } returns emptyList()
            coEvery { programmeDao.getAllWorkoutsForProgramme(any()) } returns emptyList()
            coEvery { programmeDao.getProgressForProgramme(any()) } returns null
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

            val workout =
                Workout(
                    id = workoutId,
                    date = java.time.LocalDateTime.now(),
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    weekNumber = 1,
                    dayNumber = 1,
                )

            // Create immutable snapshot
            val workoutStructureJson =
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
                """.trimIndent()

            val parsedStructure = Json.decodeFromString<WorkoutStructure>(workoutStructureJson)

            val workoutSnapshot =
                WorkoutSnapshot(
                    dayNumber = 1,
                    workoutName = parsedStructure.name,
                    exercises = parsedStructure.exercises,
                )

            val weekSnapshot =
                WeekSnapshot(
                    weekNumber = 1,
                    workouts = listOf(workoutSnapshot),
                )

            val immutableSnapshot =
                ImmutableProgrammeSnapshot(
                    programmeId = programmeId,
                    programmeName = "Test Programme",
                    durationWeeks = 4,
                    capturedAt =
                        java.time.LocalDateTime
                            .now()
                            .toString(),
                    weeks = listOf(weekSnapshot),
                )

            val snapshotJson = Programme.encodeImmutableProgrammeSnapshot(immutableSnapshot)

            val programme =
                Programme(
                    id = programmeId,
                    name = "Test Programme",
                    description = "Test",
                    durationWeeks = 4,
                    programmeType = ProgrammeType.STRENGTH,
                    difficulty = ProgrammeDifficulty.INTERMEDIATE,
                    status = ProgrammeStatus.IN_PROGRESS,
                    isActive = true,
                    startedAt =
                        java.time.LocalDateTime
                            .now()
                            .minusWeeks(2),
                    immutableProgrammeJson = snapshotJson,
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
            coEvery { programmeDao.getProgrammeById(programmeId) } returns programme
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

    @Test
    fun `getDeviationsForWorkout returns deviations from dao`() =
        runTest {
            val workoutId = "workout1"
            val deviations =
                listOf(
                    WorkoutDeviation(
                        workoutId = workoutId,
                        programmeId = "prog1",
                        exerciseLogId = "ex1",
                        deviationType = DeviationType.VOLUME_DEVIATION,
                        deviationMagnitude = -0.2f,
                        timestamp = java.time.LocalDateTime.now(),
                    ),
                )

            coEvery { workoutDeviationDao.getDeviationsForWorkout(workoutId) } returns deviations

            val result = repository.getDeviationsForWorkout(workoutId)

            assertThat(result).isEqualTo(deviations)
            coVerify(exactly = 1) { workoutDeviationDao.getDeviationsForWorkout(workoutId) }
        }

    @Test
    fun `getExerciseLogsForWorkouts returns exercise logs for multiple workouts`() =
        runTest {
            val workoutIds = listOf("workout1", "workout2", "workout3")
            val exerciseLogs =
                listOf(
                    ExerciseLog(id = "ex1", workoutId = "workout1", exerciseId = "exercise1", exerciseOrder = 0),
                    ExerciseLog(id = "ex2", workoutId = "workout1", exerciseId = "exercise2", exerciseOrder = 1),
                    ExerciseLog(id = "ex3", workoutId = "workout2", exerciseId = "exercise1", exerciseOrder = 0),
                    ExerciseLog(id = "ex4", workoutId = "workout3", exerciseId = "exercise3", exerciseOrder = 0),
                )

            coEvery { exerciseLogDao.getExerciseLogsForWorkouts(workoutIds) } returns exerciseLogs

            val result = repository.getExerciseLogsForWorkouts(workoutIds)

            assertThat(result).hasSize(4)
            assertThat(result.map { it.workoutId }).containsExactly("workout1", "workout1", "workout2", "workout3")
            coVerify(exactly = 1) { exerciseLogDao.getExerciseLogsForWorkouts(workoutIds) }
        }

    @Test
    fun `getSetLogsForExercises returns set logs for multiple exercise logs`() =
        runTest {
            val exerciseLogIds = listOf("ex1", "ex2", "ex3")
            val setLogs =
                listOf(
                    SetLog(id = "set1", exerciseLogId = "ex1", setOrder = 0, actualReps = 5, actualWeight = 100f, isCompleted = true),
                    SetLog(id = "set2", exerciseLogId = "ex1", setOrder = 1, actualReps = 5, actualWeight = 100f, isCompleted = true),
                    SetLog(id = "set3", exerciseLogId = "ex2", setOrder = 0, actualReps = 8, actualWeight = 80f, isCompleted = true),
                    SetLog(id = "set4", exerciseLogId = "ex3", setOrder = 0, actualReps = 10, actualWeight = 60f, isCompleted = true),
                )

            coEvery { setLogDao.getSetLogsForExercises(exerciseLogIds) } returns setLogs

            val result = repository.getSetLogsForExercises(exerciseLogIds)

            assertThat(result).hasSize(4)
            assertThat(result.map { it.exerciseLogId }).containsExactly("ex1", "ex1", "ex2", "ex3")
            coVerify(exactly = 1) { setLogDao.getSetLogsForExercises(exerciseLogIds) }
        }

    @Test
    fun `getExercisesByIds returns exercises for multiple IDs`() =
        runTest {
            val exerciseIds = listOf("exercise1", "exercise2", "exercise3")
            val exercises =
                listOf(
                    createExercise(id = "exercise1", name = "Squat"),
                    createExercise(id = "exercise2", name = "Bench Press"),
                    createExercise(id = "exercise3", name = "Deadlift"),
                )

            coEvery { exerciseDao.getExercisesByIds(exerciseIds) } returns exercises

            val result = repository.getExercisesByIds(exerciseIds)

            assertThat(result).hasSize(3)
            assertThat(result.map { it.name }).containsExactly("Squat", "Bench Press", "Deadlift")
            coVerify(exactly = 1) { exerciseDao.getExercisesByIds(exerciseIds) }
        }

    @Test
    fun `getExerciseLogsForWorkouts returns empty list for empty input`() =
        runTest {
            val emptyWorkoutIds = emptyList<String>()

            coEvery { exerciseLogDao.getExerciseLogsForWorkouts(emptyWorkoutIds) } returns emptyList()

            val result = repository.getExerciseLogsForWorkouts(emptyWorkoutIds)

            assertThat(result).isEmpty()
            coVerify(exactly = 1) { exerciseLogDao.getExerciseLogsForWorkouts(emptyWorkoutIds) }
        }

    @Test
    fun `getSetLogsForExercises returns empty list for empty input`() =
        runTest {
            val emptyExerciseLogIds = emptyList<String>()

            coEvery { setLogDao.getSetLogsForExercises(emptyExerciseLogIds) } returns emptyList()

            val result = repository.getSetLogsForExercises(emptyExerciseLogIds)

            assertThat(result).isEmpty()
            coVerify(exactly = 1) { setLogDao.getSetLogsForExercises(emptyExerciseLogIds) }
        }

    @Test
    fun `getExercisesByIds returns empty list for empty input`() =
        runTest {
            val emptyExerciseIds = emptyList<String>()

            coEvery { exerciseDao.getExercisesByIds(emptyExerciseIds) } returns emptyList()

            val result = repository.getExercisesByIds(emptyExerciseIds)

            assertThat(result).isEmpty()
            coVerify(exactly = 1) { exerciseDao.getExercisesByIds(emptyExerciseIds) }
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

    private fun createExercise(
        id: String,
        name: String,
    ) = Exercise(
        id = id,
        name = name,
        type = ExerciseType.SYSTEM.name,
        category = "CHEST",
        movementPattern = "PUSH",
        equipment = "BARBELL",
    )
}
