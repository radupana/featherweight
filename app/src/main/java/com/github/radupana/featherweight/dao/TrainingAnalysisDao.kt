package com.github.radupana.featherweight.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.radupana.featherweight.data.TrainingAnalysis

@Dao
interface TrainingAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: TrainingAnalysis): Long

    @Query("SELECT * FROM training_analysis ORDER BY analysisDate DESC LIMIT 1")
    suspend fun getLatestAnalysis(): TrainingAnalysis?

    @Query("SELECT * FROM training_analysis")
    suspend fun getAllAnalyses(): List<TrainingAnalysis>

    @Query("SELECT * FROM training_analysis WHERE id = :id")
    suspend fun getAnalysisById(id: Long): TrainingAnalysis?
}
