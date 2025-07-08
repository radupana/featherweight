package com.github.radupana.featherweight.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AIProgrammeRequestDao {
    @Insert
    suspend fun insert(request: AIProgrammeRequest)
    
    @Update
    suspend fun update(request: AIProgrammeRequest)
    
    @Query("SELECT * FROM ai_programme_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<AIProgrammeRequest>>
    
    @Query("SELECT * FROM ai_programme_requests WHERE id = :id")
    suspend fun getRequestById(id: String): AIProgrammeRequest?
    
    @Query("SELECT * FROM ai_programme_requests WHERE status = :status")
    suspend fun getRequestsByStatus(status: GenerationStatus): List<AIProgrammeRequest>
    
    @Query("UPDATE ai_programme_requests SET status = :status, errorMessage = :errorMessage, lastUpdatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: GenerationStatus, errorMessage: String? = null, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE ai_programme_requests SET generatedProgrammeJson = :json, status = :status, lastUpdatedAt = :timestamp WHERE id = :id")
    suspend fun saveGeneratedProgramme(id: String, json: String, status: GenerationStatus = GenerationStatus.COMPLETED, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE ai_programme_requests SET attemptCount = attemptCount + 1, lastUpdatedAt = :timestamp WHERE id = :id")
    suspend fun incrementAttemptCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE ai_programme_requests SET workManagerId = :workManagerId WHERE id = :id")
    suspend fun updateWorkManagerId(id: String, workManagerId: String)
    
    @Query("SELECT * FROM ai_programme_requests WHERE status = :status AND createdAt < :beforeTimestamp")
    suspend fun getRequestsOlderThan(status: GenerationStatus, beforeTimestamp: Long): List<AIProgrammeRequest>
    
    @Query("DELETE FROM ai_programme_requests WHERE id = :id")
    suspend fun delete(id: String)
}