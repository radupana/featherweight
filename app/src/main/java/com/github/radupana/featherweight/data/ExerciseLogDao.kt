package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog)

    @Upsert
    suspend fun upsertExerciseLog(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE workoutId = :workoutId ORDER BY exerciseOrder")
    suspend fun getExerciseLogsForWorkout(workoutId: String): List<ExerciseLog>

    @Query("SELECT * FROM exercise_logs WHERE workoutId IN (:workoutIds) ORDER BY workoutId, exerciseOrder")
    suspend fun getExerciseLogsForWorkouts(workoutIds: List<String>): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE id = :exerciseLogId")
    suspend fun deleteExerciseLog(exerciseLogId: String)

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE exerciseId = :exerciseId AND userId = :userId")
    suspend fun getExerciseUsageCount(
        exerciseId: String,
        userId: String,
    ): Int

    @Query("UPDATE exercise_logs SET exerciseOrder = :newOrder WHERE id = :exerciseLogId")
    suspend fun updateExerciseOrder(
        exerciseLogId: String,
        newOrder: Int,
    )

    @Update
    suspend fun update(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE id = :id")
    suspend fun getExerciseLogById(id: String): ExerciseLog?

    @Query("SELECT id FROM exercise_logs WHERE id IN (:ids)")
    suspend fun getExistingExerciseLogIds(ids: List<String>): List<String>

    @Query(
        """
        SELECT el.* FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.exerciseId = :exerciseId
        AND el.userId = :userId
        AND w.date >= :startDate
        AND w.date <= :endDate
        AND w.status = 'COMPLETED'
        ORDER BY w.date DESC
    """,
    )
    suspend fun getExerciseLogsInDateRange(
        exerciseId: String,
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
        WHERE el.exerciseId = :exerciseId
        AND el.userId = :userId
        AND w.status = 'COMPLETED'
    """,
    )
    suspend fun getTotalSessionsForExercise(
        exerciseId: String,
        userId: String,
    ): Int

    @Query(
        """
        SELECT DISTINCT el.exerciseId FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.userId = :userId
        AND w.status = 'COMPLETED'
        ORDER BY el.exerciseId
    """,
    )
    suspend fun getAllUniqueExerciseIds(userId: String): List<String>

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAllExerciseLogs()

    @Query(
        """
        SELECT w.* FROM exercise_logs el
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE el.exerciseId = :exerciseId
        AND el.userId = :userId
        AND w.status = 'COMPLETED'
        AND w.date >= :startDate
        AND w.date <= :endDate
        GROUP BY w.date
        ORDER BY w.date DESC
    """,
    )
    suspend fun getDistinctWorkoutsForExercise(
        exerciseId: String,
        userId: String,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<Workout>
}
