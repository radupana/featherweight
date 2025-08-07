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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.radupana.featherweight.ui.theme.ChartTheme
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.format.DateTimeFormatter

@Composable
fun VolumeBarChart(
    dataPoints: List<ExerciseDataPoint>,
    modifier: Modifier = Modifier,
    onDataPointClick: (ExerciseDataPoint) -> Unit = {},
) {
    var selectedPoint by remember { mutableStateOf<ExerciseDataPoint?>(null) }

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
                if (dataPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No volume data available for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    VolumeBarChartCanvas(
                        dataPoints = dataPoints,
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
                            text = "Week of ${point.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Volume: ${WeightFormatter.formatWeight(point.weight)}kg",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        point.context?.let { context ->
                            Text(
                                text = context,
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
private fun VolumeBarChartCanvas(
    dataPoints: List<ExerciseDataPoint>,
    selectedPoint: ExerciseDataPoint?,
    onPointSelected: (ExerciseDataPoint) -> Unit,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = ChartTheme.primaryChartColor()
    val onSurfaceColor = ChartTheme.axisLabelColor()
    val surfaceVariantColor = ChartTheme.gridLineColor()

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
        val leftPadding = 60.dp.toPx() // Space for Y-axis labels (more space for kg values)
        val rightPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 40.dp.toPx() // Space for X-axis labels

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        if (dataPoints.isEmpty()) return@Canvas

        // Calculate bounds
        val maxVolume = dataPoints.maxOf { it.weight } * 1.05f
        val minVolume = 0f

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight - (i.toFloat() / gridLines * chartHeight)
            val volume = minVolume + (i.toFloat() / gridLines * maxVolume)

            // Grid line
            drawLine(
                color = surfaceVariantColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
            )

            // Volume label
            val volumeText = "${WeightFormatter.formatWeight(volume)}kg"
            val textLayoutResult =
                textMeasurer.measure(
                    text = volumeText,
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
                        leftPadding - textLayoutResult.size.width - 8.dp.toPx(),
                        y - textLayoutResult.size.height / 2,
                    ),
            )
        }

        // Calculate bar positions and dimensions
        val barSpacing = 4.dp.toPx()
        val barWidth =
            if (dataPoints.size > 1) {
                (chartWidth - (dataPoints.size - 1) * barSpacing) / dataPoints.size
            } else {
                chartWidth * 0.5f
            }

        dataPoints.forEachIndexed { index, dataPoint ->
            val x = leftPadding + index * (barWidth + barSpacing)
            val barHeight = (dataPoint.weight / maxVolume * chartHeight) * animationProgress.value
            val y = topPadding + chartHeight - barHeight

            val isSelected = dataPoint == selectedPoint

            // Bar
            drawRect(
                color = primaryColor.copy(alpha = if (isSelected) 1f else 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
            )

            // Handle click detection
            clickPosition?.let { click ->
                if (click.x >= x &&
                    click.x <= x + barWidth &&
                    click.y >= y &&
                    click.y <= y + barHeight
                ) {
                    onPointSelected(dataPoint)
                    clickPosition = null
                }
            }
        }

        // Add smart X-axis labels (beginning, middle, end)
        if (dataPoints.size >= 2) {
            val labelIndices =
                when (dataPoints.size) {
                    2 -> listOf(0, dataPoints.size - 1)
                    else -> listOf(0, dataPoints.size / 2, dataPoints.size - 1)
                }

            labelIndices.forEach { index ->
                val dataPoint = dataPoints[index]
                val x = leftPadding + index * (barWidth + barSpacing) + barWidth / 2

                // Format date label for X-axis
                val dateLabel = dataPoint.date.format(DateTimeFormatter.ofPattern("MMM d"))

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
        }

        // Clear click position after processing
        clickPosition = null
    }
}
