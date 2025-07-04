package com.github.radupana.featherweight.ui.screens

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.components.CompactExerciseCard
import com.github.radupana.featherweight.ui.components.UnifiedTimerBar
import com.github.radupana.featherweight.ui.components.PRCelebrationDialog
import com.github.radupana.featherweight.ui.dialogs.SetEditingModal
import com.github.radupana.featherweight.ui.dialogs.SmartEditSetDialog
import com.github.radupana.featherweight.ui.dialogs.OneRMUpdateDialog
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.RestTimerViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutState
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onSelectExercise: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(),
    restTimerViewModel: RestTimerViewModel,
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()

    // Connect rest timer to workout lifecycle
    LaunchedEffect(restTimerViewModel) {
        viewModel.setRestTimerViewModel(restTimerViewModel)
    }

    // Debug log to track recompositions
    LaunchedEffect(exercises) {
        Log.d(
            "DragReorder",
            "WorkoutScreen recomposed with exercises: ${exercises.mapIndexed {
                idx,
                ex,
                ->
                "$idx:${ex.exerciseName}"
            }.joinToString()}",
        )
    }
    val sets by viewModel.selectedExerciseSets.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()
    val pendingOneRMUpdates by viewModel.pendingOneRMUpdates.collectAsState()

    // Rest timer state
    val timerState by restTimerViewModel.timerState.collectAsState()

    // Workout timer state
    val elapsedWorkoutTime by viewModel.elapsedWorkoutTime.collectAsState()

    // Dialog state
    var showEditSetDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showEditWorkoutNameDialog by remember { mutableStateOf(false) }
    var showEditModeDialog by remember { mutableStateOf(false) }
    var showSaveEditDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var showOneRMUpdateDialog by remember { mutableStateOf(false) }
    var shouldNavigateAfterCompletion by remember { mutableStateOf(false) }
    var showPRCelebration by remember { mutableStateOf(false) }

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
    val isEditMode = workoutState.isInEditMode
    
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
    LaunchedEffect(shouldNavigateAfterCompletion, pendingOneRMUpdates, showOneRMUpdateDialog) {
        if (shouldNavigateAfterCompletion && pendingOneRMUpdates.isEmpty() && !showOneRMUpdateDialog) {
            onBack()
        }
    }

    // Handle back press during edit mode
    val handleBack = {
        if (isEditMode) {
            showSaveEditDialog = true
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
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

                            // Status icons
                            if (workoutState.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED && !isEditMode) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Read-only",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isEditMode) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "EDITING",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        // Edit mode actions
                        IconButton(onClick = { showSaveEditDialog = true }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save Changes")
                        }
                        IconButton(onClick = {
                            viewModel.discardEditModeChanges()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Discard Changes")
                        }
                    } else {
                        // Normal actions
                        IconButton(onClick = { showWorkoutMenuDialog = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Workout Options")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            when {
                                isEditMode -> MaterialTheme.colorScheme.secondaryContainer
                                !canEdit -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            },
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .systemBarsPadding(NavigationContext.BOTTOM_NAVIGATION),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Normal workout view
                // Status banners
                if (workoutState.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED && !isEditMode) {
                    ReadOnlyBanner(
                        onEnterEditMode = { showEditModeDialog = true },
                    )
                } else if (isEditMode) {
                    EditModeBanner()
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

                // Unified Progress Card
                UnifiedProgressCard(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    workoutState = workoutState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(16.dp),
                )

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
                    EmptyWorkoutState(
                        canEdit = canEdit,
                        onAddExercise = onSelectExercise,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ExercisesList(
                        exercises = exercises,
                        sets = sets,
                        canEdit = canEdit,
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
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Action buttons at bottom
                if (canEdit && exercises.isNotEmpty()) {
                    WorkoutActionButtons(
                        canCompleteWorkout = workoutState.isActive && !isEditMode,
                        onAddExercise = onSelectExercise,
                        onCompleteWorkout = { showCompleteWorkoutDialog = true },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    // Edit Mode Confirmation Dialog
    if (showEditModeDialog) {
        AlertDialog(
            onDismissRequest = { showEditModeDialog = false },
            title = { Text("Edit Completed Workout?") },
            text = {
                Text(
                    "This workout has been completed. You can temporarily edit it to make corrections, " +
                        "but you'll need to save or discard your changes when done.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enterEditMode()
                        showEditModeDialog = false
                    },
                ) {
                    Text("Enter Edit Mode")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditModeDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Save Edit Mode Dialog
    if (showSaveEditDialog) {
        AlertDialog(
            onDismissRequest = { showSaveEditDialog = false },
            title = { Text("Save Changes?") },
            text = {
                Text(
                    "Do you want to save your changes to this workout or discard them?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveEditModeChanges()
                        showSaveEditDialog = false
                        onBack()
                    },
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.discardEditModeChanges()
                        showSaveEditDialog = false
                        onBack()
                    },
                ) {
                    Text("Discard Changes")
                }
            },
        )
    }

    // Workout Menu Dialog
    if (showWorkoutMenuDialog && !isEditMode) {
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
                viewModel.completeWorkout {
                    // Completion callback: let the workout completion finish before navigation
                    showCompleteWorkoutDialog = false
                    shouldNavigateAfterCompletion = true
                }
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
    if (showSetEditingModal && setEditingExercise != null && canEdit) {
        SetEditingModal(
            exercise = setEditingExercise!!,
            sets = sets.filter { it.exerciseLogId == setEditingExercise!!.id },
            onDismiss = {
                showSetEditingModal = false
                setEditingExercise = null
            },
            onUpdateSet = { setId, reps, weight, rpe ->
                viewModel.updateSet(setId, reps, weight, rpe)
            },
            onAddSet = {
                viewModel.addSetToExercise(setEditingExercise!!.id)
            },
            onCopyLastSet = {
                val lastSet =
                    sets
                        .filter { it.exerciseLogId == setEditingExercise!!.id }
                        .maxByOrNull { it.setOrder }
                if (lastSet != null) {
                    viewModel.addSetToExercise(setEditingExercise!!.id, lastSet.reps, lastSet.weight, lastSet.weight, lastSet.reps, lastSet.rpe)
                } else {
                    viewModel.addSetToExercise(setEditingExercise!!.id)
                }
            },
            onDeleteSet = { setId ->
                viewModel.deleteSet(setId)
            },
            onToggleCompleted = { setId, completed ->
                viewModel.markSetCompleted(setId, completed)
                // Auto-start smart rest timer when completing a set
                if (completed && setEditingExercise != null) {
                    val completedSet = sets.find { it.id == setId }
                    restTimerViewModel.startSmartTimer(
                        exerciseName = setEditingExercise!!.exerciseName,
                        exercise = null, // TODO: Add exercise entity lookup
                        reps = completedSet?.reps,
                        weight = completedSet?.weight,
                    )
                }
            },
            onCompleteAllSets = {
                viewModel.completeAllSetsInExercise(setEditingExercise!!.id)
                // Auto-start smart rest timer when completing all sets
                if (setEditingExercise != null) {
                    // Use the most recent set's data for timer calculation
                    val mostRecentSet =
                        sets
                            .filter { it.exerciseLogId == setEditingExercise!!.id }
                            .maxByOrNull { it.setOrder }
                    restTimerViewModel.startSmartTimer(
                        exerciseName = setEditingExercise!!.exerciseName,
                        exercise = null, // TODO: Add exercise entity lookup
                        reps = mostRecentSet?.reps,
                        weight = mostRecentSet?.weight,
                    )
                }
            },
            viewModel = viewModel,
            restTimerViewModel = restTimerViewModel,
            isProgrammeWorkout = workoutState.isProgrammeWorkout,
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
            }
        )
    }
    
    // PR Celebration Dialog
    if (showPRCelebration && workoutState.pendingPRs.isNotEmpty()) {
        PRCelebrationDialog(
            personalRecords = workoutState.pendingPRs,
            onShare = { pr ->
                // TODO: Implement share functionality
                println("Sharing PR: ${pr.exerciseName} - ${pr.weight}kg x ${pr.reps}")
            },
            onDismiss = {
                showPRCelebration = false
                viewModel.clearPendingPRs()
            }
        )
    }
}

@Composable
private fun ReadOnlyBanner(onEnterEditMode: () -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "This workout has been completed and is read-only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            TextButton(onClick = onEnterEditMode) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
        }
    }
}

@Composable
private fun EditModeBanner() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Editing",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "You're temporarily editing this completed workout. Save or discard when done.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
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
                if (canEdit && canCompleteAllSets) {
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
                
                if (canEdit) {
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

@Composable
private fun EmptyWorkoutState(
    canEdit: Boolean,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (canEdit) "No exercises yet" else "No exercises in this workout",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (canEdit) {
                    "Start by adding your first exercise"
                } else {
                    "This completed workout contains no exercises"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (canEdit) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(0.6f),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExercisesList(
    exercises: List<ExerciseLog>,
    sets: List<SetLog>,
    canEdit: Boolean,
    onDeleteExercise: (Long) -> Unit,
    onOpenSetEditingModal: (Long) -> Unit,
    onSelectExercise: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    var draggedExerciseId by remember { mutableStateOf<Long?>(null) }
    var draggedOffset by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    // Debug mode - set to true to see detailed logging
    val debugMode = false

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
                        Log.e("DragReorder", "ERROR: Exercise ${exercise.exerciseName} not found in list!")
                        return@CompactExerciseCard
                    }

                    draggedExerciseId = exercise.id
                    initialDragIndex = startIndex
                    targetIndex = startIndex
                    draggedOffset = 0f

                    // Log current state
                    val exerciseList =
                        currentExercisesList
                            .mapIndexed { idx, ex ->
                                "$idx: ${ex.exerciseName} (order=${ex.exerciseOrder})"
                            }.joinToString(", ")
                    Log.d("DragReorder", "=== DRAG START ===")
                    Log.d("DragReorder", "Dragging: ${exercise.exerciseName} from index $startIndex")
                    Log.d("DragReorder", "Current order: $exerciseList")
                },
                onDragEnd = {
                    val finalTarget = targetIndex
                    val startIndex = initialDragIndex

                    Log.d("DragReorder", "=== DRAG END ===")
                    Log.d("DragReorder", "From: $startIndex, To: $finalTarget, Offset: $draggedOffset")

                    // Perform the reorder if needed
                    if (startIndex != null && finalTarget != null && startIndex != finalTarget) {
                        Log.d("DragReorder", "Executing reorder: ${exercise.exerciseName} from $startIndex to $finalTarget")
                        viewModel.reorderExercisesInstantly(startIndex, finalTarget)
                    } else {
                        Log.d("DragReorder", "No reorder needed")
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

                        // Only log when target changes
                        if (newTargetIndex != targetIndex) {
                            targetIndex = newTargetIndex
                            Log.d(
                                "DragReorder",
                                "Target changed: ${exercise.exerciseName} -> position $newTargetIndex (offset: ${draggedOffset.toInt()}px)",
                            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Programme section (if applicable)
            if (workoutState.isProgrammeWorkout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = workoutState.programmeName ?: "Programme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    
                    programmeProgress?.let { (completed, total) ->
                        if (total > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "$completed/$total workouts",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
                
                if (workoutState.weekNumber != null && workoutState.dayNumber != null) {
                    Text(
                        text = "Week ${workoutState.weekNumber} • Day ${workoutState.dayNumber} • ${workoutState.programmeWorkoutName ?: "Workout"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
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
                Text(
                    "$completedSets / $totalSets sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { workoutProgress },
                modifier = Modifier
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

