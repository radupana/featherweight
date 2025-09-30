package com.github.radupana.featherweight.viewmodel

import android.app.Application
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkConstructor
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
    private lateinit var viewModel: ExerciseMappingViewModel

    private val testExercises =
        listOf(
            ExerciseVariation(
                id = "1",
                coreExerciseId = "1",
                name = "Barbell Back Squat",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            ),
            ExerciseVariation(
                id = "2",
                coreExerciseId = "1",
                name = "Barbell Front Squat",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.ADVANCED,
                requiresWeight = true,
            ),
            ExerciseVariation(
                id = "3",
                coreExerciseId = "1",
                name = "Barbell Hack Squat",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            ),
            ExerciseVariation(
                id = "4",
                coreExerciseId = "1",
                name = "Dumbbell Bulgarian Split Squat",
                equipment = Equipment.DUMBBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            ),
            ExerciseVariation(
                id = "5",
                coreExerciseId = "2",
                name = "Machine Leg Press",
                equipment = Equipment.MACHINE,
                difficulty = ExerciseDifficulty.BEGINNER,
                requiresWeight = true,
            ),
            ExerciseVariation(
                id = "6",
                coreExerciseId = "3",
                name = "Barbell Bench Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            ),
        )

    @Before
    fun setup() {
        application = mockk(relaxed = true)

        // Mock the FeatherweightRepository constructor
        mockkConstructor(FeatherweightRepository::class)

        // Setup the repository to return test exercises
        coEvery { anyConstructed<FeatherweightRepository>().getAllExercises() } returns testExercises

        viewModel = ExerciseMappingViewModel(application)
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
    fun `searchExercises with 'Barbell Squat' finds all barbell squat variations`() =
        runTest {
            // When
            viewModel.searchExercises("Barbell Squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results).hasSize(3)
            assertThat(results.map { it.name }).containsExactly(
                "Barbell Back Squat",
                "Barbell Front Squat",
                "Barbell Hack Squat",
            )
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
    fun `searchExercises does not find exercises missing search terms`() =
        runTest {
            // When - searching for "Barbell Squat" should NOT find "Machine Leg Press"
            viewModel.searchExercises("Barbell Squat")

            // Then
            val results = viewModel.searchResults.first()
            assertThat(results.map { it.name }).doesNotContain("Machine Leg Press")
            assertThat(results.map { it.name }).doesNotContain("Dumbbell Bulgarian Split Squat")
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
            assertThat(results).hasSize(3)
            assertThat(results.map { it.name }).contains("Barbell Back Squat")
        }

    @Test
    fun `searchExercises respects MAX_SEARCH_RESULTS limit`() =
        runTest {
            // Create a large list of exercises
            val manyExercises =
                (1..30).map { i ->
                    ExerciseVariation(
                        id = i.toString(),
                        coreExerciseId = "1",
                        name = "Test Squat Exercise $i",
                        equipment = Equipment.BARBELL,
                        difficulty = ExerciseDifficulty.INTERMEDIATE,
                        requiresWeight = true,
                    )
                }
            coEvery { anyConstructed<FeatherweightRepository>().getAllExercises() } returns manyExercises

            // Reinitialize viewModel to load new exercises
            viewModel = ExerciseMappingViewModel(application)

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
                    ExerciseVariation(
                        id = "7",
                        coreExerciseId = "1",
                        name = "Squat", // Exact match for "squat"
                        equipment = Equipment.BODYWEIGHT,
                        difficulty = ExerciseDifficulty.BEGINNER,
                        requiresWeight = false,
                    )
            coEvery { anyConstructed<FeatherweightRepository>().getAllExercises() } returns exercisesWithExactMatch
            viewModel = ExerciseMappingViewModel(application)

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
            viewModel.mapExercise("Custom Press", null, "New Custom Press") // null means create as custom

            // When
            val finalMappings = viewModel.getFinalMappings()

            // Then
            assertThat(finalMappings).hasSize(2)
            assertThat(finalMappings["Custom Squat"]).isEqualTo("1")
            assertThat(finalMappings["Custom Press"]).isNull()
        }
}
