package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExerciseCorrelationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(correlations: List<ExerciseCorrelation>)

    @Query("SELECT COUNT(*) FROM exercise_correlations")
    suspend fun getCount(): Int

    @Query("SELECT * FROM exercise_correlations")
    suspend fun getAllCorrelations(): List<ExerciseCorrelation>

    @Query("SELECT * FROM exercise_correlations WHERE id = :id")
    suspend fun getCorrelationById(id: Long): ExerciseCorrelation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrelation(correlation: ExerciseCorrelation)
}
