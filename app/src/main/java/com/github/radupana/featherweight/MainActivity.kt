package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.view.WindowManager
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.AnalyticsScreen
import com.github.radupana.featherweight.ui.screens.ExerciseSelectorScreen
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.WorkoutHubScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.AnalyticsViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    HOME,
    WORKOUT_HUB,
    ACTIVE_WORKOUT,
    EXERCISE_SELECTOR,
    HISTORY,
    ANALYTICS,
    PROGRAMMES,
    ACTIVE_PROGRAMME,
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern look
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Configure keyboard behavior - this is crucial for proper keyboard handling
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            FeatherweightTheme {
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

                // Seed database early
                LaunchedEffect(Unit) {
                    val repository = FeatherweightRepository(application)
                    repository.seedDatabaseIfEmpty()
                }

                when (currentScreen) {
                    Screen.SPLASH ->
                        SplashScreen(
                            onSplashFinished = { currentScreen = Screen.HOME },
                        )

                    else -> {
                        // Main app with bottom navigation
                        MainAppWithNavigation(
                            currentScreen = currentScreen,
                            onScreenChange = { screen -> currentScreen = screen },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppWithNavigation(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
) {
    // Track previous screen for proper back navigation
    var previousScreen by remember { mutableStateOf<Screen?>(null) }

    // Update previous screen when screen changes
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.ACTIVE_WORKOUT || currentScreen == Screen.EXERCISE_SELECTOR) {
            // Don't update previousScreen when navigating to workout or exercise selector
            // Keep the previous value to maintain proper back navigation
        } else {
            previousScreen = currentScreen
        }
    }
    val navigationItems =
        listOf(
            NavigationItem(Screen.HOME, "Workout", Icons.Filled.FitnessCenter),
            NavigationItem(Screen.PROGRAMMES, "Programmes", Icons.Filled.Schedule),
            NavigationItem(Screen.HISTORY, "History", Icons.Filled.History),
            NavigationItem(Screen.ANALYTICS, "Analytics", Icons.Filled.Analytics),
        )

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.SPLASH &&
                currentScreen != Screen.ACTIVE_WORKOUT &&
                currentScreen != Screen.EXERCISE_SELECTOR
            ) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentScreen == item.screen,
                            onClick = { onScreenChange(item.screen) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (currentScreen) {
            Screen.HOME ->
                HomeScreen(
                    onStartFreestyle = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onBrowseProgrammes = { onScreenChange(Screen.PROGRAMMES) },
                    onNavigateToActiveProgramme = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.WORKOUT_HUB ->
                WorkoutHubScreen(
                    onStartActiveWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onStartTemplate = { onScreenChange(Screen.PROGRAMMES) },
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.ACTIVE_WORKOUT -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                WorkoutScreen(
                    onBack = {
                        // Navigate back to the screen the user came from
                        val backScreen = previousScreen ?: Screen.HOME
                        onScreenChange(backScreen)
                    },
                    onSelectExercise = { onScreenChange(Screen.EXERCISE_SELECTOR) },
                    workoutViewModel = workoutViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.EXERCISE_SELECTOR -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                ExerciseSelectorScreen(
                    onExerciseSelected = { exercise ->
                        workoutViewModel.addExerciseToCurrentWorkout(exercise)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onCreateCustomExercise = { name ->
                        workoutViewModel.addExerciseToCurrentWorkout(name)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onBack = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.HISTORY -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                HistoryScreen(
                    onViewWorkout = { workoutId ->
                        workoutViewModel.resumeWorkout(workoutId)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.ANALYTICS -> {
                val analyticsViewModel: AnalyticsViewModel = viewModel()
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROGRAMMES -> {
                com.github.radupana.featherweight.ui.screens.ProgrammesScreen(
                    onNavigateToActiveProgramme = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            Screen.ACTIVE_PROGRAMME -> {
                // For now, show a placeholder screen for active programme
                // This will be developed into a full programme workout screen later
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Active Programme",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Programme workout interface coming soon!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { onScreenChange(Screen.PROGRAMMES) }
                        ) {
                            Text("Back to Programmes")
                        }
                    }
                }
            }

            Screen.SPLASH -> {
                // Should not reach here
            }
        }
    }
}

@Composable
fun HomeScreen(
    onStartFreestyle: () -> Unit,
    onBrowseProgrammes: () -> Unit,
    onNavigateToActiveProgramme: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HomeScreen(
            onStartFreestyle = onStartFreestyle,
            onBrowseProgrammes = onBrowseProgrammes,
            onNavigateToActiveProgramme = onNavigateToActiveProgramme,
        )
    }
}

@Composable
fun WorkoutHubScreen(
    onStartActiveWorkout: () -> Unit,
    onStartTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        WorkoutHubScreen(
            onStartActiveWorkout = onStartActiveWorkout,
            onStartTemplate = onStartTemplate,
        )
    }
}

@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onSelectExercise: () -> Unit,
    workoutViewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        WorkoutScreen(
            onBack = onBack,
            onSelectExercise = onSelectExercise,
            viewModel = workoutViewModel,
        )
    }
}

@Composable
fun HistoryScreen(
    onViewWorkout: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HistoryScreen(onViewWorkout = onViewWorkout)
    }
}
