package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.InsightSeverity
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.TrainingInsight
import java.time.temporal.ChronoUnit

@Composable
fun TrainingAnalysisCard(
    analysis: TrainingAnalysis?,
    isLoading: Boolean,
    currentWorkoutCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Training Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (analysis != null) {
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
            when {
                isLoading -> {
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

                analysis == null -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No analysis available. Check back later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Check if this is an insufficient data analysis
                    if (analysis.overallAssessment.startsWith("INSUFFICIENT_DATA:")) {
                        val parts = analysis.overallAssessment.split(":")
                        val required = parts.getOrNull(2)?.toIntOrNull() ?: 16
                        // Use the live currentWorkoutCount instead of the cached value
                        val current = currentWorkoutCount

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
                                    "Need at least $required workouts for analysis " +
                                        "($current/$required completed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (current.toFloat() / required).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    } else {
                        // Overall Assessment (always visible)
                        Text(
                            text = analysis.overallAssessment,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Expanded content (only show if not insufficient data)
                    val isInsufficientData = analysis.overallAssessment.startsWith("INSUFFICIENT_DATA:")
                    AnimatedVisibility(
                        visible = isExpanded && !isInsufficientData,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // Key Insights
                            if (analysis.keyInsights.isNotEmpty()) {
                                analysis.keyInsights.forEach { insight ->
                                    InsightRow(insight)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Warnings
                            if (analysis.warnings.isNotEmpty()) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                analysis.warnings.forEach { warning ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
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

                            // Recommendations
                            if (analysis.recommendations.isNotEmpty()) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )

                                analysis.recommendations.forEach { recommendation ->
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(insight: TrainingInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Icon based on severity
        val (icon, _) =
            when (insight.severity) {
                InsightSeverity.SUCCESS -> "✅" to MaterialTheme.colorScheme.primary
                InsightSeverity.WARNING -> "⚠️" to Color(0xFFFF9800)
                InsightSeverity.CRITICAL -> "❌" to MaterialTheme.colorScheme.error
                InsightSeverity.INFO -> "ℹ️" to MaterialTheme.colorScheme.onSurfaceVariant
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
