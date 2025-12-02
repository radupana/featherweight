package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger
import java.time.LocalDateTime

class UndoSetCompletionService(
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseMaxTrackingDao: ExerciseMaxTrackingDao,
    private val userExerciseUsageDao: UserExerciseUsageDao,
    private val setLogDao: SetLogDao,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
) {
    data class UndoPreview(
        val prCount: Int,
        val will1RMChange: Boolean,
        val willUsageDecrement: Boolean,
    )

    data class UndoResult(
        val prsDeleted: Int,
        val oneRMRestored: Boolean,
        val usageDecremented: Boolean,
    )

    suspend fun previewUndo(
        setId: String,
        remainingCompletedSets: List<SetLog>,
    ): UndoPreview {
        val prsFromThisSet = personalRecordDao.getPRsBySourceSetId(setId)

        val willPRsBeDeleted =
            prsFromThisSet.any { pr ->
                val supersedingPR =
                    remainingCompletedSets.any { otherSet ->
                        val otherPRs = personalRecordDao.getPRsBySourceSetId(otherSet.id)
                        otherPRs.any { otherPR ->
                            otherPR.recordType == pr.recordType &&
                                otherPR.exerciseId == pr.exerciseId &&
                                (
                                    otherPR.weight > pr.weight ||
                                        (otherPR.estimated1RM ?: 0f) > (pr.estimated1RM ?: 0f)
                                )
                        }
                    }
                !supersedingPR
            }

        val triggeredUsage = setLogDao.didSetTriggerUsageIncrement(setId) ?: false
        val willUsageDecrement = triggeredUsage && remainingCompletedSets.isEmpty()

        // Check if this set created a 1RM record (either new or updated)
        val createdMax = exerciseMaxTrackingDao.getBySourceSetId(setId)
        val will1RMChange = createdMax != null && remainingCompletedSets.isEmpty()

        return UndoPreview(
            prCount = if (willPRsBeDeleted) prsFromThisSet.size else 0,
            will1RMChange = will1RMChange,
            willUsageDecrement = willUsageDecrement,
        )
    }

    suspend fun undoSetCompletion(
        setId: String,
        exerciseId: String,
        userId: String,
        remainingCompletedSets: List<SetLog>,
    ): UndoResult {
        CloudLogger.info(
            TAG,
            "undoSetCompletion called - setId: $setId, exerciseId: $exerciseId, " +
                "remainingCompletedSets: ${remainingCompletedSets.size}",
        )

        var prsDeleted = 0
        var oneRMRestored = false
        var usageDecremented = false

        val prsFromThisSet = personalRecordDao.getPRsBySourceSetId(setId)
        CloudLogger.debug(TAG, "Found ${prsFromThisSet.size} PRs from set $setId")
        for (pr in prsFromThisSet) {
            val isSuperseded =
                remainingCompletedSets.any { otherSet ->
                    val otherPRs = personalRecordDao.getPRsBySourceSetId(otherSet.id)
                    otherPRs.any { otherPR ->
                        otherPR.recordType == pr.recordType &&
                            otherPR.exerciseId == pr.exerciseId &&
                            (
                                otherPR.weight > pr.weight ||
                                    (otherPR.estimated1RM ?: 0f) > (pr.estimated1RM ?: 0f)
                            )
                    }
                }

            if (!isSuperseded) {
                // Delete from Room first
                personalRecordDao.deletePR(pr.id)
                prsDeleted++
                CloudLogger.info(TAG, "Deleted PR ${pr.id} from Room (not superseded)")

                // Then sync deletion to Firestore
                if (userId != "local") {
                    val result = firestoreRepository.deletePersonalRecord(userId, pr.id)
                    if (result.isSuccess) {
                        CloudLogger.info(TAG, "Deleted PR ${pr.id} from Firestore")
                    } else {
                        CloudLogger.error(TAG, "Failed to delete PR ${pr.id} from Firestore", result.exceptionOrNull())
                    }
                }
            } else {
                CloudLogger.info(TAG, "PR ${pr.id} superseded by later set, not deleting")
            }
        }

        val previous1RM = setLogDao.getPrevious1RMEstimate(setId)
        if (remainingCompletedSets.isEmpty()) {
            // Delete the 1RM record created by this set (if any)
            // This works regardless of whether there was a previous 1RM
            val existingMax = exerciseMaxTrackingDao.getBySourceSetId(setId)
            CloudLogger.debug(
                TAG,
                "Checking for 1RM to delete - setId: $setId, existingMax: ${existingMax?.id}, " +
                    "existingSourceSetId: ${existingMax?.sourceSetId}",
            )
            val deleted = exerciseMaxTrackingDao.deleteBySourceSetId(setId)
            if (deleted > 0) {
                oneRMRestored = true
                CloudLogger.info(TAG, "Deleted 1RM record for exercise $exerciseId (previous1RM was: $previous1RM)")

                // Sync deletion to Firestore
                if (userId != "local") {
                    val result = firestoreRepository.deleteExerciseMaxBySourceSetId(userId, exerciseId, setId)
                    if (result.isSuccess) {
                        CloudLogger.info(TAG, "Deleted 1RM from Firestore for exercise $exerciseId")
                    } else {
                        CloudLogger.error(TAG, "Failed to delete 1RM from Firestore", result.exceptionOrNull())
                    }
                }
            }
        } else if (previous1RM != null) {
            CloudLogger.info(TAG, "1RM not rolled back - other completed sets exist")
        }

        val triggeredUsage = setLogDao.didSetTriggerUsageIncrement(setId) ?: false
        if (triggeredUsage && remainingCompletedSets.isEmpty()) {
            userExerciseUsageDao.decrementUsageCount(userId, exerciseId, LocalDateTime.now())
            usageDecremented = true
            CloudLogger.info(TAG, "Decremented usage count for exercise $exerciseId")
        } else if (triggeredUsage) {
            CloudLogger.info(TAG, "Usage count not decremented - other completed sets exist")
        }

        setLogDao.updateCompletionTracking(setId, triggered = false, previous1RM = null)

        return UndoResult(prsDeleted, oneRMRestored, usageDecremented)
    }

    companion object {
        private const val TAG = "UndoSetCompletionService"
    }
}
