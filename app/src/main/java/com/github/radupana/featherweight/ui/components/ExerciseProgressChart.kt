package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ExerciseDataPoint(
    val date: LocalDate,
    val weight: Float,
    val reps: Int,
    val isPR: Boolean = false
)

enum class ChartPeriod(val label: String, val days: Long) {
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    ALL("All", Long.MAX_VALUE)
}

@Composable
fun ExerciseProgressChart(
    dataPoints: List<ExerciseDataPoint>,
    modifier: Modifier = Modifier,
    onDataPointClick: (ExerciseDataPoint) -> Unit = {}
) {
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.THREE_MONTHS) }
    var selectedPoint by remember { mutableStateOf<ExerciseDataPoint?>(null) }
    
    // Filter data based on selected period
    val filteredData = remember(dataPoints, selectedPeriod) {
        if (selectedPeriod == ChartPeriod.ALL) {
            dataPoints
        } else {
            val cutoffDate = LocalDate.now().minusDays(selectedPeriod.days)
            dataPoints.filter { it.date >= cutoffDate }
        }
    }

    Column(modifier = modifier) {
        // Period selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChartPeriod.values().forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { 
                        selectedPeriod = period
                        selectedPoint = null
                    },
                    label = { 
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                )
            }
        }

        // Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    ChartCanvas(
                        dataPoints = filteredData,
                        selectedPoint = selectedPoint,
                        onPointSelected = { point ->
                            selectedPoint = point
                            onDataPointClick(point)
                        }
                    )
                }
            }
        }

        // Selected point details
        selectedPoint?.let { point ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = point.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${WeightFormatter.formatWeightWithUnit(point.weight)} × ${point.reps} reps",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (point.isPR) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PR",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
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
    onPointSelected: (ExerciseDataPoint) -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val prColor = Color(0xFFFFD700)
    
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Handle clicks in onDraw */ }
    ) {
        val chartWidth = size.width
        val chartHeight = size.height - 40.dp.toPx() // Leave space for labels
        val padding = 40.dp.toPx()
        
        if (dataPoints.isEmpty()) return@Canvas

        // Calculate bounds
        val minWeight = dataPoints.minOf { it.weight } * 0.95f
        val maxWeight = dataPoints.maxOf { it.weight } * 1.05f
        val weightRange = maxWeight - minWeight
        
        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = chartHeight - (i.toFloat() / gridLines * chartHeight)
            val weight = minWeight + (i.toFloat() / gridLines * weightRange)
            
            // Grid line
            drawLine(
                color = surfaceVariantColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            
            // Weight label
            val weightText = WeightFormatter.formatWeightWithUnit(weight)
            val textLayoutResult = textMeasurer.measure(
                text = weightText,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(-textLayoutResult.size.width - 8.dp.toPx(), y - textLayoutResult.size.height / 2)
            )
        }

        // Calculate point positions
        val points = dataPoints.mapIndexed { index, dataPoint ->
            val x = (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth
            val y = chartHeight - ((dataPoint.weight - minWeight) / weightRange * chartHeight)
            Triple(x, y, dataPoint)
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
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw gradient fill under the line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(chartWidth, chartHeight)
        fillPath.lineTo(0f, chartHeight)
        fillPath.close()
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.3f * animationProgress.value),
                    primaryColor.copy(alpha = 0f)
                ),
                startY = 0f,
                endY = chartHeight
            )
        )

        // Draw points
        points.forEach { (x, y, dataPoint) ->
            val animatedY = chartHeight - (chartHeight - y) * animationProgress.value
            val isSelected = dataPoint == selectedPoint
            
            // Point circle
            drawCircle(
                color = if (dataPoint.isPR) prColor else primaryColor,
                radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                center = Offset(x, animatedY)
            )
            
            // White center for better visibility
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 4.dp.toPx() else 2.dp.toPx(),
                center = Offset(x, animatedY)
            )
            
            // PR marker
            if (dataPoint.isPR) {
                val prText = "PR"
                val prTextResult = textMeasurer.measure(
                    text = prText,
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = prColor
                    )
                )
                drawText(
                    textLayoutResult = prTextResult,
                    topLeft = Offset(x - prTextResult.size.width / 2, animatedY - 20.dp.toPx())
                )
            }
        }
    }
}