package com.github.radupana.featherweight.data.profile

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ProfileDao {
    // User Profile operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile): Long

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getUserProfile(userId: Long): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    fun getUserProfileFlow(userId: Long): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles ORDER BY displayName ASC")
    suspend fun getAllUsers(): List<UserProfile>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    // User Exercise Max operations
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
        SELECT uem.* FROM user_exercise_maxes uem
        WHERE uem.userId = :userId AND uem.exerciseId = :exerciseId
        ORDER BY uem.recordedAt DESC
        LIMIT 1
    """,
    )
    suspend fun getCurrentMax(
        userId: Long,
        exerciseId: Long,
    ): UserExerciseMax?

    @Query(
        """
        SELECT uem.* FROM user_exercise_maxes uem
        WHERE uem.userId = :userId AND uem.exerciseId = :exerciseId
        ORDER BY uem.recordedAt DESC
        LIMIT 1
    """,
    )
    fun getCurrentMaxFlow(
        userId: Long,
        exerciseId: Long,
    ): Flow<UserExerciseMax?>

    @Query(
        """
        SELECT uem.*, e.name as exerciseName FROM user_exercise_maxes uem
        INNER JOIN exercises e ON e.id = uem.exerciseId
        WHERE uem.userId = :userId
        AND uem.id IN (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId
            GROUP BY exerciseId
        )
        ORDER BY 
            CASE e.name
                WHEN 'Barbell Back Squat' THEN 1
                WHEN 'Barbell Deadlift' THEN 2
                WHEN 'Barbell Bench Press' THEN 3
                WHEN 'Barbell Overhead Press' THEN 4
                ELSE 5
            END,
            e.name ASC
    """,
    )
    fun getAllCurrentMaxes(userId: Long): Flow<List<ExerciseMaxWithName>>

    @Query(
        """
        SELECT uem.* FROM user_exercise_maxes uem
        WHERE uem.userId = :userId AND uem.exerciseId = :exerciseId
        ORDER BY uem.recordedAt DESC
    """,
    )
    fun getMaxHistory(
        userId: Long,
        exerciseId: Long,
    ): Flow<List<UserExerciseMax>>

    @Query(
        """
        SELECT e.* FROM exercises e
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
    suspend fun getBig4Exercises(): List<com.github.radupana.featherweight.data.exercise.Exercise>

    @Query(
        """
        UPDATE user_exercise_maxes
        SET maxWeight = :newMax, recordedAt = :recordedAt, isEstimated = :isEstimated
        WHERE userId = :userId AND exerciseId = :exerciseId
        AND id = (
            SELECT MAX(id) FROM user_exercise_maxes
            WHERE userId = :userId AND exerciseId = :exerciseId
        )
    """,
    )
    suspend fun updateCurrentMax(
        userId: Long,
        exerciseId: Long,
        newMax: Float,
        recordedAt: LocalDateTime = LocalDateTime.now(),
        isEstimated: Boolean = false,
    )

    @Transaction
    suspend fun upsertExerciseMax(
        userId: Long,
        exerciseId: Long,
        maxWeight: Float,
        isEstimated: Boolean = false,
        notes: String? = null,
    ) {
        val currentMax = getCurrentMax(userId, exerciseId)
        if (currentMax != null && currentMax.maxWeight == maxWeight) {
            // Same weight, just update the date
            updateExerciseMax(currentMax.copy(recordedAt = LocalDateTime.now()))
        } else {
            // New max, insert new record
            insertExerciseMax(
                UserExerciseMax(
                    userId = userId,
                    exerciseId = exerciseId,
                    maxWeight = maxWeight,
                    recordedAt = LocalDateTime.now(),
                    isEstimated = isEstimated,
                    notes = notes,
                ),
            )
        }
    }
}

data class ExerciseMaxWithName(
    val id: Long,
    val userId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeight: Float,
    val recordedAt: LocalDateTime,
    val notes: String?,
    val isEstimated: Boolean,
)
