package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.ui.components.ParseRequestCard
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel

@Composable
fun ProgrammesScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgrammeViewModel = viewModel(),
    onNavigateToActiveProgramme: (() -> Unit)? = null,
    onNavigateToImport: (() -> Unit)? = null,
    onNavigateToImportWithText: ((String) -> Unit)? = null,
    onNavigateToImportWithParsedProgramme: ((com.github.radupana.featherweight.data.ParsedProgramme, Long) -> Unit)? = null,
    onClearImportedProgramme: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProgramme by viewModel.activeProgramme.collectAsState()
    val programmeProgress by viewModel.programmeProgress.collectAsState()
    val parseRequests by viewModel.parseRequests.collectAsState()
    val isKeyboardVisible by rememberKeyboardState()
    val compactPadding = if (isKeyboardVisible) 8.dp else 16.dp
    
    // Check if any parse request is currently being processed
    val isParsingInProgress = parseRequests.any { it.status == com.github.radupana.featherweight.data.ParseStatus.PROCESSING }
    val hasPendingParseRequests = parseRequests.any { 
        it.status != com.github.radupana.featherweight.data.ParseStatus.IMPORTED 
    }

    // Dialog states
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRawTextDialog by remember { mutableStateOf<String?>(null) }

    // Handle error messages
    LaunchedEffect(uiState.error) {
        // Auto-clear error messages after 3 seconds
        if (uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    // Data is loaded once in ViewModel init - no need for refresh on every screen appear

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Box
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = compactPadding),
        ) {
            // Header - outside the scrollable area

            // Success Messages
            uiState.successMessage?.let { message ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = compactPadding),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(compactPadding),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            
            // Error Messages
            uiState.error?.let { error ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = compactPadding),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(compactPadding),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(compactPadding),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding(NavigationContext.BOTTOM_NAVIGATION),
                contentPadding = PaddingValues(bottom = compactPadding),
            ) {
                // Import Programme Button - Prominent placement at the top
                if (activeProgramme == null) {
                    item {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !hasPendingParseRequests) { 
                                        if (!hasPendingParseRequests) {
                                            onNavigateToImport?.invoke()
                                        }
                                    },
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (hasPendingParseRequests) {
                                    // Show why import is disabled
                                    if (isParsingInProgress) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Parsing in progress...",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Please wait while we process your programme",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Review pending programme",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Complete the review below before importing another",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "Import",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Import Your Programme",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Paste any workout programme text and we'll parse it for you",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Active Programme Section
                activeProgramme?.let { programme ->
                    item {
                        ActiveProgrammeCard(
                            programme = programme,
                            progress = programmeProgress,
                            onDelete = { showDeleteConfirmDialog = true },
                            onNavigateToProgramme = onNavigateToActiveProgramme,
                            isCompact = isKeyboardVisible,
                        )
                    }
                    
                    // Removed secondary import button - users should not import when active programme exists
                }
                
                // Parse Requests Section
                if (parseRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "Imported Programmes Review",
                            style = if (isKeyboardVisible) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = compactPadding / 2),
                        )
                    }
                    
                    items(parseRequests) { request ->
                        ParseRequestCard(
                            request = request,
                            onView = { parsedProgramme, requestId ->
                                // Navigate to import screen with the parsed programme for preview
                                onNavigateToImportWithParsedProgramme?.invoke(parsedProgramme, requestId)
                            },
                            onViewRawText = { rawText ->
                                showRawTextDialog = rawText
                            },
                            onEditAndRetry = { rawText ->
                                // Navigate to import screen with pre-filled text
                                if (onNavigateToImportWithText != null) {
                                    onNavigateToImportWithText(rawText)
                                } else {
                                    onNavigateToImport?.invoke()
                                }
                            },
                            onDelete = {
                                viewModel.deleteParseRequest(request)
                                // Clear any imported programme state when deleting the parse request
                                onClearImportedProgramme?.invoke()
                            },
                        )
                    }
                }

            }
        }
    }


    // Profile Update Prompt Dialog - commented out for now
    /* 
    if (uiState.showProfileUpdatePrompt && uiState.pendingProfileUpdates.isNotEmpty()) {
        com.github.radupana.featherweight.ui.dialogs.ProfileUpdatePromptDialog(
            updates = uiState.pendingProfileUpdates,
            onConfirm = {
                viewModel.confirmProfileUpdate()
            },
            onDismiss = {
                viewModel.dismissProfileUpdatePrompt()
            },
        )
    }
    */

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        var inProgressWorkoutCount by remember { mutableStateOf(0) }

        // Check for in-progress workouts when dialog opens
        LaunchedEffect(showDeleteConfirmDialog) {
            activeProgramme?.let { programme ->
                inProgressWorkoutCount = viewModel.getInProgressWorkoutCount(programme)
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Programme?") },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete this programme?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val warningText =
                        buildString {
                            append("⚠️ This action cannot be undone!\n")
                            append("• All progress will be lost\n")
                            append("• All workout history will be deleted")
                            if (inProgressWorkoutCount > 0) {
                                append(
                                    "\n• $inProgressWorkoutCount in-progress workout${if (inProgressWorkoutCount > 1) "s" else ""} will be deleted",
                                )
                            }
                        }

                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeProgramme?.let { programme ->
                            viewModel.deleteProgramme(programme)
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    // Overwrite Warning Dialog - commented out (templates removed)
    /*
    if (uiState.showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelOverwriteProgramme() },
            title = { Text("Active Programme Warning") },
            text = {
                Column {
                    Text(
                        text = "You already have an active programme: ${activeProgramme?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To start a new programme, you must first delete your current one.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Would you like to delete it and continue?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeProgramme?.let { programme ->
                            viewModel.deleteProgramme(programme)
                        }
                        viewModel.confirmOverwriteProgramme()
                    },
                ) {
                    Text("Delete & Continue")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelOverwriteProgramme() },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
    */
    
    // Raw Text Dialog - Now scrollable!
    showRawTextDialog?.let { rawText ->
        AlertDialog(
            onDismissRequest = { showRawTextDialog = null },
            title = { Text("Submitted Programme Text") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp) // Fixed height so it doesn't take full screen
                ) {
                    Text(
                        text = "This is what was sent for parsing:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Take remaining space
                    ) {
                        Text(
                            text = rawText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()), // Make text scrollable
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Length: ${rawText.length} characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        // Copy to clipboard would be nice here
                        showRawTextDialog = null 
                    }
                ) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun ActiveProgrammeCard(
    programme: Programme,
    progress: ProgrammeProgress?,
    onDelete: () -> Unit,
    onNavigateToProgramme: (() -> Unit)? = null,
    isCompact: Boolean = false,
) {
    val cardPadding = if (isCompact) 16.dp else 20.dp
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onNavigateToProgramme != null) {
                        Modifier.clickable { onNavigateToProgramme() }
                    } else {
                        Modifier
                    }
                ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Programme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = programme.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Move progress info here, below the programme name
                    progress?.let { prog ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${prog.completedWorkouts}/${prog.totalWorkouts} workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete programme",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Progress indicators
            progress?.let { prog ->
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProgressMetric(
                        label = "Week",
                        value = "${prog.currentWeek}/${programme.durationWeeks}",
                        modifier = Modifier.weight(1f),
                    )
                    ProgressMetric(
                        label = "Adherence",
                        value = "${if (prog.adherencePercentage.isNaN()) 0 else prog.adherencePercentage.toInt()}%",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                val progressValue =
                    if (prog.totalWorkouts > 0) {
                        (prog.completedWorkouts.toFloat() / prog.totalWorkouts.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
