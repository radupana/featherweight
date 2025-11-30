package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExerciseSwapHistoryDao {
    @Insert
    suspend fun insert(swapHistory: ExerciseSwapHistory)

    @Query(
        """
        SELECT swappedToExerciseId, COUNT(*) as swapCount
        FROM exercise_swap_history
        WHERE originalExerciseId = :originalExerciseId
        GROUP BY swappedToExerciseId
        ORDER BY swapCount DESC
    """,
    )
    suspend fun getSwapHistoryForExercise(
        originalExerciseId: String,
    ): List<SwapHistoryCount>

    @Query("SELECT * FROM exercise_swap_history")
    suspend fun getAllSwapHistory(): List<ExerciseSwapHistory>

    @Query("SELECT * FROM exercise_swap_history WHERE id = :id")
    suspend fun getSwapHistoryById(id: String): ExerciseSwapHistory?

    @Insert
    suspend fun insertSwapHistory(swapHistory: ExerciseSwapHistory)

    @Upsert
    suspend fun upsertSwapHistory(swapHistory: ExerciseSwapHistory)

    @Query(
        """
        SELECT * FROM exercise_swap_history
        WHERE (userId = :userId OR (userId IS NULL AND :userId IS NULL))
        AND originalExerciseId = :originalExerciseId
        AND swappedToExerciseId = :swappedToExerciseId
        AND (workoutId = :workoutId OR (workoutId IS NULL AND :workoutId IS NULL))
        LIMIT 1
        """,
    )
    suspend fun getExistingSwap(
        userId: String?,
        originalExerciseId: String,
        swappedToExerciseId: String,
        workoutId: String?,
    ): ExerciseSwapHistory?

    @Query("DELETE FROM exercise_swap_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM exercise_swap_history WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM exercise_swap_history")
    suspend fun deleteAllHistory()
}

data class SwapHistoryCount(
    val swappedToExerciseId: String,
    val swapCount: Int,
)
