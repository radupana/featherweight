package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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
        originalExerciseId: Long,
    ): List<SwapHistoryCount>

    @Query("SELECT * FROM exercise_swap_history")
    suspend fun getAllSwapHistory(): List<ExerciseSwapHistory>

    @Query("SELECT * FROM exercise_swap_history WHERE id = :id")
    suspend fun getSwapHistoryById(id: Long): ExerciseSwapHistory?

    @Insert
    suspend fun insertSwapHistory(swapHistory: ExerciseSwapHistory)

    @Query("DELETE FROM exercise_swap_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM exercise_swap_history WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM exercise_swap_history WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM exercise_swap_history")
    suspend fun deleteAllHistory()
}

data class SwapHistoryCount(
    val swappedToExerciseId: Long,
    val swapCount: Int,
)
