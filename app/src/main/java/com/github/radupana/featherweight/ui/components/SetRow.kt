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
import androidx.compose.runtime.saveable.rememberSaveable
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

// Global editing state to survive recompositions
object SetEditingState {
    private val editingStates = mutableStateMapOf<Long, String>()
    
    fun isEditing(setId: Long, field: String): Boolean {
        return editingStates[setId] == field
    }
    
    fun setEditing(setId: Long, field: String) {
        Log.d("SetEditingState", "Setting editing for set $setId field $field")
        editingStates[setId] = field
    }
    
    fun clearEditing(setId: Long) {
        Log.d("SetEditingState", "Clearing editing for set $setId")
        editingStates.remove(setId)
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
    Log.d("SetRow", "Set ${set.id}: Composing SetRow - onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}, canMarkComplete = $canMarkComplete")
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Use global editing state to survive recompositions
    val isEditingReps = SetEditingState.isEditing(set.id, "reps")
    val isEditingWeight = SetEditingState.isEditing(set.id, "weight")
    val isEditingRpe = SetEditingState.isEditing(set.id, "rpe")
    
    // Track if we just started editing to ignore immediate focus loss
    var justStartedEditingReps by remember(set.id) { mutableStateOf(false) }
    var justStartedEditingWeight by remember(set.id) { mutableStateOf(false) }
    var justStartedEditingRpe by remember(set.id) { mutableStateOf(false) }

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

    // Text state that survives recomposition
    var repsText by rememberSaveable(set.id) {
        mutableStateOf(if (set.reps > 0) set.reps.toString() else "")
    }
    var weightText by rememberSaveable(set.id) {
        mutableStateOf(if (set.weight > 0) set.weight.toString() else "")
    }
    var rpeText by rememberSaveable(set.id) {
        mutableStateOf(set.rpe?.toString() ?: "")
    }

    // Update text when set values change but we're not editing
    LaunchedEffect(set.reps) {
        if (!isEditingReps) {
            repsText = if (set.reps > 0) set.reps.toString() else ""
        }
    }
    LaunchedEffect(set.weight) {
        if (!isEditingWeight) {
            weightText = if (set.weight > 0) set.weight.toString() else ""
        }
    }
    LaunchedEffect(set.rpe) {
        if (!isEditingRpe) {
            rpeText = set.rpe?.toString() ?: ""
        }
    }

    // Focus requesters
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Save functions
    fun saveReps() {
        val reps = repsText.toIntOrNull() ?: 0
        val weight = set.weight
        val rpe = set.rpe
        onUpdateSet?.invoke(reps, weight, rpe)
        SetEditingState.clearEditing(set.id)
    }

    fun saveWeight() {
        val reps = set.reps
        val weight = weightText.toFloatOrNull() ?: 0f
        val rpe = set.rpe
        onUpdateSet?.invoke(reps, weight, rpe)
        SetEditingState.clearEditing(set.id)
    }

    fun saveRpe() {
        val reps = set.reps
        val weight = set.weight
        val rpe = rpeText.toFloatOrNull()
        onUpdateSet?.invoke(reps, weight, rpe)
        SetEditingState.clearEditing(set.id)
    }

    // Request focus when entering edit mode
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
                                    Log.d("SetRow", "Set ${set.id}: Reps focus changed - isFocused: ${focusState.isFocused}, isEditingReps: $isEditingReps, justStarted: $justStartedEditingReps")
                                    
                                    if (focusState.isFocused) {
                                        // Reset the flag when we gain focus
                                        justStartedEditingReps = false
                                    } else if (isEditingReps && !justStartedEditingReps) {
                                        // Only save if we're editing and didn't just start
                                        Log.d("SetRow", "Set ${set.id}: Reps lost focus - calling saveReps()")
                                        saveReps()
                                    } else if (justStartedEditingReps) {
                                        Log.d("SetRow", "Set ${set.id}: Ignoring initial focus loss for reps")
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
                                    Log.d("SetRow", "Set ${set.id}: Reps clicked - onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}")
                                    if (onUpdateSet != null) {
                                        repsText = if (set.reps > 0) set.reps.toString() else ""
                                        Log.d("SetRow", "Set ${set.id}: Setting editing to reps, repsText = '$repsText'")
                                        justStartedEditingReps = true
                                        SetEditingState.setEditing(set.id, "reps")
                                    } else {
                                        Log.e("SetRow", "Set ${set.id}: onUpdateSet is NULL - cannot edit reps")
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
                                    // Save when losing focus while in edit mode
                                    if (!focusState.isFocused && isEditingWeight) {
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
                                    Log.d("SetRow", "Set ${set.id}: Weight clicked - onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}")
                                    if (onUpdateSet != null) {
                                        weightText = if (set.weight > 0) set.weight.toString() else ""
                                        Log.d("SetRow", "Set ${set.id}: Setting editing to weight, weightText = '$weightText'")
                                        SetEditingState.setEditing(set.id, "weight")
                                    } else {
                                        Log.e("SetRow", "Set ${set.id}: onUpdateSet is NULL - cannot edit weight")
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
                            // Only allow numbers 0-10 with up to one decimal place
                            if (newValue.isEmpty()) {
                                rpeText = newValue
                            } else {
                                val floatValue = newValue.toFloatOrNull()
                                if (floatValue != null && floatValue >= 0 && floatValue <= 10 && 
                                    newValue.matches(Regex("^(10(\\.0)?|[0-9](\\.\\d)?|0)$")) &&
                                    newValue.length <= 4) {
                                    rpeText = newValue
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
                                    // Save when losing focus while in edit mode
                                    if (!focusState.isFocused && isEditingRpe) {
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
                                    Log.d("SetRow", "Set ${set.id}: RPE clicked - onUpdateSet is ${if (onUpdateSet != null) "NOT NULL" else "NULL"}")
                                    if (onUpdateSet != null) {
                                        rpeText = set.rpe?.toString() ?: ""
                                        Log.d("SetRow", "Set ${set.id}: Setting editing to rpe, rpeText = '$rpeText'")
                                        SetEditingState.setEditing(set.id, "rpe")
                                    } else {
                                        Log.e("SetRow", "Set ${set.id}: onUpdateSet is NULL - cannot edit RPE")
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
