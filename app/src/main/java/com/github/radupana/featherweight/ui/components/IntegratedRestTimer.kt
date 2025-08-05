package com.github.radupana.featherweight.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun IntegratedRestTimer(
    seconds: Int,
    initialSeconds: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSkip: () -> Unit,
    onPresetSelected: (Int) -> Unit,
    onAdjustTime: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Handle vibration when timer completes
    LaunchedEffect(seconds) {
        if (seconds == 0 && initialSeconds > 0) {
            vibrateCompletion(context)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column {
            // Always visible header row
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpanded() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Rest Timer:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTime(seconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Thin progress line in minimized state
            if (!isExpanded && initialSeconds > 0) {
                LinearProgressIndicator(
                    progress = { seconds.toFloat() / initialSeconds },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Progress bar
                    if (initialSeconds > 0) {
                        LinearProgressIndicator(
                            progress = { seconds.toFloat() / initialSeconds },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onPresetSelected(60) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("60s")
                        }
                        OutlinedButton(
                            onClick = { onPresetSelected(120) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("120s")
                        }
                        OutlinedButton(
                            onClick = { onPresetSelected(180) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("180s")
                        }
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustTime(-15) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("-15s")
                        }
                        Button(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Skip")
                        }
                        OutlinedButton(
                            onClick = { onAdjustTime(15) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("+15s")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String =
    if (seconds >= 60) {
        val mins = seconds / 60
        val secs = seconds % 60
        "$mins:%02d".format(secs)
    } else {
        "0:%02d".format(seconds)
    }

private fun vibrateCompletion(context: Context) {
    val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
}
