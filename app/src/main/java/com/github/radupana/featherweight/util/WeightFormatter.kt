package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.manager.WeightUnitManager
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.round

object WeightFormatter {
    private var weightUnitManager: WeightUnitManager? = null

    fun initialize(manager: WeightUnitManager) {
        weightUnitManager = manager
    }

    /**
     * Formats weight to nearest quarter (0.00, 0.25, 0.50, 0.75)
     * Shows integers without decimal (82.0 -> "82")
     * Shows meaningful decimals (82.5 -> "82.5")
     */
    fun formatWeight(weight: Float): String {
        weightUnitManager?.let { manager ->
            return manager.formatWeight(weight)
        }

        val roundedWeight = roundToNearestQuarter(weight)

        return if (roundedWeight % 1.0f == 0.0f) {
            roundedWeight.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", roundedWeight).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Formats weight with kg unit
     */
    fun formatWeightWithUnit(weight: Float): String {
        weightUnitManager?.let { manager ->
            return manager.formatWeightWithUnit(weight)
        }
        return "${formatWeight(weight)}kg"
    }

    /**
     * Rounds a weight to the nearest quarter (0.25 increments)
     */
    fun roundToNearestQuarter(weight: Float): Float = round(weight * 4) / 4

    /**
     * Rounds UP to the nearest quarter (for progression suggestions)
     */
    fun roundUpToNearestQuarter(weight: Float): Float = ceil(weight * 4) / 4

    /**
     * Formats any decimal to at most 2 decimal places
     */
    fun formatDecimal(
        value: Float,
        decimals: Int = 2,
    ): String = String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')

    /**
     * Formats percentage values (0-1 scale to 0-100%)
     */
    fun formatPercentage(value: Float): String {
        val percentage = value * 100
        return if (percentage % 1.0f == 0.0f) {
            "${percentage.toInt()}%"
        } else {
            "${formatDecimal(percentage, 1)}%"
        }
    }

    /**
     * Gets the weight label for headers (e.g., "Weight (kg)" or "Weight (lbs)")
     */
    fun getWeightLabel(): String {
        weightUnitManager?.let { manager ->
            val unit = manager.getCurrentUnit().suffix
            return "Weight ($unit)"
        }
        return "Weight (kg)"
    }

    /**
     * Formats a last set display (e.g., "Last: 3x110kg")
     */
    fun formatLastSet(
        reps: Int,
        weightKg: Float,
    ): String {
        val weight = formatWeight(weightKg)
        val unit = weightUnitManager?.getCurrentUnit()?.suffix ?: "kg"
        return "Last: ${reps}x$weight$unit"
    }

    /**
     * Formats set info display (e.g., "3 sets × 5 reps × 100kg")
     */
    fun formatSetInfo(
        sets: Int,
        reps: Int,
        weightKg: Float,
    ): String {
        val weight = formatWeight(weightKg)
        val unit = weightUnitManager?.getCurrentUnit()?.suffix ?: "kg"
        return "$sets sets × $reps reps × $weight$unit"
    }

    /**
     * Formats large volumes (e.g., 1234kg -> 1.2k kg)
     */
    fun formatVolume(volume: Float): String {
        weightUnitManager?.let { manager ->
            return manager.formatVolume(volume)
        }
        return when {
            volume >= 10000 -> "${formatDecimal(volume / 1000, 1)}k kg"
            volume >= 1000 -> "${formatDecimal(volume / 1000, 2)}k kg"
            else -> formatWeightWithUnit(volume)
        }
    }

    fun formatRPE(rpe: Float?): String {
        if (rpe == null) return ""
        return if (rpe % 1 == 0f) {
            rpe.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rpe)
        }
    }

    /**
     * Rounds RPE to nearest 0.5 (0.0, 0.5, 1.0, 1.5, etc.)
     * and clamps between 0 and 10
     */
    fun roundRPE(rpe: Float?): Float? {
        if (rpe == null) return null
        return (round(rpe * 2) / 2).coerceIn(0f, 10f)
    }

    /**
     * Validates that a weight value is properly rounded to 0.25 increments
     */
    fun isValidWeight(weight: Float): Boolean {
        val rounded = roundToNearestQuarter(weight)
        return kotlin.math.abs(weight - rounded) < 0.001f
    }

    /**
     * Validates that an RPE value is properly rounded to 0.5 increments
     */
    fun isValidRPE(rpe: Float?): Boolean {
        if (rpe == null) return true
        val rounded = roundRPE(rpe)
        return kotlin.math.abs(rpe - (rounded ?: 0f)) < 0.001f
    }

    /**
     * Parses user input and converts to kg based on current unit setting
     * In LBS mode: "225" -> 102.06kg (225 lbs converted)
     * In KG mode: "100" -> 100kg (no conversion)
     */
    fun parseUserInput(input: String): Float {
        weightUnitManager?.let { manager ->
            return manager.parseUserInput(input)
        }
        // Fallback: treat as kg (original behavior)
        return input.toFloatOrNull()?.let { roundToNearestQuarter(it) } ?: 0f
    }
}
