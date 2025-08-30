package com.github.radupana.featherweight.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseSeeder
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryIntegrationTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var context: Context
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var exerciseSeeder: ExerciseSeeder

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    FeatherweightDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        // Initialize repository with database
        exerciseRepository = ExerciseRepository(db = database)

        // Initialize seeder
        exerciseSeeder =
            ExerciseSeeder(
                exerciseCoreDao = database.exerciseCoreDao(),
                exerciseVariationDao = database.exerciseVariationDao(),
                variationMuscleDao = database.variationMuscleDao(),
                variationAliasDao = database.variationAliasDao(),
                variationInstructionDao = database.variationInstructionDao(),
                context = context,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGetAllExercises_returnsNoDuplicates() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Get all exercises
            val exercises = exerciseRepository.getAllExercises()

            // Then: No duplicates should exist
            val uniqueNames = exercises.map { it.name }.toSet()
            assertEquals(exercises.size, uniqueNames.size)

            // Verify specific exercises exist only once
            val barbellPressCount = exercises.count { it.name == "Barbell Overhead Press" }
            assertEquals(1, barbellPressCount)

            val dumbbellPressCount = exercises.count { it.name == "Dumbbell Shoulder Press" }
            assertEquals(1, dumbbellPressCount)
        }

    @Test
    fun testSearchExercises_findsCorrectExercises() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Search for "shoulder press"
            val results = exerciseRepository.searchExercises("shoulder press")

            // Then: Should find relevant exercises
            assertTrue("Results should not be empty", results.isNotEmpty())

            // Should find Dumbbell Shoulder Press
            val dumbbellPress = results.find { it.name == "Dumbbell Shoulder Press" }
            assertNotNull("Should find Dumbbell Shoulder Press", dumbbellPress)

            // Should also find exercises with "press" in name
            val pressExercises = results.filter { it.name.contains("press", ignoreCase = true) }
            assertTrue("Should find press exercises", pressExercises.isNotEmpty())
        }

    @Test
    fun testGetExercisesByCategory_returnsCorrectExercises() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Get shoulder exercises
            val shoulderExercises = exerciseRepository.getExercisesByCategory(ExerciseCategory.SHOULDERS)

            // Then: Should return shoulder exercises
            assertTrue("Should have shoulder exercises", shoulderExercises.isNotEmpty())

            // Check for specific shoulder exercises
            val exerciseNames = shoulderExercises.map { it.name }
            assertTrue("Should have press exercises", exerciseNames.any { it.contains("Shoulder Press") || it.contains("Overhead Press") })

            // No duplicates
            val uniqueNames = exerciseNames.toSet()
            assertEquals("No duplicates allowed", exerciseNames.size, uniqueNames.size)
        }

    @Test
    fun testGetAllExercisesWithAliases_includesAliases() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Get exercises with aliases
            val exercisesWithAliases = exerciseRepository.getAllExercisesWithAliases()

            // Then: Should have exercises with aliases
            assertTrue("Should have exercises", exercisesWithAliases.isNotEmpty())

            // Find Barbell Overhead Press and check its aliases
            val barbellPress =
                exercisesWithAliases.find {
                    it.variation.name == "Barbell Overhead Press"
                }
            assertNotNull("Should find Barbell Overhead Press", barbellPress)
            assertTrue("Should have aliases", barbellPress?.aliases?.isNotEmpty() == true)

            // Should include common aliases
            val aliases = barbellPress?.aliases ?: emptyList()
            assertTrue("Should have OHP alias", aliases.any { it.contains("OHP", ignoreCase = true) })
        }

    @Test
    fun testCreateCustomExercise_doesNotDuplicate() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Try to create an exercise that already exists
            val existingName = "Barbell Overhead Press"
            val result1 =
                exerciseRepository.createCustomExercise(
                    name = existingName,
                    category = ExerciseCategory.SHOULDERS,
                    primaryMuscles = setOf(MuscleGroup.SHOULDERS),
                    equipment = Equipment.BARBELL,
                    requiresWeight = true,
                )

            // Then: Should return existing exercise or failure, not create duplicate
            // Result type can be success (with existing exercise) or failure
            assertNotNull("Should return result", result1)

            // Verify still only one exercise with that name
            val allExercises = exerciseRepository.getAllExercises()
            val count = allExercises.count { it.name == existingName }
            assertEquals("Should only have one", 1, count)
        }

    @Test
    fun testGetExerciseById_returnsCorrectExercise() =
        runBlocking<Unit> {
            // Given: Seed and get an exercise
            exerciseSeeder.seedExercises()
            val allExercises = exerciseRepository.getAllExercises()
            val targetExercise = allExercises.find { it.name == "Dumbbell Shoulder Press" }
            assertNotNull("Should find target exercise", targetExercise)

            // When: Get by ID
            val retrieved = exerciseRepository.getExerciseById(targetExercise!!.id)

            // Then: Should return the same exercise
            assertNotNull("Should retrieve exercise", retrieved)
            assertEquals(targetExercise.id, retrieved?.id)
            assertEquals("Dumbbell Shoulder Press", retrieved?.name)
            assertEquals(Equipment.DUMBBELL, retrieved?.equipment)
        }

    @Test
    fun testGetExerciseWithDetails_includesAllRelatedData() =
        runBlocking<Unit> {
            // Given: Seed the database
            exerciseSeeder.seedExercises()

            // When: Get exercise with details
            val exercise =
                exerciseRepository.getAllExercises().find {
                    it.name == "Barbell Overhead Press"
                }
            assertNotNull("Should find exercise", exercise)

            // Get variations with aliases to check details
            val exercisesWithAliases = exerciseRepository.getAllExercisesWithAliases()
            val exerciseWithAliases =
                exercisesWithAliases.find {
                    it.variation.id == exercise!!.id
                }

            // Then: Should include all details
            assertNotNull("Should have details", exerciseWithAliases)
            assertEquals("Barbell Overhead Press", exerciseWithAliases?.variation?.name)
            assertTrue("Should have aliases", exerciseWithAliases?.aliases?.isNotEmpty() == true)

            // Check primary muscle is shoulders
            val muscles = database.variationMuscleDao().getMusclesForVariation(exercise!!.id)
            val primaryMuscle = muscles.find { it.isPrimary }
            assertEquals(MuscleGroup.SHOULDERS, primaryMuscle?.muscle)
        }

    @Test
    fun testMultipleSeedAttempts_maintainsSingleSetOfExercises() =
        runBlocking<Unit> {
            // Given: Empty database
            val initialCount = exerciseRepository.getAllExercises().size
            assertEquals(0, initialCount)

            // When: Seed multiple times
            exerciseSeeder.seedExercises()
            val firstCount = exerciseRepository.getAllExercises().size

            exerciseSeeder.seedExercises()
            val secondCount = exerciseRepository.getAllExercises().size

            exerciseSeeder.seedExercises()
            val thirdCount = exerciseRepository.getAllExercises().size

            // Then: Count should remain stable
            assertTrue("Should have exercises", firstCount > 0)
            assertEquals(firstCount, secondCount)
            assertEquals(firstCount, thirdCount)

            // Verify no duplicates
            val exercises = exerciseRepository.getAllExercises()
            val uniqueNames = exercises.map { it.name }.toSet()
            assertEquals(exercises.size, uniqueNames.size)
        }
}
