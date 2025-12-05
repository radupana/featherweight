// Suppression justified: Room DAO interfaces inherently require many query methods to support
// the various data access patterns needed by the application. Splitting into multiple DAOs
// would create artificial boundaries and complicate transaction management.
@file:Suppress("TooManyFunctions")

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

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentPRsForExercise(
        exerciseId: String,
        limit: Int = 5,
    ): List<PersonalRecord>

    @Query(
        """
        WITH RankedPRs AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY exerciseId ORDER BY estimated1RM DESC, recordDate DESC) as rn
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
        WHERE exerciseId = :exerciseId 
        AND recordType = :recordType 
        ORDER BY recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestPRForExerciseAndType(
        exerciseId: String,
        recordType: PRType,
    ): PersonalRecord?

    @Query(
        """
        SELECT MAX(weight)
        FROM personal_records
        WHERE exerciseId = :exerciseId
        AND recordType = 'WEIGHT'
    """,
    )
    suspend fun getMaxWeightForExercise(exerciseId: String): Float?

    @Query(
        """
        SELECT MAX(reps)
        FROM personal_records
        WHERE exerciseId = :exerciseId
        AND recordType = 'REPS'
    """,
    )
    suspend fun getMaxRepsForExercise(exerciseId: String): Int?

    @Query(
        """
        SELECT MAX(estimated1RM) 
        FROM personal_records 
        WHERE exerciseId = :exerciseId 
        AND estimated1RM IS NOT NULL
    """,
    )
    suspend fun getMaxEstimated1RMForExercise(exerciseId: String): Float?

    @Query(
        """
        SELECT * FROM personal_records 
        WHERE exerciseId = :exerciseId 
        ORDER BY weight DESC, recordDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestRecordForExercise(exerciseId: String): PersonalRecord?

    @Query("DELETE FROM personal_records")
    suspend fun deleteAllPersonalRecords()

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
        AND exerciseId = :exerciseId
        AND recordType = :recordType
        LIMIT 1
    """,
    )
    suspend fun getPRForExerciseInWorkout(
        workoutId: String,
        exerciseId: String,
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

    @Query("DELETE FROM personal_records WHERE sourceSetId = :setId")
    suspend fun deletePRsBySourceSetId(setId: String)

    @Query("SELECT * FROM personal_records WHERE sourceSetId = :setId")
    suspend fun getPRsBySourceSetId(setId: String): List<PersonalRecord>

    @Query("SELECT COUNT(*) FROM personal_records WHERE sourceSetId = :setId")
    suspend fun countPRsBySourceSetId(setId: String): Int
}
