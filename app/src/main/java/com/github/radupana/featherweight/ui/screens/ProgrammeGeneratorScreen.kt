package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.*
import com.github.radupana.featherweight.ui.components.*
import com.github.radupana.featherweight.ui.dialogs.TemplateSelectionDialog
import com.github.radupana.featherweight.viewmodel.ProgrammeGeneratorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeGeneratorScreen(
    onBack: () -> Unit,
    onNavigateToPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProgrammeGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTemplateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Generate Programme",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Mode Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Generation Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenerationMode.values().forEach { mode ->
                                val isSelected = uiState.generationMode == mode
                                if (isSelected) {
                                    Button(
                                        onClick = { viewModel.selectGenerationMode(mode) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.selectGenerationMode(mode) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = uiState.generationMode.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
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
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(ProgrammeGoal.values()) { goal ->
                        GoalChip(
                            goal = goal,
                            isSelected = uiState.selectedGoal == goal,
                            onClick = { viewModel.selectGoal(goal) }
                        )
                    }
                }
            }
            
            // Frequency Selection
            item {
                AnimatedVisibility(
                    visible = uiState.selectedGoal != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column {
                        Text(
                            text = "How many days can you train?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items((2..7).toList()) { frequency ->
                                FrequencyChip(
                                    frequency = frequency,
                                    isSelected = uiState.selectedFrequency == frequency,
                                    onClick = { viewModel.selectFrequency(frequency) }
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
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column {
                        Text(
                            text = "How long are your sessions?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(SessionDuration.values()) { duration ->
                                DurationChip(
                                    duration = duration,
                                    isSelected = uiState.selectedDuration == duration,
                                    onClick = { viewModel.selectDuration(duration) }
                                )
                            }
                        }
                    }
                }
            }
            } // End Simplified mode if statement
            
            // Browse Templates Button (moved to top for easier access)
            item {
                OutlinedButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Browse Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Text Input Area
            item {
                val inputHeight = when (uiState.generationMode) {
                    GenerationMode.SIMPLIFIED -> 200.dp
                    GenerationMode.ADVANCED -> 400.dp
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(inputHeight),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = { newText ->
                                val maxLength = when (uiState.generationMode) {
                                    GenerationMode.SIMPLIFIED -> 500
                                    GenerationMode.ADVANCED -> 5000
                                }
                                if (newText.length <= maxLength) {
                                    viewModel.updateInputText(newText)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.inputText.isEmpty()) {
                                        Text(
                                            text = viewModel.getPlaceholderText(),
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        // Character count
                        Text(
                            text = "${uiState.inputText.length}/500",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.inputText.length > 450) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }
            
            // Input Validation Feedback
            item {
                InputFeedbackSection(
                    detectedElements = uiState.detectedElements,
                    completeness = uiState.inputCompleteness,
                    suggestions = viewModel.getSuggestions()
                )
            }
            
            // Character count for Advanced mode
            if (uiState.generationMode == GenerationMode.ADVANCED) {
                item {
                    val maxLength = 5000
                    val currentLength = uiState.inputText.length
                    val color = when {
                        currentLength < 500 -> MaterialTheme.colorScheme.error
                        currentLength < 1000 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLength < 500) {
                                "Need at least 500 characters for Advanced mode"
                            } else {
                                "Good! Programme description looks comprehensive"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                        Text(
                            text = "$currentLength / $maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Quick Add Chips (only for Simplified mode)
            if (uiState.generationMode == GenerationMode.SIMPLIFIED && uiState.availableChips.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Quick Add:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.availableChips) { chip ->
                                QuickAddChipComponent(
                                    chip = chip,
                                    onClick = { viewModel.addChipText(chip.text) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Generate Button
            item {
                Button(
                    onClick = { viewModel.generateProgramme(onNavigateToPreview) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = when (uiState.generationMode) {
                        GenerationMode.SIMPLIFIED -> {
                            // Simplified mode: guided values OR 20+ chars
                            ((uiState.selectedGoal != null && 
                              uiState.selectedFrequency != null && 
                              uiState.selectedDuration != null) ||
                             uiState.inputText.length >= 20) && !uiState.isLoading
                        }
                        GenerationMode.ADVANCED -> {
                            // Advanced mode: requires substantial text (2000+ chars)
                            uiState.inputText.length >= 500 && !uiState.isLoading
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Generate Programme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.generationCount < uiState.maxDailyGenerations) {
                                Text(
                                    "${uiState.maxDailyGenerations - uiState.generationCount} left today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Error message
            uiState.errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Template Selection Dialog
    if (showTemplateDialog) {
        TemplateSelectionDialog(
            selectedGoal = uiState.selectedGoal,
            selectedFrequency = uiState.selectedFrequency,
            selectedDuration = uiState.selectedDuration,
            onTemplateSelected = { template ->
                viewModel.loadTemplate(template)
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}