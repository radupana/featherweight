package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.repository.WorkoutSummary
import com.github.radupana.featherweight.ui.components.WorkoutTimer
import com.github.radupana.featherweight.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.format.DateTimeFormatter

// WorkoutSummary now imported from repository

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onViewWorkout: (Long) -> Unit = {},
    onViewProgramme: (Long) -> Unit = {},
    historyViewModel: HistoryViewModel = viewModel(),
) {
    val historyState by historyViewModel.historyState.collectAsState()
    val isRefreshing by historyViewModel.isRefreshing.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

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
            modifier = Modifier.fillMaxSize(),
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Workouts") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Programmes") },
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 ->
                    WorkoutsHistorySection(
                        workouts = historyState.workouts,
                        isLoading = historyState.isLoading,
                        isLoadingMore = historyState.isLoadingMore,
                        hasMoreData = historyState.hasMoreData,
                        onViewWorkout = onViewWorkout,
                        onDeleteWorkout = { historyViewModel.deleteWorkout(it) },
                        onLoadMore = { historyViewModel.loadNextPage() },
                        modifier = Modifier.weight(1f),
                    )
                1 ->
                    ProgrammesHistorySection(
                        programmes = historyState.programmes,
                        isLoading = historyState.isLoading,
                        isLoadingMore = historyState.isLoadingMoreProgrammes,
                        hasMoreData = historyState.hasMoreProgrammes,
                        onViewProgramme = onViewProgramme,
                        onLoadMore = { historyViewModel.loadNextProgrammePage() },
                        modifier = Modifier.weight(1f),
                    )
            }

            // Error display
            historyState.error?.let { error ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

@Composable
fun WorkoutHistoryCard(
    workout: WorkoutSummary,
    onViewWorkout: (Long) -> Unit = {},
    onDeleteWorkout: (Long) -> Unit = {},
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Improved color scheme based on workout status
    val (containerColor, statusColor, statusTextColor, statusText) =
        when (workout.status) {
            com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> {
                Quadruple(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.tertiary,
                    "Completed",
                )
            }
            com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS -> {
                Quadruple(
                    Color(0xFFFFFBE6), // Very light yellow background for in-progress
                    Color(0xFFFFF3C4).copy(alpha = 0.8f), // Light yellow for status
                    Color(0xFF8B5A2B), // Warm brown for text
                    "In Progress",
                )
            }
            com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED -> {
                Quadruple(
                    Color(0xFFF5F5F5), // Light gray background for not started
                    Color(0xFFE0E0E0).copy(alpha = 0.8f), // Light gray for status
                    Color(0xFF616161), // Dark gray for text
                    "Not Started",
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
                        color =
                            when (workout.status) {
                                com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
                                com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS ->
                                    Color(
                                        0xFF5D4037,
                                    ) // Darker brown for in-progress
                                com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED ->
                                    Color(
                                        0xFF757575,
                                    ) // Gray for not started
                            },
                    )
                    Text(
                        text = workout.date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            when (workout.status) {
                                com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                                com.github.radupana.featherweight.data.WorkoutStatus.IN_PROGRESS ->
                                    Color(
                                        0xFF8D6E63,
                                    ) // Medium brown for in-progress
                                com.github.radupana.featherweight.data.WorkoutStatus.NOT_STARTED ->
                                    Color(
                                        0xFF9E9E9E,
                                    ) // Light gray for not started
                            },
                    )
                }

                // Notes indicator and timer
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Notes indicator
                    if (workout.hasNotes) {
                        Icon(
                            Icons.Filled.Notes,
                            contentDescription = "Has notes",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Timer for completed workouts
                    if (workout.status == com.github.radupana.featherweight.data.WorkoutStatus.COMPLETED && workout.duration != null) {
                        WorkoutTimer(
                            seconds = workout.duration.toInt(),
                            modifier = Modifier,
                        )
                    }
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

@Composable
fun WorkoutsHistorySection(
    workouts: List<WorkoutSummary>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMoreData: Boolean,
    onViewWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Trigger load more when scrolling near end
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex >= workouts.size - 5 && hasMoreData && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && workouts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            workouts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = "No workouts",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No completed workouts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Your completed workouts will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = workouts,
                        key = { _, workout -> workout.id },
                    ) { _, workout ->
                        WorkoutHistoryCard(
                            workout = workout,
                            onViewWorkout = onViewWorkout,
                            onDeleteWorkout = onDeleteWorkout,
                        )
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgrammesHistorySection(
    programmes: List<com.github.radupana.featherweight.repository.ProgrammeSummary>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMoreData: Boolean,
    onViewProgramme: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Trigger load more when scrolling near end
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex >= programmes.size - 5 && hasMoreData && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && programmes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            programmes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = "No programmes",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No completed programmes yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Your completed programmes will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = programmes,
                        key = { _, programme -> programme.id },
                    ) { _, programme ->
                        ProgrammeHistoryCard(
                            programme = programme,
                            onViewProgramme = onViewProgramme,
                        )
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgrammeHistoryCard(
    programme: com.github.radupana.featherweight.repository.ProgrammeSummary,
    onViewProgramme: (Long) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onViewProgramme(programme.id) },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with programme name and dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = programme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Completed ${programme.completedAt!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Completion indicator
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Completed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Programme stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgrammeStatItem(
                    label = "Duration",
                    value = "${programme.durationWeeks} weeks",
                    modifier = Modifier.weight(1f),
                )
                ProgrammeStatItem(
                    label = "Workouts",
                    value = programme.totalWorkouts.toString(),
                    modifier = Modifier.weight(1f),
                )
                ProgrammeStatItem(
                    label = "Completed",
                    value = programme.completedWorkouts.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            // Programme type and difficulty
            Spacer(modifier = Modifier.height(8.dp))
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
                            programme.programmeType.name
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
                            programme.difficulty.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun ProgrammeStatItem(
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
