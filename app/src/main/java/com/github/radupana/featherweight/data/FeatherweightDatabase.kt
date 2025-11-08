package com.github.radupana.featherweight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.ExerciseTypeConverters
import com.github.radupana.featherweight.data.exercise.UserExerciseUsage
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao
import com.github.radupana.featherweight.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        Workout::class,
        ExerciseLog::class,
        SetLog::class,
        // Template entities
        WorkoutTemplate::class,
        TemplateExercise::class,
        TemplateSet::class,
        // Exercise entities
        Exercise::class,
        ExerciseInstruction::class,
        ExerciseAlias::class,
        ExerciseMuscle::class,
        // Usage tracking
        UserExerciseUsage::class,
        // Other entities
        Programme::class,
        ProgrammeWeek::class,
        ProgrammeWorkout::class,
        ProgrammeProgress::class,
        ExerciseMaxTracking::class,
        ExerciseSwapHistory::class,
        ProgrammeExerciseTracking::class,
        GlobalExerciseProgress::class,
        PersonalRecord::class,
        TrainingAnalysis::class,
        ParseRequest::class,
        WorkoutDeviation::class,
        // Sync metadata
        LocalSyncMetadata::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DateConverters::class, ExerciseTypeConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun setLogDao(): SetLogDao

    // Exercise DAOs
    abstract fun exerciseDao(): ExerciseDao

    abstract fun exerciseInstructionDao(): ExerciseInstructionDao

    abstract fun exerciseAliasDao(): ExerciseAliasDao

    abstract fun exerciseMuscleDao(): ExerciseMuscleDao

    abstract fun userExerciseUsageDao(): UserExerciseUsageDao

    abstract fun programmeDao(): ProgrammeDao

    abstract fun exerciseMaxTrackingDao(): ExerciseMaxTrackingDao

    abstract fun exerciseSwapHistoryDao(): ExerciseSwapHistoryDao

    abstract fun programmeExerciseTrackingDao(): ProgrammeExerciseTrackingDao

    abstract fun globalExerciseProgressDao(): GlobalExerciseProgressDao

    abstract fun personalRecordDao(): PersonalRecordDao

    abstract fun trainingAnalysisDao(): TrainingAnalysisDao

    abstract fun parseRequestDao(): ParseRequestDao

    abstract fun workoutTemplateDao(): WorkoutTemplateDao

    abstract fun templateExerciseDao(): TemplateExerciseDao

    abstract fun templateSetDao(): TemplateSetDao

    abstract fun localSyncMetadataDao(): LocalSyncMetadataDao

    abstract fun workoutDeviationDao(): WorkoutDeviationDao

    companion object {
        @Volatile
        private var INSTANCE: FeatherweightDatabase? = null

        fun getDatabase(context: Context): FeatherweightDatabase =
            INSTANCE ?: synchronized(this) {
                val keyManager = DatabaseKeyManager(context.applicationContext)
                val passphraseBytes = keyManager.getDatabasePassphrase()
                val factory = SupportFactory(passphraseBytes)

                try {
                    val instance =
                        Room
                            .databaseBuilder(
                                context.applicationContext,
                                FeatherweightDatabase::class.java,
                                "featherweight-db",
                            ).openHelperFactory(factory)
                            .build()

                    // Try to access the database to verify it can be opened
                    instance.openHelper.writableDatabase

                    INSTANCE = instance
                    return instance
                } catch (e: Exception) {
                    // If database can't be opened (wrong key), delete it and recreate
                    if (e.message?.contains("file is not a database") == true ||
                        e.message?.contains("cipher") == true
                    ) {
                        android.util.Log.w(
                            "FeatherweightDatabase",
                            "Database encryption key mismatch, deleting and recreating database",
                            e,
                        )

                        // Delete the corrupted database
                        context.applicationContext.deleteDatabase("featherweight-db")

                        // Clear the stored key to generate a new one
                        keyManager.clearDatabaseKey()

                        // Get a new passphrase and create fresh database
                        val newPassphraseBytes = keyManager.getDatabasePassphrase()
                        val newFactory = SupportFactory(newPassphraseBytes)

                        val instance =
                            Room
                                .databaseBuilder(
                                    context.applicationContext,
                                    FeatherweightDatabase::class.java,
                                    "featherweight-db",
                                ).openHelperFactory(newFactory)
                                .build()
                        INSTANCE = instance
                        return instance
                    } else {
                        throw e
                    }
                }
            }
    }
}
