package com.github.radupana.featherweight.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.radupana.featherweight.data.seeding.WorkoutSeedConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSeederDialog(
    onDismiss: () -> Unit,
    onConfirm: (WorkoutSeedConfig) -> Unit,
) {
    var squatRM by remember { mutableStateOf("140") }
    var benchRM by remember { mutableStateOf("100") }
    var deadliftRM by remember { mutableStateOf("180") }
    var ohpRM by remember { mutableStateOf("60") }

    var programStyle by remember { mutableStateOf("5/3/1") }
    var numberOfWorkouts by remember { mutableStateOf("30") }
    var workoutsPerWeek by remember { mutableStateOf(4) }
    var includeFailures by remember { mutableStateOf(true) }
    var includeVariation by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Seed Workout Data",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Generate realistic workout history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                // 1RM Inputs
                item {
                    Text(
                        "1 Rep Max Values (kg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = squatRM,
                            onValueChange = { squatRM = it },
                            label = { Text("Squat") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = benchRM,
                            onValueChange = { benchRM = it },
                            label = { Text("Bench") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = deadliftRM,
                            onValueChange = { deadliftRM = it },
                            label = { Text("Deadlift") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = ohpRM,
                            onValueChange = { ohpRM = it },
                            label = { Text("OHP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Program Style
                item {
                    Text(
                        "Program Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("5/3/1", "Linear", "Random").forEach { style ->
                            FilterChip(
                                selected = programStyle == style,
                                onClick = { programStyle = style },
                                label = { Text(style) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Configuration
                item {
                    Text(
                        "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = numberOfWorkouts,
                            onValueChange = { numberOfWorkouts = it },
                            label = { Text("Total Workouts") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Text(
                        "Workouts per Week: $workoutsPerWeek",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Slider(
                        value = workoutsPerWeek.toFloat(),
                        onValueChange = { workoutsPerWeek = it.toInt() },
                        valueRange = 3f..6f,
                        steps = 2,
                    )
                }

                // Options
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Add weight variation (±5%)")
                            Switch(
                                checked = includeVariation,
                                onCheckedChange = { includeVariation = it },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Include failed sets (~5%)")
                            Switch(
                                checked = includeFailures,
                                onCheckedChange = { includeFailures = it },
                            )
                        }
                    }
                }

                // Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val config =
                                    WorkoutSeedConfig(
                                        squatRM = squatRM.toFloatOrNull() ?: 140f,
                                        benchRM = benchRM.toFloatOrNull() ?: 100f,
                                        deadliftRM = deadliftRM.toFloatOrNull() ?: 180f,
                                        ohpRM = ohpRM.toFloatOrNull() ?: 60f,
                                        programStyle = programStyle,
                                        numberOfWorkouts = numberOfWorkouts.toIntOrNull() ?: 30,
                                        workoutsPerWeek = workoutsPerWeek,
                                        includeFailures = includeFailures,
                                        includeVariation = includeVariation,
                                    )
                                onConfirm(config)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go")
                        }
                    }
                }
            }
        }
    }
}
