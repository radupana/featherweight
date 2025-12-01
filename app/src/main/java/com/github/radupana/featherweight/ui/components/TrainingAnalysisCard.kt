package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.AdherenceAnalysis
import com.github.radupana.featherweight.data.InsightSeverity
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.TrainingInsight
import com.github.radupana.featherweight.ui.theme.FeatherweightColors
import java.time.temporal.ChronoUnit

@Composable
fun TrainingAnalysisCard(
    analysis: TrainingAnalysis?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    currentWorkoutCount: Int = 0,
    analysisQuota: com.github.radupana.featherweight.viewmodel.InsightsViewModel.AnalysisQuota? = null,
    lastAnalysisDate: java.time.LocalDateTime? = null,
    onTriggerAnalysis: () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Training Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (currentWorkoutCount in 1..5) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Limited Data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier =
                                    Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            RoundedCornerShape(4.dp),
                                        ).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (analysis != null && !analysis.overallAssessment.startsWith("INSUFFICIENT_DATA:")) {
                        val daysAgo =
                            ChronoUnit.DAYS.between(
                                analysis.analysisDate.toLocalDate(),
                                java.time.LocalDate.now(),
                            )
                        Text(
                            text =
                                when (daysAgo) {
                                    0L -> "Updated today"
                                    1L -> "Updated yesterday"
                                    else -> "Updated $daysAgo days ago"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp),
                )
            }

            // Content
            val uiState = calculateTrainingAnalysisUIState(analysis, currentWorkoutCount, isLoading)

            when (uiState) {
                is TrainingAnalysisUIState.Loading -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                is TrainingAnalysisUIState.InsufficientData -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text(
                            text = "Building training history...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text =
                                "Need at least ${uiState.required} workouts for analysis " +
                                    "(${uiState.current}/${uiState.required} completed)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (uiState.current.toFloat() / uiState.required).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                is TrainingAnalysisUIState.ReadyForAnalysis -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.assessment != null) {
                        Text(
                            text = uiState.assessment,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            text = "No analysis available yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (uiState.showAnalyzeButton) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (analysisQuota != null) {
                                val resetDateFormatted =
                                    analysisQuota.resetDate.format(
                                        java.time.format.DateTimeFormatter
                                            .ofPattern("MMM d"),
                                    )
                                Text(
                                    text = "${analysisQuota.monthlyRemaining} remaining (resets $resetDateFormatted)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Button(
                                onClick = {
                                    val daysSinceLastAnalysis =
                                        lastAnalysisDate?.let {
                                            ChronoUnit.DAYS.between(it.toLocalDate(), java.time.LocalDate.now())
                                        }
                                    if (daysSinceLastAnalysis != null && daysSinceLastAnalysis < 7) {
                                        showWarningDialog = true
                                    } else {
                                        onTriggerAnalysis()
                                    }
                                },
                                enabled = !isLoading && (analysisQuota?.monthlyRemaining ?: 1) > 0,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Analyze again",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analyze Again")
                            }
                        }
                    }

                    analysis?.let { validAnalysis ->
                        AnimatedVisibility(
                            visible = isExpanded && uiState.assessment != null,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                if (validAnalysis.keyInsights.isNotEmpty()) {
                                    validAnalysis.keyInsights.forEach { insight ->
                                        InsightRow(insight)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (validAnalysis.warnings.isNotEmpty()) {
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    validAnalysis.warnings.forEach { warning ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Warning",
                                                tint = FeatherweightColors.warning(),
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = warning,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (validAnalysis.recommendations.isNotEmpty()) {
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Recommendations",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )

                                    validAnalysis.recommendations.forEach { recommendation ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(end = 8.dp),
                                            )
                                            Text(
                                                text = recommendation,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                validAnalysis.adherenceAnalysis?.let { adherence ->
                                    AdherenceSection(adherence)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Warning dialog for recent analysis
    if (showWarningDialog) {
        lastAnalysisDate?.let {
            ChronoUnit.DAYS.between(it.toLocalDate(), java.time.LocalDate.now())
        } ?: 0

        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Recent Analysis") },
            text = {
                Text(
                    "You recently analyzed your training. " +
                        "Training patterns typically don't change significantly " +
                        "in less than a week. This will use one of your analyses.\n\n" +
                        "Continue anyway?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        onTriggerAnalysis()
                    },
                ) {
                    Text("Analyze")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWarningDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun InsightRow(insight: TrainingInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        val icon =
            when (insight.severity) {
                InsightSeverity.SUCCESS -> "✅"
                InsightSeverity.WARNING -> "⚠️"
                InsightSeverity.CRITICAL -> "❌"
                InsightSeverity.INFO -> "ℹ️"
            }

        Text(
            text = icon,
            modifier = Modifier.padding(end = 8.dp),
        )

        Text(
            text = insight.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AdherenceSection(adherence: AdherenceAnalysis) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Programme Adherence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            AdherenceScoreBadge(score = adherence.adherenceScore)
        }

        if (adherence.scoreExplanation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = adherence.scoreExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (adherence.positivePatterns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            adherence.positivePatterns.forEach { pattern ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "✓",
                        color = FeatherweightColors.success(),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (adherence.negativePatterns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            adherence.negativePatterns.forEach { pattern ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "!",
                        color = FeatherweightColors.warning(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (adherence.adherenceRecommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "To Improve Adherence",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            adherence.adherenceRecommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "→",
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AdherenceScoreBadge(score: Int) {
    val (backgroundColor, textColor) =
        when {
            score >= 90 -> FeatherweightColors.scoreExcellent() to FeatherweightColors.onSuccess()
            score >= 75 -> FeatherweightColors.scoreGood() to MaterialTheme.colorScheme.onSurface
            score >= 60 -> FeatherweightColors.scoreFair() to MaterialTheme.colorScheme.onSurface
            else -> FeatherweightColors.scorePoor() to MaterialTheme.colorScheme.onError
        }

    Box(
        modifier =
            Modifier
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$score%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
