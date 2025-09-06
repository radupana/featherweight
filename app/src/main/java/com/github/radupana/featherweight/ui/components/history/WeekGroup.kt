package com.github.radupana.featherweight.ui.components.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.domain.WorkoutSummary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    modifier: Modifier = Modifier,
    onExportWorkout: (Long) -> Unit = {},
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
                        text = "${weekGroup.startDate} - ${weekGroup.endDate}, ${weekGroup.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${weekGroup.totalWorkouts} ${if (weekGroup.totalWorkouts == 1) "workout" else "workouts"} this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    if (weekGroup.workouts.isEmpty()) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No workouts this week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            weekGroup.workouts.forEach { workout ->
                                CompactWorkoutCard(
                                    workout = workout,
                                    onClick = { onWorkoutClick(workout.id) },
                                    onExport = { onExportWorkout(workout.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactWorkoutCard(
    workout: WorkoutSummary,
    onClick: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showExportDialog by remember { mutableStateOf(false) }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showExportDialog = true },
                ),
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
                // Clean up workout name if it starts with "Repeat Workout from "
                val displayName =
                    workout.name?.let { name ->
                        if (name.startsWith("Repeat Workout from ")) {
                            name.removePrefix("Repeat Workout from ")
                        } else {
                            name
                        }
                    }

                Text(
                    text = displayName ?: "Workout",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Show relative time for the workout date
                val relativeTime = formatRelativeTime(workout.date)
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "${workout.exerciseCount} exercises • ${workout.setCount} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export workout",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Workout") },
            text = {
                Text("Export \"${workout.name ?: "Workout"}\" from ${workout.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExport()
                        showExportDialog = false
                    },
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Formats a date/time into a user-friendly relative format.
 */
internal fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val daysDiff = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

    return when {
        daysDiff == 0L -> {
            // Today - show time
            "Today at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        }
        daysDiff == 1L -> {
            // Yesterday
            "Yesterday at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        }
        daysDiff < 7L -> {
            // This week - show day name and time
            "${dateTime.format(DateTimeFormatter.ofPattern("EEEE"))} at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        }
        daysDiff < 14L -> {
            // Last week
            "Last ${dateTime.format(DateTimeFormatter.ofPattern("EEEE"))}"
        }
        daysDiff < 30L -> {
            // Within a month - show date
            dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
        }
        dateTime.year == now.year -> {
            // This year - show month and day
            dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
        }
        else -> {
            // Different year - show full date
            dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
}
