package com.github.radupana.featherweight.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
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
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.ui.components.CenteredInputField
import com.github.radupana.featherweight.ui.components.InputFieldType
import com.github.radupana.featherweight.ui.components.IntegratedRestTimer
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@Composable
fun SetEditingModal(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    onDismiss: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onUpdateSet: (Long, Int, Float, Float?) -> Unit,
    onAddSet: ((Long) -> Unit) -> Unit,
    onCopyLastSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onCompleteAllSets: () -> Unit,
    viewModel: WorkoutViewModel,
    isProgrammeWorkout: Boolean = false,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Intelligent suggestions state
    var intelligentSuggestions by remember { mutableStateOf<SmartSuggestions?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Workout timer state
    val workoutState by viewModel.workoutState.collectAsState()
    val setCompletionValidation by viewModel.setCompletionValidation.collectAsState()

    // Rest timer state
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val restTimerInitialSeconds by viewModel.restTimerInitialSeconds.collectAsState()
    val restTimerExpanded by viewModel.restTimerExpanded.collectAsState()

    // Load intelligent suggestions when modal opens
    LaunchedEffect(exercise.exerciseName) {
        intelligentSuggestions = viewModel.getIntelligentSuggestions(exercise.exerciseName)
    }

    // Ensure validation cache is updated when sets change
    LaunchedEffect(sets) {
        sets.forEach { set ->
            if (setCompletionValidation[set.id] == null) {
                viewModel.canMarkSetComplete(set)
            }
        }
    }

    // Scroll to newly added set
    LaunchedEffect(sets.size) {
        if (sets.isNotEmpty()) {
            listState.animateScrollToItem(sets.size) // Scroll to action buttons after last set
        }
    }

    // Auto-scroll to focused input field when keyboard appears
    LaunchedEffect(keyboardController) {
        // Listen for keyboard state changes and auto-scroll if needed
        // This helps ensure that the currently focused input field remains visible
        // when the keyboard appears by scrolling the list appropriately
    }

    // Handle back button and outside tap to dismiss
    BackHandler {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
        // In read-only mode, also trigger navigation back since this is a full-screen modal
        // and users expect the back button to leave the screen entirely
        if (readOnly && onNavigateBack != null) {
            onNavigateBack()
        }
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
            modifier = modifier
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
                    
                    // Rest timer (if active)
                    if (restTimerSeconds > 0) {
                        IntegratedRestTimer(
                            seconds = restTimerSeconds,
                            initialSeconds = restTimerInitialSeconds,
                            isExpanded = restTimerExpanded,
                            onToggleExpanded = { viewModel.toggleRestTimerExpanded() },
                            onSkip = { viewModel.skipRestTimer() },
                            onPresetSelected = { viewModel.selectRestTimerPreset(it) },
                            onAdjustTime = { viewModel.adjustRestTimer(it) }
                        )
                    }

                    // Collapsible Insights Section
                    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
                    val previousSets = exerciseHistory[exercise.exerciseName]?.sets ?: emptyList()
                    var showInsights by remember { mutableStateOf(false) }

                    if (previousSets.isNotEmpty() || (!isProgrammeWorkout && intelligentSuggestions != null)) {
                        InsightsSection(
                            exerciseName = exercise.exerciseName,
                            previousSets = previousSets,
                            intelligentSuggestions = if (!isProgrammeWorkout) intelligentSuggestions else null,
                            isExpanded = showInsights,
                            onToggleExpanded = { showInsights = !showInsights },
                            onSelectAlternative = { alternative ->
                                if (sets.isEmpty()) {
                                    // Create set with initial values directly
                                    viewModel.addSetToExercise(
                                        exerciseLogId = exercise.id,
                                        targetReps = alternative.actualReps,
                                        targetWeight = alternative.actualWeight,
                                        weight = alternative.actualWeight,
                                        reps = alternative.actualReps,
                                        rpe = alternative.actualRpe,
                                    )
                                } else {
                                    val firstUncompletedSet = sets.firstOrNull { !it.isCompleted }
                                    firstUncompletedSet?.let { set ->
                                        onUpdateSet(set.id, alternative.actualReps, alternative.actualWeight, alternative.actualRpe)
                                    }
                                }
                            },
                            onSelectSuggestion = { suggestion ->
                                if (sets.isEmpty()) {
                                    // Create set with initial values directly
                                    viewModel.addSetToExercise(
                                        exerciseLogId = exercise.id,
                                        targetReps = suggestion.suggestedReps,
                                        targetWeight = suggestion.suggestedWeight,
                                        weight = suggestion.suggestedWeight,
                                        reps = suggestion.suggestedReps,
                                        rpe = suggestion.suggestedRpe,
                                    )
                                } else {
                                    val firstUncompletedSet = sets.firstOrNull { !it.isCompleted }
                                    firstUncompletedSet?.let { set ->
                                        onUpdateSet(set.id, suggestion.suggestedReps, suggestion.suggestedWeight, suggestion.suggestedRpe)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    // Content area with optimal space usage
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .imePadding(), // Add IME padding to avoid keyboard overlap
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Show header only when there are sets
                            if (sets.isNotEmpty()) {
                                // Modal header for input fields
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Target",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        "Weight",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        "Reps",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        "RPE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = TextAlign.Center,
                                    )
                                    // "All" button for marking all sets complete
                                    // Note: We check reps > 0 here because the actual weight requirement
                                    // is checked in the ViewModel's canMarkSetComplete function
                                    val hasPopulatedSets = sets.any { !it.isCompleted && it.actualReps > 0 }
                                    if (hasPopulatedSets) {
                                        IconButton(
                                            onClick = onCompleteAllSets,
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Complete all",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(48.dp))
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
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                                    if (!readOnly) {
                                                        OutlinedButton(
                                                            onClick = { onAddSet { } },
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
                                                    } else {
                                                        Text(
                                                            "No sets recorded",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
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
                                                        if (!readOnly && dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                            onDeleteSet(set.id)
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                    positionalThreshold = { totalDistance ->
                                                        totalDistance * 0.33f // Require 33% swipe distance
                                                    },
                                                )

                                            CleanSetLayout(
                                                set = set,
                                                exercise = exercise,
                                                onUpdateSet = { reps, weight, rpe ->
                                                    onUpdateSet(set.id, reps, weight, rpe)
                                                },
                                                onUpdateTarget = { reps, weight ->
                                                    viewModel.updateSetTarget(set.id, reps, weight)
                                                },
                                                onToggleCompleted = { completed ->
                                                    onToggleCompleted(set.id, completed)
                                                },
                                                canMarkComplete = setCompletionValidation[set.id] ?: false,
                                                viewModel = viewModel,
                                                isProgrammeWorkout = isProgrammeWorkout,
                                                swipeToDismissState = dismissState,
                                                readOnly = readOnly,
                                            )
                                        }
                                    }

                                    // Action buttons after sets (only when there are sets)
                                    if (sets.isNotEmpty() && !readOnly) {
                                        item {
                                            val lastSet = sets.maxByOrNull { it.setOrder }
                                            val canCopyLast = lastSet != null && lastSet.actualReps > 0

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
                                                        onClick = { onAddSet { } },
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
        val text = if (set.actualReps > 0) set.actualReps.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }
    var weightValue by remember(set.id) {
        val text = if (set.actualWeight > 0) set.actualWeight.toString() else ""
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }
    var rpeValue by remember(set.id) {
        val text = set.actualRpe?.toString() ?: ""
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
    LaunchedEffect(set.actualReps) {
        if (!repsFieldHasFocus) {
            val repsText = if (set.actualReps > 0) set.actualReps.toString() else ""
            if (repsValue.text != repsText) {
                repsValue = TextFieldValue(repsText, TextRange.Zero)
            }
        }
    }

    LaunchedEffect(set.actualWeight) {
        if (!weightFieldHasFocus) {
            val weightText = if (set.actualWeight > 0) set.actualWeight.toString() else ""
            if (weightValue.text != weightText) {
                weightValue = TextFieldValue(weightText, TextRange.Zero)
            }
        }
    }

    LaunchedEffect(set.actualRpe) {
        if (!rpeFieldHasFocus) {
            val rpeText = set.actualRpe?.toString() ?: ""
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
                if (!canMarkComplete && (set.actualReps == 0 || set.actualWeight == 0f) && showDeleteConfirmation) {
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

@Composable
fun CleanSetLayout(
    set: SetLog,
    exercise: ExerciseLog,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onUpdateTarget: (Int, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    canMarkComplete: Boolean,
    viewModel: WorkoutViewModel,
    isProgrammeWorkout: Boolean,
    swipeToDismissState: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    // Input states - Store as TextFieldValue to preserve cursor position
    // Use both set.id and the actual values as remember keys so UI updates when data changes
    var weightInput by remember(set.id, set.actualWeight, set.targetWeight) {
        val text =
            if (set.actualWeight > 0) {
                WeightFormatter.formatWeight(set.actualWeight)
            } else if (set.targetWeight != null && set.targetWeight > 0) {
                // Pre-populate with target weight if no actual weight entered yet
                WeightFormatter.formatWeight(set.targetWeight)
            } else {
                ""
            }
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var repsInput by remember(set.id, set.actualReps, set.targetReps) {
        val text =
            if (set.actualReps > 0) {
                set.actualReps.toString()
            } else if (set.targetReps > 0) {
                // Pre-populate with target reps if no actual reps entered yet
                set.targetReps.toString()
            } else {
                ""
            }
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var rpeInput by remember(set.id, set.actualRpe) {
        val text = set.actualRpe?.let { WeightFormatter.formatDecimal(it, 1) } ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Swipeable Input Area - ONLY this part can be swiped to delete
        SwipeToDismissBox(
            state = swipeToDismissState,
            backgroundContent = {
                // Only show red background when actively swiping to delete
                val progress = swipeToDismissState.progress
                val targetValue = swipeToDismissState.targetValue
                val currentValue = swipeToDismissState.currentValue

                // Show red only when:
                // 1. We have swipe progress (item is displaced)
                // 2. Target is delete direction (EndToStart)
                // 3. Not already dismissed
                // Use very small threshold to detect swipe immediately
                if (progress > 0.01f &&
                    targetValue == SwipeToDismissBoxValue.EndToStart &&
                    currentValue != SwipeToDismissBoxValue.EndToStart
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        // Dynamic red stripe that grows with swipe
                        // Start with minimum width for immediate visibility
                        val minWidth = 40.dp
                        val maxAdditionalWidth = 160.dp
                        val currentWidth = minWidth + (maxAdditionalWidth.value * progress).dp

                        Surface(
                            modifier =
                                Modifier
                                    .width(currentWidth)
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            shape =
                                RoundedCornerShape(
                                    topStart = 8.dp,
                                    bottomStart = 8.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp,
                                ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Icon that becomes more visible with progress
                                val iconOffset = ((1 - progress) * 15).dp
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint =
                                        MaterialTheme.colorScheme.onError.copy(
                                            alpha = 0.6f + (0.4f * progress),
                                        ),
                                    modifier =
                                        Modifier
                                            .size((20 + (4 * progress)).dp)
                                            .offset(x = iconOffset),
                                )
                            }
                        }
                    }
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
        ) {
            // Clean Input Row - Only this gets swiped
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Target column - read-only
                val targetDisplay =
                    if (isProgrammeWorkout && set.targetReps > 0) {
                        if (set.targetWeight != null && set.targetWeight > 0) {
                            "${set.targetReps}×${WeightFormatter.formatWeight(set.targetWeight)}"
                        } else {
                            "${set.targetReps}"
                        }
                    } else {
                        ""
                    }

                Box(
                    modifier =
                        Modifier
                            .weight(0.8f)
                            .height(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = targetDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (targetDisplay.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                    )
                }

                // Weight input
                CenteredInputField(
                    value = weightInput,
                    onValueChange = { textFieldValue ->
                        if (!readOnly) {
                            weightInput = textFieldValue
                            val weight = textFieldValue.text.toFloatOrNull() ?: 0f
                            onUpdateSet(set.actualReps, weight, set.actualRpe)
                        }
                    },
                    fieldType = InputFieldType.WEIGHT,
                    placeholder = "", // No placeholder
                    modifier = Modifier.weight(0.8f).height(48.dp),
                    imeAction = ImeAction.Next,
                )

                // Reps input
                CenteredInputField(
                    value = repsInput,
                    onValueChange = { textFieldValue ->
                        if (!readOnly) {
                            repsInput = textFieldValue
                            val reps = textFieldValue.text.toIntOrNull() ?: 0
                            onUpdateSet(reps, set.actualWeight, set.actualRpe)
                        }
                    },
                    fieldType = InputFieldType.REPS,
                    placeholder = "", // No placeholder
                    modifier = Modifier.weight(0.6f).height(48.dp),
                    imeAction = ImeAction.Next,
                )

                // RPE input
                CenteredInputField(
                    value = rpeInput,
                    onValueChange = { textFieldValue ->
                        if (!readOnly) {
                            rpeInput = textFieldValue
                            val rpe = textFieldValue.text.toIntOrNull()?.toFloat()
                            onUpdateSet(set.actualReps, set.actualWeight, rpe)
                        }
                    },
                    fieldType = InputFieldType.RPE,
                    placeholder = "", // No placeholder
                    modifier = Modifier.weight(0.6f).height(48.dp),
                    imeAction = ImeAction.Done,
                )

                // Completion checkbox
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clickable(
                                enabled = !readOnly && (canMarkComplete || set.isCompleted),
                                onClick = {
                                    val newChecked = !set.isCompleted
                                    if (!newChecked || canMarkComplete) {
                                        onToggleCompleted(newChecked)
                                    }
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        checked = set.isCompleted,
                        onCheckedChange = null, // Handle clicks on the Box instead
                        enabled = !readOnly && (canMarkComplete || set.isCompleted),
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
            }
        } // Close SwipeToDismissBox
    } // Close Column
}

@Composable
private fun InsightsSection(
    exerciseName: String,
    previousSets: List<SetLog>,
    intelligentSuggestions: SmartSuggestions?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectAlternative: (SetLog) -> Unit,
    onSelectSuggestion: (SmartSuggestions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header with expand/collapse toggle
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Insights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Previous Performance Section
                    if (previousSets.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Previous Performance",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            // Show last 3 sets
                            val lastThreeSets =
                                previousSets
                                    .filter { it.isCompleted }
                                    .take(3)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                lastThreeSets.forEach { set ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { onSelectAlternative(set) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text =
                                                    if (set.actualWeight > 0) {
                                                        "${set.actualReps}×${WeightFormatter.formatWeightWithUnit(set.actualWeight)}"
                                                    } else {
                                                        "${set.actualReps} reps"
                                                    },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            if (set.actualRpe != null) {
                                                Text(
                                                    "@${WeightFormatter.formatDecimal(set.actualRpe, 1)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Fill remaining space if less than 3 sets
                                if (lastThreeSets.size < 3) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Intelligent Suggestions Section
                    if (intelligentSuggestions != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Suggestions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )

                            // Main suggestion
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { onSelectSuggestion(intelligentSuggestions) },
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Lightbulb,
                                        contentDescription = "Suggestion",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp),
                                    )

                                    Text(
                                        text =
                                            with(intelligentSuggestions) {
                                                if (suggestedWeight > 0) {
                                                    "$suggestedReps×${WeightFormatter.formatWeightWithUnit(suggestedWeight)}"
                                                } else {
                                                    "$suggestedReps reps"
                                                }
                                            },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )

                                    if (intelligentSuggestions.suggestedRpe != null) {
                                        Text(
                                            "@${WeightFormatter.formatDecimal(intelligentSuggestions.suggestedRpe, 1)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        intelligentSuggestions.reasoning,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
