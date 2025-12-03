package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.BaseDaoTest
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Integration tests for FeatherweightRepository using real in-memory Room database.
 *
 * These tests verify repository behavior with actual database operations rather than mocks,
 * catching real query issues and ensuring data integrity across operations.
 *
 * Firebase Integration Note:
 * Firebase is mocked and the AuthManager returns "local" userId to skip Firestore sync.
 * This is intentional because:
 * 1. The repository's core logic (Room operations) is fully tested with real database
 * 2. Firestore sync only triggers when userId != "local" (authenticated users)
 * 3. Firebase emulator integration tests would require androidTest with emulator setup
 *
 * For full Firebase integration testing, consider adding instrumented tests in androidTest
 * that use `firestore.useEmulator("10.0.2.2", 8080)` with the Firebase emulator running.
 */
class FeatherweightRepositoryTest : BaseDaoTest() {
    private lateinit var repository: FeatherweightRepository
    private lateinit var application: Application
    private lateinit var authManager: AuthenticationManager

    @Before
    fun setupRepository() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseFirestore::class)
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseFirestore.getInstance(any<String>()) } returns firestore

        application = mockk(relaxed = true)
        authManager = mockk(relaxed = true)

        // Return "local" to skip Firestore sync in tests
        every { authManager.getCurrentUserId() } returns "local"

        repository = FeatherweightRepository(application, database, authManager)
    }

    @After
    fun tearDownRepository() {
        unmockkStatic(Log::class)
        unmockkStatic(FirebaseFirestore::class)
    }

    // ===== Programme Deletion Tests =====

    @Test
    fun `deleteProgramme with deleteWorkouts false archives programme and preserves workouts`() =
        runTest {
            val programme = createAndInsertProgramme(id = "prog1", name = "Test Programme", isActive = false)
            val workout =
                createAndInsertWorkout(
                    id = "workout1",
                    programmeId = programme.id,
                    isProgrammeWorkout = true,
                )

            repository.deleteProgramme(programme, deleteWorkouts = false)

            // Programme should be archived (status = CANCELLED)
            val updatedProgramme = programmeDao.getProgrammeById(programme.id)
            assertThat(updatedProgramme?.status).isEqualTo(ProgrammeStatus.CANCELLED)
            assertThat(updatedProgramme?.isActive).isFalse()

            // Workout should still exist
            val retrievedWorkout = workoutDao.getWorkoutById(workout.id)
            assertThat(retrievedWorkout).isNotNull()
        }

    @Test
    fun `deleteProgramme with deleteWorkouts true removes workouts and programme`() =
        runTest {
            val programme = createAndInsertProgramme(id = "prog1", name = "Test Programme", isActive = false)
            createAndInsertWorkout(id = "workout1", programmeId = programme.id, isProgrammeWorkout = true)
            createAndInsertWorkout(id = "workout2", programmeId = programme.id, isProgrammeWorkout = true)

            repository.deleteProgramme(programme, deleteWorkouts = true)

            // Programme should be deleted
            val deletedProgramme = programmeDao.getProgrammeById(programme.id)
            assertThat(deletedProgramme).isNull()

            // Workouts should be deleted
            val workout1 = workoutDao.getWorkoutById("workout1")
            val workout2 = workoutDao.getWorkoutById("workout2")
            assertThat(workout1).isNull()
            assertThat(workout2).isNull()
        }

    @Test
    fun `deleteProgramme with deleteWorkouts true deactivates active programme first`() =
        runTest {
            val activeProgramme =
                createAndInsertProgramme(
                    id = "prog1",
                    name = "Active Programme",
                    isActive = true,
                )

            repository.deleteProgramme(activeProgramme, deleteWorkouts = true)

            // Programme should be deleted
            val deletedProgramme = programmeDao.getProgrammeById(activeProgramme.id)
            assertThat(deletedProgramme).isNull()
        }

    // ===== Workout Count Tests =====

    @Test
    fun `getCompletedWorkoutCountForProgramme returns correct count`() =
        runTest {
            val programme = createAndInsertProgramme(id = "prog1")
            createAndInsertWorkout(
                id = "workout1",
                programmeId = programme.id,
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
            )
            createAndInsertWorkout(
                id = "workout2",
                programmeId = programme.id,
                isProgrammeWorkout = true,
                status = WorkoutStatus.COMPLETED,
            )
            createAndInsertWorkout(
                id = "workout3",
                programmeId = programme.id,
                isProgrammeWorkout = true,
                status = WorkoutStatus.IN_PROGRESS,
            )

            val count = repository.getCompletedWorkoutCountForProgramme(programme.id)

            assertThat(count).isEqualTo(2)
        }

    @Test
    fun `getCompletedSetCountForProgramme returns count of all sets from completed workouts`() =
        runTest {
            val programme = createAndInsertProgramme(id = "prog1")
            // Completed workout - all its sets count
            val completedWorkout =
                createAndInsertWorkout(
                    id = "workout1",
                    programmeId = programme.id,
                    isProgrammeWorkout = true,
                    status = WorkoutStatus.COMPLETED,
                )
            val exerciseLog1 = createAndInsertExerciseLog(id = "ex1", workoutId = completedWorkout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog1.id)
            createAndInsertSetLog(id = "set2", exerciseLogId = exerciseLog1.id)

            // In-progress workout - none of its sets count
            val inProgressWorkout =
                createAndInsertWorkout(
                    id = "workout2",
                    programmeId = programme.id,
                    isProgrammeWorkout = true,
                    status = WorkoutStatus.IN_PROGRESS,
                )
            val exerciseLog2 = createAndInsertExerciseLog(id = "ex2", workoutId = inProgressWorkout.id)
            createAndInsertSetLog(id = "set3", exerciseLogId = exerciseLog2.id)

            val count = repository.getCompletedSetCountForProgramme(programme.id)

            // Only sets from completed workout count (2), not from in-progress workout
            assertThat(count).isEqualTo(2)
        }

    // ===== Deviation Tests =====

    @Test
    fun `getDeviationsForWorkout returns deviations from database`() =
        runTest {
            val programme = createAndInsertProgramme(id = "prog1")
            val workout =
                createAndInsertWorkout(
                    id = "workout1",
                    programmeId = programme.id,
                    isProgrammeWorkout = true,
                )
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)

            val deviation =
                createDeviation(
                    workoutId = workout.id,
                    programmeId = programme.id,
                    exerciseLogId = exerciseLog.id,
                )
            workoutDeviationDao.insertAll(listOf(deviation))

            val result = repository.getDeviationsForWorkout(workout.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].workoutId).isEqualTo(workout.id)
            assertThat(result[0].deviationType).isEqualTo(DeviationType.VOLUME_DEVIATION)
        }

    // ===== Batch Query Tests =====

    @Test
    fun `getExerciseLogsForWorkouts returns exercise logs for multiple workouts`() =
        runTest {
            val workout1 = createAndInsertWorkout(id = "workout1")
            val workout2 = createAndInsertWorkout(id = "workout2")
            val workout3 = createAndInsertWorkout(id = "workout3")

            createAndInsertExerciseLog(id = "ex1", workoutId = workout1.id)
            createAndInsertExerciseLog(id = "ex2", workoutId = workout1.id)
            createAndInsertExerciseLog(id = "ex3", workoutId = workout2.id)
            createAndInsertExerciseLog(id = "ex4", workoutId = workout3.id)

            val result = repository.getExerciseLogsForWorkouts(listOf("workout1", "workout2", "workout3"))

            assertThat(result).hasSize(4)
            assertThat(result.map { it.workoutId }).containsExactly("workout1", "workout1", "workout2", "workout3")
        }

    @Test
    fun `getSetLogsForExercises returns set logs for multiple exercise logs`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val ex1 = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            val ex2 = createAndInsertExerciseLog(id = "ex2", workoutId = workout.id)
            val ex3 = createAndInsertExerciseLog(id = "ex3", workoutId = workout.id)

            createAndInsertSetLog(id = "set1", exerciseLogId = ex1.id)
            createAndInsertSetLog(id = "set2", exerciseLogId = ex1.id)
            createAndInsertSetLog(id = "set3", exerciseLogId = ex2.id)
            createAndInsertSetLog(id = "set4", exerciseLogId = ex3.id)

            val result = repository.getSetLogsForExercises(listOf("ex1", "ex2", "ex3"))

            assertThat(result).hasSize(4)
            assertThat(result.map { it.exerciseLogId }).containsExactly("ex1", "ex1", "ex2", "ex3")
        }

    @Test
    fun `getExercisesByIds returns exercises for multiple IDs`() =
        runTest {
            createAndInsertExercise(id = "exercise1", name = "Squat")
            createAndInsertExercise(id = "exercise2", name = "Bench Press")
            createAndInsertExercise(id = "exercise3", name = "Deadlift")

            val result = repository.getExercisesByIds(listOf("exercise1", "exercise2", "exercise3"))

            assertThat(result).hasSize(3)
            assertThat(result.map { it.name }).containsExactly("Squat", "Bench Press", "Deadlift")
        }

    @Test
    fun `getExerciseLogsForWorkouts returns empty list for empty input`() =
        runTest {
            val result = repository.getExerciseLogsForWorkouts(emptyList())
            assertThat(result).isEmpty()
        }

    @Test
    fun `getSetLogsForExercises returns empty list for empty input`() =
        runTest {
            val result = repository.getSetLogsForExercises(emptyList())
            assertThat(result).isEmpty()
        }

    @Test
    fun `getExercisesByIds returns empty list for empty input`() =
        runTest {
            val result = repository.getExercisesByIds(emptyList())
            assertThat(result).isEmpty()
        }

    // ===== Complete Workout Tests =====

    @Test
    fun `completeWorkout marks workout as completed when sets exist`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1", status = WorkoutStatus.IN_PROGRESS)
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id, isCompleted = true)

            repository.completeWorkout(workout.id, durationSeconds = "3600")

            val updated = workoutDao.getWorkoutById(workout.id)
            assertThat(updated?.status).isEqualTo(WorkoutStatus.COMPLETED)
            assertThat(updated?.durationSeconds).isEqualTo("3600")
        }

    @Test
    fun `completeWorkout does nothing when workout not found`() =
        runTest {
            repository.completeWorkout("nonexistent")
            // No exception thrown, operation completes gracefully
        }

    @Test
    fun `completeWorkout does nothing when workout already completed`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1", status = WorkoutStatus.COMPLETED)

            repository.completeWorkout(workout.id)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.COMPLETED)
        }

    @Test
    fun `completeWorkout does nothing when no sets completed`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1", status = WorkoutStatus.IN_PROGRESS)
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id, isCompleted = false)

            repository.completeWorkout(workout.id)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.IN_PROGRESS)
        }

    @Test
    fun `completeWorkout without duration sets status but no duration`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1", status = WorkoutStatus.IN_PROGRESS)
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id, isCompleted = true)

            repository.completeWorkout(workout.id)

            val updated = workoutDao.getWorkoutById(workout.id)
            assertThat(updated?.status).isEqualTo(WorkoutStatus.COMPLETED)
            assertThat(updated?.durationSeconds).isNull()
        }

    // ===== Delete Workout Tests =====

    @Test
    fun `deleteWorkout removes workout and cascaded data`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val ex1 = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            val ex2 = createAndInsertExerciseLog(id = "ex2", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = ex1.id)
            createAndInsertSetLog(id = "set2", exerciseLogId = ex1.id)
            createAndInsertSetLog(id = "set3", exerciseLogId = ex2.id)

            repository.deleteWorkout(workout.id)

            // Workout should be deleted (via Room cascade)
            val deletedWorkout = workoutDao.getWorkoutById(workout.id)
            assertThat(deletedWorkout).isNull()
        }

    @Test
    fun `deleteWorkout handles workout with no exercises`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")

            repository.deleteWorkout(workout.id)

            val deleted = workoutDao.getWorkoutById(workout.id)
            assertThat(deleted).isNull()
        }

    // ===== Set Log Insert/Update Tests =====

    @Test
    fun `insertSetLog rounds weights to nearest quarter`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)

            val setLog =
                SetLog(
                    id = "set1",
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 82.37f,
                    targetWeight = 80.13f,
                    actualReps = 10,
                )

            repository.insertSetLog(setLog)

            val inserted = setLogDao.getSetLogById("set1")
            // 82.37 should round to 82.25, 80.13 should round to 80.25
            assertThat(inserted?.actualWeight).isEqualTo(82.25f)
            assertThat(inserted?.targetWeight).isEqualTo(80.25f)
        }

    @Test
    fun `insertSetLog preserves RPE value`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)

            val setLog =
                SetLog(
                    id = "set1",
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 100f,
                    actualReps = 10,
                    targetRpe = 8.0f,
                    actualRpe = 7.0f,
                )

            repository.insertSetLog(setLog)

            val inserted = setLogDao.getSetLogById("set1")
            assertThat(inserted?.targetRpe).isEqualTo(8f)
            assertThat(inserted?.actualRpe).isEqualTo(7f)
        }

    @Test
    fun `insertSetLog sets userId to local when auth manager returns null`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)

            every { authManager.getCurrentUserId() } returns null

            val setLog =
                SetLog(
                    id = "set1",
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 100f,
                    actualReps = 10,
                )

            repository.insertSetLog(setLog)

            val inserted = setLogDao.getSetLogById("set1")
            assertThat(inserted?.userId).isEqualTo("local")
        }

    @Test
    fun `updateSetLog rounds weights to nearest quarter`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id, actualWeight = 100f)

            val setLog =
                SetLog(
                    id = "set1",
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 105.66f,
                    targetWeight = 102.88f,
                    actualReps = 8,
                )

            repository.updateSetLog(setLog)

            val updated = setLogDao.getSetLogById("set1")
            // 105.66 should round to 105.75, 102.88 should round to 103.0
            assertThat(updated?.actualWeight).isEqualTo(105.75f)
            assertThat(updated?.targetWeight).isEqualTo(103.0f)
        }

    @Test
    fun `updateSetLog preserves existing userId when provided`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id, userId = "existing-user-id")

            val setLog =
                SetLog(
                    id = "set1",
                    userId = "existing-user-id",
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 100f,
                    actualReps = 10,
                )

            repository.updateSetLog(setLog)

            val updated = setLogDao.getSetLogById("set1")
            assertThat(updated?.userId).isEqualTo("existing-user-id")
        }

    @Test
    fun `updateSetLog uses auth manager userId when setLog userId is null`() =
        runTest {
            val workout = createAndInsertWorkout(id = "workout1")
            val exerciseLog = createAndInsertExerciseLog(id = "ex1", workoutId = workout.id)
            createAndInsertSetLog(id = "set1", exerciseLogId = exerciseLog.id)

            every { authManager.getCurrentUserId() } returns "auth-user-id"

            val setLog =
                SetLog(
                    id = "set1",
                    userId = null,
                    exerciseLogId = exerciseLog.id,
                    setOrder = 0,
                    actualWeight = 100f,
                    actualReps = 10,
                )

            repository.updateSetLog(setLog)

            val updated = setLogDao.getSetLogById("set1")
            assertThat(updated?.userId).isEqualTo("auth-user-id")
        }

    // ===== Activate Programme Tests =====

    @Test
    fun `activateProgramme does nothing when programme not found`() =
        runTest {
            repository.activateProgramme("nonexistent")
            // No exception thrown, operation completes gracefully
        }

    // ===== Helper Methods =====

    private suspend fun createAndInsertProgramme(
        id: String = "test-programme",
        name: String = "Test Programme",
        isActive: Boolean = false,
        status: ProgrammeStatus = ProgrammeStatus.NOT_STARTED,
    ): Programme {
        val programme =
            Programme(
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
        programmeDao.insertProgramme(programme)
        return programme
    }

    private suspend fun createAndInsertWorkout(
        id: String = "test-workout",
        programmeId: String? = null,
        isProgrammeWorkout: Boolean = false,
        status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    ): Workout {
        val workout =
            Workout(
                id = id,
                userId = "test-user",
                date = LocalDateTime.now(),
                status = status,
                isProgrammeWorkout = isProgrammeWorkout,
                programmeId = programmeId,
            )
        workoutDao.insertWorkout(workout)
        return workout
    }

    private suspend fun createAndInsertExercise(
        id: String = "test-exercise",
        name: String = "Test Exercise",
    ): Exercise {
        val exercise =
            Exercise(
                id = id,
                name = name,
                type = ExerciseType.SYSTEM.name,
                category = "CHEST",
                movementPattern = "PUSH",
                equipment = "BARBELL",
            )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    private suspend fun createAndInsertExerciseLog(
        id: String = "test-exercise-log",
        workoutId: String,
        exerciseId: String = "test-exercise",
    ): ExerciseLog {
        val exerciseLog =
            ExerciseLog(
                id = id,
                userId = "test-user",
                workoutId = workoutId,
                exerciseId = exerciseId,
                exerciseOrder = 0,
            )
        exerciseLogDao.insertExerciseLog(exerciseLog)
        return exerciseLog
    }

    private suspend fun createAndInsertSetLog(
        id: String = "test-set-log",
        exerciseLogId: String,
        actualWeight: Float = 100f,
        actualReps: Int = 10,
        isCompleted: Boolean = true,
        userId: String = "test-user",
    ): SetLog {
        val setLog =
            SetLog(
                id = id,
                userId = userId,
                exerciseLogId = exerciseLogId,
                setOrder = 0,
                actualWeight = actualWeight,
                actualReps = actualReps,
                isCompleted = isCompleted,
            )
        setLogDao.insertSetLog(setLog)
        return setLog
    }

    private fun createDeviation(
        workoutId: String,
        programmeId: String,
        exerciseLogId: String,
    ) = com.github.radupana.featherweight.data.programme.WorkoutDeviation(
        workoutId = workoutId,
        programmeId = programmeId,
        exerciseLogId = exerciseLogId,
        deviationType = DeviationType.VOLUME_DEVIATION,
        deviationMagnitude = -0.2f,
        timestamp = LocalDateTime.now(),
    )
}
