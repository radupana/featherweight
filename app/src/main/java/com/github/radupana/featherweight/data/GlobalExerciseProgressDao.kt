package com.github.radupana.featherweight.data

import androidx.room.*
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.ProgressTrend
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface GlobalExerciseProgressDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GlobalExerciseProgress): Long
    
    @Update
    suspend fun update(progress: GlobalExerciseProgress)
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND exerciseName = :exerciseName 
        LIMIT 1
    """)
    suspend fun getProgressForExercise(userId: Long, exerciseName: String): GlobalExerciseProgress?
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND exerciseName = :exerciseName 
        LIMIT 1
    """)
    fun observeProgressForExercise(userId: Long, exerciseName: String): Flow<GlobalExerciseProgress?>
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY lastUpdated DESC
    """)
    fun getAllProgressForUser(userId: Long): Flow<List<GlobalExerciseProgress>>
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND trend = :trend
        ORDER BY consecutiveStalls DESC
    """)
    suspend fun getExercisesByTrend(userId: Long, trend: ProgressTrend): List<GlobalExerciseProgress>
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId AND consecutiveStalls >= :minStalls
        ORDER BY consecutiveStalls DESC
    """)
    suspend fun getStalledExercises(userId: Long, minStalls: Int = 3): List<GlobalExerciseProgress>
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY totalVolumeLast30Days DESC
        LIMIT :limit
    """)
    suspend fun getTopVolumeExercises(userId: Long, limit: Int = 10): List<GlobalExerciseProgress>
    
    @Query("""
        SELECT * FROM global_exercise_progress 
        WHERE userId = :userId 
        AND lastUpdated < :cutoffDate
        ORDER BY lastUpdated ASC
    """)
    suspend fun getNeglectedExercises(userId: Long, cutoffDate: LocalDateTime): List<GlobalExerciseProgress>
    
    @Query("""
        UPDATE global_exercise_progress 
        SET estimatedMax = :newMax, lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseName = :exerciseName
    """)
    suspend fun updateEstimatedMax(userId: Long, exerciseName: String, newMax: Float, updateTime: LocalDateTime)
    
    @Query("""
        UPDATE global_exercise_progress 
        SET lastPrDate = :prDate, lastPrWeight = :prWeight, lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseName = :exerciseName
    """)
    suspend fun recordNewPR(userId: Long, exerciseName: String, prWeight: Float, prDate: LocalDateTime, updateTime: LocalDateTime)
    
    @Query("""
        UPDATE global_exercise_progress 
        SET consecutiveStalls = consecutiveStalls + 1, 
            weeksAtCurrentWeight = :weeksAtWeight,
            lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseName = :exerciseName
    """)
    suspend fun incrementStallCount(userId: Long, exerciseName: String, weeksAtWeight: Int, updateTime: LocalDateTime)
    
    @Query("""
        UPDATE global_exercise_progress 
        SET consecutiveStalls = 0, 
            weeksAtCurrentWeight = 0,
            lastProgressionDate = :progressDate,
            currentWorkingWeight = :newWeight,
            lastUpdated = :updateTime 
        WHERE userId = :userId AND exerciseName = :exerciseName
    """)
    suspend fun recordProgression(userId: Long, exerciseName: String, newWeight: Float, progressDate: LocalDateTime, updateTime: LocalDateTime)
    
    @Query("DELETE FROM global_exercise_progress WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)
    
    @Query("""
        SELECT exerciseName, totalVolumeLast30Days 
        FROM global_exercise_progress 
        WHERE userId = :userId 
        ORDER BY totalVolumeLast30Days DESC
    """)
    suspend fun getVolumeDistribution(userId: Long): List<ExerciseVolumeInfo>
    
    data class ExerciseVolumeInfo(
        val exerciseName: String,
        val totalVolumeLast30Days: Float
    )
}