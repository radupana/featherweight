package com.github.radupana.featherweight.util

import kotlin.math.ceil
import kotlin.math.round

object WeightFormatter {
    /**
     * Formats weight to nearest quarter (0.00, 0.25, 0.50, 0.75)
     * Shows integers without decimal (82.0 -> "82")
     * Shows meaningful decimals (82.5 -> "82.5")
     */
    fun formatWeight(weight: Float): String {
        // Round to nearest quarter
        val roundedWeight = roundToNearestQuarter(weight)

        // If it's a whole number, show without decimal
        return if (roundedWeight % 1.0f == 0.0f) {
            roundedWeight.toInt().toString()
        } else {
            // Show with at most 2 decimal places
            String.format("%.2f", roundedWeight).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Formats weight with kg unit
     */
    fun formatWeightWithUnit(weight: Float): String {
        return "${formatWeight(weight)}kg"
    }

    /**
     * Rounds a weight to the nearest quarter (0.25 increments)
     */
    fun roundToNearestQuarter(weight: Float): Float {
        return (round(weight * 4) / 4).toFloat()
    }

    /**
     * Rounds UP to the nearest quarter (for progression suggestions)
     */
    fun roundUpToNearestQuarter(weight: Float): Float {
        return (ceil(weight * 4) / 4).toFloat()
    }

    /**
     * Formats any decimal to at most 2 decimal places
     */
    fun formatDecimal(
        value: Float,
        decimals: Int = 2,
    ): String {
        return String.format("%.${decimals}f", value).trimEnd('0').trimEnd('.')
    }

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
     * Formats large volumes (e.g., 1234kg -> 1.2k kg)
     */
    fun formatVolume(volume: Float): String {
        return when {
            volume >= 10000 -> "${formatDecimal(volume / 1000, 1)}k kg"
            volume >= 1000 -> "${formatDecimal(volume / 1000, 2)}k kg"
            else -> formatWeightWithUnit(volume)
        }
    }
}
