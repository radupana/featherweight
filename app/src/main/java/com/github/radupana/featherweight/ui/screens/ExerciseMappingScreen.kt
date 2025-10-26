package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.ui.components.CreateCustomExerciseDialog
import com.github.radupana.featherweight.ui.components.ExerciseSearchSection
import com.github.radupana.featherweight.ui.components.MappingProgressIndicator
import com.github.radupana.featherweight.ui.components.MappingStatusCard
import com.github.radupana.featherweight.ui.components.UnmatchedExerciseCard
import com.github.radupana.featherweight.viewmodel.ExerciseMapping
import com.github.radupana.featherweight.viewmodel.ExerciseMappingUiState
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
            ExerciseMappingContent(
                currentExercise = currentExercise,
                currentExerciseIndex = currentExerciseIndex,
                unmatchedExercises = unmatchedExercises,
                uiState = uiState,
                onShowCreateDialog = { showCreateDialog = true },
                onPreviousClick = {
                    if (currentExerciseIndex > 0) {
                        currentExerciseIndex--
                    }
                },
                onNextClick = {
                    if (currentExerciseIndex < unmatchedExercises.size - 1) {
                        currentExerciseIndex++
                    } else {
                        // All exercises processed, check if all are mapped
                        if (viewModel.allExercisesMapped(unmatchedExercises)) {
                            onMappingComplete(viewModel.getFinalMappings())
                        }
                    }
                },
                onClearMapping = { viewModel.clearMapping(currentExercise) },
                onExerciseSelected = { exerciseId, exerciseName ->
                    viewModel.mapExercise(currentExercise, exerciseId, exerciseName)
                },
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues),
            )
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

@Composable
private fun ExerciseMappingContent(
    currentExercise: String,
    currentExerciseIndex: Int,
    unmatchedExercises: List<String>,
    uiState: ExerciseMappingUiState,
    onShowCreateDialog: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onClearMapping: () -> Unit,
    onExerciseSelected: (String, String) -> Unit,
    viewModel: ExerciseMappingViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header card showing the unmatched exercise
        UnmatchedExerciseCard(exerciseName = currentExercise)

        // Current mapping status
        val currentMapping = uiState.mappings[currentExercise]
        MappingStatusCard(
            mapping = currentMapping,
            onClearMapping = onClearMapping,
        )

        // Search section
        ExerciseSearchSection(
            exerciseName = currentExercise,
            onExerciseSelected = onExerciseSelected,
            viewModel = viewModel,
        )

        // Action buttons
        MappingActionButtons(
            currentMapping = currentMapping,
            currentExerciseIndex = currentExerciseIndex,
            unmatchedExercises = unmatchedExercises,
            mappedCount = uiState.mappings.size,
            onShowCreateDialog = onShowCreateDialog,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
        )
    }
}

@Composable
private fun MappingActionButtons(
    currentMapping: ExerciseMapping?,
    currentExerciseIndex: Int,
    unmatchedExercises: List<String>,
    mappedCount: Int,
    onShowCreateDialog: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Create as custom button
        OutlinedButton(
            onClick = onShowCreateDialog,
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
                onClick = onPreviousClick,
                modifier = Modifier.weight(1f),
                enabled = currentExerciseIndex > 0,
            ) {
                Text("Previous")
            }

            // Next/Complete button
            Button(
                onClick = onNextClick,
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
        MappingProgressIndicator(
            mappedCount = mappedCount,
            totalCount = unmatchedExercises.size,
        )
    }
}
