package com.github.radupana.featherweight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.UserPreferences
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.ui.screens.ExerciseSelectorScreen
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.InsightsScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.UserSelectionScreen
import com.github.radupana.featherweight.ui.screens.WorkoutCompletionScreen
import com.github.radupana.featherweight.ui.screens.WorkoutHubScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.screens.WorkoutTemplateConfigurationScreen
import com.github.radupana.featherweight.ui.screens.WorkoutsScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme
import com.github.radupana.featherweight.viewmodel.HistoryViewModel
import com.github.radupana.featherweight.viewmodel.InsightsViewModel
import com.github.radupana.featherweight.viewmodel.ProfileViewModel
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    USER_SELECTION,
    WORKOUT_HUB,
    ACTIVE_WORKOUT,
    EXERCISE_SELECTOR,
    WORKOUTS,
    HISTORY,
    INSIGHTS,
    PROGRAMMES,
    ACTIVE_PROGRAMME,
    PROGRAMME_GENERATOR,
    PROGRAMME_PREVIEW,
    PROFILE,
    EXERCISE_PROGRESS,
    PROGRAMME_HISTORY_DETAIL,
    WORKOUT_TEMPLATE_CONFIGURATION,
    WORKOUT_COMPLETION,
    PROGRAMME_COMPLETION,
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Enable edge-to-edge display for modern look
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Configure keyboard behavior - this is crucial for proper keyboard handling
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        try {
            setContent {
                FeatherweightTheme {
                    val userPreferences = remember { UserPreferences(application) }
                    var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                    var previousScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
                    var selectedExerciseName by rememberSaveable { mutableStateOf("") }
                    var completedWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var completedProgrammeId by rememberSaveable { mutableStateOf<Long?>(null) }

                    // Seed database early
                    LaunchedEffect(Unit) {
                        try {
                            val repository = FeatherweightRepository(application)
                            repository.seedDatabaseIfEmpty()
                            repository.seedTestUsers()
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error seeding database", e)
                        }
                    }

                    when (currentScreen) {
                        Screen.SPLASH ->
                            SplashScreen(
                                onSplashFinished = {
                                    currentScreen =
                                        if (userPreferences.hasSelectedUser()) {
                                            Screen.WORKOUTS
                                        } else {
                                            Screen.USER_SELECTION
                                        }
                                },
                            )

                        Screen.USER_SELECTION ->
                            UserSelectionScreen(
                                onUserSelected = {
                                    currentScreen = Screen.WORKOUTS
                                },
                            )

                        else -> {
                            // Main app with bottom navigation
                            MainAppWithNavigation(
                                currentScreen = currentScreen,
                                onScreenChange = { screen ->
                                    previousScreen = currentScreen
                                    currentScreen = screen
                                },
                                previousScreen = previousScreen,
                                selectedExerciseName = selectedExerciseName,
                                onSelectedExerciseNameChange = { exerciseName -> selectedExerciseName = exerciseName },
                                completedWorkoutId = completedWorkoutId,
                                onCompletedWorkoutIdChange = { id -> completedWorkoutId = id },
                                completedProgrammeId = completedProgrammeId,
                                onCompletedProgrammeIdChange = { id -> completedProgrammeId = id },
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling deeplink", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppWithNavigation(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    previousScreen: Screen?,
    selectedExerciseName: String,
    onSelectedExerciseNameChange: (String) -> Unit,
    completedWorkoutId: Long?,
    onCompletedWorkoutIdChange: (Long?) -> Unit,
    completedProgrammeId: Long?,
    onCompletedProgrammeIdChange: (Long?) -> Unit,
) {
    // Track selected template
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    val navigationItems =
        listOf(
            NavigationItem(Screen.WORKOUTS, "Workouts", Icons.Filled.FitnessCenter),
            NavigationItem(Screen.PROGRAMMES, "Programmes", Icons.Filled.Schedule),
            NavigationItem(Screen.HISTORY, "History", Icons.Filled.History),
            NavigationItem(Screen.INSIGHTS, "Insights", Icons.Filled.Insights),
        )

    // Determine if we should show the top bar with profile icon
    val showTopBar =
        currentScreen in
            listOf(
                Screen.PROGRAMMES,
                Screen.WORKOUTS,
                Screen.HISTORY,
                Screen.INSIGHTS,
            )

    val screenTitle =
        when (currentScreen) {
            Screen.PROGRAMMES -> "Programmes"
            Screen.WORKOUTS -> "Workouts"
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
            val shouldHideBottomBar =
                currentScreen in
                    setOf(
                        Screen.SPLASH,
                        Screen.USER_SELECTION,
                        Screen.ACTIVE_WORKOUT,
                        Screen.EXERCISE_SELECTOR,
                        Screen.PROGRAMME_GENERATOR,
                        Screen.PROGRAMME_PREVIEW,
                        Screen.PROGRAMME_HISTORY_DETAIL,
                        Screen.WORKOUT_TEMPLATE_CONFIGURATION,
                        Screen.WORKOUT_COMPLETION,
                        Screen.PROGRAMME_COMPLETION,
                    )
            if (!shouldHideBottomBar
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
            Screen.WORKOUT_HUB ->
                WorkoutHubScreen(
                    onStartActiveWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onStartTemplate = { onScreenChange(Screen.PROGRAMMES) },
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

                        // If we're in read-only mode and came from History or Programme History Detail, go back
                        if (workoutState.isReadOnly &&
                            (previousScreen == Screen.HISTORY || previousScreen == Screen.PROGRAMME_HISTORY_DETAIL)
                        ) {
                            onScreenChange(previousScreen)
                        } else {
                            // Otherwise navigate to Workouts screen
                            onScreenChange(Screen.WORKOUTS)
                        }
                    },
                    onSelectExercise = { onScreenChange(Screen.EXERCISE_SELECTOR) },
                    onWorkoutComplete = { workoutId ->
                        onCompletedWorkoutIdChange(workoutId)
                        onScreenChange(Screen.WORKOUT_COMPLETION)
                    },
                    onProgrammeComplete = { programmeId ->
                        onCompletedProgrammeIdChange(programmeId)
                        onScreenChange(Screen.PROGRAMME_COMPLETION)
                    },
                    viewModel = workoutViewModel,
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
                            workoutViewModel.confirmExerciseSwap(exercise.variation.id)
                        } else {
                            // Normal add mode (works for both existing and newly created exercises)
                            workoutViewModel.addExerciseToCurrentWorkout(exercise.variation)
                        }
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

            Screen.WORKOUTS -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val programmeViewModel: ProgrammeViewModel = viewModel()

                WorkoutsScreen(
                    onStartFreestyle = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onStartProgrammeWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    onStartTemplate = { templateName ->
                        selectedTemplate = templateName
                        onScreenChange(Screen.WORKOUT_TEMPLATE_CONFIGURATION)
                    },
                    modifier = Modifier.padding(innerPadding),
                    workoutViewModel = workoutViewModel,
                    programmeViewModel = programmeViewModel,
                )
            }

            Screen.HISTORY -> {
                val historyViewModel: HistoryViewModel = viewModel()
                val workoutViewModel: WorkoutViewModel = viewModel()

                // Refresh history when navigating to this screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.HISTORY) {
                        historyViewModel.refreshHistory()
                    }
                }

                HistoryScreen(
                    onViewWorkout = { workoutId ->
                        // Navigate to workout detail - view completed workout in read-only mode
                        workoutViewModel.viewCompletedWorkout(workoutId)
                        onScreenChange(Screen.ACTIVE_WORKOUT)
                    },
                    onViewProgramme = { programmeId ->
                        // Store programme ID for detail view
                        historyViewModel.selectedProgrammeId = programmeId
                        onScreenChange(Screen.PROGRAMME_HISTORY_DETAIL)
                    },
                    historyViewModel = historyViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROGRAMME_HISTORY_DETAIL -> {
                val historyViewModel: HistoryViewModel = viewModel()
                val workoutViewModel: WorkoutViewModel = viewModel()
                val programmeId = historyViewModel.selectedProgrammeId
                if (programmeId != null) {
                    com.github.radupana.featherweight.ui.screens.ProgrammeHistoryDetailScreen(
                        programmeId = programmeId,
                        onBack = { onScreenChange(Screen.HISTORY) },
                        onViewWorkout = { workoutId ->
                            workoutViewModel.viewCompletedWorkout(workoutId)
                            onScreenChange(Screen.ACTIVE_WORKOUT)
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    // Fallback to history if no programme selected
                    onScreenChange(Screen.HISTORY)
                }
            }

            Screen.WORKOUT_COMPLETION -> {
                completedWorkoutId?.let { workoutId ->
                    WorkoutCompletionScreen(
                        workoutId = workoutId,
                        onDismiss = {
                            // Clear the completed workout ID
                            onCompletedWorkoutIdChange(null)
                            // Navigate to workouts screen
                            onScreenChange(Screen.WORKOUTS)
                        },
                    )
                } ?: run {
                    // If no workout ID, go back to workouts
                    onScreenChange(Screen.WORKOUTS)
                }
            }

            Screen.PROGRAMME_COMPLETION -> {
                completedProgrammeId?.let { programmeId ->
                    com.github.radupana.featherweight.ui.screens.ProgrammeCompletionScreen(
                        programmeId = programmeId,
                        onDismiss = {
                            // Clear the completed programme ID
                            onCompletedProgrammeIdChange(null)
                            // Navigate to programmes screen
                            onScreenChange(Screen.PROGRAMMES)
                        },
                    )
                } ?: run {
                    // If no programme ID, go back to programmes
                    onScreenChange(Screen.PROGRAMMES)
                }
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
                val programmeViewModel: ProgrammeViewModel = viewModel()

                // Force refresh AI requests when returning from preview screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.PROGRAMMES && previousScreen == Screen.PROGRAMME_PREVIEW) {
                        programmeViewModel.forceRefreshAIRequests()
                    }
                }

                com.github.radupana.featherweight.ui.screens.ProgrammesScreen(
                    viewModel = programmeViewModel,
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
                    onBack = { onScreenChange(Screen.PROGRAMMES) },
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
                val programmeViewModel: ProgrammeViewModel = viewModel()
                com.github.radupana.featherweight.ui.screens.ProgrammeGeneratorScreen(
                    onNavigateBack = { onScreenChange(Screen.PROGRAMMES) },
                    programmeViewModel = programmeViewModel,
                )
            }

            Screen.PROGRAMME_PREVIEW -> {
                com.github.radupana.featherweight.ui.screens.ProgrammePreviewScreen(
                    onBack = { onScreenChange(Screen.PROGRAMMES) },
                    onActivated = {
                        // Navigate to active programme after successful activation
                        onScreenChange(Screen.ACTIVE_PROGRAMME)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.PROFILE -> {
                val profileViewModel: ProfileViewModel = viewModel()
                val insightsViewModel: InsightsViewModel = viewModel()
                com.github.radupana.featherweight.ui.screens.ProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { onScreenChange(previousScreen ?: Screen.WORKOUTS) },
                    modifier = Modifier.padding(innerPadding),
                    insightsViewModel = insightsViewModel,
                )
            }

            Screen.EXERCISE_PROGRESS -> {
                com.github.radupana.featherweight.ui.screens.ExerciseProgressScreen(
                    exerciseName = selectedExerciseName,
                    onBack = { onScreenChange(previousScreen ?: Screen.WORKOUTS) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.WORKOUT_TEMPLATE_CONFIGURATION -> {
                selectedTemplate?.let { templateName ->
                    val template =
                        com.github.radupana.featherweight.data.model.WorkoutTemplates
                            .getTemplate(templateName)
                    if (template != null) {
                        val workoutViewModel: WorkoutViewModel = viewModel()
                        val scope = rememberCoroutineScope()

                        WorkoutTemplateConfigurationScreen(
                            template = template,
                            onConfigurationComplete = { config ->
                                scope.launch {
                                    val repository = workoutViewModel.repository
                                    val workoutId = repository.generateWorkoutFromTemplate(template, config)
                                    repository.applyTemplateWeightSuggestions(
                                        workoutId = workoutId,
                                        config = config,
                                        userId = repository.getCurrentUserId(),
                                    )
                                    workoutViewModel.resumeWorkout(workoutId)
                                    onScreenChange(Screen.ACTIVE_WORKOUT)
                                }
                            },
                            onBack = { onScreenChange(Screen.WORKOUTS) },
                        )
                    } else {
                        onScreenChange(Screen.WORKOUTS)
                    }
                } ?: onScreenChange(Screen.WORKOUTS)
            }
        }
    }
}
