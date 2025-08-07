package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
        // Determine visualization type based on data density
        val sessionCount = dataPoints.size

        when {
            sessionCount == 0 -> {
                // No data available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No training data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            sessionCount < 10 -> {
                // Sparse data - show as cards
                SparseFrequencyView(
                    dataPoints = dataPoints,
                    selectedPoint = selectedPoint,
                    onPointSelected = { point ->
                        selectedPoint = point
                        onDayClick(point)
                    },
                )
            }
            else -> {
                // Regular heatmap for dense data
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
                        Color(0xFF212121), // Dark grey for empty
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
private fun SparseFrequencyView(
    dataPoints: List<FrequencyDataPoint>,
    selectedPoint: FrequencyDataPoint?,
    onPointSelected: (FrequencyDataPoint) -> Unit,
) {
    val maxVolume = dataPoints.maxOfOrNull { it.volume } ?: 1f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Title
        Text(
            text = "Training Sessions (${dataPoints.size} total)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Session cards
        dataPoints.sortedByDescending { it.date }.forEach { point ->
            val intensity = (point.volume / maxVolume).coerceIn(0f, 1f)
            val color =
                when {
                    intensity < 0.25f -> Color(0xFF4FC3F7) // Light blue
                    intensity < 0.5f -> Color(0xFF29B6F6) // Blue
                    intensity < 0.75f -> Color(0xFFFF9800) // Orange
                    else -> Color(0xFFF44336) // Red
                }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPointSelected(point) },
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (point == selectedPoint) {
                                color.copy(alpha = 0.2f)
                            } else {
                                Color(0xFF212121) // Dark background for sparse view
                            },
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = point.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (point == selectedPoint) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = "${point.sessions} session${if (point.sessions != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${WeightFormatter.formatWeight(point.volume)}kg",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        // Color indicator
                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color),
                        )
                    }
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

    val endDate = LocalDate.now()
    val startDate = endDate.minusWeeks(12) // Always show 12 weeks

    // Group data points by week (Monday to Sunday)
    val weeklyData = mutableMapOf<LocalDate, MutableList<FrequencyDataPoint>>()

    dataPoints.forEach { point ->
        if (point.date >= startDate && point.date <= endDate) {
            // Find Monday of the week containing this date
            val monday = point.date.minusDays(point.date.dayOfWeek.value - 1L)
            weeklyData.getOrPut(monday) { mutableListOf() }.add(point)
        }
    }

    // Calculate sessions per week for color mapping
    val weeklySessionCounts =
        weeklyData.mapValues { (_, points) ->
            points.sumOf { it.sessions }
        }

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

        // 12 weekly blocks arranged in a 4x3 grid
        val columns = 4
        val rows = 3

        val cellWidth = availableWidth / columns
        val cellHeight = availableHeight / rows
        val cellSize = kotlin.math.min(cellWidth, cellHeight) * 0.8f

        // Generate 12 weeks from start date
        for (weekOffset in 0 until 12) {
            val weekStart = startDate.plusWeeks(weekOffset.toLong())
            val columnIndex = weekOffset % columns
            val rowIndex = weekOffset / columns

            val x = padding + columnIndex * cellWidth + cellWidth / 2 - cellSize / 2
            val y = padding + rowIndex * cellHeight + cellHeight / 2 - cellSize / 2

            val sessionsThisWeek = weeklySessionCounts[weekStart] ?: 0
            val weekData = weeklyData[weekStart]
            val isSelected = weekData?.contains(selectedPoint) == true

            val color =
                when (sessionsThisWeek) {
                    0 -> Color(0xFF212121) // Dark grey for no sessions
                    1 -> Color(0xFF4FC3F7) // Light blue for 1 session
                    2 -> Color(0xFF29B6F6) // Blue for 2 sessions
                    3 -> Color(0xFFFF9800) // Orange for 3 sessions
                    else -> Color(0xFFF44336) // Red for 4+ sessions
                }

            // Draw cell
            drawRect(
                color = if (isSelected) color.copy(alpha = 1f) else color,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
            )

            // Handle click detection - select the most recent session from that week
            clickPosition?.let { click ->
                if (click.x >= x &&
                    click.x <= x + cellSize &&
                    click.y >= y &&
                    click.y <= y + cellSize
                ) {
                    val mostRecentInWeek = weekData?.maxByOrNull { it.date }
                    onPointSelected(mostRecentInWeek)
                    clickPosition = null
                }
            }
        }

        // Clear click position after processing
        clickPosition = null
    }
}
