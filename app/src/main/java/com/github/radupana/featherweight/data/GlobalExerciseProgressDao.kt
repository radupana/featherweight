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

    @Query("DELETE FROM global_exercise_progress")
    suspend fun deleteAllGlobalProgress()
}
