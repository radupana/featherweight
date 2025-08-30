package com.github.radupana.featherweight.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class WeightExtractionServiceTest {
    private lateinit var service: WeightExtractionService

    @Before
    fun setup() {
        service = WeightExtractionService()
    }

    @Test
    fun `extractWeights extracts explicit weights in kg`() {
        // Arrange
        val input = "I can squat 100kg and bench 80kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[0].weight).isEqualTo(100f)
        assertThat(result[0].source).isEqualTo("explicit")
        assertThat(result[1].exerciseName).isEqualTo("Bench Press")
        assertThat(result[1].weight).isEqualTo(80f)
        assertThat(result[1].source).isEqualTo("explicit")
    }

    @Test
    fun `extractWeights extracts 1RM mentions`() {
        // Arrange
        val input = "squat 1rm 140kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).isNotEmpty()
        val squatResult = result.find { it.exerciseName == "Back Squat" }
        assertThat(squatResult).isNotNull()
        assertThat(squatResult?.weight).isEqualTo(140f)
        assertThat(squatResult?.source).isEqualTo("1rm")
    }

    @Test
    fun `extractWeights converts pounds to kg`() {
        // Arrange
        val input = "I bench 200lbs and squat 300 lb"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].exerciseName).isEqualTo("Bench Press")
        assertThat(result[0].weight).isWithin(0.1f).of(90.7f) // 200lbs * 0.453592
        assertThat(result[1].exerciseName).isEqualTo("Back Squat")
        assertThat(result[1].weight).isWithin(0.1f).of(136.1f) // 300lbs * 0.453592
    }

    @Test
    fun `extractWeights handles plate notation`() {
        // Arrange
        val input = "I can squat 2 plates"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[0].weight).isEqualTo(100f) // 20kg bar + 80kg plates (2*40)
    }

    @Test
    fun `extractWeights recognizes exercise aliases`() {
        // Arrange
        val input = "barbell squat 100kg, military press 70kg, bent over row 90kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[1].exerciseName).isEqualTo("Overhead Press")
        assertThat(result[2].exerciseName).isEqualTo("Barbell Row")
    }

    @Test
    fun `extractWeights handles mixed case input`() {
        // Arrange
        val input = "I Can BENCH PRESS 100KG and DeadLift 150kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].exerciseName).isEqualTo("Bench Press")
        assertThat(result[0].weight).isEqualTo(100f)
        assertThat(result[1].exerciseName).isEqualTo("Conventional Deadlift")
        assertThat(result[1].weight).isEqualTo(150f)
    }

    @Test
    fun `extractWeights handles decimal weights`() {
        // Arrange
        val input = "bench 82.5kg, squat 102.5kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].exerciseName).isEqualTo("Bench Press")
        assertThat(result[0].weight).isEqualTo(82.5f)
        assertThat(result[1].exerciseName).isEqualTo("Back Squat")
        assertThat(result[1].weight).isEqualTo(102.5f)
    }

    @Test
    fun `extractWeights ignores unrecognized exercises`() {
        // Arrange
        val input = "I can jumping jack 100kg and squat 120kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[0].weight).isEqualTo(120f)
    }

    @Test
    fun `extractWeights handles bodyweight notation`() {
        // Arrange
        val input = "I can overhead press 1bodyweight"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).isNotEmpty()
        val pressResult = result.find { it.exerciseName == "Overhead Press" }
        assertThat(pressResult).isNotNull()
        assertThat(pressResult?.weight).isEqualTo(75f) // Default bodyweight assumption
    }

    @Test
    fun `extractWeights removes duplicate entries with same exercise and source`() {
        // Arrange
        val input = "squat 100kg, I squat 100kg, my squat is 100kg"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(1) // Should deduplicate based on exercise_source
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[0].weight).isEqualTo(100f)
    }

    @Test
    fun `extractWeights handles empty input`() {
        // Arrange
        val input = ""

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `extractWeights handles input with no weights`() {
        // Arrange
        val input = "I love doing squats and bench press"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `extractWeights handles various weight formats in one input`() {
        // Arrange
        val input = "squat 100kg, bench 200lbs, deadlift 3 plates"

        // Act
        val result = service.extractWeights(input)

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].exerciseName).isEqualTo("Back Squat")
        assertThat(result[0].weight).isEqualTo(100f)
        assertThat(result[1].exerciseName).isEqualTo("Bench Press")
        assertThat(result[1].weight).isWithin(0.1f).of(90.7f)
        assertThat(result[2].exerciseName).isEqualTo("Conventional Deadlift")
        assertThat(result[2].weight).isEqualTo(140f) // 20kg bar + 120kg plates
    }
}
