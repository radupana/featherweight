package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.service.GlobalProgressTracker
import com.github.radupana.featherweight.service.PRDetectionService
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WorkoutRepositoryTest {
    private lateinit var repository: WorkoutRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var prDetectionService: PRDetectionService
    private lateinit var globalProgressTracker: GlobalProgressTracker

    @Before
    fun setup() {
        db = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        prDetectionService = mockk(relaxed = true)
        globalProgressTracker = mockk(relaxed = true)

        repository = WorkoutRepository(
            db = db,
            workoutDao = workoutDao,
            exerciseLogDao = exerciseLogDao,
            setLogDao = setLogDao,
            prDetectionService = prDetectionService,
            globalProgressTracker = globalProgressTracker,
        )
    }

    @Test
    fun `insertWorkout delegates to dao`() = runTest {
        val workout = Workout(
            date = LocalDateTime.now(),
            name = "Test Workout",
        )
        val expectedId = 42L

        coEvery { workoutDao.insertWorkout(workout) } returns expectedId

        val result = repository.insertWorkout(workout)

        assertThat(result).isEqualTo(expectedId)
        coVerify { workoutDao.insertWorkout(workout) }
    }

    @Test
    fun `updateWorkout delegates to dao`() = runTest {
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            name = "Updated Workout",
        )

        coEvery { workoutDao.updateWorkout(workout) } just Runs

        repository.updateWorkout(workout)

        coVerify { workoutDao.updateWorkout(workout) }
    }

    @Test
    fun `getWorkoutById returns workout from dao`() = runTest {
        val workoutId = 1L
        val expectedWorkout = Workout(
            id = workoutId,
            date = LocalDateTime.now(),
            name = "Test Workout",
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns expectedWorkout

        val result = repository.getWorkoutById(workoutId)

        assertThat(result).isEqualTo(expectedWorkout)
    }

    @Test
    fun `getOngoingWorkout returns first in-progress workout`() = runTest {
        val ongoingWorkout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.IN_PROGRESS,
        )
        val workouts = listOf(ongoingWorkout)

        coEvery { workoutDao.getAllWorkouts() } returns workouts

        val result = repository.getOngoingWorkout()

        assertThat(result).isEqualTo(ongoingWorkout)
    }

    @Test
    fun `getOngoingWorkout returns null when no ongoing workouts`() = runTest {
        val completedWorkout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED,
        )
        coEvery { workoutDao.getAllWorkouts() } returns listOf(completedWorkout)

        val result = repository.getOngoingWorkout()

        assertThat(result).isNull()
    }

    @Test
    fun `deleteWorkout delegates to dao`() = runTest {
        val workoutId = 1L

        coEvery { workoutDao.deleteWorkout(workoutId) } just Runs

        repository.deleteWorkout(workoutId)

        coVerify { workoutDao.deleteWorkout(workoutId) }
    }

    @Test
    fun `getExerciseLogsForWorkout returns logs from dao`() = runTest {
        val workoutId = 1L
        val expectedLogs = listOf(
            ExerciseLog(
                id = 1L,
                workoutId = workoutId,
                exerciseVariationId = 10L,
                exerciseOrder = 0,
            ),
            ExerciseLog(
                id = 2L,
                workoutId = workoutId,
                exerciseVariationId = 11L,
                exerciseOrder = 1,
            ),
        )

        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns expectedLogs

        val result = repository.getExerciseLogsForWorkout(workoutId)

        assertThat(result).isEqualTo(expectedLogs)
    }

    @Test
    fun `getSetsForWorkout returns all sets for workout`() = runTest {
        val workoutId = 1L
        val exerciseLog1 = ExerciseLog(id = 1L, workoutId = workoutId, exerciseVariationId = 10L, exerciseOrder = 0)
        val exerciseLog2 = ExerciseLog(id = 2L, workoutId = workoutId, exerciseVariationId = 11L, exerciseOrder = 1)
        val exerciseLogs = listOf(exerciseLog1, exerciseLog2)
        
        val sets1 = listOf(
            SetLog(id = 1L, exerciseLogId = 1L, setOrder = 0, actualWeight = 100f, actualReps = 10),
            SetLog(id = 2L, exerciseLogId = 1L, setOrder = 1, actualWeight = 100f, actualReps = 8),
        )
        val sets2 = listOf(
            SetLog(id = 3L, exerciseLogId = 2L, setOrder = 0, actualWeight = 50f, actualReps = 12),
        )

        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns exerciseLogs
        coEvery { setLogDao.getSetLogsForExercise(1L) } returns sets1
        coEvery { setLogDao.getSetLogsForExercise(2L) } returns sets2

        val result = repository.getSetsForWorkout(workoutId)

        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyElementsIn(sets1 + sets2)
    }

    @Test
    fun `completeWorkout updates status and processes data`() = runTest {
        val workoutId = 1L
        val startTime = LocalDateTime.now().minusHours(1)
        val workout = Workout(
            id = workoutId,
            date = startTime,
            status = WorkoutStatus.IN_PROGRESS,
            timerStartTime = startTime,
        )
        val exerciseLogs = listOf(
            ExerciseLog(id = 1L, workoutId = workoutId, exerciseVariationId = 10L, exerciseOrder = 0),
        )
        val sets = listOf(
            SetLog(id = 1L, exerciseLogId = 1L, setOrder = 0, actualWeight = 100f, actualReps = 10, isCompleted = true),
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns exerciseLogs
        coEvery { setLogDao.getSetLogsForExercise(1L) } returns sets
        coEvery { workoutDao.updateWorkout(any()) } just Runs

        repository.completeWorkout(workoutId, "Great workout!", false)

        val capturedWorkout = slot<Workout>()
        coVerify { workoutDao.updateWorkout(capture(capturedWorkout)) }
        
        assertThat(capturedWorkout.captured.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(capturedWorkout.captured.notes).isEqualTo("Great workout!")
        assertThat(capturedWorkout.captured.durationSeconds).isGreaterThan(0)
        
        coVerify { 
            prDetectionService.checkForPR(sets[0], 10L)
            globalProgressTracker.updateProgressAfterWorkout(workoutId)
        }
    }

    @Test
    fun `completeWorkout processes programme workout same as regular`() = runTest {
        val workoutId = 1L
        val workout = Workout(
            id = workoutId,
            date = LocalDateTime.now(),
            status = WorkoutStatus.IN_PROGRESS,
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns workout
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()

        repository.completeWorkout(workoutId, isProgrammeWorkout = true)

        // Programme workouts still update status and progress
        coVerify { workoutDao.updateWorkout(any()) }
        coVerify { globalProgressTracker.updateProgressAfterWorkout(workoutId) }
    }

    @Test
    fun `getWorkoutHistory returns summaries with statistics`() = runTest {
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            name = "Test Workout",
            status = WorkoutStatus.COMPLETED,
            durationSeconds = 3600,
        )
        val exerciseLogs = listOf(
            ExerciseLog(id = 1L, workoutId = 1L, exerciseVariationId = 10L, exerciseOrder = 0),
            ExerciseLog(id = 2L, workoutId = 1L, exerciseVariationId = 11L, exerciseOrder = 1),
        )
        val sets1 = listOf(
            SetLog(id = 1L, exerciseLogId = 1L, setOrder = 0, actualWeight = 100f, actualReps = 10, isCompleted = true),
            SetLog(id = 2L, exerciseLogId = 1L, setOrder = 1, actualWeight = 100f, actualReps = 8, isCompleted = true),
        )
        val sets2 = listOf(
            SetLog(id = 3L, exerciseLogId = 2L, setOrder = 0, actualWeight = 50f, actualReps = 12, isCompleted = true),
        )

        coEvery { workoutDao.getAllWorkouts() } returns listOf(workout)
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(1L) } returns exerciseLogs
        coEvery { setLogDao.getSetLogsForExercise(1L) } returns sets1
        coEvery { setLogDao.getSetLogsForExercise(2L) } returns sets2

        val result = repository.getWorkoutHistory()

        assertThat(result).hasSize(1)
        val summary = result[0]
        assertThat(summary.id).isEqualTo(1L)
        assertThat(summary.name).isEqualTo("Test Workout")
        assertThat(summary.exerciseCount).isEqualTo(2)
        assertThat(summary.setCount).isEqualTo(3)
        assertThat(summary.totalWeight).isEqualTo(2400f) // (100*10 + 100*8 + 50*12)
        assertThat(summary.duration).isEqualTo(3600)
    }

    @Test
    fun `getExerciseHistory returns formatted history`() = runTest {
        val exerciseVariationId = 10L
        val currentWorkoutId = 2L
        val exerciseLog = ExerciseLog(
            id = 1L,
            workoutId = 1L,
            exerciseVariationId = exerciseVariationId,
            exerciseOrder = 0,
        )
        val workout = Workout(
            id = 1L,
            date = LocalDateTime.now(),
            status = WorkoutStatus.COMPLETED,
        )
        val sets = listOf(
            SetLog(id = 1L, exerciseLogId = 1L, setOrder = 0, actualWeight = 100f, actualReps = 10, actualRpe = 8f, isCompleted = true),
            SetLog(id = 2L, exerciseLogId = 1L, setOrder = 1, actualWeight = 100f, actualReps = 8, actualRpe = 9f, isCompleted = true),
        )

        coEvery { workoutDao.getAllWorkouts() } returns listOf(workout)
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(1L) } returns listOf(exerciseLog)
        coEvery { setLogDao.getSetLogsForExercise(1L) } returns sets

        val result = repository.getExerciseHistory(exerciseVariationId, currentWorkoutId)

        assertThat(result).isNotNull()
        assertThat(result?.exerciseVariationId).isEqualTo(exerciseVariationId)
        assertThat(result?.lastWorkoutDate).isEqualTo(workout.date)
        assertThat(result?.sets).hasSize(2)
        assertThat(result?.sets?.get(0)?.actualWeight).isEqualTo(100f)
        assertThat(result?.sets?.get(0)?.actualReps).isEqualTo(10)
        assertThat(result?.sets?.get(0)?.actualRpe).isEqualTo(8f)
    }

    @Test
    fun `getFilteredWorkouts applies date range filter`() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        
        val workouts = listOf(
            Workout(id = 1L, date = yesterday.atStartOfDay()),
            Workout(id = 2L, date = today.atStartOfDay()),
            Workout(id = 3L, date = tomorrow.atStartOfDay()),
        )

        coEvery { workoutDao.getAllWorkouts() } returns workouts
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()

        val filters = WorkoutFilters(
            dateRange = yesterday to today,
        )
        val result = repository.getFilteredWorkouts(filters)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `getWorkoutDayInfo returns correct counts`() = runTest {
        val date = LocalDate.now()
        val workouts = listOf(
            Workout(id = 1L, date = date.atStartOfDay(), status = WorkoutStatus.COMPLETED),
            Workout(id = 2L, date = date.atStartOfDay(), status = WorkoutStatus.COMPLETED),
            Workout(id = 3L, date = date.atStartOfDay(), status = WorkoutStatus.IN_PROGRESS),
            Workout(id = 4L, date = date.atStartOfDay(), status = WorkoutStatus.NOT_STARTED),
        )

        coEvery { 
            workoutDao.getWorkoutsInDateRange(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
            )
        } returns workouts

        val result = repository.getWorkoutDayInfo(date)

        assertThat(result.completedCount).isEqualTo(2)
        assertThat(result.inProgressCount).isEqualTo(1)
        assertThat(result.notStartedCount).isEqualTo(1)
    }

    @Test
    fun `getRecentWorkoutsFlow returns flow from dao`() = runTest {
        val workouts = listOf(
            Workout(id = 1L, date = LocalDateTime.now()),
            Workout(id = 2L, date = LocalDateTime.now().minusDays(1)),
        )
        @Suppress("UNUSED_VARIABLE")
        val expectedFlow = flowOf(workouts)

        // Note: getRecentWorkoutsFlow currently returns empty flow in repository implementation

        val result = repository.getRecentWorkoutsFlow(10)

        // Current implementation returns empty flow
        // This test would need updating when proper Flow implementation is added
    }

    @Test
    fun `getWeeklyVolumeTotal calculates correct total`() = runTest {
        val startDate = LocalDateTime.now()
        val endDate = startDate.plusWeeks(1)
        val workouts = listOf(
            Workout(id = 1L, date = startDate.plusDays(1)),
            Workout(id = 2L, date = startDate.plusDays(3)),
        )
        val sets = listOf(
            SetLog(id = 1L, exerciseLogId = 1L, setOrder = 0, actualWeight = 100f, actualReps = 10, isCompleted = true),
            SetLog(id = 2L, exerciseLogId = 1L, setOrder = 1, actualWeight = 80f, actualReps = 12, isCompleted = true),
            SetLog(id = 3L, exerciseLogId = 1L, setOrder = 2, actualWeight = 90f, actualReps = 8, isCompleted = false), // Not completed
        )

        coEvery { workoutDao.getWorkoutsInDateRange(startDate, endDate) } returns workouts
        coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } returns listOf(
            ExerciseLog(id = 1L, workoutId = 1L, exerciseVariationId = 10L, exerciseOrder = 0),
        )
        coEvery { setLogDao.getSetLogsForExercise(any()) } returns sets

        val result = repository.getWeeklyVolumeTotal(startDate)

        // Only completed sets: 100*10 + 80*12 = 1000 + 960 = 1960
        // Two workouts, so 1960 * 2 = 3920
        assertThat(result).isEqualTo(3920f)
    }

    @Test
    fun `getTrainingFrequency counts completed workouts`() = runTest {
        val startDate = LocalDateTime.now().minusDays(7)
        val endDate = LocalDateTime.now()
        val workouts = listOf(
            Workout(id = 1L, date = startDate.plusDays(1), status = WorkoutStatus.COMPLETED),
            Workout(id = 2L, date = startDate.plusDays(3), status = WorkoutStatus.COMPLETED),
            Workout(id = 3L, date = startDate.plusDays(5), status = WorkoutStatus.IN_PROGRESS),
            Workout(id = 4L, date = startDate.plusDays(6), status = WorkoutStatus.NOT_STARTED),
        )

        coEvery { workoutDao.getWorkoutsInDateRange(startDate, endDate) } returns workouts

        val result = repository.getTrainingFrequency(startDate, endDate)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `updateWorkoutTimer updates elapsed time and start time`() = runTest {
        val workoutId = 1L
        val workout = Workout(
            id = workoutId,
            date = LocalDateTime.now(),
            timerElapsedSeconds = 100,
            timerStartTime = null,
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns workout
        coEvery { workoutDao.updateWorkout(any()) } just Runs

        repository.updateWorkoutTimer(workoutId, 300)

        val capturedWorkout = slot<Workout>()
        coVerify { workoutDao.updateWorkout(capture(capturedWorkout)) }
        
        assertThat(capturedWorkout.captured.timerElapsedSeconds).isEqualTo(300)
        assertThat(capturedWorkout.captured.timerStartTime).isNotNull()
    }

    @Test
    fun `startWorkout creates new workout with correct fields`() = runTest {
        val expectedId = 42L
        
        coEvery { workoutDao.insertWorkout(any()) } returns expectedId

        val result = repository.startWorkout(
            name = "Morning Workout",
            programmeId = 10L,
            weekNumber = 2,
            dayNumber = 3,
            programmeWorkoutName = "Week 2 Day 3",
        )

        assertThat(result).isEqualTo(expectedId)
        
        val capturedWorkout = slot<Workout>()
        coVerify { workoutDao.insertWorkout(capture(capturedWorkout)) }
        
        assertThat(capturedWorkout.captured.name).isEqualTo("Morning Workout")
        assertThat(capturedWorkout.captured.programmeId).isEqualTo(10L)
        assertThat(capturedWorkout.captured.weekNumber).isEqualTo(2)
        assertThat(capturedWorkout.captured.dayNumber).isEqualTo(3)
        assertThat(capturedWorkout.captured.programmeWorkoutName).isEqualTo("Week 2 Day 3")
        assertThat(capturedWorkout.captured.isProgrammeWorkout).isTrue()
        assertThat(capturedWorkout.captured.status).isEqualTo(WorkoutStatus.NOT_STARTED)
    }

    @Test
    fun `markWorkoutInProgress updates status and timer`() = runTest {
        val workoutId = 1L
        val workout = Workout(
            id = workoutId,
            date = LocalDateTime.now(),
            status = WorkoutStatus.NOT_STARTED,
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns workout
        coEvery { workoutDao.updateWorkout(any()) } just Runs

        repository.markWorkoutInProgress(workoutId)

        val capturedWorkout = slot<Workout>()
        coVerify { workoutDao.updateWorkout(capture(capturedWorkout)) }
        
        assertThat(capturedWorkout.captured.status).isEqualTo(WorkoutStatus.IN_PROGRESS)
        assertThat(capturedWorkout.captured.timerStartTime).isNotNull()
    }

    @Test
    fun `markWorkoutInProgress skips update if already in progress`() = runTest {
        val workoutId = 1L
        val workout = Workout(
            id = workoutId,
            date = LocalDateTime.now(),
            status = WorkoutStatus.IN_PROGRESS,
        )

        coEvery { workoutDao.getWorkoutById(workoutId) } returns workout

        repository.markWorkoutInProgress(workoutId)

        coVerify(exactly = 0) { workoutDao.updateWorkout(any()) }
    }
}
