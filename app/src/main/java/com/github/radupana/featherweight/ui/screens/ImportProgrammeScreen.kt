package com.github.radupana.featherweight.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.SignInActivity
import com.github.radupana.featherweight.ui.components.AuthenticationRequiredPrompt
import com.github.radupana.featherweight.ui.components.FormatTipsDialog
import com.github.radupana.featherweight.ui.components.ImportLoadingContent
import com.github.radupana.featherweight.ui.components.ImportProgrammeInputForm
import com.github.radupana.featherweight.ui.components.ProgrammePreview
import com.github.radupana.featherweight.viewmodel.ImportProgrammeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgrammeScreen(
    onBack: () -> Unit,
    onProgrammeCreated: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProgrammes: () -> Unit = onBack,
    onNavigateToWorkoutEdit: (weekIndex: Int, workoutIndex: Int) -> Unit = { _, _ -> },
    onNavigateToExerciseMapping: () -> Unit = {},
    initialText: String? = null,
    viewModel: ImportProgrammeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditNotImplementedDialog by remember { mutableStateOf(false) }
    var showFormatTipsDialog by remember { mutableStateOf(false) }

    // Set initial text if provided
    LaunchedEffect(initialText) {
        initialText?.let {
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
            val context = LocalContext.current

            when {
                // Check authentication first - fail fast!
                !viewModel.isAuthenticated() -> {
                    AuthenticationRequiredPrompt(
                        featureName = "AI Programme Parsing",
                        description = "Sign in to use AI-powered programme parsing and sync your workouts across devices",
                        onSignInClick = {
                            context.startActivity(Intent(context, SignInActivity::class.java))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.isLoading -> {
                    ImportLoadingContent()
                }

                uiState.parsedProgramme != null -> {
                    uiState.parsedProgramme?.let { programme ->
                        ProgrammePreview(
                            programme = programme,
                            onConfirm = {
                                if (programme.unmatchedExercises.isNotEmpty()) {
                                    onNavigateToExerciseMapping()
                                } else {
                                    viewModel.confirmAndCreateProgramme(onSuccess = onProgrammeCreated)
                                }
                            },
                            onEditWorkout = onNavigateToWorkoutEdit,
                            modifier = Modifier.fillMaxSize(),
                            error = uiState.error,
                        )
                    }
                }

                else -> {
                    ImportProgrammeInputForm(
                        inputText = uiState.inputText,
                        error = uiState.error,
                        successMessage = uiState.successMessage,
                        editingFailedRequestId = uiState.editingFailedRequestId,
                        onTextChange = viewModel::updateInputText,
                        onShowFormatTips = { showFormatTipsDialog = true },
                        onParseProgramme = { viewModel.parseProgramme(onNavigateToProgrammes) },
                    )
                }
            }
        }
    }

    ImportProgrammeDialogs(
        showEditNotImplementedDialog = showEditNotImplementedDialog,
        showFormatTipsDialog = showFormatTipsDialog,
        onDismissEditDialog = { showEditNotImplementedDialog = false },
        onDismissFormatTipsDialog = { showFormatTipsDialog = false },
    )
}

@Composable
private fun ImportProgrammeDialogs(
    showEditNotImplementedDialog: Boolean,
    showFormatTipsDialog: Boolean,
    onDismissEditDialog: () -> Unit,
    onDismissFormatTipsDialog: () -> Unit,
) {
    if (showEditNotImplementedDialog) {
        AlertDialog(
            onDismissRequest = onDismissEditDialog,
            title = { Text("Coming Soon") },
            text = {
                Text("Workout editing is not yet implemented. For now, you can edit the text and re-parse if you need to make changes.")
            },
            confirmButton = {
                TextButton(onClick = onDismissEditDialog) {
                    Text("OK")
                }
            },
        )
    }

    if (showFormatTipsDialog) {
        FormatTipsDialog(
            onDismiss = onDismissFormatTipsDialog,
        )
    }
}
