package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.profile.ExerciseMaxWithName
import com.github.radupana.featherweight.ui.dialogs.ExerciseSelectorDialog
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showAdd1RMDialog by remember { mutableStateOf(false) }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var editingMax by remember { mutableStateOf<ExerciseMaxWithName?>(null) }
    var selectedExerciseForDialog by remember { mutableStateOf<Exercise?>(null) }
    val haptics = LocalHapticFeedback.current

    val menuWidth by animateFloatAsState(
        targetValue = if (isMenuExpanded) 180f else 64f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menuWidth",
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Collapsible Left Navigation
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(menuWidth.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Menu Toggle Button
                    IconButton(
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isMenuExpanded = !isMenuExpanded 
                        },
                        modifier =
                            Modifier
                                .padding(top = 16.dp)
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Toggle Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Menu Items
                    NavigationRailItem(
                        selected = true,
                        onClick = { },
                        icon = {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = "Maxes",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label =
                            if (isMenuExpanded) {
                                {
                                    Text(
                                        "1RM Maxes",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else {
                                null
                            },
                        colors =
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        alwaysShowLabel = false,
                    )
                }
            }

            // Main Content Area
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 80.dp, // Space for FAB
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Header
                    item {
                        Column {
                            Text(
                                "1RM Tracking",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Track your one-rep maximums",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }

                    // Big 4 Compact Grid
                    item {
                        Text(
                            "Big 4 Lifts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        
                        // Compact 2x2 Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(180.dp), // Fixed height for 2 rows
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false // Disable scrolling since it's embedded
                        ) {
                            items(uiState.big4Exercises) { exercise ->
                                val currentMax = uiState.currentMaxes.find { it.exerciseId == exercise.id }
                                Compact1RMCard(
                                    exercise = exercise,
                                    currentMax = currentMax,
                                    onEdit = { exerciseToEdit = exercise },
                                )
                            }
                        }
                    }

                    // Other 1RMs Section
                    if (uiState.currentMaxes.any { max ->
                            uiState.big4Exercises.none { it.id == max.exerciseId }
                        }
                    ) {
                        item {
                            Text(
                                "Other 1RMs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }

                        items(
                            uiState.currentMaxes.filter { max ->
                                uiState.big4Exercises.none { it.id == max.exerciseId }
                            },
                        ) { max ->
                            SwipeableExerciseMaxCard(
                                max = max,
                                onEdit = { editingMax = max },
                                onDelete = { viewModel.deleteMax(max) },
                            )
                        }
                    }
                }

                // Floating Action Button
                ExtendedFloatingActionButton(
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAdd1RMDialog = true 
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add 1RM",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add 1RM")
                }
            }
        }
    }

    // Add/Edit 1RM Bottom Sheet
    if (showAdd1RMDialog || exerciseToEdit != null || editingMax != null) {
        Add1RMBottomSheet(
            exerciseId = exerciseToEdit?.id ?: editingMax?.exerciseId ?: selectedExerciseForDialog?.id,
            exerciseName = exerciseToEdit?.name ?: editingMax?.exerciseName ?: selectedExerciseForDialog?.name,
            currentWeight =
                editingMax?.maxWeight ?: uiState.currentMaxes.find {
                    it.exerciseId == (exerciseToEdit?.id ?: 0)
                }?.maxWeight,
            onDismiss = {
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
                selectedExerciseForDialog = null
            },
            onConfirm = { exerciseId, weight ->
                if (exerciseId != null) {
                    viewModel.update1RM(exerciseId, weight)
                }
                showAdd1RMDialog = false
                exerciseToEdit = null
                editingMax = null
                selectedExerciseForDialog = null
            },
            onSelectExercise = {
                showExerciseSelector = true
            },
        )
    }

    // Exercise Selector
    if (showExerciseSelector) {
        ExerciseSelectorDialog(
            onExerciseSelected = { exercise ->
                selectedExerciseForDialog = exercise
                showExerciseSelector = false
            },
            onDismiss = {
                showExerciseSelector = false
            },
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or error dialog
            viewModel.clearError()
        }
    }
}

@Composable
private fun Compact1RMCard(
    exercise: Exercise,
    currentMax: ExerciseMaxWithName?,
    onEdit: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (exercise.name) {
                    "Back Squat" -> "Squat"
                    "Bench Press" -> "Bench" 
                    "Conventional Deadlift" -> "Deadlift"
                    "Overhead Press" -> "OHP"
                    else -> exercise.name
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentMax != null) {
                    Text(
                        "${currentMax.maxWeight.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                } else {
                    Text(
                        "Set 1RM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExerciseMaxCard(
    max: ExerciseMaxWithName,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissDirection ->
            if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Only show red background when actively dismissing
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        OtherExerciseMaxCard(
            max = max,
            onEdit = onEdit,
            onDelete = {} // Disable original delete since swipe handles it
        )
    }
}

@Composable
private fun OtherExerciseMaxCard(
    max: ExerciseMaxWithName,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                },
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    max.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${max.maxWeight.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
                Text(
                    max.recordedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Icon(
                Icons.Filled.Edit,
                contentDescription = "Tap to edit, swipe left to delete",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Add1RMBottomSheet(
    exerciseId: Long? = null,
    exerciseName: String? = null,
    currentWeight: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float) -> Unit,
    onSelectExercise: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var weightText by remember {
        mutableStateOf(TextFieldValue(currentWeight?.toInt()?.toString() ?: ""))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title
            Text(
                if (exerciseId != null) "Update 1RM" else "Add 1RM",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            // Exercise Selection
            if (exerciseName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        if (exerciseName == "Conventional Deadlift") "Deadlift" else exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectExercise()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Exercise")
                }
            }

            // Weight Input
            OutlinedTextField(
                value = weightText,
                onValueChange = { newValue ->
                    val text = newValue.text
                    if (text.isEmpty() || (text.all { it.isDigit() } && text.length <= 4)) {
                        weightText = newValue
                    }
                },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val weight = weightText.text.toFloatOrNull()
                        if (exerciseId != null && weight != null && weight > 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm(exerciseId, weight)
                        }
                    },
                    enabled = exerciseId != null && weightText.text.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
