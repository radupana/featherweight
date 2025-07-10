package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import kotlin.math.roundToInt

@Composable
fun OneRMUpdateDialog(
    pendingUpdates: List<PendingOneRMUpdate>,
    onApply: (PendingOneRMUpdate) -> Unit,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Progress",
            )
        },
        title = {
            Text(
                "New Personal Records Detected!",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pendingUpdates) { update ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                        ) {
                            Text(
                                text = update.exerciseName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Current 1RM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text =
                                            if (update.currentMax != null) {
                                                "${update.currentMax.roundToInt()}kg"
                                            } else {
                                                "Not set"
                                            },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Increase",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )

                                Column(
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Text(
                                        text = "New 1RM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${update.suggestedMax.roundToInt()}kg",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Based on: ${update.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Text(
                                text = "Confidence: ${(update.confidence * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FilledTonalButton(
                                onClick = { onApply(update) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Update ${update.exerciseName} 1RM")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSkip) {
                Text("Skip All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
