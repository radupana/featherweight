package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDateTime

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("SELECT * FROM Workout ORDER BY date DESC")
    suspend fun getAllWorkouts(): List<Workout>

    @Query("SELECT * FROM Workout WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): Workout?

    @Query("SELECT * FROM Workout WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getWorkoutsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Workout>

    @Query("DELETE FROM Workout WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Query("DELETE FROM Workout WHERE programmeId = :programmeId")
    suspend fun deleteWorkoutsByProgramme(programmeId: Long)

    @Query("SELECT COUNT(*) FROM Workout WHERE programmeId = :programmeId AND status != 'COMPLETED'")
    suspend fun getInProgressWorkoutCountByProgramme(programmeId: Long): Int

    // Programme-related queries
    @Query("SELECT * FROM Workout WHERE programmeId = :programmeId ORDER BY date DESC")
    suspend fun getWorkoutsByProgramme(programmeId: Long): List<Workout>

    @Query("DELETE FROM Workout")
    suspend fun deleteAllWorkouts()

    // Get the workout date when a specific weight was achieved for an exercise
    @Query(
        """
        SELECT w.date FROM Workout w
        INNER JOIN ExerciseLog el ON el.workoutId = w.id
        INNER JOIN SetLog sl ON sl.exerciseLogId = el.id
        WHERE el.exerciseVariationId = :exerciseVariationId
        AND sl.actualWeight = :weight
        AND sl.isCompleted = 1
        AND w.date BETWEEN :startDateTime AND :endDateTime
        ORDER BY w.date DESC
        LIMIT 1
    """,
    )
    suspend fun getWorkoutDateForMaxWeight(
        exerciseVariationId: Long,
        weight: Float,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ): LocalDateTime?

    @Query("SELECT date, COUNT(*) as count FROM workout WHERE date >= :startDate AND date < :endDate AND status = 'COMPLETED' GROUP BY date")
    suspend fun getWorkoutCountsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<WorkoutDateCount>

    @Query(
        """
        SELECT date, status, COUNT(*) as count 
        FROM workout 
        WHERE date >= :startDate 
        AND date < :endDate 
        GROUP BY date, status
    """,
    )
    suspend fun getWorkoutCountsByDateRangeWithStatus(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<WorkoutDateStatusCount>

    @Query("SELECT * FROM workout WHERE date >= :startOfWeek AND date < :endOfWeek AND status = 'COMPLETED' ORDER BY date DESC")
    suspend fun getWorkoutsByWeek(
        startOfWeek: LocalDateTime,
        endOfWeek: LocalDateTime,
    ): List<Workout>

    // Export-related queries
    @Query(
        """
        SELECT * FROM Workout 
        WHERE date BETWEEN :startDate AND :endDate 
        AND status != :excludeStatus
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getWorkoutsInDateRangePaged(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        excludeStatus: WorkoutStatus = WorkoutStatus.NOT_STARTED,
        limit: Int,
        offset: Int,
    ): List<Workout>

    @Query(
        """
        SELECT COUNT(*) FROM Workout 
        WHERE date BETWEEN :startDate AND :endDate 
        AND status != :excludeStatus
        """,
    )
    suspend fun getWorkoutCountInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        excludeStatus: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    ): Int

    @Query("SELECT * FROM Workout WHERE programmeId = :programmeId AND status = 'COMPLETED' ORDER BY weekNumber, dayNumber")
    suspend fun getCompletedWorkoutsByProgramme(programmeId: Long): List<Workout>
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
