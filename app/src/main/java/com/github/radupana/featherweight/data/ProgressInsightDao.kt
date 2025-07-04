package com.github.radupana.featherweight.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ProgressInsightDao {
    
    /**
     * Get all insights for a user, ordered by priority and generation date
     */
    @Query("""
        SELECT * FROM progress_insights 
        WHERE userId = :userId 
        ORDER BY priority ASC, generatedDate DESC
    """)
    fun getInsightsForUser(userId: Long): Flow<List<ProgressInsight>>
    
    /**
     * Get recent insights for dashboard display
     */
    @Query("""
        SELECT * FROM progress_insights 
        WHERE userId = :userId 
        AND generatedDate >= :sinceDate
        ORDER BY priority ASC, generatedDate DESC 
        LIMIT :limit
    """)
    suspend fun getRecentInsights(userId: Long, sinceDate: LocalDateTime, limit: Int = 5): List<ProgressInsight>
    
    /**
     * Get unread insights count
     */
    @Query("""
        SELECT COUNT(*) FROM progress_insights 
        WHERE userId = :userId AND isRead = 0
    """)
    suspend fun getUnreadInsightsCount(userId: Long): Int
    
    /**
     * Get actionable insights that require user attention
     */
    @Query("""
        SELECT * FROM progress_insights 
        WHERE userId = :userId 
        AND isActionable = 1 
        AND generatedDate >= :sinceDate
        ORDER BY priority ASC, generatedDate DESC
    """)
    suspend fun getActionableInsights(userId: Long, sinceDate: LocalDateTime): List<ProgressInsight>
    
    /**
     * Get insights by type
     */
    @Query("""
        SELECT * FROM progress_insights 
        WHERE userId = :userId 
        AND insightType = :type
        ORDER BY generatedDate DESC 
        LIMIT :limit
    """)
    suspend fun getInsightsByType(userId: Long, type: InsightType, limit: Int = 10): List<ProgressInsight>
    
    /**
     * Get exercise-specific insights
     */
    @Query("""
        SELECT * FROM progress_insights 
        WHERE userId = :userId 
        AND exerciseName = :exerciseName
        ORDER BY generatedDate DESC 
        LIMIT :limit
    """)
    suspend fun getExerciseInsights(userId: Long, exerciseName: String, limit: Int = 5): List<ProgressInsight>
    
    /**
     * Insert a new insight
     */
    @Insert
    suspend fun insertInsight(insight: ProgressInsight): Long
    
    /**
     * Insert multiple insights
     */
    @Insert
    suspend fun insertInsights(insights: List<ProgressInsight>)
    
    /**
     * Mark insight as read
     */
    @Query("UPDATE progress_insights SET isRead = 1 WHERE id = :insightId")
    suspend fun markAsRead(insightId: Long)
    
    /**
     * Mark all insights as read for a user
     */
    @Query("UPDATE progress_insights SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Long)
    
    /**
     * Delete old insights (cleanup)
     */
    @Query("""
        DELETE FROM progress_insights 
        WHERE generatedDate < :beforeDate
    """)
    suspend fun deleteOldInsights(beforeDate: LocalDateTime)
    
    /**
     * Delete insight by ID
     */
    @Query("DELETE FROM progress_insights WHERE id = :insightId")
    suspend fun deleteInsight(insightId: Long)
    
    /**
     * Check if similar insight already exists to avoid duplicates
     */
    @Query("""
        SELECT COUNT(*) FROM progress_insights 
        WHERE userId = :userId 
        AND insightType = :type 
        AND exerciseName = :exerciseName 
        AND generatedDate >= :sinceDate
    """)
    suspend fun getSimilarInsightCount(
        userId: Long, 
        type: InsightType, 
        exerciseName: String?, 
        sinceDate: LocalDateTime
    ): Int
}