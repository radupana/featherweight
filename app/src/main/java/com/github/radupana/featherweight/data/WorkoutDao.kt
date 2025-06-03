package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("SELECT * FROM Workout ORDER BY date DESC")
    suspend fun getAllWorkouts(): List<Workout>

    @Query("SELECT * FROM Workout WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): Workout?
}
