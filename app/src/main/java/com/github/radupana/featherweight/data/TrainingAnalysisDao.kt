package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrainingAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: TrainingAnalysis)

    @Query("SELECT * FROM training_analyses ORDER BY analysisDate DESC LIMIT 1")
    suspend fun getLatestAnalysis(): TrainingAnalysis?

    @Query("SELECT * FROM training_analyses WHERE id = :id")
    suspend fun getAnalysisById(id: String): TrainingAnalysis?

    @Query("DELETE FROM training_analyses WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM training_analyses WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("SELECT * FROM training_analyses")
    suspend fun getAllAnalyses(): List<TrainingAnalysis>

    @Query("DELETE FROM training_analyses")
    suspend fun deleteAllAnalyses()
}
