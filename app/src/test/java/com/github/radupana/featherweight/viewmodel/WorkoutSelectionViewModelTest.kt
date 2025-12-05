package com.github.radupana.featherweight.viewmodel

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class WorkoutSelectionViewModelTest {
    @Test
    fun `WorkoutWithExercises data class stores all fields correctly`() {
        val workout =
            WorkoutWithExercises(
                id = "workout-1",
                name = "Morning Lift",
                date = LocalDateTime.of(2025, 1, 15, 8, 30),
                exercises = listOf("Squat", "Bench Press", "Deadlift"),
                totalSets = 15,
                duration = 3600L,
            )

        assertThat(workout.id).isEqualTo("workout-1")
        assertThat(workout.name).isEqualTo("Morning Lift")
        assertThat(workout.date.hour).isEqualTo(8)
        assertThat(workout.exercises).hasSize(3)
        assertThat(workout.totalSets).isEqualTo(15)
        assertThat(workout.duration).isEqualTo(3600L)
    }

    @Test
    fun `WorkoutWithExercises can have null duration`() {
        val workout =
            WorkoutWithExercises(
                id = "workout-1",
                name = "Quick Session",
                date = LocalDateTime.now(),
                exercises = listOf("Curls"),
                totalSets = 3,
                duration = null,
            )

        assertThat(workout.duration).isNull()
    }

    @Test
    fun `filtering workouts by name returns matching workouts`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("Upper")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            assertThat(result).hasSize(1)
            assertThat(result.first().name).isEqualTo("Upper Body")
        }

    @Test
    fun `filtering workouts by exercise name returns matching workouts`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("squat")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            assertThat(result).hasSize(1)
            assertThat(result.first().name).isEqualTo("Lower Body")
        }

    @Test
    fun `filtering with empty query returns all workouts`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            assertThat(result).hasSize(3)
        }

    @Test
    fun `filtering is case insensitive`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("BENCH")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            assertThat(result).hasSize(1)
            assertThat(result.first().exercises).contains("Bench Press")
        }

    @Test
    fun `filtering with no matches returns empty list`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("nonexistent exercise")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            assertThat(result).isEmpty()
        }

    @Test
    fun `filtering matches partial workout names`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("Body")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            // "Body" matches "Upper Body", "Lower Body", and "Full Body"
            assertThat(result).hasSize(3)
        }

    @Test
    fun `filtering matches partial exercise names`() =
        runTest {
            val workouts = MutableStateFlow(createTestWorkouts())
            val searchQuery = MutableStateFlow("press")

            val filteredWorkouts =
                combine(workouts, searchQuery) { workoutList, query ->
                    if (query.isEmpty()) {
                        workoutList
                    } else {
                        workoutList.filter { workout ->
                            workout.name.contains(query, ignoreCase = true) ||
                                workout.exercises.any { exercise ->
                                    exercise.contains(query, ignoreCase = true)
                                }
                        }
                    }
                }

            val result = filteredWorkouts.first()

            // Should match "Bench Press" in Upper Body, "Leg Press" in Lower Body, and "Overhead Press" in Full Body
            assertThat(result).hasSize(3)
        }

    private fun createTestWorkouts(): List<WorkoutWithExercises> =
        listOf(
            WorkoutWithExercises(
                id = "1",
                name = "Upper Body",
                date = LocalDateTime.of(2025, 1, 15, 9, 0),
                exercises = listOf("Bench Press", "Rows", "Curls"),
                totalSets = 12,
                duration = 3600L,
            ),
            WorkoutWithExercises(
                id = "2",
                name = "Lower Body",
                date = LocalDateTime.of(2025, 1, 14, 10, 0),
                exercises = listOf("Squat", "Leg Press", "Calf Raises"),
                totalSets = 15,
                duration = 4200L,
            ),
            WorkoutWithExercises(
                id = "3",
                name = "Full Body",
                date = LocalDateTime.of(2025, 1, 13, 8, 0),
                exercises = listOf("Deadlift", "Overhead Press", "Pull-ups"),
                totalSets = 9,
                duration = 3000L,
            ),
        )
}
