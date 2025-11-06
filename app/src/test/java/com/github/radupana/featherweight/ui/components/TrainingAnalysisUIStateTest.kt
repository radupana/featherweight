package com.github.radupana.featherweight.ui.components

import com.github.radupana.featherweight.data.TrainingAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TrainingAnalysisUIStateTest {
    @Test
    fun `returns Loading when isLoading is true`() {
        val result =
            calculateTrainingAnalysisUIState(
                analysis = null,
                currentWorkoutCount = 15,
                isLoading = true,
            )

        assertTrue(result is TrainingAnalysisUIState.Loading)
    }

    @Test
    fun `returns InsufficientData when workouts less than minimum`() {
        val result =
            calculateTrainingAnalysisUIState(
                analysis = null,
                currentWorkoutCount = 0,
                isLoading = false,
                minimumWorkouts = 1,
            )

        assertTrue(result is TrainingAnalysisUIState.InsufficientData)
        val insufficientData = result as TrainingAnalysisUIState.InsufficientData
        assertEquals(0, insufficientData.current)
        assertEquals(1, insufficientData.required)
    }

    @Test
    fun `returns ReadyForAnalysis with null assessment when no analysis and sufficient workouts`() {
        val result =
            calculateTrainingAnalysisUIState(
                analysis = null,
                currentWorkoutCount = 15,
                isLoading = false,
            )

        assertTrue(result is TrainingAnalysisUIState.ReadyForAnalysis)
        val ready = result as TrainingAnalysisUIState.ReadyForAnalysis
        assertNull(ready.assessment)
        assertTrue(ready.showAnalyzeButton)
    }

    @Test
    fun `returns ReadyForAnalysis with null assessment when cached INSUFFICIENT_DATA but now sufficient workouts`() {
        val insufficientAnalysis =
            TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = LocalDate.now().minusWeeks(12),
                periodEnd = LocalDate.now(),
                overallAssessment = "INSUFFICIENT_DATA:0:1",
                keyInsightsJson = "[]",
                recommendationsJson = "[]",
                warningsJson = "[]",
            )

        val result =
            calculateTrainingAnalysisUIState(
                analysis = insufficientAnalysis,
                currentWorkoutCount = 1,
                isLoading = false,
            )

        assertTrue(result is TrainingAnalysisUIState.ReadyForAnalysis)
        val ready = result as TrainingAnalysisUIState.ReadyForAnalysis
        assertNull(ready.assessment)
        assertTrue(ready.showAnalyzeButton)
    }

    @Test
    fun `returns ReadyForAnalysis with assessment when real analysis exists`() {
        val realAnalysis =
            TrainingAnalysis(
                analysisDate = LocalDateTime.now(),
                periodStart = LocalDate.now().minusWeeks(12),
                periodEnd = LocalDate.now(),
                overallAssessment = "Your training is progressing well.",
                keyInsightsJson = "[]",
                recommendationsJson = "[]",
                warningsJson = "[]",
            )

        val result =
            calculateTrainingAnalysisUIState(
                analysis = realAnalysis,
                currentWorkoutCount = 52,
                isLoading = false,
            )

        assertTrue(result is TrainingAnalysisUIState.ReadyForAnalysis)
        val ready = result as TrainingAnalysisUIState.ReadyForAnalysis
        assertEquals("Your training is progressing well.", ready.assessment)
        assertTrue(ready.showAnalyzeButton)
    }

    @Test
    fun `returns InsufficientData even when cached analysis exists but workouts still insufficient`() {
        val oldAnalysis =
            TrainingAnalysis(
                analysisDate = LocalDateTime.now().minusDays(30),
                periodStart = LocalDate.now().minusWeeks(12),
                periodEnd = LocalDate.now(),
                overallAssessment = "Old analysis",
                keyInsightsJson = "[]",
                recommendationsJson = "[]",
                warningsJson = "[]",
            )

        val result =
            calculateTrainingAnalysisUIState(
                analysis = oldAnalysis,
                currentWorkoutCount = 0,
                isLoading = false,
            )

        assertTrue(result is TrainingAnalysisUIState.InsufficientData)
        val insufficientData = result as TrainingAnalysisUIState.InsufficientData
        assertEquals(0, insufficientData.current)
        assertEquals(1, insufficientData.required)
    }

    @Test
    fun `uses custom minimum workouts parameter`() {
        val result =
            calculateTrainingAnalysisUIState(
                analysis = null,
                currentWorkoutCount = 5,
                isLoading = false,
                minimumWorkouts = 3,
            )

        assertTrue(result is TrainingAnalysisUIState.ReadyForAnalysis)
    }
}
