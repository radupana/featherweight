package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.radupana.featherweight.ui.components.TrainingAnalysisCard
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.InsightsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToExercise: (String) -> Unit = {},
) {
    var recentPRs by remember { mutableStateOf<List<com.github.radupana.featherweight.data.PersonalRecord>>(emptyList()) }
    var weeklyWorkoutCount by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var isDataInitialized by remember { mutableStateOf(false) }

    val trainingAnalysis by viewModel.trainingAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val currentWorkoutCount by viewModel.currentWorkoutCount.collectAsStateWithLifecycle()
    val exerciseNames by viewModel.exerciseNames.collectAsStateWithLifecycle()

    // Load highlights data and check for scheduled analysis
    LaunchedEffect(Unit) {
        viewModel.loadHighlightsData { prs, workoutCount, streak ->
            recentPRs = prs
            weeklyWorkoutCount = workoutCount
            currentStreak = streak
            isDataInitialized = true
        }

        // Load cached analysis and check if we need to run a new one
        viewModel.loadCachedAnalysis()
        viewModel.checkAndRunScheduledAnalysis()
    }

    // Don't render anything until data is loaded
    if (!isDataInitialized) {
        return
    }

    // Main scrollable layout
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
    ) {
        // Highlights section
        item {
            val prExerciseNamesMap =
                remember(recentPRs, exerciseNames) {
                    recentPRs.take(3).associateWith { pr ->
                        exerciseNames[pr.exerciseId] ?: "Unknown Exercise"
                    }
                }

            HighlightsSection(
                recentPRs = recentPRs,
                prExerciseNames = prExerciseNamesMap,
                weeklyWorkoutCount = weeklyWorkoutCount,
                currentStreak = currentStreak,
            )
        }

        // Training Analysis section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TrainingAnalysisCard(
                analysis = trainingAnalysis,
                isLoading = isAnalyzing,
                currentWorkoutCount = currentWorkoutCount,
            )
        }

        // Spacer between sections
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Exercise Progress section with fixed height
        item {
            ExerciseProgressSection(
                viewModel = viewModel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(500.dp), // Fixed height for the entire section
                onNavigateToExercise = onNavigateToExercise,
            )
        }
    }
}

@Composable
private fun HighlightsSection(
    recentPRs: List<com.github.radupana.featherweight.data.PersonalRecord>,
    prExerciseNames: Map<com.github.radupana.featherweight.data.PersonalRecord, String>,
    weeklyWorkoutCount: Int,
    currentStreak: Int,
) {
    Column {
        Text(
            text = "Highlights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Recent PRs
        if (recentPRs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recent PRs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    recentPRs.take(3).forEach { pr ->
                        val exerciseName = prExerciseNames[pr] ?: "Unknown Exercise"
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${
                                    WeightFormatter.formatWeightWithUnit(
                                        pr.weight,
                                    )
                                } (${formatRelativeDate(pr.recordDate.toLocalDate())})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Workouts this week
            Card(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(140.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$weeklyWorkoutCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (weeklyWorkoutCount == 1) "Workout" else "Workouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "this Week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Current streak
            if (currentStreak >= 1) {
                Card(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(140.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Week Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Empty placeholder to maintain consistent layout
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// Utility function to format relative dates
private fun formatRelativeDate(date: java.time.LocalDate): String {
    val now = java.time.LocalDate.now()
    val days =
        java.time.temporal.ChronoUnit.DAYS
            .between(date, now)

    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        else ->
            date.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("MMM d"),
            )
    }
}

@Composable
private fun ExerciseProgressCard(
    exercise: com.github.radupana.featherweight.service.ExerciseSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = WeightFormatter.formatWeightWithUnit(exercise.currentMax),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "${exercise.sessionCount} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            com.github.radupana.featherweight.ui.components.MiniProgressChart(
                data = exercise.miniChartData,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp, 50.dp),
            )
        }
    }
}

@Composable
private fun EmptyExercisesState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Exercise Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Complete some workouts to see your progress here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExerciseProgressSection(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToExercise: (String) -> Unit,
) {
    Column(modifier = modifier) {
        // Header
        Text(
            text = "Exercise Progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Fixed height scrollable container
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            // Use remaining height
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            GroupedExerciseList(
                viewModel = viewModel,
                onNavigateToExercise = onNavigateToExercise,
            )
        }
    }
}

@Composable
private fun GroupedExerciseList(
    viewModel: InsightsViewModel,
    onNavigateToExercise: (String) -> Unit,
) {
    val state = rememberGroupedExerciseListState(viewModel)

    if (!state.isDataInitialized) {
        return
    }

    val isEmpty =
        state.groupedExercises == null ||
            (
                state.groupedExercises?.bigFourExercises.isNullOrEmpty() &&
                    state.groupedExercises?.otherExercises.isNullOrEmpty()
            )

    if (isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyExercisesState()
        }
    } else {
        GroupedExerciseLazyColumn(
            state = state,
            onNavigateToExercise = onNavigateToExercise,
        )
    }
}

@Composable
private fun rememberGroupedExerciseListState(
    viewModel: InsightsViewModel,
): GroupedExerciseListState {
    val listState = rememberLazyListState()
    val state = remember { GroupedExerciseListState(listState) }

    // Initial load
    LaunchedEffect(Unit) {
        state.loadInitialData(viewModel)
    }

    // Infinite scroll
    LaunchedEffect(listState) {
        state.setupInfiniteScroll()
    }

    return state
}

class GroupedExerciseListState(
    val listState: LazyListState,
    private val pageSize: Int = 10,
) {
    var groupedExercises by mutableStateOf<com.github.radupana.featherweight.service.GroupedExerciseSummary?>(null)
    var displayedOtherExercises by mutableStateOf<List<com.github.radupana.featherweight.service.ExerciseSummary>>(emptyList())
    var isLoadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var currentPage by mutableStateOf(0)
    var isDataInitialized by mutableStateOf(false)

    suspend fun loadInitialData(viewModel: InsightsViewModel) {
        groupedExercises = viewModel.getGroupedExercisesSummary()
        groupedExercises?.let {
            displayedOtherExercises = it.otherExercises.take(pageSize)
            hasMore = it.otherExercises.size > pageSize
        }
        isDataInitialized = true
    }

    suspend fun setupInfiniteScroll() {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= totalItems - 3 && totalItems > 0
        }.distinctUntilChanged()
            .filter { it && hasMore && !isLoadingMore }
            .collect { loadNextPage() }
    }

    private suspend fun loadNextPage() {
        isLoadingMore = true
        kotlinx.coroutines.delay(300)

        groupedExercises?.let {
            val nextPage = currentPage + 1
            val startIndex = nextPage * pageSize
            val endIndex = minOf(startIndex + pageSize, it.otherExercises.size)

            if (startIndex < it.otherExercises.size) {
                val newExercises = it.otherExercises.subList(startIndex, endIndex)
                displayedOtherExercises = displayedOtherExercises + newExercises
                currentPage = nextPage
                hasMore = endIndex < it.otherExercises.size
            } else {
                hasMore = false
            }
        }

        isLoadingMore = false
    }
}

@Composable
private fun GroupedExerciseLazyColumn(
    state: GroupedExerciseListState,
    onNavigateToExercise: (String) -> Unit,
) {
    LazyColumn(
        state = state.listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        bigFourSection(
            exercises = state.groupedExercises?.bigFourExercises,
            onNavigateToExercise = onNavigateToExercise,
        )

        othersSection(
            exercises = state.displayedOtherExercises,
            hasBigFour = state.groupedExercises?.bigFourExercises?.isNotEmpty() == true,
            onNavigateToExercise = onNavigateToExercise,
        )

        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        if (!state.hasMore && state.displayedOtherExercises.isNotEmpty()) {
            item {
                Text(
                    "All exercises loaded",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun LazyListScope.bigFourSection(
    exercises: List<com.github.radupana.featherweight.service.ExerciseSummary>?,
    onNavigateToExercise: (String) -> Unit,
) {
    if (exercises?.isNotEmpty() == true) {
        item {
            Text(
                text = "Big Four",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(exercises) { exercise ->
            ExerciseProgressCard(
                exercise = exercise,
                onClick = { onNavigateToExercise(exercise.exerciseName) },
            )
        }
    }
}

fun LazyListScope.othersSection(
    exercises: List<com.github.radupana.featherweight.service.ExerciseSummary>,
    hasBigFour: Boolean,
    onNavigateToExercise: (String) -> Unit,
) {
    if (exercises.isNotEmpty()) {
        item {
            Text(
                text = "Others",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.padding(
                        top = if (hasBigFour) 16.dp else 0.dp,
                        bottom = 8.dp,
                    ),
            )
        }

        items(exercises) { exercise ->
            ExerciseProgressCard(
                exercise = exercise,
                onClick = { onNavigateToExercise(exercise.exerciseName) },
            )
        }
    }
}
