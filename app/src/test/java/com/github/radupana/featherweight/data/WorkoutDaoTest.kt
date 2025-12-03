package com.github.radupana.featherweight.data

import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for WorkoutDao.
 *
 * This demonstrates the pattern for testing DAOs:
 * - Extends BaseDaoTest for database setup
 * - Uses runTest for coroutine testing
 * - Uses Truth assertions
 * - Tests actual database queries (not mocked)
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a valid programme for testing.
     * Programmes have validation requirements in their init block.
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

    // CRUD Operations Tests

    @Test
    fun `insertWorkout should insert workout into database`() =
        runTest {
            val workout =
                Workout(
                    userId = "test-user",
                    name = "Morning Workout",
                    notes = "Test workout",
                    date = LocalDateTime.of(2025, 1, 15, 8, 0),
                    status = WorkoutStatus.NOT_STARTED,
                )

            workoutDao.insertWorkout(workout)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.name).isEqualTo("Morning Workout")
            assertThat(retrieved?.userId).isEqualTo("test-user")
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        }

    @Test
    fun `getWorkoutById should return null for non-existent workout`() =
        runTest {
            val result = workoutDao.getWorkoutById("non-existent-id")

            assertThat(result).isNull()
        }

    @Test
    fun `updateWorkout should modify existing workout`() =
        runTest {
            val workout =
                Workout(
                    userId = "test-user",
                    name = "Original Name",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )

            workoutDao.insertWorkout(workout)

            val updated = workout.copy(name = "Updated Name", status = WorkoutStatus.IN_PROGRESS)
            workoutDao.updateWorkout(updated)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved?.name).isEqualTo("Updated Name")
            assertThat(retrieved?.status).isEqualTo(WorkoutStatus.IN_PROGRESS)
        }

    @Test
    fun `upsertWorkout should insert new workout`() =
        runTest {
            val workout =
                Workout(
                    userId = "test-user",
                    name = "Upsert Test",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )

            workoutDao.upsertWorkout(workout)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.name).isEqualTo("Upsert Test")
        }

    @Test
    fun `upsertWorkout should update existing workout`() =
        runTest {
            val workout =
                Workout(
                    userId = "test-user",
                    name = "Original",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )

            workoutDao.insertWorkout(workout)

            val updated = workout.copy(name = "Updated via Upsert")
            workoutDao.upsertWorkout(updated)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved?.name).isEqualTo("Updated via Upsert")
        }

    @Test
    fun `deleteWorkout should remove workout from database`() =
        runTest {
            val workout =
                Workout(
                    userId = "test-user",
                    name = "To Delete",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                )

            workoutDao.insertWorkout(workout)
            assertThat(workoutDao.getWorkoutById(workout.id)).isNotNull()

            workoutDao.deleteWorkout(workout.id)

            val retrieved = workoutDao.getWorkoutById(workout.id)
            assertThat(retrieved).isNull()
        }

    // Query Tests

    @Test
    fun `getAllWorkouts should return all workouts for user sorted by date descending`() =
        runTest {
            val now = LocalDateTime.of(2025, 1, 15, 12, 0)
            val workout1 =
                Workout(
                    userId = "user1",
                    name = "Workout 1",
                    date = now.minusDays(2),
                    status = WorkoutStatus.COMPLETED,
                )
            val workout2 =
                Workout(
                    userId = "user1",
                    name = "Workout 2",
                    date = now,
                    status = WorkoutStatus.COMPLETED,
                )
            val workout3 =
                Workout(
                    userId = "user2",
                    name = "Workout 3",
                    date = now.minusDays(1),
                    status = WorkoutStatus.COMPLETED,
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)
            workoutDao.insertWorkout(workout3)

            val user1Workouts = workoutDao.getAllWorkouts("user1")

            assertThat(user1Workouts).hasSize(2)
            assertThat(user1Workouts[0].name).isEqualTo("Workout 2") // Most recent first
            assertThat(user1Workouts[1].name).isEqualTo("Workout 1")
        }

    @Test
    fun `getRecentWorkouts should limit number of results`() =
        runTest {
            val now = LocalDateTime.now()

            for (i in 1..10) {
                val workout =
                    Workout(
                        userId = "test-user",
                        name = "Workout $i",
                        date = now.minusDays(i.toLong()),
                        status = WorkoutStatus.COMPLETED,
                    )
                workoutDao.insertWorkout(workout)
            }

            val recent = workoutDao.getRecentWorkouts("test-user", 5)

            assertThat(recent).hasSize(5)
            assertThat(recent[0].name).isEqualTo("Workout 1") // Most recent
            assertThat(recent[4].name).isEqualTo("Workout 5")
        }

    @Test
    fun `getWorkoutsInDateRange should filter by date range`() =
        runTest {
            val baseDate = LocalDateTime.of(2025, 1, 15, 12, 0)
            val workout1 =
                Workout(
                    userId = "test-user",
                    name = "Before Range",
                    date = baseDate.minusDays(10),
                    status = WorkoutStatus.COMPLETED,
                )
            val workout2 =
                Workout(
                    userId = "test-user",
                    name = "In Range 1",
                    date = baseDate,
                    status = WorkoutStatus.COMPLETED,
                )
            val workout3 =
                Workout(
                    userId = "test-user",
                    name = "In Range 2",
                    date = baseDate.plusDays(2),
                    status = WorkoutStatus.COMPLETED,
                )
            val workout4 =
                Workout(
                    userId = "test-user",
                    name = "After Range",
                    date = baseDate.plusDays(10),
                    status = WorkoutStatus.COMPLETED,
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)
            workoutDao.insertWorkout(workout3)
            workoutDao.insertWorkout(workout4)

            val startDate = baseDate.minusDays(1)
            val endDate = baseDate.plusDays(5)
            val workoutsInRange = workoutDao.getWorkoutsInDateRange("test-user", startDate, endDate)

            assertThat(workoutsInRange).hasSize(2)
            assertThat(workoutsInRange.map { it.name }).containsExactly("In Range 1", "In Range 2")
        }

    @Test
    fun `deleteAllWorkouts should remove all workouts`() =
        runTest {
            val workout1 =
                Workout(
                    userId = "user1",
                    name = "Workout 1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )
            val workout2 =
                Workout(
                    userId = "user2",
                    name = "Workout 2",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)

            workoutDao.deleteAllWorkouts()

            val allWorkouts1 = workoutDao.getAllWorkouts("user1")
            val allWorkouts2 = workoutDao.getAllWorkouts("user2")
            assertThat(allWorkouts1).isEmpty()
            assertThat(allWorkouts2).isEmpty()
        }

    @Test
    fun `deleteAllForUser should only delete workouts for specified user`() =
        runTest {
            val workout1 =
                Workout(
                    userId = "user1",
                    name = "User 1 Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )
            val workout2 =
                Workout(
                    userId = "user2",
                    name = "User 2 Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)

            workoutDao.deleteAllForUser("user1")

            val user1Workouts = workoutDao.getAllWorkouts("user1")
            val user2Workouts = workoutDao.getAllWorkouts("user2")

            assertThat(user1Workouts).isEmpty()
            assertThat(user2Workouts).hasSize(1)
            assertThat(user2Workouts[0].name).isEqualTo("User 2 Workout")
        }

    // Programme-related Tests

    @Test
    fun `getWorkoutsByProgramme should return workouts for programme`() =
        runTest {
            // Create programmes first (foreign key constraint)
            createProgramme(id = "programme-1", name = "Programme 1")
            createProgramme(id = "programme-2", name = "Programme 2")

            val workout1 =
                Workout(
                    userId = "test-user",
                    name = "Programme Workout 1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                )
            val workout2 =
                Workout(
                    userId = "test-user",
                    name = "Programme Workout 2",
                    date = LocalDateTime.now().minusDays(1),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                )
            val workout3 =
                Workout(
                    userId = "test-user",
                    name = "Other Programme",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-2",
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)
            workoutDao.insertWorkout(workout3)

            val programmeWorkouts = workoutDao.getWorkoutsByProgramme("programme-1")

            assertThat(programmeWorkouts).hasSize(2)
            assertThat(programmeWorkouts[0].name).isEqualTo("Programme Workout 1") // Most recent first
            assertThat(programmeWorkouts[1].name).isEqualTo("Programme Workout 2")
        }

    @Test
    fun `deleteWorkoutsByProgramme should remove all workouts for programme`() =
        runTest {
            // Create programmes first (foreign key constraint)
            createProgramme(id = "programme-1", name = "Programme 1")
            createProgramme(id = "programme-2", name = "Programme 2")

            val workout1 =
                Workout(
                    userId = "test-user",
                    name = "Programme 1 Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                )
            val workout2 =
                Workout(
                    userId = "test-user",
                    name = "Programme 2 Workout",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-2",
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)

            workoutDao.deleteWorkoutsByProgramme("programme-1")

            val programme1Workouts = workoutDao.getWorkoutsByProgramme("programme-1")
            val programme2Workouts = workoutDao.getWorkoutsByProgramme("programme-2")

            assertThat(programme1Workouts).isEmpty()
            assertThat(programme2Workouts).hasSize(1)
        }

    @Test
    fun `getInProgressWorkoutCountByProgramme should count non-completed workouts`() =
        runTest {
            // Create programme first (foreign key constraint)
            createProgramme(id = "programme-1")

            val workout1 =
                Workout(
                    userId = "test-user",
                    name = "Completed",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                )
            val workout2 =
                Workout(
                    userId = "test-user",
                    name = "In Progress",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.IN_PROGRESS,
                    programmeId = "programme-1",
                )
            val workout3 =
                Workout(
                    userId = "test-user",
                    name = "Not Started",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.NOT_STARTED,
                    programmeId = "programme-1",
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)
            workoutDao.insertWorkout(workout3)

            val count = workoutDao.getInProgressWorkoutCountByProgramme("programme-1")

            assertThat(count).isEqualTo(2) // IN_PROGRESS and NOT_STARTED
        }

    // WorkoutDateCount Tests

    @Test
    fun `getWorkoutCountsByDateRange should group by exact datetime and count completed workouts`() =
        runTest {
            val date1 = LocalDateTime.of(2025, 1, 15, 8, 0)
            val date2 = LocalDateTime.of(2025, 1, 16, 9, 0)

            // Two workouts at exact same time
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Workout 1",
                    date = date1,
                    status = WorkoutStatus.COMPLETED,
                ),
            )
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Workout 2",
                    date = date1, // Same exact datetime
                    status = WorkoutStatus.COMPLETED,
                ),
            )

            // One workout on date2
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Workout 3",
                    date = date2,
                    status = WorkoutStatus.COMPLETED,
                ),
            )

            val startDate = LocalDateTime.of(2025, 1, 15, 0, 0)
            val endDate = LocalDateTime.of(2025, 1, 17, 0, 0)
            val counts = workoutDao.getWorkoutCountsByDateRange("test-user", startDate, endDate)

            // Query groups by exact datetime, so we expect 2 distinct datetimes
            assertThat(counts).hasSize(2)
            // date1 should have 2 workouts
            val date1Count = counts.find { it.date == date1 }
            assertThat(date1Count?.count).isEqualTo(2)
            // date2 should have 1 workout
            val date2Count = counts.find { it.date == date2 }
            assertThat(date2Count?.count).isEqualTo(1)
        }

    @Test
    fun `getCompletedWorkoutsByProgramme should return only completed workouts sorted by week and day`() =
        runTest {
            // Create programme first (foreign key constraint)
            createProgramme(id = "programme-1")

            val workout1 =
                Workout(
                    userId = "test-user",
                    name = "Week 1 Day 1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                    weekNumber = 1,
                    dayNumber = 1,
                )
            val workout2 =
                Workout(
                    userId = "test-user",
                    name = "Week 1 Day 2",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                    weekNumber = 1,
                    dayNumber = 2,
                )
            val workout3 =
                Workout(
                    userId = "test-user",
                    name = "Week 2 Day 1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.IN_PROGRESS,
                    programmeId = "programme-1",
                    weekNumber = 2,
                    dayNumber = 1,
                )

            workoutDao.insertWorkout(workout1)
            workoutDao.insertWorkout(workout2)
            workoutDao.insertWorkout(workout3)

            val completedWorkouts = workoutDao.getCompletedWorkoutsByProgramme("programme-1")

            assertThat(completedWorkouts).hasSize(2)
            assertThat(completedWorkouts[0].weekNumber).isEqualTo(1)
            assertThat(completedWorkouts[0].dayNumber).isEqualTo(1)
            assertThat(completedWorkouts[1].weekNumber).isEqualTo(1)
            assertThat(completedWorkouts[1].dayNumber).isEqualTo(2)
        }

    @Test
    fun `getCompletedWorkoutCountByProgramme should count only completed workouts`() =
        runTest {
            // Create programme first (foreign key constraint)
            createProgramme(id = "programme-1")

            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Completed 1",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                ),
            )
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Completed 2",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.COMPLETED,
                    programmeId = "programme-1",
                ),
            )
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "In Progress",
                    date = LocalDateTime.now(),
                    status = WorkoutStatus.IN_PROGRESS,
                    programmeId = "programme-1",
                ),
            )

            val count = workoutDao.getCompletedWorkoutCountByProgramme("programme-1")

            assertThat(count).isEqualTo(2)
        }

    // Export-related Tests

    @Test
    fun `getWorkoutsInDateRangePaged should support pagination`() =
        runTest {
            val baseDate = LocalDateTime.of(2025, 1, 15, 12, 0)

            for (i in 1..10) {
                workoutDao.insertWorkout(
                    Workout(
                        userId = "test-user",
                        name = "Workout $i",
                        date = baseDate.plusDays(i.toLong()),
                        status = WorkoutStatus.COMPLETED,
                    ),
                )
            }

            val page1 =
                workoutDao.getWorkoutsInDateRangePaged(
                    userId = "test-user",
                    startDate = baseDate,
                    endDate = baseDate.plusDays(11),
                    limit = 3,
                    offset = 0,
                )

            val page2 =
                workoutDao.getWorkoutsInDateRangePaged(
                    userId = "test-user",
                    startDate = baseDate,
                    endDate = baseDate.plusDays(11),
                    limit = 3,
                    offset = 3,
                )

            assertThat(page1).hasSize(3)
            assertThat(page2).hasSize(3)
            // Verify they're different workouts
            assertThat(page1.map { it.id }).containsNoneIn(page2.map { it.id })
        }

    @Test
    fun `getWorkoutCountInDateRange should exclude specified status`() =
        runTest {
            val baseDate = LocalDateTime.of(2025, 1, 15, 12, 0)

            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Completed",
                    date = baseDate,
                    status = WorkoutStatus.COMPLETED,
                ),
            )
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "In Progress",
                    date = baseDate,
                    status = WorkoutStatus.IN_PROGRESS,
                ),
            )
            workoutDao.insertWorkout(
                Workout(
                    userId = "test-user",
                    name = "Not Started",
                    date = baseDate,
                    status = WorkoutStatus.NOT_STARTED,
                ),
            )

            val count =
                workoutDao.getWorkoutCountInDateRange(
                    userId = "test-user",
                    startDate = baseDate.minusDays(1),
                    endDate = baseDate.plusDays(1),
                    excludeStatus = WorkoutStatus.NOT_STARTED,
                )

            assertThat(count).isEqualTo(2) // Excludes NOT_STARTED
        }
}
