package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Utility function to format large numbers
private fun formatVolume(volume: Float): String {
    return when {
        volume >= 1000 -> "${String.format("%.1f", volume / 1000)}k kg"
        else -> "${volume.toInt()}kg"
    }
}

@Composable
fun StrengthProgressionChart(
    data: List<Pair<Float, LocalDateTime>>, // weight, date
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    exerciseName: String = "",
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No data available for $exerciseName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = modifier) {
        // Chart title with current max
        val currentMax = data.maxOfOrNull { it.first } ?: 0f
        Text(
            text = "$exerciseName Personal Records",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Current Max: ${currentMax.toInt()}kg",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Chart area
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
        ) {
            drawStrengthChart(data, lineColor)
        }

        // X-axis labels (simplified)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (data.isNotEmpty()) {
                val startDate = data.minByOrNull { it.second }?.second
                val endDate = data.maxByOrNull { it.second }?.second

                Text(
                    text = startDate?.format(DateTimeFormatter.ofPattern("MMM yy")) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Progress Over Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = endDate?.format(DateTimeFormatter.ofPattern("MMM yy")) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun VolumeBarChart(
    weeklyData: List<Pair<String, Float>>, // label, volume
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.secondary,
    onBarTapped: ((String, Float) -> Unit)? = null,
) {
    if (weeklyData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No volume data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    var selectedBar by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        Text(
            text = "Volume Trends",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Show selected value if any
        selectedBar?.let { index ->
            if (index in weeklyData.indices) {
                val (label, volume) = weeklyData[index]
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = "$label: ${formatVolume(volume)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Chart area with tap detection
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(weeklyData) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / weeklyData.size * 0.7f
                            val barSpacing = size.width / weeklyData.size * 0.15f
                            val padding = 20.dp.toPx()

                            weeklyData.forEachIndexed { index, (label, volume) ->
                                val x = padding + index * (barWidth + barSpacing * 2) + barSpacing

                                if (offset.x >= x && offset.x <= x + barWidth) {
                                    selectedBar = if (selectedBar == index) null else index
                                    onBarTapped?.invoke(label, volume)
                                    return@detectTapGestures
                                }
                            }
                        }
                    },
        ) {
            drawVolumeChart(weeklyData, barColor, selectedBar)
        }

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weeklyData.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun DrawScope.drawStrengthChart(
    data: List<Pair<Float, LocalDateTime>>,
    lineColor: Color,
) {
    if (data.size < 2) return

    val sortedData = data.sortedBy { it.second }

    // Calculate bounds
    val minWeight = sortedData.minOf { it.first }
    val maxWeight = sortedData.maxOf { it.first }
    val weightRange = maxWeight - minWeight
    val padding = 40.dp.toPx()

    val chartWidth = size.width - 2 * padding
    val chartHeight = size.height - 2 * padding

    // Create points
    val points =
        sortedData.mapIndexed { index, (weight, _) ->
            val x = padding + (index.toFloat() / (sortedData.size - 1)) * chartWidth
            val y = padding + (1 - (weight - minWeight) / weightRange) * chartHeight
            Offset(x, y)
        }

    // Draw grid lines (horizontal)
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    repeat(5) { i ->
        val y = padding + (i / 4f) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(size.width - padding, y),
            strokeWidth = 1.dp.toPx(),
        )
    }

    // Draw line
    val path =
        Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
        }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )

    // Draw points
    points.forEach { point ->
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = point,
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = point,
        )
    }

    // Draw Y-axis labels
    repeat(5) { i ->
        val weight = minWeight + (weightRange * i / 4f)
        val y = padding + ((4 - i) / 4f) * chartHeight

        // You could add text drawing here if needed, but it's complex with Canvas
        // For now, we'll rely on the title showing the current max
    }
}

private fun DrawScope.drawVolumeChart(
    data: List<Pair<String, Float>>,
    barColor: Color,
    selectedBar: Int? = null,
) {
    if (data.isEmpty()) return

    val maxVolume = data.maxOf { it.second }
    val padding = 20.dp.toPx()
    val chartHeight = size.height - 2 * padding
    val barWidth = (size.width - 2 * padding) / data.size * 0.7f
    val barSpacing = (size.width - 2 * padding) / data.size * 0.3f

    data.forEachIndexed { index, (_, volume) ->
        val barHeight = if (maxVolume > 0) (volume / maxVolume) * chartHeight else 0f
        val x = padding + index * (barWidth + barSpacing)
        val y = size.height - padding - barHeight

        // Use different color for selected bar
        val currentBarColor =
            if (selectedBar == index) {
                barColor.copy(alpha = 1f) // Full opacity for selected
            } else {
                barColor.copy(alpha = 0.7f) // Reduced opacity for non-selected
            }

        // Draw bar
        drawRect(
            color = currentBarColor,
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
        )

        // Draw border for selected bar
        if (selectedBar == index) {
            drawRect(
                color = barColor,
                topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
fun SimpleLineChart(
    dataPoints: List<Float>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    title: String = "",
) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
        ) {
            if (dataPoints.size < 2) return@Canvas

            val maxValue = dataPoints.maxOrNull() ?: 1f
            val minValue = dataPoints.minOrNull() ?: 0f
            val range = maxValue - minValue
            val padding = 20.dp.toPx()

            val chartWidth = size.width - 2 * padding
            val chartHeight = size.height - 2 * padding

            val points =
                dataPoints.mapIndexed { index, value ->
                    val x = padding + (index.toFloat() / (dataPoints.size - 1)) * chartWidth
                    val y = padding + (1 - if (range > 0) (value - minValue) / range else 0f) * chartHeight
                    Offset(x, y)
                }

            // Draw line
            val path =
                Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = point,
                )
            }
        }
    }
}
