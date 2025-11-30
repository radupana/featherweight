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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.components.ParseRequestCard
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.viewmodel.InProgressWorkout
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@Composable
fun ProgrammesScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgrammeViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    onNavigateToImport: (() -> Unit)? = null,
    onNavigateToImportWithText: ((String, String?) -> Unit)? = null,
    onNavigateToImportWithParsedProgramme: ((com.github.radupana.featherweight.data.ParsedProgramme, String) -> Unit)? = null,
    onClearImportedProgramme: (() -> Unit)? = null,
    onStartProgrammeWorkout: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProgramme by viewModel.activeProgramme.collectAsState()
    val programmeProgress by viewModel.programmeProgress.collectAsState()
    val nextWorkoutInfo by viewModel.nextWorkoutInfo.collectAsState()
    val parseRequests by viewModel.parseRequests.collectAsState()
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    val isKeyboardVisible by rememberKeyboardState()
    val compactPadding = if (isKeyboardVisible) 8.dp else 16.dp
    val scope = rememberCoroutineScope()

    val isParsingInProgress = parseRequests.any { it.status == com.github.radupana.featherweight.data.ParseStatus.PROCESSING }
    val hasPendingParseRequests =
        parseRequests.any {
            it.status != com.github.radupana.featherweight.data.ParseStatus.IMPORTED
        }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRawTextDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshProgrammeProgress()
        workoutViewModel.loadInProgressWorkouts()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                modifier
                    .fillMaxSize()
                    .padding(horizontal = compactPadding),
        ) {
            uiState.successMessage?.let { message ->
                GlassmorphicCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = compactPadding),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(compactPadding),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            uiState.error?.let { error ->
                GlassmorphicCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = compactPadding),
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = compactPadding, bottom = compactPadding),
            ) {
                if (activeProgramme == null) {
                    item {
                        GlassmorphicCard(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !hasPendingParseRequests) {
                                        if (!hasPendingParseRequests) {
                                            onNavigateToImport?.invoke()
                                        }
                                    },
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (hasPendingParseRequests) {
                                    if (isParsingInProgress) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.primary,
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
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
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

                activeProgramme?.let { programme ->
                    item {
                        ActiveProgrammeCard(
                            programme = programme,
                            progress = programmeProgress,
                            nextWorkoutInfo = nextWorkoutInfo,
                            inProgressWorkouts = inProgressWorkouts,
                            onDelete = { showDeleteConfirmDialog = true },
                            onStartWorkout = {
                                scope.launch {
                                    handleStartProgrammeWorkout(
                                        programme = programme,
                                        nextWorkoutInfo = nextWorkoutInfo,
                                        inProgressWorkouts = inProgressWorkouts,
                                        workoutViewModel = workoutViewModel,
                                        onNavigate = onStartProgrammeWorkout,
                                    )
                                }
                            },
                            isCompact = isKeyboardVisible,
                        )
                    }
                }

                if (parseRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "Imported Programmes Review",
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

                    items(parseRequests) { request ->
                        ParseRequestCard(
                            request = request,
                            onView = { parsedProgramme, requestId ->
                                onNavigateToImportWithParsedProgramme?.invoke(parsedProgramme, requestId)
                            },
                            onViewRawText = { rawText ->
                                showRawTextDialog = rawText
                            },
                            onEditAndRetry = { rawText ->
                                if (onNavigateToImportWithText != null) {
                                    onNavigateToImportWithText(rawText, request.id)
                                } else {
                                    onNavigateToImport?.invoke()
                                }
                            },
                            onDelete = {
                                viewModel.deleteParseRequest(request)
                                onClearImportedProgramme?.invoke()
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        var deleteWorkoutsChecked by remember { mutableStateOf(false) }
        var completedWorkoutCount by remember { mutableStateOf(0) }
        var completedSetCount by remember { mutableStateOf(0) }

        LaunchedEffect(activeProgramme) {
            activeProgramme?.let { programme ->
                completedWorkoutCount = viewModel.getCompletedWorkoutCount(programme)
                completedSetCount = viewModel.getCompletedSetCount(programme)
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (deleteWorkoutsChecked) "Delete Programme?" else "Stop Following Programme?") },
            text = {
                Column {
                    Text(
                        text =
                            if (deleteWorkoutsChecked) {
                                "This will permanently delete the programme and all workout history."
                            } else {
                                "You'll stop following this programme, but your workout history will be preserved in the History tab."
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = deleteWorkoutsChecked,
                            onCheckedChange = { deleteWorkoutsChecked = it },
                        )
                        Text(
                            text = "Also delete all workout history",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    if (deleteWorkoutsChecked && completedWorkoutCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val warningText =
                            buildString {
                                append("⚠️ This CANNOT be undone!\n\n")
                                append("This will permanently delete:\n")
                                append("• $completedWorkoutCount workout${if (completedWorkoutCount != 1) "s" else ""}\n")
                                append("• $completedSetCount set${if (completedSetCount != 1) "s" else ""} of logged data\n")
                                append("• All exercise performance history")
                            }
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeProgramme?.let { programme ->
                            viewModel.deleteProgramme(programme, deleteWorkoutsChecked)
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (deleteWorkoutsChecked) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        ),
                ) {
                    Text(
                        text =
                            if (deleteWorkoutsChecked) {
                                "Delete Everything"
                            } else {
                                "Stop Following"
                            },
                        color =
                            if (deleteWorkoutsChecked) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                    )
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

    showRawTextDialog?.let { rawText ->
        AlertDialog(
            onDismissRequest = { showRawTextDialog = null },
            title = { Text("Submitted Programme Text") },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                ) {
                    Text(
                        text = "This is what was sent for parsing:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        Text(
                            text = rawText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier =
                                Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Length: ${rawText.length} characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRawTextDialog = null
                    },
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
    nextWorkoutInfo: NextProgrammeWorkoutInfo?,
    inProgressWorkouts: List<InProgressWorkout>,
    onDelete: () -> Unit,
    onStartWorkout: () -> Unit,
    isCompact: Boolean = false,
) {
    val cardPadding = if (isCompact) 16.dp else 20.dp

    val existingWorkout =
        inProgressWorkouts.find { workout ->
            workout.isProgrammeWorkout &&
                workout.programmeId == programme.id &&
                nextWorkoutInfo != null &&
                workout.weekNumber == nextWorkoutInfo.actualWeekNumber &&
                workout.dayNumber == nextWorkoutInfo.workoutStructure.day
        }

    val hasAnyInProgressWorkout = inProgressWorkouts.isNotEmpty()

    GlassmorphicCard(
        modifier =
            Modifier
                .fillMaxWidth(),
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

                    progress?.let { prog ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${prog.completedWorkouts}/${prog.totalWorkouts} workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete programme",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

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
                        label = "Completed",
                        value = "${prog.completedWorkouts}/${prog.totalWorkouts}",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

            nextWorkoutInfo?.let { info ->
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (existingWorkout != null) "Workout In Progress" else "Next Workout",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )

                    Text(
                        text = info.workoutStructure.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text =
                            if (existingWorkout != null) {
                                "Week ${info.actualWeekNumber} • Day ${info.workoutStructure.day} • ${existingWorkout.completedSets}/${existingWorkout.setCount} sets"
                            } else {
                                "Week ${info.actualWeekNumber} • Day ${info.workoutStructure.day} • ${info.workoutStructure.exercises.size} exercises"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = existingWorkout != null || !hasAnyInProgressWorkout,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (existingWorkout != null) "Continue Workout" else "Start Next Workout",
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (existingWorkout == null && hasAnyInProgressWorkout) {
                        Text(
                            text = "Complete or discard existing in-progress workout first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } ?: run {
                if (progress?.completedWorkouts == progress?.totalWorkouts && (progress?.totalWorkouts ?: 0) > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "🎉 Programme Complete!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun handleStartProgrammeWorkout(
    programme: Programme,
    nextWorkoutInfo: NextProgrammeWorkoutInfo?,
    inProgressWorkouts: List<InProgressWorkout>,
    workoutViewModel: WorkoutViewModel,
    onNavigate: (() -> Unit)?,
) {
    if (nextWorkoutInfo == null) return

    val existingWorkout =
        inProgressWorkouts.find { workout ->
            workout.isProgrammeWorkout &&
                workout.programmeId == programme.id &&
                workout.weekNumber == nextWorkoutInfo.actualWeekNumber &&
                workout.dayNumber == nextWorkoutInfo.workoutStructure.day
        }

    if (existingWorkout != null) {
        workoutViewModel.resumeWorkout(existingWorkout.id)
        onNavigate?.invoke()
    } else {
        workoutViewModel.startProgrammeWorkout(
            programmeId = programme.id,
            weekNumber = nextWorkoutInfo.actualWeekNumber,
            dayNumber = nextWorkoutInfo.workoutStructure.day,
            onReady = {
                onNavigate?.invoke()
            },
        )
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
