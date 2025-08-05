package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExercisePerformanceTrackingDao {
    @Insert
    suspend fun insertPerformanceRecord(record: ExercisePerformanceTracking): Long

    @Update
    suspend fun updatePerformanceRecord(record: ExercisePerformanceTracking)

    @Query(
        """
        SELECT * FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        ORDER BY workoutDate DESC
    """,
    )
    suspend fun getPerformanceHistory(
        programmeId: Long,
        exerciseName: String,
    ): List<ExercisePerformanceTracking>

    @Query(
        """
        SELECT * FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        ORDER BY workoutDate DESC 
        LIMIT :limit
    """,
    )
    suspend fun getRecentPerformance(
        programmeId: Long,
        exerciseName: String,
        limit: Int = 3,
    ): List<ExercisePerformanceTracking>

    @Query(
        """
        SELECT COUNT(*) FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        AND wasSuccessful = 0 
        AND workoutDate > (
            SELECT COALESCE(MAX(workoutDate), '1970-01-01') 
            FROM exercise_performance_tracking 
            WHERE programmeId = :programmeId 
            AND exerciseName = :exerciseName 
            AND (wasSuccessful = 1 OR isDeloadWorkout = 1)
        )
    """,
    )
    suspend fun getConsecutiveFailures(
        programmeId: Long,
        exerciseName: String,
    ): Int

    @Query(
        """
        SELECT * FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        AND isDeloadWorkout = 1 
        ORDER BY workoutDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLastDeload(
        programmeId: Long,
        exerciseName: String,
    ): ExercisePerformanceTracking?

    @Query(
        """
        SELECT COUNT(*) FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        AND isDeloadWorkout = 1
    """,
    )
    suspend fun getTotalDeloads(
        programmeId: Long,
        exerciseName: String,
    ): Int

    @Query(
        """
        SELECT * FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId 
        AND exerciseName = :exerciseName 
        AND wasSuccessful = 1 
        ORDER BY workoutDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLastSuccess(
        programmeId: Long,
        exerciseName: String,
    ): ExercisePerformanceTracking?

    @Query(
        """
        DELETE FROM exercise_performance_tracking 
        WHERE programmeId = :programmeId
    """,
    )
    suspend fun deletePerformanceHistoryForProgramme(programmeId: Long)
}
