// Suppression justified: Room DAO interfaces inherently require many query methods to support
// the various data access patterns needed by the application. Splitting into multiple DAOs
// would create artificial boundaries and complicate transaction management.
@file:Suppress("TooManyFunctions")

package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import java.time.LocalDateTime

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: Workout)

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Upsert
    suspend fun upsertWorkout(workout: Workout)

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllWorkouts(userId: String): List<Workout>

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentWorkouts(
        userId: String,
        limit: Int,
    ): List<Workout>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): Workout?

    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getWorkoutsInDateRange(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Workout>

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)

    @Query("DELETE FROM workouts WHERE programmeId = :programmeId")
    suspend fun deleteWorkoutsByProgramme(programmeId: String)

    @Query("SELECT COUNT(*) FROM workouts WHERE programmeId = :programmeId AND status != 'COMPLETED'")
    suspend fun getInProgressWorkoutCountByProgramme(programmeId: String): Int

    // Programme-related queries
    @Query("SELECT * FROM workouts WHERE programmeId = :programmeId ORDER BY date DESC")
    suspend fun getWorkoutsByProgramme(programmeId: String): List<Workout>

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM workouts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM workouts WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    // Get the workout date when a specific weight was achieved for an exercise
    @Query(
        """
        SELECT w.date FROM workouts w
        INNER JOIN exercise_logs el ON el.workoutId = w.id
        INNER JOIN set_logs sl ON sl.exerciseLogId = el.id
        WHERE el.exerciseId = :exerciseId
        AND sl.actualWeight = :weight
        AND sl.isCompleted = 1
        AND w.date BETWEEN :startDateTime AND :endDateTime
        ORDER BY w.date DESC
        LIMIT 1
    """,
    )
    suspend fun getWorkoutDateForMaxWeight(
        exerciseId: String,
        weight: Float,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ): LocalDateTime?

    @Query("SELECT date, COUNT(*) as count FROM workouts WHERE userId = :userId AND date >= :startDate AND date < :endDate AND status = 'COMPLETED' GROUP BY date")
    suspend fun getWorkoutCountsByDateRange(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<WorkoutDateCount>

    @Query(
        """
        SELECT date, status, COUNT(*) as count
        FROM workouts
        WHERE userId = :userId
        AND date >= :startDate
        AND date < :endDate
        GROUP BY date, status
    """,
    )
    suspend fun getWorkoutCountsByDateRangeWithStatus(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<WorkoutDateStatusCount>

    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startOfWeek AND date < :endOfWeek AND status = 'COMPLETED' ORDER BY date DESC")
    suspend fun getWorkoutsByWeek(
        userId: String,
        startOfWeek: LocalDateTime,
        endOfWeek: LocalDateTime,
    ): List<Workout>

    // Export-related queries
    @Query(
        """
        SELECT * FROM workouts
        WHERE userId = :userId
        AND date BETWEEN :startDate AND :endDate
        AND status != :excludeStatus
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getWorkoutsInDateRangePaged(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        excludeStatus: WorkoutStatus = WorkoutStatus.NOT_STARTED,
        limit: Int,
        offset: Int,
    ): List<Workout>

    @Query(
        """
        SELECT COUNT(*) FROM workouts
        WHERE userId = :userId
        AND date BETWEEN :startDate AND :endDate
        AND status != :excludeStatus
        """,
    )
    suspend fun getWorkoutCountInDateRange(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        excludeStatus: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    ): Int

    @Query("SELECT * FROM workouts WHERE programmeId = :programmeId AND status = 'COMPLETED' ORDER BY weekNumber, dayNumber")
    suspend fun getCompletedWorkoutsByProgramme(programmeId: String): List<Workout>

    @Query("SELECT COUNT(*) FROM workouts WHERE programmeId = :programmeId AND status = 'COMPLETED'")
    suspend fun getCompletedWorkoutCountByProgramme(programmeId: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        INNER JOIN workouts w ON el.workoutId = w.id
        WHERE w.programmeId = :programmeId AND w.status = 'COMPLETED'
    """,
    )
    suspend fun getCompletedSetCountByProgramme(programmeId: String): Int
}

data class WorkoutDateCount(
    val date: LocalDateTime,
    val count: Int,
)

data class WorkoutDateStatusCount(
    val date: LocalDateTime,
    val status: WorkoutStatus,
    val count: Int,
)
