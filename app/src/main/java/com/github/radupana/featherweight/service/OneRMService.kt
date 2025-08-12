package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import kotlin.math.roundToInt

class OneRMService {
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        private const val MIN_RPE_FOR_ESTIMATE = 6.0f
        private const val MAX_REPS_FOR_ESTIMATE = 15
        private const val MIN_LOAD_PERCENTAGE = 0.6f
    }

    /**
     * Calculate estimated 1RM using Brzycki formula
     * Only valid for reps <= 15
     * For singles with RPE, adjusts based on reps in reserve
     */
    fun calculateEstimated1RM(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
    ): Float? {
        if (reps <= 0 || reps > MAX_REPS_FOR_ESTIMATE) return null

        // Calculate effective reps based on RPE for singles
        val effectiveReps =
            when {
                reps == 1 && rpe != null -> {
                    // For singles with RPE, calculate total possible reps
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f).toInt()
                    reps + repsInReserve // Total reps possible at this weight
                }
                else -> reps
            }

        // If it's a true max (RPE 10 or no RPE adjustment needed)
        if (effectiveReps == 1) return weight

        // Brzycki formula: 1RM = weight / (1.0278 - 0.0278 × reps)
        return weight / (1.0278f - 0.0278f * effectiveReps)
    }

    /**
     * Calculate confidence score for a 1RM estimate
     * Based on reps, RPE, and percentage of current 1RM
     */
    fun calculateConfidence(
        reps: Int,
        rpe: Float?,
        percentOf1RM: Float,
    ): Float {
        // Rep score: lower reps = higher confidence
        val repScore = (16f - reps) / 15f // 1 rep = 1.0, 15 reps = 0.067

        // RPE score: higher RPE = higher confidence
        val rpeScore =
            if (rpe != null && rpe >= MIN_RPE_FOR_ESTIMATE) {
                (rpe - 5f) / 5f // RPE 10 = 1.0, RPE 5 = 0.0
            } else {
                0.3f // Default if no RPE provided
            }

        // Load score: higher percentage = higher confidence
        val loadScore = percentOf1RM.coerceIn(0f, 1f)

        // Weighted average: reps matter most, then RPE, then load
        return (repScore * 0.5f) + (rpeScore * 0.3f) + (loadScore * 0.2f)
    }

    /**
     * Determine if we should update the 1RM estimate based on a completed set
     */
    fun shouldUpdateOneRM(
        set: SetLog,
        currentEstimate: Float?,
        newEstimate: Float,
    ): Boolean {
        // Basic validation
        if (set.actualReps <= 0 || set.actualReps > MAX_REPS_FOR_ESTIMATE) return false
        if (set.actualWeight <= 0) return false
        if (set.actualRpe != null && set.actualRpe < MIN_RPE_FOR_ESTIMATE) return false

        // Never decrease 1RM
        if (currentEstimate != null && newEstimate <= currentEstimate) return false

        // Check minimum load percentage if we have a current estimate
        if (currentEstimate != null && currentEstimate > 0) {
            val loadPercentage = set.actualWeight / currentEstimate
            if (loadPercentage < MIN_LOAD_PERCENTAGE) return false
        }

        // Calculate confidence
        val percentOf1RM =
            if (currentEstimate != null && currentEstimate > 0) {
                set.actualWeight / currentEstimate
            } else {
                1f // Assume 100% if no current estimate
            }

        val confidence = calculateConfidence(set.actualReps, set.actualRpe, percentOf1RM)
        return confidence >= MIN_CONFIDENCE_THRESHOLD
    }

    /**
     * Create a UserExerciseMax record from a completed set
     */
    fun createOneRMRecord(
        userId: Long,
        exerciseId: Long,
        set: SetLog,
        estimate: Float,
        confidence: Float,
        mostWeightData: MostWeightData? = null,
    ): UserExerciseMax {
        val context = buildContext(set.actualWeight, set.actualReps, set.actualRpe)

        // Use provided most weight data or use this set's data
        val mostWeight =
            mostWeightData ?: MostWeightData(
                weight = set.actualWeight,
                reps = set.actualReps,
                rpe = set.actualRpe,
                date = LocalDateTime.now(),
            )

        return UserExerciseMax(
            userId = userId,
            exerciseVariationId = exerciseId,
            mostWeightLifted = mostWeight.weight,
            mostWeightReps = mostWeight.reps,
            mostWeightRpe = mostWeight.rpe,
            mostWeightDate = mostWeight.date,
            oneRMEstimate = estimate,
            oneRMContext = context,
            oneRMConfidence = confidence,
            oneRMDate = LocalDateTime.now(),
            oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
        )
    }

    /**
     * Build a human-readable context string for a lift
     */
    fun buildContext(
        weight: Float,
        reps: Int,
        rpe: Float?,
    ): String {
        val weightStr = WeightFormatter.formatWeight(weight)
        val rpeStr = rpe?.let { " @ RPE ${it.roundToInt()}" } ?: ""
        return "${weightStr}kg × $reps$rpeStr"
    }

    data class MostWeightData(
        val weight: Float,
        val reps: Int,
        val rpe: Float?,
        val date: LocalDateTime,
    )
}
