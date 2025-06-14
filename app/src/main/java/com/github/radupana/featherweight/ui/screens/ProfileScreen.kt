package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.viewmodel.ProfileUiState
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import java.time.format.DateTimeFormatter

enum class ProfileSection {
    MAXES,
    // Future sections can be added here
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSection by remember { mutableStateOf(ProfileSection.MAXES) }
    var showAdd1RMDialog by remember { mutableStateOf(false) }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var selectedExerciseForDialog by remember { mutableStateOf<Exercise?>(null) }

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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Left Navigation Panel
            NavigationRail(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                NavigationRailItem(
                    selected = selectedSection == ProfileSection.MAXES,
                    onClick = { selectedSection = ProfileSection.MAXES },
                    icon = {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = "Maxes",
                        )
                    },
                    label = {
                        Text(
                            "1RM Maxes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight =
                                if (selectedSection == ProfileSection.MAXES) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    },
                    colors =
                        NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    alwaysShowLabel = true,
                )

                // Future menu items can be added here
            }

            // Content Area
            AnimatedVisibility(
                visible = selectedSection == ProfileSection.MAXES,
                enter =
                    slideInHorizontally(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                    ),
                exit = slideOutHorizontally(),
            ) {
                MaxesContent(
                    uiState = uiState,
                    onEditExercise = { exerciseToEdit = it },
                    onEditMax = { editingMax = it },
                    onDeleteMax = { viewModel.deleteMax(it) },
                    onAddNew = { showAdd1RMDialog = true },
                )
            }
        }
    }

    // Add/Edit 1RM Dialog
    if (showAdd1RMDialog || exerciseToEdit != null || editingMax != null) {
        Add1RMDialog(
            exerciseId = exerciseToEdit?.id ?: editingMax?.exerciseId ?: selectedExerciseForDialog?.id,
            exerciseName = exerciseToEdit?.name ?: editingMax?.exerciseName ?: selectedExerciseForDialog?.name,
            currentWeight =
                editingMax?.maxWeight ?: uiState.currentMaxes.find {
                    it.exerciseId == (exerciseToEdit?.id ?: 0)
                }?.maxWeight,
            onDismiss = {
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
                selectedExerciseForDialog = null
            },
            onConfirm = { exerciseId, weight ->
                if (exerciseId != null) {
                    viewModel.update1RM(exerciseId, weight)
                }
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
                selectedExerciseForDialog = null
            },
            onSelectExercise = {
                showExerciseSelector = true
            },
        )
    }

    // Exercise Selector
    if (showExerciseSelector) {
        ExerciseSelectorDialog(
            onExerciseSelected = { exercise ->
                selectedExerciseForDialog = exercise
                showExerciseSelector = false
            },
            onDismiss = {
                showExerciseSelector = false
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
private fun MaxesContent(
    uiState: ProfileUiState,
    onEditExercise: (Exercise) -> Unit,
    onEditMax: (ExerciseMaxWithName) -> Unit,
    onDeleteMax: (ExerciseMaxWithName) -> Unit,
    onAddNew: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "1RM Tracking",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Track your one-rep maximums",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Button(
                        onClick = onAddNew,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add 1RM")
                    }
                }
            }

            // Big 4 Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Big 4 Lifts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(uiState.big4Exercises) { exercise ->
                val currentMax = uiState.currentMaxes.find { it.exerciseId == exercise.id }
                Big4ExerciseCard(
                    exercise = exercise,
                    currentMax = currentMax,
                    onEdit = { onEditExercise(exercise) },
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
                        style = MaterialTheme.typography.titleLarge,
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
                        onEdit = { onEditMax(max) },
                        onDelete = { onDeleteMax(max) },
                    )
                }
            }
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onEdit,
                ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            if (currentMax != null) {
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                            Color.Transparent,
                                        ),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent),
                                )
                            },
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (currentMax != null) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${currentMax.maxWeight.toInt()}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "kg",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(
                            "Set ${currentMax.recordedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Not set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onEdit,
                ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
            ),
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
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${max.maxWeight.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "kg",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
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
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            ),
                    ) {
                        Text(
                            exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Button(
                        onClick = onSelectExercise,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                    shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
