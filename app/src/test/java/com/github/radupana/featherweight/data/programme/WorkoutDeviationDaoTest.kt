package com.github.radupana.featherweight.data.programme

import com.github.radupana.featherweight.data.BaseDaoTest
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test suite for WorkoutDeviationDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - Query operations with filtering
 * - Flow-based queries
 * - Complex aggregation queries
 * - Deletion operations
 */
class WorkoutDeviationDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a valid programme for testing.
     * Required for foreign key constraints.
     */
    private suspend fun createProgramme(
        id: String = "test-programme",
        name: String = "Test Programme",
        status: ProgrammeStatus = ProgrammeStatus.IN_PROGRESS,
    ): Programme {
        val programme =
            Programme(
                id = id,
                userId = "test-user",
                name = name,
                description = "Test programme description",
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                status = status,
                createdAt = LocalDateTime.now(),
                startedAt = if (status != ProgrammeStatus.NOT_STARTED) LocalDateTime.now().minusDays(7) else null,
                completedAt = if (status == ProgrammeStatus.COMPLETED) LocalDateTime.now() else null,
                isActive = status in listOf(ProgrammeStatus.NOT_STARTED, ProgrammeStatus.IN_PROGRESS),
            )
        programmeDao.insertProgramme(programme)
        return programme
    }

    /**
     * Creates and inserts a valid workout for testing.
     * Required for foreign key constraints on WorkoutDeviation.
     */
    private suspend fun createWorkout(
        id: String = "test-workout",
        userId: String = "test-user",
        programmeId: String? = null,
    ): Workout {
        val workout =
            Workout(
                id = id,
                userId = userId,
                name = "Test Workout",
                date = LocalDateTime.now(),
                status = WorkoutStatus.COMPLETED,
                programmeId = programmeId,
            )
        workoutDao.insertWorkout(workout)
        return workout
    }

    /**
     * Creates a test WorkoutDeviation.
     */
    private fun createDeviation(
        workoutId: String,
        programmeId: String,
        deviationType: DeviationType = DeviationType.VOLUME_DEVIATION,
        magnitude: Float = 10f,
        timestamp: LocalDateTime = LocalDateTime.now(),
    ): WorkoutDeviation =
        WorkoutDeviation(
            userId = "test-user",
            workoutId = workoutId,
            programmeId = programmeId,
            exerciseLogId = "test-exercise-log",
            deviationType = deviationType,
            deviationMagnitude = magnitude,
            notes = "Test deviation note",
            timestamp = timestamp,
        )

    // CRUD Operations Tests

    @Test
    fun `insert should add deviation to database`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)
            val deviation = createDeviation(workout.id, programme.id)

            workoutDeviationDao.insert(deviation)

            val retrieved = workoutDeviationDao.getDeviationsForWorkout(workout.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].id).isEqualTo(deviation.id)
            assertThat(retrieved[0].deviationType).isEqualTo(DeviationType.VOLUME_DEVIATION)
            assertThat(retrieved[0].deviationMagnitude).isEqualTo(10f)
        }

    @Test
    fun `insertAll should add multiple deviations to database`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            val deviations =
                listOf(
                    createDeviation(workout.id, programme.id, DeviationType.VOLUME_DEVIATION),
                    createDeviation(workout.id, programme.id, DeviationType.INTENSITY_DEVIATION),
                    createDeviation(workout.id, programme.id, DeviationType.RPE_DEVIATION),
                )

            workoutDeviationDao.insertAll(deviations)

            val retrieved = workoutDeviationDao.getDeviationsForWorkout(workout.id)
            assertThat(retrieved).hasSize(3)
            assertThat(retrieved.map { it.deviationType }).containsExactly(
                DeviationType.VOLUME_DEVIATION,
                DeviationType.INTENSITY_DEVIATION,
                DeviationType.RPE_DEVIATION,
            )
        }

    // Query Operations Tests

    @Test
    fun `getDeviationsForWorkout should return deviations for specific workout`() =
        runTest {
            val programme = createProgramme()
            val workout1 = createWorkout(id = "workout-1", programmeId = programme.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme.id)

            workoutDeviationDao.insert(createDeviation(workout1.id, programme.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme.id))

            val workout1Deviations = workoutDeviationDao.getDeviationsForWorkout(workout1.id)
            val workout2Deviations = workoutDeviationDao.getDeviationsForWorkout(workout2.id)

            assertThat(workout1Deviations).hasSize(1)
            assertThat(workout2Deviations).hasSize(1)
            assertThat(workout1Deviations[0].workoutId).isEqualTo(workout1.id)
            assertThat(workout2Deviations[0].workoutId).isEqualTo(workout2.id)
        }

    @Test
    fun `getDeviationsForWorkout should return empty list for workout with no deviations`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            val deviations = workoutDeviationDao.getDeviationsForWorkout(workout.id)

            assertThat(deviations).isEmpty()
        }

    @Test
    fun `getDeviationsForProgramme should return all deviations for programme`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme2.id)

            workoutDeviationDao.insert(createDeviation(workout1.id, programme1.id))
            workoutDeviationDao.insert(createDeviation(workout1.id, programme1.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme2.id))

            val programme1Deviations = workoutDeviationDao.getDeviationsForProgramme(programme1.id)
            val programme2Deviations = workoutDeviationDao.getDeviationsForProgramme(programme2.id)

            assertThat(programme1Deviations).hasSize(2)
            assertThat(programme2Deviations).hasSize(1)
            assertThat(programme1Deviations.all { it.programmeId == programme1.id }).isTrue()
            assertThat(programme2Deviations.all { it.programmeId == programme2.id }).isTrue()
        }

    @Test
    fun `getDeviationsByType should filter deviations by type`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            workoutDeviationDao.insertAll(
                listOf(
                    createDeviation(workout.id, programme.id, DeviationType.VOLUME_DEVIATION),
                    createDeviation(workout.id, programme.id, DeviationType.VOLUME_DEVIATION),
                    createDeviation(workout.id, programme.id, DeviationType.INTENSITY_DEVIATION),
                    createDeviation(workout.id, programme.id, DeviationType.EXERCISE_SKIPPED),
                ),
            )

            val volumeDeviations =
                workoutDeviationDao.getDeviationsByType(programme.id, DeviationType.VOLUME_DEVIATION)
            val intensityDeviations =
                workoutDeviationDao.getDeviationsByType(programme.id, DeviationType.INTENSITY_DEVIATION)
            val skippedDeviations =
                workoutDeviationDao.getDeviationsByType(programme.id, DeviationType.EXERCISE_SKIPPED)

            assertThat(volumeDeviations).hasSize(2)
            assertThat(intensityDeviations).hasSize(1)
            assertThat(skippedDeviations).hasSize(1)
            assertThat(volumeDeviations.all { it.deviationType == DeviationType.VOLUME_DEVIATION }).isTrue()
        }

    @Test
    fun `getDeviationsForWorkoutFlow should emit updates when deviations change`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            val flow = workoutDeviationDao.getDeviationsForWorkoutFlow(workout.id)

            // Initial state - empty
            val initial = flow.first()
            assertThat(initial).isEmpty()

            // Insert a deviation
            val deviation = createDeviation(workout.id, programme.id)
            workoutDeviationDao.insert(deviation)

            // Flow should emit the new deviation
            val updated = flow.first()
            assertThat(updated).hasSize(1)
            assertThat(updated[0].id).isEqualTo(deviation.id)
        }

    // Deletion Operations Tests

    @Test
    fun `deleteForWorkout should remove all deviations for specific workout`() =
        runTest {
            val programme = createProgramme()
            val workout1 = createWorkout(id = "workout-1", programmeId = programme.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme.id)

            workoutDeviationDao.insert(createDeviation(workout1.id, programme.id))
            workoutDeviationDao.insert(createDeviation(workout1.id, programme.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme.id))

            workoutDeviationDao.deleteForWorkout(workout1.id)

            val workout1Deviations = workoutDeviationDao.getDeviationsForWorkout(workout1.id)
            val workout2Deviations = workoutDeviationDao.getDeviationsForWorkout(workout2.id)

            assertThat(workout1Deviations).isEmpty()
            assertThat(workout2Deviations).hasSize(1)
        }

    @Test
    fun `deleteForProgramme should remove all deviations for programme`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme2.id)

            workoutDeviationDao.insert(createDeviation(workout1.id, programme1.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme2.id))

            workoutDeviationDao.deleteForProgramme(programme1.id)

            val programme1Deviations = workoutDeviationDao.getDeviationsForProgramme(programme1.id)
            val programme2Deviations = workoutDeviationDao.getDeviationsForProgramme(programme2.id)

            assertThat(programme1Deviations).isEmpty()
            assertThat(programme2Deviations).hasSize(1)
        }

    @Test
    fun `deleteAllForUser should remove all deviations for user`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val workout1 = createWorkout(id = "workout-1", userId = "user-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", userId = "user-2", programmeId = programme1.id)

            val deviation1 =
                createDeviation(workout1.id, programme1.id).copy(userId = "user-1")
            val deviation2 =
                createDeviation(workout2.id, programme1.id).copy(userId = "user-2")

            workoutDeviationDao.insert(deviation1)
            workoutDeviationDao.insert(deviation2)

            workoutDeviationDao.deleteAllForUser("user-1")

            val allDeviations = workoutDeviationDao.getDeviationsForProgramme(programme1.id)
            assertThat(allDeviations).hasSize(1)
            assertThat(allDeviations[0].userId).isEqualTo("user-2")
        }

    @Test
    fun `deleteAllWhereUserIdIsNull should remove only null userId deviations`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            val deviationWithUser = createDeviation(workout.id, programme.id).copy(userId = "test-user")
            val deviationWithoutUser = createDeviation(workout.id, programme.id).copy(userId = null)

            workoutDeviationDao.insert(deviationWithUser)
            workoutDeviationDao.insert(deviationWithoutUser)

            workoutDeviationDao.deleteAllWhereUserIdIsNull()

            val allDeviations = workoutDeviationDao.getDeviationsForProgramme(programme.id)
            assertThat(allDeviations).hasSize(1)
            assertThat(allDeviations[0].userId).isEqualTo("test-user")
        }

    @Test
    fun `deleteAll should remove all deviations from database`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme2.id)

            workoutDeviationDao.insert(createDeviation(workout1.id, programme1.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme2.id))

            workoutDeviationDao.deleteAll()

            val programme1Deviations = workoutDeviationDao.getDeviationsForProgramme(programme1.id)
            val programme2Deviations = workoutDeviationDao.getDeviationsForProgramme(programme2.id)

            assertThat(programme1Deviations).isEmpty()
            assertThat(programme2Deviations).isEmpty()
        }

    // Aggregation Query Tests

    @Test
    fun `getProgrammeIdsWithDeviations should return distinct programme IDs ordered by most recent`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val programme3 = createProgramme(id = "programme-3")

            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme2.id)
            val workout3 = createWorkout(id = "workout-3", programmeId = programme3.id)

            val now = LocalDateTime.now()

            // Insert deviations with different timestamps
            workoutDeviationDao.insert(
                createDeviation(workout1.id, programme1.id, timestamp = now.minusDays(3)),
            )
            workoutDeviationDao.insert(
                createDeviation(workout2.id, programme2.id, timestamp = now.minusDays(1)),
            )
            workoutDeviationDao.insert(
                createDeviation(workout3.id, programme3.id, timestamp = now.minusDays(2)),
            )

            val programmeIds = workoutDeviationDao.getProgrammeIdsWithDeviations()

            assertThat(programmeIds).hasSize(3)
            // Most recent first
            assertThat(programmeIds[0]).isEqualTo(programme2.id)
            assertThat(programmeIds[1]).isEqualTo(programme3.id)
            assertThat(programmeIds[2]).isEqualTo(programme1.id)
        }

    @Test
    fun `getProgrammeIdsWithDeviations should return distinct programme IDs when multiple deviations exist`() =
        runTest {
            val programme = createProgramme()
            val workout1 = createWorkout(id = "workout-1", programmeId = programme.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme.id)

            // Multiple deviations for same programme
            workoutDeviationDao.insert(createDeviation(workout1.id, programme.id))
            workoutDeviationDao.insert(createDeviation(workout2.id, programme.id))

            val programmeIds = workoutDeviationDao.getProgrammeIdsWithDeviations()

            assertThat(programmeIds).hasSize(1)
            assertThat(programmeIds[0]).isEqualTo(programme.id)
        }

    @Test
    fun `getMostRecentProgrammeWithDeviations should return programme with most recent deviation`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2 = createWorkout(id = "workout-2", programmeId = programme2.id)

            val now = LocalDateTime.now()

            // Programme 2 has more recent deviation
            workoutDeviationDao.insert(
                createDeviation(workout1.id, programme1.id, timestamp = now.minusDays(5)),
            )
            workoutDeviationDao.insert(
                createDeviation(workout2.id, programme2.id, timestamp = now.minusDays(1)),
            )

            val mostRecentProgramme = workoutDeviationDao.getMostRecentProgrammeWithDeviations()

            assertThat(mostRecentProgramme).isEqualTo(programme2.id)
        }

    @Test
    fun `getMostRecentProgrammeWithDeviations should return null when no deviations exist`() =
        runTest {
            val mostRecentProgramme = workoutDeviationDao.getMostRecentProgrammeWithDeviations()

            assertThat(mostRecentProgramme).isNull()
        }

    @Test
    fun `getDeviationsForMostRecentProgramme should return all deviations for most active programme`() =
        runTest {
            val programme1 = createProgramme(id = "programme-1")
            val programme2 = createProgramme(id = "programme-2")
            val workout1 = createWorkout(id = "workout-1", programmeId = programme1.id)
            val workout2a = createWorkout(id = "workout-2a", programmeId = programme2.id)
            val workout2b = createWorkout(id = "workout-2b", programmeId = programme2.id)

            val now = LocalDateTime.now()

            // Programme 1 has older deviation
            workoutDeviationDao.insert(
                createDeviation(workout1.id, programme1.id, timestamp = now.minusDays(5)),
            )

            // Programme 2 has multiple recent deviations
            workoutDeviationDao.insert(
                createDeviation(workout2a.id, programme2.id, timestamp = now.minusDays(2)),
            )
            workoutDeviationDao.insert(
                createDeviation(workout2b.id, programme2.id, timestamp = now.minusDays(1)),
            )

            val deviations = workoutDeviationDao.getDeviationsForMostRecentProgramme()

            // Should return both deviations from programme 2
            assertThat(deviations).hasSize(2)
            assertThat(deviations.all { it.programmeId == programme2.id }).isTrue()
        }

    @Test
    fun `getDeviationsForMostRecentProgramme should return empty list when no deviations exist`() =
        runTest {
            val deviations = workoutDeviationDao.getDeviationsForMostRecentProgramme()

            assertThat(deviations).isEmpty()
        }

    @Test
    fun `foreign key cascade should delete deviations when workout is deleted`() =
        runTest {
            val programme = createProgramme()
            val workout = createWorkout(programmeId = programme.id)

            workoutDeviationDao.insert(createDeviation(workout.id, programme.id))

            // Verify deviation exists
            val beforeDelete = workoutDeviationDao.getDeviationsForWorkout(workout.id)
            assertThat(beforeDelete).hasSize(1)

            // Delete the workout - should cascade to deviations
            workoutDao.deleteWorkout(workout.id)

            // Deviation should be gone
            val afterDelete = workoutDeviationDao.getDeviationsForWorkout(workout.id)
            assertThat(afterDelete).isEmpty()
        }
}
