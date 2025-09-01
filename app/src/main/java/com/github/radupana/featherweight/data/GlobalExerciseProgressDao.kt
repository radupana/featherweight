package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface GlobalExerciseProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GlobalExerciseProgress): Long

    @Update
    suspend fun update(progress: GlobalExerciseProgress)

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE exerciseVariationId = :exerciseVariationId 
        LIMIT 1
    """,
    )
    suspend fun getProgressForExercise(
        exerciseVariationId: Long,
    ): GlobalExerciseProgress?

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE exerciseVariationId = :exerciseVariationId 
        LIMIT 1
    """,
    )
    fun observeProgressForExercise(
        exerciseVariationId: Long,
    ): Flow<GlobalExerciseProgress?>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        ORDER BY lastUpdated DESC
    """,
    )
    fun getAllProgressForUser(): Flow<List<GlobalExerciseProgress>>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        ORDER BY lastUpdated DESC
    """,
    )
    suspend fun getAllProgress(): List<GlobalExerciseProgress>

    @Query(
        """
        UPDATE global_exercise_progress 
        SET estimatedMax = :newMax, lastUpdated = :updateTime 
        WHERE exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun updateEstimatedMax(
        exerciseVariationId: Long,
        newMax: Float,
        updateTime: LocalDateTime,
    )

    @Query(
        """
        UPDATE global_exercise_progress 
        SET lastPrDate = :prDate, lastPrWeight = :prWeight, lastUpdated = :updateTime 
        WHERE exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun recordNewPR(
        exerciseVariationId: Long,
        prWeight: Float,
        prDate: LocalDateTime,
        updateTime: LocalDateTime,
    )

    @Query(
        """
        UPDATE global_exercise_progress 
        SET consecutiveStalls = consecutiveStalls + 1, 
            weeksAtCurrentWeight = :weeksAtWeight,
            lastUpdated = :updateTime 
        WHERE exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun incrementStallCount(
        exerciseVariationId: Long,
        weeksAtWeight: Int,
        updateTime: LocalDateTime,
    )

    @Query(
        """
        UPDATE global_exercise_progress 
        SET consecutiveStalls = 0, 
            weeksAtCurrentWeight = 0,
            lastProgressionDate = :progressDate,
            currentWorkingWeight = :newWeight,
            lastUpdated = :updateTime 
        WHERE exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun recordProgression(
        exerciseVariationId: Long,
        newWeight: Float,
        progressDate: LocalDateTime,
        updateTime: LocalDateTime,
    )

    @Query("DELETE FROM global_exercise_progress")
    suspend fun deleteAllGlobalProgress()
}
