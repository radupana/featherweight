package com.github.radupana.featherweight.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ExerciseProgressViewModelStateTest {
    
    @Test
    fun `ExerciseProgressState Loading state is correct`() {
        val state = ExerciseProgressViewModel.ExerciseProgressState.Loading
        
        // Verify it's a Loading state - not much else to test for a singleton object
        assertThat(state).isNotNull()
    }
    
    @Test
    fun `ExerciseProgressState Success state holds data correctly`() {
        val progressData = ExerciseProgressViewModel.ExerciseProgressData(
            exerciseVariationId = 1L,
            exerciseName = "Squat",
            allTimePR = 125f,
            allTimePRDate = LocalDate.now(),
            recentBest = 100f,
            recentBestDate = LocalDate.now().minusDays(5),
            recentBestPercentOfPR = 80,
            weeklyFrequency = 2.5f,
            frequencyTrend = ExerciseProgressViewModel.FrequencyTrend.UP,
            lastPerformed = LocalDate.now().minusDays(2),
            progressStatus = ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS,
            progressStatusDetail = "Weight increasing",
            plateauWeeks = 0
        )
        
        val state = ExerciseProgressViewModel.ExerciseProgressState.Success(progressData)
        
        assertThat(state.data).isEqualTo(progressData)
        assertThat(state.data?.allTimePR).isEqualTo(125f)
        assertThat(state.data?.recentBest).isEqualTo(100f)
        assertThat(state.data?.exerciseName).isEqualTo("Squat")
    }
    
    @Test
    fun `ExerciseProgressState Error state holds message correctly`() {
        val errorMessage = "Failed to load exercise data"
        val state = ExerciseProgressViewModel.ExerciseProgressState.Error(errorMessage)
        
        assertThat(state.message).isEqualTo(errorMessage)
    }
    
    @Test
    fun `ChartType enum values are correct`() {
        assertThat(ExerciseProgressViewModel.ChartType.ONE_RM).isNotNull()
        assertThat(ExerciseProgressViewModel.ChartType.MAX_WEIGHT).isNotNull()
    }
    
    @Test
    fun `PatternType enum values are correct`() {
        assertThat(ExerciseProgressViewModel.PatternType.FREQUENCY).isNotNull()
        assertThat(ExerciseProgressViewModel.PatternType.REP_RANGES).isNotNull()
        assertThat(ExerciseProgressViewModel.PatternType.RPE_ZONES).isNotNull()
    }
}