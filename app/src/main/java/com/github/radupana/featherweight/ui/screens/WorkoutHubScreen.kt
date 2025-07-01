package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import com.github.radupana.featherweight.viewmodel.InProgressWorkout
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import com.github.radupana.featherweight.ui.screens.WorkoutSummary
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHubScreen(
    onStartActiveWorkout: () -> Unit,
    onStartTemplate: () -> Unit,
    workoutViewModel: WorkoutViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
) {
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    val workoutHistory by historyViewModel.workoutHistory.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var pendingWorkout by remember { mutableStateOf<InProgressWorkout?>(null) }

    // Load data when screen appears
    LaunchedEffect(Unit) {
        workoutViewModel.loadInProgressWorkouts()
        historyViewModel.loadWorkoutHistory()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Primary Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Start Training",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Button(
                    onClick = {
                        scope.launch {
                            if (workoutViewModel.hasInProgressWorkouts()) {
                                val recentWorkout = workoutViewModel.getMostRecentInProgressWorkout()
                                pendingWorkout = recentWorkout
                                showWorkoutDialog = true
                            } else {
                                workoutViewModel.startNewWorkout(forceNew = true)
                                onStartActiveWorkout()
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(6.dp),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Freestyle Workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedButton(
                    onClick = onStartTemplate,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                ) {
                    Icon(
                        Icons.Filled.LibraryBooks,
                        contentDescription = "Templates",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Templates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // In-Progress Workouts Section
        if (inProgressWorkouts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = "In Progress",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "In Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }

                    inProgressWorkouts.forEach { workout ->
                        WorkoutHubInProgressCard(
                            workout = workout,
                            onContinue = {
                                workoutViewModel.resumeWorkout(workout.id)
                                onStartActiveWorkout()
                            },
                        )
                        if (workout != inProgressWorkouts.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            workoutHistory.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = "No workouts",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No workouts yet",
                                style = MaterialTheme.typography.titleMedium,
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
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(workoutHistory.take(5)) { workout ->
                        // Show last 5 workouts
                        RecentWorkoutCard(
                            workout = workout,
                            onView = {
                                // Resume/view this specific workout
                                workoutViewModel.resumeWorkout(workout.id)
                                onStartActiveWorkout()
                            },
                            onDelete = { workoutId ->
                                historyViewModel.deleteWorkout(workoutId)
                            },
                        )
                    }

                    if (workoutHistory.size > 5) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "View all workouts in History tab",
                                        style = MaterialTheme.typography.bodyMedium,
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

    // Continue or Start New Workout Dialog
    if (showWorkoutDialog && pendingWorkout != null) {
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false },
            title = { Text("Continue Workout?") },
            text = {
                Column {
                    Text(
                        "You have an in-progress workout:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pendingWorkout!!.name
                            ?: "Started ${pendingWorkout!!.startDate.format(DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${pendingWorkout!!.exerciseCount} exercises • ${pendingWorkout!!.completedSets}/${pendingWorkout!!.setCount} sets completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Would you like to continue this workout or start a new one?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        workoutViewModel.resumeWorkout(pendingWorkout!!.id)
                        onStartActiveWorkout()
                        showWorkoutDialog = false
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        workoutViewModel.startNewWorkout(forceNew = true)
                        onStartActiveWorkout()
                        showWorkoutDialog = false
                    },
                ) {
                    Text("Start New")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentWorkoutCard(
    workout: WorkoutSummary,
    onView: () -> Unit,
    onDelete: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val (containerColor, statusColor, statusTextColor) =
        if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
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
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onView,
                    onLongClick = { showDeleteDialog = true },
                ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
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
                            if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color(0xFF5D4037)
                            },
                    )
                    Text(
                        text = workout.date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color(0xFF8D6E63)
                            },
                    )
                }

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED) "Completed" else "In Progress",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickStat(
                    label = "Exercises",
                    value = workout.exerciseCount.toString(),
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
                QuickStat(
                    label = "Sets",
                    value = workout.setCount.toString(),
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
                QuickStat(
                    label = "Volume",
                    value = "${String.format("%.1f", workout.totalWeight / 1000)}k kg",
                    isCompleted = workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED,
                    modifier = Modifier.weight(1f),
                )
            }
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
                        onDelete(workout.id)
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
fun QuickStat(
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
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color(0xFF6D4C41)
                },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color(0xFF8D6E63)
                },
        )
    }
}

@Composable
fun WorkoutHubInProgressCard(
    workout: InProgressWorkout,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            workout.name ?: workout.startDate.format(
                                DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${workout.exerciseCount} exercises • ${workout.completedSets}/${workout.setCount} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(onClick = onContinue) {
                    Text("Continue")
                }
            }
        }
    }
}
