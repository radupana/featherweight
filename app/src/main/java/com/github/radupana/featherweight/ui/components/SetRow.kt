package com.github.radupana.featherweight.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog


@Composable
fun SetRow(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenModal: () -> Unit = {},
    modifier: Modifier = Modifier,
    canMarkComplete: Boolean = true,
    isReadOnly: Boolean = false,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val bgColor =
        if (set.isCompleted) {
            // Use opaque light green to prevent any background bleed-through issues
            androidx.compose.ui.graphics.Color(0xFFE8F5E9)  // Light green background (opaque)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (set.isCompleted) 2.dp else 0.dp,
    ) {
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
                            if (set.weight > 0) "${set.weight}kg" else "—",
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
                                if (rpe == rpe.toInt().toFloat()) {
                                    rpe.toInt().toString()
                                } else {
                                    rpe.toString()
                                }
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
                            checkedColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),  // Green checkbox
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
