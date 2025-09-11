package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CompactRestTimer(
    seconds: Int,
    initialSeconds: Int,
    onSkip: () -> Unit,
    onPresetSelected: (Int) -> Unit,
    onAdjustTime: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        // Background
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        )

        // Progress indicator at top
        LinearProgressIndicator(
            progress = {
                if (initialSeconds > 0) {
                    (seconds.toFloat() / initialSeconds).coerceIn(0f, 1f)
                } else {
                    0f
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Timer display
            Text(
                text = formatTime(seconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(50.dp),
                color = if (seconds == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decrease time
                IconButton(
                    onClick = { onAdjustTime(-15) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease 15s",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Skip button (text only, no pill)
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Increase time
                IconButton(
                    onClick = { onAdjustTime(15) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase 15s",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Preset buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onPresetSelected(90) },
                    modifier = Modifier.height(40.dp),
                ) {
                    Text("90s", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { onPresetSelected(180) },
                    modifier = Modifier.height(40.dp),
                ) {
                    Text("3m", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
