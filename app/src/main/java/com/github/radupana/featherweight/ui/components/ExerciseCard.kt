package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onSwapExercise: (Long) -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false, // Add this parameter for visual feedback
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier, // Modifier for the drag handle
) {
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    val completedSets = sets.count { it.isCompleted }

    GlassCard(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (isDragging) {
                        Modifier
                            .scale(1.02f)
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    },
                ),
        onClick = null,
        elevation = if (isDragging) 4.dp else 2.dp,
    ) {
        // Delete confirmation dialog
        if (showDeleteExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteExerciseDialog = false },
                icon = {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                title = { Text("Delete Exercise?") },
                text = {
                    Text(
                        "Are you sure you want to delete this exercise and all its sets? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteExercise(exercise.id)
                            showDeleteExerciseDialog = false
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
                        onClick = { showDeleteExerciseDialog = false },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
        // Compact header with action buttons
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpansion),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drag handle (if enabled)
            if (showDragHandle) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier =
                        dragHandleModifier
                            .padding(end = 8.dp)
                            .size(24.dp),
                    tint =
                        if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                )
            }

            // Chevron icon for expand/collapse
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 0f else -90f,
                animationSpec = tween(200),
                label = "chevron rotation",
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier =
                    Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Exercise name (no progress info for cleaner look)
            val exerciseNames by viewModel.exerciseNames.collectAsState()
            val exerciseName = exerciseNames[exercise.exerciseVariationId] ?: "Unknown Exercise"
            
            // Exercise name with last performance
            val lastPerformance by viewModel.lastPerformance.collectAsState()
            val lastSet = lastPerformance[exercise.exerciseVariationId]
            
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                // Show last performance if available
                lastSet?.let { set ->
                    Text(
                        text = "Last: ${WeightFormatter.formatWeight(set.actualWeight)} × ${set.actualReps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // Set completion counter
            if (sets.isNotEmpty()) {
                Text(
                    text = "$completedSets/${sets.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        when {
                            completedSets == 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            completedSets == sets.size -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontWeight = if (completedSets > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            // Action buttons (swap and delete) - only show when editable
            if (viewModel.canEditWorkout()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onSwapExercise(exercise.id) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Swap Exercise",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { showDeleteExerciseDialog = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Exercise",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Expandable content with animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Column headers
                if (sets.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Set number column
                        Box(
                            modifier = Modifier.width(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "#",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Target column for programme workouts
                        val isProgrammeWorkout =
                            viewModel.workoutState
                                .collectAsState()
                                .value.isProgrammeWorkout
                        if (isProgrammeWorkout) {
                            Text(
                                "Target",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.Center,
                            )
                        }

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

                        // Dedicated checkbox column - complete all sets button
                        Box(
                            modifier = Modifier.width(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val canCompleteAllSets = viewModel.canCompleteAllSetsInExercise(exercise.id)
                            if (viewModel.canEditWorkout() && canCompleteAllSets) {
                                IconButton(
                                    onClick = { viewModel.completeAllSetsInExercise(exercise.id) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Complete all populated sets",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }

                // Sets list
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Get 1RM for percentage calculations
                    val oneRMEstimates by viewModel.oneRMEstimates.collectAsState()
                    val oneRMEstimate = oneRMEstimates[exercise.exerciseVariationId]
                    val isProgrammeWorkout =
                        viewModel.workoutState
                            .collectAsState()
                            .value.isProgrammeWorkout

                    sets.forEachIndexed { index, set ->
                        key(set.id) {
                            CleanSetRow(
                                set = set,
                                setNumber = index + 1,
                                oneRMEstimate = oneRMEstimate,
                                isProgrammeWorkout = isProgrammeWorkout,
                                onUpdateSet = { reps, weight, rpe ->
                                    viewModel.updateSet(set.id, reps, weight, rpe)
                                },
                                onToggleCompleted = { completed ->
                                    viewModel.markSetCompleted(set.id, completed)
                                },
                                onDeleteSet = {
                                    viewModel.deleteSet(set.id)
                                },
                                canMarkComplete = set.actualReps > 0 && set.actualWeight > 0,
                                readOnly = !viewModel.canEditWorkout(),
                            )
                        }
                    }

                    // Action buttons for adding sets
                    if (viewModel.canEditWorkout()) {
                        val lastSet = sets.maxByOrNull { it.setOrder }
                        val canCopyLast = lastSet != null && (lastSet.actualReps > 0 || lastSet.actualWeight > 0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.addSet(exercise.id) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Set")
                            }

                            if (canCopyLast) {
                                OutlinedButton(
                                    onClick = { viewModel.copyLastSet(exercise.id) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CleanSetRow(
    set: SetLog,
    setNumber: Int,
    oneRMEstimate: Float?,
    isProgrammeWorkout: Boolean,
    onUpdateSet: (Int, Float, Float?) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDeleteSet: () -> Unit,
    canMarkComplete: Boolean,
    readOnly: Boolean,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (!readOnly) {
                            onDeleteSet()
                            true // Confirm the dismissal
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
            positionalThreshold = { totalDistance ->
                totalDistance * 0.33f // Require 33% swipe distance
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val progress = dismissState.progress
            val targetValue = dismissState.targetValue
            val currentValue = dismissState.currentValue

            // Show red only when swiping to delete and not already dismissed
            if (progress > 0.01f &&
                targetValue == SwipeToDismissBoxValue.EndToStart &&
                currentValue != SwipeToDismissBoxValue.EndToStart
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    // Dynamic red stripe that grows with swipe
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
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !readOnly,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Set number column - match header layout
                Box(
                    modifier = Modifier.width(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$setNumber",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                // Target column for programme workouts
                if (isProgrammeWorkout) {
                    val targetDisplay =
                        if (set.targetReps != null && set.targetReps > 0) {
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
                }

                // Weight input with % of 1RM below
                Box(
                    modifier = Modifier.weight(0.8f),
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
                            if (!set.isCompleted && !readOnly) {
                                var weightValue by remember(set.id) {
                                    mutableStateOf(
                                        TextFieldValue(
                                            text = if (set.actualWeight > 0) WeightFormatter.formatWeight(set.actualWeight) else "",
                                            selection = TextRange.Zero,
                                        ),
                                    )
                                }

                                CenteredInputField(
                                    value = weightValue,
                                    onValueChange = { newValue ->
                                        weightValue = newValue
                                        val weight = newValue.text.toFloatOrNull() ?: 0f
                                        onUpdateSet(set.actualReps, weight, set.actualRpe)
                                    },
                                    fieldType = InputFieldType.WEIGHT,
                                    placeholder = "",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = if (set.actualWeight > 0) WeightFormatter.formatWeight(set.actualWeight) else "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (set.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Show percentage of 1RM below the weight
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
                }

                // Reps input
                Box(
                    modifier =
                        Modifier
                            .weight(0.6f)
                            .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!set.isCompleted && !readOnly) {
                        var repsValue by remember(set.id) {
                            mutableStateOf(
                                TextFieldValue(
                                    text = if (set.actualReps > 0) set.actualReps.toString() else "",
                                    selection = TextRange.Zero,
                                ),
                            )
                        }

                        CenteredInputField(
                            value = repsValue,
                            onValueChange = { newValue ->
                                repsValue = newValue
                                val reps = newValue.text.toIntOrNull() ?: 0
                                onUpdateSet(reps, set.actualWeight, set.actualRpe)
                            },
                            fieldType = InputFieldType.REPS,
                            placeholder = "",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = if (set.actualReps > 0) set.actualReps.toString() else "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (set.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // RPE input
                Box(
                    modifier =
                        Modifier
                            .weight(0.6f)
                            .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!set.isCompleted && !readOnly) {
                        var rpeValue by remember(set.id) {
                            mutableStateOf(
                                TextFieldValue(
                                    text = set.actualRpe?.toString() ?: "",
                                    selection = TextRange.Zero,
                                ),
                            )
                        }

                        CenteredInputField(
                            value = rpeValue,
                            onValueChange = { newValue ->
                                rpeValue = newValue
                                val rpe = newValue.text.toIntOrNull()?.toFloat()
                                onUpdateSet(set.actualReps, set.actualWeight, rpe)
                            },
                            fieldType = InputFieldType.RPE,
                            placeholder = "",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = set.actualRpe?.toString() ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (set.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Dedicated checkbox column - match header layout
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
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
                            onCheckedChange = null,
                            enabled = !readOnly && (canMarkComplete || set.isCompleted),
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                }
            }
        }
    }
}
