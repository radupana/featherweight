package com.github.radupana.featherweight.sync.strategies

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseCoreDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.VariationAliasDao
import com.github.radupana.featherweight.data.exercise.VariationInstructionDao
import com.github.radupana.featherweight.data.exercise.VariationMuscleDao
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class SystemExerciseSyncStrategyTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var strategy: SystemExerciseSyncStrategy

    // DAOs
    private lateinit var exerciseCoreDao: ExerciseCoreDao
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var variationMuscleDao: VariationMuscleDao
    private lateinit var variationAliasDao: VariationAliasDao
    private lateinit var variationInstructionDao: VariationInstructionDao

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        // Create mocks
        database = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        // Create DAO mocks
        exerciseCoreDao = mockk(relaxed = true)
        exerciseVariationDao = mockk(relaxed = true)
        variationMuscleDao = mockk(relaxed = true)
        variationAliasDao = mockk(relaxed = true)
        variationInstructionDao = mockk(relaxed = true)

        // Setup database to return DAOs
        every { database.exerciseCoreDao() } returns exerciseCoreDao
        every { database.exerciseVariationDao() } returns exerciseVariationDao
        every { database.variationMuscleDao() } returns variationMuscleDao
        every { database.variationAliasDao() } returns variationAliasDao
        every { database.variationInstructionDao() } returns variationInstructionDao

        // Create strategy
        strategy = SystemExerciseSyncStrategy(database, firestoreRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `downloadAndMerge downloads and processes exercises correctly`() =
        runBlocking {
            // Given: Remote exercises from Firestore
            val remoteExercises =
                mapOf(
                    "bench-press" to
                        FirestoreExercise(
                            coreName = "Bench Press",
                            coreCategory = "CHEST",
                            coreMovementPattern = "PRESS",
                            coreIsCompound = true,
                            name = "Barbell Bench Press",
                            equipment = "BARBELL",
                            difficulty = "INTERMEDIATE",
                            requiresWeight = true,
                            muscles =
                                listOf(
                                    FirestoreMuscle("CHEST", true, 1.0),
                                    FirestoreMuscle("TRICEPS", false, 0.5),
                                ),
                        ),
                    "squat" to
                        FirestoreExercise(
                            coreName = "Squat",
                            coreCategory = "LEGS",
                            coreMovementPattern = "SQUAT",
                            coreIsCompound = true,
                            name = "Barbell Back Squat",
                            equipment = "BARBELL",
                            difficulty = "INTERMEDIATE",
                            requiresWeight = true,
                            muscles =
                                listOf(
                                    FirestoreMuscle("QUADS", true, 1.0),
                                    FirestoreMuscle("GLUTES", false, 0.75),
                                ),
                        ),
                )

            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(remoteExercises)

            // Setup DAOs to return null (new exercises)
            coEvery { exerciseCoreDao.getCoreById(any()) } returns null
            coEvery { exerciseVariationDao.getExerciseVariationById(any()) } returns null

            // Mock insertion methods
            coEvery { exerciseCoreDao.insertCore(any()) } returns 1L
            coEvery { exerciseVariationDao.insertExerciseVariation(any()) } returns 1L
            coEvery { variationMuscleDao.deleteForVariation(any()) } returns Unit
            coEvery { variationMuscleDao.insertVariationMuscles(any()) } returns Unit
            coEvery { variationAliasDao.deleteForVariation(any()) } returns Unit
            coEvery { variationAliasDao.insertAliases(any()) } returns listOf(1L)
            coEvery { variationInstructionDao.deleteForVariation(any()) } returns Unit
            coEvery { variationInstructionDao.insertInstructions(any()) } returns listOf(1L)

            // When: Sync is performed
            val result = strategy.downloadAndMerge(null, null)

            // Then: Result is successful
            assertTrue(result.isSuccess)

            // Verify exercises were downloaded
            coVerify { firestoreRepository.downloadSystemExercises(null) }

            // Verify cores were inserted
            coVerify(exactly = 2) { exerciseCoreDao.insertCore(any()) }

            // Verify variations were inserted
            coVerify(exactly = 2) { exerciseVariationDao.insertExerciseVariation(any()) }

            // Verify muscles were inserted
            coVerify(exactly = 2) { variationMuscleDao.deleteForVariation(any()) }
            coVerify(exactly = 2) { variationMuscleDao.insertVariationMuscles(any()) }
        }

    @Test
    fun `downloadAndMerge updates existing exercises when remote is newer`() =
        runBlocking {
            // Given: Existing local exercise
            val oldTimestamp = LocalDateTime.of(2024, 1, 1, 0, 0)
            val newTimestamp = LocalDateTime.of(2024, 2, 1, 0, 0)

            // Calculate the stable IDs that will be generated
            val coreId = "core_Bench Press".hashCode().toLong() and 0x7FFFFFFF
            val variationId = "var_Barbell Bench Press Updated".hashCode().toLong() and 0x7FFFFFFF

            val existingCore =
                ExerciseCore(
                    id = coreId,
                    name = "Bench Press",
                    category = ExerciseCategory.CHEST,
                    movementPattern = MovementPattern.PRESS,
                    isCompound = true,
                    createdAt = oldTimestamp,
                    updatedAt = oldTimestamp,
                )

            val existingVariation =
                ExerciseVariation(
                    id = variationId,
                    coreExerciseId = coreId,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    createdAt = oldTimestamp,
                    updatedAt = oldTimestamp,
                )

            // Remote exercise with newer timestamp
            val remoteExercise =
                FirestoreExercise(
                    coreName = "Bench Press",
                    coreCategory = "CHEST",
                    coreMovementPattern = "PRESS",
                    coreIsCompound = true,
                    name = "Barbell Bench Press Updated",
                    equipment = "BARBELL",
                    difficulty = "ADVANCED",
                    requiresWeight = true,
                    updatedAt = "2024-02-01T00:00:00Z",
                )

            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns
                Result.success(mapOf("bench-press" to remoteExercise))

            coEvery { exerciseCoreDao.getCoreById(any()) } returns existingCore
            coEvery { exerciseVariationDao.getExerciseVariationById(any()) } returns existingVariation

            // Mock update and delete methods
            coEvery { exerciseCoreDao.updateCore(any()) } returns Unit
            coEvery { exerciseVariationDao.updateVariation(any()) } returns Unit
            coEvery { variationMuscleDao.deleteForVariation(any()) } returns Unit
            coEvery { variationMuscleDao.insertVariationMuscles(any()) } returns Unit
            coEvery { variationAliasDao.deleteForVariation(any()) } returns Unit
            coEvery { variationAliasDao.insertAliases(any()) } returns listOf(1L)
            coEvery { variationInstructionDao.deleteForVariation(any()) } returns Unit
            coEvery { variationInstructionDao.insertInstructions(any()) } returns listOf(1L)

            // When: Sync is performed
            val result = strategy.downloadAndMerge(null, null)

            // Then: Result is successful
            assertTrue(result.isSuccess)

            // Verify updates were called
            coVerify { exerciseCoreDao.updateCore(any()) }
            coVerify { exerciseVariationDao.updateVariation(any()) }

            // Verify related data was refreshed
            coVerify { variationMuscleDao.deleteForVariation(any()) }
        }

    @Test
    fun `downloadAndMerge skips exercises when local is newer`() =
        runBlocking {
            // Given: Existing local exercise that's newer
            val newTimestamp = LocalDateTime.of(2024, 2, 1, 0, 0)
            val oldTimestamp = LocalDateTime.of(2024, 1, 1, 0, 0)

            // Calculate the stable IDs that will be generated
            val coreId = "core_Bench Press".hashCode().toLong() and 0x7FFFFFFF
            val variationId = "var_Barbell Bench Press".hashCode().toLong() and 0x7FFFFFFF

            val existingCore =
                ExerciseCore(
                    id = coreId,
                    name = "Bench Press",
                    category = ExerciseCategory.CHEST,
                    movementPattern = MovementPattern.PRESS,
                    isCompound = true,
                    createdAt = newTimestamp,
                    updatedAt = newTimestamp,
                )

            val existingVariation =
                ExerciseVariation(
                    id = variationId,
                    coreExerciseId = coreId,
                    name = "Barbell Bench Press",
                    equipment = Equipment.BARBELL,
                    difficulty = ExerciseDifficulty.INTERMEDIATE,
                    requiresWeight = true,
                    createdAt = newTimestamp,
                    updatedAt = newTimestamp,
                )

            // Remote exercise with older timestamp
            val remoteExercise =
                FirestoreExercise(
                    coreName = "Bench Press",
                    coreCategory = "CHEST",
                    coreMovementPattern = "PRESS",
                    coreIsCompound = true,
                    name = "Barbell Bench Press",
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    updatedAt = "2024-01-01T00:00:00Z",
                )

            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns
                Result.success(mapOf("bench-press" to remoteExercise))

            coEvery { exerciseCoreDao.getCoreById(any()) } returns existingCore
            coEvery { exerciseVariationDao.getExerciseVariationById(any()) } returns existingVariation

            // Mock deletion methods (these should still be called for related data)
            coEvery { variationMuscleDao.deleteForVariation(any()) } returns Unit
            coEvery { variationMuscleDao.insertVariationMuscles(any()) } returns Unit
            coEvery { variationAliasDao.deleteForVariation(any()) } returns Unit
            coEvery { variationAliasDao.insertAliases(any()) } returns listOf(1L)
            coEvery { variationInstructionDao.deleteForVariation(any()) } returns Unit
            coEvery { variationInstructionDao.insertInstructions(any()) } returns listOf(1L)

            // When: Sync is performed
            val result = strategy.downloadAndMerge(null, null)

            // Then: Result is successful
            assertTrue(result.isSuccess)

            // Verify NO updates were called
            coVerify(exactly = 0) { exerciseCoreDao.updateCore(any()) }
            coVerify(exactly = 0) { exerciseVariationDao.updateVariation(any()) }

            // But related data is still refreshed
            coVerify { variationMuscleDao.deleteForVariation(any()) }
        }

    @Test
    fun `downloadAndMerge handles empty exercise list`() =
        runBlocking {
            // Given: No remote exercises
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns Result.success(emptyMap())

            // When: Sync is performed
            val result = strategy.downloadAndMerge(null, null)

            // Then: Result is successful
            assertTrue(result.isSuccess)

            // Verify no insertions
            coVerify(exactly = 0) { exerciseCoreDao.insertCore(any()) }
            coVerify(exactly = 0) { exerciseVariationDao.insertExerciseVariation(any()) }
        }

    @Test
    fun `downloadAndMerge handles Firestore error`() =
        runBlocking {
            // Given: Firestore returns error
            coEvery { firestoreRepository.downloadSystemExercises(any()) } returns
                Result.failure(Exception("Network error"))

            // When: Sync is performed
            val result = strategy.downloadAndMerge(null, null)

            // Then: Result is failure
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)

            // Verify no database operations
            coVerify(exactly = 0) { exerciseCoreDao.insertCore(any()) }
        }

    @Test
    fun `uploadChanges does nothing for system exercises`() =
        runBlocking {
            // When: Upload is attempted
            val result = strategy.uploadChanges("userId", null)

            // Then: Result is successful
            assertTrue(result.isSuccess)

            // Verify no upload operations
            coVerify(exactly = 0) { firestoreRepository.uploadWorkouts(any(), any()) }
        }

    @Test
    fun `getDataType returns correct type`() {
        assertEquals("SystemExercises", strategy.getDataType())
    }
}
