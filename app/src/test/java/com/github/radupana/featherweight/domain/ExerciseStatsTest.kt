package com.github.radupana.featherweight.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseStatsTest {
    @Test
    fun exerciseStats_withAllFields_createsCorrectly() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Barbell Bench Press",
                avgWeight = 65.5f,
                avgReps = 8,
                avgRpe = 7.5f,
                maxWeight = 80f,
                totalSets = 12,
            )

        // Assert
        assertThat(stats.exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(stats.avgWeight).isEqualTo(65.5f)
        assertThat(stats.avgReps).isEqualTo(8)
        assertThat(stats.avgRpe).isEqualTo(7.5f)
        assertThat(stats.maxWeight).isEqualTo(80f)
        assertThat(stats.totalSets).isEqualTo(12)
    }

    @Test
    fun exerciseStats_withNullRpe_handlesNull() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Squat",
                avgWeight = 100f,
                avgReps = 5,
                avgRpe = null,
                maxWeight = 120f,
                totalSets = 20,
            )

        // Assert
        assertThat(stats.exerciseName).isEqualTo("Squat")
        assertThat(stats.avgWeight).isEqualTo(100f)
        assertThat(stats.avgReps).isEqualTo(5)
        assertThat(stats.avgRpe).isNull()
        assertThat(stats.maxWeight).isEqualTo(120f)
        assertThat(stats.totalSets).isEqualTo(20)
    }

    @Test
    fun exerciseStats_equality_worksCorrectly() {
        // Arrange
        val stats1 =
            ExerciseStats(
                exerciseName = "Deadlift",
                avgWeight = 140f,
                avgReps = 3,
                avgRpe = 8.5f,
                maxWeight = 160f,
                totalSets = 15,
            )

        val stats2 =
            ExerciseStats(
                exerciseName = "Deadlift",
                avgWeight = 140f,
                avgReps = 3,
                avgRpe = 8.5f,
                maxWeight = 160f,
                totalSets = 15,
            )

        val stats3 =
            ExerciseStats(
                exerciseName = "Deadlift",
                avgWeight = 145f, // Different avg weight
                avgReps = 3,
                avgRpe = 8.5f,
                maxWeight = 160f,
                totalSets = 15,
            )

        // Assert
        assertThat(stats1).isEqualTo(stats2)
        assertThat(stats1).isNotEqualTo(stats3)
        assertThat(stats1.hashCode()).isEqualTo(stats2.hashCode())
    }

    @Test
    fun exerciseStats_copy_createsIndependentCopy() {
        // Arrange
        val original =
            ExerciseStats(
                exerciseName = "Overhead Press",
                avgWeight = 45f,
                avgReps = 6,
                avgRpe = 7f,
                maxWeight = 55f,
                totalSets = 9,
            )

        // Act
        val copy =
            original.copy(
                avgWeight = 47.5f,
                maxWeight = 60f,
                totalSets = 12,
            )

        // Assert
        assertThat(copy.exerciseName).isEqualTo("Overhead Press") // Unchanged
        assertThat(copy.avgWeight).isEqualTo(47.5f)
        assertThat(copy.avgReps).isEqualTo(6) // Unchanged
        assertThat(copy.avgRpe).isEqualTo(7f) // Unchanged
        assertThat(copy.maxWeight).isEqualTo(60f)
        assertThat(copy.totalSets).isEqualTo(12)

        // Verify original is unchanged
        assertThat(original.avgWeight).isEqualTo(45f)
        assertThat(original.maxWeight).isEqualTo(55f)
        assertThat(original.totalSets).isEqualTo(9)
    }

    @Test
    fun exerciseStats_withZeroValues_handlesZeros() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Bodyweight Exercise",
                avgWeight = 0f,
                avgReps = 0,
                avgRpe = 0f,
                maxWeight = 0f,
                totalSets = 0,
            )

        // Assert
        assertThat(stats.avgWeight).isEqualTo(0f)
        assertThat(stats.avgReps).isEqualTo(0)
        assertThat(stats.avgRpe).isEqualTo(0f)
        assertThat(stats.maxWeight).isEqualTo(0f)
        assertThat(stats.totalSets).isEqualTo(0)
    }

    @Test
    fun exerciseStats_withHighValues_handlesLargeNumbers() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Leg Press",
                avgWeight = 450.75f,
                avgReps = 25,
                avgRpe = 10f,
                maxWeight = 600f,
                totalSets = 1000,
            )

        // Assert
        assertThat(stats.avgWeight).isEqualTo(450.75f)
        assertThat(stats.avgReps).isEqualTo(25)
        assertThat(stats.avgRpe).isEqualTo(10f)
        assertThat(stats.maxWeight).isEqualTo(600f)
        assertThat(stats.totalSets).isEqualTo(1000)
    }

    @Test
    fun exerciseStats_withFloatPrecision_maintainsPrecision() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Dumbbell Curl",
                avgWeight = 12.5f,
                avgReps = 12,
                avgRpe = 7.25f,
                maxWeight = 15.5f,
                totalSets = 4,
            )

        // Assert
        assertThat(stats.avgWeight).isEqualTo(12.5f)
        assertThat(stats.avgRpe).isEqualTo(7.25f)
        assertThat(stats.maxWeight).isEqualTo(15.5f)
    }

    @Test
    fun exerciseStats_toString_includesAllFields() {
        // Arrange
        val stats =
            ExerciseStats(
                exerciseName = "Pull-Up",
                avgWeight = 10f,
                avgReps = 8,
                avgRpe = 8f,
                maxWeight = 20f,
                totalSets = 5,
            )

        // Act
        val stringRepresentation = stats.toString()

        // Assert
        assertThat(stringRepresentation).contains("exerciseName=Pull-Up")
        assertThat(stringRepresentation).contains("avgWeight=10.0")
        assertThat(stringRepresentation).contains("avgReps=8")
        assertThat(stringRepresentation).contains("avgRpe=8.0")
        assertThat(stringRepresentation).contains("maxWeight=20.0")
        assertThat(stringRepresentation).contains("totalSets=5")
    }

    @Test
    fun exerciseStats_differentExerciseNames_areNotEqual() {
        // Arrange
        val stats1 =
            ExerciseStats(
                exerciseName = "Barbell Row",
                avgWeight = 70f,
                avgReps = 10,
                avgRpe = 7f,
                maxWeight = 80f,
                totalSets = 12,
            )

        val stats2 =
            ExerciseStats(
                exerciseName = "Cable Row", // Different exercise
                avgWeight = 70f,
                avgReps = 10,
                avgRpe = 7f,
                maxWeight = 80f,
                totalSets = 12,
            )

        // Assert
        assertThat(stats1).isNotEqualTo(stats2)
    }

    @Test
    fun exerciseStats_withEmptyExerciseName_handlesEmptyString() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "",
                avgWeight = 50f,
                avgReps = 10,
                avgRpe = 7f,
                maxWeight = 60f,
                totalSets = 3,
            )

        // Assert
        assertThat(stats.exerciseName).isEmpty()
        assertThat(stats.avgWeight).isEqualTo(50f)
    }

    @Test
    fun exerciseStats_avgWeightLessThanMax_logicalConsistency() {
        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Bench Press",
                avgWeight = 60f,
                avgReps = 10,
                avgRpe = 7.5f,
                maxWeight = 80f,
                totalSets = 5,
            )

        // Assert - Average should be less than or equal to max
        assertThat(stats.avgWeight).isAtMost(stats.maxWeight)
    }

    @Test
    fun exerciseStats_withNegativeValues_allowsNegatives() {
        // This test verifies the data class doesn't validate values
        // In a real scenario, negative weights wouldn't make sense

        // Act
        val stats =
            ExerciseStats(
                exerciseName = "Test",
                avgWeight = -10f,
                avgReps = -5,
                avgRpe = -1f,
                maxWeight = -5f,
                totalSets = -2,
            )

        // Assert - Data class allows any values
        assertThat(stats.avgWeight).isEqualTo(-10f)
        assertThat(stats.avgReps).isEqualTo(-5)
        assertThat(stats.avgRpe).isEqualTo(-1f)
        assertThat(stats.maxWeight).isEqualTo(-5f)
        assertThat(stats.totalSets).isEqualTo(-2)
    }
}
