package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var remainingUpdates by remember { mutableStateOf(pendingUpdates) }

    AlertDialog(
        onDismissRequest = {}, // Prevent dismissal by tapping outside
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
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
                items(remainingUpdates) { update ->
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
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        onApply(update)
                                        remainingUpdates = remainingUpdates - update
                                        if (remainingUpdates.isEmpty()) {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                ) {
                                    Text("Update 1RM")
                                }

                                OutlinedButton(
                                    onClick = {
                                        remainingUpdates = remainingUpdates - update
                                        if (remainingUpdates.isEmpty()) {
                                            onSkip()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Skip")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        pendingUpdates.forEach { update ->
                            onApply(update)
                        }
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("Update All")
                }

                OutlinedButton(
                    onClick = onSkip,
                ) {
                    Text("Skip All")
                }
            }
        },
    )
}
