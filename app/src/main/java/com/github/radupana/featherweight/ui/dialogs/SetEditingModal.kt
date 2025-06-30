package com.github.radupana.featherweight.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.github.radupana.featherweight.ui.components.UnifiedTimerBar
import com.github.radupana.featherweight.viewmodel.RestTimerViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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
    restTimerViewModel: RestTimerViewModel,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val timerState by restTimerViewModel.timerState.collectAsState()

    // Workout timer state
    val workoutState by viewModel.workoutState.collectAsState()
    val elapsedWorkoutTime by viewModel.elapsedWorkoutTime.collectAsState()

    // Scroll to newly added set
    LaunchedEffect(sets.size) {
        if (sets.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(sets.size) // Scroll to action buttons after last set
            }
        }
    }

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
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier =
                modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onDismiss()
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                exercise.exerciseName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            if (sets.isNotEmpty()) {
                                val completedSets = sets.count { it.isCompleted }
                                Text(
                                    "$completedSets/${sets.size} sets completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Unified Timer Bar
                UnifiedTimerBar(
                    workoutElapsed = elapsedWorkoutTime,
                    workoutActive = workoutState.isWorkoutTimerActive,
                    restTimerState = timerState,
                    onRestAddTime = { restTimerViewModel.addTime(15.seconds) },
                    onRestSubtractTime = { restTimerViewModel.subtractTime(15.seconds) },
                    onRestSkip = { restTimerViewModel.stopTimer() },
                    onRestTogglePause = { restTimerViewModel.togglePause() },
                )

                // Content area with optimal space usage
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Show header only when there are sets
                        if (sets.isNotEmpty()) {
                            // Header row
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Set",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "Reps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "Weight (kg)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "RPE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                                // Complete All button positioned here
                                val canCompleteAll =
                                    sets.isNotEmpty() &&
                                        sets.all { viewModel.canMarkSetComplete(it) } &&
                                        sets.any { !it.isCompleted }

                                if (canCompleteAll) {
                                    OutlinedButton(
                                        onClick = onCompleteAllSets,
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Complete All",
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            "All",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(64.dp))
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                        }

                        // Always show the LazyColumn
                        Column(modifier = Modifier.weight(1f)) {
                            // Sets list
                            LazyColumn(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
                            ) {
                                // Always show action buttons first when there are no sets
                                if (sets.isEmpty()) {
                                    item {
                                        Card(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp),
                                            colors =
                                                CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                ),
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                horizontalArrangement = Arrangement.Center,
                                            ) {
                                                OutlinedButton(
                                                    onClick = onAddSet,
                                                    modifier = Modifier.fillMaxWidth(0.6f),
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = "Add Set",
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Add Set")
                                                }
                                            }
                                        }
                                    }
                                }

                                items(sets) { set ->
                                    key(set.id) {
                                        val dismissState =
                                            rememberSwipeToDismissBoxState(
                                                confirmValueChange = { dismissDirection ->
                                                    if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                        onDeleteSet(set.id)
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                },
                                            )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            backgroundContent = {
                                                // Only show red background when actively dismissing
                                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxSize(),
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = RoundedCornerShape(8.dp),
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            horizontalArrangement = Arrangement.End,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.onError,
                                                                modifier = Modifier.padding(16.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            enableDismissFromStartToEnd = false,
                                            enableDismissFromEndToStart = true,
                                        ) {
                                            ExpandedSetRow(
                                                set = set,
                                                onUpdateSet = { reps, weight, rpe ->
                                                    onUpdateSet(set.id, reps, weight, rpe)
                                                },
                                                onToggleCompleted = { completed ->
                                                    onToggleCompleted(set.id, completed)
                                                },
                                                onDelete = { }, // No longer needed as we use swipe
                                                canMarkComplete = viewModel.canMarkSetComplete(set),
                                                keyboardController = keyboardController,
                                                // Hide the delete button
                                            )
                                        }
                                    }
                                }

                                // Action buttons after sets (only when there are sets)
                                if (sets.isNotEmpty()) {
                                    item {
                                        val lastSet = sets.maxByOrNull { it.setOrder }
                                        val canCopyLast = lastSet != null && lastSet.reps > 0

                                        Card(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .imePadding(),
                                            colors =
                                                CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                ),
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                OutlinedButton(
                                                    onClick = onAddSet,
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = "Add Set",
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Add Set")
                                                }

                                                if (canCopyLast) {
                                                    OutlinedButton(
                                                        onClick = onCopyLastSet,
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.ContentCopy,
                                                            contentDescription = "Copy Last",
                                                            modifier = Modifier.size(18.dp),
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
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Text field states - start with no selection to avoid auto-select on copy
    var repsValue by remember(set.id) {
        val text = if (set.reps > 0) set.reps.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }
    var weightValue by remember(set.id) {
        val text = if (set.weight > 0) set.weight.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }
    var rpeValue by remember(set.id) {
        val text = set.rpe?.toString() ?: ""
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }

    // Focus requesters and state tracking
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    // Simple approach: track when fields have focus for replacement
    var weightFieldHasFocus by remember { mutableStateOf(false) }
    var repsFieldHasFocus by remember { mutableStateOf(false) }
    var rpeFieldHasFocus by remember { mutableStateOf(false) }

    // Update fields only when not focused (to avoid interfering with user input)
    LaunchedEffect(set.reps) {
        if (!repsFieldHasFocus) {
            val repsText = if (set.reps > 0) set.reps.toString() else ""
            if (repsValue.text != repsText) {
                repsValue = TextFieldValue(repsText, TextRange.Zero)
            }
        }
    }

    LaunchedEffect(set.weight) {
        if (!weightFieldHasFocus) {
            val weightText = if (set.weight > 0) set.weight.toString() else ""
            if (weightValue.text != weightText) {
                weightValue = TextFieldValue(weightText, TextRange.Zero)
            }
        }
    }

    LaunchedEffect(set.rpe) {
        if (!rpeFieldHasFocus) {
            val rpeText = set.rpe?.toString() ?: ""
            if (rpeValue.text != rpeText) {
                rpeValue = TextFieldValue(rpeText, TextRange.Zero)
            }
        }
    }

    // Save function
    fun saveValues() {
        val reps = repsValue.text.toIntOrNull() ?: 0
        val weight = weightValue.text.toFloatOrNull() ?: 0f
        val rpe = rpeValue.text.toFloatOrNull()
        onUpdateSet(reps, weight, rpe)
    }

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
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            ),
                )
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(12.dp),
            ) {
                // Compact single row layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Set number
                    Text(
                        "${set.setOrder + 1}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                    )
                    // Reps field
                    OutlinedTextField(
                        value = repsValue,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            if (text.isEmpty() || (text.all { it.isDigit() } && text.length <= 2)) {
                                repsValue = newValue
                                saveValues()
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { weightFocusRequester.requestFocus() },
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .focusRequester(repsFocusRequester)
                                .onFocusChanged { focusState ->
                                    repsFieldHasFocus = focusState.isFocused
                                    if (focusState.isFocused && repsValue.text.isNotEmpty()) {
                                        // Select all text for easy replacement
                                        val text = repsValue.text
                                        repsValue = repsValue.copy(selection = TextRange(0, text.length))
                                    }
                                },
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                        placeholder = { Text("", style = MaterialTheme.typography.bodyMedium) },
                    )

                    // Weight field
                    OutlinedTextField(
                        value = weightValue,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            if (text.isEmpty()) {
                                weightValue = newValue
                                saveValues()
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
                                    weightValue = newValue
                                    saveValues()
                                }
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { rpeFocusRequester.requestFocus() },
                            ),
                        modifier =
                            Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .focusRequester(weightFocusRequester)
                                .onFocusChanged { focusState ->
                                    weightFieldHasFocus = focusState.isFocused
                                    if (focusState.isFocused && weightValue.text.isNotEmpty()) {
                                        // Select all text for easy replacement
                                        val text = weightValue.text
                                        weightValue = weightValue.copy(selection = TextRange(0, text.length))
                                    }
                                },
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                        placeholder = { Text("", style = MaterialTheme.typography.bodyMedium) },
                    )

                    // RPE field
                    OutlinedTextField(
                        value = rpeValue,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            if (text.isEmpty()) {
                                rpeValue = newValue
                                saveValues()
                            } else {
                                val floatValue = text.toFloatOrNull()
                                if (floatValue != null &&
                                    floatValue >= 0 &&
                                    floatValue <= 10 &&
                                    text.matches(Regex("^(10(\\.0)?|[0-9](\\.[0-9])?)$")) &&
                                    text.length <= 4
                                ) {
                                    rpeValue = newValue
                                    saveValues()
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
                                    keyboardController?.hide()
                                },
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .focusRequester(rpeFocusRequester)
                                .onFocusChanged { focusState ->
                                    rpeFieldHasFocus = focusState.isFocused
                                    if (focusState.isFocused && rpeValue.text.isNotEmpty()) {
                                        // Select all text for easy replacement
                                        val text = rpeValue.text
                                        rpeValue = rpeValue.copy(selection = TextRange(0, text.length))
                                    }
                                },
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                        placeholder = { Text("", style = MaterialTheme.typography.bodyMedium) },
                    )

                    // Checkbox in same space as Complete All button
                    Box(
                        modifier = Modifier.width(64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            checked = set.isCompleted,
                            onCheckedChange = { newChecked ->
                                if (!newChecked || canMarkComplete) {
                                    onToggleCompleted(newChecked)
                                }
                            },
                            enabled = canMarkComplete || set.isCompleted,
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.tertiary,
                                ),
                        )
                    }
                }

                // Only show validation message if user tried to complete without data
                if (!canMarkComplete && (set.reps == 0 || set.weight == 0f) && showDeleteConfirmation) {
                    Text(
                        "Add reps & weight to complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
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
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
