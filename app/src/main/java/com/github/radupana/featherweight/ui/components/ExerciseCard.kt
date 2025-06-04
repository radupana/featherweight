package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onAddSet: () -> Unit,
    onEditSet: (SetLog) -> Unit,
    onCopyLastSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onSmartAdd: (Long, String) -> Unit,
    onCompleteAllSets: (Long) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onUpdateSet: ((Long, Int, Float, Float?) -> Unit)? = null,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    var showValidationMessage by remember { mutableStateOf(false) }
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseMenuDialog by remember { mutableStateOf(false) }

    // Calculate completable sets - FIXED LOGIC
    val validSets = sets.filter { viewModel.canMarkSetComplete(it) }
    val incompleteValidSets = validSets.filter { !it.isCompleted }

    // Complete All should only be available when:
    // 1. All sets have valid data (reps + weight)
    // 2. At least one set is not completed
    // 3. There are sets to complete
    val canCompleteAll =
        sets.isNotEmpty() &&
            sets.all { viewModel.canMarkSetComplete(it) } &&
            incompleteValidSets.isNotEmpty()

    // Reset validation message when sets change
    LaunchedEffect(sets) {
        if (sets.all { viewModel.canMarkSetComplete(it) || it.isCompleted }) {
            showValidationMessage = false
        }
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onExpand() },
        elevation = CardDefaults.cardElevation(6.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Exercise header with menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (sets.isNotEmpty()) {
                        val completedSets = sets.count { it.isCompleted }
                        Surface(
                            color =
                                if (completedSets == sets.size) {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "$completedSets/${sets.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (completedSets == sets.size) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }

                    // Exercise menu button (only when can edit)
                    if (viewModel.canEditWorkout()) {
                        IconButton(
                            onClick = { showExerciseMenuDialog = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Exercise Options",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (sets.isEmpty()) {
                    Text(
                        "No sets yet. Add your first set below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Sets table header with Complete All button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Sets table header
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Set",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.12f),
                            )
                            Text(
                                "Reps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.18f),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Weight",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.22f),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "RPE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.15f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.weight(0.33f)) // For checkbox + actions
                        }

                        // Complete All button - FIXED LOGIC
                        if (canCompleteAll && viewModel.canEditWorkout()) {
                            TextButton(
                                onClick = { onCompleteAllSets(exercise.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Complete All",
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Complete All",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sets list
                    sets.forEach { set ->
                        SetRow(
                            set = set,
                            onToggleCompleted = { completed ->
                                if (completed && !viewModel.canMarkSetComplete(set)) {
                                    // User tried to complete an invalid set
                                    showValidationMessage = true
                                } else {
                                    viewModel.markSetCompleted(set.id, completed)
                                }
                            },
                            onEdit = { onEditSet(set) },
                            onDelete = { onDeleteSet(set.id) },
                            onUpdateSet = { reps, weight, rpe ->
                                viewModel.updateSet(set.id, reps, weight, rpe)
                            },
                            canMarkComplete = viewModel.canMarkSetComplete(set),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Validation message for sets that can't be completed
                    if (showValidationMessage) {
                        val invalidSets = sets.filter { set -> !viewModel.canMarkSetComplete(set) && !set.isCompleted }
                        if (invalidSets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "⚠️ Sets need both reps and weight to be marked complete",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    TextButton(
                                        onClick = { showValidationMessage = false },
                                        contentPadding = PaddingValues(4.dp),
                                    ) {
                                        Text(
                                            "✕",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons (Add Set, Copy Last)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddSet,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Set",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set")
                    }

                    if (sets.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onCopyLastSet(exercise.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy Last",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Last")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (sets.isEmpty()) "Tap to add sets" else "${sets.size} sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Exercise menu dialog
    if (showExerciseMenuDialog) {
        AlertDialog(
            onDismissRequest = { showExerciseMenuDialog = false },
            title = { Text("Exercise Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showExerciseMenuDialog = false
                            showDeleteExerciseDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Delete Exercise",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExerciseMenuDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    // Delete exercise confirmation dialog
    if (showDeleteExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteExerciseDialog = false },
            title = { Text("Delete Exercise") },
            text = {
                Text(
                    "Are you sure you want to delete \"${exercise.exerciseName}\" and all its sets? This action cannot be undone.",
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
                    Text("Delete Exercise")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteExerciseDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
