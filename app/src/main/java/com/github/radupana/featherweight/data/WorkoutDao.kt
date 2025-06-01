package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Query("SELECT * FROM Workout ORDER BY date DESC")
    suspend fun getAllWorkouts(): List<Workout>

    @Query("SELECT * FROM ExerciseLog WHERE workoutId = :workoutId")
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog>
}