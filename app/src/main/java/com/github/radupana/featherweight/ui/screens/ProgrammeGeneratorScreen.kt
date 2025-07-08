package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.ui.components.*
import com.github.radupana.featherweight.viewmodel.ProgrammeGeneratorViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ProgrammeGeneratorScreen(
    onBack: () -> Unit,
    onNavigateToPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProgrammeGeneratorViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }
    
    // Focus management for text input
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Block navigation when loading
    BackHandler(enabled = uiState.isLoading) {
        // Do nothing - prevent back navigation during loading
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "Generate Programme",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                // Progress indicator
                WizardProgressIndicator(
                    currentStep = uiState.currentStep,
                    onStepClick = { step ->
                        if (viewModel.isStepCompleted(step)) {
                            viewModel.navigateToStep(step)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Wizard steps
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "wizard step animation"
            ) { step ->
                when (step) {
                    WizardStep.QUICK_SETUP -> QuickSetupStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNext = { viewModel.navigateToNextStep() }
                    )
                    WizardStep.ABOUT_YOU -> AboutYouStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNext = { viewModel.navigateToNextStep() },
                        onBack = { viewModel.navigateToPreviousStep() }
                    )
                    WizardStep.CUSTOMIZE -> CustomizeStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToPreviousStep() },
                        onGenerate = { viewModel.generateProgramme(onBack) },
                        focusRequester = focusRequester,
                        modifier = Modifier.imePadding()
                    )
                }
            }
            // Mode selector removed - only using simplified approach
        }
    }



    // Custom Instructions Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    "Custom Instructions",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Add anything else you'd like to share with the AI, such as:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "• Recovering from specific injuries or limitations",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Weak areas you want to focus on",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Your training history and experience",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Time constraints or schedule preferences",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Equipment you prefer or want to avoid",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Training style preferences (volume, intensity, etc.)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false }
                ) {
                    Text("Got it")
                }
            }
        )
    }
    
    // Full-screen loading overlay
    if (uiState.isLoading) {
        var loadingMessage by remember { mutableStateOf("Analyzing your training preferences...") }
        
        LaunchedEffect(Unit) {
            val messages = listOf(
                "Analyzing your training preferences...",
                "Designing progressive overload strategy...",
                "Selecting optimal exercises...",
                "Calculating personalized weights...",
                "Structuring your programme...",
                "Finalizing week-by-week progression..."
            )
            var index = 0
            while (true) {
                delay(8000) // Change message every 8 seconds
                index = (index + 1) % messages.size
                loadingMessage = messages[index]
            }
        }
        
        Dialog(
            onDismissRequest = { /* Cannot dismiss */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Animated icon
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Generating Your Programme",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        AnimatedContent(
                            targetState = loadingMessage,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "loading message animation"
                        ) { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "This typically takes 30-60 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Please don't navigate away",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WizardProgressIndicator(
    currentStep: WizardStep,
    onStepClick: (WizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WizardStep.values().forEach { step ->
            val isCompleted = step.ordinal < currentStep.ordinal
            val isCurrent = step == currentStep
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = isCompleted) {
                        onStepClick(step)
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        )
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${step.ordinal + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Text(
                    text = when (step) {
                        WizardStep.QUICK_SETUP -> "Setup"
                        WizardStep.ABOUT_YOU -> "About You"
                        WizardStep.CUSTOMIZE -> "Customize"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (step != WizardStep.values().last()) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically),
                    thickness = 2.dp,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun QuickSetupStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Goal selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "What's your primary goal?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProgrammeGoal.values()) { goal ->
                    FilterChip(
                        selected = uiState.selectedGoal == goal,
                        onClick = { viewModel.selectGoal(goal) },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(goal.emoji)
                                Text(goal.displayName)
                            }
                        }
                    )
                }
            }
        }
        
        // Frequency selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "How often can you train?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TrainingFrequency.values()) { frequency ->
                    FilterChip(
                        selected = uiState.selectedFrequency == frequency,
                        onClick = { viewModel.selectFrequency(frequency) },
                        label = { Text(frequency.displayName) }
                    )
                }
            }
        }
        
        // Duration selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "How long are your sessions?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SessionDuration.values()) { duration ->
                    FilterChip(
                        selected = uiState.selectedDuration == duration,
                        onClick = { viewModel.selectDuration(duration) },
                        label = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(duration.displayName)
                                Text(
                                    duration.minutesRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next button
        Button(
            onClick = onNext,
            enabled = viewModel.canProceedToNextStep(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AboutYouStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Experience level selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "What's your experience level?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExperienceLevel.values()) { experience ->
                    FilterChip(
                        selected = uiState.selectedExperience == experience,
                        onClick = { viewModel.selectExperienceLevel(experience) },
                        label = { Text(experience.displayName) }
                    )
                }
            }
        }
        
        // Equipment selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "What equipment do you have access to?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EquipmentAvailability.values()) { equipment ->
                    FilterChip(
                        selected = uiState.selectedEquipment == equipment,
                        onClick = { viewModel.selectEquipment(equipment) },
                        label = { Text(equipment.displayName) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Back")
            }
            Button(
                onClick = onNext,
                enabled = viewModel.canProceedToNextStep(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeStep(
    uiState: GuidedInputState,
    viewModel: ProgrammeGeneratorViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Add custom instructions (optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Text input area (takes up all available space)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            OutlinedTextField(
                value = uiState.customInstructions,
                onValueChange = { viewModel.updateCustomInstructions(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "Add any additional information about your training goals, injuries, preferences, etc.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                supportingText = {
                    Text(
                        "${uiState.customInstructions.length} / 1000 characters",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Back")
            }
            Button(
                onClick = onGenerate,
                enabled = uiState.canGenerate() && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Generate")
            }
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

