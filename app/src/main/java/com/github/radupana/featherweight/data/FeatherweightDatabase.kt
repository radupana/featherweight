package com.github.radupana.featherweight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseEquipment
import com.github.radupana.featherweight.data.exercise.ExerciseMovementPattern
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleGroup
import com.github.radupana.featherweight.data.exercise.ExerciseTypeConverters
import com.github.radupana.featherweight.data.exercise.WgerCategoryEntity
import com.github.radupana.featherweight.data.exercise.WgerExerciseMuscle
import com.github.radupana.featherweight.data.exercise.WgerMuscleEntity
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
        ExerciseMuscleGroup::class,
        ExerciseEquipment::class,
        ExerciseMovementPattern::class,
        ExerciseAlias::class,
        WgerMuscleEntity::class,
        WgerCategoryEntity::class,
        WgerExerciseMuscle::class,
        Programme::class,
        ProgrammeTemplate::class,
        ProgrammeWeek::class,
        ProgrammeWorkout::class,
        ExerciseSubstitution::class,
        ProgrammeProgress::class,
        UserProfile::class,
        UserExerciseMax::class,
    ],
    version = 21,
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

    companion object {
        @Volatile
        private var INSTANCE: FeatherweightDatabase? = null


        fun getDatabase(context: Context): FeatherweightDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            FeatherweightDatabase::class.java,
                            "featherweight-db",
                        )
                        .fallbackToDestructiveMigration() // Just nuke and recreate during development
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
