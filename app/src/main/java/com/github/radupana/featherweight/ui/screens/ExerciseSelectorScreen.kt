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
                                        if (searchQuery.isNotEmpty()) {
                                            viewModel.createCustomExercise(searchQuery)
                                        } else {
                                            showCreateDialog = true
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (searchQuery.isNotEmpty()) "Create \"$searchQuery\"" else "Create Exercise")
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
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createCustomExercise(name)
                showCreateDialog = false
                // Success will be handled by LaunchedEffect above
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
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var exerciseName by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.FULL_BODY) }
    var selectedPrimaryMuscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var selectedSecondaryMuscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var selectedEquipment by remember { mutableStateOf(setOf<Equipment>()) }
    var selectedDifficulty by remember { mutableStateOf(ExerciseDifficulty.BEGINNER) }
    var requiresWeight by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }

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
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = exerciseName.isBlank(),
                    )
                }

                item {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ExerciseCategory.values()) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.displayName) },
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
                            text = "Show advanced options",
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
                        Text(
                            text = "Primary Muscles",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(MuscleGroup.values()) { muscle ->
                                FilterChip(
                                    selected = muscle in selectedPrimaryMuscles,
                                    onClick = {
                                        selectedPrimaryMuscles =
                                            if (muscle in selectedPrimaryMuscles) {
                                                selectedPrimaryMuscles - muscle
                                            } else {
                                                selectedPrimaryMuscles + muscle
                                            }
                                        // Remove from secondary if added to primary
                                        if (muscle in selectedPrimaryMuscles) {
                                            selectedSecondaryMuscles = selectedSecondaryMuscles - muscle
                                        }
                                    },
                                    label = { Text(muscle.displayName) },
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Secondary Muscles",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(MuscleGroup.values().filter { it !in selectedPrimaryMuscles }) { muscle ->
                                FilterChip(
                                    selected = muscle in selectedSecondaryMuscles,
                                    onClick = {
                                        selectedSecondaryMuscles =
                                            if (muscle in selectedSecondaryMuscles) {
                                                selectedSecondaryMuscles - muscle
                                            } else {
                                                selectedSecondaryMuscles + muscle
                                            }
                                    },
                                    label = { Text(muscle.displayName) },
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Equipment",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(Equipment.values()) { equipment ->
                                FilterChip(
                                    selected = equipment in selectedEquipment,
                                    onClick = {
                                        selectedEquipment =
                                            if (equipment in selectedEquipment) {
                                                selectedEquipment - equipment
                                            } else {
                                                selectedEquipment + equipment
                                            }
                                    },
                                    label = { Text(equipment.displayName) },
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Difficulty",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(ExerciseDifficulty.values()) { difficulty ->
                                FilterChip(
                                    selected = selectedDifficulty == difficulty,
                                    onClick = { selectedDifficulty = difficulty },
                                    label = { Text(difficulty.displayName) },
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
                    if (exerciseName.isNotBlank()) {
                        // For now, just pass the name - we'll enhance this later
                        onCreate(exerciseName)
                    }
                },
                enabled = exerciseName.isNotBlank(),
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
}

@Composable
private fun SuggestionCard(
    suggestion: ExerciseSuggestion,
    onSelect: () -> Unit,
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
