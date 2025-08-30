package com.github.radupana.featherweight.service

import android.content.Context
import com.github.radupana.featherweight.BuildConfig
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMaxWithName
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WorkoutExportServiceTest {
    private lateinit var service: WorkoutExportService
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var oneRMDao: OneRMDao
    private lateinit var repository: FeatherweightRepository
    private lateinit var context: Context
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        workoutDao = mockk()
        exerciseLogDao = mockk()
        setLogDao = mockk()
        oneRMDao = mockk()
        repository = mockk()

        // Use Robolectric's RuntimeEnvironment to get a real Context
        context = RuntimeEnvironment.getApplication()

        // The cache dir will be a real directory provided by Robolectric
        cacheDir = context.cacheDir

        service =
            WorkoutExportService(
                workoutDao = workoutDao,
                exerciseLogDao = exerciseLogDao,
                setLogDao = setLogDao,
                oneRMDao = oneRMDao,
                repository = repository,
            )
    }

    @Test
    fun exportWorkoutsToFile_withNoWorkouts_createsEmptyExport() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 0
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns emptyList()

            var progressCalled = false

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> progressCalled = true },
                )

            // Assert
            assertThat(result).isNotNull()
            assertThat(result.exists()).isTrue()
            assertThat(progressCalled).isFalse() // No progress callbacks for empty export

            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            assertThat(json.has("metadata")).isTrue()
            assertThat(json.has("workouts")).isTrue()
            assertThat(json.getAsJsonArray("workouts").size()).isEqualTo(0)

            val metadata = json.getAsJsonObject("metadata")
            assertThat(metadata.get("totalWorkouts").asInt).isEqualTo(0)
            assertThat(metadata.get("appVersion").asString).isEqualTo(BuildConfig.VERSION_NAME)
        }

    @Test
    fun exportWorkoutsToFile_withSingleWorkout_exportsCorrectly() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = true,
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Morning Workout",
                    status = WorkoutStatus.COMPLETED,
                    durationSeconds = 3600,
                    isProgrammeWorkout = false,
                    programmeId = null,
                )

            val exerciseLog =
                ExerciseLog(
                    id = 1,
                    workoutId = 1,
                    exerciseVariationId = 10,
                    exerciseOrder = 1,
                    notes = "Felt strong today",
                    supersetGroup = null,
                )

            val setLog1 =
                SetLog(
                    id = 1,
                    exerciseLogId = 1,
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 50f,
                    actualReps = 10,
                    actualWeight = 50f,
                    actualRpe = 7.5f,
                    isCompleted = true,
                )

            val setLog2 =
                SetLog(
                    id = 2,
                    exerciseLogId = 1,
                    setOrder = 2,
                    targetReps = 10,
                    targetWeight = 50f,
                    actualReps = 8,
                    actualWeight = 50f,
                    actualRpe = 8.5f,
                    isCompleted = true,
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = 10,
                    coreExerciseId = 1,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    usageCount = 5,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns listOf(setLog1, setLog2)
            coEvery { repository.getExerciseById(10) } returns exerciseVariation

            var progressUpdates = mutableListOf<Pair<Int, Int>>()

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { current, total -> progressUpdates.add(current to total) },
                )

            // Assert
            assertThat(result.exists()).isTrue()
            assertThat(progressUpdates).contains(1 to 1)

            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            assertThat(workouts.size()).isEqualTo(1)

            val exportedWorkout = workouts[0].asJsonObject
            assertThat(exportedWorkout.get("id").asLong).isEqualTo(1)
            assertThat(exportedWorkout.get("name").asString).isEqualTo("Morning Workout")
            assertThat(exportedWorkout.get("duration").asInt).isEqualTo(3600)
            assertThat(exportedWorkout.get("status").asString).isEqualTo("COMPLETED")

            val exercises = exportedWorkout.getAsJsonArray("exercises")
            assertThat(exercises.size()).isEqualTo(1)

            val exportedExercise = exercises[0].asJsonObject
            assertThat(exportedExercise.get("exerciseName").asString).isEqualTo("Barbell Bench Press")
            assertThat(exportedExercise.get("notes").asString).isEqualTo("Felt strong today")

            val sets = exportedExercise.getAsJsonArray("sets")
            assertThat(sets.size()).isEqualTo(2)

            val set1 = sets[0].asJsonObject
            assertThat(set1.get("setNumber").asInt).isEqualTo(1)
            assertThat(set1.get("actualReps").asInt).isEqualTo(10)
            assertThat(set1.get("actualWeight").asFloat).isEqualTo(50f)
            assertThat(set1.get("rpe").asFloat).isEqualTo(7.5f)

            val set2 = sets[1].asJsonObject
            assertThat(set2.get("setNumber").asInt).isEqualTo(2)
            assertThat(set2.get("actualReps").asInt).isEqualTo(8)
            assertThat(set2.get("actualWeight").asFloat).isEqualTo(50f)
            assertThat(set2.get("rpe").asFloat).isEqualTo(8.5f)
        }

    @Test
    fun exportWorkoutsToFile_withProgrammeWorkout_includesProgrammeInfo() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Week 1 Day 1",
                    status = WorkoutStatus.COMPLETED,
                    isProgrammeWorkout = true,
                    programmeId = 5,
                    programmeWorkoutName = "5/3/1 BBB",
                    weekNumber = 1,
                    dayNumber = 1,
                    durationSeconds = null,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns emptyList()

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            val exportedWorkout = workouts[0].asJsonObject

            assertThat(exportedWorkout.has("programmeInfo")).isTrue()

            val programmeInfo = exportedWorkout.getAsJsonObject("programmeInfo")
            assertThat(programmeInfo.get("programmeName").asString).isEqualTo("5/3/1 BBB")
            assertThat(programmeInfo.get("weekNumber").asInt).isEqualTo(1)
            assertThat(programmeInfo.get("dayNumber").asInt).isEqualTo(1)
        }

    @Test
    fun exportWorkoutsToFile_withOneRepMaxes_includesUserProfile() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = true,
                    includeNotes = false,
                )

            val oneRM1 =
                UserExerciseMaxWithName(
                    id = 1,
                    exerciseVariationId = 1,
                    exerciseName = "Squat",
                    mostWeightLifted = 145f,
                    mostWeightReps = 1,
                    mostWeightRpe = 9f,
                    mostWeightDate = LocalDateTime.of(2024, 5, 1, 0, 0),
                    oneRMEstimate = 150f,
                    oneRMContext = "Test context",
                    oneRMConfidence = 0.95f,
                    oneRMDate = LocalDateTime.of(2024, 5, 1, 0, 0),
                    oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
                    notes = null,
                )

            val oneRM2 =
                UserExerciseMaxWithName(
                    id = 2,
                    exerciseVariationId = 2,
                    exerciseName = "Bench Press",
                    mostWeightLifted = 95f,
                    mostWeightReps = 2,
                    mostWeightRpe = 8.5f,
                    mostWeightDate = LocalDateTime.of(2024, 5, 15, 0, 0),
                    oneRMEstimate = 100f,
                    oneRMContext = "Test context",
                    oneRMConfidence = 0.9f,
                    oneRMDate = LocalDateTime.of(2024, 5, 15, 0, 0),
                    oneRMType = OneRMType.MANUALLY_ENTERED,
                    notes = null,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 0
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns emptyList()

            coEvery { oneRMDao.getAllCurrentMaxesForExport() } returns listOf(oneRM1, oneRM2)

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            assertThat(json.has("userProfile")).isTrue()

            val userProfile = json.getAsJsonObject("userProfile")
            assertThat(userProfile.has("oneRepMaxHistory")).isTrue()

            val oneRMHistory = userProfile.getAsJsonArray("oneRepMaxHistory")
            assertThat(oneRMHistory.size()).isEqualTo(2)

            val max1 = oneRMHistory[0].asJsonObject
            assertThat(max1.get("exerciseName").asString).isEqualTo("Squat")
            assertThat(max1.get("weight").asFloat).isEqualTo(150f)

            val max2 = oneRMHistory[1].asJsonObject
            assertThat(max2.get("exerciseName").asString).isEqualTo("Bench Press")
            assertThat(max2.get("weight").asFloat).isEqualTo(100f)
        }

    @Test
    fun exportWorkoutsToFile_withBodyweightOption_includesEmptyBodyweightHistory() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = true,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 0
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns emptyList()

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val userProfile = json.getAsJsonObject("userProfile")
            assertThat(userProfile.has("bodyweightHistory")).isTrue()

            val bodyweightHistory = userProfile.getAsJsonArray("bodyweightHistory")
            assertThat(bodyweightHistory.size()).isEqualTo(0) // Currently not tracked
        }

    @Test
    fun exportWorkoutsToFile_withLargeDataset_paginatesCorrectly() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            // Create 120 workouts (more than 2 pages)
            val firstPageWorkouts =
                (1..50).map { id ->
                    Workout(
                        id = id.toLong(),
                        date = LocalDateTime.of(2024, 1, (id - 1) % 31 + 1, 10, 0),
                        name = "Workout $id",
                        status = WorkoutStatus.COMPLETED,
                        isProgrammeWorkout = false,
                        programmeId = null,
                        durationSeconds = null,
                    )
                }

            val secondPageWorkouts =
                (51..100).map { id ->
                    Workout(
                        id = id.toLong(),
                        date = LocalDateTime.of(2024, 2, (id - 51) % 28 + 1, 10, 0),
                        name = "Workout $id",
                        status = WorkoutStatus.COMPLETED,
                        isProgrammeWorkout = false,
                        programmeId = null,
                        durationSeconds = null,
                    )
                }

            val thirdPageWorkouts =
                (101..120).map { id ->
                    Workout(
                        id = id.toLong(),
                        date = LocalDateTime.of(2024, 3, (id - 101) % 31 + 1, 10, 0),
                        name = "Workout $id",
                        status = WorkoutStatus.COMPLETED,
                        isProgrammeWorkout = false,
                        programmeId = null,
                        durationSeconds = null,
                    )
                }

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 120

            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns firstPageWorkouts

            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns secondPageWorkouts

            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    100,
                )
            } returns thirdPageWorkouts

            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    150,
                )
            } returns emptyList()

            // Mock exercise logs for all workouts
            (1..120L).forEach { workoutId ->
                coEvery { exerciseLogDao.getExerciseLogsForWorkout(workoutId) } returns emptyList()
            }

            val progressUpdates = mutableListOf<Pair<Int, Int>>()

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { current, total -> progressUpdates.add(current to total) },
                )

            // Assert
            assertThat(result.exists()).isTrue()

            // Verify all pages were requested
            coVerify { workoutDao.getWorkoutsInDateRangePaged(startDate, endDate, WorkoutStatus.NOT_STARTED, 50, 0) }
            coVerify { workoutDao.getWorkoutsInDateRangePaged(startDate, endDate, WorkoutStatus.NOT_STARTED, 50, 50) }
            coVerify { workoutDao.getWorkoutsInDateRangePaged(startDate, endDate, WorkoutStatus.NOT_STARTED, 50, 100) }
            coVerify { workoutDao.getWorkoutsInDateRangePaged(startDate, endDate, WorkoutStatus.NOT_STARTED, 50, 150) }

            // Verify progress was reported correctly
            assertThat(progressUpdates).contains(1 to 120)
            assertThat(progressUpdates).contains(50 to 120)
            assertThat(progressUpdates).contains(100 to 120)
            assertThat(progressUpdates).contains(120 to 120)

            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            assertThat(workouts.size()).isEqualTo(120)
        }

    @Test
    fun exportWorkoutsToFile_withSuperset_exportsSupersetGroup() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Superset Workout",
                    status = WorkoutStatus.COMPLETED,
                    isProgrammeWorkout = false,
                    programmeId = null,
                    durationSeconds = null,
                )

            val exercise1 =
                ExerciseLog(
                    id = 1,
                    workoutId = 1,
                    exerciseVariationId = 10,
                    exerciseOrder = 1,
                    notes = null,
                    supersetGroup = 1,
                )

            val exercise2 =
                ExerciseLog(
                    id = 2,
                    workoutId = 1,
                    exerciseVariationId = 11,
                    exerciseOrder = 2,
                    notes = null,
                    supersetGroup = 1,
                )

            val exerciseVariation1 =
                ExerciseVariation(
                    id = 10,
                    coreExerciseId = 1,
                    name = "Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    usageCount = 0,
                )

            val exerciseVariation2 =
                ExerciseVariation(
                    id = 11,
                    coreExerciseId = 2,
                    name = "Barbell Row",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    usageCount = 0,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exercise1, exercise2)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns emptyList()
            coEvery { setLogDao.getSetLogsForExercise(2) } returns emptyList()
            coEvery { repository.getExerciseById(10) } returns exerciseVariation1
            coEvery { repository.getExerciseById(11) } returns exerciseVariation2

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            val exportedWorkout = workouts[0].asJsonObject
            val exercises = exportedWorkout.getAsJsonArray("exercises")

            assertThat(exercises.size()).isEqualTo(2)

            val exportedExercise1 = exercises[0].asJsonObject
            assertThat(exportedExercise1.get("supersetGroup").asInt).isEqualTo(1)

            val exportedExercise2 = exercises[1].asJsonObject
            assertThat(exportedExercise2.get("supersetGroup").asInt).isEqualTo(1)
        }

    @Test
    fun exportWorkoutsToFile_withNotesDisabled_excludesNotes() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false, // Notes disabled
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Workout",
                    status = WorkoutStatus.COMPLETED,
                    isProgrammeWorkout = false,
                    programmeId = null,
                    durationSeconds = null,
                )

            val exerciseLog =
                ExerciseLog(
                    id = 1,
                    workoutId = 1,
                    exerciseVariationId = 10,
                    exerciseOrder = 1,
                    notes = "This note should not be exported",
                    supersetGroup = null,
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = 10,
                    coreExerciseId = 1,
                    name = "Squat",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    usageCount = 0,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns emptyList()
            coEvery { repository.getExerciseById(10) } returns exerciseVariation

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            val exportedWorkout = workouts[0].asJsonObject
            val exercises = exportedWorkout.getAsJsonArray("exercises")
            val exportedExercise = exercises[0].asJsonObject

            assertThat(exportedExercise.has("notes")).isFalse()
        }

    @Test
    fun exportWorkoutsToFile_withUnknownExercise_handlesGracefully() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Workout",
                    status = WorkoutStatus.COMPLETED,
                    isProgrammeWorkout = false,
                    programmeId = null,
                    durationSeconds = null,
                )

            val exerciseLog =
                ExerciseLog(
                    id = 1,
                    workoutId = 1,
                    exerciseVariationId = 999, // Non-existent exercise
                    exerciseOrder = 1,
                    notes = null,
                    supersetGroup = null,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns emptyList()
            coEvery { repository.getExerciseById(999) } returns null // Exercise not found

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            val exportedWorkout = workouts[0].asJsonObject
            val exercises = exportedWorkout.getAsJsonArray("exercises")
            val exportedExercise = exercises[0].asJsonObject

            assertThat(exportedExercise.get("exerciseName").asString).isEqualTo("Unknown Exercise")
        }

    @Test
    fun exportWorkoutsToFile_withIncompleteWorkout_exportsCorrectly() =
        runBlocking {
            // Arrange
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2024, 12, 31, 23, 59)
            val exportOptions =
                ExportOptions(
                    includeBodyweight = false,
                    includeOneRepMaxes = false,
                    includeNotes = false,
                )

            val workout =
                Workout(
                    id = 1,
                    date = LocalDateTime.of(2024, 6, 15, 10, 0),
                    name = "Incomplete Workout",
                    status = WorkoutStatus.IN_PROGRESS,
                    isProgrammeWorkout = false,
                    programmeId = null,
                    durationSeconds = 1800, // 30 minutes in progress
                )

            val exerciseLog =
                ExerciseLog(
                    id = 1,
                    workoutId = 1,
                    exerciseVariationId = 10,
                    exerciseOrder = 1,
                    notes = null,
                    supersetGroup = null,
                )

            val completedSet =
                SetLog(
                    id = 1,
                    exerciseLogId = 1,
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 50f,
                    actualReps = 10,
                    actualWeight = 50f,
                    actualRpe = 7f,
                    isCompleted = true,
                )

            val incompleteSet =
                SetLog(
                    id = 2,
                    exerciseLogId = 1,
                    setOrder = 2,
                    targetReps = 10,
                    targetWeight = 50f,
                    actualReps = 0,
                    actualWeight = 0f,
                    actualRpe = null,
                    isCompleted = false,
                )

            val exerciseVariation =
                ExerciseVariation(
                    id = 10,
                    coreExerciseId = 1,
                    name = "Deadlift",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.ADVANCED,
                    requiresWeight = true,
                    usageCount = 0,
                )

            coEvery { workoutDao.getWorkoutCountInDateRange(startDate, endDate) } returns 1
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    0,
                )
            } returns listOf(workout)
            coEvery {
                workoutDao.getWorkoutsInDateRangePaged(
                    startDate,
                    endDate,
                    WorkoutStatus.NOT_STARTED,
                    50,
                    50,
                )
            } returns emptyList()

            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns listOf(completedSet, incompleteSet)
            coEvery { repository.getExerciseById(10) } returns exerciseVariation

            // Act
            val result =
                service.exportWorkoutsToFile(
                    context = context,
                    startDate = startDate,
                    endDate = endDate,
                    exportOptions = exportOptions,
                    onProgress = { _, _ -> },
                )

            // Assert
            val jsonContent = result.readText()
            val json = JsonParser.parseString(jsonContent).asJsonObject

            val workouts = json.getAsJsonArray("workouts")
            val exportedWorkout = workouts[0].asJsonObject

            assertThat(exportedWorkout.get("status").asString).isEqualTo("IN_PROGRESS")
            assertThat(exportedWorkout.get("duration").asInt).isEqualTo(1800)

            val exercises = exportedWorkout.getAsJsonArray("exercises")
            val exportedExercise = exercises[0].asJsonObject
            val sets = exportedExercise.getAsJsonArray("sets")

            assertThat(sets.size()).isEqualTo(2)

            val set1 = sets[0].asJsonObject
            assertThat(set1.get("completed").asBoolean).isTrue()
            assertThat(set1.get("actualReps").asInt).isEqualTo(10)

            val set2 = sets[1].asJsonObject
            assertThat(set2.get("completed").asBoolean).isFalse()
            assertThat(set2.get("actualReps").asInt).isEqualTo(0)
        }
}
