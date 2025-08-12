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
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId 
        LIMIT 1
    """,
    )
    suspend fun getProgressForExercise(
        userId: Long,
        exerciseVariationId: Long,
    ): GlobalExerciseProgress?

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId 
        LIMIT 1
    """,
    )
    fun observeProgressForExercise(
        userId: Long,
        exerciseVariationId: Long,
    ): Flow<GlobalExerciseProgress?>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY lastUpdated DESC
    """,
    )
    fun getAllProgressForUser(userId: Long): Flow<List<GlobalExerciseProgress>>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY lastUpdated DESC
    """,
    )
    suspend fun getAllProgress(userId: Long): List<GlobalExerciseProgress>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND trend = :trend
        ORDER BY consecutiveStalls DESC
    """,
    )
    suspend fun getExercisesByTrend(
        userId: Long,
        trend: ProgressTrend,
    ): List<GlobalExerciseProgress>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND consecutiveStalls >= :minStalls
        ORDER BY consecutiveStalls DESC
    """,
    )
    suspend fun getStalledExercises(
        userId: Long,
        minStalls: Int = 3,
    ): List<GlobalExerciseProgress>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY totalVolumeLast30Days DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopVolumeExercises(
        userId: Long,
        limit: Int = 10,
    ): List<GlobalExerciseProgress>

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        AND lastUpdated < :cutoffDate
        ORDER BY lastUpdated ASC
    """,
    )
    suspend fun getNeglectedExercises(
        userId: Long,
        cutoffDate: LocalDateTime,
    ): List<GlobalExerciseProgress>

    @Query(
        """
        UPDATE global_exercise_progress 
        SET estimatedMax = :newMax, lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun updateEstimatedMax(
        userId: Long,
        exerciseVariationId: Long,
        newMax: Float,
        updateTime: LocalDateTime,
    )

    @Query(
        """
        UPDATE global_exercise_progress 
        SET lastPrDate = :prDate, lastPrWeight = :prWeight, lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun recordNewPR(
        userId: Long,
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
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun incrementStallCount(
        userId: Long,
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
        WHERE userId = :userId AND exerciseVariationId = :exerciseVariationId
    """,
    )
    suspend fun recordProgression(
        userId: Long,
        exerciseVariationId: Long,
        newWeight: Float,
        progressDate: LocalDateTime,
        updateTime: LocalDateTime,
    )

    @Query("DELETE FROM global_exercise_progress WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    @Query(
        """
        SELECT exerciseVariationId, totalVolumeLast30Days 
        FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY totalVolumeLast30Days DESC
    """,
    )
    suspend fun getVolumeDistribution(userId: Long): List<ExerciseVolumeInfo>

    data class ExerciseVolumeInfo(
        val exerciseVariationId: Long,
        val totalVolumeLast30Days: Float,
    )

    @Query("DELETE FROM global_exercise_progress")
    suspend fun deleteAllGlobalProgress()
}
