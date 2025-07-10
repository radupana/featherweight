package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.service.ExerciseMatchingService.ExerciseMatch
import com.github.radupana.featherweight.service.ExerciseMatchingService.UnmatchedExercise

@Composable
fun UnmatchedExerciseDialog(
    unmatchedExercise: UnmatchedExercise,
    allExercises: List<Exercise>,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember {
        mutableStateOf(unmatchedExercise.aiSuggested)
    }
    var selectedEquipment by remember {
        mutableStateOf(unmatchedExercise.searchHints.detectedEquipment)
    }
    var selectedMuscle by remember {
        mutableStateOf(unmatchedExercise.searchHints.detectedMuscleGroup)
    }

    val focusManager = LocalFocusManager.current

    // Filter exercises based on search and filters
    val filteredExercises =
        remember(searchQuery, selectedEquipment, selectedMuscle) {
            allExercises.filter { exercise ->
                val matchesSearch =
                    searchQuery.isBlank() ||
                        exercise.name.contains(searchQuery, ignoreCase = true)

                val currentEquipment = selectedEquipment
                val matchesEquipment =
                    currentEquipment == null ||
                        exercise.equipment.name.equals(currentEquipment, ignoreCase = true)

                val currentMuscle = selectedMuscle
                val matchesMuscle =
                    currentMuscle == null ||
                        exercise.muscleGroup.contains(currentMuscle, ignoreCase = true)

                matchesSearch && matchesEquipment && matchesMuscle
            }
        }

    // Group exercises by best matches and regular filtered results
    val (bestMatches, otherMatches) =
        remember(filteredExercises, unmatchedExercise) {
            val bestIds = unmatchedExercise.bestMatches.map { it.exercise.id }.toSet()
            filteredExercises.partition { it.id in bestIds }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exercise Not Found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "AI suggested: \"${unmatchedExercise.aiSuggested}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = { focusManager.clearFocus() },
                        ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Equipment filter
                    unmatchedExercise.searchHints.detectedEquipment?.let { equipment ->
                        FilterChip(
                            selected = selectedEquipment == equipment,
                            onClick = {
                                selectedEquipment = if (selectedEquipment == equipment) null else equipment
                            },
                            label = { Text(equipment.capitalize()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }

                    // Muscle filter
                    unmatchedExercise.searchHints.detectedMuscleGroup?.let { muscle ->
                        FilterChip(
                            selected = selectedMuscle == muscle,
                            onClick = {
                                selectedMuscle = if (selectedMuscle == muscle) null else muscle
                            },
                            label = { Text(muscle.capitalize()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Accessibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Best matches section
                    if (bestMatches.isNotEmpty()) {
                        item {
                            Text(
                                text = "Best Matches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                        items(bestMatches) { exercise ->
                            val match = unmatchedExercise.bestMatches.find { it.exercise.id == exercise.id }
                            ExerciseSelectionCard(
                                exercise = exercise,
                                match = match,
                                onClick = { onExerciseSelected(exercise) },
                            )
                        }
                    }

                    // Other matches section
                    if (otherMatches.isNotEmpty()) {
                        item {
                            Text(
                                text = if (bestMatches.isEmpty()) "All Exercises" else "Other Exercises",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                        items(otherMatches) { exercise ->
                            ExerciseSelectionCard(
                                exercise = exercise,
                                match = null,
                                onClick = { onExerciseSelected(exercise) },
                            )
                        }
                    }

                    // Empty state
                    if (filteredExercises.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "No exercises found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Try adjusting your search or filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Info card at bottom
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Week ${unmatchedExercise.weekNumber}, Workout ${unmatchedExercise.workoutNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelectionCard(
    exercise: Exercise,
    match: ExerciseMatch?,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (match != null && match.confidence > 0.8f) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    // Equipment chip
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = exercise.equipment.displayName,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )

                    // Primary muscle chip
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = exercise.muscleGroup,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }

                // Match confidence if available
                match?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${(it.confidence * 100).toInt()}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (it.matchReasons.isNotEmpty()) {
                            Text(
                                text = "• ${it.matchReasons.first()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
