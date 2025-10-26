package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.radupana.featherweight.ui.dialogs.Add1RMDialog
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.ui.dialogs.ExportShareDialog
import com.github.radupana.featherweight.ui.dialogs.ExportWorkoutsDialog
import com.github.radupana.featherweight.ui.screens.profile.DataTab
import com.github.radupana.featherweight.ui.screens.profile.DeveloperTab
import com.github.radupana.featherweight.ui.screens.profile.OneRMTab
import com.github.radupana.featherweight.ui.screens.profile.SettingsTab
import com.github.radupana.featherweight.viewmodel.ExerciseMaxWithName
import com.github.radupana.featherweight.viewmodel.ProfileTab
import com.github.radupana.featherweight.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var clearingExerciseId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signOutRequested) {
        if (uiState.signOutRequested) {
            viewModel.clearSignOutRequest()
            onSignOut()
        }
    }

    // Refresh account state when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshAccountState()
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            ProfileTabBar(
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
            ) {
                when (uiState.currentTab) {
                    ProfileTab.ONE_RM -> {
                        OneRMTab(
                            uiState = uiState,
                            onToggleBig4SubSection = { viewModel.toggleBig4SubSection() },
                            onToggleOtherSubSection = { viewModel.toggleOtherSubSection() },
                            onAddMax = { exercise ->
                                editingMax =
                                    ExerciseMaxWithName(
                                        id = "",
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
                                editingMax =
                                    ExerciseMaxWithName(
                                        id = "",
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
                            onDeleteMax = { exerciseId -> viewModel.deleteMax(exerciseId) },
                        )
                    }
                    ProfileTab.SETTINGS -> {
                        SettingsTab(
                            currentWeightUnit = uiState.currentWeightUnit,
                            onWeightUnitSelected = { unit -> viewModel.setWeightUnit(unit) },
                            syncState = uiState.syncUiState,
                            accountInfo = uiState.accountInfo,
                            onSignOut = { viewModel.signOut() },
                            onSignIn = onSignIn,
                            onSendVerificationEmail = { viewModel.sendVerificationEmail() },
                            onChangePassword = { current, new -> viewModel.changePassword(current, new) },
                            onDeleteAccount = { viewModel.deleteAccount() },
                        )
                    }
                    ProfileTab.DATA -> {
                        DataTab(
                            onShowExportDialog = { showExportDialog = true },
                        )
                    }
                    ProfileTab.DEVELOPER -> {
                        DeveloperTab(
                            uiState = uiState,
                            onUpdateSeedingWeeks = { weeks -> viewModel.updateSeedingWeeks(weeks) },
                            onSeedWorkoutData = { viewModel.seedWorkoutData() },
                            onResetSeedingState = { viewModel.resetSeedingState() },
                            onClearAllData = { viewModel.clearAllWorkoutData() },
                        )
                    }
                }
            }
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
                editingMax =
                    ExerciseMaxWithName(
                        id = "",
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
        onClearSuccessMessage = { viewModel.clearSuccessMessage() },
    )
}

@Composable
private fun ProfileTabBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Tab(
            selected = selectedTab == ProfileTab.ONE_RM,
            onClick = { onTabSelected(ProfileTab.ONE_RM) },
            text = { Text("1RMs") },
        )
        Tab(
            selected = selectedTab == ProfileTab.SETTINGS,
            onClick = { onTabSelected(ProfileTab.SETTINGS) },
            text = { Text("Settings") },
        )
        Tab(
            selected = selectedTab == ProfileTab.DATA,
            onClick = { onTabSelected(ProfileTab.DATA) },
            text = { Text("Data") },
        )
        Tab(
            selected = selectedTab == ProfileTab.DEVELOPER,
            onClick = { onTabSelected(ProfileTab.DEVELOPER) },
            text = { Text("Dev") },
        )
    }
}

@Composable
private fun ProfileDialogs(
    uiState: com.github.radupana.featherweight.viewmodel.ProfileUiState,
    showExerciseSelector: Boolean,
    editingMax: ExerciseMaxWithName?,
    showClearConfirmDialog: Boolean,
    clearingExerciseId: String?,
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
    onClearSuccessMessage: () -> Unit,
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
