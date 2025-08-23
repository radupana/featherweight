package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.radupana.featherweight.ui.components.Big4ExerciseCard
import com.github.radupana.featherweight.ui.components.CollapsibleSection
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.components.OtherExerciseCard
import com.github.radupana.featherweight.ui.components.SubSection
import com.github.radupana.featherweight.ui.dialogs.Add1RMDialog
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.ui.dialogs.ExportShareDialog
import com.github.radupana.featherweight.ui.dialogs.ExportWorkoutsDialog
import com.github.radupana.featherweight.viewmodel.ExerciseMaxWithName
import com.github.radupana.featherweight.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    insightsViewModel: com.github.radupana.featherweight.viewmodel.InsightsViewModel? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var clearingExerciseId by remember { mutableStateOf<Long?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            OneRMSection(
                uiState = uiState,
                onToggleOneRMSection = { viewModel.toggleOneRMSection() },
                onToggleBig4SubSection = { viewModel.toggleBig4SubSection() },
                onToggleOtherSubSection = { viewModel.toggleOtherSubSection() },
                onAddMax = { exercise ->
                    editingMax = ExerciseMaxWithName(
                        id = 0,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        oneRMEstimate = 0f,
                        oneRMDate = java.time.LocalDateTime.now(),
                        oneRMContext = "Manually set",
                        oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                        notes = null,
                        sessionCount = 0,
                    )
                },
                onEditMax = { exercise ->
                    editingMax = ExerciseMaxWithName(
                        id = 0,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        oneRMEstimate = exercise.oneRMValue ?: 0f,
                        oneRMDate = java.time.LocalDateTime.now(),
                        oneRMContext = exercise.oneRMContext ?: "Manually set",
                        oneRMType = exercise.oneRMType ?: com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                        notes = null,
                        sessionCount = exercise.sessionCount,
                    )
                },
                onClearMax = { exerciseId ->
                    clearingExerciseId = exerciseId
                    showClearConfirmDialog = true
                },
                onShowExerciseSelector = { showExerciseSelector = true },
                onEditOtherMax = { max -> editingMax = max },
                onDeleteMax = { exerciseId -> viewModel.deleteMax(exerciseId) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExportSection(
                uiState = uiState,
                onToggleDataManagement = { viewModel.toggleDataManagementSection() },
                onShowExportDialog = { showExportDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(
                uiState = uiState,
                onUpdateSeedingWeeks = { weeks -> viewModel.updateSeedingWeeks(weeks) },
                onSeedWorkoutData = { viewModel.seedWorkoutData() },
                onResetSeedingState = { viewModel.resetSeedingState() },
                onClearAllData = { viewModel.clearAllWorkoutData() },
                onForceAnalysis = { insightsViewModel?.forceAnalysis() }
            )
        }
    }

    ProfileDialogs(
        uiState = uiState,
        showExerciseSelector = showExerciseSelector,
        editingMax = editingMax,
        showClearConfirmDialog = showClearConfirmDialog,
        clearingExerciseId = clearingExerciseId,
        showExportDialog = showExportDialog,
        snackbarHostState = snackbarHostState,
        onExerciseSelected = { exercise ->
            val existingMax = uiState.currentMaxes.find { it.exerciseId == exercise.variation.id }
            if (existingMax != null) {
                editingMax = existingMax
            } else {
                editingMax = ExerciseMaxWithName(
                    id = 0,
                    exerciseId = exercise.variation.id,
                    exerciseName = exercise.variation.name,
                    oneRMEstimate = 0f,
                    oneRMDate = java.time.LocalDateTime.now(),
                    oneRMContext = "Manually set",
                    oneRMType = com.github.radupana.featherweight.data.profile.OneRMType.MANUALLY_ENTERED,
                    notes = null,
                    sessionCount = 0,
                )
            }
            showExerciseSelector = false
        },
        onDismissExerciseSelector = { showExerciseSelector = false },
        onExportWorkouts = { startDate, endDate ->
            viewModel.exportWorkouts(startDate, endDate)
            showExportDialog = false
        },
        onDismissExportDialog = { showExportDialog = false },
        onSave1RM = { max, newMax ->
            viewModel.update1RM(max.exerciseId, max.exerciseName, newMax)
            editingMax = null
        },
        onDismissEdit = { editingMax = null },
        onConfirmClear = {
            clearingExerciseId?.let { viewModel.deleteMax(it) }
            showClearConfirmDialog = false
            clearingExerciseId = null
        },
        onDismissClear = {
            showClearConfirmDialog = false
            clearingExerciseId = null
        },
        onClearExportedFile = { viewModel.clearExportedFile() },
        onClearError = { viewModel.clearError() },
        onClearSuccessMessage = { viewModel.clearSuccessMessage() }
    )
}

@Composable
private fun OneRMSection(
    uiState: com.github.radupana.featherweight.viewmodel.ProfileUiState,
    onToggleOneRMSection: () -> Unit,
    onToggleBig4SubSection: () -> Unit,
    onToggleOtherSubSection: () -> Unit,
    onAddMax: (com.github.radupana.featherweight.viewmodel.Big4Exercise) -> Unit,
    onEditMax: (com.github.radupana.featherweight.viewmodel.Big4Exercise) -> Unit,
    onClearMax: (Long) -> Unit,
    onShowExerciseSelector: () -> Unit,
    onEditOtherMax: (ExerciseMaxWithName) -> Unit,
    onDeleteMax: (Long) -> Unit
) {
    CollapsibleSection(
        title = "One Rep Maxes",
        isExpanded = uiState.isOneRMSectionExpanded,
        onToggle = onToggleOneRMSection,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SubSection(
                title = "Big Four",
                isExpanded = uiState.isBig4SubSectionExpanded,
                onToggle = onToggleBig4SubSection,
            ) {
                uiState.big4Exercises.forEach { exercise ->
                    Big4ExerciseCard(
                        exerciseName = getDisplayName(exercise.exerciseName),
                        oneRMValue = exercise.oneRMValue,
                        oneRMType = exercise.oneRMType,
                        oneRMContext = exercise.oneRMContext,
                        oneRMDate = exercise.oneRMDate,
                        sessionCount = exercise.sessionCount,
                        onAdd = { onAddMax(exercise) },
                        onEdit = { onEditMax(exercise) },
                        onClear = { onClearMax(exercise.exerciseId) },
                    )
                }
            }

            if (uiState.otherExercises.isNotEmpty()) {
                SubSection(
                    title = "Other",
                    isExpanded = uiState.isOtherSubSectionExpanded,
                    onToggle = onToggleOtherSubSection,
                    showDivider = true,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = onShowExerciseSelector) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add exercise",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.otherExercises.forEach { max ->
                            OtherExerciseCard(
                                exerciseName = max.exerciseName,
                                oneRMValue = max.oneRMEstimate,
                                oneRMType = max.oneRMType,
                                oneRMContext = max.oneRMContext,
                                oneRMDate = max.oneRMDate,
                                sessionCount = max.sessionCount,
                                onEdit = { onEditOtherMax(max) },
                                onDelete = { onDeleteMax(max.exerciseId) },
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Track More Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Add 1RM records for other exercises you want to track",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = onShowExerciseSelector) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
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

@Composable
private fun ExportSection(
    uiState: com.github.radupana.featherweight.viewmodel.ProfileUiState,
    onToggleDataManagement: () -> Unit,
    onShowExportDialog: () -> Unit
) {
    CollapsibleSection(
        title = "Data Management",
        isExpanded = uiState.isDataManagementSectionExpanded,
        onToggle = onToggleDataManagement,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Export Workout Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Download your workout history as JSON for analysis with AI tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onShowExportDialog,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Workout Data")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    uiState: com.github.radupana.featherweight.viewmodel.ProfileUiState,
    onUpdateSeedingWeeks: (Int) -> Unit,
    onSeedWorkoutData: () -> Unit,
    onResetSeedingState: () -> Unit,
    onClearAllData: () -> Unit,
    onForceAnalysis: () -> Unit
) {
    CollapsibleSection(
        title = "Developer Tools",
        isExpanded = true,
        onToggle = { },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Seed Workout Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Generate realistic workout data for testing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Weeks to generate: ${uiState.seedingWeeks}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row {
                            TextButton(
                                onClick = { onUpdateSeedingWeeks(uiState.seedingWeeks - 1) },
                                enabled = uiState.seedingWeeks > 1,
                            ) {
                                Text("-")
                            }
                            TextButton(
                                onClick = { onUpdateSeedingWeeks(uiState.seedingWeeks + 1) },
                                enabled = uiState.seedingWeeks < 52,
                            ) {
                                Text("+")
                            }
                        }
                    }
                    val seedingState = uiState.seedingState
                    FilledTonalButton(
                        onClick = onSeedWorkoutData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = seedingState !is com.github.radupana.featherweight.viewmodel.SeedingState.InProgress,
                    ) {
                        Text(
                            when (seedingState) {
                                is com.github.radupana.featherweight.viewmodel.SeedingState.InProgress -> "Generating..."
                                is com.github.radupana.featherweight.viewmodel.SeedingState.Success -> "Generated ${seedingState.workoutsCreated} workouts"
                                else -> "Generate Workouts"
                            },
                        )
                    }
                    if (seedingState is com.github.radupana.featherweight.viewmodel.SeedingState.Success) {
                        TextButton(
                            onClick = onResetSeedingState,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Generate More")
                        }
                    }
                }
            }

            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Clear All Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Remove all workout data (cannot be undone)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onClearAllData,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Clear All Workout Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Training Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Force training analysis to run (bypasses weekly schedule)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onForceAnalysis,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Force Training Analysis")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDialogs(
    uiState: com.github.radupana.featherweight.viewmodel.ProfileUiState,
    showExerciseSelector: Boolean,
    editingMax: ExerciseMaxWithName?,
    showClearConfirmDialog: Boolean,
    clearingExerciseId: Long?,
    showExportDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onExerciseSelected: (com.github.radupana.featherweight.data.exercise.ExerciseWithDetails) -> Unit,
    onDismissExerciseSelector: () -> Unit,
    onExportWorkouts: (java.time.LocalDateTime, java.time.LocalDateTime) -> Unit,
    onDismissExportDialog: () -> Unit,
    onSave1RM: (ExerciseMaxWithName, Float) -> Unit,
    onDismissEdit: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onClearExportedFile: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccessMessage: () -> Unit
) {
    if (showExerciseSelector) {
        ExerciseSelectorDialog(
            onExerciseSelected = onExerciseSelected,
            onDismiss = onDismissExerciseSelector,
        )
    }

    if (showExportDialog) {
        ExportWorkoutsDialog(
            onDismiss = onDismissExportDialog,
            onExport = onExportWorkouts,
        )
    }

    uiState.exportedFilePath?.let { filePath ->
        ExportShareDialog(
            filePath = filePath,
            onDismiss = onClearExportedFile,
        )
    }

    editingMax?.let { max ->
        Add1RMDialog(
            exerciseName = max.exerciseName,
            currentMax = if (max.oneRMEstimate > 0) max.oneRMEstimate else null,
            onSave = { newMax -> onSave1RM(max, newMax) },
            onDismiss = onDismissEdit,
        )
    }

    if (showClearConfirmDialog && clearingExerciseId != null) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text("Clear 1RM") },
            text = { Text("Are you sure you want to clear this 1RM record?") },
            confirmButton = {
                TextButton(onClick = onConfirmClear) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) {
                    Text("Cancel")
                }
            },
        )
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onClearSuccessMessage()
        }
    }
}

// Helper function to display user-friendly names
@Composable
private fun getDisplayName(exerciseName: String): String =
    when (exerciseName) {
        "Barbell Back Squat" -> "Squat"
        "Barbell Deadlift" -> "Deadlift"
        "Barbell Bench Press" -> "Bench Press"
        "Barbell Overhead Press" -> "Overhead Press"
        else -> exerciseName
    }
