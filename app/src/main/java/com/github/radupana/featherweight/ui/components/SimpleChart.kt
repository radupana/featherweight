package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.theme.ChartTheme
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun StrengthProgressionChart(
    data: List<Pair<Float, LocalDateTime>>, // weight, date
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    exerciseName: String = "",
    onDataPointTapped: ((Float, LocalDateTime) -> Unit)? = null,
) {
    if (data.isEmpty()) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(200.dp),
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

    var selectedDataPoint by remember { mutableStateOf<Int?>(null) }
    val gridColor = ChartTheme.gridLineColor()

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
            text = "Current Max: ${WeightFormatter.formatWeightWithUnit(currentMax)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Show selected data point if any
        selectedDataPoint?.let { index ->
            if (index in data.indices) {
                val (weight, date) = data[index]
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = ChartTheme.tooltipBackgroundColor(),
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = ChartTheme.tooltipElevation.dp,
                        ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = ChartTheme.tooltipBorderColor(),
                        ),
                ) {
                    Text(
                        text = "${
                            WeightFormatter.formatWeightWithUnit(
                                weight,
                            )
                        } on ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChartTheme.tooltipContentColor(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Chart area
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val sortedData = data.sortedBy { it.second }
                            val padding = 40.dp.toPx()
                            val chartWidth = size.width - 2 * padding
                            val tapRadius = 20.dp.toPx() // Radius around data points for tap detection

                            sortedData.forEachIndexed { index, (weight, date) ->
                                val x = padding + (index.toFloat() / (sortedData.size - 1)) * chartWidth

                                // Calculate y position (same logic as in drawStrengthChart)
                                val minWeight = sortedData.minOf { it.first }
                                val maxWeight = sortedData.maxOf { it.first }
                                val weightRange = maxWeight - minWeight
                                val chartHeight = size.height - 2 * padding
                                val y = padding + (1 - (weight - minWeight) / weightRange) * chartHeight

                                // Check if tap is within radius of data point
                                val distance =
                                    kotlin.math.sqrt(
                                        (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y),
                                    )

                                if (distance <= tapRadius) {
                                    selectedDataPoint = if (selectedDataPoint == index) null else index
                                    onDataPointTapped?.invoke(weight, date)
                                    return@detectTapGestures
                                }
                            }
                        }
                    },
        ) {
            drawStrengthChart(data, lineColor, gridColor, selectedDataPoint)
        }

        // X-axis labels (simplified)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(160.dp),
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
                            containerColor = ChartTheme.tooltipBackgroundColor(),
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = ChartTheme.tooltipElevation.dp,
                        ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = ChartTheme.tooltipBorderColor(),
                        ),
                ) {
                    Text(
                        text = "$label: ${WeightFormatter.formatVolume(volume)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChartTheme.tooltipContentColor(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
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
                            val padding = 20.dp.toPx()
                            val chartWidth = size.width - 2 * padding
                            val barWidth = chartWidth / weeklyData.size * 0.7f
                            val barSpacing = chartWidth / weeklyData.size * 0.3f

                            weeklyData.forEachIndexed { index, (label, volume) ->
                                // Match the exact calculation from drawVolumeChart
                                val x = padding + index * (barWidth + barSpacing)

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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
    gridColor: Color,
    selectedDataPoint: Int? = null,
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
    repeat(5) { i ->
        val y = padding + (i / 4f) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(size.width - padding, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
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
    points.forEachIndexed { index, point ->
        val isSelected = selectedDataPoint == index
        val pointRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
        val innerRadius = if (isSelected) 3.dp.toPx() else 2.dp.toPx()

        // Outer circle
        drawCircle(
            color = lineColor,
            radius = pointRadius,
            center = point,
        )

        // Inner circle
        drawCircle(
            color = Color.White,
            radius = innerRadius,
            center = point,
        )

        // Additional ring for selected point
        if (isSelected) {
            drawCircle(
                color = lineColor.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }

    // Draw Y-axis labels
    repeat(5) { i ->
        minWeight + (weightRange * i / 4f)
        padding + ((4 - i) / 4f) * chartHeight

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
            size =
                androidx.compose.ui.geometry
                    .Size(barWidth, barHeight),
        )

        // Draw border for selected bar
        if (selectedBar == index) {
            drawRect(
                color = barColor,
                topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                size =
                    androidx.compose.ui.geometry
                        .Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
fun SimpleLineChart(
    dataPoints: List<Float>,
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
