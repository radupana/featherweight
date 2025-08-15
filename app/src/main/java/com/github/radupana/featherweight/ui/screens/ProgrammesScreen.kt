package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
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
import com.github.radupana.featherweight.data.programme.ProgrammeTemplate
import com.github.radupana.featherweight.ui.dialogs.ProgrammeSetupDialog
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel

@Composable
fun ProgrammesScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgrammeViewModel = viewModel(),
    onNavigateToActiveProgramme: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProgramme by viewModel.activeProgramme.collectAsState()
    val programmeProgress by viewModel.programmeProgress.collectAsState()
    val isKeyboardVisible by rememberKeyboardState()
    val compactPadding = if (isKeyboardVisible) 8.dp else 16.dp

    // Confirmation dialog states
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }


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

            // Error/Success Messages
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
                }

                // Templates Section header - show even when keyboard visible
                item {
                    Text(
                        text = "Predefined Programme Templates",
                        style =
                            if (isKeyboardVisible) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = compactPadding / 2),
                    )
                }

                items(uiState.templates) { template ->
                    ProgrammeTemplateCard(
                        template = template,
                        isActive = activeProgramme?.name == template.name,
                        onClick = {
                            if (activeProgramme?.name != template.name) {
                                viewModel.selectTemplate(template)
                            }
                        },
                        isCompact = isKeyboardVisible,
                    )
                }

                if (uiState.templates.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateCard()
                    }
                }
            }
        }
    }

    // Setup Dialog
    if (uiState.showSetupDialog && uiState.selectedTemplate != null) {
        ProgrammeSetupDialog(
            template = uiState.selectedTemplate!!,
            uiState = uiState,
            viewModel = viewModel,
            repository =
                com.github.radupana.featherweight.repository
                    .FeatherweightRepository(viewModel.getApplication()),
            onProgrammeCreated = {
                // Navigate to active programme screen after creation
                onNavigateToActiveProgramme?.invoke()
            },
        )
    }

    // Profile Update Prompt Dialog
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        var inProgressWorkoutCount by remember { mutableStateOf(0) }

        // Check for in-progress workouts when dialog opens
        LaunchedEffect(showDeleteConfirmDialog) {
            if (showDeleteConfirmDialog) {
                activeProgramme?.let { programme ->
                    inProgressWorkoutCount = viewModel.getInProgressWorkoutCount(programme)
                }
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

    // Overwrite Warning Dialog
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
    GlassCard(
        modifier =
            if (onNavigateToProgramme != null) {
                Modifier.clickable { onNavigateToProgramme() }
            } else {
                Modifier
            },
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

// Helper function to format enum names properly
private fun formatEnumName(enumName: String): String =
    enumName
        .split('_')
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

@Composable
private fun ProgrammeTemplateCard(
    template: ProgrammeTemplate,
    isActive: Boolean,
    onClick: () -> Unit,
    isCompact: Boolean = false,
) {
    val cardPadding = if (isCompact) 16.dp else 20.dp
    val cardColors =
        if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            )
        } else {
            CardDefaults.cardColors()
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isActive) { onClick() },
        colors = cardColors,
        elevation = CardDefaults.cardElevation(if (isActive) 0.dp else 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "by ${template.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Programme details - arranged in two rows to prevent text wrapping
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // First row: duration and difficulty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProgrammeDetail(
                        icon = Icons.Filled.Schedule,
                        text = "${template.durationWeeks} weeks",
                    )
                    ProgrammeDetail(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        text = formatEnumName(template.difficulty.name),
                    )
                }

                // Second row: additional features (if any)
                if (template.requiresMaxes || template.allowsAccessoryCustomization) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (template.requiresMaxes) {
                            ProgrammeDetail(
                                icon = Icons.Filled.Calculate,
                                text = "Requires 1RM",
                            )
                        }
                        if (template.allowsAccessoryCustomization) {
                            ProgrammeDetail(
                                icon = Icons.Filled.Tune,
                                text = "Customizable",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgrammeDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No programmes found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Try adjusting your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
