package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.components.ExerciseCard
import com.github.radupana.featherweight.ui.components.ProgressCard
import com.github.radupana.featherweight.ui.components.SetRow
import com.github.radupana.featherweight.ui.dialogs.SmartEditSetDialog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onSelectExercise: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()
    val sets by viewModel.selectedExerciseSets.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()

    // Dialog state
    var showEditSetDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showEditWorkoutNameDialog by remember { mutableStateOf(false) }
    var showEditModeDialog by remember { mutableStateOf(false) }
    var showSaveEditDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }

    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    var editingExerciseName by remember { mutableStateOf<String?>(null) }
    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }

    // Focused editing mode state
    var focusedEditingExerciseId by remember { mutableStateOf<Long?>(null) }
    var isInFocusedEditingMode by remember { mutableStateOf(false) }

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
        if (!workoutState.isActive && !workoutState.isCompleted) {
            viewModel.startNewWorkout()
        }
    }

    val canEdit = viewModel.canEditWorkout()
    val isEditMode = workoutState.isInEditMode

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            viewModel.getWorkoutDisplayName(),
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (workoutState.isCompleted && !isEditMode) {
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
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .imePadding(),
        ) {
            if (isInFocusedEditingMode && focusedEditingExerciseId != null) {
                // Focused editing mode - show only the exercise being edited
                FocusedEditingView(
                    exercise = exercises.find { it.id == focusedEditingExerciseId },
                    sets = sets.filter { it.exerciseLogId == focusedEditingExerciseId },
                    canEdit = canEdit,
                    onExitFocusedMode = {
                        isInFocusedEditingMode = false
                        focusedEditingExerciseId = null
                    },
                    onUpdateSet = { setId, reps, weight, rpe ->
                        if (canEdit) viewModel.updateSet(setId, reps, weight, rpe)
                    },
                    onToggleCompleted = { setId, completed ->
                        viewModel.markSetCompleted(setId, completed)
                    },
                    onDeleteSet = { setId ->
                        if (canEdit) viewModel.deleteSet(setId)
                    },
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Normal workout view
                // Status banners
                if (workoutState.isCompleted && !isEditMode) {
                    ReadOnlyBanner(
                        onEnterEditMode = { showEditModeDialog = true },
                    )
                } else if (isEditMode) {
                    EditModeBanner()
                }

                // Progress section
                ProgressCard(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    modifier = Modifier.padding(16.dp),
                )

                // Exercises list or empty state
                if (exercises.isEmpty()) {
                    EmptyWorkoutState(
                        canEdit = canEdit,
                        onAddExercise = onSelectExercise,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ExercisesList(
                        exercises = exercises,
                        sets = sets,
                        expandedExerciseId = expandedExerciseId,
                        canEdit = canEdit,
                        onExerciseExpand = { exerciseId ->
                            expandedExerciseId =
                                if (expandedExerciseId == exerciseId) {
                                    null
                                } else {
                                    exerciseId.also { viewModel.loadSetsForExercise(it) }
                                }
                        },
                        onEditSet = { set, exerciseName ->
                            if (canEdit) {
                                editingSet = set
                                editingExerciseName = exerciseName
                                showEditSetDialog = true
                            }
                        },
                        onSmartAdd = { exerciseName ->
                            if (canEdit) {
                                editingSet = null
                                editingExerciseName = exerciseName
                                showEditSetDialog = true
                            }
                        },
                        onDeleteExercise = { exerciseId ->
                            if (canEdit) {
                                viewModel.deleteExercise(exerciseId)
                            }
                        },
                        onEnterFocusedEditMode = { exerciseId ->
                            focusedEditingExerciseId = exerciseId
                            isInFocusedEditingMode = true
                            expandedExerciseId = exerciseId
                            viewModel.loadSetsForExercise(exerciseId)
                        },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Action buttons at bottom
                if (canEdit && exercises.isNotEmpty()) {
                    WorkoutActionButtons(
                        hasExercises = exercises.isNotEmpty(),
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
            hasContent = hasContent,
            onEditName = {
                showWorkoutMenuDialog = false
                showEditWorkoutNameDialog = true
            },
            onDeleteWorkout = {
                showWorkoutMenuDialog = false
                showDeleteWorkoutDialog = true
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
                viewModel.completeWorkout()
                showCompleteWorkoutDialog = false
                onBack()
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
                    exercise?.let { viewModel.addSetToExercise(it.id, weight, reps, rpe) }
                }
                showEditSetDialog = false
                editingSet = null
                editingExerciseName = null
            },
            viewModel = viewModel,
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
    hasContent: Boolean,
    onEditName: () -> Unit,
    onDeleteWorkout: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Workout Options") },
        text = {
            Column {
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

@Composable
private fun ExercisesList(
    exercises: List<com.github.radupana.featherweight.data.ExerciseLog>,
    sets: List<SetLog>,
    expandedExerciseId: Long?,
    canEdit: Boolean,
    onExerciseExpand: (Long) -> Unit,
    onEditSet: (SetLog, String) -> Unit,
    onSmartAdd: (String) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onEnterFocusedEditMode: (Long) -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                sets = sets.filter { it.exerciseLogId == exercise.id },
                expanded = expandedExerciseId == exercise.id,
                onExpand = { onExerciseExpand(exercise.id) },
                onAddSet = {
                    if (canEdit) viewModel.addSetToExercise(exercise.id)
                },
                onEditSet = { set ->
                    if (canEdit) onEditSet(set, exercise.exerciseName)
                },
                onCopyLastSet = { exerciseId ->
                    if (canEdit) {
                        val lastSet =
                            sets
                                .filter { it.exerciseLogId == exerciseId }
                                .maxByOrNull { it.setOrder }
                        if (lastSet != null) {
                            viewModel.addSetToExercise(exerciseId, lastSet.weight, lastSet.reps, lastSet.rpe)
                        } else {
                            viewModel.addSetToExercise(exerciseId)
                        }
                    }
                },
                onDeleteSet = { setId ->
                    if (canEdit) viewModel.deleteSet(setId)
                },
                onSmartAdd = { _, exerciseName ->
                    if (canEdit) onSmartAdd(exerciseName)
                },
                onDeleteExercise = { exerciseId ->
                    if (canEdit) onDeleteExercise(exerciseId)
                },
                onUpdateSet = { setId, reps, weight, rpe ->
                    if (canEdit) viewModel.updateSet(setId, reps, weight, rpe)
                },
                onCompleteAllSets = { exerciseId ->
                    viewModel.completeAllSetsInExercise(exerciseId)
                },
                onEnterFocusedEditMode = { exerciseId ->
                    onEnterFocusedEditMode(exerciseId)
                },
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun FocusedEditingView(
    exercise: com.github.radupana.featherweight.data.ExerciseLog?,
    sets: List<SetLog>,
    canEdit: Boolean,
    onExitFocusedMode: () -> Unit,
    onUpdateSet: (Long, Int, Float, Float?) -> Unit,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    if (exercise == null) {
        onExitFocusedMode()
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header with back button and exercise name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExitFocusedMode) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit Focused Mode",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                exercise.exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )

            // Spacer to balance the back button
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sets table header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Set",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.12f),
            )
            Text(
                "Reps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.18f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Weight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.22f),
                textAlign = TextAlign.Center,
            )
            Text(
                "RPE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.15f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(0.33f)) // For checkbox + actions
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sets list with extra spacing
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(sets) { set ->
                key(set.id) {
                    SetRow(
                        set = set,
                        onToggleCompleted = { completed ->
                            if (completed && !(set.reps > 0 && set.weight > 0)) {
                                // Handle validation if needed
                            } else {
                                onToggleCompleted(set.id, completed)
                            }
                        },
                        onEdit = { /* Handle edit if needed */ },
                        onDelete = { onDeleteSet(set.id) },
                        onUpdateSet = { reps, weight, rpe ->
                            onUpdateSet(set.id, reps, weight, rpe)
                        },
                        canMarkComplete = set.reps > 0 && set.weight > 0,
                    )
                }
            }
        }

        // Add set button at bottom with plenty of space
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = {
                    if (canEdit) viewModel.addSetToExercise(exercise.id)
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add Set",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Set")
            }

            if (sets.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        if (canEdit) {
                            val lastSet = sets.maxByOrNull { it.setOrder }
                            if (lastSet != null) {
                                viewModel.addSetToExercise(exercise.id, lastSet.weight, lastSet.reps, lastSet.rpe)
                            } else {
                                viewModel.addSetToExercise(exercise.id)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy Last",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Last")
                }
            }
        }
    }
}

@Composable
private fun WorkoutActionButtons(
    hasExercises: Boolean,
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
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Workout")
            }
        }
    }
}
