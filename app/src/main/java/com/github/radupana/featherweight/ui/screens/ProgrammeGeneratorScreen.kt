package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    
    // Block navigation when loading
    BackHandler(enabled = uiState.isLoading) {
        // Do nothing - prevent back navigation during loading
    }

    Scaffold(
        topBar = {
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
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Mode Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Generation Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GenerationMode.values().forEach { mode ->
                                val isSelected = uiState.generationMode == mode
                                if (isSelected) {
                                    Button(
                                        onClick = { viewModel.selectGenerationMode(mode) },
                                        modifier = Modifier.weight(1f),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                            ),
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.selectGenerationMode(mode) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = uiState.generationMode.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // Goal Selection (only for Simplified mode)
            if (uiState.generationMode == GenerationMode.SIMPLIFIED) {
                item {
                    Text(
                        text = "What's your main goal?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        items(ProgrammeGoal.values()) { goal ->
                            GoalChip(
                                goal = goal,
                                isSelected = uiState.selectedGoal == goal,
                                onClick = { viewModel.selectGoal(goal) },
                            )
                        }
                    }
                }

                // Frequency Selection
                item {
                    AnimatedVisibility(
                        visible = uiState.selectedGoal != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                    ) {
                        Column {
                            Text(
                                text = "How many days can you train?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                items((2..7).toList()) { frequency ->
                                    FrequencyChip(
                                        frequency = frequency,
                                        isSelected = uiState.selectedFrequency == frequency,
                                        onClick = { viewModel.selectFrequency(frequency) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration Selection
                item {
                    AnimatedVisibility(
                        visible = uiState.selectedFrequency != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                    ) {
                        Column {
                            Text(
                                text = "How long are your sessions?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                items(SessionDuration.values()) { duration ->
                                    DurationChip(
                                        duration = duration,
                                        isSelected = uiState.selectedDuration == duration,
                                        onClick = { viewModel.selectDuration(duration) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Experience Level Selection
                item {
                    AnimatedVisibility(
                        visible = uiState.selectedDuration != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                    ) {
                        Column {
                            Text(
                                text = "What's your experience level?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                items(ExperienceLevel.values()) { experience ->
                                    ExperienceChip(
                                        experience = experience,
                                        isSelected = uiState.selectedExperience == experience,
                                        onClick = { viewModel.selectExperienceLevel(experience) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Equipment Selection
                item {
                    AnimatedVisibility(
                        visible = uiState.selectedExperience != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                    ) {
                        Column {
                            Text(
                                text = "What equipment do you have?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                items(EquipmentAvailability.values()) { equipment ->
                                    EquipmentChip(
                                        equipment = equipment,
                                        isSelected = uiState.selectedEquipment == equipment,
                                        onClick = { viewModel.selectEquipment(equipment) },
                                    )
                                }
                            }
                        }
                    }
                }
            } // End Simplified mode if statement

            // Text Input Area
            item {
                Column {
                    // Custom Instructions label for Simplified mode
                    if (uiState.generationMode == GenerationMode.SIMPLIFIED) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Custom Instructions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    
                    val inputHeight =
                        when (uiState.generationMode) {
                            GenerationMode.SIMPLIFIED -> 200.dp
                            GenerationMode.ADVANCED -> 400.dp
                        }
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(inputHeight),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                        ) {
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { newText ->
                                    val maxLength =
                                        when (uiState.generationMode) {
                                            GenerationMode.SIMPLIFIED -> 500
                                            GenerationMode.ADVANCED -> 5000
                                        }
                                    if (newText.length <= maxLength) {
                                        viewModel.updateInputText(newText)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                textStyle =
                                    TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                placeholder = if (uiState.generationMode == GenerationMode.ADVANCED) {
                                    {
                                        Text(
                                            text = "Paste your full programme description here...",
                                            style =
                                                TextStyle(
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                ),
                                        )
                                    }
                                } else null,
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                    ),
                                // Enable multiline for better text input
                                singleLine = false,
                                maxLines = if (uiState.generationMode == GenerationMode.ADVANCED) 20 else 8,
                            )

                            // Character count - only show for Simplified mode
                            if (uiState.generationMode == GenerationMode.SIMPLIFIED) {
                                Text(
                                    text = "${uiState.inputText.length}/500",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (uiState.inputText.length > 450) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        },
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                )
                            }
                        }
                    }
                }
            }



            // Character count for Advanced mode
            if (uiState.generationMode == GenerationMode.ADVANCED) {
                item {
                    val maxLength = 5000
                    val currentLength = uiState.inputText.length
                    val color =
                        when {
                            currentLength > maxLength - 500 -> MaterialTheme.colorScheme.error
                            currentLength > maxLength - 1000 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                if (currentLength < 500) {
                                    "Need at least 500 characters for Advanced mode"
                                } else {
                                    "Good! Programme description looks comprehensive"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                        )
                        Text(
                            text = "$currentLength / $maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }


            // Generate Button
            item {
                Button(
                    onClick = { viewModel.generateProgramme(onBack) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    enabled =
                        when (uiState.generationMode) {
                            GenerationMode.SIMPLIFIED -> {
                                // Simplified mode: all 5 selections required, custom text optional
                                uiState.selectedGoal != null &&
                                    uiState.selectedFrequency != null &&
                                    uiState.selectedDuration != null &&
                                    uiState.selectedExperience != null &&
                                    uiState.selectedEquipment != null &&
                                    !uiState.isLoading
                            }
                            GenerationMode.ADVANCED -> {
                                // Advanced mode: requires substantial text (500+ chars)
                                uiState.inputText.length >= 500 && !uiState.isLoading
                            }
                        },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Generate Programme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
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
                                slideInVertically { height -> height } + fadeIn() with
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
