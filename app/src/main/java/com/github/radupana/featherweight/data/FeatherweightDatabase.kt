package com.github.radupana.featherweight.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Workout::class, ExerciseLog::class, SetLog::class],
    version = 2, // Increment if this is not your first version!
)
@TypeConverters(DateConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun setLogDao(): SetLogDao
}
