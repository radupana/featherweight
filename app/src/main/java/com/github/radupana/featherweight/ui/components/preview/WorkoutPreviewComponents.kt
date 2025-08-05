package com.github.radupana.featherweight.ui.components.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseAlternative
import com.github.radupana.featherweight.data.ExerciseEditState
import com.github.radupana.featherweight.data.ExercisePreview
import com.github.radupana.featherweight.data.QuickEditAction
import com.github.radupana.featherweight.data.WorkoutPreview

@Composable
fun WorkoutPreviewCard(
    workout: WorkoutPreview,
    editStates: Map<String, ExerciseEditState>,
    onExerciseResolved: (String, Long) -> Unit,
    onExerciseSwapped: (String, String) -> Unit,
    onExerciseUpdated: (QuickEditAction.UpdateExercise) -> Unit,
    onToggleEdit: (String) -> Unit,
    onShowAlternatives: (String, Boolean) -> Unit,
    onShowResolution: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Workout Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "${workout.estimatedDuration} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        Text(
                            text = "${workout.exercises.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        Text(
                            text = "${workout.exercises.sumOf { it.sets }} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        workout.targetRPE?.let { rpe ->
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "RPE $rpe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            // Exercises
            workout.exercises.forEach { exercise ->
                ExercisePreviewCard(
                    exercise = exercise,
                    editState = editStates[exercise.tempId],
                    onUpdated = { update ->
                        onExerciseUpdated(update.copy(tempId = exercise.tempId))
                    },
                    onToggleEdit = { onToggleEdit(exercise.tempId) },
                )
            }
        }
    }
}

@Composable
fun ExercisePreviewCard(
    exercise: ExercisePreview,
    editState: ExerciseEditState?,
    onUpdated: (QuickEditAction.UpdateExercise) -> Unit,
    onToggleEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        exercise.matchConfidence >= 0.85f -> MaterialTheme.colorScheme.primary
        exercise.matchConfidence >= 0.7f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val isEditing = editState?.isEditing == true
    editState?.showAlternatives == true
    editState?.showResolution == true

    exercise.matchConfidence < 0.7f
    exercise.matchConfidence < 0.8f && exercise.matchConfidence >= 0.7f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Match confidence indicator removed - all exercises are accepted as-is
                        /*
                        ExerciseMatchIndicator(
                            confidence = exercise.matchConfidence,
                            color = confidenceColor,
                            onClick = {
                                if (exercise.matchConfidence < 0.85f) {
                                    onShowResolution(!showResolution)
                                }
                            }
                        )
                         */
                    }

                    // Exercise resolution UI removed - all exercises are accepted as-is
                    /*
                    if (needsAttention) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Needs resolution - click to select exercise",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (lowConfidence) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Low confidence match - review recommended",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                     */
                }

                // Edit button
                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Exercise Details
            if (isEditing) {
                ExerciseEditForm(
                    exercise = exercise,
                    onUpdated = onUpdated,
                )
            } else {
                ExerciseDisplayInfo(exercise = exercise)
            }

            // Resolution UI removed - all exercises are accepted as-is
            /*
            AnimatedVisibility(
                visible = showResolution && exercise.matchConfidence < 0.85f,
                enter = slideInVertically() + expandVertically(),
                exit = slideOutVertically() + shrinkVertically()
            ) {
                ExerciseResolutionSection(
                    exercise = exercise,
                    onResolved = onResolved,
                    onSwapped = onSwapped,
                    onShowAlternatives = onShowAlternatives
                )
            }

            // Alternatives section
            AnimatedVisibility(
                visible = showAlternatives && exercise.alternatives.isNotEmpty(),
                enter = slideInVertically() + expandVertically(),
                exit = slideOutVertically() + shrinkVertically()
            ) {
                ExerciseAlternativesSection(
                    alternatives = exercise.alternatives,
                    onSelected = { alternative ->
                        onResolved(alternative.exerciseId)
                        onShowAlternatives(false)
                    }
                )
            }
             */
        }
    }
}

@Composable
private fun ExerciseMatchIndicator(
    confidence: Float,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val text =
        when {
            confidence >= 0.95f -> "✓"
            confidence >= 0.85f -> "!"
            confidence >= 0.7f -> "?"
            else -> "✗"
        }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        modifier =
            modifier.then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ExerciseDisplayInfo(
    exercise: ExercisePreview,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sets x Reps
        Text(
            text =
                buildString {
                    append("${exercise.sets} sets × ")
                    if (exercise.repsMin == exercise.repsMax) {
                        append("${exercise.repsMin} reps")
                    } else {
                        append("${exercise.repsMin}-${exercise.repsMax} reps")
                    }
                },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )

        // Weight, RPE and Rest
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Suggested Weight
            exercise.suggestedWeight?.let { weight ->
                Text(
                    text = "${weight}kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            exercise.rpe?.let { rpe ->
                Text(
                    text = "RPE $rpe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Text(
                text = "${exercise.restSeconds / 60}:${(exercise.restSeconds % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }

    // Notes
    exercise.notes?.let { notes ->
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ExerciseEditForm(
    exercise: ExercisePreview,
    onUpdated: (QuickEditAction.UpdateExercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sets by remember { mutableStateOf(exercise.sets.toString()) }
    var repsMin by remember { mutableStateOf(exercise.repsMin.toString()) }
    var repsMax by remember { mutableStateOf(exercise.repsMax.toString()) }
    var weight by remember { mutableStateOf(exercise.suggestedWeight?.toString() ?: "") }
    var rpe by remember { mutableStateOf(exercise.rpe?.toString() ?: "") }
    var restSeconds by remember { mutableStateOf(exercise.restSeconds.toString()) }

    LaunchedEffect(sets, repsMin, repsMax, weight, rpe, restSeconds) {
        try {
            onUpdated(
                QuickEditAction.UpdateExercise(
                    tempId = "", // Will be set by caller
                    sets = sets.toIntOrNull(),
                    repsMin = repsMin.toIntOrNull(),
                    repsMax = repsMax.toIntOrNull(),
                    suggestedWeight = weight.toFloatOrNull(),
                    rpe = rpe.toFloatOrNull(),
                    restSeconds = restSeconds.toIntOrNull(),
                ),
            )
        } catch (e: Exception) {
            // Ignore invalid input
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Sets") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = repsMin,
                onValueChange = { repsMin = it },
                label = { Text("Min Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = repsMax,
                onValueChange = { repsMax = it },
                label = { Text("Max Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = rpe,
                onValueChange = { rpe = it },
                label = { Text("RPE") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = restSeconds,
                onValueChange = { restSeconds = it },
                label = { Text("Rest (sec)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExerciseResolutionSection(
    exercise: ExercisePreview,
    onResolved: (Long) -> Unit,
    onShowAlternatives: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp),
                ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Exercise match needs confirmation",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Text(
            text = "Match confidence: ${(exercise.matchConfidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (exercise.matchedExerciseId != null) {
                OutlinedButton(
                    onClick = { onResolved(exercise.matchedExerciseId) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Confirm Match", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (exercise.alternatives.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onShowAlternatives(true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("See Alternatives", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ExerciseAlternativesSection(
    alternatives: List<ExerciseAlternative>,
    onSelected: (ExerciseAlternative) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp),
                ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Alternative Exercises:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )

        alternatives.forEach { alternative ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(alternative) }
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(6.dp),
                        ).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alternative.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = alternative.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                Text(
                    text = "${(alternative.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
