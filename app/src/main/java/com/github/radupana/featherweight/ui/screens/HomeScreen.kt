package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.github.radupana.featherweight.viewmodel.InProgressWorkout
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class LastWorkoutInfo(
    val name: String,
    val daysAgo: String,
    val exercises: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onStartFreestyle: () -> Unit,
    onBrowseProgrammes: () -> Unit,
    onNavigateToActiveProgramme: (() -> Unit)? = null,
    onStartProgrammeWorkout: () -> Unit,
    onGenerateAIProgramme: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
    workoutViewModel: WorkoutViewModel = viewModel(),
    programmeViewModel: ProgrammeViewModel = viewModel(),
) {
    val inProgressWorkouts by workoutViewModel.inProgressWorkouts.collectAsState()
    val activeProgramme by programmeViewModel.activeProgramme.collectAsState()
    val programmeProgress by programmeViewModel.programmeProgress.collectAsState()
    val lastCompletedWorkout by workoutViewModel.lastCompletedWorkout.collectAsState()
    val lastCompletedWorkoutExercises by workoutViewModel.lastCompletedWorkoutExercises.collectAsState()
    val scope = rememberCoroutineScope()
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var pendingWorkout by remember { mutableStateOf<InProgressWorkout?>(null) }

    // Determine if there's ANY in-progress workout (most recent will be used)
    inProgressWorkouts.isNotEmpty()
    val mostRecentInProgressWorkout =
        inProgressWorkouts
            .sortedByDescending { it.startDate }
            .firstOrNull()

    // Build detailed workout label
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

    // Determine next workout info - needs to be done in LaunchedEffect
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
    var lastWorkoutInfo by remember { mutableStateOf<LastWorkoutInfo?>(null) }

    LaunchedEffect(lastCompletedWorkout, lastCompletedWorkoutExercises) {
        lastCompletedWorkout?.let { workout ->
            val daysAgo = ChronoUnit.DAYS.between(workout.date, LocalDateTime.now()).toInt()
            val daysAgoText =
                when (daysAgo) {
                    0 -> "today"
                    1 -> "yesterday"
                    else -> "$daysAgo days ago"
                }

            val exerciseNames =
                lastCompletedWorkoutExercises.take(3).joinToString(", ") { it.exerciseName } +
                    if (lastCompletedWorkoutExercises.size > 3) " +${lastCompletedWorkoutExercises.size - 3} more" else ""

            lastWorkoutInfo =
                LastWorkoutInfo(
                    name = workout.programmeWorkoutName ?: "Freestyle Workout",
                    daysAgo = daysAgoText,
                    exercises = exerciseNames,
                )
        }
    }

    // Load data when screen appears
    LaunchedEffect(Unit) {
        workoutViewModel.loadInProgressWorkouts()
        workoutViewModel.loadLastCompletedWorkout()
        programmeViewModel.refreshData()
    }

    // Main UI - just empty space for now
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Empty content area - will be filled later
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
                        scope.launch {
                            workoutViewModel.resumeWorkout(pendingWorkout!!.id)
                            onStartFreestyle()
                            showWorkoutDialog = false
                        }
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
        colors =
            CardDefaults.cardColors(
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
                        text =
                            when {
                                workout.isProgrammeWorkout && workout.programmeWorkoutName != null ->
                                    workout.programmeWorkoutName

                                workout.name != null -> workout.name
                                else ->
                                    workout.startDate.format(
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
