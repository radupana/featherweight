package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Insert
    suspend fun insert(exerciseLog: ExerciseLog): Long

    @Query("SELECT * FROM ExerciseLog WHERE workoutId = :workoutId ORDER BY exerciseOrder")
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog>

    @Query("DELETE FROM ExerciseLog WHERE id = :exerciseLogId")
    suspend fun deleteExerciseLog(exerciseLogId: Long)

    @Query("SELECT COUNT(*) FROM ExerciseLog WHERE exerciseName = :exerciseName")
    suspend fun getExerciseUsageCount(exerciseName: String): Int

    @Query("UPDATE ExerciseLog SET exerciseOrder = :newOrder WHERE id = :exerciseLogId")
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    )

    @Update
    suspend fun update(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM ExerciseLog WHERE id = :id")
    suspend fun getExerciseLogById(id: Long): ExerciseLog?

    @Query(
        """
        SELECT el.* FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseName = :exerciseName
        AND w.date >= :startDate
        AND w.date <= :endDate
        AND w.status = 'COMPLETED'
        ORDER BY w.date DESC
    """,
    )
    suspend fun getExerciseLogsInDateRange(
        exerciseName: String,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<ExerciseLog>

    @Query(
        """
        SELECT COUNT(DISTINCT w.id) FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseName = :exerciseName
        AND w.status = 'COMPLETED'
    """,
    )
    suspend fun getTotalSessionsForExercise(exerciseName: String): Int

    @Query(
        """
        SELECT DISTINCT el.exerciseName FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE w.status = 'COMPLETED'
        ORDER BY el.exerciseName
    """,
    )
    suspend fun getAllUniqueExercises(): List<String>

    @Query("DELETE FROM ExerciseLog")
    suspend fun deleteAllExerciseLogs()

    @Query(
        """
        SELECT w.* FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseName = :exerciseName
        AND w.status = 'COMPLETED'
        AND w.date >= :startDate
        AND w.date <= :endDate
        GROUP BY w.date
        ORDER BY w.date DESC
    """,
    )
    suspend fun getDistinctWorkoutsForExercise(
        exerciseName: String,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<Workout>
}
