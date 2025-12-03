package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.exercise.Exercise
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for PersonalRecordDao complex queries.
 *
 * Primary focus: Testing the CTE query using ROW_NUMBER() in getRecentPRs()
 * which is critical for the PR history feature.
 */
@RunWith(RobolectricTestRunner::class)
class PersonalRecordDaoTest : BaseDaoTest() {
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

    private suspend fun createWorkout(
        id: String = "workout-1",
        date: LocalDateTime = LocalDateTime.now(),
    ): Workout {
        val workout =
            Workout(
                id = id,
                userId = "test-user",
                name = "Test Workout",
                date = date,
                status = WorkoutStatus.COMPLETED,
            )
        workoutDao.insertWorkout(workout)
        return workout
    }

    // CRUD Tests

    @Test
    fun `insertPersonalRecord should insert PR into database`() =
        runTest {
            createExercise()

            val pr =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = 95f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(7),
                    improvementPercentage = 5.26f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f,
                )

            personalRecordDao.insertPersonalRecord(pr)

            val retrieved = personalRecordDao.getPersonalRecordById(pr.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.weight).isEqualTo(100f)
            assertThat(retrieved?.recordType).isEqualTo(PRType.WEIGHT)
        }

    @Test
    fun `getRecentPRsForExercise should return PRs sorted by date descending`() =
        runTest {
            createExercise()

            val now = LocalDateTime.now()
            val pr1 =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                )
            val pr2 =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 105f,
                    reps = 5,
                    recordDate = now.minusDays(5),
                    previousWeight = 100f,
                    previousReps = 5,
                    previousDate = now.minusDays(10),
                    improvementPercentage = 5f,
                    recordType = PRType.WEIGHT,
                )
            val pr3 =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 110f,
                    reps = 5,
                    recordDate = now,
                    previousWeight = 105f,
                    previousReps = 5,
                    previousDate = now.minusDays(5),
                    improvementPercentage = 4.76f,
                    recordType = PRType.WEIGHT,
                )

            personalRecordDao.insertPersonalRecord(pr1)
            personalRecordDao.insertPersonalRecord(pr2)
            personalRecordDao.insertPersonalRecord(pr3)

            val recent = personalRecordDao.getRecentPRsForExercise("exercise-1", limit = 5)

            assertThat(recent).hasSize(3)
            assertThat(recent[0].weight).isEqualTo(110f) // Most recent
            assertThat(recent[1].weight).isEqualTo(105f)
            assertThat(recent[2].weight).isEqualTo(100f)
        }

    // Complex CTE Query Tests - getRecentPRs()

    @Test
    fun `getRecentPRs should return one PR per exercise using ROW_NUMBER CTE`() =
        runTest {
            createExercise(id = "bench-press", name = "Barbell Bench Press")
            createExercise(id = "squat", name = "Barbell Squat")

            val now = LocalDateTime.now()

            // Bench Press - Multiple PRs, should only return the highest 1RM
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "bench-press",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f,
                ),
            )
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "bench-press",
                    weight = 105f,
                    reps = 5,
                    recordDate = now.minusDays(5),
                    previousWeight = 100f,
                    previousReps = 5,
                    previousDate = now.minusDays(10),
                    improvementPercentage = 5f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 118.1f, // Higher 1RM
                ),
            )

            // Squat - Only one PR
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "squat",
                    weight = 150f,
                    reps = 3,
                    recordDate = now.minusDays(3),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 159f,
                ),
            )

            val recentPRs = personalRecordDao.getRecentPRs(limit = 10)

            // Should return 2 PRs (one per exercise)
            assertThat(recentPRs).hasSize(2)

            // For bench-press, should return the one with highest estimated1RM
            val benchPR = recentPRs.find { it.exerciseId == "bench-press" }
            assertThat(benchPR).isNotNull()
            assertThat(benchPR?.weight).isEqualTo(105f) // Higher 1RM record

            val squatPR = recentPRs.find { it.exerciseId == "squat" }
            assertThat(squatPR).isNotNull()
            assertThat(squatPR?.weight).isEqualTo(150f)
        }

    @Test
    fun `getRecentPRs should only include PRs from last 30 days`() =
        runTest {
            createExercise(id = "bench-press", name = "Barbell Bench Press")
            createExercise(id = "squat", name = "Barbell Squat")

            val now = LocalDateTime.now()

            // Recent PR - within 30 days
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "bench-press",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(15),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f,
                ),
            )

            // Old PR - more than 30 days ago
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "squat",
                    weight = 150f,
                    reps = 5,
                    recordDate = now.minusDays(35),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 168.75f,
                ),
            )

            val recentPRs = personalRecordDao.getRecentPRs(limit = 10)

            // Should only return the bench press PR (within 30 days)
            assertThat(recentPRs).hasSize(1)
            assertThat(recentPRs[0].exerciseId).isEqualTo("bench-press")
        }

    @Test
    fun `getRecentPRs should break ties by most recent date when estimated1RM is equal`() =
        runTest {
            createExercise()

            val now = LocalDateTime.now()

            // Two PRs with same estimated 1RM
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f,
                ),
            )
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(5), // More recent
                    previousWeight = 100f,
                    previousReps = 5,
                    previousDate = now.minusDays(10),
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f, // Same 1RM
                ),
            )

            val recentPRs = personalRecordDao.getRecentPRs(limit = 10)

            // Should return only one (the more recent one)
            assertThat(recentPRs).hasSize(1)
            assertThat(recentPRs[0].recordDate).isEqualTo(now.minusDays(5))
        }

    @Test
    fun `getRecentPRs should respect limit parameter`() =
        runTest {
            val now = LocalDateTime.now()

            // Create 10 different exercises with PRs
            for (i in 1..10) {
                createExercise(id = "exercise-$i", name = "Exercise $i")
                personalRecordDao.insertPersonalRecord(
                    PersonalRecord(
                        exerciseId = "exercise-$i",
                        weight = 100f,
                        reps = 5,
                        recordDate = now.minusDays(i.toLong()),
                        previousWeight = null,
                        previousReps = null,
                        previousDate = null,
                        improvementPercentage = 0f,
                        recordType = PRType.WEIGHT,
                        estimated1RM = 112.5f,
                    ),
                )
            }

            val recentPRs = personalRecordDao.getRecentPRs(limit = 5)

            assertThat(recentPRs).hasSize(5)
        }

    @Test
    fun `getRecentPRs should return empty list when no PRs exist`() =
        runTest {
            val recentPRs = personalRecordDao.getRecentPRs(limit = 10)

            assertThat(recentPRs).isEmpty()
        }

    @Test
    fun `getRecentPRs should return empty list when all PRs are older than 30 days`() =
        runTest {
            createExercise()

            val now = LocalDateTime.now()
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(40),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    estimated1RM = 112.5f,
                ),
            )

            val recentPRs = personalRecordDao.getRecentPRs(limit = 10)

            assertThat(recentPRs).isEmpty()
        }

    // Other Query Tests

    @Test
    fun `getLatestPRForExerciseAndType should return most recent PR for specific type`() =
        runTest {
            createExercise()

            val now = LocalDateTime.now()

            // Weight PR
            val weightPR =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = now.minusDays(5),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                )

            // Estimated 1RM PR
            val estimatedPR =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 95f,
                    reps = 6,
                    recordDate = now.minusDays(3),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.ESTIMATED_1RM,
                    estimated1RM = 110f,
                )

            personalRecordDao.insertPersonalRecord(weightPR)
            personalRecordDao.insertPersonalRecord(estimatedPR)

            val latestWeight = personalRecordDao.getLatestPRForExerciseAndType("exercise-1", PRType.WEIGHT)
            val latestEstimated = personalRecordDao.getLatestPRForExerciseAndType("exercise-1", PRType.ESTIMATED_1RM)

            assertThat(latestWeight?.recordType).isEqualTo(PRType.WEIGHT)
            assertThat(latestWeight?.weight).isEqualTo(100f)

            assertThat(latestEstimated?.recordType).isEqualTo(PRType.ESTIMATED_1RM)
            assertThat(latestEstimated?.estimated1RM).isEqualTo(110f)
        }

    @Test
    fun `getMaxWeightForExercise should return highest weight for exercise`() =
        runTest {
            createExercise()

            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now().minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                ),
            )
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 110f,
                    reps = 3,
                    recordDate = LocalDateTime.now().minusDays(5),
                    previousWeight = 100f,
                    previousReps = 5,
                    previousDate = LocalDateTime.now().minusDays(10),
                    improvementPercentage = 10f,
                    recordType = PRType.WEIGHT,
                ),
            )

            val maxWeight = personalRecordDao.getMaxWeightForExercise("exercise-1")

            assertThat(maxWeight).isEqualTo(110f)
        }

    @Test
    fun `getMaxWeightForExercise should return null when no PRs exist`() =
        runTest {
            createExercise()

            val maxWeight = personalRecordDao.getMaxWeightForExercise("exercise-1")

            assertThat(maxWeight).isNull()
        }

    @Test
    fun `getMaxEstimated1RMForExercise should return highest estimated 1RM`() =
        runTest {
            createExercise()

            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now().minusDays(10),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.ESTIMATED_1RM,
                    estimated1RM = 112.5f,
                ),
            )
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 95f,
                    reps = 6,
                    recordDate = LocalDateTime.now().minusDays(5),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.ESTIMATED_1RM,
                    estimated1RM = 110.25f,
                ),
            )

            val max1RM = personalRecordDao.getMaxEstimated1RMForExercise("exercise-1")

            assertThat(max1RM).isEqualTo(112.5f)
        }

    @Test
    fun `getPRsBySourceSetId should return all PRs linked to a set`() =
        runTest {
            createExercise()
            val workout = createWorkout()

            val exerciseLog =
                ExerciseLog(
                    workoutId = workout.id,
                    exerciseId = "exercise-1",
                    exerciseOrder = 1,
                )
            exerciseLogDao.insertExerciseLog(exerciseLog)

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLog.id,
                    setOrder = 1,
                    actualReps = 5,
                    actualWeight = 100f,
                    isCompleted = true,
                )
            setLogDao.insertSetLog(setLog)

            // PR linked to this set
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    sourceSetId = setLog.id,
                ),
            )

            val prs = personalRecordDao.getPRsBySourceSetId(setLog.id)

            assertThat(prs).hasSize(1)
            assertThat(prs[0].sourceSetId).isEqualTo(setLog.id)
        }

    @Test
    fun `deletePRsBySourceSetId should delete PRs linked to a set`() =
        runTest {
            createExercise()

            val pr =
                PersonalRecord(
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                    sourceSetId = "set-123",
                )
            personalRecordDao.insertPersonalRecord(pr)

            personalRecordDao.deletePRsBySourceSetId("set-123")

            val count = personalRecordDao.countPRsBySourceSetId("set-123")
            assertThat(count).isEqualTo(0)
        }

    @Test
    fun `deleteAllForUser should only delete PRs for specified user`() =
        runTest {
            createExercise()

            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    userId = "user1",
                    exerciseId = "exercise-1",
                    weight = 100f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                ),
            )
            personalRecordDao.insertPersonalRecord(
                PersonalRecord(
                    userId = "user2",
                    exerciseId = "exercise-1",
                    weight = 110f,
                    reps = 5,
                    recordDate = LocalDateTime.now(),
                    previousWeight = null,
                    previousReps = null,
                    previousDate = null,
                    improvementPercentage = 0f,
                    recordType = PRType.WEIGHT,
                ),
            )

            personalRecordDao.deleteAllForUser("user1")

            val allPRs = personalRecordDao.getAllPersonalRecords()
            assertThat(allPRs).hasSize(1)
            assertThat(allPRs[0].userId).isEqualTo("user2")
        }
}
