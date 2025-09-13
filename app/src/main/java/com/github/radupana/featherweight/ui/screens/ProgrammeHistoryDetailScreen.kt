package com.github.radupana.featherweight.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.domain.ProgrammeHistoryDetails
import com.github.radupana.featherweight.domain.WorkoutHistoryEntry
import com.github.radupana.featherweight.viewmodel.ProgrammeHistoryDetailViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeHistoryDetailScreen(
    programmeId: Long,
    onBack: () -> Unit,
    onViewWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgrammeHistoryDetailViewModel = viewModel(),
) {
    val programmeDetails by viewModel.programmeDetails.collectAsState()
    val completionStats by viewModel.completionStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val pendingExportFile by viewModel.pendingExportFile.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    // File save launcher
    val saveFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let {
                viewModel.saveExportedFile(it)
            }
        }

    // Launch save dialog when export is ready
    LaunchedEffect(pendingExportFile) {
        pendingExportFile?.let { file ->
            saveFileLauncher.launch(file.name)
        }
    }

    LaunchedEffect(programmeId) {
        viewModel.loadProgrammeDetails(programmeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programme Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoading && programmeDetails != null) {
                        IconButton(
                            onClick = { showExportDialog = true },
                            enabled = !isExporting,
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export Programme",
                                )
                            }
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Error loading programme details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProgrammeDetails(programmeId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            programmeDetails != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProgrammeDetailsContent(
                        details = programmeDetails!!,
                        completionStats = completionStats,
                        onViewWorkout = onViewWorkout,
                        modifier = Modifier.padding(paddingValues),
                    )

                    if (isExporting && exportProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(paddingValues),
                        )
                    }
                }
            }
        }
    }

    // Export confirmation dialog
    if (showExportDialog) {
        programmeDetails?.let { details ->
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Programme") },
                text = {
                    Text("Export \"${details.name}\"?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.exportProgramme(programmeId)
                            showExportDialog = false
                        },
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
fun ProgrammeDetailsContent(
    details: ProgrammeHistoryDetails,
    completionStats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats?,
    onViewWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetailedAnalysis by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Programme Header - Simplified
        item {
            Column {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Programme metadata tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text =
                                details.programmeType.name
                                    .replace("_", " ")
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text =
                                details.difficulty.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dates
                Text(
                    text = "Started: ${details.startedAt!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Completed: ${details.completedAt!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Programme Insights Section (if stats available)
        if (completionStats != null) {
            item {
                ProgrammeInsightsSection(
                    stats = completionStats,
                    durationWeeks = details.durationWeeks,
                    showDetailedAnalysis = showDetailedAnalysis,
                    onToggleDetailedAnalysis = { showDetailedAnalysis = !showDetailedAnalysis },
                )
            }
        }

        // Workout History Section
        item {
            Text(
                text = "Workout History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // Group workouts by week
        val workoutsByWeek = details.workoutHistory.groupBy { it.weekNumber }
        val sortedWeeks = workoutsByWeek.keys.sorted()

        sortedWeeks.forEach { weekNumber ->
            val weekWorkouts = workoutsByWeek[weekNumber]!!.sortedBy { it.dayNumber }
            val totalCount = weekWorkouts.size

            item {
                CollapsibleWeekSection(
                    weekNumber = weekNumber,
                    totalCount = totalCount,
                    workouts = weekWorkouts,
                    onViewWorkout = onViewWorkout,
                )
            }
        }

        // Completion Notes (if any) - moved to bottom
        if (!details.completionNotes.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Completion Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = details.completionNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleWeekSection(
    weekNumber: Int,
    totalCount: Int,
    workouts: List<WorkoutHistoryEntry>,
    onViewWorkout: (Long) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        // Week header - always visible
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Week $weekNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (totalCount == 1) "1 workout" else "$totalCount workouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Workout entries - only visible when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                workouts.forEach { workout ->
                    WorkoutHistoryEntryCard(
                        workout = workout,
                        onViewWorkout = { onViewWorkout(workout.workoutId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryEntryCard(
    workout: WorkoutHistoryEntry,
    onViewWorkout: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onViewWorkout() },
        elevation = CardDefaults.cardElevation(1.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Day indicator
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color =
                    if (workout.completed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Day",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (workout.completed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        Text(
                            text = workout.dayNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (workout.completed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Workout details
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = workout.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (workout.completed && workout.completedAt != null) {
                    Text(
                        text = "Completed ${workout.completedAt.format(DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Not completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ProgrammeInsightsSection(
    stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats,
    durationWeeks: Int,
    showDetailedAnalysis: Boolean,
    onToggleDetailedAnalysis: () -> Unit,
) {
    var showAllImprovements by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Programme Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Hero Panel - Primary Stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // First row: Basic stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stats.totalWorkouts.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (durationWeeks == 1) "1 week" else "$durationWeeks weeks",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = formatDuration(stats.averageWorkoutDuration),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Avg Duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "N/A",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Avg RPE",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Divider
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )

                // Second row: Top Exercises
                Column {
                    Text(
                        text = "Top Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (stats.topExercises.isNotEmpty()) {
                        stats.topExercises.take(3).forEach { exercise ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = exercise.exerciseName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "${exercise.frequency}x",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No exercises recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Performance Analysis - Expandable
        if (stats.strengthImprovements.isNotEmpty()) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDetailedAnalysis() },
                elevation = CardDefaults.cardElevation(2.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    text = "Performance Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (stats.averageStrengthImprovement > 0) {
                                    Text(
                                        text = "Avg improvement: +${String.format(java.util.Locale.US, "%.1f", stats.averageStrengthImprovement)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = if (showDetailedAnalysis) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (showDetailedAnalysis) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AnimatedVisibility(
                        visible = showDetailedAnalysis,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )

                            // Top Improvements
                            Text(
                                text = "Top Improvements",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            val topImprovements =
                                stats.strengthImprovements
                                    .sortedByDescending { it.improvementPercentage }
                                    .take(5)

                            topImprovements.forEach { improvement ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = improvement.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "+${String.format(java.util.Locale.US, "%.1f", improvement.improvementPercentage)}% (${
                                            com.github.radupana.featherweight.util.WeightFormatter.formatWeightWithUnit(improvement.startingMax)
                                        } → ${
                                            com.github.radupana.featherweight.util.WeightFormatter.formatWeightWithUnit(improvement.endingMax)
                                        })",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Show All button if there are more improvements
                            if (stats.strengthImprovements.size > 5) {
                                TextButton(
                                    onClick = { showAllImprovements = !showAllImprovements },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = if (showAllImprovements) "Show Less" else "Show All ${stats.strengthImprovements.size} Exercises",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                AnimatedVisibility(visible = showAllImprovements) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        stats.strengthImprovements
                                            .sortedByDescending { it.improvementPercentage }
                                            .drop(5)
                                            .forEach { improvement ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(
                                                        text = improvement.exerciseName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Text(
                                                        text = "+${String.format(java.util.Locale.US, "%.1f", improvement.improvementPercentage)}% (${
                                                            com.github.radupana.featherweight.util.WeightFormatter.formatWeightWithUnit(improvement.startingMax)
                                                        } → ${
                                                            com.github.radupana.featherweight.util.WeightFormatter.formatWeightWithUnit(improvement.endingMax)
                                                        })",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(duration: java.time.Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
