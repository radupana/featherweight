package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParseRequestDao {
    @Insert
    suspend fun insert(request: ParseRequest): Long
    
    @Update
    suspend fun update(request: ParseRequest)
    
    @Delete
    suspend fun delete(request: ParseRequest)
    
    @Query("DELETE FROM parse_requests WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM parse_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<ParseRequest>>
    
    @Query("SELECT * FROM parse_requests WHERE id = :id")
    suspend fun getRequest(id: Long): ParseRequest?
    
    @Query("SELECT * FROM parse_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: ParseStatus): Flow<List<ParseRequest>>
    
    @Query("DELETE FROM parse_requests WHERE status = 'FAILED' OR status = 'COMPLETED'")
    suspend fun clearCompletedRequests()
}
