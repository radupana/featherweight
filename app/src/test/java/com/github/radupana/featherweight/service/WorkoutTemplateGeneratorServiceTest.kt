package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseVariationDao
import com.github.radupana.featherweight.data.model.SkillLevel
import com.github.radupana.featherweight.data.model.TimeAvailable
import com.github.radupana.featherweight.data.model.TrainingGoal
import com.github.radupana.featherweight.data.model.WorkoutTemplateGenerationConfig
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class WorkoutTemplateGeneratorServiceTest {
    private lateinit var service: WorkoutTemplateGeneratorService
    private lateinit var exerciseVariationDao: ExerciseVariationDao
    private lateinit var mockVariation: ExerciseVariation

    @Before
    fun setup() {
        exerciseVariationDao = mockk<ExerciseVariationDao>()
        service = WorkoutTemplateGeneratorService(exerciseVariationDao)

        mockVariation =
            ExerciseVariation(
                id = 1L,
                coreExerciseId = 1L,
                name = "Barbell Bench Press",
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
            )
    }

    @Test
    fun `generateTemplate_pushWorkout_quickTime_returnsCorrectExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.STRENGTH,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            val benchPress = mockVariation.copy(id = 1L, name = "Barbell Bench Press")
            val shoulderPress = mockVariation.copy(id = 2L, name = "Dumbbell Shoulder Press")
            val fly = mockVariation.copy(id = 3L, name = "Dumbbell Fly")
            val triceps = mockVariation.copy(id = 4L, name = "Cable Tricep Pushdown")

            coEvery { exerciseVariationDao.getExerciseVariationByName("Barbell Bench Press") } returns benchPress
            coEvery { exerciseVariationDao.getExerciseVariationByName("Dumbbell Shoulder Press") } returns shoulderPress
            coEvery { exerciseVariationDao.getExerciseVariationByName("Dumbbell Fly") } returns fly
            coEvery { exerciseVariationDao.getExerciseVariationByName("Cable Tricep Pushdown") } returns triceps

            // Act
            val result = service.generateTemplate("Push", config)

            // Assert
            assertThat(result).hasSize(4)
            assertThat(result[0].first.name).isEqualTo("Barbell Bench Press")
            assertThat(result[1].first.name).isEqualTo("Dumbbell Shoulder Press")
            assertThat(result[2].first.name).isEqualTo("Dumbbell Fly")
            assertThat(result[3].first.name).isEqualTo("Cable Tricep Pushdown")
        }

    @Test
    fun `generateTemplate_pullWorkout_standardTime_returnsCorrectExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.HYPERTROPHY,
                    time = TimeAvailable.STANDARD,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Pull", config)

            // Assert
            assertThat(result).hasSize(6) // Standard pull has 6 exercises
        }

    @Test
    fun `generateTemplate_legsWorkout_extendedTime_returnsCorrectExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.STRENGTH,
                    time = TimeAvailable.EXTENDED,
                    skillLevel = SkillLevel.ADVANCED,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Legs", config)

            // Assert
            assertThat(result).hasSize(7) // Extended legs has 7 exercises
        }

    @Test
    fun `generateTemplate_upperBodyWorkout_quickTime_returnsCorrectExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.HYPERTROPHY,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.INTERMEDIATE, // Change to INTERMEDIATE to avoid beginner reduction
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Upper", config)

            // Assert
            assertThat(result).hasSize(4) // Quick upper body has 4 exercises
        }

    @Test
    fun `generateTemplate_fullBodyWorkout_standardTime_returnsCorrectExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.HYPERTROPHY,
                    time = TimeAvailable.STANDARD,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Full Body", config)

            // Assert
            assertThat(result).isNotEmpty()
        }

    @Test
    fun `generateTemplate_unknownTemplate_returnsEmptyList`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.STRENGTH,
                    time = TimeAvailable.STANDARD,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            // Act
            val result = service.generateTemplate("Unknown", config)

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `generateTemplate_strength_goal_returnsCorrectSetsAndReps`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.STRENGTH,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Push", config)

            // Assert
            assertThat(result).isNotEmpty()
            // For strength, expect lower reps (3-5) and more sets (4-5)
            val (_, sets, reps) = result.first()
            assertThat(sets).isAtLeast(3)
            assertThat(reps).isAtMost(6)
        }

    @Test
    fun `generateTemplate_muscleBuilding_goal_returnsCorrectSetsAndReps`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.HYPERTROPHY,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Push", config)

            // Assert
            assertThat(result).isNotEmpty()
            // For muscle building, expect medium reps (8-12) and moderate sets (3-4)
            val (_, sets, reps) = result.first()
            assertThat(reps).isAtLeast(8)
            assertThat(reps).isAtMost(12)
        }

    @Test
    fun `generateTemplate_beginner_skill_returnsFewerExercises`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.HYPERTROPHY,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.BEGINNER,
                )

            coEvery { exerciseVariationDao.getExerciseVariationByName(any()) } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName(any<String>().lowercase()) } returns mockVariation

            // Act
            val result = service.generateTemplate("Push", config)

            // Assert
            assertThat(result).isNotEmpty()
            // Beginners get 2/3 of the exercises (4 * 2/3 = 2.66, so 2 exercises)
            assertThat(result).hasSize(2)
        }

    @Test
    fun `generateTemplate_exerciseNotFound_skipsExercise`() =
        runTest {
            // Arrange
            val config =
                WorkoutTemplateGenerationConfig(
                    goal = TrainingGoal.STRENGTH,
                    time = TimeAvailable.QUICK,
                    skillLevel = SkillLevel.INTERMEDIATE,
                )

            // Only return variations for some exercises
            coEvery { exerciseVariationDao.getExerciseVariationByName("Barbell Bench Press") } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName("barbell bench press") } returns mockVariation
            coEvery { exerciseVariationDao.getExerciseVariationByName("Dumbbell Shoulder Press") } returns null
            coEvery { exerciseVariationDao.getExerciseVariationByName("dumbbell shoulder press") } returns null
            coEvery { exerciseVariationDao.getExerciseVariationByName("Dumbbell Fly") } returns mockVariation.copy(id = 3L, name = "Dumbbell Fly")
            coEvery { exerciseVariationDao.getExerciseVariationByName("dumbbell fly") } returns mockVariation.copy(id = 3L, name = "Dumbbell Fly")
            coEvery { exerciseVariationDao.getExerciseVariationByName("Cable Tricep Pushdown") } returns null
            coEvery { exerciseVariationDao.getExerciseVariationByName("cable tricep pushdown") } returns null

            // Act
            val result = service.generateTemplate("Push", config)

            // Assert
            assertThat(result).hasSize(2) // Only 2 exercises found
            assertThat(result[0].first.name).isEqualTo("Barbell Bench Press")
            assertThat(result[1].first.name).isEqualTo("Dumbbell Fly")
        }
}
