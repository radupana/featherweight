package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.github.radupana.featherweight.ui.dialogs.AddExerciseDialog
import com.github.radupana.featherweight.ui.dialogs.SmartEditSetDialog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()
    val sets by viewModel.selectedExerciseSets.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()

    // Dialog state
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showEditSetDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }
    var showWorkoutMenuDialog by remember { mutableStateOf(false) }
    var showEditWorkoutNameDialog by remember { mutableStateOf(false) }
    var showEditModeDialog by remember { mutableStateOf(false) }
    var showSaveEditDialog by remember { mutableStateOf(false) }

    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    var editingExerciseName by remember { mutableStateOf<String?>(null) }
    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }

    // Calculate progress stats
    val totalSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id } }
    val completedSets =
        exercises.sumOf { ex ->
            sets.count { it.exerciseLogId == ex.id && it.isCompleted }
        }

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
        floatingActionButton = {
            if (canEdit) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    // Complete workout button (only for active workouts, not edit mode)
                    if (workoutState.isActive && !isEditMode) {
                        FloatingActionButton(
                            onClick = { showCompleteWorkoutDialog = true },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Complete Workout",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // Add exercise button
                    FloatingActionButton(
                        onClick = { showAddExerciseDialog = true },
                        containerColor =
                            if (isEditMode) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
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
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
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
            onEditName = {
                showWorkoutMenuDialog = false
                showEditWorkoutNameDialog = true
            },
            onClose = { showWorkoutMenuDialog = false },
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

    // Complete Workout Dialog
    if (showCompleteWorkoutDialog && canEdit) {
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

    // Add Exercise Dialog
    if (showAddExerciseDialog && canEdit) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { name ->
                viewModel.addExerciseToCurrentWorkout(name)
                showAddExerciseDialog = false
            },
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
    onEditName: () -> Unit,
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
                    "Tap the + button to add your first exercise"
                } else {
                    "This completed workout contains no exercises"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
                onUpdateSet = { setId, reps, weight, rpe ->
                    if (canEdit) viewModel.updateSet(setId, reps, weight, rpe)
                },
                onCompleteAllSets = { exerciseId ->
                    viewModel.completeAllSetsInExercise(exerciseId)
                },
                viewModel = viewModel,
            )
        }
    }
}
