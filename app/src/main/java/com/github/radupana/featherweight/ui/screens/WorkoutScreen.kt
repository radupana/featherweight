package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

    // Dialog state
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showEditSetDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    var editingExerciseName by remember { mutableStateOf<String?>(null) }
    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }

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
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Progress section
            val totalSets = exercises.sumOf { ex -> sets.count { it.exerciseLogId == ex.id } }
            val completedSets = exercises.sumOf { ex ->
                sets.count { it.exerciseLogId == ex.id && it.isCompleted }
            }

            ProgressCard(
                completedSets = completedSets,
                totalSets = totalSets,
                modifier = Modifier.padding(16.dp)
            )

            // Exercises list or empty state
            if (exercises.isEmpty()) {
                EmptyWorkoutState(modifier = Modifier.weight(1f))
            } else {
                ExercisesList(
                    exercises = exercises,
                    sets = sets,
                    expandedExerciseId = expandedExerciseId,
                    onExerciseExpand = { exerciseId ->
                        expandedExerciseId = if (expandedExerciseId == exerciseId) {
                            null
                        } else {
                            exerciseId.also { viewModel.loadSetsForExercise(it) }
                        }
                    },
                    onEditSet = { set, exerciseName ->
                        editingSet = set
                        editingExerciseName = exerciseName
                        showEditSetDialog = true
                    },
                    onSmartAdd = { exerciseName ->
                        editingSet = null
                        editingExerciseName = exerciseName
                        showEditSetDialog = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialogs
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { name ->
                viewModel.addExerciseToCurrentWorkout(name)
                showAddExerciseDialog = false
            },
        )
    }

    if (showEditSetDialog && editingExerciseName != null) {
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
            viewModel = viewModel
        )
    }
}

@Composable
private fun EmptyWorkoutState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
}

@Composable
private fun ExercisesList(
    exercises: List<com.github.radupana.featherweight.data.ExerciseLog>,
    sets: List<SetLog>,
    expandedExerciseId: Long?,
    onExerciseExpand: (Long) -> Unit,
    onEditSet: (SetLog, String) -> Unit,
    onSmartAdd: (String) -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                sets = sets.filter { it.exerciseLogId == exercise.id },
                expanded = expandedExerciseId == exercise.id,
                onExpand = { onExerciseExpand(exercise.id) },
                onAddSet = { viewModel.addSetToExercise(exercise.id) },
                onEditSet = { set -> onEditSet(set, exercise.exerciseName) },
                onCopyLastSet = { exerciseId ->
                    val lastSet = sets.filter { it.exerciseLogId == exerciseId }
                        .maxByOrNull { it.setOrder }
                    if (lastSet != null) {
                        viewModel.addSetToExercise(exerciseId, lastSet.weight, lastSet.reps, lastSet.rpe)
                    } else {
                        viewModel.addSetToExercise(exerciseId)
                    }
                },
                onDeleteSet = { setId -> viewModel.deleteSet(setId) },
                onSmartAdd = { _, exerciseName -> onSmartAdd(exerciseName) },
                onUpdateSet = { setId, reps, weight, rpe ->
                    // Add inline update callback
                    viewModel.updateSet(setId, reps, weight, rpe)
                },
                viewModel = viewModel
            )
        }
    }
}