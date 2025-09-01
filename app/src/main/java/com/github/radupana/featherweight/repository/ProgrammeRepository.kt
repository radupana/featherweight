package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats
import com.github.radupana.featherweight.data.programme.ProgrammeInsights
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.data.programme.StrengthImprovement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository for managing Programme-related data
 */
class ProgrammeRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val programmeDao = db.programmeDao()
    private val workoutDao = db.workoutDao()
    
    // Basic Programme CRUD operations
    suspend fun getAllProgrammes() = 
        withContext(ioDispatcher) {
            programmeDao.getAllProgrammes()
        }
    
    suspend fun getActiveProgramme() = 
        withContext(ioDispatcher) {
            programmeDao.getActiveProgramme()
        }
    
    suspend fun getProgrammeById(programmeId: Long) = 
        withContext(ioDispatcher) {
            programmeDao.getProgrammeById(programmeId)
        }
    
    suspend fun getProgrammeWithDetails(programmeId: Long) = 
        withContext(ioDispatcher) {
            programmeDao.getProgrammeWithDetails(programmeId)
        }
    
    suspend fun insertProgramme(programme: Programme): Long = 
        withContext(ioDispatcher) {
            programmeDao.insertProgramme(programme)
        }
    
    suspend fun updateProgramme(programme: Programme) = 
        withContext(ioDispatcher) {
            programmeDao.updateProgramme(programme)
        }
    
    suspend fun deleteProgramme(programme: Programme) = 
        withContext(ioDispatcher) {
            programmeDao.deleteProgramme(programme)
        }
    
    suspend fun activateProgramme(programmeId: Long) = 
        withContext(ioDispatcher) {
            // Deactivate all other programmes
            programmeDao.deactivateAllProgrammes()
            
            // Activate the selected programme
            programmeDao.updateProgrammeStatus(programmeId, ProgrammeStatus.IN_PROGRESS)
            
            // Update the startedAt date if not already set
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme != null && programme.startedAt == null) {
                programmeDao.updateProgramme(
                    programme.copy(
                        startedAt = LocalDateTime.now(),
                        status = ProgrammeStatus.IN_PROGRESS
                    )
                )
            }
        }
    
    suspend fun deactivateProgramme(programmeId: Long) = 
        withContext(ioDispatcher) {
            programmeDao.updateProgrammeStatus(programmeId, ProgrammeStatus.NOT_STARTED)
        }
    
    // Programme Progress tracking
    suspend fun getProgrammeWorkoutProgress(programmeId: Long): Pair<Int, Int> = 
        withContext(ioDispatcher) {
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme == null) {
                return@withContext Pair(0, 0)
            }
            
            // Get total workouts from structure or database
            val totalWorkouts = workoutDao.getTotalProgrammeWorkoutCount(programmeId)
            val completedWorkouts = workoutDao.getCompletedProgrammeWorkoutCount(programmeId)
            
            Pair(completedWorkouts, totalWorkouts)
        }
    
    suspend fun getWorkoutsByProgramme(programmeId: Long): List<Workout> =
        withContext(ioDispatcher) {
            workoutDao.getWorkoutsByProgramme(programmeId)
        }
    
    suspend fun getInProgressWorkoutCountByProgramme(programmeId: Long): Int =
        withContext(ioDispatcher) {
            workoutDao.getInProgressWorkoutCountByProgramme(programmeId)
        }
    
    suspend fun updateProgrammeProgressAfterWorkout(programmeId: Long) = 
        withContext(ioDispatcher) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext
            
            // Get total and completed workouts
            val (completedCount, totalCount) = getProgrammeWorkoutProgress(programmeId)
            
            // Check if programme is complete
            if (completedCount >= totalCount && totalCount > 0) {
                completeProgramme(programmeId)
            }
        }
    
    private suspend fun completeProgramme(programmeId: Long) = 
        withContext(ioDispatcher) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext
            
            programmeDao.updateProgramme(
                programme.copy(
                    status = ProgrammeStatus.COMPLETED,
                    completedAt = LocalDateTime.now()
                )
            )
        }
    
    // Programme Weeks
    suspend fun insertProgrammeWeek(week: ProgrammeWeek): Long = 
        withContext(ioDispatcher) {
            programmeDao.insertProgrammeWeek(week)
        }
    
    suspend fun getProgrammeWeeks(@Suppress("UNUSED_PARAMETER") programmeId: Long): List<ProgrammeWeek> = 
        withContext(ioDispatcher) {
            // Programme weeks not yet implemented in DAO
            emptyList()
        }
    
    // Programme Workouts
    suspend fun insertProgrammeWorkout(workout: ProgrammeWorkout): Long = 
        withContext(ioDispatcher) {
            programmeDao.insertProgrammeWorkout(workout)
        }
    
    suspend fun getProgrammeWorkoutsForWeek(@Suppress("UNUSED_PARAMETER") weekId: Long): List<ProgrammeWorkout> = 
        withContext(ioDispatcher) {
            // Programme workouts for week not yet implemented in DAO  
            emptyList()
        }
    
    // Programme Completion Stats
    suspend fun calculateProgrammeCompletionStats(programmeId: Long): ProgrammeCompletionStats? =
        withContext(ioDispatcher) {
            val programme = programmeDao.getProgrammeById(programmeId) ?: return@withContext null
            
            // Get all workouts for this programme
            val workouts = workoutDao.getWorkoutsByProgramme(programmeId)
            val completedWorkouts = workouts.filter { it.status == WorkoutStatus.COMPLETED }
            
            // Get total workouts from database
            val totalWorkouts = workoutDao.getTotalProgrammeWorkoutCount(programmeId)
            val completedCount = completedWorkouts.size
            if (totalWorkouts > 0) {
                (completedCount.toFloat() / totalWorkouts * 100).toInt()
            } else 0
            
            // Calculate duration
            val startDate = programme.startedAt?.toLocalDate()
            val endDate = programme.completedAt?.toLocalDate() ?: LocalDate.now()
            if (startDate != null) {
                java.time.temporal.ChronoUnit.WEEKS.between(startDate, endDate).toInt()
            } else 0
            
            // Calculate total volume
            val totalVolume = workouts.sumOf { workout ->
                // This would need to be calculated from sets - simplified for now
                0.0
            }.toFloat()
            
            // Calculate strength improvements (simplified)
            val strengthImprovements = emptyList<StrengthImprovement>()
            
            ProgrammeCompletionStats(
                programmeId = programmeId,
                programmeName = programme.name,
                startDate = programme.startedAt ?: programme.createdAt,
                endDate = programme.completedAt ?: LocalDateTime.now(),
                totalWorkouts = totalWorkouts,
                completedWorkouts = completedCount,
                totalVolume = totalVolume,
                averageWorkoutDuration = Duration.ZERO, // Would need to calculate from workout durations
                totalPRs = 0, // Would need to calculate from PR records
                strengthImprovements = strengthImprovements,
                averageStrengthImprovement = 0f, // Would need to calculate
                insights = ProgrammeInsights(
                    totalTrainingDays = completedCount,
                    mostConsistentDay = null,
                    averageRestDaysBetweenWorkouts = 0f
                ) // Would need to calculate insights
            )
        }
    
    suspend fun updateProgrammeCompletionNotes(
        programmeId: Long,
        notes: String?
    ) = withContext(ioDispatcher) {
        val programme = programmeDao.getProgrammeById(programmeId)
        if (programme != null) {
            programmeDao.updateProgramme(
                programme.copy(completionNotes = notes)
            )
        }
    }
}
