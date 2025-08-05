package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.github.radupana.featherweight.data.programme.StrengthImprovement
import com.github.radupana.featherweight.ui.dialogs.NotesInputModal
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.ProgrammeCompletionViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ProgrammeCompletionScreen(
    programmeId: Long,
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
                    animationSpec = tween(500, easing = EaseOutBack),
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

                    // Strength Progress (if any improvements)
                    if (stats.strengthImprovements.isNotEmpty()) {
                        StrengthProgressCard(stats.strengthImprovements, stats.averageStrengthImprovement)
                    }

                    // Personal Records (if any)
                    if (stats.totalPRs > 0) {
                        PersonalRecordsCard(stats.totalPRs)
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
                            text = "Save & Continue",
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgrammeStatItem(
                    label = "Workouts",
                    value = "${stats.completedWorkouts}/${stats.totalWorkouts}",
                )

                ProgrammeStatItem(
                    label = "Total Volume",
                    value = WeightFormatter.formatWeight(stats.totalVolume),
                    suffix = "kg",
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
private fun StrengthProgressCard(
    improvements: List<StrengthImprovement>,
    averageImprovement: Float,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "1RM Improvements",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Average improvement
            if (averageImprovement > 0) {
                Text(
                    text = "Average improvement: +${String.format("%.1f", averageImprovement)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Individual improvements
            improvements.take(5).forEach { improvement ->
                ImprovementItem(improvement)
                if (improvement != improvements.take(5).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (improvements.size > 5) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "...and ${improvements.size - 5} more exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImprovementItem(improvement: StrengthImprovement) {
    Column {
        Text(
            text = improvement.exerciseName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${WeightFormatter.formatWeightWithUnit(improvement.startingMax)} → ${
                    WeightFormatter.formatWeightWithUnit(
                        improvement.endingMax,
                    )
                }",
                style = MaterialTheme.typography.bodyLarge,
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "+${String.format("%.1f", improvement.improvementPercentage)}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordsCard(totalPRs: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )

            Column {
                Text(
                    text = "$totalPRs New Personal Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Crushing it! Keep up the great work!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgrammeInsightsCard(stats: com.github.radupana.featherweight.data.programme.ProgrammeCompletionStats) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Text(
                text = "Programme Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "${stats.insights.totalTrainingDays} total training days",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

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
                        text = "${String.format("%.1f", stats.insights.averageRestDaysBetweenWorkouts)} average rest days between workouts",
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
