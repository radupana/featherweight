package com.github.radupana.featherweight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.radupana.featherweight.data.exercise.*
import com.github.radupana.featherweight.data.programme.*

@Database(
    entities = [
        Workout::class,
        ExerciseLog::class,
        SetLog::class,
        Exercise::class,
        ExerciseMuscleGroup::class,
        ExerciseEquipment::class,
        ExerciseMovementPattern::class,
        Programme::class,
        ProgrammeTemplate::class,
        ProgrammeWeek::class,
        ProgrammeWorkout::class,
        ExerciseSubstitution::class,
        ProgrammeProgress::class,
    ],
    version = 11,
    exportSchema = false,
)
@TypeConverters(DateConverters::class, ExerciseTypeConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun setLogDao(): SetLogDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun programmeDao(): ProgrammeDao

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
                        ).fallbackToDestructiveMigration() // This will nuke and recreate
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
