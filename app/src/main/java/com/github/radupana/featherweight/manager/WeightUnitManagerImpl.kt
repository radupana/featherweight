package com.github.radupana.featherweight.manager

import android.content.Context
import androidx.core.content.edit
import com.github.radupana.featherweight.model.WeightUnit
import java.util.Locale
import kotlin.math.round

class WeightUnitManagerImpl(
    context: Context,
) : WeightUnitManager {
    private val prefs = context.getSharedPreferences("weight_prefs", Context.MODE_PRIVATE)
    private var currentUnit: WeightUnit = WeightUnit.KG

    companion object {
        private const val KG_TO_LBS = 2.20462f
        private const val LBS_TO_KG = 0.453592f
    }

    init {
        val savedUnit = prefs.getString("weight_unit", WeightUnit.KG.name)
        currentUnit =
            try {
                WeightUnit.valueOf(savedUnit ?: WeightUnit.KG.name)
            } catch (e: IllegalArgumentException) {
                WeightUnit.KG
            }
    }

    override fun getCurrentUnit(): WeightUnit = currentUnit

    override fun setUnit(unit: WeightUnit) {
        currentUnit = unit
        prefs.edit { putString("weight_unit", unit.name) }
    }

    override fun convertFromKg(weightKg: Float): Float =
        when (currentUnit) {
            WeightUnit.KG -> weightKg
            WeightUnit.LBS -> weightKg * KG_TO_LBS
        }

    override fun convertToKg(weight: Float): Float =
        when (currentUnit) {
            WeightUnit.KG -> weight
            WeightUnit.LBS -> weight * LBS_TO_KG
        }

    override fun roundToStoragePrecision(weightKg: Float): Float = round(weightKg * 4) / 4

    override fun roundToDisplayPrecision(
        weight: Float,
        unit: WeightUnit,
    ): Float {
        val increment = unit.increment
        return round(weight / increment) * increment
    }

    override fun formatWeight(weightKg: Float): String {
        val weight = convertFromKg(weightKg)
        val rounded = roundToDisplayPrecision(weight, getCurrentUnit())
        return if (rounded % 1.0f == 0.0f) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    override fun formatWeightWithUnit(weightKg: Float): String = "${formatWeight(weightKg)}${getCurrentUnit().suffix}"

    override fun formatVolume(volumeKg: Float): String {
        val volume = convertFromKg(volumeKg)
        val unit = getCurrentUnit().suffix
        return when {
            volume >= 10000 -> "${formatDecimal(volume / 1000, 1)}k $unit"
            volume >= 1000 -> "${formatDecimal(volume / 1000, 2)}k $unit"
            else -> formatWeightWithUnit(volumeKg)
        }
    }

    override fun parseUserInput(input: String): Float {
        val trimmedInput = input.trim()

        // Check for explicit unit suffix
        val hasKgSuffix = trimmedInput.endsWith("kg", ignoreCase = true)
        val hasLbsSuffix =
            trimmedInput.endsWith("lbs", ignoreCase = true) ||
                trimmedInput.endsWith("lb", ignoreCase = true)

        val cleanInput =
            trimmedInput
                .replace("kg", "", ignoreCase = true)
                .replace("lbs", "", ignoreCase = true)
                .replace("lb", "", ignoreCase = true)
                .trim()

        val weight = cleanInput.toFloatOrNull() ?: 0f

        // Determine which unit to interpret the input as
        val weightInKg =
            when {
                hasKgSuffix -> weight // Explicit kg
                hasLbsSuffix -> weight * LBS_TO_KG // Explicit lbs
                currentUnit == WeightUnit.LBS -> weight * LBS_TO_KG // Default to current unit (lbs)
                else -> weight // Default to current unit (kg)
            }

        return roundToStoragePrecision(weightInKg)
    }

    override fun getExportWeight(weightKg: Float): Pair<Float, String> {
        val weight = convertFromKg(weightKg)
        val unit = getCurrentUnit().suffix
        return Pair(weight, unit)
    }

    override fun parseImportWeight(
        value: Float,
        unit: String?,
    ): Float {
        val weightUnit =
            when (unit?.lowercase()) {
                "lbs", "lb" -> WeightUnit.LBS
                else -> WeightUnit.KG
            }

        return if (weightUnit == WeightUnit.KG) {
            roundToStoragePrecision(value)
        } else {
            val kgValue = value * LBS_TO_KG
            roundToStoragePrecision(kgValue)
        }
    }

    private fun formatDecimal(
        value: Float,
        decimals: Int = 2,
    ): String = String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
}
