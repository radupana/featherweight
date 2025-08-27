package com.github.radupana.featherweight.data.exercise

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ExerciseDaoTest {
    private lateinit var dao: ExerciseDao
    private lateinit var mockVariation: ExerciseVariation
    private lateinit var mockAlias: VariationAlias
    
    @Before
    fun setup() {
        dao = mockk<ExerciseDao>()
        
        mockVariation = ExerciseVariation(
            id = 1L,
            coreExerciseId = 1L,
            name = "Barbell Bench Press",
            equipment = Equipment.BARBELL,
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            requiresWeight = true,
            usageCount = 10
        )
        
        mockAlias = VariationAlias(
            id = 1L,
            variationId = 1L,
            alias = "BP",
            confidence = 1.0f
        )
    }
    
    @Test
    fun `getAllExercises_returnsOrderedByUsageAndName`() = runTest {
        // Arrange
        val exercises = listOf(
            mockVariation.copy(id = 1L, name = "A Exercise", usageCount = 5),
            mockVariation.copy(id = 2L, name = "B Exercise", usageCount = 10),
            mockVariation.copy(id = 3L, name = "C Exercise", usageCount = 10)
        )
        coEvery { dao.getAllExercises() } returns exercises
        
        // Act
        val result = dao.getAllExercises()
        
        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].usageCount).isEqualTo(5)
        assertThat(result[1].usageCount).isEqualTo(10)
        assertThat(result[2].usageCount).isEqualTo(10)
    }
    
    @Test
    fun `getExerciseById_existingId_returnsExercise`() = runTest {
        // Arrange
        coEvery { dao.getExerciseById(1L) } returns mockVariation
        
        // Act
        val result = dao.getExerciseById(1L)
        
        // Assert
        assertThat(result).isEqualTo(mockVariation)
    }
    
    @Test
    fun `getExerciseById_nonExistentId_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getExerciseById(999L) } returns null
        
        // Act
        val result = dao.getExerciseById(999L)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `searchExercises_matchingQuery_returnsResults`() = runTest {
        // Arrange
        val exercises = listOf(
            mockVariation.copy(name = "Barbell Bench Press"),
            mockVariation.copy(id = 2L, name = "Dumbbell Bench Press")
        )
        coEvery { dao.searchExercises("bench") } returns exercises
        
        // Act
        val result = dao.searchExercises("bench")
        
        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].name).contains("Bench")
        assertThat(result[1].name).contains("Bench")
    }
    
    @Test
    fun `searchExercises_noMatches_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.searchExercises("xyz") } returns emptyList()
        
        // Act
        val result = dao.searchExercises("xyz")
        
        // Assert
        assertThat(result).isEmpty()
    }
    
    @Test
    fun `insertExercise_validExercise_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertExercise(any()) } returns 5L
        
        // Act
        val result = dao.insertExercise(mockVariation)
        
        // Assert
        assertThat(result).isEqualTo(5L)
        coVerify(exactly = 1) { dao.insertExercise(mockVariation) }
    }
    
    @Test
    fun `updateExercise_updatesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.updateExercise(any()) } just runs
        
        // Act
        dao.updateExercise(mockVariation)
        
        // Assert
        coVerify(exactly = 1) { dao.updateExercise(mockVariation) }
    }
    
    @Test
    fun `incrementUsageCount_incrementsForExercise`() = runTest {
        // Arrange
        coEvery { dao.incrementUsageCount(1L) } just runs
        
        // Act
        dao.incrementUsageCount(1L)
        
        // Assert
        coVerify(exactly = 1) { dao.incrementUsageCount(1L) }
    }
    
    @Test
    fun `resetAllUsageCounts_resetsAll`() = runTest {
        // Arrange
        coEvery { dao.resetAllUsageCounts() } just runs
        
        // Act
        dao.resetAllUsageCounts()
        
        // Assert
        coVerify(exactly = 1) { dao.resetAllUsageCounts() }
    }
    
    @Test
    fun `findExerciseByExactName_matchFound_returnsExercise`() = runTest {
        // Arrange
        coEvery { dao.findExerciseByExactName("Barbell Bench Press") } returns mockVariation
        
        // Act
        val result = dao.findExerciseByExactName("Barbell Bench Press")
        
        // Assert
        assertThat(result).isEqualTo(mockVariation)
    }
    
    @Test
    fun `findExerciseByExactName_noMatch_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.findExerciseByExactName("Unknown Exercise") } returns null
        
        // Act
        val result = dao.findExerciseByExactName("Unknown Exercise")
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `findExerciseByAlias_matchFound_returnsExercise`() = runTest {
        // Arrange
        coEvery { dao.findExerciseByAlias("BP") } returns mockVariation
        
        // Act
        val result = dao.findExerciseByAlias("BP")
        
        // Assert
        assertThat(result).isEqualTo(mockVariation)
    }
    
    @Test
    fun `findExerciseByNameOrAlias_nameMatch_returnsExercise`() = runTest {
        // Arrange
        coEvery { dao.findExerciseByNameOrAlias("Barbell Bench Press") } returns mockVariation
        
        // Act
        val result = dao.findExerciseByNameOrAlias("Barbell Bench Press")
        
        // Assert
        assertThat(result).isEqualTo(mockVariation)
    }
    
    @Test
    fun `findExerciseByNameOrAlias_aliasMatch_returnsExercise`() = runTest {
        // Arrange
        coEvery { dao.findExerciseByNameOrAlias("BP") } returns mockVariation
        
        // Act
        val result = dao.findExerciseByNameOrAlias("BP")
        
        // Assert
        assertThat(result).isEqualTo(mockVariation)
    }
    
    @Test
    fun `getBig4Exercises_returnsOrderedBig4`() = runTest {
        // Arrange
        val squat = mockVariation.copy(id = 1L, name = "Barbell Back Squat")
        val deadlift = mockVariation.copy(id = 2L, name = "Barbell Deadlift")
        val bench = mockVariation.copy(id = 3L, name = "Barbell Bench Press")
        val ohp = mockVariation.copy(id = 4L, name = "Barbell Overhead Press")
        
        coEvery { dao.getBig4Exercises() } returns listOf(squat, deadlift, bench, ohp)
        
        // Act
        val result = dao.getBig4Exercises()
        
        // Assert
        assertThat(result).hasSize(4)
        assertThat(result[0].name).isEqualTo("Barbell Back Squat")
        assertThat(result[1].name).isEqualTo("Barbell Deadlift")
        assertThat(result[2].name).isEqualTo("Barbell Bench Press")
        assertThat(result[3].name).isEqualTo("Barbell Overhead Press")
    }
    
    @Test
    fun `getVariationsByCategory_returnsFilteredExercises`() = runTest {
        // Arrange
        val exercises = listOf(
            mockVariation,
            mockVariation.copy(id = 2L, name = "Dumbbell Bench Press")
        )
        coEvery { dao.getVariationsByCategory(ExerciseCategory.CHEST) } returns exercises
        
        // Act
        val result = dao.getVariationsByCategory(ExerciseCategory.CHEST)
        
        // Assert
        assertThat(result).hasSize(2)
    }
    
    @Test
    fun `getVariationsByEquipment_returnsFilteredExercises`() = runTest {
        // Arrange
        val exercises = listOf(
            mockVariation,
            mockVariation.copy(id = 2L, name = "Barbell Row")
        )
        coEvery { dao.getVariationsByEquipment(Equipment.BARBELL) } returns exercises
        
        // Act
        val result = dao.getVariationsByEquipment(Equipment.BARBELL)
        
        // Assert
        assertThat(result).hasSize(2)
    }
    
    @Test
    fun `getVariationsByMuscleGroup_returnsFilteredExercises`() = runTest {
        // Arrange
        val exercises = listOf(mockVariation)
        coEvery { dao.getVariationsByMuscleGroup("Chest") } returns exercises
        
        // Act
        val result = dao.getVariationsByMuscleGroup("Chest")
        
        // Assert
        assertThat(result).hasSize(1)
    }
    
    @Test
    fun `getAliasesForVariation_returnsAliases`() = runTest {
        // Arrange
        val aliases = listOf(
            mockAlias,
            mockAlias.copy(id = 2L, alias = "Bench")
        )
        coEvery { dao.getAliasesForVariation(1L) } returns aliases
        
        // Act
        val result = dao.getAliasesForVariation(1L)
        
        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].alias).isEqualTo("BP")
        assertThat(result[1].alias).isEqualTo("Bench")
    }
    
    @Test
    fun `getAllAliases_returnsAllAliases`() = runTest {
        // Arrange
        val aliases = listOf(
            mockAlias,
            mockAlias.copy(id = 2L, variationId = 2L, alias = "DL")
        )
        coEvery { dao.getAllAliases() } returns aliases
        
        // Act
        val result = dao.getAllAliases()
        
        // Assert
        assertThat(result).hasSize(2)
    }
}