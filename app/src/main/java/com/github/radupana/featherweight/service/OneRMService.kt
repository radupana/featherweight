package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.math.roundToInt

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

        // Skip calculation if RPE is 6 or below - too unreliable
        if (rpe != null && rpe <= 6.0f) {
            Log.d("OneRMService", "Skipping 1RM calc: RPE too low ($rpe <= 6.0) - unreliable estimate")
            return null
        }

        // Calculate total rep capacity based on RPE
        val totalRepCapacity =
            when {
                rpe != null && rpe > 6.0f -> {
                    // Convert RPE to reps in reserve for ANY rep count
                    val repsInReserve = (10f - rpe).coerceAtLeast(0f).toInt()
                    val total = reps + repsInReserve
                    Log.d("OneRMService", "RPE conversion: $reps reps @ RPE $rpe = $total total rep capacity ($repsInReserve RIR)")
                    total
                }
                rpe == null -> {
                    Log.d("OneRMService", "No RPE provided, assuming maximal effort: $reps reps")
                    reps // No RPE means assume maximal effort
                }
                else -> {
                    // This shouldn't happen due to the check above, but just in case
                    Log.d("OneRMService", "Unexpected RPE state: $rpe")
                    reps
                }
            }

        // If it's a true max (1 rep capacity)
        if (totalRepCapacity == 1) {
            Log.d("OneRMService", "True 1RM detected: $weight kg")
            return weight
        }

        // Apply formula based on scaling type
        val result =
            when (scalingType) {
                RMScalingType.WEIGHTED_BODYWEIGHT -> {
                    // More aggressive scaling for weighted bodyweight movements
                    // Use modified Epley formula with adjusted coefficient
                    val calc = weight * (1 + totalRepCapacity * 0.035f)
                    Log.d("OneRMService", "Weighted BW formula: $weight × (1 + $totalRepCapacity × 0.035) = $calc")
                    calc
                }
                RMScalingType.ISOLATION -> {
                    // More conservative scaling for isolation exercises
                    // Use Lombardi formula which is more conservative at higher reps
                    val calc = weight * totalRepCapacity.toFloat().pow(0.10f)
                    Log.d("OneRMService", "Isolation formula: $weight × $totalRepCapacity^0.10 = $calc")
                    calc
                }
                RMScalingType.STANDARD -> {
                    // Standard Brzycki formula for compound movements
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
