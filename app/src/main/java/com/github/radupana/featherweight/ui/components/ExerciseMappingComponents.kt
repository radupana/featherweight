package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.viewmodel.ExerciseMappingViewModel

@Composable
fun UnmatchedExerciseCard(
    exerciseName: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unmatched Exercise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Select an existing exercise or create a custom one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
fun MappingStatusCard(
    mapping: com.github.radupana.featherweight.viewmodel.ExerciseMapping?,
    onClearMapping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mapping != null) {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
            modifier = modifier,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text =
                        if (mapping.exerciseId != null) {
                            "Mapped to: ${mapping.exerciseName}"
                        } else {
                            "Will create as custom exercise"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onClearMapping,
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun ExerciseSearchSection(
    exerciseName: String,
    onExerciseSelected: (String, String) -> Unit,
    viewModel: ExerciseMappingViewModel,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember(exerciseName) { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Auto-search with the exercise name initially
    LaunchedEffect(exerciseName) {
        searchQuery = exerciseName
        viewModel.searchExercises(exerciseName)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                viewModel.searchExercises(query)
            },
            label = { Text("Search existing exercises") },
            placeholder = { Text("Type to search...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.clearSearch()
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    },
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            singleLine = true,
        )

        // Search results
        SearchResultsList(
            searchResults = searchResults,
            onExerciseSelected = { exercise ->
                onExerciseSelected(exercise.id.toString(), exercise.name)
                focusManager.clearFocus()
            },
        )

        // No results message
        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            NoResultsCard(searchQuery = searchQuery)
        }
    }
}

@Composable
private fun SearchResultsList(
    searchResults: List<com.github.radupana.featherweight.data.exercise.Exercise>,
    onExerciseSelected: (com.github.radupana.featherweight.data.exercise.Exercise) -> Unit,
) {
    AnimatedVisibility(visible = searchResults.isNotEmpty()) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(searchResults) { exercise ->
                    ExerciseSearchItem(
                        exercise = exercise,
                        onSelect = { onExerciseSelected(exercise) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseSearchItem(
    exercise: com.github.radupana.featherweight.data.exercise.Exercise,
    onSelect: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun NoResultsCard(
    searchQuery: String,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Text(
            text = "No exercises found for \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun MappingProgressIndicator(
    mappedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    if (totalCount > 1) {
        Text(
            text = "$mappedCount of $totalCount exercises mapped",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
