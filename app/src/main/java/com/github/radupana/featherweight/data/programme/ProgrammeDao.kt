package com.github.radupana.featherweight.data.programme

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import java.time.LocalDateTime

@Dao
@Suppress("TooManyFunctions")
interface ProgrammeDao {
    // Programme CRUD operations
    @Insert
    suspend fun insertProgramme(programme: Programme)

    @Update
    suspend fun updateProgramme(programme: Programme)

    @Delete
    suspend fun deleteProgramme(programme: Programme)

    @Query("SELECT * FROM programmes WHERE id = :id")
    suspend fun getProgrammeById(id: String): Programme?

    @Query("SELECT * FROM programmes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProgramme(): Programme?

    @Query("SELECT * FROM programmes ORDER BY createdAt DESC")
    suspend fun getAllProgrammes(): List<Programme>

    // Programme Weeks
    @Insert
    suspend fun insertProgrammeWeek(week: ProgrammeWeek)

    @Query("SELECT * FROM programme_weeks WHERE programmeId = :programmeId ORDER BY weekNumber ASC")
    suspend fun getWeeksForProgramme(programmeId: String): List<ProgrammeWeek>

    @Query("SELECT * FROM programme_weeks WHERE id = :weekId")
    suspend fun getWeekById(weekId: String): ProgrammeWeek?

    @Query("SELECT * FROM programme_weeks")
    suspend fun getAllProgrammeWeeks(): List<ProgrammeWeek>

    @Query("SELECT * FROM programme_weeks WHERE id = :id")
    suspend fun getProgrammeWeekById(id: String): ProgrammeWeek?

    // Programme Workouts
    @Insert
    suspend fun insertProgrammeWorkout(workout: ProgrammeWorkout)

    @Query(
        """
        SELECT pw.* FROM programme_workouts pw
        INNER JOIN programme_weeks weeks ON pw.weekId = weeks.id
        WHERE weeks.programmeId = :programmeId
        ORDER BY weeks.weekNumber ASC, pw.dayNumber ASC
    """,
    )
    suspend fun getAllWorkoutsForProgramme(programmeId: String): List<ProgrammeWorkout>

    @Query("SELECT * FROM programme_workouts")
    suspend fun getAllProgrammeWorkouts(): List<ProgrammeWorkout>

    @Query("SELECT * FROM programme_workouts WHERE id = :id")
    suspend fun getProgrammeWorkoutById(id: String): ProgrammeWorkout?

    // Programme Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ProgrammeProgress)

    @Query("SELECT * FROM programme_progress WHERE programmeId = :programmeId")
    suspend fun getProgressForProgramme(programmeId: String): ProgrammeProgress?

    @Query("UPDATE programme_progress SET currentWeek = :week, currentDay = :day, lastWorkoutDate = :date WHERE programmeId = :programmeId")
    suspend fun updateProgress(
        programmeId: String,
        week: Int,
        day: Int,
        date: String,
    )

    @Query("UPDATE programme_progress SET completedWorkouts = completedWorkouts + 1 WHERE programmeId = :programmeId")
    suspend fun incrementCompletedWorkouts(programmeId: String)

    @Query("SELECT * FROM programme_progress")
    suspend fun getAllProgrammeProgress(): List<ProgrammeProgress>

    @Query("SELECT * FROM programme_progress WHERE id = :id")
    suspend fun getProgrammeProgressById(id: String): ProgrammeProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgrammeProgress(progress: ProgrammeProgress)

    @Update
    suspend fun updateProgrammeProgress(progress: ProgrammeProgress)

    // Complex queries with relationships
    @Transaction
    @Query("SELECT * FROM programmes WHERE id = :programmeId")
    suspend fun getProgrammeWithDetails(programmeId: String): ProgrammeWithDetailsRaw?

    // Activation/Deactivation
    @Query("UPDATE programmes SET isActive = 0")
    suspend fun deactivateAllProgrammes()

    @Query("UPDATE programmes SET isActive = 1 WHERE id = :programmeId")
    suspend fun activateProgramme(programmeId: String)

    @Transaction
    suspend fun setActiveProgramme(programmeId: String) {
        // No longer deactivating programmes, user must delete active programme first
        // This should only be called when there's no active programme
        activateProgramme(programmeId)
    }

    // Programme completion - ATOMIC update of status, isActive, and completedAt
    @Transaction
    @Query(
        """
        UPDATE programmes 
        SET status = 'COMPLETED', 
            isActive = 0, 
            completedAt = :completedAt 
        WHERE id = :programmeId
    """,
    )
    suspend fun completeProgrammeAtomic(
        programmeId: String,
        completedAt: LocalDateTime,
    )

    @Query(
        """
        SELECT * FROM programmes
        WHERE status = 'COMPLETED'
        ORDER BY completedAt DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getCompletedProgrammesPaged(
        limit: Int,
        offset: Int,
    ): List<Programme>

    @Query(
        """
        SELECT * FROM programmes
        WHERE status IN ('COMPLETED', 'CANCELLED')
        ORDER BY completedAt DESC
    """,
    )
    suspend fun getArchivedProgrammes(): List<Programme>

    // Update programme status atomically
    @Query(
        """
        UPDATE programmes 
        SET status = :status,
            isActive = CASE 
                WHEN :status IN ('NOT_STARTED', 'IN_PROGRESS') THEN isActive 
                ELSE 0 
            END,
            startedAt = CASE
                WHEN :status = 'IN_PROGRESS' AND startedAt IS NULL THEN :startedAt
                ELSE startedAt
            END
        WHERE id = :programmeId
    """,
    )
    suspend fun updateProgrammeStatus(
        programmeId: String,
        status: ProgrammeStatus,
        startedAt: LocalDateTime = LocalDateTime.now(),
    )

    @Query("DELETE FROM programmes WHERE userId = :userId")
    suspend fun deleteAllProgrammesForUser(userId: String)

    @Query("DELETE FROM programmes WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM programmes WHERE userId IS NULL")
    suspend fun deleteAllWhereUserIdIsNull()

    @Query("DELETE FROM programme_weeks WHERE programmeId IN (SELECT id FROM programmes WHERE userId = :userId)")
    suspend fun deleteAllProgrammeWeeksForUser(userId: String)

    @Query("DELETE FROM programme_workouts WHERE weekId IN (SELECT id FROM programme_weeks WHERE programmeId IN (SELECT id FROM programmes WHERE userId = :userId))")
    suspend fun deleteAllProgrammeWorkoutsForUser(userId: String)

    @Query("DELETE FROM programme_progress WHERE programmeId IN (SELECT id FROM programmes WHERE userId = :userId)")
    suspend fun deleteAllProgrammeProgressForUser(userId: String)

    // Methods to delete ALL data (for Clear All Workout Data feature)
    @Query("DELETE FROM programmes")
    suspend fun deleteAllProgrammes()

    @Query("DELETE FROM programme_weeks")
    suspend fun deleteAllProgrammeWeeks()

    @Query("DELETE FROM programme_workouts")
    suspend fun deleteAllProgrammeWorkouts()

    @Query("DELETE FROM programme_progress")
    suspend fun deleteAllProgrammeProgress()

    // Methods to delete NULL userId data
    @Query("DELETE FROM programmes WHERE userId IS NULL")
    suspend fun deleteAllProgrammesWhereUserIdIsNull()

    @Query("DELETE FROM programme_weeks WHERE programmeId IN (SELECT id FROM programmes WHERE userId IS NULL)")
    suspend fun deleteAllProgrammeWeeksWhereUserIdIsNull()

    @Query("DELETE FROM programme_workouts WHERE weekId IN (SELECT id FROM programme_weeks WHERE programmeId IN (SELECT id FROM programmes WHERE userId IS NULL))")
    suspend fun deleteAllProgrammeWorkoutsWhereUserIdIsNull()

    @Query("DELETE FROM programme_progress WHERE programmeId IN (SELECT id FROM programmes WHERE userId IS NULL)")
    suspend fun deleteAllProgrammeProgressWhereUserIdIsNull()
}

// Raw data classes for Room @Transaction queries
data class ProgrammeWithDetailsRaw(
    @Embedded val programme: Programme,
    @Relation(
        parentColumn = "id",
        entityColumn = "programmeId",
    )
    val progress: ProgrammeProgress?,
)
