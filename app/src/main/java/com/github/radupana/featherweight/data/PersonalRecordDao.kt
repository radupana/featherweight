package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomWarnings
import java.time.LocalDateTime

@Dao
interface PersonalRecordDao {
    @Insert
    suspend fun insertPersonalRecord(personalRecord: PersonalRecord): Long

    @Query("SELECT * FROM PersonalRecord WHERE exerciseVariationId = :exerciseVariationId ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentPRsForExercise(
        exerciseVariationId: Long,
        limit: Int = 5,
    ): List<PersonalRecord>

    @Query(
        """
        WITH RankedPRs AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY exerciseVariationId ORDER BY estimated1RM DESC, recordDate DESC) as rn
            FROM PersonalRecord
            WHERE recordDate >= date('now', '-30 days')
        )
        SELECT * FROM RankedPRs 
        WHERE rn = 1 
        ORDER BY recordDate DESC 
        LIMIT :limit
    """,
    )
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord>

    @Query(
        """
        SELECT * FROM PersonalRecord 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND recordType = :recordType 
        ORDER BY recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestPRForExerciseAndType(
        exerciseVariationId: Long,
        recordType: PRType,
    ): PersonalRecord?

    @Query(
        """
        SELECT MAX(weight) 
        FROM PersonalRecord 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND recordType = 'WEIGHT'
    """,
    )
    suspend fun getMaxWeightForExercise(exerciseVariationId: Long): Float?

    @Query(
        """
        SELECT MAX(estimated1RM) 
        FROM PersonalRecord 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND estimated1RM IS NOT NULL
    """,
    )
    suspend fun getMaxEstimated1RMForExercise(exerciseVariationId: Long): Float?

    @Query(
        """
        SELECT * FROM PersonalRecord 
        WHERE exerciseVariationId = :exerciseVariationId 
        ORDER BY weight DESC, recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestRecordForExercise(exerciseVariationId: Long): PersonalRecord?

    @Query("DELETE FROM PersonalRecord")
    suspend fun deleteAllPersonalRecords()

    @Query(
        """
        SELECT * FROM PersonalRecord 
        WHERE workoutId = :workoutId 
        ORDER BY recordDate DESC
    """,
    )
    suspend fun getPersonalRecordsForWorkout(workoutId: Long): List<PersonalRecord>

    @Query(
        """
        SELECT * FROM PersonalRecord 
        WHERE workoutId = :workoutId 
        AND exerciseVariationId = :exerciseVariationId
        AND recordType = :recordType
        LIMIT 1
    """,
    )
    suspend fun getPRForExerciseInWorkout(
        workoutId: Long,
        exerciseVariationId: Long,
        recordType: PRType,
    ): PersonalRecord?

    @Query("DELETE FROM PersonalRecord WHERE id = :prId")
    suspend fun deletePR(prId: Long)

    @Query(
        """
        SELECT * FROM PersonalRecord 
        WHERE recordDate >= :startDate 
        AND recordDate <= :endDate
        ORDER BY recordDate DESC
    """,
    )
    suspend fun getPersonalRecordsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PersonalRecord>
}
