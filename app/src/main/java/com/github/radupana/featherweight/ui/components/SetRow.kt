package com.github.radupana.featherweight.ui.components

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
import androidx.compose.ui.focus.onFocusChanged
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
    onUpdateSet: ((Int, Float, Float?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Simplified state management - use set ID as key to prevent resets
    var isEditingReps by remember(set.id) { mutableStateOf(false) }
    var isEditingWeight by remember(set.id) { mutableStateOf(false) }
    var isEditingRpe by remember(set.id) { mutableStateOf(false) }

    // Flags to prevent auto-save on initial focus
    var hasGainedFocusReps by remember(set.id) { mutableStateOf(false) }
    var hasGainedFocusWeight by remember(set.id) { mutableStateOf(false) }
    var hasGainedFocusRpe by remember(set.id) { mutableStateOf(false) }

    // Text state that updates with set values
    var repsText by remember(set.id, set.reps) {
        mutableStateOf(if (set.reps > 0) set.reps.toString() else "")
    }
    var weightText by remember(set.id, set.weight) {
        mutableStateOf(if (set.weight > 0) set.weight.toString() else "")
    }
    var rpeText by remember(set.id, set.rpe) {
        mutableStateOf(set.rpe?.toString() ?: "")
    }

    val focusManager = LocalFocusManager.current

    // Save function
    fun saveReps() {
        val reps = repsText.toIntOrNull() ?: 0
        val weight = set.weight
        val rpe = set.rpe
        println("💾 SAVING REPS: reps=$reps, weight=$weight, rpe=$rpe")
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingReps = false
        println("💾 SAVED REPS: isEditingReps=$isEditingReps")
    }

    fun saveWeight() {
        val reps = set.reps
        val weight = weightText.toFloatOrNull() ?: 0f
        val rpe = set.rpe
        println("💾 SAVING WEIGHT: reps=$reps, weight=$weight, rpe=$rpe")
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingWeight = false
        println("💾 SAVED WEIGHT: isEditingWeight=$isEditingWeight")
    }

    fun saveRpe() {
        val reps = set.reps
        val weight = set.weight
        val rpe = rpeText.toFloatOrNull()
        println("💾 SAVING RPE: reps=$reps, weight=$weight, rpe=$rpe")
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingRpe = false
        println("💾 SAVED RPE: isEditingRpe=$isEditingRpe")
    }

    val bgColor =
        if (set.isCompleted) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
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

            // Reps - Simplified inline editing
            Box(
                modifier = Modifier.weight(0.18f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingReps && onUpdateSet != null) {
                    println("📝 EDITING REPS: Showing OutlinedTextField, value='$repsText'")
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { newValue ->
                            // Only allow numbers
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                repsText = newValue
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    println("⌨️ KEYBOARD DONE pressed for reps")
                                    saveReps()
                                    focusManager.clearFocus()
                                },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                            ),
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(40.dp)
                                .onFocusChanged { focusState ->
                                    println("🎯 REPS Focus changed: ${focusState.isFocused}, hasGainedFocus: $hasGainedFocusReps")
                                    if (focusState.isFocused) {
                                        hasGainedFocusReps = true
                                    } else if (!focusState.isFocused && hasGainedFocusReps && isEditingReps) {
                                        println("🔄 Auto-saving reps on focus lost")
                                        saveReps()
                                    }
                                },
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                    )
                } else {
                    Text(
                        if (set.reps > 0) "${set.reps}" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier =
                            if (onUpdateSet != null) {
                                Modifier.clickable {
                                    println("🔥 CLICK REPS: set.id=${set.id}, current reps=${set.reps}")
                                    repsText = if (set.reps > 0) set.reps.toString() else ""
                                    isEditingReps = true
                                    println("🔥 AFTER CLICK: isEditingReps=$isEditingReps, repsText='$repsText'")
                                }
                            } else {
                                Modifier
                            },
                    )
                }
            }

            // Weight - Simplified inline editing
            Box(
                modifier = Modifier.weight(0.22f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingWeight && onUpdateSet != null) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newValue ->
                            // Only allow numbers and one decimal point
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                weightText = newValue
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    println("⌨️ KEYBOARD DONE pressed for weight")
                                    saveWeight()
                                    focusManager.clearFocus()
                                },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                            ),
                        modifier =
                            Modifier
                                .width(80.dp)
                                .height(40.dp)
                                .onFocusChanged { focusState ->
                                    println("🎯 WEIGHT Focus changed: ${focusState.isFocused}, hasGainedFocus: $hasGainedFocusWeight")
                                    if (focusState.isFocused) {
                                        hasGainedFocusWeight = true
                                    } else if (!focusState.isFocused && hasGainedFocusWeight && isEditingWeight) {
                                        println("🔄 Auto-saving weight on focus lost")
                                        saveWeight()
                                    }
                                },
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                    )
                } else {
                    Text(
                        if (set.weight > 0) "${set.weight}kg" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier =
                            if (onUpdateSet != null) {
                                Modifier.clickable {
                                    weightText = if (set.weight > 0) set.weight.toString() else ""
                                    isEditingWeight = true
                                }
                            } else {
                                Modifier
                            },
                    )
                }
            }

            // RPE - Simplified inline editing
            Box(
                modifier = Modifier.weight(0.15f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingRpe && onUpdateSet != null) {
                    OutlinedTextField(
                        value = rpeText,
                        onValueChange = { newValue ->
                            // Only allow numbers 1-10 with one decimal
                            if (newValue.isEmpty() || newValue.matches(Regex("^(10(\\.0)?|[1-9](\\.\\d)?|\\d*\\.?\\d*)$"))) {
                                rpeText = newValue
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    println("⌨️ KEYBOARD DONE pressed for RPE")
                                    saveRpe()
                                    focusManager.clearFocus()
                                },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                            ),
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(40.dp)
                                .onFocusChanged { focusState ->
                                    println("🎯 RPE Focus changed: ${focusState.isFocused}, hasGainedFocus: $hasGainedFocusRpe")
                                    if (focusState.isFocused) {
                                        hasGainedFocusRpe = true
                                    } else if (!focusState.isFocused && hasGainedFocusRpe && isEditingRpe) {
                                        println("🔄 Auto-saving RPE on focus lost")
                                        saveRpe()
                                    }
                                },
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                    )
                } else {
                    Text(
                        set.rpe?.let { "$it" } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier =
                            if (onUpdateSet != null) {
                                Modifier.clickable {
                                    rpeText = set.rpe?.toString() ?: ""
                                    isEditingRpe = true
                                }
                            } else {
                                Modifier
                            },
                    )
                }
            }

            // Completion checkbox
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = onToggleCompleted,
                modifier = Modifier.weight(0.15f),
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.tertiary,
                    ),
            )

            // Delete button only
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

    // Debug current state
    println("🎯 SetRow state: set.id=${set.id}, isEditingReps=$isEditingReps, isEditingWeight=$isEditingWeight, isEditingRpe=$isEditingRpe")
}
