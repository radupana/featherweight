package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.ui.components.PrimaryGradientCard
import com.github.radupana.featherweight.viewmodel.InProgressWorkout
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onStartFreestyle: () -> Unit,
    onBrowseProgrammes: () -> Unit,
    onNavigateToActiveProgramme: (() -> Unit)? = null,
    onStartProgrammeWorkout: () -> Unit,
    onGenerateAIProgramme: () -> Unit,
    modifier: Modifier = Modifier,
    workoutViewModel: WorkoutViewModel = viewModel(),
    programmeViewModel: ProgrammeViewModel = viewModel(),
) {
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    val activeProgramme by programmeViewModel.activeProgramme.collectAsState()
    val programmeProgress by programmeViewModel.programmeProgress.collectAsState()
    val scope = rememberCoroutineScope()
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var pendingWorkout by remember { mutableStateOf<InProgressWorkout?>(null) }
    var showDeactivateProgrammeDialog by remember { mutableStateOf(false) }
    var showDeleteWorkoutDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<Long?>(null) }

    // Load in-progress workouts when screen appears
    LaunchedEffect(Unit) {
        workoutViewModel.loadInProgressWorkouts()
        programmeViewModel.refreshData()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Primary Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (workoutViewModel.hasInProgressWorkouts()) {
                                    val recentWorkout =
                                        workoutViewModel.getMostRecentInProgressWorkout()
                                    pendingWorkout = recentWorkout
                                    showWorkoutDialog = true
                                } else {
                                    workoutViewModel.startNewWorkout(forceNew = true)
                                    onStartFreestyle()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Freestyle Workout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedButton(
                        onClick = onBrowseProgrammes,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = "Programmes",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browse Programmes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onGenerateAIProgramme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Generate",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate with AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Group workouts by type early
        val programmeWorkouts = inProgressWorkouts.filter { it.isProgrammeWorkout }
        val freestyleWorkouts = inProgressWorkouts.filter { !it.isProgrammeWorkout }

        // PROGRAMMES Section
        if (activeProgramme != null) {
            item {
                Text(
                    text = "PROGRAMMES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    fontWeight = FontWeight.Bold,
                )
            }

            // Active Programme Card with integrated workout info
            activeProgramme?.let { programme ->
                // Find the current in-progress workout for this programme
                val currentProgrammeWorkout = programmeWorkouts.find { it.programmeId == programme.id }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = "Active Programme",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Active Programme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onNavigateToActiveProgramme?.invoke() },
                                            onLongClick = { showDeactivateProgrammeDialog = true },
                                        ),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                ) {
                                    // Programme Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = programme.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            programmeProgress?.let { progress ->
                                                Text(
                                                    text = "Week ${progress.currentWeek}/${programme.durationWeeks} • ${progress.completedWorkouts}/${progress.totalWorkouts} workouts",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "View Details",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }

                                    // Progress bar
                                    programmeProgress?.let { progress ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val progressValue =
                                            if (progress.totalWorkouts > 0) {
                                                (progress.completedWorkouts.toFloat() / progress.totalWorkouts.toFloat()).coerceIn(
                                                    0f,
                                                    1f,
                                                )
                                            } else {
                                                0f
                                            }
                                        LinearProgressIndicator(
                                            progress = { progressValue },
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        )
                                    }
                                    
                                    // Current Workout Section
                                    if (currentProgrammeWorkout != null) {
                                        // Show in-progress workout
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            thickness = 1.dp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "In Progress",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    text = currentProgrammeWorkout.programmeWorkoutName ?: "Workout",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                currentProgrammeWorkout.weekNumber?.let { week ->
                                                    Text(
                                                        text = "Week $week • Day ${currentProgrammeWorkout.dayNumber ?: 1}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${currentProgrammeWorkout.exerciseCount} exercises • ${currentProgrammeWorkout.completedSets}/${currentProgrammeWorkout.setCount} sets",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    workoutViewModel.resumeWorkout(currentProgrammeWorkout.id)
                                                    onStartProgrammeWorkout()
                                                },
                                                modifier = Modifier.height(36.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                            ) {
                                                Text(
                                                    "Continue",
                                                    style = MaterialTheme.typography.labelMedium,
                                                )
                                            }
                                        }
                                    } else {
                                        // Show start next workout option
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    // Get next workout info and start it
                                                    val nextWorkoutInfo = workoutViewModel.getNextProgrammeWorkout()
                                                    val programme = activeProgramme
                                                    if (nextWorkoutInfo != null && programme != null) {
                                                        workoutViewModel.startProgrammeWorkout(
                                                            programmeId = programme.id,
                                                            weekNumber = nextWorkoutInfo.actualWeekNumber,
                                                            dayNumber = nextWorkoutInfo.workoutStructure.day,
                                                            userMaxes = mapOf(
                                                                "squat" to (programme.squatMax ?: 100f),
                                                                "bench" to (programme.benchMax ?: 80f),
                                                                "deadlift" to (programme.deadliftMax ?: 120f),
                                                                "ohp" to (programme.ohpMax ?: 60f),
                                                            ),
                                                            onReady = {
                                                                onStartProgrammeWorkout()
                                                            },
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary,
                                            ),
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Start Next Workout",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FREESTYLE WORKOUTS Section
        if (freestyleWorkouts.isNotEmpty()) {
            item {
                Text(
                    text = "FREESTYLE WORKOUTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
            }

            // Show only freestyle workouts
            items(freestyleWorkouts) { workout ->
                InProgressWorkoutCard(
                    workout = workout,
                    onContinue = {
                        workoutViewModel.resumeWorkout(workout.id)
                        onStartFreestyle()
                    },
                    onLongClick = {
                        workoutToDelete = workout.id
                        showDeleteWorkoutDialog = true
                    },
                )
            }
        }
    }

    // Continue or Start New Workout Dialog
    if (showWorkoutDialog && pendingWorkout != null) {
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false },
            title = { Text("Continue Workout?") },
            text = {
                Column {
                    Text(
                        "You have an in-progress workout:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pendingWorkout!!.name
                            ?: "Started ${
                                pendingWorkout!!.startDate.format(
                                    DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
                                )
                            }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${pendingWorkout!!.exerciseCount} exercises • ${pendingWorkout!!.completedSets}/${pendingWorkout!!.setCount} sets completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Would you like to continue this workout or start a new one?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        workoutViewModel.resumeWorkout(pendingWorkout!!.id)
                        onStartFreestyle()
                        showWorkoutDialog = false
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        workoutViewModel.startNewWorkout(forceNew = true)
                        onStartFreestyle()
                        showWorkoutDialog = false
                    },
                ) {
                    Text("Start New")
                }
            },
        )
    }

    // Deactivate Programme Dialog
    if (showDeactivateProgrammeDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateProgrammeDialog = false },
            title = { Text("Deactivate Programme?") },
            text = {
                Text(
                    "Are you sure you want to deactivate '${activeProgramme?.name}'? " +
                        "You can reactivate it later from the Programmes screen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            programmeViewModel.deactivateActiveProgramme()
                            showDeactivateProgrammeDialog = false
                        }
                    },
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeactivateProgrammeDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete Workout Dialog
    if (showDeleteWorkoutDialog && workoutToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkoutDialog = false },
            title = { Text("Delete Workout?") },
            text = {
                Text(
                    "Are you sure you want to delete this in-progress workout? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        workoutToDelete?.let { id ->
                            scope.launch {
                                workoutViewModel.deleteWorkout(id)
                                workoutViewModel.loadInProgressWorkouts()
                                showDeleteWorkoutDialog = false
                                workoutToDelete = null
                            }
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteWorkoutDialog = false
                        workoutToDelete = null
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InProgressWorkoutCard(
    workout: InProgressWorkout,
    onContinue: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onContinue() },
                    onLongClick = onLongClick,
                ),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Programme badge if applicable
            if (workout.isProgrammeWorkout && workout.programmeName != null) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = "Programme Workout",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = workout.programmeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    workout.weekNumber?.let { week ->
                        Text(
                            text = " • Week $week",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            workout.isProgrammeWorkout && workout.programmeWorkoutName != null -> 
                                workout.programmeWorkoutName
                            workout.name != null -> workout.name
                            else -> workout.startDate.format(
                                DateTimeFormatter.ofPattern("MMM d 'at' h:mm a"),
                            )
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${workout.exerciseCount} exercises • ${workout.completedSets}/${workout.setCount} sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!workout.isProgrammeWorkout && workout.name == null) {
                        Text(
                            text = "Freestyle Workout",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Tap to continue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}