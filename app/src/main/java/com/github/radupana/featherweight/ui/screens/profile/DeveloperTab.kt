package com.github.radupana.featherweight.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.components.GlassmorphicCard
import com.github.radupana.featherweight.viewmodel.ProfileUiState
import com.github.radupana.featherweight.viewmodel.SeedingState

@Composable
fun DeveloperTab(
    uiState: ProfileUiState,
    onUpdateSeedingWeeks: (Int) -> Unit,
    onSeedWorkoutData: () -> Unit,
    onResetSeedingState: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Seed Workout Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Generate realistic workout data for testing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Weeks to generate: ${uiState.seedingWeeks}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        TextButton(
                            onClick = { onUpdateSeedingWeeks(uiState.seedingWeeks - 1) },
                            enabled = uiState.seedingWeeks > 1,
                        ) {
                            Text("-")
                        }
                        TextButton(
                            onClick = { onUpdateSeedingWeeks(uiState.seedingWeeks + 1) },
                            enabled = uiState.seedingWeeks < 52,
                        ) {
                            Text("+")
                        }
                    }
                }
                val seedingState = uiState.seedingState
                FilledTonalButton(
                    onClick = onSeedWorkoutData,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = seedingState !is SeedingState.InProgress,
                ) {
                    Text(
                        when (seedingState) {
                            is SeedingState.InProgress -> "Generating..."
                            is SeedingState.Success -> "Generated ${seedingState.workoutsCreated} workouts"
                            else -> "Generate Workouts"
                        },
                    )
                }
                if (seedingState is SeedingState.Success) {
                    TextButton(
                        onClick = onResetSeedingState,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Generate More")
                    }
                }
            }
        }

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Clear All Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Remove all workout data (cannot be undone)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onClearAllData,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isClearingData,
                ) {
                    Text(
                        if (uiState.isClearingData) "Clearing..." else "Clear All Workout Data",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
