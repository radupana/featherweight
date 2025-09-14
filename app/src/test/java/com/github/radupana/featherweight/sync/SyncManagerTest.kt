package com.github.radupana.featherweight.sync

import android.content.Context
import android.content.SharedPreferences
import com.github.radupana.featherweight.dao.TrainingAnalysisDao
import com.github.radupana.featherweight.data.ExerciseCorrelationDao
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExercisePerformanceTrackingDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ParseRequestDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.VariationAliasDao
import com.github.radupana.featherweight.data.exercise.VariationInstructionDao
import com.github.radupana.featherweight.data.exercise.VariationMuscleDao
import com.github.radupana.featherweight.data.exercise.VariationRelationDao
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
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
    private lateinit var exerciseCoreDao: ExerciseCoreDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var variationMuscleDao: VariationMuscleDao
    private lateinit var variationInstructionDao: VariationInstructionDao
    private lateinit var variationAliasDao: VariationAliasDao
    private lateinit var variationRelationDao: VariationRelationDao
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var oneRMDao: OneRMDao
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var exerciseSwapHistoryDao: ExerciseSwapHistoryDao
    private lateinit var exercisePerformanceTrackingDao: ExercisePerformanceTrackingDao
    private lateinit var globalExerciseProgressDao: GlobalExerciseProgressDao
    private lateinit var exerciseCorrelationDao: ExerciseCorrelationDao
    private lateinit var trainingAnalysisDao: TrainingAnalysisDao
    private lateinit var parseRequestDao: ParseRequestDao
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var syncManager: SyncManager

    @Before
    fun setup() {
        context = mockk()
        database = mockk()
        authManager = mockk()
        firestoreRepository = mockk()
        workoutDao = mockk()
        exerciseLogDao = mockk()
        setLogDao = mockk()
        exerciseCoreDao = mockk()
        exerciseVariationDao = mockk()
        variationMuscleDao = mockk()
        variationInstructionDao = mockk()
        variationAliasDao = mockk()
        variationRelationDao = mockk()
        programmeDao = mockk()
        oneRMDao = mockk()
        personalRecordDao = mockk()
        exerciseSwapHistoryDao = mockk()
        exercisePerformanceTrackingDao = mockk()
        globalExerciseProgressDao = mockk()
        exerciseCorrelationDao = mockk()
        trainingAnalysisDao = mockk()
        parseRequestDao = mockk()
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()

        every { context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.getString("device_id", null) } returns "test-device-id"
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs

        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.exerciseCoreDao() } returns exerciseCoreDao
        every { database.exerciseVariationDao() } returns exerciseVariationDao
        every { database.variationMuscleDao() } returns variationMuscleDao
        every { database.variationInstructionDao() } returns variationInstructionDao
        every { database.variationAliasDao() } returns variationAliasDao
        every { database.variationRelationDao() } returns variationRelationDao
        every { database.programmeDao() } returns programmeDao
        every { database.oneRMDao() } returns oneRMDao
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { database.exercisePerformanceTrackingDao() } returns exercisePerformanceTrackingDao
        every { database.globalExerciseProgressDao() } returns globalExerciseProgressDao
        every { database.exerciseCorrelationDao() } returns exerciseCorrelationDao
        every { database.trainingAnalysisDao() } returns trainingAnalysisDao
        every { database.parseRequestDao() } returns parseRequestDao

        syncManager = SyncManager(context, database, authManager, firestoreRepository)
    }

    private fun mockAllDaoMethods() {
        coEvery { exerciseCoreDao.getAllCores() } returns emptyList()
        coEvery { exerciseVariationDao.getAllExerciseVariations() } returns emptyList()
        coEvery { variationMuscleDao.getAllVariationMuscles() } returns emptyList()
        coEvery { variationInstructionDao.getAllInstructions() } returns emptyList()
        coEvery { variationAliasDao.getAllAliases() } returns emptyList()
        coEvery { variationRelationDao.getAllRelations() } returns emptyList()
        coEvery { programmeDao.getAllProgrammes() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWeeks() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeWorkouts() } returns emptyList()
        coEvery { programmeDao.getAllSubstitutions() } returns emptyList()
        coEvery { programmeDao.getAllProgrammeProgress() } returns emptyList()
        coEvery { oneRMDao.getAllUserExerciseMaxes() } returns emptyList()
        coEvery { oneRMDao.getAllOneRMHistory() } returns emptyList()
        coEvery { personalRecordDao.getAllPersonalRecords() } returns emptyList()
        coEvery { exerciseSwapHistoryDao.getAllSwapHistory() } returns emptyList()
        coEvery { exercisePerformanceTrackingDao.getAllTracking() } returns emptyList()
        coEvery { globalExerciseProgressDao.getAllProgress() } returns emptyList()
        coEvery { exerciseCorrelationDao.getAllCorrelations() } returns emptyList()
        coEvery { trainingAnalysisDao.getAllAnalyses() } returns emptyList()
        coEvery { parseRequestDao.getAllRequestsList() } returns emptyList()
    }

    private fun mockAllFirestoreUploads() {
        coEvery { firestoreRepository.uploadWorkouts(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseLogs(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadSetLogs(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseCores(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseVariations(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadVariationMuscles(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadVariationInstructions(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadVariationAliases(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadVariationRelations(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammes(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWeeks(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeWorkouts(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseSubstitutions(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadProgrammeProgress(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadUserExerciseMaxes(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadOneRMHistory(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadPersonalRecords(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseSwapHistory(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExercisePerformanceTracking(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadGlobalExerciseProgress(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadExerciseCorrelations(any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadTrainingAnalyses(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.uploadParseRequests(any(), any()) } returns Result.success(Unit)
        coEvery { firestoreRepository.updateSyncMetadata(any(), any(), any()) } returns Result.success(Unit)
    }

    private fun mockAllFirestoreDownloads() {
        coEvery { firestoreRepository.downloadWorkouts(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseCores(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseVariations(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadVariationMuscles() } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadVariationInstructions() } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadVariationAliases() } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadVariationRelations() } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseSubstitutions(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadOneRMHistory(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
        coEvery { firestoreRepository.downloadExerciseCorrelations() } returns Result.success(emptyList())
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
                    id = 1,
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
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns listOf(workout)
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
                    id = 1,
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
                    id = 1,
                    userId = userId,
                    workoutId = 1,
                    exerciseVariationId = 1,
                    exerciseOrder = 1,
                    supersetGroup = null,
                    notes = null,
                    originalVariationId = null,
                    isSwapped = false,
                )
            val setLog =
                SetLog(
                    id = 1,
                    userId = userId,
                    exerciseLogId = 1,
                    setOrder = 1,
                    targetReps = 10,
                    targetWeight = 100f,
                    actualReps = 10,
                    actualWeight = 100f,
                    actualRpe = null,
                    isCompleted = true,
                    completedAt = null,
                    suggestedWeight = null,
                    suggestedReps = null,
                    suggestionSource = null,
                    suggestionConfidence = null,
                    calculationDetails = null,
                    tag = null,
                    notes = null,
                )

            every { authManager.getCurrentUserId() } returns userId
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns listOf(workout)
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns listOf(exerciseLog)
            coEvery { setLogDao.getSetLogsForExercise(1) } returns listOf(setLog)
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
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns emptyList()
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            coEvery { firestoreRepository.uploadWorkouts(any(), any()) } returns Result.failure(Exception(errorMessage))
            coEvery { firestoreRepository.uploadExerciseLogs(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadSetLogs(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseCores(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseVariations(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadVariationMuscles(any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadVariationInstructions(any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadVariationAliases(any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadVariationRelations(any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammes(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammeWeeks(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammeWorkouts(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseSubstitutions(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadProgrammeProgress(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadUserExerciseMaxes(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadOneRMHistory(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadPersonalRecords(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseSwapHistory(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExercisePerformanceTracking(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadGlobalExerciseProgress(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadExerciseCorrelations(any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadTrainingAnalyses(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.uploadParseRequests(any(), any()) } returns Result.success(Unit)
            coEvery { firestoreRepository.updateSyncMetadata(any(), any(), any()) } returns Result.success(Unit)

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
            val lastSyncTime = Timestamp.now()
            val metadata = mockk<com.github.radupana.featherweight.sync.models.FirestoreSyncMetadata>()

            every { metadata.lastSyncTime } returns lastSyncTime
            every { authManager.getCurrentUserId() } returns userId
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(metadata)
            coEvery { workoutDao.getAllWorkouts() } returns emptyList()
            mockAllDaoMethods()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            coVerify { firestoreRepository.getSyncMetadata(userId) }
        }

    @Test
    fun `syncAll filters workouts by userId`() =
        runTest {
            val userId = "test-user"
            val userWorkout =
                Workout(
                    id = 1,
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
            val otherWorkout =
                Workout(
                    id = 2,
                    userId = "other-user",
                    name = "Other Workout",
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
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns listOf(userWorkout, otherWorkout)
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns emptyList()
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(2) } returns emptyList()

            mockAllDaoMethods()

            val workoutsSlot = slot<List<com.github.radupana.featherweight.sync.models.FirestoreWorkout>>()
            mockAllFirestoreDownloads()
            mockAllFirestoreUploads()
            coEvery { firestoreRepository.uploadWorkouts(any(), capture(workoutsSlot)) } returns Result.success(Unit)

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            assertEquals(1, workoutsSlot.captured.size)
            assertEquals(1, workoutsSlot.captured[0].localId)
        }

    @Test
    fun `syncAll performs bidirectional sync - download then upload`() =
        runTest {
            val userId = "test-user"
            val downloadedWorkout =
                com.github.radupana.featherweight.sync.models.FirestoreWorkout(
                    localId = 99,
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
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns emptyList()
            coEvery { workoutDao.getWorkoutById(99) } returns null
            coEvery { workoutDao.insertWorkout(any()) } returns 99L
            mockAllDaoMethods()

            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(downloadedWorkout))
            coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseCores(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseVariations(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationMuscles() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationInstructions() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationAliases() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationRelations() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSubstitutions(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadOneRMHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseCorrelations() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTrainingAnalyses(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadParseRequests(any()) } returns Result.success(emptyList())

            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify {
                firestoreRepository.downloadWorkouts(userId, null)
                workoutDao.insertWorkout(any())
            }
        }

    @Test
    fun `syncAll handles download failure gracefully`() =
        runTest {
            val userId = "test-user"
            val errorMessage = "Download failed"

            every { authManager.getCurrentUserId() } returns userId
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { firestoreRepository.downloadWorkouts(any(), any()) } returns Result.failure(Exception(errorMessage))

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Error)
            assertTrue(state.message.contains("Download failed"))

            coVerify(exactly = 0) { firestoreRepository.uploadWorkouts(any(), any()) }
        }

    @Test
    fun `restoreFromCloud downloads and merges all data for fresh install`() =
        runTest {
            val userId = "test-user"

            every { authManager.getCurrentUserId() } returns userId
            mockAllDaoMethods()
            mockAllFirestoreDownloads()

            // Mock the missing DAO methods needed for merge operations
            coEvery { workoutDao.getWorkoutById(any()) } returns null
            coEvery { workoutDao.insertWorkout(any()) } returns 1L
            coEvery { exerciseLogDao.getExerciseLogById(any()) } returns null
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 1L
            coEvery { setLogDao.getSetLogById(any()) } returns null
            coEvery { setLogDao.insertSetLog(any()) } returns 1L
            coEvery { firestoreRepository.updateSyncMetadata(any(), any(), any()) } returns Result.success(Unit)

            val result = syncManager.restoreFromCloud()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify {
                firestoreRepository.downloadWorkouts(userId, null)
                firestoreRepository.downloadExerciseLogs(userId, null)
                firestoreRepository.downloadSetLogs(userId, null)
            }
        }

    @Test
    fun `syncAll merges remote workouts with conflict resolution`() =
        runTest {
            val userId = "test-user"
            val localWorkout =
                Workout(
                    id = 1,
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
                    localId = 1,
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
            coEvery { firestoreRepository.getSyncMetadata(userId) } returns Result.success(null)
            coEvery { workoutDao.getAllWorkouts() } returns listOf(localWorkout)
            coEvery { workoutDao.getWorkoutById(1) } returns localWorkout
            coEvery { workoutDao.updateWorkout(any()) } returns Unit
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1) } returns emptyList()
            coEvery { firestoreRepository.downloadWorkouts(userId, null) } returns Result.success(listOf(remoteWorkout))

            mockAllDaoMethods()
            coEvery { firestoreRepository.downloadExerciseLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadSetLogs(any(), any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseCores(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseVariations(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationMuscles() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationInstructions() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationAliases() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadVariationRelations() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWeeks(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeWorkouts(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSubstitutions(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadProgrammeProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadUserExerciseMaxes(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadOneRMHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadPersonalRecords(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseSwapHistory(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExercisePerformanceTracking(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadGlobalExerciseProgress(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadExerciseCorrelations() } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadTrainingAnalyses(any()) } returns Result.success(emptyList())
            coEvery { firestoreRepository.downloadParseRequests(any()) } returns Result.success(emptyList())
            mockAllFirestoreUploads()

            val result = syncManager.syncAll()

            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertTrue(state is SyncState.Success)

            coVerify {
                workoutDao.updateWorkout(
                    withArg {
                        assertEquals("Remote Workout", it.name)
                        assertEquals("Updated from cloud", it.notes)
                    },
                )
            }
        }
}
