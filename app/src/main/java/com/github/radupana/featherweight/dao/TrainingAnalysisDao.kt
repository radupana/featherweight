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

    @Query("SELECT * FROM training_analysis ORDER BY analysisDate DESC LIMIT 1")
    suspend fun getLatestAnalysis(): TrainingAnalysis?

    @Query("DELETE FROM training_analysis WHERE analysisDate < :olderThan")
    suspend fun deleteOldAnalyses(
        olderThan: LocalDateTime,
    )
}
