package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for GlobalExerciseProgressDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - Query operations with filtering
 * - Progress tracking queries
 * - User-based operations
 * - Edge cases for progress data
 */
@RunWith(RobolectricTestRunner::class)
class GlobalExerciseProgressDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a test exercise.
     * Note: Foreign key constraint was removed, but we still create exercises for realism.
     */
    private suspend fun createExercise(
        id: String = "test-exercise",
        name: String = "Test Exercise",
        type: String = ExerciseType.SYSTEM.name,
    ): Exercise {
        val exercise =
            Exercise(
                id = id,
                type = type,
                userId = if (type == ExerciseType.USER.name) "test-user" else null,
                name = name,
                category = "STRENGTH",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                isCompound = true,
                requiresWeight = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    /**
     * Creates a test GlobalExerciseProgress.
     */
    private fun createProgress(
        exerciseId: String,
        userId: String? = "test-user",
        currentWorkingWeight: Float = 100f,
        estimatedMax: Float = 120f,
        lastUpdated: LocalDateTime = LocalDateTime.now(),
        trend: ProgressTrend = ProgressTrend.IMPROVING,
    ): GlobalExerciseProgress =
        GlobalExerciseProgress(
            userId = userId,
            exerciseId = exerciseId,
            currentWorkingWeight = currentWorkingWeight,
            estimatedMax = estimatedMax,
            lastUpdated = lastUpdated,
            recentAvgRpe = 7.5f,
            consecutiveStalls = 0,
            lastPrDate = LocalDateTime.now().minusDays(7),
            lastPrWeight = 115f,
            trend = trend,
            volumeTrend = VolumeTrend.INCREASING,
            totalVolumeLast30Days = 5000f,
            sessionsTracked = 10,
            bestSingleRep = 125f,
            best3Rep = 115f,
            best5Rep = 110f,
            best8Rep = 100f,
            lastSessionVolume = 3000f,
            avgSessionVolume = 2800f,
            weeksAtCurrentWeight = 2,
            lastProgressionDate = LocalDateTime.now().minusDays(14),
            failureStreak = 0,
            lastProgrammeWorkoutId = null,
            lastFreestyleWorkoutId = "freestyle-workout-1",
        )

    // CRUD Operations Tests

    @Test
    fun `insertOrUpdate should insert new progress`() =
        runTest {
            val exercise = createExercise()
            val progress = createProgress(exercise.id)

            globalExerciseProgressDao.insertOrUpdate(progress)

            val retrieved = globalExerciseProgressDao.getProgressById(progress.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.exerciseId).isEqualTo(exercise.id)
            assertThat(retrieved?.currentWorkingWeight).isEqualTo(100f)
            assertThat(retrieved?.estimatedMax).isEqualTo(120f)
        }

    @Test
    fun `insertOrUpdate should update existing progress`() =
        runTest {
            val exercise = createExercise()
            val progress = createProgress(exercise.id, currentWorkingWeight = 100f)

            globalExerciseProgressDao.insertOrUpdate(progress)

            // Update with same ID
            val updated = progress.copy(currentWorkingWeight = 110f, estimatedMax = 130f)
            globalExerciseProgressDao.insertOrUpdate(updated)

            val retrieved = globalExerciseProgressDao.getProgressById(progress.id)
            assertThat(retrieved?.currentWorkingWeight).isEqualTo(110f)
            assertThat(retrieved?.estimatedMax).isEqualTo(130f)
        }

    @Test
    fun `insertProgress should add progress to database`() =
        runTest {
            val exercise = createExercise()
            val progress = createProgress(exercise.id)

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressById(progress.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(progress.id)
        }

    @Test
    fun `getProgressById should return specific progress entry`() =
        runTest {
            val exercise = createExercise()
            val progress = createProgress(exercise.id)

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressById(progress.id)

            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.id).isEqualTo(progress.id)
            assertThat(retrieved?.exerciseId).isEqualTo(exercise.id)
        }

    @Test
    fun `getProgressById should return null for non-existent id`() =
        runTest {
            val retrieved = globalExerciseProgressDao.getProgressById("non-existent-id")

            assertThat(retrieved).isNull()
        }

    // Query Operations Tests

    @Test
    fun `getProgressForExercise should return progress for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Squat")

            globalExerciseProgressDao.insertProgress(createProgress(exercise1.id))
            globalExerciseProgressDao.insertProgress(createProgress(exercise2.id))

            val progress1 = globalExerciseProgressDao.getProgressForExercise(exercise1.id)
            val progress2 = globalExerciseProgressDao.getProgressForExercise(exercise2.id)

            assertThat(progress1).isNotNull()
            assertThat(progress2).isNotNull()
            assertThat(progress1?.exerciseId).isEqualTo(exercise1.id)
            assertThat(progress2?.exerciseId).isEqualTo(exercise2.id)
        }

    @Test
    fun `getProgressForExercise should return null for exercise with no progress`() =
        runTest {
            val exercise = createExercise()

            val progress = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(progress).isNull()
        }

    @Test
    fun `getProgressForExercise should return only one entry per exercise`() =
        runTest {
            val exercise = createExercise()

            // Insert multiple progress entries for same exercise (via REPLACE)
            val progress1 = createProgress(exercise.id, currentWorkingWeight = 100f)
            val progress2 = createProgress(exercise.id, currentWorkingWeight = 110f)

            globalExerciseProgressDao.insertOrUpdate(progress1)
            globalExerciseProgressDao.insertOrUpdate(progress2)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            // Should only return one entry (query has LIMIT 1)
            assertThat(retrieved).isNotNull()
        }

    @Test
    fun `getAllProgress should return all progress entries`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")

            globalExerciseProgressDao.insertProgress(createProgress(exercise1.id))
            globalExerciseProgressDao.insertProgress(createProgress(exercise2.id))
            globalExerciseProgressDao.insertProgress(createProgress(exercise3.id))

            val allProgress = globalExerciseProgressDao.getAllProgress()

            assertThat(allProgress).hasSize(3)
            assertThat(allProgress.map { it.exerciseId }).containsExactly(
                exercise1.id,
                exercise2.id,
                exercise3.id,
            )
        }

    @Test
    fun `getAllProgress should return empty list when no progress exists`() =
        runTest {
            val allProgress = globalExerciseProgressDao.getAllProgress()

            assertThat(allProgress).isEmpty()
        }

    // Deletion Operations Tests

    @Test
    fun `deleteAllGlobalProgress should remove all progress entries`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            globalExerciseProgressDao.insertProgress(createProgress(exercise1.id))
            globalExerciseProgressDao.insertProgress(createProgress(exercise2.id))

            globalExerciseProgressDao.deleteAllGlobalProgress()

            val allProgress = globalExerciseProgressDao.getAllProgress()
            assertThat(allProgress).isEmpty()
        }

    @Test
    fun `deleteAllForUser should remove progress for specific user only`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            globalExerciseProgressDao.insertProgress(
                createProgress(exercise1.id, userId = "user-1"),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise2.id, userId = "user-2"),
            )

            globalExerciseProgressDao.deleteAllForUser("user-1")

            val allProgress = globalExerciseProgressDao.getAllProgress()
            assertThat(allProgress).hasSize(1)
            assertThat(allProgress[0].userId).isEqualTo("user-2")
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should remove only null userId entries`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            globalExerciseProgressDao.insertProgress(
                createProgress(exercise1.id, userId = "test-user"),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise2.id, userId = null),
            )

            globalExerciseProgressDao.deleteAllWhereUserIdIsNull()

            val allProgress = globalExerciseProgressDao.getAllProgress()
            assertThat(allProgress).hasSize(1)
            assertThat(allProgress[0].userId).isEqualTo("test-user")
        }

    // Progress Tracking Tests

    @Test
    fun `progress should track all trend types correctly`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")

            globalExerciseProgressDao.insertProgress(
                createProgress(exercise1.id, trend = ProgressTrend.IMPROVING),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise2.id, trend = ProgressTrend.STALLING),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise3.id, trend = ProgressTrend.DECLINING),
            )

            val allProgress = globalExerciseProgressDao.getAllProgress()

            val improving = allProgress.find { it.exerciseId == exercise1.id }
            val stalling = allProgress.find { it.exerciseId == exercise2.id }
            val declining = allProgress.find { it.exerciseId == exercise3.id }

            assertThat(improving?.trend).isEqualTo(ProgressTrend.IMPROVING)
            assertThat(stalling?.trend).isEqualTo(ProgressTrend.STALLING)
            assertThat(declining?.trend).isEqualTo(ProgressTrend.DECLINING)
        }

    @Test
    fun `progress should track all volume trend types correctly`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")
            val exercise4 = createExercise(id = "exercise-4")

            globalExerciseProgressDao.insertProgress(
                createProgress(exercise1.id).copy(volumeTrend = VolumeTrend.INCREASING),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise2.id).copy(volumeTrend = VolumeTrend.MAINTAINING),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise3.id).copy(volumeTrend = VolumeTrend.DECREASING),
            )
            globalExerciseProgressDao.insertProgress(
                createProgress(exercise4.id).copy(volumeTrend = null),
            )

            val allProgress = globalExerciseProgressDao.getAllProgress()

            val increasing = allProgress.find { it.exerciseId == exercise1.id }
            val maintaining = allProgress.find { it.exerciseId == exercise2.id }
            val decreasing = allProgress.find { it.exerciseId == exercise3.id }
            val nullTrend = allProgress.find { it.exerciseId == exercise4.id }

            assertThat(increasing?.volumeTrend).isEqualTo(VolumeTrend.INCREASING)
            assertThat(maintaining?.volumeTrend).isEqualTo(VolumeTrend.MAINTAINING)
            assertThat(decreasing?.volumeTrend).isEqualTo(VolumeTrend.DECREASING)
            assertThat(nullTrend?.volumeTrend).isNull()
        }

    @Test
    fun `progress should track stall detection metrics correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    consecutiveStalls = 5,
                    weeksAtCurrentWeight = 3,
                    failureStreak = 2,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.consecutiveStalls).isEqualTo(5)
            assertThat(retrieved?.weeksAtCurrentWeight).isEqualTo(3)
            assertThat(retrieved?.failureStreak).isEqualTo(2)
        }

    @Test
    fun `progress should track PR history correctly`() =
        runTest {
            val exercise = createExercise()
            val prDate = LocalDateTime.of(2025, 1, 1, 10, 0)

            val progress =
                createProgress(exercise.id).copy(
                    lastPrDate = prDate,
                    lastPrWeight = 150f,
                    bestSingleRep = 160f,
                    best3Rep = 145f,
                    best5Rep = 135f,
                    best8Rep = 125f,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.lastPrDate).isEqualTo(prDate)
            assertThat(retrieved?.lastPrWeight).isEqualTo(150f)
            assertThat(retrieved?.bestSingleRep).isEqualTo(160f)
            assertThat(retrieved?.best3Rep).isEqualTo(145f)
            assertThat(retrieved?.best5Rep).isEqualTo(135f)
            assertThat(retrieved?.best8Rep).isEqualTo(125f)
        }

    @Test
    fun `progress should track volume metrics correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    totalVolumeLast30Days = 12500f,
                    lastSessionVolume = 3200f,
                    avgSessionVolume = 2950f,
                    sessionsTracked = 15,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.totalVolumeLast30Days).isEqualTo(12500f)
            assertThat(retrieved?.lastSessionVolume).isEqualTo(3200f)
            assertThat(retrieved?.avgSessionVolume).isEqualTo(2950f)
            assertThat(retrieved?.sessionsTracked).isEqualTo(15)
        }

    @Test
    fun `progress should track workout type references correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    lastProgrammeWorkoutId = "programme-workout-123",
                    lastFreestyleWorkoutId = "freestyle-workout-456",
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.lastProgrammeWorkoutId).isEqualTo("programme-workout-123")
            assertThat(retrieved?.lastFreestyleWorkoutId).isEqualTo("freestyle-workout-456")
        }

    @Test
    fun `progress should handle null optional fields correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    recentAvgRpe = null,
                    lastPrDate = null,
                    lastPrWeight = null,
                    volumeTrend = null,
                    bestSingleRep = null,
                    best3Rep = null,
                    best5Rep = null,
                    best8Rep = null,
                    lastSessionVolume = null,
                    avgSessionVolume = null,
                    lastProgressionDate = null,
                    lastProgrammeWorkoutId = null,
                    lastFreestyleWorkoutId = null,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.recentAvgRpe).isNull()
            assertThat(retrieved?.lastPrDate).isNull()
            assertThat(retrieved?.lastPrWeight).isNull()
            assertThat(retrieved?.volumeTrend).isNull()
            assertThat(retrieved?.bestSingleRep).isNull()
            assertThat(retrieved?.best3Rep).isNull()
            assertThat(retrieved?.best5Rep).isNull()
            assertThat(retrieved?.best8Rep).isNull()
            assertThat(retrieved?.lastSessionVolume).isNull()
            assertThat(retrieved?.avgSessionVolume).isNull()
            assertThat(retrieved?.lastProgressionDate).isNull()
            assertThat(retrieved?.lastProgrammeWorkoutId).isNull()
            assertThat(retrieved?.lastFreestyleWorkoutId).isNull()
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `progress can reference non-existent exercise due to removed foreign key`() =
        runTest {
            // Foreign key constraint was removed to support both system and custom exercises
            val progress = createProgress("non-existent-exercise-id")

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise("non-existent-exercise-id")
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.exerciseId).isEqualTo("non-existent-exercise-id")
        }

    @Test
    fun `progress should handle zero values correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    currentWorkingWeight = 0f,
                    estimatedMax = 0f,
                    totalVolumeLast30Days = 0f,
                    consecutiveStalls = 0,
                    sessionsTracked = 0,
                    weeksAtCurrentWeight = 0,
                    failureStreak = 0,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.currentWorkingWeight).isEqualTo(0f)
            assertThat(retrieved?.estimatedMax).isEqualTo(0f)
            assertThat(retrieved?.totalVolumeLast30Days).isEqualTo(0f)
            assertThat(retrieved?.consecutiveStalls).isEqualTo(0)
            assertThat(retrieved?.sessionsTracked).isEqualTo(0)
            assertThat(retrieved?.weeksAtCurrentWeight).isEqualTo(0)
            assertThat(retrieved?.failureStreak).isEqualTo(0)
        }

    @Test
    fun `progress should handle large weight values correctly`() =
        runTest {
            val exercise = createExercise()

            val progress =
                createProgress(exercise.id).copy(
                    currentWorkingWeight = 999999.99f,
                    estimatedMax = 1000000f,
                    totalVolumeLast30Days = 5000000f,
                )

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.currentWorkingWeight).isEqualTo(999999.99f)
            assertThat(retrieved?.estimatedMax).isEqualTo(1000000f)
            assertThat(retrieved?.totalVolumeLast30Days).isEqualTo(5000000f)
        }

    @Test
    fun `progress lastUpdated timestamp should persist correctly`() =
        runTest {
            val exercise = createExercise()
            val timestamp = LocalDateTime.of(2025, 1, 15, 14, 30, 45)

            val progress = createProgress(exercise.id, lastUpdated = timestamp)

            globalExerciseProgressDao.insertProgress(progress)

            val retrieved = globalExerciseProgressDao.getProgressForExercise(exercise.id)

            assertThat(retrieved?.lastUpdated).isEqualTo(timestamp)
        }
}
