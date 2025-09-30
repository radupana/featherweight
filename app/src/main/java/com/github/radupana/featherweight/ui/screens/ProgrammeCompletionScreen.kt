package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.ui.dialogs.NotesInputModal
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.ProgrammeCompletionViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgrammeCompletionScreen(
    programmeId: String,
    onDismiss: () -> Unit,
) {
    // Get repository from application context
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository =
        remember {
            com.github.radupana.featherweight.repository
                .FeatherweightRepository(context.applicationContext as android.app.Application)
        }
    val viewModel: ProgrammeCompletionViewModel =
        viewModel(
            factory =
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ProgrammeCompletionViewModel(repository) as T
                    }
                },
        )

    val uiState by viewModel.uiState.collectAsState()
    var showNotesModal by remember { mutableStateOf(false) }
    var completionNotes by remember { mutableStateOf("") }
    var showDetailedAnalysis by remember { mutableStateOf(false) }

    LaunchedEffect(programmeId) {
        viewModel.loadProgrammeCompletionStats(programmeId)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.completionStats != null,
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = EaseOutCubic),
                ) + fadeIn(),
            exit = fadeOut(),
        ) {
            uiState.completionStats?.let { stats ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Hero Section
                    ProgrammeHeroCard(stats)

                    // Primary Stats
                    PrimaryStatsCard(stats)

                    // Performance Analysis (consolidated 1RM improvements)
                    if (stats.strengthImprovements.isNotEmpty()) {
                        PerformanceAnalysisCard(
                            stats = stats,
                            isExpanded = showDetailedAnalysis,
                            onToggle = { showDetailedAnalysis = !showDetailedAnalysis },
                        )
                    }

                    // Programme Insights
                    ProgrammeInsightsCard(stats)

                    // Notes Section
                    NotesCard(
                        notes = completionNotes,
                        onEditNotes = { showNotesModal = true },
                    )

                    // Continue Button
                    Button(
                        onClick = {
                            // Save notes before dismissing
                            if (completionNotes.isNotBlank()) {
                                viewModel.saveProgrammeNotes(programmeId, completionNotes)
                            }
                            onDismiss()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Extra spacer for scrolling
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Loading state
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        // Error state
        uiState.error?.let { error ->
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }

    // Notes Modal
    NotesInputModal(
        isVisible = showNotesModal,
        title = "Programme Completion Notes",
        initialNotes = completionNotes,
        onNotesChanged = { newNotes ->
            completionNotes = newNotes
        },
        onDismiss = {
            showNotesModal = false
        },
    )
}

@Composable
private fun ProgrammeHeroCard(stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Programme Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stats.programmeName,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Completed on ${stats.endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrimaryStatsCard(stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgrammeStatItem(
                    label = "Total Workouts",
                    value = stats.totalWorkouts.toString(),
                )

                TopExercisesStatItem(
                    exercises = stats.topExercises,
                )

                ProgrammeStatItem(
                    label = "Avg Duration",
                    value = formatAverageDuration(stats.averageWorkoutDuration),
                )
            }
        }
    }
}

@Composable
private fun TopExercisesStatItem(
    exercises: List<com.github.radupana.featherweight.data.programme.ExerciseFrequency>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Top Exercises",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        exercises.take(3).forEach { exercise ->
            Text(
                text = "${exercise.exerciseName} (${exercise.frequency}x)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProgrammeStatItem(
    label: String,
    value: String,
    suffix: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value + if (suffix.isNotEmpty()) " $suffix" else "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PerformanceAnalysisCard(
    stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    var showAllImprovements by remember { mutableStateOf(false) }

    GlassmorphicCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (stats.averageStrengthImprovement > 0) {
                            Text(
                                text = "Average improvement: +${String.format(Locale.US, "%.1f", stats.averageStrengthImprovement)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Hide" else "Show",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    Text(
                        text = "Top Improvements",
                        style = MaterialTheme.typography.titleMedium,
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
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", improvement.improvementPercentage)}%",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${WeightFormatter.formatWeightWithUnit(improvement.startingMax)} → ${
                                        WeightFormatter.formatWeightWithUnit(improvement.endingMax)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                            ) {
                                                Text(
                                                    text = "+${String.format(Locale.US, "%.1f", improvement.improvementPercentage)}%",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    text = "${WeightFormatter.formatWeightWithUnit(improvement.startingMax)} → ${
                                                        WeightFormatter.formatWeightWithUnit(improvement.endingMax)
                                                    }",
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

@Composable
private fun ProgrammeInsightsCard(stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Programme Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.insights.mostConsistentDay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "You trained most on ${stats.insights.mostConsistentDay}s",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (stats.insights.averageRestDaysBetweenWorkouts > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", stats.insights.averageRestDaysBetweenWorkouts)} average rest days between workouts",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    onEditNotes: () -> Unit,
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Completion Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                TextButton(onClick = onEditNotes) {
                    Text(if (notes.isBlank()) "Add Notes" else "Edit Notes")
                }
            }

            if (notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How was your experience? Any thoughts on the programme?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun formatAverageDuration(duration: java.time.Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
