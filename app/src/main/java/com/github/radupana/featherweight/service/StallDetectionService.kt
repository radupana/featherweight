package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.GlobalExerciseProgress

class StallDetectionService {
    data class StallAnalysis(
        val isStalling: Boolean,
        val weeksAtCurrentWeight: Int,
        val consecutiveFailures: Int,
        val recommendation: StallRecommendation,
        val confidence: Float,
        val context: String,
    )

    enum class StallRecommendation {
        CONTINUE, // Keep pushing, progress is fine
        DELOAD, // Reduce weight by 10-15%
        CHANGE_REP_RANGE, // Switch from 5x5 to 3x8 or similar
        ADD_VARIATION, // Suggest exercise variations
        INCREASE_FREQUENCY, // Add more volume
        DECREASE_FREQUENCY, // Reduce volume for recovery
        TECHNIQUE_CHECK, // Focus on form improvement
    }

    suspend fun analyzeStall(
        globalProgress: GlobalExerciseProgress,
    ): StallAnalysis {
        // Basic stall detection
        val isStalling =
            globalProgress.consecutiveStalls >= 3 ||
                globalProgress.weeksAtCurrentWeight >= 3 ||
                globalProgress.failureStreak >= 2

        // Get recent workout data for context
        val recentWorkouts = getRecentWorkoutData()
        val avgRpe = globalProgress.recentAvgRpe ?: 8f

        // Determine recommendation based on context
        val recommendation =
            when {
                // Deload if high fatigue
                avgRpe >= 9f && globalProgress.consecutiveStalls >= 3 -> StallRecommendation.DELOAD

                // Change rep range if stuck for too long
                globalProgress.weeksAtCurrentWeight >= 4 -> StallRecommendation.CHANGE_REP_RANGE

                // Add variation if moderate stall
                globalProgress.consecutiveStalls >= 2 && avgRpe < 9f -> StallRecommendation.ADD_VARIATION

                // Check frequency
                recentWorkouts.size < 4 -> StallRecommendation.INCREASE_FREQUENCY
                recentWorkouts.size > 12 -> StallRecommendation.DECREASE_FREQUENCY

                // Technique check for early stalls
                globalProgress.failureStreak >= 2 && globalProgress.consecutiveStalls < 3 -> StallRecommendation.TECHNIQUE_CHECK

                // Default to continue
                else -> StallRecommendation.CONTINUE
            }

        // Calculate confidence based on data availability
        val confidence =
            when {
                recentWorkouts.size >= 8 -> 0.9f
                recentWorkouts.size >= 4 -> 0.7f
                recentWorkouts.size >= 2 -> 0.5f
                else -> 0.3f
            }

        // Generate context message
        val context = generateContext(globalProgress, recommendation, avgRpe)

        return StallAnalysis(
            isStalling = isStalling,
            weeksAtCurrentWeight = globalProgress.weeksAtCurrentWeight,
            consecutiveFailures = globalProgress.failureStreak,
            recommendation = recommendation,
            confidence = confidence,
            context = context,
        )
    }

    private suspend fun getRecentWorkoutData(): List<Any> {
        // This would query actual workout data
        // For now, returning empty list
        return emptyList()
    }

    private fun generateContext(
        progress: GlobalExerciseProgress,
        recommendation: StallRecommendation,
        avgRpe: Float,
    ): String =
        when (recommendation) {
            StallRecommendation.DELOAD ->
                "You've been pushing hard (RPE ${avgRpe.toInt()}/10) and hit a plateau. A strategic deload will help you recover and break through."

            StallRecommendation.CHANGE_REP_RANGE ->
                "You've been at ${progress.currentWorkingWeight}kg for ${progress.weeksAtCurrentWeight} weeks. Time to switch up your rep ranges for new stimulus."

            StallRecommendation.ADD_VARIATION ->
                "Your main lift has stalled. Adding variations like pause reps or tempo work can help break the plateau."

            StallRecommendation.INCREASE_FREQUENCY ->
                "You're training this movement infrequently. Adding another session per week could accelerate progress."

            StallRecommendation.DECREASE_FREQUENCY ->
                "High training frequency may be limiting recovery. Consider reducing to 2x per week for this exercise."

            StallRecommendation.TECHNIQUE_CHECK ->
                "Recent failures suggest form breakdown. Film your sets and focus on perfect technique with current weight."

            StallRecommendation.CONTINUE ->
                "Progress looks normal. Keep pushing with current programming."
        }

    fun getActionableSteps(recommendation: StallRecommendation): List<String> =
        when (recommendation) {
            StallRecommendation.DELOAD ->
                listOf(
                    "Reduce weight by 15% for next workout",
                    "Focus on explosive reps with perfect form",
                    "Return to previous weight after 1-2 sessions",
                    "Consider adding an extra warm-up set",
                )

            StallRecommendation.CHANGE_REP_RANGE ->
                listOf(
                    "Switch from 5x5 to 4x8-10 for 3 weeks",
                    "Or try 6x3 with 90% intensity",
                    "Add 1-2 back-off sets at lighter weight",
                    "Track total volume to ensure progression",
                )

            StallRecommendation.ADD_VARIATION ->
                listOf(
                    "Add pause reps (2-second pause)",
                    "Try tempo work (3-1-1 tempo)",
                    "Include partial reps after main sets",
                    "Consider close-grip or wide-grip variations",
                )

            StallRecommendation.INCREASE_FREQUENCY ->
                listOf(
                    "Add a light technique day mid-week",
                    "Program 70% weight for extra session",
                    "Focus on speed and form on light days",
                    "Monitor recovery between sessions",
                )

            StallRecommendation.DECREASE_FREQUENCY ->
                listOf(
                    "Reduce to 2x per week maximum",
                    "Ensure 72 hours between sessions",
                    "Add mobility work on off days",
                    "Consider a full deload week",
                )

            StallRecommendation.TECHNIQUE_CHECK ->
                listOf(
                    "Film all working sets from side angle",
                    "Reduce weight by 10% temporarily",
                    "Focus on controlled eccentrics",
                    "Consider hiring a coach for form review",
                )

            StallRecommendation.CONTINUE ->
                listOf(
                    "Maintain current programming",
                    "Ensure adequate protein intake",
                    "Focus on sleep quality",
                    "Stay patient with the process",
                )
        }
}
