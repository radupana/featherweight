package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
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
fun HistoryScreen(
    onViewWorkout: (Long) -> Unit = {},
    historyViewModel: HistoryViewModel = viewModel(),
) {
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
                        WorkoutHistoryCard(
                            workout = workout,
                            onViewWorkout = onViewWorkout,
                            onDeleteWorkout = { workoutId ->
                                historyViewModel.deleteWorkout(workoutId)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutHistoryCard(
    workout: WorkoutSummary,
    onViewWorkout: (Long) -> Unit = {},
    onDeleteWorkout: (Long) -> Unit = {},
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Improved color scheme: light yellow for in-progress, green for completed
    val (containerColor, statusColor, statusTextColor) =
        if (workout.isCompleted) {
            Triple(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.tertiary,
            )
        } else {
            Triple(
                Color(0xFFFFFBE6), // Very light yellow background for in-progress
                Color(0xFFFFF3C4).copy(alpha = 0.8f), // Light yellow for status
                Color(0xFF8B5A2B), // Warm brown for text
            )
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onViewWorkout(workout.id) },
                    onLongClick = { showDeleteDialog = true },
                ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
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
                        color =
                            if (workout.isCompleted) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color(0xFF5D4037) // Darker brown for in-progress
                            },
                    )
                    Text(
                        text = workout.date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (workout.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color(0xFF8D6E63) // Medium brown for in-progress
                            },
                    )
                }

                // Status indicator with improved colors
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (workout.isCompleted) "Completed" else "In Progress",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
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
                    isCompleted = workout.isCompleted,
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Sets",
                    value = workout.setCount.toString(),
                    isCompleted = workout.isCompleted,
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Total Volume",
                    value = "${String.format("%.1f", workout.totalWeight / 1000)}k kg",
                    isCompleted = workout.isCompleted,
                    modifier = Modifier.weight(1f),
                )
            }

            // Duration (if available)
            workout.duration?.let { duration ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration: $duration minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (workout.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color(0xFF8D6E63)
                        },
                )
            }

            // Long tap hint
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Long tap to delete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout") },
            text = {
                Text(
                    "Are you sure you want to delete this workout? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkout(workout.id)
                        showDeleteDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun WorkoutStatItem(
    label: String,
    value: String,
    isCompleted: Boolean,
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
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color(0xFF6D4C41) // Warm brown for in-progress values
                },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color(0xFF8D6E63) // Medium brown for in-progress labels
                },
        )
    }
}
