package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for ProgressAnalyticsService data classes
 * 
 * Tests data structures used for progress tracking and analytics including:
 * - Exercise progress data aggregation
 * - Performance statistics calculations
 * - Data point creation and validation
 * - Progress percentage calculations
 * - Exercise grouping and summaries
 */
class ProgressAnalyticsServiceTest {
    
    // ========== ExerciseProgressData Tests ==========
    
    @Test
    fun `ExerciseProgressData stores exercise progress information`() {
        // Arrange
        val dataPoints = listOf(
            ProgressDataPoint(
                date = LocalDateTime.now().minusDays(7),
                weight = 100f,
                volume = 500f,
                rpe = 7.5f
            ),
            ProgressDataPoint(
                date = LocalDateTime.now(),
                weight = 105f,
                volume = 525f,
                rpe = 8f
            )
        )
        
        // Act
        val progressData = ExerciseProgressData(
            exerciseName = "Barbell Back Squat",
            dataPoints = dataPoints,
            currentMax = 105f,
            progressPercentage = 5f,
            totalSessions = 10
        )
        
        // Assert
        assertThat(progressData.exerciseName).isEqualTo("Barbell Back Squat")
        assertThat(progressData.dataPoints).hasSize(2)
        assertThat(progressData.currentMax).isEqualTo(105f)
        assertThat(progressData.progressPercentage).isEqualTo(5f)
        assertThat(progressData.totalSessions).isEqualTo(10)
    }
    
    @Test
    fun `ExerciseProgressData handles empty data points`() {
        // Arrange & Act
        val progressData = ExerciseProgressData(
            exerciseName = "Deadlift",
            dataPoints = emptyList(),
            currentMax = 0f,
            progressPercentage = 0f,
            totalSessions = 0
        )
        
        // Assert
        assertThat(progressData.dataPoints).isEmpty()
        assertThat(progressData.currentMax).isEqualTo(0f)
        assertThat(progressData.progressPercentage).isEqualTo(0f)
    }
    
    @Test
    fun `ExerciseProgressData calculates progress from baseline`() {
        // Arrange
        val current = 125f
        val expectedProgress = 25f // 25% increase
        
        // Act
        val progressData = ExerciseProgressData(
            exerciseName = "Bench Press",
            dataPoints = listOf(
                ProgressDataPoint(LocalDateTime.now(), current, 1000f, null)
            ),
            currentMax = current,
            progressPercentage = expectedProgress,
            totalSessions = 15
        )
        
        // Assert
        assertThat(progressData.progressPercentage).isEqualTo(expectedProgress)
    }
    
    // ========== ProgressDataPoint Tests ==========
    
    @Test
    fun `ProgressDataPoint stores workout metrics`() {
        // Arrange
        val date = LocalDateTime.now()
        
        // Act
        val dataPoint = ProgressDataPoint(
            date = date,
            weight = 150f,
            volume = 750f,
            rpe = 8.5f
        )
        
        // Assert
        assertThat(dataPoint.date).isEqualTo(date)
        assertThat(dataPoint.weight).isEqualTo(150f)
        assertThat(dataPoint.volume).isEqualTo(750f)
        assertThat(dataPoint.rpe).isEqualTo(8.5f)
    }
    
    @Test
    fun `ProgressDataPoint handles null RPE`() {
        // Act
        val dataPoint = ProgressDataPoint(
            date = LocalDateTime.now(),
            weight = 100f,
            volume = 500f,
            rpe = null
        )
        
        // Assert
        assertThat(dataPoint.rpe).isNull()
    }
    
    @Test
    fun `ProgressDataPoint volume calculation is consistent`() {
        // Arrange
        val weight = 100f
        val reps = 5
        val expectedVolume = weight * reps
        
        // Act
        val dataPoint = ProgressDataPoint(
            date = LocalDateTime.now(),
            weight = weight,
            volume = expectedVolume,
            rpe = null
        )
        
        // Assert
        assertThat(dataPoint.volume).isEqualTo(500f)
    }
    
    // ========== ExerciseSummary Tests ==========
    
    @Test
    fun `ExerciseSummary stores exercise overview data`() {
        // Arrange
        val lastWorkout = LocalDateTime.now().minusDays(2)
        val chartData = listOf(100f, 102.5f, 105f, 107.5f, 110f)
        
        // Act
        val summary = ExerciseSummary(
            exerciseName = "Overhead Press",
            currentMax = 110f,
            progressPercentage = 10f,
            lastWorkout = lastWorkout,
            miniChartData = chartData,
            sessionCount = 20
        )
        
        // Assert
        assertThat(summary.exerciseName).isEqualTo("Overhead Press")
        assertThat(summary.currentMax).isEqualTo(110f)
        assertThat(summary.progressPercentage).isEqualTo(10f)
        assertThat(summary.lastWorkout).isEqualTo(lastWorkout)
        assertThat(summary.miniChartData).containsExactlyElementsIn(chartData).inOrder()
        assertThat(summary.sessionCount).isEqualTo(20)
    }
    
    @Test
    fun `ExerciseSummary handles no workout history`() {
        // Act
        val summary = ExerciseSummary(
            exerciseName = "New Exercise",
            currentMax = 0f,
            progressPercentage = 0f,
            lastWorkout = null,
            miniChartData = emptyList(),
            sessionCount = 0
        )
        
        // Assert
        assertThat(summary.lastWorkout).isNull()
        assertThat(summary.miniChartData).isEmpty()
        assertThat(summary.sessionCount).isEqualTo(0)
    }
    
    @Test
    fun `ExerciseSummary miniChartData maintains order`() {
        // Arrange
        val chartData = listOf(100f, 95f, 97.5f, 102.5f, 105f)
        
        // Act
        val summary = ExerciseSummary(
            exerciseName = "Squat",
            currentMax = 105f,
            progressPercentage = 5f,
            lastWorkout = LocalDateTime.now(),
            miniChartData = chartData,
            sessionCount = 5
        )
        
        // Assert
        assertThat(summary.miniChartData).containsExactlyElementsIn(chartData).inOrder()
        assertThat(summary.miniChartData.first()).isEqualTo(100f)
        assertThat(summary.miniChartData.last()).isEqualTo(105f)
    }
    
    // ========== GroupedExerciseSummary Tests ==========
    
    @Test
    fun `GroupedExerciseSummary separates big four from other exercises`() {
        // Arrange
        val bigFour = listOf(
            createSummary("Barbell Back Squat"),
            createSummary("Barbell Bench Press"),
            createSummary("Barbell Deadlift"),
            createSummary("Barbell Overhead Press")
        )
        val others = listOf(
            createSummary("Barbell Row"),
            createSummary("Pull Up")
        )
        
        // Act
        val grouped = GroupedExerciseSummary(
            bigFourExercises = bigFour,
            otherExercises = others
        )
        
        // Assert
        assertThat(grouped.bigFourExercises).hasSize(4)
        assertThat(grouped.otherExercises).hasSize(2)
        assertThat(grouped.bigFourExercises.map { it.exerciseName })
            .containsExactly(
                "Barbell Back Squat",
                "Barbell Bench Press",
                "Barbell Deadlift",
                "Barbell Overhead Press"
            )
    }
    
    @Test
    fun `GroupedExerciseSummary handles empty groups`() {
        // Act
        val grouped = GroupedExerciseSummary(
            bigFourExercises = emptyList(),
            otherExercises = emptyList()
        )
        
        // Assert
        assertThat(grouped.bigFourExercises).isEmpty()
        assertThat(grouped.otherExercises).isEmpty()
    }
    
    @Test
    fun `GroupedExerciseSummary allows partial big four`() {
        // Arrange
        val partial = listOf(
            createSummary("Barbell Back Squat"),
            createSummary("Barbell Bench Press")
        )
        
        // Act
        val grouped = GroupedExerciseSummary(
            bigFourExercises = partial,
            otherExercises = emptyList()
        )
        
        // Assert
        assertThat(grouped.bigFourExercises).hasSize(2)
    }
    
    // ========== PerformanceStats Tests ==========
    
    @Test
    fun `PerformanceStats stores exercise performance metrics`() {
        // Act
        val stats = PerformanceStats(
            exerciseName = "Barbell Back Squat",
            bestSingle = "150kg x 1",
            bestVolume = "2000kg",
            averageRpe = 7.5f,
            consistency = 0.85f,
            totalSessions = 25
        )
        
        // Assert
        assertThat(stats.exerciseName).isEqualTo("Barbell Back Squat")
        assertThat(stats.bestSingle).isEqualTo("150kg x 1")
        assertThat(stats.bestVolume).isEqualTo("2000kg")
        assertThat(stats.averageRpe).isEqualTo(7.5f)
        assertThat(stats.consistency).isEqualTo(0.85f)
        assertThat(stats.totalSessions).isEqualTo(25)
    }
    
    @Test
    fun `PerformanceStats handles null best values`() {
        // Act
        val stats = PerformanceStats(
            exerciseName = "New Exercise",
            bestSingle = null,
            bestVolume = null,
            averageRpe = null,
            consistency = 0f,
            totalSessions = 0
        )
        
        // Assert
        assertThat(stats.bestSingle).isNull()
        assertThat(stats.bestVolume).isNull()
        assertThat(stats.averageRpe).isNull()
    }
    
    @Test
    fun `PerformanceStats consistency score is bounded`() {
        // Arrange & Act
        val perfectConsistency = PerformanceStats(
            exerciseName = "Exercise 1",
            bestSingle = "100kg",
            bestVolume = "1000kg",
            averageRpe = 8f,
            consistency = 1.0f,
            totalSessions = 50
        )
        
        val noConsistency = PerformanceStats(
            exerciseName = "Exercise 2",
            bestSingle = "50kg",
            bestVolume = "500kg",
            averageRpe = 6f,
            consistency = 0.0f,
            totalSessions = 1
        )
        
        // Assert
        assertThat(perfectConsistency.consistency).isEqualTo(1.0f)
        assertThat(noConsistency.consistency).isEqualTo(0.0f)
    }
    
    @Test
    fun `PerformanceStats formats weight and volume strings`() {
        // Act
        val stats = PerformanceStats(
            exerciseName = "Deadlift",
            bestSingle = "200kg x 1",
            bestVolume = "5000kg",
            averageRpe = 8.5f,
            consistency = 0.9f,
            totalSessions = 30
        )
        
        // Assert
        assertThat(stats.bestSingle).contains("kg")
        assertThat(stats.bestSingle).contains("x")
        assertThat(stats.bestVolume).contains("kg")
    }
    
    @Test
    fun `PerformanceStats average RPE calculation`() {
        // Act
        val stats = PerformanceStats(
            exerciseName = "Bench Press",
            bestSingle = "100kg x 1",
            bestVolume = "1500kg",
            averageRpe = 7.25f, // Average of multiple sessions
            consistency = 0.75f,
            totalSessions = 20
        )
        
        // Assert
        assertThat(stats.averageRpe).isEqualTo(7.25f)
    }
    
    // ========== Helper Functions ==========
    
    private fun createSummary(exerciseName: String): ExerciseSummary {
        return ExerciseSummary(
            exerciseName = exerciseName,
            currentMax = 100f,
            progressPercentage = 5f,
            lastWorkout = LocalDateTime.now(),
            miniChartData = listOf(95f, 97.5f, 100f),
            sessionCount = 10
        )
    }
}
