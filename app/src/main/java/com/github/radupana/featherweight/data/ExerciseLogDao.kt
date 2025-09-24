package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Upsert
    suspend fun upsertExerciseLog(exerciseLog: ExerciseLog): Long

    @Query("SELECT * FROM exercise_logs WHERE workoutId = :workoutId ORDER BY exerciseOrder")
    suspend fun getExerciseLogsForWorkout(workoutId: Long): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE id = :exerciseLogId")
    suspend fun deleteExerciseLog(exerciseLogId: Long)

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE exerciseVariationId = :exerciseVariationId AND userId = :userId")
    suspend fun getExerciseUsageCount(
        exerciseVariationId: Long,
        userId: String,
    ): Int

    @Query("UPDATE exercise_logs SET exerciseOrder = :newOrder WHERE id = :exerciseLogId")
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    )

    @Update
    suspend fun update(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE id = :id")
    suspend fun getExerciseLogById(id: Long): ExerciseLog?

    @Query(
        """
        SELECT el.* FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND el.userId = :userId
        AND w.date >= :startDate
        AND w.date <= :endDate
        AND w.status = 'COMPLETED'
        ORDER BY w.date DESC
    """,
    )
    suspend fun getExerciseLogsInDateRange(
        exerciseVariationId: Long,
        userId: String,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM exercise_logs WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM exercise_logs WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query(
        """
        SELECT COUNT(DISTINCT w.id) FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND el.userId = :userId
        AND w.status = 'COMPLETED'
    """,
    )
    suspend fun getTotalSessionsForExercise(
        exerciseVariationId: Long,
        userId: String,
    ): Int

    @Query(
        """
        SELECT DISTINCT el.exerciseVariationId FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.userId = :userId
        AND w.status = 'COMPLETED'
        ORDER BY el.exerciseVariationId
    """,
    )
    suspend fun getAllUniqueExerciseVariationIds(userId: String): List<Long>

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAllExerciseLogs()

    @Query(
        """
        SELECT w.* FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND el.userId = :userId
        AND w.status = 'COMPLETED'
        AND w.date >= :startDate
        AND w.date <= :endDate
        GROUP BY w.date
        ORDER BY w.date DESC
    """,
    )
    suspend fun getDistinctWorkoutsForExercise(
        exerciseVariationId: Long,
        userId: String,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<Workout>
}
