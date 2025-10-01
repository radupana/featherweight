package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.ui.components.SearchableSelectionDialog
import com.github.radupana.featherweight.viewmodel.ExerciseMappingViewModel
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseMappingScreen(
    unmatchedExercises: List<String>,
    onMappingComplete: (Map<String, String?>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseMappingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    val currentExercise = unmatchedExercises.getOrNull(currentExerciseIndex)
    var showCreateDialog by remember { mutableStateOf(false) }
    val exerciseSelectorViewModel: ExerciseSelectorViewModel = viewModel()

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
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (currentExercise != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header card showing the unmatched exercise
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    modifier = Modifier.fillMaxWidth(),
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
                            text = currentExercise,
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

                // Current mapping status
                val currentMapping = uiState.mappings[currentExercise]
                if (currentMapping != null) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            ),
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
                                    if (currentMapping.exerciseId != null) {
                                        "Mapped to: ${currentMapping.exerciseName}"
                                    } else {
                                        "Will create as custom exercise"
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    viewModel.clearMapping(currentExercise)
                                },
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
                        viewModel.mapExercise(currentExercise, exerciseId.toString(), exerciseName)
                    },
                    viewModel = viewModel,
                )

                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Create as custom button
                    OutlinedButton(
                        onClick = {
                            showCreateDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentMapping == null,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create as Custom Exercise")
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Previous button
                        OutlinedButton(
                            onClick = {
                                if (currentExerciseIndex > 0) {
                                    currentExerciseIndex--
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = currentExerciseIndex > 0,
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
                            enabled = currentMapping != null,
                        ) {
                            Text(
                                if (currentExerciseIndex < unmatchedExercises.size - 1) {
                                    "Next"
                                } else {
                                    "Complete"
                                },
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
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (showCreateDialog && currentExercise != null) {
            CreateCustomExerciseDialog(
                initialName = currentExercise,
                viewModel = exerciseSelectorViewModel,
                onDismiss = { showCreateDialog = false },
                onCreate = { name, category, primaryMuscles, secondaryMuscles, equipment, difficulty, requiresWeight ->
                    viewModel.createCustomExercise(
                        originalName = currentExercise,
                        name = name,
                        category = category,
                        equipment = equipment,
                        difficulty = difficulty,
                        requiresWeight = requiresWeight,
                    )
                    showCreateDialog = false
                },
            )
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
        requiresWeight: Boolean,
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

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPrimaryMusclesDialog by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var showSecondaryMusclesDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }

    val nameValidationError by viewModel.nameValidationError.collectAsState()

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
            Column(
                modifier =
                    Modifier
                        .height(400.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text(
                                    text = "Format: [Equipment] [Muscle] [Movement]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category * (Required)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            ),
                    ) {
                        Text(
                            text = selectedCategory?.displayName ?: "Select category",
                            color = if (selectedCategory == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Primary Muscles * (Required - Select at least one)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = { showPrimaryMusclesDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            ),
                    ) {
                        Text(
                            text =
                                if (selectedPrimaryMuscles.isEmpty()) {
                                    "Select primary muscles"
                                } else {
                                    selectedPrimaryMuscles.joinToString(", ") { it.displayName }
                                },
                            color = if (selectedPrimaryMuscles.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Equipment * (Required - Select at least one)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = { showEquipmentDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            ),
                    ) {
                        Text(
                            text =
                                if (selectedEquipment.isEmpty()) {
                                    "Select equipment"
                                } else {
                                    selectedEquipment.joinToString(", ") { it.displayName }
                                },
                            color = if (selectedEquipment.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

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

                if (showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Secondary Muscles (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(
                            onClick = { showSecondaryMusclesDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text =
                                    if (selectedSecondaryMuscles.isEmpty()) {
                                        "Select secondary muscles"
                                    } else {
                                        selectedSecondaryMuscles.joinToString(", ") { it.displayName }
                                    },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Difficulty",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(
                            onClick = { showDifficultyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(selectedDifficulty.displayName)
                        }
                    }

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
        },
        confirmButton = {
            Button(
                onClick = {
                    val hasValidName = exerciseName.isNotBlank() && nameValidationError == null
                    val hasCategory = selectedCategory != null
                    val hasPrimaryMuscles = selectedPrimaryMuscles.isNotEmpty()
                    val hasEquipment = selectedEquipment.isNotEmpty()

                    if (hasValidName && hasCategory && hasPrimaryMuscles && hasEquipment) {
                        onCreate(
                            exerciseName,
                            selectedCategory!!,
                            selectedPrimaryMuscles,
                            selectedSecondaryMuscles,
                            selectedEquipment,
                            selectedDifficulty,
                            requiresWeight,
                        )
                    }
                },
                enabled =
                    exerciseName.isNotBlank() &&
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

    if (showCategoryDialog) {
        SearchableSelectionDialog(
            title = "Select Category",
            items = ExerciseCategory.entries.toList(),
            selectedItem = selectedCategory,
            itemLabel = { it.displayName },
            searchHint = "Search categories...",
            multiSelect = false,
            required = true,
            onDismiss = { showCategoryDialog = false },
            onSelect = { category ->
                selectedCategory = category
                showCategoryDialog = false
            },
        )
    }

    if (showPrimaryMusclesDialog) {
        SearchableSelectionDialog(
            title = "Select Primary Muscles",
            items = MuscleGroup.entries.toList(),
            selectedItems = selectedPrimaryMuscles,
            itemLabel = { it.displayName },
            searchHint = "Search muscles...",
            multiSelect = true,
            required = true,
            onDismiss = { showPrimaryMusclesDialog = false },
            onConfirm = { muscles ->
                selectedPrimaryMuscles = muscles
                selectedSecondaryMuscles = selectedSecondaryMuscles - muscles
                showPrimaryMusclesDialog = false
            },
        )
    }

    if (showEquipmentDialog) {
        SearchableSelectionDialog(
            title = "Select Equipment",
            items = Equipment.entries.toList(),
            selectedItems = selectedEquipment,
            itemLabel = { it.displayName },
            searchHint = "Search equipment...",
            multiSelect = true,
            required = true,
            onDismiss = { showEquipmentDialog = false },
            onConfirm = { equipment ->
                selectedEquipment = equipment
                showEquipmentDialog = false
            },
        )
    }

    if (showSecondaryMusclesDialog) {
        val availableSecondaryMuscles = MuscleGroup.entries.filter { it !in selectedPrimaryMuscles }
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
            },
        )
    }

    if (showDifficultyDialog) {
        SearchableSelectionDialog(
            title = "Select Difficulty",
            items = ExerciseDifficulty.entries.toList(),
            selectedItem = selectedDifficulty,
            itemLabel = { it.displayName },
            searchHint = "Search difficulty...",
            multiSelect = false,
            required = false,
            onDismiss = { showDifficultyDialog = false },
            onSelect = { difficulty ->
                selectedDifficulty = difficulty
                showDifficultyDialog = false
            },
        )
    }
}

@Composable
private fun ExerciseSearchSection(
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
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onExerciseSelected(exercise.id, exercise.name)
                                        focusManager.clearFocus()
                                    },
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
                }
            }
        }

        // No results message
        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
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
    }
}
