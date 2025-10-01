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
        userExerciseUsageDao = mockk<UserExerciseUsageDao>(relaxed = true)

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
                id = "1",
                name = "Bench Press",
                category = ExerciseCategory.CHEST,
                movementPattern = MovementPattern.PUSH,
                isCompound = true,
            )

        mockVariation =
            ExerciseVariation(
                id = "1",
                coreExerciseId = "1",
                name = "Barbell Bench Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            )

        mockExerciseLog =
            ExerciseLog(
                id = "1",
                userId = null,
                workoutId = "1",
                exerciseVariationId = "1",
                exerciseOrder = 1,
                notes = "Felt strong",
            )

        mockSetLog =
            SetLog(
                id = "1",
                userId = null,
                exerciseLogId = "1",
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
            val exercises = listOf(mockVariation, mockVariation.copy(id = "2"))
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
                    VariationAlias(id = "1", variationId = "1", alias = "BP"),
                    VariationAlias(id = "2", variationId = "1", alias = "Bench"),
                )
            coEvery { exerciseDao.getAllExercises() } returns variations
            coEvery { exerciseDao.getAliasesForVariation("1") } returns aliases

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
                    mockVariation.copy(id = "2", name = "Dumbbell Press"),
                )
            val aliases1 = listOf(VariationAlias(id = "1", variationId = "1", alias = "BP"))
            val aliases2 = listOf(VariationAlias(id = "2", variationId = "2", alias = "DB Press"))

            coEvery { exerciseDao.getAllExercises() } returns variations
            coEvery { exerciseDao.getAliasesForVariation("1") } returns aliases1
            coEvery { exerciseDao.getAliasesForVariation("2") } returns aliases2

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
                    VariationAlias(id = "1", variationId = "1", alias = "BP"),
                    VariationAlias(id = "2", variationId = "2", alias = "DL"),
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
            val ex1 = mockVariation.copy(id = "1", name = "A Exercise")
            val ex2 = mockVariation.copy(id = "2", name = "B Exercise")
            val ex3 = mockVariation.copy(id = "3", name = "C Exercise")

            coEvery { exerciseDao.getAllExercises() } returns listOf(ex1, ex2, ex3)
            // Since authManager returns "test-user", it will use userExerciseUsageDao
            coEvery { userExerciseUsageDao.getUsage("test-user", "1") } returns
                mockk {
                    every { usageCount } returns 5
                }
            coEvery { userExerciseUsageDao.getUsage("test-user", "2") } returns
                mockk {
                    every { usageCount } returns 10
                }
            coEvery { userExerciseUsageDao.getUsage("test-user", "3") } returns
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
            coEvery { exerciseDao.getExerciseVariationById("1") } returns mockVariation

            // Act
            val result = repository.getExerciseById("1")

            // Assert
            assertThat(result).isEqualTo(mockVariation)
        }

    @Test
    fun `getExerciseById_nonExistentId_returnsNull`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.getExerciseVariationById("999") } returns null

            // Act
            val result = repository.getExerciseById("999")

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
            val results = listOf(mockVariation, mockVariation.copy(id = "2"))
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
            val logs = listOf(mockExerciseLog, mockExerciseLog.copy(id = "2"))
            coEvery { exerciseLogDao.getExerciseLogsForWorkout("1") } returns logs

            // Act
            val result = repository.getExercisesForWorkout("1")

            // Assert
            assertThat(result).isEqualTo(logs)
        }

    @Test
    fun `getSetsForExercise_returnsSetLogs`() =
        runTest {
            // Arrange
            val sets = listOf(mockSetLog, mockSetLog.copy(id = "2", setOrder = 2))
            coEvery { setLogDao.getSetLogsForExercise("1") } returns sets

            // Act
            val result = repository.getSetsForExercise("1")

            // Assert
            assertThat(result).isEqualTo(sets)
        }

    @Test
    fun `insertExerciseLog_insertsAndReturnsId`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            // Act
            val result = repository.insertExerciseLog(mockExerciseLog)

            // Assert
            assertThat(result).isNotEmpty() // Auto-generated ID
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byId_insertsAndReturnsId`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            // Act
            val result = repository.insertExerciseLogWithExerciseReference("1", "2", 3)

            // Assert - returns the generated ID from the ExerciseLog
            assertThat(result).isNotNull()
            coVerify {
                exerciseLogDao.insertExerciseLog(
                    match {
                        it.workoutId == "1" &&
                            it.exerciseVariationId == "2" &&
                            it.exerciseOrder == 3
                    },
                )
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference_byVariation_insertsAndIncrementsUsage`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 2,
                    notes = "Test notes",
                )

            // Assert - returns the generated ID from the ExerciseLog
            assertThat(result).isNotNull()
            // Usage count increment is handled in FeatherweightRepository, not here
            coVerify {
                exerciseLogDao.insertExerciseLog(
                    match {
                        it.workoutId == "1" &&
                            it.exerciseVariationId == "1" &&
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
            coEvery { exerciseLogDao.deleteExerciseLog("1") } just runs

            // Act
            repository.deleteExerciseLog("1")

            // Assert
            coVerify(exactly = 1) { exerciseLogDao.deleteExerciseLog("1") }
        }

    @Test
    fun `deleteSetsForExercise_deletesAllSets`() =
        runTest {
            // Arrange
            coEvery { setLogDao.deleteAllSetsForExercise("1") } just runs

            // Act
            repository.deleteSetsForExercise("1")

            // Assert
            coVerify(exactly = 1) { setLogDao.deleteAllSetsForExercise("1") }
        }

    @Test
    fun `updateExerciseOrder_singleExercise_updatesOrder`() =
        runTest {
            // Arrange
            coEvery { exerciseLogDao.updateExerciseOrder("1", 3) } just runs

            // Act
            repository.updateExerciseOrder("1", 3)

            // Assert
            coVerify(exactly = 1) { exerciseLogDao.updateExerciseOrder("1", 3) }
        }

    @Test
    fun `updateExerciseOrder_multipleExercises_updatesAllOrders`() =
        runTest {
            // Arrange
            val orders = mapOf("1" to 2, "2" to 1, "3" to 3)
            coEvery { exerciseLogDao.updateExerciseOrder(any(), any()) } just runs

            // Act
            repository.updateExerciseOrder(orders)

            // Assert
            coVerify { exerciseLogDao.updateExerciseOrder("1", 2) }
            coVerify { exerciseLogDao.updateExerciseOrder("2", 1) }
            coVerify { exerciseLogDao.updateExerciseOrder("3", 3) }
        }

    @Test
    fun `getExerciseDetailsForLog_returnsVariation`() =
        runTest {
            // Arrange
            coEvery { exerciseDao.getExerciseVariationById("1") } returns mockVariation

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

            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                    notes = null,
                )

            // Assert
            assertThat(result).isNotEmpty() // Auto-generated ID
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = userId,
                    variationId = mockVariation.id,
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

            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                    notes = null,
                )

            // Assert
            assertThat(result).isNotEmpty() // Auto-generated ID
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = mockVariation.id,
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

            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "2",
                    exerciseVariationId = "5",
                    order = 2,
                )

            // Assert - returns the generated ID
            assertThat(result).isNotNull()
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = userId,
                    variationId = "5",
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

            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any()) } returns mockk(relaxed = true)
            coEvery { userExerciseUsageDao.incrementUsageCount(any(), any(), any()) } just runs

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "3",
                    exerciseVariationId = "10",
                    order = 3,
                )

            // Assert - returns the generated ID
            assertThat(result).isNotNull()
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = "10",
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

            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs
            coEvery { userExerciseUsageDao.getOrCreateUsage(any(), any()) } returns mockk(relaxed = true)
            coEvery {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any())
            } throws android.database.sqlite.SQLiteException("Database error")

            // Act
            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "4",
                    exerciseVariationId = "15",
                    order = 4,
                )

            // Assert - should still return the ID even if usage tracking fails
            assertThat(result).isNotNull()
            coVerify {
                userExerciseUsageDao.incrementUsageCount(
                    userId = "local",
                    variationId = "15",
                    timestamp = any(),
                )
            }
        }
}
