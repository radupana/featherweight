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

data class RepRangeDistribution(
    val range: String,
    val volume: Float,
    val sets: Int,
    val avgWeight: Float,
)

@Composable
fun RepRangeChart(
    distributionData: List<RepRangeDistribution>,
    modifier: Modifier = Modifier,
    onRangeClick: (RepRangeDistribution) -> Unit = {},
) {
    var selectedRange by remember { mutableStateOf<RepRangeDistribution?>(null) }

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
                if (distributionData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No rep range data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    RepRangeChartCanvas(
                        distributionData = distributionData,
                        selectedRange = selectedRange,
                        onRangeSelected = { range ->
                            selectedRange = range
                            onRangeClick(range)
                        },
                    )
                }
            }
        }

        // Selected range details
        selectedRange?.let { range ->
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
                        text = "${range.range} Rep Range",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Volume: ${WeightFormatter.formatWeight(range.volume)}kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${range.sets} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            text = "Avg: ${WeightFormatter.formatWeight(range.avgWeight)}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

private object RepRangeChartConstants {
    const val ANIMATION_DURATION_MS = 800
}

@Composable
private fun RepRangeChartCanvas(
    distributionData: List<RepRangeDistribution>,
    selectedRange: RepRangeDistribution?,
    onRangeSelected: (RepRangeDistribution) -> Unit,
) {
    var clickPosition by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val barColors = ChartTheme.repRangeColors
    val onSurfaceColor = ChartTheme.axisLabelColor()
    val surfaceVariantColor = ChartTheme.gridLineColor()

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(distributionData) {
        animationProgress.animateTo(1f, animationSpec = tween(RepRangeChartConstants.ANIMATION_DURATION_MS))
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

        if (distributionData.isEmpty()) return@Canvas

        // Calculate bounds - use percentages for better visualization
        val totalVolume = distributionData.sumOf { it.volume.toDouble() }.toFloat()
        val maxPercentage =
            if (totalVolume > 0f) {
                (distributionData.maxOf { it.volume } / totalVolume * 100) * 1.05f
            } else {
                100f
            }

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight - (i.toFloat() / gridLines * chartHeight)
            val percentage = i.toFloat() / gridLines * maxPercentage

            // Grid line
            drawLine(
                color = surfaceVariantColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
            )

            // Percentage label
            val percentageText = "${percentage.toInt()}%"
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
        val barWidth = (chartWidth - (distributionData.size - 1) * barSpacing) / distributionData.size

        distributionData.forEachIndexed { index, range ->
            val x = leftPadding + index * (barWidth + barSpacing)
            val rangePercentage = if (totalVolume > 0f) (range.volume / totalVolume * 100) else 0f
            val barHeight = (rangePercentage / maxPercentage * chartHeight) * animationProgress.value
            val y = topPadding + chartHeight - barHeight

            val isSelected = range == selectedRange

            // Use different colors for different rep ranges
            val barColor = barColors.getOrElse(index) { barColors.last() }

            // Bar
            drawRect(
                color = barColor.copy(alpha = if (isSelected) 1f else 0.9f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
            )

            // Percentage label on bar
            if (barHeight > 20.dp.toPx()) { // Only show if bar is tall enough
                val percentageText = "${rangePercentage.toInt()}%"
                val percentageTextResult =
                    textMeasurer.measure(
                        text = percentageText,
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                color = ChartTheme.onColoredBackground,
                                fontWeight = FontWeight.Bold,
                            ),
                    )

                drawText(
                    textLayoutResult = percentageTextResult,
                    topLeft =
                        Offset(
                            x + barWidth / 2 - percentageTextResult.size.width / 2,
                            y + 8.dp.toPx(),
                        ),
                )
            }

            // Handle click detection
            clickPosition?.let { click ->
                if (click.x >= x &&
                    click.x <= x + barWidth &&
                    click.y >= y &&
                    click.y <= y + barHeight
                ) {
                    onRangeSelected(range)
                    clickPosition = null
                }
            }

            // X-axis label (rep range)
            val textLayoutResult =
                textMeasurer.measure(
                    text = range.range,
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
                        x + barWidth / 2 - textLayoutResult.size.width / 2,
                        size.height - bottomPadding + 8.dp.toPx(),
                    ),
            )
        }

        // Clear click position after processing
        clickPosition = null
    }
}
