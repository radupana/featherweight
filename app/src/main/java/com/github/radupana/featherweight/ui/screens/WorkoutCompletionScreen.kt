package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutCompletionViewModel
import com.github.radupana.featherweight.viewmodel.CompletionSummary
import com.github.radupana.featherweight.viewmodel.SetInfo
import com.github.radupana.featherweight.viewmodel.ExerciseVolume
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun WorkoutCompletionScreen(
    workoutId: Long,
    onDismiss: () -> Unit,
) {
    // Get repository from application context
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { 
        com.github.radupana.featherweight.repository.FeatherweightRepository(context.applicationContext as android.app.Application) 
    }
    val viewModel: WorkoutCompletionViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WorkoutCompletionViewModel(repository) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(workoutId) {
        viewModel.loadWorkoutSummary(workoutId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.workoutSummary != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(500, easing = EaseOutBack)
            ) + fadeIn(),
            exit = fadeOut()
        ) {
            uiState.workoutSummary?.let { summary ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Primary Stats Card
                    PrimaryStatsCard(summary)
                    
                    // Workout Stats Card
                    WorkoutStatsCard(summary)
                    
                    // Personal Records (if any)
                    if (summary.personalRecords.isNotEmpty()) {
                        PersonalRecordsCard(summary.personalRecords)
                    }
                    
                    // Workout Insights
                    WorkoutInsightsCard(summary)
                    
                    // Motivational Message
                    MotivationalMessageCard(summary)
                    
                    // Continue Button with padding for system bar
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Workout Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Total Volume",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "🏋️ ${WeightFormatter.formatWeight(summary.totalVolume)} kg",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Duration: ${formatDuration(summary.duration)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutStatsCard(summary: CompletionSummary) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = summary.setsCompleted.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = summary.totalReps.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = summary.exerciseCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun PersonalRecordsCard(personalRecords: List<PersonalRecord>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🏆",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "New Personal Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            personalRecords.forEach { pr ->
                PRItem(pr)
                if (pr != personalRecords.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PRItem(pr: PersonalRecord) {
    Column {
        Text(
            text = pr.exerciseName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "${WeightFormatter.formatWeightWithUnit(pr.weight)} × ${pr.reps} reps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (pr.previousWeight != null && pr.previousDate != null) {
            Text(
                text = "Previous: ${WeightFormatter.formatWeightWithUnit(pr.previousWeight)} × ${pr.previousReps ?: 0} (${
                    pr.previousDate.format(DateTimeFormatter.ofPattern("MMM d"))
                })",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutInsightsCard(summary: CompletionSummary) {
    val insights = buildList {
        // Average RPE insight
        summary.averageRpe?.let { rpe ->
            val message = when {
                rpe < 6 -> "Light session - perfect for recovery"
                rpe < 7 -> "Moderate intensity - well balanced"
                rpe < 8 -> "You pushed hard today! 💪"
                else -> "Maximum effort! Time to recover"
            }
            add(InsightItem(
                title = "Average RPE: ${String.format("%.1f", rpe)}",
                description = message
            ))
        }
        
        // Average intensity insight
        summary.averageIntensity?.let { intensity ->
            add(InsightItem(
                title = "Average Intensity",
                description = "${intensity.toInt()}% of 1RM"
            ))
        }
    }
    
    if (insights.isNotEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Workout Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                insights.forEachIndexed { index, insight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val messages = listOf(
        "Great work! Every session counts towards your goals. Rest well and come back stronger! 💪",
        "Another workout complete. Consistency builds strength. Keep up the momentum!",
        "Well done! Rest, recover, and come back stronger. You're making progress!",
        "Session complete. You're one step closer to your goals. Stay consistent!",
        "Solid training today. Keep up the momentum! Every workout matters."
    )
    
    val contextMessage = when {
        summary.personalRecords.size > 1 -> "Incredible session! ${summary.personalRecords.size} new personal records!"
        summary.personalRecords.size == 1 -> "Personal record achieved! You're getting stronger!"
        summary.setsCompleted == summary.totalSets -> "Perfect execution - every set completed!"
        summary.averageRpe?.let { it > 8 } == true -> "Maximum effort today! Make sure to recover well."
        else -> messages[Random.nextInt(messages.size)]
    }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = contextMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
    }
}

@Composable
private fun AnimatedNumber(
    targetValue: Float,
    suffix: String = "",
    content: @Composable (Float) -> Unit
) {
    var animatedValue by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(targetValue) {
        animate(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = tween(1000, easing = EaseOutCubic)
        ) { value, _ ->
            animatedValue = value
        }
    }
    
    content(animatedValue)
}

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
    val description: String
)