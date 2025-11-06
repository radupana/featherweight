package com.github.radupana.featherweight.ui.components

import com.github.radupana.featherweight.data.TrainingAnalysis

sealed class TrainingAnalysisUIState {
    object Loading : TrainingAnalysisUIState()

    data class InsufficientData(
        val current: Int,
        val required: Int,
    ) : TrainingAnalysisUIState()

    data class ReadyForAnalysis(
        val assessment: String?,
        val showAnalyzeButton: Boolean,
    ) : TrainingAnalysisUIState()
}

fun calculateTrainingAnalysisUIState(
    analysis: TrainingAnalysis?,
    currentWorkoutCount: Int,
    isLoading: Boolean,
    minimumWorkouts: Int = 1,
): TrainingAnalysisUIState {
    if (isLoading) return TrainingAnalysisUIState.Loading

    if (currentWorkoutCount < minimumWorkouts) {
        return TrainingAnalysisUIState.InsufficientData(currentWorkoutCount, minimumWorkouts)
    }

    val hasRealAnalysis = analysis != null && !analysis.overallAssessment.startsWith("INSUFFICIENT_DATA:")
    val assessment = if (hasRealAnalysis) analysis?.overallAssessment else null

    return TrainingAnalysisUIState.ReadyForAnalysis(
        assessment = assessment,
        showAnalyzeButton = true,
    )
}
