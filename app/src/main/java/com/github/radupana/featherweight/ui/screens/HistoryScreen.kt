package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

// Local data classes for HistoryScreen to avoid import issues
data class WorkoutSummary(
    val id: Long,
    val date: LocalDateTime,
    val name: String?,
    val exerciseCount: Int,
    val setCount: Int,
    val totalWeight: Float,
    val duration: Long?, // minutes
    val status: com.github.radupana.featherweight.data.WorkoutStatus,
    val prCount: Int = 0, // Number of PRs achieved in this workout
)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onViewWorkout: (Long) -> Unit = {},
    historyViewModel: HistoryViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val historyState by historyViewModel.historyState.collectAsState()
    val isRefreshing by historyViewModel.isRefreshing.collectAsState()
    val lazyListState = rememberLazyListState()

    // Infinite scroll handling
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && historyViewModel.shouldLoadMore(lastVisibleIndex)) {
                    historyViewModel.loadNextPage()
                }
            }
    }

    // Error handling
    historyState.error?.let { error ->
        LaunchedEffect(error) {
            // Clear error after showing it
            historyViewModel.clearError()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { historyViewModel.refreshHistory() },
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            when {
                historyState.isLoading && historyState.workouts.isEmpty() -> {
                    // Initial loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading workout history...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                historyState.workouts.isEmpty() && !historyState.isLoading -> {
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
                    // Workout list with pagination
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(historyState.workouts) { index, workout ->
                            WorkoutHistoryCard(
                                workout = workout,
                                onViewWorkout = onViewWorkout,
                                onDeleteWorkout = { workoutId ->
                                    historyViewModel.deleteWorkout(workoutId)
                                },
                            )
                        }

                        // Loading more indicator
                        if (historyState.isLoadingMore) {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading more workouts...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // End of data indicator
                        if (!historyState.hasMoreData && historyState.workouts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "You've reached the end",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            historyState.error?.let { error ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { historyViewModel.refreshHistory() },
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Retry",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
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
    // Improved color scheme based on workout status
    val (containerColor, statusColor, statusTextColor, statusText) = when (workout.status) {
        com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> {
            Quadruple(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.tertiary,
                "Completed"
            )
        }
        com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS -> {
            Quadruple(
                Color(0xFFFFFBE6), // Very light yellow background for in-progress
                Color(0xFFFFF3C4).copy(alpha = 0.8f), // Light yellow for status
                Color(0xFF8B5A2B), // Warm brown for text
                "In Progress"
            )
        }
        com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED -> {
            Quadruple(
                Color(0xFFF5F5F5), // Light gray background for not started
                Color(0xFFE0E0E0).copy(alpha = 0.8f), // Light gray for status
                Color(0xFF616161), // Dark gray for text
                "Not Started"
            )
        }
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
                        color = when (workout.status) {
                            com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
                            com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS -> Color(0xFF5D4037) // Darker brown for in-progress
                            com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED -> Color(0xFF757575) // Gray for not started
                        },
                    )
                    Text(
                        text = workout.date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (workout.status) {
                            com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                            com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS -> Color(0xFF8D6E63) // Medium brown for in-progress
                            com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED -> Color(0xFF9E9E9E) // Light gray for not started
                        },
                    )
                }

                // Status indicator with improved colors
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = statusText,
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
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Sets",
                    value = workout.setCount.toString(),
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
                WorkoutStatItem(
                    label = "Total Volume",
                    value = "${String.format("%.1f", workout.totalWeight / 1000)}k kg",
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
            }

            // PR Badge (if any PRs were achieved)
            if (workout.prCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.2f), // Gold background
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🏆",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (workout.prCount == 1) "1 Personal Record" else "${workout.prCount} Personal Records",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB8860B) // Dark goldenrod
                        )
                    }
                }
            }

            // Duration (if available)
            workout.duration?.let { duration ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration: $duration minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
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
