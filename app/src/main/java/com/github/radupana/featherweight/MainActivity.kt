package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.ui.AddWorkoutDialog
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    private val workoutViewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatherweightTheme {
                WorkoutHistoryScreen(workoutViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(viewModel: WorkoutViewModel) {
    val workouts by viewModel.workouts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Featherweight: Workouts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add workout")
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No workouts logged yet.")
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(workouts) { workout ->
                        WorkoutListItem(workout)
                        Divider()
                    }
                }
            }
            if (showAddDialog) {
                AddWorkoutDialog(
                    onAdd = { exerciseName, sets, reps, weight ->
                        viewModel.addWorkout(exerciseName, sets, reps, weight)
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
        }
    }
}

@Composable
fun WorkoutListItem(workout: Workout) {
    Text("Workout on: ${workout.date}")
}
