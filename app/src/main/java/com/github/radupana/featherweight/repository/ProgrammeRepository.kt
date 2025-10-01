package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.util.ExceptionLogger
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

    suspend fun getProgrammeById(programmeId: String) =
        withContext(ioDispatcher) {
            programmeDao.getProgrammeById(programmeId)
        }

    suspend fun getProgrammeWithDetails(programmeId: String) =
        withContext(ioDispatcher) {
            val trace = safeNewTrace("programme_load_full")
            trace?.start()
            val result = programmeDao.getProgrammeWithDetails(programmeId)
            trace?.putAttribute("programme_id", programmeId.toString())
            trace?.stop()
            result
        }

    // Suppress TooGenericExceptionCaught: This is a safe wrapper that must handle ALL exceptions
    // from Firebase Performance initialization, including RuntimeException from unmocked Android
    // methods in test environments. The method is explicitly designed to never throw.
    @Suppress("TooGenericExceptionCaught")
    private fun safeNewTrace(name: String): Trace? =
        try {
            FirebasePerformance.getInstance().newTrace(name)
        } catch (e: Throwable) {
            ExceptionLogger.logNonCritical(TAG, "Firebase Performance not available: ${e.javaClass.simpleName}", e)
            null
        }

    suspend fun getInProgressWorkoutCountByProgramme(programmeId: String): Int =
        withContext(ioDispatcher) {
            workoutDao.getInProgressWorkoutCountByProgramme(programmeId)
        }

    suspend fun updateProgrammeCompletionNotes(
        programmeId: String,
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
