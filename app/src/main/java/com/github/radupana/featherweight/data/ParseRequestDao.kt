package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParseRequestDao {
    @Insert
    suspend fun insert(request: ParseRequest)

    @Update
    suspend fun update(request: ParseRequest)

    @Query("DELETE FROM parse_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM parse_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<ParseRequest>>

    @Query("SELECT * FROM parse_requests WHERE id = :id")
    suspend fun getRequest(id: String): ParseRequest?

    @Query("SELECT * FROM parse_requests WHERE status IN ('PROCESSING', 'COMPLETED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPendingRequest(): ParseRequest?

    @Query("SELECT COUNT(*) FROM parse_requests WHERE status IN ('PROCESSING', 'COMPLETED')")
    suspend fun getPendingRequestCount(): Int

    @Query("SELECT * FROM parse_requests")
    suspend fun getAllRequestsList(): List<ParseRequest>

    @Query("SELECT * FROM parse_requests WHERE id = :id")
    suspend fun getParseRequestById(id: String): ParseRequest?

    @Insert
    suspend fun insertParseRequest(request: ParseRequest)

    @Query("DELETE FROM parse_requests WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM parse_requests WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM parse_requests")
    suspend fun deleteAllRequests()
}
