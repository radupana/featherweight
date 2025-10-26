package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.github.radupana.featherweight.ui.components.CategoryFilterChips
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
    val exerciseCreated by viewModel.exerciseCreated.collectAsState()
    val exerciseToDelete by viewModel.exerciseToDelete.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    // Handle successful exercise creation
    LaunchedEffect(exerciseCreated) {
        exerciseCreated?.let { exercise ->
            val exerciseWithDetails =
                ExerciseWithDetails(
                    variation = exercise,
                    muscles = emptyList(),
                    aliases = emptyList(),
                    instructions = emptyList(),
                )
            onExerciseSelected(exerciseWithDetails)
            viewModel.clearExerciseCreated()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExercises()
    }

    // Load swap suggestions when in swap mode
    LaunchedEffect(isSwapMode, currentExercise) {
        if (isSwapMode && currentExercise != null) {
            viewModel.clearSearchQuery()
            viewModel.clearSwapSuggestions()
            viewModel.loadSwapSuggestions(currentExercise.exerciseId)
        } else if (!isSwapMode) {
            viewModel.clearSwapSuggestions()
        }
    }

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
        ExerciseSelectorContent(
            isSwapMode = isSwapMode,
            currentExercise = currentExercise,
            onExerciseSelected = onExerciseSelected,
            onDeleteExercise = viewModel::requestDeleteExercise,
            onCreateExercise = { showCreateDialog = true },
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }

    ExerciseSelectorDialogs(
        showCreateDialog = showCreateDialog,
        searchQuery = searchQuery,
        exerciseToDelete = exerciseToDelete,
        deleteError = deleteError,
        viewModel = viewModel,
        onDismissCreateDialog = { showCreateDialog = false },
    )
}

@Composable
private fun ExerciseSelectorContent(
    isSwapMode: Boolean,
    currentExercise: ExerciseLog?,
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onDeleteExercise: (ExerciseWithDetails) -> Unit,
    onCreateExercise: () -> Unit,
    viewModel: ExerciseSelectorViewModel,
    modifier: Modifier = Modifier,
) {
    val compactPadding = 16.dp
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Collect all needed states
    val exercises by viewModel.filteredExercises.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val swapSuggestions by viewModel.swapSuggestions.collectAsState()
    val previouslySwappedExercises by viewModel.previouslySwappedExercises.collectAsState()
    val currentSwapExerciseName by viewModel.currentSwapExerciseName.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Show current exercise when in swap mode
        if (isSwapMode && currentExercise != null) {
            SwapModeHeader(
                currentExerciseName = currentSwapExerciseName,
                modifier = Modifier.padding(compactPadding),
            )
        }

        // Error handling
        errorMessage?.let { error ->
            ErrorMessageCard(
                errorMessage = error,
                onClearError = viewModel::clearError,
                modifier = Modifier.padding(compactPadding),
            )
        }

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier.padding(compactPadding),
        )

        // Category Filter Chips
        CategoryFilterChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                viewModel.selectCategory(category)
            },
            modifier = Modifier.padding(bottom = compactPadding),
        )

        // Exercise List
        ExerciseListContent(
            isLoading = isLoading,
            exercises = exercises,
            searchQuery = searchQuery,
            isSwapMode = isSwapMode,
            previouslySwappedExercises = previouslySwappedExercises,
            swapSuggestions = swapSuggestions,
            onExerciseSelected = onExerciseSelected,
            onDeleteExercise = onDeleteExercise,
            onCreateExercise = onCreateExercise,
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

@Composable
private fun ExerciseSelectorDialogs(
    showCreateDialog: Boolean,
    searchQuery: String,
    exerciseToDelete: ExerciseWithDetails?,
    deleteError: String?,
    viewModel: ExerciseSelectorViewModel,
    onDismissCreateDialog: () -> Unit,
) {
    if (showCreateDialog) {
        CreateCustomExerciseDialog(
            initialName = searchQuery,
            viewModel = viewModel,
            onDismiss = {
                onDismissCreateDialog()
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
                onDismissCreateDialog()
                viewModel.clearNameValidation()
            },
        )
    }

    exerciseToDelete?.let { exercise ->
        DeleteExerciseDialog(
            exercise = exercise,
            deleteError = deleteError,
            onConfirm = viewModel::confirmDeleteExercise,
            onDismiss = viewModel::cancelDelete,
        )
    }
}
