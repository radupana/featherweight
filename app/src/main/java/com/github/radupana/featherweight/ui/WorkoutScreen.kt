package com.github.radupana.featherweight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(),
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()
    val expandedExerciseId = remember { mutableStateOf<Long?>(null) }
    val sets by viewModel.selectedExerciseSets.collectAsState()
    var showEditSetDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<SetLog?>(null) }

    // Progress calculation
    val totalSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id } }
    val completedSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id && it.isCompleted } }
    val progress = if (totalSets > 0) completedSets.toFloat() / totalSets.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Freestyle Workout",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            var showAddExerciseDialog by remember { mutableStateOf(false) }

            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }

            if (showAddExerciseDialog) {
                AddExerciseDialog(
                    onDismiss = { showAddExerciseDialog = false },
                    onAdd = { name ->
                        viewModel.addExerciseToCurrentWorkout(name)
                        showAddExerciseDialog = false
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Enhanced progress section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Workout Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$completedSets / $totalSets sets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "${(progress * 100).roundToInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (exercises.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No exercises yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the + button to add your first exercise",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            sets = sets.filter { it.exerciseLogId == exercise.id },
                            expanded = expandedExerciseId.value == exercise.id,
                            onExpand = {
                                if (expandedExerciseId.value == exercise.id) {
                                    expandedExerciseId.value = null
                                } else {
                                    expandedExerciseId.value = exercise.id
                                    viewModel.loadSetsForExercise(exercise.id)
                                }
                            },
                            onAddSet = { viewModel.addSetToExercise(exercise.id) },
                            onEditSet = { set ->
                                editingSet = set
                                showEditSetDialog = true
                            },
                            onCopyLastSet = { exerciseId ->
                                val lastSet = sets.filter { it.exerciseLogId == exerciseId }
                                    .maxByOrNull { it.setOrder }
                                if (lastSet != null) {
                                    viewModel.addSetToExercise(exerciseId, lastSet.weight, lastSet.reps, lastSet.rpe)
                                } else {
                                    viewModel.addSetToExercise(exerciseId)
                                }
                            },
                            onDeleteSet = { setId ->
                                viewModel.deleteSet(setId)
                            },
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }

    // Edit Set Dialog
    if (showEditSetDialog && editingSet != null) {
        EditSetDialog(
            set = editingSet!!,
            onDismiss = { showEditSetDialog = false },
            onSave = { reps, weight, rpe ->
                viewModel.updateSet(editingSet!!.id, reps, weight, rpe)
                showEditSetDialog = false
                editingSet = null
            }
        )
    }
}

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
    viewModel: WorkoutViewModel,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (sets.isNotEmpty()) {
                    val completedSets = sets.count { it.isCompleted }
                    Surface(
                        color = if (completedSets == sets.size)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$completedSets/${sets.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completedSets == sets.size)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (sets.isEmpty()) {
                    Text(
                        "No sets yet. Add your first set below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Sets header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Set",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.12f)
                        )
                        Text(
                            "Reps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.18f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Weight",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.22f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "RPE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.15f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.weight(0.15f)) // For checkbox
                        Spacer(modifier = Modifier.weight(0.18f)) // For actions
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    sets.forEach { set ->
                        SetRow(
                            set = set,
                            onToggleCompleted = { completed ->
                                viewModel.markSetCompleted(set.id, completed)
                            },
                            onEdit = { onEditSet(set) },
                            onDelete = { onDeleteSet(set.id) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddSet,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Set",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set")
                    }

                    if (sets.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onCopyLastSet(exercise.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy Last",
                                modifier = Modifier.size(16.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SetRow(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Proper completed set styling - green tint, not red!
    val bgColor = if (set.isCompleted) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (set.isCompleted) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${set.setOrder + 1}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.12f)
            )

            Text(
                if (set.reps > 0) "${set.reps}" else "—",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.18f),
                textAlign = TextAlign.Center
            )

            Text(
                if (set.weight > 0) "${set.weight}kg" else "—",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.22f),
                textAlign = TextAlign.Center
            )

            Text(
                set.rpe?.let { "$it" } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.15f),
                textAlign = TextAlign.Center
            )

            // Completion checkbox with green tint when completed
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = onToggleCompleted,
                modifier = Modifier.weight(0.15f),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary
                )
            )

            // Action buttons
            Row(
                modifier = Modifier.weight(0.18f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Set",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Set",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditSetDialog(
    set: SetLog,
    onDismiss: () -> Unit,
    onSave: (reps: Int, weight: Float, rpe: Float?) -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var weight by remember { mutableStateOf(set.weight.toString()) }
    var rpe by remember { mutableStateOf(set.rpe?.toString() ?: "") }

    val focusManager = LocalFocusManager.current
    val repsFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Set ${set.setOrder + 1}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { weightFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(repsFocusRequester),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { rpeFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(weightFocusRequester),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rpe,
                    onValueChange = { rpe = it },
                    label = { Text("RPE (1-10, optional)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rpeFocusRequester),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val repsInt = reps.toIntOrNull() ?: 0
                    val weightFloat = weight.toFloatOrNull() ?: 0f
                    val rpeFloat = rpe.toFloatOrNull()
                    onSave(repsInt, weightFloat, rpeFloat)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    LaunchedEffect(Unit) {
        repsFocusRequester.requestFocus()
    }
}

@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var exerciseName by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Exercise name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (exerciseName.text.isNotBlank()) {
                            onAdd(exerciseName.text)
                        }
                    }
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(exerciseName.text) },
                enabled = exerciseName.text.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}