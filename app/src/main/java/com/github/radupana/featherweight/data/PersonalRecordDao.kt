package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    
    @Insert
    suspend fun insertPersonalRecord(personalRecord: PersonalRecord): Long
    
    @Query("SELECT * FROM PersonalRecord WHERE exerciseName = :exerciseName ORDER BY recordDate DESC")
    fun getPRHistoryForExercise(exerciseName: String): Flow<List<PersonalRecord>>
    
    @Query("SELECT * FROM PersonalRecord WHERE exerciseName = :exerciseName ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentPRsForExercise(exerciseName: String, limit: Int = 5): List<PersonalRecord>
    
    @Query("""
        WITH RankedPRs AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY exerciseName ORDER BY estimated1RM DESC, recordDate DESC) as rn
            FROM PersonalRecord
            WHERE recordDate >= date('now', '-30 days')
        )
        SELECT * FROM RankedPRs 
        WHERE rn = 1 
        ORDER BY recordDate DESC 
        LIMIT :limit
    """)
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord>
    
    @Query("SELECT * FROM PersonalRecord WHERE recordDate >= :sinceDate ORDER BY recordDate DESC")
    suspend fun getPRsSince(sinceDate: String): List<PersonalRecord>
    
    @Query("""
        SELECT * FROM PersonalRecord 
        WHERE exerciseName = :exerciseName 
        AND recordType = :recordType 
        ORDER BY recordDate DESC 
        LIMIT 1
    """)
    suspend fun getLatestPRForExerciseAndType(exerciseName: String, recordType: PRType): PersonalRecord?
    
    @Query("""
        SELECT MAX(weight) 
        FROM PersonalRecord 
        WHERE exerciseName = :exerciseName 
        AND recordType = 'WEIGHT'
    """)
    suspend fun getMaxWeightForExercise(exerciseName: String): Float?
    
    @Query("""
        SELECT MAX(volume) 
        FROM PersonalRecord 
        WHERE exerciseName = :exerciseName 
        AND recordType = 'VOLUME'
    """)
    suspend fun getMaxVolumeForExercise(exerciseName: String): Float?
    
    @Query("SELECT COUNT(*) FROM PersonalRecord WHERE exerciseName = :exerciseName")
    suspend fun getPRCountForExercise(exerciseName: String): Int
    
    @Query("SELECT COUNT(*) FROM PersonalRecord WHERE recordDate >= :sinceDate")
    suspend fun getPRCountSince(sinceDate: String): Int
    
    @Query("DELETE FROM PersonalRecord WHERE id = :id")
    suspend fun deletePersonalRecord(id: Long)
    
    @Query("""
        SELECT * FROM PersonalRecord 
        WHERE exerciseName = :exerciseName 
        ORDER BY weight DESC, recordDate DESC 
        LIMIT 1
    """)
    suspend fun getLatestRecordForExercise(exerciseName: String): PersonalRecord?

    @Query("DELETE FROM PersonalRecord")
    suspend fun clearAllPersonalRecords()
    
    @Query("DELETE FROM PersonalRecord")
    suspend fun deleteAllPersonalRecords()
    
    @Query("SELECT * FROM PersonalRecord ORDER BY recordDate DESC")
    suspend fun getAllPersonalRecords(): List<PersonalRecord>
}