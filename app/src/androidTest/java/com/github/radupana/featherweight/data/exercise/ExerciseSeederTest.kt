package com.github.radupana.featherweight.data.exercise

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.radupana.featherweight.data.FeatherweightDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSeederTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var context: Context
    private lateinit var exerciseSeeder: ExerciseSeeder
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseCoreDao: ExerciseCoreDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var variationMuscleDao: VariationMuscleDao
    private lateinit var variationAliasDao: VariationAliasDao
    private lateinit var variationInstructionDao: VariationInstructionDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create an in-memory database for testing
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    FeatherweightDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        // Get DAOs
        exerciseDao = database.exerciseDao()
        exerciseCoreDao = database.exerciseCoreDao()
        exerciseVariationDao = database.exerciseVariationDao()
        variationMuscleDao = database.variationMuscleDao()
        variationAliasDao = database.variationAliasDao()
        variationInstructionDao = database.variationInstructionDao()

        // Create seeder with real context to access assets
        exerciseSeeder =
            ExerciseSeeder(
                exerciseCoreDao = exerciseCoreDao,
                exerciseVariationDao = exerciseVariationDao,
                variationMuscleDao = variationMuscleDao,
                variationAliasDao = variationAliasDao,
                variationInstructionDao = variationInstructionDao,
                context = context,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSeedExercises_loadsAllExercisesFromJson() =
        runBlocking<Unit> {
            // Given: Empty database
            val initialCount = exerciseVariationDao.getAllExerciseVariations().size
            assertEquals(0, initialCount)

            // When: Seed exercises
            exerciseSeeder.seedExercises()

            // Then: All exercises should be loaded (should be around 489 unique exercises)
            val variations = exerciseVariationDao.getAllExerciseVariations()
            assertTrue("Expected more than 450 exercises, got ${variations.size}", variations.size > 450)
            assertTrue("Expected less than 550 exercises, got ${variations.size}", variations.size < 550)

            // Verify no duplicates by checking unique names
            val uniqueNames = variations.map { it.name }.toSet()
            assertEquals("Found duplicate exercise names", variations.size, uniqueNames.size)
        }

    @Test
    fun testSeedExercises_doesNotDuplicateOnSecondCall() =
        runBlocking<Unit> {
            // Given: Seed once
            exerciseSeeder.seedExercises()
            val firstCount = exerciseVariationDao.getAllExerciseVariations().size

            // When: Try to seed again
            exerciseSeeder.seedExercises()

            // Then: Count should remain the same (no duplicates)
            val secondCount = exerciseVariationDao.getAllExerciseVariations().size
            assertEquals(firstCount, secondCount)
        }

    @Test
    fun testSeedExercises_createsCorrectExerciseHierarchy() =
        runBlocking<Unit> {
            // When: Seed exercises
            exerciseSeeder.seedExercises()

            // Then: Verify specific exercises exist with correct structure
            // Check for "Barbell Overhead Press"
            val barbellPress = exerciseVariationDao.findVariationByName("Barbell Overhead Press")
            assertNotNull("Barbell Overhead Press not found", barbellPress)
            assertEquals(Equipment.BARBELL, barbellPress?.equipment)

            // Check for "Dumbbell Shoulder Press"
            val dumbbellPress = exerciseVariationDao.findVariationByName("Dumbbell Shoulder Press")
            assertNotNull("Dumbbell Shoulder Press not found", dumbbellPress)
            assertEquals(Equipment.DUMBBELL, dumbbellPress?.equipment)

            // Check they have different core exercises (since they're different movements)
            assertNotEquals("Should have different core exercises", barbellPress?.coreExerciseId, dumbbellPress?.coreExerciseId)
        }

    @Test
    fun testSeedExercises_createsAliasesCorrectly() =
        runBlocking<Unit> {
            // When: Seed exercises
            exerciseSeeder.seedExercises()

            // Then: Check aliases are created
            val barbellPress = exerciseVariationDao.findVariationByName("Barbell Overhead Press")
            assertNotNull("Barbell Overhead Press not found", barbellPress)

            val aliases = variationAliasDao.getAliasesForVariation(barbellPress!!.id)
            assertTrue("No aliases found", aliases.isNotEmpty())

            // Should have aliases like "OHP", "Overhead Press", "Shoulder Press"
            val aliasStrings = aliases.map { it.alias }
            assertTrue("OHP alias not found", aliasStrings.contains("OHP"))
        }

    @Test
    fun testSeedExercises_createsMuscleGroupsCorrectly() =
        runBlocking<Unit> {
            // When: Seed exercises
            exerciseSeeder.seedExercises()

            // Then: Check muscle groups are created
            val barbellPress = exerciseVariationDao.findVariationByName("Barbell Overhead Press")
            assertNotNull("Barbell Overhead Press not found for muscle test", barbellPress)

            val muscles = variationMuscleDao.getMusclesForVariation(barbellPress!!.id)
            assertTrue("No muscles found", muscles.isNotEmpty())

            // Should have shoulders as primary muscle
            val primaryMuscle = muscles.find { it.isPrimary }
            assertNotNull("Primary muscle not found", primaryMuscle)
            assertEquals(MuscleGroup.SHOULDERS, primaryMuscle?.muscle)
        }

    @Test
    fun testSeedExercises_handlesSquatVariationsCorrectly() =
        runBlocking<Unit> {
            // When: Seed exercises
            exerciseSeeder.seedExercises()

            // Then: Verify all squat variations exist
            val barbellBackSquat = exerciseVariationDao.findVariationByName("Barbell Back Squat")
            val barbellFrontSquat = exerciseVariationDao.findVariationByName("Barbell Front Squat")
            val dumbbellSquat = exerciseVariationDao.findVariationByName("Dumbbell Bulgarian Split Squat")

            assertNotNull("Barbell Back Squat not found", barbellBackSquat)
            assertNotNull("Barbell Front Squat not found", barbellFrontSquat)
            assertNotNull("Dumbbell Bulgarian Split Squat not found", dumbbellSquat)

            // All should be unique exercises
            val ids = setOf(barbellBackSquat?.id, barbellFrontSquat?.id, dumbbellSquat?.id)
            assertEquals("All exercises should have different IDs", 3, ids.size)
        }

    @Test
    fun testExerciseSearch_findsExercisesCorrectly() =
        runBlocking<Unit> {
            // Given: Seeded database
            exerciseSeeder.seedExercises()

            // When: Search for shoulder exercises
            val allExercises = exerciseDao.getAllExercises()
            val shoulderExercises =
                allExercises.filter {
                    it.name.contains("shoulder", ignoreCase = true) ||
                        it.name.contains("press", ignoreCase = true)
                }

            // Then: Should find various press exercises
            assertTrue("No shoulder/press exercises found", shoulderExercises.isNotEmpty())

            // Should include both barbell and dumbbell variations
            val exerciseNames = shoulderExercises.map { it.name }
            assertTrue("No Barbell exercises found", exerciseNames.any { it.contains("Barbell") })
            assertTrue("No Dumbbell exercises found", exerciseNames.any { it.contains("Dumbbell") })
        }
}
