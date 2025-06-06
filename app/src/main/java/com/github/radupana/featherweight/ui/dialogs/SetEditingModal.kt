package com.github.radupana.featherweight.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@Composable
fun SetEditingModal(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    onDismiss: () -> Unit,
    onUpdateSet: (Long, Int, Float, Float?) -> Unit,
    onAddSet: () -> Unit,
    onCopyLastSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onCompleteAllSets: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Handle back button and outside tap to dismiss
    BackHandler {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onDismiss()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                exercise.exerciseName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            if (sets.isNotEmpty()) {
                                val completedSets = sets.count { it.isCompleted }
                                Text(
                                    "$completedSets/${sets.size} sets completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Complete All button
                        val canCompleteAll = sets.isNotEmpty() && 
                            sets.all { viewModel.canMarkSetComplete(it) } &&
                            sets.any { !it.isCompleted }
                        
                        if (canCompleteAll) {
                            TextButton(
                                onClick = onCompleteAllSets
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Complete All",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("All")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }

                // Content area with optimal space usage
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (sets.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No sets yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Add your first set below",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Sets list with generous spacing and keyboard padding
                            LazyColumn(
                                modifier = Modifier.weight(1f).imePadding(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 200.dp)
                            ) {
                                items(sets) { set ->
                                    key(set.id) {
                                        ExpandedSetRow(
                                            set = set,
                                            onUpdateSet = { reps, weight, rpe ->
                                                onUpdateSet(set.id, reps, weight, rpe)
                                            },
                                            onToggleCompleted = { completed ->
                                                onToggleCompleted(set.id, completed)
                                            },
                                            onDelete = { onDeleteSet(set.id) },
                                            canMarkComplete = viewModel.canMarkSetComplete(set),
                                            keyboardController = keyboardController
                                        )
                                    }
                                }
                            }
                        }

                        // Action buttons at bottom - always visible
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onAddSet,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Set",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Set")
                                }

                                if (sets.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = onCopyLastSet,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Filled.ContentCopy,
                                            contentDescription = "Copy Last",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Last")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedSetRow(
    set: SetLog,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    canMarkComplete: Boolean,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Text field states
    var repsValue by remember(set.id) {
        val text = if (set.reps > 0) set.reps.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }
    var weightValue by remember(set.id) {
        val text = if (set.weight > 0) set.weight.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }
    var rpeValue by remember(set.id) {
        val text = set.rpe?.toString() ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }

    // Focus requesters
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    // Save function
    fun saveValues() {
        val reps = repsValue.text.toIntOrNull() ?: 0
        val weight = weightValue.text.toFloatOrNull() ?: 0f
        val rpe = rpeValue.text.toFloatOrNull()
        onUpdateSet(reps, weight, rpe)
    }


    val bgColor = if (set.isCompleted) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (set.isCompleted) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Set header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Set ${set.setOrder + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = set.isCompleted,
                        onCheckedChange = { newChecked ->
                            if (!newChecked || canMarkComplete) {
                                onToggleCompleted(newChecked)
                            }
                        },
                        enabled = canMarkComplete || set.isCompleted,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Set",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input fields with generous spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reps
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Reps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repsValue,
                        onValueChange = { newValue ->
                            if (newValue.text.isEmpty() || (newValue.text.all { it.isDigit() } && newValue.text.length <= 2)) {
                                repsValue = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { weightFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(repsFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    // Select all text when gaining focus
                                    val text = repsValue.text
                                    repsValue = repsValue.copy(selection = TextRange(0, text.length))
                                } else {
                                    saveValues()
                                }
                            },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    )
                }

                // Weight
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Weight (kg)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weightValue,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            if (text.isEmpty()) {
                                weightValue = newValue
                            } else {
                                val parts = text.split(".")
                                val isValid = when (parts.size) {
                                    1 -> parts[0].all { it.isDigit() } && parts[0].length <= 4
                                    2 -> parts[0].all { it.isDigit() } && 
                                         parts[0].length <= 4 && 
                                         parts[1].all { it.isDigit() } && 
                                         parts[1].length <= 2
                                    else -> false
                                }
                                if (isValid) {
                                    weightValue = newValue
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { rpeFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    // Select all text when gaining focus
                                    val text = weightValue.text
                                    weightValue = weightValue.copy(selection = TextRange(0, text.length))
                                } else {
                                    saveValues()
                                }
                            },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    )
                }

                // RPE
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RPE (0-10)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rpeValue,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            if (text.isEmpty()) {
                                rpeValue = newValue
                            } else {
                                val floatValue = text.toFloatOrNull()
                                if (floatValue != null &&
                                    floatValue >= 0 &&
                                    floatValue <= 10 &&
                                    text.matches(Regex("^(10(\\.0)?|[0-9](\\.[0-9])?)$")) &&
                                    text.length <= 4) {
                                    rpeValue = newValue
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                keyboardController?.hide()
                                saveValues()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(rpeFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    // Select all text when gaining focus
                                    val text = rpeValue.text
                                    rpeValue = rpeValue.copy(selection = TextRange(0, text.length))
                                } else {
                                    saveValues()
                                }
                            },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    )
                }
            }

            // Validation message
            if (!canMarkComplete && (set.reps == 0 || set.weight == 0f)) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "💡 Add both reps and weight to mark this set as complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}