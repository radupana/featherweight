package com.github.radupana.featherweight.ui.components

import androidx.compose.ui.graphics.Color
import com.github.radupana.featherweight.service.ProgressDataPoint

enum class ChartType {
    LINE,
    BAR,
    AREA,
}

data class ChartData(
    val dataPoints: List<ProgressDataPoint>,
    val title: String,
    val primaryColor: Color,
    val showPRmarkers: Boolean = true,
)
