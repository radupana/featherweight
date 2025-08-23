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
import com.github.radupana.featherweight.viewmodel.ImportProgrammeViewModel
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
    PROFILE,
    EXERCISE_PROGRESS,
    PROGRAMME_HISTORY_DETAIL,
    WORKOUT_TEMPLATE_CONFIGURATION,
    WORKOUT_COMPLETION,
    PROGRAMME_COMPLETION,
    IMPORT_PROGRAMME,
    EXERCISE_MAPPING,
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
        ) { _ ->
            // Permission result handled in UI - no action needed as notification permission is optional
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
                            Log.e("MainActivity", "Database seeding error", e)
                            // Database seeding errors are non-critical - app continues to function
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
            Log.e("MainActivity", "UI initialization error", e)
            // UI initialization errors are handled gracefully by the framework
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
    // Track initial text for import programme screen
    var importProgrammeInitialText by remember { mutableStateOf<String?>(null) }
    // Track parsed programme for import screen
    var importParsedProgramme by remember { mutableStateOf<com.github.radupana.featherweight.data.ParsedProgramme?>(null) }

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
                        Screen.PROGRAMME_HISTORY_DETAIL,
                        Screen.WORKOUT_TEMPLATE_CONFIGURATION,
                        Screen.WORKOUT_COMPLETION,
                        Screen.PROGRAMME_COMPLETION,
                        Screen.EXERCISE_MAPPING,
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
                val importViewModel: ImportProgrammeViewModel = viewModel()
                val coroutineScope = rememberCoroutineScope()
                WorkoutScreen(
                    onBack = {
                        // Refresh programme data if this was a programme workout
                        val workoutState = workoutViewModel.workoutState.value
                        if (workoutState.isProgrammeWorkout) {
                            programmeViewModel.refreshData()
                        }

                        // If we're in template edit mode, go back to import screen
                        if (workoutState.mode == com.github.radupana.featherweight.data.WorkoutMode.TEMPLATE_EDIT) {
                            onScreenChange(Screen.IMPORT_PROGRAMME)
                        } else if (workoutState.isReadOnly &&
                            (previousScreen == Screen.HISTORY || previousScreen == Screen.PROGRAMME_HISTORY_DETAIL)
                        ) {
                            // If we're in read-only mode and came from History or Programme History Detail, go back
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
                    onTemplateSaved = {
                        Log.d("MainActivity", "onTemplateSaved called")
                        // Get the saved workout from WorkoutViewModel
                        val workoutState = workoutViewModel.workoutState.value
                        val weekIndex = workoutState.templateWeekIndex
                        val workoutIndex = workoutState.templateWorkoutIndex
                        
                        Log.d("MainActivity", "Week index: $weekIndex, Workout index: $workoutIndex")
                        
                        if (weekIndex != null && workoutIndex != null) {
                            // Get current workout data
                            val exercises = workoutViewModel.selectedWorkoutExercises.value
                            val sets = workoutViewModel.selectedExerciseSets.value
                            val exerciseNames = workoutViewModel.exerciseNames.value
                            
                            // Create updated ParsedWorkout
                            val parsedExercises = exercises.map { exerciseLog ->
                                val exerciseSets = sets.filter { it.exerciseLogId == exerciseLog.id }
                                com.github.radupana.featherweight.data.ParsedExercise(
                                    exerciseName = exerciseNames[exerciseLog.exerciseVariationId] ?: "Unknown Exercise",
                                    sets = exerciseSets.map { setLog ->
                                        com.github.radupana.featherweight.data.ParsedSet(
                                            // In template edit mode, we're editing TARGET values (what the programme prescribes)
                                            reps = setLog.targetReps,
                                            weight = setLog.targetWeight,
                                            rpe = null
                                        )
                                    },
                                    notes = exerciseLog.notes
                                )
                            }
                            
                            val updatedWorkout = com.github.radupana.featherweight.data.ParsedWorkout(
                                dayOfWeek = null,  // Use null for numbered days
                                name = workoutState.workoutName ?: "",
                                exercises = parsedExercises,
                                estimatedDurationMinutes = exercises.size * 15
                            )
                            
                            // Update the parsed programme in ImportProgrammeViewModel
                            importViewModel.updateParsedWorkout(weekIndex, workoutIndex, updatedWorkout)
                        } else {
                            Log.e("MainActivity", "Week or workout index is null!")
                        }
                        
                        // Delay navigation to show the "Saved" feedback
                        Log.d("MainActivity", "Starting navigation delay...")
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(800)
                            Log.d("MainActivity", "Navigation delay complete, navigating to IMPORT_PROGRAMME")
                            // Navigate back to import screen
                            onScreenChange(Screen.IMPORT_PROGRAMME)
                        }
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
                val importViewModel: ImportProgrammeViewModel = viewModel()
                
                // Refresh data when navigating to this screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.PROGRAMMES) {
                        programmeViewModel.refreshData()
                    }
                }

                com.github.radupana.featherweight.ui.screens.ProgrammesScreen(
                    viewModel = programmeViewModel,
                    onNavigateToActiveProgramme = { onScreenChange(Screen.ACTIVE_PROGRAMME) },
                    onNavigateToImport = { onScreenChange(Screen.IMPORT_PROGRAMME) },
                    onNavigateToImportWithText = { text ->
                        importProgrammeInitialText = text
                        onScreenChange(Screen.IMPORT_PROGRAMME)
                    },
                    onNavigateToImportWithParsedProgramme = { programme, requestId ->
                        importParsedProgramme = programme
                        // Store the request ID so we can mark it as IMPORTED later
                        importViewModel.setParsedProgramme(programme, requestId)
                        onScreenChange(Screen.IMPORT_PROGRAMME)
                    },
                    onClearImportedProgramme = {
                        // Clear all programme import state when parse request is deleted
                        importParsedProgramme = null
                        importProgrammeInitialText = null
                        importViewModel.clearAll() // Clear the view model state too
                    },
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
            
            Screen.IMPORT_PROGRAMME -> {
                val importViewModel: ImportProgrammeViewModel = viewModel()
                val workoutViewModel: WorkoutViewModel = viewModel()
                
                // Set parsed programme if we have one AND the view model doesn't already have one
                // This prevents overwriting edits when navigating back from workout edit
                LaunchedEffect(importParsedProgramme) {
                    importParsedProgramme?.let {
                        val currentProgramme = importViewModel.uiState.value.parsedProgramme
                        if (currentProgramme == null) {
                            importViewModel.setParsedProgramme(it)
                        }
                    }
                }
                
                com.github.radupana.featherweight.ui.screens.ImportProgrammeScreen(
                    onBack = { 
                        importProgrammeInitialText = null // Clear the text when navigating back
                        importParsedProgramme = null // Clear the parsed programme
                        onScreenChange(Screen.PROGRAMMES) 
                    },
                    onProgrammeCreated = {
                        importProgrammeInitialText = null // Clear the text after creation
                        importParsedProgramme = null // Clear the parsed programme
                        onScreenChange(Screen.PROGRAMMES) 
                    },
                    onNavigateToProgrammes = { 
                        importProgrammeInitialText = null // Clear the text when navigating
                        importParsedProgramme = null // Clear the parsed programme
                        onScreenChange(Screen.PROGRAMMES) 
                    },
                    onNavigateToWorkoutEdit = { weekIndex, workoutIndex ->
                        Log.d("MainActivity", "onNavigateToWorkoutEdit: week=$weekIndex, workout=$workoutIndex")
                        // Get the workout to edit
                        val parsedWorkout = importViewModel.getParsedWorkout(weekIndex, workoutIndex)
                        if (parsedWorkout != null) {
                            Log.d("MainActivity", "Retrieved parsedWorkout: $parsedWorkout")
                            // Start template edit mode in workout view model
                            workoutViewModel.startTemplateEdit(weekIndex, workoutIndex, parsedWorkout)
                            // Navigate to workout screen
                            onScreenChange(Screen.ACTIVE_WORKOUT)
                        } else {
                            Log.e("MainActivity", "Failed to get parsed workout!")
                        }
                    },
                    onNavigateToExerciseMapping = {
                        // Navigate to exercise mapping screen
                        onScreenChange(Screen.EXERCISE_MAPPING)
                    },
                    initialText = importProgrammeInitialText,
                    modifier = Modifier.padding(innerPadding),
                    viewModel = importViewModel
                )
            }
            
            Screen.EXERCISE_MAPPING -> {
                val importViewModel: ImportProgrammeViewModel = viewModel()
                val uiState by importViewModel.uiState.collectAsState()
                val programme = uiState.parsedProgramme
                
                if (programme != null && programme.unmatchedExercises.isNotEmpty()) {
                    com.github.radupana.featherweight.ui.screens.ExerciseMappingScreen(
                        unmatchedExercises = programme.unmatchedExercises,
                        onMappingComplete = { mappings ->
                            // Store the mappings and navigate back
                            importViewModel.setExerciseMappings(mappings)
                            importViewModel.applyExerciseMappings()
                            onScreenChange(Screen.IMPORT_PROGRAMME)
                        },
                        onBack = {
                            onScreenChange(Screen.IMPORT_PROGRAMME)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    // No unmatched exercises, go back
                    onScreenChange(Screen.IMPORT_PROGRAMME)
                }
            }
        }
    }
}
