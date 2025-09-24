package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import kotlin.math.pow

class OneRMService {
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        private const val MIN_RPE_FOR_ESTIMATE = 6.0f
        private const val MAX_REPS_FOR_ESTIMATE = 15
        private const val MIN_LOAD_PERCENTAGE = 0.6f
    }

    /**
     * Calculate estimated 1RM using appropriate formula based on exercise type
     * Only valid for reps <= 15
     * Properly converts RPE to total rep capacity
     * Skips calculation for RPE <= 6 (too unreliable)
     */
    fun calculateEstimated1RM(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        scalingType: RMScalingType = RMScalingType.STANDARD,
    ): Float? {
        Log.d("OneRMService", "calculateEstimated1RM: weight=$weight, reps=$reps, rpe=$rpe, scalingType=$scalingType")

        if (reps <= 0 || reps > MAX_REPS_FOR_ESTIMATE) {
            Log.d("OneRMService", "Skipping 1RM calc: reps out of range ($reps)")
            return null
        }

        if (rpe != null && rpe <= 6.0f) {
            Log.d("OneRMService", "Skipping 1RM calc: RPE too low ($rpe <= 6.0) - unreliable estimate")
            return null
        }

        val totalRepCapacity =
            when {
                rpe != null && rpe > 6.0f -> {
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f)
                    val total = reps + repsInReserve
                    Log.d("OneRMService", "RPE conversion: $reps reps @ RPE $rpe = $total total rep capacity ($repsInReserve RIR)")
                    total
                }
                rpe == null -> {
                    Log.d("OneRMService", "No RPE provided, assuming maximal effort: $reps reps")
                    reps.toFloat()
                }
                else -> {
                    // This shouldn't happen due to the check above, but just in case
                    Log.d("OneRMService", "Unexpected RPE state: $rpe")
                    reps.toFloat()
                }
            }

        if (totalRepCapacity == 1f) {
            Log.d("OneRMService", "True 1RM detected: $weight kg")
            return weight
        }

        val result =
            when (scalingType) {
                RMScalingType.WEIGHTED_BODYWEIGHT -> {
                    val calc = weight * (1 + totalRepCapacity * 0.035f)
                    Log.d("OneRMService", "Weighted BW formula: $weight × (1 + $totalRepCapacity × 0.035) = $calc")
                    calc
                }
                RMScalingType.ISOLATION -> {
                    val calc = weight * totalRepCapacity.pow(0.10f)
                    Log.d("OneRMService", "Isolation formula: $weight × $totalRepCapacity^0.10 = $calc")
                    calc
                }
                RMScalingType.STANDARD -> {
                    val calc = weight / (1.0278f - 0.0278f * totalRepCapacity)
                    Log.d("OneRMService", "Standard Brzycki: $weight / (1.0278 - 0.0278 × $totalRepCapacity) = $calc")
                    calc
                }
            }

        Log.d("OneRMService", "Final 1RM estimate: ${WeightFormatter.formatDecimal(result, 2)}kg")
        return result
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
        if (reps <= 0) return 0f

        // Rep score: lower reps = higher confidence, capped at 15 reps
        val cappedReps = reps.coerceAtMost(15)
        val repScore = (16f - cappedReps) / 15f

        // RPE score: higher RPE = higher confidence
        val rpeScore =
            if (rpe != null && rpe >= MIN_RPE_FOR_ESTIMATE) {
                (rpe - 5f) / 5f
            } else {
                0.3f
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
        val isBasicValidationPassed = isSetValidForOneRM(set)
        val isEstimateImproving = isNewEstimateBetter(currentEstimate, newEstimate)
        val isLoadSufficient = isLoadPercentageSufficient(set, currentEstimate)
        val isConfidenceHigh = isConfidenceAboveThreshold(set, currentEstimate)

        return isBasicValidationPassed && isEstimateImproving && isLoadSufficient && isConfidenceHigh
    }

    private fun isSetValidForOneRM(set: SetLog): Boolean =
        set.actualReps > 0 &&
            set.actualReps <= MAX_REPS_FOR_ESTIMATE &&
            set.actualWeight > 0 &&
            (set.actualRpe == null || set.actualRpe >= MIN_RPE_FOR_ESTIMATE)

    private fun isNewEstimateBetter(
        currentEstimate: Float?,
        newEstimate: Float,
    ): Boolean = currentEstimate == null || newEstimate > currentEstimate

    private fun isLoadPercentageSufficient(
        set: SetLog,
        currentEstimate: Float?,
    ): Boolean {
        if (currentEstimate == null || currentEstimate <= 0) return true

        val loadPercentage = set.actualWeight / currentEstimate
        return loadPercentage >= MIN_LOAD_PERCENTAGE
    }

    private fun isConfidenceAboveThreshold(
        set: SetLog,
        currentEstimate: Float?,
    ): Boolean {
        val percentOf1RM =
            if (currentEstimate != null && currentEstimate > 0) {
                set.actualWeight / currentEstimate
            } else {
                1f
            }

        val confidence = calculateConfidence(set.actualReps, set.actualRpe, percentOf1RM)
        return confidence >= MIN_CONFIDENCE_THRESHOLD
    }

    /**
     * Create a UserExerciseMax record from a completed set
     */
    fun createOneRMRecord(
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
            userId = set.userId,
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
            // Note: isCustomExercise should be set by the caller since we can't check async here
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
        val weightWithUnit = WeightFormatter.formatWeightWithUnit(weight)
        val rpeStr = rpe?.let { " @ RPE ${WeightFormatter.formatRPE(it)}" } ?: ""
        return "$weightWithUnit × $reps$rpeStr"
    }

    data class MostWeightData(
        val weight: Float,
        val reps: Int,
        val rpe: Float?,
        val date: LocalDateTime,
    )
}
