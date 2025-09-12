package com.github.radupana.featherweight.repository

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.service.OneRMService
import com.github.radupana.featherweight.service.PRDetectionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Personal Record data
 */
class PersonalRecordRepository(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    db: FeatherweightDatabase = FeatherweightDatabase.getDatabase(application),
    prDetectionService: PRDetectionService? = null,
) {
    companion object {
        private const val TAG = "PersonalRecordRepository"
    }

    private val personalRecordDao = db.personalRecordDao()
    private val setLogDao = db.setLogDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val prService = prDetectionService ?: PRDetectionService(personalRecordDao, setLogDao, exerciseVariationDao)

    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> =
        withContext(ioDispatcher) {
            personalRecordDao.getRecentPRs(limit)
        }

    suspend fun getPersonalRecordsForWorkout(workoutId: Long): List<PersonalRecord> =
        withContext(ioDispatcher) {
            personalRecordDao.getPersonalRecordsForWorkout(workoutId)
        }

    suspend fun getPersonalRecordForExercise(exerciseVariationId: Long): PersonalRecord? =
        withContext(ioDispatcher) {
            personalRecordDao.getLatestRecordForExercise(exerciseVariationId)
        }

    suspend fun checkForPR(
        setLog: SetLog,
        exerciseVariationId: Long,
        updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit,
    ): List<PersonalRecord> =
        withContext(ioDispatcher) {
            val prs = prService.checkForPR(setLog, exerciseVariationId)

            if (prs.isNotEmpty()) {
                val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                val exerciseName = exercise?.name ?: "Unknown"

                prs.forEach { pr ->
                    Log.i(
                        TAG,
                        "Personal record achieved - exercise: $exerciseName, type: ${pr.recordType.name}, " +
                            "weight: ${pr.weight}kg, reps: ${pr.reps}, estimated 1RM: ${pr.estimated1RM ?: 0f}kg, " +
                            "previous: ${pr.previousWeight ?: 0f}kg",
                    )
                }
            }

            val oneRMPR = prs.find { it.recordType == PRType.ESTIMATED_1RM }
            if (oneRMPR != null && oneRMPR.estimated1RM != null) {
                val oneRMService = OneRMService()

                val userExerciseMax =
                    oneRMService.createOneRMRecord(
                        exerciseId = exerciseVariationId,
                        set = setLog,
                        estimate = oneRMPR.estimated1RM,
                        confidence = 1.0f, // High confidence since it's a PR
                    )

                updateOrInsertOneRM(userExerciseMax)
            }

            prs
        }
}
