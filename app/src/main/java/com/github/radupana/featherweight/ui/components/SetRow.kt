package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.util.WeightFormatter

@Composable
fun SetRow(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenModal: () -> Unit = {},
    modifier: Modifier = Modifier,
    canMarkComplete: Boolean = true,
    isReadOnly: Boolean = false,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Green accent stripe for completed sets
            if (set.isCompleted) {
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            ),
                )
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Set number
                Text(
                    "${set.setOrder + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.12f),
                )

                // Reps
                Box(
                    modifier = Modifier.weight(0.18f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .width(80.dp)
                                .height(40.dp)
                                .clickable {
                                    if (!isReadOnly) {
                                        onOpenModal()
                                    }
                                },
                        color =
                            if (!isReadOnly) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                if (set.reps > 0) "${set.reps}" else "—",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Weight
                Box(
                    modifier = Modifier.weight(0.22f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .width(100.dp)
                                .height(40.dp)
                                .clickable {
                                    if (!isReadOnly) {
                                        onOpenModal()
                                    }
                                },
                        color =
                            if (!isReadOnly) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                if (set.weight > 0) WeightFormatter.formatWeightWithUnit(set.weight) else "—",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // RPE
                Box(
                    modifier = Modifier.weight(0.15f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .width(80.dp)
                                .height(40.dp)
                                .clickable {
                                    if (!isReadOnly) {
                                        onOpenModal()
                                    }
                                },
                        color =
                            if (!isReadOnly) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                set.rpe?.let { rpe ->
                                    WeightFormatter.formatDecimal(rpe, 1)
                                } ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Checkbox
                Box(
                    modifier = Modifier.weight(0.15f),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        checked = set.isCompleted,
                        onCheckedChange = { newChecked ->
                            // Only allow checking if the set can be marked complete, or if unchecking
                            if (!newChecked || canMarkComplete) {
                                onToggleCompleted(newChecked)
                            }
                        },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                            ),
                        enabled = canMarkComplete || set.isCompleted, // Enable if can mark complete OR already completed (for unchecking)
                    )
                }

                // Delete button
                Box(
                    modifier = Modifier.weight(0.18f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Set",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Set") },
            text = { Text("Are you sure you want to delete this set? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
