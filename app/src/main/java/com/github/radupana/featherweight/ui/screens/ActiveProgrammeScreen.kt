package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.viewmodel.InProgressWorkout
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveProgrammeScreen(
    onBack: () -> Unit,
    onStartProgrammeWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    programmeViewModel: ProgrammeViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
) {
    val activeProgramme by programmeViewModel.activeProgramme.collectAsState()
    val programmeProgress by programmeViewModel.programmeProgress.collectAsState()
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    var nextWorkout by remember { mutableStateOf<WorkoutStructure?>(null) }
    var nextWorkoutWeek by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var existingWorkout by remember { mutableStateOf<InProgressWorkout?>(null) }
    val scope = rememberCoroutineScope()

    // Refresh programme progress and check for in-progress workouts when screen appears
    LaunchedEffect(Unit) {
        programmeViewModel.refreshProgrammeProgress()
        workoutViewModel.loadInProgressWorkouts()
    }

    // Check for existing in-progress programme workout
    LaunchedEffect(inProgressWorkouts, activeProgramme, nextWorkout, nextWorkoutWeek) {
        activeProgramme?.let { programme ->
            // Find workout that matches the current programme AND the specific week/day
            existingWorkout =
                inProgressWorkouts.find {
                    it.isProgrammeWorkout &&
                        it.programmeId == programme.id &&
                        it.weekNumber == nextWorkoutWeek &&
                        it.dayNumber == nextWorkout?.day
                }
        }
    }

    // Load next workout when screen appears
    LaunchedEffect(activeProgramme) {
        isLoading = true
        try {
            activeProgramme?.let { programme ->
                // Get next workout info
                val nextWorkoutInfo = workoutViewModel.getNextProgrammeWorkout()
                if (nextWorkoutInfo != null) {
                    nextWorkout = nextWorkoutInfo.workoutStructure
                    nextWorkoutWeek = nextWorkoutInfo.actualWeekNumber
                } else {
                    nextWorkout = null
                }
            }
        } catch (e: Exception) {
            println("❌ Error loading next workout: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Active Programme",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Unified Programme Card
            activeProgramme?.let { programme ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Programme Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = programme.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                programmeProgress?.let { progress ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Week ${progress.currentWeek} of ${programme.durationWeeks}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete programme",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Progress Bar
                        programmeProgress?.let { progress ->
                            val overallProgress =
                                if (progress.totalWorkouts > 0) {
                                    progress.completedWorkouts.toFloat() / progress.totalWorkouts.toFloat()
                                } else {
                                    0f
                                }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${progress.completedWorkouts}/${progress.totalWorkouts} workouts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${(overallProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { overallProgress },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )

                        // Next Workout Section
                        if (isLoading) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (nextWorkout != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Timeline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (existingWorkout != null) "Workout In Progress" else "Next Workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Text(
                                    text = nextWorkout!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                val workout = existingWorkout
                                Text(
                                    text =
                                        if (workout != null) {
                                            "Week $nextWorkoutWeek • Day ${nextWorkout!!.day} • ${workout.completedSets}/${workout.setCount} sets"
                                        } else {
                                            "Week $nextWorkoutWeek • Day ${nextWorkout!!.day}"
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                // Exercise preview (compact)
                                if (nextWorkout!!.exercises.isNotEmpty()) {
                                    Card(
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            nextWorkout!!.exercises.take(3).forEach { exercise ->
                                                Text(
                                                    text = "• ${exercise.name} - ${exercise.sets} sets",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                )
                                            }
                                            if (nextWorkout!!.exercises.size > 3) {
                                                Text(
                                                    text = "... and ${nextWorkout!!.exercises.size - 3} more",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Start workout button
                                Button(
                                    onClick = {
                                        // Start the programme workout
                                        println("🎯 Starting programme workout from ActiveProgrammeScreen:")
                                        println("  - Programme: ${programme.name} (id=${programme.id})")
                                        println("  - Week: $nextWorkoutWeek")
                                        println("  - Workout day: ${nextWorkout!!.day}")
                                        println("  - Workout name: ${nextWorkout!!.name}")

                                        // Launch coroutine to handle async workout creation
                                        scope.launch {
                                            val workout = existingWorkout
                                            if (workout != null) {
                                                // Resume existing workout
                                                workoutViewModel.resumeWorkout(workout.id)
                                                onStartProgrammeWorkout()
                                            } else {
                                                // Start new workout
                                                workoutViewModel.startProgrammeWorkout(
                                                    programmeId = programme.id,
                                                    weekNumber = nextWorkoutWeek,
                                                    dayNumber = nextWorkout!!.day,
                                                    userMaxes =
                                                        mapOf(
                                                            "squat" to (programme.squatMax ?: 100f),
                                                            "bench" to (programme.benchMax ?: 80f),
                                                            "deadlift" to (programme.deadliftMax ?: 120f),
                                                            "ohp" to (programme.ohpMax ?: 60f),
                                                        ),
                                                    onReady = {
                                                        // Navigate only after workout is fully created
                                                        onStartProgrammeWorkout()
                                                    },
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (existingWorkout != null) "Continue Workout" else "Start Next Workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        } else {
                            // Programme completed
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "🎉",
                                    style = MaterialTheme.typography.displaySmall,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Programme Complete!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You've completed all workouts in this programme.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Delete Programme Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Programme?") },
            text = {
                Text(
                    "Are you sure you want to delete '${activeProgramme?.name}'? " +
                        "This will permanently remove the programme and all its progress data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeProgramme?.let { programme ->
                            scope.launch {
                                programmeViewModel.deleteProgramme(programme)
                                showDeleteDialog = false
                                // Navigate back after deletion
                                onBack()
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
                    onClick = { showDeleteDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
