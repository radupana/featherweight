package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseProgressionStatus
import com.github.radupana.featherweight.data.ProgressionAction

/**
 * Shows deload notification when an exercise needs to be deloaded
 */
@Composable
fun DeloadNotificationCard(
    progressionStatus: ExerciseProgressionStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (progressionStatus.suggestedAction != ProgressionAction.DELOAD) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.TrendingDown,
                contentDescription = "Deload",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Deload Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "${progressionStatus.exerciseName}: ${progressionStatus.consecutiveFailures} consecutive failures",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Weight will be reduced by 15% this workout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }

            TextButton(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
            ) {
                Text("Got it")
            }
        }
    }
}

/**
 * Small inline deload indicator for exercise cards
 */
@Composable
fun DeloadIndicator(
    isDeload: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isDeload,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = "Deload",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "DELOAD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Shows progression status summary with deload history
 */
@Composable
fun ProgressionStatusCard(
    status: ExerciseProgressionStatus,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (status.suggestedAction) {
                        ProgressionAction.DELOAD -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ProgressionAction.PROGRESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = status.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                DeloadIndicator(isDeload = status.isInDeloadCycle)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Current Weight",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${status.currentWeight}kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Failures",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${status.consecutiveFailures}/3",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color =
                            when (status.consecutiveFailures) {
                                0 -> MaterialTheme.colorScheme.primary
                                1 -> MaterialTheme.colorScheme.onSurface
                                2 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.error
                            },
                    )
                }

                if (status.totalDeloads > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Deloads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = status.totalDeloads.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            when (status.suggestedAction) {
                ProgressionAction.DELOAD -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Deload required - weight will be reduced by 15%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ProgressionAction.PROGRESS -> {
                    Text(
                        text = "Ready to progress! Weight will increase next workout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                ProgressionAction.MAINTAIN -> {
                    Text(
                        text = "Maintain current weight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {}
            }
        }
    }
}
