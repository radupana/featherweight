package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.viewmodel.ExerciseProgressViewModel
import com.github.radupana.featherweight.ui.components.ExerciseProgressChart
import com.github.radupana.featherweight.ui.components.ExerciseDataPoint
import com.github.radupana.featherweight.util.WeightFormatter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    exerciseName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseProgressViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(exerciseName) {
        viewModel.loadExerciseData(exerciseName)
        viewModel.loadChartData(exerciseName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseName,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {
            is ExerciseProgressViewModel.ExerciseProgressState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ExerciseProgressViewModel.ExerciseProgressState.Success -> {
                val successState = state as ExerciseProgressViewModel.ExerciseProgressState.Success
                ExerciseInsightsContent(
                    data = successState.data,
                    chartData = chartData,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is ExerciseProgressViewModel.ExerciseProgressState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading exercise data",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { scope.launch { viewModel.loadExerciseData(exerciseName) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseInsightsContent(
    data: ExerciseProgressViewModel.ExerciseProgressData?,
    chartData: List<ExerciseDataPoint>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (data == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No data available for this exercise",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Last Performed info
            data.lastPerformed?.let { lastDate ->
                val daysSince = ChronoUnit.DAYS.between(lastDate, java.time.LocalDate.now())
                Text(
                    text = when {
                        daysSince == 0L -> "Last performed: Today"
                        daysSince == 1L -> "Last performed: Yesterday"
                        daysSince < 7 -> "Last performed: $daysSince days ago"
                        else -> "Last performed: ${lastDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // 2x2 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // All-Time PR Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                text = "All-Time PR",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = if (data.allTimePR > 0) {
                                WeightFormatter.formatWeightWithUnit(data.allTimePR)
                            } else "No PR yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        data.allTimePRDate?.let { date ->
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Recent Best Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Recent Best",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = if (data.recentBest > 0) {
                                WeightFormatter.formatWeightWithUnit(data.recentBest)
                            } else "No recent lifts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (data.recentBest > 0) {
                            if (data.allTimePR > 0) {
                                Text(
                                    text = "${data.recentBestPercentOfPR}% of PR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            // Add date when recent best was achieved
                            val recentBestDate = data.recentBestDate
                            if (recentBestDate != null) {
                                val daysSince = ChronoUnit.DAYS.between(recentBestDate, java.time.LocalDate.now())
                                Text(
                                    text = when {
                                        daysSince == 0L -> "Today"
                                        daysSince == 1L -> "Yesterday"
                                        daysSince < 7 -> "$daysSince days ago"
                                        else -> recentBestDate.format(DateTimeFormatter.ofPattern("MMM d"))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frequency Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Frequency",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "${WeightFormatter.formatDecimal(data.weeklyFrequency, 1)}x/week",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "8 week avg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = when (data.progressStatus) {
                            ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                            ExerciseProgressViewModel.ProgressStatus.PLATEAU -> Color(0xFFFF9800).copy(alpha = 0.2f)
                            ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> Color(0xFFFF5722).copy(alpha = 0.2f)
                            ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = when (data.progressStatus) {
                                    ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> Color(0xFF4CAF50)
                                    ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> MaterialTheme.colorScheme.primary
                                    ExerciseProgressViewModel.ProgressStatus.PLATEAU -> Color(0xFFFF9800)
                                    ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> Color(0xFFFF5722)
                                    ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = when (data.progressStatus) {
                                ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> "Making Gains"
                                ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> "Steady Progress"
                                ExerciseProgressViewModel.ProgressStatus.PLATEAU -> "Plateau"
                                ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> "Extended Break"
                                ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> "Working Lighter"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = data.progressStatusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Progress Chart
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Weight Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    ExerciseProgressChart(
                        dataPoints = chartData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Plateau Intervention Card (only if plateau detected)
            if (data.progressStatus == ExerciseProgressViewModel.ProgressStatus.PLATEAU && data.plateauWeeks >= 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "Plateau Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = data.progressStatusDetail,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* TODO: Implement deload suggestion */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Take Deload Week")
                            }
                            OutlinedButton(
                                onClick = { /* TODO: Implement rep range suggestion */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Switch Rep Range")
                            }
                        }
                    }
                }
            }
        }
    }
}
