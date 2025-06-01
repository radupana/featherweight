package com.github.radupana.featherweight.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Workout::class, ExerciseLog::class], version = 1)
@TypeConverters(DateConverters::class)
abstract class FeatherweightDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}