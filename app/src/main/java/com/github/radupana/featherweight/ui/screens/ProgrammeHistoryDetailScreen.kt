package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.repository.ProgrammeHistoryDetails
import com.github.radupana.featherweight.repository.WorkoutHistoryEntry
import com.github.radupana.featherweight.viewmodel.ProgrammeHistoryDetailViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeHistoryDetailScreen(
    programmeId: Long,
    onBack: () -> Unit,
    onViewWorkout: (Long) -> Unit,
    viewModel: ProgrammeHistoryDetailViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val programmeDetails by viewModel.programmeDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(programmeId) {
        viewModel.loadProgrammeDetails(programmeId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programme Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Error loading programme details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProgrammeDetails(programmeId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            programmeDetails != null -> {
                ProgrammeDetailsContent(
                    details = programmeDetails!!,
                    onViewWorkout = onViewWorkout,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
fun ProgrammeDetailsContent(
    details: ProgrammeHistoryDetails,
    onViewWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Programme Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = details.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Programme metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = details.programmeType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = details.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duration and completion stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem(
                            label = "Duration",
                            value = "${details.durationWeeks} weeks",
                            modifier = Modifier.weight(1f),
                        )
                        StatItem(
                            label = "Completed",
                            value = "${details.completedWorkouts}/${details.totalWorkouts}",
                            modifier = Modifier.weight(1f),
                        )
                        StatItem(
                            label = "Total Days",
                            value = details.programDurationDays.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Dates
                    Column {
                        Text(
                            text = "Started: ${details.startedAt!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Completed: ${details.completedAt!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        
        // Workout History Section
        item {
            Text(
                text = "Workout History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        
        // Group workouts by week
        val workoutsByWeek = details.workoutHistory.groupBy { it.weekNumber }
        val sortedWeeks = workoutsByWeek.keys.sorted()
        
        sortedWeeks.forEach { weekNumber ->
            val weekWorkouts = workoutsByWeek[weekNumber]!!.sortedBy { it.dayNumber }
            val completedCount = weekWorkouts.count { it.completed }
            val totalCount = weekWorkouts.size
            
            item {
                CollapsibleWeekSection(
                    weekNumber = weekNumber,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    workouts = weekWorkouts,
                    onViewWorkout = onViewWorkout,
                )
            }
        }
    }
}

@Composable
fun CollapsibleWeekSection(
    weekNumber: Int,
    completedCount: Int,
    totalCount: Int,
    workouts: List<WorkoutHistoryEntry>,
    onViewWorkout: (Long) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column {
        // Week header - always visible
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = if (isExpanded) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isExpanded) 4.dp else 2.dp
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Week $weekNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$completedCount of $totalCount workouts completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                // Progress indicator
                if (totalCount > 0) {
                    val progress = completedCount.toFloat() / totalCount
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                
                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        
        // Workout entries - only visible when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                workouts.forEach { workout ->
                    WorkoutHistoryEntryCard(
                        workout = workout,
                        onViewWorkout = { onViewWorkout(workout.workoutId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryEntryCard(
    workout: WorkoutHistoryEntry,
    onViewWorkout: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewWorkout() },
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Day indicator
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (workout.completed) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (workout.completed) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = workout.dayNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (workout.completed) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Workout details
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = workout.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (workout.completed && workout.completedAt != null) {
                    Text(
                        text = "Completed ${workout.completedAt.format(DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Not completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            
            // Completion indicator
            if (workout.completed) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}