package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAliasDao
import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseMuscleDao
import com.github.radupana.featherweight.data.exercise.UserExerciseUsageDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserExerciseUsageRepositoryTest {
    private lateinit var repository: ExerciseRepository
    private lateinit var db: FeatherweightDatabase
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var userExerciseUsageDao: UserExerciseUsageDao
    private lateinit var authManager: AuthenticationManager
    private lateinit var mockVariation: Exercise

    private val testUserId = "test-user-123"

    @Before
    fun setUp() {
        db = mockk()
        exerciseLogDao = mockk()
        userExerciseUsageDao = mockk()
        authManager = mockk()

        val exerciseDao = mockk<ExerciseDao>()
        val setLogDao = mockk<SetLogDao>()
        val exerciseSwapHistoryDao = mockk<ExerciseSwapHistoryDao>()
        val exerciseAliasDao = mockk<ExerciseAliasDao>()
        val exerciseMuscleDao = mockk<ExerciseMuscleDao>()

        every { db.exerciseDao() } returns exerciseDao
        every { db.exerciseLogDao() } returns exerciseLogDao
        every { db.setLogDao() } returns setLogDao
        every { db.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { db.exerciseAliasDao() } returns exerciseAliasDao
        every { db.exerciseMuscleDao() } returns exerciseMuscleDao
        every { db.userExerciseUsageDao() } returns userExerciseUsageDao

        every { authManager.getCurrentUserId() } returns testUserId

        repository = ExerciseRepository(db, authManager)

        mockVariation =
            Exercise(
                id = "1",
                name = "Barbell Bench Press",
                category = com.github.radupana.featherweight.data.exercise.ExerciseCategory.CHEST.name,
                movementPattern = com.github.radupana.featherweight.data.exercise.MovementPattern.PUSH.name,
                isCompound = true,
                equipment = Equipment.BARBELL.name,
                difficulty = ExerciseDifficulty.INTERMEDIATE.name,
                requiresWeight = true,
            )
    }

    @Test
    fun `insertExerciseLogWithExerciseReference does not increment usage count`() =
        runTest {
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            assertThat(result).isNotEmpty()
            coVerify(exactly = 0) {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any())
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference by ID does not increment usage count`() =
        runTest {
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseId = "1",
                    order = 1,
                )

            assertThat(result).isNotEmpty()
            coVerify(exactly = 0) {
                userExerciseUsageDao.incrementUsageCount(any(), any(), any())
            }
        }

    @Test
    fun `insertExerciseLogWithExerciseReference returns valid ID for authenticated user`() =
        runTest {
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            val result =
                repository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            assertThat(result).isNotEmpty()
        }

    @Test
    fun `insertExerciseLogWithExerciseReference returns valid ID for unauthenticated user`() =
        runTest {
            val mockAuthManager = mockk<AuthenticationManager>(relaxed = true)
            every { mockAuthManager.getCurrentUserId() } returns null
            val unauthRepository = ExerciseRepository(db, mockAuthManager)
            coEvery { exerciseLogDao.insertExerciseLog(any()) } just runs

            val result =
                unauthRepository.insertExerciseLogWithExerciseReference(
                    workoutId = "1",
                    exerciseVariation = mockVariation,
                    exerciseOrder = 1,
                )

            assertThat(result).isNotEmpty()
        }
}
