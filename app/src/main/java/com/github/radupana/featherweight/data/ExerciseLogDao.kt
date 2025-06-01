package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Query("SELECT * FROM ExerciseLog WHERE workoutId = :workoutId ORDER BY exerciseOrder")
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog>
}
