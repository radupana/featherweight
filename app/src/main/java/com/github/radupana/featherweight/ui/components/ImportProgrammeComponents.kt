package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedWorkout

@Composable
fun ImportLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text("Analyzing your programme...")
        }
    }
}

@Composable
fun ImportHeader(
    onShowFormatTips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Import Your Programme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Paste from any source - we'll parse it!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onShowFormatTips,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Format tips",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun ProgrammeTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = text.take(10000),
            onValueChange = { newText ->
                if (newText.length <= 10000) {
                    onTextChange(newText)
                }
            },
            label = { Text("Programme Text") },
            placeholder = {
                Text(
                    "Week 1 - Volume Phase\n\n" +
                        "Monday - Upper Power\n" +
                        "Bench Press 3x5 @ 80%\n" +
                        "Barbell Row 3x5 @ 75kg\n" +
                        "Overhead Press 3x8\n\n" +
                        "Wednesday - Lower Power\n" +
                        "Squat 3x5 @ 85%\n" +
                        "Romanian Deadlift 3x8\n...",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(350.dp),
            singleLine = false,
            maxLines = Int.MAX_VALUE,
            isError = text.length > 10000,
        )

        Text(
            text = "${text.length} / 10,000 characters",
            style = MaterialTheme.typography.bodySmall,
            color =
                when {
                    text.length > 10000 -> MaterialTheme.colorScheme.error
                    text.length > 9500 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier =
                Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
        )
    }
}

@Composable
fun ImportErrorCard(
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun ImportSuccessCard(
    successMessage: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = successMessage,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun FormatTipsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Programme Parser",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FormatTipsHeader()
                HorizontalDivider()
                FormatTipsHowItWorks()
                HorizontalDivider()
                FormatTipsBestPractices()
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Got It")
            }
        },
    )
}

@Composable
private fun FormatTipsHeader() {
    Text(
        text = "Convert any workout text into a trackable programme.\nKeep your format consistent and we'll handle the rest.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FormatTipsHowItWorks() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "HOW IT WORKS",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormatTipItem(
                icon = Icons.Default.Check,
                text = "Paste workout text from any source",
                isPrimary = true,
            )
            FormatTipItem(
                icon = Icons.Default.Check,
                text = "Use any exercise names consistently",
                isPrimary = true,
            )
            FormatTipItem(
                icon = Icons.Default.Check,
                text = "Any sets/reps format (3x10, 3 sets of 10)",
                isPrimary = true,
            )
            FormatTipItem(
                icon = Icons.Default.Check,
                text = "We'll match exercises to our database",
                isPrimary = true,
            )
        }
    }
}

@Composable
private fun FormatTipsBestPractices() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "TIPS FOR BEST RESULTS",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormatTipItem(
                icon = Icons.Default.Info,
                text = "Group exercises by day or workout",
                isPrimary = false,
            )
            FormatTipItem(
                icon = Icons.Default.Info,
                text = "Include weights when known",
                isPrimary = false,
            )
            FormatTipItem(
                icon = Icons.Default.Info,
                text = "Be consistent with exercise names",
                isPrimary = false,
            )
        }
    }
}

@Composable
private fun FormatTipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isPrimary: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint =
                if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ProgrammePreview(
    programme: ParsedProgramme,
    onConfirm: () -> Unit,
    onEditWorkout: (weekIndex: Int, workoutIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var expandedWeeks by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            ImportErrorCard(errorMessage = it)
        }
        ProgrammeEditHintCard()
        ProgrammeSummaryCard(programme = programme)

        // Programme weeks
        programme.weeks.forEachIndexed { weekIndex, week ->
            ProgrammeWeekCard(
                week = week,
                isExpanded = weekIndex in expandedWeeks,
                onToggleExpanded = {
                    expandedWeeks =
                        if (weekIndex in expandedWeeks) {
                            expandedWeeks - weekIndex
                        } else {
                            expandedWeeks + weekIndex
                        }
                },
                onEditWorkout = { workoutIndex ->
                    onEditWorkout(weekIndex, workoutIndex)
                },
            )
        }

        // Unmatched exercises warning
        if (programme.unmatchedExercises.isNotEmpty()) {
            UnmatchedExercisesCard(
                unmatchedCount = programme.unmatchedExercises.size,
            )
        }

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(
                text =
                    if (programme.unmatchedExercises.isNotEmpty()) {
                        "Review Unmatched Exercises (${programme.unmatchedExercises.size})"
                    } else {
                        "Create Programme"
                    },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ProgrammeEditHintCard() {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit hint",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Tap any workout to edit exercises",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgrammeSummaryCard(
    programme: ParsedProgramme,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = programme.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProgrammeStatItem(
                    icon = Icons.Default.Schedule,
                    value = "${programme.weeks.size}",
                    label = "Weeks",
                )
                ProgrammeStatItem(
                    icon = Icons.Default.FitnessCenter,
                    value = "${programme.weeks.sumOf { it.workouts.size }}",
                    label = "Workouts",
                )
                ProgrammeStatItem(
                    icon = Icons.Default.AccessTime,
                    value = "${programme.durationWeeks}w",
                    label = "Duration",
                )
            }
        }
    }
}

@Composable
private fun ProgrammeStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgrammeWeekCard(
    week: com.github.radupana.featherweight.data.ParsedWeek,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEditWorkout: (workoutIndex: Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            // Week header (always visible)
            Surface(
                onClick = onToggleExpanded,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = week.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${week.workouts.size} workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    week.workouts.forEachIndexed { workoutIndex, workout ->
                        WorkoutCard(
                            workout = workout,
                            onEdit = { onEditWorkout(workoutIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: ParsedWorkout,
    onEdit: () -> Unit,
) {
    Surface(
        onClick = onEdit,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${workout.exercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit workout",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UnmatchedExercisesCard(
    unmatchedCount: Int,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Unmatched exercises warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = "$unmatchedCount unmatched exercises",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "You'll be able to match them to our database or create custom exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
