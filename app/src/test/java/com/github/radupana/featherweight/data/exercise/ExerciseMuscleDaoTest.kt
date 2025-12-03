package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.data.BaseDaoTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Test suite for ExerciseMuscleDao.
 *
 * Tests all DAO methods including:
 * - CRUD operations
 * - JOIN queries for finding exercises by muscle group
 * - Query operations with filtering
 * - Soft delete operations
 * - Foreign key constraints
 * - Batch operations
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseMuscleDaoTest : BaseDaoTest() {
    // Helper Methods

    /**
     * Creates and inserts a test exercise.
     * Required for foreign key constraints on ExerciseMuscle.
     */
    private suspend fun createExercise(
        id: String = "test-exercise",
        name: String = "Test Exercise",
        type: String = ExerciseType.SYSTEM.name,
    ): Exercise {
        val exercise =
            Exercise(
                id = id,
                type = type,
                userId = if (type == ExerciseType.USER.name) "test-user" else null,
                name = name,
                category = "STRENGTH",
                movementPattern = "PUSH",
                equipment = "BARBELL",
                isCompound = true,
                requiresWeight = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    /**
     * Creates a test ExerciseMuscle.
     */
    private fun createExerciseMuscle(
        exerciseId: String,
        muscle: String,
        targetType: String = "PRIMARY",
        isDeleted: Boolean = false,
    ): ExerciseMuscle =
        ExerciseMuscle(
            exerciseId = exerciseId,
            muscle = muscle,
            targetType = targetType,
            isDeleted = isDeleted,
        )

    // CRUD Operations Tests

    @Test
    fun `insertExerciseMuscle should add muscle to database`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST")

            exerciseMuscleDao.insertExerciseMuscle(muscle)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].muscle).isEqualTo("CHEST")
            assertThat(retrieved[0].targetType).isEqualTo("PRIMARY")
        }

    @Test
    fun `insertExerciseMuscle with REPLACE should update existing muscle`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST", "PRIMARY")

            exerciseMuscleDao.insertExerciseMuscle(muscle)

            val updated = muscle.copy(targetType = "SECONDARY")
            exerciseMuscleDao.insertExerciseMuscle(updated)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].targetType).isEqualTo("SECONDARY")
        }

    @Test
    fun `insertExerciseMuscles should add multiple muscles to database`() =
        runTest {
            val exercise = createExercise()

            val muscles =
                listOf(
                    createExerciseMuscle(exercise.id, "CHEST", "PRIMARY"),
                    createExerciseMuscle(exercise.id, "TRICEPS", "SECONDARY"),
                    createExerciseMuscle(exercise.id, "SHOULDERS", "SECONDARY"),
                )

            exerciseMuscleDao.insertExerciseMuscles(muscles)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(3)
            assertThat(retrieved.map { it.muscle }).containsExactly("CHEST", "TRICEPS", "SHOULDERS")
        }

    @Test
    fun `upsertExerciseMuscle should insert new muscle`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST")

            exerciseMuscleDao.upsertExerciseMuscle(muscle)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].muscle).isEqualTo("CHEST")
        }

    @Test
    fun `upsertExerciseMuscle should update existing muscle`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST", "PRIMARY")

            exerciseMuscleDao.insertExerciseMuscle(muscle)

            val updated = muscle.copy(targetType = "SECONDARY")
            exerciseMuscleDao.upsertExerciseMuscle(updated)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].targetType).isEqualTo("SECONDARY")
        }

    // Query Operations Tests

    @Test
    fun `getMusclesForVariation should return all muscles for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Squat")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise1.id, "TRICEPS"),
                    createExerciseMuscle(exercise2.id, "QUADS"),
                ),
            )

            val exercise1Muscles = exerciseMuscleDao.getMusclesForVariation(exercise1.id)
            val exercise2Muscles = exerciseMuscleDao.getMusclesForVariation(exercise2.id)

            assertThat(exercise1Muscles).hasSize(2)
            assertThat(exercise2Muscles).hasSize(1)
            assertThat(exercise1Muscles.map { it.muscle }).containsExactly("CHEST", "TRICEPS")
            assertThat(exercise2Muscles[0].muscle).isEqualTo("QUADS")
        }

    @Test
    fun `getMusclesForVariation should return empty list for exercise with no muscles`() =
        runTest {
            val exercise = createExercise()

            val muscles = exerciseMuscleDao.getMusclesForVariation(exercise.id)

            assertThat(muscles).isEmpty()
        }

    @Test
    fun `getAllExerciseMuscles should return all muscles in database`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise1.id, "TRICEPS"),
                    createExerciseMuscle(exercise2.id, "QUADS"),
                ),
            )

            val allMuscles = exerciseMuscleDao.getAllExerciseMuscles()

            assertThat(allMuscles).hasSize(3)
            assertThat(allMuscles.map { it.muscle }).containsExactly("CHEST", "TRICEPS", "QUADS")
        }

    @Test
    fun `getExercisesByMuscleGroup should return exercises targeting specific muscle`() =
        runTest {
            val benchPress = createExercise(id = "bench-press", name = "Barbell Bench Press")
            val inclineBench = createExercise(id = "incline-bench", name = "Incline Bench Press")
            val squat = createExercise(id = "squat", name = "Barbell Squat")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(benchPress.id, "CHEST"),
                    createExerciseMuscle(benchPress.id, "TRICEPS"),
                    createExerciseMuscle(inclineBench.id, "CHEST"),
                    createExerciseMuscle(squat.id, "QUADS"),
                ),
            )

            val chestExercises = exerciseMuscleDao.getExercisesByMuscleGroup("CHEST")
            val quadExercises = exerciseMuscleDao.getExercisesByMuscleGroup("QUADS")

            assertThat(chestExercises).hasSize(2)
            assertThat(quadExercises).hasSize(1)
            assertThat(chestExercises.map { it.name })
                .containsExactly("Barbell Bench Press", "Incline Bench Press")
            assertThat(quadExercises[0].name).isEqualTo("Barbell Squat")
        }

    @Test
    fun `getExercisesByMuscleGroup should return exercises sorted by name`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1", name = "Dumbbell Bench Press")
            val exercise2 = createExercise(id = "exercise-2", name = "Barbell Bench Press")
            val exercise3 = createExercise(id = "exercise-3", name = "Cable Fly")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise2.id, "CHEST"),
                    createExerciseMuscle(exercise3.id, "CHEST"),
                ),
            )

            val exercises = exerciseMuscleDao.getExercisesByMuscleGroup("CHEST")

            assertThat(exercises).hasSize(3)
            assertThat(exercises.map { it.name })
                .containsExactly("Barbell Bench Press", "Cable Fly", "Dumbbell Bench Press")
                .inOrder()
        }

    @Test
    fun `getExercisesByMuscleGroup should return empty list for muscle with no exercises`() =
        runTest {
            val exercises = exerciseMuscleDao.getExercisesByMuscleGroup("CALVES")

            assertThat(exercises).isEmpty()
        }

    @Test
    fun `getExercisesByMuscleGroup should not return duplicate exercises`() =
        runTest {
            val exercise = createExercise(name = "Bench Press")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise.id, "CHEST", "PRIMARY"),
                    createExerciseMuscle(exercise.id, "CHEST", "SECONDARY"),
                ),
            )

            val exercises = exerciseMuscleDao.getExercisesByMuscleGroup("CHEST")

            assertThat(exercises).hasSize(1)
        }

    @Test
    fun `getExercisesByMuscleGroup should perform JOIN query correctly`() =
        runTest {
            val exercise = createExercise(name = "Barbell Bench Press")
            exerciseMuscleDao.insertExerciseMuscle(createExerciseMuscle(exercise.id, "CHEST"))

            val exercises = exerciseMuscleDao.getExercisesByMuscleGroup("CHEST")

            assertThat(exercises).hasSize(1)
            val retrieved = exercises[0]
            assertThat(retrieved.id).isEqualTo(exercise.id)
            assertThat(retrieved.name).isEqualTo("Barbell Bench Press")
            assertThat(retrieved.equipment).isEqualTo("BARBELL")
            assertThat(retrieved.category).isEqualTo("STRENGTH")
        }

    // Deletion Operations Tests

    @Test
    fun `deleteMuscleMappingsForExercise should remove all muscles for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise1.id, "TRICEPS"),
                    createExerciseMuscle(exercise2.id, "QUADS"),
                ),
            )

            exerciseMuscleDao.deleteMuscleMappingsForExercise(exercise1.id)

            val exercise1Muscles = exerciseMuscleDao.getMusclesForVariation(exercise1.id)
            val exercise2Muscles = exerciseMuscleDao.getMusclesForVariation(exercise2.id)

            assertThat(exercise1Muscles).isEmpty()
            assertThat(exercise2Muscles).hasSize(1)
        }

    @Test
    fun `deleteForVariation should remove all muscles for specific exercise`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise1.id, "TRICEPS"),
                    createExerciseMuscle(exercise2.id, "QUADS"),
                ),
            )

            exerciseMuscleDao.deleteForVariation(exercise1.id)

            val exercise1Muscles = exerciseMuscleDao.getMusclesForVariation(exercise1.id)
            val exercise2Muscles = exerciseMuscleDao.getMusclesForVariation(exercise2.id)

            assertThat(exercise1Muscles).isEmpty()
            assertThat(exercise2Muscles).hasSize(1)
        }

    @Test
    fun `deleteForExercises should remove muscles for multiple exercises`() =
        runTest {
            val exercise1 = createExercise(id = "exercise-1")
            val exercise2 = createExercise(id = "exercise-2")
            val exercise3 = createExercise(id = "exercise-3")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise1.id, "CHEST"),
                    createExerciseMuscle(exercise2.id, "TRICEPS"),
                    createExerciseMuscle(exercise3.id, "QUADS"),
                ),
            )

            exerciseMuscleDao.deleteForExercises(listOf(exercise1.id, exercise2.id))

            val exercise1Muscles = exerciseMuscleDao.getMusclesForVariation(exercise1.id)
            val exercise2Muscles = exerciseMuscleDao.getMusclesForVariation(exercise2.id)
            val exercise3Muscles = exerciseMuscleDao.getMusclesForVariation(exercise3.id)

            assertThat(exercise1Muscles).isEmpty()
            assertThat(exercise2Muscles).isEmpty()
            assertThat(exercise3Muscles).hasSize(1)
        }

    @Test
    fun `deleteById should remove specific muscle mapping`() =
        runTest {
            val exercise = createExercise()
            val muscle1 = createExerciseMuscle(exercise.id, "CHEST")
            val muscle2 = createExerciseMuscle(exercise.id, "TRICEPS")

            exerciseMuscleDao.insertExerciseMuscles(listOf(muscle1, muscle2))

            exerciseMuscleDao.deleteById(muscle1.id)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].muscle).isEqualTo("TRICEPS")
        }

    // Soft Delete Operations Tests

    @Test
    fun `softDeleteExerciseMuscle should set isDeleted flag to true`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST")

            exerciseMuscleDao.insertExerciseMuscle(muscle)

            exerciseMuscleDao.softDeleteExerciseMuscle(muscle.id)

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(1)
            assertThat(retrieved[0].isDeleted).isTrue()
        }

    @Test
    fun `soft deleted muscles should still appear in queries`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST")

            exerciseMuscleDao.insertExerciseMuscle(muscle)
            exerciseMuscleDao.softDeleteExerciseMuscle(muscle.id)

            val muscles = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(muscles).hasSize(1)
            assertThat(muscles[0].isDeleted).isTrue()
        }

    @Test
    fun `isDeleted flag should persist correctly`() =
        runTest {
            val exercise = createExercise()
            val deletedMuscle = createExerciseMuscle(exercise.id, "CHEST", isDeleted = true)
            val activeMuscle = createExerciseMuscle(exercise.id, "TRICEPS", isDeleted = false)

            exerciseMuscleDao.insertExerciseMuscles(listOf(deletedMuscle, activeMuscle))

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(2)

            val deletedRetrieved = retrieved.find { it.id == deletedMuscle.id }
            val activeRetrieved = retrieved.find { it.id == activeMuscle.id }

            assertThat(deletedRetrieved?.isDeleted).isTrue()
            assertThat(activeRetrieved?.isDeleted).isFalse()
        }

    // Foreign Key Constraint Tests

    @Test
    fun `foreign key cascade should delete muscles when exercise is deleted`() =
        runTest {
            val exercise = createExercise()
            val muscle = createExerciseMuscle(exercise.id, "CHEST")

            exerciseMuscleDao.insertExerciseMuscle(muscle)

            val beforeDelete = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(beforeDelete).hasSize(1)

            exerciseDao.deleteExercise(exercise.id)

            val afterDelete = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(afterDelete).isEmpty()
        }

    // Edge Cases and Special Scenarios

    @Test
    fun `should support different muscles with different target types`() =
        runTest {
            val exercise = createExercise()
            val primaryMuscle = createExerciseMuscle(exercise.id, "CHEST", "PRIMARY")
            val secondaryMuscle = createExerciseMuscle(exercise.id, "TRICEPS", "SECONDARY")

            exerciseMuscleDao.insertExerciseMuscles(listOf(primaryMuscle, secondaryMuscle))

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            assertThat(retrieved).hasSize(2)
            assertThat(retrieved.map { it.targetType }).containsExactly("PRIMARY", "SECONDARY")
        }

    @Test
    fun `should differentiate between primary and secondary muscles`() =
        runTest {
            val exercise = createExercise()

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise.id, "CHEST", "PRIMARY"),
                    createExerciseMuscle(exercise.id, "TRICEPS", "SECONDARY"),
                    createExerciseMuscle(exercise.id, "SHOULDERS", "SECONDARY"),
                ),
            )

            val retrieved = exerciseMuscleDao.getMusclesForVariation(exercise.id)
            val primary = retrieved.filter { it.targetType == "PRIMARY" }
            val secondary = retrieved.filter { it.targetType == "SECONDARY" }

            assertThat(primary).hasSize(1)
            assertThat(secondary).hasSize(2)
            assertThat(primary[0].muscle).isEqualTo("CHEST")
        }

    @Test
    fun `getExercisesByMuscleGroup should handle exercises with multiple muscle groups`() =
        runTest {
            val exercise = createExercise(name = "Bench Press")

            exerciseMuscleDao.insertExerciseMuscles(
                listOf(
                    createExerciseMuscle(exercise.id, "CHEST"),
                    createExerciseMuscle(exercise.id, "TRICEPS"),
                    createExerciseMuscle(exercise.id, "SHOULDERS"),
                ),
            )

            val chestExercises = exerciseMuscleDao.getExercisesByMuscleGroup("CHEST")
            val tricepsExercises = exerciseMuscleDao.getExercisesByMuscleGroup("TRICEPS")
            val shoulderExercises = exerciseMuscleDao.getExercisesByMuscleGroup("SHOULDERS")

            assertThat(chestExercises).hasSize(1)
            assertThat(tricepsExercises).hasSize(1)
            assertThat(shoulderExercises).hasSize(1)
            assertThat(chestExercises[0].id).isEqualTo(exercise.id)
            assertThat(tricepsExercises[0].id).isEqualTo(exercise.id)
            assertThat(shoulderExercises[0].id).isEqualTo(exercise.id)
        }
}
