package com.github.radupana.featherweight.sync.strategies

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseInstructionDao
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.sync.models.FirestoreCustomExercise
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CustomExerciseSyncStrategyTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var exerciseAliasDao: ExerciseAliasDao
    private lateinit var exerciseInstructionDao: ExerciseInstructionDao
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var strategy: CustomExerciseSyncStrategy

    private val userId = "test-user-123"
    private val exerciseId = "test-exercise-id"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        database = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        exerciseMuscleDao = mockk(relaxed = true)
        exerciseAliasDao = mockk(relaxed = true)
        exerciseInstructionDao = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)

        every { database.exerciseDao() } returns exerciseDao
        every { database.exerciseMuscleDao() } returns exerciseMuscleDao
        every { database.exerciseAliasDao() } returns exerciseAliasDao
        every { database.exerciseInstructionDao() } returns exerciseInstructionDao

        strategy = CustomExerciseSyncStrategy(database, firestoreRepository)
    }

    @Test
    fun `downloadAndMerge with null userId returns success without syncing`() =
        runTest {
            val result = strategy.downloadAndMerge(userId = null, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 0) { firestoreRepository.downloadCustomExercises(any(), any()) }
        }

    @Test
    fun `downloadAndMerge inserts new remote exercises`() =
        runTest {
            val remoteExercise =
                FirestoreCustomExercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Remote Exercise",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                )

            coEvery { firestoreRepository.downloadCustomExercises(userId, null) } returns
                Result.success(listOf(remoteExercise))
            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns emptyList()

            val result = strategy.downloadAndMerge(userId, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify { exerciseDao.insertExercise(match { it.id == exerciseId && it.name == "Remote Exercise" }) }
        }

    @Test
    fun `downloadAndMerge updates local exercises when remote is newer`() =
        runTest {
            val now = LocalDateTime.now()
            val remoteTimestamp = Timestamp(now.toEpochSecond(ZoneOffset.UTC), 0)

            val localExercise =
                Exercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Old Name",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    updatedAt = now.minusDays(1),
                )

            val remoteExercise =
                FirestoreCustomExercise(
                    id = exerciseId,
                    userId = userId,
                    name = "New Name",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    lastModified = remoteTimestamp,
                )

            coEvery { firestoreRepository.downloadCustomExercises(userId, null) } returns
                Result.success(listOf(remoteExercise))
            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns listOf(localExercise)
            coEvery { exerciseDao.getExerciseById(exerciseId) } returns localExercise

            val result = strategy.downloadAndMerge(userId, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify { exerciseDao.updateExercise(match { it.name == "New Name" }) }
        }

    @Test
    fun `downloadAndMerge does not update when local is newer`() =
        runTest {
            val now = LocalDateTime.now()
            val remoteTimestamp = Timestamp(now.minusDays(1).toEpochSecond(ZoneOffset.UTC), 0)

            val localExercise =
                Exercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Newer Local Name",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    updatedAt = now,
                )

            val remoteExercise =
                FirestoreCustomExercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Older Remote Name",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    lastModified = remoteTimestamp,
                )

            coEvery { firestoreRepository.downloadCustomExercises(userId, null) } returns
                Result.success(listOf(remoteExercise))
            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns listOf(localExercise)

            val result = strategy.downloadAndMerge(userId, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 0) { exerciseDao.updateExercise(any()) }
        }

    @Test
    fun `downloadAndMerge uploads local changes that are newer than lastSyncTime`() =
        runTest {
            val now = LocalDateTime.now()
            val lastSyncTime = Timestamp(now.minusDays(2).toEpochSecond(ZoneOffset.UTC), 0)

            val localExercise =
                Exercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Local Exercise",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    updatedAt = now.minusDays(1), // Newer than lastSyncTime
                )

            val muscles = listOf(ExerciseMuscle("muscle-1", exerciseId, "CHEST", "primary", false))
            val aliases = listOf(ExerciseAlias("alias-1", exerciseId, "Bench Press"))
            val instructions = listOf(ExerciseInstruction("inst-1", exerciseId, "SETUP", 0, "Lie on bench", false))

            coEvery { firestoreRepository.downloadCustomExercises(userId, lastSyncTime) } returns
                Result.success(emptyList())
            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns listOf(localExercise)
            coEvery { exerciseMuscleDao.getMusclesForVariation(exerciseId) } returns muscles
            coEvery { exerciseAliasDao.getAliasesForExercise(exerciseId) } returns aliases
            coEvery { exerciseInstructionDao.getInstructionsForVariation(exerciseId) } returns instructions
            coEvery {
                firestoreRepository.uploadCustomExercise(
                    userId,
                    localExercise,
                    muscles,
                    aliases,
                    instructions,
                )
            } returns Result.success(Unit)

            val result = strategy.downloadAndMerge(userId, lastSyncTime)

            assertThat(result.isSuccess).isTrue()
            coVerify {
                firestoreRepository.uploadCustomExercise(
                    userId,
                    localExercise,
                    muscles,
                    aliases,
                    instructions,
                )
            }
        }

    @Test
    fun `downloadAndMerge does not upload local changes that are older than lastSyncTime`() =
        runTest {
            val now = LocalDateTime.now()
            val lastSyncTime = Timestamp(now.minusDays(1).toEpochSecond(ZoneOffset.UTC), 0)

            val localExercise =
                Exercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Local Exercise",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                    updatedAt = now.minusDays(2), // Older than lastSyncTime
                )

            coEvery { firestoreRepository.downloadCustomExercises(userId, lastSyncTime) } returns
                Result.success(emptyList())
            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns listOf(localExercise)

            val result = strategy.downloadAndMerge(userId, lastSyncTime)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 0) {
                firestoreRepository.uploadCustomExercise(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `uploadChanges uploads all local exercises when lastSyncTime is null`() =
        runTest {
            val localExercise =
                Exercise(
                    id = exerciseId,
                    userId = userId,
                    name = "Local Exercise",
                    category = "CHEST",
                    movementPattern = "PUSH",
                    isCompound = true,
                    equipment = "BARBELL",
                    difficulty = "INTERMEDIATE",
                    requiresWeight = true,
                    rmScalingType = "STANDARD",
                    restDurationSeconds = 120,
                )

            val muscles = listOf(ExerciseMuscle("muscle-1", exerciseId, "CHEST", "primary", false))
            val aliases = listOf(ExerciseAlias("alias-1", exerciseId, "Bench Press"))
            val instructions = listOf(ExerciseInstruction("inst-1", exerciseId, "SETUP", 0, "Lie on bench", false))

            coEvery { exerciseDao.getCustomExercisesByUser(userId) } returns listOf(localExercise)
            coEvery { exerciseMuscleDao.getMusclesForVariation(exerciseId) } returns muscles
            coEvery { exerciseAliasDao.getAliasesForExercise(exerciseId) } returns aliases
            coEvery { exerciseInstructionDao.getInstructionsForVariation(exerciseId) } returns instructions
            coEvery {
                firestoreRepository.uploadCustomExercise(
                    userId,
                    localExercise,
                    muscles,
                    aliases,
                    instructions,
                )
            } returns Result.success(Unit)

            val result = strategy.uploadChanges(userId, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify {
                firestoreRepository.uploadCustomExercise(
                    userId,
                    localExercise,
                    muscles,
                    aliases,
                    instructions,
                )
            }
        }

    @Test
    fun `uploadChanges with null userId returns success without uploading`() =
        runTest {
            val result = strategy.uploadChanges(userId = null, lastSyncTime = null)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 0) { firestoreRepository.uploadCustomExercise(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `getDataType returns CustomExercises`() {
        assertThat(strategy.getDataType()).isEqualTo("CustomExercises")
    }
}
