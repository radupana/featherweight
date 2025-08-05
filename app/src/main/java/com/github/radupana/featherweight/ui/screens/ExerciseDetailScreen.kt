package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.service.ExerciseProgressData
import com.github.radupana.featherweight.service.PerformanceStats
import com.github.radupana.featherweight.ui.components.ChartData
import com.github.radupana.featherweight.ui.components.ChartType
import com.github.radupana.featherweight.ui.components.ProgressChart
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var exerciseData by remember { mutableStateOf<ExerciseProgressData?>(null) }
    var performanceStats by remember { mutableStateOf<PerformanceStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(exerciseName) {
        // TODO: Load data from repository
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { paddingValues ->
        if (isLoading) {
            LoadingState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
            )
        } else if (exerciseData == null) {
            EmptyState(
                exerciseName = exerciseName,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    // Weight progression chart
                    ProgressChart(
                        data =
                            ChartData(
                                dataPoints = exerciseData!!.dataPoints,
                                title = "Weight Progression",
                                primaryColor = MaterialTheme.colorScheme.primary,
                                showPRmarkers = true,
                            ),
                        type = ChartType.LINE,
                        height = 200.dp,
                    )
                }

                item {
                    // Volume chart
                    ProgressChart(
                        data =
                            ChartData(
                                dataPoints = exerciseData!!.dataPoints,
                                title = "Volume Over Time",
                                primaryColor = Color(0xFF4CAF50),
                                showPRmarkers = false,
                            ),
                        type = ChartType.BAR,
                        height = 180.dp,
                    )
                }

                item {
                    // RPE trend chart (if RPE data available)
                    val hasRpeData = exerciseData!!.dataPoints.any { it.rpe != null }
                    if (hasRpeData) {
                        ProgressChart(
                            data =
                                ChartData(
                                    dataPoints = exerciseData!!.dataPoints.filter { it.rpe != null },
                                    title = "RPE Trend",
                                    primaryColor = Color(0xFFFF9800),
                                    showPRmarkers = false,
                                ),
                            type = ChartType.AREA,
                            height = 160.dp,
                        )
                    }
                }

                item {
                    // Performance statistics
                    performanceStats?.let { stats ->
                        PerformanceStatsCard(stats = stats)
                    }
                }

                item {
                    // PR History
                    PRHistoryCard(exerciseData = exerciseData!!)
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (it == 0) 200.dp else 120.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    exerciseName: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Complete some workouts with $exerciseName to see progress charts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PerformanceStatsCard(
    stats: PerformanceStats,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Performance Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatRow(
                    label = "Best Single",
                    value = stats.bestSingle ?: "No data",
                )

                StatRow(
                    label = "Best Volume",
                    value = stats.bestVolume ?: "No data",
                )

                StatRow(
                    label = "Average RPE",
                    value = stats.averageRpe?.let { String.format("%.1f", it) } ?: "No data",
                )

                StatRow(
                    label = "Consistency",
                    value = "${(stats.consistency * 100).toInt()}%",
                )

                StatRow(
                    label = "Total Sessions",
                    value = stats.totalSessions.toString(),
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PRHistoryCard(
    exerciseData: ExerciseProgressData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Recent Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val sortedData = exerciseData.dataPoints.sortedByDescending { it.date }

            if (sortedData.isEmpty()) {
                Text(
                    text = "No workout data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sortedData.take(5).forEach { dataPoint ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = dataPoint.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )

                                Text(
                                    text = "${dataPoint.weight}kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Vol: ${dataPoint.volume.toInt()}kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                dataPoint.rpe?.let { rpe ->
                                    Text(
                                        text = " • RPE: ${String.format("%.1f", rpe)}",
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
