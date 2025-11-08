package com.github.radupana.featherweight.data.programme

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDeviationDao {
    @Insert
    suspend fun insert(deviation: WorkoutDeviation)

    @Insert
    suspend fun insertAll(deviations: List<WorkoutDeviation>)

    @Query("SELECT * FROM workout_deviations WHERE workoutId = :workoutId")
    suspend fun getDeviationsForWorkout(workoutId: String): List<WorkoutDeviation>

    @Query("SELECT * FROM workout_deviations WHERE programmeId = :programmeId")
    suspend fun getDeviationsForProgramme(programmeId: String): List<WorkoutDeviation>

    @Query(
        """
        SELECT * FROM workout_deviations
        WHERE programmeId = :programmeId
        AND deviationType = :deviationType
        """,
    )
    suspend fun getDeviationsByType(
        programmeId: String,
        deviationType: DeviationType,
    ): List<WorkoutDeviation>

    @Query("SELECT * FROM workout_deviations WHERE workoutId = :workoutId")
    fun getDeviationsForWorkoutFlow(workoutId: String): Flow<List<WorkoutDeviation>>

    @Query("DELETE FROM workout_deviations WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: String)

    @Query("DELETE FROM workout_deviations WHERE programmeId = :programmeId")
    suspend fun deleteForProgramme(programmeId: String)

    @Query("DELETE FROM workout_deviations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM workout_deviations WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM workout_deviations")
    suspend fun deleteAll()
}
