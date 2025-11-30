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

    @Query(
        """
        SELECT DISTINCT programmeId FROM workout_deviations
        ORDER BY timestamp DESC
        """,
    )
    suspend fun getProgrammeIdsWithDeviations(): List<String>

    /**
     * Returns the programme ID with the most recent deviation activity.
     *
     * Selection logic:
     * - Only considers programmes that have at least one recorded deviation
     * - Orders by most recent deviation timestamp (MAX(wd.timestamp))
     * - Returns the programme the user has been most recently working on
     *
     * Rationale:
     * - A programme with recent deviation activity is most relevant for AI analysis
     * - User may have started a new programme but not recorded any workouts yet
     * - Using deviation timestamp ensures we analyze the programme with actual data
     *
     * Edge cases:
     * - If user has multiple programmes with deviations, returns the one with most recent activity
     * - Returns null if no programmes have any deviation data
     *
     * This is used for AI training analysis to provide adherence context based on the user's
     * most relevant programme history.
     */
    @Query(
        """
        SELECT programmeId FROM workout_deviations
        GROUP BY programmeId
        ORDER BY MAX(timestamp) DESC
        LIMIT 1
        """,
    )
    suspend fun getMostRecentProgrammeWithDeviations(): String?

    /**
     * Returns all deviations for the programme with the most recent deviation activity.
     *
     * This is an optimized single-query alternative to calling:
     * 1. getMostRecentProgrammeWithDeviations() to get the programme ID
     * 2. getDeviationsForProgramme(programmeId) to get the deviations
     *
     * The subquery identifies the most recently active programme (by MAX deviation timestamp),
     * then the outer query fetches all deviations for that programme.
     *
     * Returns empty list if no programmes have any deviation data.
     *
     * Used by InsightsViewModel to build the AI training analysis payload efficiently.
     */
    @Query(
        """
        SELECT * FROM workout_deviations
        WHERE programmeId = (
            SELECT programmeId FROM workout_deviations
            GROUP BY programmeId
            ORDER BY MAX(timestamp) DESC
            LIMIT 1
        )
        """,
    )
    suspend fun getDeviationsForMostRecentProgramme(): List<WorkoutDeviation>
}
