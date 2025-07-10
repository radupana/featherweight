package com.github.radupana.featherweight.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.ui.components.*
import com.github.radupana.featherweight.viewmodel.ProgrammeGeneratorViewModel
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeGeneratorScreen(
    onNavigateBack: () -> Unit,
    programmeViewModel: ProgrammeViewModel,
) {
    val viewModel: ProgrammeGeneratorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Reset state when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    BackHandler {
        if (uiState.currentStep != WizardStep.QUICK_SETUP) {
            viewModel.navigateToPreviousStep()
        } else {
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Generate Programme") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.currentStep != WizardStep.QUICK_SETUP) {
                                viewModel.navigateToPreviousStep()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = {
                        when (uiState.currentStep) {
                            WizardStep.QUICK_SETUP -> 0.33f
                            WizardStep.ABOUT_YOU -> 0.66f
                            WizardStep.CUSTOMIZE -> 1f
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Step indicator
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    WizardStep.values().forEachIndexed { index, step ->
                        StepIndicator(
                            stepNumber = index + 1,
                            stepName =
                                when (step) {
                                    WizardStep.QUICK_SETUP -> "Setup"
                                    WizardStep.ABOUT_YOU -> "About You"
                                    WizardStep.CUSTOMIZE -> "Customize"
                                },
                            isCompleted = uiState.isStepComplete(step),
                            isCurrent = uiState.currentStep == step,
                        )
                    }
                }

                // Content based on current step
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "step_transition",
                ) { step ->
                    when (step) {
                        WizardStep.QUICK_SETUP ->
                            QuickSetupStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNext = { viewModel.navigateToNextStep() },
                            )
                        WizardStep.ABOUT_YOU ->
                            AboutYouStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNext = { viewModel.navigateToNextStep() },
                                onBack = { viewModel.navigateToPreviousStep() },
                            )
                        WizardStep.CUSTOMIZE ->
                            CustomizeStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateToPreviousStep() },
                                onGenerate = {
                                    viewModel.generateProgramme(
                                        onNavigateToProgrammes = {
                                            programmeViewModel.refreshData()
                                            onNavigateBack()
                                        },
                                    )
                                },
                                focusRequester = remember { FocusRequester() },
                            )
                    }
                }
            }
        }

        // Loading overlay when generating
        if (uiState.isLoading) {
            val animatedProgress = remember { Animatable(0f) }
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "rotation",
            )

            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "scale",
            )

            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "offsetY",
            )

            LaunchedEffect(key1 = true) {
                animatedProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 60000, easing = LinearEasing),
                )
            }

            var currentMessageIndex by remember { mutableIntStateOf(0) }
            val loadingMessages =
                listOf(
                    "Analyzing your training preferences...",
                    "Designing progressive overload strategy...",
                    "Selecting optimal exercises...",
                    "Calculating personalized weights...",
                    "Structuring your programme...",
                    "Finalizing week-by-week progression...",
                )

            LaunchedEffect(Unit) {
                while (uiState.isLoading) {
                    delay(8000)
                    currentMessageIndex = (currentMessageIndex + 1) % loadingMessages.size
                }
            }

            Dialog(
                onDismissRequest = { },
                properties =
                    DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                        usePlatformDefaultWidth = false,
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .blur(radius = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        // Layered progress indicators
                        Box(contentAlignment = Alignment.Center) {
                            // Outer ring
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(200.dp)
                                        .rotate(rotation)
                                        .alpha(0.3f),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 8.dp,
                            )

                            // Middle ring
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(160.dp)
                                        .rotate(-rotation * 1.5f)
                                        .alpha(0.5f),
                                color = MaterialTheme.colorScheme.secondary,
                                strokeWidth = 6.dp,
                            )

                            // Inner ring
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(120.dp)
                                        .rotate(rotation * 2f)
                                        .alpha(0.7f),
                                color = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 4.dp,
                            )

                            // Central icon
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(60.dp)
                                        .scale(scale)
                                        .offset(y = offsetY.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Loading text with fade animation
                        AnimatedContent(
                            targetState = loadingMessages[currentMessageIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith
                                    fadeOut(animationSpec = tween(500))
                            },
                            label = "loading_message",
                        ) { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier =
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    stepNumber: Int,
    stepName: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color =
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        shape = MaterialTheme.shapes.small,
                    ),
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(
                    stepNumber.toString(),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            stepName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun QuickSetupStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onNext: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Goal selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "What's your primary goal?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ProgrammeGoal.values()) { goal ->
                    FilterChip(
                        selected = uiState.selectedGoal == goal,
                        onClick = { viewModel.selectGoal(goal) },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(goal.displayName)
                                Text(goal.emoji)
                            }
                        },
                    )
                }
            }
        }

        // Training frequency
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "How often can you train?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TrainingFrequency.values()) { frequency ->
                    FilterChip(
                        selected = uiState.selectedFrequency == frequency,
                        onClick = { viewModel.selectFrequency(frequency) },
                        label = { Text(frequency.displayName) },
                    )
                }
            }
        }

        // Session duration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "How long are your sessions?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SessionDuration.values()) { duration ->
                    FilterChip(
                        selected = uiState.selectedDuration == duration,
                        onClick = { viewModel.selectDuration(duration) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(duration.displayName)
                                Text(
                                    duration.minutesRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button
        Button(
            onClick = onNext,
            enabled = uiState.isStepComplete(WizardStep.QUICK_SETUP),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next")
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun AboutYouStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Experience level selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var showExperienceDialog by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "What's your experience level?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = { showExperienceDialog = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Experience level definitions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ExperienceLevel.values()) { experience ->
                    FilterChip(
                        selected = uiState.selectedExperience == experience,
                        onClick = { viewModel.selectExperienceLevel(experience) },
                        label = { Text(experience.displayName) },
                    )
                }
            }

            if (showExperienceDialog) {
                ExperienceLevelDialog(
                    onDismiss = { showExperienceDialog = false },
                )
            }
        }

        // Equipment selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "What equipment do you have access to?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(EquipmentAvailability.values()) { equipment ->
                    FilterChip(
                        selected = uiState.selectedEquipment == equipment,
                        onClick = { viewModel.selectEquipment(equipment) },
                        label = { Text(equipment.displayName) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Back")
            }
            Button(
                onClick = onNext,
                enabled = uiState.isStepComplete(WizardStep.ABOUT_YOU),
                modifier = Modifier.weight(1f),
            ) {
                Text("Next")
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun CustomizeStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(), // Handle keyboard insets
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 80.dp) // Reserve space for fixed buttons
                    .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Text(
                "Add custom instructions (optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Text input area
            OutlinedTextField(
                value = uiState.customInstructions,
                onValueChange = {
                    if (it.length <= 1000) {
                        viewModel.updateCustomInstructions(it)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 200.dp) // Ensure minimum height
                        .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "Add any additional information about your training goals, injuries, preferences, etc.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                maxLines = Int.MAX_VALUE,
            )

            // Character counter
            Text(
                "${uiState.customInstructions.length} / 1000 characters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                textAlign = TextAlign.End,
            )
        }

        // Fixed bottom section with buttons
        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                // Error message (if any)
                uiState.errorMessage?.let { error ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    ) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Back")
                    }
                    Button(
                        onClick = onGenerate,
                        enabled = uiState.canGenerate() && !uiState.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Generate")
                    }
                }
            }
        }
    }
}

@Composable
fun ExperienceLevelDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Experience Levels",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ExperienceLevelItem(
                    title = "Beginner (0-1 year)",
                    points =
                        listOf(
                            "Learning movement patterns",
                            "Building base strength",
                            "Linear progression works well",
                            "Example: Can't bench bodyweight yet",
                        ),
                )

                ExperienceLevelItem(
                    title = "Intermediate (1-3 years)",
                    points =
                        listOf(
                            "Solid technique on main lifts",
                            "Needs periodization",
                            "Some plateaus appearing",
                            "Example: 1.5x BW squat, 1x BW bench",
                        ),
                )

                ExperienceLevelItem(
                    title = "Advanced (3-5+ years)",
                    points =
                        listOf(
                            "Refined technique",
                            "Requires advanced programming",
                            "Progress measured monthly",
                            "Example: 2x BW squat, 1.5x BW bench",
                        ),
                )

                ExperienceLevelItem(
                    title = "Elite (5+ years competitive)",
                    points =
                        listOf(
                            "Competition-level lifts",
                            "Highly specific programming",
                            "Progress measured quarterly",
                            "Example: Regional/national competitor",
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}

@Composable
private fun ExperienceLevelItem(
    title: String,
    points: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        points.forEach { point ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(
                    point,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
