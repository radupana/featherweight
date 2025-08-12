package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.ui.components.CompactSearchField
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorDialog(
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onDismiss: () -> Unit,
    excludeExerciseIds: Set<Long> = emptySet(),
    viewModel: ExerciseSelectorViewModel = viewModel(),
) {
    val filteredExercises by viewModel.filteredExercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    // Remove keyboard state to prevent layout shifts
    val compactPadding = 16.dp

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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(compactPadding)
                    .systemBarsPadding(NavigationContext.DIALOG),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )

                // Search Bar
                CompactSearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = compactPadding),
                    placeholder = "Search exercises",
                )

                Spacer(modifier = Modifier.height(compactPadding))

                // Exercise List
                if (isLoading) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentPadding =
                            PaddingValues(
                                horizontal = compactPadding,
                                vertical = compactPadding / 2,
                            ),
                        verticalArrangement = Arrangement.spacedBy(compactPadding / 2),
                    ) {
                        items(
                            filteredExercises.filter { exerciseWithDetails ->
                                exerciseWithDetails.variation.id !in excludeExerciseIds
                            },
                        ) { exerciseWithDetails ->
                            ExerciseItem(
                                exerciseWithDetails = exerciseWithDetails,
                                onClick = {
                                    onExerciseSelected(exerciseWithDetails)
                                    onDismiss()
                                },
                                isCompact = false,
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
    val exercise = exerciseWithDetails.variation
    val itemPadding = 16.dp

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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(itemPadding),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
