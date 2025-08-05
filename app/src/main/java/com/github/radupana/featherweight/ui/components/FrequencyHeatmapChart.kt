package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.theme.ChartTheme
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class FrequencyDataPoint(
    val date: LocalDate,
    val volume: Float,
    val sessions: Int,
)

@Composable
fun FrequencyHeatmapChart(
    dataPoints: List<FrequencyDataPoint>,
    modifier: Modifier = Modifier,
    onDayClick: (FrequencyDataPoint?) -> Unit = {},
) {
    var selectedPoint by remember { mutableStateOf<FrequencyDataPoint?>(null) }

    Column(modifier = modifier) {
        // Chart
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No frequency data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    FrequencyHeatmapCanvas(
                        dataPoints = dataPoints,
                        selectedPoint = selectedPoint,
                        onPointSelected = { point ->
                            selectedPoint = point
                            onDayClick(point)
                        },
                    )
                }
            }
        }

        // Legend
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            // Color scale
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val colors =
                    listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        Color(0xFF4FC3F7), // Light blue
                        Color(0xFF29B6F6), // Blue
                        Color(0xFFFF9800), // Orange
                        Color(0xFFF44336), // Red
                    )

                colors.forEach { color ->
                    Box(
                        modifier =
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color),
                    )
                }
            }

            Text(
                text = "More",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        // Selected point details
        selectedPoint?.let { point ->
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = point.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Volume: ${WeightFormatter.formatWeight(point.volume)}kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${point.sessions} session${if (point.sessions != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyHeatmapCanvas(
    dataPoints: List<FrequencyDataPoint>,
    selectedPoint: FrequencyDataPoint?,
    onPointSelected: (FrequencyDataPoint?) -> Unit,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    ChartTheme.primaryChartColor()
    val gridLineColor = ChartTheme.gridLineColor()

    // Create a complete 12-week grid starting from 12 weeks ago
    val endDate = LocalDate.now()
    val startDate = endDate.minusWeeks(12)
    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    // Create map for quick lookup
    val dataMap = dataPoints.associateBy { it.date }
    val maxVolume = if (dataPoints.isNotEmpty()) dataPoints.maxOf { it.volume } else 1f

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        clickPosition = offset
                    }
                },
    ) {
        val padding = 8.dp.toPx()
        val availableWidth = size.width - 2 * padding
        val availableHeight = size.height - 2 * padding

        // Calculate grid dimensions (7 days x ~12 weeks)
        val daysPerWeek = 7
        val weeks = kotlin.math.ceil(totalDays.toDouble() / daysPerWeek).toInt()

        val cellWidth = availableWidth / weeks
        val cellHeight = availableHeight / daysPerWeek
        val cellSize = kotlin.math.min(cellWidth, cellHeight) * 0.8f
        cellSize * 0.1f

        for (dayOffset in 0 until totalDays) {
            val currentDate = startDate.plusDays(dayOffset.toLong())
            val weekIndex = dayOffset / daysPerWeek
            val dayIndex = dayOffset % daysPerWeek

            val x = padding + weekIndex * cellWidth + cellWidth / 2 - cellSize / 2
            val y = padding + dayIndex * cellHeight + cellHeight / 2 - cellSize / 2

            val dataPoint = dataMap[currentDate]
            val isSelected = dataPoint == selectedPoint

            val color =
                if (dataPoint != null) {
                    val intensity = (dataPoint.volume / maxVolume).coerceIn(0f, 1f)
                    // Blue to red gradient
                    when {
                        intensity < 0.25f -> Color(0xFF4FC3F7) // Light blue
                        intensity < 0.5f -> Color(0xFF29B6F6) // Blue
                        intensity < 0.75f -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFFF44336) // Red
                    }
                } else {
                    gridLineColor.copy(alpha = 0.1f)
                }

            // Draw cell
            drawRect(
                color = if (isSelected) color.copy(alpha = 1f) else color,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
            )

            // Handle click detection
            clickPosition?.let { click ->
                if (click.x >= x &&
                    click.x <= x + cellSize &&
                    click.y >= y &&
                    click.y <= y + cellSize
                ) {
                    onPointSelected(dataPoint)
                    clickPosition = null
                }
            }
        }

        // Clear click position after processing
        clickPosition = null
    }
}
