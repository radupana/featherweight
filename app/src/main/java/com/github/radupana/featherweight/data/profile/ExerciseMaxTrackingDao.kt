// Suppression justified: Room DAO interfaces inherently require many query methods to support
// the various data access patterns needed by the application. Splitting into multiple DAOs
// would create artificial boundaries and complicate transaction management.
@file:Suppress("TooManyFunctions")

package com.github.radupana.featherweight.data.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExerciseMaxTrackingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracking: ExerciseMaxTracking)

    @Insert
    suspend fun insertTracking(tracking: ExerciseMaxTracking)

    @Update
    suspend fun update(tracking: ExerciseMaxTracking)

    /**
     * Get the most recent (current) 1RM for an exercise.
     * Returns null if no records exist.
     */
    @Query(
        """
        SELECT * FROM exercise_max_history
        WHERE exerciseId = :exerciseId
        AND userId = :userId
        ORDER BY recordedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getCurrentMax(
        exerciseId: String,
        userId: String,
    ): ExerciseMaxTracking?

    /**
     * Get just the 1RM estimate value for an exercise.
     */
    @Query(
        """
        SELECT oneRMEstimate FROM exercise_max_history
        WHERE exerciseId = :exerciseId
        AND userId = :userId
        AND oneRMEstimate > 0
        ORDER BY recordedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getCurrentOneRMEstimate(
        exerciseId: String,
        userId: String,
    ): Float?

    /**
     * Get current maxes for multiple exercises.
     */
    @Query(
        """
        SELECT * FROM exercise_max_history
        WHERE exerciseId IN (:exerciseIds)
        AND userId = :userId
        AND id IN (
            SELECT id FROM exercise_max_history
            WHERE exerciseId IN (:exerciseIds)
            AND userId = :userId
            GROUP BY exerciseId
            HAVING recordedAt = MAX(recordedAt)
        )
        """,
    )
    suspend fun getCurrentMaxesForExercises(
        exerciseIds: List<String>,
        userId: String,
    ): List<ExerciseMaxTracking>

    /**
     * Get all current maxes with exercise names for display.
     */
    @Query(
        """
        SELECT
            t.id,
            t.exerciseId,
            ev.name as exerciseName,
            t.oneRMEstimate,
            t.recordedAt as oneRMDate,
            t.context as oneRMContext,
            t.mostWeightLifted,
            t.mostWeightReps,
            t.mostWeightRpe,
            t.mostWeightDate,
            t.oneRMConfidence,
            t.oneRMType,
            t.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM exercise_max_history t
        INNER JOIN exercises ev ON ev.id = t.exerciseId
        WHERE t.userId = :userId
        AND t.id IN (
            SELECT id FROM exercise_max_history
            WHERE userId = :userId
            GROUP BY exerciseId
            HAVING recordedAt = MAX(recordedAt)
        )
        ORDER BY ev.name ASC
        """,
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getAllCurrentMaxesWithNames(userId: String): Flow<List<OneRMWithExerciseName>>

    /**
     * Get Big 4 exercises with their maxes (if they exist).
     */
    @Query(
        """
        SELECT
            COALESCE(t.id, '') as id,
            ev.id as exerciseId,
            ev.name as exerciseName,
            t.oneRMEstimate,
            t.recordedAt as oneRMDate,
            t.context as oneRMContext,
            t.mostWeightLifted,
            t.mostWeightReps,
            t.mostWeightRpe,
            t.mostWeightDate,
            t.oneRMConfidence,
            t.oneRMType,
            t.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM exercises ev
        LEFT JOIN (
            SELECT * FROM exercise_max_history
            WHERE userId = :userId
            AND id IN (
                SELECT id FROM exercise_max_history
                WHERE userId = :userId
                GROUP BY exerciseId
                HAVING recordedAt = MAX(recordedAt)
            )
        ) t ON ev.id = t.exerciseId
        WHERE ev.name IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        ORDER BY
            CASE ev.name
                WHEN 'Barbell Back Squat' THEN 1
                WHEN 'Barbell Deadlift' THEN 2
                WHEN 'Barbell Bench Press' THEN 3
                WHEN 'Barbell Overhead Press' THEN 4
            END
        """,
    )
    fun getBig4ExercisesWithMaxes(userId: String): Flow<List<Big4ExerciseWithOptionalMax>>

    /**
     * Get all exercises except Big 4 with their maxes.
     */
    @Query(
        """
        SELECT
            t.id,
            t.exerciseId,
            ev.name as exerciseName,
            t.oneRMEstimate,
            t.recordedAt as oneRMDate,
            t.context as oneRMContext,
            t.mostWeightLifted,
            t.mostWeightReps,
            t.mostWeightRpe,
            t.mostWeightDate,
            t.oneRMConfidence,
            t.oneRMType,
            t.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM exercise_max_history t
        INNER JOIN exercises ev ON ev.id = t.exerciseId
        WHERE ev.name NOT IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        AND t.userId = :userId
        AND t.id IN (
            SELECT id FROM exercise_max_history
            WHERE userId = :userId
            GROUP BY exerciseId
            HAVING recordedAt = MAX(recordedAt)
        )
        ORDER BY ev.name ASC
        """,
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getOtherExercisesWithMaxes(userId: String): Flow<List<OneRMWithExerciseName>>

    /**
     * Get 1RM history within a date range for an exercise.
     */
    @Query(
        """
        SELECT * FROM exercise_max_history
        WHERE exerciseId = :exerciseId
        AND userId = :userId
        AND recordedAt >= :startDate
        AND recordedAt <= :endDate
        ORDER BY recordedAt ASC
        """,
    )
    suspend fun getHistoryInRange(
        exerciseId: String,
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<ExerciseMaxTracking>

    /**
     * Get all records for a specific exercise (for Firestore sync before deletion).
     */
    @Query("SELECT * FROM exercise_max_history WHERE exerciseId = :exerciseId AND userId = :userId")
    suspend fun getAllMaxesForExercise(
        exerciseId: String,
        userId: String,
    ): List<ExerciseMaxTracking>

    /**
     * Delete all records for a specific exercise.
     */
    @Query("DELETE FROM exercise_max_history WHERE exerciseId = :exerciseId AND userId = :userId")
    suspend fun deleteAllForExercise(
        exerciseId: String,
        userId: String,
    )

    /**
     * Delete all records for a user.
     */
    @Query("DELETE FROM exercise_max_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /**
     * Delete all records where userId is null.
     */
    @Query("DELETE FROM exercise_max_history WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    /**
     * Get all records for a user.
     */
    @Query("SELECT * FROM exercise_max_history WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<ExerciseMaxTracking>

    /**
     * Get all records (for data migration/fixes).
     */
    @Query("SELECT * FROM exercise_max_history")
    suspend fun getAll(): List<ExerciseMaxTracking>

    /**
     * Get record by ID.
     */
    @Query("SELECT * FROM exercise_max_history WHERE id = :id")
    suspend fun getById(id: String): ExerciseMaxTracking?

    /**
     * Delete all records (for testing/data cleanup).
     */
    @Query("DELETE FROM exercise_max_history")
    suspend fun deleteAll()

    /**
     * Get all current maxes for export.
     */
    @Query(
        """
        SELECT
            t.id,
            t.exerciseId,
            ev.name as exerciseName,
            t.mostWeightLifted,
            t.mostWeightReps,
            t.mostWeightRpe,
            t.mostWeightDate,
            t.oneRMEstimate,
            t.context as oneRMContext,
            t.oneRMConfidence,
            t.recordedAt as oneRMDate,
            t.oneRMType,
            t.notes
        FROM exercise_max_history t
        INNER JOIN exercises ev ON ev.id = t.exerciseId
        WHERE t.userId = :userId
        AND t.id IN (
            SELECT id FROM exercise_max_history
            WHERE userId = :userId
            GROUP BY exerciseId
            HAVING recordedAt = MAX(recordedAt)
        )
        ORDER BY ev.name ASC
        """,
    )
    suspend fun getAllCurrentMaxesForExport(userId: String): List<UserExerciseMaxWithName>

    /**
     * Upsert a 1RM record: update if exists (by sourceSetId), insert otherwise.
     */
    @Suppress("LongParameterList")
    @Transaction
    suspend fun upsert(
        exerciseId: String,
        userId: String,
        oneRMEstimate: Float,
        context: String,
        sourceSetId: String?,
        recordedAt: LocalDateTime,
        mostWeightLifted: Float,
        mostWeightReps: Int,
        mostWeightRpe: Float?,
        mostWeightDate: LocalDateTime,
        oneRMConfidence: Float,
        oneRMType: OneRMType,
        notes: String?,
    ) {
        val existing = getCurrentMax(exerciseId, userId)
        if (existing != null && existing.oneRMEstimate == oneRMEstimate) {
            // Same weight, just update the date
            update(existing.copy(recordedAt = recordedAt))
        } else {
            // New max, insert new record
            insert(
                ExerciseMaxTracking(
                    userId = userId,
                    exerciseId = exerciseId,
                    oneRMEstimate = oneRMEstimate,
                    context = context,
                    sourceSetId = sourceSetId,
                    recordedAt = recordedAt,
                    mostWeightLifted = mostWeightLifted,
                    mostWeightReps = mostWeightReps,
                    mostWeightRpe = mostWeightRpe,
                    mostWeightDate = mostWeightDate,
                    oneRMConfidence = oneRMConfidence,
                    oneRMType = oneRMType,
                    notes = notes,
                ),
            )
        }
    }

    @Query("DELETE FROM exercise_max_history WHERE sourceSetId = :setId")
    suspend fun deleteBySourceSetId(setId: String): Int

    @Query("SELECT * FROM exercise_max_history WHERE sourceSetId = :setId LIMIT 1")
    suspend fun getBySourceSetId(setId: String): ExerciseMaxTracking?

    @Query(
        """
        SELECT * FROM exercise_max_history
        WHERE exerciseId = :exerciseId AND userId = :userId AND sourceSetId != :excludeSetId
        ORDER BY recordedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getPreviousMaxExcludingSet(
        exerciseId: String,
        userId: String,
        excludeSetId: String,
    ): ExerciseMaxTracking?

    @Query(
        """
        UPDATE exercise_max_history
        SET oneRMEstimate = ROUND(oneRMEstimate * 4) / 4
        """,
    )
    suspend fun roundAllEstimates()

    /**
     * Remove duplicate entries based on userId, exerciseId, rounded estimate, and time.
     */
    @Query(
        """
        DELETE FROM exercise_max_history
        WHERE id NOT IN (
            SELECT MIN(id)
            FROM exercise_max_history
            GROUP BY
                userId,
                exerciseId,
                ROUND(oneRMEstimate * 4) / 4,
                strftime('%Y-%m-%d %H:%M', recordedAt)
        )
        """,
    )
    suspend fun removeDuplicates()
}
