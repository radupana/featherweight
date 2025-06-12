package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.ui.components.StrengthProgressionChart
import com.github.radupana.featherweight.ui.components.VolumeBarChart
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier,
) {
    val analyticsState by viewModel.analyticsState.collectAsState()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Stats Cards
        if (analyticsState.isQuickStatsLoading) {
            QuickStatsLoadingSection()
        } else {
            QuickStatsSection(analyticsState)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (analyticsState.isStrengthLoading) {
                    LoadingCard("Strength Progression")
                } else {
                    StrengthProgressionSection(analyticsState, viewModel)
                }
            }

            item {
                if (analyticsState.isVolumeLoading) {
                    LoadingCard("Volume Analysis")
                } else {
                    VolumeAnalysisSection(analyticsState)
                }
            }

            item {
                if (analyticsState.isPerformanceLoading) {
                    LoadingCard("Performance Insights")
                } else {
                    PerformanceInsightsSection(analyticsState)
                }
            }
        }
    }
}

// Utility function to format large numbers
private fun formatVolume(volume: Float): String {
    return when {
        volume >= 1000 -> "${String.format("%.1f", volume / 1000)}k kg"
        else -> "${volume.toInt()}kg"
    }
}

@Composable
private fun QuickStatsSection(analyticsState: com.github.radupana.featherweight.viewmodel.AnalyticsState) {
    var showTooltip by remember { mutableStateOf<String?>(null) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val quickStats = analyticsState.quickStats
        val recentPR = quickStats.recentPR
        val progressText = quickStats.monthlyProgress?.let { "${if (it >= 0) "+" else ""}${String.format("%.1f", it)}%" } ?: "N/A"

        // Parse volume and format properly
        val volumeValue = quickStats.weeklyVolume.replace("kg", "").toFloatOrNull() ?: 0f
        val formattedVolume = formatVolume(volumeValue)

        val statsData =
            listOf(
                Triple(
                    QuickStat("This Week", formattedVolume, "Volume", Icons.Filled.FitnessCenter, Color(0xFF4CAF50)),
                    "volume",
                    "Total weight lifted this week (weight × reps for all completed sets)",
                ),
                Triple(
                    QuickStat(
                        "Latest PR",
                        recentPR?.let {
                            "${it.second.toInt()}kg"
                        } ?: "No PRs",
                        recentPR?.let {
                            // Shorten exercise names for better display
                            when (it.first) {
                                "Conventional Deadlift" -> "Deadlift"
                                "Back Squat" -> "Squat"
                                "Bench Press" -> "Bench"
                                "Overhead Press" -> "OHP"
                                else -> it.first
                            }
                        } ?: "Set a record!",
                        Icons.Filled.TrendingUp,
                        Color(0xFF2196F3),
                    ),
                    "pr",
                    "Your most recent personal record across the main lifts (Squat, Deadlift, Bench Press, Overhead Press)",
                ),
                Triple(
                    QuickStat(
                        "Frequency",
                        "${String.format("%.1f", quickStats.avgTrainingDaysPerWeek)}",
                        "days/week",
                        Icons.Filled.CalendarMonth,
                        Color(0xFFFF6F00),
                    ),
                    "frequency",
                    "Average training days per week since you started using the app",
                ),
                Triple(
                    QuickStat("Strength Gain", progressText, "This Month", Icons.Filled.ShowChart, Color(0xFF9C27B0)),
                    "strength",
                    "Average strength improvement across the main lifts (Squat, Deadlift, Bench Press, Overhead Press) in the last 30 days",
                ),
            )

        items(statsData) { (stat, tooltipKey, tooltipText) ->
            QuickStatCard(
                stat = stat,
                onClick = { showTooltip = tooltipText },
            )
        }
    }

    // Tooltip dialog
    showTooltip?.let { tooltipText ->
        Dialog(
            onDismissRequest = { showTooltip = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "How this is calculated:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTooltip = null }) {
                            Text("Got it")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    stat: QuickStat,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    GlassCard(
        modifier =
            modifier
                .width(160.dp)
                .height(120.dp),
        // Fixed height for consistency
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = stat.color,
                modifier = Modifier.size(20.dp),
            )

            // Value with proper text handling
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stat.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StrengthProgressionSection(
    analyticsState: com.github.radupana.featherweight.viewmodel.AnalyticsState,
    viewModel: AnalyticsViewModel,
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Progression",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilterChip(
                        onClick = { viewModel.setChartViewMode(com.github.radupana.featherweight.viewmodel.ChartViewMode.ONE_RM) },
                        label = {
                            Text(
                                text = "1RM",
                                maxLines = 1,
                            )
                        },
                        selected = analyticsState.chartViewMode == com.github.radupana.featherweight.viewmodel.ChartViewMode.ONE_RM,
                        modifier = Modifier.height(32.dp),
                    )
                    FilterChip(
                        onClick = { viewModel.setChartViewMode(com.github.radupana.featherweight.viewmodel.ChartViewMode.VOLUME) },
                        label = {
                            Text(
                                text = "Vol",
                                maxLines = 1,
                            )
                        },
                        selected = analyticsState.chartViewMode == com.github.radupana.featherweight.viewmodel.ChartViewMode.VOLUME,
                        modifier = Modifier.height(32.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart display based on view mode
            when (analyticsState.chartViewMode) {
                com.github.radupana.featherweight.viewmodel.ChartViewMode.ONE_RM -> {
                    StrengthProgressionChart(
                        data = analyticsState.strengthMetrics.personalRecords,
                        exerciseName = analyticsState.strengthMetrics.selectedExercise,
                        lineColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        onDataPointTapped = { weight, date ->
                            // Data point interaction is handled internally by the chart
                            println("📊 Analytics: Tapped data point - ${weight.toInt()}kg on $date")
                        },
                    )
                }
                com.github.radupana.featherweight.viewmodel.ChartViewMode.VOLUME -> {
                    // Show volume trend for selected exercise
                    VolumeBarChart(
                        weeklyData = analyticsState.volumeMetrics.weeklyHistory.takeLast(6),
                        barColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(analyticsState.availableExercises) { exercise ->
                    FilterChip(
                        onClick = { viewModel.selectExercise(exercise) },
                        label = { Text(exercise) },
                        selected = exercise == analyticsState.strengthMetrics.selectedExercise,
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeAnalysisSection(analyticsState: com.github.radupana.featherweight.viewmodel.AnalyticsState) {
    GlassCard {
        Column {
            Text(
                text = "Volume Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val volumeMetrics = analyticsState.volumeMetrics
                VolumeMetric(
                    title = "This Week",
                    value = "${volumeMetrics.thisWeek.toInt()}kg",
                    change = "${if (volumeMetrics.weeklyChange >= 0) "+" else ""}${String.format("%.1f", volumeMetrics.weeklyChange)}%",
                    isPositive =
                        if (volumeMetrics.weeklyChange > 0) {
                            true
                        } else if (volumeMetrics.weeklyChange < 0) {
                            false
                        } else {
                            null
                        },
                )
                VolumeMetric(
                    title = "This Month",
                    value = "${volumeMetrics.thisMonth.toInt()}kg",
                    change = "${if (volumeMetrics.monthlyChange >= 0) "+" else ""}${String.format("%.1f", volumeMetrics.monthlyChange)}%",
                    isPositive =
                        if (volumeMetrics.monthlyChange > 0) {
                            true
                        } else if (volumeMetrics.monthlyChange < 0) {
                            false
                        } else {
                            null
                        },
                )
                VolumeMetric(
                    title = "Average/Week",
                    value = "${volumeMetrics.averageWeekly.toInt()}kg",
                    change = "Average",
                    isPositive = null,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume trends chart
            val volumeMetrics = analyticsState.volumeMetrics
            VolumeBarChart(
                weeklyData = volumeMetrics.weeklyHistory.takeLast(6), // Show last 6 weeks
                barColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PerformanceInsightsSection(analyticsState: com.github.radupana.featherweight.viewmodel.AnalyticsState) {
    GlassCard {
        Column {
            Text(
                text = "Performance Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val performanceMetrics = analyticsState.performanceMetrics
                val strengthMetrics = analyticsState.strengthMetrics
                val volumeMetrics = analyticsState.volumeMetrics
                val quickStats = analyticsState.quickStats

                // Strength trend insight
                val strengthDescription =
                    strengthMetrics.recentProgress?.let { progress ->
                        "Your ${strengthMetrics.selectedExercise.lowercase()} has ${if (progress >= 0) "increased" else "decreased"} ${String.format("%.1f", kotlin.math.abs(progress))}% this month"
                    } ?: "Need more data to show strength trends"

                InsightCard(
                    icon = Icons.Filled.TrendingUp,
                    title = "Strength Trend",
                    description = strengthDescription,
                    color = if ((strengthMetrics.recentProgress ?: 0f) >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                )

                // Volume progression insight
                val volumeDescription =
                    when {
                        volumeMetrics.weeklyChange > 10 -> "Volume increased ${String.format(
                            "%.1f",
                            volumeMetrics.weeklyChange,
                        )}% this week - great intensity!"
                        volumeMetrics.weeklyChange > 0 -> "Volume up ${String.format(
                            "%.1f",
                            volumeMetrics.weeklyChange,
                        )}% - steady progress"
                        volumeMetrics.weeklyChange > -10 -> "Volume down ${String.format(
                            "%.1f",
                            kotlin.math.abs(volumeMetrics.weeklyChange),
                        )}% - recovery week?"
                        else -> "Volume dropped ${String.format(
                            "%.1f",
                            kotlin.math.abs(volumeMetrics.weeklyChange),
                        )}% - consider increasing intensity"
                    }

                InsightCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Volume Progression",
                    description = volumeDescription,
                    color = if (volumeMetrics.weeklyChange >= 0) Color(0xFF4CAF50) else Color(0xFFFF9800),
                )

                // Training consistency insight
                val consistencyDescription =
                    when {
                        quickStats.avgTrainingDaysPerWeek >= 4 -> "${String.format(
                            "%.1f",
                            quickStats.avgTrainingDaysPerWeek,
                        )} days/week average - outstanding consistency!"
                        quickStats.avgTrainingDaysPerWeek >= 3 -> "${String.format(
                            "%.1f",
                            quickStats.avgTrainingDaysPerWeek,
                        )} days/week average - solid routine"
                        quickStats.avgTrainingDaysPerWeek >= 2 -> "${String.format(
                            "%.1f",
                            quickStats.avgTrainingDaysPerWeek,
                        )} days/week average - room for improvement"
                        else -> "${String.format("%.1f", quickStats.avgTrainingDaysPerWeek)} days/week average - build the habit"
                    }

                InsightCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Training Consistency",
                    description = consistencyDescription,
                    color =
                        when {
                            quickStats.avgTrainingDaysPerWeek >= 4 -> Color(0xFF4CAF50)
                            quickStats.avgTrainingDaysPerWeek >= 3 -> Color(0xFF2196F3)
                            quickStats.avgTrainingDaysPerWeek >= 2 -> Color(0xFFFF9800)
                            else -> Color(0xFFE53935)
                        },
                )

                // Personal Record insight
                val prDescription =
                    quickStats.recentPR?.let { (exercise, weight) ->
                        "Latest PR: ${weight.toInt()}kg on ${exercise.lowercase()} - keep pushing those limits!"
                    } ?: "No recent PRs tracked - time to set some new records!"

                InsightCard(
                    icon = Icons.Filled.EmojiEvents,
                    title = "Personal Records",
                    description = prDescription,
                    color = if (quickStats.recentPR != null) Color(0xFFFFD700) else Color(0xFF9E9E9E),
                )

                // RPE/Recovery insight
                val rpeDescription =
                    performanceMetrics.averageRPE?.let { avgRPE ->
                        when {
                            avgRPE < 6 -> "Average RPE ${String.format("%.1f", avgRPE)} - you might be able to push harder"
                            avgRPE <= 8 -> "Average RPE ${String.format("%.1f", avgRPE)} - perfect training intensity"
                            else -> "Average RPE ${String.format("%.1f", avgRPE)} - consider more recovery time"
                        }
                    } ?: "Track RPE to monitor training intensity and recovery"

                InsightCard(
                    icon = Icons.Filled.Psychology,
                    title = "Training Intensity",
                    description = rpeDescription,
                    color = Color(0xFF9C27B0),
                )
            }
        }
    }
}

@Composable
private fun VolumeMetric(
    title: String,
    value: String,
    change: String,
    isPositive: Boolean?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = change,
            style = MaterialTheme.typography.labelSmall,
            color =
                when (isPositive) {
                    true -> Color(0xFF4CAF50)
                    false -> Color(0xFFE53935)
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class QuickStat(
    val label: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
)

@Composable
private fun QuickStatsLoadingSection() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(4) { // 4 loading cards
            LoadingStatCard()
        }
    }
}

@Composable
private fun LoadingStatCard() {
    GlassCard(
        modifier =
            Modifier
                .width(160.dp)
                .height(120.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
        ) {
            // Loading icon placeholder
            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shimmerEffect(),
            )

            // Loading value placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Loading label placeholders
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmerEffect(),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmerEffect(),
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(title: String) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading content placeholder
            repeat(3) { index ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(if (index == 1) 120.dp else 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect(),
                )
                if (index < 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun Modifier.shimmerEffect(): Modifier =
    composed {
        var size by remember {
            mutableStateOf(Size.Zero)
        }
        val transition = rememberInfiniteTransition(label = "shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width,
            targetValue = 2 * size.width,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000),
                ),
            label = "shimmer",
        )

        background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color(0xFFB0BEC5),
                            Color(0xFF90A4AE),
                            Color(0xFFB0BEC5),
                        ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width, size.height),
                ),
        ).onGloballyPositioned {
            size = Size(it.size.width.toFloat(), it.size.height.toFloat())
        }
    }
