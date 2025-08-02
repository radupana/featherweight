package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import com.github.radupana.featherweight.viewmodel.ExerciseMaxWithName
import com.github.radupana.featherweight.ui.dialogs.Add1RMDialog
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.ui.components.Big4ExerciseCard
import com.github.radupana.featherweight.ui.components.OtherExerciseCard
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.components.CollapsibleSection
import com.github.radupana.featherweight.ui.components.SubSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var clearingExerciseId by remember { mutableStateOf<Long?>(null) }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            CollapsibleSection(
                title = "One Rep Maxes",
                isExpanded = uiState.isOneRMSectionExpanded,
                onToggle = { viewModel.toggleOneRMSection() }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Big Four Sub-section
                    SubSection(
                        title = "Big Four",
                        isExpanded = uiState.isBig4SubSectionExpanded,
                        onToggle = { viewModel.toggleBig4SubSection() }
                    ) {
                        uiState.big4Exercises.forEach { exercise ->
                            Big4ExerciseCard(
                                exerciseName = getDisplayName(exercise.exerciseName),
                                oneRMValue = exercise.oneRMValue,
                                oneRMType = exercise.oneRMType,
                                oneRMContext = exercise.oneRMContext,
                                oneRMDate = exercise.oneRMDate,
                                sessionCount = exercise.sessionCount,
                                onAdd = {
                                    editingMax = ExerciseMaxWithName(
                                        id = 0,
                                        exerciseId = exercise.exerciseId,
                                        exerciseName = exercise.exerciseName,
                                        oneRMEstimate = 0f,
                                        oneRMDate = java.time.LocalDateTime.now(),
                                        oneRMContext = "Manually set",
                                        oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                                        notes = null,
                                        sessionCount = 0
                                    )
                                },
                                onEdit = {
                                    editingMax = ExerciseMaxWithName(
                                        id = 0,
                                        exerciseId = exercise.exerciseId,
                                        exerciseName = exercise.exerciseName,
                                        oneRMEstimate = exercise.oneRMValue ?: 0f,
                                        oneRMDate = java.time.LocalDateTime.now(),
                                        oneRMContext = exercise.oneRMContext ?: "Manually set",
                                        oneRMType = exercise.oneRMType ?: com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                                        notes = null,
                                        sessionCount = exercise.sessionCount
                                    )
                                },
                                onClear = {
                                    clearingExerciseId = exercise.exerciseId
                                    showClearConfirmDialog = true
                                }
                            )
                        }
                    }
                    
                    // Other Exercises Sub-section
                    if (uiState.otherExercises.isNotEmpty()) {
                        SubSection(
                            title = "Other",
                            isExpanded = uiState.isOtherSubSectionExpanded,
                            onToggle = { viewModel.toggleOtherSubSection() },
                            showDivider = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { showExerciseSelector = true }
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add exercise",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.otherExercises.forEach { max ->
                                    OtherExerciseCard(
                                        exerciseName = max.exerciseName,
                                        oneRMValue = max.oneRMEstimate,
                                        oneRMType = max.oneRMType,
                                        oneRMContext = max.oneRMContext,
                                        oneRMDate = max.oneRMDate,
                                        sessionCount = max.sessionCount,
                                        onEdit = { editingMax = max },
                                        onDelete = { viewModel.deleteMax(max.exerciseId) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Add button when no other exercises
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Track More Exercises",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Text(
                                    text = "Add 1RM records for other exercises you want to track",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                FilledTonalButton(
                                    onClick = { showExerciseSelector = true }
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Exercise")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exercise Selector Dialog
    if (showExerciseSelector) {
        ExerciseSelectorDialog(
            onExerciseSelected = { exercise ->
                // Check if we already have a max for this exercise
                val existingMax = uiState.currentMaxes.find { it.exerciseId == exercise.id }
                if (existingMax != null) {
                    editingMax = existingMax
                } else {
                    // Create a new temporary max for editing
                    editingMax = ExerciseMaxWithName(
                        id = 0,
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        oneRMEstimate = 0f,
                        oneRMDate = java.time.LocalDateTime.now(),
                        oneRMContext = "Manually set",
                        oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                        notes = null,
                        sessionCount = 0
                    )
                }
                showExerciseSelector = false
            },
            onDismiss = { showExerciseSelector = false },
        )
    }

    // Edit/Add 1RM Dialog
    editingMax?.let { max ->
        Add1RMDialog(
            exerciseName = max.exerciseName,
            currentMax = if (max.oneRMEstimate > 0) max.oneRMEstimate else null,
            onSave = { newMax ->
                viewModel.update1RM(max.exerciseId, max.exerciseName, newMax)
                editingMax = null
            },
            onDismiss = { editingMax = null },
        )
    }
    
    // Clear confirmation dialog
    if (showClearConfirmDialog && clearingExerciseId != null) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmDialog = false
                clearingExerciseId = null
            },
            title = { Text("Clear 1RM") },
            text = { Text("Are you sure you want to clear this 1RM record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearingExerciseId?.let { viewModel.deleteMax(it) }
                        showClearConfirmDialog = false
                        clearingExerciseId = null
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        clearingExerciseId = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Success message handling
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }
}

// Helper function to display user-friendly names
@Composable
private fun getDisplayName(exerciseName: String): String {
    return when (exerciseName) {
        "Barbell Back Squat" -> "Squat"
        "Barbell Deadlift" -> "Deadlift"
        "Barbell Bench Press" -> "Bench Press"
        "Barbell Overhead Press" -> "Overhead Press"
        else -> exerciseName
    }
}