package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseWithDetails
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.ui.components.SearchableSelectionDialog
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel
import com.github.radupana.featherweight.viewmodel.ExerciseSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorScreen(
    onExerciseSelected: (ExerciseWithDetails) -> Unit,
    onCreateCustomExercise: (String) -> Unit,
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
        exerciseCreated?.let { exerciseName ->
            onCreateCustomExercise(exerciseName)
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
            viewModel.loadSwapSuggestions(currentExercise.exerciseVariationId)
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
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(compactPadding),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(compactPadding),
                    ) {
                        Text(
                            text = "Currently selected:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = currentSwapExerciseName ?: "Loading...",
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

            // Error handling at the top
            errorMessage?.let { error ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(compactPadding),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(compactPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
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
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    exercises.isEmpty() && searchQuery.isNotEmpty() -> {
                        // No results state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(compactPadding),
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
                                Spacer(modifier = Modifier.height(compactPadding))
                                OutlinedButton(
                                    onClick = {
                                        showCreateDialog = true
                                    },
                                ) {
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

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = compactPadding,
                                    end = compactPadding,
                                    top = compactPadding,
                                    bottom = compactPadding + navigationBarPadding.calculateBottomPadding(),
                                ),
                            verticalArrangement = Arrangement.spacedBy(compactPadding / 2),
                        ) {
                            // Show suggestions when in swap mode
                            if (isSwapMode) {
                                // Filter suggestions based on search query
                                val filteredPreviouslySwapped = filterSuggestions(previouslySwappedExercises, searchQuery)
                                val filteredSwapSuggestions = filterSuggestions(swapSuggestions, searchQuery)

                                // Previously swapped exercises section
                                if (filteredPreviouslySwapped.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Previously Swapped",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = compactPadding / 2),
                                        )
                                    }
                                    items(filteredPreviouslySwapped) { suggestion ->
                                        SuggestionCard(
                                            suggestion = suggestion,
                                            onSelect = { onExerciseSelected(suggestion.exercise) },
                                            onDelete = if (suggestion.exercise.variation.isCustom) {
                                                { viewModel.requestDeleteExercise(suggestion.exercise) }
                                            } else null
                                        )
                                    }
                                }

                                // Smart suggestions section
                                if (filteredSwapSuggestions.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Suggested Alternatives",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = compactPadding / 2),
                                        )
                                    }
                                    items(filteredSwapSuggestions) { suggestion ->
                                        SuggestionCard(
                                            suggestion = suggestion,
                                            onSelect = { onExerciseSelected(suggestion.exercise) },
                                            onDelete = if (suggestion.exercise.variation.isCustom) {
                                                { viewModel.requestDeleteExercise(suggestion.exercise) }
                                            } else null
                                        )
                                    }
                                }

                                // Divider before all exercises if we have suggestions
                                if (filteredPreviouslySwapped.isNotEmpty() || filteredSwapSuggestions.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(compactPadding))
                                        Text(
                                            text = "All Exercises",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = compactPadding / 2),
                                        )
                                    }
                                }
                            }

                            items(exercises) { exercise ->
                                ExerciseCard(
                                    exercise = exercise,
                                    onSelect = { onExerciseSelected(exercise) },
                                    onDelete = if (exercise.variation.isCustom) {
                                        { viewModel.requestDeleteExercise(exercise) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
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
                    primaryMuscles = primaryMuscles,
                    secondaryMuscles = secondaryMuscles,
                    equipment = equipment,
                    difficulty = difficulty,
                    requiresWeight = requiresWeight
                )
                showCreateDialog = false
                viewModel.clearNameValidation()
                // Success will be handled by LaunchedEffect above
            },
        )
    }
    
    // Delete Confirmation Dialog
    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
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
                            colors = CardDefaults.cardColors(
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
                    onClick = { viewModel.confirmDeleteExercise() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            },
        )
    }
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

@Composable
private fun SearchBar(
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
private fun ExerciseCard(
    exercise: ExerciseWithDetails,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cardPadding = 12.dp // Keep consistent padding
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Exercise name
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
                    // Usage count indicator - only show if > 0
                    if (exercise.variation.usageCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = "${exercise.variation.usageCount}×",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    
                    // Delete button for custom exercises
                    if (onDelete != null) {
                        IconButton(
                            onClick = { 
                                onDelete()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete exercise",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Details row with equipment and muscles
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Equipment badge
                if (exercise.variation.equipment != Equipment.BODYWEIGHT) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = exercise.variation.equipment.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Primary muscles badge (if available)
                val primaryMuscles = exercise.getPrimaryMuscles()
                if (primaryMuscles.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = primaryMuscles.take(2).joinToString(", ") { it.displayName },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Difficulty badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = exercise.variation.difficulty.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCustomExerciseDialog(
    initialName: String = "",
    viewModel: ExerciseSelectorViewModel,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        category: ExerciseCategory,
        primaryMuscles: Set<MuscleGroup>,
        secondaryMuscles: Set<MuscleGroup>,
        equipment: Set<Equipment>,
        difficulty: ExerciseDifficulty,
        requiresWeight: Boolean
    ) -> Unit,
) {
    var exerciseName by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    var selectedPrimaryMuscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var selectedSecondaryMuscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var selectedEquipment by remember { mutableStateOf(setOf<Equipment>()) }
    var selectedDifficulty by remember { mutableStateOf(ExerciseDifficulty.BEGINNER) }
    var requiresWeight by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }
    
    // Dialog states for searchable selections
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPrimaryMusclesDialog by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var showSecondaryMusclesDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    
    // Get validation state from ViewModel
    val nameValidationError by viewModel.nameValidationError.collectAsState()
    val nameSuggestion by viewModel.nameSuggestion.collectAsState()
    
    // Validate name when it changes
    LaunchedEffect(exerciseName) {
        viewModel.validateExerciseName(exerciseName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Custom Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            label = { Text("Exercise name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = nameValidationError != null || exerciseName.isBlank(),
                            supportingText = {
                                if (nameValidationError != null) {
                                    Text(
                                        text = nameValidationError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "Format: [Equipment] [Muscle] [Movement]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category * (Required)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = selectedCategory?.displayName ?: "Select category",
                                color = if (selectedCategory == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Primary Muscles * (Required - Select at least one)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showPrimaryMusclesDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = if (selectedPrimaryMuscles.isEmpty()) {
                                    "Select primary muscles"
                                } else {
                                    selectedPrimaryMuscles.joinToString(", ") { it.displayName }
                                },
                                color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Equipment * (Required - Select at least one)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showEquipmentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = if (selectedEquipment.isEmpty()) {
                                    "Select equipment"
                                } else {
                                    selectedEquipment.joinToString(", ") { it.displayName }
                                },
                                color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Show advanced options (Optional)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = showAdvanced,
                            onCheckedChange = { showAdvanced = it },
                        )
                    }
                }

                if (showAdvanced) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Secondary Muscles (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            OutlinedButton(
                                onClick = { showSecondaryMusclesDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (selectedSecondaryMuscles.isEmpty()) {
                                        "Select secondary muscles"
                                    } else {
                                        selectedSecondaryMuscles.joinToString(", ") { it.displayName }
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Difficulty",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            OutlinedButton(
                                onClick = { showDifficultyDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedDifficulty.displayName)
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Requires weight",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Turn off for bodyweight exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = requiresWeight,
                                onCheckedChange = { requiresWeight = it },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (exerciseName.isNotBlank() && 
                        nameValidationError == null && 
                        selectedCategory != null &&
                        selectedPrimaryMuscles.isNotEmpty() &&
                        selectedEquipment.isNotEmpty()) {
                        onCreate(
                            exerciseName,
                            selectedCategory!!,
                            selectedPrimaryMuscles,
                            selectedSecondaryMuscles,
                            selectedEquipment,
                            selectedDifficulty,
                            requiresWeight
                        )
                    }
                },
                enabled = exerciseName.isNotBlank() && 
                          nameValidationError == null &&
                          selectedCategory != null &&
                          selectedPrimaryMuscles.isNotEmpty() &&
                          selectedEquipment.isNotEmpty(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
    
    // Category Selection Dialog
    if (showCategoryDialog) {
        SearchableSelectionDialog(
            title = "Select Category",
            items = ExerciseCategory.values().toList(),
            selectedItem = selectedCategory,
            itemLabel = { it.displayName },
            searchHint = "Search categories...",
            multiSelect = false,
            required = true,
            onDismiss = { showCategoryDialog = false },
            onSelect = { category ->
                selectedCategory = category
                showCategoryDialog = false
            }
        )
    }
    
    // Primary Muscles Selection Dialog
    if (showPrimaryMusclesDialog) {
        SearchableSelectionDialog(
            title = "Select Primary Muscles",
            items = MuscleGroup.values().toList(),
            selectedItems = selectedPrimaryMuscles,
            itemLabel = { it.displayName },
            searchHint = "Search muscles...",
            multiSelect = true,
            required = true,
            onDismiss = { showPrimaryMusclesDialog = false },
            onConfirm = { muscles ->
                selectedPrimaryMuscles = muscles
                // Remove any selected primary muscles from secondary
                selectedSecondaryMuscles = selectedSecondaryMuscles - muscles
                showPrimaryMusclesDialog = false
            }
        )
    }
    
    // Equipment Selection Dialog
    if (showEquipmentDialog) {
        SearchableSelectionDialog(
            title = "Select Equipment",
            items = Equipment.values().toList(),
            selectedItems = selectedEquipment,
            itemLabel = { it.displayName },
            searchHint = "Search equipment...",
            multiSelect = true,
            required = true,
            onDismiss = { showEquipmentDialog = false },
            onConfirm = { equipment ->
                selectedEquipment = equipment
                showEquipmentDialog = false
            }
        )
    }
    
    // Secondary Muscles Selection Dialog
    if (showSecondaryMusclesDialog) {
        val availableSecondaryMuscles = MuscleGroup.values().filter { it !in selectedPrimaryMuscles }
        SearchableSelectionDialog(
            title = "Select Secondary Muscles",
            items = availableSecondaryMuscles,
            selectedItems = selectedSecondaryMuscles,
            itemLabel = { it.displayName },
            searchHint = "Search muscles...",
            multiSelect = true,
            required = false,
            onDismiss = { showSecondaryMusclesDialog = false },
            onConfirm = { muscles ->
                selectedSecondaryMuscles = muscles
                showSecondaryMusclesDialog = false
            }
        )
    }
    
    // Difficulty Selection Dialog
    if (showDifficultyDialog) {
        SearchableSelectionDialog(
            title = "Select Difficulty",
            items = ExerciseDifficulty.values().toList(),
            selectedItem = selectedDifficulty,
            itemLabel = { it.displayName },
            searchHint = "Search difficulty...",
            multiSelect = false,
            required = false,
            onDismiss = { showDifficultyDialog = false },
            onSelect = { difficulty ->
                selectedDifficulty = difficulty
                showDifficultyDialog = false
            }
        )
    }
}

@Composable
private fun SuggestionCard(
    suggestion: ExerciseSuggestion,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val cardPadding = 12.dp // Keep consistent padding
    Card(
        modifier =
            Modifier
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
            modifier = Modifier.padding(cardPadding),
        ) {
            // Top row: Exercise name and swap count badge
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

                    // Swap count badge for previously swapped exercises
                    if (suggestion.swapCount > 0) {
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
                                    text = "${suggestion.swapCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
                
                // Delete button for custom exercises
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete exercise",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Details row with equipment, muscles, and reason
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Equipment badge
                if (suggestion.exercise.variation.equipment != Equipment.BODYWEIGHT) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = suggestion.exercise.variation.equipment.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Primary muscles badge
                val primaryMuscles = suggestion.exercise.getPrimaryMuscles()
                if (primaryMuscles.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = primaryMuscles.take(2).joinToString(", ") { it.displayName },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Difficulty badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = suggestion.exercise.variation.difficulty.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            
            // Show suggestion reason only for smart suggestions (not for previously swapped)
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
