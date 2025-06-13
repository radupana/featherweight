package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

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
    var nextWorkout by remember { mutableStateOf<WorkoutStructure?>(null) }
    var nextWorkoutWeek by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }

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
            // Programme Overview Card
            activeProgramme?.let { programme ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = programme.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        programmeProgress?.let { progress ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Week ${progress.currentWeek} of ${programme.durationWeeks}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = "${progress.completedWorkouts}/${progress.totalWorkouts} workouts completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }

                                // Overall progress
                                Column(
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    val overallProgress =
                                        if (progress.totalWorkouts > 0) {
                                            progress.completedWorkouts.toFloat() / progress.totalWorkouts.toFloat()
                                        } else {
                                            0f
                                        }

                                    Text(
                                        text = "${(overallProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    LinearProgressIndicator(
                                        progress = { overallProgress },
                                        modifier =
                                            Modifier
                                                .width(80.dp)
                                                .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Next Workout Card
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (nextWorkout != null) {
                NextWorkoutCard(
                    workout = nextWorkout!!,
                    programmeProgress = programmeProgress,
                    actualWeekNumber = nextWorkoutWeek,
                    onStartWorkout = {
                        // Start the programme workout
                        activeProgramme?.let { programme ->
                            println("🎯 Starting programme workout from ActiveProgrammeScreen:")
                            println("  - Programme: ${programme.name} (id=${programme.id})")
                            println("  - Week: $nextWorkoutWeek")
                            println("  - Workout day: ${nextWorkout!!.day}")
                            println("  - Workout name: ${nextWorkout!!.name}")
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
                            )
                            onStartProgrammeWorkout()
                        }
                    },
                )
            } else {
                // Programme completed or no workouts
                ProgrammeCompletedCard(onBack = onBack)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NextWorkoutCard(
    workout: WorkoutStructure,
    programmeProgress: com.github.radupana.featherweight.data.programme.ProgrammeProgress?,
    actualWeekNumber: Int = 1,
    onStartWorkout: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Next Workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Week $actualWeekNumber • Day ${workout.day}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            Text(
                text = workout.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Exercise preview
            if (workout.exercises.isNotEmpty()) {
                Text(
                    text = "Exercises:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                workout.exercises.take(4).forEach { exercise ->
                    Text(
                        text = "• ${exercise.name} - ${exercise.sets} sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }

                if (workout.exercises.size > 4) {
                    Text(
                        text = "... and ${workout.exercises.size - 4} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Estimated duration
            workout.estimatedDuration?.let { duration ->
                Text(
                    text = "Estimated duration: $duration minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            // Start workout button
            Button(
                onClick = onStartWorkout,
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
                    text = "Start Workout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ProgrammeCompletedCard(onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Programme Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Congratulations! You've completed all workouts in this programme.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Browse New Programmes")
            }
        }
    }
}
