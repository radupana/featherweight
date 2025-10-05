package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.ui.components.ExerciseDataPoint
import com.github.radupana.featherweight.ui.components.ExerciseProgressChart
import com.github.radupana.featherweight.ui.components.FrequencyDataPoint
import com.github.radupana.featherweight.ui.components.IntensityZoneChart
import com.github.radupana.featherweight.ui.components.IntensityZoneData
import com.github.radupana.featherweight.ui.components.RepRangeChart
import com.github.radupana.featherweight.ui.components.RepRangeDistribution
import com.github.radupana.featherweight.ui.components.TrainingPatternsChart
import com.github.radupana.featherweight.ui.components.VolumeBarChart
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.ExerciseProgressViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ExerciseChartData(
    val chartData: List<ExerciseDataPoint>,
    val maxWeightChartData: List<ExerciseDataPoint>,
    val volumeChartData: List<ExerciseDataPoint>,
    val selectedChartType: ExerciseProgressViewModel.ChartType,
    val onChartTypeChanged: (ExerciseProgressViewModel.ChartType) -> Unit,
)

data class ExercisePatternData(
    val frequencyData: List<FrequencyDataPoint>,
    val repRangeData: List<RepRangeDistribution>,
    val rpeZoneData: List<IntensityZoneData>,
    val selectedPatternType: ExerciseProgressViewModel.PatternType,
    val onPatternTypeChanged: (ExerciseProgressViewModel.PatternType) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseProgressScreen(
    exerciseName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseProgressViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val maxWeightChartData by viewModel.maxWeightChartData.collectAsState()
    val volumeChartData by viewModel.volumeChartData.collectAsState()
    val selectedChartType by viewModel.selectedChartType.collectAsState()
    val frequencyData by viewModel.frequencyData.collectAsState()
    val repRangeData by viewModel.repRangeData.collectAsState()
    val rpeZoneData by viewModel.rpeZoneData.collectAsState()
    val selectedPatternType by viewModel.selectedPatternType.collectAsState()
    val scope = rememberCoroutineScope()

    // Convert exercise name to ID
    var exerciseId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exerciseName) {
        exerciseId = viewModel.repository.getExerciseByName(exerciseName)?.id
    }

    LaunchedEffect(exerciseId) {
        exerciseId?.let { id ->
            viewModel.loadExerciseData(id)
            viewModel.loadChartData(id)
            viewModel.loadMaxWeightChartData(id)
            viewModel.loadVolumeChartData(id)
            viewModel.loadTrainingPatternsData(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseName,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when (state) {
            is ExerciseProgressViewModel.ExerciseProgressState.Loading -> {
                LoadingState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }

            is ExerciseProgressViewModel.ExerciseProgressState.Success -> {
                val successState = state as ExerciseProgressViewModel.ExerciseProgressState.Success
                ExerciseInsightsContent(
                    data = successState.data,
                    chartData =
                        ExerciseChartData(
                            chartData = chartData,
                            maxWeightChartData = maxWeightChartData,
                            volumeChartData = volumeChartData,
                            selectedChartType = selectedChartType,
                            onChartTypeChanged = viewModel::setChartType,
                        ),
                    patternData =
                        ExercisePatternData(
                            frequencyData = frequencyData,
                            repRangeData = repRangeData,
                            rpeZoneData = rpeZoneData,
                            selectedPatternType = selectedPatternType,
                            onPatternTypeChanged = viewModel::setPatternType,
                        ),
                    modifier =
                        modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }

            is ExerciseProgressViewModel.ExerciseProgressState.Error -> {
                ErrorState(
                    onRetry = { scope.launch { exerciseId?.let { viewModel.loadExerciseData(it) } } },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Error loading exercise data",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ExerciseInsightsContent(
    data: ExerciseProgressViewModel.ExerciseProgressData?,
    chartData: ExerciseChartData,
    patternData: ExercisePatternData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (data == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Text(
                    text = "No data available for this exercise",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Last Performed info
            data.lastPerformed?.let { lastDate ->
                val daysSince = ChronoUnit.DAYS.between(lastDate, java.time.LocalDate.now())
                Text(
                    text =
                        when {
                            daysSince == 0L -> "Last performed: Today"
                            daysSince == 1L -> "Last performed: Yesterday"
                            daysSince < 7 -> "Last performed: $daysSince days ago"
                            else -> "Last performed: ${lastDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            // Most Weight Lifted Card
            MostWeightLiftedCard(
                data = data,
                modifier = Modifier.fillMaxWidth(),
            )

            // Quick Stats Cards
            QuickStatsSection(
                data = data,
                modifier = Modifier.fillMaxWidth(),
            )

            // Strength Progress Chart
            ChartSection(
                chartData = chartData,
                modifier = Modifier.fillMaxWidth(),
            )

            // Training Patterns Section
            PatternAnalysisSection(
                patternData = patternData,
                modifier = Modifier.fillMaxWidth(),
            )

            // Plateau Intervention Card
            if (data.progressStatus == ExerciseProgressViewModel.ProgressStatus.PLATEAU && data.plateauWeeks >= 3) {
                PlateauInterventionCard(
                    data = data,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QuickStatsSection(
    data: ExerciseProgressViewModel.ExerciseProgressData,
    modifier: Modifier = Modifier,
) {
    var showStatusTooltip by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FrequencyCard(
            weeklyFrequency = data.weeklyFrequency,
            modifier = Modifier.weight(1f),
        )

        StatusCard(
            data = data,
            onClick = { showStatusTooltip = true },
            modifier = Modifier.weight(1f),
        )
    }

    if (showStatusTooltip) {
        StatusTooltipDialog(
            onDismiss = { showStatusTooltip = false },
        )
    }
}

@Composable
private fun ChartSection(
    chartData: ExerciseChartData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Strength Progress Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Strength Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                ChartTypeSelector(
                    selectedChartType = chartData.selectedChartType,
                    onChartTypeChanged = chartData.onChartTypeChanged,
                    modifier = Modifier.fillMaxWidth(),
                )

                val currentChartData =
                    when (chartData.selectedChartType) {
                        ExerciseProgressViewModel.ChartType.ONE_RM -> chartData.chartData
                        ExerciseProgressViewModel.ChartType.MAX_WEIGHT -> chartData.maxWeightChartData
                    }

                if (currentChartData.isNotEmpty()) {
                    ExerciseProgressChart(
                        dataPoints = currentChartData,
                        isMaxWeightChart = chartData.selectedChartType == ExerciseProgressViewModel.ChartType.MAX_WEIGHT,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    EmptyChartState(
                        message = "No data available",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Training Volume Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Training Volume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (chartData.volumeChartData.isNotEmpty()) {
                    VolumeBarChart(
                        dataPoints = chartData.volumeChartData,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    EmptyChartState(
                        message = "No volume data available",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternAnalysisSection(
    patternData: ExercisePatternData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Training Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            PatternTypeSelector(
                selectedPatternType = patternData.selectedPatternType,
                onPatternTypeChanged = patternData.onPatternTypeChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            when (patternData.selectedPatternType) {
                ExerciseProgressViewModel.PatternType.FREQUENCY -> {
                    TrainingPatternsChart(
                        dataPoints = patternData.frequencyData,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ExerciseProgressViewModel.PatternType.REP_RANGES -> {
                    if (patternData.repRangeData.isNotEmpty()) {
                        RepRangeChart(
                            distributionData = patternData.repRangeData,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        EmptyChartState(
                            message = "No rep range data available",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                ExerciseProgressViewModel.PatternType.RPE_ZONES -> {
                    if (patternData.rpeZoneData.isNotEmpty()) {
                        IntensityZoneChart(
                            intensityData = patternData.rpeZoneData,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        EmptyChartState(
                            message = "No RPE data available",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTypeSelector(
    selectedChartType: ExerciseProgressViewModel.ChartType,
    onChartTypeChanged: (ExerciseProgressViewModel.ChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedChartType == ExerciseProgressViewModel.ChartType.ONE_RM,
            onClick = { onChartTypeChanged(ExerciseProgressViewModel.ChartType.ONE_RM) },
            label = { Text("1RM") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = selectedChartType == ExerciseProgressViewModel.ChartType.MAX_WEIGHT,
            onClick = { onChartTypeChanged(ExerciseProgressViewModel.ChartType.MAX_WEIGHT) },
            label = { Text("Max Weight Lifted") },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PatternTypeSelector(
    selectedPatternType: ExerciseProgressViewModel.PatternType,
    onPatternTypeChanged: (ExerciseProgressViewModel.PatternType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selectedPatternType == ExerciseProgressViewModel.PatternType.FREQUENCY,
            onClick = { onPatternTypeChanged(ExerciseProgressViewModel.PatternType.FREQUENCY) },
            label = { Text("Consistency", maxLines = 1) },
            modifier = Modifier.weight(1.3f),
        )
        FilterChip(
            selected = selectedPatternType == ExerciseProgressViewModel.PatternType.REP_RANGES,
            onClick = { onPatternTypeChanged(ExerciseProgressViewModel.PatternType.REP_RANGES) },
            label = { Text("Reps", maxLines = 1) },
            modifier = Modifier.weight(0.85f),
        )
        FilterChip(
            selected = selectedPatternType == ExerciseProgressViewModel.PatternType.RPE_ZONES,
            onClick = { onPatternTypeChanged(ExerciseProgressViewModel.PatternType.RPE_ZONES) },
            label = { Text("RPE", maxLines = 1) },
            modifier = Modifier.weight(0.85f),
        )
    }
}

@Composable
private fun EmptyChartState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PlateauInterventionCard(
    data: ExerciseProgressViewModel.ExerciseProgressData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFF9800).copy(alpha = 0.1f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                )
                Text(
                    text = "Plateau Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = data.progressStatusDetail,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Consider: Deload week • Change rep range • Vary intensity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun FrequencyCard(
    weeklyFrequency: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .height(120.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = "${WeightFormatter.formatDecimal(weeklyFrequency, 1)}x/week",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun StatusCard(
    data: ExerciseProgressViewModel.ExerciseProgressData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .height(120.dp)
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = getStatusBackgroundColor(data.progressStatus),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = getStatusIconColor(data.progressStatus),
                    )
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Long press for details",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            Text(
                text = getStatusDisplayName(data.progressStatus),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = data.progressStatusDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun getStatusBackgroundColor(status: ExerciseProgressViewModel.ProgressStatus): Color =
    when (status) {
        ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
        ExerciseProgressViewModel.ProgressStatus.PLATEAU -> Color(0xFFFF9800).copy(alpha = 0.2f)
        ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> Color(0xFFFF5722).copy(alpha = 0.2f)
        ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> MaterialTheme.colorScheme.surfaceVariant
    }

@Composable
private fun getStatusIconColor(status: ExerciseProgressViewModel.ProgressStatus): Color =
    when (status) {
        ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> Color(0xFF4CAF50)
        ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> MaterialTheme.colorScheme.primary
        ExerciseProgressViewModel.ProgressStatus.PLATEAU -> Color(0xFFFF9800)
        ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> Color(0xFFFF5722)
        ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun getStatusDisplayName(status: ExerciseProgressViewModel.ProgressStatus): String =
    when (status) {
        ExerciseProgressViewModel.ProgressStatus.MAKING_GAINS -> "Making Gains"
        ExerciseProgressViewModel.ProgressStatus.STEADY_PROGRESS -> "Steady Progress"
        ExerciseProgressViewModel.ProgressStatus.PLATEAU -> "Plateau"
        ExerciseProgressViewModel.ProgressStatus.EXTENDED_BREAK -> "Extended Break"
        ExerciseProgressViewModel.ProgressStatus.WORKING_LIGHTER -> "Working Lighter"
    }

@Composable
private fun StatusTooltipDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How Status Works") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusExplanation(
                    status = "Making Gains",
                    color = Color(0xFF4CAF50),
                    description = "Your weights are increasing. Shows progress made in the last month.",
                )
                StatusExplanation(
                    status = "Steady Progress",
                    color = MaterialTheme.colorScheme.primary,
                    description = "Consistent training without major weight changes.",
                )
                StatusExplanation(
                    status = "Plateau",
                    color = Color(0xFFFF9800),
                    description = "Stuck at the same weight for multiple weeks. Consider a deload or changing rep ranges.",
                )
                StatusExplanation(
                    status = "Extended Break",
                    color = Color(0xFFFF5722),
                    description = "Haven't performed this exercise in 14+ days.",
                )
                StatusExplanation(
                    status = "Working Lighter",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = "Current working weight is less than 90% of your estimated max, possibly due to deload or recovery.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}

@Composable
private fun StatusExplanation(
    status: String,
    color: Color,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.width(100.dp),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MostWeightLiftedCard(
    data: ExerciseProgressViewModel.ExerciseProgressData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Most weight lifted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            Text(
                text =
                    if (data.allTimePR > 0) {
                        WeightFormatter.formatWeightWithUnit(data.allTimePR)
                    } else {
                        "No lifts yet"
                    },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            data.allTimePRDate?.let { date ->
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}
