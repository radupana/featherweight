package com.github.radupana.featherweight.ui.components.preview

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeHeaderCard(
    preview: GeneratedProgrammePreview,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(preview.name) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Programme Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditingName) {
                    BasicTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            onNameChanged(tempName)
                            isEditingName = false
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                } else {
                    Text(
                        text = preview.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { 
                            tempName = preview.name
                            isEditingName = true 
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit name")
                    }
                }
            }
            
            // Description
            Text(
                text = preview.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            // Generation Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Generated with AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProgrammeOverviewCard(
    preview: GeneratedProgrammePreview,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Programme Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewItem(
                    label = "Duration",
                    value = "${preview.durationWeeks} weeks"
                )
                OverviewItem(
                    label = "Frequency",
                    value = "${preview.daysPerWeek} days/week"
                )
                OverviewItem(
                    label = "Volume",
                    value = preview.volumeLevel.name.lowercase().replaceFirstChar { it.uppercase() }
                )
            }
            
            // Focus Areas
            if (preview.focus.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Focus Areas:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(preview.focus) { goal ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(goal.emoji)
                                    Text(
                                        text = goal.displayName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ValidationResultCard(
    validationResult: ValidationResult,
    onFixIssue: (ValidationIssue) -> Unit,
    onBulkFix: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (validationResult.isValid) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (validationResult.isValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (validationResult.isValid) {
                        "Programme Validation Passed"
                    } else {
                        "Programme Issues Found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Validation Score
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${(validationResult.score * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Top-level Fix button for auto-fixable issues only
                    val hasAutoFixableIssues = validationResult.errors.any { it.isAutoFixable }
                    if (hasAutoFixableIssues && onBulkFix != null) {
                        TextButton(
                            onClick = onBulkFix,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "Fix",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Show errors and warnings
            validationResult.errors.forEach { error ->
                ValidationIssueItem(
                    issue = error,
                    onFix = { onFixIssue(error) }
                )
            }
            
            validationResult.warnings.forEach { warning ->
                ValidationIssueItem(
                    issue = warning,
                    onFix = { onFixIssue(warning) }
                )
            }
        }
    }
}

@Composable
private fun ValidationIssueItem(
    issue: ValidationIssue,
    onFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = when (issue.severity) {
                    ValidationSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ValidationSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (issue.severity) {
                ValidationSeverity.ERROR -> Icons.Default.Error
                ValidationSeverity.WARNING -> Icons.Default.Warning
            },
            contentDescription = null,
            tint = when (issue.severity) {
                ValidationSeverity.ERROR -> MaterialTheme.colorScheme.error
                ValidationSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            when (issue) {
                is ValidationWarning -> issue.suggestion?.let { suggestion ->
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                is ValidationError -> {
                    Text(
                        text = issue.requiredAction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        if (issue is ValidationError && issue.isAutoFixable) {
            TextButton(onClick = onFix) {
                Text("Fix")
            }
        }
    }
}

@Composable
fun WeekSelectorCard(
    weeks: List<WeekPreview>,
    selectedWeek: Int,
    onWeekSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Week Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weeks) { week ->
                    WeekTab(
                        week = week,
                        isSelected = week.weekNumber == selectedWeek,
                        onClick = { onWeekSelected(week.weekNumber) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekTab(
    week: WeekPreview,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Week ${week.weekNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "${week.weeklyVolume.totalSets} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        selected = isSelected,
        modifier = modifier
    )
}

@Composable
fun ActionButtonsCard(
    validationResult: ValidationResult,
    onRegenerate: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regenerate")
            }
            
            Button(
                onClick = onActivate,
                enabled = validationResult.isValid,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Activate")
            }
        }
        
        if (!validationResult.isValid) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Issues to fix before activating:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                
                validationResult.errors.forEach { error ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = error.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                val hasExerciseResolutionIssues = validationResult.errors.any { 
                    it.category == ValidationCategory.EXERCISE_SELECTION && !it.isAutoFixable 
                }
                if (hasExerciseResolutionIssues) {
                    Text(
                        text = "💡 Scroll down to find exercises highlighted in red and click them to select correct matches",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BulkEditCard(
    onBulkEdit: (QuickEditAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBulkOptions by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBulkOptions = !showBulkOptions },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Quick Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    if (showBulkOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showBulkOptions) "Hide options" else "Show options"
                )
            }
            
            AnimatedVisibility(
                visible = showBulkOptions,
                enter = slideInVertically() + expandVertically(),
                exit = slideOutVertically() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Apply changes to the entire programme:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            BulkEditChip(
                                label = "Reduce Volume",
                                icon = Icons.Default.Remove,
                                onClick = { onBulkEdit(QuickEditAction.AdjustVolume(0.8f)) }
                            )
                        }
                        item {
                            BulkEditChip(
                                label = "Increase Volume",
                                icon = Icons.Default.Add,
                                onClick = { onBulkEdit(QuickEditAction.AdjustVolume(1.2f)) }
                            )
                        }
                        item {
                            BulkEditChip(
                                label = "Beginner Mode",
                                icon = Icons.Default.School,
                                onClick = { onBulkEdit(QuickEditAction.SimplifyForBeginner) }
                            )
                        }
                        item {
                            BulkEditChip(
                                label = "Add Progression",
                                icon = Icons.Default.TrendingUp,
                                onClick = { onBulkEdit(QuickEditAction.AddProgressiveOverload) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        selected = false,
        modifier = modifier
    )
}