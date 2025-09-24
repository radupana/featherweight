package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface SetLogDao {
    @Insert
    suspend fun insertSetLog(setLog: SetLog): Long

    @Insert
    suspend fun insert(setLog: SetLog): Long

    @Upsert
    suspend fun upsertSetLog(setLog: SetLog): Long

    @Query("SELECT * FROM set_logs WHERE exerciseLogId = :exerciseLogId ORDER BY setOrder")
    suspend fun getSetLogsForExercise(exerciseLogId: Long): List<SetLog>

    @Query("SELECT * FROM set_logs WHERE id = :setLogId")
    suspend fun getSetLogById(setLogId: Long): SetLog?

    @Query("UPDATE set_logs SET isCompleted = :completed, completedAt = :completedAt WHERE id = :setId")
    suspend fun markSetCompleted(
        setId: Long,
        completed: Boolean,
        completedAt: String?,
    )

    @Update
    suspend fun updateSetLog(setLog: SetLog)

    @Update
    suspend fun update(setLog: SetLog)

    @Query("DELETE FROM set_logs WHERE id = :setId")
    suspend fun deleteSetLog(setId: Long)

    @Query("DELETE FROM set_logs WHERE exerciseLogId = :exerciseLogId")
    suspend fun deleteAllSetsForExercise(exerciseLogId: Long)

    // Progress Analytics queries
    @Query(
        """
        SELECT s.* FROM set_logs s 
        INNER JOIN exercise_logs e ON s.exerciseLogId = e.id 
        INNER JOIN workouts w ON e.workoutId = w.id
        WHERE e.exerciseVariationId = :exerciseVariationId 
        AND s.isCompleted = 1 
        AND w.date >= :sinceDate 
        ORDER BY w.date DESC
    """,
    )
    suspend fun getSetsForExerciseSince(
        exerciseVariationId: Long,
        sinceDate: String,
    ): List<SetLog>

    @Query(
        """
        SELECT w.date 
        FROM workouts w 
        INNER JOIN exercise_logs e ON w.id = e.workoutId 
        INNER JOIN set_logs s ON e.id = s.exerciseLogId 
        WHERE s.id = :setLogId
    """,
    )
    suspend fun getWorkoutDateForSetLog(setLogId: Long): String?

    @Query(
        """
        SELECT w.id 
        FROM workouts w 
        INNER JOIN exercise_logs e ON w.id = e.workoutId 
        INNER JOIN set_logs s ON e.id = s.exerciseLogId 
        WHERE s.id = :setLogId
    """,
    )
    suspend fun getWorkoutIdForSetLog(setLogId: Long): Long?

    @Query("DELETE FROM set_logs")
    suspend fun deleteAllSetLogs()

    @Query("DELETE FROM set_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM set_logs WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM set_logs WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query(
        """
        SELECT MAX(s.actualWeight) 
        FROM set_logs s 
        INNER JOIN exercise_logs e ON s.exerciseLogId = e.id 
        INNER JOIN workouts w ON e.workoutId = w.id
        WHERE e.exerciseVariationId = :exerciseVariationId 
        AND s.isCompleted = 1 
        AND s.actualWeight > 0
        AND w.date >= :startDate
        AND w.date <= :endDate
    """,
    )
    suspend fun getMaxWeightForExerciseInDateRange(
        exerciseVariationId: Long,
        startDate: String,
        endDate: String,
    ): Float?

    @Query(
        """
        SELECT s.* FROM set_logs s
        INNER JOIN exercise_logs e ON s.exerciseLogId = e.id
        INNER JOIN workouts w ON e.workoutId = w.id
        WHERE e.exerciseVariationId = :exerciseVariationId
        AND s.isCompleted = 1
        AND w.status = 'COMPLETED'
        AND s.actualWeight > 0
        ORDER BY w.date DESC, s.setOrder DESC
        LIMIT 1
    """,
    )
    suspend fun getLastCompletedSetForExercise(exerciseVariationId: Long): SetLog?
}
