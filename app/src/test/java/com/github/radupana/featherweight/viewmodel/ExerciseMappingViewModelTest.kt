package com.github.radupana.featherweight.viewmodel

import android.app.Application
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseMappingViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var application: Application
    private lateinit var repository: FeatherweightRepository
    private lateinit var viewModel: ExerciseMappingViewModel

    private val testExercises =
        listOf(
            Exercise(
                id = "1",
                name = "Barbell Back Squat",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
            ),
            Exercise(
                id = "2",
                name = "Barbell Front Squat",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.ADVANCED.name,
                requiresWeight = true,
            ),
            Exercise(
                id = "3",
                name = "Barbell Hack Squat",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
            ),
            Exercise(
                id = "4",
                name = "Dumbbell Bulgarian Split Squat",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                isCompound = true,
                equipment = Equipment.DUMBBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
            ),
            Exercise(
                id = "5",
                name = "Machine Leg Press",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.PUSH.name,
                isCompound = true,
                equipment = Equipment.MACHINE.name,
                difficulty = ExerciseDifficulty.BEGINNER.name,
                requiresWeight = true,
            ),
            Exercise(
                id = "6",
                name = "Barbell Bench Press",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.PUSH.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
            ),
        )

    private val testExercisesWithAliases =
        listOf(
            ExerciseWithAliases(testExercises[0], listOf("Back Squat", "BB Squat")),
            ExerciseWithAliases(testExercises[1], listOf("Front Squat")),
            ExerciseWithAliases(testExercises[2], listOf("Hack Squat")),
            ExerciseWithAliases(testExercises[3], emptyList()),
            ExerciseWithAliases(testExercises[4], emptyList()),
            ExerciseWithAliases(testExercises[5], listOf("Bench Press", "BP")),
        )

    @Before
    fun setup() {
        application = mockk(relaxed = true)

        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0

        repository = mockk()
        coEvery { repository.getAllExercises() } returns testExercises
        coEvery { repository.getAllExercisesWithAliases() } returns testExercisesWithAliases

        viewModel = ExerciseMappingViewModel(application, repository)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `searchExercises with blank query returns empty list`() =
        runTest {
            // When
            viewModel.searchExercises("   ")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).isEmpty()
        }

    @Test
    fun `searchExercises with 'Barbell Squat' finds exercises with at least 50 percent word match`() =
        runTest {
            // When
            viewModel.searchExercises("Barbell Squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).hasSize(5)
            assertThat(results.map { it.name }).contains("Barbell Back Squat")
            assertThat(results.map { it.name }).contains("Barbell Front Squat")
            assertThat(results.map { it.name }).contains("Barbell Hack Squat")
            assertThat(results.map { it.name }).contains("Barbell Bench Press")
            assertThat(results.map { it.name }).contains("Dumbbell Bulgarian Split Squat")
        }

    @Test
    fun `searchExercises with 'squat' finds all squat exercises`() =
        runTest {
            // When
            viewModel.searchExercises("squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).hasSize(4)
            assertThat(results.map { it.name }).contains("Barbell Back Squat")
            assertThat(results.map { it.name }).contains("Barbell Front Squat")
            assertThat(results.map { it.name }).contains("Barbell Hack Squat")
            assertThat(results.map { it.name }).contains("Dumbbell Bulgarian Split Squat")
        }

    @Test
    fun `searchExercises does not find exercises with less than 50 percent word match`() =
        runTest {
            // When - searching for "Barbell Squat" should NOT find "Machine Leg Press" (0 matching words)
            viewModel.searchExercises("Barbell Squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results.map { it.name }).doesNotContain("Machine Leg Press")
        }

    @Test
    fun `searchExercises handles partial word matches`() =
        runTest {
            // When - searching for "bar sq" should find barbell squats
            viewModel.searchExercises("bar sq")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).isNotEmpty()
            assertThat(results.map { it.name }).contains("Barbell Back Squat")
        }

    @Test
    fun `searchExercises is case insensitive`() =
        runTest {
            // When
            viewModel.searchExercises("BARBELL SQUAT")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).hasSize(5)
            assertThat(results.map { it.name }).contains("Barbell Back Squat")
            assertThat(results.map { it.name }).contains("Barbell Front Squat")
            assertThat(results.map { it.name }).contains("Barbell Hack Squat")
        }

    @Test
    fun `searchExercises respects MAX_SEARCH_RESULTS limit`() =
        runTest {
            // Create a large list of exercises
            val manyExercises =
                (1..30).map { i ->
                    Exercise(
                        id = i.toString(),
                        name = "Test Squat Exercise $i",
                        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                        isCompound = true,
                        equipment = Equipment.BARBELL.name,
                        difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                        requiresWeight = true,
                    )
                }
            coEvery { repository.getAllExercises() } returns manyExercises

            // Reinitialize viewModel to load new exercises
            viewModel = ExerciseMappingViewModel(application, repository)

            // When
            viewModel.searchExercises("squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results.size).isAtMost(20) // MAX_SEARCH_RESULTS = 20
        }

    @Test
    fun `clearSearch clears search results`() =
        runTest {
            // Given - perform a search first
            viewModel.searchExercises("squat")
            val initialResults = viewModel.searchResults.first()
            assertThat(initialResults).isNotEmpty()

            // When
            viewModel.clearSearch()

            // Then
            val clearedResults = viewModel.searchResults.first()
            assertThat(clearedResults).isEmpty()
        }

    @Test
    fun `searchExercises sorts results by relevance`() =
        runTest {
            // Add an exact match to test sorting
            val exercisesWithExactMatch =
                testExercises +
                    Exercise(
                        id = "7",
                        name = "Squat", // Exact match for "squat"
                        category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.LEGS.name,
                        movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.SQUAT.name,
                        isCompound = true,
                        equipment = Equipment.BODYWEIGHT.name,
                        difficulty = ExerciseDifficulty.BEGINNER.name,
                        requiresWeight = false,
                    )
            coEvery { repository.getAllExercises() } returns exercisesWithExactMatch
            viewModel = ExerciseMappingViewModel(application, repository)

            // When
            viewModel.searchExercises("squat")

            // Then
            val results = viewModel.searchResults.first()
            // Exact match should be first
            assertThat(results.first().name).isEqualTo("Squat")
        }

    @Test
    fun `mapExercise adds mapping to UI state`() =
        runTest {
            // When
            viewModel.mapExercise(
                originalName = "Custom Squat",
                exerciseId = "1",
                exerciseName = "Barbell Back Squat",
            )

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.mappings).containsKey("Custom Squat")
            assertThat(state.mappings["Custom Squat"]?.exerciseId).isEqualTo("1")
            assertThat(state.mappings["Custom Squat"]?.exerciseName).isEqualTo("Barbell Back Squat")
        }

    @Test
    fun `clearMapping removes mapping from UI state`() =
        runTest {
            // Given
            viewModel.mapExercise(
                originalName = "Custom Squat",
                exerciseId = "1",
                exerciseName = "Barbell Back Squat",
            )
            val stateWithMapping = viewModel.uiState.first()
            assertThat(stateWithMapping.mappings).containsKey("Custom Squat")

            // When
            viewModel.clearMapping("Custom Squat")

            // Then
            val stateWithoutMapping = viewModel.uiState.first()
            assertThat(stateWithoutMapping.mappings).doesNotContainKey("Custom Squat")
        }

    @Test
    fun `allExercisesMapped returns true when all exercises are mapped`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Exercise A", "Exercise B")
            viewModel.mapExercise("Exercise A", "1", "Barbell Back Squat")
            viewModel.mapExercise("Exercise B", "2", "Barbell Front Squat")

            // When
            val allMapped = viewModel.allExercisesMapped(unmatchedExercises)

            // Then
            assertThat(allMapped).isTrue()
        }

    @Test
    fun `allExercisesMapped returns false when some exercises are not mapped`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Exercise A", "Exercise B", "Exercise C")
            viewModel.mapExercise("Exercise A", "1", "Barbell Back Squat")
            // Exercise B and C not mapped

            // When
            val allMapped = viewModel.allExercisesMapped(unmatchedExercises)

            // Then
            assertThat(allMapped).isFalse()
        }

    @Test
    fun `getFinalMappings returns correct mapping structure`() =
        runTest {
            // Given
            viewModel.mapExercise("Custom Squat", "1", "Barbell Back Squat")
            viewModel.mapExercise("Custom Press", null, "New Custom Press")

            // When
            val finalMappings = viewModel.getFinalMappings()

            // Then
            assertThat(finalMappings).hasSize(2)
            assertThat(finalMappings["Custom Squat"]).isEqualTo("1")
            assertThat(finalMappings["Custom Press"]).isNull()
        }

    @Test
    fun `initializeMappings with high confidence match auto-proposes top suggestion`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Back Squat")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).containsKey("Back Squat")
            assertThat(state.suggestions["Back Squat"]).isNotEmpty()
            assertThat(state.mappings).containsKey("Back Squat")
            assertThat(state.mappings["Back Squat"]?.exerciseId).isEqualTo("1")
        }

    @Test
    fun `initializeMappings with medium confidence provides suggestions but no auto-proposal`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Barbell High-Bar Squat")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).containsKey("Barbell High-Bar Squat")
            assertThat(state.suggestions["Barbell High-Bar Squat"]).isNotEmpty()
            assertThat(state.mappings).doesNotContainKey("Barbell High-Bar Squat")
        }

    @Test
    fun `initializeMappings with no matches provides empty suggestions map`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Completely Unknown Exercise Name")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).doesNotContainKey("Completely Unknown Exercise Name")
            assertThat(state.mappings).doesNotContainKey("Completely Unknown Exercise Name")
        }

    @Test
    fun `initializeMappings returns maximum 3 suggestions per exercise`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Squat")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).containsKey("Squat")
            assertThat(state.suggestions["Squat"]?.size).isAtMost(3)
        }

    @Test
    fun `initializeMappings handles multiple unmatched exercises`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("Back Squat", "Bench Press", "Unknown Exercise")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).hasSize(2)
            assertThat(state.suggestions).containsKey("Back Squat")
            assertThat(state.suggestions).containsKey("Bench Press")
            assertThat(state.suggestions).doesNotContainKey("Unknown Exercise")
        }

    @Test
    fun `initializeMappings uses aliases for matching`() =
        runTest {
            // Given
            val unmatchedExercises = listOf("BP")

            // When
            viewModel.initializeMappings(unmatchedExercises)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.first()
            assertThat(state.suggestions).containsKey("BP")
            assertThat(state.suggestions["BP"]).isNotEmpty()
            assertThat(state.suggestions["BP"]?.first()?.name).isEqualTo("Barbell Bench Press")
        }
}
