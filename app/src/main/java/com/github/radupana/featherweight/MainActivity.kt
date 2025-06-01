package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    private val workoutViewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatherweightTheme {
                AppContent(workoutViewModel)
            }
        }
    }
}

@Composable
fun AppContent(viewModel: WorkoutViewModel) {
    var showAddWorkout by remember { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }

    if (showAddWorkout) {
        AddWorkoutDialog(
            onAdd = { exercises ->
                viewModel.addWorkout(exercises)
                showAddWorkout = false
            },
            onDismiss = { showAddWorkout = false },
        )
    }

    if (selectedWorkout != null) {
        WorkoutDetailScreen(
            workout = selectedWorkout!!,
            viewModel = viewModel,
            onBack = { selectedWorkout = null },
        )
    } else {
        WorkoutHistoryScreen(
            viewModel = viewModel,
            onAddWorkout = { showAddWorkout = true },
            onWorkoutClick = { workout ->
                selectedWorkout = workout
                viewModel.loadExercisesForWorkout(workout.id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    viewModel: WorkoutViewModel,
    onAddWorkout: () -> Unit,
    onWorkoutClick: (Workout) -> Unit,
) {
    val workouts by viewModel.workouts.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Featherweight: Workouts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWorkout) {
                Text("+")
            }
        },
    ) { paddingValues ->
        Surface(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        ) {
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text("No workouts logged yet.")
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    items(workouts) { workout ->
                        ListItem(
                            headlineContent = { Text("Workout on: ${workout.date}") },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onWorkoutClick(workout) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workout: Workout,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
) {
    val exercises by viewModel.selectedWorkoutExercises.collectAsState()

    LaunchedEffect(workout.id) {
        viewModel.loadExercisesForWorkout(workout.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                },
            )
        },
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(Modifier.padding(16.dp)) {
                items(exercises) { exercise ->
                    ExerciseDetail(exercise, viewModel)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ExerciseDetail(
    exercise: ExerciseLog,
    viewModel: WorkoutViewModel,
) {
    val sets by viewModel.selectedExerciseSets.collectAsState()

    LaunchedEffect(exercise.id) {
        viewModel.loadSetsForExercise(exercise.id)
    }

    Column(Modifier.padding(8.dp)) {
        Text(text = "Exercise: ${exercise.exerciseName}", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(sets) { set ->
                SetDetail(set)
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SetDetail(set: SetLog) {
    Row(Modifier.padding(4.dp)) {
        Text("Set ${set.setOrder + 1}: ")
        Text("${set.reps} reps x ${set.weight} kg")
        set.rpe?.let { Text(" (RPE: $it)") }
        set.tag?.let { Text(" [$it]") }
    }
}
