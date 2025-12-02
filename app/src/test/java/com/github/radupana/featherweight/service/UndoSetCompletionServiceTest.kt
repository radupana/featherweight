package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class UndoSetCompletionServiceTest {
    private lateinit var service: UndoSetCompletionService
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var exerciseMaxTrackingDao: ExerciseMaxTrackingDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var firestoreRepository: FirestoreRepository

    private val testUserId = "test-user-123"
    private val testExerciseId = "exercise-1"
    private val testSetId = "set-1"

    @Before
    fun setUp() {
        personalRecordDao = mockk(relaxed = true)
        exerciseMaxTrackingDao = mockk(relaxed = true)
        userExerciseUsageDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        service =
            UndoSetCompletionService(
                personalRecordDao,
                exerciseMaxTrackingDao,
                userExerciseUsageDao,
                setLogDao,
                firestoreRepository,
            )
    }

    @Test
    fun `undoSetCompletion deletes PRs when no other completed sets exist`() =
        runTest {
            val pr = createPR(testSetId, 100f)
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    emptyList(),
                )

            assertThat(result.prsDeleted).isEqualTo(1)
            coVerify { personalRecordDao.deletePR(pr.id) }
        }

    @Test
    fun `undoSetCompletion does not delete PR when superseded by higher PR`() =
        runTest {
            val pr1 = createPR(testSetId, 100f)
            val pr2 = createPR("set-2", 110f)
            val remainingSet = createSetLog("set-2")

            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr1)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns listOf(pr2)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    listOf(remainingSet),
                )

            assertThat(result.prsDeleted).isEqualTo(0)
            coVerify(exactly = 0) { personalRecordDao.deletePR(pr1.id) }
        }

    @Test
    fun `undoSetCompletion decrements usage when triggered and no other sets remain`() =
        runTest {
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns true
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    emptyList(),
                )

            assertThat(result.usageDecremented).isTrue()
            coVerify { userExerciseUsageDao.decrementUsageCount(testUserId, testExerciseId, any()) }
        }

    @Test
    fun `undoSetCompletion does not decrement usage when other sets remain`() =
        runTest {
            val remainingSet = createSetLog("set-2")
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns true
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    listOf(remainingSet),
                )

            assertThat(result.usageDecremented).isFalse()
            coVerify(exactly = 0) { userExerciseUsageDao.decrementUsageCount(any(), any(), any()) }
        }

    @Test
    fun `undoSetCompletion deletes 1RM when no other sets remain`() =
        runTest {
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns 95f
            coEvery { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) } returns 1

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    emptyList(),
                )

            assertThat(result.oneRMRestored).isTrue()
            coVerify { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) }
        }

    @Test
    fun `undoSetCompletion does not delete 1RM when other sets remain`() =
        runTest {
            val remainingSet = createSetLog("set-2")
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns 95f

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    listOf(remainingSet),
                )

            assertThat(result.oneRMRestored).isFalse()
            coVerify(exactly = 0) { exerciseMaxTrackingDao.deleteBySourceSetId(any()) }
        }

    @Test
    fun `undoSetCompletion clears tracking fields`() =
        runTest {
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            service.undoSetCompletion(
                testSetId,
                testExerciseId,
                testUserId,
                emptyList(),
            )

            coVerify {
                setLogDao.updateCompletionTracking(testSetId, triggered = false, previous1RM = null)
            }
        }

    @Test
    fun `previewUndo returns correct PR count when PRs will be deleted`() =
        runTest {
            val pr = createPR(testSetId, 100f)
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val preview = service.previewUndo(testSetId, emptyList())

            assertThat(preview.prCount).isEqualTo(1)
        }

    @Test
    fun `previewUndo returns zero PR count when superseded`() =
        runTest {
            val pr1 = createPR(testSetId, 100f)
            val pr2 = createPR("set-2", 110f)
            val remainingSet = createSetLog("set-2")

            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr1)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns listOf(pr2)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val preview = service.previewUndo(testSetId, listOf(remainingSet))

            assertThat(preview.prCount).isEqualTo(0)
        }

    @Test
    fun `undoSetCompletion handles multiple PRs correctly`() =
        runTest {
            val weightPR = createPR(testSetId, 100f, PRType.WEIGHT)
            val estimated1RMPR = createPR(testSetId, 100f, PRType.ESTIMATED_1RM, estimated1RM = 120f)
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns
                listOf(
                    weightPR,
                    estimated1RMPR,
                )
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    emptyList(),
                )

            assertThat(result.prsDeleted).isEqualTo(2)
            coVerify { personalRecordDao.deletePR(weightPR.id) }
            coVerify { personalRecordDao.deletePR(estimated1RMPR.id) }
        }

    @Test
    fun `undoSetCompletion with three sets where middle set unchecked does not rollback PR`() =
        runTest {
            val pr1 = createPR("set-1", 100f)
            val pr2 = createPR("set-2", 105f)
            val pr3 = createPR("set-3", 110f)
            val set1 = createSetLog("set-1")
            val set3 = createSetLog("set-3")

            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns listOf(pr2)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-1") } returns listOf(pr1)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-3") } returns listOf(pr3)
            coEvery { setLogDao.didSetTriggerUsageIncrement("set-2") } returns false
            coEvery { setLogDao.getPrevious1RMEstimate("set-2") } returns 95f

            val result =
                service.undoSetCompletion(
                    "set-2",
                    testExerciseId,
                    testUserId,
                    listOf(set1, set3),
                )

            assertThat(result.prsDeleted).isEqualTo(0)
            coVerify(exactly = 0) { personalRecordDao.deletePR(any()) }
        }

    @Test
    fun `undoSetCompletion with three sets where highest PR set unchecked rolls back`() =
        runTest {
            val pr1 = createPR("set-1", 100f)
            val pr2 = createPR("set-2", 105f)
            val pr3 = createPR("set-3", 110f)
            val set1 = createSetLog("set-1")
            val set2 = createSetLog("set-2")

            coEvery { personalRecordDao.getPRsBySourceSetId("set-3") } returns listOf(pr3)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-1") } returns listOf(pr1)
            coEvery { personalRecordDao.getPRsBySourceSetId("set-2") } returns listOf(pr2)
            coEvery { setLogDao.didSetTriggerUsageIncrement("set-3") } returns false
            coEvery { setLogDao.getPrevious1RMEstimate("set-3") } returns 100f

            val result =
                service.undoSetCompletion(
                    "set-3",
                    testExerciseId,
                    testUserId,
                    listOf(set1, set2),
                )

            assertThat(result.prsDeleted).isEqualTo(1)
            coVerify { personalRecordDao.deletePR(pr3.id) }
        }

    @Test
    fun `undoSetCompletion deletes first-time 1RM when no previous exists`() =
        runTest {
            // This is the key test: when it's the first 1RM ever recorded (no previous1RM),
            // and there are no remaining sets, we should still delete the 1RM record
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null // No previous 1RM!
            coEvery { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) } returns 1 // But a record was created

            val result =
                service.undoSetCompletion(
                    testSetId,
                    testExerciseId,
                    testUserId,
                    emptyList(),
                )

            // The 1RM should be deleted even though there was no previous 1RM
            assertThat(result.oneRMRestored).isTrue()
            coVerify { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) }
        }

    @Test
    fun `undoSetCompletion syncs 1RM deletion to Firestore for authenticated user`() =
        runTest {
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null
            coEvery { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) } returns 1
            coEvery {
                firestoreRepository.deleteExerciseMaxBySourceSetId(testUserId, testExerciseId, testSetId)
            } returns Result.success(Unit)

            service.undoSetCompletion(
                testSetId,
                testExerciseId,
                testUserId,
                emptyList(),
            )

            coVerify {
                firestoreRepository.deleteExerciseMaxBySourceSetId(testUserId, testExerciseId, testSetId)
            }
        }

    @Test
    fun `undoSetCompletion does not sync 1RM deletion to Firestore for local user`() =
        runTest {
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns emptyList()
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null
            coEvery { exerciseMaxTrackingDao.deleteBySourceSetId(testSetId) } returns 1

            service.undoSetCompletion(
                testSetId,
                testExerciseId,
                "local", // Local user
                emptyList(),
            )

            coVerify(exactly = 0) {
                firestoreRepository.deleteExerciseMaxBySourceSetId(any(), any(), any())
            }
        }

    @Test
    fun `undoSetCompletion syncs PR deletion to Firestore for authenticated user`() =
        runTest {
            val pr = createPR(testSetId, 100f)
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null
            coEvery { firestoreRepository.deletePersonalRecord(testUserId, pr.id) } returns Result.success(Unit)

            service.undoSetCompletion(
                testSetId,
                testExerciseId,
                testUserId,
                emptyList(),
            )

            coVerify { firestoreRepository.deletePersonalRecord(testUserId, pr.id) }
        }

    @Test
    fun `undoSetCompletion does not sync PR deletion to Firestore for local user`() =
        runTest {
            val pr = createPR(testSetId, 100f)
            coEvery { personalRecordDao.getPRsBySourceSetId(testSetId) } returns listOf(pr)
            coEvery { setLogDao.didSetTriggerUsageIncrement(testSetId) } returns false
            coEvery { setLogDao.getPrevious1RMEstimate(testSetId) } returns null

            service.undoSetCompletion(
                testSetId,
                testExerciseId,
                "local", // Local user
                emptyList(),
            )

            coVerify(exactly = 0) { firestoreRepository.deletePersonalRecord(any(), any()) }
        }

    private fun createPR(
        sourceSetId: String,
        weight: Float,
        recordType: PRType = PRType.WEIGHT,
        estimated1RM: Float? = null,
    ) = PersonalRecord(
        id = "pr-$sourceSetId",
        userId = testUserId,
        exerciseId = testExerciseId,
        weight = weight,
        reps = 5,
        recordDate = LocalDateTime.now(),
        previousWeight = null,
        previousReps = null,
        previousDate = null,
        improvementPercentage = 10f,
        recordType = recordType,
        sourceSetId = sourceSetId,
        estimated1RM = estimated1RM,
    )

    private fun createSetLog(id: String) =
        SetLog(
            id = id,
            userId = testUserId,
            exerciseLogId = "exercise-log-1",
            setOrder = 1,
            actualReps = 5,
            actualWeight = 100f,
            isCompleted = true,
        )
}
