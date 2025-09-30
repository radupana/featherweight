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
interface OneRMDao {
    @Insert
    suspend fun insertExerciseMax(max: UserExerciseMax)

    @Update
    suspend fun updateExerciseMax(max: UserExerciseMax)

    @Query("DELETE FROM user_exercise_maxes WHERE exerciseVariationId = :exerciseVariationId AND userId = :userId")
    suspend fun deleteAllMaxesForExercise(
        exerciseVariationId: String,
        userId: String,
    )

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        AND userId = :userId
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentMax(
        exerciseVariationId: String,
        userId: String,
    ): UserExerciseMax?

    @Query(
        """
        SELECT oneRMEstimate FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        AND userId = :userId
        AND oneRMEstimate > 0
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentOneRMEstimate(
        exerciseVariationId: String,
        userId: String,
    ): Float?

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId IN (:exerciseVariationIds)
        AND userId = :userId
        AND id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE exerciseVariationId IN (:exerciseVariationIds)
            AND userId = :userId
            GROUP BY exerciseVariationId
        )
    """,
    )
    suspend fun getCurrentMaxesForExercises(
        exerciseVariationIds: List<String>,
        userId: String,
    ): List<UserExerciseMax>

    @Transaction
    suspend fun upsertExerciseMax(
        exerciseVariationId: String,
        userId: String,
        maxWeight: Float,
        notes: String? = null,
    ) {
        val currentMax = getCurrentMax(exerciseVariationId, userId)
        if (currentMax != null && currentMax.oneRMEstimate == maxWeight) {
            // Same weight, just update the date
            updateExerciseMax(currentMax.copy(oneRMDate = LocalDateTime.now()))
        } else {
            // New max, insert new record
            insertExerciseMax(
                UserExerciseMax(
                    userId = userId,
                    exerciseVariationId = exerciseVariationId,
                    mostWeightLifted = maxWeight,
                    mostWeightReps = 1,
                    mostWeightRpe = null,
                    mostWeightDate = LocalDateTime.now(),
                    oneRMEstimate = maxWeight,
                    oneRMContext = "${com.github.radupana.featherweight.util.WeightFormatter.formatWeightWithUnit(maxWeight)} × 1",
                    oneRMConfidence = 1.0f,
                    oneRMDate = LocalDateTime.now(),
                    notes = notes,
                ),
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExerciseMax(max: UserExerciseMax)

    @Query(
        """
        SELECT
            uem.id,
            uem.exerciseVariationId,
            ev.name as exerciseName,
            uem.oneRMEstimate,
            uem.oneRMDate,
            uem.oneRMContext,
            uem.mostWeightLifted,
            uem.mostWeightReps,
            uem.mostWeightRpe,
            uem.mostWeightDate,
            uem.oneRMConfidence,
            uem.oneRMType,
            uem.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercise_variations ev ON ev.id = uem.exerciseVariationId
        WHERE uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.name ASC
    """,
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getAllCurrentMaxesWithNames(userId: String): Flow<List<OneRMWithExerciseName>>

    @Query(
        """
        SELECT
            COALESCE(uem.id, 0) as id,
            ev.id as exerciseVariationId,
            ev.name as exerciseName,
            uem.oneRMEstimate,
            uem.oneRMDate,
            uem.oneRMContext,
            uem.mostWeightLifted,
            uem.mostWeightReps,
            uem.mostWeightRpe,
            uem.mostWeightDate,
            uem.oneRMConfidence,
            uem.oneRMType,
            uem.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM exercise_variations ev
        LEFT JOIN (
            SELECT * FROM user_exercise_maxes
            WHERE userId = :userId
            AND id IN (
                SELECT MAX(id) FROM user_exercise_maxes
                WHERE userId = :userId
                GROUP BY exerciseVariationId
            )
        ) uem ON ev.id = uem.exerciseVariationId
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

    @Query(
        """
        SELECT
            uem.id,
            uem.exerciseVariationId,
            ev.name as exerciseName,
            uem.oneRMEstimate,
            uem.oneRMDate,
            uem.oneRMContext,
            uem.mostWeightLifted,
            uem.mostWeightReps,
            uem.mostWeightRpe,
            uem.mostWeightDate,
            uem.oneRMConfidence,
            uem.oneRMType,
            uem.notes,
            (
                SELECT COUNT(DISTINCT w.id)
                FROM exercise_logs el
                INNER JOIN workouts w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
                AND w.userId = :userId
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercise_variations ev ON ev.id = uem.exerciseVariationId
        WHERE ev.name NOT IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        AND uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.name ASC
    """,
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getOtherExercisesWithMaxes(userId: String): Flow<List<OneRMWithExerciseName>>

    // OneRM History methods
    @Insert
    suspend fun insertOneRMHistory(history: OneRMHistory)

    @Query(
        """
        SELECT * FROM one_rm_history
        WHERE exerciseVariationId = :exerciseVariationId
        AND userId = :userId
        AND recordedAt >= :startDate
        AND recordedAt <= :endDate
        ORDER BY recordedAt ASC
        """,
    )
    suspend fun getOneRMHistoryInRange(
        exerciseVariationId: String,
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<OneRMHistory>

    @Query("DELETE FROM user_exercise_maxes")
    suspend fun deleteAllUserExerciseMaxes()

    @Query("DELETE FROM one_rm_history")
    suspend fun deleteAllOneRMHistory()

    // Data cleanup queries for removing duplicates
    @Query(
        """
        DELETE FROM one_rm_history
        WHERE id NOT IN (
            SELECT MIN(id)
            FROM one_rm_history
            GROUP BY
                userId,
                exerciseVariationId,
                ROUND(oneRMEstimate * 4) / 4,
                strftime('%Y-%m-%d %H:%M', recordedAt)
        )
        """,
    )
    suspend fun removeDuplicateHistoryEntries()

    @Query(
        """
        UPDATE one_rm_history
        SET oneRMEstimate = ROUND(oneRMEstimate * 4) / 4
        """,
    )
    suspend fun roundAllHistoryValues()

    @Query(
        """
        UPDATE user_exercise_maxes
        SET oneRMEstimate = ROUND(oneRMEstimate * 4) / 4
        """,
    )
    suspend fun roundAllMaxValues()

    // Export-related queries

    @Query(
        """
        SELECT
            uem.id,
            uem.exerciseVariationId,
            ev.name as exerciseName,
            uem.mostWeightLifted,
            uem.mostWeightReps,
            uem.mostWeightRpe,
            uem.mostWeightDate,
            uem.oneRMEstimate,
            uem.oneRMContext,
            uem.oneRMConfidence,
            uem.oneRMDate,
            uem.oneRMType,
            uem.notes
        FROM user_exercise_maxes uem
        INNER JOIN exercise_variations ev ON ev.id = uem.exerciseVariationId
        WHERE uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.name ASC
        """,
    )
    suspend fun getAllCurrentMaxesForExport(userId: String): List<UserExerciseMaxWithName>

    @Query("SELECT * FROM user_exercise_maxes WHERE userId = :userId")
    suspend fun getAllUserExerciseMaxes(userId: String): List<UserExerciseMax>

    @Query("SELECT * FROM one_rm_history WHERE userId = :userId")
    suspend fun getAllOneRMHistory(userId: String): List<OneRMHistory>

    @Query("SELECT * FROM user_exercise_maxes WHERE id = :id")
    suspend fun getUserExerciseMaxById(id: String): UserExerciseMax?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserExerciseMax(max: UserExerciseMax)

    @Update
    suspend fun updateUserExerciseMax(max: UserExerciseMax)

    @Query("SELECT * FROM one_rm_history WHERE id = :id")
    suspend fun getOneRMHistoryById(id: String): OneRMHistory?

    @Query("DELETE FROM user_exercise_maxes WHERE userId = :userId")
    suspend fun deleteAllUserExerciseMaxesForUser(userId: String)

    @Query("DELETE FROM user_exercise_maxes WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM user_exercise_maxes WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM one_rm_history WHERE userId = :userId")
    suspend fun deleteAllOneRMHistoryForUser(userId: String)

    @Query("DELETE FROM one_rm_history WHERE userId = :userId")
    suspend fun deleteAllHistoryByUserId(userId: String)

    @Query("DELETE FROM one_rm_history WHERE userId IS NULL")
    suspend fun deleteAllHistoryWhereUserIdIsNull()

    @Transaction
    suspend fun deleteAllForUser(userId: String) {
        deleteAllUserExerciseMaxesForUser(userId)
        deleteAllOneRMHistoryForUser(userId)
    }
}

data class OneRMWithExerciseName(
    val id: String,
    val exerciseVariationId: String,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val oneRMDate: LocalDateTime,
    val oneRMContext: String,
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime,
    val oneRMConfidence: Float,
    val oneRMType: OneRMType,
    val notes: String?,
    val sessionCount: Int = 0,
)

data class Big4ExerciseWithOptionalMax(
    val id: String,
    val exerciseVariationId: String,
    val exerciseName: String,
    val oneRMEstimate: Float?,
    val oneRMDate: LocalDateTime?,
    val oneRMContext: String?,
    val mostWeightLifted: Float?,
    val mostWeightReps: Int?,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime?,
    val oneRMConfidence: Float?,
    val oneRMType: OneRMType?,
    val notes: String?,
    val sessionCount: Int = 0,
)

data class OneRMHistoryWithName(
    val id: String,
    val exerciseVariationId: String,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val context: String,
    val recordedAt: LocalDateTime,
)

data class UserExerciseMaxWithName(
    val id: String,
    val exerciseVariationId: String,
    val exerciseName: String,
    val mostWeightLifted: Float,
    val mostWeightReps: Int,
    val mostWeightRpe: Float?,
    val mostWeightDate: LocalDateTime,
    val oneRMEstimate: Float,
    val oneRMContext: String,
    val oneRMConfidence: Float,
    val oneRMDate: LocalDateTime,
    val oneRMType: OneRMType,
    val notes: String?,
)
