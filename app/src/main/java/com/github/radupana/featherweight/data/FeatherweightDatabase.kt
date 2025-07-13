package com.github.radupana.featherweight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseTypeConverters
import com.github.radupana.featherweight.data.profile.ProfileDao
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.profile.UserProfile
import com.github.radupana.featherweight.data.programme.ExerciseSubstitution
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeTemplate
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout

@Database(
    entities = [
        Workout::class,
        ExerciseLog::class,
        SetLog::class,
        Exercise::class,
        ExerciseAlias::class,
        Programme::class,
        ProgrammeTemplate::class,
        ProgrammeWeek::class,
        ProgrammeWorkout::class,
        ExerciseSubstitution::class,
        ProgrammeProgress::class,
        UserProfile::class,
        UserExerciseMax::class,
        ExerciseSwapHistory::class,
        ExercisePerformanceTracking::class,
        GlobalExerciseProgress::class,
        ExerciseCorrelation::class,
        PersonalRecord::class,
        AIProgrammeRequest::class,
    ],
    version = 42,
    exportSchema = false,
)
@TypeConverters(DateConverters::class, ExerciseTypeConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun setLogDao(): SetLogDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun programmeDao(): ProgrammeDao

    abstract fun profileDao(): ProfileDao

    abstract fun exerciseSwapHistoryDao(): ExerciseSwapHistoryDao

    abstract fun exercisePerformanceTrackingDao(): ExercisePerformanceTrackingDao

    abstract fun globalExerciseProgressDao(): GlobalExerciseProgressDao

    abstract fun exerciseCorrelationDao(): ExerciseCorrelationDao

    abstract fun personalRecordDao(): PersonalRecordDao

    abstract fun aiProgrammeRequestDao(): AIProgrammeRequestDao

    companion object {
        @Volatile
        private var INSTANCE: FeatherweightDatabase? = null

        fun getDatabase(context: Context): FeatherweightDatabase =
            INSTANCE ?: synchronized(this) {
                android.util.Log.e("FeatherweightDebug", "FeatherweightDatabase.getDatabase: Starting database creation")
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            FeatherweightDatabase::class.java,
                            "featherweight-db",
                        ).fallbackToDestructiveMigration() // Just nuke and recreate during development
                        .build()
                android.util.Log.e("FeatherweightDebug", "FeatherweightDatabase.getDatabase: Database instance created")
                INSTANCE = instance
                instance
            }
    }
}
