package com.github.radupana.featherweight.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Applies a background brush that is a seamless "slice" of a parent's vertical gradient.
 * This makes a composable opaque while perfectly matching a gradient background
 * from a parent, ideal for use in SwipeToDismissBox where content must be opaque
 * to properly reveal the background when swiping.
 *
 * @param parentCoordinates The layout coordinates of the container that defines the gradient bounds.
 * @param parentGradientColors The colors of the parent's vertical gradient.
 */
fun Modifier.seamlessGradient(
    parentCoordinates: LayoutCoordinates?,
    parentGradientColors: List<Color>,
): Modifier =
    composed {
        var childCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        val brush =
            remember(parentCoordinates, childCoordinates, parentGradientColors) {
                val parent = parentCoordinates
                val child = childCoordinates
                if (parent != null && child != null && parentGradientColors.isNotEmpty()) {
                    val parentHeight = parent.size.height.toFloat()
                    if (parentHeight <= 0) return@remember Brush.verticalGradient(parentGradientColors)

                    val parentTop = parent.positionInRoot().y
                    val childTop = child.positionInRoot().y
                    val relativeY = childTop - parentTop

                    Brush.verticalGradient(
                        colors = parentGradientColors,
                        startY = -relativeY,
                        endY = parentHeight - relativeY,
                    )
                } else {
                    Brush.verticalGradient(parentGradientColors)
                }
            }

        this
            .onGloballyPositioned { childCoordinates = it }
            .background(brush)
    }
