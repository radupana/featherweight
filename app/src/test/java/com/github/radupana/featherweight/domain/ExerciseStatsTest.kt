package com.github.radupana.featherweight.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseStatsTest {
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
