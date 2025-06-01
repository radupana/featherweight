package com.github.radupana.featherweight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.TextFieldValue
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

    // For now, progress = completed sets / total sets (all visible sets)
    val totalSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id } }
    val completedSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id && it.isCompleted } }
    val progress = if (totalSets > 0) completedSets.toFloat() / totalSets.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freestyle Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            var showAddExerciseDialog by remember { mutableStateOf(false) }
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
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
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(vertical = 8.dp),
            )
            Text(
                "${(progress * 100).roundToInt()}% completed",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f),
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
                        viewModel = viewModel,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onAddSet: () -> Unit,
    viewModel: WorkoutViewModel,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onExpand() },
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(exercise.exerciseName, style = MaterialTheme.typography.titleLarge)
            if (expanded) {
                if (sets.isEmpty()) {
                    Text("No sets. Add a set.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    sets.forEach { set ->
                        SetRow(
                            set = set,
                            onToggleCompleted = { completed ->
                                viewModel.markSetCompleted(
                                    set.id,
                                    completed,
                                )
                            },
                            onDelete = { viewModel.deleteSet(set.id) },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                OutlinedButton(
                    onClick = onAddSet,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Set")
                    Text("Add Set", modifier = Modifier.padding(start = 4.dp))
                }
            } else {
                Text(
                    if (sets.isEmpty()) "No sets. Add a set." else "${sets.size} sets",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SetRow(
    set: SetLog,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState =
        rememberDismissState(
            confirmStateChange = { value ->
                if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                    onDelete()
                    false // Don't keep the dismissed animation (since we reload)
                } else {
                    true
                }
            },
        )
    SwipeToDismiss(
        state = dismissState,
        background = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Red)
                        .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("Delete", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        },
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        dismissContent = {
            // Original row
            val bgColor =
                if (set.isCompleted) {
                    lerp(MaterialTheme.colorScheme.surface, Color(0xFFB9F6CA), 0.4f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = set.isCompleted,
                    onCheckedChange = { checked -> onToggleCompleted(checked) },
                )
                Text(
                    "Set ${set.setOrder + 1}: ${set.reps} x ${set.weight}kg" +
                        (set.rpe?.let { ", RPE $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                )
                if (set.isCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color(0xFF00C853))
                }
            }
        },
    )
}

@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var exerciseName by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Exercise name") },
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(exerciseName.text) },
                enabled = exerciseName.text.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
