package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.viewmodel.ExerciseSelectorViewModel

@Composable
@Suppress("LongMethod")
fun CreateCustomExerciseDialog(
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

    // Dialog states for searchable selections
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPrimaryMusclesDialog by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var showSecondaryMusclesDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }

    // Get validation state from ViewModel
    val nameValidationError by viewModel.nameValidationError.collectAsState()

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
                    ExerciseNameField(
                        exerciseName = exerciseName,
                        onNameChange = { exerciseName = it },
                        nameValidationError = nameValidationError,
                    )
                }

                item {
                    SelectionField(
                        label = "Category * (Required)",
                        value = selectedCategory?.displayName ?: "Select category",
                        isRequired = true,
                        isEmpty = selectedCategory == null,
                        onClick = { showCategoryDialog = true },
                    )
                }

                item {
                    SelectionField(
                        label = "Primary Muscles * (Required - Select at least one)",
                        value =
                            if (selectedPrimaryMuscles.isEmpty()) {
                                "Select primary muscles"
                            } else {
                                selectedPrimaryMuscles.joinToString(", ") { it.displayName }
                            },
                        isRequired = true,
                        isEmpty = selectedPrimaryMuscles.isEmpty(),
                        onClick = { showPrimaryMusclesDialog = true },
                    )
                }

                item {
                    SelectionField(
                        label = "Equipment * (Required - Select at least one)",
                        value =
                            if (selectedEquipment.isEmpty()) {
                                "Select equipment"
                            } else {
                                selectedEquipment.joinToString(", ") { it.displayName }
                            },
                        isRequired = true,
                        isEmpty = selectedEquipment.isEmpty(),
                        onClick = { showEquipmentDialog = true },
                    )
                }

                item {
                    AdvancedOptionsToggle(
                        showAdvanced = showAdvanced,
                        onToggle = { showAdvanced = it },
                    )
                }

                if (showAdvanced) {
                    item {
                        SelectionField(
                            label = "Secondary Muscles (Optional)",
                            value =
                                if (selectedSecondaryMuscles.isEmpty()) {
                                    "Select secondary muscles"
                                } else {
                                    selectedSecondaryMuscles.joinToString(", ") { it.displayName }
                                },
                            isRequired = false,
                            isEmpty = false,
                            onClick = { showSecondaryMusclesDialog = true },
                        )
                    }

                    item {
                        SelectionField(
                            label = "Difficulty",
                            value = selectedDifficulty.displayName,
                            isRequired = false,
                            isEmpty = false,
                            onClick = { showDifficultyDialog = true },
                        )
                    }

                    item {
                        RequiresWeightToggle(
                            requiresWeight = requiresWeight,
                            onToggle = { requiresWeight = it },
                        )
                    }
                }
            }
        },
        confirmButton = {
            CreateButton(
                enabled =
                    exerciseName.isNotBlank() &&
                        nameValidationError == null &&
                        selectedCategory != null &&
                        selectedPrimaryMuscles.isNotEmpty() &&
                        selectedEquipment.isNotEmpty(),
                onClick = {
                    val hasValidName = exerciseName.isNotBlank() && nameValidationError == null
                    val hasCategory = selectedCategory != null
                    val hasPrimaryMuscles = selectedPrimaryMuscles.isNotEmpty()
                    val hasEquipment = selectedEquipment.isNotEmpty()
                    val isValidExercise = hasValidName && hasCategory && hasPrimaryMuscles && hasEquipment

                    if (isValidExercise) {
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
            )
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    // Selection Dialogs
    SelectionDialogs(
        showCategoryDialog = showCategoryDialog,
        showPrimaryMusclesDialog = showPrimaryMusclesDialog,
        showEquipmentDialog = showEquipmentDialog,
        showSecondaryMusclesDialog = showSecondaryMusclesDialog,
        showDifficultyDialog = showDifficultyDialog,
        selectedCategory = selectedCategory,
        selectedPrimaryMuscles = selectedPrimaryMuscles,
        selectedSecondaryMuscles = selectedSecondaryMuscles,
        selectedEquipment = selectedEquipment,
        selectedDifficulty = selectedDifficulty,
        onCategorySelected = {
            selectedCategory = it
            showCategoryDialog = false
        },
        onPrimaryMusclesSelected = { muscles ->
            selectedPrimaryMuscles = muscles
            // Remove any selected primary muscles from secondary
            selectedSecondaryMuscles = selectedSecondaryMuscles - muscles
            showPrimaryMusclesDialog = false
        },
        onSecondaryMusclesSelected = { muscles ->
            selectedSecondaryMuscles = muscles
            showSecondaryMusclesDialog = false
        },
        onEquipmentSelected = { equipment ->
            selectedEquipment = equipment
            showEquipmentDialog = false
        },
        onDifficultySelected = { difficulty ->
            selectedDifficulty = difficulty
            showDifficultyDialog = false
        },
        onDismissCategoryDialog = { showCategoryDialog = false },
        onDismissPrimaryMusclesDialog = { showPrimaryMusclesDialog = false },
        onDismissSecondaryMusclesDialog = { showSecondaryMusclesDialog = false },
        onDismissEquipmentDialog = { showEquipmentDialog = false },
        onDismissDifficultyDialog = { showDifficultyDialog = false },
    )
}

@Composable
private fun ExerciseNameField(
    exerciseName: String,
    onNameChange: (String) -> Unit,
    nameValidationError: String?,
) {
    Column {
        OutlinedTextField(
            value = exerciseName,
            onValueChange = onNameChange,
            label = { Text("Exercise name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameValidationError != null || exerciseName.isBlank(),
            supportingText = {
                if (nameValidationError != null) {
                    Text(
                        text = nameValidationError,
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
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    isRequired: Boolean,
    isEmpty: Boolean,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color =
                if (isRequired && isEmpty) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        if (isRequired && isEmpty) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                ),
        ) {
            Text(
                text = value,
                color =
                    if (isEmpty && isRequired) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdvancedOptionsToggle(
    showAdvanced: Boolean,
    onToggle: (Boolean) -> Unit,
) {
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
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun RequiresWeightToggle(
    requiresWeight: Boolean,
    onToggle: (Boolean) -> Unit,
) {
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
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun CreateButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text("Create")
    }
}

@Composable
@Suppress("LongParameterList")
private fun SelectionDialogs(
    showCategoryDialog: Boolean,
    showPrimaryMusclesDialog: Boolean,
    showEquipmentDialog: Boolean,
    showSecondaryMusclesDialog: Boolean,
    showDifficultyDialog: Boolean,
    selectedCategory: ExerciseCategory?,
    selectedPrimaryMuscles: Set<MuscleGroup>,
    selectedSecondaryMuscles: Set<MuscleGroup>,
    selectedEquipment: Set<Equipment>,
    selectedDifficulty: ExerciseDifficulty,
    onCategorySelected: (ExerciseCategory) -> Unit,
    onPrimaryMusclesSelected: (Set<MuscleGroup>) -> Unit,
    onSecondaryMusclesSelected: (Set<MuscleGroup>) -> Unit,
    onEquipmentSelected: (Set<Equipment>) -> Unit,
    onDifficultySelected: (ExerciseDifficulty) -> Unit,
    onDismissCategoryDialog: () -> Unit,
    onDismissPrimaryMusclesDialog: () -> Unit,
    onDismissSecondaryMusclesDialog: () -> Unit,
    onDismissEquipmentDialog: () -> Unit,
    onDismissDifficultyDialog: () -> Unit,
) {
    // Category Selection Dialog
    if (showCategoryDialog) {
        SearchableSelectionDialog(
            title = "Select Category",
            items = ExerciseCategory.entries.toList(),
            selectedItem = selectedCategory,
            itemLabel = { it.displayName },
            searchHint = "Search categories...",
            multiSelect = false,
            required = true,
            onDismiss = onDismissCategoryDialog,
            onSelect = onCategorySelected,
        )
    }

    // Primary Muscles Selection Dialog
    if (showPrimaryMusclesDialog) {
        SearchableSelectionDialog(
            title = "Select Primary Muscles",
            items = MuscleGroup.entries.toList(),
            selectedItems = selectedPrimaryMuscles,
            itemLabel = { it.displayName },
            searchHint = "Search muscles...",
            multiSelect = true,
            required = true,
            onDismiss = onDismissPrimaryMusclesDialog,
            onConfirm = onPrimaryMusclesSelected,
        )
    }

    // Equipment Selection Dialog
    if (showEquipmentDialog) {
        SearchableSelectionDialog(
            title = "Select Equipment",
            items = Equipment.entries.toList(),
            selectedItems = selectedEquipment,
            itemLabel = { it.displayName },
            searchHint = "Search equipment...",
            multiSelect = true,
            required = true,
            onDismiss = onDismissEquipmentDialog,
            onConfirm = onEquipmentSelected,
        )
    }

    // Secondary Muscles Selection Dialog
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
            onDismiss = onDismissSecondaryMusclesDialog,
            onConfirm = onSecondaryMusclesSelected,
        )
    }

    // Difficulty Selection Dialog
    if (showDifficultyDialog) {
        SearchableSelectionDialog(
            title = "Select Difficulty",
            items = ExerciseDifficulty.entries.toList(),
            selectedItem = selectedDifficulty,
            itemLabel = { it.displayName },
            searchHint = "Search difficulty...",
            multiSelect = false,
            required = false,
            onDismiss = onDismissDifficultyDialog,
            onSelect = onDifficultySelected,
        )
    }
}
