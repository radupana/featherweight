package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.github.radupana.featherweight.service.StallDetectionService
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

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
    var selectedTab by remember { mutableStateOf(0) }

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
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Performance") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Records") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Insights") }
                        )
                    }
                    
                    // Tab Content
                    when (selectedTab) {
                        0 -> PerformanceTab(successState.data, chartData, viewModel)
                        1 -> RecordsTab(successState.data, viewModel)
                        2 -> InsightsTab(successState.data, viewModel)
                    }
                }
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
private fun PerformanceTab(
    data: ExerciseProgressViewModel.ExerciseProgressData?,
    chartData: List<ExerciseDataPoint>,
    viewModel: ExerciseProgressViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            // Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Performance Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "Current Max",
                            value = "${data.currentMax}kg",
                            icon = Icons.Default.TrendingUp
                        )
                        MetricItem(
                            label = "All-Time PR",
                            value = "${data.allTimePR}kg",
                            icon = Icons.Default.Star,
                            tint = Color(0xFFFFD700)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "Monthly Progress",
                            value = "${if (data.monthlyProgress >= 0) "+" else ""}${String.format("%.1f", data.monthlyProgress)}%",
                            icon = if (data.monthlyProgress >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            tint = if (data.monthlyProgress >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        MetricItem(
                            label = "Weekly Frequency",
                            value = "${String.format("%.1f", data.weeklyFrequency)}x",
                            icon = Icons.Default.CalendarMonth
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
            
            // Volume Stats
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Volume Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Sessions",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${data.totalSessions}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last 30 Days Volume",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${data.totalVolume.toInt()}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Avg Session Volume",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${data.avgSessionVolume.toInt()}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsTab(
    data: ExerciseProgressViewModel.ExerciseProgressData?,
    viewModel: ExerciseProgressViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    text = "No records available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // PR Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = "Personal Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "${data.allTimePR}kg",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    data.lastPerformed?.let { date ->
                        Text(
                            text = "Last performed: ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Best Sets Timeline (placeholder)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Best Sets Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Coming soon...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsTab(
    data: ExerciseProgressViewModel.ExerciseProgressData?,
    viewModel: ExerciseProgressViewModel
) {
    val scope = rememberCoroutineScope()
    var stallAnalysis by remember { mutableStateOf<StallDetectionService.StallAnalysis?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    text = "No insights available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Current Trend Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (data.currentTrend) {
                        "IMPROVING" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "STALLING" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (data.currentTrend) {
                            "IMPROVING" -> Icons.Default.TrendingUp
                            "STALLING" -> Icons.Default.Remove
                            else -> Icons.Default.TrendingDown
                        },
                        contentDescription = null,
                        tint = when (data.currentTrend) {
                            "IMPROVING" -> Color(0xFF4CAF50)
                            "STALLING" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    
                    Column {
                        Text(
                            text = "Current Trend",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = when (data.currentTrend) {
                                "IMPROVING" -> "Steady Progress"
                                "STALLING" -> "Plateau Detected"
                                else -> "Performance Declining"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Stall Detection Card (placeholder for now)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Training Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    when (data.currentTrend) {
                        "IMPROVING" -> {
                            Text(
                                text = "Keep up the great work! Your current training approach is working well.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        "STALLING" -> {
                            Text(
                                text = "Consider implementing one of these strategies:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "• Take a deload week (reduce weight by 10-15%)",
                                "• Switch to a different rep range temporarily",
                                "• Add exercise variations for new stimulus",
                                "• Ensure adequate recovery between sessions"
                            ).forEach { tip ->
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "Review your training frequency and recovery. Consider:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "• Reducing training frequency for this exercise",
                                "• Focusing on form with lighter weights",
                                "• Improving sleep and nutrition",
                                "• Taking a full rest week if needed"
                            ).forEach { tip ->
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
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
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
