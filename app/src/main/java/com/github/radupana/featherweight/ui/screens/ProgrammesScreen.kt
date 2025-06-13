package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.programme.*
import com.github.radupana.featherweight.ui.dialogs.ProgrammeSetupDialog
import com.github.radupana.featherweight.ui.theme.GlassCard
import com.github.radupana.featherweight.viewmodel.ProgrammeViewModel
import com.github.radupana.featherweight.viewmodel.ProgrammeUiState

@Composable
fun ProgrammesScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgrammeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProgramme by viewModel.activeProgramme.collectAsState()
    val programmeProgress by viewModel.programmeProgress.collectAsState()

    // Handle messages
    LaunchedEffect(uiState.error, uiState.successMessage) {
        // Auto-clear messages after 3 seconds
        if (uiState.error != null || uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Programmes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Error/Success Messages
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        uiState.successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF4CAF50)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Only apply keyboard padding to the scrollable content
        ) {
            // Active Programme Section
            activeProgramme?.let { programme ->
                item {
                    ActiveProgrammeCard(
                        programme = programme,
                        progress = programmeProgress,
                        onDeactivate = { viewModel.deactivateActiveProgramme() }
                    )
                }
            }

            // Search Section
            item {
                SearchSection(
                    searchText = uiState.searchText,
                    onSearchTextChange = viewModel::updateSearchText
                )
            }

            // Filter Section
            item {
                FilterSection(
                    onDifficultyFilter = viewModel::filterByDifficulty,
                    onTypeFilter = viewModel::filterByType,
                    onClearFilters = viewModel::clearFilters,
                    hasActiveFilters = uiState.searchText.isNotEmpty()
                )
            }

            // Templates Section
            item {
                Text(
                    text = if (activeProgramme != null) "Browse Other Programmes" else "Choose a Programme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.templates) { template ->
                ProgrammeTemplateCard(
                    template = template,
                    isActive = activeProgramme?.name == template.name,
                    onClick = { 
                        if (activeProgramme?.name != template.name) {
                            viewModel.selectTemplate(template)
                        }
                    }
                )
            }

            if (uiState.templates.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateCard()
                }
            }
        }
    }

    // Setup Dialog
    if (uiState.showSetupDialog && uiState.selectedTemplate != null) {
        ProgrammeSetupDialog(
            template = uiState.selectedTemplate!!,
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ActiveProgrammeCard(
    programme: Programme,
    progress: ProgrammeProgress?,
    onDeactivate: () -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Programme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = programme.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onDeactivate) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Deactivate programme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicators
            progress?.let { prog ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProgressMetric(
                        label = "Week",
                        value = "${prog.currentWeek}/${programme.durationWeeks}",
                        modifier = Modifier.weight(1f)
                    )
                    ProgressMetric(
                        label = "Workouts",
                        value = "${prog.completedWorkouts}/${if (prog.totalWorkouts > 0) prog.totalWorkouts else "0"}",
                        modifier = Modifier.weight(1f)
                    )
                    ProgressMetric(
                        label = "Adherence",
                        value = "${if (prog.adherencePercentage.isNaN()) 0 else prog.adherencePercentage.toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                val progressValue = if (prog.totalWorkouts > 0) {
                    (prog.completedWorkouts.toFloat() / prog.totalWorkouts.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        label = { Text("Search programmes") },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun FilterSection(
    onDifficultyFilter: (ProgrammeDifficulty?) -> Unit,
    onTypeFilter: (ProgrammeType?) -> Unit,
    onClearFilters: () -> Unit,
    hasActiveFilters: Boolean = false,
) {
    var selectedDifficulty by remember { mutableStateOf<ProgrammeDifficulty?>(null) }
    var selectedType by remember { mutableStateOf<ProgrammeType?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (selectedDifficulty != null || selectedType != null || hasActiveFilters) {
                TextButton(
                    onClick = {
                        selectedDifficulty = null
                        selectedType = null
                        onClearFilters()
                    }
                ) {
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Difficulty filter section
        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ProgrammeDifficulty.values()) { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = {
                        selectedDifficulty = if (selectedDifficulty == difficulty) null else difficulty
                        onDifficultyFilter(selectedDifficulty)
                    },
                    label = { Text(formatEnumName(difficulty.name)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Type filter section
        Text(
            text = "Type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ProgrammeType.values()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = {
                        selectedType = if (selectedType == type) null else type
                        onTypeFilter(selectedType)
                    },
                    label = { Text(formatEnumName(type.name)) }
                )
            }
        }
    }
}

// Helper function to format enum names properly
private fun formatEnumName(enumName: String): String {
    return enumName.split('_')
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

@Composable
private fun ProgrammeTemplateCard(
    template: ProgrammeTemplate,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val cardColors = if (isActive) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive) { onClick() },
        colors = cardColors,
        elevation = CardDefaults.cardElevation(if (isActive) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "by ${template.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Programme details - arranged in two rows to prevent text wrapping
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: duration and difficulty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProgrammeDetail(
                        icon = Icons.Filled.Schedule,
                        text = "${template.durationWeeks} weeks"
                    )
                    ProgrammeDetail(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        text = formatEnumName(template.difficulty.name)
                    )
                }
                
                // Second row: additional features (if any)
                if (template.requiresMaxes || template.allowsAccessoryCustomization) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (template.requiresMaxes) {
                            ProgrammeDetail(
                                icon = Icons.Filled.Calculate,
                                text = "Requires 1RM"
                            )
                        }
                        if (template.allowsAccessoryCustomization) {
                            ProgrammeDetail(
                                icon = Icons.Filled.Tune,
                                text = "Customizable"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgrammeDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No programmes found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try adjusting your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}