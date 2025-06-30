package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.*
import com.github.radupana.featherweight.data.ExerciseLog
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.ui.utils.systemBarsPadding
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

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

    var showCreateDialog by remember { mutableStateOf(false) }

    // Handle successful exercise creation
    LaunchedEffect(exerciseCreated) {
        exerciseCreated?.let { exerciseName ->
            onCreateCustomExercise(exerciseName)
            viewModel.clearExerciseCreated()
        }
    }

    val isKeyboardVisible by rememberKeyboardState()
    val compactPadding = if (isKeyboardVisible) 8.dp else 16.dp
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.loadExercises()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Show current exercise when in swap mode
            if (isSwapMode && currentExercise != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(compactPadding),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(compactPadding)
                    ) {
                        Text(
                            text = "Currently selected:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = currentExercise.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All sets will be cleared when you swap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Error handling at the top
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(compactPadding),
                    colors = CardDefaults.cardColors(
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
            
            // Filter Chips - hide when keyboard is visible to save space
            if (!isKeyboardVisible) {
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
                                modifier = Modifier.padding(compactPadding)
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
                            contentPadding = PaddingValues(
                                start = compactPadding,
                                end = compactPadding,
                                top = compactPadding,
                                bottom = compactPadding + navigationBarPadding.calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(compactPadding / 2),
                        ) {
                            items(exercises) { exercise ->
                                ExerciseCard(
                                    exercise = exercise,
                                    onSelect = { onExerciseSelected(exercise) },
                                    isCompact = isKeyboardVisible
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
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}


@Composable
private fun ExerciseCard(
    exercise: ExerciseWithDetails,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val cardPadding = if (isCompact) 10.dp else 12.dp
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
        ) {
            // Top row: Exercise name and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    
                    // Usage count indicator - only show if > 0
                    if (exercise.exercise.usageCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = "${exercise.exercise.usageCount}×",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = exercise.exercise.category.displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Bottom row: All metadata in one line
            if (exercise.exercise.muscleGroup.isNotEmpty() || 
                exercise.exercise.equipment != Equipment.BODYWEIGHT || 
                exercise.exercise.difficulty != ExerciseDifficulty.BEGINNER) {
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left side: Muscles and equipment
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Primary muscles
                        if (exercise.exercise.muscleGroup.isNotEmpty()) {
                            Text(
                                text = exercise.exercise.muscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        
                        // Equipment (if any)
                        if (exercise.exercise.equipment != Equipment.BODYWEIGHT) {
                            if (exercise.exercise.muscleGroup.isNotEmpty()) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = exercise.exercise.equipment.name.replace('_', ' '),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    
                    // Right side: Difficulty
                    if (exercise.exercise.difficulty != ExerciseDifficulty.BEGINNER) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = when (exercise.exercise.difficulty) {
                                ExerciseDifficulty.NOVICE -> MaterialTheme.colorScheme.secondaryContainer
                                ExerciseDifficulty.INTERMEDIATE -> MaterialTheme.colorScheme.surfaceVariant
                                ExerciseDifficulty.ADVANCED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ExerciseDifficulty.EXPERT -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = exercise.exercise.difficulty.displayName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (exercise.exercise.difficulty) {
                                    ExerciseDifficulty.ADVANCED, ExerciseDifficulty.EXPERT ->
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
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
