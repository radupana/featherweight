package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SetLogDao {
    @Insert
    suspend fun insertSetLog(setLog: SetLog): Long
    
    @Insert
    suspend fun insert(setLog: SetLog): Long

    @Query("SELECT * FROM SetLog WHERE exerciseLogId = :exerciseLogId ORDER BY setOrder")
    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog>

    @Query("UPDATE SetLog SET isCompleted = :completed, completedAt = :completedAt WHERE id = :setId")
    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    )

    @Update
    suspend fun updateSetLog(setLog: SetLog)
    
    @Update
    suspend fun update(setLog: SetLog)

    @Query("DELETE FROM SetLog WHERE id = :setId")
    suspend fun deleteSetLog(setId: Long)

    @Query("DELETE FROM SetLog WHERE exerciseLogId = :exerciseLogId")
    suspend fun deleteAllSetsForExercise(exerciseLogId: Long)

    // Progress Analytics queries
    @Query(
        """
        SELECT s.* FROM SetLog s 
        INNER JOIN ExerciseLog e ON s.exerciseLogId = e.id 
        INNER JOIN Workout w ON e.workoutId = w.id
        WHERE e.exerciseName = :exerciseName 
        AND s.isCompleted = 1 
        AND w.date >= :sinceDate 
        ORDER BY w.date DESC
    """,
    )
    suspend fun getSetsForExerciseSince(
        exerciseName: String,
        sinceDate: String,
    ): List<SetLog>

    @Query(
        """
        SELECT DISTINCT e.exerciseName 
        FROM ExerciseLog e 
        INNER JOIN SetLog s ON e.id = s.exerciseLogId 
        WHERE s.isCompleted = 1
    """,
    )
    suspend fun getAllCompletedExerciseNames(): List<String>

    @Query(
        """
        SELECT w.date 
        FROM Workout w 
        INNER JOIN ExerciseLog e ON w.id = e.workoutId 
        INNER JOIN SetLog s ON e.id = s.exerciseLogId 
        WHERE s.id = :setLogId
    """,
    )
    suspend fun getWorkoutDateForSetLog(setLogId: Long): String?

    @Query(
        """
        SELECT MAX(s.actualWeight) 
        FROM SetLog s 
        INNER JOIN ExerciseLog e ON s.exerciseLogId = e.id 
        INNER JOIN Workout w ON e.workoutId = w.id
        WHERE e.exerciseName = :exerciseName 
        AND s.isCompleted = 1 
        AND w.date < :beforeDate
    """,
    )
    suspend fun getMaxWeightForExerciseBefore(
        exerciseName: String,
        beforeDate: String,
    ): Float?

    @Query("DELETE FROM SetLog")
    suspend fun deleteAllSetLogs()

    @Query(
        """
        SELECT MAX(s.actualWeight) 
        FROM SetLog s 
        INNER JOIN ExerciseLog e ON s.exerciseLogId = e.id 
        INNER JOIN Workout w ON e.workoutId = w.id
        WHERE e.exerciseName = :exerciseName 
        AND s.isCompleted = 1 
        AND s.actualWeight > 0
        AND w.date >= :startDate
        AND w.date <= :endDate
    """,
    )
    suspend fun getMaxWeightForExerciseInDateRange(
        exerciseName: String,
        startDate: String,
        endDate: String,
    ): Float?
}
