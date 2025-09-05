package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Analyzing your programme...")
                        }
                    }
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
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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
                                        text = "Import Your Programme",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Paste from any source - we'll parse it!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { showFormatTipsDialog = true },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Format tips",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        Column {
                            OutlinedTextField(
                                value = uiState.inputText.take(10000),
                                onValueChange = { newText ->
                                    if (newText.length <= 10000) {
                                        // Preserve the editing request ID when updating text
                                        viewModel.updateInputText(newText, uiState.editingFailedRequestId)
                                    }
                                },
                                label = { Text("Programme Text") },
                                placeholder = {
                                    Text(
                                        "Week 1 - Volume Phase\n\n" +
                                            "Monday - Upper Power\n" +
                                            "Bench Press 3x5 @ 80%\n" +
                                            "Barbell Row 3x5 @ 75kg\n" +
                                            "Overhead Press 3x8\n\n" +
                                            "Wednesday - Lower Power\n" +
                                            "Squat 3x5 @ 85%\n" +
                                            "Romanian Deadlift 3x8\n...",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(350.dp),
                                singleLine = false,
                                maxLines = Int.MAX_VALUE,
                                isError = uiState.inputText.length > 10000,
                            )

                            Text(
                                text = "${uiState.inputText.length} / 10,000 characters",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    when {
                                        uiState.inputText.length > 10000 -> MaterialTheme.colorScheme.error
                                        uiState.inputText.length > 9500 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                modifier =
                                    Modifier
                                        .align(Alignment.End)
                                        .padding(top = 4.dp),
                            )
                        }

                        if (uiState.error != null) {
                            val errorMessage = uiState.error
                            if (errorMessage != null) {
                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = errorMessage,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }

                        if (uiState.successMessage != null) {
                            val successMessage = uiState.successMessage
                            if (successMessage != null) {
                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = successMessage,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
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

@Composable
private fun FormatTipsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Programme Parser",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Main description - no card needed
                Text(
                    text = "Convert any workout text into a trackable programme.\nKeep your format consistent and we'll handle the rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                // How it works section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "HOW IT WORKS",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Paste workout text from any source",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Use any exercise names consistently",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Any sets/reps format (3x10, 3 sets of 10)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "We'll match exercises to our database",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Tips section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "TIPS FOR BEST RESULTS",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Group exercises by day or workout",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Include weights when known",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Be consistent with exercise names",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Got It")
            }
        },
    )
}

@Composable
private fun ProgrammePreview(
    programme: com.github.radupana.featherweight.data.ParsedProgramme,
    onConfirm: () -> Unit,
    onEditWorkout: (weekIndex: Int, workoutIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedWeeks by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Edit Hint Card
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap any workout to edit exercises and sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        // Programme Header Card
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = programme.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                val totalWorkouts = programme.weeks.sumOf { it.workouts.size }
                val actualWeeks = programme.weeks.size

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$actualWeeks ${if (actualWeeks == 1) "week" else "weeks"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalWorkouts ${if (totalWorkouts == 1) "workout" else "workouts"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (programme.description.isNotBlank()) {
                    Text(
                        text = programme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Weekly Cards - All weeks shown, collapsible
        programme.weeks.forEachIndexed { weekIndex, week ->
            WeekCard(
                week = week,
                isExpanded = expandedWeeks.contains(week.weekNumber),
                onToggleExpand = {
                    expandedWeeks =
                        if (expandedWeeks.contains(week.weekNumber)) {
                            expandedWeeks - week.weekNumber
                        } else {
                            expandedWeeks + week.weekNumber
                        }
                },
                onEditWorkout = { workoutIndex ->
                    onEditWorkout(weekIndex, workoutIndex)
                },
            )
        }

        // Unmatched Exercises Warning
        if (programme.unmatchedExercises.isNotEmpty()) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${programme.unmatchedExercises.size} Unmatched Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "These exercises need to be mapped to existing ones or created as custom:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    programme.unmatchedExercises.take(5).forEach { exercise ->
                        Text(
                            text = "• $exercise",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (programme.unmatchedExercises.size > 5) {
                        Text(
                            text = "... and ${programme.unmatchedExercises.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        // Create/Continue Button
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (programme.unmatchedExercises.isEmpty()) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (programme.unmatchedExercises.isEmpty()) {
                    "Create Programme"
                } else {
                    "Map ${programme.unmatchedExercises.size} Custom Exercises"
                },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun WeekCard(
    week: com.github.radupana.featherweight.data.ParsedWeek,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditWorkout: (workoutIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggleExpand,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Week ${week.weekNumber}${if (week.name.isNotBlank() && week.name != "Week ${week.weekNumber}") " - ${week.name}" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = "${week.workouts.size} workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        week.intensityLevel?.let { intensity ->
                            Surface(
                                color =
                                    when (intensity) {
                                        "MAX" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        "HIGH" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                        "MODERATE" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = intensity,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color =
                                        when (intensity) {
                                            "MAX" -> MaterialTheme.colorScheme.error
                                            "HIGH" -> MaterialTheme.colorScheme.tertiary
                                            "MODERATE" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            }
                        }

                        if (week.isDeload) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = "DELOAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    week.workouts.forEachIndexed { workoutIndex, workout ->
                        WorkoutSummaryCard(
                            workout = workout,
                            workoutIndex = workoutIndex,
                            onClick = { onEditWorkout(workoutIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    workout: com.github.radupana.featherweight.data.ParsedWorkout,
    workoutIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = workout.dayOfWeek ?: "Day ${workoutIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit workout",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${workout.estimatedDurationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Exercise summary
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val exercisesToShow = workout.exercises.take(3)
                exercisesToShow.forEach { exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "• ${exercise.exerciseName}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${exercise.sets.size} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (workout.exercises.size > 3) {
                    Text(
                        text = "... and ${workout.exercises.size - 3} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
