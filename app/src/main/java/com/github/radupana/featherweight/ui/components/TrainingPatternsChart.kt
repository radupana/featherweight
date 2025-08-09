package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class FrequencyDataPoint(
    val date: LocalDate,
    val volume: Float,
    val sessions: Int,
)

@Composable
fun TrainingPatternsChart(
    dataPoints: List<FrequencyDataPoint>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (dataPoints.isEmpty()) {
                // No data state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No training data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                // Calculate metrics
                val (consistencyScore, restDaysDistribution, typicalRest) = remember(dataPoints) {
                    calculateTrainingPatternMetrics(dataPoints)
                }
                
                // Consistency Score
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${consistencyScore.toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = { consistencyScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    
                    Text(
                        text = "Last 12 weeks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                
                // Rest Days Distribution - only show if we have enough data
                if (dataPoints.size >= 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Days Between Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        
                        // Distribution bars
                        restDaysDistribution.forEach { (range, percentage) ->
                            RestDayBar(
                                label = range,
                                percentage = percentage,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        
                        // Typical rest pattern
                        if (typicalRest.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your typical rest: $typicalRest",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                } else {
                    // Not enough data for rest patterns
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "More training needed to show rest patterns",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestDayBar(
    label: String,
    percentage: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
        )
        
        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            )
        }
        
        // Percentage
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(35.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

private fun calculateTrainingPatternMetrics(
    dataPoints: List<FrequencyDataPoint>
): Triple<Float, List<Pair<String, Float>>, String> {
    // 1. Calculate consistency score
    val endDate = LocalDate.now()
    val startDate = endDate.minusWeeks(12)
    val weeksWithTraining = mutableSetOf<LocalDate>()
    
    dataPoints.forEach { point ->
        if (!point.date.isBefore(startDate) && !point.date.isAfter(endDate)) {
            // Find Monday of this week
            val monday = point.date.minusDays(point.date.dayOfWeek.value - 1L)
            weeksWithTraining.add(monday)
        }
    }
    
    val consistencyScore = (weeksWithTraining.size / 12f) * 100
    
    // 2. Calculate rest days distribution
    val restDaysList = mutableListOf<Long>()
    val sortedDates = dataPoints.map { it.date }.sorted()
    
    for (i in 1 until sortedDates.size) {
        val daysBetween = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
        restDaysList.add(daysBetween)
    }
    
    // Group into buckets
    val buckets = mapOf(
        "1 day" to restDaysList.count { it == 1L },
        "2-3 days" to restDaysList.count { it in 2..3 },
        "4-5 days" to restDaysList.count { it in 4..5 },
        "6-7 days" to restDaysList.count { it in 6..7 },
        "8+ days" to restDaysList.count { it >= 8 }
    )
    
    val total = restDaysList.size.toFloat()
    val distribution = if (total > 0) {
        buckets
            .filter { it.value > 0 }
            .map { (range, count) -> 
                range to (count / total * 100)
            }
    } else {
        emptyList()
    }
    
    // 3. Determine typical rest pattern
    val typicalRest = if (distribution.isNotEmpty()) {
        val maxBucket = distribution.maxByOrNull { it.second }
        maxBucket?.first?.replace(" days", " days") ?: ""
    } else {
        ""
    }
    
    return Triple(consistencyScore, distribution, typicalRest)
}