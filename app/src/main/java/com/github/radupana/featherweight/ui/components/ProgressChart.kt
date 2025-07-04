package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.radupana.featherweight.service.ProgressDataPoint
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ChartType {
    LINE, BAR, AREA
}

data class ChartData(
    val dataPoints: List<ProgressDataPoint>,
    val title: String,
    val primaryColor: Color,
    val showPRmarkers: Boolean = true
)

@Composable
fun ProgressChart(
    data: ChartData,
    type: ChartType = ChartType.LINE,
    height: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    if (data.dataPoints.isEmpty()) {
        EmptyChartState(
            title = data.title,
            height = height,
            modifier = modifier
        )
        return
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Chart title
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                when (type) {
                    ChartType.LINE -> drawLineChart(data, density.density)
                    ChartType.BAR -> drawBarChart(data, density.density)
                    ChartType.AREA -> drawAreaChart(data, density.density)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Chart info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstPoint = data.dataPoints.first()
                val lastPoint = data.dataPoints.last()
                val progressPercentage = if (firstPoint.weight > 0) {
                    ((lastPoint.weight - firstPoint.weight) / firstPoint.weight) * 100
                } else 0f
                
                Text(
                    text = "${data.dataPoints.size} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val progressColor = when {
                    progressPercentage > 0 -> Color(0xFF4CAF50)
                    progressPercentage < 0 -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Text(
                    text = "${if (progressPercentage >= 0) "+" else ""}${String.format("%.1f", progressPercentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
private fun EmptyChartState(
    title: String,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(height),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawLineChart(data: ChartData, density: Float) {
    val points = data.dataPoints
    if (points.size < 2) return
    
    val minWeight = points.minOf { it.weight }
    val maxWeight = points.maxOf { it.weight }
    val weightRange = maxWeight - minWeight
    
    // Add padding to prevent points from touching edges
    val paddingPercentage = 0.1f
    val adjustedMin = minWeight - (weightRange * paddingPercentage)
    val adjustedMax = maxWeight + (weightRange * paddingPercentage)
    val adjustedRange = adjustedMax - adjustedMin
    
    val width = size.width
    val height = size.height
    
    // Convert data points to screen coordinates
    val screenPoints = points.mapIndexed { index, point ->
        val x = (index.toFloat() / (points.size - 1)) * width
        val y = height - ((point.weight - adjustedMin) / adjustedRange) * height
        Offset(x, y)
    }
    
    // Draw the line
    val path = Path().apply {
        moveTo(screenPoints.first().x, screenPoints.first().y)
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
    }
    
    drawPath(
        path = path,
        color = data.primaryColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
    
    // Draw data points
    screenPoints.forEachIndexed { index, point ->
        val isFirst = index == 0
        val isLast = index == screenPoints.size - 1
        val isPR = data.showPRmarkers && (isFirst || isLast || 
            points[index].weight > points.getOrNull(index - 1)?.weight ?: 0f)
        
        val pointColor = if (isPR) Color(0xFFFFD700) else data.primaryColor
        val pointRadius = if (isPR) 6.dp.toPx() else 4.dp.toPx()
        
        drawCircle(
            color = pointColor,
            radius = pointRadius,
            center = point
        )
        
        // Draw stroke around point
        drawCircle(
            color = Color.White,
            radius = pointRadius,
            center = point,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawBarChart(data: ChartData, density: Float) {
    val points = data.dataPoints
    if (points.isEmpty()) return
    
    val minWeight = points.minOf { it.weight }
    val maxWeight = points.maxOf { it.weight }
    val weightRange = maxWeight - minWeight
    
    // Add padding
    val paddingPercentage = 0.1f
    val adjustedMin = minWeight - (weightRange * paddingPercentage)
    val adjustedMax = maxWeight + (weightRange * paddingPercentage)
    val adjustedRange = adjustedMax - adjustedMin
    
    val width = size.width
    val height = size.height
    val barWidth = width / points.size * 0.7f // Leave some space between bars
    val barSpacing = width / points.size * 0.3f
    
    points.forEachIndexed { index, point ->
        val barHeight = ((point.weight - adjustedMin) / adjustedRange) * height
        val x = index * (barWidth + barSpacing) + barSpacing / 2
        val y = height - barHeight
        
        drawRect(
            color = data.primaryColor,
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawAreaChart(data: ChartData, density: Float) {
    val points = data.dataPoints
    if (points.size < 2) return
    
    val minWeight = points.minOf { it.weight }
    val maxWeight = points.maxOf { it.weight }
    val weightRange = maxWeight - minWeight
    
    // Add padding
    val paddingPercentage = 0.1f
    val adjustedMin = minWeight - (weightRange * paddingPercentage)
    val adjustedMax = maxWeight + (weightRange * paddingPercentage)
    val adjustedRange = adjustedMax - adjustedMin
    
    val width = size.width
    val height = size.height
    
    // Convert data points to screen coordinates
    val screenPoints = points.mapIndexed { index, point ->
        val x = (index.toFloat() / (points.size - 1)) * width
        val y = height - ((point.weight - adjustedMin) / adjustedRange) * height
        Offset(x, y)
    }
    
    // Create area path
    val areaPath = Path().apply {
        moveTo(screenPoints.first().x, height) // Start at bottom
        lineTo(screenPoints.first().x, screenPoints.first().y)
        
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
        
        lineTo(screenPoints.last().x, height) // End at bottom
        close()
    }
    
    // Draw filled area with gradient
    val gradient = Brush.verticalGradient(
        colors = listOf(
            data.primaryColor.copy(alpha = 0.3f),
            data.primaryColor.copy(alpha = 0.1f)
        )
    )
    
    drawPath(
        path = areaPath,
        brush = gradient
    )
    
    // Draw the line on top
    val linePath = Path().apply {
        moveTo(screenPoints.first().x, screenPoints.first().y)
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
    }
    
    drawPath(
        path = linePath,
        color = data.primaryColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

@Composable
fun MiniProgressChart(
    data: List<Float>,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .size(60.dp, 30.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)
                )
        )
        return
    }
    
    Canvas(
        modifier = modifier.size(60.dp, 30.dp)
    ) {
        val minValue = data.minOrNull() ?: 0f
        val maxValue = data.maxOrNull() ?: 0f
        val range = maxValue - minValue
        
        if (range == 0f) return@Canvas
        
        val width = size.width
        val height = size.height
        
        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - ((value - minValue) / range) * height
            Offset(x, y)
        }
        
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}