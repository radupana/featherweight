package com.github.radupana.featherweight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.InsightsScreen
import com.github.radupana.featherweight.ui.screens.ExerciseSelectorScreen
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.UserSelectionScreen
import com.github.radupana.featherweight.ui.screens.WorkoutHubScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.InsightsViewModel
import com.github.radupana.featherweight.viewmodel.HistoryViewModel
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.RestTimerViewModel
import com.github.radupana.featherweight.viewmodel.RestTimerViewModelFactory
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel

enum class Screen {
    SPLASH,
    USER_SELECTION,
    HOME,
    WORKOUT_HUB,
    ACTIVE_WORKOUT,
    EXERCISE_SELECTOR,
    HISTORY,
    INSIGHTS,
    PROGRAMMES,
    ACTIVE_PROGRAMME,
    PROGRAMME_GENERATOR,
    PROGRAMME_PREVIEW,
    PROFILE,
    EXERCISE_PROGRESS,
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

class MainActivity : ComponentActivity() {
    
    companion object {
        init {
            android.util.Log.e("FeatherweightDebug", "MainActivity: Companion object initialized")
        }
    }
    
    init {
        android.util.Log.e("FeatherweightDebug", "MainActivity: Class initialized")
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: Starting")
        installSplashScreen()
        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: Splash screen installed")

        super.onCreate(savedInstanceState)
        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: super.onCreate completed")
        
        // Request notification permission for Android 13+
        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: Checking notification permission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Enable edge-to-edge display for modern look
        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: Enabling edge-to-edge")
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Configure keyboard behavior - this is crucial for proper keyboard handling
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: About to setContent")
        try {
            setContent {
                android.util.Log.e("FeatherweightDebug", "MainActivity.setContent: Inside setContent")
                FeatherweightTheme {
                android.util.Log.e("FeatherweightDebug", "MainActivity.setContent: Inside FeatherweightTheme")
                val userPreferences = remember { UserPreferences(application) }
                var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                var selectedExerciseName by rememberSaveable { mutableStateOf("") }
                
                // App-level ViewModels for persistence across screens
                android.util.Log.e("FeatherweightDebug", "MainActivity.setContent: Creating RestTimerViewModel")
                val restTimerViewModel: RestTimerViewModel = viewModel(
                    factory = RestTimerViewModelFactory(this@MainActivity)
                )
                android.util.Log.e("FeatherweightDebug", "MainActivity.setContent: RestTimerViewModel created")

                // Seed database early
                LaunchedEffect(Unit) {
                    try {
                        android.util.Log.e("FeatherweightDebug", "MainActivity.LaunchedEffect: Creating repository")
                        val repository = FeatherweightRepository(application)
                        android.util.Log.e("FeatherweightDebug", "MainActivity.LaunchedEffect: Repository created, seeding database")
                        repository.seedDatabaseIfEmpty()
                        android.util.Log.e("FeatherweightDebug", "MainActivity.LaunchedEffect: Database seeded, seeding test users")
                        repository.seedTestUsers()
                        android.util.Log.e("FeatherweightDebug", "MainActivity.LaunchedEffect: Test users seeded")
                    } catch (e: Exception) {
                        android.util.Log.e("FeatherweightDebug", "MainActivity.LaunchedEffect: Exception caught!", e)
                        e.printStackTrace()
                    }
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
                            restTimerViewModel = restTimerViewModel,
                            selectedExerciseName = selectedExerciseName,
                            onSelectedExerciseNameChange = { exerciseName -> selectedExerciseName = exerciseName },
                        )
                    }
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("FeatherweightDebug", "MainActivity.onCreate: Exception in setContent!", e)
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppWithNavigation(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    restTimerViewModel: RestTimerViewModel,
    selectedExerciseName: String,
    onSelectedExerciseNameChange: (String) -> Unit,
) {
    // Track previous screen for proper back navigation
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var lastScreen by remember { mutableStateOf(currentScreen) }

    // Update previous screen when screen changes
    LaunchedEffect(currentScreen) {
        if (currentScreen != lastScreen) {
            // Save the last screen as previous before updating
            if (currentScreen == Screen.ACTIVE_WORKOUT || currentScreen == Screen.EXERCISE_SELECTOR || currentScreen == Screen.PROGRAMME_PREVIEW) {
                // For these screens, keep the previousScreen from before navigation
                // This ensures proper back navigation
            } else {
                previousScreen = lastScreen
            }
            lastScreen = currentScreen
        }
    }

    val navigationItems =
        listOf(
            NavigationItem(Screen.HOME, "Home", Icons.Filled.Home),
            NavigationItem(Screen.HISTORY, "Workouts", Icons.Filled.FitnessCenter),
            NavigationItem(Screen.INSIGHTS, "Insights", Icons.Filled.Insights),
        )

    // Determine if we should show the top bar with profile icon
    val showTopBar =
        currentScreen in
            listOf(
                Screen.HOME,
                Screen.PROGRAMMES,
                Screen.HISTORY,
                Screen.INSIGHTS,
            )

    val screenTitle =
        when (currentScreen) {
            Screen.HOME -> "Featherweight"
            Screen.PROGRAMMES -> "Programmes"
            Screen.HISTORY -> "History"
            Screen.INSIGHTS -> "Insights"
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
                currentScreen != Screen.EXERCISE_SELECTOR &&
                currentScreen != Screen.PROGRAMME_GENERATOR &&
                currentScreen != Screen.PROGRAMME_PREVIEW
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
                    onStartProgrammeWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onGenerateAIProgramme = { onScreenChange(Screen.PROGRAMME_GENERATOR) },
                    onNavigateToHistory = { onScreenChange(Screen.HISTORY) },
                    onNavigateToAnalytics = { onScreenChange(Screen.INSIGHTS) },
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
                    restTimerViewModel = restTimerViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.EXERCISE_SELECTOR -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val swappingExercise by workoutViewModel.swappingExercise.collectAsState()
                
                ExerciseSelectorScreen(
                    onExerciseSelected = { exercise ->
                        if (swappingExercise != null) {
                            // We're in swap mode
                            workoutViewModel.confirmExerciseSwap(exercise.exercise.id)
                        } else {
                            // Normal add mode
                            workoutViewModel.addExerciseToCurrentWorkout(exercise)
                        }
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onCreateCustomExercise = { name ->
                        workoutViewModel.addExerciseToCurrentWorkout(name)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onBack = { 
                        workoutViewModel.cancelExerciseSwap()
                        onScreenChange(Screen.ACTIVE_WORKOUT) 
                    },
                    isSwapMode = swappingExercise != null,
                    currentExercise = swappingExercise,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.HISTORY -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val historyViewModel: HistoryViewModel = viewModel()
                
                // Refresh history data when returning to this screen
                LaunchedEffect(currentScreen) {
                    historyViewModel.refreshHistory()
                }
                
                HistoryScreen(
                    onViewWorkout = { workoutId ->
                        workoutViewModel.resumeWorkout(workoutId)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onNavigateToExercise = { exerciseName ->
                        onSelectedExerciseNameChange(exerciseName)
                        onScreenChange(Screen.EXERCISE_PROGRESS)
                    },
                    historyViewModel = historyViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.INSIGHTS -> {
                val insightsViewModel: InsightsViewModel = viewModel()
                InsightsScreen(
                    viewModel = insightsViewModel,
                    onNavigateToExercise = { exerciseName ->
                        onSelectedExerciseNameChange(exerciseName)
                        onScreenChange(Screen.EXERCISE_PROGRESS)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROGRAMMES -> {
                val profileViewModel: com.github.radupana.featherweight.viewmodel.ProfileViewModel = viewModel()
                com.github.radupana.featherweight.ui.screens.ProgrammesScreen(
                    profileViewModel = profileViewModel,
                    onNavigateToActiveProgramme = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    onNavigateToAIGenerator = { onScreenChange(Screen.PROGRAMME_GENERATOR) },
                    onNavigateToAIProgrammePreview = { onScreenChange(Screen.PROGRAMME_PREVIEW) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.ACTIVE_PROGRAMME -> {
                val programmeViewModel: ProgrammeViewModel = viewModel()
                val workoutViewModel: WorkoutViewModel = viewModel()

                // Refresh programme data and in-progress workouts when returning to this screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.ACTIVE_PROGRAMME) {
                        programmeViewModel.refreshData()
                        workoutViewModel.loadInProgressWorkouts()
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

            Screen.PROGRAMME_GENERATOR -> {
                com.github.radupana.featherweight.ui.screens.ProgrammeGeneratorScreen(
                    onBack = { onScreenChange(Screen.PROGRAMMES) },
                    onNavigateToPreview = { onScreenChange(Screen.PROGRAMME_PREVIEW) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROGRAMME_PREVIEW -> {
                com.github.radupana.featherweight.ui.screens.ProgrammePreviewScreen(
                    onBack = { onScreenChange(previousScreen ?: Screen.PROGRAMMES) },
                    onActivated = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROFILE -> {
                com.github.radupana.featherweight.ui.screens.ProfileScreen(
                    onBack = { onScreenChange(Screen.HOME) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            
            Screen.EXERCISE_PROGRESS -> {
                com.github.radupana.featherweight.ui.screens.ExerciseProgressScreen(
                    exerciseName = selectedExerciseName,
                    onBack = { onScreenChange(previousScreen ?: Screen.HISTORY) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
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
    restTimerViewModel: RestTimerViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        WorkoutScreen(
            onBack = onBack,
            onSelectExercise = onSelectExercise,
            viewModel = workoutViewModel,
            restTimerViewModel = restTimerViewModel,
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
