package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.LocalDateTime

/**
 * DAO for user exercise usage tracking.
 */
@Dao
interface UserExerciseUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UserExerciseUsage)

    @Update
    suspend fun updateUsage(usage: UserExerciseUsage)

    @Query(
        """
        SELECT * FROM user_exercise_usage
        WHERE userId = :userId AND exerciseId = :exerciseId
        LIMIT 1
    """,
    )
    suspend fun getUsage(
        userId: String,
        exerciseId: String,
    ): UserExerciseUsage?

    @Query(
        """
        SELECT * FROM user_exercise_usage
        WHERE userId = :userId
        ORDER BY usageCount DESC, lastUsedAt DESC
    """,
    )
    suspend fun getAllUsageForUser(userId: String): List<UserExerciseUsage>

    @Query(
        """
        UPDATE user_exercise_usage
        SET usageCount = usageCount + 1, lastUsedAt = :timestamp, updatedAt = :timestamp
        WHERE userId = :userId AND exerciseId = :exerciseId
    """,
    )
    suspend fun incrementUsageCount(
        userId: String,
        exerciseId: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
    )

    @Query(
        """
        UPDATE user_exercise_usage
        SET usageCount = MAX(0, usageCount - 1), updatedAt = :timestamp
        WHERE userId = :userId AND exerciseId = :exerciseId
    """,
    )
    suspend fun decrementUsageCount(
        userId: String,
        exerciseId: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
    )

    @Query("DELETE FROM user_exercise_usage WHERE userId = :userId")
    suspend fun deleteAllUsageForUser(userId: String)

    @Query("DELETE FROM user_exercise_usage WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun deleteUsage(
        userId: String,
        exerciseId: String,
    )

    // Get or create usage record
    @Transaction
    suspend fun getOrCreateUsage(
        userId: String,
        exerciseId: String,
    ): UserExerciseUsage {
        val existing = getUsage(userId, exerciseId)
        return if (existing != null) {
            existing
        } else {
            val newUsage =
                UserExerciseUsage(
                    userId = userId,
                    exerciseId = exerciseId,
                )
            insertUsage(newUsage)
            newUsage
        }
    }

    // Most used exercises for recommendations
    @Query(
        """
        SELECT * FROM user_exercise_usage
        WHERE userId = :userId AND usageCount > 0
        ORDER BY usageCount DESC
        LIMIT :limit
    """,
    )
    suspend fun getMostUsedExercises(
        userId: String,
        limit: Int = 20,
    ): List<UserExerciseUsage>

    // Recently used exercises
    @Query(
        """
        SELECT * FROM user_exercise_usage
        WHERE userId = :userId AND lastUsedAt IS NOT NULL
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """,
    )
    suspend fun getRecentlyUsedExercises(
        userId: String,
        limit: Int = 10,
    ): List<UserExerciseUsage>
}
