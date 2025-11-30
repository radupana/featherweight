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

    val prMarkerColor = Color(0xFFFCD34D)

    @Composable
    fun chartBackgroundColor() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    @Composable
    fun positiveChangeColor() = Color(0xFF10B981)

    @Composable
    fun negativeChangeColor() = Color(0xFFEF4444)

    val repRangeColors =
        listOf(
            Color(0xFF10B981),
            Color(0xFF6366F1),
            Color(0xFFF59E0B),
            Color(0xFFEF4444),
        )

    val onColoredBackground = Color.White
}
