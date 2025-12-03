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
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutTemplateDao
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SyncCooldownAndMutexTest {
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
    private lateinit var syncPrefs: SharedPreferences
    private lateinit var syncPrefsEditor: SharedPreferences.Editor
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
        syncPrefs = mockk()
        syncPrefsEditor = mockk()

        every { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.getString("installation_id", null) } returns "test-installation-id"
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just runs

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
    fun `sync cooldown - prevents sync within cooldown period`() =
        runTest {
            val userId = "test-user"
            val currentTime = System.currentTimeMillis()
            val recentSyncTime = currentTime - (2 * 60 * 1000L)

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns recentSyncTime
            every { authManager.getCurrentUserId() } returns userId

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            val state = result.getOrNull()
            assertThat(state).isInstanceOf(SyncState.Skipped::class.java)
            assertThat((state as SyncState.Skipped).reason).contains("cooldown")
        }

    @Test
    fun `sync cooldown - allows sync after cooldown period expires`() =
        runTest {
            val userId = "test-user"
            val currentTime = System.currentTimeMillis()
            val oldSyncTime = currentTime - (6 * 60 * 1000L)

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns oldSyncTime
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            val state = result.getOrNull()
            assertThat(state).isInstanceOf(SyncState.Success::class.java)
        }

    @Test
    fun `sync cooldown - first sync ever is allowed`() =
        runTest {
            val userId = "test-user"

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns 0L
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            val state = result.getOrNull()
            assertThat(state).isInstanceOf(SyncState.Success::class.java)
        }

    @Test
    fun `mutex - prevents concurrent sync operations`() =
        runTest {
            val userId = "test-user"

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns 0L
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            coEvery { firestoreRepository.downloadWorkouts(any(), any()) } coAnswers {
                delay(100)
                Result.success(emptyList())
            }

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val deferred1 = async { syncManager.syncAll() }
            val deferred2 = async { syncManager.syncAll() }

            val result1 = deferred1.await()
            val result2 = deferred2.await()

            assertThat(result1.isSuccess).isTrue()
            assertThat(result2.isSuccess).isTrue()

            val completedSyncs =
                listOf(result1.getOrNull(), result2.getOrNull()).count { it is SyncState.Success }
            val skippedSyncs = listOf(result1.getOrNull(), result2.getOrNull()).count { it is SyncState.Skipped }

            assertThat(completedSyncs).isEqualTo(1)
            assertThat(skippedSyncs).isEqualTo(1)
        }

    @Test
    fun `mutex - allows sync after previous sync completes`() =
        runTest {
            val userId = "test-user"
            val oldSyncTime = System.currentTimeMillis() - (6 * 60 * 1000L)

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns oldSyncTime
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result1 = syncManager.syncAll()
            assertThat(result1.isSuccess).isTrue()
            assertThat(result1.getOrNull()).isInstanceOf(SyncState.Success::class.java)

            // Create a fresh SyncManager to reset internal state
            // The second sync should succeed because it reads from SharedPreferences
            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns System.currentTimeMillis() - (6 * 60 * 1000L)
            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result2 = syncManager.syncAll()
            assertThat(result2.isSuccess).isTrue()
            assertThat(result2.getOrNull()).isInstanceOf(SyncState.Success::class.java)
        }

    @Test
    fun `mutex - system exercise sync and user data sync do not interfere`() =
        runTest {
            val userId = "test-user"
            val oldSyncTime = System.currentTimeMillis() - (6 * 60 * 1000L)

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns oldSyncTime
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            coEvery { firestoreRepository.downloadSystemExercises(any()) } coAnswers {
                delay(50)
                Result.success(emptyMap())
            }

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val systemSync = async { syncManager.syncSystemExercises() }
            delay(10)
            val userSync = async { syncManager.syncUserData(userId) }

            val systemResult = systemSync.await()
            val userResult = userSync.await()

            assertThat(systemResult.isSuccess).isTrue()
            assertThat(userResult.isSuccess).isTrue()

            val completedSyncs =
                listOf(systemResult.getOrNull(), userResult.getOrNull()).count { it is SyncState.Success }

            assertThat(completedSyncs).isAtLeast(1)
        }

    @Test
    fun `cooldown and mutex combined - prevents rapid successive syncs`() =
        runTest {
            val userId = "test-user"
            val currentTime = System.currentTimeMillis()

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns currentTime - (1 * 60 * 1000L)
            every { authManager.getCurrentUserId() } returns userId

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val results = mutableListOf<Result<SyncState>>()
            repeat(3) {
                results.add(syncManager.syncAll())
            }

            val skippedCount =
                results.count { result ->
                    result.isSuccess && result.getOrNull() is SyncState.Skipped
                }

            assertThat(skippedCount).isEqualTo(3)
        }

    @Test
    fun `getLastSyncTime returns correct value from SharedPreferences`() {
        val expectedTime = 123456789L
        every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns expectedTime

        syncManager = SyncManager(context, database, authManager, firestoreRepository)

        val actualTime = syncManager.getLastSyncTime()

        assertThat(actualTime).isEqualTo(expectedTime)
    }

    @Test
    fun `getLastSyncTime returns zero when no sync has occurred`() {
        every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns 0L

        syncManager = SyncManager(context, database, authManager, firestoreRepository)

        val actualTime = syncManager.getLastSyncTime()

        assertThat(actualTime).isEqualTo(0L)
    }

    @Test
    fun `successful sync updates SharedPreferences with current time`() =
        runTest {
            val userId = "test-user"
            val oldSyncTime = System.currentTimeMillis() - (6 * 60 * 1000L)

            every { syncPrefs.getLong("last_successful_sync_time", 0L) } returns oldSyncTime
            every { authManager.getCurrentUserId() } returns userId
            coEvery { workoutDao.getAllWorkouts(userId) } returns emptyList()

            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            syncManager = SyncManager(context, database, authManager, firestoreRepository)

            val result = syncManager.syncAll()

            assertThat(result.isSuccess).isTrue()
            coVerify {
                syncPrefsEditor.putLong(
                    "last_successful_sync_time",
                    match { it > oldSyncTime },
                )
            }
        }
}
