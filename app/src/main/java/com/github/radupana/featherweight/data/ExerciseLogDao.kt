package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Query("SELECT * FROM ExerciseLog WHERE workoutId = :workoutId ORDER BY exerciseOrder")
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog>

    @Query("DELETE FROM ExerciseLog WHERE id = :exerciseLogId")
    suspend fun deleteExerciseLog(exerciseLogId: Long)

    @Query("SELECT COUNT(*) FROM ExerciseLog WHERE exerciseName = :exerciseName")
    suspend fun getExerciseUsageCount(exerciseName: String): Int

    @Query("UPDATE ExerciseLog SET exerciseOrder = :newOrder WHERE id = :exerciseLogId")
    suspend fun updateExerciseOrder(exerciseLogId: Long, newOrder: Int)
    
    @Update
    suspend fun update(exerciseLog: ExerciseLog)
    
    @Query("SELECT * FROM ExerciseLog WHERE id = :id")
    suspend fun getExerciseLogById(id: Long): ExerciseLog?
}
