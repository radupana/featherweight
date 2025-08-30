package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseRequestDao
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.programme.ExerciseSubstitution
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeInsights
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWithDetailsRaw
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.service.ProgressionService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

/**
 * Repository for managing programme-related database operations.
 * Handles programme structure, progress tracking, insights, and JSON parsing.
 */
class ProgrammeRepository(
    @Suppress("UNUSED_PARAMETER") private val db: FeatherweightDatabase,
    private val programmeDao: ProgrammeDao,
    private val workoutDao: WorkoutDao,
    private val parseRequestDao: ParseRequestDao,
    @Suppress("UNUSED_PARAMETER") private val progressionService: ProgressionService,
    @Suppress("UNUSED_PARAMETER") private val gson: Gson,
) {
    /**
     * Creates a new programme
     */
    suspend fun createProgramme(programme: Programme): Long = programmeDao.insertProgramme(programme)

    /**
     * Updates an existing programme
     */
    suspend fun updateProgramme(programme: Programme) = programmeDao.updateProgramme(programme)

    /**
     * Gets a programme by ID
     */
    suspend fun getProgrammeById(programmeId: Long): Programme? = programmeDao.getProgrammeById(programmeId)

    /**
     * Gets all programmes
     */
    suspend fun getAllProgrammes(): List<Programme> = programmeDao.getAllProgrammes()

    /**
     * Gets the active programme
     */
    suspend fun getActiveProgramme(): Programme? = programmeDao.getActiveProgramme()

    /**
     * Deletes a programme
     */
    suspend fun deleteProgramme(programme: Programme) = programmeDao.deleteProgramme(programme)

    /**
     * Gets programme weeks
     */
    suspend fun getProgrammeWeeks(programmeId: Long): List<ProgrammeWeek> = programmeDao.getWeeksForProgramme(programmeId)

    /**
     * Gets workouts for a programme week
     */
    suspend fun getWorkoutsForWeek(weekId: Long): List<ProgrammeWorkout> = programmeDao.getWorkoutsForWeek(weekId)

    /**
     * Gets or creates programme progress
     */
    suspend fun getOrCreateProgrammeProgress(programmeId: Long): ProgrammeProgress {
        val existing = programmeDao.getProgressForProgramme(programmeId)
        if (existing != null) {
            return existing
        }

        // Create new progress
        val progress =
            ProgrammeProgress(
                programmeId = programmeId,
                currentWeek = 1,
                currentDay = 1,
                completedWorkouts = 0,
                totalWorkouts = 0,
                lastWorkoutDate = null,
                adherencePercentage = 0f,
                strengthProgress = null,
            )
        programmeDao.insertOrUpdateProgress(progress)
        return progress
    }

    /**
     * Updates programme progress
     */
    suspend fun updateProgrammeProgress(
        programmeId: Long,
        week: Int,
        day: Int,
        incrementCompleted: Boolean = false,
    ) {
        withContext(Dispatchers.IO) {
            programmeDao.updateProgress(
                programmeId = programmeId,
                week = week,
                day = day,
                date = LocalDateTime.now().toString(),
            )

            if (incrementCompleted) {
                programmeDao.incrementCompletedWorkouts(programmeId)
            }
        }
    }

    /**
     * Gets programme with full details
     */
    suspend fun getProgrammeWithDetails(programmeId: Long): ProgrammeWithDetailsRaw? = programmeDao.getProgrammeWithDetails(programmeId)

    /**
     * Activates a programme
     */
    suspend fun activateProgramme(programmeId: Long) {
        withContext(Dispatchers.IO) {
            programmeDao.setActiveProgramme(programmeId)
            programmeDao.updateProgrammeStatus(
                programmeId = programmeId,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
            )
        }
    }

    /**
     * Completes a programme
     */
    suspend fun completeProgramme(programmeId: Long) {
        withContext(Dispatchers.IO) {
            programmeDao.completeProgrammeAtomic(
                programmeId = programmeId,
                completedAt = LocalDateTime.now(),
            )
        }
    }

    /**
     * Gets workouts for a programme
     */
    suspend fun getWorkoutsForProgramme(programmeId: Long): List<Workout> = workoutDao.getWorkoutsByProgramme(programmeId)

    /**
     * Gets programme completion statistics
     */
    suspend fun calculateProgrammeCompletionStats(programmeId: Long): ProgrammeCompletionStats? {
        return withContext(Dispatchers.IO) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext null
            val progress = programmeDao.getProgressForProgramme(programmeId)
            val workouts = workoutDao.getWorkoutsByProgramme(programmeId)

            val completedWorkouts = workouts.count { it.status == WorkoutStatus.COMPLETED }
            val totalScheduled = progress?.totalWorkouts ?: 0

            @Suppress("UNUSED_VARIABLE")
            val adherence =
                if (totalScheduled > 0) {
                    (completedWorkouts.toFloat() / totalScheduled) * 100
                } else {
                    0f
                }

            ProgrammeCompletionStats(
                programmeId = programmeId,
                programmeName = programme.name,
                startDate = programme.startedAt ?: LocalDateTime.now(),
                endDate = programme.completedAt ?: LocalDateTime.now(),
                totalWorkouts = totalScheduled,
                completedWorkouts = completedWorkouts,
                totalVolume = 0f, // Would need to calculate from sets
                averageWorkoutDuration = Duration.ZERO, // Would need to calculate from workouts
                totalPRs = 0, // Would need to calculate from PRs
                strengthImprovements = emptyList(), // Would need to calculate from PRs
                averageStrengthImprovement = 0f,
                insights =
                    ProgrammeInsights(
                        totalTrainingDays = completedWorkouts,
                        mostConsistentDay = null,
                        averageRestDaysBetweenWorkouts = 0f,
                    ),
            )
        }
    }

    /**
     * Gets completed programmes with pagination
     */
    suspend fun getCompletedProgrammesPaged(
        limit: Int,
        offset: Int,
    ): List<Programme> = programmeDao.getCompletedProgrammesPaged(limit, offset)

    /**
     * Creates exercise substitution
     */
    suspend fun createSubstitution(substitution: ExerciseSubstitution): Long = programmeDao.insertSubstitution(substitution)

    /**
     * Gets substitutions for a programme
     */
    suspend fun getSubstitutionsForProgramme(programmeId: Long): List<ExerciseSubstitution> = programmeDao.getSubstitutionsForProgramme(programmeId)

    /**
     * Creates a parse request
     */
    suspend fun createParseRequest(rawText: String): Long {
        val request =
            ParseRequest(
                rawText = rawText,
                status = ParseStatus.PROCESSING,
                createdAt = LocalDateTime.now(),
            )
        return parseRequestDao.insert(request)
    }

    /**
     * Updates a parse request
     */
    suspend fun updateParseRequest(request: ParseRequest) = parseRequestDao.update(request)

    /**
     * Gets a parse request
     */
    suspend fun getParseRequest(id: Long): ParseRequest? = parseRequestDao.getRequest(id)

    /**
     * Gets all parse requests
     */
    fun getAllParseRequests(): Flow<List<ParseRequest>> = parseRequestDao.getAllRequests()

    /**
     * Deletes a parse request
     */
    suspend fun deleteParseRequest(request: ParseRequest) = parseRequestDao.delete(request)

    /**
     * Gets average adherence across all programmes
     */
    suspend fun getAverageAdherence(): Float? = programmeDao.getAverageAdherence()

    /**
     * Gets count of completed programmes
     */
    suspend fun getCompletedProgrammeCount(): Int = programmeDao.getCompletedProgrammeCount()

    /**
     * Gets count of active programmes
     */
    suspend fun getActiveProgrammeCount(): Int = programmeDao.getActiveProgrammeCount()
}
