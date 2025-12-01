package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.PendingOneRMUpdate
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.WorkoutMode
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.ui.components.CompactRestTimer
import com.github.radupana.featherweight.ui.components.ExerciseCard
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.components.PRCelebrationDialog
import com.github.radupana.featherweight.ui.components.WorkoutTimer
import com.github.radupana.featherweight.ui.dialogs.NotesInputModal
import com.github.radupana.featherweight.ui.dialogs.OneRMUpdateDialog
import com.github.radupana.featherweight.util.DeviationFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutState
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onSelectExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onWorkoutComplete: (String) -> Unit = {},
    onProgrammeComplete: (String) -> Unit = {},
    onTemplateSaved: () -> Unit = {},
    onSaveAsTemplate: (String) -> Unit = {},
    onExportWorkout: (String) -> Unit = {},
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

    // Observe lifecycle to resume timers when app returns from background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Resume rest timer if it was active
                    viewModel.resumeRestTimerIfActive()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dialog state
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showCompletedWorkoutMenu by remember { mutableStateOf(false) }
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

    val canEdit = viewModel.canEditWorkout()
    val currentWorkoutId by viewModel.currentWorkoutId.collectAsState()

    WorkoutStateEffects(
        workoutState = workoutState,
        pendingOneRMUpdates = pendingOneRMUpdates,
        shouldNavigateAfterCompletion = shouldNavigateAfterCompletion,
        showOneRMUpdateDialog = showOneRMUpdateDialog,
        currentWorkoutId = currentWorkoutId,
        viewModel = viewModel,
        onWorkoutComplete = onWorkoutComplete,
        onShowOneRMUpdateDialog = { showOneRMUpdateDialog = it },
        onShowPRCelebration = { showPRCelebration = it },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            WorkoutHeader(
                workoutState = workoutState,
                workoutTimerSeconds = workoutTimerSeconds,
                totalSets = totalSets,
                completedSets = completedSets,
                canEdit = canEdit,
                currentWorkoutId = currentWorkoutId,
                currentNotes = currentNotes,
                viewModel = viewModel,
                onBack = onBack,
                onShowWorkoutMenuDialog = { showWorkoutMenuDialog = true },
                onShowCompletedWorkoutMenu = { showCompletedWorkoutMenu = true },
                onShowNotesModal = { workoutId ->
                    viewModel.loadWorkoutNotes(workoutId) { notes ->
                        currentNotes = notes ?: ""
                        showNotesModal = true
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(top = innerPadding.calculateTopPadding()) // Only apply top padding
                    .fillMaxSize(),
        ) {
            // Exercises list or empty state
            if (workoutState.isLoadingExercises) {
                // Show loading indicator while exercises are being loaded
                Box(
                    modifier =
                        Modifier
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
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (canEdit) {
                        WorkoutActionButtons(
                            canCompleteWorkout = false,
                            isTemplateEdit = workoutState.mode == WorkoutMode.TEMPLATE_EDIT,
                            onAddExercise = onSelectExercise,
                            onCompleteWorkout = {},
                            onSaveTemplate = {
                                viewModel.saveTemplateChanges()
                                onTemplateSaved()
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                ExercisesList(
                    exercises = exercises,
                    sets = sets,
                    canEdit = canEdit,
                    canCompleteWorkout = workoutState.isActive && workoutState.mode != WorkoutMode.TEMPLATE_EDIT,
                    completedSets = completedSets,
                    expandedExerciseIds = expandedExerciseIds,
                    workoutState = workoutState,
                    onDeleteExercise = { exerciseId ->
                        if (canEdit) {
                            viewModel.deleteExercise(exerciseId)
                        }
                    },
                    onSelectExercise = onSelectExercise,
                    onCompleteWorkout = { showCompleteWorkoutDialog = true },
                    onTemplateSaved = onTemplateSaved,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }

            // Rest timer at bottom (if active) - positioned at absolute bottom
            WorkoutFooter(
                restTimerSeconds = restTimerSeconds,
                restTimerInitialSeconds = restTimerInitialSeconds,
                viewModel = viewModel,
            )
        }
    }

    WorkoutDialogs(
        showWorkoutMenuDialog = showWorkoutMenuDialog,
        showDeleteWorkoutDialog = showDeleteWorkoutDialog,
        showEditWorkoutNameDialog = showEditWorkoutNameDialog,
        showCompleteWorkoutDialog = showCompleteWorkoutDialog,
        showCompletedWorkoutMenu = showCompletedWorkoutMenu,
        showOneRMUpdateDialog = showOneRMUpdateDialog,
        showPRCelebration = showPRCelebration,
        showNotesModal = showNotesModal,
        canEdit = canEdit,
        hasContent = hasContent,
        completedSets = completedSets,
        totalSets = totalSets,
        workoutState = workoutState,
        pendingOneRMUpdates = pendingOneRMUpdates,
        shouldNavigateAfterCompletion = shouldNavigateAfterCompletion,
        currentNotes = currentNotes,
        currentWorkoutId = currentWorkoutId,
        viewModel = viewModel,
        onBack = onBack,
        onProgrammeComplete = onProgrammeComplete,
        onSaveAsTemplate = onSaveAsTemplate,
        onExportWorkout = onExportWorkout,
        onWorkoutMenuDialogChange = { showWorkoutMenuDialog = it },
        onDeleteWorkoutDialogChange = { showDeleteWorkoutDialog = it },
        onEditWorkoutNameDialogChange = { showEditWorkoutNameDialog = it },
        onCompleteWorkoutDialogChange = { showCompleteWorkoutDialog = it },
        onCompletedWorkoutMenuChange = { showCompletedWorkoutMenu = it },
        onOneRMUpdateDialogChange = { showOneRMUpdateDialog = it },
        onPRCelebrationChange = { showPRCelebration = it },
        onNotesModalChange = { showNotesModal = it },
        onCurrentNotesChange = { currentNotes = it },
        onShouldNavigateAfterCompletionChange = { shouldNavigateAfterCompletion = it },
    )
}

@Composable
private fun WorkoutStateEffects(
    workoutState: WorkoutState,
    pendingOneRMUpdates: List<PendingOneRMUpdate>,
    shouldNavigateAfterCompletion: Boolean,
    showOneRMUpdateDialog: Boolean,
    currentWorkoutId: String?,
    viewModel: WorkoutViewModel,
    onWorkoutComplete: (String) -> Unit,
    onShowOneRMUpdateDialog: (Boolean) -> Unit,
    onShowPRCelebration: (Boolean) -> Unit,
) {
    // If no active workout, start one (except in template edit mode or when viewing completed workout)
    LaunchedEffect(workoutState.isActive, workoutState.mode, workoutState.isReadOnly, currentWorkoutId) {
        if (!workoutState.isActive &&
            workoutState.status != WorkoutStatus.COMPLETED &&
            workoutState.mode != WorkoutMode.TEMPLATE_EDIT &&
            !workoutState.isReadOnly &&
            currentWorkoutId == null // Don't start new workout if we're already loading one
        ) {
            viewModel.startNewWorkout()
        }
    }

    // Show 1RM update dialog when there are pending updates
    LaunchedEffect(pendingOneRMUpdates, workoutState.status) {
        if (pendingOneRMUpdates.isNotEmpty() && workoutState.status == WorkoutStatus.COMPLETED) {
            onShowOneRMUpdateDialog(true)
        }
    }

    // Show PR celebration when there are pending PRs
    LaunchedEffect(workoutState.shouldShowPRCelebration) {
        if (workoutState.shouldShowPRCelebration) {
            onShowPRCelebration(true)
        }
    }

    // Navigate away after dealing with 1RM updates
    LaunchedEffect(shouldNavigateAfterCompletion, pendingOneRMUpdates, showOneRMUpdateDialog) {
        if (shouldNavigateAfterCompletion && pendingOneRMUpdates.isEmpty() && !showOneRMUpdateDialog) {
            currentWorkoutId?.let { workoutId ->
                onWorkoutComplete(workoutId)
            }
        }
    }
}

@Composable
private fun WorkoutDialogs(
    showWorkoutMenuDialog: Boolean,
    showDeleteWorkoutDialog: Boolean,
    showEditWorkoutNameDialog: Boolean,
    showCompleteWorkoutDialog: Boolean,
    showCompletedWorkoutMenu: Boolean,
    showOneRMUpdateDialog: Boolean,
    showPRCelebration: Boolean,
    showNotesModal: Boolean,
    canEdit: Boolean,
    hasContent: Boolean,
    completedSets: Int,
    totalSets: Int,
    workoutState: WorkoutState,
    pendingOneRMUpdates: List<PendingOneRMUpdate>,
    shouldNavigateAfterCompletion: Boolean,
    currentNotes: String,
    currentWorkoutId: String?,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onProgrammeComplete: (String) -> Unit,
    onSaveAsTemplate: (String) -> Unit,
    onExportWorkout: (String) -> Unit,
    onWorkoutMenuDialogChange: (Boolean) -> Unit,
    onDeleteWorkoutDialogChange: (Boolean) -> Unit,
    onEditWorkoutNameDialogChange: (Boolean) -> Unit,
    onCompleteWorkoutDialogChange: (Boolean) -> Unit,
    onCompletedWorkoutMenuChange: (Boolean) -> Unit,
    onOneRMUpdateDialogChange: (Boolean) -> Unit,
    onPRCelebrationChange: (Boolean) -> Unit,
    onNotesModalChange: (Boolean) -> Unit,
    onCurrentNotesChange: (String) -> Unit,
    onShouldNavigateAfterCompletionChange: (Boolean) -> Unit,
) {
    // Workout Menu Dialog
    if (showWorkoutMenuDialog) {
        WorkoutMenuDialog(
            canEdit = canEdit,
            canCompleteAllSets = viewModel.canCompleteAllSetsInWorkout(),
            onEditName = {
                onWorkoutMenuDialogChange(false)
                onEditWorkoutNameDialogChange(true)
            },
            onDeleteWorkout = {
                onWorkoutMenuDialogChange(false)
                onDeleteWorkoutDialogChange(true)
            },
            onCompleteAllSets = {
                onWorkoutMenuDialogChange(false)
                viewModel.completeAllSetsInWorkout()
            },
            onClose = { onWorkoutMenuDialogChange(false) },
        )
    }

    if (showCompletedWorkoutMenu) {
        CompletedWorkoutMenuDialog(
            onSaveAsTemplate = {
                onCompletedWorkoutMenuChange(false)
                currentWorkoutId?.let { onSaveAsTemplate(it) }
            },
            onExportWorkout = {
                onCompletedWorkoutMenuChange(false)
                currentWorkoutId?.let { onExportWorkout(it) }
            },
            onClose = { onCompletedWorkoutMenuChange(false) },
        )
    }

    // Delete Workout Dialog
    if (showDeleteWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { onDeleteWorkoutDialogChange(false) },
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
                        onDeleteWorkoutDialogChange(false)
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
                OutlinedButton(onClick = { onDeleteWorkoutDialogChange(false) }) {
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
                onEditWorkoutNameDialogChange(false)
            },
            onDismiss = { onEditWorkoutNameDialogChange(false) },
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
                        onCompleteWorkoutDialogChange(false)
                        onShouldNavigateAfterCompletionChange(true)
                    },
                    onProgrammeComplete = { programmeId ->
                        // Programme completion callback
                        onCompleteWorkoutDialogChange(false)
                        onProgrammeComplete(programmeId)
                    },
                )
            },
            onDismiss = { onCompleteWorkoutDialogChange(false) },
        )
    }

    // 1RM Update Dialog
    if (showOneRMUpdateDialog && pendingOneRMUpdates.isNotEmpty()) {
        val exerciseNames by viewModel.exerciseNames.collectAsState()
        OneRMUpdateDialog(
            pendingUpdates = pendingOneRMUpdates,
            exerciseNames = exerciseNames,
            onApply = { update ->
                viewModel.applyOneRMUpdate(update)
            },
            onDismiss = {
                onOneRMUpdateDialogChange(false)
                // If we were waiting to navigate after completion, do it now
                if (shouldNavigateAfterCompletion) {
                    onBack()
                }
            },
            onSkip = {
                viewModel.clearPendingOneRMUpdates()
                onOneRMUpdateDialogChange(false)
                // If we were waiting to navigate after completion, do it now
                if (shouldNavigateAfterCompletion) {
                    onBack()
                }
            },
        )
    }

    // PR Celebration Dialog
    if (showPRCelebration && workoutState.pendingPRs.isNotEmpty()) {
        val prExerciseNames by viewModel.exerciseNames.collectAsState()
        PRCelebrationDialog(
            personalRecords = workoutState.pendingPRs,
            exerciseNames = prExerciseNames,
            onDismiss = {
                onPRCelebrationChange(false)
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
                onCurrentNotesChange(newNotes)
                // Auto-save notes
                currentWorkoutId?.let { workoutId ->
                    viewModel.saveWorkoutNotes(workoutId, newNotes)
                }
            }
        },
        onDismiss = {
            onNotesModalChange(false)
        },
        readOnly = !canEdit,
    )
}

@Composable
private fun WorkoutFooter(
    restTimerSeconds: Int,
    restTimerInitialSeconds: Int,
    viewModel: WorkoutViewModel,
) {
    if (restTimerSeconds > 0) {
        CompactRestTimer(
            seconds = restTimerSeconds,
            initialSeconds = restTimerInitialSeconds,
            onSkip = { viewModel.skipRestTimer() },
            onPresetSelected = { viewModel.selectRestTimerPreset(it) },
            onAdjustTime = { viewModel.adjustRestTimer(it) },
            modifier = Modifier.fillMaxWidth(), // No bottom padding - sits right at bottom
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutHeader(
    workoutState: WorkoutState,
    workoutTimerSeconds: Int,
    totalSets: Int,
    completedSets: Int,
    canEdit: Boolean,
    currentWorkoutId: String?,
    currentNotes: String,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onShowWorkoutMenuDialog: () -> Unit,
    onShowCompletedWorkoutMenu: () -> Unit,
    onShowNotesModal: (String) -> Unit,
) {
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

                        // Subtitle - programme info, template edit, from template, or "Freestyle Workout"
                        if (workoutState.mode == WorkoutMode.TEMPLATE_EDIT) {
                            Text(
                                text = "Editing Template",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else if (workoutState.isProgrammeWorkout) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = "Programme workout",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text =
                                        buildString {
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
                                text = "${workoutState.workoutName ?: "Freestyle Workout"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (workoutState.mode != WorkoutMode.TEMPLATE_EDIT && completedSets > 0) {
                        WorkoutTimer(
                            seconds = workoutTimerSeconds,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (workoutState.mode != WorkoutMode.TEMPLATE_EDIT) {
                    IconButton(onClick = {
                        currentWorkoutId?.let { workoutId ->
                            onShowNotesModal(workoutId)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Workout Notes",
                            tint = if (currentNotes.isNotBlank()) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                }

                // Show action buttons for completed workouts, menu for active
                if (!canEdit && workoutState.status == WorkoutStatus.COMPLETED) {
                    Row {
                        IconButton(onClick = onShowCompletedWorkoutMenu) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else if (canEdit) {
                    IconButton(onClick = onShowWorkoutMenuDialog) {
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { (completedSets.toFloat() / totalSets).coerceIn(0f, 1f) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(4.dp),
                    color =
                        if (completedSets == totalSets) {
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
                    color =
                        if (completedSets == totalSets) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun CompletedWorkoutMenuDialog(
    onSaveAsTemplate: () -> Unit,
    onExportWorkout: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Workout Options") },
        text = {
            Column {
                TextButton(
                    onClick = onSaveAsTemplate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Save as template")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as Template")
                    }
                }
                TextButton(
                    onClick = onExportWorkout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export workout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Workout")
                    }
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
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Complete all sets")
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
                            Icon(Icons.Filled.Edit, contentDescription = "Edit workout name")
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
                                contentDescription = "Delete workout",
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
    completedSets: Int,
    expandedExerciseIds: Set<String>,
    workoutState: WorkoutState,
    onDeleteExercise: (String) -> Unit,
    onSelectExercise: () -> Unit,
    onCompleteWorkout: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onTemplateSaved: () -> Unit = {},
) {
    val lazyListState = rememberLazyListState()

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            viewModel.reorderExercises(from.index, to.index)
        }

    val formattedDeviations = DeviationFormatter.formatDeviations(workoutState.deviations)

    val imeInsets = WindowInsets.ime.asPaddingValues()
    val horizontalPadding = 16.dp
    val verticalPadding = 8.dp

    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = verticalPadding,
                bottom = maxOf(imeInsets.calculateBottomPadding(), verticalPadding),
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        itemsIndexed(
            items = exercises,
            key = { _, exercise -> exercise.id },
        ) { index, exercise ->
            ReorderableItem(
                state = reorderableLazyListState,
                key = exercise.id,
                enabled = canEdit,
            ) { isDragging ->
                Column {
                    ExerciseCard(
                        exercise = exercise,
                        sets = sets.filter { it.exerciseLogId == exercise.id },
                        isExpanded = expandedExerciseIds.contains(exercise.id),
                        isDragging = isDragging,
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
                        showDragHandle = canEdit,
                        dragHandleModifier =
                            if (canEdit) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        viewModel.collapseAllExercises()
                                    },
                                )
                            } else {
                                Modifier
                            },
                        modifier = Modifier.animateItem(),
                    )

                    // Only show deviation and prescribed sections when exercise is expanded
                    if (workoutState.isReadOnly && workoutState.isProgrammeWorkout && expandedExerciseIds.contains(exercise.id)) {
                        val deviationInfo = formattedDeviations.exerciseDeviations[exercise.id]
                        val isAddedExercise = formattedDeviations.addedExercises.contains(exercise.id)

                        if (deviationInfo != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DeviationSection(
                                deviations = deviationInfo.deviations,
                            )
                        } else if (isAddedExercise) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AddedExerciseBadge()
                        }

                        if (workoutState.prescribedExercises.isNotEmpty()) {
                            val prescribedExercise = workoutState.prescribedExercises.getOrNull(index)
                            if (prescribedExercise != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                PrescribedVsActualSection(
                                    prescribedExercise = prescribedExercise,
                                    actualSets = sets.filter { it.exerciseLogId == exercise.id },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (workoutState.isReadOnly && workoutState.isProgrammeWorkout && formattedDeviations.skippedExercises.isNotEmpty()) {
            item {
                SkippedExercisesSection(
                    skippedExercises = formattedDeviations.skippedExercises,
                )
            }
        }

        if (canEdit) {
            item {
                val workoutState by viewModel.workoutState.collectAsState()
                WorkoutActionButtons(
                    canCompleteWorkout = canCompleteWorkout && exercises.isNotEmpty() && completedSets > 0 && workoutState.mode != WorkoutMode.TEMPLATE_EDIT,
                    isTemplateEdit = workoutState.mode == WorkoutMode.TEMPLATE_EDIT,
                    onAddExercise = onSelectExercise,
                    onCompleteWorkout = onCompleteWorkout,
                    onSaveTemplate = {
                        viewModel.saveTemplateChanges()
                        onTemplateSaved()
                    },
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
    isTemplateEdit: Boolean = false,
    onSaveTemplate: () -> Unit = {},
) {
    var isSaving by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
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
                contentDescription = "Add exercise",
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Exercise")
        }

        // Complete Workout button or Save Template button
        if (isTemplateEdit) {
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        showSaved = true
                        onSaveTemplate()
                        // Reset after delay
                        coroutineScope.launch {
                            delay(1000)
                            isSaving = false
                            showSaved = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (showSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    ),
                enabled = !isSaving,
            ) {
                Icon(
                    if (showSaved) Icons.Filled.CheckCircle else Icons.Filled.Check,
                    contentDescription = "Save template",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showSaved) "Saved ✓" else "Save Changes")
            }
        } else if (canCompleteWorkout) {
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
                    contentDescription = "Complete workout",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete")
            }
        }
    }
}

@Composable
private fun PrescribedVsActualSection(
    prescribedExercise: com.github.radupana.featherweight.data.programme.ExerciseStructure,
    actualSets: List<SetLog>,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val weightUnitManager =
        remember {
            com.github.radupana.featherweight.di.ServiceLocator
                .provideWeightUnitManager(context)
        }
    var isExpanded by remember { mutableStateOf(false) }

    // Don't show if there's no prescribed data (added exercises)
    val hasPrescribedData = prescribedExercise.weights != null && prescribedExercise.weights!!.any { it > 0f }
    if (!hasPrescribedData) return

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Prescribed vs Actual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Set",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.5f),
                    )
                    Text(
                        text = "Prescribed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val maxSets = maxOf(prescribedExercise.sets, actualSets.size)
                for (setIndex in 0 until maxSets) {
                    val prescribedWeight = prescribedExercise.weights?.getOrNull(setIndex)
                    val prescribedReps = formatPrescribedReps(prescribedExercise.reps, setIndex)
                    val prescribedRpe = prescribedExercise.rpeValues?.getOrNull(setIndex)
                    val actualSet = actualSets.getOrNull(setIndex)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${setIndex + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.5f),
                        )
                        Text(
                            text = formatPrescribedSet(prescribedReps, prescribedWeight, prescribedRpe, weightUnitManager),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text =
                                if (actualSet != null) {
                                    formatActualSet(actualSet, weightUnitManager)
                                } else {
                                    "—"
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (setIndex < maxSets - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun formatPrescribedReps(
    reps: com.github.radupana.featherweight.data.programme.RepsStructure,
    setIndex: Int,
): String =
    when (reps) {
        is com.github.radupana.featherweight.data.programme.RepsStructure.Single -> "${reps.value}"
        is com.github.radupana.featherweight.data.programme.RepsStructure.Range -> "${reps.min}-${reps.max}"
        is com.github.radupana.featherweight.data.programme.RepsStructure.RangeString -> reps.value
        is com.github.radupana.featherweight.data.programme.RepsStructure.PerSet ->
            reps.values.getOrNull(setIndex) ?: reps.values.lastOrNull() ?: "—"
    }

private fun formatPrescribedSet(
    reps: String,
    weight: Float?,
    rpe: Float?,
    weightUnitManager: com.github.radupana.featherweight.manager.WeightUnitManager,
): String {
    val weightStr =
        if (weight != null && weight > 0f) {
            " × ${weightUnitManager.formatWeightWithUnit(weight)}"
        } else {
            ""
        }

    val rpeStr =
        if (rpe != null && rpe > 0f) {
            " @ RPE ${String.format(java.util.Locale.US, "%.1f", rpe)}"
        } else {
            ""
        }

    return "$reps$weightStr$rpeStr"
}

private fun formatActualSet(
    set: SetLog,
    weightUnitManager: com.github.radupana.featherweight.manager.WeightUnitManager,
): String {
    val weightStr = weightUnitManager.formatWeightWithUnit(set.actualWeight)
    val rpeStr =
        if (set.actualRpe != null && set.actualRpe!! > 0f) {
            " @ RPE ${String.format(java.util.Locale.US, "%.1f", set.actualRpe)}"
        } else {
            ""
        }

    return "${set.actualReps} × $weightStr$rpeStr"
}

@Composable
private fun DeviationSection(deviations: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Deviations from Programme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                deviations.forEach { deviation ->
                    Text(
                        text = "• $deviation",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AddedExerciseBadge() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Not in programme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SkippedExercisesSection(skippedExercises: List<com.github.radupana.featherweight.util.SkippedExerciseInfo>) {
    var isExpanded by remember { mutableStateOf(false) }
    val skippedCount = skippedExercises.size

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Skipped Exercises ($skippedCount)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                skippedExercises.forEach { skippedExercise ->
                    Text(
                        text = skippedExercise.exerciseName ?: "Unknown exercise",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
