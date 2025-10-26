package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.ui.components.FormatTipsDialog
import com.github.radupana.featherweight.ui.components.ImportErrorCard
import com.github.radupana.featherweight.ui.components.ImportHeader
import com.github.radupana.featherweight.ui.components.ImportLoadingContent
import com.github.radupana.featherweight.ui.components.ImportSuccessCard
import com.github.radupana.featherweight.ui.components.ProgrammePreview
import com.github.radupana.featherweight.ui.components.ProgrammeTextInput
import com.github.radupana.featherweight.viewmodel.ImportProgrammeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgrammeScreen(
    onBack: () -> Unit,
    onProgrammeCreated: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProgrammes: () -> Unit = onBack, // Default to going back
    onNavigateToWorkoutEdit: (weekIndex: Int, workoutIndex: Int) -> Unit = { _, _ -> },
    onNavigateToExerciseMapping: () -> Unit = {},
    initialText: String? = null,
    viewModel: ImportProgrammeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditNotImplementedDialog by remember { mutableStateOf(false) }
    var showFormatTipsDialog by remember { mutableStateOf(false) }

    // Set initial text if provided (only used for non-editing flows)
    LaunchedEffect(initialText) {
        initialText?.let {
            // Only update if text is not already set (prevents overwriting edit state)
            if (uiState.inputText.isEmpty()) {
                viewModel.updateInputText(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Programme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    ImportLoadingContent()
                }

                uiState.parsedProgramme != null -> {
                    val programme = uiState.parsedProgramme
                    if (programme != null) {
                        ProgrammePreview(
                            programme = programme,
                            onConfirm = {
                                // Check if there are unmatched exercises
                                if (programme.unmatchedExercises.isNotEmpty()) {
                                    // Navigate to exercise mapping screen
                                    onNavigateToExerciseMapping()
                                } else {
                                    // No unmatched exercises, create programme directly
                                    viewModel.confirmAndCreateProgramme(onSuccess = onProgrammeCreated)
                                }
                            },
                            onEditWorkout = { weekIndex, workoutIndex ->
                                onNavigateToWorkoutEdit(weekIndex, workoutIndex)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                else -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Header card with format tips button
                        ImportHeader(
                            onShowFormatTips = { showFormatTipsDialog = true },
                        )

                        ProgrammeTextInput(
                            text = uiState.inputText,
                            onTextChange = { newText ->
                                // Preserve the editing request ID when updating text
                                viewModel.updateInputText(newText, uiState.editingFailedRequestId)
                            },
                        )

                        uiState.error?.let { errorMessage ->
                            ImportErrorCard(errorMessage = errorMessage)
                        }

                        uiState.successMessage?.let { successMessage ->
                            ImportSuccessCard(successMessage = successMessage)
                        }

                        Button(
                            onClick = { viewModel.parseProgramme(onNavigateToProgrammes) },
                            enabled = uiState.inputText.length >= 50 && uiState.inputText.length <= 10000,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                        ) {
                            Text(
                                text = "Parse Programme",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        if (uiState.inputText.isNotEmpty() && uiState.inputText.length < 50) {
                            Text(
                                text = "Minimum 50 characters needed (${uiState.inputText.length}/50)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Edit Not Implemented Dialog
    if (showEditNotImplementedDialog) {
        AlertDialog(
            onDismissRequest = { showEditNotImplementedDialog = false },
            title = { Text("Coming Soon") },
            text = {
                Text("Workout editing is not yet implemented. For now, you can edit the text and re-parse if you need to make changes.")
            },
            confirmButton = {
                TextButton(onClick = { showEditNotImplementedDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    // Format Tips Dialog
    if (showFormatTipsDialog) {
        FormatTipsDialog(
            onDismiss = { showFormatTipsDialog = false },
        )
    }
}
