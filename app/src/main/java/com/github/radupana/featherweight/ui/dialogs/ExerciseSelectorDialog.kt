package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.ui.components.CompactSearchField
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorDialog(
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    excludeExerciseIds: Set<Long> = emptySet(),
    viewModel: ExerciseSelectorViewModel = viewModel(),
) {
    val filteredExercises by viewModel.filteredExercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isKeyboardVisible by rememberKeyboardState()
    val compactPadding = if (isKeyboardVisible) 8.dp else 16.dp

    LaunchedEffect(Unit) {
        viewModel.loadExercises()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(compactPadding),
            shape = RoundedCornerShape(if (isKeyboardVisible) 16.dp else 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header
                TopAppBar(
                    title = {
                        Text(
                            "Select Exercise",
                            style = if (isKeyboardVisible) 
                                MaterialTheme.typography.titleMedium 
                            else 
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                // Search Bar
                CompactSearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = compactPadding),
                    placeholder = "Search exercises"
                )

                if (!isKeyboardVisible) {
                    Spacer(modifier = Modifier.height(compactPadding))
                }

                // Exercise List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .systemBarsPadding(NavigationContext.DIALOG),
                        contentPadding = PaddingValues(
                            horizontal = compactPadding, 
                            vertical = compactPadding / 2
                        ),
                        verticalArrangement = Arrangement.spacedBy(compactPadding / 2),
                    ) {
                        items(
                            filteredExercises.filter { exerciseWithDetails ->
                                exerciseWithDetails.exercise.id !in excludeExerciseIds
                            }
                        ) { exerciseWithDetails ->
                            ExerciseItem(
                                exerciseWithDetails = exerciseWithDetails,
                                onClick = {
                                    onExerciseSelected(exerciseWithDetails.exercise)
                                    onDismiss()
                                },
                                isCompact = isKeyboardVisible
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseItem(
    exerciseWithDetails: ExerciseWithDetails,
    onClick: () -> Unit,
    isCompact: Boolean = false,
) {
    val exercise = exerciseWithDetails.exercise
    val itemPadding = if (isCompact) 12.dp else 16.dp

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(itemPadding),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = exercise.category.name.replace('_', ' '),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = exerciseWithDetails.exercise.muscleGroup,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
