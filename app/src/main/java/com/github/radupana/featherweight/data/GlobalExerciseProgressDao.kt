package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GlobalExerciseProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GlobalExerciseProgress): Long

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

    @Query("DELETE FROM global_exercise_progress")
    suspend fun deleteAllGlobalProgress()

    @Query("SELECT * FROM global_exercise_progress")
    suspend fun getAllProgress(): List<GlobalExerciseProgress>

    @Query("SELECT * FROM global_exercise_progress WHERE id = :id")
    suspend fun getProgressById(id: Long): GlobalExerciseProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: GlobalExerciseProgress)
}
