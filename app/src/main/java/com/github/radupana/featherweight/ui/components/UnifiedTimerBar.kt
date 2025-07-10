package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.domain.RestTimerState
import kotlin.time.Duration

@Composable
fun UnifiedTimerBar(
    workoutElapsed: Long,
    workoutActive: Boolean,
    restTimerState: RestTimerState,
    onRestAddTime: () -> Unit,
    onRestSubtractTime: () -> Unit,
    onRestSkip: () -> Unit,
    onRestTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show the bar if either timer is active
    val shouldShow = workoutActive || restTimerState.isActive

    AnimatedVisibility(
        visible = shouldShow,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                        ),
                                ),
                        ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Workout Timer Section (Left Side)
                    if (workoutActive) {
                        WorkoutTimerSection(
                            elapsedSeconds = workoutElapsed,
                            modifier = Modifier.weight(1f),
                        )

                        // Divider if both timers are active
                        if (restTimerState.isActive) {
                            VerticalDivider(
                                modifier =
                                    Modifier
                                        .height(24.dp)
                                        .padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            )
                        }
                    }

                    // Rest Timer Section (Right Side)
                    if (restTimerState.isActive) {
                        RestTimerSection(
                            timerState = restTimerState,
                            onAddTime = onRestAddTime,
                            onSubtractTime = onRestSubtractTime,
                            onSkip = onRestSkip,
                            onTogglePause = onRestTogglePause,
                            modifier = if (workoutActive) Modifier else Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTimerSection(
    elapsedSeconds: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = "🏋️",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formatWorkoutTime(elapsedSeconds),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RestTimerSection(
    timerState: RestTimerState,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Timer display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "⏱️",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatRestTime(timerState.remainingTime),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (timerState.isFinished) {
                        MaterialTheme.colorScheme.error
                    } else if (timerState.isPaused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Compact controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pause/Resume button
            FilledIconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(24.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor =
                            if (timerState.isPaused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                    ),
            ) {
                Icon(
                    if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(12.dp),
                )
            }

            FilledTonalIconButton(
                onClick = onSubtractTime,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Remove 15s",
                    modifier = Modifier.size(12.dp),
                )
            }

            FilledTonalIconButton(
                onClick = onAddTime,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add 15s",
                    modifier = Modifier.size(12.dp),
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
) {
    Box(
        modifier =
            modifier
                .width(1.dp)
                .background(color),
    )
}

private fun formatWorkoutTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private fun formatRestTime(duration: kotlin.time.Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
