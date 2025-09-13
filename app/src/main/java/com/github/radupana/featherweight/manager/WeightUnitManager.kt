package com.github.radupana.featherweight.manager

import com.github.radupana.featherweight.model.WeightUnit

interface WeightUnitManager {
    fun getCurrentUnit(): WeightUnit

    fun setUnit(unit: WeightUnit)

    fun convertFromKg(weightKg: Float): Float

    fun convertToKg(weight: Float): Float

    fun roundToStoragePrecision(weightKg: Float): Float

    fun roundToDisplayPrecision(
        weight: Float,
        unit: WeightUnit,
    ): Float

    fun formatWeight(weightKg: Float): String

    fun formatWeightWithUnit(weightKg: Float): String

    fun formatVolume(volumeKg: Float): String

    fun parseUserInput(input: String): Float

    fun getExportWeight(weightKg: Float): Pair<Float, String>

    fun parseImportWeight(
        value: Float,
        unit: String?,
    ): Float
}
