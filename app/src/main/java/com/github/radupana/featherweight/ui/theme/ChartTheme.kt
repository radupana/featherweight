package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChartTheme {
    @Composable
    fun primaryChartColor() = MaterialTheme.colorScheme.primary

    @Composable
    fun gridLineColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    @Composable
    fun axisLabelColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun prMarkerColor() = FeatherweightColors.gold()

    @Composable
    fun chartBackgroundColor() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    @Composable
    fun positiveChangeColor() = FeatherweightColors.success()

    @Composable
    fun negativeChangeColor() = FeatherweightColors.danger()

    @Composable
    fun repRangeColors(): List<Color> =
        listOf(
            FeatherweightColors.success(),
            MaterialTheme.colorScheme.secondary,
            FeatherweightColors.warning(),
            FeatherweightColors.danger(),
        )

    val onColoredBackground = Color.White
}
