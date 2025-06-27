package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.domain.RestTimerState
import kotlin.time.Duration.Companion.seconds

@Composable
fun RestTimerPill(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    
    AnimatedVisibility(
        visible = timerState.isActive,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(24.dp),
            color = if (isExpanded) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            },
            border = if (isExpanded) {
                BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            } else null,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                if (isExpanded) {
                    ExpandedTimerControls(
                        timerState = timerState,
                        onAddTime = onAddTime,
                        onSubtractTime = onSubtractTime,
                        onSkip = onSkip,
                        onCollapse = { isExpanded = false }
                    )
                } else {
                    CollapsedTimerPill(
                        timerState = timerState
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedTimerPill(
    timerState: RestTimerState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⏱️",
                style = MaterialTheme.typography.titleMedium
            )
            Column {
                Text(
                    text = formatTime(timerState.remainingTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (timerState.isFinished) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (timerState.suggestion != null) {
                    Text(
                        text = timerState.suggestion!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Progress indicator
        LinearProgressIndicator(
            progress = { timerState.progress },
            modifier = Modifier
                .width(80.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (timerState.isFinished) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ExpandedTimerControls(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with exercise name and close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timerState.exerciseName ?: "Rest Timer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Large time display
        Text(
            text = formatTime(timerState.remainingTime),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (timerState.isFinished) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        
        // Progress bar
        LinearProgressIndicator(
            progress = { timerState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (timerState.isFinished) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalIconButton(
                onClick = onSubtractTime,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Remove 30s")
            }
            
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Skip Rest")
            }
            
            FilledTonalIconButton(
                onClick = onAddTime,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add 30s")
            }
        }
    }
}

@Composable
fun CompactRestTimer(
    timerState: RestTimerState,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = timerState.isActive,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = formatTime(timerState.remainingTime),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (timerState.isFinished) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (timerState.exerciseName != null) {
                        val displayText = if (timerState.suggestion != null) {
                            "• ${timerState.exerciseName} (${timerState.suggestion})"
                        } else {
                            "• ${timerState.exerciseName}"
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private fun formatTime(duration: kotlin.time.Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}