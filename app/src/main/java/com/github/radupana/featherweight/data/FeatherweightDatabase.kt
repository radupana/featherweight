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
import com.github.radupana.featherweight.data.migrations.MIGRATION_1_2
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.WorkoutDeviation
import com.github.radupana.featherweight.data.programme.WorkoutDeviationDao

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
    version = 2,
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
                // SQLCipher removed - using Android's built-in encryption
                // Android provides full-disk encryption by default (minSdk 26)
                // See: https://github.com/radupana/featherweight/issues/126
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            FeatherweightDatabase::class.java,
                            "featherweight-db",
                        ).addMigrations(MIGRATION_1_2)
                        .build()

                INSTANCE = instance
                instance
            }
    }
}
