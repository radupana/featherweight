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
    canMarkComplete: Boolean = true,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Simplified state management - use set ID as key to prevent resets
    var isEditingReps by remember(set.id) { mutableStateOf(false) }
    var isEditingWeight by remember(set.id) { mutableStateOf(false) }
    var isEditingRpe by remember(set.id) { mutableStateOf(false) }

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

    // Focus requesters
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Save functions
    fun saveReps() {
        Log.d("SetRow", "Set ${set.id}: saveReps() called - repsText: '$repsText'")
        val reps = repsText.toIntOrNull() ?: 0
        val weight = set.weight
        val rpe = set.rpe
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingReps = false
        Log.d("SetRow", "Set ${set.id}: saveReps() completed - isEditingReps now false")
    }

    fun saveWeight() {
        Log.d("SetRow", "Set ${set.id}: saveWeight() called - weightText: '$weightText'")
        val reps = set.reps
        val weight = weightText.toFloatOrNull() ?: 0f
        val rpe = set.rpe
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingWeight = false
        Log.d("SetRow", "Set ${set.id}: saveWeight() completed - isEditingWeight now false")
    }

    fun saveRpe() {
        Log.d("SetRow", "Set ${set.id}: saveRpe() called - rpeText: '$rpeText'")
        val reps = set.reps
        val weight = set.weight
        val rpe = rpeText.toFloatOrNull()
        onUpdateSet?.invoke(reps, weight, rpe)
        isEditingRpe = false
        Log.d("SetRow", "Set ${set.id}: saveRpe() completed - isEditingRpe now false")
    }

    // Track if we should ignore the next focus change (to prevent immediate save after click)
    var ignoreNextRepsFocusChange by remember { mutableStateOf(false) }
    var ignoreNextWeightFocusChange by remember { mutableStateOf(false) }
    var ignoreNextRpeFocusChange by remember { mutableStateOf(false) }

    // Request focus immediately after composition using SideEffect
    SideEffect {
        if (isEditingReps) {
            Log.d("SetRow", "Set ${set.id}: SideEffect - requesting reps focus")
            repsFocusRequester.requestFocus()
        }
        if (isEditingWeight) {
            Log.d("SetRow", "Set ${set.id}: SideEffect - requesting weight focus")
            weightFocusRequester.requestFocus()
        }
        if (isEditingRpe) {
            Log.d("SetRow", "Set ${set.id}: SideEffect - requesting RPE focus")
            rpeFocusRequester.requestFocus()
        }
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

            // Reps - FIXED: Better focus handling
            Box(
                modifier = Modifier.weight(0.18f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingReps && onUpdateSet != null) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { newValue ->
                            // Only allow numbers
                            if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 3)) {
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
                                onDone = { saveReps() },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            ),
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(48.dp)
                                .focusRequester(repsFocusRequester)
                                .onFocusChanged { focusState ->
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: Reps focus changed - isFocused: ${focusState.isFocused}, isEditing: $isEditingReps, ignoreNext: $ignoreNextRepsFocusChange",
                                    )

                                    if (ignoreNextRepsFocusChange) {
                                        Log.d("SetRow", "Set ${set.id}: Ignoring reps focus change")
                                        ignoreNextRepsFocusChange = false
                                        return@onFocusChanged
                                    }

                                    // Save when losing focus while in edit mode
                                    if (!focusState.isFocused && isEditingReps) {
                                        Log.d("SetRow", "Set ${set.id}: Reps lost focus - saving")
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
                    Surface(
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(40.dp)
                                .clickable {
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: Reps surface clicked - onUpdateSet is ${if (onUpdateSet != null) "not null" else "null"}",
                                    )
                                    if (onUpdateSet != null) {
                                        repsText = if (set.reps > 0) set.reps.toString() else ""
                                        Log.d("SetRow", "Set ${set.id}: Setting isEditingReps to true, repsText: '$repsText'")
                                        ignoreNextRepsFocusChange = true
                                        isEditingReps = true
                                    }
                                },
                        color =
                            if (onUpdateSet != null) {
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
            }

            // Weight - FIXED: Better focus handling
            Box(
                modifier = Modifier.weight(0.22f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingWeight && onUpdateSet != null) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newValue ->
                            // Only allow numbers and one decimal point
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 6) {
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
                                onDone = { saveWeight() },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            ),
                        modifier =
                            Modifier
                                .width(90.dp)
                                .height(48.dp)
                                .focusRequester(weightFocusRequester)
                                .onFocusChanged { focusState ->
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: Weight focus changed - isFocused: ${focusState.isFocused}, isEditing: $isEditingWeight, ignoreNext: $ignoreNextWeightFocusChange",
                                    )

                                    if (ignoreNextWeightFocusChange) {
                                        Log.d("SetRow", "Set ${set.id}: Ignoring weight focus change")
                                        ignoreNextWeightFocusChange = false
                                        return@onFocusChanged
                                    }

                                    // Save when losing focus while in edit mode
                                    if (!focusState.isFocused && isEditingWeight) {
                                        Log.d("SetRow", "Set ${set.id}: Weight lost focus - saving")
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
                    Surface(
                        modifier =
                            Modifier
                                .width(90.dp)
                                .height(40.dp)
                                .clickable {
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: Weight surface clicked - onUpdateSet is ${if (onUpdateSet != null) "not null" else "null"}",
                                    )
                                    if (onUpdateSet != null) {
                                        weightText = if (set.weight > 0) set.weight.toString() else ""
                                        Log.d("SetRow", "Set ${set.id}: Setting isEditingWeight to true, weightText: '$weightText'")
                                        ignoreNextWeightFocusChange = true
                                        isEditingWeight = true
                                    }
                                },
                        color =
                            if (onUpdateSet != null) {
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
            }

            // RPE - FIXED: Better focus handling
            Box(
                modifier = Modifier.weight(0.15f),
                contentAlignment = Alignment.Center,
            ) {
                if (isEditingRpe && onUpdateSet != null) {
                    OutlinedTextField(
                        value = rpeText,
                        onValueChange = { newValue ->
                            // Only allow numbers 1-10 with one decimal
                            if (newValue.isEmpty() ||
                                newValue.matches(Regex("^(10(\\.0)?|[1-9](\\.\\d)?|\\d*\\.?\\d*)$")) &&
                                newValue.length <= 4
                            ) {
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
                                onDone = { saveRpe() },
                            ),
                        textStyle =
                            LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            ),
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(48.dp)
                                .focusRequester(rpeFocusRequester)
                                .onFocusChanged { focusState ->
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: RPE focus changed - isFocused: ${focusState.isFocused}, isEditing: $isEditingRpe, ignoreNext: $ignoreNextRpeFocusChange",
                                    )

                                    if (ignoreNextRpeFocusChange) {
                                        Log.d("SetRow", "Set ${set.id}: Ignoring RPE focus change")
                                        ignoreNextRpeFocusChange = false
                                        return@onFocusChanged
                                    }

                                    // Save when losing focus while in edit mode
                                    if (!focusState.isFocused && isEditingRpe) {
                                        Log.d("SetRow", "Set ${set.id}: RPE lost focus - saving")
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
                    Surface(
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(40.dp)
                                .clickable {
                                    Log.d(
                                        "SetRow",
                                        "Set ${set.id}: RPE surface clicked - onUpdateSet is ${if (onUpdateSet != null) "not null" else "null"}",
                                    )
                                    if (onUpdateSet != null) {
                                        rpeText = set.rpe?.toString() ?: ""
                                        Log.d("SetRow", "Set ${set.id}: Setting isEditingRpe to true, rpeText: '$rpeText'")
                                        ignoreNextRpeFocusChange = true
                                        isEditingRpe = true
                                    }
                                },
                        color =
                            if (onUpdateSet != null) {
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
                                set.rpe?.let { "$it" } ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
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
                            checkedColor = MaterialTheme.colorScheme.tertiary,
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
