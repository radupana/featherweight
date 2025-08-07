package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.radupana.featherweight.ui.theme.ChartTheme
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ExerciseDataPoint(
    val date: LocalDate,
    val weight: Float,
    val reps: Int,
    val isPR: Boolean = false,
    val context: String? = null, // e.g., "90kg × 3 @ RPE 8"
)

@Composable
fun ExerciseProgressChart(
    dataPoints: List<ExerciseDataPoint>,
    modifier: Modifier = Modifier,
    isMaxWeightChart: Boolean = false,
    onDataPointClick: (ExerciseDataPoint) -> Unit = {},
) {
    var selectedPoint by remember { mutableStateOf<ExerciseDataPoint?>(null) }

    // Filter data to show last 12 weeks
    val filteredData =
        remember(dataPoints) {
            val cutoffDate = LocalDate.now().minusWeeks(12)
            dataPoints.filter { it.date >= cutoffDate }
        }

    Column(modifier = modifier) {
        // Chart
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No data available for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    ChartCanvas(
                        dataPoints = filteredData,
                        selectedPoint = selectedPoint,
                        onPointSelected = { point ->
                            selectedPoint = point
                            onDataPointClick(point)
                        },
                    )
                }
            }
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
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = point.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${if (isMaxWeightChart) "Weight Lifted" else "1RM"}: ${WeightFormatter.formatWeightWithUnit(point.weight)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        point.context?.let { context ->
                            Text(
                                text = "From: $context",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    dataPoints: List<ExerciseDataPoint>,
    selectedPoint: ExerciseDataPoint?,
    onPointSelected: (ExerciseDataPoint) -> Unit,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = ChartTheme.primaryChartColor()
    val onSurfaceColor = ChartTheme.axisLabelColor()
    val surfaceVariantColor = ChartTheme.gridLineColor()
    val prColor = ChartTheme.prMarkerColor

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 0.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        clickPosition = offset
                    }
                },
    ) {
        val leftPadding = 50.dp.toPx() // Space for Y-axis labels
        val rightPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 40.dp.toPx() // Space for X-axis labels

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        if (dataPoints.isEmpty()) return@Canvas

        // Calculate bounds
        val minWeight = dataPoints.minOf { it.weight } * 0.95f
        val maxWeight = dataPoints.maxOf { it.weight } * 1.05f
        val weightRange = maxWeight - minWeight

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight - (i.toFloat() / gridLines * chartHeight)
            val weight = minWeight + (i.toFloat() / gridLines * weightRange)

            // Grid line
            drawLine(
                color = surfaceVariantColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
            )

            // Weight label
            val weightText = WeightFormatter.formatWeightWithUnit(weight)
            val textLayoutResult =
                textMeasurer.measure(
                    text = weightText,
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            color = onSurfaceColor,
                        ),
                )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(leftPadding - textLayoutResult.size.width - 8.dp.toPx(), y - textLayoutResult.size.height / 2),
            )
        }

        // Calculate point positions
        val points =
            dataPoints.mapIndexed { index, dataPoint ->
                val x = leftPadding + (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth
                val y = topPadding + chartHeight - ((dataPoint.weight - minWeight) / weightRange * chartHeight)
                Triple(x, y, dataPoint)
            }

        // Handle click detection
        clickPosition?.let { click ->
            val clickRadius = 20.dp.toPx() // Touch target radius
            points.forEach { (x, y, dataPoint) ->
                val distance =
                    kotlin.math.sqrt(
                        (click.x - x) * (click.x - x) + (click.y - y) * (click.y - y),
                    )
                if (distance <= clickRadius) {
                    onPointSelected(dataPoint)
                    clickPosition = null
                    return@let
                }
            }
            clickPosition = null
        }

        // Draw line with animation
        val path = Path()
        points.forEachIndexed { index, (x, y, _) ->
            val animatedY = chartHeight - (chartHeight - y) * animationProgress.value
            if (index == 0) {
                path.moveTo(x, animatedY)
            } else {
                path.lineTo(x, animatedY)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        // Draw gradient fill under the line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(size.width - rightPadding, topPadding + chartHeight)
        fillPath.lineTo(leftPadding, topPadding + chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            primaryColor.copy(alpha = 0.3f * animationProgress.value),
                            primaryColor.copy(alpha = 0f),
                        ),
                    startY = topPadding,
                    endY = topPadding + chartHeight,
                ),
        )

        // Draw X-axis labels with improved distribution to avoid duplicates
        val xLabelCount = minOf(5, dataPoints.size) // Show max 5 labels
        val usedLabels = mutableSetOf<String>() // Track used labels to prevent duplicates

        for (i in 0 until xLabelCount) {
            val index =
                if (xLabelCount == 1) {
                    0
                } else {
                    (i * (dataPoints.size - 1) / (xLabelCount - 1)).coerceIn(0, dataPoints.size - 1)
                }
            val dataPoint = dataPoints[index]
            val x = leftPadding + (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth

            // Format date label with improved logic
            val now = LocalDate.now()
            val daysBetween =
                java.time.temporal.ChronoUnit.DAYS
                    .between(dataPoint.date, now)
            val weeksBetween = daysBetween / 7
            val monthsBetween =
                java.time.temporal.ChronoUnit.MONTHS
                    .between(dataPoint.date, now)

            val dateLabel =
                when {
                    daysBetween == 0L -> "Today"
                    daysBetween == 1L -> "1d"
                    daysBetween < 7 -> "${daysBetween}d"
                    weeksBetween < 4 -> "${weeksBetween}w"
                    monthsBetween < 12 -> "${monthsBetween}m"
                    else -> {
                        // For dates more than a year old, still use month format
                        val totalMonths = monthsBetween.coerceAtMost(99)
                        "${totalMonths}m"
                    }
                }

            // Skip this label if it's a duplicate, except for the first and last labels
            val finalLabel =
                if (usedLabels.contains(dateLabel) && i != 0 && i != xLabelCount - 1) {
                    // If it's a duplicate, use a more specific format based on context
                    when {
                        daysBetween < 30 -> "${daysBetween}d"
                        monthsBetween < 2 -> "${weeksBetween}w"
                        else -> dataPoint.date.format(DateTimeFormatter.ofPattern("MMM d"))
                    }
                } else {
                    dateLabel
                }
            usedLabels.add(finalLabel)

            val textLayoutResult =
                textMeasurer.measure(
                    text = finalLabel,
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            color = onSurfaceColor,
                        ),
                )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft =
                    Offset(
                        x - textLayoutResult.size.width / 2,
                        size.height - bottomPadding + 8.dp.toPx(),
                    ),
            )
        }

        // Draw points
        points.forEach { (x, y, dataPoint) ->
            val animatedY = chartHeight - (chartHeight - y) * animationProgress.value
            val isSelected = dataPoint == selectedPoint

            // Point circle
            drawCircle(
                color = if (dataPoint.isPR) prColor else primaryColor,
                radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                center = Offset(x, animatedY),
            )

            // White center for better visibility
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 4.dp.toPx() else 2.dp.toPx(),
                center = Offset(x, animatedY),
            )

            // PR marker - no text needed since 1RM progression is monotonically increasing
        }
    }
}
