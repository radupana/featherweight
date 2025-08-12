package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COUNT(*) FROM ExerciseLog WHERE exerciseVariationId = :exerciseVariationId")
    suspend fun getExerciseUsageCount(exerciseVariationId: Long): Int

    @Query("UPDATE ExerciseLog SET exerciseOrder = :newOrder WHERE id = :exerciseLogId")
    suspend fun updateExerciseOrder(
        exerciseLogId: Long,
        newOrder: Int,
    )

    // Optimized queries with JOINs to get exercise names
    @Query(
        """
        SELECT el.*, ev.name as exerciseName 
        FROM ExerciseLog el 
        JOIN exercise_variations ev ON el.exerciseVariationId = ev.id 
        WHERE el.workoutId = :workoutId 
        ORDER BY el.exerciseOrder
    """,
    )
    suspend fun getExerciseLogsWithNames(workoutId: Long): List<ExerciseLogWithName>

    @Query(
        """
        SELECT el.*, ev.name as exerciseName 
        FROM ExerciseLog el 
        JOIN exercise_variations ev ON el.exerciseVariationId = ev.id 
        WHERE el.id = :exerciseLogId
    """,
    )
    suspend fun getExerciseLogWithName(exerciseLogId: Long): ExerciseLogWithName?

    // Flow versions for reactive UI
    @Query(
        """
        SELECT el.*, ev.name as exerciseName 
        FROM ExerciseLog el 
        JOIN exercise_variations ev ON el.exerciseVariationId = ev.id 
        WHERE el.workoutId = :workoutId 
        ORDER BY el.exerciseOrder
    """,
    )
    fun getExerciseLogsWithNamesFlow(workoutId: Long): Flow<List<ExerciseLogWithName>>

    @Update
    suspend fun update(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM ExerciseLog WHERE id = :id")
    suspend fun getExerciseLogById(id: Long): ExerciseLog?

    @Query(
        """
        SELECT el.* FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND w.date >= :startDate
        AND w.date <= :endDate
        AND w.status = 'COMPLETED'
        ORDER BY w.date DESC
    """,
    )
    suspend fun getExerciseLogsInDateRange(
        exerciseVariationId: Long,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<ExerciseLog>

    @Query(
        """
        SELECT COUNT(DISTINCT w.id) FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND w.status = 'COMPLETED'
    """,
    )
    suspend fun getTotalSessionsForExercise(exerciseVariationId: Long): Int

    @Query(
        """
        SELECT DISTINCT el.exerciseVariationId FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE w.status = 'COMPLETED'
        ORDER BY el.exerciseVariationId
    """,
    )
    suspend fun getAllUniqueExerciseVariationIds(): List<Long>

    @Query("DELETE FROM ExerciseLog")
    suspend fun deleteAllExerciseLogs()

    @Query(
        """
        SELECT w.* FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND w.status = 'COMPLETED'
        AND w.date >= :startDate
        AND w.date <= :endDate
        GROUP BY w.date
        ORDER BY w.date DESC
    """,
    )
    suspend fun getDistinctWorkoutsForExercise(
        exerciseVariationId: Long,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
    ): List<Workout>

    @Query(
        """
        SELECT el.exerciseVariationId, COUNT(*) as count
        FROM ExerciseLog el
        INNER JOIN Workout w ON el.workoutId = w.id
        WHERE w.status = 'COMPLETED'
        GROUP BY el.exerciseVariationId
        ORDER BY count DESC
    """,
    )
    suspend fun getExerciseVariationUsageStatistics(): List<ExerciseUsageStatistic>
}

/**
 * Data class for exercise usage statistics.
 */
data class ExerciseUsageStatistic(
    val exerciseVariationId: Long,
    val count: Int,
)
