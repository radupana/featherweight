package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.ui.components.CreateCustomExerciseDialog
import com.github.radupana.featherweight.ui.components.DeleteExerciseDialog
import com.github.radupana.featherweight.ui.components.ErrorMessageCard
import com.github.radupana.featherweight.ui.components.ExerciseListContent
import com.github.radupana.featherweight.ui.components.SearchBar
import com.github.radupana.featherweight.ui.components.SwapModeHeader
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorScreen(
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseSelectorViewModel = viewModel(),
    isSwapMode: Boolean = false,
    currentExercise: ExerciseLog? = null,
) {
    val exercises by viewModel.filteredExercises.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val exerciseCreated by viewModel.exerciseCreated.collectAsState()
    val swapSuggestions by viewModel.swapSuggestions.collectAsState()
    val previouslySwappedExercises by viewModel.previouslySwappedExercises.collectAsState()
    val currentSwapExerciseName by viewModel.currentSwapExerciseName.collectAsState()
    val exerciseToDelete by viewModel.exerciseToDelete.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    // Handle successful exercise creation
    LaunchedEffect(exerciseCreated) {
        exerciseCreated?.let { exercise ->
            // Convert to ExerciseWithDetails and pass to the regular selection handler
            val exerciseWithDetails =
                ExerciseWithDetails(
                    variation = exercise,
                    muscles = emptyList(), // These will be loaded separately
                    aliases = emptyList(),
                    instructions = emptyList(),
                    // isCustom is now a derived property based on variation.userId
                )
            onExerciseSelected(exerciseWithDetails)
            viewModel.clearExerciseCreated()
        }
    }

    val compactPadding = 16.dp // Keep consistent padding
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.loadExercises()
    }

    // Load swap suggestions when in swap mode
    LaunchedEffect(isSwapMode, currentExercise) {
        if (isSwapMode && currentExercise != null) {
            viewModel.clearSearchQuery() // Clear search when entering swap mode
            viewModel.clearSwapSuggestions() // Clear previous suggestions
            viewModel.loadSwapSuggestions(currentExercise.exerciseId)
        } else if (!isSwapMode) {
            // Clear suggestions when not in swap mode
            viewModel.clearSwapSuggestions()
        }
    }

    // For now, let's use a simpler approach without the adaptive layout
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSwapMode) "Swap Exercise" else "Select Exercise",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Exercise")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Show current exercise when in swap mode
            if (isSwapMode && currentExercise != null) {
                SwapModeHeader(
                    currentExerciseName = currentSwapExerciseName,
                    modifier = Modifier.padding(compactPadding),
                )
            }

            // Error handling at the top
            errorMessage?.let { error ->
                ErrorMessageCard(
                    errorMessage = error,
                    onClearError = viewModel::clearError,
                    modifier = Modifier.padding(compactPadding),
                )
            }

            // Search Bar - always visible
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.padding(compactPadding),
            )

            // Filter Chips - always visible
            LazyRow(
                contentPadding = PaddingValues(horizontal = compactPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = compactPadding),
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            viewModel.selectCategory(
                                if (selectedCategory == category) null else category,
                            )
                        },
                        label = {
                            Text(
                                category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            // Exercise List - fills remaining space
            ExerciseListContent(
                isLoading = isLoading,
                exercises = exercises,
                searchQuery = searchQuery,
                isSwapMode = isSwapMode,
                previouslySwappedExercises = previouslySwappedExercises,
                swapSuggestions = swapSuggestions,
                onExerciseSelected = onExerciseSelected,
                onDeleteExercise = viewModel::requestDeleteExercise,
                onCreateExercise = { showCreateDialog = true },
                contentPadding =
                    PaddingValues(
                        start = compactPadding,
                        end = compactPadding,
                        top = compactPadding,
                        bottom = compactPadding + navigationBarPadding.calculateBottomPadding(),
                    ),
            )
        }
    }

    // Create Custom Exercise Dialog
    if (showCreateDialog) {
        CreateCustomExerciseDialog(
            initialName = searchQuery,
            viewModel = viewModel,
            onDismiss = {
                showCreateDialog = false
                viewModel.clearNameValidation()
            },
            onCreate = { name, category, primaryMuscles, secondaryMuscles, equipment, difficulty, requiresWeight ->
                viewModel.createCustomExercise(
                    name = name,
                    category = category,
                    equipment = equipment,
                    difficulty = difficulty,
                    requiresWeight = requiresWeight,
                )
                showCreateDialog = false
                viewModel.clearNameValidation()
                // Success will be handled by LaunchedEffect above
            },
        )
    }

    // Delete Confirmation Dialog
    exerciseToDelete?.let { exercise ->
        DeleteExerciseDialog(
            exercise = exercise,
            deleteError = deleteError,
            onConfirm = viewModel::confirmDeleteExercise,
            onDismiss = viewModel::cancelDelete,
        )
    }
}
