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

    @Query("DELETE FROM user_exercise_maxes WHERE exerciseVariationId = :exerciseVariationId")
    suspend fun deleteAllMaxesForExercise(
        exerciseVariationId: Long,
    )

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentMax(
        exerciseVariationId: Long,
    ): UserExerciseMax?

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    fun getCurrentMaxFlow(
        exerciseVariationId: Long,
    ): Flow<UserExerciseMax?>

    @Query(
        """
        SELECT oneRMEstimate FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        AND oneRMEstimate > 0
        ORDER BY oneRMDate DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentOneRMEstimate(
        exerciseVariationId: Long,
    ): Float?

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId IN (:exerciseVariationIds)
        AND id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE exerciseVariationId IN (:exerciseVariationIds)
            GROUP BY exerciseVariationId
        )
    """,
    )
    suspend fun getCurrentMaxesForExercises(
        exerciseVariationIds: List<Long>,
    ): List<UserExerciseMax>

    @Query(
        """
        SELECT * FROM user_exercise_maxes
        WHERE exerciseVariationId = :exerciseVariationId
        ORDER BY oneRMDate DESC
    """,
    )
    fun getMaxHistory(
        exerciseVariationId: Long,
    ): Flow<List<UserExerciseMax>>

    @Transaction
    suspend fun upsertExerciseMax(
        exerciseVariationId: Long,
        maxWeight: Float,
        notes: String? = null,
    ) {
        val currentMax = getCurrentMax(exerciseVariationId)
        if (currentMax != null && currentMax.oneRMEstimate == maxWeight) {
            // Same weight, just update the date
            updateExerciseMax(currentMax.copy(oneRMDate = LocalDateTime.now()))
        } else {
            // New max, insert new record
            insertExerciseMax(
                UserExerciseMax(
                    exerciseVariationId = exerciseVariationId,
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
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercise_variations ev ON ev.id = uem.exerciseVariationId
        WHERE uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.name ASC
    """,
    )
    fun getAllCurrentMaxesWithNames(): Flow<List<OneRMWithExerciseName>>

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
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM exercise_variations ev
        LEFT JOIN (
            SELECT * FROM user_exercise_maxes
            WHERE id IN (
                SELECT MAX(id) FROM user_exercise_maxes
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
    fun getBig4ExercisesWithMaxes(): Flow<List<Big4ExerciseWithOptionalMax>>

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
            ev.usageCount,
            (
                SELECT COUNT(DISTINCT w.id) 
                FROM ExerciseLog el
                INNER JOIN Workout w ON el.workoutId = w.id
                WHERE el.exerciseVariationId = ev.id
                AND w.status = 'COMPLETED'
            ) as sessionCount
        FROM user_exercise_maxes uem
        INNER JOIN exercise_variations ev ON ev.id = uem.exerciseVariationId
        WHERE ev.name NOT IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.usageCount DESC, ev.name ASC
    """,
    )
    fun getOtherExercisesWithMaxes(): Flow<List<OneRMWithExerciseName>>

    // OneRM History methods
    @Insert
    suspend fun insertOneRMHistory(history: OneRMHistory): Long

    @Query(
        """
        SELECT * FROM one_rm_history
        WHERE exerciseVariationId = :exerciseVariationId
        AND recordedAt >= :startDate
        AND recordedAt <= :endDate
        ORDER BY recordedAt ASC
        """,
    )
    suspend fun getOneRMHistoryInRange(
        exerciseVariationId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<OneRMHistory>

    @Query(
        """
        SELECT * FROM one_rm_history
        WHERE exerciseVariationId = :exerciseVariationId
        ORDER BY recordedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentOneRMHistory(
        exerciseVariationId: Long,
        limit: Int = 10,
    ): List<OneRMHistory>

    @Query("DELETE FROM user_exercise_maxes")
    suspend fun deleteAllUserExerciseMaxes()

    @Query("DELETE FROM one_rm_history")
    suspend fun deleteAllOneRMHistory()

    // Export-related queries
    @Query(
        """
        SELECT 
            h.id,
            h.exerciseVariationId,
            ev.name as exerciseName,
            h.oneRMEstimate,
            h.context,
            h.recordedAt
        FROM one_rm_history h
        INNER JOIN exercise_variations ev ON ev.id = h.exerciseVariationId
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
        WHERE uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            GROUP BY exerciseVariationId
        )
        ORDER BY ev.name ASC
        """,
    )
    suspend fun getAllCurrentMaxesForExport(): List<UserExerciseMaxWithName>
}

data class OneRMWithExerciseName(
    val id: Long,
    val exerciseVariationId: Long,
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
    val exerciseVariationId: Long,
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
    val exerciseVariationId: Long,
    val exerciseName: String,
    val oneRMEstimate: Float,
    val context: String,
    val recordedAt: LocalDateTime,
)

data class UserExerciseMaxWithName(
    val id: Long,
    val exerciseVariationId: Long,
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
