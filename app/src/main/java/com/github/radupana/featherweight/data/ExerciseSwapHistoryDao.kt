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
        WHERE userId = :userId AND originalExerciseId = :originalExerciseId
        GROUP BY swappedToExerciseId
        ORDER BY swapCount DESC
    """,
    )
    suspend fun getSwapHistoryForExercise(
        userId: Long,
        originalExerciseId: Long,
    ): List<SwapHistoryCount>

    @Query(
        """
        SELECT * FROM exercise_swap_history
        WHERE userId = :userId
        ORDER BY swapDate DESC
        LIMIT :limit
    """,
    )
    suspend fun getRecentSwaps(
        userId: Long,
        limit: Int = 10,
    ): List<ExerciseSwapHistory>
}

data class SwapHistoryCount(
    val swappedToExerciseId: Long,
    val swapCount: Int,
)
