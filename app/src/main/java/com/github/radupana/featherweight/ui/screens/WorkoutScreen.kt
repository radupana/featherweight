package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.components.CompactExerciseCard
import com.github.radupana.featherweight.ui.components.CompactRestTimer
import com.github.radupana.featherweight.ui.components.PRCelebrationDialog
import com.github.radupana.featherweight.ui.components.WorkoutTimer
import com.github.radupana.featherweight.ui.dialogs.NotesInputModal
import com.github.radupana.featherweight.ui.dialogs.OneRMUpdateDialog
import com.github.radupana.featherweight.ui.dialogs.SetEditingModal
import com.github.radupana.featherweight.ui.dialogs.SmartEditSetDialog
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.WorkoutState
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlin.math.roundToInt

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

    // Rest timer state
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val restTimerInitialSeconds by viewModel.restTimerInitialSeconds.collectAsState()

    // Workout timer state
    val workoutTimerSeconds by viewModel.workoutTimerSeconds.collectAsState()

    // Dialog state
    var showEditSetDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showEditWorkoutNameDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var showOneRMUpdateDialog by remember { mutableStateOf(false) }
    var shouldNavigateAfterCompletion by remember { mutableStateOf(false) }
    var showPRCelebration by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var currentNotes by remember { mutableStateOf("") }

    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    var editingExerciseName by remember { mutableStateOf<String?>(null) }

    // Set editing modal state
    var showSetEditingModal by remember { mutableStateOf(false) }
    var setEditingExercise by remember { mutableStateOf<ExerciseLog?>(null) }

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

    // Handle back press
    val handleBack = {
        onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
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

                                // Programme subtitle (if this is a programme workout)
                                if (workoutState.isProgrammeWorkout) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Filled.FitnessCenter,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text =
                                                buildString {
                                                    workoutState.programmeName?.let { append(it) }
                                                    if (workoutState.weekNumber != null && workoutState.dayNumber != null) {
                                                        if (isNotEmpty()) append(" • ")
                                                        append("Week ${workoutState.weekNumber}, Day ${workoutState.dayNumber}")
                                                    }
                                                },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            // Workout timer
                            WorkoutTimer(
                                seconds = workoutTimerSeconds,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
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
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(NavigationContext.BOTTOM_NAVIGATION),
            ) {
                // Normal workout view

                // Unified Progress Card
                UnifiedProgressCard(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    workoutState = workoutState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(16.dp),
                )

                // Removed rest timer from here - moved to bottom

                // Exercises list or empty state
                if (workoutState.isLoadingExercises) {
                    // Show loading indicator while exercises are being loaded
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = if (restTimerSeconds > 0) 56.dp else 0.dp), // Add padding when timer is visible
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
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = if (restTimerSeconds > 0) 56.dp else 0.dp), // Add padding when timer is visible
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
                        onDeleteExercise = { exerciseId ->
                            if (canEdit) {
                                viewModel.deleteExercise(exerciseId)
                            }
                        },
                        onOpenSetEditingModal = { exerciseId ->
                            val exercise = exercises.find { it.id == exerciseId }
                            if (exercise != null) {
                                setEditingExercise = exercise
                                showSetEditingModal = true
                                viewModel.loadSetsForExercise()
                                viewModel.loadExerciseHistoryForName(exercise.exerciseName)
                            }
                        },
                        onSelectExercise = onSelectExercise,
                        onCompleteWorkout = { showCompleteWorkoutDialog = true },
                        viewModel = viewModel,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(bottom = if (restTimerSeconds > 0) 56.dp else 0.dp),
                        // Add padding when timer is visible
                    )
                }
            }

            // Rest timer at bottom (if active)
            if (restTimerSeconds > 0) {
                CompactRestTimer(
                    seconds = restTimerSeconds,
                    initialSeconds = restTimerInitialSeconds,
                    onSkip = { viewModel.skipRestTimer() },
                    onPresetSelected = { viewModel.selectRestTimerPreset(it) },
                    onAdjustTime = { viewModel.adjustRestTimer(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .systemBarsPadding(NavigationContext.BOTTOM_NAVIGATION), // Position above nav bar
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

    // Edit Set Dialog
    if (showEditSetDialog && editingExerciseName != null && canEdit) {
        SmartEditSetDialog(
            set = editingSet,
            exerciseName = editingExerciseName!!,
            onDismiss = {
                showEditSetDialog = false
                editingSet = null
                editingExerciseName = null
            },
            onSave = { reps, weight, rpe ->
                if (editingSet != null) {
                    viewModel.updateSet(editingSet!!.id, reps, weight, rpe)
                } else {
                    val exercise = exercises.find { it.exerciseName == editingExerciseName }
                    exercise?.let { viewModel.addSetToExercise(it.id, reps, weight, weight, reps, rpe) }
                }
                showEditSetDialog = false
                editingSet = null
                editingExerciseName = null
            },
            viewModel = viewModel,
        )
    }

    // Set Editing Modal
    if (showSetEditingModal && setEditingExercise != null) {
        SetEditingModal(
            exercise = setEditingExercise!!,
            sets = sets.filter { it.exerciseLogId == setEditingExercise!!.id },
            workoutTimerSeconds = workoutTimerSeconds,
            onDismiss = {
                showSetEditingModal = false
                setEditingExercise = null
            },
            onNavigateBack = if (!canEdit) onBack else null,
            onUpdateSet = { setId, reps, weight, rpe ->
                if (canEdit) viewModel.updateSet(setId, reps, weight, rpe)
            },
            onAddSet = { onSetCreated ->
                if (canEdit) viewModel.addSetToExercise(setEditingExercise!!.id, onSetCreated = onSetCreated)
            },
            onCopyLastSet = {
                if (canEdit) {
                    val lastSet =
                        sets
                            .filter { it.exerciseLogId == setEditingExercise!!.id }
                            .maxByOrNull { it.setOrder }
                    if (lastSet != null) {
                        viewModel.addSetToExercise(
                            setEditingExercise!!.id,
                            lastSet.actualReps,
                            lastSet.actualWeight,
                            lastSet.actualWeight,
                            lastSet.actualReps,
                            lastSet.actualRpe,
                        )
                    } else {
                        viewModel.addSetToExercise(setEditingExercise!!.id)
                    }
                }
            },
            onDeleteSet = { setId ->
                if (canEdit) viewModel.deleteSet(setId)
            },
            onToggleCompleted = { setId, completed ->
                if (canEdit) {
                    viewModel.markSetCompleted(setId, completed)
                }
            },
            onCompleteAllSets = {
                if (canEdit) {
                    viewModel.completeAllSetsInExercise(setEditingExercise!!.id)
                }
            },
            viewModel = viewModel,
            isProgrammeWorkout = workoutState.isProgrammeWorkout,
            readOnly = !canEdit,
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
    onDeleteExercise: (Long) -> Unit,
    onOpenSetEditingModal: (Long) -> Unit,
    onSelectExercise: () -> Unit,
    onCompleteWorkout: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    var draggedExerciseId by remember { mutableStateOf<Long?>(null) }
    var draggedOffset by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    // Item measurements - CompactExerciseCard height + spacing
    val cardHeightDp = 80.dp // Estimated height of CompactExerciseCard
    val spacingDp = 8.dp // From verticalArrangement
    val itemHeightDp = cardHeightDp + spacingDp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Helper function to find current index of an exercise
    fun findCurrentIndex(exerciseId: Long): Int = exercises.indexOfFirst { it.id == exerciseId }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), // Reduced spacing
        modifier = modifier,
    ) {
        items(
            items = exercises,
            key = { exercise -> exercise.id },
        ) { exercise ->
            // Find the current index dynamically
            val currentPosition = findCurrentIndex(exercise.id)
            val isDragged = draggedExerciseId == exercise.id

            // Make sure dragged items observe the offset with derivedStateOf
            val currentOffset by remember(isDragged) {
                derivedStateOf { if (isDragged) draggedOffset else 0f }
            }

            // Calculate translation for non-dragged items
            val shouldTranslate =
                if (!isDragged && draggedExerciseId != null && targetIndex != null && initialDragIndex != null) {
                    val fromIdx = initialDragIndex!!
                    val toIdx = targetIndex!!

                    // Only move items between the drag positions
                    when {
                        fromIdx < toIdx && currentPosition in (fromIdx + 1)..toIdx -> -itemHeightPx
                        fromIdx > toIdx && currentPosition in toIdx..<fromIdx -> itemHeightPx
                        else -> 0f
                    }
                } else {
                    0f
                }

            val animatedTranslation by animateFloatAsState(
                targetValue = shouldTranslate,
                animationSpec =
                    tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing,
                    ),
                label = "translation_${exercise.id}",
            )

            CompactExerciseCard(
                exercise = exercise,
                sets = sets.filter { it.exerciseLogId == exercise.id },
                onEditSets = {
                    onOpenSetEditingModal(exercise.id)
                },
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
                showDragHandle = canEdit,
                onDragStart = {
                    // Get the current exercises list from ViewModel to ensure we have the latest
                    val currentExercisesList = viewModel.selectedWorkoutExercises.value
                    val startIndex = currentExercisesList.indexOfFirst { it.id == exercise.id }

                    if (startIndex == -1) {
                        return@CompactExerciseCard
                    }

                    draggedExerciseId = exercise.id
                    initialDragIndex = startIndex
                    targetIndex = startIndex
                    draggedOffset = 0f
                },
                onDragEnd = {
                    val finalTarget = targetIndex
                    val startIndex = initialDragIndex

                    // Perform the reorder if needed
                    if (startIndex != null && finalTarget != null && startIndex != finalTarget) {
                        viewModel.reorderExercisesInstantly(startIndex, finalTarget)
                    }

                    // Reset state
                    draggedExerciseId = null
                    targetIndex = null
                    initialDragIndex = null
                    draggedOffset = 0f
                },
                onDrag = { delta ->
                    // Only process drag for the item that initiated the drag
                    if (draggedExerciseId == exercise.id && initialDragIndex != null) {
                        draggedOffset += delta

                        // Get current exercises count from ViewModel
                        val currentCount = viewModel.selectedWorkoutExercises.value.size

                        // Calculate target position based on drag offset
                        val dragDirection = if (draggedOffset > 0) 1 else -1
                        val absoluteOffset = kotlin.math.abs(draggedOffset)
                        val positionsMovedFloat = absoluteOffset / itemHeightPx
                        val positionsMoved = positionsMovedFloat.toInt()

                        // Calculate new target index
                        var newTargetIndex = initialDragIndex!! + (positionsMoved * dragDirection)

                        // Add hysteresis to prevent jittery movement
                        // Only change target if we're more than 60% into the next position
                        val remainder = positionsMovedFloat - positionsMoved
                        if (remainder > 0.6f && dragDirection > 0) {
                            newTargetIndex++
                        } else if (remainder > 0.6f && dragDirection < 0) {
                            newTargetIndex--
                        }

                        newTargetIndex = newTargetIndex.coerceIn(0, currentCount - 1)

                        // Only update when target changes
                        if (newTargetIndex != targetIndex) {
                            targetIndex = newTargetIndex
                        }
                    }
                },
                modifier =
                    Modifier
                        .graphicsLayer {
                            translationY = if (isDragged) currentOffset else animatedTranslation
                            this.shadowElevation = if (isDragged) 8.dp.toPx() else 0f
                            alpha = if (isDragged) 0.9f else 1f
                        }.zIndex(if (isDragged) 1f else 0f),
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

@Composable
private fun UnifiedProgressCard(
    completedSets: Int,
    totalSets: Int,
    workoutState: WorkoutState,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    var programmeProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Load programme progress if this is a programme workout
    LaunchedEffect(workoutState.programmeId) {
        if (workoutState.isProgrammeWorkout && workoutState.programmeId != null) {
            programmeProgress = viewModel.getProgrammeProgress()
        }
    }

    val workoutProgress = if (totalSets > 0) completedSets.toFloat() / totalSets.toFloat() else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Programme section (if applicable)
            if (workoutState.isProgrammeWorkout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = workoutState.programmeName ?: "Programme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    programmeProgress?.let { (completed, total) ->
                        if (total > 0) {
                            Text(
                                text = "$completed/$total workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                if (workoutState.weekNumber != null && workoutState.dayNumber != null) {
                    Text(
                        text = "Week ${workoutState.weekNumber} • Day ${workoutState.dayNumber} • ${workoutState.programmeWorkoutName ?: "Workout"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Workout section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (workoutState.isProgrammeWorkout) "Workout Progress" else "Freestyle Workout",
                    style = if (workoutState.isProgrammeWorkout) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = if (workoutState.isProgrammeWorkout) FontWeight.Medium else FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { workoutProgress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${(workoutProgress * 100).roundToInt()}% completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
