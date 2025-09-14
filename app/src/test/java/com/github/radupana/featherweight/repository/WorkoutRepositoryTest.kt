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
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
    private val testDispatcher = StandardTestDispatcher()

    // Storage for mock data
    private val workouts = mutableMapOf<Long, Workout>()
    private val exerciseLogs = mutableMapOf<Long, ExerciseLog>()
    private val setLogs = mutableMapOf<Long, SetLog>()
    private val programmes = mutableMapOf<Long, Programme>()
    private var nextWorkoutId = 1L
    private var nextExerciseId = 1L
    private var nextSetId = 1L
    private var nextProgrammeId = 1L

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

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
        repository = WorkoutRepository(application, testDispatcher, database)
    }

    private fun setupWorkoutDaoMocks() {
        coEvery { workoutDao.insertWorkout(any()) } answers {
            val workout = firstArg<Workout>()
            val id = nextWorkoutId++
            workouts[id] = workout.copy(id = id)
            id
        }

        coEvery { workoutDao.getWorkoutById(any()) } answers {
            val id = firstArg<Long>()
            workouts[id]
        }

        coEvery { workoutDao.deleteWorkout(any()) } answers {
            val id = firstArg<Long>()
            workouts.remove(id)
        }

        coEvery { workoutDao.updateWorkout(any()) } answers {
            val workout = firstArg<Workout>()
            workouts[workout.id] = workout
        }

        coEvery { workoutDao.getAllWorkouts() } answers {
            workouts.values.toList()
        }
    }

    private fun setupExerciseLogDaoMocks() {
        coEvery { exerciseLogDao.insertExerciseLog(any()) } answers {
            val exerciseLog = firstArg<ExerciseLog>()
            val id = nextExerciseId++
            exerciseLogs[id] = exerciseLog.copy(id = id)
            id
        }

        coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } answers {
            val workoutId = firstArg<Long>()
            exerciseLogs.values.filter { it.workoutId == workoutId }
        }

        coEvery { exerciseLogDao.deleteExerciseLog(any()) } answers {
            val id = firstArg<Long>()
            exerciseLogs.remove(id)
        }
    }

    private fun setupSetLogDaoMocks() {
        coEvery { setLogDao.insertSetLog(any()) } answers {
            val setLog = firstArg<SetLog>()
            val id = nextSetId++
            setLogs[id] = setLog.copy(id = id)
            id
        }

        coEvery { setLogDao.getSetLogsForExercise(any()) } answers {
            val exerciseLogId = firstArg<Long>()
            setLogs.values.filter { it.exerciseLogId == exerciseLogId }
        }

        coEvery { setLogDao.deleteSetLog(any()) } answers {
            val id = firstArg<Long>()
            setLogs.remove(id)
        }
    }

    private fun setupProgrammeDaoMocks() {
        coEvery { programmeDao.insertProgramme(any<Programme>()) } answers {
            val programme = firstArg<Programme>()
            val id = nextProgrammeId++
            programmes[id] = programme.copy(id = id)
            id
        }

        coEvery { programmeDao.getProgrammeById(any<Long>()) } answers {
            val id = firstArg<Long>()
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

            assertThat(id).isGreaterThan(0)
            val retrieved = repository.getWorkoutById(id)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.name).isEqualTo("Morning Workout")
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        }

    @Test
    fun `getWorkoutById should return null for non-existent workout`() =
        runTest(testDispatcher) {
            val result = repository.getWorkoutById(999)
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
            val workoutId = nextWorkoutId++
            workouts[workoutId] = workout.copy(id = workoutId)

            // Add exercise
            val exerciseLog =
                ExerciseLog(
                    workoutId = workoutId,
                    exerciseVariationId = 1,
                    exerciseOrder = 1,
                )
            val exerciseId = nextExerciseId++
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
            val setId = nextSetId++
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
            repository.deleteWorkoutById(999)
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
            val id = nextWorkoutId++
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
            val id = nextWorkoutId++
            workouts[id] = workout.copy(id = id)

            repository.completeWorkout(id, duration = 3600)
            testDispatcher.scheduler.advanceUntilIdle()

            val completed = workouts[id]
            assertThat(completed?.status).isEqualTo(WorkoutStatus.COMPLETED)
            assertThat(completed?.durationSeconds).isEqualTo(3600)
        }

    @Test
    fun `completeWorkout should handle missing workout gracefully`() =
        runTest(testDispatcher) {
            repository.completeWorkout(999, duration = 1000)
            testDispatcher.scheduler.advanceUntilIdle()
            // Should not throw exception
        }

    @Test
    fun `getExerciseLogsForWorkout should return all exercises for workout`() =
        runTest(testDispatcher) {
            val workoutId =
                database.workoutDao().insertWorkout(
                    Workout(userId = null, name = "Test", date = LocalDateTime.now(), status = WorkoutStatus.IN_PROGRESS),
                )

            val exercise1 = ExerciseLog(workoutId = workoutId, exerciseVariationId = 1, exerciseOrder = 1)
            val exercise2 = ExerciseLog(workoutId = workoutId, exerciseVariationId = 2, exerciseOrder = 2)
            database.exerciseLogDao().insertExerciseLog(exercise1)
            database.exerciseLogDao().insertExerciseLog(exercise2)

            val exercises = repository.getExerciseLogsForWorkout(workoutId)

            assertThat(exercises).hasSize(2)
            assertThat(exercises[0].exerciseVariationId).isEqualTo(1)
            assertThat(exercises[1].exerciseVariationId).isEqualTo(2)
        }

    @Test
    fun `getSetLogsForExercise should return all sets for exercise`() =
        runTest(testDispatcher) {
            val workoutId =
                database.workoutDao().insertWorkout(
                    Workout(userId = null, name = "Test", date = LocalDateTime.now(), status = WorkoutStatus.IN_PROGRESS),
                )
            val exerciseId =
                database.exerciseLogDao().insertExerciseLog(
                    ExerciseLog(workoutId = workoutId, exerciseVariationId = 1, exerciseOrder = 1),
                )

            val set1 = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10)
            val set2 = SetLog(exerciseLogId = exerciseId, setOrder = 2, targetReps = 8)
            database.setLogDao().insertSetLog(set1)
            database.setLogDao().insertSetLog(set2)

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
                    durationSeconds = 3600,
                )
            database.workoutDao().insertWorkout(workout1)

            // Create in-progress workout with exercises
            val workout2 =
                Workout(
                    name = "In Progress",
                    date = now.minusDays(1),
                    status = WorkoutStatus.IN_PROGRESS,
                )
            val id2 = database.workoutDao().insertWorkout(workout2)
            val exerciseId =
                database.exerciseLogDao().insertExerciseLog(
                    ExerciseLog(workoutId = id2, exerciseVariationId = 1, exerciseOrder = 1),
                )
            database.setLogDao().insertSetLog(
                SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10, actualReps = 10, actualWeight = 100f, isCompleted = true),
            )

            // Create template (should be excluded)
            val template =
                Workout(
                    name = "Template",
                    date = now,
                    status = WorkoutStatus.TEMPLATE,
                    isTemplate = true,
                )
            database.workoutDao().insertWorkout(template)

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
                )
            val id = nextWorkoutId++
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
            val programmeId = nextProgrammeId++
            programmes[programmeId] = programme.copy(id = programmeId)

            // Create programme workout
            val workout =
                Workout(
                    name = "Programme Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    isProgrammeWorkout = true,
                    programmeId = programmeId,
                    programmeWorkoutName = "Week 1 Day 1",
                    weekNumber = 1,
                    dayNumber = 1,
                )
            val id = nextWorkoutId++
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
    fun `createTemplateFromWorkout should create template with exercises and sets`() =
        runTest(testDispatcher) {
            // Create source workout
            val workout =
                Workout(
                    name = "Source Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )
            val workoutId = database.workoutDao().insertWorkout(workout)

            // Add exercise with completed sets
            val exerciseId =
                database.exerciseLogDao().insertExerciseLog(
                    ExerciseLog(workoutId = workoutId, exerciseVariationId = 1, exerciseOrder = 1),
                )
            database.setLogDao().insertSetLog(
                SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10, actualReps = 10, actualWeight = 100f, actualRpe = 7f),
            )
            database.setLogDao().insertSetLog(
                SetLog(exerciseLogId = exerciseId, setOrder = 2, targetReps = 8, actualReps = 8, actualWeight = 100f, actualRpe = 8f),
            )

            // Add exercise with no completed sets (should be skipped)
            val emptyExerciseId =
                database.exerciseLogDao().insertExerciseLog(
                    ExerciseLog(workoutId = workoutId, exerciseVariationId = 2, exerciseOrder = 2),
                )
            database.setLogDao().insertSetLog(
                SetLog(exerciseLogId = emptyExerciseId, setOrder = 1, targetReps = 10, actualReps = 0, actualWeight = 0f),
            )

            val templateId =
                repository.createTemplateFromWorkout(
                    workoutId = workoutId,
                    templateName = "My Template",
                    templateDescription = "Test template",
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify template was created
            val template = database.workoutDao().getWorkoutById(templateId)
            assertThat(template).isNotNull()
            assertThat(template?.name).isEqualTo("My Template")
            assertThat(template?.notes).isEqualTo("Test template")
            assertThat(template?.status).isEqualTo(WorkoutStatus.TEMPLATE)
            assertThat(template?.isTemplate).isTrue()

            // Verify exercises were copied (only one with completed sets)
            val templateExercises = database.exerciseLogDao().getExerciseLogsForWorkout(templateId)
            assertThat(templateExercises).hasSize(1)
            assertThat(templateExercises[0].exerciseVariationId).isEqualTo(1)

            // Verify sets were copied with actual values as targets
            val templateSets = database.setLogDao().getSetLogsForExercise(templateExercises[0].id)
            assertThat(templateSets).hasSize(2)
            assertThat(templateSets[0].targetReps).isEqualTo(10)
            assertThat(templateSets[0].targetWeight).isEqualTo(100f)
            assertThat(templateSets[0].targetRpe).isEqualTo(7f)
            assertThat(templateSets[0].actualReps).isEqualTo(0)
            assertThat(templateSets[1].targetReps).isEqualTo(8)
            assertThat(templateSets[1].targetRpe).isEqualTo(8f)
        }

    @Test
    fun `getTemplates should return only template workouts`() =
        runTest(testDispatcher) {
            // Create regular workout
            val regularId = nextWorkoutId++
            workouts[regularId] = Workout(id = regularId, userId = null, name = "Regular", date = LocalDateTime.now(), status = WorkoutStatus.COMPLETED)

            // Create templates
            val template1Id = nextWorkoutId++
            workouts[template1Id] = Workout(id = template1Id, userId = null, name = "Template 1", date = LocalDateTime.now().minusDays(1), status = WorkoutStatus.TEMPLATE, isTemplate = true)

            val template2Id = nextWorkoutId++
            workouts[template2Id] = Workout(id = template2Id, userId = null, name = "Template 2", date = LocalDateTime.now(), status = WorkoutStatus.TEMPLATE, isTemplate = true, notes = "Description")

            // Add exercises to template 2
            val exerciseLog = ExerciseLog(workoutId = template2Id, exerciseVariationId = 1, exerciseOrder = 1)
            val exerciseId = nextExerciseId++
            exerciseLogs[exerciseId] = exerciseLog.copy(id = exerciseId)

            val setLog = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10)
            val setId = nextSetId++
            setLogs[setId] = setLog.copy(id = setId)

            val templates = repository.getTemplates()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(templates).hasSize(2)
            assertThat(templates[0].name).isEqualTo("Template 2") // Most recent first
            assertThat(templates[0].hasNotes).isTrue()
            assertThat(templates[0].exerciseCount).isEqualTo(1)
            assertThat(templates[0].setCount).isEqualTo(1)
            assertThat(templates[1].name).isEqualTo("Template 1")
        }

    @Test
    fun `startWorkoutFromTemplate should create new workout with exercises and sets`() =
        runTest(testDispatcher) {
            // Create template
            val templateId = nextWorkoutId++
            workouts[templateId] = Workout(id = templateId, userId = null, name = "Template", date = LocalDateTime.now(), status = WorkoutStatus.TEMPLATE, isTemplate = true)

            // Add exercises and sets to template
            val exerciseLog = ExerciseLog(workoutId = templateId, exerciseVariationId = 1, exerciseOrder = 1)
            val exerciseId = nextExerciseId++
            exerciseLogs[exerciseId] = exerciseLog.copy(id = exerciseId)

            val set1 = SetLog(exerciseLogId = exerciseId, setOrder = 1, targetReps = 10, targetWeight = 100f, targetRpe = 7f)
            val set2 = SetLog(exerciseLogId = exerciseId, setOrder = 2, targetReps = 8, targetWeight = 110f, targetRpe = 8f)
            val setId1 = nextSetId++
            val setId2 = nextSetId++
            setLogs[setId1] = set1.copy(id = setId1)
            setLogs[setId2] = set2.copy(id = setId2)

            val newWorkoutId = repository.startWorkoutFromTemplate(templateId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify new workout was created
            val newWorkout = workouts[newWorkoutId]
            assertThat(newWorkout).isNotNull()
            assertThat(newWorkout?.name).isEqualTo("Template")
            assertThat(newWorkout?.status).isEqualTo(WorkoutStatus.NOT_STARTED)
            assertThat(newWorkout?.isTemplate).isFalse()
            assertThat(newWorkout?.fromTemplateId).isEqualTo(templateId)

            // Verify exercises were copied
            val newExercises = exerciseLogs.values.filter { it.workoutId == newWorkoutId }
            assertThat(newExercises).hasSize(1)
            assertThat(newExercises[0].exerciseVariationId).isEqualTo(1)

            // Verify sets were copied with targets but no actuals
            val newSets = setLogs.values.filter { it.exerciseLogId == newExercises[0].id }
            assertThat(newSets).hasSize(2)
            val sortedNewSets = newSets.sortedBy { it.setOrder }
            assertThat(sortedNewSets[0].targetReps).isEqualTo(10)
            assertThat(sortedNewSets[0].targetWeight).isEqualTo(100f)
            assertThat(sortedNewSets[0].targetRpe).isEqualTo(7f)
            assertThat(sortedNewSets[0].actualReps).isEqualTo(0)
            assertThat(sortedNewSets[0].actualWeight).isEqualTo(0f)
            assertThat(sortedNewSets[0].isCompleted).isFalse()
        }

    @Test
    fun `startWorkoutFromTemplate should throw exception for invalid template`() =
        runTest(testDispatcher) {
            // Create non-template workout
            val regularWorkoutId = nextWorkoutId++
            workouts[regularWorkoutId] = Workout(id = regularWorkoutId, userId = null, name = "Regular", date = LocalDateTime.now(), status = WorkoutStatus.COMPLETED)

            try {
                repository.startWorkoutFromTemplate(regularWorkoutId)
                testDispatcher.scheduler.advanceUntilIdle()
                // Should not reach here
                assertThat(false).isTrue()
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).contains("Invalid template ID")
            }
        }

    @Test
    fun `startWorkoutFromTemplate should throw exception for non-existent template`() =
        runTest(testDispatcher) {
            try {
                repository.startWorkoutFromTemplate(999)
                testDispatcher.scheduler.advanceUntilIdle()
                // Should not reach here
                assertThat(false).isTrue()
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).contains("Invalid template ID")
            }
        }
}
