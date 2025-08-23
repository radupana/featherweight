package com.github.radupana.featherweight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.model.IntensityLevel
import com.github.radupana.featherweight.data.model.TimeAvailable
import com.github.radupana.featherweight.data.model.TrainingGoal
import com.github.radupana.featherweight.data.model.WorkoutTemplate
import com.github.radupana.featherweight.data.model.WorkoutTemplateConfig
import com.github.radupana.featherweight.ui.components.GlassmorphicCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateConfigurationScreen(
    template: WorkoutTemplate,
    onConfigurationComplete: (WorkoutTemplateConfig) -> Unit,
    onBack: () -> Unit,
) {
    var timeAvailable by remember { mutableStateOf<TimeAvailable?>(null) }
    var goal by remember { mutableStateOf<TrainingGoal?>(null) }
    var intensity by remember { mutableStateOf<IntensityLevel?>(null) }

    val isValid = timeAvailable != null && goal != null && intensity != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${template.name} Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Template info
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "This template focuses on:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    template.muscleGroups.forEach { muscle ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("•", style = MaterialTheme.typography.bodyMedium)
                            Text(muscle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Time selection
            ConfigurationSection(
                title = "How much time do you have?",
                selectedValue = timeAvailable,
                options = TimeAvailable.entries.toList(),
                onValueSelected = { timeAvailable = it },
                getDisplayName = { getTimeDisplayName(it) },
            )

            // Goal selection
            ConfigurationSection(
                title = "What's your goal today?",
                selectedValue = goal,
                options = TrainingGoal.entries.toList(),
                onValueSelected = { goal = it },
                getDisplayName = { getGoalDisplayName(it) },
            )

            // Intensity selection
            ConfigurationSection(
                title = "How hard do you want to push?",
                selectedValue = intensity,
                options = IntensityLevel.entries.toList(),
                onValueSelected = { intensity = it },
                getDisplayName = { getIntensityDisplayName(it) },
            )

            // Exercise selection info
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "How we choose your exercises:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "We prioritize compound movements that give you the most muscle activation per set. Exercises are selected in order of effectiveness:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Big compounds first - Multi-joint movements like squats, deadlifts, and presses",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Secondary compounds next - Movements like rows, dips, and lunges",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Isolation exercises last - Single-joint movements if time allows",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // Weight suggestion info
            GlassmorphicCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Weight suggestions based on your profile 1RM or recent history",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Generate button
            Button(
                onClick = {
                    if (isValid) {
                        onConfigurationComplete(
                            WorkoutTemplateConfig(
                                timeAvailable = timeAvailable!!,
                                goal = goal!!,
                                intensity = intensity!!,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid,
            ) {
                Text("Generate Workout")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ConfigurationSection(
    title: String,
    selectedValue: T?,
    options: List<T>,
    onValueSelected: (T) -> Unit,
    getDisplayName: (T) -> String,
) {
    GlassmorphicCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedValue == option,
                        onClick = { onValueSelected(option) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                    ) {
                        Text(
                            text = getDisplayName(option),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun getTimeDisplayName(time: TimeAvailable): String =
    when (time) {
        TimeAvailable.QUICK -> "Quick"
        TimeAvailable.STANDARD -> "Standard"
        TimeAvailable.EXTENDED -> "Extended"
    }

private fun getGoalDisplayName(goal: TrainingGoal): String =
    when (goal) {
        TrainingGoal.STRENGTH -> "Strength"
        TrainingGoal.HYPERTROPHY -> "Hypertrophy"
        TrainingGoal.ENDURANCE -> "Endurance"
    }

private fun getIntensityDisplayName(intensity: IntensityLevel): String =
    when (intensity) {
        IntensityLevel.CONSERVATIVE -> "Conservative"
        IntensityLevel.MODERATE -> "Moderate"
        IntensityLevel.AGGRESSIVE -> "Aggressive"
    }
