package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.viewmodel.HistoryViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Local data class for HistoryScreen to avoid import issues
data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?, // minutes
    val isCompleted: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel()) {
    val workoutHistory by historyViewModel.workoutHistory.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()

    // Load history on first composition
    LaunchedEffect(Unit) {
        historyViewModel.loadWorkoutHistory()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Workout History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            workoutHistory.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = "No workouts",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No workouts yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start your first workout to see it here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                // Workout list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(workoutHistory) { workout ->
                        WorkoutHistoryCard(workout = workout)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryCard(workout: WorkoutSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (workout.isCompleted) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with date and workout name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            workout.name ?: workout.date.format(
                                DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = workout.date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Status indicator
                Surface(
                    color =
                        if (workout.isCompleted) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (workout.isCompleted) "Completed" else "In Progress",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (workout.isCompleted) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WorkoutStatItem(
                    label = "Exercises",
                    value = workout.exerciseCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Sets",
                    value = workout.setCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Total Volume",
                    value = "${String.format("%.1f", workout.totalWeight / 1000)}k lbs",
                    modifier = Modifier.weight(1f),
                )
            }

            // Duration (if available)
            workout.duration?.let { duration ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration: $duration minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun WorkoutStatItem(
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
