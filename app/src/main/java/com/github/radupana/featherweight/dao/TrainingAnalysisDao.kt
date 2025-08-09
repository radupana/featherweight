package com.github.radupana.featherweight.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.radupana.featherweight.data.TrainingAnalysis
import java.time.LocalDateTime

@Dao
interface TrainingAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: TrainingAnalysis): Long

    @Query("SELECT * FROM training_analysis WHERE userId = :userId ORDER BY analysisDate DESC LIMIT 1")
    suspend fun getLatestAnalysis(userId: Long): TrainingAnalysis?

    @Query("DELETE FROM training_analysis WHERE userId = :userId AND analysisDate < :olderThan")
    suspend fun deleteOldAnalyses(userId: Long, olderThan: LocalDateTime)

    @Query("DELETE FROM training_analysis WHERE userId = :userId")
    suspend fun deleteAllAnalyses(userId: Long)
}