package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.exercise.Exercise
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test suite for ProgrammeExerciseTrackingDao complex queries.
 *
 * Primary focus: Testing the nested subquery in getConsecutiveFailures()
 * which is critical for deload decisions in programme progression.
 */
class ProgrammeExerciseTrackingDaoTest : BaseDaoTest() {
    // Helper Methods

    private suspend fun createExercise(
        id: String = "exercise-1",
        name: String = "Barbell Bench Press",
    ): Exercise {
        val exercise =
            Exercise(
                id = id,
                name = name,
                category = "UPPER_BODY",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    private suspend fun createTracking(
        programmeId: String,
        exerciseId: String,
        exerciseName: String,
        workoutDate: LocalDateTime,
        wasSuccessful: Boolean,
        isDeload: Boolean = false,
    ): ProgrammeExerciseTracking {
        val tracking =
            ProgrammeExerciseTracking(
                programmeId = programmeId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                targetWeight = 100f,
                achievedWeight = if (wasSuccessful) 100f else 95f,
                targetSets = 3,
                completedSets = if (wasSuccessful) 3 else 2,
                targetReps = 5,
                achievedReps = if (wasSuccessful) 5 else 4,
                missedReps = if (wasSuccessful) 0 else 3,
                wasSuccessful = wasSuccessful,
                workoutDate = workoutDate,
                workoutId = "workout-${workoutDate.dayOfMonth}",
                isDeloadWorkout = isDeload,
            )
        programmeExerciseTrackingDao.insertTracking(tracking)
        return tracking
    }

    // CRUD Tests

    @Test
    fun `insertPerformanceRecord should insert tracking record`() =
        runTest {
            val exercise = createExercise()

            val tracking =
                ProgrammeExerciseTracking(
                    programmeId = "programme-1",
                    exerciseId = exercise.id,
                    exerciseName = "Barbell Bench Press",
                    targetWeight = 100f,
                    achievedWeight = 100f,
                    targetSets = 3,
                    completedSets = 3,
                    targetReps = 5,
                    achievedReps = 5,
                    missedReps = 0,
                    wasSuccessful = true,
                    workoutDate = LocalDateTime.now(),
                    workoutId = "workout-1",
                )

            programmeExerciseTrackingDao.insertPerformanceRecord(tracking)

            val retrieved = programmeExerciseTrackingDao.getTrackingById(tracking.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.wasSuccessful).isTrue()
        }

    @Test
    fun `getRecentPerformance should return records sorted by date descending`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val recent = programmeExerciseTrackingDao.getRecentPerformance("programme-1", "Barbell Bench Press", 3)

            assertThat(recent).hasSize(3)
            assertThat(recent[0].workoutDate).isEqualTo(now) // Most recent
            assertThat(recent[1].workoutDate).isEqualTo(now.minusDays(5))
            assertThat(recent[2].workoutDate).isEqualTo(now.minusDays(10))
        }

    @Test
    fun `getRecentPerformance should respect limit parameter`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            for (i in 1..10) {
                createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(i.toLong()), true)
            }

            val recent = programmeExerciseTrackingDao.getRecentPerformance("programme-1", "Barbell Bench Press", 5)

            assertThat(recent).hasSize(5)
        }

    // Complex Nested Subquery Tests - getConsecutiveFailures()

    @Test
    fun `getConsecutiveFailures should count failures since last success`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Sequence: Success -> Failure -> Failure -> Failure
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(7), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(4), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Should count 3 consecutive failures after the last success
            assertThat(failures).isEqualTo(3)
        }

    @Test
    fun `getConsecutiveFailures should return zero when last workout was successful`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Sequence: Failure -> Failure -> Success
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, true)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            assertThat(failures).isEqualTo(0)
        }

    @Test
    fun `getConsecutiveFailures should reset count after deload workout`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Sequence: Failure -> Failure -> Deload -> Failure
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(15), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(7), false, isDeload = true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Should only count failure after the deload (1 failure)
            assertThat(failures).isEqualTo(1)
        }

    @Test
    fun `getConsecutiveFailures should count all failures when no success or deload exists`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // All failures, no success or deload
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(15), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Should count all 4 failures
            assertThat(failures).isEqualTo(4)
        }

    @Test
    fun `getConsecutiveFailures should return zero when no tracking records exist`() =
        runTest {
            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            assertThat(failures).isEqualTo(0)
        }

    @Test
    fun `getConsecutiveFailures should handle single failure after success`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            assertThat(failures).isEqualTo(1)
        }

    @Test
    fun `getConsecutiveFailures should handle success after deload`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Sequence: Failure -> Failure -> Deload -> Success
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(15), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(7), false, isDeload = true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, true)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Deload resets, then success, so 0 failures
            assertThat(failures).isEqualTo(0)
        }

    @Test
    fun `getConsecutiveFailures should only count failures for specific programme`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Programme 1: Success -> Failure -> Failure
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            // Programme 2: All failures (different programme)
            createTracking("programme-2", exercise.id, "Barbell Bench Press", now.minusDays(8), false)
            createTracking("programme-2", exercise.id, "Barbell Bench Press", now.minusDays(4), false)
            createTracking("programme-2", exercise.id, "Barbell Bench Press", now.minusDays(1), false)

            val programme1Failures =
                programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")
            val programme2Failures =
                programmeExerciseTrackingDao.getConsecutiveFailures("programme-2", "Barbell Bench Press")

            assertThat(programme1Failures).isEqualTo(2)
            assertThat(programme2Failures).isEqualTo(3)
        }

    @Test
    fun `getConsecutiveFailures should only count failures for specific exercise`() =
        runTest {
            val benchPress = createExercise(id = "bench-press", name = "Barbell Bench Press")
            val squat = createExercise(id = "squat", name = "Barbell Squat")
            val now = LocalDateTime.now()

            // Bench Press: Success -> Failure -> Failure
            createTracking("programme-1", benchPress.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", benchPress.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-1", benchPress.id, "Barbell Bench Press", now, false)

            // Squat: All failures
            createTracking("programme-1", squat.id, "Barbell Squat", now.minusDays(8), false)
            createTracking("programme-1", squat.id, "Barbell Squat", now.minusDays(4), false)
            createTracking("programme-1", squat.id, "Barbell Squat", now.minusDays(1), false)

            val benchFailures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")
            val squatFailures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Squat")

            assertThat(benchFailures).isEqualTo(2)
            assertThat(squatFailures).isEqualTo(3)
        }

    @Test
    fun `getConsecutiveFailures should handle mixed successes and failures correctly`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Complex sequence: Success -> Failure -> Success -> Failure -> Failure -> Failure
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(20), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(17), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(14), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Should count 3 failures after the most recent success (day 14)
            assertThat(failures).isEqualTo(3)
        }

    @Test
    fun `getConsecutiveFailures should handle deload that is also successful`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Sequence: Failure -> Failure -> Deload (successful) -> Failure
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(15), false)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), false)
            // Deload that is successful (both flags true)
            val deloadTracking =
                ProgrammeExerciseTracking(
                    programmeId = "programme-1",
                    exerciseId = exercise.id,
                    exerciseName = "Barbell Bench Press",
                    targetWeight = 80f,
                    achievedWeight = 80f,
                    targetSets = 3,
                    completedSets = 3,
                    targetReps = 5,
                    achievedReps = 5,
                    missedReps = 0,
                    wasSuccessful = true,
                    workoutDate = now.minusDays(7),
                    workoutId = "deload-workout",
                    isDeloadWorkout = true,
                )
            programmeExerciseTrackingDao.insertTracking(deloadTracking)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, false)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            // Should count 1 failure after deload (deload resets the count)
            assertThat(failures).isEqualTo(1)
        }

    @Test
    fun `getConsecutiveFailures should handle only successes`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(15), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now, true)

            val failures = programmeExerciseTrackingDao.getConsecutiveFailures("programme-1", "Barbell Bench Press")

            assertThat(failures).isEqualTo(0)
        }

    // Cleanup Tests

    @Test
    fun `deleteAllForUser should only delete tracking for specified user`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            val tracking1 =
                ProgrammeExerciseTracking(
                    userId = "user1",
                    programmeId = "programme-1",
                    exerciseId = exercise.id,
                    exerciseName = "Barbell Bench Press",
                    targetWeight = 100f,
                    achievedWeight = 100f,
                    targetSets = 3,
                    completedSets = 3,
                    targetReps = 5,
                    achievedReps = 5,
                    missedReps = 0,
                    wasSuccessful = true,
                    workoutDate = now,
                    workoutId = "workout-1",
                )

            val tracking2 =
                ProgrammeExerciseTracking(
                    userId = "user2",
                    programmeId = "programme-1",
                    exerciseId = exercise.id,
                    exerciseName = "Barbell Bench Press",
                    targetWeight = 100f,
                    achievedWeight = 100f,
                    targetSets = 3,
                    completedSets = 3,
                    targetReps = 5,
                    achievedReps = 5,
                    missedReps = 0,
                    wasSuccessful = true,
                    workoutDate = now,
                    workoutId = "workout-2",
                )

            programmeExerciseTrackingDao.insertTracking(tracking1)
            programmeExerciseTrackingDao.insertTracking(tracking2)

            programmeExerciseTrackingDao.deleteAllForUser("user1")

            val allTracking = programmeExerciseTrackingDao.getAllTracking()
            assertThat(allTracking).hasSize(1)
            assertThat(allTracking[0].userId).isEqualTo("user2")
        }

    @Test
    fun `deleteAllTracking should remove all tracking records`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(10), true)
            createTracking("programme-1", exercise.id, "Barbell Bench Press", now.minusDays(5), false)
            createTracking("programme-2", exercise.id, "Barbell Bench Press", now, true)

            programmeExerciseTrackingDao.deleteAllTracking()

            val allTracking = programmeExerciseTrackingDao.getAllTracking()
            assertThat(allTracking).isEmpty()
        }
}
