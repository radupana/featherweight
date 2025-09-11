package com.github.radupana.featherweight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Feedback
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
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.FirebaseFeedbackService
import com.github.radupana.featherweight.ui.screens.CreateTemplateFromWorkoutScreen
import com.github.radupana.featherweight.ui.screens.ExerciseSelectorScreen
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.InsightsScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.WorkoutCompletionScreen
import com.github.radupana.featherweight.ui.screens.WorkoutHubScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.screens.WorkoutSelectionForTemplateScreen
import com.github.radupana.featherweight.ui.screens.WorkoutTemplateSelectionScreen
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
    WORKOUT_HUB,
    ACTIVE_WORKOUT,
    EXERCISE_SELECTOR,
    WORKOUTS,
    HISTORY,
    INSIGHTS,
    PROGRAMMES,
    PROFILE,
    EXERCISE_PROGRESS,
    PROGRAMME_HISTORY_DETAIL,
    WORKOUT_TEMPLATE_SELECTION,
    WORKOUT_SELECTION_FOR_TEMPLATE,
    WORKOUT_COMPLETION,
    PROGRAMME_COMPLETION,
    IMPORT_PROGRAMME,
    EXERCISE_MAPPING,
    CREATE_TEMPLATE_FROM_WORKOUT,
    CREATE_TEMPLATE_FROM_SELECTED_WORKOUT,
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

class MainActivity : ComponentActivity() {
    private companion object {
        private const val TAG = "MainActivity"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            Log.i(TAG, "Notification permission result: granted=$isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        Log.i(
            TAG,
            "App started - version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}), " +
                "debug: ${BuildConfig.DEBUG}",
        )

        // Request notification permission for Android 13+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Requesting notification permission")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.i(TAG, "Notification permission already granted")
        }

        // Enable edge-to-edge display for modern look
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Configure keyboard behavior - this is crucial for proper keyboard handling
        // Using WindowInsets API instead of deprecated SOFT_INPUT_ADJUST_RESIZE
        // The WindowCompat.setDecorFitsSystemWindows(window, false) above handles this

        try {
            setContent {
                FeatherweightTheme {
                    var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                    var previousScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
                    var selectedExerciseName by rememberSaveable { mutableStateOf("") }
                    var completedWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var completedProgrammeId by rememberSaveable { mutableStateOf<Long?>(null) }

                    // Seed database early
                    LaunchedEffect(Unit) {
                        try {
                            val repository = FeatherweightRepository(application)
                            val startTime = System.currentTimeMillis()
                            repository.seedDatabaseIfEmpty()
                            val duration = System.currentTimeMillis() - startTime
                            Log.d(TAG, "Database seeding took ${duration}ms")
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Database seeding failed", e)
                            // Database seeding errors are non-critical - app continues to function
                        }
                    }

                    when (currentScreen) {
                        Screen.SPLASH ->
                            SplashScreen(
                                onSplashFinished = {
                                    currentScreen = Screen.WORKOUTS
                                },
                            )

                        else -> {
                            // Main app with bottom navigation
                            MainAppWithNavigation(
                                currentScreen = currentScreen,
                                onScreenChange = { screen ->
                                    Log.i(
                                        TAG,
                                        "Navigation: ${currentScreen.name} -> ${screen.name}",
                                    )
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
        } catch (e: IllegalStateException) {
            Log.e(TAG, "UI initialization error", e)
            // UI initialization errors are handled gracefully by the framework
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Activity stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Activity destroyed")
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
    // Track initial text for import programme screen
    var importProgrammeInitialText by remember { mutableStateOf<String?>(null) }
    // Track parsed programme for import screen
    var importParsedProgramme by remember { mutableStateOf<com.github.radupana.featherweight.data.ParsedProgramme?>(null) }
    // Track selected workout ID for template creation
    var selectedTemplateWorkoutId by remember { mutableStateOf<Long?>(null) }

    // Initialize feedback service for debug builds
    val feedbackService = remember { FirebaseFeedbackService() }

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
                        // Add feedback button for debug builds
                        if (BuildConfig.DEBUG) {
                            IconButton(onClick = { feedbackService.startFeedback() }) {
                                Icon(
                                    Icons.Filled.Feedback,
                                    contentDescription = "Feedback",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
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
                        Screen.ACTIVE_WORKOUT,
                        Screen.EXERCISE_SELECTOR,
                        Screen.PROGRAMME_HISTORY_DETAIL,
                        Screen.WORKOUT_TEMPLATE_SELECTION,
                        Screen.WORKOUT_COMPLETION,
                        Screen.PROGRAMME_COMPLETION,
                        Screen.EXERCISE_MAPPING,
                    )
            if (!shouldHideBottomBar) {
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
                    onStartTemplate = { onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION) },
                )

            Screen.ACTIVE_WORKOUT -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val programmeViewModel: ProgrammeViewModel = viewModel()
                val importViewModel: ImportProgrammeViewModel = viewModel()
                val historyViewModel: HistoryViewModel = viewModel()
                val coroutineScope = rememberCoroutineScope()

                // Handle workout export file saving
                val historyState by historyViewModel.historyState.collectAsState()
                val saveFileLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        uri?.let {
                            historyViewModel.saveExportedFile(it)
                        }
                    }

                // Launch save dialog when export is ready
                LaunchedEffect(historyState.pendingExportFile) {
                    historyState.pendingExportFile?.let { file ->
                        saveFileLauncher.launch(file.name)
                    }
                }

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
                    onSaveAsTemplate = { workoutId ->
                        Log.i("MainActivity", "onSaveAsTemplate called from WorkoutScreen with workoutId: $workoutId")
                        Log.i("MainActivity", "Current completedWorkoutId before change: $completedWorkoutId")
                        onCompletedWorkoutIdChange(workoutId)
                        Log.i("MainActivity", "Set completedWorkoutId to: $workoutId")
                        Log.i("MainActivity", "Navigating to CREATE_TEMPLATE_FROM_WORKOUT")
                        onScreenChange(Screen.CREATE_TEMPLATE_FROM_WORKOUT)
                    },
                    onExportWorkout = { workoutId ->
                        historyViewModel.exportWorkout(workoutId)
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
                            val parsedExercises =
                                exercises.map { exerciseLog ->
                                    val exerciseSets = sets.filter { it.exerciseLogId == exerciseLog.id }
                                    com.github.radupana.featherweight.data.ParsedExercise(
                                        exerciseName = exerciseNames[exerciseLog.exerciseVariationId] ?: "Unknown Exercise",
                                        matchedExerciseId = exerciseLog.exerciseVariationId,
                                        sets =
                                            exerciseSets.map { setLog ->
                                                com.github.radupana.featherweight.data.ParsedSet(
                                                    // In template edit mode, we're editing TARGET values (what the programme prescribes)
                                                    reps = setLog.targetReps,
                                                    weight = setLog.targetWeight,
                                                    rpe = setLog.targetRpe, // Preserve RPE from parsed programme
                                                )
                                            },
                                        notes = exerciseLog.notes,
                                    )
                                }

                            val updatedWorkout =
                                com.github.radupana.featherweight.data.ParsedWorkout(
                                    dayOfWeek = null, // Use null for numbered days
                                    name = workoutState.workoutName ?: "",
                                    exercises = parsedExercises,
                                    estimatedDurationMinutes = exercises.size * 15,
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
                    onNavigateToTemplateSelection = { onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION) },
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
                Log.i("MainActivity", "WORKOUT_COMPLETION screen - completedWorkoutId: $completedWorkoutId")
                completedWorkoutId?.let { workoutId ->
                    WorkoutCompletionScreen(
                        workoutId = workoutId,
                        onDismiss = {
                            Log.i("MainActivity", "Dismissing workout completion screen")
                            // Clear the completed workout ID
                            onCompletedWorkoutIdChange(null)
                            // Navigate to workouts screen
                            onScreenChange(Screen.WORKOUTS)
                        },
                        onSaveAsTemplate = { passedWorkoutId ->
                            Log.i("MainActivity", "onSaveAsTemplate from WORKOUT_COMPLETION - passedWorkoutId: $passedWorkoutId, completedWorkoutId: $completedWorkoutId")
                            // Make sure we have the workout ID set before navigating
                            if (passedWorkoutId != completedWorkoutId) {
                                Log.w("MainActivity", "Passed workout ID ($passedWorkoutId) differs from completedWorkoutId ($completedWorkoutId), using passed ID")
                                onCompletedWorkoutIdChange(passedWorkoutId)
                            }
                            onScreenChange(Screen.CREATE_TEMPLATE_FROM_WORKOUT)
                        },
                    )
                } ?: run {
                    Log.w("MainActivity", "WORKOUT_COMPLETION - No workout ID, going back to workouts")
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
                val workoutViewModel: WorkoutViewModel = viewModel()
                val importViewModel: ImportProgrammeViewModel = viewModel()

                // Refresh data when navigating to this screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == Screen.PROGRAMMES) {
                        programmeViewModel.refreshData()
                        workoutViewModel.loadInProgressWorkouts()
                    }
                }

                com.github.radupana.featherweight.ui.screens.ProgrammesScreen(
                    viewModel = programmeViewModel,
                    workoutViewModel = workoutViewModel,
                    onNavigateToImport = { onScreenChange(Screen.IMPORT_PROGRAMME) },
                    onNavigateToImportWithText = { text, requestId ->
                        // Don't set initialText when we're directly updating the ViewModel
                        // This prevents the LaunchedEffect from overwriting our editing state
                        importProgrammeInitialText = null
                        // Pass the request ID so the ViewModel knows we're editing
                        importViewModel.updateInputText(text, requestId)
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
                    onStartProgrammeWorkout = { onScreenChange(Screen.ACTIVE_WORKOUT) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.SPLASH -> {
                // Should not reach here
            }

            Screen.PROFILE -> {
                val profileViewModel: ProfileViewModel = viewModel()
                com.github.radupana.featherweight.ui.screens.ProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { onScreenChange(previousScreen ?: Screen.WORKOUTS) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.EXERCISE_PROGRESS -> {
                com.github.radupana.featherweight.ui.screens.ExerciseProgressScreen(
                    exerciseName = selectedExerciseName,
                    onBack = { onScreenChange(previousScreen ?: Screen.WORKOUTS) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.WORKOUT_TEMPLATE_SELECTION -> {
                val workoutViewModel: WorkoutViewModel = viewModel()
                val coroutineScope = rememberCoroutineScope()

                WorkoutTemplateSelectionScreen(
                    onTemplateSelected = { templateId ->
                        Log.i("MainActivity", "Template selected: $templateId, starting workout from template...")
                        coroutineScope.launch {
                            workoutViewModel.startWorkoutFromTemplate(templateId)
                            Log.i("MainActivity", "Navigating to ACTIVE_WORKOUT")
                            onScreenChange(Screen.ACTIVE_WORKOUT)
                        }
                    },
                    onCreateFromHistory = {
                        Log.i("MainActivity", "Navigate to workout selection for template")
                        onScreenChange(Screen.WORKOUT_SELECTION_FOR_TEMPLATE)
                    },
                    onBack = { onScreenChange(Screen.WORKOUTS) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            Screen.WORKOUT_SELECTION_FOR_TEMPLATE -> {
                WorkoutSelectionForTemplateScreen(
                    onWorkoutSelected = { workoutId ->
                        Log.i("MainActivity", "Workout selected for template: $workoutId")
                        selectedTemplateWorkoutId = workoutId
                        onScreenChange(Screen.CREATE_TEMPLATE_FROM_SELECTED_WORKOUT)
                    },
                    onBack = {
                        onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
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
                    viewModel = importViewModel,
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
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    // No unmatched exercises, go back
                    onScreenChange(Screen.IMPORT_PROGRAMME)
                }
            }

            Screen.CREATE_TEMPLATE_FROM_WORKOUT -> {
                Log.i("MainActivity", "=== CREATE_TEMPLATE_FROM_WORKOUT screen entered ===")
                Log.i("MainActivity", "completedWorkoutId value: $completedWorkoutId")
                Log.i("MainActivity", "previousScreen: $previousScreen")
                Log.i("MainActivity", "currentScreen: $currentScreen")

                completedWorkoutId?.let { workoutId ->
                    Log.i("MainActivity", "Using completedWorkoutId: $workoutId")
                    CreateTemplateFromWorkoutScreen(
                        workoutId = workoutId,
                        onBack = {
                            Log.i("MainActivity", "Going back from CREATE_TEMPLATE_FROM_WORKOUT")
                            onScreenChange(Screen.WORKOUT_COMPLETION)
                        },
                        onTemplateCreated = {
                            Log.i("MainActivity", "onTemplateCreated callback invoked from CREATE_TEMPLATE_FROM_WORKOUT")
                            Log.i("MainActivity", "Current screen: $currentScreen, Previous screen: $previousScreen")
                            Log.i("MainActivity", "completedWorkoutId before clearing: $completedWorkoutId")
                            // Clear the completed workout ID
                            onCompletedWorkoutIdChange(null)
                            Log.i("MainActivity", "Cleared completedWorkoutId")
                            // Navigate to templates screen to see the new template
                            Log.i("MainActivity", "Navigating to WORKOUT_TEMPLATE_SELECTION")
                            onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION)
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                } ?: run {
                    Log.w("MainActivity", "completedWorkoutId is null! Trying fallback...")
                    // If no workout ID is set, try to get it from the current workout
                    val workoutViewModel: WorkoutViewModel = viewModel()
                    val currentWorkoutId = workoutViewModel.currentWorkoutId.collectAsState().value
                    Log.i("MainActivity", "Current workout ID from viewModel: $currentWorkoutId")

                    if (currentWorkoutId != null) {
                        Log.w("MainActivity", "Using fallback currentWorkoutId: $currentWorkoutId")
                        CreateTemplateFromWorkoutScreen(
                            workoutId = currentWorkoutId,
                            onBack = {
                                Log.i("MainActivity", "Going back from CREATE_TEMPLATE_FROM_WORKOUT (fallback)")
                                onScreenChange(Screen.ACTIVE_WORKOUT)
                            },
                            onTemplateCreated = {
                                Log.i("MainActivity", "onTemplateCreated callback invoked (fallback)")
                                Log.i("MainActivity", "Current screen before navigation: $currentScreen")
                                // Clear any workout ID
                                onCompletedWorkoutIdChange(null)
                                Log.i("MainActivity", "Cleared completedWorkoutId (fallback)")
                                // Navigate to templates screen to see the new template
                                Log.i("MainActivity", "Navigating to WORKOUT_TEMPLATE_SELECTION (fallback)")
                                onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION)
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        Log.e("MainActivity", "CRITICAL: No workout ID available for template creation!")
                        Log.e("MainActivity", "Navigating to WORKOUT_TEMPLATE_SELECTION as fallback")
                        // Go back to templates since that's where user expects to be
                        onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION)
                    }
                }
            }

            Screen.CREATE_TEMPLATE_FROM_SELECTED_WORKOUT -> {
                selectedTemplateWorkoutId?.let { workoutId ->
                    CreateTemplateFromWorkoutScreen(
                        workoutId = workoutId,
                        onBack = {
                            onScreenChange(Screen.WORKOUT_SELECTION_FOR_TEMPLATE)
                        },
                        onTemplateCreated = {
                            // Clear the selected workout ID
                            selectedTemplateWorkoutId = null
                            // Navigate to templates screen to see the new template
                            onScreenChange(Screen.WORKOUT_TEMPLATE_SELECTION)
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                } ?: run {
                    // If no workout selected, go back to selection
                    onScreenChange(Screen.WORKOUT_SELECTION_FOR_TEMPLATE)
                }
            }
        }
    }
}
