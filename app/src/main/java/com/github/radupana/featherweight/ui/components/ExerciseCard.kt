package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.ui.theme.FeatherweightColors
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.ui.theme.GradientProgressIndicator
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

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (viewModel.canEditWorkout()) {
            { onEditSets() }
        } else null,
        elevation = 8.dp
    ) {
        // Delete confirmation dialog
        if (showDeleteExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteExerciseDialog = false },
                icon = {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete Exercise?") },
                text = {
                    Text(
                        "Are you sure you want to delete \"${exercise.exerciseName}\" and all its sets? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteExercise(exercise.id)
                            showDeleteExerciseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteExerciseDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Exercise header with improved visual hierarchy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (sets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedSets of ${sets.size} sets completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Modern status indicator
            if (sets.isNotEmpty()) {
                val progress = completedSets.toFloat() / sets.size
                val isComplete = completedSets == sets.size
                
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isComplete) {
                        FeatherweightColors.successGradientStart.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isComplete) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Complete",
                                tint = FeatherweightColors.successGradientStart,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                }
            }
            
            // Delete option for editable workouts
            if (viewModel.canEditWorkout()) {
                IconButton(
                    onClick = { showDeleteExerciseDialog = true }
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sets.isEmpty()) {
            // Modern empty state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to start training",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap to add your first set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            // Modern progress visualization
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val progressPercentage = (completedSets.toFloat() / sets.size * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (progressPercentage == 100) {
                            FeatherweightColors.successGradientStart.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = "$progressPercentage%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (progressPercentage == 100) {
                                FeatherweightColors.successGradientStart
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                GradientProgressIndicator(
                    progress = completedSets.toFloat() / sets.size,
                    modifier = Modifier.fillMaxWidth(),
                    strokeWidth = 6.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modern metrics cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    icon = "📊",
                    value = if (totalVolume > 0) "${totalVolume.toInt()}kg" else "—",
                    label = "Volume",
                    modifier = Modifier.weight(1f)
                )
                
                MetricCard(
                    icon = "💪",
                    value = if (bestSet != null) "${bestSet.reps}×${bestSet.weight}kg" else "—",
                    label = "Best Set",
                    modifier = Modifier.weight(1f)
                )
                
                MetricCard(
                    icon = "⚡",
                    value = if (avgRpe != null) {
                        if (avgRpe == avgRpe.toInt().toFloat()) {
                            avgRpe.toInt().toString()
                        } else {
                            String.format("%.1f", avgRpe)
                        }
                    } else "—",
                    label = "Avg RPE",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Interactive hint for editable workouts
        if (viewModel.canEditWorkout()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to edit sets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}