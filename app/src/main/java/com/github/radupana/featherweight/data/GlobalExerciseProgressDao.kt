package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GlobalExerciseProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GlobalExerciseProgress)

    @Query(
        """
        SELECT * FROM global_exercise_progress 
        WHERE exerciseId = :exerciseId 
        LIMIT 1
    """,
    )
    suspend fun getProgressForExercise(
        exerciseId: String,
    ): GlobalExerciseProgress?

    @Query("DELETE FROM global_exercise_progress")
    suspend fun deleteAllGlobalProgress()

    @Query("SELECT * FROM global_exercise_progress")
    suspend fun getAllProgress(): List<GlobalExerciseProgress>

    @Query("SELECT * FROM global_exercise_progress WHERE id = :id")
    suspend fun getProgressById(id: String): GlobalExerciseProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: GlobalExerciseProgress)

    @Query("DELETE FROM global_exercise_progress WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM global_exercise_progress WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()
}
