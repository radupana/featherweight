package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExercisePerformanceTrackingDao {
    @Insert
    suspend fun insertPerformanceRecord(record: ExercisePerformanceTracking): Long

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

}
