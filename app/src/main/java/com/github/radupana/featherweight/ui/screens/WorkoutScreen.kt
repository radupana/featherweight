package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.components.CompactRestTimer
import com.github.radupana.featherweight.ui.components.ExerciseCard
import com.github.radupana.featherweight.ui.components.PRCelebrationDialog
import com.github.radupana.featherweight.ui.components.WorkoutTimer
import com.github.radupana.featherweight.ui.dialogs.NotesInputModal
import com.github.radupana.featherweight.ui.dialogs.OneRMUpdateDialog
import com.github.radupana.featherweight.viewmodel.WorkoutState
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onSelectExercise: () -> Unit,
    onWorkoutComplete: (Long) -> Unit = {},
    onProgrammeComplete: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()

    val sets by viewModel.selectedExerciseSets.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()
    val pendingOneRMUpdates by viewModel.pendingOneRMUpdates.collectAsState()
    val expandedExerciseIds by viewModel.expandedExerciseIds.collectAsState()

    // Rest timer state
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val restTimerInitialSeconds by viewModel.restTimerInitialSeconds.collectAsState()

    // Workout timer state
    val workoutTimerSeconds by viewModel.workoutTimerSeconds.collectAsState()

    // Dialog state
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showEditWorkoutNameDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var showOneRMUpdateDialog by remember { mutableStateOf(false) }
    var shouldNavigateAfterCompletion by remember { mutableStateOf(false) }
    var showPRCelebration by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var currentNotes by remember { mutableStateOf("") }


    // Calculate progress stats
    val totalSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id } }
    val completedSets =
        exercises.sumOf { ex ->
            sets.count { it.exerciseLogId == ex.id && it.isCompleted }
        }

    // Check if workout has any exercises or sets - prevent completing empty workouts
    val hasContent = exercises.isNotEmpty()

    // If no active workout, start one
    LaunchedEffect(workoutState.isActive) {
        if (!workoutState.isActive && workoutState.status != com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
            viewModel.startNewWorkout()
        }
    }

    val canEdit = viewModel.canEditWorkout()

    // Show 1RM update dialog when there are pending updates
    LaunchedEffect(pendingOneRMUpdates, workoutState.status) {
        if (pendingOneRMUpdates.isNotEmpty() && workoutState.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
            showOneRMUpdateDialog = true
        }
    }

    // Show PR celebration when there are pending PRs
    LaunchedEffect(workoutState.shouldShowPRCelebration) {
        if (workoutState.shouldShowPRCelebration) {
            showPRCelebration = true
        }
    }

    // Navigate away after dealing with 1RM updates
    val currentWorkoutId by viewModel.currentWorkoutId.collectAsState()
    LaunchedEffect(shouldNavigateAfterCompletion, pendingOneRMUpdates, showOneRMUpdateDialog) {
        if (shouldNavigateAfterCompletion && pendingOneRMUpdates.isEmpty() && !showOneRMUpdateDialog) {
            currentWorkoutId?.let { workoutId ->
                onWorkoutComplete(workoutId)
            }
        }
    }


    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Main workout title
                                Text(
                                    text =
                                        if (workoutState.isProgrammeWorkout) {
                                            workoutState.programmeWorkoutName ?: viewModel.getWorkoutDisplayName()
                                        } else {
                                            viewModel.getWorkoutDisplayName()
                                        },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                // Subtitle - programme info or "Freestyle Workout"
                                if (workoutState.isProgrammeWorkout) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.FitnessCenter,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = buildString {
                                                workoutState.programmeName?.let { append(it) }
                                                if (workoutState.weekNumber != null && workoutState.dayNumber != null) {
                                                    if (isNotEmpty()) append(" • ")
                                                    append("W${workoutState.weekNumber}D${workoutState.dayNumber}")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Freestyle Workout",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Workout timer
                            WorkoutTimer(
                                seconds = workoutTimerSeconds,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Notes button
                    IconButton(onClick = {
                        // Load current notes from repository
                        currentWorkoutId?.let { workoutId ->
                            viewModel.loadWorkoutNotes(workoutId) { notes ->
                                currentNotes = notes ?: ""
                                showNotesModal = true
                            }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Workout Notes",
                            tint = if (currentNotes.isNotBlank()) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }

                    // Show repeat button for completed workouts, menu for active
                    if (!canEdit && workoutState.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
                        IconButton(onClick = { viewModel.repeatWorkout() }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Repeat Workout",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else if (canEdit) {
                        IconButton(onClick = { showWorkoutMenuDialog = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Workout Options")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            if (!canEdit) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                
                // Progress bar below TopAppBar
                if (totalSets > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { completedSets.toFloat() / totalSets },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = if (completedSets == totalSets) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$completedSets / $totalSets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (completedSets == totalSets) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // Exercises list or empty state
            if (workoutState.isLoadingExercises) {
                // Show loading indicator while exercises are being loaded
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (exercises.isEmpty()) {
                // Empty state - show buttons
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (canEdit) {
                        WorkoutActionButtons(
                            canCompleteWorkout = false,
                            onAddExercise = onSelectExercise,
                            onCompleteWorkout = {},
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                ExercisesList(
                    exercises = exercises,
                    sets = sets,
                    canEdit = canEdit,
                    canCompleteWorkout = workoutState.isActive,
                    expandedExerciseIds = expandedExerciseIds,
                    onDeleteExercise = { exerciseId ->
                        if (canEdit) {
                            viewModel.deleteExercise(exerciseId)
                        }
                    },
                    onSelectExercise = onSelectExercise,
                    onCompleteWorkout = { showCompleteWorkoutDialog = true },
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .imePadding() // Handle keyboard
                )
            }

            // Rest timer at bottom (if active)
            if (restTimerSeconds > 0) {
                CompactRestTimer(
                    seconds = restTimerSeconds,
                    initialSeconds = restTimerInitialSeconds,
                    onSkip = { viewModel.skipRestTimer() },
                    onPresetSelected = { viewModel.selectRestTimerPreset(it) },
                    onAdjustTime = { viewModel.adjustRestTimer(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Workout Menu Dialog
    if (showWorkoutMenuDialog) {
        WorkoutMenuDialog(
            canEdit = canEdit,
            canCompleteAllSets = viewModel.canCompleteAllSetsInWorkout(),
            onEditName = {
                showWorkoutMenuDialog = false
                showEditWorkoutNameDialog = true
            },
            onDeleteWorkout = {
                showWorkoutMenuDialog = false
                showDeleteWorkoutDialog = true
            },
            onCompleteAllSets = {
                showWorkoutMenuDialog = false
                viewModel.completeAllSetsInWorkout()
            },
            onClose = { showWorkoutMenuDialog = false },
        )
    }

    // Delete Workout Dialog
    if (showDeleteWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkoutDialog = false },
            title = { Text("Delete Workout") },
            text = {
                Text(
                    "Are you sure you want to delete this entire workout? This will remove all exercises and sets. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentWorkout()
                        showDeleteWorkoutDialog = false
                        onBack()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete Workout")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteWorkoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Edit Workout Name Dialog
    if (showEditWorkoutNameDialog && canEdit) {
        EditWorkoutNameDialog(
            currentName = workoutState.workoutName ?: "",
            onSave = { name ->
                viewModel.updateWorkoutName(name.takeIf { it.isNotBlank() })
                showEditWorkoutNameDialog = false
            },
            onDismiss = { showEditWorkoutNameDialog = false },
        )
    }

    // Complete Workout Dialog - FIXED: Only show if workout has content
    if (showCompleteWorkoutDialog && canEdit && hasContent) {
        CompleteWorkoutDialog(
            completedSets = completedSets,
            totalSets = totalSets,
            onComplete = {
                viewModel.completeWorkout(
                    onComplete = {
                        // Completion callback: let the workout completion finish before navigation
                        showCompleteWorkoutDialog = false
                        shouldNavigateAfterCompletion = true
                    },
                    onProgrammeComplete = { programmeId ->
                        // Programme completion callback
                        showCompleteWorkoutDialog = false
                        onProgrammeComplete(programmeId)
                    },
                )
            },
            onDismiss = { showCompleteWorkoutDialog = false },
        )
    }



    // 1RM Update Dialog
    if (showOneRMUpdateDialog && pendingOneRMUpdates.isNotEmpty()) {
        OneRMUpdateDialog(
            pendingUpdates = pendingOneRMUpdates,
            onApply = { update ->
                viewModel.applyOneRMUpdate(update)
            },
            onDismiss = {
                showOneRMUpdateDialog = false
                // If we were waiting to navigate after completion, do it now
                if (shouldNavigateAfterCompletion) {
                    onBack()
                }
            },
            onSkip = {
                viewModel.clearPendingOneRMUpdates()
                showOneRMUpdateDialog = false
                // If we were waiting to navigate after completion, do it now
                if (shouldNavigateAfterCompletion) {
                    onBack()
                }
            },
        )
    }

    // PR Celebration Dialog
    if (showPRCelebration && workoutState.pendingPRs.isNotEmpty()) {
        PRCelebrationDialog(
            personalRecords = workoutState.pendingPRs,
            onDismiss = {
                showPRCelebration = false
                viewModel.clearPendingPRs()
            },
        )
    }

    // Notes Input Modal
    NotesInputModal(
        isVisible = showNotesModal,
        title = "Workout Notes",
        initialNotes = currentNotes,
        onNotesChanged = { newNotes ->
            if (canEdit) {
                currentNotes = newNotes
                // Auto-save notes
                currentWorkoutId?.let { workoutId ->
                    viewModel.saveWorkoutNotes(workoutId, newNotes)
                }
            }
        },
        onDismiss = {
            showNotesModal = false
        },
        readOnly = !canEdit,
    )
}

@Composable
private fun WorkoutMenuDialog(
    canEdit: Boolean,
    canCompleteAllSets: Boolean,
    onEditName: () -> Unit,
    onDeleteWorkout: () -> Unit,
    onCompleteAllSets: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Workout Options") },
        text = {
            Column {
                // Only show edit options for non-completed workouts
                if (canEdit) {
                    if (canCompleteAllSets) {
                        TextButton(
                            onClick = onCompleteAllSets,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark all populated sets done")
                            }
                        }
                    }

                    TextButton(
                        onClick = onEditName,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Workout Name")
                        }
                    }

                    TextButton(
                        onClick = onDeleteWorkout,
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
                                "Delete Workout",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    Text(
                        "This workout is completed and cannot be modified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun EditWorkoutNameDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var workoutName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Workout Name") },
        text = {
            OutlinedTextField(
                value = workoutName,
                onValueChange = { workoutName = it },
                label = { Text("Workout name (optional)") },
                placeholder = { Text("e.g., Upper Body Push") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onSave(workoutName) }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CompleteWorkoutDialog(
    completedSets: Int,
    totalSets: Int,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val completionPercentage =
        if (totalSets > 0) {
            (completedSets * 100) / totalSets
        } else {
            100
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Workout?") },
        text = {
            Column {
                if (completionPercentage < 100) {
                    Text(
                        "You've completed $completionPercentage% of your sets ($completedSets/$totalSets).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Are you sure you want to finish this workout?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        "Great job! You've completed all $totalSets sets.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ready to finish this workout?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onComplete,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
            ) {
                Text("Complete Workout")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Continue Training")
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExercisesList(
    exercises: List<ExerciseLog>,
    sets: List<SetLog>,
    canEdit: Boolean,
    canCompleteWorkout: Boolean,
    expandedExerciseIds: Set<Long>,
    onDeleteExercise: (Long) -> Unit,
    onSelectExercise: () -> Unit,
    onCompleteWorkout: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(
            items = exercises,
            key = { exercise -> exercise.id },
        ) { exercise ->
            ExerciseCard(
                exercise = exercise,
                sets = sets.filter { it.exerciseLogId == exercise.id },
                isExpanded = expandedExerciseIds.contains(exercise.id),
                onToggleExpansion = { viewModel.toggleExerciseExpansion(exercise.id) },
                onDeleteExercise = { exerciseId ->
                    if (canEdit) onDeleteExercise(exerciseId)
                },
                onSwapExercise = { exerciseId ->
                    if (canEdit) {
                        viewModel.initiateExerciseSwap(exerciseId)
                        onSelectExercise()
                    }
                },
                viewModel = viewModel,
                modifier = Modifier.animateItem(),
            )
        }

        // Action buttons at the end of the list
        if (canEdit) {
            item {
                WorkoutActionButtons(
                    canCompleteWorkout = canCompleteWorkout && exercises.isNotEmpty(),
                    onAddExercise = onSelectExercise,
                    onCompleteWorkout = onCompleteWorkout,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkoutActionButtons(
    canCompleteWorkout: Boolean,
    onAddExercise: () -> Unit,
    onCompleteWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Add Exercise button
        OutlinedButton(
            onClick = onAddExercise,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Exercise")
        }

        // Complete Workout button
        if (canCompleteWorkout) {
            Button(
                onClick = onCompleteWorkout,
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete")
            }
        }
    }
}

