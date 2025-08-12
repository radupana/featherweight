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
interface ProgrammeDao {
    // Programme CRUD operations
    @Insert
    suspend fun insertProgramme(programme: Programme): Long

    @Update
    suspend fun updateProgramme(programme: Programme)

    @Delete
    suspend fun deleteProgramme(programme: Programme)

    @Query("SELECT * FROM programmes WHERE id = :id")
    suspend fun getProgrammeById(id: Long): Programme?

    @Query("SELECT * FROM programmes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProgramme(): Programme?

    @Query("SELECT * FROM programmes ORDER BY createdAt DESC")
    suspend fun getAllProgrammes(): List<Programme>

    @Query("SELECT * FROM programmes WHERE isCustom = :isCustom ORDER BY createdAt DESC")
    suspend fun getProgrammesByType(isCustom: Boolean): List<Programme>

    // Programme Templates
    @Insert
    suspend fun insertProgrammeTemplate(template: ProgrammeTemplate): Long

    @Query("SELECT * FROM programme_templates ORDER BY difficulty ASC, name ASC")
    suspend fun getAllTemplates(): List<ProgrammeTemplate>

    @Query("SELECT * FROM programme_templates WHERE difficulty = :difficulty")
    suspend fun getTemplatesByDifficulty(difficulty: ProgrammeDifficulty): List<ProgrammeTemplate>

    @Query("SELECT * FROM programme_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): ProgrammeTemplate?

    @Query("DELETE FROM programme_templates WHERE name = :name")
    suspend fun deleteProgrammeTemplateByName(name: String)

    @Query("DELETE FROM programmes WHERE id = :id")
    suspend fun deleteProgrammeById(id: Long)

    // Programme Weeks
    @Insert
    suspend fun insertProgrammeWeek(week: ProgrammeWeek): Long

    @Insert
    suspend fun insertProgrammeWeeks(weeks: List<ProgrammeWeek>)

    @Query("SELECT * FROM programme_weeks WHERE programmeId = :programmeId ORDER BY weekNumber ASC")
    suspend fun getWeeksForProgramme(programmeId: Long): List<ProgrammeWeek>

    @Query("SELECT * FROM programme_weeks WHERE id = :weekId")
    suspend fun getWeekById(weekId: Long): ProgrammeWeek?

    // Programme Workouts
    @Insert
    suspend fun insertProgrammeWorkout(workout: ProgrammeWorkout): Long

    @Insert
    suspend fun insertProgrammeWorkouts(workouts: List<ProgrammeWorkout>)

    @Query("SELECT * FROM programme_workouts WHERE weekId = :weekId ORDER BY dayNumber ASC")
    suspend fun getWorkoutsForWeek(weekId: Long): List<ProgrammeWorkout>

    @Query("SELECT * FROM programme_workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): ProgrammeWorkout?

    @Query(
        """
        SELECT pw.* FROM programme_workouts pw
        INNER JOIN programme_weeks weeks ON pw.weekId = weeks.id
        WHERE weeks.programmeId = :programmeId
        ORDER BY weeks.weekNumber ASC, pw.dayNumber ASC
    """,
    )
    suspend fun getAllWorkoutsForProgramme(programmeId: Long): List<ProgrammeWorkout>

    // Exercise Substitutions
    @Insert
    suspend fun insertSubstitution(substitution: ExerciseSubstitution): Long

    @Insert
    suspend fun insertSubstitutions(substitutions: List<ExerciseSubstitution>)

    @Query("SELECT * FROM exercise_substitutions WHERE programmeId = :programmeId")
    suspend fun getSubstitutionsForProgramme(programmeId: Long): List<ExerciseSubstitution>

    @Query("SELECT * FROM exercise_substitutions WHERE programmeId = :programmeId AND originalExerciseName = :exerciseName")
    suspend fun getSubstitutionsForExercise(
        programmeId: Long,
        exerciseName: String,
    ): List<ExerciseSubstitution>

    @Delete
    suspend fun deleteSubstitution(substitution: ExerciseSubstitution)

    // Programme Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ProgrammeProgress): Long

    @Query("SELECT * FROM programme_progress WHERE programmeId = :programmeId")
    suspend fun getProgressForProgramme(programmeId: Long): ProgrammeProgress?

    @Query("UPDATE programme_progress SET currentWeek = :week, currentDay = :day, lastWorkoutDate = :date WHERE programmeId = :programmeId")
    suspend fun updateProgress(
        programmeId: Long,
        week: Int,
        day: Int,
        date: String,
    )

    @Query("UPDATE programme_progress SET completedWorkouts = completedWorkouts + 1 WHERE programmeId = :programmeId")
    suspend fun incrementCompletedWorkouts(programmeId: Long)

    // Complex queries with relationships
    @Transaction
    @Query("SELECT * FROM programmes WHERE id = :programmeId")
    suspend fun getProgrammeWithDetails(programmeId: Long): ProgrammeWithDetailsRaw?

    @Transaction
    @Query("SELECT * FROM programmes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProgrammeWithDetails(): ProgrammeWithDetailsRaw?

    @Transaction
    @Query("SELECT * FROM programme_weeks WHERE programmeId = :programmeId ORDER BY weekNumber ASC")
    suspend fun getWeeksWithWorkouts(programmeId: Long): List<ProgrammeWeekWithWorkoutsRaw>

    // Activation/Deactivation
    @Query("UPDATE programmes SET isActive = 0")
    suspend fun deactivateAllProgrammes()

    @Query("UPDATE programmes SET isActive = 1 WHERE id = :programmeId")
    suspend fun activateProgramme(programmeId: Long)

    @Transaction
    suspend fun setActiveProgramme(programmeId: Long) {
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
        programmeId: Long,
        completedAt: LocalDateTime,
    )


    // Statistics and analytics
    @Query("SELECT COUNT(*) FROM programmes WHERE completedAt IS NOT NULL")
    suspend fun getCompletedProgrammeCount(): Int

    @Query("SELECT COUNT(*) FROM programmes WHERE isActive = 1")
    suspend fun getActiveProgrammeCount(): Int

    @Query("SELECT AVG(adherencePercentage) FROM programme_progress WHERE adherencePercentage > 0")
    suspend fun getAverageAdherence(): Float?

    // Paginated query for completed programmes - uses status for reliability
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
        programmeId: Long,
        status: ProgrammeStatus,
        startedAt: LocalDateTime = LocalDateTime.now(),
    )
}

// Raw data classes for Room @Transaction queries
data class ProgrammeWithDetailsRaw(
    @Embedded val programme: Programme,
    @Relation(
        parentColumn = "id",
        entityColumn = "programmeId",
    )
    val progress: ProgrammeProgress?,
    @Relation(
        parentColumn = "id",
        entityColumn = "programmeId",
    )
    val substitutions: List<ExerciseSubstitution>,
)

data class ProgrammeWeekWithWorkoutsRaw(
    @Embedded val week: ProgrammeWeek,
    @Relation(
        parentColumn = "id",
        entityColumn = "weekId",
    )
    val workouts: List<ProgrammeWorkout>,
)
