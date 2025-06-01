package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
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
import kotlinx.coroutines.launch

@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    sets: List<SetLog>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onAddSet: () -> Unit,
    onEditSet: (SetLog) -> Unit,
    onCopyLastSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onSmartAdd: (Long, String) -> Unit,
    onUpdateSet: ((Long, Int, Float, Float?) -> Unit)? = null, // Add callback for inline updates
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    // Check if smart suggestions are available for this exercise
    var hasSmartSuggestions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Check for smart suggestions when card expands
    LaunchedEffect(expanded, exercise.exerciseName) {
        if (expanded) {
            scope.launch {
                val suggestions = viewModel.getSmartSuggestions(exercise.exerciseName)
                hasSmartSuggestions = suggestions != null
                println("🔍 Smart suggestions for ${exercise.exerciseName}: $suggestions") // Debug log
            }
        } else {
            hasSmartSuggestions = false // Reset when collapsed
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (sets.isNotEmpty()) {
                    val completedSets = sets.count { it.isCompleted }
                    Surface(
                        color = if (completedSets == sets.size)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$completedSets/${sets.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completedSets == sets.size)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (sets.isEmpty()) {
                    Text(
                        "No sets yet. Add your first set below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Sets header
                    SetTableHeader()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sets list
                    sets.forEach { set ->
                        SetRow(
                            set = set,
                            onToggleCompleted = { completed ->
                                viewModel.markSetCompleted(set.id, completed)
                            },
                            onEdit = { onEditSet(set) },
                            onDelete = { onDeleteSet(set.id) },
                            onUpdateSet = { reps, weight, rpe ->
                                viewModel.updateSet(set.id, reps, weight, rpe)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                ExerciseActionButtons(
                    hasExistingSets = sets.isNotEmpty(),
                    hasSmartSuggestions = hasSmartSuggestions,
                    onSmartAdd = { onSmartAdd(exercise.id, exercise.exerciseName) },
                    onCopyLast = { onCopyLastSet(exercise.id) },
                    onAddSet = onAddSet
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (sets.isEmpty()) "Tap to add sets" else "${sets.size} sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Set",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.12f)
        )
        Text(
            "Reps",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.18f),
            textAlign = TextAlign.Center
        )
        Text(
            "Weight",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.22f),
            textAlign = TextAlign.Center
        )
        Text(
            "RPE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.15f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(0.15f)) // For checkbox
        Spacer(modifier = Modifier.weight(0.18f)) // For actions
    }
}

@Composable
private fun ExerciseActionButtons(
    hasExistingSets: Boolean,
    hasSmartSuggestions: Boolean,
    onSmartAdd: () -> Unit,
    onCopyLast: () -> Unit,
    onAddSet: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Smart Add button (only if suggestions available)
        if (hasSmartSuggestions) {
            Button(
                onClick = onSmartAdd,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Lightbulb,
                    contentDescription = "Smart Add - Uses data from previous workouts",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Smart Add")
            }
        } else {
            // Regular add button when no smart suggestions
            Button(
                onClick = onAddSet,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add Set",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Set")
            }
        }

        // Copy Last Set button (if sets exist in current workout)
        if (hasExistingSets) {
            OutlinedButton(
                onClick = onCopyLast,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy Last - Duplicates your previous set",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Last")
            }
        } else if (!hasSmartSuggestions) {
            // Empty space to maintain layout when no smart suggestions and no existing sets
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}