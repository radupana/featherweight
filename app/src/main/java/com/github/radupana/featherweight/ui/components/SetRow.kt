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

// Global editing state to survive recompositions
object SetEditingState {
    private val editingStates = mutableStateMapOf<Long, String>()

    fun isEditing(
        setId: Long,
        field: String,
    ): Boolean = editingStates[setId] == field

    fun setEditing(
        setId: Long,
        field: String,
    ) {
        Log.d("SetEditingState", "Setting editing for set $setId field $field")
        editingStates[setId] = field
    }

    fun clearEditing(setId: Long) {
        Log.d("SetEditingState", "Clearing editing for set $setId")
        editingStates.remove(setId)
    }

    // Atomic switch between fields
    fun switchToField(
        setId: Long,
        newField: String,
    ) {
        Log.d("SetEditingState", "Switching set $setId to field $newField")
        editingStates[setId] = newField
    }
}

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
    Log.d(
        "SetRow",
        "Set ${set.id}: Composing SetRow - onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}, canMarkComplete = $canMarkComplete",
    )
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Use global editing state to survive recompositions
    val isEditingReps = SetEditingState.isEditing(set.id, "reps")
    val isEditingWeight = SetEditingState.isEditing(set.id, "weight")
    val isEditingRpe = SetEditingState.isEditing(set.id, "rpe")
    val isEditingAny = isEditingReps || isEditingWeight || isEditingRpe

    // Debug state changes
    LaunchedEffect(isEditingReps) {
        Log.d("SetRow", "Set ${set.id}: isEditingReps changed to $isEditingReps")
    }
    LaunchedEffect(isEditingWeight) {
        Log.d("SetRow", "Set ${set.id}: isEditingWeight changed to $isEditingWeight")
    }
    LaunchedEffect(isEditingRpe) {
        Log.d("SetRow", "Set ${set.id}: isEditingRpe changed to $isEditingRpe")
    }

    // Text state with cursor position control
    var repsTextFieldValue by remember(set.id) {
        val text = if (set.reps > 0) set.reps.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var weightTextFieldValue by remember(set.id) {
        val text = if (set.weight > 0) set.weight.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var rpeTextFieldValue by remember(set.id) {
        val text = set.rpe?.toString() ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    // Update text when set values change but we're not editing
    LaunchedEffect(set.reps) {
        if (!isEditingReps) {
            val text = if (set.reps > 0) set.reps.toString() else ""
            repsTextFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }
    LaunchedEffect(set.weight) {
        if (!isEditingWeight) {
            val text = if (set.weight > 0) set.weight.toString() else ""
            weightTextFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }
    LaunchedEffect(set.rpe) {
        if (!isEditingRpe) {
            val text = set.rpe?.toString() ?: ""
            rpeTextFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    // Focus requesters
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Save functions
    fun saveCurrentField() {
        when {
            isEditingReps -> {
                val reps = repsTextFieldValue.text.toIntOrNull() ?: 0
                onUpdateSet?.invoke(reps, set.weight, set.rpe)
            }
            isEditingWeight -> {
                val weight = weightTextFieldValue.text.toFloatOrNull() ?: 0f
                onUpdateSet?.invoke(set.reps, weight, set.rpe)
            }
            isEditingRpe -> {
                val rpe = rpeTextFieldValue.text.toFloatOrNull()
                onUpdateSet?.invoke(set.reps, set.weight, rpe)
            }
        }
    }

    // Switch to a new field, saving the current one first
    fun switchToField(newField: String) {
        if (isEditingAny) {
            saveCurrentField()
        }
        SetEditingState.switchToField(set.id, newField)
    }

    // Request focus and set cursor position when entering edit mode
    LaunchedEffect(isEditingReps) {
        if (isEditingReps) {
            val text = if (set.reps > 0) set.reps.toString() else ""
            repsTextFieldValue = TextFieldValue(text, TextRange(text.length))
            repsFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isEditingWeight) {
        if (isEditingWeight) {
            val text = if (set.weight > 0) set.weight.toString() else ""
            weightTextFieldValue = TextFieldValue(text, TextRange(text.length))
            weightFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isEditingRpe) {
        if (isEditingRpe) {
            val text = set.rpe?.toString() ?: ""
            rpeTextFieldValue = TextFieldValue(text, TextRange(text.length))
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
                        value = repsTextFieldValue,
                        onValueChange = { newValue: TextFieldValue ->
                            // Only allow numbers up to 3 digits
                            if (newValue.text.isEmpty() || (newValue.text.all { it.isDigit() } && newValue.text.length <= 3)) {
                                repsTextFieldValue = newValue
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
                                    saveCurrentField()
                                    SetEditingState.clearEditing(set.id)
                                },
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
                                .focusRequester(repsFocusRequester),
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
                                    if (onUpdateSet != null) {
                                        if (isEditingAny && !isEditingReps) {
                                            // Switching from another field
                                            switchToField("reps")
                                        } else {
                                            // Starting fresh
                                            SetEditingState.setEditing(set.id, "reps")
                                        }
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
                        value = weightTextFieldValue,
                        onValueChange = { newValue: TextFieldValue ->
                            // Only allow numbers with up to 4 integer digits and one decimal point
                            val text = newValue.text
                            if (text.isEmpty()) {
                                weightTextFieldValue = newValue
                            } else {
                                val parts = text.split(".")
                                val isValid =
                                    when (parts.size) {
                                        1 -> parts[0].all { it.isDigit() } && parts[0].length <= 4
                                        2 ->
                                            parts[0].all { it.isDigit() } &&
                                                parts[0].length <= 4 &&
                                                parts[1].all { it.isDigit() } &&
                                                parts[1].length <= 2
                                        else -> false
                                    }
                                if (isValid) {
                                    weightTextFieldValue = newValue
                                }
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
                                    saveCurrentField()
                                    SetEditingState.clearEditing(set.id)
                                },
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
                                .focusRequester(weightFocusRequester),
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
                                    if (onUpdateSet != null) {
                                        if (isEditingAny && !isEditingWeight) {
                                            // Switching from another field
                                            switchToField("weight")
                                        } else {
                                            // Starting fresh
                                            SetEditingState.setEditing(set.id, "weight")
                                        }
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
                        value = rpeTextFieldValue,
                        onValueChange = { newValue: TextFieldValue ->
                            // Only allow numbers 0-10 with up to one decimal place
                            val text = newValue.text
                            if (text.isEmpty()) {
                                rpeTextFieldValue = newValue
                            } else {
                                val floatValue = text.toFloatOrNull()
                                if (floatValue != null &&
                                    floatValue >= 0 &&
                                    floatValue <= 10 &&
                                    text.matches(Regex("^(10(\\.0)?|[0-9](\\.\\d)?|0)$")) &&
                                    text.length <= 4
                                ) {
                                    rpeTextFieldValue = newValue
                                }
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
                                    saveCurrentField()
                                    SetEditingState.clearEditing(set.id)
                                },
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
                                .focusRequester(rpeFocusRequester),
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
                                    if (onUpdateSet != null) {
                                        if (isEditingAny && !isEditingRpe) {
                                            // Switching from another field
                                            switchToField("rpe")
                                        } else {
                                            // Starting fresh
                                            SetEditingState.setEditing(set.id, "rpe")
                                        }
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
