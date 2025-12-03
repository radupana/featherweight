package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.exercise.Exercise
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test suite for SetLogDao complex queries.
 *
 * Primary focus: Testing the CTE query using WITH clause in getMaxWeightSetFromLastWorkout()
 * which is critical for last workout weight calculations.
 */
class SetLogDaoTest : BaseDaoTest() {
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
        status: WorkoutStatus = WorkoutStatus.COMPLETED,
    ): Workout {
        val workout =
            Workout(
                id = id,
                userId = "test-user",
                name = "Test Workout",
                date = date,
                status = status,
            )
        workoutDao.insertWorkout(workout)
        return workout
    }

    private suspend fun createExerciseLog(
        workoutId: String,
        exerciseId: String,
        order: Int = 1,
    ): ExerciseLog {
        val exerciseLog =
            ExerciseLog(
                workoutId = workoutId,
                exerciseId = exerciseId,
                exerciseOrder = order,
            )
        exerciseLogDao.insertExerciseLog(exerciseLog)
        return exerciseLog
    }

    private suspend fun createSetLog(
        exerciseLogId: String,
        setOrder: Int,
        weight: Float,
        reps: Int,
        isCompleted: Boolean = true,
    ): SetLog {
        val setLog =
            SetLog(
                exerciseLogId = exerciseLogId,
                setOrder = setOrder,
                actualWeight = weight,
                actualReps = reps,
                isCompleted = isCompleted,
            )
        setLogDao.insertSetLog(setLog)
        return setLog
    }

    // CRUD Tests

    @Test
    fun `insertSetLog should insert set into database`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            val setLog =
                SetLog(
                    exerciseLogId = exerciseLog.id,
                    setOrder = 1,
                    targetReps = 5,
                    targetWeight = 100f,
                    actualReps = 5,
                    actualWeight = 100f,
                    isCompleted = true,
                )

            setLogDao.insertSetLog(setLog)

            val retrieved = setLogDao.getSetLogById(setLog.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.actualWeight).isEqualTo(100f)
            assertThat(retrieved?.actualReps).isEqualTo(5)
        }

    @Test
    fun `getSetLogsForExercise should return sets sorted by order`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            createSetLog(exerciseLog.id, 3, 90f, 5)
            createSetLog(exerciseLog.id, 1, 100f, 5)
            createSetLog(exerciseLog.id, 2, 95f, 5)

            val sets = setLogDao.getSetLogsForExercise(exerciseLog.id)

            assertThat(sets).hasSize(3)
            assertThat(sets[0].setOrder).isEqualTo(1)
            assertThat(sets[1].setOrder).isEqualTo(2)
            assertThat(sets[2].setOrder).isEqualTo(3)
        }

    @Test
    fun `markSetCompleted should update completion status`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)
            val setLog = createSetLog(exerciseLog.id, 1, 100f, 5, isCompleted = false)

            val completedAt = LocalDateTime.now().toString()
            setLogDao.markSetCompleted(setLog.id, true, completedAt)

            val retrieved = setLogDao.getSetLogById(setLog.id)
            assertThat(retrieved?.isCompleted).isTrue()
            assertThat(retrieved?.completedAt).isEqualTo(completedAt)
        }

    // Complex CTE Query Tests - getMaxWeightSetFromLastWorkout()

    @Test
    fun `getMaxWeightSetFromLastWorkout should return heaviest set from most recent workout`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Earlier workout
            val workout1 = createWorkout(id = "workout-1", date = now.minusDays(7))
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            createSetLog(exerciseLog1.id, 1, 90f, 5) // Lighter weight in older workout

            // Most recent workout
            val workout2 = createWorkout(id = "workout-2", date = now)
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            val set1 = createSetLog(exerciseLog2.id, 1, 100f, 5)
            val set2 = createSetLog(exerciseLog2.id, 2, 105f, 3) // Heaviest set in last workout
            createSetLog(exerciseLog2.id, 3, 95f, 6)

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNotNull()
            assertThat(maxSet?.id).isEqualTo(set2.id)
            assertThat(maxSet?.actualWeight).isEqualTo(105f)
            assertThat(maxSet?.actualReps).isEqualTo(3)
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should only consider completed workouts`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Completed workout
            val workout1 = createWorkout(id = "workout-1", date = now.minusDays(7), status = WorkoutStatus.COMPLETED)
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            val set1 = createSetLog(exerciseLog1.id, 1, 90f, 5)

            // In-progress workout (more recent but not completed)
            val workout2 = createWorkout(id = "workout-2", date = now, status = WorkoutStatus.IN_PROGRESS)
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            createSetLog(exerciseLog2.id, 1, 110f, 5) // Higher weight but workout not completed

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            // Should return set from the completed workout, not the in-progress one
            assertThat(maxSet).isNotNull()
            assertThat(maxSet?.id).isEqualTo(set1.id)
            assertThat(maxSet?.actualWeight).isEqualTo(90f)
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should only consider completed sets`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            createSetLog(exerciseLog.id, 1, 100f, 5, isCompleted = true)
            createSetLog(exerciseLog.id, 2, 120f, 5, isCompleted = false) // Not completed

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNotNull()
            assertThat(maxSet?.actualWeight).isEqualTo(100f) // Only completed set
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should only consider sets with positive weight`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            val set1 = createSetLog(exerciseLog.id, 1, 100f, 5)
            createSetLog(exerciseLog.id, 2, 0f, 5) // Zero weight (bodyweight exercise maybe)

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNotNull()
            assertThat(maxSet?.id).isEqualTo(set1.id)
            assertThat(maxSet?.actualWeight).isEqualTo(100f)
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should break ties by higher reps`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            createSetLog(exerciseLog.id, 1, 100f, 3)
            val set2 = createSetLog(exerciseLog.id, 2, 100f, 5) // Same weight, more reps

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNotNull()
            assertThat(maxSet?.id).isEqualTo(set2.id)
            assertThat(maxSet?.actualReps).isEqualTo(5)
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should return null when no completed workouts exist`() =
        runTest {
            val exercise = createExercise()

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNull()
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should return null when last workout has no valid sets`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)

            // All sets are invalid (not completed or zero weight)
            createSetLog(exerciseLog.id, 1, 0f, 5)
            createSetLog(exerciseLog.id, 2, 100f, 5, isCompleted = false)

            val maxSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise.id)

            assertThat(maxSet).isNull()
        }

    @Test
    fun `getMaxWeightSetFromLastWorkout should handle multiple exercises in same workout`() =
        runTest {
            val exercise1 = createExercise(id = "bench-press", name = "Barbell Bench Press")
            val exercise2 = createExercise(id = "squat", name = "Barbell Squat")
            val workout = createWorkout()

            // Bench press sets
            val benchLog = createExerciseLog(workout.id, exercise1.id, 1)
            val benchSet = createSetLog(benchLog.id, 1, 100f, 5)

            // Squat sets
            val squatLog = createExerciseLog(workout.id, exercise2.id, 2)
            val squatSet = createSetLog(squatLog.id, 1, 150f, 5)

            val maxBenchSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise1.id)
            val maxSquatSet = setLogDao.getMaxWeightSetFromLastWorkout(exercise2.id)

            assertThat(maxBenchSet?.id).isEqualTo(benchSet.id)
            assertThat(maxBenchSet?.actualWeight).isEqualTo(100f)

            assertThat(maxSquatSet?.id).isEqualTo(squatSet.id)
            assertThat(maxSquatSet?.actualWeight).isEqualTo(150f)
        }

    // Other Complex Query Tests

    @Test
    fun `getLastCompletedSetForExercise should return most recent completed set`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Older workout
            val workout1 = createWorkout(id = "workout-1", date = now.minusDays(7))
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            createSetLog(exerciseLog1.id, 1, 90f, 5)

            // More recent workout
            val workout2 = createWorkout(id = "workout-2", date = now)
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            val lastSet = createSetLog(exerciseLog2.id, 2, 100f, 5) // Last set by date and order

            val result = setLogDao.getLastCompletedSetForExercise(exercise.id)

            assertThat(result).isNotNull()
            assertThat(result?.id).isEqualTo(lastSet.id)
        }

    @Test
    fun `getSetsForExerciseSince should return completed sets since date`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()
            val sinceDate = now.minusDays(5).toString()

            // Old set (before sinceDate)
            val workout1 = createWorkout(id = "workout-1", date = now.minusDays(10))
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            createSetLog(exerciseLog1.id, 1, 80f, 5)

            // Recent sets (after sinceDate)
            val workout2 = createWorkout(id = "workout-2", date = now.minusDays(3))
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            createSetLog(exerciseLog2.id, 1, 90f, 5)
            createSetLog(exerciseLog2.id, 2, 95f, 5)

            val sets = setLogDao.getSetsForExerciseSince(exercise.id, sinceDate)

            assertThat(sets).hasSize(2)
            assertThat(sets.all { it.actualWeight >= 90f }).isTrue()
        }

    @Test
    fun `getMaxWeightForExerciseInDateRange should find max weight in range`() =
        runTest {
            val exercise = createExercise()
            val now = LocalDateTime.now()

            // Before range
            val workout1 = createWorkout(id = "workout-1", date = now.minusDays(20))
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            createSetLog(exerciseLog1.id, 1, 150f, 5)

            // In range
            val workout2 = createWorkout(id = "workout-2", date = now.minusDays(5))
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            createSetLog(exerciseLog2.id, 1, 100f, 5)
            createSetLog(exerciseLog2.id, 2, 105f, 3) // Max in range

            // After range
            val workout3 = createWorkout(id = "workout-3", date = now.plusDays(5))
            val exerciseLog3 = createExerciseLog(workout3.id, exercise.id)
            createSetLog(exerciseLog3.id, 1, 200f, 5)

            val startDate = now.minusDays(10).toString()
            val endDate = now.toString()
            val maxWeight = setLogDao.getMaxWeightForExerciseInDateRange(exercise.id, startDate, endDate)

            assertThat(maxWeight).isEqualTo(105f)
        }

    @Test
    fun `getWorkoutDateForSetLog should return workout date via joins`() =
        runTest {
            val exercise = createExercise()
            val workoutDate = LocalDateTime.of(2025, 1, 15, 10, 0)
            val workout = createWorkout(date = workoutDate)
            val exerciseLog = createExerciseLog(workout.id, exercise.id)
            val setLog = createSetLog(exerciseLog.id, 1, 100f, 5)

            val retrievedDate = setLogDao.getWorkoutDateForSetLog(setLog.id)

            // Room uses ISO_LOCAL_DATE_TIME formatter for LocalDateTime
            val expectedDate = workoutDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            assertThat(retrievedDate).isEqualTo(expectedDate)
        }

    @Test
    fun `getWorkoutIdForSetLog should return workout id via joins`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)
            val setLog = createSetLog(exerciseLog.id, 1, 100f, 5)

            val workoutId = setLogDao.getWorkoutIdForSetLog(setLog.id)

            assertThat(workoutId).isEqualTo(workout.id)
        }

    @Test
    fun `updateCompletionTracking should update tracking fields`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog = createExerciseLog(workout.id, exercise.id)
            val setLog = createSetLog(exerciseLog.id, 1, 100f, 5)

            setLogDao.updateCompletionTracking(setLog.id, true, 95f)

            val triggered = setLogDao.didSetTriggerUsageIncrement(setLog.id)
            val previous1RM = setLogDao.getPrevious1RMEstimate(setLog.id)

            assertThat(triggered).isTrue()
            assertThat(previous1RM).isEqualTo(95f)
        }

    @Test
    fun `deleteAllSetsForExercise should remove all sets for exercise log`() =
        runTest {
            val exercise = createExercise()
            val workout = createWorkout()
            val exerciseLog1 = createExerciseLog(workout.id, exercise.id, 1)
            val exerciseLog2 = createExerciseLog(workout.id, exercise.id, 2)

            createSetLog(exerciseLog1.id, 1, 100f, 5)
            createSetLog(exerciseLog1.id, 2, 105f, 5)
            createSetLog(exerciseLog2.id, 1, 110f, 5)

            setLogDao.deleteAllSetsForExercise(exerciseLog1.id)

            val sets1 = setLogDao.getSetLogsForExercise(exerciseLog1.id)
            val sets2 = setLogDao.getSetLogsForExercise(exerciseLog2.id)

            assertThat(sets1).isEmpty()
            assertThat(sets2).hasSize(1)
        }

    @Test
    fun `deleteAllForUser should only delete sets for specified user`() =
        runTest {
            val exercise = createExercise()

            val workout1 = createWorkout(id = "workout-1")
            val exerciseLog1 = createExerciseLog(workout1.id, exercise.id)
            val set1 =
                SetLog(
                    userId = "user1",
                    exerciseLogId = exerciseLog1.id,
                    setOrder = 1,
                    actualWeight = 100f,
                    actualReps = 5,
                )
            setLogDao.insertSetLog(set1)

            val workout2 = createWorkout(id = "workout-2")
            val exerciseLog2 = createExerciseLog(workout2.id, exercise.id)
            val set2 =
                SetLog(
                    userId = "user2",
                    exerciseLogId = exerciseLog2.id,
                    setOrder = 1,
                    actualWeight = 110f,
                    actualReps = 5,
                )
            setLogDao.insertSetLog(set2)

            setLogDao.deleteAllForUser("user1")

            val allSets = setLogDao.getSetLogsForExercises(listOf(exerciseLog1.id, exerciseLog2.id))
            assertThat(allSets).hasSize(1)
            assertThat(allSets[0].userId).isEqualTo("user2")
        }
}
