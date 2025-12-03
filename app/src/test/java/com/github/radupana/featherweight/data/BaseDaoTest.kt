package com.github.radupana.featherweight.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base class for DAO tests that provides Room in-memory database setup.
 *
 * This class:
 * - Creates an in-memory Room database before each test
 * - Allows synchronous queries for simpler testing (allowMainThreadQueries)
 * - Provides access to all DAOs
 * - Properly closes the database after each test
 *
 * Usage:
 * ```
 * @RunWith(RobolectricTestRunner::class)
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
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use SDK 28 for Robolectric tests
abstract class BaseDaoTest {
    protected lateinit var database: FeatherweightDatabase

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
        // Create in-memory database
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FeatherweightDatabase::class.java,
                ).allowMainThreadQueries() // Allow sync queries for testing
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
    }
}
