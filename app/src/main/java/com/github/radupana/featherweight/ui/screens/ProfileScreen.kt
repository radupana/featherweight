package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.ui.dialogs.WorkoutSeederDialog
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
    var showExerciseSelector by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var selectedExerciseForDialog by remember { mutableStateOf<Exercise?>(null) }
    var currentTab by remember { mutableStateOf("1RM") } // Track current tab
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh data when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Static Left Navigation
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 1RM Menu Item
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false),
                                    onClick = { currentTab = "1RM" },
                                ).padding(vertical = 12.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .background(
                                        if (currentTab == "1RM") {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        RoundedCornerShape(12.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = "1 Rep Max",
                                tint =
                                    if (currentTab == "1RM") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "1RM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (currentTab == "1RM") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Developer Tools Menu Item
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false),
                                    onClick = { currentTab = "Developer" },
                                ).padding(vertical = 12.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .background(
                                        if (currentTab == "Developer") {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        RoundedCornerShape(12.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = "Developer Tools",
                                tint =
                                    if (currentTab == "Developer") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Dev",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (currentTab == "Developer") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            // Main Content Area
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                when (currentTab) {
                    "1RM" -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 12.dp,
                                    bottom = 80.dp, // Space for FAB
                                ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Header
                            item {
                                Column {
                                    Text(
                                        "1 Rep Max Tracking",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Track your one-rep maximums",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 16.dp),
                                    )
                                }
                            }

                            // Big 4 Section Title
                            item {
                                Text(
                                    "Big 4 Lifts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }

                            // Define Big 4 exercises with their expected names (matching database)
                            val big4Names =
                                listOf(
                                    "Barbell Back Squat",
                                    "Barbell Bench Press",
                                    "Barbell Deadlift",
                                    "Barbell Overhead Press",
                                )

                            // Big 4 Exercises - One per row
                            items(big4Names) { exerciseName ->
                                val exercise = uiState.big4Exercises.find { it.name == exerciseName }
                                val currentMax =
                                    if (exercise != null) {
                                        uiState.currentMaxes.find { it.exerciseId == exercise.id }
                                    } else {
                                        uiState.currentMaxes.find { it.exerciseName == exerciseName }
                                    }

                                if (exercise != null) {
                                    Big4ExerciseCard(
                                        exercise = exercise,
                                        currentMax = currentMax,
                                        onEdit = { exerciseToEdit = exercise },
                                        onClear = {
                                            viewModel.clearAllMaxesForExercise(exercise.id)
                                        },
                                    )
                                } else {
                                    // Show placeholder card for missing exercise
                                    Big4ExercisePlaceholder(
                                        exerciseName = exerciseName,
                                        onAdd = {
                                            // Try to find the exercise in the database
                                            viewModel.findAndSelectExercise(exerciseName)
                                            showAdd1RMDialog = true
                                        },
                                    )
                                }
                            }

                            // Other 1RMs Section
                            if (uiState.currentMaxes.any { max ->
                                    uiState.big4Exercises.none { it.id == max.exerciseId }
                                }
                            ) {
                                item {
                                    Text(
                                        "Other One Rep Maxes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 16.dp),
                                    )
                                }

                                items(
                                    uiState.currentMaxes.filter { max ->
                                        uiState.big4Exercises.none { it.id == max.exerciseId }
                                    },
                                ) { max ->
                                    ExerciseMaxCardWrapper(
                                        max = max,
                                        onEdit = { editingMax = max },
                                        onDelete = { viewModel.deleteMax(max) },
                                    )
                                }
                            }
                        }

                        // Floating Action Button - only show for 1RM tab
                        ExtendedFloatingActionButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAdd1RMDialog = true
                            },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add 1RM",
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add 1RM")
                        }
                    }

                    "Developer" -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 12.dp,
                                    bottom = 12.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Header
                            item {
                                Column {
                                    Text(
                                        "Developer Tools",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Debug and test features",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 16.dp),
                                    )
                                }
                            }

                            // Developer tools content
                            item {
                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showDeveloperDialog = true
                                            },
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                "Seed Workout Data",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                "Generate realistic workout history",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            // Clear data option
                            item {
                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showClearConfirmDialog = true
                                            },
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                "Clear All Workouts",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            Text(
                                                "Remove all workout history",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit 1RM Bottom Sheet
    if (showAdd1RMDialog || exerciseToEdit != null || editingMax != null) {
        // Capture editingMax in a local variable to avoid smart cast issues
        val currentEditingMax = editingMax

        // Determine which exercise is currently selected
        // selectedExerciseForDialog takes priority when set (user changed selection)
        val displayExerciseId = selectedExerciseForDialog?.id ?: exerciseToEdit?.id ?: currentEditingMax?.exerciseId
        val displayExerciseName = selectedExerciseForDialog?.name ?: exerciseToEdit?.name ?: currentEditingMax?.exerciseName

        // For weight, only use editingMax weight if we haven't changed the exercise
        val displayWeight =
            if (selectedExerciseForDialog == null && currentEditingMax != null) {
                currentEditingMax.maxWeight
            } else {
                uiState.currentMaxes.find { it.exerciseId == displayExerciseId }?.maxWeight
            }

        Add1RMBottomSheet(
            exerciseId = displayExerciseId,
            exerciseName = displayExerciseName,
            currentWeight = displayWeight,
            onDismiss = {
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
                selectedExerciseForDialog = null
            },
            onConfirm = { exerciseId, weight ->
                viewModel.update1RM(exerciseId, weight)
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
                // Update the selected exercise and keep the modal open
                selectedExerciseForDialog = exercise
                // Clear other states to ensure the new selection takes precedence
                exerciseToEdit = null
                editingMax = null
                showExerciseSelector = false
            },
            onDismiss = {
                showExerciseSelector = false
            },
            excludeExerciseIds = uiState.currentMaxes.map { it.exerciseId }.toSet(),
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or error dialog
            viewModel.clearError()
        }
    }

    // Developer Workout Seeder Dialog
    if (showDeveloperDialog) {
        WorkoutSeederDialog(
            onDismiss = { showDeveloperDialog = false },
            onConfirm = { config ->
                viewModel.seedWorkouts(config)
                showDeveloperDialog = false
            },
        )
    }

    // Clear All Workouts Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear All Workouts?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    "This will permanently delete all workout history, exercise logs, personal records, and progress data. This action cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllWorkouts()
                        showClearConfirmDialog = false
                    },
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearError()
        }
    }

    // Success message handling
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearSuccessMessage()
        }
    }
}

@Composable
private fun Compact1RMCardPlaceholder(
    exerciseName: String,
    onAdd: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdd()
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Text(
                "Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Compact1RMCard(
    exercise: Exercise,
    currentMax: ExerciseMaxWithName?,
    onEdit: () -> Unit,
    onClear: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    var showClearDialog by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                    onLongClick =
                        if (currentMax != null) {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearDialog = true
                            }
                        } else {
                            null
                        },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Set 1RM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear 1RM?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text("Clear your ${exercise.name} 1RM?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        showClearDialog = false
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun Big4ExerciseCard(
    exercise: Exercise,
    currentMax: ExerciseMaxWithName?,
    onEdit: () -> Unit,
    onClear: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    var showClearDialog by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                    onLongClick =
                        if (currentMax != null) {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearDialog = true
                            }
                        } else {
                            null
                        },
                ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
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
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.height(36.dp),
            ) {
                Text("Set 1RM")
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear 1RM?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text("Clear your ${exercise.name} 1RM?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        showClearDialog = false
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun Big4ExercisePlaceholder(
    exerciseName: String,
    onAdd: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onAdd() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.height(36.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
private fun ExerciseMaxCardWrapper(
    max: ExerciseMaxWithName,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    OtherExerciseMaxCard(
        max = max,
        onEdit = onEdit,
        onDelete = { showDeleteDialog = true },
    )

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete 1RM?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text("Delete ${max.exerciseName} - ${max.maxWeight.toInt()}kg?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun OtherExerciseMaxCard(
    max: ExerciseMaxWithName,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                ),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 1.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    max.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${max.maxWeight.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
                Text(
                    max.recordedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Add1RMBottomSheet(
    exerciseId: Long? = null,
    exerciseName: String? = null,
    currentWeight: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float) -> Unit,
    onSelectExercise: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var weightText by remember {
        mutableStateOf(TextFieldValue(currentWeight?.toInt()?.toString() ?: ""))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title
            Text(
                if (exerciseId != null) "Update 1RM" else "Add 1RM",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            // Exercise Selection
            if (exerciseName != null) {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectExercise()
                            },
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Change",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectExercise()
                    },
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

            // Weight Input
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val weight = weightText.text.toFloatOrNull()
                        if (exerciseId != null && weight != null && weight > 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm(exerciseId, weight)
                        }
                    },
                    enabled = exerciseId != null && weightText.text.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
