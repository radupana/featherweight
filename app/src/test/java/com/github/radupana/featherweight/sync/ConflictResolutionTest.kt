package com.github.radupana.featherweight.sync

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ParseRequestDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.ProgrammeExerciseTrackingDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.TemplateExerciseDao
import com.github.radupana.featherweight.data.TemplateSetDao
import com.github.radupana.featherweight.data.TrainingAnalysisDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.WorkoutTemplateDao
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConflictResolutionTest {
    private lateinit var context: Context
    private lateinit var database: FeatherweightDatabase
    private lateinit var authManager: AuthenticationManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var exerciseInstructionDao: ExerciseInstructionDao
    private lateinit var exerciseAliasDao: ExerciseAliasDao
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var oneRMDao: ExerciseMaxTrackingDao
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var exerciseSwapHistoryDao: ExerciseSwapHistoryDao
    private lateinit var programmeExerciseTrackingDao: ProgrammeExerciseTrackingDao
    private lateinit var globalExerciseProgressDao: GlobalExerciseProgressDao
    private lateinit var trainingAnalysisDao: TrainingAnalysisDao
    private lateinit var parseRequestDao: ParseRequestDao
    private lateinit var workoutTemplateDao: WorkoutTemplateDao
    private lateinit var templateExerciseDao: TemplateExerciseDao
    private lateinit var templateSetDao: TemplateSetDao
    private lateinit var localSyncMetadataDao: com.github.radupana.featherweight.data.LocalSyncMetadataDao
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var syncManager: SyncManager

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            val str = arg<CharSequence?>(0)
            str == null || str.isEmpty()
        }

        context = mockk()
        database = mockk()
        authManager = mockk()
        firestoreRepository = mockk()
        workoutDao = mockk()
        exerciseLogDao = mockk()
        setLogDao = mockk()
        exerciseDao = mockk()
        exerciseMuscleDao = mockk()
        exerciseInstructionDao = mockk()
        exerciseAliasDao = mockk()
        programmeDao = mockk()
        oneRMDao = mockk()
        personalRecordDao = mockk()
        userExerciseUsageDao = mockk()
        exerciseSwapHistoryDao = mockk()
        programmeExerciseTrackingDao = mockk()
        globalExerciseProgressDao = mockk()
        trainingAnalysisDao = mockk()
        parseRequestDao = mockk()
        workoutTemplateDao = mockk()
        templateExerciseDao = mockk()
        templateSetDao = mockk()
        localSyncMetadataDao = mockk()
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()

        every { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.getString("installation_id", null) } returns "test-installation-id"
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just runs

        val syncPrefs = mockk<SharedPreferences>()
        val syncPrefsEditor = mockk<SharedPreferences.Editor>()
        every { context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE) } returns syncPrefs
        every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns 0L
        every { syncPrefs.edit() } returns syncPrefsEditor
        every { syncPrefsEditor.putLong("last_successful_sync_time", any()) } returns syncPrefsEditor
        every { syncPrefsEditor.apply() } just runs

        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.exerciseDao() } returns exerciseDao
        every { database.exerciseMuscleDao() } returns exerciseMuscleDao
        every { database.exerciseInstructionDao() } returns exerciseInstructionDao
        every { database.exerciseAliasDao() } returns exerciseAliasDao
        every { database.programmeDao() } returns programmeDao
        every { database.exerciseMaxTrackingDao() } returns oneRMDao
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.userExerciseUsageDao() } returns userExerciseUsageDao
        every { database.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { database.programmeExerciseTrackingDao() } returns programmeExerciseTrackingDao
        every { database.globalExerciseProgressDao() } returns globalExerciseProgressDao
        every { database.trainingAnalysisDao() } returns trainingAnalysisDao
        every { database.parseRequestDao() } returns parseRequestDao
        every { database.workoutTemplateDao() } returns workoutTemplateDao
        every { database.templateExerciseDao() } returns templateExerciseDao
        every { database.templateSetDao() } returns templateSetDao
        every { database.localSyncMetadataDao() } returns localSyncMetadataDao

        coEvery { localSyncMetadataDao.getLastSyncTime(any(), any(), any()) } returns null
        coEvery { localSyncMetadataDao.insertOrUpdate(any()) } just runs
        coEvery { localSyncMetadataDao.hasDeviceEverSynced(any(), any()) } returns false

        syncManager = SyncManager(context, database, authManager, firestoreRepository)
    }

    private fun mockAllDaoMethods() {
        coEvery { workoutTemplateDao.getTemplates(any()) } returns emptyList()
        coEvery { workoutTemplateDao.upsertTemplate(any()) } returns Unit
        coEvery { templateExerciseDao.getExercisesForTemplate(any()) } returns emptyList()
        coEvery { templateExerciseDao.getExercisesForTemplates(any()) } returns emptyList()
        coEvery { templateExerciseDao.upsertTemplateExercise(any()) } returns Unit
        coEvery { templateSetDao.getSetsForTemplateExercise(any()) } returns emptyList()
        coEvery { templateSetDao.getSetsForTemplateExercises(any()) } returns emptyList()
        coEvery { templateSetDao.upsertTemplateSet(any()) } returns Unit
        coEvery { setLogDao.getSetLogsForExercises(any()) } returns emptyList()
        coEvery { exerciseDao.getAllExercises() } returns emptyList()
        coEvery { exerciseDao.getCustomExercisesByUser(any()) } returns emptyList()
        coEvery { exerciseLogDao.getExerciseLogsForWorkouts(any()) } returns emptyList()
        coEvery { exerciseLogDao.getExistingExerciseLogIds(any()) } returns emptyList()
        coEvery { exerciseMuscleDao.getAllExerciseMuscles() } returns emptyList()
        coEvery { exerciseInstructionDao.getAllInstructions() } returns emptyList()
        coEvery { exerciseAliasDao.getAllAliases() } returns emptyList()
        coEvery { programmeDao.getAllProgrammes() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWeeks() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWorkouts() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeProgress() } returns emptyList()
        coEvery { oneRMDao.getAllForUser(any()) } returns emptyList()
        coEvery { personalRecordDao.getAllPersonalRecords() } returns emptyList()
        coEvery { userExerciseUsageDao.getAllUsageForUser(any()) } returns emptyList()
        coEvery { userExerciseUsageDao.getUsage(any(), any()) } returns null
        coEvery { userExerciseUsageDao.insertUsage(any()) } returns Unit
        coEvery { userExerciseUsageDao.updateUsage(any()) } returns Unit
        coEvery { exerciseSwapHistoryDao.getAllSwapHistory() } returns emptyList()
        coEvery { programmeExerciseTrackingDao.getAllTracking() } returns emptyList()
        coEvery { globalExerciseProgressDao.getAllProgress() } returns emptyList()
        coEvery { trainingAnalysisDao.getAllAnalyses() } returns emptyList()
        coEvery { parseRequestDao.getAllRequestsList() } returns emptyList()
    }

    private fun mockAllFirestoreUploads() {
        coEvery { firestoreRepository.uploadWorkouts(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseLogs(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadSetLogs(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadWorkoutTemplates(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadTemplateExercises(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadTemplateSets(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExercises(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammes(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWeeks(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWorkouts(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeProgress(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadUserExerciseMaxes(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadPersonalRecords(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadUserExerciseUsages(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseSwapHistory(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExercisePerformanceTracking(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadGlobalExerciseProgress(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadTrainingAnalyses(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadParseRequests(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadCustomExercise(any(), any(), any(), any(), any()) } returns Result.success(Unit)
    }

    private fun mockAllFirestoreDownloads() {
        coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())
        coEvery { firestoreRepository.downloadCustomExercises(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadWorkouts(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadWorkoutTemplates(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadTemplateExercises(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadTemplateSets(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExercises() } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadUserExerciseUsages(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadTrainingAnalyses(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadParseRequests(any()) } returns Result.success(emptyList())
    }

    @Test
    fun `conflict resolution - same timestamp results in upsert with last-write-wins`() =
        runTest {
            val userId = "test-user"
            val sameTimestamp = Timestamp(1000, 500000000)
            val localWorkout =
                Workout(
                    id = "1",
                    userId = userId,
                    name = "Local Workout",
                    notes = "Local notes",
                    date = LocalDateTime.ofEpochSecond(1000, 500000000, ZoneOffset.UTC),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                )
            val remoteWorkout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Remote Workout",
                    notes = "Remote notes",
                    date = sameTimestamp,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = sameTimestamp,
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns listOf(localWorkout)
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(remoteWorkout))
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            coVerify {
                workoutDao.upsertWorkout(
                    withArg {
                        assertThat(it.name).isEqualTo("Remote Workout")
                        assertThat(it.notes).isEqualTo("Remote notes")
                    },
                )
            }
        }

    @Test
    fun `conflict resolution - null timestamps are handled gracefully`() =
        runTest {
            val userId = "test-user"
            val remoteSetLog =
                FirestoreSetLog(
                    localId = "1",
                    exerciseLogId = "ex-1",
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 100f,
                    actualReps = 10,
                    actualWeight = 100f,
                    actualRpe = null,
                    isCompleted = true,
                    completedAt = null,
                    tag = null,
                    notes = null,
                    lastModified = null,
                )

            val workout =
                Workout(
                    id = "workout-1",
                    userId = userId,
                    name = "Test Workout",
                    notes = null,
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                )

            every { authManager.getCurrentUserId() } returns userId
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()
            // Override specific mocks after general mocks
            coEvery { workoutDao.getAllWorkouts(userId) } returns listOf(workout)
            coEvery { exerciseLogDao.getExistingExerciseLogIds(listOf("ex-1")) } returns listOf("ex-1")
            coEvery { setLogDao.upsertSetLog(any()) } returns Unit
            coEvery { firestoreRepository.downloadSetLogs(userId, null) } returns Result.success(listOf(remoteSetLog))

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            coVerify { setLogDao.upsertSetLog(any()) }
        }

    @Test
    fun `conflict resolution - sub-second precision differences handled correctly`() =
        runTest {
            val userId = "test-user"
            val baseTime = 1000L
            val timestamp1 = Timestamp(baseTime, 100000000)
            val timestamp2 = Timestamp(baseTime, 900000000)

            val remoteWorkout1 =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Workout with 100ms",
                    notes = null,
                    date = timestamp1,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = timestamp1,
                )

            val remoteWorkout2 =
                FirestoreWorkout(
                    localId = "2",
                    userId = userId,
                    name = "Workout with 900ms",
                    notes = null,
                    date = timestamp2,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = timestamp2,
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery {
                firestoreRepository.downloadWorkouts(
                    userId,
                    null,
                )
            } returns Result.success(listOf(remoteWorkout1, remoteWorkout2))
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 2) { workoutDao.upsertWorkout(any()) }
        }

    @Test
    fun `conflict resolution - multi-field conflicts resolved by upsert`() =
        runTest {
            val userId = "test-user"
            val localWorkout =
                Workout(
                    id = "1",
                    userId = userId,
                    name = "Local Name",
                    notes = "Local Notes",
                    date = LocalDateTime.now().minusDays(1),
                    status = WorkoutStatus.IN_PROGRESS,
                    programmeId = "prog-1",
                    weekNumber = 1,
                    dayNumber = 2,
                    programmeWorkoutName = "Day 2",
                    isProgrammeWorkout = true,
                    durationSeconds = "3600",
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                )

            val remoteWorkout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Remote Name",
                    notes = "Remote Notes",
                    date = Timestamp.now(),
                    status = "COMPLETED",
                    programmeId = "prog-2",
                    weekNumber = 2,
                    dayNumber = 3,
                    programmeWorkoutName = "Day 3",
                    isProgrammeWorkout = true,
                    durationSeconds = 7200L,
                    timerStartTime = null,
                    timerElapsedSeconds = 100,
                    lastModified = Timestamp.now(),
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns listOf(localWorkout)
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(remoteWorkout))
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            coVerify {
                workoutDao.upsertWorkout(
                    withArg {
                        assertThat(it.name).isEqualTo("Remote Name")
                        assertThat(it.notes).isEqualTo("Remote Notes")
                        assertThat(it.status).isEqualTo(WorkoutStatus.COMPLETED)
                        assertThat(it.programmeId).isEqualTo("prog-2")
                        assertThat(it.weekNumber).isEqualTo(2)
                        assertThat(it.dayNumber).isEqualTo(3)
                        assertThat(it.timerElapsedSeconds).isEqualTo(100)
                    },
                )
            }
        }

    @Test
    fun `conflict resolution - concurrent updates from multiple devices with different timestamps`() =
        runTest {
            val userId = "test-user"
            val device1Timestamp = Timestamp(1000, 0)
            val device2Timestamp = Timestamp(2000, 0)

            val device1Workout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "From Device 1",
                    notes = "Device 1 notes",
                    date = device1Timestamp,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = device1Timestamp,
                )

            val device2Workout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "From Device 2",
                    notes = "Device 2 notes",
                    date = device2Timestamp,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = device2Timestamp,
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            val capturedWorkouts = mutableListOf<Workout>()
            coEvery { workoutDao.upsertWorkout(capture(capturedWorkouts)) } returns Unit

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery {
                firestoreRepository.downloadWorkouts(
                    userId,
                    null,
                )
            } returns Result.success(listOf(device1Workout, device2Workout))
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            assertThat(capturedWorkouts).hasSize(2)
            val finalWorkout = capturedWorkouts.last()
            assertThat(finalWorkout.name).isEqualTo("From Device 2")
            assertThat(finalWorkout.notes).isEqualTo("Device 2 notes")
        }

    @Test
    fun `conflict resolution - out-of-order sync completion does not corrupt data`() =
        runTest {
            val userId = "test-user"
            val earlierTimestamp = Timestamp(1000, 0)
            val laterTimestamp = Timestamp(2000, 0)

            val olderWorkout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Older Workout",
                    notes = "This is older",
                    date = earlierTimestamp,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = earlierTimestamp,
                )

            val newerWorkout =
                FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Newer Workout",
                    notes = "This is newer",
                    date = laterTimestamp,
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = laterTimestamp,
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            val capturedWorkouts = mutableListOf<Workout>()
            coEvery { workoutDao.upsertWorkout(capture(capturedWorkouts)) } returns Unit

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery {
                firestoreRepository.downloadWorkouts(
                    userId,
                    null,
                )
            } returns Result.success(listOf(newerWorkout, olderWorkout))
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            assertThat(capturedWorkouts).hasSize(2)
            val finalWorkout = capturedWorkouts.last()
            assertThat(finalWorkout.name).isEqualTo("Older Workout")
        }
}
