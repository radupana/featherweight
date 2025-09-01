package com.github.radupana.featherweight.repository

import android.app.Application
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.service.OneRMService
import com.github.radupana.featherweight.service.PRDetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Personal Record data
 */
class PersonalRecordRepository(application: Application) {
    private val db = FeatherweightDatabase.getDatabase(application)
    private val personalRecordDao = db.personalRecordDao()
    private val setLogDao = db.setLogDao()
    private val exerciseVariationDao = db.exerciseVariationDao()
    private val prDetectionService = PRDetectionService(personalRecordDao, setLogDao, exerciseVariationDao)
    
    suspend fun getRecentPRs(limit: Int = 10): List<PersonalRecord> = 
        withContext(Dispatchers.IO) {
            personalRecordDao.getRecentPRs(limit)
        }
    
    suspend fun getPersonalRecordsForWorkout(workoutId: Long): List<PersonalRecord> = 
        withContext(Dispatchers.IO) {
            personalRecordDao.getPersonalRecordsForWorkout(workoutId)
        }
    
    suspend fun getPersonalRecordForExercise(exerciseVariationId: Long): PersonalRecord? = 
        withContext(Dispatchers.IO) {
            personalRecordDao.getLatestRecordForExercise(exerciseVariationId)
        }
    
    suspend fun checkForPR(
        setLog: SetLog,
        exerciseVariationId: Long,
        updateOrInsertOneRM: suspend (UserExerciseMax) -> Unit
    ): List<PersonalRecord> =
        withContext(Dispatchers.IO) {
            val prs = prDetectionService.checkForPR(setLog, exerciseVariationId)

            // If we detected an estimated 1RM PR, update the UserExerciseMax table
            val oneRMPR = prs.find { it.recordType == PRType.ESTIMATED_1RM }
            if (oneRMPR != null && oneRMPR.estimated1RM != null) {
                val oneRMService = OneRMService()

                // Create UserExerciseMax record from the PR
                val userExerciseMax =
                    oneRMService.createOneRMRecord(
                        exerciseId = exerciseVariationId,
                        set = setLog,
                        estimate = oneRMPR.estimated1RM,
                        confidence = 1.0f, // High confidence since it's a PR
                    )

                // Update or insert the 1RM record
                updateOrInsertOneRM(userExerciseMax)
            }

            prs
        }
}
