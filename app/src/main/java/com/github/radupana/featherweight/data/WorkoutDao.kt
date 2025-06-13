package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

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

    @Query("DELETE FROM Workout WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    // Programme-related queries
    @Query("SELECT * FROM Workout WHERE programmeId = :programmeId ORDER BY date DESC")
    suspend fun getWorkoutsByProgramme(programmeId: Long): List<Workout>

    @Query("SELECT * FROM Workout WHERE programmeId = :programmeId AND weekNumber = :weekNumber ORDER BY dayNumber ASC")
    suspend fun getProgrammeWorkoutsByWeek(
        programmeId: Long,
        weekNumber: Int,
    ): List<Workout>

    @Query("SELECT * FROM Workout WHERE isProgrammeWorkout = 0 ORDER BY date DESC")
    suspend fun getFreestyleWorkouts(): List<Workout>

    @Query("SELECT * FROM Workout WHERE isProgrammeWorkout = 1 ORDER BY date DESC")
    suspend fun getProgrammeWorkouts(): List<Workout>

    @Query(
        """
        SELECT COALESCE(pp.completedWorkouts, 0) 
        FROM programme_progress pp 
        WHERE pp.programmeId = :programmeId
    """,
    )
    suspend fun getCompletedProgrammeWorkoutCount(programmeId: Long): Int

    @Query(
        """
        SELECT COALESCE(pp.totalWorkouts, 0) 
        FROM programme_progress pp 
        WHERE pp.programmeId = :programmeId
    """,
    )
    suspend fun getTotalProgrammeWorkoutCount(programmeId: Long): Int

    @Query(
        """
        SELECT * FROM Workout 
        WHERE programmeId = :programmeId 
        AND programmeWorkoutId = :programmeWorkoutId 
        ORDER BY date DESC 
        LIMIT 1
    """,
    )
    suspend fun getLastProgrammeWorkoutExecution(
        programmeId: Long,
        programmeWorkoutId: Long,
    ): Workout?

    @Query(
        """
        SELECT DISTINCT weekNumber FROM Workout 
        WHERE programmeId = :programmeId 
        AND weekNumber IS NOT NULL 
        ORDER BY weekNumber ASC
    """,
    )
    suspend fun getCompletedWeeksForProgramme(programmeId: Long): List<Int>
}
