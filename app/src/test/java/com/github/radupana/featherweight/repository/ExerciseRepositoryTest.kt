package com.github.radupana.featherweight.repository

import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationMuscleDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ExerciseRepositoryTest {
    private lateinit var repository: ExerciseRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var exerciseSwapHistoryDao: ExerciseSwapHistoryDao
    private lateinit var exerciseCoreDao: ExerciseCoreDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var variationMuscleDao: VariationMuscleDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao

    private lateinit var mockCore: ExerciseCore
    private lateinit var mockVariation: ExerciseVariation
    private lateinit var mockExerciseLog: ExerciseLog
    private lateinit var mockSetLog: SetLog

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        db = mockk()
        exerciseDao = mockk<ExerciseDao>()
        exerciseLogDao = mockk<ExerciseLogDao>()
        setLogDao = mockk<SetLogDao>()
        exerciseSwapHistoryDao = mockk<ExerciseSwapHistoryDao>()
        exerciseCoreDao = mockk<ExerciseCoreDao>()
        exerciseVariationDao = mockk<ExerciseVariationDao>()
        variationMuscleDao = mockk<VariationMuscleDao>()
        userExerciseUsageDao = mockk<UserExerciseUsageDao>()

        every { db.exerciseDao() } returns exerciseDao
        every { db.exerciseLogDao() } returns exerciseLogDao
        every { db.setLogDao() } returns setLogDao
        every { db.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { db.exerciseCoreDao() } returns exerciseCoreDao
        every { db.exerciseVariationDao() } returns exerciseVariationDao
        every { db.variationMuscleDao() } returns variationMuscleDao
        every { db.userExerciseUsageDao() } returns userExerciseUsageDao

        val authManager = mockk<AuthenticationManager>(relaxed = true)
        every { authManager.getCurrentUserId() } returns "test-user"
        repository = ExerciseRepository(db, authManager)

        mockCore =
            ExerciseCore(
                id = 1L,
                name = "Bench Press",
                category = ExerciseCategory.CHEST,
                movementPattern = MovementPattern.PUSH,
                isCompound = true,
            )

        mockVariation =
            ExerciseVariation(
                id = 1L,
                coreExerciseId = 1L,
                name = "Barbell Bench Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            )

        mockExerciseLog =
            ExerciseLog(
                id = 1L,
                userId = null,
                workoutId = 1L,
                exerciseVariationId = 1L,
                exerciseOrder = 1,
                notes = "Felt strong",
            )

        mockSetLog =
            SetLog(
                id = 1L,
                userId = null,
                exerciseLogId = 1L,
                setOrder = 1,
                targetReps = 10,
                targetWeight = 100f,
                actualReps = 10,
                actualWeight = 100f,
                actualRpe = 8f,
            )
    }

    @Test
    fun `getAllExercises_returnsAllExercises`() =
        runTest {
            // Arrange
            val exercises = listOf(mockVariation, mockVariation.copy(id = 2L))
            coEvery { exerciseDao.getAllExercises() } returns exercises

            // Act
            val result = repository.getAllExercises()

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(exercises)
        }

    @Test
    fun `getAllExercisesWithAliases_returnsVariationsWithAliases`() =
        runTest {
            // Arrange
            val variations = listOf(mockVariation)
            val aliases =
                listOf(
                    VariationAlias(id = 1L, variationId = 1L, alias = "BP"),
                    VariationAlias(id = 2L, variationId = 1L, alias = "Bench"),
                )
            coEvery { exerciseDao.getAllExercises() } returns variations
            coEvery { exerciseDao.getAliasesForVariation(1L) } returns aliases

            // Act
            val result = repository.getAllExercisesWithAliases()

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].aliases).containsExactly("BP", "Bench")
        }

    @Test
    fun `getAllExerciseNamesIncludingAliases_returnsDistinctSortedNames`() =
        runTest {
            // Arrange
            val variations =
                listOf(
                    mockVariation,
                    mockVariation.copy(id = 2L, name = "Dumbbell Press"),
                )
            val aliases1 = listOf(VariationAlias(id = 1L, variationId = 1L, alias = "BP"))
            val aliases2 = listOf(VariationAlias(id = 2L, variationId = 2L, alias = "DB Press"))

            coEvery { exerciseDao.getAllExercises() } returns variations
            coEvery { exerciseDao.getAliasesForVariation(1L) } returns aliases1
            coEvery { exerciseDao.getAliasesForVariation(2L) } returns aliases2

            // Act
            val result = repository.getAllExerciseNamesIncludingAliases()

            // Assert
            assertThat(result)
                .containsExactly(
                    "BP",
                    "Barbell Bench Press",
                    "DB Press",
                    "Dumbbell Press",
                ).inOrder()
        }

    @Test
    fun `getAllExerciseAliases_returnsAllAliases`() =
        runTest {
            // Arrange
            val aliases =
                listOf(
                    VariationAlias(id = 1L, variationId = 1L, alias = "BP"),
                    VariationAlias(id = 2L, variationId = 2L, alias = "DL"),
                )
            coEvery { exerciseDao.getAllAliases() } returns aliases

            // Act
            val result = repository.getAllExerciseAliases()

            // Assert
            assertThat(result).isEqualTo(aliases)
        }

    @Test
    fun `getAllExercisesWithUsageStats_returnsSortedByUsageAndName`() =
        runTest {
            // Arrange
            val ex1 = mockVariation.copy(id = 1L, name = "A Exercise")
            val ex2 = mockVariation.copy(id = 2L, name = "B Exercise")
            val ex3 = mockVariation.copy(id = 3L, name = "C Exercise")

            coEvery { exerciseDao.getAllExercises() } returns listOf(ex1, ex2, ex3)
            // Since authManager returns "test-user", it will use userExerciseUsageDao
            coEvery { userExerciseUsageDao.getUsage("test-user", 1L, false) } returns
                mockk {
                    every { usageCount } returns 5
                }
            coEvery { userExerciseUsageDao.getUsage("test-user", 2L, false) } returns
                mockk {
                    every { usageCount } returns 10
                }
            coEvery { userExerciseUsageDao.getUsage("test-user", 3L, false) } returns
                mockk {
                    every { usageCount } returns 5
                }

            // Act
            val result = repository.getAllExercisesWithUsageStats()

            // Assert
            assertThat(result).hasSize(3)
            assertThat(result[0].first.name).isEqualTo("B Exercise") // Highest usage
            assertThat(result[0].second).isEqualTo(10)
            assertThat(result[1].first.name).isEqualTo("A Exercise") // Same usage as C, but alphabetically first
            assertThat(result[2].first.name).isEqualTo("C Exercise")
        }

    @Test
    fun `getExerciseById_existingId_returnsExercise`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.getExerciseVariationById(1L) } returns mockVariation

            // Act
            val result = repository.getExerciseById(1L)

            // Assert
            assertThat(result).isEqualTo(mockVariation)
        }

    @Test
    fun `getExerciseById_nonExistentId_returnsNull`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.getExerciseVariationById(999L) } returns null

            // Act
            val result = repository.getExerciseById(999L)

            // Assert
            assertThat(result).isNull()
        }

    @Test
    fun `getExerciseByName_exactMatch_returnsExercise`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.findVariationByExactName("Barbell Bench Press") } returns mockVariation

            // Act
            val result = repository.getExerciseByName("Barbell Bench Press")

            // Assert
            assertThat(result).isEqualTo(mockVariation)
        }

    @Test
    fun `getExerciseByName_aliasMatch_returnsExercise`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.findVariationByExactName("BP") } returns null
            coEvery { exerciseDao.findVariationByAlias("BP") } returns mockVariation

            // Act
            val result = repository.getExerciseByName("BP")

            // Assert
            assertThat(result).isEqualTo(mockVariation)
        }

    @Test
    fun `searchExercises_returnsMatchingExercises`() =
        runTest {
            // Arrange
            val results = listOf(mockVariation, mockVariation.copy(id = 2L))
            coEvery { exerciseDao.searchVariations("bench") } returns results

            // Act
            val result = repository.searchExercises("bench")

            // Assert
            assertThat(result).isEqualTo(results)
        }

    @Test
    fun `getExercisesByCategory_returnsFilteredExercises`() =
        runTest {
            // Arrange
            val exercises = listOf(mockVariation)
            coEvery { exerciseDao.getVariationsByCategory(ExerciseCategory.CHEST) } returns exercises

            // Act
            val result = repository.getExercisesByCategory(ExerciseCategory.CHEST)

            // Assert
            assertThat(result).isEqualTo(exercises)
        }

    @Test
    fun `getExercisesByMuscleGroup_returnsFilteredExercises`() =
        runTest {
            // Arrange
            val exercises = listOf(mockVariation)
            coEvery { exerciseDao.getVariationsByMuscleGroup("Chest") } returns exercises

            // Act
            val result = repository.getExercisesByMuscleGroup("Chest")

            // Assert
            assertThat(result).isEqualTo(exercises)
        }

    @Test
    fun `getExercisesByEquipment_returnsFilteredExercises`() =
        runTest {
            // Arrange
            val exercises = listOf(mockVariation)
            coEvery { exerciseDao.getVariationsByEquipment(Equipment.BARBELL) } returns exercises

            // Act
            val result = repository.getExercisesByEquipment(Equipment.BARBELL)

            // Assert
            assertThat(result).isEqualTo(exercises)
        }

    @Test
    fun `getExercisesForWorkout_returnsExerciseLogs`() =
        runTest {
            // Arrange
            val logs = listOf(mockExerciseLog, mockExerciseLog.copy(id = 2L))
            coEvery { exerciseLogDao.getExerciseLogsForWorkout(1L) } returns logs

            // Act
            val result = repository.getExercisesForWorkout(1L)

            // Assert
            assertThat(result).isEqualTo(logs)
        }

    @Test
    fun `getSetsForExercise_returnsSetLogs`() =
        runTest {
            // Arrange
            val sets = listOf(mockSetLog, mockSetLog.copy(id = 2L, setOrder = 2))
            coEvery { setLogDao.getSetLogsForExercise(1L) } returns sets

            // Act
            val result = repository.getSetsForExercise(1L)

            // Assert
            assertThat(result).isEqualTo(sets)
        }

    @Test
    fun `insertExerciseLog_insertsAndReturnsId`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 1L

            // Act
            val result = repository.insertExerciseLog(mockExerciseLog)

            // Assert
            assertThat(result).isEqualTo(1L)
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byId_insertsAndReturnsId`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L

            // Act
            val result = repository.insertExerciseLogWithExerciseReference(1L, 2L, 3)

            // Assert
            assertThat(result).isEqualTo(5L)
            coVerify {
                exerciseLogDao.insertExerciseLog(
                    match {
                        it.workoutId == 1L &&
                            it.exerciseVariationId == 2L &&
                            it.exerciseOrder == 3
                    },
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byVariation_insertsAndIncrementsUsage`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 5L

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 2,
                    notes = "Test notes",
                )

            // Assert
            assertThat(result).isEqualTo(5L)
            // Usage count increment is handled in FeatherweightRepository, not here
            coVerify {
                exerciseLogDao.insertExerciseLog(
                    match {
                        it.workoutId == 1L &&
                            it.exerciseVariationId == 1L &&
                            it.exerciseOrder == 2 &&
                            it.notes == "Test notes"
                    },
                )
            }
        }

    @Test
    fun `updateExerciseLog_updatesSuccessfully`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.update(mockExerciseLog) } just runs

            // Act
            repository.updateExerciseLog(mockExerciseLog)

            // Assert
            coVerify(exactly = 1) { exerciseLogDao.update(mockExerciseLog) }
        }

    @Test
    fun `deleteExerciseLog_deletesSuccessfully`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.deleteExerciseLog(1L) } just runs

            // Act
            repository.deleteExerciseLog(1L)

            // Assert
            coVerify(exactly = 1) { exerciseLogDao.deleteExerciseLog(1L) }
        }

    @Test
    fun `deleteSetsForExercise_deletesAllSets`() =
        runTest {
            // Arrange
            coEvery { setLogDao.deleteAllSetsForExercise(1L) } just runs

            // Act
            repository.deleteSetsForExercise(1L)

            // Assert
            coVerify(exactly = 1) { setLogDao.deleteAllSetsForExercise(1L) }
        }

    @Test
    fun `updateExerciseOrder_singleExercise_updatesOrder`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.updateExerciseOrder(1L, 3) } just runs

            // Act
            repository.updateExerciseOrder(1L, 3)

            // Assert
            coVerify(exactly = 1) { exerciseLogDao.updateExerciseOrder(1L, 3) }
        }

    @Test
    fun `updateExerciseOrder_multipleExercises_updatesAllOrders`() =
        runTest {
            // Arrange
            val orders = mapOf(1L to 2, 2L to 1, 3L to 3)
            coEvery { exerciseLogDao.updateExerciseOrder(any(), any()) } just runs

            // Act
            repository.updateExerciseOrder(orders)

            // Assert
            coVerify { exerciseLogDao.updateExerciseOrder(1L, 2) }
            coVerify { exerciseLogDao.updateExerciseOrder(2L, 1) }
            coVerify { exerciseLogDao.updateExerciseOrder(3L, 3) }
        }

    @Test
    fun `getExerciseDetailsForLog_returnsVariation`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.getExerciseVariationById(1L) } returns mockVariation

            // Act
            val result = repository.getExerciseDetailsForLog(mockExerciseLog)

            // Assert
            assertThat(result).isEqualTo(mockVariation)
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_incrementsUsageForAuthenticatedUser`() =
        runTest {
            // Arrange
            val userId = "user123"
            repository =
                ExerciseRepository(
                    db,
                    authManager =
                        mockk {
                            every { getCurrentUserId() } returns userId
                        },
                )

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 1L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                    notes = null,
                    isCustomExercise = false,
                )

            // Assert
            assertThat(result).isEqualTo(1L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = userId,
                    variationId = mockVariation.id,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_incrementsUsageForUnauthenticatedUser`() =
        runTest {
            // Arrange
            repository =
                ExerciseRepository(
                    db,
                    authManager =
                        mockk {
                            every { getCurrentUserId() } returns null
                        },
                )

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 1L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 1L,
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                    notes = null,
                    isCustomExercise = false,
                )

            // Assert
            assertThat(result).isEqualTo(1L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = mockVariation.id,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byId_incrementsUsageForAuthenticatedUser`() =
        runTest {
            // Arrange
            val userId = "user456"
            repository =
                ExerciseRepository(
                    db,
                    authManager =
                        mockk {
                            every { getCurrentUserId() } returns userId
                        },
                )

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 2L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 2L,
                    exerciseVariationId = 5L,
                    order = 2,
                    isCustomExercise = true,
                )

            // Assert
            assertThat(result).isEqualTo(2L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = userId,
                    variationId = 5L,
                    isCustom = true,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byId_incrementsUsageForUnauthenticatedUser`() =
        runTest {
            // Arrange
            val mockAuthManager = mockk<AuthenticationManager>(relaxed = true)
            every { mockAuthManager.getCurrentUserId() } returns "local"
            repository = ExerciseRepository(db, mockAuthManager)

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 3L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 3L,
                    exerciseVariationId = 10L,
                    order = 3,
                    isCustomExercise = false,
                )

            // Assert
            assertThat(result).isEqualTo(3L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = 10L,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }

    @Test
    fun `insertExerciseLog_handlesUsageTrackingException`() =
        runTest {
            // Arrange
            val mockAuthManager = mockk<AuthenticationManager>(relaxed = true)
            every { mockAuthManager.getCurrentUserId() } returns "local"
            repository = ExerciseRepository(db, mockAuthManager)

            coEvery { exerciseLogDao.insertExerciseLog(any()) } returns 4L
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any(), any()) } returns mockk(relaxed = true)
            coEvery {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any(), any())
            } throws Exception("Database error")

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = 4L,
                    exerciseVariationId = 15L,
                    order = 4,
                    isCustomExercise = false,
                )

            // Assert - should still return the ID even if usage tracking fails
            assertThat(result).isEqualTo(4L)
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = 15L,
                    isCustom = false,
                    timestamp = any(),
                )
            }
        }
}
