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
        SELECT DISTINCT e.exerciseVariationId 
        FROM ExerciseLog e 
        INNER JOIN SetLog s ON e.id = s.exerciseLogId 
        WHERE s.isCompleted = 1
    """,
    )
    suspend fun getAllCompletedExerciseVariationIds(): List<Long>

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
        SELECT w.id 
        FROM Workout w 
        INNER JOIN ExerciseLog e ON w.id = e.workoutId 
        INNER JOIN SetLog s ON e.id = s.exerciseLogId 
        WHERE s.id = :setLogId
    """,
    )
    suspend fun getWorkoutIdForSetLog(setLogId: Long): Long?

    @Query(
        """
        SELECT MAX(s.actualWeight) 
        FROM SetLog s 
        INNER JOIN ExerciseLog e ON s.exerciseLogId = e.id 
        INNER JOIN Workout w ON e.workoutId = w.id
        WHERE e.exerciseVariationId = :exerciseVariationId 
        AND s.isCompleted = 1 
        AND w.date < :beforeDate
    """,
    )
    suspend fun getMaxWeightForExerciseBefore(
        exerciseVariationId: Long,
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
        SELECT s.* FROM SetLog s
        INNER JOIN ExerciseLog e ON s.exerciseLogId = e.id
        INNER JOIN Workout w ON e.workoutId = w.id
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
