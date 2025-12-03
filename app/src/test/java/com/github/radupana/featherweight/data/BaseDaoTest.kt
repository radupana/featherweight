package com.github.radupana.featherweight.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import java.io.File

/**
 * Base class for DAO tests that provides Room in-memory database setup.
 *
 * This class uses BundledSQLiteDriver for pure JVM testing without Robolectric.
 * This approach:
 * - Runs on pure JVM (no Android framework needed)
 * - Is faster than Robolectric
 * - Works reliably in CI without native library issues
 * - Uses the same SQLite implementation as Android
 *
 * Usage:
 * ```
 * class MyDaoTest : BaseDaoTest() {
 *     @Test
 *     fun `test something`() = runTest {
 *         // Access DAOs via workoutDao, exerciseDao, etc.
 *         workoutDao.insertWorkout(workout)
 *         // ...
 *     }
 * }
 * ```
 */
abstract class BaseDaoTest {
    protected lateinit var database: FeatherweightDatabase
    private lateinit var dbFile: File

    // Workout-related DAOs
    protected lateinit var workoutDao: WorkoutDao
    protected lateinit var exerciseLogDao: ExerciseLogDao
    protected lateinit var setLogDao: SetLogDao

    // Template DAOs
    protected lateinit var workoutTemplateDao: WorkoutTemplateDao
    protected lateinit var templateExerciseDao: TemplateExerciseDao
    protected lateinit var templateSetDao: TemplateSetDao

    // Exercise DAOs
    protected lateinit var exerciseDao: ExerciseDao
    protected lateinit var exerciseInstructionDao: ExerciseInstructionDao
    protected lateinit var exerciseAliasDao: ExerciseAliasDao
    protected lateinit var exerciseMuscleDao: ExerciseMuscleDao
    protected lateinit var userExerciseUsageDao: UserExerciseUsageDao

    // Programme DAOs
    protected lateinit var programmeDao: ProgrammeDao
    protected lateinit var exerciseMaxTrackingDao: ExerciseMaxTrackingDao
    protected lateinit var exerciseSwapHistoryDao: ExerciseSwapHistoryDao
    protected lateinit var programmeExerciseTrackingDao: ProgrammeExerciseTrackingDao
    protected lateinit var globalExerciseProgressDao: GlobalExerciseProgressDao
    protected lateinit var personalRecordDao: PersonalRecordDao
    protected lateinit var trainingAnalysisDao: TrainingAnalysisDao
    protected lateinit var parseRequestDao: ParseRequestDao
    protected lateinit var workoutDeviationDao: WorkoutDeviationDao

    // Sync DAO
    protected lateinit var localSyncMetadataDao: LocalSyncMetadataDao

    @Before
    fun setupDatabase() {
        // Create a temp file for the database (deleted after test)
        dbFile = File.createTempFile("test_featherweight_", ".db")
        dbFile.deleteOnExit()

        // Create database using BundledSQLiteDriver (pure JVM via room-runtime-jvm)
        // The JVM API uses the reified type parameter and doesn't need Android Context
        database =
            Room
                .databaseBuilder<FeatherweightDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()

        // Initialize all DAOs
        workoutDao = database.workoutDao()
        exerciseLogDao = database.exerciseLogDao()
        setLogDao = database.setLogDao()

        workoutTemplateDao = database.workoutTemplateDao()
        templateExerciseDao = database.templateExerciseDao()
        templateSetDao = database.templateSetDao()

        exerciseDao = database.exerciseDao()
        exerciseInstructionDao = database.exerciseInstructionDao()
        exerciseAliasDao = database.exerciseAliasDao()
        exerciseMuscleDao = database.exerciseMuscleDao()
        userExerciseUsageDao = database.userExerciseUsageDao()

        programmeDao = database.programmeDao()
        exerciseMaxTrackingDao = database.exerciseMaxTrackingDao()
        exerciseSwapHistoryDao = database.exerciseSwapHistoryDao()
        programmeExerciseTrackingDao = database.programmeExerciseTrackingDao()
        globalExerciseProgressDao = database.globalExerciseProgressDao()
        personalRecordDao = database.personalRecordDao()
        trainingAnalysisDao = database.trainingAnalysisDao()
        parseRequestDao = database.parseRequestDao()
        workoutDeviationDao = database.workoutDeviationDao()

        localSyncMetadataDao = database.localSyncMetadataDao()
    }

    @After
    fun closeDatabase() {
        database.close()
        // Clean up temp database file
        if (::dbFile.isInitialized && dbFile.exists()) {
            dbFile.delete()
        }
    }
}
