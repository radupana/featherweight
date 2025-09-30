package com.github.radupana.featherweight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomWarnings
import java.time.LocalDateTime

@Dao
interface PersonalRecordDao {
    @Insert
    suspend fun insertPersonalRecord(personalRecord: PersonalRecord)

    @Query("SELECT * FROM personal_records WHERE exerciseVariationId = :exerciseVariationId ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentPRsForExercise(
        exerciseVariationId: String,
        limit: Int = 5,
    ): List<PersonalRecord>

    @Query(
        """
        WITH RankedPRs AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY exerciseVariationId ORDER BY estimated1RM DESC, recordDate DESC) as rn
            FROM personal_records
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
        SELECT * FROM personal_records 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND recordType = :recordType 
        ORDER BY recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestPRForExerciseAndType(
        exerciseVariationId: String,
        recordType: PRType,
    ): PersonalRecord?

    @Query(
        """
        SELECT MAX(weight) 
        FROM personal_records 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND recordType = 'WEIGHT'
    """,
    )
    suspend fun getMaxWeightForExercise(exerciseVariationId: String): Float?

    @Query(
        """
        SELECT MAX(estimated1RM) 
        FROM personal_records 
        WHERE exerciseVariationId = :exerciseVariationId 
        AND estimated1RM IS NOT NULL
    """,
    )
    suspend fun getMaxEstimated1RMForExercise(exerciseVariationId: String): Float?

    @Query(
        """
        SELECT * FROM personal_records 
        WHERE exerciseVariationId = :exerciseVariationId 
        ORDER BY weight DESC, recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestRecordForExercise(exerciseVariationId: String): PersonalRecord?

    @Query("DELETE FROM personal_records")
    suspend fun deleteAllPersonalRecords()

    @Query("DELETE FROM personal_records WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM personal_records WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query(
        """
        SELECT * FROM personal_records 
        WHERE workoutId = :workoutId 
        ORDER BY recordDate DESC
    """,
    )
    suspend fun getPersonalRecordsForWorkout(workoutId: String): List<PersonalRecord>

    @Query(
        """
        SELECT * FROM personal_records 
        WHERE workoutId = :workoutId 
        AND exerciseVariationId = :exerciseVariationId
        AND recordType = :recordType
        LIMIT 1
    """,
    )
    suspend fun getPRForExerciseInWorkout(
        workoutId: String,
        exerciseVariationId: String,
        recordType: PRType,
    ): PersonalRecord?

    @Query("DELETE FROM personal_records WHERE id = :prId")
    suspend fun deletePR(prId: String)

    @Query(
        """
        SELECT * FROM personal_records 
        WHERE recordDate >= :startDate 
        AND recordDate <= :endDate
        ORDER BY recordDate DESC
    """,
    )
    suspend fun getPersonalRecordsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PersonalRecord>

    @Query("SELECT * FROM personal_records")
    suspend fun getAllPersonalRecords(): List<PersonalRecord>

    @Query("SELECT * FROM personal_records WHERE id = :id")
    suspend fun getPersonalRecordById(id: String): PersonalRecord?

    @androidx.room.Update
    suspend fun updatePersonalRecord(record: PersonalRecord)

    @Query("DELETE FROM personal_records WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
