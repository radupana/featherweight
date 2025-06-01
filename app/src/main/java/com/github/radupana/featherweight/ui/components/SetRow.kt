package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.SetLog

@Composable
fun SetRow(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateSet: ((Int, Float, Float?) -> Unit)? = null, // New callback for inline updates
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Inline editing state
    var isEditingReps by remember { mutableStateOf(false) }
    var isEditingWeight by remember { mutableStateOf(false) }
    var isEditingRpe by remember { mutableStateOf(false) }

    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var weightText by remember { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var rpeText by remember { mutableStateOf(set.rpe?.toString() ?: "") }

    val focusManager = LocalFocusManager.current
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    // Helper function to save changes
    fun saveChanges() {
        val reps = repsText.toIntOrNull() ?: 0
        val weight = weightText.toFloatOrNull() ?: 0f
        val rpe = rpeText.toFloatOrNull()
        onUpdateSet?.invoke(reps, weight, rpe)
    }

    val bgColor = if (set.isCompleted) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (set.isCompleted) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Set number
            Text(
                "${set.setOrder + 1}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.12f)
            )

            // Reps - Inline editable
            Box(
                modifier = Modifier.weight(0.18f),
                contentAlignment = Alignment.Center
            ) {
                if (isEditingReps && onUpdateSet != null) {
                    BasicTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                isEditingReps = false
                                isEditingWeight = true
                                // Use LaunchedEffect to safely request focus
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .focusRequester(repsFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && isEditingReps) {
                                    isEditingReps = false
                                    saveChanges()
                                }
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        if (set.reps > 0) "${set.reps}" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = if (onUpdateSet != null) {
                            Modifier.clickable {
                                println("🔥 Tapping reps for set ${set.id}, onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}")
                                repsText = if (set.reps > 0) set.reps.toString() else ""
                                isEditingReps = true
                                println("🔥 Set isEditingReps = true, repsText = '$repsText'")
                            }
                        } else {
                            println("🔥 onUpdateSet is NULL for set ${set.id}")
                            Modifier
                        }
                    )
                }
            }

            // Weight - Inline editable
            Box(
                modifier = Modifier.weight(0.22f),
                contentAlignment = Alignment.Center
            ) {
                if (isEditingWeight && onUpdateSet != null) {
                    BasicTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                isEditingWeight = false
                                isEditingRpe = true
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .focusRequester(weightFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && isEditingWeight) {
                                    isEditingWeight = false
                                    saveChanges()
                                }
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        if (set.weight > 0) "${set.weight}kg" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = if (onUpdateSet != null) {
                            Modifier.clickable {
                                weightText = if (set.weight > 0) set.weight.toString() else ""
                                isEditingWeight = true
                            }
                        } else Modifier
                    )
                }
            }

            // RPE - Inline editable
            Box(
                modifier = Modifier.weight(0.15f),
                contentAlignment = Alignment.Center
            ) {
                if (isEditingRpe && onUpdateSet != null) {
                    BasicTextField(
                        value = rpeText,
                        onValueChange = { rpeText = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isEditingRpe = false
                                saveChanges()
                                focusManager.clearFocus()
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .focusRequester(rpeFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && isEditingRpe) {
                                    isEditingRpe = false
                                    saveChanges()
                                }
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        set.rpe?.let { "$it" } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = if (onUpdateSet != null) {
                            Modifier.clickable {
                                rpeText = set.rpe?.toString() ?: ""
                                isEditingRpe = true
                            }
                        } else Modifier
                    )
                }
            }

            // Completion checkbox
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = onToggleCompleted,
                modifier = Modifier.weight(0.15f),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary
                )
            )

            // Delete button only (edit is now inline)
            Box(
                modifier = Modifier.weight(0.18f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Set",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Handle focus requests safely using LaunchedEffect
    LaunchedEffect(isEditingReps) {
        if (isEditingReps) {
            repsFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isEditingWeight) {
        if (isEditingWeight) {
            weightFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isEditingRpe) {
        if (isEditingRpe) {
            rpeFocusRequester.requestFocus()
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}