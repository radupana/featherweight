package com.github.radupana.featherweight.data

import androidx.room.Dao
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

    @Query("DELETE FROM parse_requests WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM parse_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<ParseRequest>>

    @Query("SELECT * FROM parse_requests WHERE id = :id")
    suspend fun getRequest(id: Long): ParseRequest?
}
