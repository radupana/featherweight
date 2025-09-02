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
}

data class SwapHistoryCount(
    val swappedToExerciseId: Long,
    val swapCount: Int,
)
