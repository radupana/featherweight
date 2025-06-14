package com.github.radupana.featherweight

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.AnalyticsScreen
import com.github.radupana.featherweight.ui.screens.ExerciseSelectorScreen
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.UserSelectionScreen
import com.github.radupana.featherweight.ui.screens.WorkoutHubScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.AnalyticsViewModel
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

enum class Screen {
    SPLASH,
    USER_SELECTION,
    HOME,
    WORKOUT_HUB,
    ACTIVE_WORKOUT,
    EXERCISE_SELECTOR,
    HISTORY,
    ANALYTICS,
    PROGRAMMES,
    ACTIVE_PROGRAMME,
    PROFILE,
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
                val userPreferences = remember { UserPreferences(application) }

                // Seed database early
                LaunchedEffect(Unit) {
                    val repository = FeatherweightRepository(application)
                    repository.seedDatabaseIfEmpty()
                    repository.seedTestUsers()
                }

                when (currentScreen) {
                    Screen.SPLASH ->
                        SplashScreen(
                            onSplashFinished = {
                                currentScreen =
                                    if (userPreferences.hasSelectedUser()) {
                                        Screen.HOME
                                    } else {
                                        Screen.USER_SELECTION
                                    }
                            },
                        )

                    Screen.USER_SELECTION ->
                        UserSelectionScreen(
                            onUserSelected = {
                                currentScreen = Screen.HOME
                            },
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

@OptIn(ExperimentalMaterial3Api::class)
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
            NavigationItem(Screen.HOME, "Home", Icons.Filled.FitnessCenter),
            NavigationItem(Screen.PROGRAMMES, "Programmes", Icons.Filled.Schedule),
            NavigationItem(Screen.HISTORY, "History", Icons.Filled.History),
            NavigationItem(Screen.ANALYTICS, "Analytics", Icons.Filled.Analytics),
        )

    // Determine if we should show the top bar with profile icon
    val showTopBar =
        currentScreen in
            listOf(
                Screen.PROGRAMMES,
                Screen.HISTORY,
                Screen.ANALYTICS,
            )

    val screenTitle =
        when (currentScreen) {
            Screen.PROGRAMMES -> "Programmes"
            Screen.HISTORY -> "History"
            Screen.ANALYTICS -> "Analytics"
            else -> ""
        }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            screenTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(onClick = { onScreenChange(Screen.PROFILE) }) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.SPLASH &&
                currentScreen != Screen.USER_SELECTION &&
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
            Screen.HOME -> {
                val programmeViewModel: ProgrammeViewModel = viewModel()

                // Refresh programme data when returning to home screen
                LaunchedEffect(currentScreen) {
                    programmeViewModel.refreshData()
                }

                HomeScreen(
                    onStartFreestyle = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onBrowseProgrammes = { onScreenChange(Screen.PROGRAMMES) },
                    onNavigateToActiveProgramme = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    onNavigateToProfile = { onScreenChange(Screen.PROFILE) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.WORKOUT_HUB ->
                WorkoutHubScreen(
                    onStartActiveWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onStartTemplate = { onScreenChange(Screen.PROGRAMMES) },
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.ACTIVE_WORKOUT -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val programmeViewModel: ProgrammeViewModel = viewModel()
                WorkoutScreen(
                    onBack = {
                        // Refresh programme data if this was a programme workout
                        val workoutState = workoutViewModel.workoutState.value
                        if (workoutState.isProgrammeWorkout) {
                            programmeViewModel.refreshData()
                        }

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
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.ACTIVE_PROGRAMME -> {
                val programmeViewModel: ProgrammeViewModel = viewModel()
                val workoutViewModel: WorkoutViewModel = viewModel()

                // Refresh programme data when returning to this screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.ACTIVE_PROGRAMME) {
                        programmeViewModel.refreshData()
                    }
                }

                com.github.radupana.featherweight.ui.screens.ActiveProgrammeScreen(
                    onBack = { onScreenChange(Screen.HOME) },
                    onStartProgrammeWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    programmeViewModel = programmeViewModel,
                    workoutViewModel = workoutViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.SPLASH -> {
                // Should not reach here
            }

            Screen.USER_SELECTION -> {
                // Should not reach here - handled in parent
            }

            Screen.PROFILE -> {
                com.github.radupana.featherweight.ui.screens.ProfileScreen(
                    onBack = { onScreenChange(Screen.HOME) },
                    modifier = Modifier.padding(innerPadding),
                )
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
