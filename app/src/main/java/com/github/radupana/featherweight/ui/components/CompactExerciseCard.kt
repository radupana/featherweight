package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.theme.FeatherweightColors
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.ui.utils.DragHandle
import com.github.radupana.featherweight.util.WeightFormatter
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@Composable
fun CompactExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    onEditSets: () -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onSwapExercise: (Long) -> Unit,
    viewModel: WorkoutViewModel,
    showDragHandle: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Calculate metrics
    val completedSets = sets.count { it.isCompleted }
    val totalVolume = sets.filter { it.isCompleted }.sumOf { (it.actualReps * it.actualWeight).toDouble() }.toFloat()
    val bestSet = sets.filter { it.isCompleted }.maxByOrNull { it.actualReps * it.actualWeight }

    GlassCard(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(),
        onClick = { onEditSets() },
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Drag handle on the left
            if (showDragHandle) {
                DragHandle(
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDrag = onDrag,
                )
            }

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Exercise name and progress in one row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        // Swap indicator
                        if (exercise.isSwapped) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = "Exercise was swapped",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    if (sets.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color =
                                if (completedSets == sets.size) {
                                    FeatherweightColors.successGradientStart.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                },
                        ) {
                            Text(
                                text = "$completedSets/${sets.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (completedSets == sets.size) {
                                        FeatherweightColors.successGradientStart
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Compact metrics row
                if (sets.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Volume
                        if (totalVolume > 0) {
                            CompactMetric(
                                icon = Icons.Filled.FitnessCenter,
                                value = WeightFormatter.formatWeightWithUnit(totalVolume),
                                contentDescription = "Total volume",
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // Best set
                        if (bestSet != null) {
                            CompactMetric(
                                icon = Icons.Filled.Star,
                                value = "${bestSet.actualReps}×${WeightFormatter.formatWeight(bestSet.actualWeight)}",
                                contentDescription = "Best set",
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { 
                                if (sets.isEmpty()) 0f 
                                else (completedSets.toFloat() / sets.size).coerceIn(0f, 1f) 
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(4.dp),
                            color =
                                if (completedSets == sets.size) {
                                    FeatherweightColors.successGradientStart
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                } else {
                    // Empty state - very compact
                    Text(
                        text = "Tap to add sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // Action buttons
            if (viewModel.canEditWorkout()) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.SwapHoriz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text("Swap Exercise")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onSwapExercise(exercise.id)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Text("Delete Exercise")
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }
            }
        }
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise?") },
            text = { Text("Remove ${exercise.exerciseName} and all its sets?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExercise(exercise.id)
                        showDeleteDialog = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CompactMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    contentDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
