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
import androidx.compose.ui.graphics.Color
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

data class IntensityZoneData(
    val zone: String,
    val range: String,
    val volume: Float,
    val sets: Int,
    val avgWeight: Float,
    val color: Color,
)

@Composable
fun IntensityZoneChart(
    intensityData: List<IntensityZoneData>,
    modifier: Modifier = Modifier,
    onZoneClick: (IntensityZoneData) -> Unit = {},
) {
    var selectedZone by remember { mutableStateOf<IntensityZoneData?>(null) }
    val totalSets = remember(intensityData) { intensityData.sumOf { it.sets } }

    Column(modifier = modifier) {
        // Chart
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (intensityData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No RPE data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    IntensityZoneChartCanvas(
                        intensityData = intensityData,
                        selectedZone = selectedZone,
                        onZoneSelected = { zone ->
                            selectedZone = zone
                            onZoneClick(zone)
                        },
                    )
                }
            }
        }

        // Selected zone details
        selectedZone?.let { zone ->
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
                        text = "${zone.zone} (${zone.range})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${zone.sets} sets (${((zone.sets.toFloat() / totalSets) * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Total Volume: ${WeightFormatter.formatWeight(zone.volume)}kg",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${zone.sets} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            text = "Avg: ${WeightFormatter.formatWeight(zone.avgWeight)}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntensityZoneChartCanvas(
    intensityData: List<IntensityZoneData>,
    selectedZone: IntensityZoneData?,
    onZoneSelected: (IntensityZoneData) -> Unit,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = ChartTheme.axisLabelColor()
    val surfaceVariantColor = ChartTheme.gridLineColor()

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(intensityData) {
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 0.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        clickPosition = offset
                    }
                },
    ) {
        val leftPadding = 60.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 40.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        if (intensityData.isEmpty()) return@Canvas

        // Calculate bounds - use percentages for better visualization
        val totalSets = intensityData.sumOf { it.sets }.toFloat()
        val maxPercentage =
            if (totalSets > 0) {
                (intensityData.maxOf { it.sets }.toFloat() / totalSets * 100) * 1.05f
            } else {
                100f
            }

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight - (i.toFloat() / gridLines * chartHeight)
            val percentage = (i.toFloat() / gridLines * maxPercentage).toInt()

            // Grid line
            drawLine(
                color = surfaceVariantColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
            )

            // Percentage label
            val percentageText = "$percentage%"
            val textLayoutResult =
                textMeasurer.measure(
                    text = percentageText,
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
        val barSpacing = 8.dp.toPx()
        val barWidth = (chartWidth - (intensityData.size - 1) * barSpacing) / intensityData.size

        intensityData.forEachIndexed { index, zone ->
            val x = leftPadding + index * (barWidth + barSpacing)
            val zonePercentage = if (totalSets > 0) (zone.sets.toFloat() / totalSets * 100) else 0f
            val barHeight = (zonePercentage / maxPercentage * chartHeight) * animationProgress.value
            val y = topPadding + chartHeight - barHeight

            val isSelected = zone == selectedZone

            // Bar with zone-specific color
            drawRect(
                color = zone.color.copy(alpha = if (isSelected) 1f else 0.8f),
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
                    onZoneSelected(zone)
                    clickPosition = null
                }
            }

            // X-axis label (RPE range)
            val rpeRangeLabel = zone.range // Use the range instead of zone name
            val textLayoutResult =
                textMeasurer.measure(
                    text = rpeRangeLabel,
                    style =
                        TextStyle(
                            fontSize = 9.sp,
                            color = onSurfaceColor,
                        ),
                )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft =
                    Offset(
                        x + barWidth / 2 - textLayoutResult.size.width / 2,
                        size.height - bottomPadding + 8.dp.toPx(),
                    ),
            )

            // Display percentage on top of bar if bar is tall enough
            if (barHeight > 20.dp.toPx()) {
                val percentageText = "${zonePercentage.toInt()}%"
                val percentageLayout =
                    textMeasurer.measure(
                        text = percentageText,
                        style =
                            TextStyle(
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                drawText(
                    textLayoutResult = percentageLayout,
                    topLeft =
                        Offset(
                            x + barWidth / 2 - percentageLayout.size.width / 2,
                            y + 4.dp.toPx(),
                        ),
                )
            }
        }

        // Clear click position after processing
        clickPosition = null
    }
}
