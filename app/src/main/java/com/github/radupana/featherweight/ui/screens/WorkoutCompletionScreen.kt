package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.CompletionSummary
import com.github.radupana.featherweight.viewmodel.WorkoutCompletionViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

@Composable
fun WorkoutCompletionScreen(
    workoutId: String,
    onDismiss: () -> Unit,
    onSaveAsTemplate: (String) -> Unit = {},
) {
    // Get repository from application context
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository =
        remember {
            com.github.radupana.featherweight.repository
                .FeatherweightRepository(context.applicationContext as android.app.Application)
        }
    val viewModel: WorkoutCompletionViewModel =
        viewModel(
            factory =
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return WorkoutCompletionViewModel(repository) as T
                    }
                },
        )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workoutId) {
        viewModel.loadWorkoutSummary(workoutId)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.workoutSummary != null,
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = EaseOutCubic),
                ) + fadeIn(),
            exit = fadeOut(),
        ) {
            uiState.workoutSummary?.let { summary ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Primary Stats Card
                    PrimaryStatsCard(summary)

                    // Workout Stats Card
                    WorkoutStatsCard(summary)

                    // Personal Records (if any)
                    if (summary.personalRecords.isNotEmpty()) {
                        val exerciseNames by viewModel.exerciseNames.collectAsState()
                        PersonalRecordsCard(summary.personalRecords, repository, exerciseNames)
                    }

                    // Workout Insights
                    WorkoutInsightsCard(summary)

                    // Motivational Message
                    MotivationalMessageCard(summary)

                    // Save as Template Button
                    Button(
                        onClick = { onSaveAsTemplate(workoutId) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            ),
                    ) {
                        Text(
                            text = "Save as Template",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue Button with padding for system bar
                    Button(
                        onClick = onDismiss,
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
                            text = "Done",
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
    }
}

@Composable
private fun PrimaryStatsCard(summary: CompletionSummary) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Workout Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Total Volume",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "🏋️ ${WeightFormatter.formatWeightWithUnit(summary.totalVolume)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Duration: ${formatDuration(summary.duration)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutStatsCard(summary: CompletionSummary) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = summary.setsCompleted.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = summary.totalReps.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = summary.exerciseCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordsCard(
    personalRecords: List<PersonalRecord>,
    repository: com.github.radupana.featherweight.repository.FeatherweightRepository,
    exerciseNames: Map<String, String>,
) {
    GlassmorphicCard(
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
                Text(
                    text = "🏆",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "New Personal Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            personalRecords.forEach { pr ->
                PRItem(pr, repository, exerciseNames)
                if (pr != personalRecords.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PRItem(
    pr: PersonalRecord,
    repository: com.github.radupana.featherweight.repository.FeatherweightRepository,
    exerciseNames: Map<String, String>,
) {
    Column {
        // Use unified key to look up exercise name
        val key = "exercise_${pr.exerciseVariationId}"
        val exerciseName = exerciseNames[key] ?: "Unknown Exercise"
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "${WeightFormatter.formatWeightWithUnit(pr.weight)} × ${pr.reps} reps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        if (pr.previousWeight != null && pr.previousDate != null) {
            Text(
                text = "Previous: ${WeightFormatter.formatWeightWithUnit(pr.previousWeight)} × ${pr.previousReps ?: 0} (${
                    pr.previousDate.format(DateTimeFormatter.ofPattern("MMM d"))
                })",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutInsightsCard(summary: CompletionSummary) {
    val insights =
        buildList {
            // Average RPE insight
            summary.averageRpe?.let { rpe ->
                val message =
                    when {
                        rpe < 6 -> "Light session - perfect for recovery"
                        rpe < 7 -> "Moderate intensity - well balanced"
                        rpe < 8 -> "You pushed hard today! 💪"
                        else -> "Maximum effort! Time to recover"
                    }
                add(
                    InsightItem(
                        title = "Average RPE: ${String.format(Locale.US, "%.1f", rpe)}",
                        description = message,
                    ),
                )
            }

            // Average intensity insight
            summary.averageIntensity?.let { intensity ->
                add(
                    InsightItem(
                        title = "Average Intensity",
                        description = "${intensity.toInt()}% of 1RM",
                    ),
                )
            }
        }

    if (insights.isNotEmpty()) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
            ) {
                Text(
                    text = "Workout Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                insights.forEachIndexed { index, insight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (index < insights.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MotivationalMessageCard(summary: CompletionSummary) {
    val messages =
        listOf(
            "Great work! Every session counts towards your goals. Rest well and come back stronger! 💪",
            "Another workout complete. Consistency builds strength. Keep up the momentum!",
            "Well done! Rest, recover, and come back stronger. You're making progress!",
            "Session complete. You're one step closer to your goals. Stay consistent!",
            "Solid training today. Keep up the momentum! Every workout matters.",
        )

    val contextMessage =
        when {
            summary.personalRecords.size > 1 -> "Incredible session! ${summary.personalRecords.size} new personal records!"
            summary.personalRecords.size == 1 -> "Personal record achieved! You're getting stronger!"
            summary.setsCompleted == summary.totalSets -> "Perfect execution - every set completed!"
            summary.averageRpe?.let { it > 8 } == true -> "Maximum effort today! Make sure to recover well."
            else -> messages[Random.nextInt(messages.size)]
        }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = contextMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        )
    }
}

@Composable
private fun formatDuration(duration: java.time.Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private data class InsightItem(
    val title: String,
    val description: String,
)
