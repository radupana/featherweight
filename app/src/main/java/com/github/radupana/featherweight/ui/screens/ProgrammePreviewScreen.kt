package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseEditState
import com.github.radupana.featherweight.data.GeneratedProgrammePreview
import com.github.radupana.featherweight.data.PreviewState
import com.github.radupana.featherweight.data.QuickEditAction
import com.github.radupana.featherweight.data.RegenerationMode
import com.github.radupana.featherweight.data.WeekPreview
import com.github.radupana.featherweight.ui.components.preview.ActionButtonsCard
import com.github.radupana.featherweight.ui.components.preview.CollapsibleWeekCard
import com.github.radupana.featherweight.ui.components.preview.ProgrammeHeaderCard
import com.github.radupana.featherweight.ui.components.preview.ProgrammeOverviewCard
import com.github.radupana.featherweight.ui.dialogs.UnmatchedExerciseDialog
import com.github.radupana.featherweight.viewmodel.GeneratedProgrammeHolder
import com.github.radupana.featherweight.viewmodel.ProgrammePreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammePreviewScreen(
    onBack: () -> Unit,
    onActivated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgrammePreviewViewModel = viewModel(),
) {
    val previewState by viewModel.previewState.collectAsState()
    val editStates by viewModel.editStates.collectAsState()
    val unmatchedExercises by viewModel.unmatchedExercises.collectAsState()
    val showUnmatchedDialog by viewModel.showUnmatchedDialog.collectAsState()
    val currentUnmatchedExercise by viewModel.currentUnmatchedExercise.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()

    // Auto-load generated programme when screen appears
    LaunchedEffect(Unit) {
        val generatedResponse = GeneratedProgrammeHolder.getGeneratedProgramme()
        val validationResult = GeneratedProgrammeHolder.getValidationResult()
        if (generatedResponse != null) {
            viewModel.loadGeneratedProgramme(generatedResponse, validationResult)
            // Note: We do NOT clear GeneratedProgrammeHolder here anymore
            // It will be cleared in ProgrammePreviewViewModel.activateProgramme() after successful activation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Generated Programme",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when (val state = previewState) {
            is PreviewState.Loading -> {
                LoadingPreviewContent(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }

            is PreviewState.Success -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                ) {
                    // Show unmatched exercises banner if any
                    if (unmatchedExercises.isNotEmpty()) {
                        UnmatchedExercisesBanner(
                            count = unmatchedExercises.size,
                            onFixExercises = {
                                // Show first unmatched exercise
                                unmatchedExercises.firstOrNull()?.let {
                                    viewModel.showUnmatchedExerciseDialog(it)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    SuccessPreviewContent(
                        preview = state.preview,
                        editStates = editStates,
                        isActivating = false,
                        onExerciseResolved = viewModel::resolveExercise,
                        onExerciseSwapped = viewModel::swapExercise,
                        onExerciseUpdated = viewModel::updateExercise,
                        onToggleEdit = viewModel::toggleExerciseEdit,
                        onShowAlternatives = viewModel::showExerciseAlternatives,
                        onShowResolution = viewModel::showExerciseResolution,
                        onProgrammeNameChanged = viewModel::updateProgrammeName,
                        onActivate = {
                            if (unmatchedExercises.isEmpty()) {
                                viewModel.activateProgramme(
                                    onSuccess = onActivated,
                                )
                            } else {
                                // Show first unmatched exercise
                                unmatchedExercises.firstOrNull()?.let {
                                    viewModel.showUnmatchedExerciseDialog(it)
                                }
                            }
                        },
                        onDiscard =
                            if (GeneratedProgrammeHolder.getAIRequestId() != null) {
                                {
                                    viewModel.discardProgramme(
                                        onSuccess = onBack,
                                    )
                                }
                            } else {
                                null
                            },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is PreviewState.Activating -> {
                // Show simple loading state during activation
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = "Activating programme...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            is PreviewState.Error -> {
                ErrorPreviewContent(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = { viewModel.activateProgramme(onSuccess = onActivated) },
                    onBack = onBack,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }
        }

        // Show unmatched exercise dialog
        currentUnmatchedExercise?.let { unmatchedExercise ->
            if (showUnmatchedDialog) {
                UnmatchedExerciseDialog(
                    unmatchedExercise = unmatchedExercise,
                    allExercises = allExercises,
                    onExerciseSelected = { exercise ->
                        viewModel.selectExerciseForUnmatched(unmatchedExercise, exercise)

                        // Check if there are more unmatched exercises
                        val remainingUnmatched = unmatchedExercises.filter { it != unmatchedExercise }
                        if (remainingUnmatched.isNotEmpty()) {
                            // Show next unmatched exercise
                            viewModel.showUnmatchedExerciseDialog(remainingUnmatched.first())
                        }
                    },
                    onDismiss = { viewModel.hideUnmatchedExerciseDialog() },
                )
            }
        }
    }

    // Load exercises for the dialog
    LaunchedEffect(Unit) {
        viewModel.loadExercises()
    }
}

@Composable
private fun UnmatchedExercisesBanner(
    count: Int,
    onFixExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
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
                    text = "$count exercises need selection",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "AI suggested exercises not found in database",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Button(
                onClick = onFixExercises,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Fix Now")
            }
        }
    }
}

@Composable
private fun LoadingPreviewContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Generating your programme...",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "This may take a few moments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SuccessPreviewContent(
    preview: GeneratedProgrammePreview,
    editStates: Map<String, ExerciseEditState>,
    isActivating: Boolean,
    onExerciseResolved: (String, Long) -> Unit,
    onExerciseSwapped: (String, String) -> Unit,
    onExerciseUpdated: (QuickEditAction.UpdateExercise) -> Unit,
    onToggleEdit: (String) -> Unit,
    onShowAlternatives: (String, Boolean) -> Unit,
    onShowResolution: (String, Boolean) -> Unit,
    onProgrammeNameChanged: (String) -> Unit,
    onActivate: () -> Unit,
    onDiscard: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expandedWeeks by remember { mutableStateOf(setOf(1)) } // Start with week 1 expanded

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Programme Header
        item {
            ProgrammeHeaderCard(
                preview = preview,
                onNameChanged = onProgrammeNameChanged,
            )
        }

        // Programme Overview
        item {
            ProgrammeOverviewCard(preview = preview)
        }

        // Validation Results removed for simpler UX

        // All Weeks with Collapsible UI
        items(preview.weeks) { week ->
            CollapsibleWeekCard(
                week = week,
                weekProgress = getWeekProgressInfo(week),
                isExpanded = week.weekNumber in expandedWeeks,
                onToggleExpanded = { weekNumber ->
                    expandedWeeks =
                        if (weekNumber in expandedWeeks) {
                            expandedWeeks - weekNumber
                        } else {
                            expandedWeeks + weekNumber
                        }
                },
                editStates = editStates,
                onExerciseResolved = onExerciseResolved,
                onExerciseSwapped = onExerciseSwapped,
                onExerciseUpdated = onExerciseUpdated,
                onToggleEdit = onToggleEdit,
                onShowAlternatives = onShowAlternatives,
                onShowResolution = onShowResolution,
            )
        }

        // Action Buttons
        item {
            ActionButtonsCard(
                isActivating = isActivating,
                onActivate = onActivate,
                onDiscard = onDiscard,
            )
        }
    }
}

@Composable
private fun ErrorPreviewContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp),
                )

                Text(
                    text = "Generation Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    ) {
                        Text("Go Back")
                    }

                    if (canRetry) {
                        Button(
                            onClick = onRetry,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                    contentColor = MaterialTheme.colorScheme.errorContainer,
                                ),
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
private fun RegenerateDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Regenerate Programme")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(RegenerationMode.values()) { mode ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        onClick = { /* Regenerate not implemented */ },
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
        },
    )
}

private fun getWeekProgressInfo(week: WeekPreview): String {
    // Simple progress info based on actual data we have
    val totalExercises = week.workouts.sumOf { it.exercises.size }
    val totalSets = week.weeklyVolume.totalSets

    return "$totalExercises exercises • $totalSets total sets"
}
