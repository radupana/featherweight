package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProgrammeExerciseTrackingDao {
    @Insert
    suspend fun insertPerformanceRecord(record: ProgrammeExerciseTracking)

    @Query(
        """
        SELECT * FROM programme_exercise_tracking
        WHERE programmeId = :programmeId
        AND exerciseName = :exerciseName
        ORDER BY workoutDate DESC
        LIMIT :limit
    """,
    )
    suspend fun getRecentPerformance(
        programmeId: String,
        exerciseName: String,
        limit: Int = 3,
    ): List<ProgrammeExerciseTracking>

    @Query(
        """
        SELECT COUNT(*) FROM programme_exercise_tracking
        WHERE programmeId = :programmeId
        AND exerciseName = :exerciseName
        AND wasSuccessful = 0
        AND workoutDate > (
            SELECT COALESCE(MAX(workoutDate), '1970-01-01')
            FROM programme_exercise_tracking
            WHERE programmeId = :programmeId
            AND exerciseName = :exerciseName
            AND (wasSuccessful = 1 OR isDeloadWorkout = 1)
        )
    """,
    )
    suspend fun getConsecutiveFailures(
        programmeId: String,
        exerciseName: String,
    ): Int

    @Query("SELECT * FROM programme_exercise_tracking")
    suspend fun getAllTracking(): List<ProgrammeExerciseTracking>

    @Query("SELECT * FROM programme_exercise_tracking WHERE id = :id")
    suspend fun getTrackingById(id: String): ProgrammeExerciseTracking?

    @Insert
    suspend fun insertTracking(tracking: ProgrammeExerciseTracking)

    @Query("DELETE FROM programme_exercise_tracking WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM programme_exercise_tracking WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM programme_exercise_tracking WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM programme_exercise_tracking")
    suspend fun deleteAllTracking()
}
