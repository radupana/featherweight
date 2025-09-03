package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.logging.BugfenderLogger
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
) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val personalRecordDao = db.personalRecordDao()
    private val setLogDao = db.setLogDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val prDetectionService = PRDetectionService(personalRecordDao, setLogDao, exerciseVariationDao)

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
            val prs = prDetectionService.checkForPR(setLog, exerciseVariationId)
            
            if (prs.isNotEmpty()) {
                val exercise = exerciseVariationDao.getExerciseVariationById(exerciseVariationId)
                val exerciseName = exercise?.name ?: "Unknown"
                
                prs.forEach { pr ->
                    BugfenderLogger.logUserAction(
                        "personal_record_achieved",
                        mapOf(
                            "exercise" to exerciseName,
                            "type" to pr.recordType.name,
                            "weight" to pr.weight,
                            "reps" to pr.reps,
                            "estimated1RM" to (pr.estimated1RM ?: 0f),
                            "previousWeight" to (pr.previousWeight ?: 0f)
                        )
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
