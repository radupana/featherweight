package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdd1RMDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd1RMDialog = true },
                text = { Text("Add 1RM") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Big 4 Section
            item {
                Text(
                    "Big 4 Lifts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(uiState.big4Exercises) { exercise ->
                val currentMax = uiState.currentMaxes.find { it.exerciseId == exercise.id }
                Big4ExerciseCard(
                    exercise = exercise,
                    currentMax = currentMax,
                    onEdit = { exerciseToEdit = exercise },
                )
            }

            // Other 1RMs Section
            if (uiState.currentMaxes.any { max ->
                    uiState.big4Exercises.none { it.id == max.exerciseId }
                }
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Other 1RMs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    uiState.currentMaxes.filter { max ->
                        uiState.big4Exercises.none { it.id == max.exerciseId }
                    },
                ) { max ->
                    OtherExerciseMaxCard(
                        max = max,
                        onEdit = { editingMax = max },
                        onDelete = { viewModel.deleteMax(max) },
                    )
                }
            }
        }
    }

    // Add/Edit 1RM Dialog
    if (showAdd1RMDialog || exerciseToEdit != null || editingMax != null) {
        Add1RMDialog(
            exerciseId = exerciseToEdit?.id ?: editingMax?.exerciseId,
            exerciseName = exerciseToEdit?.name ?: editingMax?.exerciseName,
            currentWeight =
                editingMax?.maxWeight ?: uiState.currentMaxes.find {
                    it.exerciseId == (exerciseToEdit?.id ?: 0)
                }?.maxWeight,
            onDismiss = {
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
            },
            onConfirm = { exerciseId, weight ->
                if (exerciseId != null) {
                    viewModel.update1RM(exerciseId, weight)
                }
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
            },
            onSelectExercise = {
                // TODO: Navigate to exercise selector
            },
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or error dialog
            viewModel.clearError()
        }
    }
}

@Composable
private fun Big4ExerciseCard(
    exercise: Exercise,
    currentMax: ExerciseMaxWithName?,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (currentMax != null) {
                    Text(
                        "${currentMax.maxWeight.toInt()} kg",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Set ${currentMax.recordedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OtherExerciseMaxCard(
    max: ExerciseMaxWithName,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    max.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${max.maxWeight.toInt()} kg",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Set ${max.recordedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete 1RM?") },
            text = { Text("Are you sure you want to delete the 1RM for ${max.exerciseName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun Add1RMDialog(
    exerciseId: Long? = null,
    exerciseName: String? = null,
    currentWeight: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float) -> Unit,
    onSelectExercise: () -> Unit,
) {
    var weightText by remember {
        mutableStateOf(TextFieldValue(currentWeight?.toInt()?.toString() ?: ""))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (exerciseId != null) "Update 1RM" else "Add 1RM",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (exerciseName != null) {
                    Text(
                        exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    OutlinedButton(
                        onClick = onSelectExercise,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Select Exercise")
                    }
                }

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { newValue ->
                        val text = newValue.text
                        if (text.isEmpty() || (text.all { it.isDigit() } && text.length <= 4)) {
                            weightText = newValue
                        }
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.text.toFloatOrNull()
                    if (exerciseId != null && weight != null && weight > 0) {
                        onConfirm(exerciseId, weight)
                    }
                },
                enabled = exerciseId != null && weightText.text.isNotEmpty(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
