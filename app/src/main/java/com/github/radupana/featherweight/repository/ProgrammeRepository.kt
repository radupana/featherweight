package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Repository for managing Programme-related data
 */
class ProgrammeRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val programmeDao = db.programmeDao()
    private val workoutDao = db.workoutDao()

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






    suspend fun getProgrammeWorkoutProgress(programmeId: Long): Pair<Int, Int> =
        withContext(ioDispatcher) {
            val programme = programmeDao.getProgrammeById(programmeId)
            if (programme == null) {
                return@withContext Pair(0, 0)
            }

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








    suspend fun updateProgrammeCompletionNotes(
        programmeId: Long,
        notes: String?,
    ) = withContext(ioDispatcher) {
        val programme = programmeDao.getProgrammeById(programmeId)
        if (programme != null) {
            programmeDao.updateProgramme(
                programme.copy(completionNotes = notes),
            )
        }
    }
}
