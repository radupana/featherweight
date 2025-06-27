package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.domain.RestTimerState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun RestTimerPill(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
    onStartPreset: (Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = timerState.isActive,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
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
                // Always show all controls
                AllControlsTimerPill(
                    timerState = timerState,
                    onAddTime = onAddTime,
                    onSubtractTime = onSubtractTime,
                    onSkip = onSkip,
                    onTogglePause = onTogglePause,
                    onStartPreset = onStartPreset
                )
            }
        }
    }
}


@Composable
fun CompactRestTimer(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
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
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer info (compact)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
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
                        } else if (timerState.isPaused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (timerState.suggestion != null) {
                        Text(
                            text = "• ${timerState.suggestion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Control buttons - always visible
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume button
                    FilledIconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (timerState.isPaused) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    FilledTonalIconButton(
                        onClick = onSubtractTime,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove, 
                            contentDescription = "Remove 15s",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    FilledTonalIconButton(
                        onClick = onAddTime,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add 15s",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 50.dp)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllControlsTimerPill(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
    onStartPreset: (Duration) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timer info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "⏱️",
                style = MaterialTheme.typography.titleMedium
            )
            Column {
                Text(
                    text = if (timerState.isPaused) {
                        "${formatTime(timerState.remainingTime)} (Paused)"
                    } else {
                        formatTime(timerState.remainingTime)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (timerState.isFinished) {
                        MaterialTheme.colorScheme.error
                    } else if (timerState.isPaused) {
                        MaterialTheme.colorScheme.primary
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
        
        // Control buttons - always visible
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause/Resume button
            FilledIconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (timerState.isPaused) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            FilledTonalIconButton(
                onClick = onSubtractTime,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Remove, 
                    contentDescription = "Remove 30s",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            FilledTonalIconButton(
                onClick = onAddTime,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add 30s",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 60.dp)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelMedium
                )
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