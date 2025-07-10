package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
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
)

enum class ChartViewMode(val label: String) {
    ACTUAL("Actual"),
    POTENTIAL("Potential"),
}

@Composable
fun ExerciseProgressChart(
    dataPoints: List<ExerciseDataPoint>,
    modifier: Modifier = Modifier,
    showEstimated1RM: Boolean = false,
    onDataPointClick: (ExerciseDataPoint) -> Unit = {},
) {
    var selectedPoint by remember { mutableStateOf<ExerciseDataPoint?>(null) }
    var viewMode by remember { mutableStateOf(ChartViewMode.ACTUAL) }

    // Filter data to show last 12 weeks
    val filteredData =
        remember(dataPoints) {
            val cutoffDate = LocalDate.now().minusWeeks(12)
            dataPoints.filter { it.date >= cutoffDate }
        }

    Column(modifier = modifier) {
        // Header with title and toggle
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Weight Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // View mode toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartViewMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewMode = mode },
                    ) {
                        RadioButton(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

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
                    // Transform data based on view mode
                    val displayData =
                        if (viewMode == ChartViewMode.POTENTIAL) {
                            filteredData.map { point ->
                                // Calculate estimated 1RM using Brzycki formula
                                val estimated1RM =
                                    if (point.reps == 1) {
                                        point.weight
                                    } else {
                                        point.weight * (36f / (37f - point.reps))
                                    }
                                point.copy(weight = estimated1RM)
                            }
                        } else {
                            filteredData
                        }

                    ChartCanvas(
                        dataPoints = displayData,
                        selectedPoint = selectedPoint,
                        onPointSelected = { point ->
                            // Find original data point for selection
                            val originalPoint = filteredData.find { it.date == point.date }
                            selectedPoint = originalPoint
                            onDataPointClick(originalPoint ?: point)
                        },
                        isPotentialView = viewMode == ChartViewMode.POTENTIAL,
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
                            text = "${WeightFormatter.formatWeightWithUnit(point.weight)} × ${point.reps} reps",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (viewMode == ChartViewMode.POTENTIAL && point.reps > 1) {
                            val estimated1RM = point.weight * (36f / (37f - point.reps))
                            Text(
                                text = "Estimated 1RM: ${WeightFormatter.formatWeightWithUnit(estimated1RM)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                    if (point.isPR) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "PR",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B),
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
    isPotentialView: Boolean = false,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val primaryColor =
        if (isPotentialView) {
            MaterialTheme.colorScheme.tertiary
        } else {
            ChartTheme.primaryChartColor()
        }
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

        // Draw X-axis labels
        val xLabelCount = minOf(5, dataPoints.size) // Show max 5 labels
        val xLabelInterval = (dataPoints.size - 1).coerceAtLeast(1) / (xLabelCount - 1).coerceAtLeast(1)

        for (i in 0 until xLabelCount) {
            val index = (i * xLabelInterval).toInt().coerceIn(0, dataPoints.size - 1)
            val dataPoint = dataPoints[index]
            val x = leftPadding + (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth

            // Format date label
            val now = LocalDate.now()
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dataPoint.date, now)
            val dateLabel =
                when {
                    daysBetween == 0L -> "Today"
                    daysBetween == 1L -> "1d"
                    daysBetween < 7 -> "${daysBetween}d"
                    daysBetween < 30 -> "${daysBetween / 7}w"
                    daysBetween < 365 -> "${daysBetween / 30}m"
                    else -> dataPoint.date.format(DateTimeFormatter.ofPattern("MMM yy"))
                }

            val textLayoutResult =
                textMeasurer.measure(
                    text = dateLabel,
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

            // PR marker
            if (dataPoint.isPR) {
                val prText = "PR"
                val prTextResult =
                    textMeasurer.measure(
                        text = prText,
                        style =
                            TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = prColor,
                            ),
                    )
                drawText(
                    textLayoutResult = prTextResult,
                    topLeft = Offset(x - prTextResult.size.width / 2, animatedY - 20.dp.toPx()),
                )
            }
        }
    }
}
