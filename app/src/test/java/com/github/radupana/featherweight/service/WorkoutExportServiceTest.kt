package com.github.radupana.featherweight.service

import android.content.Context
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMaxWithName
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WorkoutExportServiceTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var service: WorkoutExportService
    private val mockWorkoutDao: WorkoutDao = mockk(relaxed = true)
    private val mockExerciseLogDao: ExerciseLogDao = mockk(relaxed = true)
    private val mockSetLogDao: SetLogDao = mockk(relaxed = true)
    private val mockOneRMDao: ExerciseMaxTrackingDao = mockk(relaxed = true)
    private val mockRepository: FeatherweightRepository = mockk(relaxed = true)
    private val mockAuthManager: AuthenticationManager = mockk(relaxed = true)
    private val mockWeightUnitManager: WeightUnitManager = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        every { mockContext.cacheDir } returns tempFolder.root
        every { mockAuthManager.getCurrentUserId() } returns "test-user"
        every { mockWeightUnitManager.getCurrentUnit() } returns WeightUnit.KG
        every { mockWeightUnitManager.convertFromKg(any()) } answers { firstArg() }

        service =
            WorkoutExportService(
                workoutDao = mockWorkoutDao,
                exerciseLogDao = mockExerciseLogDao,
                setLogDao = mockSetLogDao,
                oneRMDao = mockOneRMDao,
                repository = mockRepository,
                authManager = mockAuthManager,
                weightUnitManager = mockWeightUnitManager,
            )
    }

    @Test
    fun `exportSingleWorkout generates valid JSON file`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)
            val setLogs = createTestSetLogs("ex1")

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns setLogs
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = true,
                    includeProfile = false,
                )

            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            assertThat(file.exists()).isTrue()
            assertThat(file.name).contains("workout_")
            assertThat(file.extension).isEqualTo("json")

            val json = JSONObject(file.readText())
            assertThat(json.has("metadata")).isTrue()
            assertThat(json.has("workouts")).isTrue()

            val metadata = json.getJSONObject("metadata")
            assertThat(metadata.getString("exportType")).isEqualTo("single_workout")
            assertThat(metadata.getString("weightUnit")).isEqualTo("KG")

            val workouts = json.getJSONArray("workouts")
            assertThat(workouts.length()).isEqualTo(1)

            val exportedWorkout = workouts.getJSONObject(0)
            assertThat(exportedWorkout.getString("id")).isEqualTo(workoutId)
            assertThat(exportedWorkout.has("exercises")).isTrue()
        }

    @Test
    fun `exportSingleWorkout throws exception when workout not found`() =
        runTest {
            coEvery { mockWorkoutDao.getWorkoutById("nonexistent") } returns null

            val exportOptions = ExportOptions()

            try {
                service.exportSingleWorkout(mockContext, "nonexistent", exportOptions)
                error("Should have thrown IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).isEqualTo("Workout not found")
            }
        }

    @Test
    fun `exportSingleWorkout includes exercise data correctly`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)
            val setLogs =
                listOf(
                    createTestSetLog("set1", "ex1", 1, 100f, 5, 100f, 5),
                    createTestSetLog("set2", "ex1", 2, 100f, 5, 100f, 4),
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns setLogs
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exportedWorkout = workouts.getJSONObject(0)
            val exercises = exportedWorkout.getJSONArray("exercises")

            assertThat(exercises.length()).isEqualTo(1)

            val exercise = exercises.getJSONObject(0)
            assertThat(exercise.getString("exerciseId")).isEqualTo("exercise1")
            assertThat(exercise.getString("exerciseName")).isEqualTo("Barbell Back Squat")

            val sets = exercise.getJSONArray("sets")
            assertThat(sets.length()).isEqualTo(2)

            val firstSet = sets.getJSONObject(0)
            assertThat(firstSet.getInt("setNumber")).isEqualTo(1)
            assertThat(firstSet.getDouble("targetWeight")).isEqualTo(100.0)
            assertThat(firstSet.getInt("targetReps")).isEqualTo(5)
            assertThat(firstSet.getInt("actualReps")).isEqualTo(5)
            assertThat(firstSet.getBoolean("completed")).isTrue()
        }

    @Test
    fun `exportSingleWorkout handles programme workout metadata`() =
        runTest {
            val workoutId = "workout1"
            val workout =
                Workout(
                    id = workoutId,
                    date = LocalDateTime.now(),
                    name = "Test Workout",
                    notes = null,
                    notesUpdatedAt = null,
                    programmeId = "prog1",
                    weekNumber = 2,
                    dayNumber = 3,
                    programmeWorkoutName = "Upper Body A",
                    isProgrammeWorkout = true,
                    status = WorkoutStatus.COMPLETED,
                    durationSeconds = "3600",
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exportedWorkout = workouts.getJSONObject(0)

            assertThat(exportedWorkout.has("programmeInfo")).isTrue()

            val programmeInfo = exportedWorkout.getJSONObject("programmeInfo")
            assertThat(programmeInfo.getString("programmeName")).isEqualTo("Upper Body A")
            assertThat(programmeInfo.getInt("weekNumber")).isEqualTo(2)
            assertThat(programmeInfo.getInt("dayNumber")).isEqualTo(3)
        }

    @Test
    fun `exportSingleWorkout includes notes when option enabled`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog =
                ExerciseLog(
                    id = "ex1",
                    workoutId = workoutId,
                    exerciseId = "exercise1",
                    exerciseOrder = 0,
                    notes = "Felt strong today",
                    originalExerciseId = null,
                    isSwapped = false,
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns createTestSetLogs("ex1")
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions(includeNotes = true)
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val exercise = exercises.getJSONObject(0)

            assertThat(exercise.has("notes")).isTrue()
            assertThat(exercise.getString("notes")).isEqualTo("Felt strong today")
        }

    @Test
    fun `exportSingleWorkout excludes notes when option disabled`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog =
                ExerciseLog(
                    id = "ex1",
                    workoutId = workoutId,
                    exerciseId = "exercise1",
                    exerciseOrder = 0,
                    notes = "Should not appear",
                    originalExerciseId = null,
                    isSwapped = false,
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns createTestSetLogs("ex1")
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions(includeNotes = false)
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val exercise = exercises.getJSONObject(0)

            assertThat(exercise.has("notes")).isFalse()
        }

    @Test
    fun `exportSingleWorkout includes 1RM data when requested`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()
            coEvery { mockOneRMDao.getAllCurrentMaxesForExport("test-user") } returns
                listOf(
                    UserExerciseMaxWithName(
                        id = "max1",
                        exerciseId = "exercise1",
                        exerciseName = "Barbell Back Squat",
                        mostWeightLifted = 120f,
                        mostWeightReps = 3,
                        mostWeightRpe = 9f,
                        mostWeightDate = LocalDateTime.now(),
                        oneRMEstimate = 140f,
                        oneRMContext = "3×120kg @ RPE 9",
                        oneRMConfidence = 0.85f,
                        oneRMDate = LocalDateTime.now(),
                        oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                        notes = null,
                    ),
                )

            val exportOptions =
                ExportOptions(
                    includeProfile = true,
                    includeOneRepMaxes = true,
                )
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            assertThat(json.has("userProfile")).isTrue()

            val userProfile = json.getJSONObject("userProfile")
            assertThat(userProfile.has("oneRepMaxHistory")).isTrue()

            val oneRMHistory = userProfile.getJSONArray("oneRepMaxHistory")
            assertThat(oneRMHistory.length()).isEqualTo(1)

            val oneRM = oneRMHistory.getJSONObject(0)
            assertThat(oneRM.getString("exerciseId")).isEqualTo("exercise1")
            assertThat(oneRM.getString("exerciseName")).isEqualTo("Barbell Back Squat")
            assertThat(oneRM.getDouble("weight")).isEqualTo(140.0)
        }

    @Test
    fun `exportSingleWorkout converts weight units correctly`() =
        runTest {
            every { mockWeightUnitManager.getCurrentUnit() } returns WeightUnit.LBS
            every { mockWeightUnitManager.convertFromKg(100f) } returns 220.46f
            every { mockWeightUnitManager.convertFromKg(140f) } returns 308.64f

            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)
            val setLogs =
                listOf(
                    createTestSetLog("set1", "ex1", 1, 100f, 5, 100f, 5),
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns setLogs
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val metadata = json.getJSONObject("metadata")
            assertThat(metadata.getString("weightUnit")).isEqualTo("LBS")

            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val sets = exercises.getJSONObject(0).getJSONArray("sets")
            val firstSet = sets.getJSONObject(0)

            assertThat(firstSet.getDouble("targetWeight")).isEqualTo(220.46)
            assertThat(firstSet.getDouble("actualWeight")).isEqualTo(220.46)
        }

    @Test
    fun `exportWorkoutsToFile generates valid JSON with multiple workouts`() =
        runTest {
            val startDate = LocalDateTime.now().minusDays(7)
            val endDate = LocalDateTime.now()
            val workouts =
                listOf(
                    createTestWorkout("w1", startDate),
                    createTestWorkout("w2", startDate.plusDays(3)),
                )

            coEvery { mockWorkoutDao.getWorkoutCountInDateRange("test-user", startDate, endDate) } returns 2
            coEvery {
                mockWorkoutDao.getWorkoutsInDateRangePaged(
                    "test-user",
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns workouts
            coEvery {
                mockWorkoutDao.getWorkoutsInDateRangePaged(
                    "test-user",
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()

            val exportOptions = ExportOptions(includeProfile = false)
            var currentProgress = 0
            var totalProgress = 0

            val file =
                service.exportWorkoutsToFile(
                    mockContext,
                    startDate,
                    endDate,
                    exportOptions,
                ) { current, total ->
                    currentProgress = current
                    totalProgress = total
                }

            assertThat(file.exists()).isTrue()
            assertThat(currentProgress).isEqualTo(2)
            assertThat(totalProgress).isEqualTo(2)

            val json = JSONObject(file.readText())
            val metadata = json.getJSONObject("metadata")
            assertThat(metadata.getInt("totalWorkouts")).isEqualTo(2)

            val exportedWorkouts = json.getJSONArray("workouts")
            assertThat(exportedWorkouts.length()).isEqualTo(2)
        }

    @Test
    fun `exportWorkoutsToFile isolates data by user`() =
        runTest {
            val startDate = LocalDateTime.now().minusDays(7)
            val endDate = LocalDateTime.now()

            coEvery { mockWorkoutDao.getWorkoutCountInDateRange("test-user", startDate, endDate) } returns 0
            coEvery {
                mockWorkoutDao.getWorkoutsInDateRangePaged(
                    "test-user",
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns emptyList()

            val exportOptions = ExportOptions()
            val file =
                service.exportWorkoutsToFile(
                    mockContext,
                    startDate,
                    endDate,
                    exportOptions,
                ) { _, _ -> }

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            assertThat(workouts.length()).isEqualTo(0)
        }

    @Test
    fun `exportProgrammeWorkouts exports only completed programme workouts`() =
        runTest {
            val programmeId = "prog1"
            val workouts =
                listOf(
                    Workout(
                        id = "w1",
                        date = LocalDateTime.now().minusDays(2),
                        name = "Programme Day 1",
                        notes = null,
                        notesUpdatedAt = null,
                        programmeId = programmeId,
                        weekNumber = 1,
                        dayNumber = 1,
                        programmeWorkoutName = "Test Programme",
                        isProgrammeWorkout = true,
                        status = WorkoutStatus.COMPLETED,
                        durationSeconds = "3600",
                        timerStartTime = null,
                        timerElapsedSeconds = 0,
                    ),
                )

            coEvery { mockWorkoutDao.getCompletedWorkoutsByProgramme(programmeId) } returns workouts
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()

            val exportOptions = ExportOptions()
            var progressCalled = false

            val file =
                service.exportProgrammeWorkouts(
                    mockContext,
                    programmeId,
                    exportOptions,
                ) { current, total ->
                    progressCalled = true
                    assertThat(total).isEqualTo(1)
                }

            assertThat(progressCalled).isTrue()
            assertThat(file.exists()).isTrue()

            val json = JSONObject(file.readText())
            val metadata = json.getJSONObject("metadata")
            assertThat(metadata.getString("exportType")).isEqualTo("programme")
            assertThat(metadata.getString("programmeName")).isEqualTo("Test Programme")
            assertThat(metadata.getInt("totalWorkouts")).isEqualTo(1)
        }

    @Test
    fun `exportProgrammeWorkouts throws exception when no workouts found`() =
        runTest {
            coEvery { mockWorkoutDao.getCompletedWorkoutsByProgramme("empty-prog") } returns emptyList()

            val exportOptions = ExportOptions()

            try {
                service.exportProgrammeWorkouts(
                    mockContext,
                    "empty-prog",
                    exportOptions,
                ) { _, _ -> }
                error("Should have thrown IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).isEqualTo("No completed workouts found for programme")
            }
        }

    @Test
    fun `exportSingleWorkout handles unknown exercise gracefully`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns createTestSetLogs("ex1")
            coEvery { mockRepository.getExerciseById("exercise1") } returns null

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val exercise = exercises.getJSONObject(0)

            assertThat(exercise.getString("exerciseName")).isEqualTo("Unknown Exercise")
        }

    @Test
    fun `exportSingleWorkout handles sets with RPE correctly`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)
            val setLogs =
                listOf(
                    SetLog(
                        id = "set1",
                        userId = "test-user",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        actualRpe = 8.5f,
                        tag = null,
                        notes = null,
                        isCompleted = true,
                        completedAt = null,
                    ),
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns setLogs
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val sets = exercises.getJSONObject(0).getJSONArray("sets")
            val firstSet = sets.getJSONObject(0)

            assertThat(firstSet.has("rpe")).isTrue()
            assertThat(firstSet.getDouble("rpe")).isEqualTo(8.5)
        }

    @Test
    fun `exportSingleWorkout omits RPE when null`() =
        runTest {
            val workoutId = "workout1"
            val workout = createTestWorkout(workoutId)
            val exerciseLog = createTestExerciseLog("ex1", workoutId)
            val setLogs =
                listOf(
                    createTestSetLog("set1", "ex1", 1, 100f, 5, 100f, 5, rpe = null),
                )

            coEvery { mockWorkoutDao.getWorkoutById(workoutId) } returns workout
            coEvery { mockExerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns listOf(exerciseLog)
            coEvery { mockSetLogDao.getSetLogsForExercise("ex1") } returns setLogs
            coEvery { mockRepository.getExerciseById("exercise1") } returns createTestExercise()

            val exportOptions = ExportOptions()
            val file = service.exportSingleWorkout(mockContext, workoutId, exportOptions)

            val json = JSONObject(file.readText())
            val workouts = json.getJSONArray("workouts")
            val exercises = workouts.getJSONObject(0).getJSONArray("exercises")
            val sets = exercises.getJSONObject(0).getJSONArray("sets")
            val firstSet = sets.getJSONObject(0)

            assertThat(firstSet.has("rpe")).isFalse()
        }

    private fun createTestWorkout(
        id: String,
        date: LocalDateTime = LocalDateTime.now(),
    ) = Workout(
        id = id,
        date = date,
        name = "Test Workout",
        notes = null,
        notesUpdatedAt = null,
        programmeId = null,
        weekNumber = null,
        dayNumber = null,
        programmeWorkoutName = null,
        isProgrammeWorkout = false,
        status = WorkoutStatus.COMPLETED,
        durationSeconds = "3600",
        timerStartTime = null,
        timerElapsedSeconds = 0,
    )

    private fun createTestExerciseLog(
        id: String,
        workoutId: String,
    ) = ExerciseLog(
        id = id,
        workoutId = workoutId,
        exerciseId = "exercise1",
        exerciseOrder = 0,
        notes = null,
        originalExerciseId = null,
        isSwapped = false,
    )

    private fun createTestSetLogs(exerciseLogId: String) =
        listOf(
            createTestSetLog("set1", exerciseLogId, 1, 100f, 5, 100f, 5),
            createTestSetLog("set2", exerciseLogId, 2, 100f, 5, 100f, 5),
        )

    private fun createTestSetLog(
        id: String,
        exerciseLogId: String,
        order: Int,
        targetWeight: Float,
        targetReps: Int,
        actualWeight: Float,
        actualReps: Int,
        rpe: Float? = 8f,
    ) = SetLog(
        id = id,
        userId = "test-user",
        exerciseLogId = exerciseLogId,
        setOrder = order,
        targetReps = targetReps,
        targetWeight = targetWeight,
        actualReps = actualReps,
        actualWeight = actualWeight,
        actualRpe = rpe,
        tag = null,
        notes = null,
        isCompleted = true,
        completedAt = null,
    )

    private fun createTestExercise() =
        Exercise(
            id = "exercise1",
            name = "Barbell Back Squat",
            category = "LEGS",
            movementPattern = "SQUAT",
            equipment = "BARBELL",
        )
}
