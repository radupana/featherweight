package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MiniProgressChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val maxValue = data.maxOrNull() ?: 0f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue

        // Ensure we have a valid range
        val normalizedRange = if (range > 0) range else 1f

        // Calculate points
        val points =
            data.mapIndexed { index, value ->
                val x =
                    if (data.size > 1) {
                        index * width / (data.size - 1)
                    } else {
                        width / 2
                    }
                val normalizedValue = (value - minValue) / normalizedRange
                val y = height - (normalizedValue * height * 0.8f) - height * 0.1f
                Offset(x, y)
            }

        // Draw the line
        if (points.size > 1) {
            val path =
                Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )

            // Draw gradient fill under the line
            val fillPath =
                Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }

            drawPath(
                path = fillPath,
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.0f),
                            ),
                        startY = 0f,
                        endY = height,
                    ),
            )
        } else if (points.size == 1) {
            // Draw a single point
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = points.first(),
            )
        }
    }
}
