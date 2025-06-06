package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    onEditSets: () -> Unit,
    onDeleteExercise: (Long) -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }

    // Calculate summary metrics
    val completedSets = sets.count { it.isCompleted }
    val totalVolume = sets.filter { it.isCompleted }.sumOf { (it.reps * it.weight).toDouble() }.toFloat()
    val bestSet = sets.filter { it.isCompleted }.maxByOrNull { it.reps * it.weight }
    val avgRpe = sets.filter { it.isCompleted && it.rpe != null }.map { it.rpe!! }.average().takeIf { !it.isNaN() }?.toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    if (viewModel.canEditWorkout()) {
                        onEditSets()
                    }
                },
                onLongClick = {
                    if (viewModel.canEditWorkout()) {
                        showDeleteExerciseDialog = true
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Exercise header with menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Progress indicator
                    if (sets.isNotEmpty()) {
                        Surface(
                            color = if (completedSets == sets.size) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "$completedSets/${sets.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (completedSets == sets.size) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    // Long tap hint when editable
                    if (viewModel.canEditWorkout()) {
                        Text(
                            "Long tap to delete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sets.isEmpty()) {
                // Empty state
                Text(
                    "No sets yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(completedSets.toFloat() / sets.size * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { completedSets.toFloat() / sets.size },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Volume
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "📊",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (totalVolume > 0) "${totalVolume.toInt()}kg" else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Volume",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Best Set
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "💪",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (bestSet != null) "${bestSet.reps}×${bestSet.weight}kg" else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Best Set",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // RPE Average
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "⚡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (avgRpe != null) {
                                if (avgRpe == avgRpe.toInt().toFloat()) {
                                    avgRpe.toInt().toString()
                                } else {
                                    String.format("%.1f", avgRpe)
                                }
                            } else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Avg RPE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tap hint for interaction
            if (viewModel.canEditWorkout()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Tap to edit sets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    "${sets.size} sets completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Delete exercise confirmation dialog
    if (showDeleteExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteExerciseDialog = false },
            title = { Text("Delete Exercise") },
            text = {
                Text(
                    "Are you sure you want to delete \"${exercise.exerciseName}\" and all its sets? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExercise(exercise.id)
                        showDeleteExerciseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete Exercise")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteExerciseDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}