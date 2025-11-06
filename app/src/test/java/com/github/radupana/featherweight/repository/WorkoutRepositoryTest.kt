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
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class WorkoutRepositoryTest {
    private lateinit var repository: WorkoutRepository
    private lateinit var database: FeatherweightDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var application: Application
    private lateinit var authManager: AuthenticationManager
    private val testDispatcher = StandardTestDispatcher()

    // Storage for mock data
    private val workouts = mutableMapOf<String, Workout>()
    private val exerciseLogs = mutableMapOf<String, ExerciseLog>()
    private val setLogs = mutableMapOf<String, SetLog>()
    private val programmes = mutableMapOf<String, Programme>()
    private var nextWorkoutIdCounter = 1
    private var nextExerciseIdCounter = 1
    private var nextSetIdCounter = 1
    private var nextProgrammeIdCounter = 1

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mock ServiceLocator for AuthenticationManager
        authManager = mockk(relaxed = true)
        every { authManager.getCurrentUserId() } returns "test-user-id"
        mockkObject(ServiceLocator)
        every { ServiceLocator.provideAuthenticationManager(any()) } returns authManager

        // Mock application and database
        application = mockk<Application>(relaxed = true)
        database = mockk()
        workoutDao = mockk()
        exerciseLogDao = mockk()
        setLogDao = mockk()
        programmeDao = mockk<ProgrammeDao>()

        // Setup database to return DAOs
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.programmeDao() } returns programmeDao

        // Setup DAO behaviors with in-memory storage
        setupWorkoutDaoMocks()
        setupExerciseLogDaoMocks()
        setupSetLogDaoMocks()
        setupProgrammeDaoMocks()

        // Create repository with mocked database
        repository = WorkoutRepository(application, testDispatcher, database, authManager)
    }

    private fun setupWorkoutDaoMocks() {
        coEvery { workoutDao.insertWorkout(any()) } answers {
            val workout = firstArg<Workout>()
            // DAO doesn't return ID anymore, the ID is already in the workout
            workouts[workout.id] = workout
        }

        coEvery { workoutDao.getWorkoutById(any()) } answers {
            val id = firstArg<String>()
            workouts[id]
        }

        coEvery { workoutDao.deleteWorkout(any()) } answers {
            val id = firstArg<String>()
            workouts.remove(id)
        }

        coEvery { workoutDao.updateWorkout(any()) } answers {
            val workout = firstArg<Workout>()
            workouts[workout.id] = workout
        }

        coEvery { workoutDao.getAllWorkouts(any()) } answers {
            val userId = firstArg<String>()
            workouts.values.filter { it.userId == userId }.toList()
        }

        coEvery { workoutDao.getWorkoutsByUserId(any()) } answers {
            val userId = firstArg<String>()
            workouts.values
                .filter {
                    it.userId == userId ||
                        it.userId == null ||
                        // For backward compatibility with tests that don't set userId
                        it.userId == "test-user-id" // Default test user
                }.toList()
        }

        coEvery { workoutDao.getRecentWorkouts(any(), any()) } answers {
            val userId = firstArg<String>()
            val limit = secondArg<Int>()
            workouts.values
                .filter { it.userId == userId }
                .sortedByDescending { it.date }
                .take(limit)
        }
    }

    private fun setupExerciseLogDaoMocks() {
        coEvery { exerciseLogDao.insertExerciseLog(any()) } answers {
            val exerciseLog = firstArg<ExerciseLog>()
            exerciseLogs[exerciseLog.id] = exerciseLog
        }

        coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } answers {
            val workoutId = firstArg<String>()
            exerciseLogs.values.filter { it.workoutId == workoutId }
        }

        coEvery { exerciseLogDao.deleteExerciseLog(any()) } answers {
            val id = firstArg<String>()
            exerciseLogs.remove(id)
        }
    }

    private fun setupSetLogDaoMocks() {
        coEvery { setLogDao.insertSetLog(any()) } answers {
            val setLog = firstArg<SetLog>()
            setLogs[setLog.id] = setLog
        }

        coEvery { setLogDao.getSetLogsForExercise(any()) } answers {
            val exerciseLogId = firstArg<String>()
            setLogs.values.filter { it.exerciseLogId == exerciseLogId }
        }

        coEvery { setLogDao.deleteSetLog(any()) } answers {
            val id = firstArg<String>()
            setLogs.remove(id)
        }
    }

    private fun setupProgrammeDaoMocks() {
        coEvery { programmeDao.insertProgramme(any<Programme>()) } answers {
            val programme = firstArg<Programme>()
            programmes[programme.id] = programme
        }

        coEvery { programmeDao.getProgrammeById(any<String>()) } answers {
            val id = firstArg<String>()
            programmes[id]
        }
    }

    @Test
    fun `createWorkout should insert workout and return id`() =
        runTest(testDispatcher) {
            val workout =
                Workout(
                    userId = null,
                    name = "Morning Workout",
                    notes = "Test workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )

            val id = repository.createWorkout(workout)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(id).isNotNull()
            val retrieved = repository.getWorkoutById(id)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.name).isEqualTo("Morning Workout")
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        }

    @Test
    fun `getWorkoutById should return null for non-existent workout`() =
        runTest(testDispatcher) {
            val result = repository.getWorkoutById("999")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(result).isNull()
        }

    @Test
    fun `deleteWorkout should remove workout and all associated data`() =
        runTest(testDispatcher) {
            // Create workout with exercises and sets
            val workout =
                Workout(
                    userId = null,
                    name = "Test Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.IN_PROGRESS,
                )
            val workoutId = (nextWorkoutIdCounter++).toString()
            workouts[workoutId] = workout.copy(id = workoutId)

            // Add exercise
            val exerciseLog =
                ExerciseLog(
                    workoutId = workoutId,
                    exerciseId = "1",
                    exerciseOrder = 1,
                )
            val exerciseId = (nextExerciseIdCounter++).toString()
            exerciseLogs[exerciseId] = exerciseLog.copy(id = exerciseId)

            // Add sets
            val set1 =
                SetLog(
                    exerciseLogId = exerciseId,
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 100f,
                    actualReps = 10,
                    actualWeight = 100f,
                )
            val setId = (nextSetIdCounter++).toString()
            setLogs[setId] = set1.copy(id = setId)

            val workoutToDelete = workouts[workoutId]!!
            repository.deleteWorkout(workoutToDelete)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify everything is deleted
            assertThat(workouts[workoutId]).isNull()
            assertThat(exerciseLogs.values.filter { it.workoutId == workoutId }).isEmpty()
            assertThat(setLogs.values.filter { it.exerciseLogId == exerciseId }).isEmpty()
        }

    @Test
    fun `deleteWorkoutById should handle non-existent workout gracefully`() =
        runTest(testDispatcher) {
            // Should not throw exception
            repository.deleteWorkoutById("999")
            testDispatcher.scheduler.advanceUntilIdle()
        }

    @Test
    fun `updateWorkoutStatus should change workout status`() =
        runTest(testDispatcher) {
            val workout =
                Workout(
                    name = "Status Test",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )
            val id = (nextWorkoutIdCounter++).toString()
            workouts[id] = workout.copy(id = id)

            repository.updateWorkoutStatus(id, WorkoutStatus.IN_PROGRESS)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = workouts[id]
            assertThat(updated?.status).isEqualTo(WorkoutStatus.IN_PROGRESS)
        }

    @Test
    fun `completeWorkout should update status and duration`() =
        runTest(testDispatcher) {
            val workout =
                Workout(
                    name = "Complete Test",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.IN_PROGRESS,
                )
            val id = (nextWorkoutIdCounter++).toString()
            workouts[id] = workout.copy(id = id)

            repository.completeWorkout(id, duration = 3600)
            testDispatcher.scheduler.advanceUntilIdle()

            val completed = workouts[id]
            assertThat(completed?.status).isEqualTo(WorkoutStatus.COMPLETED)
            assertThat(completed?.durationSeconds).isEqualTo("3600")
        }

    @Test
    fun `completeWorkout should handle missing workout gracefully`() =
        runTest(testDispatcher) {
            repository.completeWorkout("999", duration = 1000)
            testDispatcher.scheduler.advanceUntilIdle()
            // Should not throw exception
        }

    @Test
    fun `getExerciseLogsForWorkout should return all exercises for workout`() =
        runTest(testDispatcher) {
            val workout = Workout(userId = null, name = "Test", date = LocalDateTime.now(), status = WorkoutStatus.IN_PROGRESS)
            val workoutId = workout.id
            workouts[workoutId] = workout

            val exercise1 = ExerciseLog(workoutId = workoutId, exerciseId = "1", exerciseOrder = 1)
            val exercise2 = ExerciseLog(workoutId = workoutId, exerciseId = "2", exerciseOrder = 2)
            exerciseLogs[exercise1.id] = exercise1
            exerciseLogs[exercise2.id] = exercise2

            val exercises = repository.getExerciseLogsForWorkout(workoutId)

            assertThat(exercises).hasSize(2)
            assertThat(exercises[0].exerciseId).isEqualTo("1")
            assertThat(exercises[1].exerciseId).isEqualTo("2")
        }

    @Test
    fun `getSetLogsForExercise should return all sets for exercise`() =
        runTest(testDispatcher) {
            val workout = Workout(userId = null, name = "Test", date = LocalDateTime.now(), status = WorkoutStatus.IN_PROGRESS)
            val workoutId = workout.id
            workouts[workoutId] = workout
            val exercise = ExerciseLog(workoutId = workoutId, exerciseId = "1", exerciseOrder = 1)
            val exerciseId = exercise.id
            exerciseLogs[exerciseId] = exercise

            val set1 = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10)
            val set2 = SetLog(exerciseLogId = exerciseId, setOrder = 2, targetReps = 8)
            setLogs[set1.id] = set1
            setLogs[set2.id] = set2

            val sets = repository.getSetLogsForExercise(exerciseId)

            assertThat(sets).hasSize(2)
            assertThat(sets[0].targetReps).isEqualTo(10)
            assertThat(sets[1].targetReps).isEqualTo(8)
        }

    @Test
    fun `getWorkoutHistory should return completed and in-progress workouts sorted by date`() =
        runTest(testDispatcher) {
            val now = LocalDateTime.now()

            // Create completed workout
            val workout1 =
                Workout(
                    name = "Completed",
                    date = now.minusDays(2),
                    status = WorkoutStatus.COMPLETED,
                    durationSeconds = "3600",
                    userId = "test-user-id",
                )
            workouts[workout1.id] = workout1

            // Create in-progress workout with exercises
            val workout2 =
                Workout(
                    name = "In Progress",
                    date = now.minusDays(1),
                    status = WorkoutStatus.IN_PROGRESS,
                    userId = "test-user-id",
                )
            val id2 = workout2.id
            workouts[id2] = workout2
            val exercise = ExerciseLog(workoutId = id2, exerciseId = "1", exerciseOrder = 1)
            val exerciseId = exercise.id
            exerciseLogs[exerciseId] = exercise
            val set = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10, actualReps = 10, actualWeight = 100f, isCompleted = true)
            setLogs[set.id] = set

            val history = repository.getWorkoutHistory()

            assertThat(history).hasSize(2)
            assertThat(history[0].name).isEqualTo("In Progress") // Most recent first
            assertThat(history[1].name).isEqualTo("Completed")
            assertThat(history[0].totalWeight).isEqualTo(1000f) // 10 reps * 100kg
        }

    @Test
    fun `getWorkoutHistory should exclude empty non-completed workouts`() =
        runTest(testDispatcher) {
            // Create not-started workout with no exercises
            val workout =
                Workout(
                    name = "Empty",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                    userId = "test-user-id",
                )
            val id = (nextWorkoutIdCounter++).toString()
            workouts[id] = workout.copy(id = id)

            val history = repository.getWorkoutHistory()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(history).isEmpty()
        }

    @Test
    fun `getWorkoutHistory should include programme information`() =
        runTest(testDispatcher) {
            // Create a programme first
            val programme =
                Programme(
                    name = "Test Programme",
                    description = "Test",
                    durationWeeks = 8,
                    programmeType = ProgrammeType.STRENGTH,
                    difficulty = ProgrammeDifficulty.INTERMEDIATE,
                    createdAt = LocalDateTime.now(),
                )
            val programmeId = (nextProgrammeIdCounter++).toString()
            programmes[programmeId] = programme.copy(id = programmeId)

            // Create programme workout
            val workout =
                Workout(
                    name = "Programme Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    userId = "test-user-id",
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    programmeWorkoutName = "Week 1 Day 1",
                    weekNumber = 1,
                    dayNumber = 1,
                )
            val id = (nextWorkoutIdCounter++).toString()
            workouts[id] = workout.copy(id = id)

            val history = repository.getWorkoutHistory()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(history).hasSize(1)
            assertThat(history[0].isProgrammeWorkout).isTrue()
            assertThat(history[0].programmeName).isEqualTo("Test Programme")
            assertThat(history[0].programmeWorkoutName).isEqualTo("Week 1 Day 1")
            assertThat(history[0].weekNumber).isEqualTo(1)
            assertThat(history[0].dayNumber).isEqualTo(1)
        }

    @Test
    fun `getRecentWorkouts should return limited number of workouts sorted by date`() =
        runTest(testDispatcher) {
            val now = LocalDateTime.now()

            // Create 10 completed workouts
            for (i in 0 until 10) {
                val workout =
                    Workout(
                        name = "Workout ${i + 1}",
                        date = now.minusDays(i.toLong()),
                        status = WorkoutStatus.COMPLETED,
                        userId = "test-user-id",
                        durationSeconds = "3600",
                    )
                workouts[workout.id] = workout
            }

            val recent = repository.getRecentWorkouts(5)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(recent).hasSize(5)
            assertThat(recent[0].name).isEqualTo("Workout 1")
            assertThat(recent[4].name).isEqualTo("Workout 5")
            for (i in 0 until recent.size - 1) {
                assertThat(recent[i].date).isAtLeast(recent[i + 1].date)
            }
        }

    @Test
    fun `getRecentWorkouts should return all workouts if limit exceeds total`() =
        runTest(testDispatcher) {
            val now = LocalDateTime.now()

            for (i in 0 until 3) {
                val workout =
                    Workout(
                        name = "Workout ${i + 1}",
                        date = now.minusDays(i.toLong()),
                        status = WorkoutStatus.COMPLETED,
                        userId = "test-user-id",
                        durationSeconds = "3600",
                    )
                workouts[workout.id] = workout
            }

            val recent = repository.getRecentWorkouts(10)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(recent).hasSize(3)
        }

    @Test
    fun `getRecentWorkouts should exclude empty non-completed workouts`() =
        runTest(testDispatcher) {
            val now = LocalDateTime.now()

            val completedWorkout =
                Workout(
                    name = "Completed",
                    date = now.minusDays(1),
                    status = WorkoutStatus.COMPLETED,
                    userId = "test-user-id",
                    durationSeconds = "3600",
                )
            workouts[completedWorkout.id] = completedWorkout

            val emptyWorkout =
                Workout(
                    name = "Empty",
                    date = now,
                    status = WorkoutStatus.NOT_STARTED,
                    userId = "test-user-id",
                )
            workouts[emptyWorkout.id] = emptyWorkout

            val recent = repository.getRecentWorkouts(10)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(recent).hasSize(1)
            assertThat(recent[0].name).isEqualTo("Completed")
        }

    @Test
    fun `getRecentWorkouts should include in-progress workouts with exercises`() =
        runTest(testDispatcher) {
            val now = LocalDateTime.now()

            val inProgressWorkout =
                Workout(
                    name = "In Progress",
                    date = now,
                    status = WorkoutStatus.IN_PROGRESS,
                    userId = "test-user-id",
                )
            val id = inProgressWorkout.id
            workouts[id] = inProgressWorkout

            val exercise = ExerciseLog(workoutId = id, exerciseId = "1", exerciseOrder = 1)
            val exerciseId = exercise.id
            exerciseLogs[exerciseId] = exercise

            val set = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10, actualReps = 10, actualWeight = 100f, isCompleted = true)
            setLogs[set.id] = set

            val recent = repository.getRecentWorkouts(10)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(recent).hasSize(1)
            assertThat(recent[0].name).isEqualTo("In Progress")
            assertThat(recent[0].totalWeight).isEqualTo(1000f)
        }

    // Template tests removed - templates now use WorkoutTemplateRepository
}
