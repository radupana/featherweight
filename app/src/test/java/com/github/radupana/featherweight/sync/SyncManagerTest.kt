package com.github.radupana.featherweight.sync

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ParseRequestDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.ProgrammeExerciseTrackingDao
import com.github.radupana.featherweight.data.SetLog
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
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncManagerTest {
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
    private lateinit var userExerciseUsageDao: com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
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
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Mock TextUtils
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            val str = arg<CharSequence?>(0)
            str == null || str.isEmpty()
        }
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

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

        // Mock InstallationIdProvider
        every { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.getString("installation_id", null) } returns "test-installation-id"
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just runs

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

        // Default mocks for localSyncMetadataDao
        coEvery { localSyncMetadataDao.getLastSyncTime(any(), any(), any()) } returns null
        coEvery { localSyncMetadataDao.insertOrUpdate(any()) } just runs
        coEvery { localSyncMetadataDao.hasDeviceEverSynced(any(), any()) } returns false

        syncManager = SyncManager(context, database, authManager, false, firestoreRepository)
    }

    private fun mockAllDaoMethods() {
        coEvery { workoutTemplateDao.getTemplates(any()) } returns emptyList()
        coEvery { workoutTemplateDao.upsertTemplate(any()) } returns Unit
        coEvery { templateExerciseDao.getExercisesForTemplate(any()) } returns emptyList()
        coEvery { templateExerciseDao.upsertTemplateExercise(any()) } returns Unit
        coEvery { templateSetDao.getSetsForTemplateExercise(any()) } returns emptyList()
        coEvery { templateSetDao.upsertTemplateSet(any()) } returns Unit
        coEvery { exerciseDao.getAllExercises() } returns emptyList()
        coEvery { exerciseDao.getCustomExercisesByUser(any()) } returns emptyList()
        coEvery { exerciseMuscleDao.getAllExerciseMuscles() } returns emptyList()
        coEvery { exerciseInstructionDao.getAllInstructions() } returns emptyList()
        coEvery { exerciseAliasDao.getAllAliases() } returns emptyList()
        coEvery { programmeDao.getAllProgrammes() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWeeks() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWorkouts() } returns emptyList()
        // Note: getAllSubstitutions() method doesn't exist in ProgrammeDao
        coEvery { programmeDao.getAllProgrammeProgress() } returns emptyList()
        coEvery { oneRMDao.getAllForUser(any()) } returns emptyList()
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
        // Exercise-related data is now embedded in FirestoreExercise - no separate upload needed
        coEvery { firestoreRepository.uploadProgrammes(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWeeks(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWorkouts(any(), any()) } returns Result.success(Unit)
        // Note: uploadExerciseSubstitutions() method doesn't exist in FirestoreRepository
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
        // Firestore sync metadata no longer used - only local tracking
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
        // Exercise-related data is now embedded in FirestoreExercise - no separate download needed
        coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
        // Note: downloadExerciseSubstitutions() method doesn't exist in FirestoreRepository
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
    fun `syncAll returns error when user not authenticated`() =
        runTest {
            every { authManager.getCurrentUserId() } returns null

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Error)
            assertEquals("User not authenticated", state.message)
        }

    @Test
    fun `syncAll uploads user workouts successfully`() =
        runTest {
            val userId = "test-user"
            val workout =
                Workout(
                    id = "1",
                    userId = userId,
                    name = "Test Workout",
                    notes = "Test notes",
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
            // Firestore sync metadata no longer used - only local tracking
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())
            coEvery { firestoreRepository.downloadCustomExercises(any(), any()) } returns Result.success(emptyList())
            coEvery { workoutDao.getAllWorkouts("test-user") } returns listOf(workout)
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify { firestoreRepository.uploadWorkouts(userId, any()) }
        }

    @Test
    fun `syncAll uploads exercise logs and set logs`() =
        runTest {
            val userId = "test-user"
            val workout =
                Workout(
                    id = "1",
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
            val exerciseLog =
                ExerciseLog(
                    id = "1",
                    userId = userId,
                    workoutId = "1",
                    exerciseId = "1",
                    exerciseOrder = 1,
                    notes = null,
                    originalExerciseId = null,
                    isSwapped = false,
                )
            val setLog =
                SetLog(
                    id = "1",
                    userId = userId,
                    exerciseLogId = "1",
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
                )

            every { authManager.getCurrentUserId() } returns userId
            // Firestore sync metadata no longer used - only local tracking
            coEvery { workoutDao.getAllWorkouts("test-user") } returns listOf(workout)
            coEvery { exerciseLogDao.getExerciseLogsForWorkout("1") } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise("1") } returns listOf(setLog)
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify { firestoreRepository.uploadExerciseLogs(userId, any()) }
            coVerify { firestoreRepository.uploadSetLogs(userId, any()) }
        }

    @Test
    fun `syncAll handles upload failure gracefully`() =
        runTest {
            val userId = "test-user"
            val errorMessage = "Network error"

            every { authManager.getCurrentUserId() } returns userId
            // Firestore sync metadata no longer used - only local tracking
            coEvery { workoutDao.getAllWorkouts("test-user") } returns emptyList()
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery { firestoreRepository.uploadWorkouts(any(), any()) } returns Result.failure(com.google.firebase.FirebaseException(errorMessage))
            coEvery { firestoreRepository.uploadExerciseLogs(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadSetLogs(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExercises(any(), any()) } returns Result.success(Unit)
            // Exercise-related data is now embedded in FirestoreExercise - no separate upload needed
            coEvery { firestoreRepository.uploadProgrammes(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammeWeeks(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammeWorkouts(any(), any()) } returns Result.success(Unit)
            // Note: uploadExerciseSubstitutions() method doesn't exist in FirestoreRepository
            coEvery { firestoreRepository.uploadProgrammeProgress(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadUserExerciseMaxes(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadPersonalRecords(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseSwapHistory(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExercisePerformanceTracking(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadGlobalExerciseProgress(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadTrainingAnalyses(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadParseRequests(any(), any()) } returns Result.success(Unit)
            // Firestore sync metadata no longer used - only local tracking

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Error)
            assertTrue(state.message.contains("Network error"), "Error message was: ${state.message}")
        }

    @Test
    fun `syncAll uses existing sync metadata when available`() =
        runTest {
            val userId = "test-user"
            val lastSyncTime = java.time.LocalDateTime.now()

            every { authManager.getCurrentUserId() } returns userId
            // Set up local sync metadata to return a timestamp
            coEvery { localSyncMetadataDao.getLastSyncTime(userId, any(), "all") } returns lastSyncTime
            coEvery { workoutDao.getAllWorkouts("test-user") } returns emptyList()
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            // Verify local sync metadata was checked
            coVerify { localSyncMetadataDao.getLastSyncTime(userId, any(), "all") }
        }

    @Test
    fun `syncAll filters workouts by userId`() =
        runTest {
            val userId = "test-user"
            val userWorkout =
                Workout(
                    id = "1",
                    userId = userId,
                    name = "User Workout",
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
            // Firestore sync metadata no longer used - only local tracking
            coEvery { workoutDao.getAllWorkouts("test-user") } returns listOf(userWorkout)
            coEvery { exerciseLogDao.getExerciseLogsForWorkout("1") } returns emptyList()
            coEvery { exerciseLogDao.getExerciseLogsForWorkout("2") } returns emptyList()

            mockAllDaoMethods()

            val workoutsSlot = slot<List<com.github.radupana.featherweight.sync.models.FirestoreWorkout>>()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()
            coEvery { firestoreRepository.uploadWorkouts(any(), capture(workoutsSlot)) } returns Result.success(Unit)

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            assertEquals(1, workoutsSlot.captured.size)
            assertEquals("1", workoutsSlot.captured[0].localId)
        }

    @Test
    fun `syncAll performs bidirectional sync - download then upload`() =
        runTest {
            val userId = "test-user"
            val downloadedWorkout =
                com.github.radupana.featherweight.sync.models.FirestoreWorkout(
                    localId = "99",
                    userId = userId,
                    name = "Downloaded Workout",
                    notes = "From cloud",
                    date = Timestamp.now(),
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = Timestamp.now(),
                )

            every { authManager.getCurrentUserId() } returns userId
            // Firestore sync metadata no longer used - only local tracking
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())
            coEvery { firestoreRepository.downloadCustomExercises(any(), any()) } returns Result.success(emptyList())
            coEvery { workoutDao.getAllWorkouts("test-user") } returns emptyList()
            coEvery { workoutDao.getWorkoutById("99") } returns null
            coEvery { workoutDao.insertWorkout(any()) } returns Unit
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            coEvery { exerciseLogDao.upsertExerciseLog(any()) } returns Unit
            coEvery { setLogDao.upsertSetLog(any()) } returns Unit
            mockAllDaoMethods()

            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(downloadedWorkout))
            coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadWorkoutTemplates(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateExercises(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateSets(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercises() } returns Result.success(emptyList())
            // Exercise-related data is now embedded in FirestoreExercise - no separate download needed
            coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
            // Note: downloadExerciseSubstitutions() method doesn't exist in FirestoreRepository
            coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseUsages(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTrainingAnalyses(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadParseRequests(any()) } returns Result.success(emptyList())

            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify {
                firestoreRepository.downloadWorkouts(userId, null)
                workoutDao.upsertWorkout(any())
            }
        }

    @Test
    fun `syncAll handles download failure gracefully`() =
        runTest {
            val userId = "test-user"
            val errorMessage = "Download failed"

            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts("test-user") } returns emptyList() // Mock isDatabaseEmpty check
            // Firestore sync metadata no longer used - only local tracking
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery { firestoreRepository.downloadWorkouts(any(), any()) } returns Result.failure(com.google.firebase.FirebaseException(errorMessage))

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Error)
            assertTrue(state.message.contains("Download failed"))

            coVerify(exactly = 0) { firestoreRepository.uploadWorkouts(any(), any()) }
        }

    @Test
    fun `syncAll merges remote workouts with conflict resolution`() =
        runTest {
            val userId = "test-user"
            val localWorkout =
                Workout(
                    id = "1",
                    userId = userId,
                    name = "Local Workout",
                    notes = "Local notes",
                    date = LocalDateTime.of(2024, 1, 1, 10, 0),
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
                com.github.radupana.featherweight.sync.models.FirestoreWorkout(
                    localId = "1",
                    userId = userId,
                    name = "Remote Workout",
                    notes = "Updated from cloud",
                    date = Timestamp(Date(2024 - 1900, 0, 2, 10, 0)),
                    status = "COMPLETED",
                    programmeId = null,
                    weekNumber = null,
                    dayNumber = null,
                    programmeWorkoutName = null,
                    isProgrammeWorkout = false,
                    durationSeconds = null,
                    timerStartTime = null,
                    timerElapsedSeconds = 0,
                    lastModified = Timestamp.now(),
                )

            every { authManager.getCurrentUserId() } returns userId
            // Firestore sync metadata no longer used - only local tracking
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())
            coEvery { firestoreRepository.downloadCustomExercises(any(), any()) } returns Result.success(emptyList())
            coEvery { workoutDao.getAllWorkouts("test-user") } returns listOf(localWorkout)
            coEvery { workoutDao.getWorkoutById("1") } returns localWorkout
            coEvery { workoutDao.updateWorkout(any()) } returns Unit
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            coEvery { exerciseLogDao.upsertExerciseLog(any()) } returns Unit
            coEvery { setLogDao.upsertSetLog(any()) } returns Unit
            coEvery { exerciseLogDao.getExerciseLogsForWorkout("1") } returns emptyList()
            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(remoteWorkout))

            mockAllDaoMethods()
            coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadWorkoutTemplates(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateExercises(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateSets(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercises() } returns Result.success(emptyList())
            // Exercise-related data is now embedded in FirestoreExercise - no separate download needed
            coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
            // Note: downloadExerciseSubstitutions() method doesn't exist in FirestoreRepository
            coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseUsages(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTrainingAnalyses(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadParseRequests(any()) } returns Result.success(emptyList())
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify {
                workoutDao.upsertWorkout(
                    withArg {
                        assertEquals("Remote Workout", it.name)
                        assertEquals("Updated from cloud", it.notes)
                    },
                )
            }
        }

    @Test
    fun `syncAll forces full sync when database is empty but sync metadata exists`() =
        runTest {
            val userId = "test-user-123"
            every { authManager.getCurrentUserId() } returns userId

            // Database is empty - no workouts
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            // Firestore sync metadata no longer used - only local tracking

            // Mock remote data that should be downloaded
            val remoteWorkout =
                mockk<com.github.radupana.featherweight.sync.models.FirestoreWorkout> {
                    every { id } returns "1"
                    every { localId } returns "1"
                    every { name } returns "Remote Workout"
                    every { date } returns Timestamp.now()
                    every { durationSeconds } returns 3600L
                    every { status } returns "COMPLETED"
                    every { notes } returns "From cloud"
                    every { notesUpdatedAt } returns null
                    every { this@mockk.userId } returns userId
                    every { lastModified } returns Timestamp.now()
                    every { programmeId } returns null
                    every { weekNumber } returns null
                    every { dayNumber } returns null
                    every { programmeWorkoutName } returns null
                    every { isProgrammeWorkout } returns false
                    every { timerStartTime } returns null
                    every { timerElapsedSeconds } returns 0
                }

            // Important: downloadWorkouts should be called with null timestamp (full sync)
            coEvery {
                firestoreRepository.downloadWorkouts(
                    userId,
                    null, // This should be null for full sync
                )
            } returns Result.success(listOf(remoteWorkout))

            // Mock all other downloads
            coEvery { firestoreRepository.downloadExerciseLogs(userId, null) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(userId, null) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadWorkoutTemplates(userId, null) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateExercises(userId, null) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateSets(userId, null) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSystemExercises(null) } returns Result.success(emptyMap())
            coEvery { firestoreRepository.downloadCustomExercises(userId, null) } returns Result.success(emptyList())
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

            // Mock upsert
            coEvery { workoutDao.upsertWorkout(any()) } returns Unit
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()
            coEvery { setLogDao.getSetLogsForExercise(any()) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            // Verify that download was called with null timestamp (full sync)
            coVerify {
                firestoreRepository.downloadWorkouts(userId, null)
            }

            // Verify workout was inserted
            coVerify {
                workoutDao.upsertWorkout(
                    withArg {
                        assertEquals("Remote Workout", it.name)
                    },
                )
            }
        }

    @Test
    fun `syncAll uses lastSyncTime when database has data`() =
        runTest {
            val userId = "test-user-123"
            every { authManager.getCurrentUserId() } returns userId

            // Database has existing workouts
            val existingWorkout =
                Workout(
                    id = "1",
                    name = "Existing Workout",
                    date = LocalDateTime.now(),
                    durationSeconds = "3600",
                    status = WorkoutStatus.COMPLETED,
                    notes = "Already here",
                    userId = userId,
                )
            coEvery { workoutDao.getAllWorkouts(userId) } returns listOf(existingWorkout)

            // Sync metadata exists - mock local sync metadata to return lastSyncTime
            val lastSyncDateTime = LocalDateTime.now().minusHours(1)
            // Convert LocalDateTime to Timestamp for consistency
            val instant = lastSyncDateTime.toInstant(ZoneOffset.UTC)
            val lastSyncTime = Timestamp(instant.epochSecond, instant.nano)

            // Mock local sync metadata to return the expected timestamp
            coEvery { localSyncMetadataDao.getLastSyncTime(userId, any(), "all") } returns lastSyncDateTime

            // downloadWorkouts should be called WITH lastSyncTime (incremental sync)
            coEvery {
                firestoreRepository.downloadWorkouts(
                    userId,
                    lastSyncTime, // Should use the actual timestamp
                )
            } returns Result.success(emptyList())

            // Mock all other downloads with lastSyncTime
            coEvery { firestoreRepository.downloadExerciseLogs(userId, lastSyncTime) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(userId, lastSyncTime) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadWorkoutTemplates(userId, lastSyncTime) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateExercises(userId, lastSyncTime) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTemplateSets(userId, lastSyncTime) } returns Result.success(emptyList())
            // System exercises always use null for lastSyncTime (they're global, not user-specific)
            coEvery { firestoreRepository.downloadSystemExercises(null) } returns Result.success(emptyMap())
            coEvery { firestoreRepository.downloadCustomExercises(userId, lastSyncTime) } returns Result.success(emptyList())
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

            // Mock DAO methods for existing data
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(any()) } returns emptyList()
            coEvery { setLogDao.getSetLogsForExercise(any()) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            // Verify that download was called WITH the lastSyncTime (incremental sync)
            coVerify {
                firestoreRepository.downloadWorkouts(userId, lastSyncTime)
            }
        }
}
