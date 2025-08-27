package com.github.radupana.featherweight.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseStatsTest {
    
    @Test
    fun `create ExerciseStats with all fields`() {
        // Arrange & Act
        val stats = ExerciseStats(
            exerciseName = "Barbell Bench Press",
            avgWeight = 100f,
            avgReps = 8,
            avgRpe = 7.5f,
            maxWeight = 120f,
            totalSets = 12
        )
        
        // Assert
        assertThat(stats.exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(stats.avgWeight).isEqualTo(100f)
        assertThat(stats.avgReps).isEqualTo(8)
        assertThat(stats.avgRpe).isEqualTo(7.5f)
        assertThat(stats.maxWeight).isEqualTo(120f)
        assertThat(stats.totalSets).isEqualTo(12)
    }
    
    @Test
    fun `create ExerciseStats with null avgRpe`() {
        // Arrange & Act
        val stats = ExerciseStats(
            exerciseName = "Pull Up",
            avgWeight = 0f,
            avgReps = 10,
            avgRpe = null,
            maxWeight = 20f,
            totalSets = 3
        )
        
        // Assert
        assertThat(stats.avgRpe).isNull()
    }
    
    @Test
    fun `equals and hashCode work correctly`() {
        // Arrange
        val stats1 = ExerciseStats(
            exerciseName = "Squat",
            avgWeight = 140f,
            avgReps = 5,
            avgRpe = 8f,
            maxWeight = 160f,
            totalSets = 5
        )
        
        val stats2 = ExerciseStats(
            exerciseName = "Squat",
            avgWeight = 140f,
            avgReps = 5,
            avgRpe = 8f,
            maxWeight = 160f,
            totalSets = 5
        )
        
        val stats3 = ExerciseStats(
            exerciseName = "Deadlift",
            avgWeight = 140f,
            avgReps = 5,
            avgRpe = 8f,
            maxWeight = 160f,
            totalSets = 5
        )
        
        // Assert
        assertThat(stats1).isEqualTo(stats2)
        assertThat(stats1.hashCode()).isEqualTo(stats2.hashCode())
        assertThat(stats1).isNotEqualTo(stats3)
    }
    
    @Test
    fun `copy with modified fields`() {
        // Arrange
        val original = ExerciseStats(
            exerciseName = "Barbell Row",
            avgWeight = 80f,
            avgReps = 10,
            avgRpe = 7f,
            maxWeight = 90f,
            totalSets = 4
        )
        
        // Act
        val modified = original.copy(avgWeight = 85f, maxWeight = 95f)
        
        // Assert
        assertThat(modified.avgWeight).isEqualTo(85f)
        assertThat(modified.maxWeight).isEqualTo(95f)
        assertThat(modified.exerciseName).isEqualTo("Barbell Row")
        assertThat(modified.avgReps).isEqualTo(10)
        assertThat(modified.avgRpe).isEqualTo(7f)
        assertThat(modified.totalSets).isEqualTo(4)
    }
    
    @Test
    fun `toString provides readable representation`() {
        // Arrange & Act
        val stats = ExerciseStats(
            exerciseName = "Overhead Press",
            avgWeight = 60f,
            avgReps = 6,
            avgRpe = 8.5f,
            maxWeight = 70f,
            totalSets = 3
        )
        
        // Assert
        val stringRep = stats.toString()
        assertThat(stringRep).contains("Overhead Press")
        assertThat(stringRep).contains("60")
        assertThat(stringRep).contains("6")
        assertThat(stringRep).contains("8.5")
        assertThat(stringRep).contains("70")
        assertThat(stringRep).contains("3")
    }
}