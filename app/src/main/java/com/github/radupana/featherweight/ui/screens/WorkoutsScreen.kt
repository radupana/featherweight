package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.repository.NextProgrammeWorkoutInfo
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private data class WorkoutInfo(
    val name: String,
    val daysAgo: String,
    val exercises: String,
)

@Composable
fun WorkoutsScreen(
    onStartFreestyle: () -> Unit,
    onStartProgrammeWorkout: () -> Unit,
    onStartTemplate: (String) -> Unit,
    modifier: Modifier = Modifier,
    workoutViewModel: WorkoutViewModel = viewModel(),
    programmeViewModel: ProgrammeViewModel = viewModel(),
) {
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    val lastCompletedWorkout by workoutViewModel.lastCompletedWorkout.collectAsState()
    val lastCompletedWorkoutExercises by workoutViewModel.lastCompletedWorkoutExercises.collectAsState()
    val activeProgramme by programmeViewModel.activeProgramme.collectAsState()
    val programmeProgress by programmeViewModel.programmeProgress.collectAsState()
    val exerciseNames by workoutViewModel.exerciseNames.collectAsState()
    val scope = rememberCoroutineScope()

    // Determine if there's ANY in-progress workout (most recent will be used)
    val hasAnyInProgressWorkout = inProgressWorkouts.isNotEmpty()
    val mostRecentInProgressWorkout =
        inProgressWorkouts.maxByOrNull { it.startDate }

    // Build detailed workout label
    val activeWorkoutLabel =
        mostRecentInProgressWorkout?.let { workout ->
            when {
                workout.isProgrammeWorkout && workout.programmeName != null -> {
                    val workoutDetail = workout.programmeWorkoutName ?: "Week ${workout.weekNumber ?: ""} Day ${workout.dayNumber ?: ""}"
                    "${workout.programmeName} $workoutDetail"
                }

                workout.name != null -> workout.name
                else -> "Freestyle Workout"
            }
        }

    // Determine next workout info
    var nextWorkoutInfo by remember { mutableStateOf<NextProgrammeWorkoutInfo?>(null) }
    var nextWorkoutLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeProgramme, programmeProgress) {
        val programme = activeProgramme
        if (programme != null && programmeProgress != null) {
            nextWorkoutInfo = workoutViewModel.getNextProgrammeWorkout()
            nextWorkoutInfo?.let { info ->
                val workoutDetail = info.workoutStructure.name
                nextWorkoutLabel = "${programme.name} $workoutDetail"
            }
        }
    }

    // Format last workout info
    var lastWorkoutInfo by remember { mutableStateOf<WorkoutInfo?>(null) }

    LaunchedEffect(lastCompletedWorkout, lastCompletedWorkoutExercises, exerciseNames) {
        lastCompletedWorkout?.let { workout ->
            val daysAgoText =
                when (val daysAgo = ChronoUnit.DAYS.between(workout.date.toLocalDate(), LocalDateTime.now().toLocalDate()).toInt()) {
                    0 -> "today"
                    1 -> "yesterday"
                    else -> "$daysAgo days ago"
                }

            val exerciseNamesStr =
                lastCompletedWorkoutExercises.take(3).joinToString(", ") { exercise ->
                    exerciseNames[exercise.exerciseVariationId] ?: "Unknown"
                } +
                    if (lastCompletedWorkoutExercises.size > 3) " +${lastCompletedWorkoutExercises.size - 3} more" else ""

            lastWorkoutInfo =
                WorkoutInfo(
                    name = workout.programmeWorkoutName ?: "Freestyle Workout",
                    daysAgo = daysAgoText,
                    exercises = exerciseNamesStr,
                )
        }
    }

    // Load data when screen appears
    LaunchedEffect(Unit) {
        workoutViewModel.loadInProgressWorkouts()
        workoutViewModel.loadLastCompletedWorkout()
        programmeViewModel.refreshData()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Active workout card if there's an in-progress workout
        if (hasAnyInProgressWorkout && mostRecentInProgressWorkout != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue Workout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeWorkoutLabel ?: "In Progress",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            // Show programme name if it's a programme workout
                            if (mostRecentInProgressWorkout.isProgrammeWorkout && mostRecentInProgressWorkout.programmeName != null) {
                                Text(
                                    text = mostRecentInProgressWorkout.programmeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${mostRecentInProgressWorkout.exerciseCount} exercises • ${mostRecentInProgressWorkout.completedSets}/${mostRecentInProgressWorkout.setCount} sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    workoutViewModel.resumeWorkout(mostRecentInProgressWorkout.id)
                                    onStartFreestyle()
                                }
                            },
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue")
                        }
                    }
                }
            }
        }

        // Only show start workout options if there's no in-progress workout
        if (!hasAnyInProgressWorkout) {
            // Programme workout card if available
            if (nextWorkoutLabel != null && activeProgramme != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Text(
                            text = "Start Programme Workout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nextWorkoutLabel ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val programme = activeProgramme
                                    val workoutInfo = nextWorkoutInfo
                                    if (workoutInfo != null && programme != null) {
                                        workoutViewModel.startProgrammeWorkout(
                                            programmeId = programme.id,
                                            weekNumber = workoutInfo.actualWeekNumber,
                                            dayNumber = workoutInfo.workoutStructure.day,
                                            onReady = {
                                                onStartProgrammeWorkout()
                                            },
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Workout")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Start Freestyle Workout button
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Start Freestyle Workout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Begin a freestyle workout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            workoutViewModel.startNewWorkout(forceNew = true)
                            onStartFreestyle()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Freestyle Workout")
                }
            }

            // Template workouts section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Start From Template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Template cards in a 2x3 grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TemplateButton(
                        name = "Push",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("PUSH") },
                    )
                    TemplateButton(
                        name = "Pull",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("PULL") },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TemplateButton(
                        name = "Legs",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("LEGS") },
                    )
                    TemplateButton(
                        name = "Upper",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("UPPER") },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TemplateButton(
                        name = "Lower",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("LOWER") },
                    )
                    TemplateButton(
                        name = "Full Body",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartTemplate("FULL BODY") },
                    )
                }
            }
        }

        // Last workout info
        lastWorkoutInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Last: ${info.name} (${info.daysAgo})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = info.exercises,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateButton(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muscleGroups =
        when (name) {
            "Push" -> "Chest, Shoulders, Triceps"
            "Pull" -> "Back, Biceps"
            "Legs" -> "Quads, Hamstrings, Glutes, Calves"
            "Upper" -> "All upper body"
            "Lower" -> "All lower body"
            "Full Body" -> "Balanced mix"
            else -> ""
        }

    GlassmorphicCard(
        modifier =
            modifier
                .height(100.dp)
                .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = muscleGroups,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
