package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.toEquipment
import com.github.radupana.featherweight.data.exercise.toExerciseDifficultyOrNull
import com.github.radupana.featherweight.data.exercise.toMuscleGroup
import com.github.radupana.featherweight.viewmodel.ExerciseSuggestion

@Composable
fun SwapModeHeader(
    currentExerciseName: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Currently selected:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = currentExerciseName ?: "Loading...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "All sets will be cleared when you swap",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun ErrorMessageCard(
    errorMessage: String,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClearError) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Clear error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search exercises...") },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            } else {
                null
            },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun ExerciseCard(
    exercise: ExerciseWithDetails,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            ExerciseCardHeader(
                exercise = exercise,
                onDelete = onDelete,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExerciseCardDetails(exercise = exercise)
        }
    }
}

@Composable
private fun ExerciseCardHeader(
    exercise: ExerciseWithDetails,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = exercise.variation.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Usage count indicator
            if (exercise.usageCount > 0) {
                UsageCountBadge(count = exercise.usageCount)
            }

            // Delete button for custom exercises
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete exercise",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCardDetails(exercise: ExerciseWithDetails) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Equipment badge
        if (exercise.variation.equipment != Equipment.BODYWEIGHT.name) {
            DetailBadge(
                text =
                    exercise.variation.equipment
                        .toEquipment()
                        .displayName,
            )
        }

        // Primary muscles badge
        val primaryMuscles = exercise.getPrimaryMuscles()
        if (primaryMuscles.isNotEmpty()) {
            DetailBadge(
                text =
                    primaryMuscles.take(2).joinToString(", ") {
                        it.toMuscleGroup().displayName
                    },
            )
        }

        // Difficulty badge
        DetailBadge(
            text =
                exercise.variation.difficulty
                    .toExerciseDifficultyOrNull()
                    ?.displayName ?: "Beginner",
        )
    }
}

@Composable
fun SuggestionCard(
    suggestion: ExerciseSuggestion,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            SuggestionCardHeader(
                suggestion = suggestion,
                onDelete = onDelete,
            )
            Spacer(modifier = Modifier.height(4.dp))
            SuggestionCardDetails(suggestion = suggestion)

            // Show suggestion reason for smart suggestions
            if (suggestion.suggestionReason.isNotEmpty() && suggestion.swapCount == 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = suggestion.suggestionReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun SuggestionCardHeader(
    suggestion: ExerciseSuggestion,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = suggestion.exercise.variation.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false),
            )

            // Swap count badge
            if (suggestion.swapCount > 0) {
                SwapCountBadge(count = suggestion.swapCount)
            }
        }

        // Delete button for custom exercises
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete exercise",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCardDetails(suggestion: ExerciseSuggestion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Equipment badge
        if (suggestion.exercise.variation.equipment != Equipment.BODYWEIGHT.name) {
            DetailBadge(
                text =
                    suggestion.exercise.variation.equipment
                        .toEquipment()
                        .displayName,
            )
        }

        // Primary muscles badge
        val primaryMuscles = suggestion.exercise.getPrimaryMuscles()
        if (primaryMuscles.isNotEmpty()) {
            DetailBadge(
                text =
                    primaryMuscles.take(2).joinToString(", ") {
                        it.toMuscleGroup().displayName
                    },
            )
        }

        // Difficulty badge
        DetailBadge(
            text =
                suggestion.exercise.variation.difficulty
                    .toExerciseDifficultyOrNull()
                    ?.displayName ?: "Beginner",
        )
    }
}

@Composable
fun DeleteExerciseDialog(
    exercise: ExerciseWithDetails,
    deleteError: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete Custom Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to delete this exercise?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    exercise.variation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                deleteError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun UsageCountBadge(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = "Usage count",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${count}x",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SwapCountBadge(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun DetailBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ExerciseListContent(
    isLoading: Boolean,
    exercises: List<ExerciseWithDetails>,
    searchQuery: String,
    isSwapMode: Boolean,
    previouslySwappedExercises: List<ExerciseSuggestion>,
    swapSuggestions: List<ExerciseSuggestion>,
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onDeleteExercise: (ExerciseWithDetails) -> Unit,
    onCreateExercise: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                LoadingContent()
            }

            exercises.isEmpty() && searchQuery.isNotEmpty() -> {
                NoResultsContent(onCreateExercise = onCreateExercise)
            }

            else -> {
                ExercisesList(
                    exercises = exercises,
                    isSwapMode = isSwapMode,
                    searchQuery = searchQuery,
                    previouslySwappedExercises = previouslySwappedExercises,
                    swapSuggestions = swapSuggestions,
                    onExerciseSelected = onExerciseSelected,
                    onDeleteExercise = onDeleteExercise,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoResultsContent(
    onCreateExercise: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                "No exercises found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try a different search or create a custom exercise",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onCreateExercise) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Exercise")
            }
        }
    }
}

@Composable
private fun ExercisesList(
    exercises: List<ExerciseWithDetails>,
    isSwapMode: Boolean,
    searchQuery: String,
    previouslySwappedExercises: List<ExerciseSuggestion>,
    swapSuggestions: List<ExerciseSuggestion>,
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onDeleteExercise: (ExerciseWithDetails) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Show suggestions when in swap mode
        if (isSwapMode) {
            // Filter suggestions based on search query
            val filteredPreviouslySwapped = filterSuggestions(previouslySwappedExercises, searchQuery)
            val filteredSwapSuggestions = filterSuggestions(swapSuggestions, searchQuery)

            // Previously swapped exercises section
            if (filteredPreviouslySwapped.isNotEmpty()) {
                item {
                    SuggestionSectionHeader(title = "Previously Swapped")
                }
                items(filteredPreviouslySwapped) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onSelect = { onExerciseSelected(suggestion.exercise) },
                        onDelete =
                            if (suggestion.exercise.isCustom) {
                                { onDeleteExercise(suggestion.exercise) }
                            } else {
                                null
                            },
                    )
                }
            }

            // Smart suggestions section
            if (filteredSwapSuggestions.isNotEmpty()) {
                item {
                    SuggestionSectionHeader(title = "Suggested Alternatives")
                }
                items(filteredSwapSuggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onSelect = { onExerciseSelected(suggestion.exercise) },
                        onDelete =
                            if (suggestion.exercise.isCustom) {
                                { onDeleteExercise(suggestion.exercise) }
                            } else {
                                null
                            },
                    )
                }
            }

            // Divider before all exercises if we have suggestions
            if (filteredPreviouslySwapped.isNotEmpty() || filteredSwapSuggestions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionSectionHeader(title = "All Exercises")
                }
            }
        }

        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                onSelect = { onExerciseSelected(exercise) },
                onDelete =
                    if (exercise.isCustom) {
                        { onDeleteExercise(exercise) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun SuggestionSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color =
            if (title == "All Exercises") {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private fun filterSuggestions(
    suggestions: List<ExerciseSuggestion>,
    searchQuery: String,
): List<ExerciseSuggestion> {
    if (searchQuery.isEmpty()) return suggestions

    val searchWords = searchQuery.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

    return suggestions.filter { suggestion ->
        val exercise = suggestion.exercise.variation
        val nameLower = exercise.name.lowercase()
        val queryLower = searchQuery.lowercase()

        // Check for exact match or contains
        if (nameLower.contains(queryLower)) {
            return@filter true
        }

        // Check individual words
        searchWords.any { searchWord ->
            val searchWordLower = searchWord.lowercase()
            nameLower.contains(searchWordLower)
        }
    }
}
