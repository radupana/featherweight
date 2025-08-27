package com.github.radupana.featherweight.validation

import com.github.radupana.featherweight.data.exercise.ExerciseDao
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExerciseValidatorTest {

    @MockK
    private lateinit var mockExerciseDao: ExerciseDao

    private lateinit var validator: ExerciseValidator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        validator = ExerciseValidator(mockExerciseDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initialize loads exercise names and ids from database`() = runTest {
        // Arrange
        val mockExercises = listOf(
            createMockExerciseWithDetails(1L, "Barbell Squat"),
            createMockExerciseWithDetails(2L, "Bench Press"),
            createMockExerciseWithDetails(3L, "Deadlift")
        )
        coEvery { mockExerciseDao.getAllExercisesWithDetails() } returns mockExercises

        // Act
        validator.initialize()

        // Assert
        coVerify { mockExerciseDao.getAllExercisesWithDetails() }
    }

    @Test
    fun `validateExerciseName returns Valid for existing exercise`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Barbell Squat")

        // Assert
        assertThat(result).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `validateExerciseName returns Invalid for non-existing exercise`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Non-Existent Exercise")

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.providedName).isEqualTo("Non-Existent Exercise")
        assertThat(invalidResult.reason).contains("does not exist")
    }

    @Test
    fun `validateExerciseName suggests closest match for similar name`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Squat")

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.suggestion).isEqualTo("Barbell Squat")
    }

    @Test
    fun `validateExerciseId returns Valid for existing id`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseId(1L)

        // Assert
        assertThat(result).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `validateExerciseId returns Invalid for non-existing id`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseId(999L)

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.reason).contains("ID 999 does not exist")
    }

    @Test
    fun `validateExerciseNames validates multiple names`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val results = validator.validateExerciseNames(
            listOf("Barbell Squat", "Invalid Exercise", "Bench Press")
        )

        // Assert
        assertThat(results).hasSize(3)
        assertThat(results["Barbell Squat"]).isEqualTo(ValidationResult.Valid)
        assertThat(results["Bench Press"]).isEqualTo(ValidationResult.Valid)
        assertThat(results["Invalid Exercise"]).isInstanceOf(ValidationResult.Invalid::class.java)
    }

    @Test
    fun `validateProgrammeStructure finds invalid exercises in JSON`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()
        val jsonStructure = """
            {
                "exercises": [
                    {"name": "Barbell Squat", "sets": 3},
                    {"name": "Invalid Exercise", "sets": 3},
                    {"name": "Bench Press", "sets": 3}
                ]
            }
        """.trimIndent()

        // Act
        val errors = validator.validateProgrammeStructure(jsonStructure)

        // Assert
        assertThat(errors).hasSize(1)
        assertThat(errors[0].value).isEqualTo("Invalid Exercise")
        assertThat(errors[0].field).isEqualTo("exercise")
    }

    @Test
    fun `validateProgrammeStructure returns empty list for valid exercises`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()
        val jsonStructure = """
            {
                "exercises": [
                    {"name": "Barbell Squat", "sets": 3},
                    {"name": "Bench Press", "sets": 3}
                ]
            }
        """.trimIndent()

        // Act
        val errors = validator.validateProgrammeStructure(jsonStructure)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    fun `validateProgrammeStructure handles malformed JSON gracefully`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()
        val malformedJson = "not a valid json structure"

        // Act
        val errors = validator.validateProgrammeStructure(malformedJson)

        // Assert
        assertThat(errors).isEmpty() // No exercises pattern found, so no errors
    }

    @Test
    fun `findClosestMatch finds exact substring matches`() = runTest {
        // Arrange
        val mockExercises = listOf(
            createMockExerciseWithDetails(1L, "Barbell Back Squat"),
            createMockExerciseWithDetails(2L, "Front Squat"),
            createMockExerciseWithDetails(3L, "Bulgarian Split Squat")
        )
        coEvery { mockExerciseDao.getAllExercisesWithDetails() } returns mockExercises
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Back Squat")

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.suggestion).isEqualTo("Barbell Back Squat")
    }

    @Test
    fun `findClosestMatch handles word-based matching`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Overhead Press")

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.suggestion).isEqualTo("Barbell Overhead Press")
    }

    @Test
    fun `findClosestMatch returns null when no exercises loaded`() = runTest {
        // Arrange
        coEvery { mockExerciseDao.getAllExercisesWithDetails() } returns emptyList()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("Some Exercise")

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.suggestion).isNull()
    }

    @Test
    fun `ValidationResult Valid is singleton`() {
        // Assert
        assertThat(ValidationResult.Valid).isSameInstanceAs(ValidationResult.Valid)
    }

    @Test
    fun `ValidationResult Invalid contains all fields`() {
        // Arrange & Act
        val invalid = ValidationResult.Invalid(
            providedName = "Test",
            suggestion = "Suggestion",
            reason = "Reason"
        )

        // Assert
        assertThat(invalid.providedName).isEqualTo("Test")
        assertThat(invalid.suggestion).isEqualTo("Suggestion")
        assertThat(invalid.reason).isEqualTo("Reason")
    }

    @Test
    fun `ValidationError data class properties`() {
        // Arrange & Act
        val error = ValidationError(
            field = "exercise",
            value = "Invalid",
            error = "Not found",
            suggestion = "Valid Exercise"
        )

        // Assert
        assertThat(error.field).isEqualTo("exercise")
        assertThat(error.value).isEqualTo("Invalid")
        assertThat(error.error).isEqualTo("Not found")
        assertThat(error.suggestion).isEqualTo("Valid Exercise")
    }

    @Test
    fun `validateProgrammeStructure handles nested exercises arrays`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()
        val jsonStructure = """
            {
                "week1": {
                    "exercises": [
                        {"name": "Barbell Squat"},
                        {"name": "Invalid Exercise"}
                    ]
                },
                "week2": {
                    "exercises": [
                        {"name": "Bench Press"},
                        {"name": "Another Invalid"}
                    ]
                }
            }
        """.trimIndent()

        // Act
        val errors = validator.validateProgrammeStructure(jsonStructure)

        // Assert
        assertThat(errors).hasSize(2)
        assertThat(errors.map { it.value }).containsExactly("Invalid Exercise", "Another Invalid")
    }

    @Test
    fun `case insensitive matching for exercise suggestions`() = runTest {
        // Arrange
        setupMockExercises()
        validator.initialize()

        // Act
        val result = validator.validateExerciseName("barbell squat") // lowercase

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.suggestion).isEqualTo("Barbell Squat")
    }

    private fun setupMockExercises() {
        val mockExercises = listOf(
            createMockExerciseWithDetails(1L, "Barbell Squat"),
            createMockExerciseWithDetails(2L, "Bench Press"),
            createMockExerciseWithDetails(3L, "Deadlift"),
            createMockExerciseWithDetails(4L, "Barbell Overhead Press"),
            createMockExerciseWithDetails(5L, "Pull Up")
        )
        coEvery { mockExerciseDao.getAllExercisesWithDetails() } returns mockExercises
    }

    private fun createMockExerciseWithDetails(id: Long, name: String): ExerciseWithDetails {
        return ExerciseWithDetails(
            variation = ExerciseVariation(
                id = id,
                coreExerciseId = 1L,
                name = name,
                equipment = Equipment.BARBELL,
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                requiresWeight = true,
                recommendedRepRange = "8-12",
                rmScalingType = RMScalingType.STANDARD,
                usageCount = 0
            )
        )
    }
}