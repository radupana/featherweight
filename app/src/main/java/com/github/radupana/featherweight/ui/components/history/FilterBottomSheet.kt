package com.github.radupana.featherweight.ui.components.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryFilters(
    val selectedExercises: Set<String> = emptySet(),
    val muscleGroups: Set<String> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filters: HistoryFilters,
    availableExercises: List<String>,
    availableMuscleGroups: List<String>,
    onFiltersChanged: (HistoryFilters) -> Unit,
    onApplyFilters: (HistoryFilters) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentFilters by remember(filters) { mutableStateOf(filters) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filter Workouts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Date Range Section
                item {
                    FilterSection(
                        title = "Date Range",
                        icon = Icons.Filled.DateRange,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Start Date
                            DateFilterChip(
                                label = "Start Date",
                                date = currentFilters.startDate,
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.weight(1f),
                            )

                            // End Date
                            DateFilterChip(
                                label = "End Date",
                                date = currentFilters.endDate,
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (currentFilters.startDate != null || currentFilters.endDate != null) {
                            TextButton(
                                onClick = {
                                    currentFilters =
                                        currentFilters.copy(
                                            startDate = null,
                                            endDate = null,
                                        )
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Clear Date Range")
                            }
                        }
                    }
                }

                // Exercise Section
                if (availableExercises.isNotEmpty()) {
                    item {
                        FilterSection(
                            title = "Exercises",
                            icon = Icons.Filled.FitnessCenter,
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                availableExercises.forEach { exercise ->
                                    FilterChip(
                                        selected = exercise in currentFilters.selectedExercises,
                                        onClick = {
                                            currentFilters =
                                                if (exercise in currentFilters.selectedExercises) {
                                                    currentFilters.copy(
                                                        selectedExercises = currentFilters.selectedExercises - exercise,
                                                    )
                                                } else {
                                                    currentFilters.copy(
                                                        selectedExercises = currentFilters.selectedExercises + exercise,
                                                    )
                                                }
                                        },
                                        label = {
                                            Text(
                                                text = exercise,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        leadingIcon =
                                            if (exercise in currentFilters.selectedExercises) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                        colors =
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            ),
                                    )
                                }
                            }

                            if (currentFilters.selectedExercises.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        currentFilters = currentFilters.copy(selectedExercises = emptySet())
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Clear Exercises")
                                }
                            }
                        }
                    }
                }

                // Muscle Groups Section
                if (availableMuscleGroups.isNotEmpty()) {
                    item {
                        FilterSection(
                            title = "Muscle Groups",
                            icon = Icons.Filled.FitnessCenter,
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                availableMuscleGroups.forEach { muscleGroup ->
                                    FilterChip(
                                        selected = muscleGroup in currentFilters.muscleGroups,
                                        onClick = {
                                            currentFilters =
                                                if (muscleGroup in currentFilters.muscleGroups) {
                                                    currentFilters.copy(
                                                        muscleGroups = currentFilters.muscleGroups - muscleGroup,
                                                    )
                                                } else {
                                                    currentFilters.copy(
                                                        muscleGroups = currentFilters.muscleGroups + muscleGroup,
                                                    )
                                                }
                                        },
                                        label = {
                                            Text(
                                                text = muscleGroup,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        leadingIcon =
                                            if (muscleGroup in currentFilters.muscleGroups) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                        colors =
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            ),
                                    )
                                }
                            }

                            if (currentFilters.muscleGroups.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        currentFilters = currentFilters.copy(muscleGroups = emptySet())
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Clear Muscle Groups")
                                }
                            }
                        }
                    }
                }

                // Bottom action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                currentFilters = HistoryFilters()
                                onClearFilters()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Clear All")
                        }

                        Button(
                            onClick = {
                                onApplyFilters(currentFilters)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }
    }

    // Date Pickers
    if (showStartDatePicker) {
        DatePickerSheet(
            title = "Select Start Date",
            selectedDate = currentFilters.startDate,
            onDateSelected = { date ->
                currentFilters = currentFilters.copy(startDate = date)
            },
            onDismiss = { showStartDatePicker = false },
        )
    }

    if (showEndDatePicker) {
        DatePickerSheet(
            title = "Select End Date",
            selectedDate = currentFilters.endDate,
            onDateSelected = { date ->
                currentFilters = currentFilters.copy(endDate = date)
            },
            onDismiss = { showEndDatePicker = false },
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DateFilterChip(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color =
            if (date != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = date?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (date != null) FontWeight.Medium else FontWeight.Normal,
                    color =
                        if (date != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    title: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )

    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    val localDate =
                        millis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    onDateSelected(localDate)
                    onDismiss()
                },
                enabled = confirmEnabled,
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
