package com.github.radupana.featherweight.data.exercise

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
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ExerciseVariationDaoTest {
    
    private lateinit var database: FeatherweightDatabase
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var exerciseCoreDao: ExerciseCoreDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            context,
            FeatherweightDatabase::class.java
        ).allowMainThreadQueries().build()
        
        exerciseVariationDao = database.exerciseVariationDao()
        exerciseCoreDao = database.exerciseCoreDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testUniqueConstraint_preventsDuplicateNames() = runBlocking<Unit> {
        // Given: Create a core exercise first
        val core = ExerciseCore(
            id = 0,
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PRESS,
            isCompound = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val coreId = exerciseCoreDao.insertExerciseCore(core)
        
        // When: Insert first variation
        val variation1 = ExerciseVariation(
            id = 0,
            coreExerciseId = coreId,
            name = "Barbell Overhead Press",
            equipment = Equipment.BARBELL,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            requiresWeight = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val id1 = exerciseVariationDao.insertExerciseVariation(variation1)
        assertTrue("First insert should succeed", id1 > 0)
        
        // When: Try to insert duplicate with same name (should be ignored due to OnConflictStrategy.IGNORE)
        val variation2 = ExerciseVariation(
            id = 0,
            coreExerciseId = coreId,
            name = "Barbell Overhead Press", // Same name!
            equipment = Equipment.BARBELL,
            difficulty = ExerciseDifficulty.ADVANCED, // Different difficulty
            requiresWeight = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val id2 = exerciseVariationDao.insertExerciseVariation(variation2)
        
        // Then: Second insert should be ignored (returns -1 for IGNORE strategy)
        assertEquals("Duplicate insert should be ignored", -1L, id2)
        
        // Verify only one exercise exists
        val allVariations = exerciseVariationDao.getAllExerciseVariations()
        assertEquals("Only one variation should exist", 1, allVariations.size)
        assertEquals("Barbell Overhead Press", allVariations[0].name)
        assertEquals("Original difficulty should remain", ExerciseDifficulty.INTERMEDIATE, allVariations[0].difficulty)
    }
    
    @Test
    fun testInsertVariations_allowsDifferentNames() = runBlocking<Unit> {
        // Given: Create core exercises
        val core1 = ExerciseCore(
            id = 0,
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PRESS,
            isCompound = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val coreId1 = exerciseCoreDao.insertExerciseCore(core1)
        
        val core2 = ExerciseCore(
            id = 0,
            name = "Shoulder Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PRESS,
            isCompound = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val coreId2 = exerciseCoreDao.insertExerciseCore(core2)
        
        // When: Insert different variations
        val barbellPress = ExerciseVariation(
            id = 0,
            coreExerciseId = coreId1,
            name = "Barbell Overhead Press",
            equipment = Equipment.BARBELL,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            requiresWeight = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val dumbbellPress = ExerciseVariation(
            id = 0,
            coreExerciseId = coreId2,
            name = "Dumbbell Shoulder Press",
            equipment = Equipment.DUMBBELL,
            difficulty = ExerciseDifficulty.BEGINNER,
            requiresWeight = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val id1 = exerciseVariationDao.insertExerciseVariation(barbellPress)
        val id2 = exerciseVariationDao.insertExerciseVariation(dumbbellPress)
        
        // Then: Both should be inserted successfully
        assertTrue("First insert should succeed", id1 > 0)
        assertTrue("Second insert should succeed", id2 > 0)
        assertNotEquals("IDs should be different", id1, id2)
        
        val allVariations = exerciseVariationDao.getAllExerciseVariations()
        assertEquals("Both variations should exist", 2, allVariations.size)
        
        val names = allVariations.map { it.name }.sorted()
        assertEquals("Barbell Overhead Press", names[0])
        assertEquals("Dumbbell Shoulder Press", names[1])
    }
    
    @Test
    fun testFindVariationByName_isCaseInsensitive() = runBlocking<Unit> {
        // Given: Create and insert a variation
        val core = ExerciseCore(
            id = 0,
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PRESS,
            isCompound = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val coreId = exerciseCoreDao.insertExerciseCore(core)
        
        val variation = ExerciseVariation(
            id = 0,
            coreExerciseId = coreId,
            name = "Barbell Overhead Press",
            equipment = Equipment.BARBELL,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            requiresWeight = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        exerciseVariationDao.insertExerciseVariation(variation)
        
        // When: Search with different cases
        val found1 = exerciseVariationDao.findVariationByName("barbell overhead press")
        val found2 = exerciseVariationDao.findVariationByName("BARBELL OVERHEAD PRESS")
        val found3 = exerciseVariationDao.findVariationByName("Barbell Overhead Press")
        
        // Then: All should find the same exercise
        assertNotNull("Should find with lowercase", found1)
        assertNotNull("Should find with uppercase", found2)
        assertNotNull("Should find with mixed case", found3)
        assertEquals("All should find same exercise", found1?.id, found2?.id)
        assertEquals("All should find same exercise", found2?.id, found3?.id)
    }
    
    @Test
    fun testBatchInsert_ignoresDuplicates() = runBlocking<Unit> {
        // Given: Create a core exercise
        val core = ExerciseCore(
            id = 0,
            name = "Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PRESS,
            isCompound = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val coreId = exerciseCoreDao.insertExerciseCore(core)
        
        // When: Insert batch with duplicates
        val variations = listOf(
            ExerciseVariation(
                id = 0,
                coreExerciseId = coreId,
                name = "Barbell Overhead Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true
            ),
            ExerciseVariation(
                id = 0,
                coreExerciseId = coreId,
                name = "Dumbbell Shoulder Press",
                equipment = Equipment.DUMBBELL,
                difficulty = ExerciseDifficulty.BEGINNER,
                requiresWeight = true
            ),
            ExerciseVariation(
                id = 0,
                coreExerciseId = coreId,
                name = "Barbell Overhead Press", // Duplicate!
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.ADVANCED,
                requiresWeight = true
            )
        )
        
        val ids = exerciseVariationDao.insertExerciseVariations(variations)
        
        // Then: Should have 2 successful inserts, 1 ignored
        val successfulInserts = ids.filter { it > 0 }.size
        assertEquals("Should have 2 successful inserts", 2, successfulInserts)
        
        val allVariations = exerciseVariationDao.getAllExerciseVariations()
        assertEquals("Should have 2 exercises total", 2, allVariations.size)
        
        // Verify the first version of duplicate was kept
        val barbellPress = allVariations.find { it.name == "Barbell Overhead Press" }
        assertEquals("Original difficulty should be kept", ExerciseDifficulty.INTERMEDIATE, barbellPress?.difficulty)
    }
}
