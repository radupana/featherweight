package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.ui.components.preview.*
import com.github.radupana.featherweight.viewmodel.ProgrammePreviewViewModel
import com.github.radupana.featherweight.viewmodel.GeneratedProgrammeHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammePreviewScreen(
    onBack: () -> Unit,
    onActivated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgrammePreviewViewModel = viewModel()
) {
    val previewState by viewModel.previewState.collectAsState()
    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val editStates by viewModel.editStates.collectAsState()
    
    // Auto-load generated programme when screen appears
    LaunchedEffect(Unit) {
        val generatedResponse = GeneratedProgrammeHolder.getGeneratedProgramme()
        if (generatedResponse != null) {
            viewModel.loadGeneratedProgramme(generatedResponse)
            // Clear after loading to prevent re-loading on configuration changes
            GeneratedProgrammeHolder.clearGeneratedProgramme()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Generated Programme",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = previewState) {
            is PreviewState.Loading -> {
                LoadingPreviewContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is PreviewState.Success -> {
                SuccessPreviewContent(
                    preview = state.preview,
                    selectedWeek = selectedWeek,
                    editStates = editStates,
                    onWeekSelected = viewModel::selectWeek,
                    onExerciseResolved = viewModel::resolveExercise,
                    onExerciseSwapped = viewModel::swapExercise,
                    onExerciseUpdated = viewModel::updateExercise,
                    onToggleEdit = viewModel::toggleExerciseEdit,
                    onShowAlternatives = viewModel::showExerciseAlternatives,
                    onShowResolution = viewModel::showExerciseResolution,
                    onBulkEdit = viewModel::applyBulkEdit,
                    onProgrammeNameChanged = viewModel::updateProgrammeName,
                    onRegenerate = viewModel::regenerate,
                    onActivate = { 
                        viewModel.activateProgramme(
                            startDate = java.time.LocalDate.now(),
                            onSuccess = onActivated
                        )
                    },
                    onFixIssue = viewModel::autoFixValidationIssue,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is PreviewState.Error -> {
                ErrorPreviewContent(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = { /* TODO: Implement retry */ },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun LoadingPreviewContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Generating your programme...",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "This may take a few moments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuccessPreviewContent(
    preview: GeneratedProgrammePreview,
    selectedWeek: Int,
    editStates: Map<String, ExerciseEditState>,
    onWeekSelected: (Int) -> Unit,
    onExerciseResolved: (String, Long) -> Unit,
    onExerciseSwapped: (String, String) -> Unit,
    onExerciseUpdated: (QuickEditAction.UpdateExercise) -> Unit,
    onToggleEdit: (String) -> Unit,
    onShowAlternatives: (String, Boolean) -> Unit,
    onShowResolution: (String, Boolean) -> Unit,
    onBulkEdit: (QuickEditAction) -> Unit,
    onProgrammeNameChanged: (String) -> Unit,
    onRegenerate: (RegenerationMode) -> Unit,
    onActivate: () -> Unit,
    onFixIssue: (ValidationIssue) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRegenerateDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Programme Header
        item {
            ProgrammeHeaderCard(
                preview = preview,
                onNameChanged = onProgrammeNameChanged
            )
        }
        
        // Programme Overview
        item {
            ProgrammeOverviewCard(preview = preview)
        }
        
        // Validation Results
        if (preview.validationResult.hasWarnings || !preview.validationResult.isValid) {
            item {
                ValidationResultCard(
                    validationResult = preview.validationResult,
                    onFixIssue = onFixIssue,
                    onBulkFix = {
                        // Apply auto-fix to all auto-fixable issues only
                        preview.validationResult.errors
                            .filter { it.isAutoFixable }
                            .forEach { error ->
                                onFixIssue(error)
                            }
                    }
                )
            }
        }
        
        // Week Selector
        if (preview.weeks.size > 1) {
            item {
                WeekSelectorCard(
                    weeks = preview.weeks,
                    selectedWeek = selectedWeek,
                    onWeekSelected = onWeekSelected
                )
            }
        }
        
        // Bulk Edit Options
        item {
            BulkEditCard(
                onBulkEdit = onBulkEdit
            )
        }
        
        // Workouts for Selected Week
        val currentWeek = preview.weeks.find { it.weekNumber == selectedWeek }
        currentWeek?.let { week ->
            items(week.workouts) { workout ->
                WorkoutPreviewCard(
                    workout = workout,
                    editStates = editStates,
                    onExerciseResolved = onExerciseResolved,
                    onExerciseSwapped = onExerciseSwapped,
                    onExerciseUpdated = onExerciseUpdated,
                    onToggleEdit = onToggleEdit,
                    onShowAlternatives = onShowAlternatives,
                    onShowResolution = onShowResolution
                )
            }
        }
        
        // Action Buttons
        item {
            ActionButtonsCard(
                validationResult = preview.validationResult,
                onRegenerate = { showRegenerateDialog = true },
                onActivate = onActivate
            )
        }
    }
    
    // Regenerate Dialog
    if (showRegenerateDialog) {
        RegenerateDialog(
            onDismiss = { showRegenerateDialog = false },
            onRegenerate = { mode ->
                showRegenerateDialog = false
                onRegenerate(mode)
            }
        )
    }
}

@Composable
private fun ErrorPreviewContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Generation Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Go Back")
                    }
                    
                    if (canRetry) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegenerateDialog(
    onDismiss: () -> Unit,
    onRegenerate: (RegenerationMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Regenerate Programme")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RegenerationMode.values()) { mode ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { onRegenerate(mode) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}