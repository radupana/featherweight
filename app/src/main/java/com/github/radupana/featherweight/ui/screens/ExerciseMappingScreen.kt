package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.viewmodel.ExerciseMappingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseMappingScreen(
    unmatchedExercises: List<String>,
    onMappingComplete: (Map<String, Long?>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseMappingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentExerciseIndex by remember { mutableStateOf(0) }
    val currentExercise = unmatchedExercises.getOrNull(currentExerciseIndex)
    
    // Initialize mappings
    LaunchedEffect(unmatchedExercises) {
        viewModel.initializeMappings(unmatchedExercises)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Map Exercises (${currentExerciseIndex + 1}/${unmatchedExercises.size})") 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (currentExercise != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card showing the unmatched exercise
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unmatched Exercise",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = currentExercise,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Select an existing exercise or create a custom one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                // Current mapping status
                val currentMapping = uiState.mappings[currentExercise]
                if (currentMapping != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentMapping.exerciseId != null) {
                                    "Mapped to: ${currentMapping.exerciseName}"
                                } else {
                                    "Will create as custom exercise"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { 
                                    viewModel.clearMapping(currentExercise) 
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
                
                // Search section
                ExerciseSearchSection(
                    exerciseName = currentExercise,
                    onExerciseSelected = { exerciseId, exerciseName ->
                        viewModel.mapExercise(currentExercise, exerciseId, exerciseName)
                    },
                    viewModel = viewModel
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Create as custom button
                    OutlinedButton(
                        onClick = {
                            viewModel.mapExercise(currentExercise, null, currentExercise)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentMapping == null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create as Custom Exercise")
                    }
                    
                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Previous button
                        OutlinedButton(
                            onClick = { 
                                if (currentExerciseIndex > 0) {
                                    currentExerciseIndex--
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = currentExerciseIndex > 0
                        ) {
                            Text("Previous")
                        }
                        
                        // Next/Complete button
                        Button(
                            onClick = {
                                if (currentExerciseIndex < unmatchedExercises.size - 1) {
                                    currentExerciseIndex++
                                } else {
                                    // All exercises processed, check if all are mapped
                                    if (viewModel.allExercisesMapped(unmatchedExercises)) {
                                        onMappingComplete(viewModel.getFinalMappings())
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = currentMapping != null
                        ) {
                            Text(
                                if (currentExerciseIndex < unmatchedExercises.size - 1) "Next" 
                                else "Complete"
                            )
                        }
                    }
                    
                    // Progress indicator
                    if (unmatchedExercises.size > 1) {
                        val mappedCount = uiState.mappings.size
                        Text(
                            text = "$mappedCount of ${unmatchedExercises.size} exercises mapped",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSearchSection(
    exerciseName: String,
    onExerciseSelected: (Long, String) -> Unit,
    viewModel: ExerciseMappingViewModel,
    modifier: Modifier = Modifier
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true
        )
        
        // Search results
        AnimatedVisibility(visible = searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { exercise ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onExerciseSelected(exercise.id, exercise.name)
                                    focusManager.clearFocus()
                                },
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    )
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        
        // No results message
        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No exercises found for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
