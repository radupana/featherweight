package com.github.radupana.featherweight.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ExerciseProgressViewModelStateTest {
    
    @Test
    fun `ExerciseProgressData creation with all fields`() {
        // Arrange & Act
        val data = ExerciseProgressViewModel.ExerciseProgressData(
            exerciseVariationId = 1L,
            exerciseName = "Barbell Bench Press",
            allTimePR = 120f,
            allTimePRDate = LocalDate.of(2024, 1, 15),
            recentBest = 115f,
            recentBestDate = LocalDate.of(2024, 2, 1),
            recentBestPercentOfPR = 96,
            weeklyFrequency = 2.5f,
            frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.UP,
            lastPerformed = LocalDate.of(2024, 2, 5),
            progressStatus = ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS,
            progressStatusDetail = "5kg increase in 2 weeks",
            plateauWeeks = 0
        )
        
        // Assert
        assertThat(data.exerciseVariationId).isEqualTo(1L)
        assertThat(data.exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(data.allTimePR).isEqualTo(120f)
        assertThat(data.recentBest).isEqualTo(115f)
        assertThat(data.recentBestPercentOfPR).isEqualTo(96)
        assertThat(data.weeklyFrequency).isEqualTo(2.5f)
        assertThat(data.frequencyTrend).isEqualTo(ExerciseProgressViewModel.FrequencyTrend.UP)
        assertThat(data.progressStatus).isEqualTo(ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS)
        assertThat(data.plateauWeeks).isEqualTo(0)
    }
    
    @Test
    fun `ExerciseProgressData with null dates`() {
        // Arrange & Act
        val data = ExerciseProgressViewModel.ExerciseProgressData(
            exerciseVariationId = 2L,
            exerciseName = "Pull Up",
            allTimePR = 20f,
            allTimePRDate = null,
            recentBest = 15f,
            recentBestDate = null,
            recentBestPercentOfPR = 75,
            weeklyFrequency = 1.0f,
            frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.STABLE,
            lastPerformed = null,
            progressStatus = ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS,
            progressStatusDetail = "Consistent performance"
        )
        
        // Assert
        assertThat(data.allTimePRDate).isNull()
        assertThat(data.recentBestDate).isNull()
        assertThat(data.lastPerformed).isNull()
    }
    
    @Test
    fun `ExerciseProgressState Loading state`() {
        // Arrange & Act
        val state = ExerciseProgressViewModel.ExerciseProgressState.Loading
        
        // Assert
        assertThat(state).isInstanceOf(ExerciseProgressViewModel.ExerciseProgressState.Loading::class.java)
    }
    
    @Test
    fun `ExerciseProgressState Success state with data`() {
        // Arrange
        val data = ExerciseProgressViewModel.ExerciseProgressData(
            exerciseVariationId = 1L,
            exerciseName = "Squat",
            allTimePR = 150f,
            allTimePRDate = LocalDate.now(),
            recentBest = 145f,
            recentBestPercentOfPR = 97,
            weeklyFrequency = 3f,
            frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.UP,
            lastPerformed = LocalDate.now().minusDays(2),
            progressStatus = ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS,
            progressStatusDetail = "Great progress!"
        )
        
        // Act
        val state = ExerciseProgressViewModel.ExerciseProgressState.Success(data)
        
        // Assert
        assertThat(state).isInstanceOf(ExerciseProgressViewModel.ExerciseProgressState.Success::class.java)
        assertThat(state.data).isEqualTo(data)
    }
    
    @Test
    fun `ExerciseProgressState Success state with null data`() {
        // Arrange & Act
        val state = ExerciseProgressViewModel.ExerciseProgressState.Success(null)
        
        // Assert
        assertThat(state.data).isNull()
    }
    
    @Test
    fun `ExerciseProgressState Error state`() {
        // Arrange & Act
        val state = ExerciseProgressViewModel.ExerciseProgressState.Error("Failed to load data")
        
        // Assert
        assertThat(state).isInstanceOf(ExerciseProgressViewModel.ExerciseProgressState.Error::class.java)
        assertThat(state.message).isEqualTo("Failed to load data")
    }
    
    @Test
    fun `FrequencyTrend enum values`() {
        // Assert
        assertThat(ExerciseProgressViewModel.FrequencyTrend.values()).hasLength(3)
        assertThat(ExerciseProgressViewModel.FrequencyTrend.UP).isNotNull()
        assertThat(ExerciseProgressViewModel.FrequencyTrend.DOWN).isNotNull()
        assertThat(ExerciseProgressViewModel.FrequencyTrend.STABLE).isNotNull()
    }
    
    @Test
    fun `ProgressStatus enum values`() {
        // Assert
        val statuses = ExerciseProgressViewModel.ProgressStatus.values()
        assertThat(statuses).hasLength(5)
        assertThat(statuses.toList()).contains(ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS)
        assertThat(statuses.toList()).contains(ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS)
        assertThat(statuses.toList()).contains(ExerciseProgressViewModel.ProgressStatus.PLATEAU)
        assertThat(statuses.toList()).contains(ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK)
        assertThat(statuses.toList()).contains(ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER)
    }
    
    @Test
    fun `ChartType enum values`() {
        // Assert
        val types = ExerciseProgressViewModel.ChartType.values()
        assertThat(types).hasLength(2)
        assertThat(types.toList()).contains(ExerciseProgressViewModel.ChartType.ONE_RM)
        assertThat(types.toList()).contains(ExerciseProgressViewModel.ChartType.MAX_WEIGHT)
    }
    
    @Test
    fun `PatternType enum values`() {
        // Assert
        val types = ExerciseProgressViewModel.PatternType.values()
        assertThat(types).hasLength(3)
        assertThat(types.toList()).contains(ExerciseProgressViewModel.PatternType.FREQUENCY)
        assertThat(types.toList()).contains(ExerciseProgressViewModel.PatternType.REP_RANGES)
        assertThat(types.toList()).contains(ExerciseProgressViewModel.PatternType.RPE_ZONES)
    }
    
    @Test
    fun `ExerciseProgressData copy with modifications`() {
        // Arrange
        val original = ExerciseProgressViewModel.ExerciseProgressData(
            exerciseVariationId = 1L,
            exerciseName = "Deadlift",
            allTimePR = 200f,
            allTimePRDate = LocalDate.of(2024, 1, 1),
            recentBest = 190f,
            recentBestPercentOfPR = 95,
            weeklyFrequency = 2f,
            frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.STABLE,
            lastPerformed = LocalDate.of(2024, 2, 1),
            progressStatus = ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS,
            progressStatusDetail = "Consistent",
            plateauWeeks = 2
        )
        
        // Act
        val modified = original.copy(
            recentBest = 195f,
            recentBestPercentOfPR = 98,
            progressStatus = ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS,
            plateauWeeks = 0
        )
        
        // Assert
        assertThat(modified.recentBest).isEqualTo(195f)
        assertThat(modified.recentBestPercentOfPR).isEqualTo(98)
        assertThat(modified.progressStatus).isEqualTo(ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS)
        assertThat(modified.plateauWeeks).isEqualTo(0)
        assertThat(modified.exerciseName).isEqualTo("Deadlift") // Unchanged
        assertThat(modified.allTimePR).isEqualTo(200f) // Unchanged
    }
}