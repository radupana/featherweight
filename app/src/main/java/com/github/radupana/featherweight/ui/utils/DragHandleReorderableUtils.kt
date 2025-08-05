package com.github.radupana.featherweight.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun DragHandle(
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .size(width = 32.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                ).pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { _ ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            isDragging = true
                            isHovered = true
                            onDragStart()
                        },
                        onDragEnd = {
                            isDragging = false
                            isHovered = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging = false
                            isHovered = false
                            onDragEnd()
                        },
                        onDrag = { _, dragAmount ->
                            onDrag(dragAmount.y)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "Long press and drag to reorder",
            tint =
                when {
                    isDragging -> MaterialTheme.colorScheme.primary
                    isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
            modifier = Modifier.size(20.dp),
        )
    }
}
