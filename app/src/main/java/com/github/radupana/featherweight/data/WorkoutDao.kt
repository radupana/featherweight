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

    @Query("SELECT * FROM Workout WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getWorkoutsInDateRange(
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime,
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
        SELECT DISTINCT weekNumber FROM Workout 
        WHERE programmeId = :programmeId 
        AND weekNumber IS NOT NULL 
        ORDER BY weekNumber ASC
    """,
    )
    suspend fun getCompletedWeeksForProgramme(programmeId: Long): List<Int>

    // ===== INTELLIGENT SUGGESTIONS QUERIES =====

    @Query("SELECT * FROM ExerciseLog WHERE exerciseName = :exerciseName ORDER BY id DESC")
    suspend fun getExerciseLogsByName(exerciseName: String): List<ExerciseLog>

    @Query(
        """
        SELECT s.* FROM SetLog s 
        WHERE s.exerciseLogId IN (:exerciseIds) 
        AND s.actualReps BETWEEN :minReps AND :maxReps 
        AND s.isCompleted = 1
        ORDER BY s.id DESC 
        LIMIT 50
    """,
    )
    suspend fun getSetsForExercisesInRepRange(
        exerciseIds: List<Long>,
        minReps: Int,
        maxReps: Int,
    ): List<SetLog>

    @Query(
        """
        SELECT s.* FROM SetLog s 
        WHERE s.exerciseLogId IN (:exerciseIds) 
        AND s.isCompleted = 1
        ORDER BY s.id DESC 
        LIMIT :limit
    """,
    )
    suspend fun getRecentSetsForExercises(
        exerciseIds: List<Long>,
        limit: Int,
    ): List<SetLog>

    @Query(
        """
        SELECT s.* FROM SetLog s 
        WHERE s.exerciseLogId IN (:exerciseIds) 
        AND s.actualWeight BETWEEN :minWeight AND :maxWeight 
        AND s.isCompleted = 1
        ORDER BY s.id DESC 
        LIMIT 50
    """,
    )
    suspend fun getSetsForExercisesInWeightRange(
        exerciseIds: List<Long>,
        minWeight: Float,
        maxWeight: Float,
    ): List<SetLog>

    @Query("DELETE FROM Workout")
    suspend fun deleteAllWorkouts()

    // Paginated query for completed workouts
    @Query(
        """
        SELECT * FROM Workout 
        WHERE status = 'COMPLETED' 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getCompletedWorkoutsPaged(
        limit: Int,
        offset: Int,
    ): List<Workout>

    // Get the workout date when a specific weight was achieved for an exercise
    @Query(
        """
        SELECT w.date FROM Workout w
        INNER JOIN ExerciseLog el ON el.workoutId = w.id
        INNER JOIN SetLog sl ON sl.exerciseLogId = el.id
        WHERE el.exerciseName = :exerciseName
        AND sl.actualWeight = :weight
        AND sl.isCompleted = 1
        AND w.date BETWEEN :startDateTime AND :endDateTime
        ORDER BY w.date DESC
        LIMIT 1
    """,
    )
    suspend fun getWorkoutDateForMaxWeight(
        exerciseName: String,
        weight: Float,
        startDateTime: java.time.LocalDateTime,
        endDateTime: java.time.LocalDateTime,
    ): java.time.LocalDateTime?
}
