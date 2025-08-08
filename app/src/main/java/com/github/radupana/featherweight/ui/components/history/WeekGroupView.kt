package com.github.radupana.featherweight.ui.components.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.repository.WorkoutSummary
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class WeekGroup(
    val weekNumber: Int,
    val year: Int,
    val workouts: List<WorkoutSummary>,
    val startDate: String,
    val endDate: String,
    val totalVolume: Double,
    val totalWorkouts: Int,
)

@Composable
fun WeekGroupView(
    weekGroup: WeekGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onWorkoutLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column {
            // Week header (clickable)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpanded() }
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Week ${weekGroup.weekNumber}, ${weekGroup.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${weekGroup.startDate} - ${weekGroup.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Week summary chips
                    WeekSummaryChip(
                        label = "${weekGroup.totalWorkouts}",
                        description = "workouts",
                    )

                    if (weekGroup.totalVolume > 0) {
                        WeekSummaryChip(
                            label = WeightFormatter.formatWeight(weekGroup.totalVolume.toFloat()),
                            description = "volume",
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier =
                            Modifier
                                .size(20.dp)
                                .rotate(rotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expandable workout list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        weekGroup.workouts.forEach { workout ->
                            CompactWorkoutCard(
                                workout = workout,
                                onClick = { onWorkoutClick(workout.id) },
                                onLongClick = { onWorkoutLongClick(workout.id) },
                            )
                        }

                        if (weekGroup.workouts.isEmpty()) {
                            Text(
                                text = "No workouts this week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSummaryChip(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactWorkoutCard(
    workout: WorkoutSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        workout.name ?: workout.date.format(
                            DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "${workout.exerciseCount} exercises • ${workout.setCount} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (workout.totalWeight > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = WeightFormatter.formatWeight(workout.totalWeight.toFloat()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * Groups workouts by ISO week
 */
fun groupWorkoutsByWeek(workouts: List<WorkoutSummary>): List<WeekGroup> {
    val weekFields = WeekFields.of(Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    return workouts
        .groupBy { workout ->
            val weekNumber = workout.date.get(weekFields.weekOfYear())
            val year = workout.date.year
            Pair(weekNumber, year)
        }.map { (weekYear, workoutsInWeek) ->
            val (weekNumber, year) = weekYear
            val sortedWorkouts = workoutsInWeek.sortedByDescending { it.date }
            val startOfWeek = sortedWorkouts.minOf { it.date }
            val endOfWeek = sortedWorkouts.maxOf { it.date }

            WeekGroup(
                weekNumber = weekNumber,
                year = year,
                workouts = sortedWorkouts,
                startDate = startOfWeek.format(dateFormatter),
                endDate = endOfWeek.format(dateFormatter),
                totalVolume = workoutsInWeek.sumOf { it.totalWeight.toDouble() },
                totalWorkouts = workoutsInWeek.size,
            )
        }.sortedWith(compareByDescending<WeekGroup> { it.year }.thenByDescending { it.weekNumber })
}
