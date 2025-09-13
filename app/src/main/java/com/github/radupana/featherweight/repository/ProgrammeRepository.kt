package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Programme-related data
 */
class ProgrammeRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
) {
    companion object {
        private const val TAG = "ProgrammeRepository"
    }

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
            val trace = safeNewTrace("programme_load_full")
            trace?.start()
            val result = programmeDao.getProgrammeWithDetails(programmeId)
            trace?.putAttribute("programme_id", programmeId.toString())
            trace?.stop()
            result
        }

    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Firebase Performance not available - likely in test environment")
            null
        } catch (e: RuntimeException) {
            Log.d(TAG, "Firebase Performance not available - likely in test environment")
            null
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
