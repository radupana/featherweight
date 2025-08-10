package com.github.radupana.featherweight.data.profile

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface OneRMDao {
    @Insert
    suspend fun insertExerciseMax(max: UserExerciseMax): Long

    @Update
    suspend fun updateExerciseMax(max: UserExerciseMax)

    @Delete
    suspend fun deleteExerciseMax(max: UserExerciseMax)

    @Query("DELETE FROM user_exercise_maxes WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun deleteAllMaxesForExercise(
        userId: Long,
        exerciseId: Long,
    )

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE userId = :userId AND exerciseId = :exerciseId
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentMax(
        userId: Long,
        exerciseId: Long,
    ): UserExerciseMax?

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE userId = :userId AND exerciseId = :exerciseId
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    fun getCurrentMaxFlow(
        userId: Long,
        exerciseId: Long,
    ): Flow<UserExerciseMax?>

    @Query(
        """
        SELECT oneRMEstimate FROM user_exercise_maxes
        WHERE userId = :userId AND exerciseId = :exerciseId
        AND oneRMEstimate > 0
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentOneRMEstimate(
        userId: Long,
        exerciseId: Long,
    ): Float?

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE userId = :userId AND exerciseId IN (:exerciseIds)
        AND id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId AND exerciseId IN (:exerciseIds)
            GROUP BY exerciseId
        )
    """,
    )
    suspend fun getCurrentMaxesForExercises(
        userId: Long,
        exerciseIds: List<Long>,
    ): List<UserExerciseMax>

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE userId = :userId AND exerciseId = :exerciseId
        ORDER BY oneRMDate DESC
    """,
    )
    fun getMaxHistory(
        userId: Long,
        exerciseId: Long,
    ): Flow<List<UserExerciseMax>>

    @Transaction
    suspend fun upsertExerciseMax(
        userId: Long,
        exerciseId: Long,
        maxWeight: Float,
        notes: String? = null,
    ) {
        val currentMax = getCurrentMax(userId, exerciseId)
        if (currentMax != null && currentMax.oneRMEstimate == maxWeight) {
            // Same weight, just update the date
            updateExerciseMax(currentMax.copy(oneRMDate = LocalDateTime.now()))
        } else {
            // New max, insert new record
            insertExerciseMax(
                UserExerciseMax(
                    userId = userId,
                    exerciseId = exerciseId,
                    mostWeightLifted = maxWeight,
                    mostWeightReps = 1,
                    mostWeightRpe = null,
                    mostWeightDate = LocalDateTime.now(),
                    oneRMEstimate = maxWeight,
                    oneRMContext = "${maxWeight}kg × 1",
                    oneRMConfidence = 1.0f,
                    oneRMDate = LocalDateTime.now(),
                    notes = notes,
                ),
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExerciseMax(max: UserExerciseMax): Long

    @Query(
        """
        SELECT 
            uem.id,
            uem.userId,
            uem.exerciseId,
            e.name as exerciseName,
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
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseName = e.name
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercises e ON e.id = uem.exerciseId
        WHERE uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseId
        )
        ORDER BY e.name ASC
    """,
    )
    fun getAllCurrentMaxesWithNames(userId: Long): Flow<List<OneRMWithExerciseName>>

    @Query(
        """
        SELECT 
            COALESCE(uem.id, 0) as id,
            :userId as userId,
            e.id as exerciseId,
            e.name as exerciseName,
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
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseName = e.name
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM exercises e
        LEFT JOIN (
            SELECT * FROM user_exercise_maxes
            WHERE userId = :userId
            AND id IN (
                SELECT MAX(id) FROM user_exercise_maxes
                WHERE userId = :userId
                GROUP BY exerciseId
            )
        ) uem ON e.id = uem.exerciseId
        WHERE e.name IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        ORDER BY 
            CASE e.name
                WHEN 'Barbell Back Squat' THEN 1
                WHEN 'Barbell Deadlift' THEN 2
                WHEN 'Barbell Bench Press' THEN 3
                WHEN 'Barbell Overhead Press' THEN 4
            END
    """,
    )
    fun getBig4ExercisesWithMaxes(userId: Long): Flow<List<Big4ExerciseWithOptionalMax>>

    @Query(
        """
        SELECT 
            uem.id,
            uem.userId,
            uem.exerciseId,
            e.name as exerciseName,
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
            e.usageCount,
            (
                SELECT COUNT(DISTINCT w.id) 
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseName = e.name
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercises e ON e.id = uem.exerciseId
        WHERE uem.userId = :userId
        AND e.name NOT IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseId
        )
        ORDER BY e.usageCount DESC, e.name ASC
    """,
    )
    fun getOtherExercisesWithMaxes(userId: Long): Flow<List<OneRMWithExerciseName>>

    // OneRM History methods
    @Insert
    suspend fun insertOneRMHistory(history: OneRMHistory): Long

    @Query(
        """
        SELECT * FROM one_rm_history
        WHERE exerciseId = :exerciseId
        AND recordedAt >= :startDate
        AND recordedAt <= :endDate
        ORDER BY recordedAt ASC
        """,
    )
    suspend fun getOneRMHistoryInRange(
        exerciseId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<OneRMHistory>

    @Query(
        """
        SELECT * FROM one_rm_history
        WHERE userId = :userId
        AND exerciseId = :exerciseId
        ORDER BY recordedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentOneRMHistory(
        userId: Long,
        exerciseId: Long,
        limit: Int = 10,
    ): List<OneRMHistory>

    @Query("DELETE FROM user_exercise_maxes WHERE userId = :userId")
    suspend fun deleteAllUserExerciseMaxes(userId: Long)

    @Query("DELETE FROM one_rm_history WHERE userId = :userId")
    suspend fun deleteAllOneRMHistory(userId: Long)

    // Export-related queries
    @Query(
        """
        SELECT 
            h.id,
            h.userId,
            h.exerciseId,
            h.exerciseName,
            h.oneRMEstimate,
            h.context,
            h.recordedAt
        FROM one_rm_history h
        WHERE h.recordedAt >= :startDate
        AND h.recordedAt <= :endDate
        ORDER BY h.recordedAt DESC
        """,
    )
    suspend fun getAllOneRMHistoryInRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<OneRMHistoryWithName>

    @Query(
        """
        SELECT 
            uem.id,
            uem.userId,
            uem.exerciseId,
            e.name as exerciseName,
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
        INNER JOIN exercises e ON e.id = uem.exerciseId
        WHERE uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseId
        )
        ORDER BY e.name ASC
        """,
    )
    suspend fun getAllCurrentMaxesForExport(userId: Long): List<UserExerciseMaxWithName>
}

data class OneRMWithExerciseName(
    val id: Long,
    val userId: Long,
    val exerciseId: Long,
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
    val id: Long,
    val userId: Long,
    val exerciseId: Long,
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
    val id: Long,
    val userId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val context: String,
    val recordedAt: LocalDateTime,
)

data class UserExerciseMaxWithName(
    val id: Long,
    val userId: Long,
    val exerciseId: Long,
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
