package com.github.radupana.featherweight.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.domain.SmartSuggestions
import com.github.radupana.featherweight.ui.components.CenteredInputField
import com.github.radupana.featherweight.ui.components.CompactRestTimer
import com.github.radupana.featherweight.ui.components.InputFieldType
import com.github.radupana.featherweight.ui.components.WorkoutTimer
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    workoutTimerSeconds: Int = 0,
    isProgrammeWorkout: Boolean = false,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Get 1RM estimate for this exercise
    val oneRMEstimates by viewModel.oneRMEstimates.collectAsState()
    val oneRMEstimate = oneRMEstimates[exercise.exerciseVariationId]

    // Intelligent suggestions state
    var intelligentSuggestions by remember { mutableStateOf<SmartSuggestions?>(null) }

    // Workout timer state
    val setCompletionValidation by viewModel.setCompletionValidation.collectAsState()

    // Rest timer state
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val restTimerInitialSeconds by viewModel.restTimerInitialSeconds.collectAsState()

    // Get exercise name from repository
    val exerciseNames by viewModel.exerciseNames.collectAsState()
    val exerciseName = exerciseNames[exercise.exerciseVariationId] ?: "Unknown Exercise"

    // Load intelligent suggestions when modal opens
    LaunchedEffect(exercise.exerciseVariationId) {
        intelligentSuggestions = viewModel.getIntelligentSuggestions(exercise.exerciseVariationId)
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
            modifier =
                modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .imePadding(),
            ) {
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
                                exerciseName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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

                        // Workout timer on the right
                        if (workoutTimerSeconds > 0) {
                            WorkoutTimer(
                                seconds = workoutTimerSeconds,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }

                // Rest timer removed - now shown at bottom of WorkoutScreen

                // Collapsible Insights Section
                val exerciseHistory by viewModel.exerciseHistory.collectAsState()
                val previousSets = exerciseHistory[exercise.exerciseVariationId]?.sets ?: emptyList()
                var showInsights by remember { mutableStateOf(false) }

                if (previousSets.isNotEmpty() || (!isProgrammeWorkout && intelligentSuggestions != null)) {
                    InsightsSection(
                        previousSets = previousSets,
                        intelligentSuggestions = if (!isProgrammeWorkout) intelligentSuggestions else null,
                        isExpanded = showInsights,
                        onToggleExpanded = { showInsights = !showInsights },
                        onSelectAlternative = { alternative ->
                            if (sets.isEmpty()) {
                                // Create set with initial values directly (no targets for freestyle workouts)
                                viewModel.addSetToExercise(
                                    exerciseLogId = exercise.id,
                                    targetReps = null, // No targets for freestyle workouts
                                    targetWeight = null, // No targets for freestyle workouts
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
                                // Create set with initial values directly (no targets for freestyle workouts)
                                viewModel.addSetToExercise(
                                    exerciseLogId = exercise.id,
                                    targetReps = null, // No targets for freestyle workouts
                                    targetWeight = null, // No targets for freestyle workouts
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

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                ) {
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

                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding =
                            PaddingValues(
                                start = 0.dp,
                                top = 8.dp,
                                end = 0.dp,
                                bottom = 80.dp,
                            ),
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
                                    oneRMEstimate = oneRMEstimate,
                                    onUpdateSet = { reps, weight, rpe ->
                                        onUpdateSet(set.id, reps, weight, rpe)
                                    },
                                    onToggleCompleted = { completed ->
                                        onToggleCompleted(set.id, completed)
                                    },
                                    canMarkComplete = setCompletionValidation[set.id] ?: false,
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
                                val canCopyLast = lastSet != null && (lastSet.actualReps > 0 || lastSet.actualWeight > 0)

                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
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

                        if (restTimerSeconds > 0) {
                            item {
                                CompactRestTimer(
                                    seconds = restTimerSeconds,
                                    initialSeconds = restTimerInitialSeconds,
                                    onSkip = { viewModel.skipRestTimer() },
                                    onPresetSelected = { viewModel.selectRestTimerPreset(it) },
                                    onAdjustTime = { viewModel.adjustRestTimer(it) },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CleanSetLayout(
    set: SetLog,
    oneRMEstimate: Float? = null,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    canMarkComplete: Boolean,
    isProgrammeWorkout: Boolean,
    swipeToDismissState: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val inputStates = rememberSetInputStates(set)

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SwipeToDismissBox(
            state = swipeToDismissState,
            backgroundContent = {
                SwipeDeleteBackground(swipeToDismissState)
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SetInputRow(
                    set = set,
                    inputStates = inputStates,
                    oneRMEstimate = oneRMEstimate,
                    isProgrammeWorkout = isProgrammeWorkout,
                    readOnly = readOnly,
                    onUpdateSet = onUpdateSet,
                    onToggleCompleted = onToggleCompleted,
                    canMarkComplete = canMarkComplete,
                )
            }
        }
    }
}

@Composable
private fun InsightsSection(
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

data class SetInputStates(
    val weightInput: MutableState<TextFieldValue>,
    val repsInput: MutableState<TextFieldValue>,
    val rpeInput: MutableState<TextFieldValue>,
)

@Composable
private fun rememberSetInputStates(set: SetLog): SetInputStates {
    val weightInput = remember(set.id, set.actualWeight, set.targetWeight) {
        val text = formatWeightInput(set.actualWeight, set.targetWeight)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    val repsInput = remember(set.id, set.actualReps, set.targetReps) {
        val text = formatRepsInput(set.actualReps, set.targetReps)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    val rpeInput = remember(set.id, set.actualRpe) {
        val text = set.actualRpe?.let { WeightFormatter.formatDecimal(it, 1) } ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    
    return SetInputStates(weightInput, repsInput, rpeInput)
}

private fun formatWeightInput(actualWeight: Float, targetWeight: Float?): String {
    return if (actualWeight > 0) {
        WeightFormatter.formatWeight(actualWeight)
    } else if (targetWeight != null && targetWeight > 0) {
        WeightFormatter.formatWeight(targetWeight)
    } else {
        ""
    }
}

private fun formatRepsInput(actualReps: Int, targetReps: Int?): String {
    return if (actualReps > 0) {
        actualReps.toString()
    } else if (targetReps != null && targetReps > 0) {
        targetReps.toString()
    } else {
        ""
    }
}

@Composable
private fun SwipeDeleteBackground(swipeToDismissState: SwipeToDismissBoxState) {
    val progress = swipeToDismissState.progress
    val targetValue = swipeToDismissState.targetValue
    val currentValue = swipeToDismissState.currentValue

    if (progress > 0.01f &&
        targetValue == SwipeToDismissBoxValue.EndToStart &&
        currentValue != SwipeToDismissBoxValue.EndToStart
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
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
}

@Composable
private fun SetInputRow(
    set: SetLog,
    inputStates: SetInputStates,
    oneRMEstimate: Float?,
    isProgrammeWorkout: Boolean,
    readOnly: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    canMarkComplete: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TargetColumn(
            set = set,
            isProgrammeWorkout = isProgrammeWorkout,
            modifier = Modifier.weight(0.8f),
        )

        WeightInputColumn(
            weightInput = inputStates.weightInput,
            set = set,
            oneRMEstimate = oneRMEstimate,
            readOnly = readOnly,
            onUpdateSet = onUpdateSet,
            modifier = Modifier.weight(0.8f),
        )

        RepsInputColumn(
            repsInput = inputStates.repsInput,
            set = set,
            readOnly = readOnly,
            onUpdateSet = onUpdateSet,
            modifier = Modifier.weight(0.6f),
        )

        RpeInputColumn(
            rpeInput = inputStates.rpeInput,
            set = set,
            readOnly = readOnly,
            onUpdateSet = onUpdateSet,
            modifier = Modifier.weight(0.6f),
        )

        CompletionCheckbox(
            set = set,
            readOnly = readOnly,
            canMarkComplete = canMarkComplete,
            onToggleCompleted = onToggleCompleted,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun TargetColumn(
    set: SetLog,
    isProgrammeWorkout: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetDisplay = getTargetDisplay(set, isProgrammeWorkout)

    Box(
        modifier = modifier
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
            fontWeight = if (targetDisplay != "—") FontWeight.Medium else FontWeight.Normal,
        )
    }
}

private fun getTargetDisplay(set: SetLog, isProgrammeWorkout: Boolean): String {
    return if (isProgrammeWorkout && set.targetReps != null && set.targetReps > 0) {
        if (set.targetWeight != null && set.targetWeight > 0) {
            "${set.targetReps}×${WeightFormatter.formatWeight(set.targetWeight)}"
        } else {
            "${set.targetReps}"
        }
    } else {
        "—"
    }
}

@Composable
private fun WeightInputColumn(
    weightInput: MutableState<TextFieldValue>,
    set: SetLog,
    oneRMEstimate: Float?,
    readOnly: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CenteredInputField(
                    value = weightInput.value,
                    onValueChange = { textFieldValue ->
                        if (!readOnly) {
                            weightInput.value = textFieldValue
                            val weight = textFieldValue.text.toFloatOrNull() ?: 0f
                            onUpdateSet(set.actualReps, weight, set.actualRpe)
                        }
                    },
                    fieldType = InputFieldType.WEIGHT,
                    placeholder = "",
                    modifier = Modifier.fillMaxSize(),
                    imeAction = ImeAction.Next,
                )
            }

            OneRMPercentageText(
                set = set,
                oneRMEstimate = oneRMEstimate,
            )
        }
    }
}

@Composable
private fun OneRMPercentageText(
    set: SetLog,
    oneRMEstimate: Float?,
) {
    val displayWeight = if (set.actualWeight > 0) set.actualWeight else set.targetWeight ?: 0f
    if (oneRMEstimate != null && oneRMEstimate > 0 && displayWeight > 0) {
        val percentage = ((displayWeight / oneRMEstimate) * 100).toInt()
        Text(
            text = "$percentage% of 1RM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun RepsInputColumn(
    repsInput: MutableState<TextFieldValue>,
    set: SetLog,
    readOnly: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CenteredInputField(
            value = repsInput.value,
            onValueChange = { textFieldValue ->
                if (!readOnly) {
                    repsInput.value = textFieldValue
                    val reps = textFieldValue.text.toIntOrNull() ?: 0
                    onUpdateSet(reps, set.actualWeight, set.actualRpe)
                }
            },
            fieldType = InputFieldType.REPS,
            placeholder = "",
            modifier = Modifier.fillMaxSize(),
            imeAction = ImeAction.Next,
        )
    }
}

@Composable
private fun RpeInputColumn(
    rpeInput: MutableState<TextFieldValue>,
    set: SetLog,
    readOnly: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CenteredInputField(
            value = rpeInput.value,
            onValueChange = { textFieldValue ->
                if (!readOnly) {
                    rpeInput.value = textFieldValue
                    val rpe = textFieldValue.text.toFloatOrNull()
                    onUpdateSet(set.actualReps, set.actualWeight, rpe)
                }
            },
            fieldType = InputFieldType.RPE,
            placeholder = "",
            modifier = Modifier.fillMaxSize(),
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun CompletionCheckbox(
    set: SetLog,
    readOnly: Boolean,
    canMarkComplete: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
            onCheckedChange = null,
            enabled = !readOnly && (canMarkComplete || set.isCompleted),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
        )
    }
}
